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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;

/**
 *
 */
public class NettyHeaderUtils {

    private static final TraceComponent tc = Tr.register(NettyHeaderUtils.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /**
     * Returns the value of the last occurrence of the given header name.
     * If no headers are found, null is returned.
     *
     * @param headers
     * @param name
     * @return
     */
    public static String getLast(HttpHeaders headers, String name) {
        String value = null;

        List<String> values = headers.getAll(name);
        if (Objects.nonNull(values) && !values.isEmpty()) {
            value = values.get(values.size() - 1);
        }

        return value;
    }

    /**
     * Utility method that sets a Vary header with the given value. If a Vary header
     * already exists, this method will append it to the value using a comma ',' as
     * the delimiter.
     *
     * @param headers
     * @param value
     * @return
     */
    public static void setVary(HttpHeaders headers, String value) {

        Objects.nonNull(headers);
        Objects.nonNull(value);

        String headerValue;

        if (headers.contains(HttpHeaderKeys.HDR_VARY.getName())) {
            headerValue = headers.get(HttpHeaderKeys.HDR_VARY.getName()).toLowerCase();

            if (!headerValue.isEmpty() && !headerValue.contains(value.toLowerCase())) {
                headerValue = new StringBuilder().append(headerValue).append(", ").append(value).toString();
                headers.set(HttpHeaderKeys.HDR_VARY.getName(), headerValue);

            }
        } else {
            headers.set(HttpHeaderKeys.HDR_VARY.getName(), value);
        }
    }

    /**
     * Strip whitespace from a given String.
     *
     * @param string The String to strip whitespace from.
     * @return an empty String if the given String is null, otherwise a String with all whitespace removed.
     */
    public static String stripWhiteSpace(String string) {
        return Objects.isNull(string) ? HttpConstants.EMPTY_STRING : string.replaceAll("\\s+", "");
    }

    /**
     * Removes any WAS private ($WS*) headers from the Netty request header map
     * that the peer on the other end of the connection is not trusted to send
     * For $WSSP, the header value affects the decision - values "80" and "443"
     * are permitted from any source; the desensitizePrivatePortHeader
     * flag changes the trust list consulted.
     *
     *
     * @param remoteAddr the resolved remote InetAddress, or null
     * @param headers the mutable Netty HttpHeaders} map
     * @param desensitizePrivatePortHeader when true $WSSP is evaluated against the non-sensitive trust list
     */
    public static void filterPrivateHeaders(InetAddress remoteAddr, HttpHeaders headers,
                                            boolean desensitizePrivatePortHeader) {
        List<String> untrusted = null;

        for (String name : headers.names()) {
            // Fast-path: skip any header whose name does not start with '$' to linit the check only to private headers
            if (name.charAt(0) != '$') {
                continue;
            }
            if (!HttpHeaderKeys.isWasPrivateHeader(name)) {
                continue;
            }

            boolean trusted;
            if (HttpHeaderKeys.HDR_$WSSP.getName().equalsIgnoreCase(name)) {
                // $WSSP: value affects the decision — check every occurrence.
                // One untrusted value taints the whole header.
                trusted = true;
                for (String value : headers.getAll(name)) {
                    if (!HttpDispatcher.isPrivateHeaderTrusted(remoteAddr, name, value, desensitizePrivatePortHeader)) {
                        trusted = false;
                        break;
                    }
                }
            } else {
                // All other $WS* headers: decision is source-only, value is irrelevant.
                trusted = HttpDispatcher.isPrivateHeaderTrusted(remoteAddr, name, null, false);
            }

            if (!trusted) {
                if (untrusted == null) {
                    untrusted = new ArrayList<>();
                }
                untrusted.add(name);
            }
        }

        if (untrusted != null) {
            for (String name : untrusted) {
                headers.remove(name);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "filterPrivateHeaders: removed untrusted private header " + name + (remoteAddr != null ? " sent by " + remoteAddr.getHostAddress() : " for this host"));
                }
            }
        }
    }

}
