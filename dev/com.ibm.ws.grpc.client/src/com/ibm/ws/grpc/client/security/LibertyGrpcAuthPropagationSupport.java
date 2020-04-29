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
package com.ibm.ws.grpc.client.security;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.grpc.client.GrpcClientConstants;
import com.ibm.ws.grpc.client.config.GrpcClientConfigHolder;
import com.ibm.ws.grpc.client.security.oauth.GrpcOAuthPropagationHelper;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Support for adding authorization tokens to outbound client calls
 */
public class LibertyGrpcAuthPropagationSupport {

	private static final TraceComponent tc = Tr.register(LibertyGrpcAuthPropagationSupport.class);

	public LibertyGrpcAuthPropagationSupport() {
	}

	/**
	 * Add an authorization token using the configuration for the current method.
	 * Configurable via <grpcTarget authnToken="jwt,oauth,..."/>
	 * 
	 * @param method   the scope to check
	 * @param Metadata headers to which to add the Authorization token
	 */
	@SuppressWarnings("rawtypes")
	public static void handleAuthorization(MethodDescriptor method, Metadata headers) {

		// get the current token propagation support from the applicable grpcTarget
		// authnToken config
		String authSupport = GrpcClientConfigHolder.getAuthnSupport(method.getFullMethodName());

		if (authSupport == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "handleAuthorization: no authorization propagation configured");
			}
			return;
		}

		switch (authSupport) {
		case GrpcClientConstants.JWT:
			handleOAuthTokensCommon(headers, GrpcOAuthPropagationHelper.getJwtToken());
			break;
		case GrpcClientConstants.OAUTH:
			handleOAuthTokensCommon(headers, GrpcOAuthPropagationHelper.getAccessToken());
			break;
//		case GrpcClientConstants.MPJWT:
//			break;
//		case GrpcClientConstants.SAML:
//			break;
//		case GrpcClientConstants.LTPA:
//			break;
		}
	}

	/**
	 * Add a token header the outbound headers
	 * 
	 * @param token
	 * @param headers
	 */
	private static void addAuthnHeader(String token, Metadata headers) {
		if (token != null && headers != null) {
			// Authorization=[Bearer="<accessToken>"]
			headers.put(GrpcClientConstants.AUTHZ_KEY, "Bearer " + token);
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Authorization header with Bearer token is added successfully");
			}
		}
	}

	/**
	 * See
	 * com.ibm.ws.jaxrs20.client.security.oauth.LibertyJaxRsClientOAuthInterceptor
	 */
	private static void handleOAuthTokensCommon(Metadata headers, String accessToken) {
		// retrieve the token from the Subject in current thread
		try {
			if (accessToken != null && !accessToken.isEmpty()) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Retrieved an OAuth access/jwt token. About to set a request cookie: " + accessToken);
				}
				// Authorization=[Bearer="<accessToken>"]
				addAuthnHeader(accessToken, headers);
			} else { // no user credential available
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Cannot find an OAuth access token out of the WSSubject");
				}
				// Because this is a client configuration property, we won't throws exception if
				// it doesn't work, please analyze trace for detail
				// throw new ProcessingException("Cannot find a ltpa authentication token off of
				// the thread");
			}
		} catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Exception caught", e);
			}
		}
	}
}
