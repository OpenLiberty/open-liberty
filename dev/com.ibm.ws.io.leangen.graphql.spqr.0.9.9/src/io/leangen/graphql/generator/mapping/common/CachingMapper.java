package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class CachingMapper<O extends GraphQLOutputType, I extends GraphQLInputType> implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        String typeName = getTypeName(javaType, buildContext);
        if (buildContext.typeCache.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.typeCache.register(typeName);
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        String typeName = getInputTypeName(javaType, buildContext);
        if (buildContext.typeCache.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.typeCache.register(typeName);
        return toGraphQLInputType(typeName, javaType, operationMapper, buildContext);
    }

    protected abstract O toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext);

    protected abstract I toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext);

    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(0), buildContext.typeInfoGenerator, buildContext.messageBundle);
    }

    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, getTypeArguments(1), buildContext.typeInfoGenerator, buildContext.messageBundle);
    }

    private String getTypeName(AnnotatedType javaType, AnnotatedType graphQLType, TypeInfoGenerator typeInfoGenerator, MessageBundle messageBundle) {
        if (GenericTypeReflector.isSuperType(GraphQLScalarType.class, graphQLType.getType())) {
            return typeInfoGenerator.generateScalarTypeName(javaType, messageBundle);
        }
        if (GenericTypeReflector.isSuperType(GraphQLEnumType.class, graphQLType.getType())) {
            return typeInfoGenerator.generateTypeName(javaType, messageBundle);
        }
        if (GenericTypeReflector.isSuperType(GraphQLInputType.class, graphQLType.getType())) {
            return typeInfoGenerator.generateInputTypeName(javaType, messageBundle);
        }
        return typeInfoGenerator.generateTypeName(javaType, messageBundle);
    }

    private AnnotatedType getTypeArguments(int index) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), CachingMapper.class.getTypeParameters()[index]);
    }
}
