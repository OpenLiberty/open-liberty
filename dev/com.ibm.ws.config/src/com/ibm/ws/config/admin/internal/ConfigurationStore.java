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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 * ConfigurationStore manages all active configurations along with persistence. The current
 * implementation uses a filestore and serialization of the configuration dictionaries to files
 * identified by their pid. Persistence details are in the constructor, saveConfiguration, and
 * deleteConfiguration and can be factored out separately if required.
 */
class ConfigurationStore {
    private static final TraceComponent tc = Tr.register(ConfigurationStore.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private final ConfigAdminServiceFactory caFactory;
    /** Centralized store/retrieve of cached config files */
    protected PersistedConfigManager persistedConfig;

    private final Map<String, ExtendedConfigurationImpl> configurations = new HashMap<String, ExtendedConfigurationImpl>();

    protected boolean cachedConfigScanned = false;

    /** A lock to control cachedConfigScanned modification */
    protected final Object cachedConfigScannedLock = new Object() {};

    /** A counter for PID names to avoid collisions - the instance of this class should be synchronized before modifying this field */
    private long configCount = 0;

    public ConfigurationStore(ConfigAdminServiceFactory configAdminServiceFactory, BundleContext bc) {
        this.caFactory = configAdminServiceFactory;

        this.persistedConfig = new PersistedConfigManager(bc.getDataFile(ConfigAdminConstants.CONFIG_PERSISTENT_SUBDIR));

        synchronized (cachedConfigScannedLock) {
            if (!cachedConfigScanned) {
                cachedConfigScanned = true;
                String[] pids = persistedConfig.getCachedPids();
                for (String pid : pids) {
                    boolean deleteFile = false;
                    try {
                        ExtendedConfigurationImpl config = deserializeConfigurationData(pid);
                        if (config != null) {
                            configurations.put(config.getPid(), config);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "added persisted config " + config);
                        }
                    } catch (IOException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Exception while deserializing a ConfigurationImpl");
                        FFDCFilter.processException(e, getClass().getName(),
                                                    "Exception while deserializing a ConfigurationImpl",
                                                    new Object[] { pid });

                        deleteFile = true;
                    }

                    if (deleteFile) {
                        persistedConfig.deleteConfigFile(pid);
                    }

                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "config store pids are [" + configurations.keySet() + "]");
    }

    public void saveConfiguration(String pid, final ExtendedConfigurationImpl config) throws IOException {
        config.checkLocked();

        final File configFile = persistedConfig.getConfigFile(pid);

        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    serializeConfigurationData(configFile, config);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    public synchronized void removeConfiguration(final String pid) {
        configurations.remove(pid);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removed configuration pid = " + pid + ", remaining pids are [" + configurations.keySet() + "]");

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                persistedConfig.deleteConfigFile(pid);
                return null;
            }
        });
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

    void serializeConfigurationData(File configFile, ExtendedConfigurationImpl config) throws IOException {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            // get data outside of the sync block to prevent a deadlock, however we may have inconsistent information
            Dictionary<String, ?> properties = config.getReadOnlyProperties();
            Set<ConfigID> references = config.getReferences();
            Set<String> uniqueVars = config.getUniqueVariables();

            String bundleLocation = config.getBundleLocation();

            // write config data
            fos = new FileOutputStream(configFile, false);
            oos = new ObjectOutputStream(new BufferedOutputStream(fos));
            oos.writeObject(properties);
            oos.writeObject(bundleLocation);
            oos.writeBoolean(Boolean.FALSE);
            oos.writeObject(references);
            oos.writeObject(uniqueVars);
        } finally {
            ConfigUtil.closeIO(oos);
            ConfigUtil.closeIO(fos);
        }
    }

    /**
     * If a serialized file does not exist, a null is returned.
     * If a serialized file exists for the given pid, it deserializes
     * configuration dictionary
     * and bound location and returns in an array of size 2.
     * Index 0 of returning array contains configuration dictionary.
     * Index 1 of returning array contains bound bundle location in String
     * Index 2 of returning array contains whether Meta-Type processsing was done
     * or not
     * (if CMConstants.METATYPE_PROCESSED, then yes. if null, then no.)
     *
     * @param pid
     */
    ExtendedConfigurationImpl deserializeConfigurationData(String pid) throws IOException {
        ExtendedConfigurationImpl config = null;
        File configFile = persistedConfig.getConfigFile(pid);
        if (configFile != null) {

            if (configFile.length() > 0) {

                FileInputStream fis = null;
                ObjectInputStream ois = null;

                try {
                    fis = new FileInputStream(configFile);
                    ois = new ObjectInputStream(new BufferedInputStream(fis));
                    @SuppressWarnings("unchecked")
                    Dictionary<String, Object> d = (Dictionary<String, Object>) ois.readObject();
                    String location;
                    location = (String) ois.readObject();
                    ois.readBoolean();
                    @SuppressWarnings("unchecked")
                    Set<ConfigID> references = (Set<ConfigID>) ois.readObject();
                    @SuppressWarnings("unchecked")
                    Set<String> uniqueVariables = (Set<String>) ois.readObject();

                    String factoryPid = (String) d.get(ConfigurationAdmin.SERVICE_FACTORYPID);
                    VariableRegistry variableRegistry = caFactory.getVariableRegistry();
                    for (String variable : uniqueVariables) {
                        variableRegistry.addVariable(variable, ConfigAdminConstants.VAR_IN_USE);
                    }

                    config = new ExtendedConfigurationImpl(caFactory, location, factoryPid, pid, d, references, uniqueVariables);
                } catch (ClassNotFoundException e) {
                    throw new IOException(e);
                } finally {
                    ConfigUtil.closeIO(ois);
                    ConfigUtil.closeIO(fis);
                }
            }
        }

        return config;
    }
}