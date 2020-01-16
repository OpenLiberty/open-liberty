/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import javax.servlet.http.HttpServletRequest;

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
     * Provides the full URL request context servlet path,
     * defined by scheme://serverName(:port)/ctxPath/servletPath
     * 
     * @param request is the HttpServletRequest
     * @return a string of the full URL request context servlet path
     */
    public static String getFullCtxServletPath(HttpServletRequest request) {
        StringBuffer fullCtxServletPath = new StringBuffer();

        fullCtxServletPath.append(request.getScheme());
        fullCtxServletPath.append("://");
        fullCtxServletPath.append(request.getServerName());

        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            fullCtxServletPath.append(":");
            fullCtxServletPath.append(request.getServerPort());
        }

        fullCtxServletPath.append(request.getContextPath());
        fullCtxServletPath.append(request.getServletPath());

        return fullCtxServletPath.toString();
    }

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
