/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.openliberty.mcp.internal.schemas.SchemaCreationBlueprintGenerator.FieldInfo;
import io.openliberty.mcp.internal.schemas.blueprints.ClassSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.ListSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.MapSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.OptionalSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.SchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.TypeVariableSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.WildcardSchemaCreationBlueprint;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import io.openliberty.mcp.tools.ToolResponse;
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
     * A tool argument and its associated Schema annotation
     */
    public record AnnotatedToolArgument(ToolArgument argument, SchemaAnnotation schemaAnnotation) {
        AnnotatedToolArgument(ToolArgument argument, AnnotatedElement element) {
            this(argument, SchemaAnnotation.read(element));
        }

        AnnotatedToolArgument(ToolArgument argument) {
            this(argument, SchemaAnnotation.EMPTY);
        }
    }

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
     * @param arguments the tool arguments and their associated schema annotation
     * @param blueprintRegistry the blueprint registry to use
     * @return the schema as a json object
     */
    public static JsonObject generateToolInputSchema(List<AnnotatedToolArgument> arguments, SchemaCreationBlueprintRegistry blueprintRegistry) {
        // create base schema components
        JsonObjectBuilder properties = Json.createObjectBuilder();
        JsonArrayBuilder required = Json.createArrayBuilder();
        SchemaGenerationContext ctx = new SchemaGenerationContext(blueprintRegistry, INPUT);
        // for each parameter
        for (AnnotatedToolArgument argument : arguments) {
            calculateClassFrequency(argument.argument().type(), SchemaDirection.INPUT, ctx);
        }

        for (AnnotatedToolArgument argument : arguments) {
            String argumentName = argument.argument().name();
            SchemaAnnotation annotation = argument.schemaAnnotation();

            JsonObjectBuilder parameterSchemaBuilder = generateSubSchema(argument.argument().type(), ctx, annotation);

            String description = argument.argument().description();
            if (description != null && !description.isEmpty()) {
                parameterSchemaBuilder.add(DESCRIPTION, description);
            }
            // - add it as a property
            properties.add(argumentName, parameterSchemaBuilder.build());
            // - add it as required (if it is)
            if (argument.argument().required()) {
                required.add(argument.argument().name());
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
     * @param toolMethod the tool method to generate a schema for, or {@code null} for a tool without a method
     * @param toolOutputType the resolved output type of the tool, same as {@code toolMethod} for sync tools, unwrapped for async tools
     * @param blueprintRegistry the blueprint registry to use
     * @return the schema as a json object
     */
    public static JsonObject generateToolOutputSchema(AnnotatedMethod<?> toolMethod, Type toolOutputType,
                                                      SchemaCreationBlueprintRegistry blueprintRegistry) {

        Type returnType = toolOutputType;
        SchemaAnnotation schemaAnnotation;
        if (toolMethod != null) {
            Method method = toolMethod.getJavaMember();
            schemaAnnotation = SchemaAnnotation.read(method);
        } else {
            schemaAnnotation = SchemaAnnotation.EMPTY;
        }

        SchemaGenerationContext ctx = new SchemaGenerationContext(blueprintRegistry, OUTPUT);
        if (!(toolOutputType instanceof Class<?> c) || !c.isAssignableFrom(ToolResponse.class)) {
            calculateClassFrequency(returnType, SchemaDirection.OUTPUT, ctx);
        }

        JsonObjectBuilder outputSchema = generateSubSchema(returnType, ctx, schemaAnnotation);
        addDefs(outputSchema, ctx);
        return outputSchema.build();
    }

    public record TypeKey(Type type, Map<TypeVariable<?>, Type> typeVariableMappings) {
        public static TypeKey from(Type type, SchemaGenerationContext sgc) {
            if (type instanceof ParameterizedType pType) {
                return new TypeKey(pType.getRawType(), TypeUtility.createCoreTypeVariableMap(type, sgc));
            } else {
                return new TypeKey(type, null);
            }
        }
    };

    public static class SchemaGenerationContext {
        /** Map of type to whether it's been seen more than once */
        private HashMap<TypeKey, Boolean> typeMultiUse = new HashMap<>();
        /** Map of type to name */
        private HashMap<TypeKey, String> nameMap = new HashMap<>();
        /** The values of nameMap */
        private Set<String> namesInUse = new HashSet<>();
        /** Map of types and their corresponding JSON schemas which should be added to defs */
        private HashMap<TypeKey, JsonObject> defsBuilder = new HashMap<>();
        // Maps generic type variables to concrete Types
        private Deque<Map<TypeVariable<?>, Type>> genericMapStack = new ArrayDeque<>();
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
            return typeMultiUse.compute(TypeKey.from(type, this), (k, v) -> v == null ? false : true);
        }

        /**
         * Returns whether a type is used multiple times in the schema.
         *
         * @param type the type
         * @return {@code true} if it was used multiple times, otherwise false
         */
        public boolean isMultiUse(Type type) {
            return typeMultiUse.getOrDefault(TypeKey.from(type, this), false);
        }

        /**
         * Reserve a name for a type. The name can be looked up later with {@link #getName(Type)}.
         *
         * @param type the type
         * @param baseName the name to use. A suffix will be added if required to make the name unique.
         */
        public void reserveName(Type type, String baseName) {
            String name = getName(type);
            if (name == null) {
                int suffix = 1;
                name = baseName;
                while (namesInUse.contains(name)) {
                    suffix++;
                    name = baseName + suffix;
                }
                nameMap.put(TypeKey.from(type, this), name);
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
            return nameMap.get(TypeKey.from(type, this));
        }

        /**
         * Get the name for a type when you already have a stored TypeKey.
         * <p>
         * The name must have been reserved earlier using {@link #reserveName(Type, String)}
         *
         * @param typeKey the type key
         * @return the name
         */
        public String getName(TypeKey typeKey) {
            return nameMap.get(typeKey);
        }

        public SchemaDirection getDirection() {
            return direction;
        }

        public HashMap<TypeKey, JsonObject> getDefsBuilder() {
            return defsBuilder;
        }

        public SchemaCreationBlueprintRegistry getBlueprintRegistry() {
            return blueprintRegistry;
        }

        /**
         * Add type as the current context for resolving type variables.
         * <p>
         * This method must be called before generating the schema for {@code type} and must be matched by a call to {@link #popTypeContext()} when generating the schema for
         * {@code type} is finished.
         *
         * @param type the type to add to the current context
         */
        public void pushTypeContext(Type type) {
            genericMapStack.push(TypeUtility.createTypeVariableMap(type, this));
        }

        /**
         * Remove the last type from the context which was added with {@link #pushTypeContext(Type)}
         * <p>
         * This must be called once we're finished generating the schema for {@code type}
         */
        public void popTypeContext() {
            genericMapStack.pop();
        }

        /**
         * Get the value of a type variable in the current context
         *
         * @param typeVariable the type variable to resolve
         * @return the value assigned to that type variable in the current context
         */
        public Type resolveTypeVariable(TypeVariable<?> typeVariable) {
            if (genericMapStack.isEmpty()) {
                return typeVariable;
            }
            var genericMap = genericMapStack.peek();
            if (genericMap != null) {
                Type resolved = genericMap.get(typeVariable);
                if (resolved != null) {
                    return resolved;
                }
            }
            return typeVariable;
        }
    }

    public static void calculateClassFrequency(Type type, SchemaDirection direction, SchemaGenerationContext ctx) {
        if (type instanceof TypeVariable<?> tv) {
            type = ctx.resolveTypeVariable(tv);
        }
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
            ctx.pushTypeContext(type);
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
            ctx.popTypeContext();
        }
    }

    private static final JsonObject GENERATION_IN_PROGRESS = Json.createObjectBuilder().build();

    public static JsonObjectBuilder generateSubSchema(Type type, SchemaGenerationContext ctx, SchemaAnnotation annotation) {
        // Allow a schema to be overridden from the point that it's used
        var schemaFromAnnotation = annotation.asJsonSchema();
        if (schemaFromAnnotation.isPresent()) {
            return schemaFromAnnotation.get();
        }

        if (type instanceof TypeVariable<?> typeVar) {
            type = ctx.resolveTypeVariable(typeVar);
        }

        SchemaCreationBlueprint blueprint = ctx.getBlueprintRegistry().getSchemaCreationBlueprint(type);
        String description = annotation.description().orElse(null);
        JsonObject defsSchema = ctx.getDefsBuilder().get(TypeKey.from(type, ctx));
        if (defsSchema != null) {
            // Schema is already in defs, just return a reference
            return createReference(ctx.getName(type), description, defsSchema);
        } else if (blueprint.getDefsName().isPresent() && ctx.isMultiUse(type)) {
            // Put a placeholder in the defs map, so that we will make references to the current object
            // rather than trying to generate it again (and getting stuck in a loop)
            ctx.getDefsBuilder().put(TypeKey.from(type, ctx), GENERATION_IN_PROGRESS);
            ctx.pushTypeContext(type);
            // Generate the real schema object and add it to defs
            JsonObject result = blueprint.toJsonSchemaObject(ctx, null).build();
            ctx.popTypeContext();
            ctx.getDefsBuilder().put(TypeKey.from(type, ctx), result);
            // Now return a reference to it
            return createReference(ctx.getName(type), description, result);
        } else {
            // Schema should not be added to defs, just generate it and return
            ctx.pushTypeContext(type);
            JsonObjectBuilder result = blueprint.toJsonSchemaObject(ctx, description);
            ctx.popTypeContext();
            return result;
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
        ctx.getDefsBuilder().forEach((typeKey, schema) -> {
            defs.add(ctx.getName(typeKey), schema);
        });

        result.add(DEFS, defs);
    }

}
