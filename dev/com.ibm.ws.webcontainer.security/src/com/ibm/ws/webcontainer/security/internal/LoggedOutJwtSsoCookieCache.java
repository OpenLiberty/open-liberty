/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.webcontainer.security.LoggedOutCookieCache;
import com.ibm.ws.webcontainer.security.LoggedOutCookieCacheHelper;

/**
 * A class to "remember" which jwtsso cookies have been logged out.
 * Allows for defense against cookie hijacking attacks,
 * reuse of a previously logged out cookie value can be detected and authentication refused.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class LoggedOutJwtSsoCookieCache {

    private static MessageDigest md = null;

    // Only visible so unit tests can access the local cookie cache.
    static final LocalLoggedOutJwtSsoCookieCache localCookieCache = new LocalLoggedOutJwtSsoCookieCache();

    static {
        try {
            md = MessageDigest.getInstance("SHA-1"); // nice short string
        } catch (Exception e) {
        }
    }

    // store as digests to save space
    public static String toDigest(String input) {
        md.reset();
        try {
            md.update(input.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        String result = com.ibm.ws.common.internal.encoder.Base64Coder.base64EncodeToString(md.digest());
        //String result = Base64.getEncoder().encodeToString(md.digest());  // java8 only
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
}
