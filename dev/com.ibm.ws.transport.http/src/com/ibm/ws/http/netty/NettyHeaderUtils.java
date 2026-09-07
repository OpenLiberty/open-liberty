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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;

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
     * @param headers the mutable Netty HttpHeaders map
     * @param desensitizePrivatePortHeader when true $WSSP is evaluated against the non-sensitive trust list
     * @param trustNonSensitive computed at HttpDispatcher.usePrivateHeaders(remoteAddr)
     * @param trustSensitive    computed at HttpDispatcher.usePrivateSensitiveHeaders(remoteAddr)
     
     */
    public static void filterPrivateHeaders(InetAddress remoteAddr, HttpHeaders headers,
                                            boolean desensitizePrivatePortHeader,
                                            boolean trustNonSensitive, boolean trustSensitive) {
        // Fast-path: source is trusted for everything, so nothing here could be stripped.
        if (trustNonSensitive && trustSensitive) {
            return;
        }

        List<HttpHeaderKeys> toRemove = null;
        boolean wsspNeedsPrune = false;

        Iterator<Map.Entry<CharSequence, CharSequence>> it = headers.iteratorCharSequence();
        while (it.hasNext()) {
            Map.Entry<CharSequence, CharSequence> entry = it.next();
            CharSequence nameCSeq = entry.getKey();

            // Fast-path: every WAS private header begins with '$'.
            if (nameCSeq.length() == 0 || nameCSeq.charAt(0) != '$') {
                continue;
            }

            HttpHeaderKeys key = matchHeaderKey(nameCSeq);
            if (key == null || !HttpHeaderKeys.isWasPrivateHeader(key.getName())) {
                continue;
            }

            if (HttpDispatcher.isPrivateHeaderTrusted(trustNonSensitive, trustSensitive,
                                                      key, entry.getValue(),
                                                      desensitizePrivatePortHeader)) {
                continue;
            }

            if (key == HttpHeaderKeys.HDR_$WSSP) {
                // Per-occurrence, admits or drops each $WSSP instance on its own value,
                wsspNeedsPrune = true;
            } else {
                // All other $WS* headers: the decision is source-only and therefore identical
                // for every occurrence, so the whole header goes.
                if (toRemove == null) {
                    toRemove = new ArrayList<>(2);
                }
                if (!toRemove.contains(key)) {
                    toRemove.add(key);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "filterPrivateHeaders: untrusted WAS private header ["
                        + key.getName() + "] from remote address [" + remoteAddr + "]");
            }
        }

        // Removal is deferred until the entry iteration has finished. Netty's entry iterator
        // holds a raw pointer, so mutating the map mid-walk risks silently skipping later headers.
        if (toRemove != null) {
            for (int i = 0; i < toRemove.size(); i++) {
                // Uses canonical name as Netty hashes header names case-insensitively, so this
                // removes every occurrence whatever casing arrived on the wire.
                headers.remove(toRemove.get(i).getName());
            }
        }

        // $WSSP: drop only the untrusted values, keeping any trusted ones
        // Only reached when at least one occurrence was untrusted.
        if (wsspNeedsPrune) {
            pruneUntrustedWssp(headers, desensitizePrivatePortHeader, trustNonSensitive, trustSensitive, remoteAddr);
        }
    }

    /**
     * Removes the untrusted values of a repeated $WSSP header while leaving trusted
     * ones in place, matching the per-occurrence admission the legacy channel 
     *
     */
    private static void pruneUntrustedWssp(HttpHeaders headers, boolean desensitizePrivatePortHeader,
                                           boolean trustNonSensitive, boolean trustSensitive,
                                           InetAddress remoteAddr) {
        Iterator<? extends CharSequence> values =
                headers.valueCharSequenceIterator(HttpHeaderKeys.HDR_$WSSP.getName());
        while (values.hasNext()) {
            CharSequence value = values.next();
            if (!HttpDispatcher.isPrivateHeaderTrusted(trustNonSensitive, trustSensitive,
                                                       HttpHeaderKeys.HDR_$WSSP, value,
                                                       desensitizePrivatePortHeader)) {
                values.remove();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "filterPrivateHeaders: removed untrusted $WSSP value from ["
                            + remoteAddr + "]");
                }
            }
        }
    }

    /**
     * Resolve a header name to its HttpHeaderKeys singleton without allocating.
     * Netty's H1 codec produces AsciiString names; anything else falls back to the String form.
     */
    private static HttpHeaderKeys matchHeaderKey(CharSequence name) {
        if (name instanceof AsciiString) {
            AsciiString a = (AsciiString) name;
            return HttpHeaderKeys.match(a.array(), a.arrayOffset(), a.length());
        }
        String s = name.toString();
        return HttpHeaderKeys.match(s, 0, s.length());
    }

}
