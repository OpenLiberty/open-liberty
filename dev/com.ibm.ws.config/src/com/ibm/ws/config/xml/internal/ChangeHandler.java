/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.ibm.websphere.config.ConfigRetrieverException;
import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.SystemConfigSupport;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.internal.ConfigComparator.ComparatorResult;
import com.ibm.ws.config.xml.internal.ConfigComparator.DeltaType;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 *
 */
class ChangeHandler {

    static final TraceComponent tc = Tr.register(ChangeHandler.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private final SystemConfigSupport caSupport;
    private final ConfigVariableRegistry variableRegistry;
    private final ExtendedMetatypeManager extendedMetatypeManager;
    private final ConfigValidator validator;
    private final ConfigRetriever configRetriever;
    private final ConfigUpdater configUpdater;

    private final MetaTypeRegistry metatypeRegistry;

    public ChangeHandler(SystemConfigSupport caSupport, ConfigVariableRegistry vr,
                         ExtendedMetatypeManager emm,
                         ConfigRetriever retriever, ConfigValidator validator,
                         ConfigUpdater configUpdater, MetaTypeRegistry registry) {
        this.caSupport = caSupport;
        this.variableRegistry = vr;
        this.extendedMetatypeManager = emm;
        this.configRetriever = retriever;
        this.validator = validator;
        this.configUpdater = configUpdater;
        this.metatypeRegistry = registry;

    }

    public synchronized Collection<ConfigurationInfo> switchConfiguration(ServerXMLConfiguration serverXMLConfig,
                                                                          ComparatorResult result) throws ConfigUpdateException {
        serverXMLConfig.setNewConfiguration(result.getNewConfiguration());

        if (!result.getVariableDelta().isEmpty()) {
            configUpdater.updateSystemVariables(serverXMLConfig);
            if (result.getVariableDelta().containsKey(OnErrorUtil.CFG_KEY_ON_ERROR)) {
                LibertyVariable var = serverXMLConfig.getVariables().get(OnErrorUtil.CFG_KEY_ON_ERROR);
                if (var == null) {
                    ErrorHandler.INSTANCE.setOnError(OnErrorUtil.getDefaultOnError());
                } else {
                    OnError onErrorVal = OnError.valueOf(var.getValue().trim().toUpperCase());
                    ErrorHandler.INSTANCE.setOnError(onErrorVal);
                }
            }
        }

        List<ConfigDelta> deltas = result.getConfigDelta();

        Map<ConfigID, ConfigurationInfo> updatedConfigurations = new HashMap<ConfigID, ConfigurationInfo>();
        Map<ConfigID, ConfigurationInfo> deletedConfigurations = new HashMap<ConfigID, ConfigurationInfo>();

        processDelta(deltas, updatedConfigurations, deletedConfigurations, null);

        /*
         * Configurations should be updated in the right dependency order otherwise looking
         * up child configurations might return incomplete data (depending on timing).
         * The next two steps avoid the incomplete data problem by first saving the updated
         * configuration data to disk and then firing the "updated" events.
         */

        // Step 1. Save configuration info to disk

        Collection<ConfigurationInfo> newConfigurations = configUpdater.update(false, updatedConfigurations.values());

        // Clear out the list of updated configurations from delta processing, add in
        // the legitimately updated configurations
        updatedConfigurations.clear();
        for (ConfigurationInfo info : newConfigurations) {
            updatedConfigurations.put(info.configElement.getConfigID(), info);
        }

        configUpdater.processUnresolvedReferences(ErrorHandler.INSTANCE.getOnError());

        ConfigProcessor processor = new ConfigProcessor(serverXMLConfig.getConfiguration());

        // Step 2. Figure out dependent configurations and save them to disk
        Collection<ConfigurationInfo> dependentConfigurations = processor.getDependentConfigurations(deltas, updatedConfigurations);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Dependent configurations: " + dependentConfigurations);
        }

        // This update needs to happen because ref elements need to be reprocessed
        // However, we want to notify ALL dependent configurations, regardless of
        // whether there were property updates or not.
        dependentConfigurations = configUpdater.update(true, dependentConfigurations);

        for (ConfigurationInfo info : dependentConfigurations) {
            updatedConfigurations.put(info.configElement.getConfigID(), info);
        }

        // Create a List containing all updates. These should not be sorted -- they are sent out on different threads,
        // so no one should be relying on ordering behavior.
        Collection<ConfigurationInfo> updates = new ArrayList<ConfigurationInfo>(deletedConfigurations.values());
        updates.addAll(updatedConfigurations.values());

        return updates;
    }

    private void processDelta(List<ConfigDelta> deltas, Map<ConfigID, ConfigurationInfo> updatedConfigurations, Map<ConfigID, ConfigurationInfo> deletedConfigurations,
                              ConfigElement parent) throws ConfigUpdateException {
        for (ConfigDelta delta : deltas) {

            if (!delta.getNestedDelta().isEmpty()) {
                processDelta(delta.getNestedDelta(), updatedConfigurations, deletedConfigurations, delta.getConfigElement());
            }

            ConfigElement configElement = delta.getConfigElement();
            configElement.setParent(parent);

            DeltaType deltaType = delta.getDelta();
            RegistryEntry registry = delta.getRegistryEntry();
            ExtendedObjectClassDefinition ocd = (registry != null) ? registry.getObjectClassDefinition() : null;

            if (deltaType == DeltaType.REMOVED) {
                if (registry == null) {
                    ConfigurationInfo info = removeConfigAndCreateInfo(configElement, false);
                    if (info != null) {
                        deletedConfigurations.put(configElement.getConfigID(), info);
                    }
                } else {
                    // metatype info present
                    try {
                        ExtendedConfiguration configImpl = configRetriever.getConfiguration(configElement.getConfigID());
                        if (configImpl != null) {
                            //Remove the unique variable
                            removeUniqueVariables(configImpl, registry);
                            //Remove any supertypes -- make sure we're starting from the leaf
                            Dictionary<String, Object> properties = configImpl.getProperties();
                            String pid = null;
                            if (properties != null) {
                                pid = (String) properties.get(ExtendedMetatypeManager.EXT_SOURCE_PID_KEY);
                            }

                            if (pid == null)
                                pid = configImpl.getPid();
                            extendedMetatypeManager.deleteSuperType(pid);
                        }
                    } catch (ConfigNotFoundException ex) {
                        throw new ConfigUpdateException(ex);
                    }

                    // singleton can have some default values so if singleton is removed from server.xml,
                    // the configuration object is not removed but updated with default values
                    if (registry.isSingleton() && ocd.hasAllRequiredDefaults() && configElement.isEnabled() &&
                        (parent == null || (!parent.containsAttribute(registry.getAlias()) && !parent.containsAttribute(registry.getPid())))) {
                        SingletonElement oldElement = (SingletonElement) configElement;
                        SingletonElement newConfigElement = new SingletonElement(oldElement.getNodeName(), oldElement.pid);

                        try {
                            ConfigurationInfo info = createConfigurationInfo(newConfigElement, registry);
                            updatedConfigurations.put(newConfigElement.getConfigID(), info);
                        } catch (ConfigNotFoundException ex) {
                            throw new ConfigUpdateException(ex);
                        }
                    } else {
                        ConfigurationInfo info = removeConfigAndCreateInfo(configElement, false);
                        if (info != null) {
                            deletedConfigurations.put(configElement.getConfigID(), info);
                        }
                    }
                }
            } else {
                // do simple validation
                if (registry != null) {
                    String pid = registry.getPid();
                    String alias = registry.getAlias();
                    if (registry.isFactory()) {
                        validator.validateFactoryInstance(pid, alias, configElement.getConfigID());
                    } else {
                        validator.validateSingleton(pid, alias);
                    }
                }

                try {
                    ConfigurationInfo info = createConfigurationInfo(configElement, registry);
                    if (parent == null) {
                        // Only add it to the update list if it's a top level change. Everything nested under
                        // the parent will be evaluated and updated, so it won't be lost.
                        updatedConfigurations.put(configElement.getConfigID(), info);
                    }
                } catch (ConfigNotFoundException ex) {
                    throw new ConfigUpdateException(ex);
                }
            }
            if (parent != null) {
                configElement.setParent(parent);
            }
        }

    }

    /**
     * @param configElement
     * @param ocd
     */
    private void removeUniqueVariables(ExtendedConfiguration config, RegistryEntry registryEntry) {
        if (registryEntry != null) {
            for (Map.Entry<String, ExtendedAttributeDefinition> entry : registryEntry.getAttributeMap().entrySet()) {
                ExtendedAttributeDefinition attrDef = entry.getValue();
                if (attrDef.isUnique()) {
                    //Fetch the attribute value using the property map in the configuration (will get an NPE
                    //if the key is null, so must check first)
                    Object value = attrDef.getID() == null ? null : config.getProperty(attrDef.getID());
                    String attributeValue = value == null ? null : String.valueOf(value);

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "removing value from config for unique attribute[" + attrDef.getID() + "]: " + attributeValue);
                    }

                    //Remove the attribute from the registry
                    variableRegistry.removeUniqueVariable(attrDef, attributeValue);
                }
            }
        }

    }

    /**
     * @param removedConfig
     * @return
     * @throws ConfigUpdateException
     */
    protected synchronized void removeDefaultConfiguration(BaseConfiguration oldConfig, ServerXMLConfiguration serverConfig) throws ConfigUpdateException {
        ConfigComparator cc = new ConfigComparator(oldConfig, serverConfig.getConfiguration(), metatypeRegistry);
        ComparatorResult cr = cc.computeDelta();
        if (cr.hasDelta()) {
            cr.setNewConfiguration(serverConfig.getConfiguration());
            Collection<ConfigurationInfo> configurations = switchConfiguration(serverConfig, cr);
            if (configurations != null) {
                Collection<Future<?>> futures = new ArrayList<Future<?>>();
                for (ConfigurationInfo info : configurations) {
                    // create futures for configuration update events
                    info.fireEvents(futures);
                }
            }
        }

    }

    /**
     * @param sc
     * @param updatedPids
     */
    public Set<String> removeMetatypeConvertedConfig(ServerConfiguration sc, Set<RegistryEntry> updatedEntries) {
        Set<String> updates = new HashSet<String>();

        for (RegistryEntry entry : updatedEntries) {
            removeMetatypeConvertedConfig(sc, entry);
            updates.add(entry.getPid());
        }

        return updates;

    }

    /**
     * @param name
     * @return
     */
    private void removeMetatypeConvertedConfig(BaseConfiguration config, RegistryEntry entry) {
        try {
            if (entry.isFactory()) {
                Map<ConfigID, FactoryElement> map = config.getFactoryInstancesUsingDefaultId(entry.getPid(), entry.getAlias(),
                                                                                             entry.getDefaultId());
                for (FactoryElement element : map.values()) {
                    ExtendedConfiguration ec = findConfigurationToRemove(element);
                    if (ec != null) {
                        removeUniqueVariables(ec, entry);

                        //Remove any supertypes
                        extendedMetatypeManager.deleteSuperType(ec.getPid());

                        // delete and notify
                        ec.delete(true);
                    }
                }

            } else {
                SingletonElement element = config.getSingleton(entry.getPid(), entry.getAlias());
                ExtendedConfiguration ec = findConfigurationToRemove(element);
                removeUniqueVariables(ec, entry);
                //Remove any supertypes
                extendedMetatypeManager.deleteSuperType(ec.getPid());

                ec.delete(true);

            }
        } catch (ConfigMergeException ex) {
            // Should never happen
            ex.getStackTrace();
        }
    }

    protected ConfigurationInfo removeConfigAndCreateInfo(ConfigElement configElement, boolean notify) {
        ExtendedConfiguration config = findConfigurationToRemove(configElement);
        if (config == null)
            return null;

        config.delete(notify);

        return new ConfigurationInfo(configElement, config, null, true);
    }

    private ExtendedConfiguration findConfigurationToRemove(ConfigElement configElement) {

        try {
            ExtendedConfiguration[] configs = configRetriever.findConfigurations(configElement.getConfigID());

            if (configs != null) {
                if (configs.length == 1) {
                    return configs[0];
                } else if (configs.length > 1) {
                    if (tc.isWarningEnabled()) {
                        Tr.warning(tc, "warn.config.delete.failed.multiple", configElement.getDisplayId());
                    }
                }
            }
        } catch (ConfigRetrieverException e) {
            e.getCause();
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "warn.config.delete.failed", configElement.getDisplayId());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Configuration not found for: " + configElement.getConfigID());
        }
        return null;
    }

    protected ConfigurationInfo createConfigurationInfo(ConfigElement configElement, RegistryEntry registryEntry) throws ConfigNotFoundException {
        // Get configuration, creating if necessary
        ExtendedConfiguration config = configRetriever.getConfiguration(configElement.getConfigID());
        ConfigurationInfo info = new ConfigurationInfo(configElement, config, registryEntry, false);
        extendedMetatypeManager.createSuperTypes(info);
        return info;
    }

    public void updateAtStartup(ServerConfiguration serverConfiguration) throws ConfigUpdateException {
        List<ConfigurationInfo> infos = getConfigurationsToPopulate(serverConfiguration);
        configUpdater.update(true, infos);
    }

    @FFDCIgnore(ConfigNotFoundException.class)
    private List<ConfigurationInfo> getConfigurationsToPopulate(ServerConfiguration serverConfiguration) throws ConfigUpdateException {
        // Step 1: Get all existing configurations (if any)

        ExtendedConfiguration[] oldConfigs = null;
        try {
            oldConfigs = configRetriever.listAllConfigurations();
        } catch (Exception e) {
            throw new ConfigUpdateException(e);
        }

        Map<String, ExtendedConfiguration> oldConfigurations = Collections.emptyMap();
        if (oldConfigs != null) {
            oldConfigurations = new HashMap<String, ExtendedConfiguration>(oldConfigs.length);
            for (ExtendedConfiguration config : oldConfigs) {
                oldConfigurations.put(config.getPid(), config);
                for (String oldVar : config.getUniqueVariables()) {
                    variableRegistry.removeVariable(oldVar);
                }
            }
        }

        // Step 2: Collected all new and existing configurations (for top level or nested configs)
        List<ConfigurationInfo> infos = new ArrayList<ConfigurationInfo>();
        List<ExtendedConfiguration> newConfigurations = new ArrayList<ExtendedConfiguration>();

        for (String pid : serverConfiguration.getSingletonNames()) {
            SingletonElement configElement = serverConfiguration.getSingleton(pid, null);
            if (!configElement.isEnabled()) {
                continue;
            }
            try {
                ConfigurationInfo info = createConfigurationInfo(configElement, null);
                infos.add(info);
                newConfigurations.add(info.config);
                configRetriever.collectConfigurations(configElement, newConfigurations);
            } catch (ConfigNotFoundException e) {
                throw new ConfigUpdateException(e);
            }
        }

        for (String factoryPid : serverConfiguration.getFactoryNames()) {
            Map<ConfigID, FactoryElement> factoryInstances = serverConfiguration.getFactoryInstances(factoryPid, null);
            for (FactoryElement configElement : factoryInstances.values()) {
                if (!configElement.isEnabled()) {
                    continue;
                }
                try {
                    ConfigurationInfo info = createConfigurationInfo(configElement, null);
                    infos.add(info);
                    newConfigurations.add(info.config);
                    ExtendedConfiguration superConfig = (ExtendedConfiguration) extendedMetatypeManager.getSuperTypeConfig(info.config.getPid());
                    if (superConfig != null) {
                        newConfigurations.add(superConfig);
                    }
                    configRetriever.collectConfigurations(configElement, newConfigurations);
                } catch (ConfigNotFoundException ex) {
                    throw new ConfigUpdateException(ex);
                }
            }
        }

        // Step 3: Delete configurations that don't exist anymore
        if (!oldConfigurations.isEmpty()) {
            for (ExtendedConfiguration config : newConfigurations) {
                oldConfigurations.remove(config.getPid());
            }

            for (ExtendedConfiguration config : oldConfigurations.values()) {
                if (config.isInOverridesFile()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Deleted configuration: " + ErrorHandler.INSTANCE.toTraceString(config, null));
                    }
                    extendedMetatypeManager.removeSuperTypeForPid(config.getPid());
                    config.delete(false);
                }
            }
        }

        return infos;
    }

    /**
     * ConfigProcessor performs two functions:
     * 1) Computes dependent configurations based on configuration delta.
     * 2) Sorts configurations based on dependency order.
     */
    private class ConfigProcessor {

        private final ServerConfiguration configuration;

        /**
         * @param configuration
         */
        public ConfigProcessor(ServerConfiguration configuration) {
            this.configuration = configuration;
        }

        private RegistryEntry getRegistryEntry(String name) {
            return (metatypeRegistry != null) ? metatypeRegistry.getRegistryEntry(name) : null;
        }

        /*
         * Resolves ConfigID. The returned array will contain one or two ConfigIDs. The first
         * element is ConfigID with fully resolved PID and second element is ConfigID for
         * the alias/child alias (if alias/child alias is defined).
         */
        private ConfigID[] resolveConfigID(ConfigID configId, RegistryEntry registryEntry) {
            if (registryEntry == null) {
                return new ConfigID[] { configId };
            } else {
                if (configId.getPid().equals(registryEntry.getPid())) {
                    String alias = registryEntry.getChildAlias() == null ? registryEntry.getAlias() : registryEntry.getChildAlias();
                    if (alias == null) {
                        return new ConfigID[] { configId };
                    } else {
                        // pids are the same, add entry for alias
                        ConfigID pid = configId;
                        ConfigID aliasID = new ConfigID(registryEntry.getAlias(), configId.getId());
                        return new ConfigID[] { pid, aliasID };
                    }
                } else {
                    // pids are different so must be an alias
                    ConfigID pid = new ConfigID(registryEntry.getPid(), configId.getId());
                    ConfigID alias = configId;
                    return new ConfigID[] { pid, alias };
                }
            }
        }

        public Collection<ConfigurationInfo> getDependentConfigurations(List<ConfigDelta> deltas,
                                                                        Map<ConfigID, ConfigurationInfo> updatedConfigurations) throws ConfigUpdateException {
            Set<ConfigID> visited = new HashSet<ConfigID>();
            List<ConfigurationInfo> dependentConfigurations = new ArrayList<ConfigurationInfo>();

            for (ConfigDelta delta : deltas) {
                ConfigElement configElement = delta.getConfigElement();
                ConfigID configId = configElement.getConfigID();
                RegistryEntry registry = delta.getRegistryEntry();
                ConfigID[] resolvedIds = resolveConfigID(configId, registry);
                processReferences(resolvedIds, dependentConfigurations, updatedConfigurations, visited);
            }

            return dependentConfigurations;
        }

        private void processReferences(ConfigID[] configIds,
                                       List<ConfigurationInfo> dependentConfigurations,
                                       Map<ConfigID, ConfigurationInfo> updatedConfigurations,
                                       Set<ConfigID> visited) throws ConfigUpdateException {
            for (ConfigID configId : configIds) {
                processReferences(configId, dependentConfigurations, updatedConfigurations, visited);
            }
        }

        private void processReferences(ConfigID configId,
                                       List<ConfigurationInfo> dependentConfigurations,
                                       Map<ConfigID, ConfigurationInfo> updatedConfigurations,
                                       Set<ConfigID> visited) throws ConfigUpdateException {
            if (visited.contains(configId)) {
                return;
            }
            visited.add(configId);
            Set<ConfigID> referenceIds = caSupport.getReferences(configId);
            for (ConfigID referenceId : referenceIds) {

                // 0. resolve the pid of the reference
                RegistryEntry referenceRegistry = getRegistryEntry(referenceId.getPid());
                ConfigID[] resolvedIds = resolveConfigID(referenceId, referenceRegistry);

                // 1. process references of this reference
                processReferences(resolvedIds, dependentConfigurations, updatedConfigurations, visited);

                // 2. check if the reference id is already in modified configuration list
                if (updatedConfigurations.containsKey(resolvedIds[0])) {
                    continue;
                }

                // 3. get config element for the reference
                ConfigElement configElement = null;
                if (referenceRegistry == null) {
                    configElement = getConfigElement(resolvedIds[0].getPid(), resolvedIds[0].getId());
                } else {
                    configElement = getConfigElement(referenceRegistry, resolvedIds[0].getId());
                }

                if (configElement != null) {
                    try {
                        ConfigurationInfo info = createConfigurationInfo(configElement, referenceRegistry);
                        dependentConfigurations.add(info);
                    } catch (ConfigNotFoundException ex) {
                        throw new ConfigUpdateException(ex);
                    }
                }
            }
        }

        private ConfigElement getConfigElement(String name, String id) throws ConfigMergeException {
            if (id != null) {
                return configuration.getFactoryInstance(name, null, id);
            } else {
                return configuration.getSingleton(name, null);
            }

        }

        private ConfigElement getConfigElement(RegistryEntry registry, String id) throws ConfigMergeException {
            if (registry.isFactory()) {
                return configuration.getFactoryInstance(registry.getPid(), registry.getAlias(), id);
            } else {
                return configuration.getSingleton(registry.getPid(), registry.getAlias());
            }

        }

    }

}
