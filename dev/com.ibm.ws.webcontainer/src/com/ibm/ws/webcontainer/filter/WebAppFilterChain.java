/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
import com.ibm.ws.webcontainer.osgi.interceptor.RegisterRequestInterceptor;
import com.ibm.ws.webcontainer.servlet.WsocHandler;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
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
            logger.logp(Level.FINE, CLASS_NAME,"doFilter", "entry");

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
                	 	logger.logp(Level.FINE, CLASS_NAME,"doFilter", "executing filter -->" + wrapper.getFilterName());
                	 }
                     wrapper.doFilter(request, response, this);
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
                
                logger.logp(Level.FINE, CLASS_NAME,"doFilter", "exit");
            throw e;
        }
        catch (RuntimeException re) {
        	throw re;	
        }
        catch (Throwable th) {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.filter.WebAppFilterChain.doFilter", "89", this);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"doFilter", "exit");
            	throw new ServletErrorReport(th);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"doFilter", "exit");
    }

    private void invokeTarget(ServletRequest request, ServletResponse response) throws Exception {
        if (requestProcessor != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) ServletUtil.unwrapRequest(request, HttpServletRequest.class);
            HttpServletResponse httpResponse = (HttpServletResponse) ServletUtil.unwrapResponse(response, HttpServletResponse.class);
            if (!RegisterRequestInterceptor.notifyRequestInterceptors("AfterFilters",httpRequest,httpResponse)){
                WsocHandler wsocHandler = ((com.ibm.ws.webcontainer.osgi.webapp.WebApp) webapp).getWebSocketHandler();
                if (wsocHandler != null) {
                    //Should WebSocket handle this request?
                    if (wsocHandler.isWsocRequest(request)) {
                        wsocHandler.handleRequest(httpRequest, httpResponse);
                    } else {
                        requestProcessor.handleRequest(request, response);
                    }
                }
                else {
                    requestProcessor.handleRequest(request, response);
                }
            }
        }
        else {
            webapp.finishedFiltersWithNullTarget(request, response, requestProcessor);
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
