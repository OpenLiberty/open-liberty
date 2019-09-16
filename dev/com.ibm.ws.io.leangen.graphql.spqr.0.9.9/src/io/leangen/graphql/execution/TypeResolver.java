package io.leangen.graphql.execution;

import graphql.schema.GraphQLObjectType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface TypeResolver {

    GraphQLObjectType resolveType(TypeResolutionEnvironment environment);
}
