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

import java.lang.reflect.Type;

import io.openliberty.mcp.internal.schemas.SchemaAnnotation;
import io.openliberty.mcp.internal.schemas.SchemaGenerator;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.JsonObjectBuilder;

public record OptionalSchemaCreationBlueprint(Type type, Type optionalType) implements SchemaCreationBlueprint {

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String description) {
        return SchemaGenerator.generateSubSchema(optionalType, ctx, SchemaAnnotation.ofDescription(description));
    }
}