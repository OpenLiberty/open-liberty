/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.services.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.eclipse.microprofile.openapi.OASConfig;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.microprofile.openapi20.internal.services.ConfigField;
import io.openliberty.microprofile.openapi20.internal.services.ConfigFieldProvider;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfig.OperationIdStrategy;
import io.smallrye.openapi.api.constants.OpenApiConstants;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ConfigFieldProvider20Impl implements ConfigFieldProvider {

    private enum ConfigField20 implements ConfigField {
        MODEL_READER("modelReader", OASConfig.MODEL_READER, OpenApiConfig::modelReader),
        FILTER("filter", OASConfig.FILTER, OpenApiConfig::filter),
        SCAN_DISABLE("scanDisable", OASConfig.SCAN_DISABLE, c -> Boolean.toString(c.scanDisable())),
        SCAN_PACKAGES("scanPackages", OASConfig.SCAN_PACKAGES, OpenApiConfig::scanPackages, Pattern::pattern),
        SCAN_CLASSES("scanClasses", OASConfig.SCAN_CLASSES, OpenApiConfig::scanClasses, Pattern::pattern),
        SCAN_EXCLUDE_PACKAGES("scanExcludePackages", OASConfig.SCAN_EXCLUDE_PACKAGES, OpenApiConfig::scanExcludePackages, Pattern::pattern),
        SCAN_EXCLUDE_CLASSES("scanExcludeClasses", OASConfig.SCAN_EXCLUDE_CLASSES, OpenApiConfig::scanExcludeClasses, Pattern::pattern),
        SERVERS("servers", OASConfig.SERVERS, c -> ConfigField.serializeSet(c.servers())),
        // pathServers handled in writeConfig
        // operationServers handled in writeConfig
        SCAN_DEPENDENCIES_DISABLE("scanDependenciesDisable", OpenApiConstants.SMALLRYE_SCAN_DEPENDENCIES_DISABLE, c -> Boolean.toString(c.scanDependenciesDisable())),
        SCAN_DEPENDENCIES_JARS("scanDependenciesJars", OpenApiConstants.SMALLRYE_SCAN_DEPENDENCIES_JARS, c -> ConfigField.serializeSet(c.scanDependenciesJars())),
        CUSTOM_SCHEMA_REGISTRY_CLASS("customSchemaRegistryClass", OpenApiConstants.SMALLRYE_CUSTOM_SCHEMA_REGISTRY_CLASS, OpenApiConfig::customSchemaRegistryClass),
        APPLICATION_PATH_DISABLE("applicationPathDisable", OpenApiConstants.SMALLRYE_APP_PATH_DISABLE, c -> Boolean.toString(c.applicationPathDisable())),
        PRIVATE_PROPERTIES_ENABLE("privatePropertiesEnable", OpenApiConstants.SMALLRYE_PRIVATE_PROPERTIES_ENABLE, c -> Boolean.toString(c.privatePropertiesEnable())),
        PROPERTY_NAMING_STRATEGY("propertyNamingStrategy", OpenApiConstants.SMALLRYE_PROPERTY_NAMING_STRATEGY, OpenApiConfig::propertyNamingStrategy),
        SORTED_PROPERTIES_ENABLE("sortedPropertiesEnable", OpenApiConstants.SMALLRYE_SORTED_PROPERTIES_ENABLE, c -> Boolean.toString(c.sortedPropertiesEnable())),
        SCHEMAS("getSchemas", "mp.openapi.schema.*", c -> ConfigField.serializeMap(c.getSchemas())),
        OPEN_API_VERSION("getOpenApiVersion", OpenApiConstants.VERSION, OpenApiConfig::getOpenApiVersion),
        INFO_TITLE("getInfoTitle", OpenApiConstants.INFO_TITLE, OpenApiConfig::getInfoTitle),
        INFO_VERSION("getInfoVersion", OpenApiConstants.INFO_VERSION, OpenApiConfig::getInfoVersion),
        INFO_DESCRIPTION("getInfoDescription", OpenApiConstants.INFO_DESCRIPTION, OpenApiConfig::getInfoDescription),
        INFO_TERMS_OF_SERVICE("getInfoTermsOfService", OpenApiConstants.INFO_TERMS, OpenApiConfig::getInfoTermsOfService),
        INFO_CONTACT_EMAIL("getInfoContactEmail", OpenApiConstants.INFO_CONTACT_EMAIL, OpenApiConfig::getInfoContactEmail),
        INFO_CONTACT_NAME("getInfoContactName", OpenApiConstants.INFO_CONTACT_NAME, OpenApiConfig::getInfoContactName),
        INFO_CONTACT_URL("getInfoContactUrl", OpenApiConstants.INFO_CONTACT_URL, OpenApiConfig::getInfoContactUrl),
        INFO_LICENSE_NAME("getInfoLicenseName", OpenApiConstants.INFO_LICENSE_NAME, OpenApiConfig::getInfoLicenseName),
        INFO_LICENSE_URL("getInfoLicenseUrl", OpenApiConstants.INFO_LICENSE_URL, OpenApiConfig::getInfoLicenseName),
        OPERATION_ID_STRATEGY("getOperationIdStrategy", OpenApiConstants.OPERATION_ID_STRAGEGY, OpenApiConfig::getOperationIdStrategy, OperationIdStrategy::name),
        ARRAY_REFERENCES_ENABLE("arrayReferencesEnable", OpenApiConstants.SMALLRYE_ARRAY_REFERENCES_ENABLE, c -> Boolean.toString(c.arrayReferencesEnable())),
        DEFAULT_PRODUCES("getDefaultProduces", OpenApiConstants.DEFAULT_PRODUCES, OpenApiConfig::getDefaultProduces, Optional::toString),
        DEFAULT_CONSUMES("getDefaultConsumes", OpenApiConstants.DEFAULT_CONSUMES, OpenApiConfig::getDefaultConsumes, Optional::toString),
        ALLOW_NAKED_PATH_PARAMETER("allowNakedPathParameter", "allowNakedPathParameter", OpenApiConfig::allowNakedPathParameter, Optional::toString),
        ;

        Function<OpenApiConfig, String> function;
        String methodName;
        String propertyName;

        ConfigField20(String name, String propertyName, Function<OpenApiConfig, String> function) {
            this.methodName = name;
            this.function = function;
            this.propertyName = propertyName;
        }

        <V> ConfigField20(String methodName, String propertyName, Function<OpenApiConfig, V> valueGetter, Function<V, String> stringConverter) {
            this.methodName = methodName;
            this.propertyName = propertyName;
            this.function = c -> {
                V value = valueGetter.apply(c);
                return value == null ? null : stringConverter.apply(value);
            };
        }

        @Override
        public String getMethod() {
            return methodName;
        }

        @Override
        public String getValue(OpenApiConfig config) {
            return function.apply(config);
        }

        @Override
        public String getProperty() {
            return propertyName;
        }

    }

    @Override
    public Collection<ConfigField> getConfigFields() {
        return Arrays.asList(ConfigField20.values());
    }

    @Override
    public String getPathServers(OpenApiConfig config, String path) {
        return ConfigField.serializeSet(config.pathServers(path));
    }

    @Override
    public String getOperationServers(OpenApiConfig config, String operationId) {
        return ConfigField.serializeSet(config.operationServers(operationId));
    }

}
