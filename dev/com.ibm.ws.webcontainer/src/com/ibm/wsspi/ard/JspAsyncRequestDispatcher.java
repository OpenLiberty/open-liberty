/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.ard;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.websphere.webcontainer.async.AsyncRequestDispatcher;
import com.ibm.websphere.webcontainer.async.FragmentResponse;

/**
 * JspAsyncRequestDispatcher is intended for use by IBM JSP custom tags created specifically 
 * for asynchronous includes. 
 * 
 * @ibm-private-in-use
 *
 */
public interface JspAsyncRequestDispatcher extends AsyncRequestDispatcher{
	
	/**
	 * Return a new FragmentResponse object and execute the asynchronous include 
	 * 
	 * @param req ServletRequest
	 * @param resp ServletResponse
	 * @return new FragmentResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	public FragmentResponse getFragmentResponse(ServletRequest req, ServletResponse resp) throws ServletException, IOException;

    /**
     * Return a new ServletResponseWrapper 
     * 
     * @param req ServletRequest
     * @param resp ServletResponse
     * @return ServletResponseWrapper 
     * @throws ServletException
     * @throws IOException
     */
    public ServletResponse getFragmentResponseWrapper(ServletRequest req, ServletResponse resp) throws ServletException, IOException;
    /**
     * Execute a special type of fragment response in which a wrapped response is provided. 
     * 
     * @param req ServletRequest
     * @param resp ServletRespnse
     * @return new FragmentResponse
     * @throws ServletException
     * @throws IOException
     */
    public FragmentResponse executeFragmentWithWrapper(ServletRequest req, ServletResponse resp) throws ServletException, IOException;

}

