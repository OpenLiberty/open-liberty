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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * JspFragmentResponse is a special type of FragmentResponse intended for use by IBM JSP 
 * custom tags created specifically for asynchronous includes. 
 * 
 *@ibm-private-in-use
 */
public interface JspFragmentResponse extends com.ibm.websphere.webcontainer.async.FragmentResponse{
	/**
	 * Insert the fragment contents into the response using the provided PrintWriter. 
	 * 
	 * @param req ServletRequest
	 * @param resp ServletResponse
	 * @param pw PrintWriter
	 * @throws ServletException
	 * @throws IOException
	 */
	public void insertFragmentFromJsp(ServletRequest req, ServletResponse resp,PrintWriter pw) throws ServletException, IOException;
    /**
     * Insert the blocking fragment into the response using the provided PrintWriter 
     * 
     * @param req ServletRequest
     * @param resp SevrletResponse
     * @param pw PrintWriter
     * @throws ServletException
     * @throws IOException
     */
    public void insertFragmentBlockingFromJsp(ServletRequest req, ServletResponse resp,PrintWriter pw) throws ServletException, IOException;

}

