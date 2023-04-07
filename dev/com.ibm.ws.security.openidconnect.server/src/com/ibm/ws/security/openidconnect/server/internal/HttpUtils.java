/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

/**
 * Utility class to assist with HTTP related operations
 */
public class HttpUtils {
    public static final String CT_APPLICATION_JSON = "application/json";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String PUBLIC = "public";
    public static final String PRIVATE = "private";
    public static final String MAX_AGE = "max-age";

    /**
     * Constructs a cache control header with the format:
     * Cache-Control: public|private, max-age=xxx
     *
     * @param isPublic public or private
     * @param maxAge maximum age
     * @return header value
     */
    public static String constructCacheControlHeaderWithMaxAge(boolean isPublic, String maxAge) {
        String type = (isPublic ? PUBLIC : PRIVATE);
        String headerValue = String.format("%s, %s=%s", type, MAX_AGE, maxAge);
        return headerValue;
    }

}
