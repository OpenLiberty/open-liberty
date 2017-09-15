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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * AsyncRequestDispatcher is a special RequestDispatcher used to execute includes asynchronously.
 * 
 * @ibm-api
 *
 */
public interface AsyncRequestDispatcher extends RequestDispatcher {
    /**
     * getFragmentResponse kicks off an asynchronous includes and returns a FragmentRespnse object
     * which is used later to insert the include contents.
     * 
     * @param req ServletRequest
     * @param resp ServletResponse
     * @return FragmentResponse
     * @throws ServletException
     * @throws IOException
     */
    public FragmentResponse getFragmentResponse(ServletRequest req, ServletResponse resp) throws ServletException, IOException ;
    /**
     * Set the AsyncRequestDispatcherConfig object used to customize messages and timeouts for
     * specific includes.
     * 
     * @param config
     */
    public void setAsyncRequestDispatcherConfig (AsyncRequestDispatcherConfig config);
}
