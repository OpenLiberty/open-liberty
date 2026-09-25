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
package com.ibm.ws.security.token.ltpa.internal;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.ws.common.encoder.Base64Coder;

/**
 * Utility class for splitting large LTPA tokens across multiple HTTP cookies.
 *
 * This is necessary because LTPA3 tokens with Post-Quantum Cryptography (PQC)
 * can exceed the standard HTTP cookie size limit of 4KB. This class handles:
 * - Splitting tokens into multiple cookie-sized fragments
 * - Reassembling tokens from cookie fragments
 * - Maintaining fragment metadata and ordering
 *
 * Cookie Naming Convention:
 * - Main cookie: LtpaToken3 (contains metadata + first fragment)
 * - Fragment cookies: LtpaToken3_1, LtpaToken3_2, etc.
 *
 * Fragment Format:
 * Main Cookie: [Version:1][FragmentCount:1][TotalSize:4][Fragment0Data]
 * Fragment Cookies: [FragmentData]
 */
public class LTPACookieSplitter {

    private static final TraceComponent tc = Tr.register(LTPACookieSplitter.class);

    /** Maximum size per cookie fragment (3KB, conservative to account for overhead) */
    private static final int MAX_FRAGMENT_SIZE = 3000;

    /** Size of metadata in main cookie: version(1) + count(1) + size(4) */
    private static final int METADATA_SIZE = 6;

    /** Current version of the fragment format */
    private static final byte VERSION = 1;

    /** Cookie name prefix for LTPA3 tokens */
    private static final String COOKIE_PREFIX = "LtpaToken2";

    /**
     * Check if a token requires splitting into multiple cookies.
     *
     * @param tokenBytes The raw token bytes
     * @return true if the token needs to be split
     */
    public static boolean requiresSplitting(byte[] tokenBytes) {
        if (tokenBytes == null) {
            return false;
        }

        // Calculate Base64-encoded size (4/3 ratio)
        int base64Size = ((tokenBytes.length + 2) / 3) * 4;

        // Add metadata overhead for main cookie
        int totalSize = base64Size + METADATA_SIZE;

        boolean needsSplit = totalSize > MAX_FRAGMENT_SIZE;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Token size check: raw=" + tokenBytes.length +
                         ", base64=" + base64Size +
                         ", total=" + totalSize +
                         ", needsSplit=" + needsSplit);
        }

        return needsSplit;
    }

    /**
     * Split a token into multiple cookie-sized fragments.
     *
     * @param tokenBytes The raw token bytes to split
     * @param cookieName The base cookie name (e.g., "LtpaToken3")
     * @return Map of cookie names to Base64-encoded fragment data
     * @throws IllegalArgumentException if tokenBytes is null or empty
     */
    public static Map<String, String> splitToken(byte[] tokenBytes, String cookieName) {
        if (tokenBytes == null || tokenBytes.length == 0) {
            throw new IllegalArgumentException("Token bytes cannot be null or empty");
        }

        if (cookieName == null || cookieName.isEmpty()) {
            cookieName = COOKIE_PREFIX;
        }

        Map<String, String> cookies = new HashMap<>();

        // Calculate how many fragments we need
        int availableInMainCookie = MAX_FRAGMENT_SIZE - METADATA_SIZE;
        int remainingBytes = tokenBytes.length - availableInMainCookie;
        int additionalFragments = remainingBytes > 0 ? (remainingBytes + MAX_FRAGMENT_SIZE - 1) / MAX_FRAGMENT_SIZE : 0;
        int totalFragments = 1 + additionalFragments;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Splitting token: size=" + tokenBytes.length +
                         ", fragments=" + totalFragments);
        }

        // Build main cookie with metadata
        ByteBuffer mainCookie = ByteBuffer.allocate(METADATA_SIZE + availableInMainCookie);
        mainCookie.put(VERSION);
        mainCookie.put((byte) totalFragments);
        mainCookie.putInt(tokenBytes.length);

        // Add first fragment to main cookie
        int firstFragmentSize = Math.min(availableInMainCookie, tokenBytes.length);
        mainCookie.put(tokenBytes, 0, firstFragmentSize);

        // Base64 encode and store main cookie
        byte[] mainCookieBytes = new byte[mainCookie.position()];
        mainCookie.flip();
        mainCookie.get(mainCookieBytes);
        cookies.put(cookieName, Base64Coder.base64EncodeToString(mainCookieBytes));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Main cookie: " + cookieName + ", size=" + mainCookieBytes.length);
        }

        // Create additional fragment cookies
        int offset = firstFragmentSize;
        for (int i = 1; i < totalFragments; i++) {
            int fragmentSize = Math.min(MAX_FRAGMENT_SIZE, tokenBytes.length - offset);
            byte[] fragmentBytes = new byte[fragmentSize];
            System.arraycopy(tokenBytes, offset, fragmentBytes, 0, fragmentSize);

            String fragmentName = cookieName + "_" + i;
            cookies.put(fragmentName, Base64Coder.base64EncodeToString(fragmentBytes));

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Fragment cookie: " + fragmentName + ", size=" + fragmentSize);
            }

            offset += fragmentSize;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Token split into " + totalFragments + " cookies");
        }

        return cookies;
    }

    /**
     * Reassemble a token from cookie fragments.
     *
     * @param cookies    Map of cookie names to Base64-encoded values
     * @param cookieName The base cookie name (e.g., "LtpaToken3")
     * @return The reassembled raw token bytes
     * @throws InvalidTokenException if fragments are missing, invalid, or corrupted
     */
    public static byte[] reassembleToken(Map<String, String> cookies, String cookieName) throws InvalidTokenException {

        if (cookies == null || cookies.isEmpty()) {
            throw new InvalidTokenException("No cookies provided");
        }

        if (cookieName == null || cookieName.isEmpty()) {
            cookieName = COOKIE_PREFIX;
        }

        // Get and decode main cookie
        String mainCookieValue = cookies.get(cookieName);
        if (mainCookieValue == null) {
            throw new InvalidTokenException("Main cookie not found: " + cookieName);
        }

        byte[] mainCookieBytes;
        try {
            mainCookieBytes = Base64Coder.base64DecodeString(mainCookieValue);
        } catch (Exception e) {
            throw new InvalidTokenException("Failed to decode main cookie: " + e.getMessage());
        }

        if (mainCookieBytes.length < METADATA_SIZE) {
            throw new InvalidTokenException("Main cookie too small: " + mainCookieBytes.length);
        }

        // Parse metadata
        ByteBuffer metadata = ByteBuffer.wrap(mainCookieBytes);
        byte version = metadata.get();
        byte fragmentCount = metadata.get();
        int totalSize = metadata.getInt();

        if (version != VERSION) {
            throw new InvalidTokenException("Unsupported fragment version: " + version);
        }

        if (fragmentCount < 1 || fragmentCount > 255) {
            throw new InvalidTokenException("Invalid fragment count: " + fragmentCount);
        }

        if (totalSize <= 0 || totalSize > 10 * 1024 * 1024) { // 10MB sanity check
            throw new InvalidTokenException("Invalid total size: " + totalSize);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Reassembling token: fragments=" + fragmentCount +
                         ", totalSize=" + totalSize);
        }

        // Allocate buffer for reassembled token
        ByteBuffer tokenBuffer = ByteBuffer.allocate(totalSize);

        // Copy first fragment from main cookie
        int firstFragmentSize = mainCookieBytes.length - METADATA_SIZE;
        tokenBuffer.put(mainCookieBytes, METADATA_SIZE, firstFragmentSize);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Main fragment size: " + firstFragmentSize);
        }

        // Collect and sort additional fragments
        if (fragmentCount > 1) {
            Map<Integer, byte[]> fragments = new TreeMap<>();

            for (int i = 1; i < fragmentCount; i++) {
                String fragmentName = cookieName + "_" + i;
                String fragmentValue = cookies.get(fragmentName);

                if (fragmentValue == null) {
                    throw new InvalidTokenException("Missing fragment: " + fragmentName);
                }

                try {
                    byte[] fragmentBytes = Base64Coder.base64DecodeString(fragmentValue);
                    fragments.put(i, fragmentBytes);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Fragment " + i + " size: " + fragmentBytes.length);
                    }
                } catch (Exception e) {
                    throw new InvalidTokenException("Failed to decode fragment " + i + ": " +
                                                    e.getMessage());
                }
            }

            // Append fragments in order
            for (Map.Entry<Integer, byte[]> entry : fragments.entrySet()) {
                tokenBuffer.put(entry.getValue());
            }
        }

        // Verify we got the expected amount of data
        if (tokenBuffer.position() != totalSize) {
            throw new InvalidTokenException("Size mismatch: expected=" + totalSize +
                                            ", actual=" + tokenBuffer.position());
        }

        byte[] tokenBytes = tokenBuffer.array();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Token reassembled from " + fragmentCount + " cookies, size=" +
                         tokenBytes.length);
        }

        return tokenBytes;
    }

    /**
     * Check if a set of cookies contains a fragmented token.
     *
     * @param cookies    Map of cookie names to values
     * @param cookieName The base cookie name
     * @return true if the cookies contain fragment cookies
     */
    public static boolean isFragmented(Map<String, String> cookies, String cookieName) {
        if (cookies == null || cookieName == null) {
            return false;
        }

        // Check for fragment cookies (e.g., LtpaToken3_1)
        String fragmentPattern = cookieName + "_";
        for (String name : cookies.keySet()) {
            if (name.startsWith(fragmentPattern)) {
                return true;
            }
        }

        return false;
    }
}

// Made with Bob
