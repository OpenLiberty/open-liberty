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
package io.openliberty.grpc.internal.client;

import io.grpc.Metadata;

public class GrpcClientConstants {
	
	// config related constants
    public static final String WEB_TARGET = "webTarget";
    public static final String AUTH_TOKEN_PROP = "authnToken";
    public static final String HEADER_PROPAGATION_PROP = "headersToPropagate";
    public static final String TARGET_PROP = "target";
    public static final String ENABLE_KEEP_ALIVE_TIME_PROP = "enableKeepAlive";
    public static final String KEEP_ALIVE_TIME_PROP = "keepAliveTime";
    public static final String KEEP_ALIVE_TIMEOUT_PROP = "keepAliveTimeout";
    public static final String MAX_INBOUND_MSG_SIZE_PROP = "maxInboundMessageSize";
    public static final String CLIENT_INTERCEPTORS_PROP = "clientInterceptors";
    public static final String SSL_CFG_PROP = "sslRef";
    
    // security config related constants
	public static final Metadata.Key<String> AUTHZ_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
	public static final String JWT = "jwt";
	public static final String MPJWT = "mpjwt";
	public static final String OAUTH = "oauth";
	public static final String SAML = "saml";
	public static final String LTPA = "ltpa";
	public static final String BASIC = "basic";
	public static final String BEARER_TOKEN = "bearer";
}
