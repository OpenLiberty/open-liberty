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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.EntryAction;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.PidReference;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;

public class ServerConfiguration extends BaseConfiguration {

    private static final TraceComponent tc = Tr.register(ServerConfiguration.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    protected BaseConfiguration defaultConfiguration;

    public ServerConfiguration() {}

    public void setDefaultConfiguration(BaseConfiguration defaultConfiguration) {
        this.defaultConfiguration = defaultConfiguration;
    }

    public BaseConfiguration getDefaultConfiguration() {
        return defaultConfiguration;
    }

    @Override
    void getDefaultConfigurationNames(Set<String> names) {
        if (defaultConfiguration != null) {
            defaultConfiguration.getConfigurationNames(names);
        }
    }

    @Override
    boolean defaultConfigurationHasId(String pid) {
        if (defaultConfiguration == null) {
            return false;
        } else {
            return defaultConfiguration.hasId(pid);
        }
    }

    public boolean isDropinsEnabled() {

        // Default is that drop-ins are enabled
        boolean dropinsEnabled = true;

        // Determines if drop-ins have been disabled
        SingletonElement configElement;
        try {
            configElement = getSingleton("applicationMonitor", null);
        } catch (ConfigMergeException e) {
            // Really shouldn't happen.. just FFDC
            e.getStackTrace();
            return false;
        }
        if (configElement != null) {
            String dropinsEnabledValue = (String) configElement.getAttribute("dropinsEnabled");
            if (dropinsEnabledValue != null && dropinsEnabledValue.equals("false"))
                dropinsEnabled = false;
        }

        return dropinsEnabled;
    }

    public Set<String> getSingletonNames() {
        Set<String> singletons = new HashSet<String>();
        getSingletonNames(singletons);
        if (defaultConfiguration != null) {
            defaultConfiguration.getSingletonNames(singletons);
        }
        return singletons;
    }

    public Set<String> getFactoryNames() {
        Set<String> factories = new HashSet<String>();
        getFactoryNames(factories);
        if (defaultConfiguration != null) {
            defaultConfiguration.getFactoryNames(factories);
        }
        return factories;
    }

    @Override
    public SingletonElement getSingleton(String pid, String alias) throws ConfigMergeException {
        List<SimpleElement> elements = getSingletonElements(pid, alias);
        if (elements.isEmpty()) {
            if (defaultConfiguration == null) {
                return null;
            } else {
                return defaultConfiguration.getSingleton(pid, alias, false);
            }
        } else {
            List<SimpleElement> defaults = Collections.emptyList();
            if (defaultConfiguration != null)
                defaults = defaultConfiguration.getSingletonElements(pid, alias);

            if (defaults.isEmpty()) {
                SingletonElement merged = new SingletonElement(elements, pid);
                return merged;
            } else {
                // We have defined elements, so remove any defaults that are marked "merge when missing"
                Iterator<SimpleElement> iter = defaults.iterator();
                while (iter.hasNext()) {
                    if (iter.next().mergeBehavior == MergeBehavior.MERGE_WHEN_MISSING) {
                        iter.remove();
                    }
                }
                SingletonElement merged = new SingletonElement(defaults, pid);
                merged.merge(elements);
                return merged;
            }

        }
    }

    @Override
    public FactoryElement getFactoryInstance(String pid, String alias, String id) throws ConfigMergeException {
        List<SimpleElement> elements = getFactoryElements(pid, alias, id);
        if (elements.isEmpty()) {
            if (defaultConfiguration == null) {
                return null;
            } else {
                return defaultConfiguration.getFactoryInstance(pid, alias, id, false);
            }
        } else {

            if (defaultConfiguration != null) {
                List<SimpleElement> defaults = defaultConfiguration.getFactoryElements(pid, alias, id);
                if (defaults.isEmpty())
                    return new FactoryElement(elements, pid, id);
                else {
                    FactoryElement merged = new FactoryElement(defaults, pid, id);
                    merged.merge(elements);
                    return merged;
                }
            } else {
                return new FactoryElement(elements, pid, id);
            }
        }
    }

    @Override
    Map<ConfigID, List<SimpleElement>> defaultConfigurationFactories(String pid, String alias, String defaultId) {
        if (defaultConfiguration == null) {
            return Collections.emptyMap();
        } else {
            Map<ConfigID, List<SimpleElement>> retVal = defaultConfiguration.getAllFactoryElements(pid, alias, defaultId);
            return retVal == null ? Collections.<ConfigID, List<SimpleElement>> emptyMap() : retVal;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("configurations: ").append(configurationMap);
        if (defaultConfiguration != null) {
            builder.append(" default configurations: ").append(defaultConfiguration);
        }
        return builder.toString();
    }

    /**
     * Given a Pid and corresponding metatype registry entry, this finds all the ConfigElements with the parentPID specified in the registry entry
     * and then all the children with given (factory) pid of those parent instances.
     *
     * @param pid (factory) pid of a registry entry.
     *
     * @return list of all the instances of the supplied (factory) pid that are nested inside a parent of the parentPID from the registry entry.
     * @throws ConfigMergeException
     */
    public List<ConfigElement> getNestedInstances(RegistryEntry re) throws ConfigMergeException {
        return getNestedInstances(re, new HashSet<RegistryEntry>());
    }

    private List<ConfigElement> getNestedInstances(RegistryEntry re, final Set<RegistryEntry> visited) throws ConfigMergeException {
        String pid = re.getPid();
        final List<ConfigElement> retVal = new ArrayList<ConfigElement>();
        for (RegistryEntry test = re; test != null; test = test.getExtendedRegistryEntry()) {
            for (final PidReference parentRef : test.getReferencingEntries()) {

                final Set<ConfigElement> parentInstances = new HashSet<ConfigElement>();
                RegistryEntry parentEntry = parentRef.getReferencingEntry();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Looking for instances of {0} nested under {1}", pid, parentEntry);
                }

                if (parentEntry.isSingleton()) {
                    if (!visited.contains(parentEntry)) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Getting singleton instances of {0} using alias {1}", parentEntry, parentEntry.getAlias());
                        }
                        SingletonElement element = getSingleton(parentEntry.getPid(), parentEntry.getAlias());
                        if (element != null) {
                            parentInstances.add(element);
                        }
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "About to look for nested instances of parentPID {0}. Current parentInstances size: {1}", parentEntry, parentInstances.size());
                        }
                        visited.add(parentEntry);
                        parentInstances.addAll(getNestedInstances(parentEntry, visited));
                    }
                } else {
                    // Only use the alias here, not the child alias. We're looking for top level configurations only. Nested
                    // configurations are handled by the recursive call.
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Getting factory instances of {0} using alias {1}", parentEntry, parentEntry.getAlias());
                    }
                    ConfigMergeException e = parentEntry.traverseHierarchyWithRootPreOrder(new EntryAction<ConfigMergeException>() {

                        private ConfigMergeException e;

                        @Override
                        public boolean entry(RegistryEntry registryEntry) {
                            if (!visited.contains(registryEntry)) {
                                visited.add(registryEntry);
                                try {
                                    Map<ConfigID, FactoryElement> parentFactoryInstances = getFactoryInstancesUsingDefaultId(registryEntry.getPid(), registryEntry.getAlias(),
                                                                                                                             registryEntry.getDefaultId());
                                    parentInstances.addAll(parentFactoryInstances.values());
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "About to look for nested instances of parentPID {0}. Current parentInstances size: {1}", registryEntry,
                                                 parentInstances.size());
                                    }
                                    parentInstances.addAll(getNestedInstances(registryEntry, visited));
                                } catch (ConfigMergeException e) {
                                    this.e = e;
                                    return false;
                                }
                            }
                            return true;
                        }

                        @Override
                        public ConfigMergeException getResult() {
                            return e;
                        }
                    });
                    if (e != null) {
                        throw e;
                    }
                }

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Finished looking for nested instances of parentPID {0}. Current parentInstances size: {1}", parentEntry, parentInstances.size());
                }

                // Look through all possible parents to find children of the type we're looking for
                for (final ConfigElement parent : parentInstances) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Examing parent instance {0}", parent);
                    }

                    if (parentRef.isParentFirst()) {

                        String alias = re.getEffectiveAD(parentRef.getAccessor());
                        Object attr = parent.getAttribute(alias);
                        if ((attr != null) && (attr instanceof List)) {
                            List<?> children = (List<?>) attr;
                            for (Object o : children) {
                                if (o instanceof ConfigElement) {
                                    ConfigElement child = (ConfigElement) o;
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Found child {0} for parent {1} using alias {2}", child, parent, alias);
                                        Tr.debug(tc, "Creating NestedConfigElement with registry entry ", re);
                                    }
                                    retVal.add(new NestedConfigElement(child, parent, re));
                                }
                            }
                        }
                    } else {

                        // child first case.
                        String alias = parentRef.getAccessor();
                        if (!pid.equals(alias)) {
                            Object attr = parent.getAttribute(alias);
                            if ((attr != null) && (attr instanceof List)) {
                                @SuppressWarnings("unchecked")
                                List<Object> children = (List<Object>) attr;
                                for (int i = 0; i < children.size(); i++) {
                                    Object o = children.get(i);
                                    if (o instanceof ConfigElement) {
                                        ConfigElement child = (ConfigElement) o;
                                        if (tc.isDebugEnabled()) {
                                            Tr.debug(tc, "Found child {0} for parent {1} using alias {2}", child, parent, alias);
                                            Tr.debug(tc, "Creating NestedConfigElement with registry entry ", parentEntry);
                                        }
                                        retVal.add(new NestedConfigElement(child, parent, parentEntry));
                                    }
                                }
                            }
                        }
                        //TODO this code looks extremely odd and appears to add a parent rather than child????
                        //child-first case where nested element is presented directly with pid, not childAlias. Is this actually valid?
                        Object attr = parent.getAttribute(pid);
                        if ((attr != null) && (attr instanceof List)) {
                            @SuppressWarnings("unchecked")
                            List<Object> children = (List<Object>) attr;
                            for (int i = 0; i < children.size(); i++) {
                                Object o = children.get(i);
                                if (o instanceof ConfigElement) {
                                    ConfigElement child = (ConfigElement) o;
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Found child {0} for parent {1} using pid {2}", child, parent, pid);
                                        Tr.debug(tc, "Creating NestedConfigElement with registry entry ", parentEntry);
                                    }
                                    if (parent.isSimple()) {
                                        if (parentEntry.isFactory()) {
                                            retVal.add(new FactoryElement((SimpleElement) parent, -1, parentEntry));
                                        } else {
                                            retVal.add(new SingletonElement((SimpleElement) parent, parentEntry.getPid()));
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        return retVal;
    }

    public static class NestedConfigElement extends ConfigElement {
        private final ConfigElement parent;
        private final RegistryEntry parentRegistryEntry;
        private final String id;

        public NestedConfigElement(ConfigElement child, ConfigElement parent, RegistryEntry parentEntry) {
            super(child);
            this.id = child.getId();
            this.parent = parent;
            this.parentRegistryEntry = parentEntry;
        }

        @Override
        public ConfigElement getParent() {
            return this.parent;
        }

        public RegistryEntry getParentRegistryEntry() {
            return this.parentRegistryEntry;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.xml.internal.ConfigElement#getId()
         */
        @Override
        public String getId() {
            return this.id;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.config.xml.internal.ConfigElement#isSimple()
         */
        @Override
        public boolean isSimple() {
            return true;
        }

    }

}
