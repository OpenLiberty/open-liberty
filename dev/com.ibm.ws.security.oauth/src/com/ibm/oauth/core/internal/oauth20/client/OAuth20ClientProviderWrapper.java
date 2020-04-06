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
package com.ibm.oauth.core.internal.oauth20.client;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.oauth.core.api.oauth20.client.OAuth20ClientProvider;
import com.ibm.oauth.core.api.statistics.OAuthStatisticNames;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.statistics.OAuthStatHelper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;

/**
 * Wraps a customers ClientProvider with statistics.
 * 
 */
public class OAuth20ClientProviderWrapper implements OAuth20ClientProvider {

    OAuth20ClientProvider _real;
    OAuthStatisticsImpl _stats;

    // used during development phase to look for redundant calls
    static boolean _debugDumpCallStacks = false;

    public OAuth20ClientProviderWrapper(OAuth20ClientProvider real,
            OAuthStatisticsImpl stats) {
        _real = real;
        _stats = stats;
    }

    public boolean exists(String clientIdentifier) {
        if (_debugDumpCallStacks) {
            System.out.println(this.getClass().getName()
                    + ".exists CALLED FROM: "
                    + OAuth20Util.getCurrentStackTraceString(new Exception()));
        }

        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_CLIENTPROVIDER_EXISTS);

        boolean result = _real.exists(clientIdentifier);

        _stats.addMeasurement(statHelper);
        return result;
    }

    public OAuth20Client get(String clientIdentifier) {
        if (_debugDumpCallStacks) {
            System.out.println(this.getClass().getName() + ".get CALLED FROM: "
                    + OAuth20Util.getCurrentStackTraceString(new Exception()));
        }
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_CLIENTPROVIDER_GETCLIENT);

        OAuth20Client result = _real.get(clientIdentifier);

        _stats.addMeasurement(statHelper);
        return result;
    }

    public boolean validateClient(String clientIdentifier, String clientSecret) {
        if (_debugDumpCallStacks) {
            System.out.println(this.getClass().getName() + ".get CALLED FROM: "
                    + OAuth20Util.getCurrentStackTraceString(new Exception()));
        }
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH20_CLIENTPROVIDER_VALIDATECLIENT);

        boolean result = _real.validateClient(clientIdentifier, clientSecret);

        _stats.addMeasurement(statHelper);
        return result;
    }

    public void init(OAuthComponentConfiguration config) {
        // no stats for this one
        _real.init(config);
    }

}
