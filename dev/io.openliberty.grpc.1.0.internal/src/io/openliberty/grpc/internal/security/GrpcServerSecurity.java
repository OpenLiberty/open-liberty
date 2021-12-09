/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.grpc.internal.security;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authorization.util.RoleMethodAuthUtil;
import com.ibm.ws.security.authorization.util.UnauthenticatedException;

import io.grpc.Metadata;
import io.openliberty.grpc.internal.GrpcMessages;
import io.openliberty.grpc.internal.servlet.GrpcServletUtils;

public class GrpcServerSecurity {
    
    private static final TraceComponent tc = Tr.register(GrpcServerSecurity.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

    public final static String LIBERTY_AUTH_KEY_STRING = "libertyAuthCheck";
    private final static Map<String, Boolean> authMap = new ConcurrentHashMap<String, Boolean>();
    public static final Metadata.Key<String> LIBERTY_AUTH_KEY = Metadata.Key.of(LIBERTY_AUTH_KEY_STRING,
            Metadata.ASCII_STRING_MARSHALLER);
    
    
    /**
     * Helper method to add the "authorized" flag to the byte arrays that will get
     * built into Metadata
     * 
     * @param byteArrays
     * @param req
     * @param authorized
     */
    public static void addLibertyAuthHeader(List<byte[]> byteArrays, HttpServletRequest req, boolean authorized) {
        byteArrays.add(LIBERTY_AUTH_KEY.name().getBytes(StandardCharsets.US_ASCII));
        byteArrays.add((String.valueOf(req.hashCode())).getBytes(StandardCharsets.US_ASCII));
        authMap.put(String.valueOf(req.hashCode()), authorized);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "adding {0} to authMap with value {1}", req.hashCode(), authorized);
        }
    }
    
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
    @FFDCIgnore({ UnauthenticatedException.class, UnauthenticatedException.class, UnauthorizedException.class })
    public static boolean doServletAuth(HttpServletRequest req, HttpServletResponse res, String requestPath) {
        try {
            handleSecurity(req, requestPath);
            return true;
        } catch (UnauthenticatedException ex) {			
            Tr.error(tc, "authentication.error", new Object[] {requestPath , ex.getMessage()});	
        } catch (UnauthorizedException e) {
            Tr.error(tc, "authorization.error", new Object[] {requestPath , e.getMessage()});	
        }
        return false;
    }

    private static void handleSecurity(HttpServletRequest req, String path)
            throws UnauthenticatedException, UnauthorizedException {

        Method method = GrpcServletUtils.getTargetMethod(path);
        if (method == null) {
            // the requested service doesn't exist - we'll handle this further up
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "gRPC target service for this path {0} does not exist", path);
            }
            return;
        }
        if (RoleMethodAuthUtil.parseMethodSecurity(method, req.getUserPrincipal(), s -> req.isUserInRole(s))) {
            return;
        }
        throw new UnauthorizedException("Unauthorized");
    }

    /**
     * 
     * @param key the LIBERTY_AUTH_KEY to check
     * @return the authorization value for the key in GrpcServletUtils.authMap, or
     *         false if the key is null
     */
    public static boolean isAuthorized(String key) {
        if (key == null) {
            return false;
        }
        else return Boolean.TRUE.equals(authMap.remove(key));
    }

}

