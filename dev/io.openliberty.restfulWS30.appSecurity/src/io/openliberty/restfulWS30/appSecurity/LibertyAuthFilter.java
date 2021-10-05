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
package io.openliberty.restfulWS30.appSecurity;

import java.io.IOException;
import java.lang.reflect.Method;
import jakarta.annotation.Priority;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authorization.util.RoleMethodAuthUtil;
import com.ibm.ws.security.authorization.util.UnauthenticatedException;

// This code is practically an EE9 version of the LibertyAuthFilter in the com.ibm.ws.jaxrs.2.1.common project.
// Changes here may need to be reflected there also - and vice versa.

// Set the Priority to Priorities.AUTHORIZATION + 1 so that user filters take precedence.
@Priority(Priorities.AUTHORIZATION + 1)
@Provider
public class LibertyAuthFilter implements ContainerRequestFilter {

    @Context
    HttpServletRequest req;

    @Context
    HttpServletResponse resp;

    @Context
    SecurityContext securityContext;

    @Context
    ResourceInfo resourceInfo;

    @Override
    @FFDCIgnore({ UnauthenticatedException.class, UnauthenticatedException.class })
    public void filter(ContainerRequestContext context) {
        try {
            handleMessage();
        } catch (UnauthenticatedException ex) {
            try { 
                if (authenticate()) {
                    // try again with authenticated user
                    handleMessage();
                    return;
                }
            } catch (UnauthenticatedException ex2) {
                // ignore - still abort with 401
            }
            // could not authenticate - return 401
            // TODO: check response code on servlet response and use the same status?
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private boolean authenticate() {
        try {
            return req.authenticate(resp);
        } catch (IOException | ServletException e) {
            // AutoFFDC
        }
        return false;
    }

    private void handleMessage() throws UnauthenticatedException, ForbiddenException {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            throw new ForbiddenException("Method is not available : Unauthorized");
        }
        if (securityContext != null && RoleMethodAuthUtil.parseMethodSecurity(method,
                                                       securityContext.getUserPrincipal(),
                                                       s -> securityContext.isUserInRole(s))) {
            return;
        } else if (RoleMethodAuthUtil.parseMethodSecurity(method,
                                                       req.getUserPrincipal(),
                                                       s -> req.isUserInRole(s))) {
            return;
        }

        throw new ForbiddenException("Unauthorized");
    }
}
