/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * WebAuthenticator is used for performing authentication for web users.
 * Userid/password, ltpa token, single sign-on token or certificates are the
 * types of authentication data that can be used to perform the task.
 * Authenticators are used by the WebCollaborators when performing security
 * checks.
 */
public interface WebAuthenticator {

    /**
     * Authenticate the web request.
     * 
     * @param webRequest
     * @return If successful, AuthenticationResult.getStatus() must answer SUCCESS,
     *         all other values indicate failure
     */
    AuthenticationResult authenticate(WebRequest webRequest);

    /**
     * Authenticate the web request.
     * 
     * @param request
     * @param response
     * @param props
     * @return If successful, AuthenticationResult.getStatus() must answer SUCCESS,
     *         all other values indicate failure
     * @throws Exception
     */
    AuthenticationResult authenticate(HttpServletRequest request,
                                      HttpServletResponse response,
                                      HashMap<String, Object> props) throws Exception;
}
