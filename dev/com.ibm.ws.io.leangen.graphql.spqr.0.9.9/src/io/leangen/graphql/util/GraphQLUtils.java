package io.leangen.graphql.util;

import graphql.language.Field;
import graphql.relay.Relay;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLModifiedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import io.leangen.graphql.annotations.GraphQLId;

public class GraphQLUtils {

    public static final String BASIC_INTROSPECTION_QUERY = "{ __schema { queryType { name fields { name type { name kind ofType { name kind fields { name } }}}}}}";
    public static final String FULL_INTROSPECTION_QUERY = "query IntrospectionQuery { __schema { queryType { name } mutationType { name } types { ...FullType } directives { name description args { ...InputValue } onOperation onFragment onField } } } fragment FullType on __Type { kind name description fields(includeDeprecated: true) { name description args { ...InputValue } type { ...TypeRef } isDeprecated deprecationReason } inputFields { ...InputValue } interfaces { ...TypeRef } enumValues(includeDeprecated: true) { name description isDeprecated deprecationReason } possibleTypes { ...TypeRef } } fragment InputValue on __InputValue { name description type { ...TypeRef } defaultValue } fragment TypeRef on __Type { kind name ofType { kind name ofType { kind name ofType { kind name } } } }";

    public static final String CLIENT_MUTATION_ID = "clientMutationId";
    public static final String NODE = "node";
    private static final String EDGES = "edges";
    private static final String PAGE_INFO = "pageInfo";
    private static final String CONNECTION = "Connection";

    public static boolean isRelayId(GraphQLFieldDefinition field) {
        return field.getName().equals(GraphQLId.RELAY_ID_FIELD_NAME) && isRelayId(field.getType());
    }

    public static boolean isRelayId(GraphQLArgument argument) {
        return argument.getName().equals(GraphQLId.RELAY_ID_FIELD_NAME) && isRelayId(argument.getType());
    }

    public static boolean isRelayId(GraphQLType type) {
        return type.equals(Scalars.RelayId);
    }

    public static boolean isRelayNodeInterface(GraphQLType node) {
        return node instanceof GraphQLInterfaceType
                && node.getName().equals(Relay.NODE)
                && ((GraphQLInterfaceType) node).getFieldDefinitions().size() == 1
                && ((GraphQLInterfaceType) node).getFieldDefinition(GraphQLId.RELAY_ID_FIELD_NAME) != null
                && isRelayId(((GraphQLInterfaceType) node).getFieldDefinition(GraphQLId.RELAY_ID_FIELD_NAME));
    }

    public static boolean isRelayConnectionType(GraphQLType type) {
        return type instanceof GraphQLObjectType
                && !type.getName().equals(CONNECTION) && type.getName().endsWith(CONNECTION)
                && ((GraphQLObjectType) type).getFieldDefinition(EDGES) != null
                && ((GraphQLObjectType) type).getFieldDefinition(PAGE_INFO) != null;
    }

    public static boolean isRelayConnectionField(GraphQLFieldDefinition fieldDefinition) {
        return fieldDefinition.getName().equals(EDGES) || fieldDefinition.getName().equals(
                PAGE_INFO);
    }

    public static boolean isRelayEdgeField(GraphQLFieldDefinition fieldDefinition) {
        return fieldDefinition.getName().equals(NODE) || fieldDefinition.getName().equals("cursor");
    }

    public static boolean isIntrospectionType(GraphQLType type) {
        return isIntrospection(type.getName());
    }

    public static boolean isIntrospectionField(Field field) {
        return isIntrospection(field.getName());
    }

    public static GraphQLType unwrapNonNull(GraphQLType type) {
        while (type instanceof GraphQLNonNull) {
            type = ((GraphQLNonNull) type).getWrappedType();
        }
        return type;
    }

    public static GraphQLType unwrap(GraphQLType type) {
        while (type instanceof GraphQLModifiedType) {
            type = ((GraphQLModifiedType) type).getWrappedType();
        }
        return type;
    }

    private static boolean isIntrospection(String name) {
        return name.startsWith("__");
    }
}
