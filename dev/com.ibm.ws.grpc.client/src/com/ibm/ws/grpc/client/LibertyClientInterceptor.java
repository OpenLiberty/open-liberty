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

import com.ibm.ws.grpc.Utils;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An interceptor to forward authorization tokens. This works by using the
 * WebContainerRequestState to grab the authorization header off of an
 * underlying ServletRequest. That WebContainerRequestState makes use of a
 * ThreadLocal to store the ServletRequest, so for this authorization token
 * propagation to work the client RPC must not be made on a new thread.
 * 
 */
public class LibertyClientInterceptor implements ClientInterceptor {

	private static final Logger logger = Logger.getLogger(LibertyClientInterceptor.class.getName());
	private static final String CLASS_NAME = LibertyClientInterceptor.class.getName();

	static final Metadata.Key<String> AUTHZ_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
			CallOptions callOptions, Channel next) {

		return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

			@Override
			public void start(Listener<RespT> responseListener, Metadata headers) {
				/*
				 * grab the request state ThreadLocal, get the authorization header, and pass it
				 * along with this call's headers
				 * 
				 * TODO: this needs to be configurable with some new element like 
				 * <grpcTarget authnToken="ltpa, saml, oauth, mpjwt"/>
				 */
				WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
				String authzValue = null;
				if (reqState != null) {
					authzValue = reqState.getCurrentThreadsIExtendedRequest().getIRequest().getHeader("authorization");
				}
				if (authzValue != null) {
					headers.put(AUTHZ_KEY, authzValue);
					Utils.traceMessage(logger, CLASS_NAME, Level.FINEST, "interceptCall",
							"authorization token propagated: " + authzValue);
				}
				super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
				}, headers);
			}
		};
	}
}