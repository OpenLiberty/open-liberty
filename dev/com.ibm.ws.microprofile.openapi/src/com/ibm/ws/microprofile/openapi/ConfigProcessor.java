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

import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

public class ConfigProcessor {

    private static final TraceComponent tc = Tr.register(ConfigProcessor.class);

    private String modelReaderClassName = null;
    private String openAPIFilterClassName = null;
    private boolean scanDisabled = false;
    private final Set<String> classesToScan = null;
    private final Set<String> packagesToScan = null;

    public ConfigProcessor(ClassLoader appClassloader) {
        Config config = ConfigProvider.getConfig(appClassloader);
        try {
            modelReaderClassName = config.getOptionalValue(OASConfig.MODEL_READER, String.class).orElse(null);
            scanDisabled = config.getOptionalValue(OASConfig.SCAN_DISABLE, Boolean.class).orElse(false);
            openAPIFilterClassName = config.getOptionalValue(OASConfig.FILTER, String.class).orElse(null);

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
}
