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
package com.ibm.ws.webcontainer31.async;

import java.io.IOException;

import javax.servlet.WriteListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.response.WCOutputStream31;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.ee7.HttpOutputStreamEE7;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;

/**
 * This class is required when application has set WriteListener on an output stream.
 * 
 * When the async write requested is completed or get an error at OS the callback is generated from the TCP. 
 * This class will take the appropriate action i.e. call the application API's based on the callback.
 * 
 * Added since Servlet 3.1
 * 
 */
public class AsyncWriteCallback implements InterChannelCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(AsyncWriteCallback.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );

    //The users WriteListener so we can callback to them
    private WriteListener _wl;
    //Reference to the OutputStream that created this particular callback
    private WCOutputStream31 _out;
    private HttpOutputStreamEE7 _hout = null;
    //The ThreadContextManager object which allows us to push and pop thread's context data
    private ThreadContextManager tcm;
    private SRTServletRequestThreadData _requestDataAsyncWriteCallbackThread;

    /**
     * @param wl
     * @param out
     * @param hout
     * @param originalCL
     */
    public AsyncWriteCallback(WriteListener wl, WCOutputStream31 out, HttpOutputStreamEE7 hout, ThreadContextManager tcm){
        this._wl = wl;
        this._out = out;
        this._hout = hout;
        this.tcm = tcm;
        _requestDataAsyncWriteCallbackThread = SRTServletRequestThreadData.getInstance();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "AsyncWriteCallback created, " + this._wl +  ", hout --> "+this._hout +" ,current thread -->"+ Thread.currentThread().getName());  
        }  


    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.channelfw.InterChannelCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    public void complete(VirtualConnection vc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
            Tr.debug(tc, "complete callback called , WriteListener enabled: " + this._wl); 


        synchronized(_hout) {
            if (null == vc) {
                return;
            }
            if (_hout.getExceptionDuringOnWP()){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                    Tr.debug(tc, "earlier exception happened, onError was called , WriteListener enabled: " + this._wl); 
                _hout.setExceptionDuringOnWP(false);
                return;
            }
            //clean up any request state we had on this thread.
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);                 
            reqState.init();

            try {                    
                _hout.writeRemainingToBuffers();
            } catch (Exception ex) {
                //Print exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                    Tr.debug(tc, "Exception during writeRemainingToBuffers : " + ex);
                }
                this.error(vc, ex);
                return;
            }

            // everything must be written now , 
            if (_hout.get_internalReady()) {
                // this case will only be true if write which went aysnc was from println, now we have to write crlf
                if(_hout.write_crlf_pending){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                        Tr.debug(tc, "ready to write CRLF bytes  , WriteListener enabled: " + this._wl); 
                    try {                    
                        //_hout.set_internalReady(true); // since CRLF needs to be written we are still not external ready
                        reqState.setAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread", true);        
                        reqState.setAttribute("com.ibm.ws.webcontainer.CRLFWriteinPorgress", true);                            
                        _out.writeCRLFIfNeeded();
                    } catch (IOException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                            Tr.debug(tc, "Exception during write CRLF bytes: " + e);
                        }
                        _hout.write_crlf_pending = false;
                        this.error(vc, e);                       
                        return;
                    }                   
                }
            }
            if (_hout.get_internalReady()) {
                synchronized(_hout._writeReadyLockObj){   

                    //Setting isReady to true since a complete call means data is written out                      
                    WebContainerRequestState.getInstance(true).setAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread", true);
                    _hout.setWriteReady(true);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                    Tr.debug(tc, "WriteListener enabled: " + this._wl + " , status_not_ready_checked -->" + _hout.status_not_ready_checked);
                }
                if( _hout.status_not_ready_checked) {                                       
                    try{
                        _hout.status_not_ready_checked = false;
                        
                        SRTServletRequestThreadData.getInstance().init(_requestDataAsyncWriteCallbackThread);
                        
                        //Push the original thread's context onto the current thread, also save off the current thread's context
                        tcm.pushContextData();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                            Tr.debug(tc, "WriteListener enabled: " + this._wl + " , call onWritePossible.");
                        }
                        //Call into the user's WriteListener to indicate more data cane be written
                        _wl.onWritePossible();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                            Tr.debug(tc, "WriteListener enabled: " + this._wl + " , returned from onWritePossible.");
                        }
                    } catch (Exception ex) {
                        //Print exception
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                            Tr.debug(tc, "Exception during onWritePossible : " + ex);
                        }
                        this.error(vc, ex);    
                    }
                    finally{
                        //Revert back to the thread's current context
                        tcm.popContextData();
                    }
                }
                else{
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                        Tr.debug(tc, "WriteListener enabled: " + this._wl + " , onWritePossible will be skipped as isReady has not been checked since write has gone async");
                    }
                }
            }
        }


    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.channelfw.InterChannelCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Throwable)
     */
    @Override
    public void error(VirtualConnection vc, Throwable t) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
            Tr.debug(tc, "error callback called , WriteListener enabled: " + this._wl); 

        // reset the reqState
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);                         
        reqState.init();  

        synchronized(_hout._writeReadyLockObj){
            // Make sure the ready is set to true before calling onError
            _hout.set_internalReady(true);            
            _hout.setWriteReady(true);   
            reqState.setAttribute("com.ibm.ws.webcontainer.AllowWriteFromE", true); 
            reqState.setAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread", true);
        }
        
        SRTServletRequestThreadData.getInstance().init(_requestDataAsyncWriteCallbackThread);
        
        //Push the original thread's context onto the current thread, also save off the current thread's context
        tcm.pushContextData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "WriteListener enabled: " + this._wl +" , calling user's onError : " + vc );            
        }       
        try{  
            synchronized(_hout) {
                //An error occurred. Issue the onError call on the user's WriteListener
                _wl.onError(t);
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "WriteListener enabled: " + this._wl +" , returned from user's onError : " + vc );            
            }  
        } catch (Exception e) {
             
            Tr.error(tc, "writeListener.onError.failed", new Object[] {this._wl, e.toString()});
            
        }
        finally{

            tcm.popContextData();//Revert back to the thread's current context

            reqState.removeAttribute("com.ibm.ws.webcontainer.AllowWriteFromE");
        }
    }


}
