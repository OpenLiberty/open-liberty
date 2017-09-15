/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 04/26/10 F743-25523     leou      Initial version
 * 05/10/10 F743-25523.1   leou      Move Jaspi hooks to WebAuthenticator
 * 05/27/10 654357         leou      CTS6: jaspic failure - testName:  CheckValidateReqAuthException, do not call secureResponse during postInvoke 
 * 08/11/10 665302         leou      Authorization problem with cache key using JASPI authentication
 */
package com.ibm.ws.webcontainer.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UnprotectedResourceService {

    /**
     * Returns true if the unprotected Resource need to be authenticated further
     * by the service owner later
     */
    boolean isAuthenticationRequired(HttpServletRequest request);

    /**
     * Returns true if we take some actions.
     * Otherwise, return false
     * example of userName: "user:sp2_realm_No/user2"
     */
    boolean logout(HttpServletRequest request, HttpServletResponse response, String userName);
}
