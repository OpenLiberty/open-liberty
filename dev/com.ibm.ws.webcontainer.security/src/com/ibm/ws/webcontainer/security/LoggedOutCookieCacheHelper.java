/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.security.NoSuchAlgorithmException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.crypto.MessageDigestUtils;

/**
 * Utility 'helper' class to get the singleton {@link LoggedOutCookieCache}
 * instance.
 */
public class LoggedOutCookieCacheHelper {

    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(LoggedOutCookieCacheHelper.class, "LoggedOutCookieCache");

    private static LoggedOutCookieCache cookieCacheService = null;

    public static final String LOGOUT_KEY_PREFIX = "LOGOUT:";
    private static final String SHA_512 = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_512;

    /**
     * Get the singleton {@link LoggedOutCookieCache} instance.
     *
     * @return The {@link LoggedOutCookieCache} instance, or null if one was not
     *         set.
     */
    public static LoggedOutCookieCache getLoggedOutCookieCacheService() {
        return cookieCacheService;
    }

    /**
     * Set the singleton {@link LoggedOutCookieCache} instance.
     *
     * @param The {@link LoggedOutCookieCache} instance, or null to unset.
     */
    public static void setLoggedOutCookieCacheService(LoggedOutCookieCache service) {
        cookieCacheService = service;
    }

    /**
     * Generate a hash key from the token string for cache storage using SHA-512.
     * Uses a cloneable MessageDigest for better performance (approximately 50% faster).
     * This method is used for both LTPA and JWT SSO tokens.
     *
     * @param tokenString The token string (LTPA or JWT SSO)
     * @return Hash string with LOGOUT: prefix, or null if error
     */
    public static String generateTokenHashKey(String tokenString) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "generateTokenHashKey()", tokenString);

        if (tokenString == null || tokenString.isEmpty()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "generateTokenHashKey()", "null or empty token");
            return null;
        }

        String hashedToken = null;
        try {
            hashedToken = MessageDigestUtils.getHashedValue(tokenString, SHA_512);
        } catch (NoSuchAlgorithmException nsae) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "MessageDigest unavailable; token hash cannot be generated. Logout tracking is disabled.");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "generateTokenHashKey()", null);
            return null;
        }

        String hashKey = LOGOUT_KEY_PREFIX + hashedToken;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "generateTokenHashKey()", hashKey);
        return hashKey;
    }
}
