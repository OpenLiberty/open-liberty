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

import static io.openliberty.mcp.internal.schemas.JsonConstants.ADDITIONAL_PROPERTIES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.DESCRIPTION;
import static io.openliberty.mcp.internal.schemas.JsonConstants.PROPERTY_NAMES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_OBJECT;

import java.lang.reflect.Type;

import io.openliberty.mcp.internal.schemas.SchemaAnnotation;
import io.openliberty.mcp.internal.schemas.SchemaGenerator;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

public record MapSchemaCreationBlueprint(Type baseType, Type keyType, Type valueType) implements SchemaCreationBlueprint {

    public MapSchemaCreationBlueprint {
        if (keyType == null) {
            keyType = String.class;
        }
        if (valueType == null) {
            valueType = Object.class;
        }
    }

    /** {@inheritDoc} */
    @Override
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String description) {

        JsonObjectBuilder valueSchemaBuilder = SchemaGenerator.generateSubSchema(valueType, ctx, SchemaAnnotation.EMPTY);
        SchemaCreationBlueprint keyScb = ctx.getBlueprintRegistry().getSchemaCreationBlueprint(keyType);

        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                              .add(TYPE, TYPE_OBJECT)
                                              .add(ADDITIONAL_PROPERTIES, valueSchemaBuilder.build());

        if (keyScb instanceof EnumSchemaCreationBlueprint) {
            JsonObjectBuilder keySchemaBuilder = SchemaGenerator.generateSubSchema(keyType, ctx, SchemaAnnotation.EMPTY);
            schemaBuilder.add(PROPERTY_NAMES, keySchemaBuilder.build());
        }
        if (description != null) {
            schemaBuilder.add(DESCRIPTION, description);
        }

        return schemaBuilder;
    }
}