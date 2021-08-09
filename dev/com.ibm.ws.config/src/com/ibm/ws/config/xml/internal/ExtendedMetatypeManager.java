/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;

/**
 *
 */
class ExtendedMetatypeManager {

    static final TraceComponent tc = Tr.register(ExtendedMetatypeManager.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    public static final String EXT_SOURCE_PID_KEY = "ibm.extends.source.pid";
    public static final String EXT_IMMEDIATE_SUBTYPE_PID_KEY = "ibm.extends.subtype.pid";
    private static final String EXT_SOURCE_FACTORY_KEY = "ibm.extends.source.factoryPid";

    private final MetaTypeRegistry metatypeRegistry;
    private final ConfigurationAdmin configAdmin;
    private final HashMap<String, ExtendedConfiguration> knownSupertypeConfigs = new HashMap<String, ExtendedConfiguration>();
    private static int index = -1;

    public ExtendedMetatypeManager(MetaTypeRegistry metatypeRegistry, ConfigurationAdmin ca) {
        this.metatypeRegistry = metatypeRegistry;
        this.configAdmin = ca;
    }

    public void init() {
        try {
            ExtendedConfiguration[] superTypeConfigs = (ExtendedConfiguration[]) configAdmin.listConfigurations("(" + EXT_SOURCE_PID_KEY + "=*)");
            if (superTypeConfigs != null) {
                for (ExtendedConfiguration superTypeConfig : superTypeConfigs) {
                    String subTypeConfigPid = (String) superTypeConfig.getProperties().get(EXT_IMMEDIATE_SUBTYPE_PID_KEY);
                    if (subTypeConfigPid == null) {
                        // only here for backward compatibility, should never really be used.
                        subTypeConfigPid = (String) superTypeConfig.getProperties().get(EXT_SOURCE_PID_KEY);
                    }
                    knownSupertypeConfigs.put(subTypeConfigPid, superTypeConfig);
                }
            }
        } catch (InvalidSyntaxException e) {
            e.getCause();
        } catch (IOException e) {
            e.getCause();
        }
    }

    public void createSuperTypes(ConfigurationInfo info) {
        // Create supertype config if necessary
        if (info.registryEntry != null && info.registryEntry.getExtends() != null) {
            // ibm:extends is only allowed on factory pids
            if (info.config.getFactoryPid() == null) {
                Tr.error(tc, "error.factoryOnly", new Object[] { info.config.getPid(), info.registryEntry.getExtends() });
            } else {
                createSuperTypes(info, info.registryEntry.getExtends(), info.config);
            }
        }
    }

    private void createSuperTypes(final ConfigurationInfo info, String superFactoryPid, final ExtendedConfiguration leafTypeConfig) {
        ExtendedConfiguration subTypeConfig = leafTypeConfig;
        try {
            boolean requireActualUpdate = info.registryEntry.supportsHiddenExtensions();
            while (superFactoryPid != null) {
                ExtendedConfiguration config = null;
                synchronized (knownSupertypeConfigs) {
                    config = knownSupertypeConfigs.get(subTypeConfig.getPid());
                    if (config == null) {
                        //we don't have a super config for this sub config yet
                        config = (ExtendedConfiguration) configAdmin.createFactoryConfiguration(superFactoryPid, null);
                        String id = subTypeConfig.getFullId().getId();
                        if ((id != null) && (id.startsWith("default-"))) {
                            // If there is no ID for the sub config, generate one here. No one is looking for
                            // it, so it just needs to be unique.
                            id = "extends-" + ++index;
                        }
                        config.setFullId(new ConfigID(superFactoryPid, id));
                        knownSupertypeConfigs.put(subTypeConfig.getPid(), config);
                    }
                }

                //get the configuration for the sub type
                Dictionary<String, Object> properties = subTypeConfig.getProperties();

                if (properties != null) {
                    //manipulate the properties for going from sub to super
                    //perform any required property renames
                    renameProps(subTypeConfig.getFactoryPid(), properties);

                    //insert additional properties on the super config
                    //indicating the source of the sub config, but only if
                    //it hasn't already been set so we keep the original
                    if (properties.get(EXT_SOURCE_PID_KEY) == null) {
                        // ibm.extends.source.pid always points to the leaf configuration, not the immediate supertype
                        properties.put(EXT_SOURCE_PID_KEY, leafTypeConfig.getPid());

                        // ibm.extends.source.factoryPid is always the leaf factory pid, not the immediate supertype
                        properties.put(EXT_SOURCE_FACTORY_KEY, leafTypeConfig.getFactoryPid());
                    }

                    // ibm.extends.supertype.pid always points to the immediate supertype. We need this to properly
                    // restore the list of knownSuperTypeConfigs by querying config admin on a restart
                    properties.put(EXT_IMMEDIATE_SUBTYPE_PID_KEY, subTypeConfig.getPid());

                    // Make sure config.id refers to this type rather than the leaf type
                    // The display ID still uses the leaf type
                    properties.put(XMLConfigConstants.CFG_CONFIG_INSTANCE_ID, config.getFullId().toString());

                    properties.remove(XMLConfigConstants.CFG_PARENT_PID);
                    if (requiresUpdate(requireActualUpdate, config, properties)) {
                        //now update the config, without notifying anyone
                        config.updateProperties(properties);
                    }
                    //config.update(properties);
                    info.addSuperTypeConfig(config);
                }

                // Continue up the tree
                RegistryEntry re = metatypeRegistry.getRegistryEntry(superFactoryPid);
                if (re == null)
                    return;

                if (re.getObjectClassDefinition().getExtends() != null) {
                    superFactoryPid = re.getObjectClassDefinition().getExtends();
                    subTypeConfig = config;
                    requireActualUpdate |= re.supportsHiddenExtensions();
                } else {
                    return;
                }
            }
        } catch (IOException ex) {
            // auto ffdc
        }

    }

    /**
     * @param requireActualUpdate
     * @param config
     * @param properties
     * @return whether to actually update the properties
     */
    private boolean requiresUpdate(boolean requireActualUpdate, ExtendedConfiguration config, Dictionary<String, Object> properties) {
        if (requireActualUpdate) {
            Dictionary<String, Object> existingProperties = config.getProperties();
            if (existingProperties != null) {
                return !equals(existingProperties, properties);
            }
        }
        return true;
    }

    private boolean equals(Dictionary<String, Object> cd, Dictionary<String, Object> otherCD) {
        boolean result = true;
        int thisId = cd.get("id") == null ? 0 : 1;
        int otherId = otherCD.get("id") == null ? 0 : 1;
        if (otherCD.size() - otherId == cd.size() - thisId) {
            for (Enumeration<String> keys = cd.keys(); keys.hasMoreElements();) {
                String key = keys.nextElement();
                if (key.equals("service.pid") || key.equals("service.factoryPid")) {
                    continue;
                }
                Object value = cd.get(key);
                Object otherValue = otherCD.get(key);
                if (otherValue == null) {
                    result = key.equals("id");
                }
                if (value != otherValue) {
//                        if (value.getClass().isArray() && otherValue.getClass().isArray()) {
                    if ((value instanceof String[]) && (otherValue instanceof String[])) {

                        if (!Arrays.equals((String[]) value, (String[]) otherValue)) {
                            result = false;
                        }
                    } else if (!value.equals(otherValue)) {
                        result = false;
                    }
                }
            }
        } else {
            result = false;
        }
        if (!result && tc.isDebugEnabled()) {
            Tr.debug(tc, "differing configurations old: " + cd + "\n new: " + otherCD);
        }

        return result;

    }

    private final void renameProps(String pid, Dictionary<String, Object> properties) {
        for (Map.Entry<String, String> renameMapping : getRenameMappings(pid).entrySet()) {
            Object value;
            if ((value = properties.get(renameMapping.getKey())) != null) {
                properties.put(renameMapping.getValue(), value);
            }
        }
    }

    private final Map<String, String> getRenameMappings(String pid) {
        //check if any attributes have renames and build a map of new to old
        Map<String, String> renameMappings = new HashMap<String, String>();
        RegistryEntry pidEntry = metatypeRegistry.getRegistryEntry(pid);
        String superPid = pidEntry.getExtends();
        if (superPid != null) {
            for (Map.Entry<String, ExtendedAttributeDefinition> entry : pidEntry.getObjectClassDefinition().getAttributeMap().entrySet()) {
                String rename;
                if ((rename = entry.getValue().getRename()) != null) {
                    renameMappings.put(entry.getKey(), rename);
                }
            }

        }
        return renameMappings;
    }

    public void deleteSuperType(String pid) {
        // Remove all supertype configurations in the hierarchy
        while (true) {
            ExtendedConfiguration c = removeSuperTypeForPid(pid);
            if (c != null) {
                pid = c.getPid();
                c.delete(true);
            } else {
                return;
            }

        }
    }

    public ExtendedConfiguration removeSuperTypeForPid(String pid) {
        synchronized (knownSupertypeConfigs) {
            return knownSupertypeConfigs.remove(pid);
        }
    }

    /**
     *
     */
    public Configuration getSuperTypeConfig(String pid) {
        return knownSupertypeConfigs.get(pid);

    }
}
