/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.token;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.api.statistics.OAuthStatisticNames;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.statistics.OAuthStatHelper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;

/**
 * Token cache wrapped with statistics
 * 
 */
public class OAuth20TokenCacheWrapper implements OAuth20TokenCache {

    OAuth20TokenCache _real;
    OAuthStatisticsImpl _stats;

    // used during development phase to look for redundant calls
    static boolean _debugDumpCallStacks = false;

    public OAuth20TokenCacheWrapper(OAuth20TokenCache real,
            OAuthStatisticsImpl stats) {
        _real = real;
        _stats = stats;
    }

    public void init(OAuthComponentConfiguration config) {
        // no stats for this one
        _real.init(config);
    }

    public void add(String lookupKey, OAuth20Token entry, int lifetime) {

        if (_debugDumpCallStacks) {
            System.out.println(this.getClass().getName() + ".add CALLED FROM: "
                    + OAuth20Util.getCurrentStackTraceString(new Exception()));
        }

        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_TOKENCACHE_ADD);
        _real.add(lookupKey, entry, lifetime);
        _stats.addMeasurement(statHelper);
    }

    public OAuth20Token get(String lookupKey) {
        if (_debugDumpCallStacks) {
            System.out.println(this.getClass().getName() + ".get CALLED FROM: "
                    + OAuth20Util.getCurrentStackTraceString(new Exception()));
        }

        OAuth20Token result = null;
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_TOKENCACHE_GET);
        result = _real.get(lookupKey);
        _stats.addMeasurement(statHelper);
        return result;
    }

    public void remove(String lookupKey) {

        if (_debugDumpCallStacks) {
            System.out.println(this.getClass().getName() + ".remove CALLED FROM: "
                    + OAuth20Util.getCurrentStackTraceString(new Exception()));
        }

        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_TOKENCACHE_REMOVE);
        _real.remove(lookupKey);
        _stats.addMeasurement(statHelper);
    }
}
