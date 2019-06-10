package io.leangen.graphql.module.common.jackson;

import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class JacksonScalarTypeMapper implements TypeMapper {

    @Override
    public GraphQLScalarType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return JacksonScalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return toGraphQLType(javaType, operationMapper, mappersToSkip, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return JacksonScalars.isScalar(type.getType());
    }
}
