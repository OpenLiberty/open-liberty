/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.tx.config;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.tx.util.alarm.AlarmManager;

public class ConfigurationProviderManager {
    private static volatile ConfigurationProvider _provider;

    static {
        start();
    }

    public static void stop(boolean immediate) {
        if (_provider != null) {
            final AlarmManager am = _provider.getAlarmManager();

            if (immediate) {
                am.shutdownNow();
            } else {
                am.shutdown();
            }

            _provider = null;
        }
    }

    public synchronized static void start() {
        final String property = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("com.ibm.tx.config.ConfigurationProviderClassName");
            }
        });

        if (_provider == null && property != null) {
            try {
                final Class configProviderClass = Class.forName(property);
                _provider = (ConfigurationProvider) configProviderClass.newInstance();
            } catch (Throwable t) {
                _provider = null;
                t.printStackTrace();
            }
        }

    }

    public static ConfigurationProvider getConfigurationProvider() {
        return _provider;
    }

    public static void setConfigurationProvider(ConfigurationProvider p) {
        _provider = p;
    }
}