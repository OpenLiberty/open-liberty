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

package com.ibm.ws.grpc;

import static io.grpc.internal.GrpcUtil.CONTENT_TYPE_KEY;

import io.grpc.internal.GrpcUtil;

/**
 * Utilities for gRPC processing
 */
class Utils {

	// TODO: non-ascii string format??
	public static final String STATUS_OK = "200";
	public static final String HTTP_METHOD = GrpcUtil.HTTP_METHOD;
	public static final String HTTP_GET_METHOD = "GET";
	public static final String HTTPS = "https";
	public static final String HTTP = "http";
	public static final String CONTENT_TYPE_HEADER = CONTENT_TYPE_KEY.name();
	public static final String CONTENT_TYPE_GRPC = GrpcUtil.CONTENT_TYPE_GRPC;
	public static final String TE_HEADER = GrpcUtil.TE_HEADER.name();
	public static final String TE_TRAILERS = GrpcUtil.TE_TRAILERS;
	public static final String USER_AGENT = GrpcUtil.USER_AGENT_KEY.name();
	
    public final static String AUTHORITY = ":authority";
    public final static String METHOD = ":method";
    public final static String PATH = ":path";
    public final static String SCHEME = ":scheme";
    public final static String STATUS = ":status";
}
