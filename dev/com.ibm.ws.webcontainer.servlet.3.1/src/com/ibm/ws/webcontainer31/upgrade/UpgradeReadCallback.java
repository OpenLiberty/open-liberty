/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.upgrade;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.servlet.ReadListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeInputStream31;
import com.ibm.ws.webcontainer31.util.UpgradeInputByteBufferUtil;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.webcontainer31.WCCustomProperties31;

/**
 *
 */
public class UpgradeReadCallback implements TCPReadCompletedCallback {

    private final static TraceComponent tc = Tr.register(UpgradeReadCallback.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    //The users ReadListener so we can callback to them
    ReadListener _rl;
    UpgradeInputByteBufferUtil _upgradeStream;
    protected SRTUpgradeInputStream31 _srtUpgradeStream;
    //ThreadContextManager to push and pop the thread's context data
    protected ThreadContextManager _contextManager;

    public UpgradeReadCallback(ReadListener rl, UpgradeInputByteBufferUtil uIBBU, ThreadContextManager tcm, SRTUpgradeInputStream31 srtUpgradeStream){
        _rl = rl;
        _upgradeStream = uIBBU;
        _contextManager = tcm;
        _srtUpgradeStream = srtUpgradeStream;
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
     */
    @Override
    @FFDCIgnore(IOException.class)
    public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {

        if(vc == null){
            return;
        }
        synchronized(_srtUpgradeStream){
            if(_upgradeStream.isClosing()){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The upgradedStream is closing, won't notify user of data");
                }
                _srtUpgradeStream.notify();
                return;
            }

            //We have read our 1 byte of data. Now configure the buffer for use when the user calls read
            _upgradeStream.configurePostInitialReadBuffer();
        }
        try{
            //Set the original Context Class Loader before calling the users onDataAvailable
            //Push the original thread's context onto the current thread, also save off the current thread's context
            _contextManager.pushContextData();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling user's ReadListener.onDataAvailable : " + _rl);
            }
            //Call into the user's ReadListener to indicate there is data available
            _rl.onDataAvailable();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User's ReadListener.onDataAvailable complete, reading for more data : " + _rl);
            }
        } catch(Throwable onDataAvailableException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ReadListener.onDataAvailable threw an exception : " + onDataAvailableException + ", " + _rl);
            }
            //Call directly into the customers ReadListener.onError
            _rl.onError(onDataAvailableException);
        } finally {
            //Revert back to the thread's current context
            _contextManager.popContextData();

            synchronized(_srtUpgradeStream){
                if(!_upgradeStream.isClosing()){
                    _upgradeStream.initialRead();
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The upgradedStream is closing, won't issue the initial read");
                    }
                    _srtUpgradeStream.notify();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext, java.io.IOException)
     */
    @Override
    @FFDCIgnore(IOException.class)
    public void error(VirtualConnection vc, TCPReadRequestContext rsc, IOException ioe) {
        boolean closing = false;
        boolean isFirstRead = false;
        synchronized(_srtUpgradeStream){
            closing = _upgradeStream.isClosing();
            isFirstRead = _upgradeStream.isFirstRead();
            _upgradeStream.setIsInitialRead(false);
        }
        if(!closing){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Encountered an error during the initial read for data : " + ioe + ", " + _rl);
            }

            try{
                //Set the original Context Class Loader before calling the users onDataAvailable
                //Push the original thread's context onto the current thread, also save off the current thread's context
                _contextManager.pushContextData();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error encountered while we are not closing : " + _rl);
                }

                if(!isFirstRead){
                    if(WCCustomProperties31.UPGRADE_READ_TIMEOUT != TCPReadRequestContext.NO_TIMEOUT && ioe instanceof SocketTimeoutException){
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Other side did not send data within the timeout time, calling onError : " + _rl);
                        }
                        if(! _upgradeStream.isIsonErrorCalled()) {
                            _upgradeStream.setIsonErrorCalled(true);
                            _rl.onError(ioe);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Other side must be closed, check if not called before call onAllDataRead : " + _rl);
                        }
                        if(! _upgradeStream.isAlldataReadCalled()) {
                            try{
                                //We will call onAllDataRead if we encounter the error because we assume the other side has closed the connection since there is no timeout currently
                                //The other side closing the connection means that we have read all the data
                                _upgradeStream.setAlldataReadCalled(true);
                                _rl.onAllDataRead();

                            } catch(Throwable onAllDataReadException){
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Encountered an error during ReadListener.onAllDataRead : " + onAllDataReadException + ", " + _rl);
                                }
                                if(! _upgradeStream.isIsonErrorCalled()) {
                                    _upgradeStream.setIsonErrorCalled(true);                                
                                    _rl.onError(onAllDataReadException);
                                }
                            }
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Encountered an error during the first initialRead for data, calling the ReadListener.onError : " + _rl);
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                        Tr.error(tc, "setReadListener.initialread.failed");

                    if(! _upgradeStream.isIsonErrorCalled()) {
                        _upgradeStream.setIsonErrorCalled(true);
                        _rl.onError(ioe);
                    }
                }
            } finally {
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "finally call close from ReadListener.onError : " + _rl);
                    }
                    _upgradeStream.getWebConn().close();
                } catch( Exception e ) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught exception during WebConnection.close : " + e + "," + _rl);
                    }
                }

                //Revert back to the thread's current context
                _contextManager.popContextData();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "We are closing, skipping the call to onError");
            }
            synchronized(_srtUpgradeStream){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Issuing the notify");
                }
                _srtUpgradeStream.notify();
            }
        }
    }

}
