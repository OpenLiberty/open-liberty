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

import static io.openliberty.mcp.internal.schemas.JsonConstants.ALL_OF;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_OBJECT;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

import io.openliberty.mcp.internal.schemas.SchemaAnnotation;
import io.openliberty.mcp.internal.schemas.SchemaGenerator;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

public record TypeVariableSchemaCreationBlueprint(Type baseType, TypeVariable<?> tv) implements SchemaCreationBlueprint {

    @Override
    public Optional<String> getDefsName() {
        return Optional.of(tv.getName());
    }

    @Override
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String referenceDescription) {
        Type[] bounds = tv.getBounds();
        if (bounds[0] == Object.class && bounds.length == 1) {
            return Json.createObjectBuilder().add(TYPE, TYPE_OBJECT);
        } else if (bounds[0] != Object.class && bounds.length == 1) {
            return SchemaGenerator.generateSubSchema(bounds[0], ctx, SchemaAnnotation.ofDescription(referenceDescription));
        } else {
            JsonArrayBuilder allOffArrayBuilder = Json.createArrayBuilder();
            for (Type bound : bounds) {
                allOffArrayBuilder.add(SchemaGenerator.generateSubSchema(bound, ctx, SchemaAnnotation.ofDescription(referenceDescription)));
            }
            return Json.createObjectBuilder().add(ALL_OF, allOffArrayBuilder.build());
        }
    }
}