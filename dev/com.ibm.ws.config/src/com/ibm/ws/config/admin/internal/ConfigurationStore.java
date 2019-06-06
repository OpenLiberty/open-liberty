/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.ConfigStorageConsumer;

/**
 * ConfigurationStore manages all active configurations along with persistence. The current
 * implementation uses a filestore and serialization of the configuration dictionaries to files
 * identified by their pid. Persistence details are in the constructor, saveConfiguration, and
 * deleteConfiguration and can be factored out separately if required.
 */
class ConfigurationStore implements Runnable {
    private static final TraceComponent tc = Tr.register(ConfigurationStore.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private final ConfigAdminServiceFactory caFactory;

    private final Map<String, ExtendedConfigurationImpl> configurations;

    /** A counter for PID names to avoid collisions - the instance of this class should be synchronized before modifying this field */
    private long configCount = 0;

    private final File persistentConfig;

    private boolean dirty = false;
    private Future<?> saveTask = null;

    public ConfigurationStore(ConfigAdminServiceFactory configAdminServiceFactory, BundleContext bc) {
        this.caFactory = configAdminServiceFactory;
        this.persistentConfig = bc.getDataFile(ConfigAdminConstants.CONFIG_PERSISTENT);
        this.configurations = loadConfigurationDatas(this.persistentConfig);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "config store pids are [" + configurations.keySet() + "]");
    }

    public synchronized void removeConfiguration(final String pid) {
        configurations.remove(pid);
        save();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removed configuration pid = " + pid + ", remaining pids are [" + configurations.keySet() + "]");
    }

    public synchronized ExtendedConfigurationImpl getConfiguration(String pid, String location) {
        ExtendedConfigurationImpl config = configurations.get(pid);
        if (config == null) {
            config = new ExtendedConfigurationImpl(caFactory, location, null, pid, null, null, null);
            configurations.put(pid, config);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "added config " + config);
        }
        return config;
    }

    public synchronized ExtendedConfiguration createFactoryConfiguration(String factoryPid, String location) {
        String pid;
        do {
            pid = factoryPid + "_" + configCount++;
        } while (configurations.containsKey(pid));
        ExtendedConfigurationImpl config = new ExtendedConfigurationImpl(caFactory, location, factoryPid, pid, null, null, null);
        configurations.put(pid, config);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "added created factory config " + config);
        return config;
    }

    public synchronized ExtendedConfigurationImpl findConfiguration(String pid) {
        return configurations.get(pid);
    }

    public synchronized ExtendedConfigurationImpl[] getFactoryConfigurations(String factoryPid) {
        List<ExtendedConfigurationImpl> resultList = new ArrayList<ExtendedConfigurationImpl>();
        for (ExtendedConfigurationImpl config : configurations.values()) {
            String otherFactoryPid = config.getFactoryPid();
            if (otherFactoryPid != null && otherFactoryPid.equals(factoryPid))
                resultList.add(config);
        }
        return resultList.toArray(new ExtendedConfigurationImpl[resultList.size()]);
    }

    public synchronized ExtendedConfiguration[] listConfigurations(Filter filter) {
        List<ExtendedConfiguration> resultList = new ArrayList<ExtendedConfiguration>();
        for (ExtendedConfigurationImpl config : configurations.values()) {
            if (config.matchesFilter(filter))
                resultList.add(config);
        }
        int size = resultList.size();
        return size == 0 ? null : (ExtendedConfiguration[]) resultList.toArray(new ExtendedConfiguration[size]);
    }

    public synchronized void unbindConfigurations(Bundle bundle) {
        for (ExtendedConfigurationImpl config : configurations.values()) {
            config.unbind(bundle);
        }
    }

    synchronized void saveConfigurationDatas(boolean cancelSaveTask) throws IOException {
        if (dirty) {
            List<ExtendedConfigurationImpl> persistConfigs = new ArrayList<>();
            for (ExtendedConfigurationImpl persistConfig : configurations.values()) {
                if (persistConfig.getReadOnlyProperties() != null) {
                    persistConfigs.add(persistConfig);
                }
            }
            ConfigurationStorageHelper.store(persistentConfig, persistConfigs);
            dirty = false;
            if (cancelSaveTask && saveTask != null) {
                saveTask.cancel(false);
            }
            saveTask = null;
        }
    }

    private Map<String, ExtendedConfigurationImpl> loadConfigurationDatas(File configDatas) {
        if (configDatas.isFile()) {
            ConfigStorageConsumer<String, ExtendedConfigurationImpl> consumer = new ConfigStorageConsumer<String, ExtendedConfigurationImpl>() {
                @Override
                public ExtendedConfigurationImpl consumeConfigData(String location, Set<String> uniqueVars, Set<ConfigID> references, ConfigurationDictionary dict) {
                    String pid = (String) dict.get(Constants.SERVICE_PID);
                    String factoryPid = (String) dict.get(ConfigurationAdmin.SERVICE_FACTORYPID);
                    return new ExtendedConfigurationImpl(caFactory, location, factoryPid, pid, dict, references, uniqueVars);
                }

                @Override
                public String getKey(ExtendedConfigurationImpl configuration) {
                    return configuration.getPid(false);
                }
            };
            try {
                return ConfigurationStorageHelper.load(configDatas, consumer);
            } catch (IOException e) {
                // auto FFDC is fine
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    /**
     *
     */
    synchronized void save() {
        dirty = true;
        if (saveTask == null) {
            saveTask = caFactory.updateQueue.addScheduled(this);
        }
    }

    @Override
    public void run() {
        try {
            saveConfigurationDatas(false);
        } catch (IOException e) {
            // Auto-FFDC is fine here
        }
    }
}
