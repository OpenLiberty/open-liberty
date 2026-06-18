/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.security;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.internal.StringUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    private static final Object SYNC_OBJECT = new Object();
    private static MessageDigest CLONEABLE_MESSAGE_DIGEST = null;

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
        if (tc.isEntryEnabled()) Tr.entry(tc, "generateTokenHashKey()", tokenString);

        if (tokenString == null || tokenString.isEmpty()) {
            if (tc.isEntryEnabled()) Tr.exit(tc, "generateTokenHashKey()", "null or empty token");
            return null;
        }

        MessageDigest md = getCloneableMessageDigest();
        if (md == null) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "MessageDigest unavailable; token hash cannot be generated. Logout tracking is disabled.");
            if (tc.isEntryEnabled()) Tr.exit(tc, "generateTokenHashKey()", null);
            return null;
        }
        
        md.update(tokenString.getBytes(StandardCharsets.UTF_8));
        String hashKey = LOGOUT_KEY_PREFIX + Base64Coder.base64EncodeToString(md.digest());
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "generateTokenHashKey()", hashKey);
        return hashKey;
    }

    /**
     * Get a MessageDigest instance using clone() for better performance.
     * Uses SHA-512 algorithm. Approximately 50% faster than creating new instances.
     *
     * @return A MessageDigest instance, or null if unavailable
     */
    @Trivial
    @FFDCIgnore({ CloneNotSupportedException.class, NoSuchAlgorithmException.class })
    private static MessageDigest getCloneableMessageDigest() {
        /*
         * If we've never been asked for a MessageDigest, create the parent of
         * our clones.
         */
        if (CLONEABLE_MESSAGE_DIGEST == null) {
            synchronized (SYNC_OBJECT) {
                if (CLONEABLE_MESSAGE_DIGEST == null) {
                    try {
                        CLONEABLE_MESSAGE_DIGEST = MessageDigest.getInstance(SHA_512);
                    } catch (NoSuchAlgorithmException nsae) {
                        // Not possible. SHA-512 is required by all JREs.
                    }
                }
            }
        }

        /*
         * Try to clone the parent. If we can't, then we'll ignore the FFDC and create a
         * new instance. If the clone fails, which is REALLY unlikely, as we
         * know the SHA MessageDigest is cloneable on IBM and Sun JDKs
         */
        try {
            return (MessageDigest) CLONEABLE_MESSAGE_DIGEST.clone();
        } catch (CloneNotSupportedException cnse) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "CloneNotSupportedException caught while trying to clone MessageDigest with algorithm " + SHA_512
                             + ". This is pretty unlikely, and we need to get details about the JDK which is in use.",
                         cnse);
            }
            try {
                return MessageDigest.getInstance(SHA_512);
            } catch (NoSuchAlgorithmException nsae) {
                // Not possible. SHA-512 is required by all JREs.
                return null;
            }
        }
    }
}
