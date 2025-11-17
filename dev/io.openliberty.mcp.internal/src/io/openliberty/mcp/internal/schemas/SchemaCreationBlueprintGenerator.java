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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.openliberty.mcp.internal.schemas.TypeUtility.MapTypes;
import io.openliberty.mcp.internal.schemas.blueprints.ClassSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.EnumSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.ListSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.MapSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.OptionalSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.SchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.TypeVariableSchemaCreationBlueprint;
import io.openliberty.mcp.internal.schemas.blueprints.WildcardSchemaCreationBlueprint;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 * Methods for generating SchemaCreationBlueprints. This class should always be accessed via {@link SchemaCreationBlueprintRegistry}
 */
public class SchemaCreationBlueprintGenerator {

    /**
     * generates blueprint for a type
     *
     * @param type the type to generate the blueprint for
     * @return the blueprint
     */
    public static SchemaCreationBlueprint generateSchemaCreationBlueprint(Type type) {
        if (type instanceof Class<?> cls) {
            if (cls.isEnum()) {
                return generateEnumSchemaCreationBlueprint(type);

            } else if (cls.isArray()) {
                return generateArraySchemaCreationBlueprint(cls);

            } else if (Optional.class.isAssignableFrom(cls)) {
                return generateRawOptionalSchemaCreationBlueprint(type);

            } else if (Map.class.isAssignableFrom(cls)) {
                return generateRawMapSchemaCreationBlueprint(type);

            } else if (Collection.class.isAssignableFrom(cls)) {
                return generateRawCollectionSchemaCreationBlueprint(type);

            } else {
                ClassSchemaCreationBlueprint schemaCreationContext = generateClassSchemaCreationBlueprint(type);
                return schemaCreationContext;
            }

        } else if (type instanceof ParameterizedType pt) {
            if (Optional.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                return generateParameterizedOptionalSchemaCreationBlueprint(type);

            } else if (Map.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                return generateParameterizedMapSchemaCreationBlueprint(type);

            } else if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                return generateParameterizedCollectionSchemaCreationBlueprint(type);
            } else {
                ClassSchemaCreationBlueprint schemaCreationContext = generateParameterizedClassSchemaCreationBlueprint(pt);
                return schemaCreationContext;
            }

        } else if (type instanceof TypeVariable) {
            TypeVariableSchemaCreationBlueprint schemaCreationContext = generateTypeVariableSchemaCreationBlueprint(type);
            return schemaCreationContext;

        } else if (type instanceof WildcardType) {
            WildcardSchemaCreationBlueprint schemaCreationContext = generateWildcardSchemaCreationBlueprint(type);
            return schemaCreationContext;

        } else if (type instanceof GenericArrayType) {
            return generateGenericArraySchemaCreationBlueprint(type);
        } else {
            throw new IllegalArgumentException("Cannot generate schema blueprint for " + type);
        }
    }

    public static ClassSchemaCreationBlueprint generateClassSchemaCreationBlueprint(Type type) {
        Class<?> cls = (Class<?>) type;
        List<JsonProperty> properties = JsonProperty.extract(cls);
        return new ClassSchemaCreationBlueprint(cls,
                                                getInputFields(properties),
                                                getOutputFields(properties));
    }

    public static ClassSchemaCreationBlueprint generateParameterizedClassSchemaCreationBlueprint(Type type) {
        if (type instanceof ParameterizedType pt) {
            Class<?> cls = (Class<?>) pt.getRawType();
            Map<TypeVariable<?>, Type> genericMap = new HashMap<>();
            TypeVariable<?>[] typeVariables = cls.getTypeParameters();
            Type[] actualTypes = pt.getActualTypeArguments();

            for (int i = 0; i < typeVariables.length; i++) {
                genericMap.put(typeVariables[i], actualTypes[i]);
            }
            List<JsonProperty> properties = JsonProperty.extract(cls);

            return new ClassSchemaCreationBlueprint((Class<?>) pt.getRawType(),
                                                    getInputFields(properties, genericMap),
                                                    getOutputFields(properties, genericMap));
        }
        return null;

    }

    public static TypeVariableSchemaCreationBlueprint generateTypeVariableSchemaCreationBlueprint(Type type) {
        if (type instanceof TypeVariable<?> tv) {
            return new TypeVariableSchemaCreationBlueprint(type, tv);
        }
        return null;
    }

    public static WildcardSchemaCreationBlueprint generateWildcardSchemaCreationBlueprint(Type type) {
        if (type instanceof WildcardType wt) {
            return new WildcardSchemaCreationBlueprint(type, wt, wt.getUpperBounds(), wt.getLowerBounds());
        }
        return null;
    }

    public static EnumSchemaCreationBlueprint generateEnumSchemaCreationBlueprint(Type type) {
        Class<?> cls = (Class<?>) type;
        List<String> enumValues = getEnumConstants(cls);
        return new EnumSchemaCreationBlueprint(cls, enumValues);
    }

    public static ListSchemaCreationBlueprint generateRawCollectionSchemaCreationBlueprint(Type type) {
        return new ListSchemaCreationBlueprint(type, null);
    }

    public static ListSchemaCreationBlueprint generateArraySchemaCreationBlueprint(Type type) {
        Type elementType = ((Class<?>) type).getComponentType();
        return new ListSchemaCreationBlueprint(type, elementType);
    }

    public static ListSchemaCreationBlueprint generateGenericArraySchemaCreationBlueprint(Type type) {
        if (type instanceof GenericArrayType gat) {
            Type elementType = gat.getGenericComponentType();
            return new ListSchemaCreationBlueprint(type, elementType);
        }
        return null;

    }

    public static MapSchemaCreationBlueprint generateRawMapSchemaCreationBlueprint(Type type) {
        return new MapSchemaCreationBlueprint(type, null, null);
    }

    public static OptionalSchemaCreationBlueprint generateRawOptionalSchemaCreationBlueprint(Type type) {
        return new OptionalSchemaCreationBlueprint(type, null);
    }

    public static ListSchemaCreationBlueprint generateParameterizedCollectionSchemaCreationBlueprint(Type type) {
        Type elementType = TypeUtility.getCollectionType(type);
        return new ListSchemaCreationBlueprint(type, elementType);
    }

    public static MapSchemaCreationBlueprint generateParameterizedMapSchemaCreationBlueprint(Type type) {
        MapTypes mapTypes = TypeUtility.getMapTypes(type);
        Type keyType = mapTypes.key();
        Type valueType = mapTypes.value();
        if (keyType.equals(String.class) || isEnum(keyType)) {
            return new MapSchemaCreationBlueprint(type, keyType, valueType);
        } else {
            throw new RuntimeException(type + " represents a map which does not have String or Enum keys");
        }
    }

    private static boolean isEnum(Type type) {
        if (type instanceof Class<?> cls) {
            return cls.isEnum();
        } else {
            return false;
        }
    }

    public static OptionalSchemaCreationBlueprint generateParameterizedOptionalSchemaCreationBlueprint(Type type) {
        Type elementType = TypeUtility.getOptionalType(type);
        return new OptionalSchemaCreationBlueprint(type, elementType);
    }

    private static List<String> getEnumConstants(Class<?> cls) {
        List<String> result = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.isEnumConstant()) {
                String name = field.getAnnotation(JsonbProperty.class) != null ? field.getAnnotation(JsonbProperty.class).value() : field.getName();
                if (field.getAnnotation(JsonbTransient.class) == null)
                    result.add(name);
            }
        }
        return result;
    }

    private static List<FieldInfo> getInputFields(List<JsonProperty> properties) {
        return properties.stream()
                         .filter(p -> p.isInput())
                         .map(p -> new FieldInfo(p.getInputName(), p.getInputType(), p.getInputAnnotations(), SchemaDirection.INPUT))
                         .collect(Collectors.toList());
    }

    private static List<FieldInfo> getInputFields(List<JsonProperty> properties, Map<TypeVariable<?>, Type> genericMap) {
        return properties.stream()
                         .filter(p -> p.isInput())
                         .map(p -> new FieldInfo(p.getInputName(), genericMap.get(p.getInputType()), p.getInputAnnotations(), SchemaDirection.INPUT))
                         .collect(Collectors.toList());
    }

    private static List<FieldInfo> getOutputFields(List<JsonProperty> properties) {
        return properties.stream()
                         .filter(p -> p.isOutput())
                         .map(p -> new FieldInfo(p.getOutputName(), p.getOutputType(), p.getOutputAnnotations(), SchemaDirection.OUTPUT))
                         .collect(Collectors.toList());
    }

    private static List<FieldInfo> getOutputFields(List<JsonProperty> properties, Map<TypeVariable<?>, Type> genericMap) {
        return properties.stream()
                         .filter(p -> p.isOutput())
                         .map(p -> new FieldInfo(p.getOutputName(), genericMap.get(p.getOutputType()), p.getOutputAnnotations(), SchemaDirection.OUTPUT))
                         .collect(Collectors.toList());
    }

    public record FieldInfo(String name, Type type, Annotation[] annotations, SchemaDirection direction) {}

}
