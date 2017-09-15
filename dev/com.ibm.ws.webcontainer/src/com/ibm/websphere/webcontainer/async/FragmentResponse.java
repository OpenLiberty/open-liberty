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
package com.ibm.websphere.webcontainer.async;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * FragmentResponse is a placeholder for the results of an async include so they can be inserted later in the response
 * 
 * @ibm-api
 *
 */
public interface FragmentResponse {
	/**
     * insertFragment marks the location a fragment's response contents are supposed to be inserted
     * without blocking
     * 
     * @param req ServletRequest
     * @param resp ServletResponse
     * @throws ServletException
     * @throws IOException
     */
    public void insertFragment(ServletRequest req, ServletResponse resp) throws ServletException, IOException;
    
    /**
     * insertFragmentBlocking waits for the include to complete and inserts the response content.
     * 
     * @param req ServletRequest
     * @param resp ServletResponse
     * @throws ServletException
     * @throws IOException
     */
    public void insertFragmentBlocking(ServletRequest req, ServletResponse resp) throws ServletException, IOException;
}
