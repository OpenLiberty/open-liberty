/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.MetaTypeInformation;

import com.ibm.websphere.config.ConfigRetrieverException;
import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.PidReference;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * Processes bundle's default configuration files (bundle*.xml) stored inside
 * bundles under <code>META-INF/configuration/</code> directory and updates
 * ConfigurationAdmin.
 */
class BundleProcessor implements SynchronousBundleListener, EventHandler, RuntimeUpdateListener {

    private static final TraceComponent tc = Tr.register(BundleProcessor.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private static final String EXTENDED_BUNDLE_CACHE = "extended.bundle.cache";
    // increment this static by 1 each time the cache format changes
    private static final int EXTENDED_BUNDLE_CACHE_VERSION = 0;
    private final BundleContext bundleContext;
    private final SystemConfiguration systemConfiguration;
    private final WsLocationAdmin locationService;
    private final MetaTypeRegistry metatypeRegistry;
    private final ConfigUpdater configUpdater;
    private final ChangeHandler changeHandler;
    private final ConfigValidator validator;
    private final ConfigRetriever configRetriever;

    private boolean reprocessConfig;
    private final Object bundleChangedLock = new Object() {
    };
    private final Object metatypeChangedLock = new Object() {
    };

    /** The registration of this as an EventHandler for MTP changes */
    private final ServiceRegistration<EventHandler> eventHandlerService;

    /** The registration of this as a RuntimeUpdateListener for feature changes */
    private final ServiceRegistration<RuntimeUpdateListener> updateListenerService;

    private final Map<Bundle, ExtendedBundle> extendedBundles = new WeakHashMap<>();

    /**
     * Construct BundleProcessor and processes bundle's default configuration
     * files
     * for those bundles that are already installed.
     *
     * @param bc
     *                               BundleContext
     * @param caImpl
     *                               ConfigurationAdmin
     * @param onError
     *                               whether to continue or not when config error is detected
     * @param bundleResolveOrder
     * @throws ConfigUpdateException
     * @throws ConfigValidationException
     */
    public BundleProcessor(BundleContext bc, SystemConfiguration systemConfiguration,
                           WsLocationAdmin locationService,
                           ConfigUpdater configUpdater,
                           ChangeHandler changeHandler,
                           ConfigValidator validator, ConfigRetriever configRetriever) {
        this.bundleContext = bc;
        this.systemConfiguration = systemConfiguration;
        this.locationService = locationService;
        this.configUpdater = configUpdater;
        this.changeHandler = changeHandler;
        this.validator = validator;
        this.configRetriever = configRetriever;

        loadExtendedBundles();

        ServiceReference<MetaTypeRegistry> ref = bundleContext.getServiceReference(MetaTypeRegistry.class);
        this.metatypeRegistry = bundleContext.getService(ref);

        // register this as an event handler for MetaTypeProvider (MTP) topics
        Dictionary<String, Object> eventHandlerServiceProps = new Hashtable<String, Object>();
        eventHandlerServiceProps.put(EventConstants.EVENT_TOPIC, new String[] { MetaTypeRegistry.MTP_ADDED_TOPIC,
                                                                                MetaTypeRegistry.MTP_REMOVED_TOPIC });
        eventHandlerService = bundleContext.registerService(EventHandler.class, this, eventHandlerServiceProps);

        //register this as a runtime update listener
        Dictionary<String, Object> updateListenerProps = new Hashtable<String, Object>();
        updateListenerService = bundleContext.registerService(RuntimeUpdateListener.class, this, updateListenerProps);
    }

    private void loadExtendedBundles() {
        BundleContext systemContext = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        File extendedBundlesCache = bundleContext.getDataFile(EXTENDED_BUNDLE_CACHE);
        if (extendedBundlesCache.isFile()) {
            synchronized (extendedBundles) {
                try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(extendedBundlesCache)))) {
                    if (in.readInt() != EXTENDED_BUNDLE_CACHE_VERSION) {
                        // ignore cache when it is a different version than we expect
                        return;
                    }
                    int numBundles = in.readInt();
                    for (int i = 0; i < numBundles; i++) {
                        long id = in.readLong();
                        long lastProcessed = in.readLong();
                        String nameAndVersion = in.readUTF();
                        boolean hasDefaultConfig = in.readBoolean();
                        Bundle b = systemContext.getBundle(id);
                        if (b != null) {
                            extendedBundles.put(b, new ExtendedBundle(b, lastProcessed, nameAndVersion, hasDefaultConfig));
                        }
                    }
                } catch (IOException e) {
                    // auto FFDC is fine here
                }
            }
        }
    }

    private void saveExtendedBundles() {
        Collection<ExtendedBundle> saveExtendedBundles;
        synchronized (extendedBundles) {
            saveExtendedBundles = new ArrayList<>(extendedBundles.values());
        }
        File extendedBundlesCache = bundleContext.getDataFile(EXTENDED_BUNDLE_CACHE);
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(extendedBundlesCache)))) {
            out.writeInt(EXTENDED_BUNDLE_CACHE_VERSION);
            out.writeInt(saveExtendedBundles.size());
            for (ExtendedBundle extendedBundle : saveExtendedBundles) {
                out.writeLong(extendedBundle.getBundle().getBundleId());
                out.writeLong(extendedBundle.getLastProcessed());
                out.writeUTF(extendedBundle.getNameAndVersion());
                out.writeBoolean(extendedBundle.hasDefaultConfig());
            }
        } catch (IOException e) {
            // auto FFDC is fine here
        }
    }

    ExtendedBundle getExtendedBundle(Bundle b) {
        synchronized (extendedBundles) {
            ExtendedBundle result = extendedBundles.get(b);
            if (result != null) {
                return result;
            }
            result = new ExtendedBundle(b);
            extendedBundles.put(b, result);
            return result;
        }
    }

    private final Collection<Bundle> existingBundles = new ArrayList<Bundle>();

    void startProcessor(boolean reprocessConfig) {
        synchronized (this) {
            this.reprocessConfig = reprocessConfig;
        }
        synchronized (bundleChangedLock) {
            bundleContext.addBundleListener(this);
            ArrayList<Bundle> bundlesToProcess = new ArrayList<Bundle>();
            for (Bundle b : bundleContext.getBundles()) {
                if (b.getState() >= Bundle.RESOLVED) {
                    if (b.getLocation().startsWith(XMLConfigConstants.BUNDLE_LOC_KERNEL_TAG)) {
                        //process kernel bundles immediately
                        bundlesToProcess.add(b);
                    } else {
                        //add it to the queue for processing later
                        existingBundles.add(b);
                    }
                }
            }
            addBundles(bundlesToProcess);
        }
    }

    void stopProcessor() {
        bundleContext.removeBundleListener(this);
        eventHandlerService.unregister();
        updateListenerService.unregister();
        saveExtendedBundles();
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (FrameworkState.isStopping()) {
            // things are occurring when we are stopping, so just ignore them
            return;
        }

        synchronized (bundleChangedLock) {
            int type = event.getType();
            Bundle b = event.getBundle();
            boolean isFeatureBundle = b.getLocation().startsWith(XMLConfigConstants.BUNDLE_LOC_FEATURE_TAG);
            if (type == BundleEvent.RESOLVED) {

                if (isFeatureBundle) {

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Bundle resolved event for feature bundle {0}", b);
                    }

                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding bundle {0}", b);
                    }
                    //process non-feature bundles immediately
                    addBundles(Arrays.asList(b));
                }
            } else if (type == BundleEvent.UNRESOLVED) {
                if (!isFeatureBundle) {
                    //process non-feature bundles immediately
                    removeBundles(Arrays.asList(b));
                }
            }
        }
    }

    public void metaTypeAdded(Bundle bundle, Set<RegistryEntry> registryEntries) {

        Set<String> updatedPids = new HashSet<String>();
        for (RegistryEntry entry : registryEntries) {
            if (entry != null) {
                updatedPids.add(entry.getPid());
            }
        }
        synchronized (metatypeChangedLock) {
            try {
                PIDProcessor processor = new PIDProcessor(new ExtendedBundle(bundle).getNameAndVersion());
                processor.processRegistryEntries(registryEntries);

                configUpdater.updateAndFireEvents(processor.getConfigurationsToUpdate(), ErrorHandler.INSTANCE.getOnError());
                configUpdater.fireMetatypeAddedEvents(updatedPids);

            } catch (ConfigUpdateException e) {
                Tr.error(tc, "error.config.update.init", e.getMessage());
            }
        }
    }

    public void metaTypeRemoved(MetaTypeInformation info, Set<RegistryEntry> updatedPids) {
        if (FrameworkState.isStopping()) {
            // if the framework is stopping, just ignore this
            return;
        }
        synchronized (metatypeChangedLock) {

            ServerConfiguration sc = systemConfiguration.getServerConfiguration();

            Set<String> updates = changeHandler.removeMetatypeConvertedConfig(sc, updatedPids);
            configUpdater.fireMetatypeDeletedEvents(updates);
            if (tc.isDebugEnabled()) {
                for (String update : updates) {
                    Tr.debug(tc, "Removed metatype for PID " + update);
                }
            }

        }

    }

    @Override
    public void notificationCreated(final RuntimeUpdateManager updateManager, final RuntimeUpdateNotification notification) {
        if (FrameworkState.isStopping()) {
            // if the framework is stopping, just ignore incoming events
            return;
        }

        if (RuntimeUpdateNotification.FEATURE_BUNDLES_RESOLVED.equals(notification.getName())) {
            notification.onCompletion(new CompletionListener<Boolean>() {

                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (FrameworkState.isStopping()) {
                        // if the framework is stopping, just ignore incoming events
                        return;
                    }

                    RuntimeUpdateNotification processed = updateManager.createNotification(RuntimeUpdateNotification.FEATURE_BUNDLES_PROCESSED);
                    Map<String, Object> props = notification.getProperties();
                    if (props != null) {
                        @SuppressWarnings("unchecked")
                        Set<Bundle> addedBundles = (Set<Bundle>) props.get(RuntimeUpdateNotification.INSTALLED_BUNDLES_IN_UPDATE);
                        @SuppressWarnings("unchecked")
                        Set<Bundle> removedBundles = (Set<Bundle>) props.get(RuntimeUpdateNotification.REMOVED_BUNDLES_IN_UPDATE);

                        synchronized (BundleProcessor.this) {
                            // We first add from 'existingBundles', which is the list of bundles that
                            // existed in the cache at startup. We remove bundles after that as a feature
                            // update may result in a cached bundle being removed. Finally, we add the bundles
                            // installed in the feature update.

                            if (!existingBundles.isEmpty()) {
                                if (tc.isDebugEnabled()) {
                                    for (Bundle bundle : existingBundles) {
                                        Tr.debug(tc, "Processing cached bundle: " + bundle);
                                    }
                                }
                                addBundles(existingBundles);
                                existingBundles.clear();
                            }

                            if (removedBundles != null) {
                                if (tc.isDebugEnabled()) {
                                    for (Bundle bundle : removedBundles) {
                                        Tr.debug(tc, "Processing removed bundle: " + bundle);
                                    }
                                }
                                removeBundles(removedBundles);
                            }
                            if (addedBundles != null) {
                                if (tc.isDebugEnabled()) {
                                    for (Bundle bundle : addedBundles) {
                                        Tr.debug(tc, "Processing added bundle: " + bundle);
                                    }
                                }
                                addBundles(addedBundles);
                            }
                        }
                    }

                    processed.setResult(true);

                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    // nothing to do here
                }
            });
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (FrameworkState.isStopping()) {
            // if the framework is stopping, just ignore incoming events
            return;
        }

        String topic = event.getTopic();
        if (MetaTypeRegistry.MTP_ADDED_TOPIC.equals(topic)) {
            Bundle bundle = (Bundle) event.getProperty(MetaTypeRegistry.BUNDLE);
            @SuppressWarnings("unchecked")
            Set<RegistryEntry> updatedPids = (Set<RegistryEntry>) event.getProperty(MetaTypeRegistry.UPDATED_PIDS);
            metaTypeAdded(bundle, updatedPids);
        } else if (MetaTypeRegistry.MTP_REMOVED_TOPIC.equals(topic)) {
            MetaTypeInformation info = (MetaTypeInformation) event.getProperty(MetaTypeRegistry.MTP_INFO);
            @SuppressWarnings("unchecked")
            Set<RegistryEntry> updatedPids = (Set<RegistryEntry>) event.getProperty(MetaTypeRegistry.UPDATED_PIDS);
            metaTypeRemoved(info, updatedPids);

        }
    }

    @FFDCIgnore(ConfigUpdateException.class)
    private void addBundles(Collection<Bundle> bundles) {
        synchronized (this) {
            try {
                if (FrameworkState.isStopping()) {
                    return;
                }

                Set<RegistryEntry> newEntries = new HashSet<RegistryEntry>();
                for (Bundle b : bundles) {
                    if (b.getState() >= Bundle.RESOLVED) {
                        Set<RegistryEntry> entries = metatypeRegistry.addMetaType(b);
                        if (reprocessConfig || getExtendedBundle(b).needsReprocessing()) {
                            newEntries.addAll(entries);
                        }
                    }
                }

                for (Bundle b : bundles) {
                    if (b.getState() >= Bundle.RESOLVED) {
                        newEntries.addAll(processBundleConfig(b));
                    }
                }

                PIDProcessor pidProcessor = new PIDProcessor("Feature Update");
                pidProcessor.processRegistryEntries(newEntries);

                /*
                 * Configurations should be updated in the right dependency order otherwise looking
                 * up child configurations might return incomplete data (depending on timing).
                 * The next two steps avoid the incomplete data problem by first saving the updated
                 * configuration data to disk and then firing the "updated" events.
                 */

                configUpdater.updateAndFireEvents(pidProcessor.getConfigurationsToUpdate(), ErrorHandler.INSTANCE.getOnError());
                configUpdater.processUnresolvedReferences(ErrorHandler.INSTANCE.getOnError());
            } catch (ConfigUpdateException e) {
                handleConfigUpdateException(e);
            } catch (ConfigValidationException cve) {
                handleConfigValidationException(cve);
            }
        }
    }

    @FFDCIgnore(ConfigUpdateException.class)
    private void removeBundles(Collection<Bundle> bundles) {
        synchronized (this) {
            for (Bundle bundle : bundles) {
                if (FrameworkState.isStopping()) {
                    return;
                }
                ServerConfiguration oldConfig = systemConfiguration.copyServerConfiguration();
                systemConfiguration.bundleRemoved(bundle);
                try {
                    systemConfiguration.removeDefaultConfiguration(oldConfig);
                } catch (ConfigUpdateException e) {
                    handleConfigUpdateException(e);
                }

                metatypeRegistry.removeMetaType(bundle);
            }
            try {
                configUpdater.processUnresolvedReferences(ErrorHandler.INSTANCE.getOnError());
            } catch (ConfigUpdateException e) {
                handleConfigUpdateException(e);
            }
        }
    }

    /**
     * Checks for existence of bundle.cfg configuration file inside the bundle.
     * If exists, parse it and merge with serverBaseConfig and update ConfigAdmin.
     * Update happens only if one of the following is true:
     * <UL>
     * <LI>the PID has not been notified before</LI>
     * <LI>configuration for PID has changed from what is stored in ConfigAdmin</LI>
     * </UL>
     * If a server's root configuration document doesn't exist, only the
     * bundle.cfg is processed and updates ConfigAdmin.
     * If bundle.cfg doesn't define any configuration, values from
     * serverBaseConfig will be used to update ConfigAdmin.
     *
     * @param bundle
     * @param newEntries Set of new entries to process. This will be modified by this method.
     * @throws ConfigValidationException
     */

    private synchronized Set<RegistryEntry> processBundleConfig(Bundle bundle) throws ConfigUpdateException, ConfigValidationException {

        //get the MetaTypeInformation from the registry because we will always have processed
        //metatype first before config (it is a req. of being able to use extends in different bundles)
        MetaTypeInformation metaTypeInformation = metatypeRegistry.getMetaTypeInformation(bundle);
        if (metaTypeInformation != null) {
            //For factory PIDs that did not result in a RegistryEntry, mark them missing if it was due to missing extended entry.
            for (String factoryPid : metaTypeInformation.getFactoryPids()) {
                if (metatypeRegistry.getRegistryEntry(factoryPid) == null) {
                    metatypeRegistry.missingPid(factoryPid);
                }
            }
        }

        ExtendedBundle eb = getExtendedBundle(bundle);
        //reprocess any registry entries used in the default config from this bundle.
        Set<RegistryEntry> entriesToProcess = new HashSet<RegistryEntry>();
        if (eb.hasDefaultConfig()) {
            BaseConfiguration newDefaultConfig = systemConfiguration.loadDefaultConfiguration(bundle);
            if (newDefaultConfig != null) {
                Set<String> defaultConfigNames = newDefaultConfig.getConfigurationNames();
                for (String defaultConfigName : defaultConfigNames) {
                    RegistryEntry entry = metatypeRegistry.getRegistryEntryByPidOrAlias(defaultConfigName);
                    if (entry != null) {
                        entriesToProcess.add(entry);
                    }
                }
            } else {
                eb.setHasDefaultConfig(false);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processBundle():  Processing bundle name/version=" + eb.getNameAndVersion());
        }

        boolean reprocessBundle = reprocessConfig || eb.needsReprocessing();

        // we are done if we don't have to reprocess the bundle
        if (!reprocessBundle) {
            // still need to do validation
            validator.validate(entriesToProcess);
            return Collections.emptySet();
        }

        eb.updateLastProcessed();
        return entriesToProcess;
    }

    /**
     * @param pid
     * @param properties
     * @throws ConfigUpdateException
     */
    public void addDefaultConfiguration(String pid, Dictionary<String, String> properties) throws ConfigUpdateException {

        updateWithNewDefaults(systemConfiguration.addDefaultConfiguration(pid, properties));

    }

    public void addDefaultConfiguration(InputStream defaultConfig) throws ConfigUpdateException {
        try {
            updateWithNewDefaults(systemConfiguration.addDefaultConfiguration(defaultConfig));
        } catch (ConfigValidationException ex) {
            throw new ConfigUpdateException(ex);
        }
    }

    private void updateWithNewDefaults(BaseConfiguration newDefaultConfig) throws ConfigUpdateException {
        if (newDefaultConfig != null) {
            Set<String> defaultConfigNames = newDefaultConfig.getConfigurationNames();
            Set<RegistryEntry> registryEntries = new HashSet<RegistryEntry>(defaultConfigNames.size());
            for (String configName : defaultConfigNames) {
                RegistryEntry registryEntry = metatypeRegistry.getRegistryEntryByPidOrAlias(configName);
                if (registryEntry != null) {
                    registryEntries.add(registryEntry);
                }
            }
            PIDProcessor processor = new PIDProcessor("default configuration");
            processor.processRegistryEntries(registryEntries);

            configUpdater.updateAndFireEvents(processor.getConfigurationsToUpdate(), ErrorHandler.INSTANCE.getOnError());
        }
    }

    /**
     * @param pid
     * @throws ConfigUpdateException
     */
    public boolean removeDefaultConfiguration(String pid) throws ConfigUpdateException {

        return systemConfiguration.removeDefaultConfiguration(pid, null);

    }

    /**
     * @param pid
     * @param id
     * @return
     * @throws ConfigUpdateException
     */
    public boolean removeDefaultConfiguration(String pid, String id) throws ConfigUpdateException {
        if (FrameworkState.isStopping()) {
            // if the framework is stopping, just ignore this
            return false;
        }

        return systemConfiguration.removeDefaultConfiguration(pid, id);

    }

    /**
     * Processor for the set of registry entries introduced in a feature update.
     */
    private class PIDProcessor {

        private final String updateName;
        private final ServerConfiguration serverConfig;
        private final List<ConfigurationInfo> infos = new ArrayList<ConfigurationInfo>();

        /**
         * @param updateName arbitrary name for the set of updates.
         */
        public PIDProcessor(String updateName) {
            this.updateName = updateName;
            this.serverConfig = systemConfiguration.getServerConfiguration();
        }

        /**
         * @return
         */
        public List<ConfigurationInfo> getConfigurationsToUpdate() {
            return this.infos;
        }

        void processRegistryEntries(Set<RegistryEntry> entries) throws ConfigUpdateException {
            Set<RegistryEntry> allEntriesToProcess = new HashSet<RegistryEntry>();
            // Add all referring entries to the list of entries to process. This ensures that references are
            // evaluated when new metatype arrives.
            for (RegistryEntry entry : entries) {
                allEntriesToProcess.add(entry);
                List<PidReference> pids = entry.getReferencingEntries();
                for (PidReference pid : pids) {
                    allEntriesToProcess.add(pid.getReferencingEntry());
                }
            }

            for (RegistryEntry entry : allEntriesToProcess) {
                if (entry.isSingleton()) {
                    processSingletonPid(entry);
                } else {
                    processFactoryPid(entry);
                }
            }
        }

        /**
         * @param pid
         * @throws ConfigUpdateException
         */
        private void processFactoryPid(RegistryEntry registry) throws ConfigUpdateException {
            String defaultId = registry.getDefaultId();
            Map<ConfigID, FactoryElement> instances = serverConfig.getFactoryInstancesUsingDefaultId(registry.getPid(), registry.getAlias(), defaultId);

            processFactoryInstances(registry, instances);

            // Find configs that may reference this config by ibm:service and add them to the update list
            findReferringConfigs(metatypeRegistry.getEntriesUsingService(registry));
            // Find nested instances in the config and add them to the update list.
            List<ConfigElement> nestedElements = serverConfig.getNestedInstances(registry);
            for (ConfigElement nested : nestedElements) {

                ConfigElement parentCE = nested.getParent();

                RegistryEntry parentEntry = metatypeRegistry.getRegistryEntry(parentCE);
                while (parentCE.getParent() != null) {
                    parentCE = parentCE.getParent();
                }
                parentEntry = metatypeRegistry.getRegistryEntry(parentCE);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Creating configuration info for new element {0} using registry entry {1}", parentEntry);
                }

                // Delete all nested, pre-metatype instances
                try {
                    ExtendedConfiguration[] configs = configRetriever.findAllNestedConfigurations(nested.getConfigID());
                    if (configs != null) {
                        for (ExtendedConfiguration config : configs) {
                            if (!config.isDeleted()) {
                                config.delete(false);
                            }
                        }
                    }
                } catch (ConfigRetrieverException e) {
                    // Really shouldn't happen, just Tr.debug it
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception when retrieving pre-metatype configurations to delete: " + e);
                    }
                }

                try {
                    ConfigurationInfo info = changeHandler.createConfigurationInfo(parentCE, parentEntry);
                    addIfUnique(infos, info);
                } catch (ConfigNotFoundException ex) {
                    throw new ConfigUpdateException(ex);
                }
            }

            // Delete all pre-metatype configurations
            if (registry.getAlias() != null && !registry.getPid().equals(registry.getAlias())) {
                //remove aliased configuration
                try {
                    ExtendedConfiguration[] aliasedConfigs = configRetriever.findAllConfigurationsByPid(registry.getAlias());
                    if (aliasedConfigs != null) {
                        for (ExtendedConfiguration aliased : aliasedConfigs) {
                            if (!aliased.isDeleted()) {
                                // We may have already deleted this as part of nested element processing
                                aliased.delete(false);
                            }
                        }
                    }
                } catch (ConfigRetrieverException e) {
                    // Really shouldn't happen, just Tr.debug it
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception when retrieving pre-metatype configurations to delete: " + e);
                    }
                }
            }
        }

        /**
         * @param registry
         * @throws ConfigUpdateException
         */
        private void processSingletonPid(RegistryEntry registry) throws ConfigUpdateException {
            SingletonElement configElement = serverConfig.getSingleton(registry.getPid(), registry.getAlias());
            ConfigurationInfo singleton = processSingletonInstances(configElement, registry);

            // Find configs that may reference this config by ibm:service and add them to the update list
            findReferringConfigs(metatypeRegistry.getEntriesUsingService(registry));

            String parentPID = registry.getObjectClassDefinition().getParentPID();
            if (parentPID != null) {
                // Find nested instances in the config and add them to the update list.
                List<ConfigElement> nestedElements = serverConfig.getNestedInstances(registry);
                for (ConfigElement nested : nestedElements) {

                    ConfigElement parentCE = nested.getParent();

                    while (parentCE.getParent() != null) {
                        parentCE = parentCE.getParent();
                    }

                    try {
                        RegistryEntry parentEntry = metatypeRegistry.getRegistryEntry(parentCE);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Creating configuration info for new element {0} using registry entry {1}", parentEntry);
                        }
                        ConfigurationInfo info = changeHandler.createConfigurationInfo(parentCE, parentEntry);
                        infos.add(info);
                    } catch (ConfigNotFoundException ex) {
                        throw new ConfigUpdateException(ex);
                    }
                    // If the configElement is null, we may generate a default singleton. We've now found
                    // a legitimate singleton, so get rid of the default.
                    if (serverConfig.getSingletonElements(registry.getPid(), registry.getAlias()).isEmpty())
                        singleton = null;
                }
            }

            if (singleton != null) {
                infos.add(singleton);
            }
        }

        void findReferringConfigs(List<RegistryEntry> entries) throws ConfigMergeException, ConfigUpdateException {
            for (RegistryEntry parentEntry : entries) {
                try {

                    if (parentEntry.isSingleton()) {
                        SingletonElement parentCE = serverConfig.getSingleton(parentEntry.getPid(), parentEntry.getAlias());
                        if (parentCE != null) {
                            ConfigurationInfo info = changeHandler.createConfigurationInfo(parentCE, parentEntry);
                            addIfUnique(infos, info);
                        }
                    } else {
                        Map<ConfigID, FactoryElement> parentFactoryInstances = serverConfig.getFactoryInstancesUsingDefaultId(parentEntry.getPid(), parentEntry.getAlias(),
                                                                                                                              parentEntry.getDefaultId());
                        for (Map.Entry<ConfigID, FactoryElement> entry : parentFactoryInstances.entrySet()) {
                            ConfigurationInfo info = changeHandler.createConfigurationInfo(entry.getValue(), parentEntry);
                            addIfUnique(infos, info);
                        }
                    }

                } catch (ConfigNotFoundException ex) {
                    throw new ConfigUpdateException(ex);
                }
            }
        }

        /**
         * @param pid
         * @return
         * @throws ConfigUpdateException
         */
        private ConfigurationInfo processSingletonInstances(SingletonElement configElement, RegistryEntry registry) throws ConfigUpdateException {
            String pid = registry.getPid();

            try {
                if (configElement == null) {
                    if (registry.getObjectClassDefinition().hasAllRequiredDefaults()) {
                        String nodeName = registry.getAlias() == null ? pid : registry.getAlias();
                        configElement = new SingletonElement(nodeName, pid);
                    } else {
                        return null;
                    }
                } else if (!configElement.isEnabled()) {
                    return null;
                }

                validator.validateSingleton(pid, registry.getAlias());

                //remove unaliased configuration
                if (!pid.equals(registry.getAlias())) {
                    ExtendedConfiguration aliased = configRetriever.findConfiguration(registry.getAlias());
                    if (aliased != null && !aliased.isDeleted()) {
                        // This may have already been deleted by nested element processing
                        aliased.delete(false);
                    }
                }

                return changeHandler.createConfigurationInfo(configElement, registry);
            } catch (ConfigNotFoundException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "processInstances(): Exception while processing bundle/version/pid=" + updateName + "/" + pid + ".  Message=" + e.getMessage());

                if (ErrorHandler.INSTANCE.fail()) {
                    Tr.error(tc, "error.parse.bundle", new Object[] { updateName, pid, e.getMessage() });
                    throw new ConfigUpdateException(e);
                }
                return null;
            }
        }

        /**
         * @param registry
         * @param instances
         * @throws ConfigUpdateException
         */
        private void processFactoryInstances(RegistryEntry registry, Map<ConfigID, FactoryElement> instances) throws ConfigUpdateException {
            String pid = registry.getPid();

            for (Map.Entry<ConfigID, FactoryElement> entry : instances.entrySet()) {
                try {
                    FactoryElement configElement = entry.getValue();
                    if (!configElement.isEnabled()) {
                        continue;
                    }

                    validator.validateFactoryInstance(pid, registry.getAlias(), entry.getKey());

                    ConfigurationInfo info = changeHandler.createConfigurationInfo(configElement, registry);
                    addIfUnique(infos, info);
                } catch (ConfigNotFoundException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "processFactoryInstances(): Exception while processing bundle/version/pid=" + updateName + "/" + pid + ".  Message="
                                     + e.getMessage());

                    if (ErrorHandler.INSTANCE.fail()) {
                        Tr.error(tc, "error.parse.bundle", new Object[] { updateName, pid, e.getMessage() });
                        throw new ConfigUpdateException(e);
                    }
                }
            }
        }

        private void addIfUnique(List<ConfigurationInfo> list, ConfigurationInfo info) {
            boolean insert = true;
            for (ConfigurationInfo infoAlreadyInList : list) {
                if (info.matches(infoAlreadyInList)) {
                    insert = false;
                    break;
                }
            }
            if (insert) {
                list.add(info);
            }
        }
    }

    private class ExtendedBundle {
        private final Bundle bundle;
        private final AtomicLong lastProcessed = new AtomicLong(Long.MIN_VALUE);
        private final String nameAndVersion;
        private final AtomicBoolean hasDefaultConfig = new AtomicBoolean(true);

        public ExtendedBundle(Bundle b) {
            this.bundle = b;
            Version ver = b.getVersion();
            if (ver == null)
                ver = Version.emptyVersion;
            this.nameAndVersion = b.getSymbolicName() + "_" + ver.toString();
        }

        public ExtendedBundle(Bundle b, long lastProcessed, String nameAndVersion, boolean hasDefaultConfig) {
            this.bundle = b;
            this.lastProcessed.set(lastProcessed);
            this.nameAndVersion = nameAndVersion;
            this.hasDefaultConfig.set(hasDefaultConfig);
        }

        public String getNameAndVersion() {
            return this.nameAndVersion;
        }

        public long getLastProcessed() {
            return lastProcessed.get();
        }

        public boolean hasDefaultConfig() {
            return hasDefaultConfig.get();
        }

        public boolean needsReprocessing() {
            return lastProcessed.get() != bundle.getLastModified();
        }

        public Bundle getBundle() {
            return bundle;
        }

        public void setHasDefaultConfig(boolean hasDefaultConfig) {
            this.hasDefaultConfig.set(hasDefaultConfig);
        }

        public void updateLastProcessed() {
            this.lastProcessed.set(bundle.getLastModified());
        }
    }

    @FFDCIgnore(Exception.class)
    private void quit(Exception cause) {
        Tr.audit(tc, "frameworkShutdown", locationService.getServerName());

        try {
            Bundle bundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if (bundle != null)
                bundle.stop();
        } catch (Exception e) {
            // Exception could happen here if bundle context is bad, or system bundle
            // is already stopping: not an exceptional condition, as we
            // want to shutdown anyway.
        }
    }

    private void handleConfigUpdateException(ConfigUpdateException e) {
        Tr.error(tc, "error.config.update.init", e.getMessage());
        if (ErrorHandler.INSTANCE.getOnError().equals(OnError.FAIL)) {
            quit(e);
        }
    }

    private void handleConfigValidationException(ConfigValidationException cve) {
        if (!cve.docLocation.isEmpty()) {
            Tr.fatal(tc, "fatal.configValidator.documentNotValid", cve.docLocation);
        }
        quit(cve);
    }

}
