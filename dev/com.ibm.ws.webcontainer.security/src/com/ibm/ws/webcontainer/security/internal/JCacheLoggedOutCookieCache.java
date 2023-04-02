/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.LoggedOutCookieCache;
import com.ibm.ws.webcontainer.security.TraceConstants;

import io.openliberty.jcache.CacheService;

/**
 * A JCache backed {@link LoggedOutCookieCache} implementation.
 */
public class JCacheLoggedOutCookieCache implements LoggedOutCookieCache {

    private static final TraceComponent tc = Tr.register(JCacheLoggedOutCookieCache.class, "LoggedOutCookieCache", TraceConstants.MESSAGE_BUNDLE);
    private final CacheService cacheService;

    public JCacheLoggedOutCookieCache(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public boolean contains(String key) {
        /*
         * Default the response to be 'false' in the case there is an issue
         * communicating with the JCache cache. If we defaulted to 'true', it is
         * possible an network outage, or the similar, could result in all cookies being
         * determined to be "logged out".
         */
        boolean contains = false;

        try {
            contains = cacheService.getCache().containsKey(key);

            if (tc.isDebugEnabled()) {
                if (contains) {
                    Tr.debug(tc, "JCache HIT for key " + key);
                } else {
                    Tr.debug(tc, "JCache MISS for key " + key);
                }
            }
        } catch (Exception e) {
            /*
             * Don't let a JCache failure propagate up the call stack. Log it and move on.
             */
            if (tc.isErrorEnabled()) {
                Tr.error(tc, "JCACHE_CONTAINSKEY_FAILURE", e);
            }
        }
        return contains;
    }

    @Override
    public void put(String key, Object value) {
        try {
            cacheService.getCache().put(key, value);
        } catch (Exception e) {
            /*
             * Don't let a JCache failure propagate up the call stack. Log it and move on.
             */
            if (tc.isErrorEnabled()) {
                Tr.error(tc, "JCACHE_PUT_FAILURE", e);
            }
        }
    }
}
