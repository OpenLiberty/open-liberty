/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.outbound;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.MSP;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;

/**
 *
 */
public class HeaderHandler {

    private static final TraceComponent tc = Tr.register(HeaderHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    HttpChannelConfig config;
    HttpResponse response;
    HttpHeaders headers;

    private static final String NOCACHE_VALUE = "no-cache=\"set-cookie, set-cookie2\"";
    private static final String LONG_AGO = "Thu, 01 Dec 1994 16:00:00 GMT";

    public HeaderHandler(HttpChannelConfig config, HttpResponse response) {
        Objects.requireNonNull(config);
        this.config = config;

        Objects.requireNonNull(response);
        this.response = response;
        this.headers = response.headers();

    }

    public void complianceCheck() {
        String method = "headerComplianceCheck";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, method);
        }

        if (!headers.contains(HttpHeaderKeys.HDR_DATE.getName())) {

            headers.set(HttpHeaderKeys.HDR_DATE.getName(),
                        new String(HttpDispatcher.getDateFormatter().getRFC1123TimeAsBytes(config.getDateHeaderRange()), StandardCharsets.UTF_8));
        }

        if (config.removeServerHeader()) {
            if (headers.contains(HttpHeaderKeys.HDR_SERVER.getName())) {
                Tr.debug(tc, "Configuration forcing removal of Server header");
                headers.remove(HttpHeaderKeys.HDR_SERVER.getName());
            }
        } else if (!headers.contains(HttpHeaderKeys.HDR_SERVER.getName())) {
            byte[] serverHeader = config.getServerHeaderValue();
            if (Objects.nonNull(serverHeader)) {
                headers.set(HttpHeaderKeys.HDR_SERVER.getName(), new String(serverHeader, StandardCharsets.UTF_8));
                Tr.debug(tc, "Adding the Server header value: " + headers.get(HttpHeaderKeys.HDR_SERVER.getName()));
            }

        }

        if (config.shouldCookiesConfigureNoCache()) {
            updateCacheControl();
        }

        if (config.useHeadersConfiguration()) {
            //Add all headers configured through the ADD configuration option
            for (List<Map.Entry<String, String>> headersToAdd : config.getConfiguredHeadersToAdd().values()) {
                for (Entry<String, String> header : headersToAdd) {
                    MSP.log("Custom header to add: " + header.getKey() + ": " + header.getValue());
                    headers.add(header.getKey(), header.getValue());
                }
            }

            //Set all headers configured through the SET configuration option
            for (Entry<String, String> header : config.getConfiguredHeadersToSet().values()) {
                headers.set(header.getKey(), header.getValue());
            }

            //Set all headers configured through the SET_IF_MISSING configuration option
            for (Entry<String, String> header : config.getConfiguredHeadersToSetIfMissing().values()) {
                //Only set if not present
                if (!headers.contains(header.getKey())) {
                    headers.set(header.getKey(), header.getValue());
                }
            }

            //Remove all headers configured through the REMOVE configuration option
            for (String headerName : config.getConfiguredHeadersToRemove().values()) {
                if (headers.contains(headerName)) {
                    headers.remove(headerName);
                }
            }

        }

//        if (HttpUtil.isKeepAlive(response) && config.isKeepAliveEnabled()) {
//            headers.remove(HttpHeaderKeys.HDR_CONNECTION.getName());
//        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, method);
        }
    }

    /**
     * Update the caching related headers for a response. This will configure the
     * response headers such that if Set-Cookie information is present, then additional
     * headers will be added to ensure that the message is not cached on any intermediate
     * caches.
     */
    private void updateCacheControl() {
        boolean hasCookiev1 = headers.contains(HttpHeaderKeys.HDR_SET_COOKIE.getName());
        boolean hasCookiev2 = headers.contains(HttpHeaderKeys.HDR_SET_COOKIE2.getName());

        if (!hasCookiev1 && !hasCookiev2) {
            return;
        }

        if (headers.contains(HttpHeaderKeys.HDR_EXPIRES.toString())) {
            headers.set(HttpHeaderKeys.HDR_EXPIRES.getName(), LONG_AGO);
        }

        if (headers.contains(HttpHeaderKeys.HDR_CACHE_CONTROL.getName())) {
            StringBuilder builder = new StringBuilder();
            String header;
            if (hasCookiev1) {
                builder.append("set-cookie");
            }
            if (hasCookiev1 && hasCookiev2) {
                builder.append(", ");
            }
            if (hasCookiev2) {
                builder.append("set-cookie2");
            }
            header = builder.toString();

            if (!header.isEmpty()) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {

                    Tr.event(tc, "Updating Cache-Control for Set-Cookie");
                }
                headers.set(HttpHeaderKeys.HDR_CACHE_CONTROL.getName(), header);
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Adding Cache-Control due to Set-Cookie");
            }
            headers.set(HttpHeaderKeys.HDR_CACHE_CONTROL.getName(), NOCACHE_VALUE);
        }

    }

}
