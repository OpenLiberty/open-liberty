/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import io.netty.util.AttributeKey;

/**
 *
 */
public class NettyHttpConstants {

    public static final AttributeKey<String> FORWARDED_PROTO_KEY = AttributeKey.valueOf("forwardedProto");
    public static final AttributeKey<String> FORWARDED_HOST_KEY = AttributeKey.valueOf("forwardedHost");
    public static final AttributeKey<String> FORWARDED_PORT_KEY = AttributeKey.valueOf("forwardedPort");
    public static final AttributeKey<String[]> FORWARDED_BY_KEY = AttributeKey.valueOf("forwardedBy");
    public static final AttributeKey<String[]> FORWARDED_FOR_KEY = AttributeKey.valueOf("forwardedFor");
    public static final AttributeKey<Long> REQUEST_START_TIME = AttributeKey.valueOf("requestStartTime");
    public static final AttributeKey<Long> RESPONSE_BYTES_WRITTEN = AttributeKey.valueOf("bytesWritten");
    public static final AttributeKey<Boolean> IS_SECURE = AttributeKey.valueOf("isSecure");
    public static final AttributeKey<String> ACCEPT_ENCODING = AttributeKey.valueOf("acceptEncoding");

}
