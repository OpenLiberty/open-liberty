package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ListMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return new GraphQLList(operationMapper.toGraphQLType(getElementType(javaType), buildContext));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return new GraphQLList(operationMapper.toGraphQLInputType(getElementType(javaType), buildContext));
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Collection.class, type.getType());
    }
    
    private AnnotatedType getElementType(AnnotatedType javaType) {
        return GenericTypeReflector.getTypeParameter(javaType, Collection.class.getTypeParameters()[0]);
    }
}
