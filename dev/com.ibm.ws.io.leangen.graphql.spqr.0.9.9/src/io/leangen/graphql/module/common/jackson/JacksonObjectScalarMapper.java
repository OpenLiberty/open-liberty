package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.node.POJONode;
import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class JacksonObjectScalarMapper implements TypeMapper {

    @Override
    public GraphQLScalarType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return JacksonObjectScalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        if (POJONode.class.equals(javaType.getType())) {
            throw new UnsupportedOperationException(POJONode.class.getSimpleName() + " can not be used as input");
        }
        return toGraphQLType(javaType, operationMapper, mappersToSkip, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return JacksonObjectScalars.isScalar(type.getType());
    }
}
