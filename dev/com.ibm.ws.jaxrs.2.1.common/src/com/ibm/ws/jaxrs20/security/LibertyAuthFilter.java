/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.security;

import java.io.IOException;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authorization.util.RoleMethodAuthUtil;
import com.ibm.ws.security.authorization.util.UnauthenticatedException;

// Set the Priority to Priorities.AUTHORIZATION + 1 so that user filters take precedence.
@Priority(Priorities.AUTHORIZATION + 1)
public class LibertyAuthFilter implements ContainerRequestFilter {

    private static final TraceComponent tc = Tr
                    .register(LibertyAuthFilter.class);
    
    @Override
    @FFDCIgnore({ UnauthenticatedException.class, UnauthenticatedException.class, AccessDeniedException.class })
    public void filter(ContainerRequestContext context) {
        Message m = JAXRSUtils.getCurrentMessage();
        try {
            try {
                handleMessage(m);
            } catch (UnauthenticatedException ex) {
                try { 
                    if (authenticate(m)) {
                        // try again with authenticated user
                        handleMessage(m);
                        return;
                    }
                } catch (UnauthenticatedException ex2) {
                    // ignore - still abort with 401
                }
                // could not authenticate - return 401
                // TODO: check response code on servlet response and use the same status?
                context.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            }
        } catch (AccessDeniedException ex) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

    private boolean authenticate(Message m) {
        HttpServletRequest req = (HttpServletRequest) m.get(AbstractHTTPDestination.HTTP_REQUEST);
        HttpServletResponse res = (HttpServletResponse) m.get(AbstractHTTPDestination.HTTP_RESPONSE);
        try {
            return req.authenticate(res);
        } catch (IOException | ServletException e) {
            // AutoFFDC
        }
        return false;
    }

    private void handleMessage(Message message) throws UnauthenticatedException{
        SecurityContext jaxrsSecurityContext = message.get(SecurityContext.class);
        if (jaxrsSecurityContext != null && jaxrsSecurityContext instanceof SecurityContext) {
            Method method = getTargetMethod(message);
            if (RoleMethodAuthUtil.parseMethodSecurity(method,
                                                       jaxrsSecurityContext.getUserPrincipal(),
                                                       s -> jaxrsSecurityContext.isUserInRole(s))) {
                return;
            }
        } else {
            HttpServletRequest req = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
            Method method = MessageUtils.getTargetMethod(message, () -> 
                new AccessDeniedException("Method is not available : Unauthorized"));
            if (RoleMethodAuthUtil.parseMethodSecurity(method,
                                                       req.getUserPrincipal(),
                                                       s -> req.isUserInRole(s))) {
                return;
            }
        }

        throw new AccessDeniedException("Unauthorized");
    }
    
    protected Method getTargetMethod(Message m) {
        BindingOperationInfo bop = m.getExchange().getBindingOperationInfo();
        if (bop != null) {
            MethodDispatcher md = (MethodDispatcher) 
                m.getExchange().getService().get(MethodDispatcher.class.getName());
            return md.getMethod(bop);
        } 
        Method method = (Method)m.get("org.apache.cxf.resource.method");
        if (method != null) {
            return method;
        }
        throw new AccessDeniedException("Method is not available : Unauthorized");
    }
}
