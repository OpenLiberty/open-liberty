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
package com.ibm.oauth.core.internal.config;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.statistics.OAuthStatisticNames;
import com.ibm.oauth.core.internal.statistics.OAuthStatHelper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;

/**
 * Wraps a customer's configuration provider with statistics measurements.
 */
public class OAuthComponentConfigurationWrapper implements
        OAuthComponentConfiguration {

    OAuthComponentConfiguration _real;
    OAuthStatisticsImpl _stats;

    public OAuthComponentConfigurationWrapper(OAuthComponentConfiguration real,
            OAuthStatisticsImpl stats) {
        _real = real;
        _stats = stats;
    }

    public String getUniqueId() {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH_CONFIG_GETUNIQUEID);
        String result = _real.getUniqueId();
        _stats.addMeasurement(statHelper);
        return result;
    }

    public ClassLoader getPluginClassLoader() {
        // no stats for this one
        return _real.getPluginClassLoader();
    }

    public boolean getConfigPropertyBooleanValue(String name) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH_CONFIG_GETPROPERTY);
        boolean result = _real.getConfigPropertyBooleanValue(name);
        _stats.addMeasurement(statHelper);
        return result;
    }

    public int getConfigPropertyIntValue(String name) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH_CONFIG_GETPROPERTY);
        int result = _real.getConfigPropertyIntValue(name);
        _stats.addMeasurement(statHelper);
        return result;
    }

    public String getConfigPropertyValue(String name) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH_CONFIG_GETPROPERTY);
        String result = _real.getConfigPropertyValue(name);
        _stats.addMeasurement(statHelper);
        return result;
    }

    public String[] getConfigPropertyValues(String name) {
        OAuthStatHelper statHelper = new OAuthStatHelper(
                OAuthStatisticNames.OAUTH_CONFIG_GETPROPERTY);
        String[] result = _real.getConfigPropertyValues(name);
        _stats.addMeasurement(statHelper);
        return result;
    }
}
