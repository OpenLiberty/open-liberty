package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

import static graphql.schema.GraphQLInputObjectType.newInputObject;

public class AnnotationMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

    @Override
    protected GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        throw new UnsupportedOperationException("Annotation type used as output");
    }

    @Override
    protected GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType, buildContext.messageBundle));

        InputFieldBuilderParams params = InputFieldBuilderParams.builder()
                .withType(javaType)
                .withEnvironment(buildContext.globalEnvironment)
                .build();
        buildContext.inputFieldBuilders.getInputFields(params).forEach(field -> typeBuilder.field(operationMapper.toGraphQLInputField(field, buildContext)));

        return typeBuilder.build();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isAnnotation();
    }
}
