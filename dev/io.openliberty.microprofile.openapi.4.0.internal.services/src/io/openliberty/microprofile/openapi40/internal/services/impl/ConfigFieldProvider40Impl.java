/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi40.internal.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.openapi.OASConfig;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.microprofile.openapi20.internal.services.ConfigField;
import io.openliberty.microprofile.openapi20.internal.services.ConfigFieldProvider;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfig.AutoInheritance;
import io.smallrye.openapi.api.OpenApiConfig.DuplicateOperationIdBehavior;
import io.smallrye.openapi.api.OpenApiConfig.OperationIdStrategy;
import io.smallrye.openapi.api.SmallRyeOASConfig;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ConfigFieldProvider40Impl implements ConfigFieldProvider {

    // Package protected for unit testing
    enum ConfigField40 implements ConfigField {
        MODEL_READER("modelReader", OASConfig.MODEL_READER, OpenApiConfig::modelReader),
        FILTER("filter", OASConfig.FILTER, OpenApiConfig::filter),
        SCAN_DISABLE("scanDisable", OASConfig.SCAN_DISABLE, c -> Boolean.toString(c.scanDisable())),
        SCAN_PACKAGES("scanPackages", OASConfig.SCAN_PACKAGES, OpenApiConfig::scanPackages, ConfigField::serializeSet),
        SCAN_CLASSES("scanClasses", OASConfig.SCAN_CLASSES, OpenApiConfig::scanClasses, ConfigField::serializeSet),
        SCAN_EXCLUDE_PACKAGES("scanExcludePackages", OASConfig.SCAN_EXCLUDE_PACKAGES, OpenApiConfig::scanExcludePackages, ConfigField::serializeSet),
        SCAN_EXCLUDE_CLASSES("scanExcludeClasses", OASConfig.SCAN_EXCLUDE_CLASSES, OpenApiConfig::scanExcludeClasses, ConfigField::serializeSet),
        SCAN_BEAN_VALIDATION("scanBeanValidation", OASConfig.SCAN_BEANVALIDATION, c -> Boolean.toString(c.scanBeanValidation())),
        SERVERS("servers", OASConfig.SERVERS, c -> String.join(",", c.servers())),
        // pathServers handled in writeConfig
        // operationServers handled in writeConfig
        SCAN_DEPENDENCIES_DISABLE("scanDependenciesDisable", SmallRyeOASConfig.SMALLRYE_SCAN_DEPENDENCIES_DISABLE, c -> Boolean.toString(c.scanDependenciesDisable())),
        SCAN_DEPENDENCIES_JARS("scanDependenciesJars", SmallRyeOASConfig.SMALLRYE_SCAN_DEPENDENCIES_JARS, c -> ConfigField.serializeSet(c.scanDependenciesJars())),
        CUSTOM_SCHEMA_REGISTRY_CLASS("customSchemaRegistryClass", SmallRyeOASConfig.SMALLRYE_CUSTOM_SCHEMA_REGISTRY_CLASS, OpenApiConfig::customSchemaRegistryClass),
        APPLICATION_PATH_DISABLE("applicationPathDisable", SmallRyeOASConfig.SMALLRYE_APP_PATH_DISABLE, c -> Boolean.toString(c.applicationPathDisable())),
        PRIVATE_PROPERTIES_ENABLE("privatePropertiesEnable", SmallRyeOASConfig.SMALLRYE_PRIVATE_PROPERTIES_ENABLE, c -> Boolean.toString(c.privatePropertiesEnable())),
        PROPERTY_NAMING_STRATEGY("propertyNamingStrategy", SmallRyeOASConfig.SMALLRYE_PROPERTY_NAMING_STRATEGY, OpenApiConfig::propertyNamingStrategy),
        SORTED_PROPERTIES_ENABLE("sortedPropertiesEnable", SmallRyeOASConfig.SMALLRYE_SORTED_PROPERTIES_ENABLE, c -> Boolean.toString(c.sortedPropertiesEnable())),
        SCHEMAS("getSchemas", "mp.openapi.schema.*", c -> ConfigField.serializeMap(c.getSchemas())),
        OPEN_API_VERSION("getOpenApiVersion", SmallRyeOASConfig.VERSION, OpenApiConfig::getOpenApiVersion),
        INFO_TITLE("getInfoTitle", SmallRyeOASConfig.INFO_TITLE, OpenApiConfig::getInfoTitle),
        INFO_VERSION("getInfoVersion", SmallRyeOASConfig.INFO_VERSION, OpenApiConfig::getInfoVersion),
        INFO_DESCRIPTION("getInfoDescription", SmallRyeOASConfig.INFO_DESCRIPTION, OpenApiConfig::getInfoDescription),
        INFO_TERMS_OF_SERVICE("getInfoTermsOfService", SmallRyeOASConfig.INFO_TERMS, OpenApiConfig::getInfoTermsOfService),
        INFO_SUMMARY("getInfoSummary", SmallRyeOASConfig.INFO_SUMMARY, OpenApiConfig::getInfoSummary),
        INFO_CONTACT_EMAIL("getInfoContactEmail", SmallRyeOASConfig.INFO_CONTACT_EMAIL, OpenApiConfig::getInfoContactEmail),
        INFO_CONTACT_NAME("getInfoContactName", SmallRyeOASConfig.INFO_CONTACT_NAME, OpenApiConfig::getInfoContactName),
        INFO_CONTACT_URL("getInfoContactUrl", SmallRyeOASConfig.INFO_CONTACT_URL, OpenApiConfig::getInfoContactUrl),
        INFO_LICENSE_NAME("getInfoLicenseName", SmallRyeOASConfig.INFO_LICENSE_NAME, OpenApiConfig::getInfoLicenseName),
        INFO_LICENSE_IDENTIFIER("getInfoLicenseIdentifier", SmallRyeOASConfig.INFO_LICENSE_IDENTIFIER, OpenApiConfig::getInfoLicenseIdentifier),
        INFO_LICENSE_URL("getInfoLicenseUrl", SmallRyeOASConfig.INFO_LICENSE_URL, OpenApiConfig::getInfoLicenseName),
        OPERATION_ID_STRATEGY("getOperationIdStrategy", SmallRyeOASConfig.OPERATION_ID_STRAGEGY, OpenApiConfig::getOperationIdStrategy, OperationIdStrategy::name),
        ARRAY_REFERENCES_ENABLE("arrayReferencesEnable", SmallRyeOASConfig.SMALLRYE_ARRAY_REFERENCES_ENABLE, c -> Boolean.toString(c.arrayReferencesEnable())),
        DUPLICATE_OPERATION_ID_BEHAVIOR("getDuplicateOperationIdBehavior", SmallRyeOASConfig.DUPLICATE_OPERATION_ID_BEHAVIOR, OpenApiConfig::getDuplicateOperationIdBehavior,
                                        DuplicateOperationIdBehavior::name),
        DEFAULT_PRODUCES("getDefaultProduces", SmallRyeOASConfig.DEFAULT_PRODUCES, OpenApiConfig::getDefaultProduces, Optional::toString),
        DEFAULT_CONSUMES("getDefaultConsumes", SmallRyeOASConfig.DEFAULT_CONSUMES, OpenApiConfig::getDefaultConsumes, Optional::toString),
        DEFAULT_PRIMITIVES_PRODUCES("getDefaultPrimitivesProduces", SmallRyeOASConfig.DEFAULT_PRODUCES_PRIMITIVES, OpenApiConfig::getDefaultPrimitivesProduces, Optional::toString),
        DEFAULT_PRIMITIVES_CONSUMES("getDefaultPrimitivesConsumes", SmallRyeOASConfig.DEFAULT_CONSUMES_PRIMITIVES, OpenApiConfig::getDefaultPrimitivesConsumes, Optional::toString),
        DEFAULT_STREAMING_PRODUCES("getDefaultStreamingProduces", SmallRyeOASConfig.DEFAULT_PRODUCES_STREAMING, OpenApiConfig::getDefaultStreamingProduces, Optional::toString),
        DEFAULT_STREAMING_CONSUMES("getDefaultStreamingConsumes", SmallRyeOASConfig.DEFAULT_CONSUMES_STREAMING, OpenApiConfig::getDefaultStreamingConsumes, Optional::toString),
        ALLOW_NAKED_PATH_PARAMETER("allowNakedPathParameter", "allowNakedPathParameter", OpenApiConfig::allowNakedPathParameter, Optional::toString),
        SCAN_PROFILES("getScanProfiles", SmallRyeOASConfig.SCAN_PROFILES, OpenApiConfig::getScanProfiles, ConfigField::serializeSet),
        SCAN_EXCLUDE_PROFILES("getScanExcludeProfiles", SmallRyeOASConfig.SCAN_EXCLUDE_PROFILES, OpenApiConfig::getScanExcludeProfiles, ConfigField::serializeSet),
        SCAN_RESOURCE_CLASSES("getScanResourceClasses", SmallRyeOASConfig.SCAN_RESOURCE_CLASS_PREFIX, OpenApiConfig::getScanResourceClasses, ConfigField::serializeMap),
        REMOVE_UNUSED_SCHEMAS("removeUnusedSchemas", SmallRyeOASConfig.SMALLRYE_REMOVE_UNUSED_SCHEMAS, c -> Boolean.toString(c.removeUnusedSchemas())),
        MAXIMUM_STATIC_FILE_SIZE("getMaximumStaticFileSize", SmallRyeOASConfig.MAXIMUM_STATIC_FILE_SIZE, c -> c.getMaximumStaticFileSize().toString()),
        AUTO_INHERITANCE("getAutoInheritance", SmallRyeOASConfig.AUTO_INHERITANCE, OpenApiConfig::getAutoInheritance, AutoInheritance::name),
        SCAN_COMPOSITION_EXCLUDE_PACKAGES("getScanCompositionExcludePackages", SmallRyeOASConfig.SCAN_COMPOSITION_EXCLUDE_PACKAGES,
                                          OpenApiConfig::getScanCompositionExcludePackages, ConfigField::serializeSet),
        MERGE_SCHEMA_EXAMPLES("mergeSchemaExamples", SmallRyeOASConfig.SMALLRYE_MERGE_SCHEMA_EXAMPLES, c -> Boolean.toString(c.mergeSchemaExamples()));

        Function<OpenApiConfig, String> function;
        String methodName;
        String propertyName;

        ConfigField40(String name, String propertyName, Function<OpenApiConfig, String> function) {
            this.methodName = name;
            this.function = function;
            this.propertyName = propertyName;
        }

        <V> ConfigField40(String methodName, String propertyName, Function<OpenApiConfig, V> valueGetter, Function<V, String> stringConverter) {
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
        return Arrays.asList(ConfigField40.values());
    }

    @Override
    public Collection<ConfigField> getIndexingConfigFields() {
        ArrayList<ConfigField> result = new ArrayList<>();
        result.add(ConfigField40.SCAN_DISABLE);
        result.add(ConfigField40.SCAN_CLASSES);
        result.add(ConfigField40.SCAN_EXCLUDE_CLASSES);
        result.add(ConfigField40.SCAN_PACKAGES);
        result.add(ConfigField40.SCAN_EXCLUDE_PACKAGES);
        result.add(ConfigField40.SCAN_DEPENDENCIES_DISABLE);
        result.add(ConfigField40.SCAN_DEPENDENCIES_JARS);
        return result;
    }

    @Override
    public String getPathServers(OpenApiConfig config, String path) {
        return String.join(",", config.pathServers(path));
    }

    @Override
    public String getOperationServers(OpenApiConfig config, String operationId) {
        return String.join(",", config.operationServers(operationId));
    }

}
