/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.token.ltpa.fat;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Shared utility methods for LTPA token refresh tests.
 */
public class LTPATestUtils {

    private static final String LTPA_COOKIE_NAME = "LtpaToken2";

    /**
     * Extract LTPA cookie value from HTTP response.
     *
     * @param conn the HTTP connection
     * @return the LTPA cookie value, or null if not found
     */
    public static String extractLTPACookie(HttpURLConnection conn) {
        Map<String, List<String>> headers = conn.getHeaderFields();
        List<String> cookies = headers.get("Set-Cookie");

        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith(LTPA_COOKIE_NAME + "=")) {
                    int start = cookie.indexOf("=") + 1;
                    int end = cookie.indexOf(";");
                    if (end == -1) {
                        end = cookie.length();
                    }
                    return cookie.substring(start, end);
                }
            }
        }

        return null;
    }

    /**
     * Get the Set-Cookie header for a specific cookie name.
     *
     * @param headers the response headers
     * @param cookieName the name of the cookie to find
     * @return the full Set-Cookie header value, or null if not found
     */
    public static String getCookieHeader(Map<String, List<String>> headers, String cookieName) {
        List<String> cookies = headers.get("Set-Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith(cookieName + "=")) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * Mask cookie value for logging (show only first and last few characters).
     *
     * @param cookie the cookie value to mask
     * @return masked cookie value for safe logging
     */
    public static String maskCookie(String cookie) {
        if (cookie == null) {
            return "null";
        }
        if (cookie.length() < 20) {
            return "***";
        }
        return cookie.substring(0, 10) + "..." + cookie.substring(cookie.length() - 10);
    }
}

// Made with Bob
