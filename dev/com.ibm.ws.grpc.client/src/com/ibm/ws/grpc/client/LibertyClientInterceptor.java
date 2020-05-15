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
package com.ibm.ws.grpc.client;

import com.ibm.ws.grpc.client.security.LibertyGrpcAuthPropagationSupport;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * An Interceptor used to provide Liberty integrations to outbound gRPC calls.
 * This currently provides authorization token propagation and generic HTTP
 * header propagation
 */
public class LibertyClientInterceptor implements ClientInterceptor {

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
			CallOptions callOptions, Channel next) {

		return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

			@Override
			public void start(Listener<RespT> responseListener, Metadata headers) {

				// add authorization header if configured
				LibertyGrpcAuthPropagationSupport.handleAuthorization(method, headers);

				// forward any headers that are configured
				LibertyHeaderPropagationSupport.handleHeaderPropagation(method, headers);

				super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
				}, headers);
			}
		};
	}
}