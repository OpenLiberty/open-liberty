/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.grpc.internal.server.security;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authorization.util.RoleMethodAuthUtil;
import com.ibm.ws.security.authorization.util.UnauthenticatedException;

import io.openliberty.grpc.internal.GrpcMessages;
import io.openliberty.grpc.internal.security.GrpcSecurityService;
import io.openliberty.grpc.internal.servlet.GrpcServletUtils;
import io.openliberty.grpc.internal.servlet.GrpcServletUtils.GrpcMethodNotFoundException;

@Component(service = GrpcSecurityService.class)
public class GrpcSecurityComponent implements GrpcSecurityService {

    private static final TraceComponent tc = Tr.register(GrpcSecurityComponent.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

    /**
     * Checks if a given request is authorized to access the requested method, by
     * scanning the requested method for @DenyAll, @RolesAllowed, or @AllowAll and
     * validating the request's Subject
     *
     * @param req
     * @param res
     * @param requestPath
     * @return
     */
    @FFDCIgnore({ UnauthenticatedException.class, GrpcMethodNotFoundException.class })
    public boolean doServletAuth(HttpServletRequest req, HttpServletResponse res, String requestPath) {
        Method method = null;
        try {
            method = GrpcServletUtils.getTargetMethod(requestPath);
        } catch (GrpcMethodNotFoundException e) {
            // The service is registered but the method could not be resolved — deny.
            Tr.error(tc, "authorization.error", new Object[] {requestPath, "Unauthorized"});
            return false;
        }
        boolean isAuthorized = false;
        if (method == null) {
            // The service is not registered — let the gRPC layer handle this further up.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "gRPC target service for this path {0} does not exist", requestPath);
            }
            isAuthorized = true;
        } else {
            try {
            	if (RoleMethodAuthUtil.parseMethodSecurity(method,
                        new Supplier<Principal>() {
                    @Override
                    public Principal get() {
                        return req.getUserPrincipal();
                    }
                },
                s -> req.isUserInRole(s))) {
            		isAuthorized = true;
            	} else {
            		Tr.error(tc, "authorization.error", new Object[] {requestPath , "Unauthorized"});
            	}
            } catch (UnauthenticatedException ex) {
                Tr.error(tc, "authentication.error", new Object[] {requestPath , ex.getMessage()});
            }
        }
        return isAuthorized;
    }
}
