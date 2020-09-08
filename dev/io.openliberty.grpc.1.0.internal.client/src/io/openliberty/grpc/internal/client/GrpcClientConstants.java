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

public class GrpcClientConstants {
	
	// config related constants
    public static final String HEADER_PROPAGATION_PROP = "headersToPropagate";
    public static final String HOST_PROP = "host";
    public static final String PATH_PROP = "path";
    public static final String KEEP_ALIVE_WITHOUT_CALLS_PROP = "keepAliveWithoutCalls";
    public static final String KEEP_ALIVE_TIME_PROP = "keepAliveTime";
    public static final String KEEP_ALIVE_TIMEOUT_PROP = "keepAliveTimeout";
    public static final String MAX_INBOUND_MSG_SIZE_PROP = "maxInboundMessageSize";
    public static final String MAX_INBOUND_METADATA_SIZE_PROP = "maxInboundMetadataSize";
    public static final String CLIENT_INTERCEPTORS_PROP = "clientInterceptors";
    public static final String SSL_CFG_PROP = "sslRef";
    public static final String OVERRIDE_AUTHORITY_PROP = "overrideAuthority";
    public static final String USER_AGENT_PROP = "userAgent";
}
