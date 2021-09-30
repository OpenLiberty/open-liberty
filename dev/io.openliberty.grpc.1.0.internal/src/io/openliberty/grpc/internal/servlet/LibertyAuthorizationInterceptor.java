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
package io.openliberty.grpc.internal.servlet;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.openliberty.grpc.internal.security.GrpcServerSecurity;

/**
 * Interceptor used to perform Liberty-specific authorization. If the backing
 * request for the call was not authorized, this Interceptor will set the status
 * to UNAUTHENTICATED.
 */
public class LibertyAuthorizationInterceptor implements ServerInterceptor {

    /**
     * Grab the LIBERTY_AUTH_KEY from the headers on this call and use that key to
     * check the authorization map. If this call is not authorized, set the status
     * to UNAUTHENTICATED and stop the call chain
     */
    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String key = headers.get(GrpcServerSecurity.LIBERTY_AUTH_KEY);
        if (!GrpcServerSecurity.isAuthorized(key)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Unauthorized"), headers);
            // return no-op listener
            return new ServerCall.Listener<ReqT>() {
            };
        }
        return next.startCall(call, headers);
    }
}
