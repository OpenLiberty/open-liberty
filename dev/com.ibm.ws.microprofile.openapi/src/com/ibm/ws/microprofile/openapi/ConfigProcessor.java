/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

public class ConfigProcessor {

    private static final TraceComponent tc = Tr.register(ConfigProcessor.class);

    /**
     * Configuration property to enable/disable validation of the OpenAPI model.
     */
    private static final String VALIDATION = "mp.openapi.extensions.validation";

    private String modelReaderClassName = null;
    private String openAPIFilterClassName = null;
    private boolean scanDisabled = false;
    private boolean validation = true;
    private Set<String> classesToScan = null;
    private Set<String> classesToExclude = null;
    private Set<String> packagesToScan = null;
    private Set<String> packagesToExclude = null;

    private final Config config;

    public ConfigProcessor(ClassLoader appClassloader) {
        config = ConfigProvider.getConfig(appClassloader);
        try {
            modelReaderClassName = config.getOptionalValue(OASConfig.MODEL_READER, String.class).orElse(null);
            scanDisabled = config.getOptionalValue(OASConfig.SCAN_DISABLE, Boolean.class).orElse(false);
            openAPIFilterClassName = config.getOptionalValue(OASConfig.FILTER, String.class).orElse(null);
            validation = config.getOptionalValue(VALIDATION, Boolean.class).orElse(true);

            classesToScan = getConfigPropAsSet(OASConfig.SCAN_CLASSES);
            packagesToScan = getConfigPropAsSet(OASConfig.SCAN_PACKAGES);
            classesToExclude = getConfigPropAsSet(OASConfig.SCAN_EXCLUDE_CLASSES);
            packagesToExclude = getConfigPropAsSet(OASConfig.SCAN_EXCLUDE_PACKAGES);

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
        builder.append("{\n");
        builder.append(OASConfig.MODEL_READER + "=" + modelReaderClassName + "\n");
        builder.append(OASConfig.FILTER + "=" + openAPIFilterClassName + "\n");
        builder.append(OASConfig.SCAN_DISABLE + "=" + scanDisabled + "\n");
        builder.append(VALIDATION + "=" + validation + "\n");
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
}
