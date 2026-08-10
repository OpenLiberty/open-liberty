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
package com.ibm.ws.ui.internal.v1.utils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Validates URLs to prevent SSRF (Server-Side Request Forgery) attacks.
 * Implements allowlisting for schemes and hosts, and blocks access to
 * private/internal network ranges.
 */
public class URLValidator {
    private static final TraceComponent tc = Tr.register(URLValidator.class);
    
    // Allowed URL schemes - only HTTP and HTTPS
    private static final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("http", "https"));
    
    // Private IP ranges (RFC 1918, RFC 4193, loopback, link-local)
    private static final Pattern PRIVATE_IPV4_PATTERN = Pattern.compile(
        "^(10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|127\\.|169\\.254\\.|0\\.)"
    );
    
    // IPv6 private ranges
    private static final Pattern PRIVATE_IPV6_PATTERN = Pattern.compile(
        "^(::1|[fF][cCdD]|[fF][eE]80:)"
    );
    
    // Localhost variations
    private static final Set<String> LOCALHOST_NAMES = new HashSet<>(Arrays.asList(
        "localhost", "localhost.localdomain", "127.0.0.1", "::1", "0.0.0.0"
    ));
    
    // Maximum URL length to prevent DoS
    private static final int MAX_URL_LENGTH = 2048;
    
    /**
     * Validates a URL for security concerns including:
     * - Scheme allowlisting (only http/https)
     * - Blocking private/internal IP ranges
     * - Blocking localhost access
     * - URL length limits
     * 
     * @param url The URL to validate
     * @throws SecurityException if the URL fails validation
     */
    public static void validateURL(URL url) throws SecurityException {
        if (url == null) {
            throw new SecurityException("URL cannot be null");
        }
        
        String urlString = url.toString();
        
        // Check URL length
        if (urlString.length() > MAX_URL_LENGTH) {
            String msg = "URL exceeds maximum allowed length of " + MAX_URL_LENGTH + " characters";
            Tr.warning(tc, "CWWKX1050W: " + msg);
            throw new SecurityException(msg);
        }
        
        // Validate scheme
        String scheme = url.getProtocol();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            String msg = "URL scheme '" + scheme + "' is not allowed. Only HTTP and HTTPS are permitted.";
            Tr.warning(tc, "CWWKX1051W: " + msg);
            throw new SecurityException(msg);
        }
        
        // Validate host
        String host = url.getHost();
        if (host == null || host.trim().isEmpty()) {
            String msg = "URL must contain a valid host";
            Tr.warning(tc, "CWWKX1052W: " + msg);
            throw new SecurityException(msg);
        }
        
        // Check for localhost
        if (isLocalhost(host)) {
            String msg = "Access to localhost is not permitted for security reasons";
            Tr.warning(tc, "CWWKX1053W: " + msg);
            throw new SecurityException(msg);
        }
        
        // Check for private IP ranges
        if (isPrivateOrInternalIP(host)) {
            String msg = "Access to private/internal IP addresses is not permitted for security reasons";
            Tr.warning(tc, "CWWKX1054W: " + msg);
            throw new SecurityException(msg);
        }
        
        // Additional DNS resolution check to prevent DNS rebinding attacks
        validateResolvedAddress(host);
    }
    
    /**
     * Checks if the host is localhost or a localhost variant
     */
    private static boolean isLocalhost(String host) {
        String lowerHost = host.toLowerCase();
        return LOCALHOST_NAMES.contains(lowerHost);
    }
    
    /**
     * Checks if the host is a private or internal IP address
     */
    @FFDCIgnore(UnknownHostException.class)
    private static boolean isPrivateOrInternalIP(String host) {
        // Check if it matches private IP patterns
        if (PRIVATE_IPV4_PATTERN.matcher(host).find()) {
            return true;
        }
        
        if (PRIVATE_IPV6_PATTERN.matcher(host).find()) {
            return true;
        }
        
        // Try to resolve and check the IP address
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.isLoopbackAddress() || 
                   addr.isLinkLocalAddress() || 
                   addr.isSiteLocalAddress() ||
                   addr.isAnyLocalAddress();
        } catch (UnknownHostException e) {
            // If we can't resolve it, allow it to proceed
            // The connection attempt will fail naturally
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not resolve host: " + host, e);
            }
            return false;
        }
    }
    
    /**
     * Validates the resolved IP address to prevent DNS rebinding attacks
     */
    @FFDCIgnore(UnknownHostException.class)
    private static void validateResolvedAddress(String host) throws SecurityException {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                String ipAddress = addr.getHostAddress();
                
                // Check resolved IP against private ranges
                if (PRIVATE_IPV4_PATTERN.matcher(ipAddress).find() ||
                    PRIVATE_IPV6_PATTERN.matcher(ipAddress).find() ||
                    addr.isLoopbackAddress() ||
                    addr.isLinkLocalAddress() ||
                    addr.isSiteLocalAddress() ||
                    addr.isAnyLocalAddress()) {
                    
                    String msg = "Host '" + host + "' resolves to a private/internal IP address: " + ipAddress;
                    Tr.warning(tc, "CWWKX1055W: " + msg);
                    throw new SecurityException(msg);
                }
            }
        } catch (UnknownHostException e) {
            // If we can't resolve, log but don't block
            // The connection will fail naturally
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not resolve host for validation: " + host, e);
            }
        }
    }
    
    /**
     * Validates a URL string and returns a validated URL object
     * 
     * @param urlString The URL string to validate
     * @return A validated URL object
     * @throws MalformedURLException if the URL is malformed
     * @throws SecurityException if the URL fails security validation
     */
    public static URL validateAndCreateURL(String urlString) throws MalformedURLException, SecurityException {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new SecurityException("URL string cannot be null or empty");
        }
        
        URL url = new URL(urlString);
        validateURL(url);
        return url;
    }
}

// Made with Bob
