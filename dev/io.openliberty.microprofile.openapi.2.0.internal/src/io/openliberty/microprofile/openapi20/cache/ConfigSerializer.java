/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfig.OperationIdStrategy;

public class ConfigSerializer {

    // Package protected for unit testing
    enum ConfigField {
        MODEL_READER("modelReader", OpenApiConfig::modelReader),
        FILTER("filter", OpenApiConfig::filter),
        SCAN_DISABLE("scanDisable", c -> Boolean.toString(c.scanDisable())),
        SCAN_PACKAGES("scanPackages", OpenApiConfig::scanPackages, Pattern::pattern),
        SCAN_CLASSES("scanClasses", OpenApiConfig::scanClasses, Pattern::pattern),
        SCAN_EXCLUDE_PACKAGES("scanExcludePackages", OpenApiConfig::scanExcludePackages, Pattern::pattern),
        SCAN_EXCLUDE_CLASSES("scanExcludeClasses", OpenApiConfig::scanExcludeClasses, Pattern::pattern),
        SERVERS("servers", c -> serializeSet(c.servers())),
        // pathServers handled in writeConfig
        // operationServers handled in writeConfig
        SCAN_DEPENDENCIES_DISABLE("scanDependenciesDisable", c -> Boolean.toString(c.scanDependenciesDisable())),
        SCAN_DEPENDENCIES_JARS("scanDependenciesJars", c -> serializeSet(c.scanDependenciesJars())),
        CUSTOM_SCHEMA_REGISTRY_CLASS("customSchemaRegistryClass", OpenApiConfig::customSchemaRegistryClass),
        APPLICATION_PATH_DISABLE("applicationPathDisable", c -> Boolean.toString(c.applicationPathDisable())),
        PRIVATE_PROPERTIES_ENABLE("privatePropertiesEnable", c -> Boolean.toString(c.privatePropertiesEnable())),
        PROPERTY_NAMING_STRATEGY("propertyNamingStrategy", OpenApiConfig::propertyNamingStrategy),
        SORTED_PROPERTIES_ENABLE("sortedPropertiesEnable", c -> Boolean.toString(c.sortedPropertiesEnable())),
        SCHEMAS("getSchemas", c -> serializeMap(c.getSchemas())),
        OPEN_API_VERSION("getOpenApiVersion", OpenApiConfig::getOpenApiVersion),
        INFO_TITLE("getInfoTitle", OpenApiConfig::getInfoTitle),
        INFO_VERSION("getInfoVersion", OpenApiConfig::getInfoVersion),
        INFO_DESCRIPTION("getInfoDescription", OpenApiConfig::getInfoDescription),
        INFO_TERMS_OF_SERVICE("getInfoTermsOfService", OpenApiConfig::getInfoTermsOfService),
        INFO_CONTACT_EMAIL("getInfoContactEmail", OpenApiConfig::getInfoContactEmail),
        INFO_CONTACT_NAME("getInfoContactName", OpenApiConfig::getInfoContactName),
        INFO_CONTACT_URL("getInfoContactUrl", OpenApiConfig::getInfoContactUrl),
        INFO_LICENSE_NAME("getInfoLicenseName", OpenApiConfig::getInfoLicenseName),
        INFO_LICENSE_URL("getInfoLicenseUrl", OpenApiConfig::getInfoLicenseName),
        OPERATION_ID_STRATEGY("getOperationIdStrategy", OpenApiConfig::getOperationIdStrategy, OperationIdStrategy::name),
        ARRAY_REFERENCES_ENABLE("arrayReferencesEnable", c -> Boolean.toString(c.arrayReferencesEnable())),
        DEFAULT_PRODUCES("getDefaultProduces", OpenApiConfig::getDefaultProduces, Optional::toString),
        DEFAULT_CONSUMES("getDefaultConsumes", OpenApiConfig::getDefaultConsumes, Optional::toString),
        ALLOW_NAKED_PATH_PARAMETER("allowNakedPathParameter", OpenApiConfig::allowNakedPathParameter, Optional::toString),
        ;

        Function<OpenApiConfig, String> function;
        String name;

        private ConfigField(String name, Function<OpenApiConfig, String> function) {
            this.name = name;
            this.function = function;
        }

        private <V> ConfigField(String name, Function<OpenApiConfig, V> valueGetter, Function<V, String> stringConverter) {
            this.name = name;
            this.function = c -> {
                V value = valueGetter.apply(c);
                return value == null ? null : stringConverter.apply(value);
            };
        }
    }

    /**
     * Convert an OpenApiConfig into a Properties object so that it can be easily stored.
     * 
     * @param config the config
     * @param model the model used to retrieve paths and operation ids
     * @return a Properties object containing the relevant keys and values from the config
     */
    public static Properties serializeConfig(OpenApiConfig config, OpenAPI model) {
        Properties result = new Properties();
        for (ConfigField field : ConfigField.values()) {
            String value = field.function.apply(config);
            if (value != null) {
                result.put(field.name, value);
            }
        }
        for (String pathName : getPathNames(model)) {
            Set<String> value = config.pathServers(pathName);
            if (value != null && !value.isEmpty()) {
                result.put("pathServer." + pathName, serializeSet(value));
            }
        }
        for (String operationId : getOperationIds(model)) {
            Set<String> value = config.operationServers(operationId);
            if (value != null && !value.isEmpty()) {
                result.put("operationServer." + operationId, serializeSet(value));
            }
        }
        return result;
    }

    private static String serializeSet(Set<String> set) {
        // Sort to ensure repeatability
        ArrayList<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return String.join(",", list);
    }

    private static String serializeMap(Map<String, String> map) {
        // Sort to ensure repeatability
        List<Entry<String, String>> entryList = new ArrayList<>(map.entrySet());
        Collections.sort(entryList, Map.Entry.comparingByKey());

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Entry<String, String> e : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
        }
        return sb.toString();
    }

    private static Set<String> getPathNames(OpenAPI model) {
        return getPathItems(model).keySet();
    }

    private static Set<String> getOperationIds(OpenAPI model) {
        return getPathItems(model).values().stream() // Get the paths
                    .flatMap(i -> i.getOperations().values().stream()) // Get all the operations from all the paths
                    .filter(o -> o.getOperationId() != null) // Filter out the ones without an id
                    .map(o -> o.getOperationId()) // Extract just the operation id
                    .collect(Collectors.toSet()); // Collect the IDs into a set
    }
    
    private static Map<String, PathItem> getPathItems(OpenAPI model) {
        Paths paths = model.getPaths();
        if (paths == null) {
            return Collections.emptyMap();
        }
        
        Map<String, PathItem> pathItems = paths.getPathItems();
        if (pathItems == null) {
            return Collections.emptyMap();
        }
        
        return pathItems;
    }

}
