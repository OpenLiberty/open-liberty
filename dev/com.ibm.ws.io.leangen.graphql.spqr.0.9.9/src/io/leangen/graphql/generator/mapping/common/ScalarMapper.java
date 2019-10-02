package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.util.Scalars;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ScalarMapper extends CachingMapper<GraphQLScalarType, GraphQLScalarType> {
    
    private final Scalars scalars;

    public ScalarMapper(Scalars scalars) {
        this.scalars = scalars;
    }

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return scalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return scalars.isScalar(type.getType());
    }

    @Override
    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return scalars.toGraphQLScalarType(type.getType()).getName();
    }

    @Override
    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, buildContext);
    }
}
