/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.async;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class CompleteRunnable implements Runnable {
    
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.async");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.async.CompleteRunnable";
    
    private IExtendedRequest iExtendedRequest;
    private AsyncContextImpl asyncContextImpl;
    private SRTServletRequestThreadData requestDataOnCompleteRequestThread;;

    
    public CompleteRunnable(IExtendedRequest extendedRequest,AsyncContextImpl asyncContextImpl) {
        this.iExtendedRequest = extendedRequest;
        this.asyncContextImpl = asyncContextImpl;
       
        requestDataOnCompleteRequestThread = new SRTServletRequestThreadData();
        SRTServletRequestThreadData existing = extendedRequest != null && extendedRequest instanceof SRTServletRequest ? SRTServletRequestThreadData.getInstance(((SRTServletRequest) extendedRequest).getRequestData()) : SRTServletRequestThreadData.getInstance();
        requestDataOnCompleteRequestThread.init(existing);
    }

    @Override
    public void run() {
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "run",this);
        }
    	WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
    	 
    	 //initialize the request state in case we've already access one previously on the same thread
    	if (reqState!=null)
    		reqState.init(); 
    	
        // Add the request data from the thread on which complete was called to the request data for
        // the thread of the complete runnable.
        SRTServletRequestThreadData.getInstance().init(requestDataOnCompleteRequestThread);
   	
    	try{
    	    invokeOnComplete();
    	}
    	catch(Exception e){
    	    logger.logp(Level.FINE, CLASS_NAME, "run", "There was an exception during onComplete: "+e.getMessage());
    	}
    	finally{
            iExtendedRequest.closeResponseOutput();
            
            asyncContextImpl.setComplete(true);
            
            if (!asyncContextImpl.transferContext()) {
                asyncContextImpl.notifyITransferContextCompleteState();
            }

            asyncContextImpl.invalidate();
            
            //Allows quiesce to finish
            com.ibm.wsspi.webcontainer.WebContainer.getWebContainer().decrementNumRequests();
    
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
                logger.exiting(CLASS_NAME, "run",this);
            }
        }
    }
    
    protected void invokeOnComplete() {
    	long endTime = System.currentTimeMillis();
    	long elapsedTime = endTime - asyncContextImpl.getStartTime();
        List<AsyncListenerEntry> list = this.asyncContextImpl.getAsyncListenerEntryList();
        if (list != null) {
            // If there are other listeners: 
            // Give weld or other registered listeners the chance to add a listener to the end of the list.
            if (asyncContextImpl.registerPostEventAsyncListeners()) {
                // then refresh the list to allow for the added ones.
                list = asyncContextImpl.getAsyncListenerEntryList();
            }    
            for (AsyncListenerEntry entry : list) {
                entry.invokeOnComplete(elapsedTime);
            }
        }
    }
}
