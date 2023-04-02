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
package io.openliberty.microprofile.openapi20.internal.cache;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.microprofile.openapi20.internal.services.ConfigField;
import io.openliberty.microprofile.openapi20.internal.services.ConfigFieldProvider;
import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Handles serializing an OpenApiConfig object to a map of properties
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = ConfigSerializer.class)
public class ConfigSerializer {

    @Reference
    private ConfigFieldProvider configFieldProvider;

    /**
     * Convert an OpenApiConfig into a Properties object so that it can be easily stored.
     *
     * @param config the config
     * @param model the model used to retrieve paths and operation ids
     * @return a Properties object containing the relevant keys and values from the config
     */
    public Properties serializeConfig(OpenApiConfig config, OpenAPI model) {
        Properties result = new Properties();
        for (ConfigField field : configFieldProvider.getConfigFields()) {
            String value = field.getValue(config);
            if (value != null) {
                result.put(field.getMethod(), value);
            }
        }
        for (String pathName : getPathNames(model)) {
            String value = configFieldProvider.getPathServers(config, pathName);
            if (value != null && !value.isEmpty()) {
                result.put("pathServer." + pathName, value);
            }
        }
        for (String operationId : getOperationIds(model)) {
            String value = configFieldProvider.getOperationServers(config, operationId);
            if (value != null && !value.isEmpty()) {
                result.put("operationServer." + operationId, value);
            }
        }
        return result;
    }

    private static Set<String> getPathNames(OpenAPI model) {
        return getPathItems(model).keySet();
    }

    private static Set<String> getOperationIds(OpenAPI model) {
        return getPathItems(model).values().stream() // Get the paths
                                  .flatMap(i -> i.getOperations().values().stream()) // Get all the operations from all the paths
                                  .filter(o -> o.getOperationId() != null) // Filter out the ones without an id
                                  .map(Operation::getOperationId) // Extract just the operation id
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
