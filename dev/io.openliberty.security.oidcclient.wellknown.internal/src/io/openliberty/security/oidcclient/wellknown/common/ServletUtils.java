/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclient.wellknown.common;

/**
 * Shared helper methods for protected resource metadata servlet implementations.
 */
public final class ServletUtils {

    private ServletUtils() {
        // Prevent instantiation.
    }

    /**
     * Builds the absolute protected resource URL from the current metadata request.
     * <p>
     * For example, if the metadata request is:
     * </p>
     *
     * <pre>
     * https://localhost:9443/.well-known/oauth-protected-resource/myApp/protected
     * </pre>
     *
     * <p>
     * and the protected resource path is {@code /myApp/protected}, this method returns:
     * </p>
     *
     * <pre>
     * https://localhost:9443/myApp/protected
     * </pre>
     *
     * @param request               current request adapter
     * @param protectedResourcePath normalized protected resource path, such as {@code /myApp/protected}
     * @return absolute protected resource URL
     */
    public static String buildResourceUrl(HttpRequestAdapter request, String protectedResourcePath) {
        StringBuilder resourceUrl = new StringBuilder();

        resourceUrl.append(request.getScheme()).append("://").append(request.getServerName());

        int serverPort = request.getServerPort();
        if (serverPort > 0 && !isDefaultPort(request.getScheme(), serverPort)) {
            resourceUrl.append(':').append(serverPort);
        }

        resourceUrl.append(protectedResourcePath);

        return resourceUrl.toString();
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
               || ("https".equalsIgnoreCase(scheme) && port == 443);
    }
}
