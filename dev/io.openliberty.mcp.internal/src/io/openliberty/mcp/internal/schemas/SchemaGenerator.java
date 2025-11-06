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

import static io.openliberty.mcp.internal.schemas.JsonConstants.DEFS;
import static io.openliberty.mcp.internal.schemas.JsonConstants.DESCRIPTION;
import static io.openliberty.mcp.internal.schemas.JsonConstants.PROPERTIES;
import static io.openliberty.mcp.internal.schemas.JsonConstants.REF;
import static io.openliberty.mcp.internal.schemas.JsonConstants.REQUIRED;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE;
import static io.openliberty.mcp.internal.schemas.JsonConstants.TYPE_OBJECT;
import static io.openliberty.mcp.internal.schemas.SchemaDirection.INPUT;
import static io.openliberty.mcp.internal.schemas.SchemaDirection.OUTPUT;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.schemas.SchemaCreationBlueprintGenerator.FieldInfo;
import io.openliberty.mcp.internal.schemas.blueprints.ClassSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.ListSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.MapSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.OptionalSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.SchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.TypeVariableSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.WildcardSchemaCreationBlueprint;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 *
 */
public class SchemaGenerator {

    /**
     * <p>If Schema annotation is present then that will be returned if not then a blueprint will be generated for all properties that jsonb would serialise for both
     * directions.
     * After the `blueprint` is generated the method will check if any classes are duplicated.
     * Using all the context info a JSON schema be generated from blueprint.</p>
     *
     * @param cls the class to generate a schema for
     * @param direction whether to generate an input or output schema
     * @param blueprintRegistry the blueprintRegistry to use for getting schema information for types
     * @return the schema as a JSON object
     */
    public static JsonObject generateSchema(Class<?> cls, SchemaDirection direction, SchemaCreationBlueprintRegistry blueprintRegistry) {
        SchemaAnnotation annotationInfo = SchemaAnnotation.read(cls);
        Optional<JsonObjectBuilder> annotationSchema = annotationInfo.asJsonSchema();
        if (annotationSchema.isPresent()) {
            return annotationSchema.get().build();
        } else {
            SchemaGenerationContext ctx = new SchemaGenerationContext(blueprintRegistry, direction);
            calculateClassFrequency(cls, direction, ctx);

            JsonObjectBuilder result = generateSubSchema(cls, ctx, annotationInfo);
            addDefs(result, ctx);
            return result.build();
        }
    }

    /**
     * Generate the input schema for a tool
     *
     * @param tool the tool to generate the schema for
     * @param blueprintRegistry the blueprint registry to use
     * @return the schema as a json object
     */
    public static JsonObject generateToolInputSchema(AnnotatedMethod<?> toolMethod, SchemaCreationBlueprintRegistry blueprintRegistry) {
        // create base schema components
        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonArrayBuilder required = Json.createArrayBuilder();
        Parameter[] parameters = toolMethod.getJavaMember().getParameters();
        SchemaGenerationContext ctx = new SchemaGenerationContext(blueprintRegistry, INPUT);
        Map<String, ArgumentMetadata> arguments = ToolMetadata.getArgumentMap(toolMethod);
        // for each parameter
        for (ArgumentMetadata argument : arguments.values()) {
            Parameter parameter = parameters[argument.index()];
            calculateClassFrequency(parameter.getParameterizedType(), SchemaDirection.INPUT, ctx);
        }

        for (var entry : arguments.entrySet()) {
            String argumentName = entry.getKey();
            ArgumentMetadata argument = entry.getValue();
            Parameter parameter = parameters[argument.index()];
            SchemaAnnotation annotation = SchemaAnnotation.read(parameter);

            JsonObjectBuilder parameterSchemaBuilder = generateSubSchema(parameter.getParameterizedType(), ctx, annotation);

            if (argument.description() != null && !argument.description().equals("")) {
                parameterSchemaBuilder.add(DESCRIPTION, argument.description());
            }
            // - add it as a property
            properties.add(argumentName, parameterSchemaBuilder.build());
            // - add it as required (if it is)
            if (argument.required()) {
                required.add(argumentName);
            }
        }

        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                                              .add(TYPE, TYPE_OBJECT)
                                              .add(PROPERTIES, properties.build())
                                              .add(REQUIRED, required.build());
        addDefs(schemaBuilder, ctx);
        return schemaBuilder.build();
    }

    /**
     * Generate the output schema for a tool
     *
     * @param tool the tool to generate the schema for
     * @param blueprintRegistry the blueprint registry to use
     * @return the schema as a json object
     */
    public static JsonObject generateToolOutputSchema(AnnotatedMethod<?> toolMethod, SchemaCreationBlueprintRegistry blueprintRegistry) {

        Type returnType = toolMethod.getJavaMember().getGenericReturnType();

        Method method = toolMethod.getJavaMember();
        SchemaAnnotation schemaAnnotation = SchemaAnnotation.read(method);

        SchemaGenerationContext ctx = new SchemaGenerationContext(blueprintRegistry, OUTPUT);
        calculateClassFrequency(returnType, SchemaDirection.OUTPUT, ctx);

        JsonObjectBuilder outputSchema = generateSubSchema(returnType, ctx, schemaAnnotation);
        addDefs(outputSchema, ctx);

        return outputSchema.build();
    }

    public static class SchemaGenerationContext {
        /** Map of type to whether it's been seen more than once */
        private HashMap<Type, Boolean> typeMultiUse = new HashMap<>();
        /** Map of type to name */
        private HashMap<Type, String> nameMap = new HashMap<>();
        /** The values of nameMap */
        private Set<String> namesInUse = new HashSet<>();
        /** Map of types and their corresponding JSON schemas which should be added to defs */
        private HashMap<Type, JsonObject> defsBuilder = new HashMap<>();
        /** The blueprint registry to be used when generating schemas */
        private SchemaCreationBlueprintRegistry blueprintRegistry;
        /** Whether we're generating input or output schemas */
        private SchemaDirection direction;

        public SchemaGenerationContext(SchemaCreationBlueprintRegistry blueprintRegistry, SchemaDirection direction) {
            Objects.requireNonNull(blueprintRegistry, "blueprintRegistry");
            Objects.requireNonNull(direction, "direction");
            this.blueprintRegistry = blueprintRegistry;
            this.direction = direction;
        }

        /**
         * Registers a type as having been seen
         *
         * @param type the type
         * @return {@code true} if this method was called for {@code type} before, otherwise {@code false}
         */
        public boolean registerSeen(Type type) {
            // If this is the first time we've seen this type, add it to the map with false,
            // If it's not the first time, set it to true
            return typeMultiUse.compute(type, (k, v) -> v == null ? false : true);
        }

        /**
         * Returns whether a type is used multiple times in the schema.
         *
         * @param type the type
         * @return {@code true} if it was used multiple times, otherwise false
         */
        public boolean isMultiUse(Type type) {
            return typeMultiUse.getOrDefault(type, false);
        }

        /**
         * Reserve a name for a type. The name can be looked up later with {@link #getName(Type)}.
         *
         * @param type the type
         * @param baseName the name to use. A suffix will be added if required to make the name unique.
         */
        public void reserveName(Type type, String baseName) {
            String name = nameMap.get(type);
            if (name == null) {
                int suffix = 1;
                name = baseName;
                while (namesInUse.contains(name)) {
                    suffix++;
                    name = baseName + suffix;
                }
                nameMap.put(type, name);
                namesInUse.add(name);
            }
        }

        /**
         * Get the name for a type.
         * <p>
         * The name must have been reserved earlier using {@link #reserveName(Type, String)}
         *
         * @param type the type
         * @return the name
         */
        public String getName(Type type) {
            return nameMap.get(type);
        }

        public SchemaDirection getDirection() {
            return direction;
        }

        public HashMap<Type, JsonObject> getDefsBuilder() {
            return defsBuilder;
        }

        public SchemaCreationBlueprintRegistry getBlueprintRegistry() {
            return blueprintRegistry;
        }
    }

    public static void calculateClassFrequency(Type type, SchemaDirection direction, SchemaGenerationContext ctx) {
        SchemaCreationBlueprint scc = ctx.getBlueprintRegistry().getSchemaCreationBlueprint(type);
        boolean previouslySeen = false;
        if (scc.getDefsName().isPresent()) {
            // We might add this type to defs, so we need to add it to the typeFrequency map
            // If this is the first time we've seen this type, set it to false, if it's not the first time, set it to true
            previouslySeen = ctx.registerSeen(type);

            if (previouslySeen) {
                ctx.reserveName(type, scc.getDefsName().get());
            }
        }

        if (!previouslySeen) {
            // Process children
            if (scc instanceof ListSchemaCreationBlueprint listScb) {
                calculateClassFrequency(listScb.itemType(), direction, ctx);
            } else if (scc instanceof ClassSchemaCreationBlueprint classScb) {
                List<FieldInfo> fields = direction == SchemaDirection.INPUT ? classScb.inputFields() : classScb.outputFields();
                for (FieldInfo fi : fields) {
                    calculateClassFrequency(fi.type(), direction, ctx);
                }
            } else if (scc instanceof MapSchemaCreationBlueprint mapScb) {
                calculateClassFrequency(mapScb.valueType(), direction, ctx);
                calculateClassFrequency(mapScb.keyType(), direction, ctx);
            } else if (scc instanceof OptionalSchemaCreationBlueprint optionalScb) {
                calculateClassFrequency(optionalScb.optionalType(), direction, ctx);
            } else if (scc instanceof TypeVariableSchemaCreationBlueprint tvScb) {
                for (Type bound : tvScb.tv().getBounds()) {
                    calculateClassFrequency(bound, direction, ctx);
                }
            } else if (scc instanceof WildcardSchemaCreationBlueprint wtScb) {
                for (Type bound : wtScb.upperBounds()) {
                    calculateClassFrequency(bound, direction, ctx);
                }
                for (Type bound : wtScb.lowerBounds()) {
                    calculateClassFrequency(bound, direction, ctx);
                }
            }
        }
    }

    private static final JsonObject GENERATION_IN_PROGRESS = Json.createObjectBuilder().build();

    public static JsonObjectBuilder generateSubSchema(Type type, SchemaGenerationContext ctx, SchemaAnnotation annotation) {
        // Allow a schema to be overridden from the point that it's used
        var schemaFromAnnotation = annotation.asJsonSchema();
        if (schemaFromAnnotation.isPresent()) {
            return schemaFromAnnotation.get();
        }

        SchemaCreationBlueprint blueprint = ctx.getBlueprintRegistry().getSchemaCreationBlueprint(type);
        String description = annotation.description().orElse(null);
        JsonObject defsSchema = ctx.getDefsBuilder().get(type);
        if (defsSchema != null) {
            // Schema is already in defs, just return a reference
            return createReference(ctx.getName(type), description, defsSchema);
        } else if (blueprint.getDefsName().isPresent() && ctx.isMultiUse(type)) {
            // Put a placeholder in the defs map, so that we will make references to the current object
            // rather than trying to generate it again (and getting stuck in a loop)
            ctx.getDefsBuilder().put(type, GENERATION_IN_PROGRESS);
            // Generate the real schema object and add it to defs
            JsonObject result = blueprint.toJsonSchemaObject(ctx, null).build();
            ctx.getDefsBuilder().put(type, result);
            // Now return a reference to it
            return createReference(ctx.getName(type), description, result);
        } else {
            // Schema should not be added to defs, just generate it and return
            return blueprint.toJsonSchemaObject(ctx, description);
        }
    }

    private static JsonObjectBuilder createReference(String name, String referenceDescription, JsonObject referencedSchema) {
        String schemaDescription = referencedSchema.getString(DESCRIPTION, null);

        JsonObjectBuilder ref = Json.createObjectBuilder().add(REF, "#/$defs/" + name);
        if (referenceDescription != null && !referenceDescription.equals(schemaDescription)) {
            ref.add(DESCRIPTION, referenceDescription);
        }
        return ref;
    }

    /**
     * @param result
     * @param ctx
     */
    private static void addDefs(JsonObjectBuilder result, SchemaGenerationContext ctx) {
        if (ctx.getDefsBuilder().isEmpty()) {
            return;
        }

        JsonObjectBuilder defs = Json.createObjectBuilder();
        ctx.getDefsBuilder().forEach((type, schema) -> {
            defs.add(ctx.getName(type), schema);
        });

        result.add(DEFS, defs);
    }

}
