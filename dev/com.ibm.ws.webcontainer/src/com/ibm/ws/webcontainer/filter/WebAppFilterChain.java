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
package com.ibm.ws.webcontainer.filter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.websphere.servlet.request.extended.IRequestExtended;
import com.ibm.ws.webcontainer.osgi.interceptor.RegisterRequestInterceptor;
import com.ibm.ws.webcontainer.servlet.H2Handler;
import com.ibm.ws.webcontainer.servlet.WsocHandler;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.util.ServletUtil;

/**
 * WAS implementation of the FilterChain object
 *
 */
@SuppressWarnings("unchecked")
public class WebAppFilterChain implements FilterChain {
    private ArrayList _filters = new ArrayList(5);
    private int _currentFilterIndex = -1;
    private int _numberOfFilters = 0;
    private RequestProcessor requestProcessor;
    private boolean _filtersDefined = false;

protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.filter");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.filter.WebAppFilterChain";
    private WebApp webapp = null; // PK02277: Cache the webapp

    public WebAppFilterChain() {
    }
    
    // PK02277: Constructor to initialise the webapp
    public WebAppFilterChain( WebApp webapp ) 
    {
        this.webapp = webapp;
    } 

    
    /**
     * Causes the next filter in the chain to be invoked, or, if at the end
     * of the chain, causes the requested resource to be invoked
     *
     * @return a String containing the filter name
     */
    public void doFilter(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.entering(CLASS_NAME, "doFilter");

        try {
            // if there are no filters, just invoke the requested servlet
            if (!_filtersDefined) {
                    invokeTarget(request, response);
            }
            else {
                // increment the filter index
                _currentFilterIndex++;

                if (_currentFilterIndex < _numberOfFilters) {
                    // more filters to go...invoke the next one
                    FilterInstanceWrapper wrapper = ((FilterInstanceWrapper) _filters.get(_currentFilterIndex));
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"doFilter", "enter filter [" + wrapper.getFilterName() + "] class [" + wrapper.getFilterInstance() + "] request [" + request + "] response [" + response + "]");
                    }
                    wrapper.doFilter(request, response, this);
                    
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){  
                        logger.logp(Level.FINE, CLASS_NAME,"doFilter", "exit filter [" + wrapper.getFilterName() + "]"); 
                    }
                }
                else {
                    invokeTarget(request, response);
                }
            }
        }
        catch (UnavailableException e) {
            throw e;
        }
        catch (IOException ioe) {
            throw ioe;
        }
        catch (ServletException e) {
            Throwable t = e.getCause();
            if (t!=null && t instanceof FileNotFoundException) {
                //don't log a FFDC
                logger.logp(Level.FINE, CLASS_NAME, "doFilter", "FileNotFound");
            } 
            else{
                //start 140014
                if(webapp.getDestroyed() != true)
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.filter.WebAppFilterChain.doFilter", "82", this);
                else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"doFilter", "Can not invoke filter because application is destroyed", e);
                //end 140014
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.exiting(CLASS_NAME, "doFilter");
            throw e;
        }
        catch (RuntimeException re) {
        	throw re;	
        }
        catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.filter.WebAppFilterChain.doFilter", "89", this);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.exiting(CLASS_NAME, "doFilter");

            	throw new ServletErrorReport(th);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.exiting(CLASS_NAME, "doFilter");
    }

    private void invokeTarget(ServletRequest request, ServletResponse response) throws Exception {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.entering(CLASS_NAME, "invokeTarget", "request [" + request + "] response [" + response + "]");
        }
        
        try {
            if (requestProcessor != null) {
                HttpServletRequest httpRequest = (HttpServletRequest) ServletUtil.unwrapRequest(request, HttpServletRequest.class);
                HttpServletResponse httpResponse = (HttpServletResponse) ServletUtil.unwrapResponse(response, HttpServletResponse.class);
                if (!RegisterRequestInterceptor.notifyRequestInterceptors("AfterFilters", httpRequest, httpResponse)){
                    boolean handled = false;
                    WsocHandler wsocHandler = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp) webapp).getWebSocketHandler();
                    H2Handler h2Handler = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp) webapp).getH2Handler();

                    // Should WebSocket handle this request?
                    if (wsocHandler != null) {
                        if (wsocHandler.isWsocRequest(request)) {
                            wsocHandler.handleRequest(httpRequest, httpResponse);
                            handled = true;
                        }
                    }

                    // Should this be handled as an h2c upgrade request?
                    if (!handled) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "invokeTarget", " looking at H2 upgrade");
                        }
                        HttpInboundConnection httpInboundConnection = null;
                        if (request instanceof IExtendedRequest) {
                            IExtendedRequest srtReq = (IExtendedRequest)request;
                            IRequestExtended iReq = (IRequestExtended)srtReq.getIRequest();
                            if (iReq != null) {
                                httpInboundConnection = iReq.getHttpInboundConnection();
                                logger.logp(Level.FINE, CLASS_NAME, "invokeTarget", "HttpInboundConnection: " + httpInboundConnection);
                            }
                        }

                        if (h2Handler != null && httpInboundConnection != null && request instanceof HttpServletRequest) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, "invokeTarget", "looking at isH2Request");
                            }
                            if (h2Handler.isH2Request(httpInboundConnection, request)) {
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                    logger.logp(Level.FINE, CLASS_NAME, "invokeTarget", "upgrading to H2");
                                }
                                h2Handler.handleRequest(httpInboundConnection, httpRequest, httpResponse);
                            }
                        }
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME, "invokeTarget", "calling requestProcessor.handleRequest");
                        }
                        requestProcessor.handleRequest(request, response);
                    }
                }
            }
            else {
                webapp.finishedFiltersWithNullTarget(request, response, requestProcessor);
            }
        }
        finally {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){ 
                logger.exiting(CLASS_NAME, "invokeTarget");
            }
        }
    }

    /** WebAppFilterChain public interface
    
    /**
     * Adds the passed in filter wrapper to the filters array
     *
     * @param the filter wrapper containing the filter to be added
     */
    public void addFilter(FilterInstanceWrapper fiw) {
        _filtersDefined = true;
        _numberOfFilters++;
        _filters.add(fiw);
        //if addFilter is called, filters will be invoked so go on and set invokedFilters to true on the request state
        //WebContainerRequestState.getInstance(true).setInvokedFilters(true);
    }

    public void setRequestProcessor(RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
}
