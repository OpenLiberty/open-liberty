/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import java.io.Closeable;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.services.ConfigField;
import io.openliberty.microprofile.openapi20.internal.services.ConfigFieldProvider;
import io.openliberty.microprofile.openapi20.internal.utils.LoggingUtils;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;

public class ConfigProcessor implements Closeable {

    private static final TraceComponent tc = Tr.register(ConfigProcessor.class);

    /**
     * Configuration property to enable/disable validation of the OpenAPI model. The default value is true.
     */
    private static final String VALIDATION = OASConfig.EXTENSIONS_PREFIX + "liberty.validation";
    private static final boolean VALIDATION_DEFAULT_VALUE = true;
    private boolean validation = VALIDATION_DEFAULT_VALUE;

    /**
     * Configuration property that specifies how frequently monitored files are checked for updates.
     * The value of this property is a non-negative integer. The unit for the interval is seconds.
     * The default value is 2 (two seconds). Setting the value to 0 will disable file monitoring.
     */
    private static final String FILE_POLLING_INTERVAL = OASConfig.EXTENSIONS_PREFIX + "liberty.file.polling.interval";
    private static final int FILE_POLLING_INTERVAL_DEFAULT_VALUE = 2;
    private int pollingInterval = FILE_POLLING_INTERVAL_DEFAULT_VALUE;

    private Config oaConfig;
    private final OpenApiConfigImpl smallryeConfig;
    private final ConfigFieldProvider configFieldProvider;

    /**
     * Constructor
     *
     * This creates a brand new Config instance without using the classloader cache in the ConfigProviderResolver. It
     * must be closed again after use (see close() method).
     *
     * @param appClassloader
     *     The ClassLoader to use when discovering the MicroProfile configuration.
     */
    public ConfigProcessor(ClassLoader appClassloader, ConfigFieldProvider configFieldProvider) {
        this.oaConfig = ConfigProvider.getConfig(appClassloader);
        this.smallryeConfig = new OpenApiConfigImpl(oaConfig);
        this.configFieldProvider = configFieldProvider;

        validation = oaConfig.getOptionalValue(VALIDATION, Boolean.class).orElse(VALIDATION_DEFAULT_VALUE);
        pollingInterval = oaConfig.getOptionalValue(FILE_POLLING_INTERVAL, Integer.class).filter(v -> v >= 0).orElse(FILE_POLLING_INTERVAL_DEFAULT_VALUE);
    }

    public OpenApiConfig getOpenAPIConfig() {
        return this.smallryeConfig;
    }

    public boolean isValidating() {
        return validation;
    }

    public int getFilePollingInterval() {
        return pollingInterval;
    }

    @Override
    public void close() {
        try {
            if (Closeable.class.isAssignableFrom(this.oaConfig.getClass())) {
                ((Closeable) this.oaConfig).close();
                this.oaConfig = null;
            }
        } catch (Exception e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to close config: " + e.getMessage());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("ConfigProcessor : {\n");

        for (ConfigField field : configFieldProvider.getConfigFields()) {
            builder.append(field.getProperty()).append("=").append(field.getValue(smallryeConfig)).append("\n");
        }

        builder.append(VALIDATION).append("=").append(validation).append("\n");
        builder.append(FILE_POLLING_INTERVAL).append("=").append(pollingInterval).append("\n");
        builder.append("}\n");
        return builder.toString();
    }
}
