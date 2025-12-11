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

import static io.openliberty.mcp.internal.schemas.JsonConstants.DESCRIPTION;
import static io.openliberty.mcp.internal.schemas.JsonConstants.ENUM;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_STRING;

import java.util.List;
import java.util.Optional;

import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

public record EnumSchemaCreationBlueprint(Class<?> baseType, List<String> values) implements SchemaCreationBlueprint {

    @Override
    public Optional<String> getDefsName() {
        return Optional.of(baseType.getSimpleName());
    }

    /** {@inheritDoc} */
    @Override
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String description) {

        JsonArrayBuilder enumValuesJsonArray = Json.createArrayBuilder();
        values.forEach(e -> {
            enumValuesJsonArray.add(e);
        });
        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder().add(TYPE, TYPE_STRING).add(ENUM, enumValuesJsonArray);
        if (description != null) {
            schemaBuilder.add(DESCRIPTION, description);
        }

        return schemaBuilder;
    }
}