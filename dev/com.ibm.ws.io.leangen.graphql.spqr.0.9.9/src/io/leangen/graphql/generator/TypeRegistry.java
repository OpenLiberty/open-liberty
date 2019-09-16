package io.leangen.graphql.generator;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Directives;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public class TypeRegistry {

    private final Map<String, Map<String, MappedType>> covariantOutputTypes = new ConcurrentHashMap<>();
    private final Set<GraphQLObjectType> discoveredTypes = new HashSet<>();

    private static final Logger log = LoggerFactory.getLogger(TypeRegistry.class);

    public TypeRegistry(Collection<GraphQLType> knownTypes) {
        //extract known interface implementations
        knownTypes.stream()
                .filter(type -> type instanceof GraphQLObjectType && Directives.isMappedType(type))
                .map(type -> (GraphQLObjectType) type)
                .forEach(obj -> obj.getInterfaces().forEach(
                        inter -> registerCovariantType(inter.getName(), Directives.getMappedType(obj), obj)));

        //extract known union members
        knownTypes.stream()
                .filter(type -> type instanceof GraphQLUnionType)
                .map(type -> (GraphQLUnionType) type)
                .forEach(union -> union.getTypes().stream()
                        .filter(type -> type instanceof GraphQLObjectType && Directives.isMappedType(type))
                        .map(type -> (GraphQLObjectType) type)
                        .forEach(obj -> registerCovariantType(union.getName(), Directives.getMappedType(obj), obj)));
    }

    public void registerDiscoveredCovariantType(String compositeTypeName, AnnotatedType javaSubType, GraphQLObjectType subType) {
        this.discoveredTypes.add(subType);
        registerCovariantType(compositeTypeName, javaSubType, subType);
    }
    
    public void registerCovariantType(String compositeTypeName, AnnotatedType javaSubType, GraphQLOutputType subType) {
        this.covariantOutputTypes.putIfAbsent(compositeTypeName, new ConcurrentHashMap<>());
        Map<String, MappedType> covariantTypes = this.covariantOutputTypes.get(compositeTypeName);
        //never overwrite an exact type with a reference
        if (subType instanceof GraphQLObjectType || covariantTypes.get(subType.getName()) == null || covariantTypes.get(subType.getName()).graphQLType instanceof GraphQLTypeReference) {
            covariantTypes.put(subType.getName(), new MappedType(javaSubType, subType));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public List<MappedType> getOutputTypes(String compositeTypeName, Class objectType) {
        Map<String, MappedType> mappedTypes = this.covariantOutputTypes.get(compositeTypeName);
        if (mappedTypes == null) return Collections.emptyList();
        if (objectType == null) return new ArrayList<>(mappedTypes.values());
        return mappedTypes.values().stream()
                .filter(mappedType -> ClassUtils.getRawType(mappedType.javaType.getType()).isAssignableFrom(objectType))
                .collect(Collectors.toList());
    }

    public List<MappedType> getOutputTypes(String compositeTypeName) {
        return new ArrayList<>(this.covariantOutputTypes.get(compositeTypeName).values());
    }

    public Set<GraphQLObjectType> getDiscoveredTypes() {
        return discoveredTypes;
    }
    
    void resolveTypeReferences(Map<String, GraphQLType> resolvedTypes) {
        for (Map<String, MappedType> covariantTypes : this.covariantOutputTypes.values()) {
            Set<String> toRemove = new HashSet<>();
            for (Map.Entry<String, MappedType> entry : covariantTypes.entrySet()) {
                if (entry.getValue().graphQLType instanceof GraphQLTypeReference) {
                    GraphQLOutputType resolvedType = (GraphQLOutputType) resolvedTypes.get(entry.getKey());
                    if (resolvedType != null) {
                        entry.setValue(new MappedType(entry.getValue().javaType, resolvedType));
                    } else {
                        log.warn("Type reference " + entry.getKey() + " could not be replaced correctly. " +
                                "This can occur when the schema generator is initialized with " +
                                "additional types not built by GraphQL SPQR. If this type implements " +
                                "Node, in some edge cases it may end up not exposed via the 'node' query.");
                        //the edge case is when the primary resolver returns an interface or a union and not the node type directly
                        toRemove.add(entry.getKey());
                    }
                }
            }
            toRemove.forEach(covariantTypes::remove);
            covariantTypes.replaceAll((typeName, mapped) -> mapped.graphQLType instanceof GraphQLTypeReference
                    ? new MappedType(mapped.javaType, (GraphQLOutputType) resolvedTypes.get(typeName)) : mapped);
        }
    }
}
