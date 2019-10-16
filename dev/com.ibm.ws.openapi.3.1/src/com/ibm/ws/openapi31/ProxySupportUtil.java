/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class ProxySupportUtil {

    private static final String HTTP_HEADER_REFERER = "Referer";
    private static TraceComponent tc = Tr.register(ProxySupportUtil.class);

    @FFDCIgnore(MalformedURLException.class)
    private static URL extractURL(HttpServletRequest request) {
        String urlString;
        String refererHeader = request.getHeader(HTTP_HEADER_REFERER);
        if (refererHeader != null) {
            if (OpenAPIUtils.isDebugEnabled(tc)) {
                Tr.debug(tc, "Using referer header to generate servers: " + refererHeader);
            }
            refererHeader = refererHeader.endsWith("/") ? refererHeader.substring(0, refererHeader.length() - 1) : refererHeader;
            if (!refererHeader.endsWith("/explorer") && !refererHeader.endsWith("/docs")) {
                refererHeader = null;
            }
            if (refererHeader != null) {
                urlString = refererHeader;
            } else {
                //fall back to using request url
                urlString = request.getRequestURL().toString();
            }
        } else {
            urlString = request.getRequestURL().toString();
            if (OpenAPIUtils.isDebugEnabled(tc)) {
                Tr.debug(tc, "Using request url to generate servers: " + urlString);
            }
        }

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            if (OpenAPIUtils.isDebugEnabled(tc)) {
                Tr.event(tc, "Failed to create URL for " + urlString + ": " + e.getMessage());
            }
        }
        return url;
    }

    public static void processRequest(HttpServletRequest request, ServerInfo serverInfo) {
        URL url = extractURL(request);
        if (url == null)
            return;
        serverInfo.setHost(url.getHost());
        Integer port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        if (port != serverInfo.getHttpPort() && port != serverInfo.getHttpsPort()) {
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                serverInfo.setHttpPort(-1);
                serverInfo.setHttpsPort(port);
            } else {
                serverInfo.setHttpPort(port);
                serverInfo.setHttpsPort(-1);
            }
        }
    }
}
