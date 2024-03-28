/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer31.upgrade;

import java.io.IOException;

import javax.servlet.ReadListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeInputStream31;
import com.ibm.ws.webcontainer31.util.UpgradeInputByteBufferUtil;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 *
 */
public class NettyUpgradeReadCallBack extends UpgradeReadCallback {
    
    private final static TraceComponent tc = Tr.register(NettyUpgradeReadCallBack.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * @param rl
     * @param uIBBU
     * @param tcm
     * @param srtUpgradeStream
     */
    public NettyUpgradeReadCallBack(ReadListener rl, UpgradeInputByteBufferUtil uIBBU, ThreadContextManager tcm, SRTUpgradeInputStream31 srtUpgradeStream) {
        super(rl, uIBBU, tcm, srtUpgradeStream);
        // TODO Auto-generated constructor stub
    }

    @Override
    @FFDCIgnore(IOException.class)
    public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {
        synchronized(_srtUpgradeStream){
            if(_upgradeStream.isClosing()){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The upgradedStream is closing, won't notify user of data");
                }
                _srtUpgradeStream.notify();
                return;
            }
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
                if(_upgradeStream.isClosing()){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The upgradedStream is closing, won't issue the initial read");
                    }
                    _srtUpgradeStream.notify();
                }
            }
        }
    }
    
}
