/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilities to protect against DNS rebinding attacks
 */
public class LocalhostHeaderChecks {

    private static final Pattern LOCAL_ADDR = Pattern.compile("localhost|127\\.\\d+\\.\\d+\\.\\d+|\\[::1\\]|::1");
    private static final boolean DISABLE_REBINDING_PROTECTION = Boolean.getBoolean("mcp.disable.rebinding.protection");

    /**
     * Validate the host and origin headers if the request comes from a loopback address to protect against DNS rebinding.
     *
     * @param req the request
     * @return {@code true} if the origin and host headers are acceptable for the request
     */
    public static boolean validateLocalhostHeaders(HttpServletRequest req) {
        if (req.isSecure()) {
            // Do not need this check for secure connections since TLS verifies the hostname
            return true;
        }

        if (DISABLE_REBINDING_PROTECTION) {
            // Provide an escape hatch in case this check causes unforeseen problems
            return true;
        }

        if (!isLocalAddr(req.getRemoteAddr())) {
            // Only do this check for requests from localhost where we know what the requested hostname _should_ be
            return true;
        }

        return isOriginValidForLocalhost(req.getHeader("Origin"));
    }

    /**
     * Checks whether the Origin header is valid for a request from the local host
     *
     * @param originHeader the Origin header, or {@code null} if there was no Origin header set
     * @return {@code true} if {@code originHeader} is valid
     */
    @FFDCIgnore(URISyntaxException.class)
    public static boolean isOriginValidForLocalhost(String originHeader) {
        if (originHeader == null) {
            return true;
        }
        try {
            URI originUri = new URI(originHeader);
            String originHost = originUri.getHost();
            if (originHost == null) {
                // No host in origin header
                return false;
            }

            return isLocalAddr(originHost);
        } catch (URISyntaxException e) {
            // origin header not a valid URI
            return false;
        }
    }

    /**
     * Checks whether an IP address or hostname represents localhost
     *
     * @param address the address or hostname
     * @return {@code true} if {@code address} is "localhost" or a loopback IP address
     */
    public static boolean isLocalAddr(String address) {
        return LOCAL_ADDR.matcher(address).matches();
    }

}
