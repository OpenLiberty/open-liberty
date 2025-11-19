/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas;

import static io.openliberty.mcp.internal.schemas.JsonConstants.JSON_BOOLEAN_TYPES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.JSON_INT_TYPES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.JSON_NUMBER_TYPES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.JSON_STRING_TYPES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_BOOLEAN;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_INTEGER;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_NUMBER;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_STRING;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.openliberty.mcp.internal.schemas.blueprints.PrimitiveSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.SchemaCreationBlueprint;

/**
 *
 */
public class SchemaCreationBlueprintRegistry {
    private Map<Type, SchemaCreationBlueprint> cache = new HashMap<>();

    public SchemaCreationBlueprintRegistry() {
        JSON_INT_TYPES.forEach(t -> cache.put(t, new PrimitiveSchemaCreationBlueprint(t, TYPE_INTEGER)));
        JSON_NUMBER_TYPES.forEach(t -> cache.put(t, new PrimitiveSchemaCreationBlueprint(t, TYPE_NUMBER)));
        JSON_STRING_TYPES.forEach(t -> cache.put(t, new PrimitiveSchemaCreationBlueprint(t, TYPE_STRING)));
        JSON_BOOLEAN_TYPES.forEach(t -> cache.put(t, new PrimitiveSchemaCreationBlueprint(t, TYPE_BOOLEAN)));
    }

    /**
     * Get the schema creation blueprint for a type, creating it first using reflection if necessary
     *
     * @param type the type to get the creation blueprint for
     * @return the blueprint
     */
    public SchemaCreationBlueprint getSchemaCreationBlueprint(Type type) {
        SchemaCreationBlueprint result = cache.get(type);
        if (result == null) {
            result = SchemaCreationBlueprintGenerator.generateSchemaCreationBlueprint(type);
            cache.put(type, result);
        }
        return result;
    }

}
