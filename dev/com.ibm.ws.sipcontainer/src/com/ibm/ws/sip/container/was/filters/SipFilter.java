/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.was.DummySipServletRequestResponse;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.container.was.message.SipMessage;

/**
 * @author Nitzan, Aug 10, 2005
 *
 * We use this filter to Extract deliver the Sip Servlet service method 
 * SipServletRequest and SipServletResponse objects
 */
public class SipFilter implements Filter {
    
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipFilter.class);
    
    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig fc) throws ServletException {
        if(c_logger.isTraceEntryExitEnabled()){
            c_logger.traceEntryExit( this, "init", fc);
        }
    }

    /* This method is called right before the servlet service method is called. 
     * more filters can be added to the chain of filters, but this one is guaranteed
     * to be the first.
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest req, ServletResponse res,
            			FilterChain fc) throws IOException, ServletException {
        if(c_logger.isTraceEntryExitEnabled()){
            c_logger.traceEntry( this, "doFilter", new Object[]{req, res});
        }
        
        if (req instanceof HttpServletRequest)
        {
        	HttpServletRequest httpRequest = (HttpServletRequest) req;
        	HttpServletResponse httpResponse = (HttpServletResponse) res;
        	
        	SipServletRequestImpl sipRequest = null;
        	SipServletResponseImpl sipResponse = null;
            // Read back the servlet request\response which we stored locally for 
            // this thread before entering the web-container.
            SipMessage msg = ThreadLocalStorage.getSipMessage();
        	
            if(msg == null){
            	if(c_logger.isTraceEntryExitEnabled()){
                    c_logger.traceDebug("doFilter: No SIP message. This will be considered as an HTTP servlet");
                }
            	return;
            }
            sipResponse = (SipServletResponseImpl) msg.getResponse();
            sipRequest = (SipServletRequestImpl) msg.getRequest();
            
            SipServletMessageImpl message = sipResponse != null ? sipResponse : sipRequest;
            
			message.setHttpServletRequest(httpRequest);
			message.setHttpServletResponse(httpResponse);
			
			DummySipServletRequestResponse dummy = new DummySipServletRequestResponse(httpRequest, httpResponse);
			
			
			ServletRequest request = sipRequest != null ? sipRequest : dummy;
			ServletResponse response = sipResponse != null ? sipResponse : dummy; 
			 
			fc.doFilter(request, response);
			
        } else {
		    c_logger.error( "error.non.http.request", null, req);
			fc.doFilter( req, res);
		}
        
        if(c_logger.isTraceEntryExitEnabled()){
            c_logger.traceExit( this, "doFilter", new Object[]{req, res});
        }
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        if(c_logger.isTraceEntryExitEnabled()){
            c_logger.traceEntryExit( this, "destroy", null);
        }
    }

}
