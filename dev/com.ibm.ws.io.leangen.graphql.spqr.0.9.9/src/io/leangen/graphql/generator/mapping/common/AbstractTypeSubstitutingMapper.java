package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeSubstituter;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
//The substitute type S is reflectively accessed by the default #getSubstituteType impl
@SuppressWarnings("unused")
public abstract class AbstractTypeSubstitutingMapper<S> implements TypeMapper, TypeSubstituter {

    protected final AnnotatedType substituteType;

    public AbstractTypeSubstitutingMapper() {
        substituteType = GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeSubstitutingMapper.class.getTypeParameters()[0]);
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return operationMapper.toGraphQLType(getSubstituteType(javaType), mappersToSkip, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        return operationMapper.toGraphQLInputType(getSubstituteType(javaType), mappersToSkip, buildContext);
    }

    /**
     * Returns the type to map instead of the original. This implementation always returns the type of the
     * generic type parameter {@code S}.
     *
     * @param original The type to be replaced
     * @return The substitute type to use for mapping
     */
    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return substituteType;
    }
}
