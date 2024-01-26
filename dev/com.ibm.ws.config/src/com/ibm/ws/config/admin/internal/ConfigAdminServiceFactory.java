/*******************************************************************************
 * Copyright (c) 2010,2024 IBM Corporation and others.
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

//@formatter:off
class ConfigAdminServiceFactory implements ServiceFactory<ConfigurationAdmin>, BundleListener {
    private static final TraceComponent tc =
        Tr.register(ConfigAdminServiceFactory.class,
                    ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    // TODO: These appear to be unused.

    protected final static int INDEX_DICTIONARY = 0;
    protected final static int INDEX_LOCATION = 1;
    protected final static int INDEX_METATYPE_PROCESSED = 2;
    protected final static int INDEX_IN_OVERRIDES_FILE = 3;
    protected final static int INDEX_REFERENCES = 4;
    protected final static int INDEX_UNIQUES = 5;

    //

    public ConfigAdminServiceFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;

        this.variableRegistryTracker = new ServiceTracker<VariableRegistry, VariableRegistry>(
                        bundleContext,
                        VariableRegistry.class.getName(), null);
        this.variableRegistryTracker.open();
        this.variableRegistry = variableRegistryTracker.getService();

        this.configurationStore = new ConfigurationStore(this, bundleContext);

        this.pluginManager = new PluginManager(bundleContext);
        this.pluginManager.start();

        this.eventDispatcher = new ConfigEventDispatcher(this, bundleContext);

        this.managedServiceTracker = new ManagedServiceTracker(this, bundleContext);
        this.managedServiceFactoryTracker = new ManagedServiceFactoryTracker(this, bundleContext);

        this.bundleContext.addBundleListener(this);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("service.vendor", "IBM");
        this.configurationAdminRef = bundleContext.registerService(ConfigurationAdmin.class.getName(), this, properties);
    }

    public void closeServices() {
        bundleContext.removeBundleListener(this);

        configurationAdminRef.unregister();

        managedServiceFactoryTracker.close();
        managedServiceTracker.close();

        try {
            configurationStore.saveConfigurationDatas(true);
        } catch ( IOException e ) {
            // Auto FFDC
        }

        this.updateQueue.shutdown();
        this.eventDispatcher.close();

        this.pluginManager.stop();

        if ( variableRegistryTracker != null ) {
            variableRegistryTracker.close();
            variableRegistryTracker = null;
        }
    }

    // Bundle context ...

    final BundleContext bundleContext;

    // Variable tracking ...

    private ServiceTracker<VariableRegistry, VariableRegistry> variableRegistryTracker;

    private final VariableRegistry variableRegistry;

    @Trivial
    public VariableRegistry getVariableRegistry() {
        return variableRegistry;
    }

    // Configuration store ...

    private final ConfigurationStore configurationStore;

    @Trivial
    protected ConfigurationStore getConfigurationStore() {
        return configurationStore;
    }

    @Trivial
    public ExtendedConfigurationImpl getConfiguration(String pid, Bundle bundle) {
        return configurationStore.getConfiguration(pid, bundle);
    }

    @Trivial
    public ExtendedConfigurationImpl getConfiguration(String pid, String location) {
        return configurationStore.getConfiguration(pid, location);
    }

    @Trivial
    public ExtendedConfiguration findConfiguration(String pid) {
        return configurationStore.findConfiguration(pid);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if ( event.getType() == BundleEvent.UNINSTALLED ) {
            configurationStore.unbindConfigurations( event.getBundle() );
        }
    }

    /**
     * Table of configurations in the store.
     *
     * Keys are full configuration IDs.  Values are configurations.
     *
     * This is a table of all active configurations, not just the top
     * level configurations managed by the configuration store.
     */

    private final Map<ConfigID, ExtendedConfigurationImpl> idMap =
        Collections.synchronizedMap(new HashMap<ConfigID, ExtendedConfigurationImpl>());

    /**
     * Register (add) a configuration to the table of active configurations.
     *
     * As a side effect, call back to the configuration to set its full ID.
     * See {@link ExtendedConfigurationImpl#setFullId(ConfigID)}.
     *
     * @param id The full ID of the configuration.
     * @param config The configuration which is being registered.
     */
    public void registerConfiguration(ConfigID id, ExtendedConfigurationImpl config) {
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, "Register configuration [ " + id + " : " + config + " ]");
        }
        config.setFullId(id);
        idMap.put(id, config);
    }

    /**
     * Un-register (remove) a configuration to the table of active configurations.
     *
     * @param id The full ID of the configuration which is to be removed.
     */
    public void unregisterConfiguration(ConfigID id) {
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, "Unregister configuration [ " + id + " ]");
        }
        idMap.remove(id);
    }

    public ExtendedConfigurationImpl lookupConfiguration(ConfigID id) {
        return idMap.get(id);
    }

    //

    /**
     * Table of configuration references in the store:
     *
     * Keys are the configuration IDs of parent (referencing)
     *
     * Values are sets of IDs of child (referenced) configurations.
     */
    private final Map<ConfigID, Set<ConfigID>> references =
        Collections.synchronizedMap(new HashMap<ConfigID, Set<ConfigID>>());

    public void addReferences(Set<ConfigID> parentIds, ConfigID childId) {
        if ( (parentIds != null) && !parentIds.isEmpty() ) {
            for ( ConfigID parentId : parentIds ) {
                addReference(parentId, childId);
            }
        }
    }

    private void addReference(ConfigID parentId, ConfigID childId) {
        Set<ConfigID> referenceIds = references.computeIfAbsent(
            parentId,
            (ConfigID useParentId) -> Collections.synchronizedSet(new HashSet<ConfigID>()) );

        referenceIds.add(childId);
    }

    public void removeReferences(Set<ConfigID> parentIds, ConfigID childId) {
        if ( (parentIds != null) && !parentIds.isEmpty() ) {
            for ( ConfigID parentId : parentIds ) {
                removeReference(parentId, childId);
            }
        }
    }

    private void removeReference(ConfigID parentId, ConfigID childId) {
        Set<ConfigID> referenceIds = references.get(parentId);
        if ( referenceIds != null ) {
            referenceIds.remove(childId);
        }
    }

    public Set<ConfigID> getReferences(ConfigID childId) {
        Set<ConfigID> parentIds = references.get(childId);
        return ( (parentIds == null) ? Collections.<ConfigID> emptySet() : parentIds );
    }

    //

    private final PluginManager pluginManager;

    void modifyConfiguration(ServiceReference<?> reference,
                             Dictionary<String, Object> properties,
                             @SuppressWarnings("unused") Object service) {

        pluginManager.modifyConfiguration(reference, properties);
    }

    //

    /** Queue used by the event dispatcher. */
    protected final UpdateQueue<String> updateQueue = new UpdateQueue<>();

    private final ConfigEventDispatcher eventDispatcher;

    @Override
    public ConfigurationAdmin getService(Bundle bundle,
                                         ServiceRegistration<ConfigurationAdmin> registration) {

        ServiceReference<ConfigurationAdmin> reference = registration.getReference();
        eventDispatcher.setServiceReference(reference);

        return new ConfigurationAdminImpl(this, bundle);
    }

    @Override
    public void ungetService(Bundle bundle,
                             ServiceRegistration<ConfigurationAdmin> registration,
                             ConfigurationAdmin service) {
        // EMPTY
    }

    Future<?> dispatchEvent(int type, String factoryPid, String pid) {
        return eventDispatcher.dispatch(type, factoryPid, pid);
    }

    //

    protected final ManagedServiceTracker managedServiceTracker;
    protected final ManagedServiceFactoryTracker managedServiceFactoryTracker;

    public void openManagedServiceTrackers() {
        managedServiceTracker.open();
        managedServiceFactoryTracker.open();
    }

    Future<?> notifyConfigurationUpdated(ExtendedConfigurationImpl config, boolean isFactory) {
        if ( isFactory ) {
            return managedServiceFactoryTracker.notifyUpdated(config);
        } else {
            return managedServiceTracker.notifyUpdated(config);
        }
    }

    Future<?> notifyConfigurationDeleted(ExtendedConfigurationImpl config, boolean isFactory) {
        if ( isFactory ) {
            return managedServiceFactoryTracker.notifyDeleted(config);
        } else {
            return managedServiceTracker.notifyDeleted(config);
        }
    }

    //

    private final ServiceRegistration<?> configurationAdminRef;

    // Permission ...

    private final Permission configurationPermission =
        new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE);

    /**
     * If security is enabled, verify that {@link #configurationPermission}
     * is enabled for the current security context.
     *
     * @throws SecurityException Thrown if security is not enabled and
     *     configuration permission is not enabled.
     */
    @Trivial
    public void checkConfigurationPermission() throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if ( sm != null) {
            sm.checkPermission(configurationPermission);
        }
    }
}
//@formatter:on