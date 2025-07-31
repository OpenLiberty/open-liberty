/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.utils;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIEndpointProvider;

/**
 * Handle proxy requests
 */
public class ProxySupportUtil {

    private static final String HTTP_HEADER_REFERER = "Referer";
    private static final String DEFAULT_DOC_PATH = "/openapi";
    private static final String DEFAULT_UI_PATH = "/ui";
    private static final TraceComponent tc = Tr.register(ProxySupportUtil.class);

    @FFDCIgnore(MalformedURLException.class)
    private static URL extractURL(HttpServletRequest request, OpenAPIEndpointProvider openAPIEndpointProvider) {
        String urlString;
        String refererHeader = request.getHeader(HTTP_HEADER_REFERER);

        //Default endpoints
        String docPath = DEFAULT_DOC_PATH;
        String uiPath = docPath + DEFAULT_UI_PATH;

        //Ensure we have a context as change this will be null if bundle is STOPPED
        if (openAPIEndpointProvider != null) {
            docPath = openAPIEndpointProvider.getOpenAPIDocUrl();
            uiPath = openAPIEndpointProvider.getOpenAPIUIUrl();
        }

        //Ensure that in case somehow the values retrieved are null
        if (docPath == null) {
            docPath = DEFAULT_DOC_PATH;
        }
        if (uiPath == null) {
            uiPath = docPath + DEFAULT_UI_PATH;
        }

        if (refererHeader != null) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Using referer header to generate servers: " + refererHeader);
            }
            refererHeader = refererHeader.endsWith("/") ? refererHeader.substring(0, refererHeader.length() - 1) : refererHeader;
            //Follow original behavior if path values match their defaults
            if (docPath.equals(DEFAULT_UI_PATH) && uiPath.equals(DEFAULT_DOC_PATH + DEFAULT_UI_PATH)) {
                if (!refererHeader.endsWith("/ui") && !refererHeader.endsWith("/openapi")) {
                    refererHeader = null;
                }
                // If either path has been modified to not be default do a better check
            } else {
                try {
                    URL refURL = new URL(refererHeader);
                    String refPath = refURL.getPath();
                    if (!refPath.equals(docPath) && !refPath.equals(uiPath)) {
                        refererHeader = null;
                    }
                } catch (MalformedURLException e) {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Failed to create URL for " + refererHeader + ": " + e.getMessage());
                    }
                    // Given url in header is invalid, remove the value
                    refererHeader = null;
                }
            }
            if (refererHeader != null) {
                urlString = refererHeader;
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Using Referer header to generate servers: " + urlString);
                }
            } else {
                //fall back to using request url
                urlString = request.getRequestURL().toString();
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Unable to use Referer header, using request url to generate servers: " + urlString);
                }
            }
        } else {
            urlString = request.getRequestURL().toString();
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Using request url to generate servers: " + urlString);
            }
        }

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to create URL for " + urlString + ": " + e.getMessage());
            }
        }
        return url;
    }

    public static void processRequest(HttpServletRequest request, OpenAPIEndpointProvider openAPIEndpointProvider, ServerInfo serverInfo) {
        URL url = extractURL(request, openAPIEndpointProvider);
        if (url == null)
            return;
        serverInfo.setHost(url.getHost());
        Integer port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        if ("http".equalsIgnoreCase(url.getProtocol()) && port != serverInfo.getHttpPort()) {
            // We're using HTTP but not connecting to liberty's HTTP port
            // -> we're not connecting directly to liberty
            // -> we should overwrite the ports read from liberty's config
            serverInfo.setHttpPort(port);
            serverInfo.setHttpsPort(-1);
        } else if ("https".equalsIgnoreCase(url.getProtocol()) && port != serverInfo.getHttpsPort()) {
            // We're using HTTPS but not connecting to liberty's HTTPS port
            // -> we're not connecting directly to liberty
            // -> we should overwrite the ports read from liberty's config
            serverInfo.setHttpPort(-1);
            serverInfo.setHttpsPort(port);
        }
    }
}
