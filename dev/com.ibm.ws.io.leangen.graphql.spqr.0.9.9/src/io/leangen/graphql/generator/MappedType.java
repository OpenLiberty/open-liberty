package io.leangen.graphql.generator;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedType {
    public final AnnotatedType javaType;
    public final GraphQLOutputType graphQLType;

    MappedType(AnnotatedType javaType, GraphQLOutputType graphQLType) {
        if (!(graphQLType instanceof GraphQLTypeReference || graphQLType instanceof GraphQLObjectType)) {
            throw new IllegalArgumentException("Implementation types can only be object types or references to object types");
        }
        this.javaType = javaType;
        this.graphQLType = graphQLType;
    }
    
    public GraphQLObjectType getAsObjectType() {
        return (GraphQLObjectType) graphQLType;
    }
}
