/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private final Lock saveMonitor = new ReentrantLock();
    private volatile Future<?> saveTask = null;
    private boolean shutdown = false;
    private final ReentrantReadWriteLock storeMonitor = new ReentrantReadWriteLock();

    public ConfigurationStore(ConfigAdminServiceFactory configAdminServiceFactory, BundleContext bc) {
        this.caFactory = configAdminServiceFactory;
        this.persistentConfig = bc.getDataFile(ConfigAdminConstants.CONFIG_PERSISTENT);
        this.configurations = loadConfigurationDatas(this.persistentConfig);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "config store pids are [" + configurations.keySet() + "]");
    }

    private final void readLock() {
        storeMonitor.readLock().lock();
    }

    private final void readUnlock() {
        storeMonitor.readLock().unlock();
    }

    public final void writeLock() {
        if (storeMonitor.getReadHoldCount() > 0) {
            // this is not supported and will cause deadlock if allowed to proceed.
            // fail fast instead of deadlocking
            throw new IllegalMonitorStateException("Requesting upgrade to write lock.");
        }
        storeMonitor.writeLock().lock();
    }

    public final void writeUnlock() {
        storeMonitor.writeLock().unlock();
    }

    public void removeConfiguration(final String pid) {
        writeLock();
        try {
            configurations.remove(pid);
        } finally {
            writeUnlock();
        }
        save();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removed configuration pid = " + pid + ", remaining pids are [" + configurations.keySet() + "]");
    }

    public ExtendedConfigurationImpl getConfiguration(String pid, String location) {
        readLock();
        ExtendedConfigurationImpl config;
        try {
            config = configurations.get(pid);
        } finally {
            readUnlock();
        }
        if (config != null) {
            return config;
        }
        writeLock();
        try {
            config = configurations.get(pid);
            if (config == null) {
                config = new ExtendedConfigurationImpl(caFactory, location, null, pid, null, null, null);
                configurations.put(pid, config);
            }
        } finally {
            writeUnlock();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "added config " + config);
        return config;
    }

    public ExtendedConfiguration createFactoryConfiguration(String factoryPid, String location) {
        ExtendedConfigurationImpl config;
        writeLock();
        try {
            String pid;
            do {
                pid = factoryPid + "_" + configCount++;
            } while (configurations.containsKey(pid));
            config = new ExtendedConfigurationImpl(caFactory, location, factoryPid, pid, null, null, null);
            configurations.put(pid, config);
        } finally {
            writeUnlock();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "added created factory config " + config);
        return config;
    }

    public ExtendedConfigurationImpl findConfiguration(String pid) {
        readLock();
        try {
            return configurations.get(pid);
        } finally {
            readUnlock();
        }
    }

    /*
     * This method can return deleted configurations
     */
    public ExtendedConfigurationImpl[] getFactoryConfigurations(String factoryPid) {
        readLock();
        try {
            List<ExtendedConfigurationImpl> resultList = new ArrayList<ExtendedConfigurationImpl>();
            for (ExtendedConfigurationImpl config : configurations.values()) {
                //bypassing the check to see if configuration was deleted
                String otherFactoryPid = config.getFactoryPid(false);
                if (otherFactoryPid != null && otherFactoryPid.equals(factoryPid))
                    resultList.add(config);
            }
            return resultList.toArray(new ExtendedConfigurationImpl[resultList.size()]);
        } finally {
            readUnlock();
        }
    }

    public ExtendedConfiguration[] listConfigurations(Filter filter) {
        List<ExtendedConfigurationImpl> resultList = new ArrayList<>();
        readLock();
        try {
            resultList.addAll(configurations.values());
        } finally {
            readUnlock();
        }
        for (Iterator<ExtendedConfigurationImpl> it = resultList.iterator(); it.hasNext();) {
            ExtendedConfigurationImpl config = it.next();
            if (!config.matchesFilter(filter)) {
                it.remove();
            }
        }
        int size = resultList.size();
        return size == 0 ? null : (ExtendedConfigurationImpl[]) resultList.toArray(new ExtendedConfigurationImpl[size]);
    }

    public void unbindConfigurations(Bundle bundle) {
        Collection<ExtendedConfigurationImpl> currentConfigs;
        readLock();
        try {
            currentConfigs = new ArrayList<>(configurations.values());
        } finally {
            readUnlock();
        }
        for (ExtendedConfigurationImpl config : currentConfigs) {
            config.unbind(bundle);
        }
    }

    void saveConfigurationDatas(boolean shutdown) throws IOException {
        Future<?> currentSaveTask;
        saveMonitor.lock();
        try {
            currentSaveTask = saveTask;
            saveTask = null;
            this.shutdown = shutdown;
        } finally {
            saveMonitor.unlock();
        }
        if (currentSaveTask == null) {
            return;
        }
        readLock();
        try {
            ConfigurationStorageHelper.store(persistentConfig, configurations.values());
            if (shutdown && currentSaveTask != null) {
                currentSaveTask.cancel(false);
            }

        } finally {
            readUnlock();
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
    void save() {
        if (saveTask != null) {
            return;
        }
        saveMonitor.lock();
        try {
            if (shutdown) {
                return;
            }
            if (saveTask == null) {
                saveTask = caFactory.updateQueue.addScheduled(this);
            }
        } finally {
            saveMonitor.unlock();
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
