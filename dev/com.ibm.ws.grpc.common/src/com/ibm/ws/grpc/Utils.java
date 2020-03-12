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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

import io.grpc.internal.GrpcUtil;

public class Utils {

	protected static final String STATUS_OK = "200";
	protected static final String HTTP_METHOD = GrpcUtil.HTTP_METHOD;
	protected static final String HTTP_GET_METHOD = "GET";
	protected static final String HTTPS = "https";
	protected static final String HTTP = "http";
	protected static final String CONTENT_TYPE_HEADER = CONTENT_TYPE_KEY.name();
	protected static final String CONTENT_TYPE_GRPC = GrpcUtil.CONTENT_TYPE_GRPC;
	protected static final String TE_HEADER = GrpcUtil.TE_HEADER.name();
	protected static final String TE_TRAILERS = GrpcUtil.TE_TRAILERS;
	protected static final String USER_AGENT = GrpcUtil.USER_AGENT_KEY.name();

	protected final static String AUTHORITY = ":authority";
	protected final static String METHOD = ":method";
	protected final static String PATH = ":path";
	protected final static String SCHEME = ":scheme";
	protected final static String STATUS = ":status";

	public static void traceMessage(Logger logger, String className, Level level, String method, String message) {
		traceMessage(logger, className, level, method, message, null);
	}

	public static void traceMessage(Logger logger, String className, Level level, String method, String message,
			Throwable exception) {
		if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(level)) {
			if (exception != null) {
				logger.logp(level, className, method, message, exception);
			} else {
				logger.logp(level, className, method, message);
			}
		}
	}	
}
