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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTInputStream31;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;



/**
 *T his class is required when application has set ReadListener on an input stream but the post data has already
 * been fully read as a result of the login procedure.
 * 
 * As all data is read just loop round calling onDataAvailable() until all data is read and then call onAllDataRead.
 * 
 * Added since Servlet 3.1
 * 
 */
public class AsyncAlreadyReadCallback implements InterChannelCallback {

    private final static TraceComponent tc = Tr.register(AsyncAlreadyReadCallback.class, WebContainerConstants.TR_GROUP, LoggerFactory.MESSAGES);
    //Reference to the SRTInputStream31 that created this particular callback
    private SRTInputStream31 in;
    //ThreadContextManager to push and pop the thread's context data
    private ThreadContextManager threadContextManager;
    private SRTServletRequestThreadData _requestDataAsyncReadCallbackThread;
    
    public AsyncAlreadyReadCallback(SRTInputStream31 in, ThreadContextManager tcm){
        this.in = in;
        this.threadContextManager = tcm;
        _requestDataAsyncReadCallbackThread = SRTServletRequestThreadData.getInstance(in.getRequest().getRequestData());
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.channelfw.InterChannelCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    @FFDCIgnore(IOException.class)
    public void complete(VirtualConnection vc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "complete",  vc);
        }
        synchronized( this.in.getCompleteLockObj()){
            
            SRTServletRequestThreadData.getInstance().init(_requestDataAsyncReadCallbackThread);
            
            //Push the original thread's context onto the current thread, also save off the current thread's context
            this.threadContextManager.pushContextData();

            //Call into the user's ReadListener to indicate there is data available
            try{
                if (!in.isFinished()) {
                    this.in.getReadListener().onDataAvailable();
                }   
            } catch (Throwable onDataAvailableException){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception occurred during ReadListener.onDataAvailable : " + onDataAvailableException + ", " + this.in.getReadListener());
                }
                this.threadContextManager.popContextData();
                error(vc, onDataAvailableException);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "complete");
                }
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                 Tr.debug(tc, "Message is fully read, calling ReadListener onAllDataRead : " + this.in.getReadListener());
            }
            
            if(this.in.getReadListener() != null){
                // if all the data has been read we are done.
                if (in.isFinished()) {
                    try{
                        this.in.getReadListener().onAllDataRead();
                    } catch (Throwable onAllDataReadException){
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception occurred during ReadListener.onAllDataRead : " + onAllDataReadException + ", " + this.in.getReadListener());
                        }
                        this.threadContextManager.popContextData();
                        error(vc, onAllDataReadException);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "complete");
                        }
                        return;
                    }
                } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "onDataAavailabe returned without reading all data. Read Listener will no be called again : "  + this.in.getReadListener());
                }
                else{
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ReadListener is not set , stream must be closed, cannot call onAllDataRead()");
                    }
                }
            }
            
            //Revert back to the thread's current context
            this.threadContextManager.popContextData();

        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "complete");
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.channelfw.InterChannelCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Throwable)
     */
    @Override
    public void error(VirtualConnection vc, Throwable t) { 
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Calling user's ReadListener onError : " + this.in.getReadListener());
        }
        Exception e = null;
        
        SRTServletRequestThreadData.getInstance().init(_requestDataAsyncReadCallbackThread);
        
        //Push the original thread's context onto the current thread, also save off the current thread's context
        this.threadContextManager.pushContextData();
        
        if(this.in.getReadListener() != null){
            synchronized( this.in.getCompleteLockObj()){
                try {
                    //An error occurred. Issue the onError call on the user's ReadListener
                    this.in.getReadListener().onError(t);
                } catch (Exception onErrorException) {
                    e = onErrorException;
                }
            }

            if (e != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occurred during ReadListener.onError : " + e + ", " + this.in.getReadListener());
            }
            else{
                if (t != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception occurred during ReadListener and error cannot handle : " + t.getMessage() + ", " + this.in.getReadListener());
                }
            }
        }
        
        //Revert back to the thread's current context
        this.threadContextManager.popContextData();

    }

}
