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
package com.ibm.ws.webcontainer.security.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.LoggedOutCookieCache;
import com.ibm.ws.webcontainer.security.LoggedOutCookieCacheHelper;

/**
 * A class to "remember" which jwtsso cookies have been logged out.
 * Allows for defense against cookie hijacking attacks,
 * reuse of a previously logged out cookie value can be detected and authentication refused.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class LoggedOutJwtSsoCookieCache {
    private static final TraceComponent tc = Tr.register(LoggedOutJwtSsoCookieCache.class);

    private static MessageDigest CLONEABLE_MESSAGE_DIGEST = null;
    private static final String SHA_512 = "SHA-512";
    private static final Object SYNC_OBJECT = new Object();

    // Only visible so unit tests can access the local cookie cache.
    static final LocalLoggedOutJwtSsoCookieCache localCookieCache = new LocalLoggedOutJwtSsoCookieCache();

    // store as digests to save space
    public static String toDigest(String input) {
        MessageDigest md = getMessageDigest();
        md.update(input.getBytes(StandardCharsets.UTF_8));
        String result = com.ibm.ws.common.encoder.Base64Coder.base64EncodeToString(md.digest());
        return result;
    }

    public static boolean contains(String tokenString) {
        LoggedOutCookieCache service = LoggedOutCookieCacheHelper.getLoggedOutCookieCacheService();
        String digest = toDigest(tokenString);
        if (service == null) {
            return localCookieCache.contains(digest);
        } else {
            return service.contains(digest);
        }
    }

    public static void put(String tokenString) {
        LoggedOutCookieCache service = LoggedOutCookieCacheHelper.getLoggedOutCookieCacheService();
        String digest = toDigest(tokenString);
        if (service == null) {
            localCookieCache.put(digest);
        } else {
            service.put(digest, Boolean.TRUE);
        }
    }

    /**
     * Use clone() to get a new instance as its approximately 50% faster (as
     * seen in empirical testing), if we can. Worst case scenario is we will
     * create a new one each time.
     *
     * @return
     */
    @Trivial
    @FFDCIgnore({ CloneNotSupportedException.class, NoSuchAlgorithmException.class })
    private static MessageDigest getMessageDigest() {
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
