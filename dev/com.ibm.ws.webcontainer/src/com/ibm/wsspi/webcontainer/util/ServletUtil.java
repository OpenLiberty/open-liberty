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
package com.ibm.wsspi.webcontainer.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.request.HttpServletRequestProxy;
import com.ibm.websphere.servlet.response.HttpServletResponseProxy;
import com.ibm.websphere.servlet.response.StoredResponse;
import com.ibm.ws.webcontainer.core.Response;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletRequestWrapper;
import com.ibm.wsspi.webcontainer.servlet.IServletResponseWrapper;

/**
 *
 * 
 * ServletUtil provides methods for unwrapping requests and response
 * to get to the core websphere request and response
 * 
 * @ibm-private-in-use
 * 
 * @since   WAS7.0
 * 
 */

public class ServletUtil {
	protected static Logger logger = Logger.getLogger("com.ibm.wsspi.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.util.ServletUtil";

	protected static TraceNLS nls = TraceNLS.getTraceNLS(ServletUtil.class, "com.ibm.ws.webcontainer.resources.Messages");
	/**
	 * 
	 * @param req
	 * @return
	 */
	public static com.ibm.wsspi.webcontainer.servlet.IExtendedRequest unwrapRequest(ServletRequest req)
	{
		return unwrapRequest(req, com.ibm.wsspi.webcontainer.servlet.IExtendedRequest.class);
	}

	public static <T extends ServletRequest, V extends ServletRequest> T unwrapRequest(V req, Class<T> requestClass)
	{ 
		return unwrapRequest(req,requestClass,false);
	}
	
	public static <T extends ServletRequest, V extends ServletRequest> T unwrapRequest(V req, Class<T> requestClass, boolean logWarning)
	{
		ServletRequest r = req;
		while (!(requestClass.isInstance(r)))
		{
			if (logWarning && logger.isLoggable(Level.WARNING)) {
		            logger.logp(Level.WARNING, CLASS_NAME, "unwrapRequest",MessageFormat.format(nls.getString(
                    "servletrequestwrapper.is.not.an.instance.of.so.wrapped.logic.may.be.bypassed",
                    "ServletRequestWrapper [{0}] is not an instance of [{1}] so wrapped logic may be bypassed."),new Object[] {r,requestClass}));
		    }
			
			if (r instanceof ServletRequestWrapper)
			{
			    r = ((ServletRequestWrapper) r).getRequest();
			}
			else if (r instanceof HttpServletRequestProxy)
			{
			    r = ((HttpServletRequestProxy)r).getRequest();
			} 
			else if (r instanceof IServletRequestWrapper)
			{
				r = ((IServletRequestWrapper)r).getWrappedRequest();
			}
			else
			{
				throw new RuntimeException("SRV.8.2: RequestWrapper objects must extend ServletRequestWrapper or HttpServletRequestWrapper");
			}
		}
			    
		return (T)r;
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	public static ServletResponse unwrapResponse(ServletResponse res)
	{
		return unwrapResponse(res, Response.class);
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	public static ServletResponse unwrapResponse(ServletResponse res, Class className)
	{
		ServletResponse r = res;
		boolean gotCurrentThreadsIExtendedResponse = false;
		while (!(className.isInstance(r)))
		{
			if (r instanceof ServletResponseWrapper)
			{
			    r = ((ServletResponseWrapper) r).getResponse();
			}
			else if (r instanceof HttpServletResponseProxy)
			{
			    r = ((HttpServletResponseProxy)r).getResponse();
			}
			else if (r instanceof IServletResponseWrapper)
			{
				r = ((IServletResponseWrapper)r).getWrappedResponse();
			}
			else{
				if (!gotCurrentThreadsIExtendedResponse){
					WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
					if (reqState!=null)
					{
						r = reqState.getCurrentThreadsIExtendedResponse();
						gotCurrentThreadsIExtendedResponse=true;
						continue;
					}
				}
				
				throw new RuntimeException("SRV.8.2: ResponseWrapper objects must extend either ServletResponseWrapper or HttpServletResponseWrapper");
			}
			
		}
			    
		return r;
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	public static ServletResponse unwrapResponseKeepGoing(ServletResponse res, Class className)
	{
		ServletResponse r = res;
		ServletResponse deepestResponse=null;
		while (r!=null)
		{
			if (className.isInstance(r)){
				deepestResponse=r;
			}
			if (r instanceof ServletResponseWrapper)
			{
			    r = ((ServletResponseWrapper) r).getResponse();
			}
			else if (r instanceof HttpServletResponseProxy)
			{
			    r = ((HttpServletResponseProxy)r).getResponse();
			}
			else if (r instanceof IServletResponseWrapper)
			{
				r = ((IServletResponseWrapper)r).getWrappedResponse();
			}
			else{
				return deepestResponse;
			}
			
			
		}
		return deepestResponse;
		
	}
	
	public static void main (String[]args){
		StoredResponse storedResponse=new StoredResponse(new StoredResponse(null,true),true);
		ServletUtil.unwrapResponseKeepGoing(storedResponse, IExtendedResponse.class);
	}
}
