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
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.interceptor.security.AuthenticationException;
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
            System.out.println("handleMessage javax");
            System.out.println(jaxrsSecurityContext);
            Method method = getTargetMethod(message);
            if (parseMethodSecurity(method, jaxrsSecurityContext)) {
                return;
            }
        } else {
            HttpServletRequest req = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
            Method method = MessageUtils.getTargetMethod(message, () -> 
                new AccessDeniedException("Method is not available : Unauthorized"));
            if (RoleMethodAuthUtil.parseMethodSecurity(method, req.getUserPrincipal(), s -> req.isUserInRole(s))) {
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
    
    private boolean parseMethodSecurity(Method method, SecurityContext sc) {

        boolean denyAll = getDenyAll(method);
        if (denyAll) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found DenyAll for method: {} " + method.getName()
                             + ", Injection Processing for web service is ignored");
            }
            // throw new WebApplicationException(Response.Status.FORBIDDEN);
            return false;

        } else { // try RolesAllowed

            RolesAllowed rolesAllowed = getRolesAllowed(method);
            if (rolesAllowed != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(
                             tc,
                             "found RolesAllowed in method: {} "
                                 + method.getName(),
                             new Object[] { rolesAllowed.value() });
                }
                if (!ensureAuthentication(sc)) {
                    return false;
                }
                String[] theseroles = rolesAllowed.value();

                if (!isUserInRole(sc, Arrays.asList(theseroles), false)) {
                    return false;
                }
                return true;

            } else {
                boolean permitAll = getPermitAll(method);
                if (permitAll) {
                    if (TraceComponent.isAnyTracingEnabled()
                        && tc.isDebugEnabled()) {
                        Tr.debug(
                                 tc,
                                 "Found PermitAll for method: {}"
                                                 + method.getName());
                    }
                    return true;
                } else { // try class level annotations
                    Class<?> cls = method.getDeclaringClass();
                    return parseClassSecurity(cls, sc);
                }
            }
        }
    }
    
    private boolean parseClassSecurity(Class<?> cls, SecurityContext sc) {

        // try DenyAll
        DenyAll denyAll = cls.getAnnotation(DenyAll.class);
        if (denyAll != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found class level @DenyAll - authorization denied for " + cls.getName());
            }
            return false;
        } else { // try RolesAllowed

            RolesAllowed rolesAllowed = cls.getAnnotation(RolesAllowed.class);
            if (rolesAllowed != null) {

                String[] theseroles = rolesAllowed.value();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(
                             tc,
                             "found RolesAllowed in class level: {} "
                                 + cls.getName(),
                             new Object[] { theseroles });
                }
                if (!ensureAuthentication(sc)) {
                    return false;
                }
                if (!isUserInRole(sc, Arrays.asList(theseroles), false)) {
                    return false;
                }
                return true;
            } else {
                return true;
            }
        }
    }
    
    private boolean ensureAuthentication(SecurityContext sc) {
        Principal p = sc.getUserPrincipal();
        if (p == null || "UNAUTHENTICATED".equals(p.getName())) {
            throw new AuthenticationException();
        }
        return true;
    }
    
    private RolesAllowed getRolesAllowed(Method method) {
        return method.getAnnotation(RolesAllowed.class);
    }

    private boolean getPermitAll(Method method) {
        return method.isAnnotationPresent(PermitAll.class);
    }

    private boolean getDenyAll(Method method) {
        return method.isAnnotationPresent(DenyAll.class);
    }
    
    private static final String ALL_ROLES = "*";
    
    protected boolean isUserInRole(javax.ws.rs.core.SecurityContext sc, List<String> roles, boolean deny) {
        
        if (roles.size() == 1 && ALL_ROLES.equals(roles.get(0))) {
            return !deny;
        }
        
        for (String role : roles) {
            if (sc.isUserInRole(role)) {
                return !deny;
            }
        }
        return deny;
    }
}
