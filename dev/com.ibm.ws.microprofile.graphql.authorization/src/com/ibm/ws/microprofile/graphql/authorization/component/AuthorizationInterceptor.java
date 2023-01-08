/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.authorization.component;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.function.Supplier;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.GraphQLException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authorization.util.RoleMethodAuthUtil;
import com.ibm.ws.security.authorization.util.UnauthenticatedException;

@Dependent
@GraphQLApi
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AuthorizationInterceptor {

    private final static TraceComponent tc = Tr.register(AuthorizationInterceptor.class);
    private final static AuthorizationFilter AUTH_FILTER = AuthorizationFilter.getInstance();
    private final static Supplier<Principal> AUTH_FILTER_PRINCIPAL = new Supplier<Principal>() {
        @Override
        public Principal get() {
            return AUTH_FILTER.getUserPrincipal();
        }
    };

    @AroundInvoke
    public Object checkAuthorized(InvocationContext ctx) throws Exception {

        Method m = ctx.getMethod();
        if (isAuthorized(m)) {
            return ctx.proceed();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Unauthorized");
        }
        throw new GraphQLException("Unauthorized");
    }

    @FFDCIgnore({UnauthenticatedException.class})
    private boolean isAuthorized(Method m) {
        try {
            return RoleMethodAuthUtil.parseMethodSecurity(m,
                AUTH_FILTER_PRINCIPAL,
                AUTH_FILTER::isUserInRole);
        } catch (UnauthenticatedException ex) {
            try {
                return AUTH_FILTER.authenticate() && RoleMethodAuthUtil.parseMethodSecurity(m,
                    AUTH_FILTER_PRINCIPAL,
                    AUTH_FILTER::isUserInRole);
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to authenticate or failed auth check", t);
                }
            }
        }
        return false;
    }
}