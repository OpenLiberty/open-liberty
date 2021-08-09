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


import java.net.URISyntaxException;
import java.util.logging.Level;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer31.async.listener.ReadListenerRunnable;
import com.ibm.ws.webcontainer31.osgi.listener.RegisterEventListenerProvider;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.webapp.WebApp31;
import com.ibm.ws.webcontainer31.srt.SRTInputStream31;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

/**
 *
 */
public class AsyncContext31Impl extends AsyncContextImpl implements AsyncContext  {
    
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(AsyncContext31Impl.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );   
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.async.AsyncContext31Impl";
    
    private boolean readListenerRunning = false;
    
    public AsyncContext31Impl(IExtendedRequest iExtendedRequest, IExtendedResponse iExtendedResponse, IWebAppDispatcherContext webAppDispatcherContext) {
        super(iExtendedRequest,iExtendedResponse,webAppDispatcherContext);
    }
    
    @Override
    public void dispatch() throws IllegalStateException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "dispatch",this);
        }
        
        String dispatchTo = originalRequestURI;

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

            dispatchTo = this.dispatchURI.substring(webApp.getContextPath().length());
        }
        //PI43752 stop

        if (iExtendedRequest.getQueryString()!=null) {
            dispatch(dispatchTo + "?" + iExtendedRequest.getQueryString());
        } else {
            dispatch(dispatchTo);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "dispatch()", "AsyncContextImpl->" + this);
            logger.exiting(CLASS_NAME, "dispatch",this);
        }
    }

 
    @Override
    public ServletRequest getRequest() throws IllegalStateException {
        if (isCompletePending() || isDispatchPending()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "asynccontext.getRequestResponse.illegalstateexception");  
            throw new IllegalStateException(Tr.formatMessage(tc, "asynccontext.getRequestResponse.illegalstateexception"));
        }
        return servletRequest;
    }

    @Override
    public ServletResponse getResponse() throws IllegalStateException {
        if (isCompletePending() || isDispatchPending()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "asynccontext.getRequestResponse.illegalstateexception");  
            throw new IllegalStateException(Tr.formatMessage(tc, "asynccontext.getRequestResponse.illegalstateexception"));
        }
        return servletResponse;
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.ws.webcontainer.async.AsyncContextImpl#createListener(java.lang.Class)
     */
    @Override
    public <T extends AsyncListener> T createListener(Class<T> listenerClass) throws ServletException {
        // This method needs to be overridden because the implementation in the parent class calls webApp.createListener and
        // WebApp31 now has a createListener that only allows for certain listeners to be created per the specification for ServletContext.createListener.
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {                  
            logger.entering(CLASS_NAME, "createListener",new Object [] {this,listenerClass});
        }
        T listener = ((WebApp31)webApp).createAsyncListener(listenerClass);
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.exiting(CLASS_NAME, "createListener",listener);
        }
        
        return listener;
    }
    
    @Override
    public boolean registerPreEventAsyncListeners() {
        boolean result=false;
        if (asyncListenerEntryList!=null) {
            // Give Weld (or others registered) a chance to register the last AsyncListener
            result = RegisterEventListenerProvider.notifyPreEventListenerProviders(webApp, this);
        }    
        return result;
    }
   
    @Override
    public boolean registerPostEventAsyncListeners() {
        boolean result=false;
        if (asyncListenerEntryList!=null) {
            // Give Weld (or others registered) a chance to register the last AsyncListener
            result = RegisterEventListenerProvider.notifyPostEventListenerProviders(webApp, this);
        }    
        return result;
    }
    
    /**
     * With servlet3.1 and up WCCustomProperties.TRANSFER_CONTEXT_IN_ASYNC_SERVLET_REQUEST is no longer valid
     */
    @Override
    public boolean transferContext() {
        return true;
    }

    /**
     * @param b
     */
    public synchronized void setReadListenerRunning(boolean b) {
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {                  
            logger.logp(Level.FINE,CLASS_NAME, "setReadListenerRunning","Current value = " + readListenerRunning + ", newValue = " + b);
        }
        readListenerRunning = b;
        if (!readListenerRunning && this.isCompletePending() && !this.isDispatching()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {                  
                logger.logp(Level.FINE,CLASS_NAME, "setReadListenerRunning","Start complete processing");
            }
           executeNextRunnable();   
        }
    }
    
    public boolean isReadListenerRunning() {
        return readListenerRunning;
    }
    
    /**
     * A read listener has been set on the SRTInputStream and we will set it up to do its
     * first read on another thread.
     */
    public void startReadListener(ThreadContextManager tcm, SRTInputStream31 inputStream) throws Exception{
        try {
            ReadListenerRunnable rlRunnable = new ReadListenerRunnable(tcm, inputStream, this);
            this.setReadListenerRunning(true);
            com.ibm.ws.webcontainer.osgi.WebContainer.getExecutorService().execute(rlRunnable);
        
            // The read listener will now be invoked on another thread.  Call pre-join to
            // notify interested components that this will be happening.  It's possible that
            // the read listener may run before the pre-join is complete, and callers need to
            // be able to cope with that.
            notifyITransferContextPreProcessWorkState();
        } catch (Exception e) {
            this.setReadListenerRunning(false);
            throw e;
        }
    }

    /**
     * A read listener has been previously set on the SRTInputStream and we are out of data
     * to pass to it.  An async read has been scheduled and we must prepare for the read to
     * complete on another thrad.
     */
    public void continueReadListener() {
        // The read listener will be invoked on another thread when more data is available.  
        // Call pre-join to notify interested components that this will be happening.  It's 
        // possible that the read listener may run before the pre-join is complete, and 
        // callers need to be able to cope with that.
        notifyITransferContextPreProcessWorkState();
    }
    
    @Override
    public boolean runComplete() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {                  
            logger.logp(Level.FINE,CLASS_NAME, "runComplete","readListenerRunning = " + readListenerRunning);
        }        
        return !readListenerRunning;
       
    }

}
