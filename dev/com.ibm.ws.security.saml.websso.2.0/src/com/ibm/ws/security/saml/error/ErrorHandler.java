/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.error;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public interface ErrorHandler {

    /**
     * See an implement in ErrorHandlerImpl
     */
    /**
     * This handle the error response to show the messages and error status
     * in the browser of end-user
     * 
     * @param request
     * @param response
     * @param e
     * @throws ServletException
     * @throws IOException
     */
    public void handleException(HttpServletRequest request,
                                HttpServletResponse response,
                                SamlException e) throws ServletException, IOException;
}
