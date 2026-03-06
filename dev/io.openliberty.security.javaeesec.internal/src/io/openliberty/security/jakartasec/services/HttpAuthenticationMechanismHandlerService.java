/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.services;

import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Implementation for Jakarta Security 1.0, 2.0 and 3.0, used by the AuthModule.
 *
 * This Handler Service executes the exact same functionality as prior to
 * Jakarta Security 4.0, but simply abstracts it so that this service can be
 * rewritten for Jakarta Security 4.0, but the integration (with AuthModule)
 * therefore remains unchanged.
 *
 * NOTE: For the Jakarta Security 2.0 and 3.0 implementations, they will
 * use the this *same* bundle but transformed for jakarta instead of javax.
 *
 */

public class HttpAuthenticationMechanismHandlerService {

    private static final TraceComponent tc = Tr.register(HttpAuthenticationMechanismHandlerService.class);
    
    // static flag to ensure the debug message is logged only once
    private static volatile boolean debugMessageLogged = false;

    /**
     * Logs the implementation version message once across all methods which is useful debugging information.
     */
    private static void logImplementationOnce() {
        if (!debugMessageLogged && tc.isDebugEnabled()) {
        	Tr.debug(tc, "Using Jakarta Security 1.0/2.0/3.0 implementation.");
        	debugMessageLogged = true;
        }
    }

    /**
     * Validate an authentication request.  Simply uses the passed authentication mechanism to
     * perform the actual request.
     *
     * @param httpMessageContext is the calculated Http context
     * @param authMech is the authentication mechanism
     * @return the status of the validate request.
     * @throws AuthenticationException raised by the auth mechanism
     */
	public static AuthenticationStatus validateRequest(HttpMessageContext httpMessageContext,
	                                                    HttpAuthenticationMechanism authMech) throws AuthenticationException {
		
		logImplementationOnce();
        AuthenticationStatus authenticationStatus = authMech.validateRequest(httpMessageContext.getRequest(),
                                                                             httpMessageContext.getResponse(),
                                                                             httpMessageContext);
        return authenticationStatus;
	}

    /**
     * Execute the secure response functionality of a passed authentication mechanism.
     *
     * @param httpMessageContext is the calculated Http context
     * @param authMech is the authentication mechanism
     * @return the status of the secure response
     * @throws AuthenticationException raised by the auth mechanism
     */
	public static AuthenticationStatus secureResponse(HttpMessageContext httpMessageContext,
            HttpAuthenticationMechanism authMech) throws AuthenticationException {
		logImplementationOnce();
		AuthenticationStatus authenticationStatus = authMech.secureResponse(httpMessageContext.getRequest(),
                                                                            httpMessageContext.getResponse(),
                                                                            httpMessageContext);
		return authenticationStatus;
	}

    /**
     * Execute the clean subject functionality of a passed authentication mechanism.
     *
     * @param httpMessageContext is the calculated Http context
     * @param authMech is the authentication mechanism
     * @return the status of the clean subject
     * @throws AuthenticationException raised by the auth mechanism
     */
	public static void cleanSubject(HttpMessageContext httpMessageContext,
	                                 HttpAuthenticationMechanism authMech) {
		logImplementationOnce();
        authMech.cleanSubject(httpMessageContext.getRequest(), httpMessageContext.getResponse(), httpMessageContext);
	}
}
