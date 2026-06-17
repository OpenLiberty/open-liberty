/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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

    // Only visible so unit tests can access the local cookie cache.
    static final LocalLoggedOutJwtSsoCookieCache localCookieCache = new LocalLoggedOutJwtSsoCookieCache();

    public static boolean contains(String tokenString) {
        LoggedOutCookieCache service = LoggedOutCookieCacheHelper.getLoggedOutCookieCacheService();
        String digest = LoggedOutCookieCacheHelper.generateTokenHashKey(tokenString);
        if (service == null) {
            return localCookieCache.contains(digest);
        } else {
            return service.contains(digest);
        }
    }

    public static void put(String tokenString) {
        LoggedOutCookieCache service = LoggedOutCookieCacheHelper.getLoggedOutCookieCacheService();
        String digest = LoggedOutCookieCacheHelper.generateTokenHashKey(tokenString);
        if (service == null) {
            localCookieCache.put(digest);
        } else {
            service.put(digest, Boolean.TRUE);
        }
    }
}
