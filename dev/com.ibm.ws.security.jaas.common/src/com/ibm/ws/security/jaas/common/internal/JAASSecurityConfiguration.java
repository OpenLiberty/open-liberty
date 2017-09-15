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
package com.ibm.ws.security.jaas.common.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class JAASSecurityConfiguration extends Configuration {
    private static final TraceComponent tc = Tr.register(JAASSecurityConfiguration.class);
    private final Map<String, List<AppConfigurationEntry>> instancesAppConfigurationEntries;
    private final ReentrantReadWriteLock reentrantReadWriteLock;
    private final WriteLock writeLock;
    private final ReadLock readLock;

    public JAASSecurityConfiguration() {
        instancesAppConfigurationEntries = new HashMap<String, List<AppConfigurationEntry>>();
        reentrantReadWriteLock = new ReentrantReadWriteLock();
        writeLock = reentrantReadWriteLock.writeLock();
        readLock = reentrantReadWriteLock.readLock();
    }

    public void setAppConfigurationEntries(Map<String, List<AppConfigurationEntry>> configurationEntries) {
        writeLock.lock();
        try {
            instancesAppConfigurationEntries.putAll(configurationEntries);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "appConfigurationEntries: " + instancesAppConfigurationEntries.toString());
            }
        } finally {
            writeLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String jaasEntryName) {
        List<AppConfigurationEntry> appConfigurationEntry = getAppConfigurationEntryListFor(jaasEntryName);
        return createAppConfigurationEntryArrayFrom(appConfigurationEntry);
    }

    private List<AppConfigurationEntry> getAppConfigurationEntryListFor(String jaasEntryName) {
        List<AppConfigurationEntry> appConfigurationEntry = null;
        readLock.lock();
        try {
            appConfigurationEntry = instancesAppConfigurationEntries.get(jaasEntryName);
        } finally {
            readLock.unlock();
        }
        return appConfigurationEntry;
    }

    private AppConfigurationEntry[] createAppConfigurationEntryArrayFrom(List<AppConfigurationEntry> appConfigurationEntry) {
        AppConfigurationEntry[] appConfigurationEntryAsArray = null;
        if (appConfigurationEntry != null) {
            appConfigurationEntryAsArray = new AppConfigurationEntry[appConfigurationEntry.size()];
            appConfigurationEntryAsArray = appConfigurationEntry.toArray(appConfigurationEntryAsArray);
        }
        return appConfigurationEntryAsArray;
    }
}
