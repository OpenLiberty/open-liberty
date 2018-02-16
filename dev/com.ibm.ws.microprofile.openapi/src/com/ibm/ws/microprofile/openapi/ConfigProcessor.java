/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

public class ConfigProcessor {

    private static final TraceComponent tc = Tr.register(ConfigProcessor.class);

    /**
     * Configuration property to enable/disable validation of the OpenAPI model. The default value is true.
     */
    private static final String VALIDATION = OASConfig.EXTENSIONS_PREFIX + "liberty.validation";
    private static final boolean VALIDATION_DEFAULT_VALUE = true;

    /**
     * Configuration property that specifies how frequently monitored files are checked for updates.
     * The value of this property is a non-negative integer. The unit for the interval is seconds.
     * The default value is 2 (two seconds). Setting the value to 0 will disable file monitoring.
     */
    private static final String FILE_POLLING_INTERVAL = OASConfig.EXTENSIONS_PREFIX + "liberty.file.polling.interval";
    private static final int FILE_POLLING_INTERVAL_DEFAULT_VALUE = 2;

    private String modelReaderClassName = null;
    private String openAPIFilterClassName = null;
    private boolean scanDisabled = false;
    private boolean validation = VALIDATION_DEFAULT_VALUE;
    private int pollingInterval = FILE_POLLING_INTERVAL_DEFAULT_VALUE;
    private Set<String> classesToScan = null;
    private Set<String> classesToExclude = null;
    private Set<String> packagesToScan = null;
    private Set<String> packagesToExclude = null;

    private Set<String> servers = null;
    private Map<String, Set<String>> pathsServers = null;
    private Map<String, Set<String>> operationsServers = null;

    private final Config config;

    public ConfigProcessor(ClassLoader appClassloader) {
        config = ConfigProvider.getConfig(appClassloader);
        try {
            modelReaderClassName = config.getOptionalValue(OASConfig.MODEL_READER, String.class).orElse(null);
            scanDisabled = config.getOptionalValue(OASConfig.SCAN_DISABLE, Boolean.class).orElse(false);
            openAPIFilterClassName = config.getOptionalValue(OASConfig.FILTER, String.class).orElse(null);
            validation = config.getOptionalValue(VALIDATION, Boolean.class).orElse(VALIDATION_DEFAULT_VALUE);
            pollingInterval = config.getOptionalValue(FILE_POLLING_INTERVAL, Integer.class).filter(v -> v >= 0).orElse(FILE_POLLING_INTERVAL_DEFAULT_VALUE);

            classesToScan = getConfigPropAsSet(OASConfig.SCAN_CLASSES);
            packagesToScan = getConfigPropAsSet(OASConfig.SCAN_PACKAGES);
            classesToExclude = getConfigPropAsSet(OASConfig.SCAN_EXCLUDE_CLASSES);
            packagesToExclude = getConfigPropAsSet(OASConfig.SCAN_EXCLUDE_PACKAGES);

            retrieveServers();
        } catch (IllegalArgumentException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to read config: " + e.getMessage());
            }
        }
    }

    public String getModelReaderClassName() {
        return modelReaderClassName;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConfigProcessor : {\n");
        builder.append(OASConfig.MODEL_READER + "=" + modelReaderClassName + "\n");
        builder.append(OASConfig.FILTER + "=" + openAPIFilterClassName + "\n");
        builder.append(OASConfig.SCAN_DISABLE + "=" + scanDisabled + "\n");
        builder.append(VALIDATION + "=" + validation + "\n");
        builder.append(FILE_POLLING_INTERVAL + "=" + pollingInterval + "\n");
        builder.append(OASConfig.SCAN_CLASSES + "=" + classesToScan + "\n");
        builder.append(OASConfig.SCAN_PACKAGES + "=" + packagesToScan + "\n");
        builder.append(OASConfig.SCAN_EXCLUDE_CLASSES + "=" + classesToExclude + "\n");
        builder.append(OASConfig.SCAN_EXCLUDE_PACKAGES + "=" + packagesToExclude + "\n");
        builder.append(OASConfig.SERVERS + "=" + servers + "\n");
        builder.append(OASConfig.SERVERS_PATH_PREFIX + "=" + pathsServers + "\n");
        builder.append(OASConfig.SERVERS_OPERATION_PREFIX + "=" + operationsServers + "\n");
        builder.append("}\n");
        return builder.toString();
    }

    /**
     * @return the scanDisabled
     */
    public boolean isScanDisabled() {
        return scanDisabled;
    }

    public String getOpenAPIFilterClassName() {
        return openAPIFilterClassName;
    }

    public boolean isValidating() {
        return validation;
    }

    public int getFilePollingInterval() {
        return pollingInterval;
    }

    private Set<String> getConfigPropAsSet(String configProperty) {
        String[] configValues = config.getOptionalValue(configProperty, String[].class).orElse(null);
        if (configValues == null || configValues.length == 0) {
            return null;
        } else {
            Set<String> configPropSet = new HashSet<>();
            for (String s : configValues) {
                configPropSet.add(s);
            }
            return configPropSet;
        }

    }

    private void retrieveServers() {

        // Global servers
        servers = getConfigPropAsSet(OASConfig.SERVERS);

        // Servers at path and operation level
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(OASConfig.SERVERS_PATH_PREFIX)) {
                Set<String> servers = getConfigPropAsSet(propertyName);
                if (servers != null) {
                    String path = propertyName.substring(OASConfig.SERVERS_PATH_PREFIX.length());
                    if (StringUtils.isNotBlank(path)) {
                        if (pathsServers == null) {
                            pathsServers = new HashMap<>();
                        }
                        pathsServers.put(path, servers);
                    }
                }
            } else if (propertyName.startsWith(OASConfig.SERVERS_OPERATION_PREFIX)) {
                Set<String> servers = getConfigPropAsSet(propertyName);
                if (servers != null) {
                    String path = propertyName.substring(OASConfig.SERVERS_OPERATION_PREFIX.length());
                    if (StringUtils.isNotBlank(path)) {
                        if (operationsServers == null) {
                            operationsServers = new HashMap<>();
                        }
                        operationsServers.put(path, servers);
                    }
                }
            }
        }
    }

    /**
     * @return the classesToScan
     */
    public Set<String> getClassesToScan() {
        return classesToScan;
    }

    /**
     * @return the classesToExclude
     */
    public Set<String> getClassesToExclude() {
        return classesToExclude;
    }

    /**
     * @return the packagesToScan
     */
    public Set<String> getPackagesToScan() {
        return packagesToScan;
    }

    /**
     * @return the packagesToExclude
     */
    public Set<String> getPackagesToExclude() {
        return packagesToExclude;
    }

    /**
     * @return the servers
     */
    public Set<String> getServers() {
        return servers;
    }

    /**
     * @return the pathsServers
     */
    public Map<String, Set<String>> getPathsServers() {
        return pathsServers;
    }

    /**
     * @return the operationsServers
     */
    public Map<String, Set<String>> getOperationsServers() {
        return operationsServers;
    }
}
