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
package com.ibm.ws.sip.container.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.container.was.WebsphereInvoker;

/**
 * @author Amir Perlman, Jan 2, 2004
 *
 * Wraps the actual implementation of the Request Dispatcher in order to replace
 * Sip Request/Response into Http Request/Response that are familiar to the
 * wrapped implemention,e.g Websphere  . 
 */
public class RequestDispatcherWrapper implements RequestDispatcher
{
    /**
     * The actual implementation of the Request Dispatcher. 
     */
    private RequestDispatcher m_impl;
    
    /**
     * The requested servlet name for dispatch
     */
    private String m_requestedDispatcherServletName;
    
    /**
     * Construct a Request Dispatcher Wrapper for the specified implemention
     * of Request Dispatcher. 
     * @param impl
     * @param servletName
     */
    public RequestDispatcherWrapper(RequestDispatcher impl,String servletName)
    {
        m_impl = impl;
        m_requestedDispatcherServletName = servletName;
    }

    /**
     * @see javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void forward(ServletRequest req, ServletResponse res)
        throws ServletException, IOException
    {
        //If we got here then it must be SipServletMessageImpl object.
        HttpServletRequest httpRequest = null;
        HttpServletResponse httpResponse = null;
         
        String invokerSipplet = ThreadLocalStorage.getSipServletName();
        ThreadLocalStorage.getSipMessage().setServletName(this.m_requestedDispatcherServletName);
        
        /* in a scenario when siplet forward to another siplet, originating from onReponse
         * mean the response object contains all the Http data used by the web container.
         * in a scenario where originating from an onRequest, but the originating siplet also
         * created a response and forward it also (this is optional) the Http data will be kept in the 
         * sip request object
         * look at SPR #CSCL69XSFC 
        */
        if(res!=null){
        	httpRequest = ((SipServletMessageImpl) res).getHttpServletRequest();
        	httpResponse = ((SipServletMessageImpl) res).getHttpServletResponse();
        }
        
        if (httpRequest==null){ 
        	// mean that res==null or res does not contain the original Http req/resp
        	httpRequest = ((SipServletMessageImpl) req).getHttpServletRequest();
        	httpResponse = ((SipServletMessageImpl) req).getHttpServletResponse();
        }
        
    	try {
	        if(httpRequest!=null){
	            //both SIP Request/Response are wrapped by the Http Request, No need
	        	//to pass the response object.
	        	m_impl.forward(httpRequest, httpResponse);
	        }else{
	        	// the SIP Request or Response were not wrapped by the Http Request,
	        	// this scenario can be servlet acting as UAC
	        	// created SipServletMessage and sending it.
	        	// Need to "simulate" servlet invocation. 
	        	// this is using SipServletInvoker only using the same thread
	        	SipServletDesc sipServletDesc = 
	        		SipRouter.getInstance().getSipletByName(m_requestedDispatcherServletName);
	        	WebsphereInvoker.getInstance().invokeSipServlet((SipServletRequest)req, (SipServletResponse)res,sipServletDesc , null);
	        }
    	} finally{
            ThreadLocalStorage.getSipMessage().setServletName(invokerSipplet);
    	}
    }

    /**
     * @see javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void include(ServletRequest req, ServletResponse res)
        throws ServletException, IOException
    {
		HttpServletRequest httpRequest =
			((SipServletMessageImpl) req).getHttpServletRequest();
		HttpServletResponse httpResponse =
            ((SipServletMessageImpl) req).getHttpServletResponse();
		
		m_impl.include(httpRequest, httpResponse);

    }

}
