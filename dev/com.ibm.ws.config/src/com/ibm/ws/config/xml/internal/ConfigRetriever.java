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
package com.ibm.ws.config.xml.internal;

import java.io.IOException;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.config.ConfigRetrieverException;
import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.SystemConfigSupport;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 *
 */
class ConfigRetriever {

    private static final TraceComponent tc = Tr.register(ConfigRetriever.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);
    private final SystemConfigSupport caSupport;
    private final ConfigurationAdmin configAdmin;
    private final ConfigVariableRegistry variableRegistry;

    ConfigRetriever(SystemConfigSupport caSupport, ConfigurationAdmin configAdmin, ConfigVariableRegistry vr) {
        this.caSupport = caSupport;
        this.configAdmin = configAdmin;
        this.variableRegistry = vr;
    }

    /**
     * Find a configuration PID from a configuration ID.
     *
     * @param referenceId the ID to lookup
     * @return the configuration PID, or null if no configuration is found
     */
    String lookupPid(ConfigID referenceId) {
        ExtendedConfiguration config = caSupport.lookupConfiguration(referenceId);
        String pid = (config == null) ? null : config.getPid();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "lookupPid(): Lookup of " + referenceId + " reference resolved to " + pid);
        }
        return pid;
    }

    /**
     * Find or create a PID for a configuration ID.
     *
     * @param configId the ID to lookup or create
     * @return the configuration PID
     */
    String getPid(ConfigID configId) throws ConfigNotFoundException {
        ExtendedConfiguration config = getConfiguration(configId);
        String pid = config.getPid();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getPid(): Lookup of " + configId + " configuration id resolved to " + pid);
        }
        return pid;
    }

    ExtendedConfiguration lookupConfiguration(ConfigID configId) {
        return caSupport.lookupConfiguration(configId);
    }

    ExtendedConfiguration findConfiguration(String pid) {
        return caSupport.findConfiguration(pid);
    }

    ExtendedConfiguration getConfiguration(ConfigID configId) throws ConfigNotFoundException {
        try {
            ExtendedConfiguration config = caSupport.lookupConfiguration(configId);
            if (config == null) {
                String pid = configId.getPid();
                String factoryFilter = configId.getId() != null ? getConfigurationFilter(configId) : null;

                config = (ExtendedConfiguration) getConfiguration(pid, factoryFilter);
                config.setInOverridesFile(true);
                // This really needs to move somewhere else. ConfigRetriever should not have a variable registry
                for (String variable : config.getUniqueVariables()) {
                    variableRegistry.addVariableInUse(variable);
                }
                if (configId.getId() != null) {
                    caSupport.registerConfiguration(configId, config);
                }

            }
            return config;
        } catch (ConfigRetrieverException ex) {
            throw new ConfigNotFoundException(ex);
        }
    }

    private Configuration getConfiguration(String actualPID, String factoryFilter) throws ConfigRetrieverException {
        try {
            Configuration config = null;
            if (factoryFilter != null) {
                Configuration[] configs = configAdmin.listConfigurations(factoryFilter);
                if (configs == null || configs.length == 0) {
                    config = configAdmin.createFactoryConfiguration(actualPID, null);
                } else if (configs.length == 1) {
                    config = configs[0];
                } else {
                    throw new ConfigRetrieverException("Too many factory configurations found: " + factoryFilter);
                }
            } else {
                config = configAdmin.getConfiguration(actualPID, null);
            }
            return config;
        } catch (InvalidSyntaxException e) {
            // this should not happen
            throw new ConfigRetrieverException("Error listing configurations with filter " + factoryFilter, e);
        } catch (IOException ex) {
            // This should really never happen
            throw new ConfigRetrieverException("Encountered an IOException while getting configuration", ex);
        }
    }

    Configuration[] listConfigurations(String filter) throws ConfigRetrieverException {
        try {
            return configAdmin.listConfigurations(filter);
        } catch (IOException e) {
            // This should really never happen
            throw new ConfigRetrieverException("Encountered an IOException while getting configuration", e);
        } catch (InvalidSyntaxException e) {
            // this should not happen
            throw new ConfigRetrieverException("Error listing configurations with filter " + filter, e);
        }
    }

    private String getConfigurationFilter(ConfigID configId) {
        String filter = null;
        if (configId.getId() != null) {
            String key = XMLConfigConstants.CFG_CONFIG_INSTANCE_ID;
            String value = configId.toString();
            filter = "(&" + FilterUtils.createPropertyFilter(ConfigurationAdmin.SERVICE_FACTORYPID, configId.getPid()) + FilterUtils.createPropertyFilter(key, value) + ")";
        } else {
            filter = "(&(" + Constants.SERVICE_PID + '=' + configId.getPid() + "))";
        }
        return filter;
    }

    void collectConfigurations(ConfigElement configElement, List<ExtendedConfiguration> configurations) throws ConfigUpdateException {
        if (configElement.hasNestedElements()) {
            for (Object attributeValue : configElement.getAttributes().values()) {
                if (attributeValue instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> values = (List<Object>) attributeValue;
                    int size = values.size();
                    for (int i = 0; i < size; i++) {
                        if (values.get(i) instanceof ConfigElement) {
                            ConfigElement nested = (ConfigElement) values.get(i);

                            ConfigID configID = null;
                            if (nested.getId() == null) {
                                String id = "default-" + i;
                                configID = new ConfigID(configElement.getConfigID(), nested.getNodeName(), id);
                            } else {
                                configID = nested.getConfigID();
                            }

                            try {
                                configurations.add(getConfiguration(configID));
                            } catch (ConfigNotFoundException e) {
                                throw new ConfigUpdateException(e);
                            }

                            collectConfigurations(nested, configurations);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return
     * @throws InvalidSyntaxException
     */
    ExtendedConfiguration[] listAllConfigurations() throws InvalidSyntaxException, IOException {
        return (ExtendedConfiguration[]) configAdmin.listConfigurations(null);
    }

    /**
     * @param configElement
     * @return
     * @throws ConfigRetrieverException
     */
    ExtendedConfiguration[] findConfigurations(ConfigID configID) throws ConfigRetrieverException {
        String filter = getConfigurationFilter(configID);
        try {
            return (ExtendedConfiguration[]) configAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            // this should not happen
            throw new ConfigRetrieverException("Error listing configurations with filter " + filter, e);
        } catch (IOException ex) {
            // This should really never happen
            throw new ConfigRetrieverException("Encountered an IOException while finding configurations", ex);
        }
    }

    /**
     * @param configID
     * @return
     * @throws ConfigRetrieverException
     */
    public ExtendedConfiguration[] findAllNestedConfigurations(ConfigID configID) throws ConfigRetrieverException {
        String filter = ("(| " +
                         "(" + XMLConfigConstants.CFG_CONFIG_INSTANCE_ID + "=*//" + configID.getPid() + "[*)" +
                         "(" + XMLConfigConstants.CFG_CONFIG_INSTANCE_ID + "=*//" + configID.getPid() + "\\(*)" +
                         ")");

        try {
            return (ExtendedConfiguration[]) configAdmin.listConfigurations(filter);
        } catch (IOException e) {
            // this should not happen
            throw new ConfigRetrieverException("Error listing configurations with filter " + filter, e);
        } catch (InvalidSyntaxException e) {
            // This should really never happen
            throw new ConfigRetrieverException("Encountered an IOException while finding configurations", e);
        }
    }

    public ExtendedConfiguration[] findAllConfigurationsByPid(String pid) throws ConfigRetrieverException {
        String filter = "(|(" + Constants.SERVICE_PID + "=" + pid + ")(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + pid + "))";
        try {
            return (ExtendedConfiguration[]) configAdmin.listConfigurations(filter);
        } catch (IOException e) {
            // this should not happen
            throw new ConfigRetrieverException("Error listing configurations with filter " + filter, e);
        } catch (InvalidSyntaxException e) {
            // This should really never happen
            throw new ConfigRetrieverException("Encountered an IOException while finding configurations", e);
        }
    }
}
