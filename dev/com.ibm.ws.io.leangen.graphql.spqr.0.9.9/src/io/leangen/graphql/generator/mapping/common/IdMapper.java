package io.leangen.graphql.generator.mapping.common;

import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import org.eclipse.microprofile.graphql.Id;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class IdMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return Scalars.GraphQLID;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return Scalars.GraphQLID;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(Id.class) && GenericTypeReflector.isSuperType(String.class, type.getType());
    }
}
