/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.upgrade;

import java.io.IOException;

import javax.servlet.WriteListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeOutputStream31;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;

/**
 * This class is required when application has set WriteListener on an Upgraded output stream.
 * When the async write requested is completed or get an error at OS the callback is generated from the TCP. 
 * This class will take the appropriate action i.e. call the application API's based on the callback.
 * 
 * Added since Servlet 3.1
 * 
 */
public class UpgradeAsyncWriteCallback implements TCPWriteCompletedCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(UpgradeAsyncWriteCallback.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );

    //The users WriteListener so we can callback to them
    private WriteListener _wl;
    private SRTUpgradeOutputStream31 _upgradeOut;
    //ThreadContextManager to push and pop the thread's context data
    private ThreadContextManager _contextManager;
    private UpgradedWebConnectionImpl _upCon = null;

    /**
     * @param _listener
     * @param upgradeOut
     * @param tcm
     * @param _upConn
     */
    public UpgradeAsyncWriteCallback(WriteListener _listener, SRTUpgradeOutputStream31 upgradeOut, ThreadContextManager tcm, UpgradedWebConnectionImpl _upConn) {
        this._wl = _listener;
        this._upgradeOut = upgradeOut;
        this._contextManager = tcm;
        _upCon = _upConn;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "Upgrade Async Callback, current thread -->"+ Thread.currentThread().getName());  
        }  
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext)
     */
    @Override
    public void complete(com.ibm.wsspi.channelfw.VirtualConnection vc, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext wsc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  
            Tr.debug(tc, "complete called : " + vc + "WriteListener enabled: " + this._wl);

        synchronized(_upgradeOut) {

            if (null == vc) {
                return;
            }

            if (_upgradeOut.getBufferHelper().isOutputStream_closed()) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "complete , outputStream closed ignoring complete : " + vc);

                return;
            }

            // clean up any request state we had on this thread.
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
            reqState.init();
            try{    
                _upgradeOut.getBufferHelper().writeRemainingToBuffers();                 

            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                    Tr.debug(tc, "exception during writeBuffers" + e.toString());
                }
                this.error(vc, e);
                return;
            }


            // rest must be written now ,
            // this case will only be true if write which went aysnc was
            // from println, now we have to write crlf
            if ( _upgradeOut.getBufferHelper().isInternalReady() && _upgradeOut.getBufferHelper().write_crlf_pending) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "write CRLF bytes  , WriteListener enabled: " + this._wl);
                try {
                    reqState.setAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread", true);
                    reqState.setAttribute("com.ibm.ws.webcontainer.upgrade.CRLFWriteinPorgress", true);
                    _upgradeOut.getBufferHelper().writeCRLFIfNeeded();
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                        Tr.debug(tc, " Exception during write CRLF bytes: " + e);

                    _upgradeOut.getBufferHelper().write_crlf_pending = false;
                    this.error(vc, e);
                    return;
                }
            }

            if( _upgradeOut.getBufferHelper().isInternalReady()){
                WebContainerRequestState.getInstance(true).setAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread", true);

                _upgradeOut.getBufferHelper().setReadyForApp(true);

                // if close was in progress
                if(_upgradeOut.getBufferHelper().isOutputStream_close_initiated_but_not_Flush_ready()){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                        Tr.debug(tc, "complete , In process of closing outputStream, no more data is left to write, call close");
                    }
                    try {
                        if(_upCon.isOutputStream_CloseStartedFromWC()){
                            //at this point inputstream and handler are already closed
                            _upCon.closeOutputandConnection();
                        }
                        else{
                            _upgradeOut.close();
                        }
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "complete , stack exception" + e.toString());

                        this.error(vc, e);
                    }

                }// no close in progress now check if onWP needs to be called.
                else{
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                        Tr.debug(tc, "WriteListener enabled: " + this._wl + " ,status_not_ready_checked -->" + _upgradeOut.getBufferHelper().status_not_ready_checked);
                    }
                    if (_upgradeOut.getBufferHelper().status_not_ready_checked) {
                        try{
                            _upgradeOut.getBufferHelper().status_not_ready_checked = false;

                            // Push the original thread's context onto the current thread,
                            // also save off the current thread's context
                            _contextManager.pushContextData();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                                Tr.debug(tc, "Calling user's WriteListener onWritePossible");
                            }
                            // Call into the user's WriteListener to indicate more data cane
                            // be written
                            _wl.onWritePossible();
                        } catch (Exception e) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                                Tr.debug(tc, "stack exception" + e.toString());
                            }
                            this.error(vc, e);
                        } finally {
                            // Revert back to the thread's current context
                            _contextManager.popContextData();
                        }
                    }
                    else{
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                            Tr.debug(tc, "WriteListener enabled: " 
                                            + this._wl + " .onWritePossible will be skipped as isReady has not been checked since write went async.");
                        }
                    }
                }
            }
        }
    }



    /**
     * @param vc
     * @param t
     */
    public void error(VirtualConnection vc, Throwable t) {       
        synchronized(_upgradeOut) { 
            if (null == vc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "error , vc is null , connection must be closed ");
                return;
            }
            if (_upgradeOut.getBufferHelper().isOutputStream_closed()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "error , outputStream closed ignoring error : " + vc);
                return;
            }

            // reset the reqState
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);                         
            reqState.init(); 

            _upgradeOut.getBufferHelper().setInternalReady(true);
            _upgradeOut.getBufferHelper().setReadyForApp(true);
            reqState.setAttribute("com.ibm.ws.webcontainer.upgrade.AllowWriteFromE", true); 
            reqState.setAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread", true);
            try {
                _contextManager.pushContextData();                                  
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {  
                    Tr.debug(tc, "Calling user's WriteListener onError : " + vc + ", " + t);
                }
                _wl.onError(t);
            } catch (Exception e) {
                Tr.error(tc, "writeListener.onError.failed", new Object[] {this._wl, e.toString()} );

            }
            finally {
                // Revert back to the thread's current context
                _contextManager.popContextData();
                reqState.removeAttribute("com.ibm.ws.webcontainer.upgrade.AllowWriteFromE");
            } 
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext, java.io.IOException)
     */
    @Override
    public void error(com.ibm.wsspi.channelfw.VirtualConnection vc, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext wsc, IOException ioe) {
        // This error is a TCP callback.

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  
            Tr.debug(tc, "error callback called , WriteListener enabled: " + this._wl); 

        this.error(vc, ioe); 

    }
}
