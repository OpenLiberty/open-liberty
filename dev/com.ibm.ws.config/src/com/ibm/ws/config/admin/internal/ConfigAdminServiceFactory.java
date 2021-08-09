/*******************************************************************************
 * Copyright (c) 2010,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin.internal;

import java.io.IOException;
import java.security.Permission;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 * A factory to create instances of ConfigurationAdmin.
 * It also contains all of centralized maps and tables
 *
 */
class ConfigAdminServiceFactory implements ServiceFactory<ConfigurationAdmin>, BundleListener {

    private static final TraceComponent tc = Tr.register(ConfigAdminServiceFactory.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private final Permission configurationPermission = new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE);

    protected final static int INDEX_DICTIONARY = 0;
    protected final static int INDEX_LOCATION = 1;
    protected final static int INDEX_METATYPE_PROCESSED = 2;
    protected final static int INDEX_IN_OVERRIDES_FILE = 3;
    protected final static int INDEX_REFERENCES = 4;
    protected final static int INDEX_UNIQUES = 5;

    /** BundleContext */
    final BundleContext bundleContext;

    /** a ManagedServiceTracker */
    protected final ManagedServiceTracker msTracker;

    /** a ManagedServiceFactorytracker */
    protected final ManagedServiceFactoryTracker msfTracker;

    private final PluginManager pluginManager;
    private final ConfigurationStore configurationStore;

    protected final UpdateQueue<String> updateQueue = new UpdateQueue<>();

    private final ConfigEventDispatcher ced;

    /** Nested configuration map to hold id to generated pid mapping */
    private final Map<ConfigID, ExtendedConfigurationImpl> idMap = Collections.synchronizedMap(new HashMap<ConfigID, ExtendedConfigurationImpl>());

    private final Map<ConfigID, Set<ConfigID>> referenceMap = Collections.synchronizedMap(new HashMap<ConfigID, Set<ConfigID>>());

    /** Tracker for variable registry service */
    private ServiceTracker<VariableRegistry, VariableRegistry> variableRegistryTracker = null;

    private final VariableRegistry variableRegistry;

    private final ServiceRegistration<?> configurationAdminRef;

    /**
     * Constructor.
     *
     * @param bc
     *               BundleContext
     */
    public ConfigAdminServiceFactory(BundleContext bc) {
        this.bundleContext = bc;

        variableRegistryTracker = new ServiceTracker<VariableRegistry, VariableRegistry>(bc, VariableRegistry.class.getName(), null);
        variableRegistryTracker.open();

        this.variableRegistry = variableRegistryTracker.getService();

        configurationStore = new ConfigurationStore(this, bc);
        pluginManager = new PluginManager(bc);
        pluginManager.start();

        this.ced = new ConfigEventDispatcher(this, bc);

        this.msTracker = new ManagedServiceTracker(this, bc);
        this.msfTracker = new ManagedServiceFactoryTracker(this, bc);

        bundleContext.addBundleListener(this);

        // register ConfigurationAdmin service
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("service.vendor", "IBM");
        this.configurationAdminRef = bc.registerService(ConfigurationAdmin.class.getName(), this, properties);

    }

    public void openManagedServiceTrackers() {
        this.msTracker.open();
        this.msfTracker.open();
    }

    public void closeServices() {
        bundleContext.removeBundleListener(this);
        this.configurationAdminRef.unregister();
        this.msfTracker.close();
        this.msTracker.close();
        try {
            configurationStore.saveConfigurationDatas(true);
        } catch (IOException e) {
            // Auto FFDC
        }
        this.updateQueue.shutdown();
        this.ced.close();
        pluginManager.stop();

        if (null != variableRegistryTracker) {
            variableRegistryTracker.close();
            variableRegistryTracker = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle,
     * org.osgi.framework.ServiceRegistration)
     */
    @Override
    public ConfigurationAdmin getService(Bundle bundle, ServiceRegistration<ConfigurationAdmin> registration) {
        ServiceReference<ConfigurationAdmin> reference = registration.getReference();
        ced.setServiceReference(reference);
        return new ConfigurationAdminImpl(this, bundle);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle,
     * org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<ConfigurationAdmin> registration, ConfigurationAdmin service) {
    }

    public void registerConfiguration(ConfigID id, ExtendedConfigurationImpl config) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Registering configuration with config id " + id + " : " + config);
        }
        config.setFullId(id);
        idMap.put(id, config);
    }

    public void unregisterConfiguration(ConfigID id) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Removing configuration with config id " + id);
        }
        idMap.remove(id);
    }

    public ExtendedConfigurationImpl lookupConfiguration(ConfigID id) {
        return idMap.get(id);
    }

    public void addReferences(Set<ConfigID> references, ConfigID id) {
        if (references != null) {
            for (ConfigID reference : references) {
                addReference(reference, id);
            }
        }
    }

    private void addReference(ConfigID reference, ConfigID id) {
        Set<ConfigID> referenceIds = null;
        synchronized (referenceMap) {
            referenceIds = referenceMap.get(reference);
            if (referenceIds == null) {
                referenceIds = Collections.synchronizedSet(new HashSet<ConfigID>());
                referenceMap.put(reference, referenceIds);
            }
        }
        referenceIds.add(id);
    }

    public void removeReferences(Set<ConfigID> references, ConfigID id) {
        if (references != null) {
            for (ConfigID reference : references) {
                removeReference(reference, id);
            }
        }
    }

    private void removeReference(ConfigID reference, ConfigID id) {
        Set<ConfigID> referenceIds = referenceMap.get(reference);
        if (referenceIds != null) {
            referenceIds.remove(id);
        }
    }

    public Set<ConfigID> getReferences(ConfigID id) {
        Set<ConfigID> references = referenceMap.get(id);
        return (references == null) ? Collections.<ConfigID> emptySet() : references;
    }

    Future<?> dispatchEvent(int type, String factoryPid, String pid) {
        return ced.dispatch(type, factoryPid, pid);
    }

    Future<?> notifyConfigurationUpdated(ExtendedConfigurationImpl config, boolean isFactory) {
        if (isFactory)
            return msfTracker.notifyUpdated(config);
        else
            return msTracker.notifyUpdated(config);
    }

    Future<?> notifyConfigurationDeleted(ExtendedConfigurationImpl config, boolean isFactory) {
        if (isFactory)
            return msfTracker.notifyDeleted(config);
        else
            return msTracker.notifyDeleted(config);
    }

    void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties, Object service) {
        pluginManager.modifyConfiguration(reference, properties);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED)
            configurationStore.unbindConfigurations(event.getBundle());
    }

    @Trivial
    public void checkConfigurationPermission() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(configurationPermission);
    }

    /**
     * @return
     */
    protected ConfigurationStore getConfigurationStore() {
        return configurationStore;
    }

    public VariableRegistry getVariableRegistry() {
        return variableRegistry;
    }

    public ExtendedConfiguration findConfiguration(String pid) {
        return configurationStore.findConfiguration(pid);
    }
}
