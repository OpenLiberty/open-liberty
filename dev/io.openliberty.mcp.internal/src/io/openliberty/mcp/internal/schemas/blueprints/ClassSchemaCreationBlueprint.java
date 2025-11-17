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
import static io.openliberty.mcp.internal.schemas.JsonConstants.PROPERTIES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.REQUIRED;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_OBJECT;
import static io.openliberty.mcp.internal.schemas.SchemaDirection.INPUT;

import java.util.List;
import java.util.Optional;

import io.openliberty.mcp.internal.schemas.SchemaAnnotation;
import io.openliberty.mcp.internal.schemas.SchemaCreationBlueprintGenerator.FieldInfo;
import io.openliberty.mcp.internal.schemas.SchemaGenerator;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

public record ClassSchemaCreationBlueprint(Class<?> baseType, List<FieldInfo> inputFields, List<FieldInfo> outputFields) implements SchemaCreationBlueprint {

    @Override
    public Optional<String> getDefsName() {
        return Optional.of(baseType.getSimpleName());
    }

    /**
     * <p>If the class has a schema annotation that will be used..
     * If the defs builder inside context already contains the basetype then a JsonSchemPrimitive with only a reference will be returned as the type is duplicated.
     * A class json object should contain required list and properities map.
     * Each of the fields in the class is recursively converted to a JsonSchema Object.
     * The final output is dependent on if the class has defs or is a base class thats in defs</p>
     */
    @Override
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String referenceDescription) {

        Optional<JsonObjectBuilder> annotationSchema = SchemaAnnotation.read(baseType).asJsonSchema();
        if (annotationSchema.isPresent()) {
            JsonObjectBuilder result = annotationSchema.get();
            if (referenceDescription != null) {
                result.add(DESCRIPTION, referenceDescription);
            }
            return annotationSchema.get();
        }

        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonArrayBuilder required = Json.createArrayBuilder();
        List<FieldInfo> fields = ctx.getDirection() == INPUT ? inputFields() : outputFields();
        for (FieldInfo fi : fields) {
            SchemaAnnotation childSchemaAnn = SchemaAnnotation.read(fi.annotations());

            JsonObjectBuilder subSchemaObjectBuilder = SchemaGenerator.generateSubSchema(fi.type(), ctx, childSchemaAnn);
            properties.add(fi.name(), subSchemaObjectBuilder.build());

            SchemaCreationBlueprint scb = ctx.getBlueprintRegistry().getSchemaCreationBlueprint(fi.type());
            if (scb.isRequired()) {
                required.add(fi.name());
            }
        }

        String schemaDescription = referenceDescription;
        if (schemaDescription == null) {
            SchemaAnnotation baseSchemaAnn = SchemaAnnotation.read(((Class<?>) baseType()).getAnnotations());
            schemaDescription = baseSchemaAnn.description().orElse(null);
        }

        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                              .add(TYPE, TYPE_OBJECT)
                                              .add(PROPERTIES, properties.build())
                                              .add(REQUIRED, required.build());
        if (schemaDescription != null) {
            schemaBuilder.add(DESCRIPTION, schemaDescription);
        }

        return schemaBuilder;
    }
}