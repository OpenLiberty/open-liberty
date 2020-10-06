/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.internal.ConfigDelta.REASON;
import com.ibm.ws.config.xml.internal.ConfigElement.Reference;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;

/**
 * Computes delta between two server configurations.
 */
public class ConfigComparator {

    private final BaseConfiguration oldConfiguration;
    private final BaseConfiguration newConfiguration;
    private final MetaTypeRegistry metatypeRegistry;
    private RegistryEntry parentRegistryEntry;
    private final Map<String, DeltaType> serviceBindingVariableChanges;

    public ConfigComparator(BaseConfiguration oldConfiguration, BaseConfiguration newConfiguration, MetaTypeRegistry registry) {
        this(oldConfiguration, newConfiguration, registry, null);
    }

    public ConfigComparator(BaseConfiguration oldConfiguration, BaseConfiguration newConfiguration, MetaTypeRegistry registry, Map<String, DeltaType> variableDelta) {
        this.oldConfiguration = oldConfiguration;
        this.newConfiguration = newConfiguration;
        this.metatypeRegistry = registry;
        this.serviceBindingVariableChanges = variableDelta;
    }

    private RegistryEntry getRegistry(RegistryEntry parent, String childNodeName) {
        if (metatypeRegistry == null)
            return null;

        RegistryEntry entry = null;
        // Attempt to look up the registry entry by childAlias value
        RegistryEntry pe = parent;
        while (pe != null) {
            // The parent entry must have ibm:supportsExtensions defined for ibm:childAlias to be valid here
            if (pe.getObjectClassDefinition().supportsExtensions()) {
                RegistryEntry childAliasEntry = metatypeRegistry.getRegistryEntry(pe.getPid(), childNodeName);
                if (childAliasEntry != null)
                    return childAliasEntry;
            }
            pe = pe.getExtendedRegistryEntry();
        }

        return (entry == null) ? metatypeRegistry.getRegistryEntryByPidOrAlias(childNodeName) : entry;
    }

    public ComparatorResult computeDelta() throws ConfigUpdateException {
        Map<String, DeltaType> variableDelta = computeVariableDelta();
        List<ConfigDelta> configDelta = computeConfigDelta(variableDelta);
        return new ComparatorResult(configDelta, variableDelta);
    }

    private List<ConfigDelta> computeConfigDelta(Map<String, DeltaType> variableDelta) throws ConfigMergeException {
        List<ConfigDelta> delta = new ArrayList<ConfigDelta>();

        Set<String> ignorePids = new HashSet<String>();

        Set<String> oldPids = oldConfiguration.getConfigurationNames();
        for (String pid : oldPids) {
            if (ignorePids.contains(pid)) {
                continue;
            }

            RegistryEntry registry = getRegistry(parentRegistryEntry, pid);
            if (registry == null) {
                boolean oldIsFactory = oldConfiguration.hasId(pid);

                if (oldIsFactory) {
                    computeFactoryDelta(pid, null, null, delta, variableDelta);
                } else {
                    computeSingletonDelta(pid, null, null, delta, variableDelta);
                }

            } else {
                if (registry.isSingleton()) {
                    computeSingletonDelta(registry.getPid(), registry.getAlias(), registry, delta, variableDelta);
                } else {
                    computeFactoryDelta(registry.getPid(), registry.getAlias(), registry, delta, variableDelta);
                }
                // add pid & alias to ignore list so that we don't generate
                // multiple delta for the same config element
                ignorePids.add(registry.getPid());
                ignorePids.add(registry.getAlias());
            }
        }

        // process new configurations
        Set<String> remainingPids = new HashSet<String>(newConfiguration.getConfigurationNames());
        remainingPids.removeAll(oldPids);
        for (String pid : remainingPids) {
            if (ignorePids.contains(pid)) {
                continue;
            }

            RegistryEntry registry = getRegistry(parentRegistryEntry, pid);
            if (registry == null) {
                boolean isFactory = newConfiguration.hasId(pid);
                if (isFactory) {
                    newFactoryDelta(pid, null, null, delta);
                } else {
                    newSingletonDelta(pid, null, null, delta);
                }
            } else {
                if (registry.isSingleton()) {
                    newSingletonDelta(registry.getPid(), registry.getAlias(), registry, delta);
                } else {
                    newFactoryDelta(registry.getPid(), registry.getAlias(), registry, delta);
                }
                // add pid & alias to ignore list so that we don't generate
                // multiple delta for the same config element
                ignorePids.add(registry.getPid());
                ignorePids.add(registry.getAlias());
            }
        }

        // TODO: to be complete we might have to look for variables in metatype singletons that we didn't process before.

        return delta;
    }

    private void computeSingletonDelta(String pid, String alias, RegistryEntry registryEntry, List<ConfigDelta> delta,
                                       Map<String, DeltaType> variableDelta) throws ConfigMergeException {
        if (registryEntry != null && registryEntry.getChildAlias() != null)
            alias = registryEntry.getChildAlias();

        SingletonElement oldConfig = oldConfiguration.getSingleton(pid, alias);
        SingletonElement newConfig = newConfiguration.getSingleton(pid, alias);
        ConfigDelta configDelta = computeDelta(oldConfig, newConfig, registryEntry, variableDelta);
        if (configDelta != null) {
            delta.add(configDelta);
        }
    }

    private void computeFactoryDelta(String pid, String alias, RegistryEntry registryEntry, List<ConfigDelta> delta,
                                     Map<String, DeltaType> variableDelta) throws ConfigMergeException {
        String defaultId = null;
        if (registryEntry != null) {
            if (registryEntry.getChildAlias() != null) {
                alias = registryEntry.getChildAlias();
            }

            defaultId = registryEntry.getDefaultId();

        }

        Map<ConfigID, FactoryElement> oldInstances = oldConfiguration.getFactoryInstancesUsingDefaultId(pid, alias, defaultId);
        Map<ConfigID, FactoryElement> newInstances = newConfiguration.getFactoryInstancesUsingDefaultId(pid, alias, defaultId);

        // compute delta for any instances in old configuration
        for (Map.Entry<ConfigID, FactoryElement> oldEntry : oldInstances.entrySet()) {
            FactoryElement oldConfig = oldEntry.getValue();
            FactoryElement newConfig = newInstances.get(oldEntry.getKey());

            ConfigDelta configDelta = computeDelta(oldConfig, newConfig, registryEntry, variableDelta);
            if (configDelta != null) {
                delta.add(configDelta);
            }
        }

        // process any newly added instances in new configuration
        Set<ConfigID> remainingInstanceIds = new HashSet<ConfigID>(newInstances.keySet());
        remainingInstanceIds.removeAll(oldInstances.keySet());
        for (ConfigID id : remainingInstanceIds) {
            ConfigElement newConfig = newInstances.get(id);
            ConfigDelta configDelta = computeDelta(null, newConfig, registryEntry, variableDelta);
            if (configDelta != null) {
                delta.add(configDelta);
            }
        }
    }

    private void newSingletonDelta(String pid, String alias, RegistryEntry registryEntry, List<ConfigDelta> delta) throws ConfigMergeException {
        SingletonElement newConfig = newConfiguration.getSingleton(pid, alias);
        ConfigDelta configDelta = computeDelta(null, newConfig, registryEntry, null);
        if (configDelta != null) {
            delta.add(configDelta);
        }
    }

    private void newFactoryDelta(String pid, String alias, RegistryEntry registryEntry, List<ConfigDelta> delta) throws ConfigMergeException {
        String defaultId = null;
        if (registryEntry != null) {
            if (registryEntry.getChildAlias() != null) {
                alias = registryEntry.getChildAlias();
            }

            defaultId = registryEntry.getDefaultId();

        }

        Map<ConfigID, FactoryElement> instances = newConfiguration.getFactoryInstancesUsingDefaultId(pid, alias, defaultId);
        for (FactoryElement instance : instances.values()) {
            ConfigDelta configDelta = computeDelta(null, instance, registryEntry, null);
            if (configDelta != null) {
                delta.add(configDelta);
            }
        }
    }

    private ConfigDelta computeDelta(ConfigElement oldConfig, ConfigElement newConfig, RegistryEntry registryEntry,
                                     Map<String, DeltaType> variableDelta) throws ConfigMergeException {
        ConfigElement oldElement = (oldConfig == null || !oldConfig.isEnabled()) ? null : oldConfig;
        ConfigElement newElement = (newConfig == null || !newConfig.isEnabled()) ? null : newConfig;

        List<ConfigDelta> nestedDelta = null;
        if ((oldElement != null && oldElement.hasNestedElements()) || (newElement != null && newElement.hasNestedElements())) {
            BaseConfiguration oldNestedConfiguration = buildConfiguration(oldElement, registryEntry);
            BaseConfiguration newNestedConfiguration = buildConfiguration(newElement, registryEntry);
            ConfigComparator nestedComparator = new ConfigComparator(oldNestedConfiguration, newNestedConfiguration, metatypeRegistry);
            nestedComparator.setParent(registryEntry);
            nestedDelta = nestedComparator.computeConfigDelta(variableDelta);
        }

        if (oldElement == null) {
            if (newElement == null) {
                return null;
            } else {
                return new ConfigDelta(newElement, DeltaType.ADDED, nestedDelta, registryEntry, REASON.PROPERTIES_UPDATE);
            }
        }

        if (newElement == null) {
            ConfigElement removedConfig = newConfig == null ? oldConfig : newConfig;
            return new ConfigDelta(removedConfig, DeltaType.REMOVED, nestedDelta, registryEntry, REASON.PROPERTIES_UPDATE);
        }

        if (!compare(oldElement, newElement) ||
            hasChangedVariables(newElement, registryEntry, variableDelta)) {
            // If either properties or variables have changed, process an update
            return new ConfigDelta(newElement, DeltaType.MODIFIED, nestedDelta, registryEntry, REASON.PROPERTIES_UPDATE);
        } else if (nestedDelta != null && !nestedDelta.isEmpty()) {
            // We produce a delta in this case because we update parents when
            // nested elements change (unless hideFromParent is specified.)
            return new ConfigDelta(newElement, DeltaType.MODIFIED, nestedDelta, registryEntry, REASON.NESTED_UPDATE_ONLY);
        } else {
            // No changes
            return null;
        }
    }

    /**
     * @param registryEntry
     */
    private void setParent(RegistryEntry registryEntry) {
        this.parentRegistryEntry = registryEntry;
    }

    private boolean compare(ConfigElement obj1, ConfigElement obj2) {
        if (!obj1.getNodeName().equals(obj2.getNodeName())) {
            return false;
        }

        if (obj1.getId() == null) {
            if (obj2.getId() != null) {
                return false;
            }
        } else if (!obj1.getId().equals(obj2.getId())) {
            return false;
        }

        if (obj1.getAttributes() == null) {
            if ((obj2.getAttributes() != null) && (!obj2.getAttributes().isEmpty())) {
                return false;
            }
        } else {
            return compareAttributes(obj1.getAttributes(), obj2.getAttributes());
        }

        return true;
    }

    /**
     * @param attributes
     * @param attributes2
     * @return
     */
    private boolean compareAttributes(Map<String, Object> attr1, Map<String, Object> attr2) {

        if (attr1 == attr2)
            return true;
        if (attr1.size() != attr2.size())
            return false;
        for (Map.Entry<String, Object> entry : attr1.entrySet()) {
            String key = entry.getKey();
            if (!attr2.containsKey(key))
                return false;

            Object value1 = entry.getValue();
            Object value2 = attr2.get(key);

            if (value2 == null) {
                if (value1 == null)
                    return true;
                return false;
            }

            if ((value1 instanceof List) && (value2 instanceof List)) {
                @SuppressWarnings("unchecked")
                List<Object> list1 = (List<Object>) value1;
                @SuppressWarnings("unchecked")
                List<Object> list2 = (List<Object>) value2;
                if (!compareLists(list1, list2))
                    return false;
            } else if (!value2.equals(value1))
                return false;
        }

        return true;
    }

    /**
     * @param value1
     * @param value2
     * @return
     */
    private boolean compareLists(List<Object> value1, List<Object> value2) {
        if (value1.size() != value2.size())
            return false;

        for (int i = 0; i < value1.size(); i++) {
            Object o1 = value1.get(i);
            Object o2 = value2.get(i);
            if (o1 == null) {
                if (o2 != null)
                    return false;
            } else if ((o1 instanceof ConfigElement) && (o2 instanceof ConfigElement)) {
                ConfigElement one = (ConfigElement) o1;
                ConfigElement two = ((ConfigElement) o2);
                if (!one.getNodeName().equals(two.getNodeName()))
                    return false;
                if (one.getId() != null) {
                    if (two.getId() == null)
                        return false;
                    if (!one.getId().equals(two.getId()))
                        return false;
                }
            } else {
                if (!o1.equals(o2))
                    return false;
            }
        }
        return true;
    }

    private boolean hasChangedVariables(ConfigElement configElement, RegistryEntry registryEntry, Map<String, DeltaType> variableDelta) {
        if (variableDelta == null || variableDelta.isEmpty()) {
            return false;
        }
        // step 1: examine all attributes first
        for (Map.Entry<String, Object> attributeEntry : configElement.getAttributes().entrySet()) {
            Object attributeValue = attributeEntry.getValue();
            if (attributeValue instanceof String) {
                if (hasVariable((String) attributeValue, variableDelta)) {
                    return true;
                }
            } else if (attributeValue instanceof List<?>) {
                List<?> values = (List<?>) attributeValue;
                for (Object value : values) {
                    if (value instanceof String) {
                        if (hasVariable((String) value, variableDelta)) {
                            return true;
                        }
                    } else if (value instanceof Reference) {
                        if (hasVariable(((Reference) value).getId(), variableDelta)) {
                            return true;
                        }
                    } else if (value instanceof ConfigElement) {
                        if (hasVariable(((ConfigElement) value).getId(), variableDelta)) {
                            return true;
                        }
                    } else {
                        throw new IllegalStateException("Unexpected attribute type: " + value.getClass());
                    }
                }
            }
        }
        // step 2: examine metatype info
        if (registryEntry != null) {
            Map<String, ExtendedAttributeDefinition> metaTypeAttributes = registryEntry.getObjectClassDefinition().getAttributeMap();
            for (Map.Entry<String, ExtendedAttributeDefinition> attributeEntry : metaTypeAttributes.entrySet()) {
                String attributeName = attributeEntry.getKey();
                // skip attributes we already processed before
                if (configElement.containsAttribute(attributeName)) {
                    continue;
                }
                // check ibm:variable first
                ExtendedAttributeDefinition attribute = attributeEntry.getValue();
                String variable = attribute.getVariable();
                if (variable != null && variableDelta.containsKey(variable)) {
                    return true;
                }
                // check for variables in default values
                String[] defaultValue = attribute.getDefaultValue();
                if (defaultValue != null && defaultValue.length > 0) {
                    for (String value : defaultValue) {
                        if (hasVariable(value, variableDelta)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean hasVariable(String value, Map<String, DeltaType> variableDelta) {
        Matcher matcher = XMLConfigConstants.VAR_PATTERN.matcher(value);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (variableDelta.containsKey(variable)) {
                return true;
            }
        }
        return false;
    }

    private ExtendedAttributeDefinition getAttributeDefinition(String attribute, Map<String, ExtendedAttributeDefinition> attributes) {
        ExtendedAttributeDefinition attrDef = null;
        if (attributes != null) {
            attrDef = attributes.get(attribute);
            if (attrDef == null) {
                if (attribute.endsWith(XMLConfigConstants.CFG_REFERENCE_SUFFIX)) {
                    attribute = attribute.substring(0, attribute.length() - XMLConfigConstants.CFG_REFERENCE_SUFFIX.length());
                } else {
                    attribute = attribute + XMLConfigConstants.CFG_REFERENCE_SUFFIX;
                }
                attrDef = attributes.get(attribute);
            }
        }
        return attrDef;
    }

    private BaseConfiguration buildConfiguration(ConfigElement configElement, RegistryEntry registryEntry) {
        BaseConfiguration configuration = new BaseConfiguration();

        if (configElement != null) {
            Map<String, ExtendedAttributeDefinition> metaTypeAttributes = null;
            if (registryEntry != null) {
                metaTypeAttributes = registryEntry.getObjectClassDefinition().getAttributeMap();
            }
            for (Map.Entry<String, Object> attributeEntry : configElement.getAttributes().entrySet()) {
                String attributeName = attributeEntry.getKey();
                Object attributeValue = attributeEntry.getValue();
                if (attributeValue instanceof List) {
                    ExtendedAttributeDefinition attrDef = getAttributeDefinition(attributeName, metaTypeAttributes);
                    @SuppressWarnings("unchecked")
                    List<Object> values = (List<Object>) attributeValue;
                    for (int i = 0; i < values.size(); i++) {
                        if (values.get(i) instanceof SimpleElement) {
                            SimpleElement nested = (SimpleElement) values.get(i);
                            SimpleElement updated = null;
                            //TODO this almost works:
                            //It should work because it avoids creating configurations for nested config elements with no metatype,
                            //which can never possibly be used for anything.
                            /*
                             * if (attrDef != null && attrDef.getType() == MetaTypeFactory.PID_TYPE) {
                             * String pid = attrDef.getReferencePid();
                             *
                             * String defaultId = null;
                             * // RegistryEntry nestedRegistry = getRegistry(registryEntry, pid);
                             * RegistryEntry nestedRegistry = metatypeRegistry.getRegistryEntry(pid);
                             * if (nestedRegistry != null) {
                             * defaultId = nestedRegistry.getDefaultId();
                             * if (attrDef.getCardinality() == 0) {
                             * updated = new ComparableElement(nested, -1, pid, defaultId);
                             * } else {
                             * updated = new ComparableElement(nested, i, pid, defaultId);
                             * }
                             *
                             * updated.setChildAttributeName(attributeName);
                             * updated.setIdAttribute();
                             *
                             * // Recursively build configuration. Since we're creating new ConfigElements here,
                             * // any updates to the ID in nested elements from later delta processing will be
                             * // to a different object.
                             * // buildConfiguration(updated, getRegistry(pid));
                             * buildConfiguration(updated, nestedRegistry);
                             * }
                             * } else {
                             * //child first case
                             * RegistryEntry nestedRegistry = getRegistry(registryEntry, nested.getNodeName());
                             * if (nestedRegistry != null) {
                             * String defaultId = nestedRegistry.getDefaultId();
                             * updated = new ComparableElement(nested, i, nestedRegistry.getPid(), defaultId);
                             * //child-first must have metatype.
                             * // } else {
                             * // updated = new ComparableElement(nested, i, null, null);
                             * }
                             * }
                             *
                             * if (updated != null) {
                             * updated.setParent(configElement);
                             * configuration.addConfigElement(updated);
                             * values.set(i, updated);
                             * }
                             */
                            if (attrDef != null && attrDef.getType() == MetaTypeFactory.PID_TYPE) {
                                String pid = attrDef.getReferencePid();

                                String defaultId = null;
                                RegistryEntry nestedRegistryEntry = metatypeRegistry.getRegistryEntry(pid);
                                if (nestedRegistryEntry != null) {
                                    defaultId = nestedRegistryEntry.getDefaultId();
                                }
                                if (attrDef.getCardinality() == 0) {
                                    updated = new ComparableElement(nested, -1, pid, defaultId);
                                } else {
                                    updated = new ComparableElement(nested, i, pid, defaultId);
                                }

                                updated.setChildAttributeName(attributeName);
                                updated.setIdAttribute();

                                // Recursively build configuration. Since we're creating new ConfigElements here,
                                // any updates to the ID in nested elements from later delta processing will be
                                // to a different object.
                                buildConfiguration(updated, nestedRegistryEntry);
                            } else {
                                //child first case
                                RegistryEntry nestedRegistryEntry = getRegistry(registryEntry, nested.getNodeName());
                                if (nestedRegistryEntry != null) {
                                    String defaultId = nestedRegistryEntry.getDefaultId();
                                    updated = new ComparableElement(nested, i, nestedRegistryEntry.getPid(), defaultId);
                                } else {
                                    updated = new ComparableElement(nested, i, null, null);
                                }
                            }

                            updated.setParent(configElement);
                            configuration.addConfigElement(updated);
                            values.set(i, updated);

                        }
                    }
                }
            }
        }

        return configuration;
    }

    private Map<String, DeltaType> computeVariableDelta() throws ConfigUpdateException {
        // server.xml variables and file system variables can't change at the same time
        if (this.serviceBindingVariableChanges != null) {
            return this.serviceBindingVariableChanges;
        }

        Map<String, DeltaType> deltaMap = new HashMap<String, DeltaType>();

        LinkedList<String> stack = new LinkedList<String>();

        Map<String, LibertyVariable> newVariables = newConfiguration.getVariables();

        Map<String, LibertyVariable> oldVariables = oldConfiguration.getVariables();
        for (LibertyVariable oldVariable : oldVariables.values()) {
            String variableName = oldVariable.getName();
            DeltaType delta = compareVariable(oldVariables, newVariables, variableName, stack);
            if (delta != null) {
                deltaMap.put(variableName, delta);
            }
        }

        // process new variables
        for (LibertyVariable newVariable : newVariables.values()) {
            String variableName = newVariable.getName();
            if (oldVariables.containsKey(variableName)) {
                continue;
            }
            DeltaType delta = compareVariable(oldVariables, newVariables, variableName, stack);
            deltaMap.put(variableName, delta);
        }

        return deltaMap;
    }

    private DeltaType compareVariable(Map<String, LibertyVariable> oldVariables,
                                      Map<String, LibertyVariable> newVariables,
                                      String variableName,
                                      LinkedList<String> stack) throws ConfigUpdateException {
        if (stack.contains(variableName)) {
            throw new ConfigUpdateException("Variable loop detected: " + stack.subList(stack.indexOf(variableName), stack.size()));
        } else {
            stack.add(variableName);
        }

        LibertyVariable oldVariable = oldVariables.get(variableName);
        LibertyVariable newVariable = newVariables.get(variableName);

        DeltaType delta = null;

        if (oldVariable == null) {
            delta = (newVariable == null) ? null : DeltaType.ADDED;
        } else if (newVariable == null) {
            delta = DeltaType.REMOVED;
        } else if (oldVariable.getValue() != null && oldVariable.getValue().equals(newVariable.getValue())) {
            delta = compareVariableReferences(oldVariables, newVariables, oldVariable.getValue(), stack);
        } else if (oldVariable.getDefaultValue() != null && oldVariable.getDefaultValue().equals(newVariable.getDefaultValue())) {
            delta = compareVariableReferences(oldVariables, newVariables, oldVariable.getDefaultValue(), stack);
        } else {
            delta = DeltaType.MODIFIED;
        }

        stack.removeLast();

        return delta;
    }

    private DeltaType compareVariableReferences(Map<String, LibertyVariable> oldVariables,
                                                Map<String, LibertyVariable> newVariables,
                                                String variableValue,
                                                LinkedList<String> stack) throws ConfigUpdateException {
        Matcher matcher = XMLConfigConstants.VAR_PATTERN.matcher(variableValue);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (compareVariable(oldVariables, newVariables, variable, stack) != null) {
                return DeltaType.MODIFIED;
            }
        }
        return null;
    }

    public static enum DeltaType {
        ADDED, MODIFIED, REMOVED
    }

    public static class ComparatorResult {

        private final List<ConfigDelta> configDelta;
        private final Map<String, DeltaType> variableDelta;
        private ServerConfiguration newConfiguration;

        public ComparatorResult(List<ConfigDelta> configDelta, Map<String, DeltaType> variableDelta) {
            this.configDelta = configDelta;
            this.variableDelta = variableDelta;
        }

        public List<ConfigDelta> getConfigDelta() {
            return configDelta;
        }

        public Map<String, DeltaType> getVariableDelta() {
            return variableDelta;
        }

        public boolean hasDelta() {
            return !configDelta.isEmpty() || !variableDelta.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("ComparatorResult[");
            builder.append("configDelta=").append(configDelta).append(", ");
            builder.append("variableDelta=").append(variableDelta);
            builder.append("]");
            return builder.toString();
        }

        /**
         * @param newConfiguration
         */
        public void setNewConfiguration(ServerConfiguration newConfiguration) {
            this.newConfiguration = newConfiguration;

        }

        public ServerConfiguration getNewConfiguration() {
            return this.newConfiguration;
        }
    }
}
