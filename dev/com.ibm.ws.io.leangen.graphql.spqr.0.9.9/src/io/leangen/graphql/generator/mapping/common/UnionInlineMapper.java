package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.union.Union;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class UnionInlineMapper extends UnionMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        GraphQLUnion annotation = javaType.getAnnotation(GraphQLUnion.class);
        List<AnnotatedType> possibleJavaTypes = Arrays.asList(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments());
        return toGraphQLUnion(buildContext.interpolate(annotation.name()), buildContext.interpolate(annotation.description()), javaType, possibleJavaTypes, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Union.class, type.getType());
    }
}
