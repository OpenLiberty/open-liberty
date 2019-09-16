package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ObjectScalarMapper extends CachingMapper<GraphQLScalarType, GraphQLScalarType> implements Comparator<AnnotatedType> {

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLDirective[] directives = buildContext.directiveBuilder.buildScalarTypeDirectives(javaType, buildContext.directiveBuilderParams()).stream()
                .map(directive -> operationMapper.toGraphQLDirective(directive, buildContext))
                .toArray(GraphQLDirective[]::new);
        return GenericTypeReflector.isSuperType(Map.class, javaType.getType())
                ? Scalars.graphQLMapScalar(typeName, directives)
                : Scalars.graphQLObjectScalar(typeName, directives);
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class)
                || Object.class.equals(type.getType())
                || GenericTypeReflector.isSuperType(Map.class, type.getType());
    }

    @Override
    public int compare(AnnotatedType o1, AnnotatedType o2) {
        Set<Class<? extends Annotation>> scalarAnnotation = Collections.singleton(GraphQLScalar.class);
        return ClassUtils.removeAnnotations(o1, scalarAnnotation).equals(ClassUtils.removeAnnotations(o2, scalarAnnotation)) ? 0 : -1;
    }
}
