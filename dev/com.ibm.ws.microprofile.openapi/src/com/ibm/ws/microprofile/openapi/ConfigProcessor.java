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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ConfigProcessor {

    /**  */
    private static final String MP_OPENAPI_MODEL_READER = "mp.openapi.model.reader";
    private static final String MP_OPENAPI_FILTER = "mp.openapi.filter";
    private static final String MP_OPENAPI_SCAN_DISABLE = "mp.openapi.scan.disable";
    private static final String MP_OPENAPI_SCAN_PACKAGES = "mp.openapi.scan.packages";
    private static final String MP_OPENAPI_SCAN_CLASSES = "mp.openapi.scan.classes";
    private static final String MP_OPENAPI_SERVERS = "mp.openapi.scan.servers";
    private static final String MP_OPENAPI_SERVERS_PATH = "mp.openapi.scan.servers.path.";
    private static final String MP_OPENAPI_SERVERS_OPERATION = "mp.openapi.scan.servers.operation.";

    private static final TraceComponent tc = Tr.register(ConfigProcessor.class);

    private String modelReaderClassName = null;
    private String openAPIFilterClassName = null;
    private boolean scanDisabled = false;
    private final Set<String> classesToScan = null;
    private final Set<String> packagesToScan = null;

    public ConfigProcessor(ClassLoader appClassloader) {
        Config config = ConfigProvider.getConfig(appClassloader);
        try {
            modelReaderClassName = config.getOptionalValue(MP_OPENAPI_MODEL_READER, String.class).orElse(null);
            scanDisabled = config.getOptionalValue(MP_OPENAPI_SCAN_DISABLE, Boolean.class).orElse(false);
            openAPIFilterClassName = config.getOptionalValue(MP_OPENAPI_FILTER, String.class).orElse(null);

        } catch (IllegalArgumentException e) {

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
        builder.append(MP_OPENAPI_MODEL_READER + "=" + modelReaderClassName + "\n");
        builder.append("}\n");
        return builder.toString();
    }
}
