/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas.blueprints;

import static io.openliberty.mcp.internal.schemas.JsonConstants.ARRAY;
import static io.openliberty.mcp.internal.schemas.JsonConstants.DESCRIPTION;
import static io.openliberty.mcp.internal.schemas.JsonConstants.ITEMS;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE;

import java.lang.reflect.Type;

import io.openliberty.mcp.internal.schemas.SchemaAnnotation;
import io.openliberty.mcp.internal.schemas.SchemaGenerator;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

public record ListSchemaCreationBlueprint(Type baseType, Type itemType) implements SchemaCreationBlueprint {

    /** {@inheritDoc} */
    @Override
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String description) {

        JsonObjectBuilder itemsSubSchemaBuilder = SchemaGenerator.generateSubSchema(itemType, ctx, SchemaAnnotation.EMPTY);

        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                              .add(TYPE, ARRAY)
                                              .add(ITEMS, itemsSubSchemaBuilder.build());
        if (description != null) {
            schemaBuilder.add(DESCRIPTION, description);
        }

        return schemaBuilder;
    }

}