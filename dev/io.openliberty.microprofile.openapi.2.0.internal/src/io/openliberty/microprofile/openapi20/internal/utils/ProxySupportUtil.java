/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.utils;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIEndpointProvider;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Handle proxy requests
 */
public class ProxySupportUtil {

    private static final String HTTP_HEADER_REFERER = "Referer";
    private static TraceComponent tc = Tr.register(ProxySupportUtil.class);

    public static void processRequest(HttpServletRequest request, ServerInfo serverInfo) {
        URL url = extractURL(request);
        if (url == null)
            return;
        serverInfo.setHost(url.getHost());
        Integer port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        if (port != serverInfo.getHttpPort() && port != serverInfo.getHttpsPort()) {
            if (Constants.PROTOCOL_HTTPS.equalsIgnoreCase(url.getProtocol())) {
                serverInfo.setHttpPort(-1);
                serverInfo.setHttpsPort(port);
            } else {
                serverInfo.setHttpPort(port);
                serverInfo.setHttpsPort(-1);
            }
        }
    }

    @FFDCIgnore(MalformedURLException.class)
    private static URL extractURL(HttpServletRequest request) {
        String urlString;
        String refererHeader = request.getHeader(HTTP_HEADER_REFERER);

        //default endpoints
        String docPath = "/openapi";
        String uiPath = docPath+"/ui";

        BundleContext bundleContext = (BundleContext) request.getServletContext().getAttribute("osgi-bundlecontext");
        //ensure we have a context as change this will be null if bundle is STOPPED
        if(bundleContext != null) {
            ServiceTracker<OpenAPIEndpointProvider,OpenAPIEndpointProvider> openAPIEndpointTracker = new ServiceTracker<>(bundleContext, OpenAPIEndpointProvider.class, null);
            openAPIEndpointTracker.open();
            docPath = openAPIEndpointTracker.getService().getOpenAPIDocUrl();
            uiPath = openAPIEndpointTracker.getService().getOpenAPIUIUrl();
            openAPIEndpointTracker.close();
        }

        if (refererHeader != null) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Using referer header to generate servers: " + refererHeader);
            }
            refererHeader = refererHeader.endsWith("/") ? refererHeader.substring(0, refererHeader.length() - 1) : refererHeader;
            //original behavior based on default paths
            if(docPath.equals("/openapi") && uiPath.equals("/openapi/ui")) {
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
                } catch (MalformedURLException e){
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Failed to create URL for " + refererHeader + ": " + e.getMessage());
                    }
                    //given url in header is invalid, remove the value
                    refererHeader = null;
                }
            }
            if (refererHeader != null) {
                urlString = refererHeader;
            } else {
                //fall back to using request url
                urlString = request.getRequestURL().toString();
            }
        } else {
            urlString = request.getRequestURL().toString();
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Using request url to generate servers: " + urlString);
            }
        }

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to create URL for " + urlString + ": " + e.getMessage());
            }
        }
        return url;
    }
}
