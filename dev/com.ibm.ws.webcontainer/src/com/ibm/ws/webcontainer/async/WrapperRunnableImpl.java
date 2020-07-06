/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.webcontainer.async.ListenerHelper.CheckDispatching;
import com.ibm.ws.webcontainer.async.ListenerHelper.ExecuteNextRunnable;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.async.WrapperRunnable;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class WrapperRunnableImpl extends ServiceWrapper implements WrapperRunnable {
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.wsspi.webcontainer.async");
    private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.async.WrapperRunnableImpl";
    
    private Runnable runnable;
    private AsyncContextImpl asyncContext;
    private SRTServletRequestThreadData requestDataOnStartRequestThread;

    
    public WrapperRunnableImpl(Runnable run, AsyncContextImpl asyncContext, IExtendedRequest extendedRequest) {
            super(asyncContext);
            this.runnable = run;
            this.asyncContext = asyncContext;
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST, CLASS_NAME, "constructor", " "+ this);
            }
            
            requestDataOnStartRequestThread = new  SRTServletRequestThreadData();
            SRTServletRequestThreadData existing = extendedRequest != null && extendedRequest instanceof SRTServletRequest ? SRTServletRequestThreadData.getInstance(((SRTServletRequest) extendedRequest).getRequestData()) : SRTServletRequestThreadData.getInstance();
            requestDataOnStartRequestThread.init(existing);
    }


    @Override
    public void run() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
            logger.entering(CLASS_NAME,"run",this);
        }

        // Start:PM90834
        if (!this.asyncContext.transferContext()) {
            popContextData();
        }
        // End:PM90834

        synchronized(asyncContext){
            asyncContext.removeStartRunnable(this);
        }

        //we could try to run this runnable even though it will be removed from the list
        //if the expiration timer executes. Therefore add AtomicBoolean to see if we've already run
        //it or cancelled it.
        if (!getAndSetRunning(true)){
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
                logger.logp(Level.FINEST, CLASS_NAME, "run", "running");
            }
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
            if (reqState!=null)
            {
                reqState.init();
            }

            // Add the request data from the thread on which start was called to the request data for
            // the thread of thet started runnable.
            SRTServletRequestThreadData.getInstance().init(requestDataOnStartRequestThread);

            //The spec says "The container MAY take care of the errors from the thread issued via AsyncContext.start."
            //We will catch an error and invoke async error handling, but allow an already dispatched thread to continue processing
            //We do not need to complete the async context as this will be done either by the error handling on the thread which created this (?)
            try {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "run", "Context Class loader before run: " + Thread.currentThread().getContextClassLoader());
                }
                runnable.run();
            } catch(Throwable th) {
                logger.logp(Level.WARNING, CLASS_NAME, "run", "error.occurred.during.async.servlet.handling", th);
                ListenerHelper.invokeAsyncErrorHandling(asyncContext, reqState, th, AsyncListenerEnum.ERROR,ExecuteNextRunnable.FALSE,CheckDispatching.TRUE);
            }
        }
        else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
                logger.logp(Level.FINEST, CLASS_NAME, "run", "not running because it has already ran or been cancelled");
            }
        }

        // Start:PM90834
        if (!this.asyncContext.transferContext()) {
            resetContextData();
        }
        // End:PM90834

        SRTServletRequestThreadData.getInstance().init(null);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
            logger.exiting(CLASS_NAME,"run",this);
        }
    }
    
    public String toString(){
            return "WrapperRunnable hashCode->" + this.hashCode() + ", start(runnable)->" + runnable;
    }
    
    private AtomicBoolean running = new AtomicBoolean(false);

    public boolean getAndSetRunning(boolean b) {
            return running.getAndSet(b);
    }


}
