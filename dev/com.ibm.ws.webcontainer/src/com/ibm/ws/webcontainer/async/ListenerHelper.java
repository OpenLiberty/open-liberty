/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.async;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ws.webcontainer.collaborator.CollaboratorMetaDataImpl;
import com.ibm.ws.webcontainer.servlet.IServletContextExtended;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.async.WrapperRunnable;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.logging.LoggerHelper;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.AsyncContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.util.ServletUtil;

public class ListenerHelper {
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.async");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.async.ListenerHelper";
    protected static TraceNLS nls = TraceNLS.getTraceNLS(ListenerHelper.class, "com.ibm.ws.webcontainer.resources.Messages");
    public enum ExecuteNextRunnable {
    	TRUE, FALSE
    }
    
    public enum CheckDispatching {
    	TRUE, FALSE
    }
    
    public static void invokeAsyncErrorHandling(AsyncContext asyncContext, WebContainerRequestState reqState, Throwable th, AsyncListenerEnum asyncEnum,ExecuteNextRunnable executeNextRunnable,CheckDispatching checkDispatching) {
    	AsyncServletReentrantLock lock = asyncContext.getErrorHandlingLock();
    	invokeAsyncErrorHandling(asyncContext, reqState, th, asyncEnum,executeNextRunnable,checkDispatching,lock);
    }
    
    public static void invokeAsyncErrorHandling(AsyncContext asyncContext, WebContainerRequestState reqState, Throwable th, AsyncListenerEnum asyncEnum,ExecuteNextRunnable executeNextRunnable,CheckDispatching checkDispatching,AsyncServletReentrantLock lock) {
	    	boolean unlock=false;
	    	boolean invokeAsyncErrorHandling=false;
	    	
    		try{
	    		
				synchronized(asyncContext){
					boolean gotValidLock=false;
					if (lock.tryLock()){
		        		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))
		        		{
		        			logger.logp(Level.FINER,CLASS_NAME,"invokeAsyncErrorHandling","locking by current thread->"+lock);
		        		}
		        		if (lock.getAndSetIsValid(false))
		        			gotValidLock=true;
		        		
			        	unlock = true; //unlock regardless of whether it was a "valid" lock
			        	
		        	} else {
		        		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))
		        		{
		        			logger.logp(Level.FINER,CLASS_NAME,"invokeAsyncErrorHandling","lock is held by a different thread so skipping->"+lock);
		        		}
		        	}
					if (gotValidLock){
						asyncContext.cancelAsyncTimer(); // cancel the async timer so it too doesn't try to invokeAsyncErrorHandling
						
						//cancel all the runnables scheduled
						Collection<WrapperRunnable> startRunnables = asyncContext.getAndClearStartRunnables();
				    	if (startRunnables!=null){
							for (WrapperRunnable startRunnable:startRunnables){
								 startRunnable.getAndSetRunning(true);
							}
				    	}
				    	
						if (checkDispatching==CheckDispatching.TRUE&&asyncContext.isDispatching()){	//If we were dispatching when workRejected was called,
							asyncContext.setInvokeErrorHandling(true); //asyncContext can't enter executeNextRunnable
																	   //and not have invokeErrorHandling be true.
																	   //In which case, the original dispatching thread 
																	   //calls invokeAsyncErrorHandling
							
							//need to reset the lock to be valid so that when we exit dispatch we can reexecute this method
							lock.getAndSetIsValid(true);
							if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
								logger.logp(Level.FINER, CLASS_NAME, "workRejected", "still dispatching so invoke error handling later");
							}
						}
						//    	In certain cases _invokeAsyncErrorHandling will be called back to back and we don't want to 
						//    	re-execute it if the asyncContext is already complete or pending complete or dispatch
						else if (asyncContext.isComplete()){
					    		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))
					    		{
					    			logger.logp(Level.FINER,CLASS_NAME,"_invokeAsyncErrorHandling","asyncContext is already completed");
					    		}
				    	} 
						else if (asyncContext.isCompletePending()||asyncContext.isDispatchPending()){
							
				    		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))
				    		{
				    			logger.logp(Level.FINER,CLASS_NAME,"_invokeAsyncErrorHandling","asyncContext is already pending complete or dispatch");
				    		}
				    		
				    		//There's a tiny window when invokeAsyncErrorHandling can be called from processExpiredEntry or workRejected
				    		//where complete or dispatch was called from another thread with dispatching=true.
				    		
				    		if (executeNextRunnable==ExecuteNextRunnable.TRUE)
				        		asyncContext.executeNextRunnable();
				    	}
						else {
							//set dispatching to true so async listeners invoking dispatch back to back will
							//be prevented
							asyncContext.setDispatching(true);
							invokeAsyncErrorHandling = true;
						}
					}
				}
	        	if (invokeAsyncErrorHandling){
	        		_invokeAsyncErrorHandling(asyncContext,reqState,th,asyncEnum,executeNextRunnable);
	        	}
    		}
	        finally {
	        	if (unlock)
	        		lock.unlock();
	        }
	    	
    }
	
    /*
	* You cannot invokeAsyncErrorHandling from more than one thread at a time 
	* because we retrieve the error handling lock. Once that lock is retrieved
	* another thread cannot call complete or dispatch. Those calls will be ignored.
	* 
	* If complete or dispatch happens to get called right before we retrieve the lock,
	* but after the timeout or whatever occured, we will not do anything here because of
	* the check to isCompleted, etc.
	* 
	*/
    private static void _invokeAsyncErrorHandling(AsyncContext asyncContext, WebContainerRequestState reqState, Throwable th, AsyncListenerEnum asyncEnum,ExecuteNextRunnable executeNextRunnable) {
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
    		logger.entering(CLASS_NAME,"_invokeAsyncErrorHandling", new Object [] {asyncContext,reqState,th,asyncEnum});
	    	
	    	try {
		    	ServletRequest servletRequest = asyncContext.getRequest();
		        ServletResponse servletResponse = asyncContext.getResponse();
		        IExtendedRequest wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(servletRequest);
	        	HttpServletResponse httpRes = (HttpServletResponse) ServletUtil.unwrapResponse(servletResponse, HttpServletResponse.class);
	        		        
		    	IServletContext iServletContext = asyncContext.getWebApp();
		    	ICollaboratorHelper collabHelper = ((IServletContextExtended) iServletContext).getCollaboratorHelper();
		    	
		    	WebComponentMetaData componentMetaData = iServletContext.getWebAppCmd();
		    	
		    	
		        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) wasreq.getWebAppDispatcherContext();
		    	
		    	CollaboratorMetaDataImpl collabMetaData = new CollaboratorMetaDataImpl(componentMetaData, wasreq, httpRes, dispatchContext, null,
		    			iServletContext,null);
		    	
		    	try {
		    	    if (reqState != null)
		    	        reqState.setAttribute("_invokeAsyncErrorHandling", true);

		    	    collabHelper.preInvokeCollaborators(collabMetaData, CollaboratorHelper.allCollabEnum);

		    	    List<AsyncListenerEntry> list = asyncContext.getAsyncListenerEntryList();
		    	    if (list != null) {
		    	        // If there are other listeners: 
		    	        // Give weld or other registered listeners the chance to add a listener to the end of the list.
		    	        try { 
		    	            if (((AsyncContextImpl)asyncContext).registerPostEventAsyncListeners()) {
		    	                // then refresh the list to allow for the added ones.
		    	                list = asyncContext.getAsyncListenerEntryList();
		    	            }    
		    	        } catch (java.lang.ClassCastException exc) {
		    	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))
		    	                logger.fine("_invokeAsyncErrorHandling cannot cast AsyncContext to an impl class so post event Asynclistenrs will not be registered");
		    	        }
		    	        for (AsyncListenerEntry entry : list) {
		    	            try {
		    	                if (asyncEnum==AsyncListenerEnum.ERROR)
		    	                    entry.invokeOnError(th);
		    	                else
		    	                    entry.invokeOnTimeout();
		    	            } catch (Throwable throwable){
		    	                LoggerHelper.logParamsAndException(logger, Level.WARNING,CLASS_NAME,"_invokeAsyncErrorHandling","exception.invoking.async.listener",new Object[] {entry.getAsyncListener()},throwable);
		    	            }
		    	        }
		    	    }

		    	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
		    	        logger.logp(Level.FINEST, CLASS_NAME, "_invokeAsyncErrorHandling", 
		    	                    "vals after listeners [asyncContext.isComplete(),asyncContext.isCompletePending(),asyncContext.isDispatchPending()]->["
		    	                                    +asyncContext.isComplete()+","
		    	                                    + asyncContext.isCompletePending()+","
		    	                                    + asyncContext.isDispatchPending()+"]");
		    	    }

		    	    //only invoke sendError if we didn't complete or dispatch from the listeners
		    	    if (!asyncContext.isComplete()&&!asyncContext.isCompletePending()&&!asyncContext.isDispatchPending()){
		    	        if (th!=null){
		    	            ServletErrorReport ser;
		    	            if (!(th instanceof ServletErrorReport))
		    	            {
		    	                ser = new ServletErrorReport(th);

		    	            } else{
		    	                ser = (ServletErrorReport)th;
		    	            }
		    	            ser.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		    	            asyncContext.getWebApp().sendError(wasreq, httpRes, ser);
		    	        } else {
		    	            //TODO: change this to webApp send error or something else that will prevent IllegalstateException from being thrown
		    	            IExtendedRequest iExtendedRequest = ServletUtil.unwrapRequest(servletRequest, IExtendedRequest.class);
		    	            iExtendedRequest.getWebAppDispatcherContext().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		    	                                                                    nls.getFormattedMessage("error.occurred.during.async.servlet.handling", new Object[] {asyncEnum}, asyncEnum + " occurred during async handling"),true);
		    	        }
		    	    }
		    	} finally {
		    	    collabHelper.postInvokeCollaborators(collabMetaData, CollaboratorHelper.allCollabEnum);

		    	    if (reqState != null)
		    	        reqState.removeAttribute("_invokeAsyncErrorHandling");
		    	}
	        } catch (Throwable throwable){
	        	logger.logp(Level.WARNING,CLASS_NAME,"_invokeAsyncErrorHandling","exception.invoking.asnyc.error.mechanism",throwable);
	        } finally {        	
	            //if we still haven't completed or dispatched, complete now
	            if (!asyncContext.isComplete()&&!asyncContext.isCompletePending()&&!asyncContext.isDispatchPending())
	                asyncContext.complete();

	            asyncContext.executeNextRunnable();

	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
	                logger.exiting(CLASS_NAME,"_invokeAsyncErrorHandling", asyncContext);
	        }
    }

}
