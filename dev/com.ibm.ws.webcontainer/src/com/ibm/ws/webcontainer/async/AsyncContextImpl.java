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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.webcontainer.async.ListenerHelper.CheckDispatching;
import com.ibm.ws.webcontainer.async.ListenerHelper.ExecuteNextRunnable;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.async.WrapperRunnable;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.AsyncContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextService;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextServiceExt;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

/**
 * @author mmolden
 * 
 */
public class AsyncContextImpl implements AsyncContext {
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.async");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.async.AsyncContextImpl";


    protected IExtendedRequest iExtendedRequest;
    private IExtendedResponse iExtendedResponse;

    protected ServletRequest servletRequest;
    protected ServletResponse servletResponse;

    protected IServletContext webApp;

    public IServletContext getWebApp() {
        return webApp;
    }

    public void setWebApp(IServletContext webApp) {
        this.webApp = webApp;
    }

    protected String originalRequestURI;

    private boolean completePending = false;
    private boolean dispatching = true;

    public void setDispatching(boolean dispatching) {
        this.dispatching = dispatching;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "setDispatching", "dispatching -->" + this.dispatching);             
        }
    }

    @Override
    public boolean isDispatching() {
        return dispatching;
    }

    private CompleteRunnable completeRunnable;
    private DispatchRunnable dispatchRunnable;

    private boolean dispatchAllowed = true;
    protected List<AsyncListenerEntry> asyncListenerEntryList;

    private long _asyncTimeout = WCCustomProperties.DEFAULT_ASYNC_SERVLET_TIMEOUT;

    //Use lazy initialization holder class idiom to prevent initialization of
    //scheduled thread pool executor when Async Servlets are not used.
    public static class ExecutorFieldHolder {
        static {
            executorRetrieved.set(true);
        }
        public static final ScheduledThreadPoolExecutor field =  new ScheduledThreadPoolExecutor (WCCustomProperties.NUMBER_ASYNC_TIMER_THREADS);

        public static final AtomicLong fieldTimeout = new AtomicLong(System.currentTimeMillis());
    }

    public static AtomicBoolean executorRetrieved = new AtomicBoolean(false);

    private ScheduledFuture<?> timeoutScheduledFuture;
    private boolean dispatchPending;
    private Collection<WrapperRunnable> startRunnables;
    private AsyncServletReentrantLock errorHandlingLock = new AsyncServletReentrantLock();
    private boolean invokeErrorHandling=false;
    private boolean complete;
    private long startTime;

    //PI43752 start
    protected String dispatchURI = null;
    //PI43752 end

    public long getStartTime() {
        return startTime;
    }

    protected static final TraceNLS nls = TraceNLS.getTraceNLS(AsyncContextImpl.class, "com.ibm.ws.webcontainer.resources.Messages");

    protected ServiceWrapper serviceWrapper; // PM90834
    
    // Context information stored post ITransferContextService.storeState(). It may contain implementer specific context to be used 
    // by implementations of ITransferContextServiceExt.
    protected HashMap<String, Object> storeStateCtxData;

    /**
     * @param servletRequest
     * @param servletContext
     */
    public AsyncContextImpl(IExtendedRequest iExtendedRequest, IExtendedResponse iExtendedResponse, IWebAppDispatcherContext webAppDispatcherContext) {        
        this.servletRequest = this.iExtendedRequest = iExtendedRequest;
        this.servletResponse = this.iExtendedResponse = iExtendedResponse;
        this.originalRequestURI = webAppDispatcherContext.getOriginalRelativeURI();
        this.webApp = webAppDispatcherContext.getWebApp();

        // Start:PM90834
        if (this.transferContext()) {
            this.captureContext();
        }
        // End:PM90834

        this.startTime = System.currentTimeMillis();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "<init>", "[this servletRequest servletResponse originalRequestURI webApp] [" + this + " "
                            + servletRequest + " " + servletResponse + " " + originalRequestURI + " " + webApp + "]");
        }

    }

    public synchronized boolean lockHeldByDifferentThread(){
        ReentrantLock lock = getErrorHandlingLock();
        boolean isLocked = lock.isLocked();
        boolean heldByCurrentThread = lock.isHeldByCurrentThread();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "lockHeldByDifferentThread", "isLocked [{0}] heldByCurrentThread [{1}]",
                        new Object [] {isLocked,heldByCurrentThread});
        }
        return (isLocked&&!heldByCurrentThread);
    }

    //because the real complete stuff happens on another thread
    //we're okay to sync on the scheduling of the complete
    @Override
    public synchronized void complete() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "complete",this);
        }

        if (!lockHeldByDifferentThread())
        {
            //We can't call this or WebContainer won't know to run the complete runnable which kicks off the async listeners
            //        WebContainerRequestState.getInstance(true).setAsyncMode(false);

            if (!completePending){
                //Move this inside of if block because it complete is called
                //after a previous complete, you can get a NPE because the
                //request may have already been cleaned up. If complete is not
                //pending, then it shouldn't have been cleaned up.
                
                if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() > 31) {
                    //setAsyncStarted(false) upon exit service()
                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
                    reqState.setAttribute("webcontainer.resetAsyncStartedOnExit", "true"); 
                }
                else {
                    iExtendedRequest.setAsyncStarted(false);
                }

                createNewAsyncServletReeentrantLock();

                cancelAsyncTimer();

                completeRunnable = new CompleteRunnable(iExtendedRequest,this);
                completePending = true;
                if (!dispatching){
                    executeNextRunnable();
                }

            }
        } else {
            if (WCCustomProperties.THROW_EXCEPTION_WHEN_UNABLE_TO_COMPLETE_OR_DISPATCH) {
                throw new IllegalStateException(nls.getString("AsyncContext.lock.already.held", "Unable to obtain the lock.  Error processing has already been invoked by another thread."));
            }
        }

        dispatchURI = null;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "complete",this);
        }

    }

    //because the real dispatch stuff happens on another thread
    //we're okay to sync on the scheduling of the dispatch
    @Override
    public synchronized void dispatch(ServletContext context, String path) throws IllegalStateException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "dispatch(ctx,path)",new Object [] {this,context,path});
        }

        if (!lockHeldByDifferentThread()){
            if (completePending) {
                throw new IllegalStateException(nls.getString("called.dispatch.after.complete"));
            } else if (!dispatchAllowed){
                throw new IllegalStateException(nls.getString("trying.to.call.dispatch.twice.for.the.same.async.operation"));
            }

            // PI28910, the effect of calling dispatch should not take effect until the calling
            // thread completes so it is too early to set asyncStarted to false
            // iExtendedRequest.setAsyncStarted(false);

            createNewAsyncServletReeentrantLock();

            //cancel timer inside lock so we don't kick off timeout events after we've decided to dispatch
            cancelAsyncTimer();

            WebAppRequestDispatcher requestDispatcher = (WebAppRequestDispatcher) context.getRequestDispatcher(path);
            dispatchRunnable = new DispatchRunnable(requestDispatcher, this);
            dispatchPending = true;
            dispatchAllowed = false;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST, CLASS_NAME, "dispatch(ctx,path)", "dispatching -->" + dispatching);
            }
            if (!dispatching) {
                executeNextRunnable();
            }
        } else {
            if (WCCustomProperties.THROW_EXCEPTION_WHEN_UNABLE_TO_COMPLETE_OR_DISPATCH) {
                throw new IllegalStateException(nls.getString("AsyncContext.lock.already.held", "Unable to obtain the lock.  Error processing has already been invoked by another thread."));
            }
        }

        dispatchURI = null;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "dispatch(ctx,path)",this);
        }
    }

    public synchronized boolean cancelAsyncTimer() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "cancelAsyncTimer", this);
        }
        boolean timerCancelled = true;
        if (timeoutScheduledFuture!=null) {
            timeoutScheduledFuture.cancel(false);

            //actively purge instead of waiting for the worker thread which may delay the purging if system is busy and eventually can cause oom.
            if (ExecutorFieldHolder.field.getQueue().size() > WCCustomProperties.ASYNC_MAX_SIZE_TASK_POOL &&
                            ((ExecutorFieldHolder.fieldTimeout.get() + WCCustomProperties.ASYNC_PURGE_INTERVAL) < System.currentTimeMillis())){

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "cancelAsyncTimer", "purging the tasks queue, size ->["+ExecutorFieldHolder.field.getQueue().size()+"]");
                }

                ExecutorFieldHolder.fieldTimeout.set(System.currentTimeMillis());
                ExecutorFieldHolder.field.purge();

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "cancelAsyncTimer", "purged the tasks queue, size ->["+ExecutorFieldHolder.field.getQueue().size()+"]");
                }
            }

            timeoutScheduledFuture = null;
        } 
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "cancelAsyncTimer", timerCancelled);
        }
        return timerCancelled;
    }

    private synchronized void startAsyncTimer() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "startAsyncTimer", this);
        }
        if (getTimeout() > 0) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST, CLASS_NAME, "startAsyncTimer", "about to start async timer, timeout->"
                                + getTimeout());
            }

            scheduleTimeout();

        } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "startAsyncTimer", "not starting async timer");
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "startAsyncTimer", this);
        }
    }

    protected void scheduleTimeout() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "scheduleTimeout", this);
        }
        if (timeoutScheduledFuture!=null)
            throw new AsyncIllegalStateException(nls.getString("trying.to.schedule.timeout.without.cancelling.previous.timeout"));
        AsyncTimeoutRunnable asyncTimeoutRunnable = new AsyncTimeoutRunnable (this);
        timeoutScheduledFuture = ExecutorFieldHolder.field.schedule(asyncTimeoutRunnable, getTimeout(),TimeUnit.MILLISECONDS);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "scheduleTimeout", this);
        }
    }

    public void executeNextRunnable() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "executeNextRunnable", this);
        }
        //TODO:performance optimization when we're on a webcontainer thread already?
        boolean invokeAsyncErrorHandlingOutsideLock = false;
        synchronized(this){
            /*Have to setDispatching inside the synchronized block.
			If you recieved a workRejected exception, you don't want to be caught between
			setting the dispatching to false in Thread A, starting the async error hanlding in
			thread B, and then calling executeNextRunnable in Thread A
             */

            if (this.dispatchRunnable!=null){
                setDispatching(true);
                dispatchPending = false;
                DispatchRunnable localDispatchRunnable = dispatchRunnable;
                dispatchRunnable = null;

                // Start:PM90834
                if (!this.transferContext()) {	                
                    localDispatchRunnable.pushContextData();
                }
                // End:PM90834

                notifyITransferContextPreProcessWorkState();
                
                startWithExecutorThread(localDispatchRunnable);

            } else if (this.completeRunnable!=null && runComplete()){
                setDispatching(true);
                CompleteRunnable localCompleteRunnable = completeRunnable;
                completeRunnable = null;
                
                if (this.transferContext()) {
                    notifyITransferContextPreProcessWorkState();
                }
                
                startWithExecutorThread(localCompleteRunnable);
            } else if (this.isInvokeErrorHandling()){ 
                // This will only be true if we are in a webcontainer thread.
                // It is only set to true when workRejected is called and we are still dispatching.
                // A call from a different thread to complete or dispatch is the only way a different
                // thread can invoke this method. However, they will always see dispatching=true
                // as well and not invoke executeNextRunnable when isInvokeErrorHandling returns true.
                // The webcontainer handleRequest or DispatchRunnable exit will always have the first
                // shot at getting inside this if block

                //workRejected was called while we were still dispatching so we should call
                //the error handling mechanism now.

                //don't need this any more because we do executeNextRunnable at end of error handlers
//	        	//set dispatching to false so a call to complete or dispatch from the async listeners
//	        	//will complete
//	        	//otherwise, a call from the following example would never complete
////	        	at com.ibm.ws.webcontainer.async.ListenerHelper._invokeAsyncErrorHandling(ListenerHelper.java:184)
////	        	at com.ibm.ws.webcontainer.async.ListenerHelper.invokeAsyncErrorHandling(ListenerHelper.java:119)
////	        	at com.ibm.ws.webcontainer.async.ListenerHelper.invokeAsyncErrorHandling(ListenerHelper.java:42)
////	        	at com.ibm.ws.webcontainer.async.AsyncContextImpl.executeNextRunnable(AsyncContextImpl.java:341)
////	        	at com.ibm.ws.webcontainer.WebContainer.handleRequest(WebContainer.java:1073)
//	        	setDispatching(false);

                //set the local var
                invokeAsyncErrorHandlingOutsideLock = true;

                //reset the field so we don't invoke again
                setInvokeErrorHandling(false);
            } 
            else {
                if (!isComplete()){ //if we're already complete, no need to start the timer and what not
                    setDispatching(false);
                    /*
                     * If both are null, then we're exiting out WebContainer.handleRequest
                     * without having called dispatch or complete and now we're
                     * no longer dispatching. Calls to complete or dispatch after
                     * this point should call executeNextRunnable within complete or dispatch
                     * since there won't be a dispatch already occurring
                     */


                    //only start the async timer if we don't have a complete or dispatch ready to go
                    //and we are not invoking error handling
                    startAsyncTimer();
                }

            }

        }

        //invoke outside lock so we don't get a deadlock if the listener requires a lock
        //on something else
        //
        //See comments above about when this is possible.
        if (invokeAsyncErrorHandlingOutsideLock){
            WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
            //Set ExecuteNextRunnable to true so that if we get a dispatch or complete pending before
            // we start the error handling, we recurse back into this method and start them
            //
            //Don't check dispatching because we know its true and want to invoke error handling anyway because we are in control
            //of when it is invoked.
            ListenerHelper.invokeAsyncErrorHandling(this, reqState, null, AsyncListenerEnum.ERROR,ExecuteNextRunnable.TRUE,CheckDispatching.FALSE);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "executeNextRunnable",this);
        }
    }

    @Override
    public void dispatch() throws IllegalStateException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "dispatch",this);
        }

        //PI43752 start
        if(WCCustomProperties.SET_ASYNC_DISPATCH_REQUEST_URI && dispatchURI != null){
            // Remove context root from dispatchURI since dispatch() method calls dispatch(webApp, dispatchURI) 
            // and it expects the dispatchURI to be relative to the webApp context path but dispatch() uses the 
            // value of ServletRequest.getRequestURI() which will include the context root. This will cause an 
            // error where we end up having a duplicate context root in the dispatch URI and subsequently cause 
            // a 500 error when the application's context root is different than /. If this code ever gets rewritten 
            // this might need to be revisited.
            if(((WebApp) webApp).getConfiguration().isEncodeDispatchedRequestURI()){
                try {
                    dispatchURI = new java.net.URI(dispatchURI).getPath();
                } catch (URISyntaxException e) {
                    // This should never happen since we already know this is a valid URI (came from the request)
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                        logger.logp(Level.FINEST, CLASS_NAME, "dispatch()", "URISyntaxException while decoding URI, dispatchURI = "+dispatchURI);
                    }
                }
            }
            dispatchURI = dispatchURI.substring(webApp.getContextPath().length());

            dispatch(webApp, dispatchURI);
        }
        else{
            dispatch(webApp, this.originalRequestURI);
        }
        //PI43752 stop

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "dispatch()", "AsyncContextImpl->" + this);
            logger.exiting(CLASS_NAME, "dispatch",this);
        }
    }

    @Override
    public void dispatch(String path) throws IllegalStateException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "dispatch(path)",new Object [] {this,path});
        }

        dispatch(webApp, path);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "dispatch(path))",this);
        }
    }



    public void startWithExecutorThread(Runnable run) {
        startWithExecutorThread(run,false);
    }

    public void startWithExecutorThread(Runnable run,boolean startWrappedRunnable) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "startWithExecutorThread",new Object [] {this,run,startWrappedRunnable});
        }
        try {
            // Start:PM90834
            if (this.transferContext()) {
                run = new ContextWrapper(run, ServiceWrapper.newFromPushedData(this));
            }
            // End:PM90834

            if (startWrappedRunnable){

                WrapperRunnableImpl wrapperRunnable = new WrapperRunnableImpl(run,this);


                // Start:PM90834
                if (!this.transferContext()) {
                    wrapperRunnable.pushContextData();
                }
                // End:PM90834

                addStartRunnable(wrapperRunnable);

                notifyITransferContextPreProcessWorkState();
                
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
                    logger.logp(Level.FINEST, CLASS_NAME, "startWithExecutorThread(Runnable...)", "start new thread with wrapperRunnable");
                }

                com.ibm.ws.webcontainer.osgi.WebContainer.getExecutorService().execute(wrapperRunnable);

            } else { //don't wrap for complete and dispatch

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
                    logger.logp(Level.FINEST, CLASS_NAME, "startWithExecutorThread(Runnable...)", "start new thread with Runnable");
                }

                com.ibm.ws.webcontainer.osgi.WebContainer.getExecutorService().execute(run);

            }
        } catch (Exception e) {        
            // shouldn't be here unless OS is having major threading issues.
            logger.logp(Level.SEVERE, CLASS_NAME, "startWithExecutorThread(Runnable...)", "thread.interrupted.scheduling.async.runnable.on.thread.pool",e);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "startWithExecutorThread(Runnable...)",this);
        }
    }

    /**
     * Informs registered ITransferContextServiceExt implementations that contextual work has been initiated.
     * This notification allows implementers to perform needed actions prior to the work being queued for execution.
     * It is called by complete() when the transferContext property is set to true, dispatch(), and start().
     */
    protected void notifyITransferContextPreProcessWorkState() {

      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
          logger.entering(CLASS_NAME,"notifyITransferContextPostStoreState", this);
      }

      Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
      if (TransferIterator != null) { 
        while(TransferIterator.hasNext()){
          ITransferContextService tcs = TransferIterator.next();
          if (tcs instanceof ITransferContextServiceExt) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
              logger.logp(Level.FINEST, CLASS_NAME, "notifyITransferContextPostStoreState", "calling postStoreState on: " + tcs);
            }
            ((ITransferContextServiceExt)tcs).preProcessWorkState(storeStateCtxData);
          }
        }
      }
      
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
          logger.exiting(CLASS_NAME,"notifyITransferContextPostStoreState", this);
      }
    }
    
    /**
     * Informs registered ITransferContextServiceExt implementations that the request has completed.
     */
    protected void notifyITransferContextCompleteState() {

      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
          logger.entering(CLASS_NAME,"notifyITransferContextCompleteState", this);
      }

      Iterator<ITransferContextService> TransferIterator = com.ibm.ws.webcontainer.osgi.WebContainer.getITransferContextServices();
      if (TransferIterator != null) { 
        while(TransferIterator.hasNext()){
          ITransferContextService tcs = TransferIterator.next();
          if (tcs instanceof ITransferContextServiceExt) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
              logger.logp(Level.FINEST, CLASS_NAME, "notifyITransferContextCompleteState", "calling completeState on: " + tcs);
            }
            ((ITransferContextServiceExt)tcs).completeState(storeStateCtxData);
          }
        }
      }

      storeStateCtxData = null;
      
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)) { 
          logger.exiting(CLASS_NAME,"notifyITransferContextCompleteState", this);
      }
    }

    @Override
    public synchronized void addStartRunnable(WrapperRunnable wrapperRunnable) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "addStartRunnable",new Object [] {this,wrapperRunnable});
        }
        if (startRunnables==null)
            startRunnables = new ArrayList<WrapperRunnable>();
        startRunnables.add(wrapperRunnable);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "addStartRunnable",this);
        }
    }

    @Override
    public synchronized void removeStartRunnable(WrapperRunnable wrapperRunnable) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "removeStartRunnable",new Object [] {this,wrapperRunnable});
        }
        if (startRunnables!=null)
            startRunnables.remove(wrapperRunnable);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "removeStartRunnable",this);
        }
    }


    @Override
    public synchronized void start(Runnable run) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "start",new Object [] {this,run});
        }
        if (!lockHeldByDifferentThread()){
            startWithExecutorThread(run,true);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "start",this);
        }
    }

    public void setRequestAndResponse(ServletRequest servletRequest, ServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }



    public boolean isCompletePending() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "isCompletePending",this);
            logger.exiting(CLASS_NAME, "isCompletePending",completePending);
        }
        return completePending;
    }

    public IExtendedRequest getIExtendedRequest() {
        return iExtendedRequest;
    }

    public IExtendedResponse getIExtendedResponse() {
        return iExtendedResponse;
    }

    @Override
    public ServletRequest getRequest() {
        return this.servletRequest;
    }

    @Override
    public ServletResponse getResponse() {
        return this.servletResponse;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        boolean hasOriginal = (this.servletRequest == this.iExtendedRequest) && (this.servletResponse == this.iExtendedResponse);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "hasOriginalRequestAndResponse",this);
            logger.exiting(CLASS_NAME, "hasOriginalRequestAndResponse",hasOriginal);
        }
        return (hasOriginal);
    }

    @Override
    public void invalidate() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "invalidate",this);
        }
        this.servletRequest = null;
        this.iExtendedRequest = null;
        this.servletResponse = null;
        this.iExtendedResponse = null;
        this.dispatchRunnable = null;
        this.completeRunnable = null;
        this.serviceWrapper = null; // PM90834
        this.webApp = null;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "invalidate",this);
        }
    }

    public boolean isDispatchAllowed() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "isDispatchAllowed",this);
            logger.exiting(CLASS_NAME, "isDispatchAllowed",dispatchAllowed);
        }
        return dispatchAllowed;
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> listenerClass)
                    throws ServletException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "createListener",new Object [] {this,listenerClass});
        }
        T listener = webApp.createListener(listenerClass);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "createListener",listener);
        }
        return listener;
    }

    @Override
    public void addListener(AsyncListener listener,
                            ServletRequest servletRequest, ServletResponse servletResponse)  throws IllegalStateException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "addListener",new Object [] {this,listener,servletRequest,servletResponse});
        }
        if (asyncListenerEntryList==null){
            asyncListenerEntryList = new ArrayList<AsyncListenerEntry>();
            registerPreEventAsyncListeners();
        }    

        AsyncListenerEntry asyncListenerEntry = new AsyncListenerEntry(this,listener,servletRequest,servletResponse);
        asyncListenerEntryList.add(asyncListenerEntry);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "addListener",this);
        }
    }

    @Override
    public long getTimeout() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "getTimeout",this);
            logger.exiting(CLASS_NAME, "getTimeout",_asyncTimeout);
        }
        return this._asyncTimeout;
    }

    @Override
    public void addListener(AsyncListener listener) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "addListener",new Object [] {this,listener});
        }
        if (asyncListenerEntryList==null) {
            asyncListenerEntryList = new ArrayList<AsyncListenerEntry>();
            registerPreEventAsyncListeners();
        }    

        AsyncListenerEntry asyncListenerEntry = new AsyncListenerEntry(this,listener);
        asyncListenerEntryList.add(asyncListenerEntry);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "addListener",this);
        }
    }

    @Override
    public List<AsyncListenerEntry> getAsyncListenerEntryList() {
        return asyncListenerEntryList;
    }

    @Override
    public void setTimeout(long timeout) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "setTimeout",new Object [] {this,timeout});
        }
        //throws IllegalStateException - if this method is called after the container-initiated dispatch, during which one of the ServletRequest#startAsync methods was called, has returned to the container
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
        if (this.dispatching&&reqState!=null&&reqState.isAsyncMode()) {
            this._asyncTimeout = timeout;
        } else {
            throw new IllegalStateException("called setTimeout after the container-initiated dispatch which called startAsync has returned");
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "setTimeout",this);
        }
    }

    public boolean isDispatchPending() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "isDispatchPending",this);
            logger.exiting(CLASS_NAME, "isDispatchPending",dispatchPending);
        }
        return dispatchPending;
    }

    @Override
    public void initialize() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "initialize",this);
        }
        if (asyncListenerEntryList!=null){
            List<AsyncListenerEntry> tempList = this.asyncListenerEntryList;
            this.asyncListenerEntryList = new ArrayList<AsyncListenerEntry>();

            for (AsyncListenerEntry asyncListenerEntry:tempList){
                asyncListenerEntry.invokeOnStartAsync();
            }

        }

        this.dispatchAllowed = true;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "initialize",this);
        }
    }

    // Start:PM90834
    protected void captureContext () {
        this.serviceWrapper = new ServiceWrapper(this);
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))           
            logger.logp(Level.FINEST, CLASS_NAME, "captureContext","created ServiceWrapper [" + this.serviceWrapper +"] for context " + this );
        
        this.serviceWrapper.pushContextData();
    }
    // End:PM90834

    @Override
    public synchronized Collection<WrapperRunnable> getAndClearStartRunnables() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.entering(CLASS_NAME, "getAndClearStartRunnables",this);
        }
        //set the pointer to null so that the remove cannot remove an Runnable
        //in the middle of traversing over the list.
        Collection<WrapperRunnable> tempStartRunnables = startRunnables;
        this.startRunnables=null;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {            
            logger.exiting(CLASS_NAME, "getAndClearStartRunnables",this);
        }
        return tempStartRunnables;
    }

    public synchronized AsyncServletReentrantLock getErrorHandlingLock(){
        return errorHandlingLock;
    }

    @Override
    public void setInvokeErrorHandling(boolean invokeErrorHandling) {
        this.invokeErrorHandling = invokeErrorHandling;

    }

    public boolean isInvokeErrorHandling() {
        return invokeErrorHandling;
    }

    public synchronized void createNewAsyncServletReeentrantLock() {
        this.errorHandlingLock.getAndSetIsValid(false);
        this.errorHandlingLock = new AsyncServletReentrantLock();
    }

    public void setComplete(boolean b) {
        this.complete = b;
    }

    public boolean isComplete() {
        return this.complete ;
    }

    /*
     * This method is overridden in servlet 3.1.
     */     
    public boolean registerPreEventAsyncListeners() {
        // Empty method to allow implementation for servlet 3.1
        return false;
    }

    /*
     * This method is overridden in servlet 3.1.
     */	
    public boolean registerPostEventAsyncListeners() {
        // Empty method to allow implementation for servlet 3.1
        return false;
    }

    /*
     * This method is overridden in servlet 3.1.
     */
    public boolean transferContext() {
        return WCCustomProperties.TRANSFER_CONTEXT_IN_ASYNC_SERVLET_REQUEST;
    }

    //PI43752 start
    public void setDispatchURI(String uri){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))           
            logger.logp(Level.FINEST, CLASS_NAME, "setDispatchURI","uri -> "+uri);
        this.dispatchURI = uri;
    }
    //PI43752 end

    /**
     * @return
     */
    public boolean runComplete() {
        // TODO Auto-generated method stub
        return true;
    }

}
