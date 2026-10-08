/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.grpc.internal.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.grpc.Metadata;
import io.openliberty.grpc.internal.GrpcMessages;

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

