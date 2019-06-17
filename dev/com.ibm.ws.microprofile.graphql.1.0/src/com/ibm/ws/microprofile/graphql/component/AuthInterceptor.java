/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.component;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.Dependent;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.graphql.GraphQLApi;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@Dependent
@GraphQLApi
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AuthInterceptor {

    private final static TraceComponent tc = Tr.register(AuthInterceptor.class);

    @AroundInvoke
    public Object checkAuthorized(InvocationContext ctx) throws Exception {
        Method m = ctx.getMethod();

        if (isAuthorized(m)) {
            return ctx.proceed();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Unauthorized!");
        }
        // TODO: determine if this is the right thing to do here... maybe different exception?
        throw new Exception("Unauthorized");
    }

    private static boolean isAuthorized(Method m) {
        // First check annotations on methods
        if (m.getAnnotation(DenyAll.class) != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@DenyAll on method");
            }
            return false;
        }
        if (m.getAnnotation(PermitAll.class) != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@PermitAll on method");
            }
            return true;
        }
        RolesAllowed rolesAllowed = m.getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null) {
            HttpServletRequest req = AuthFilter.getCurrentRequest();
            for (String role : rolesAllowed.value()) {
                if (req.isUserInRole(role)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "User " + req.getUserPrincipal() + " is in (method) @RolesAllowed declared role, " + role);
                    }
                    return true;
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User " + req.getUserPrincipal() + " is not in role defined in @RolesAllowed on method.");
            }
            return false;
        }

        // if method has no annotations, check class level
        Class<?> cls = m.getDeclaringClass();
        if (cls.getAnnotation(DenyAll.class) != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@DenyAll on class");
            }
            return false;
        }
        if (cls.getAnnotation(PermitAll.class) != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@PermitAll on class");
            }
            return true;
        }
        rolesAllowed = cls.getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null) {
            HttpServletRequest req = AuthFilter.getCurrentRequest();
            for (String role : rolesAllowed.value()) {
                if (req.isUserInRole(role)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "User " + req.getUserPrincipal() + " is in (class) @RolesAllowed declared role, " + role);
                    }
                    return true;
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User " + req.getUserPrincipal() + " is not in role defined in @RolesAllowed on class.");
            }
            return false;
        }

        // no annotations on either class or method - authorize it
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "No annotation on method or class - permitting");
        }
        return true;
    }
}
