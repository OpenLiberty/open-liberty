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
 *This class is required when application has set ReadListener on an input stream.
 * 
 * When the async read requested is completed or get an error at OS the callback is generated from the TCP. 
 * This class will take the appropriate action i.e. call the application API's based on the callback.
 * 
 * Added since Servlet 3.1
 * 
 */
public class AsyncReadCallback implements InterChannelCallback {

    private final static TraceComponent tc = Tr.register(AsyncReadCallback.class, WebContainerConstants.TR_GROUP, LoggerFactory.MESSAGES);

    //Reference to the SRTInputStream31 that created this particular callback
    private SRTInputStream31 in;
    //ThreadContextManager to push and pop the thread's context data
    private ThreadContextManager threadContextManager;
    private SRTServletRequestThreadData _requestDataAsyncReadCallbackThread;

    public AsyncReadCallback(SRTInputStream31 in, ThreadContextManager tcm){
        this.in = in;
        this.threadContextManager = tcm;
        _requestDataAsyncReadCallbackThread = SRTServletRequestThreadData.getInstance();
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
        if (null == vc) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "complete");
            }
            return;
        }
        //We don't need to do an initial seeding of the buffer here as HTTP Channel has already done that in their
        //complete callback from the TCP Channel. When we call an async read on them they will return indicating
        //there is data to read
        synchronized( this.in.getCompleteLockObj()){

            //This variable was introduced to prevent us from calling into Channel again when there is an outstanding ready
            //Once isReady returns false once, we don't want to change it back until the next call into onDataAvailable
            //This variable prevents isReady from returning true if there is an outstanding read           
            this.in.setAsyncReadOutstanding(false);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling user's ReadListener onDataAvailable : " + this.in.getReadListener());
            }       
            
            SRTServletRequestThreadData.getInstance().init(_requestDataAsyncReadCallbackThread);
            
            //Push the original thread's context onto the current thread, also save off the current thread's context
            this.threadContextManager.pushContextData();

            //Call into the user's ReadListener to indicate there is data available
            try{
                this.in.getReadListener().onDataAvailable();
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
            
            if(this.in.getReadListener() != null){ // the stream may have been closed during onDataAvailable
                // cannot call onAllDataRead()

                //Determine if the message has been fully read. If so call the user's ReadListener to indicate all data has been read
                //If the message isn't fully read then issue a forced async read to the channel
                if(in.isFinished()){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Message is fully read, calling ReadListener onAllDataRead : " + this.in.getReadListener());
                    }
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
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Data hasn't been fully read yet. There should be an outstanding read at this point : " + this.in.getReadListener());
                    }
                }  
            }
            else{
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ReadListener is not set , stream must be closed, cannot call onAllDataRead()");
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
                this.in.setAsyncReadOutstanding(false);
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
        }
        else{
            if (t != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occurred during ReadListener and error cannot handle : " + t.getMessage() + ", " + this.in.getReadListener());
            }
        }
        
        //Revert back to the thread's current context
        this.threadContextManager.popContextData();

    }

}
