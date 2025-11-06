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

import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_OBJECT;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import io.openliberty.mcp.internal.schemas.SchemaAnnotation;
import io.openliberty.mcp.internal.schemas.SchemaGenerator;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

public record WildcardSchemaCreationBlueprint(Type baseType, WildcardType wt, Type[] upperBounds, Type[] lowerBounds) implements SchemaCreationBlueprint {

    @Override
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String referenceDescription) {
        if (upperBounds[0] == Object.class && lowerBounds.length == 0) {
            return Json.createObjectBuilder().add(TYPE, TYPE_OBJECT);
        } else if (upperBounds[0] != Object.class && lowerBounds.length == 0) {
            return SchemaGenerator.generateSubSchema(upperBounds()[0], ctx, SchemaAnnotation.ofDescription(referenceDescription));
        } else if (upperBounds[0] == Object.class && lowerBounds.length == 1) {
            return Json.createObjectBuilder().add(TYPE, TYPE_OBJECT);
        }
        return Json.createObjectBuilder();
    }
}