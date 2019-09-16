package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Directives;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ObjectTypeMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

    @Override
    public GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));

        List<GraphQLFieldDefinition> fields = getFields(javaType, buildContext, operationMapper);
        fields.forEach(typeBuilder::field);

        List<GraphQLOutputType> interfaces = getInterfaces(javaType, fields, buildContext, operationMapper);
        interfaces.forEach(inter -> {
            if (inter instanceof GraphQLInterfaceType) {
                typeBuilder.withInterface((GraphQLInterfaceType) inter);
            } else {
                typeBuilder.withInterface((GraphQLTypeReference) inter);
            }
        });

        typeBuilder.withDirective(Directives.mappedType(javaType));
        buildContext.directiveBuilder.buildObjectTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                typeBuilder.withDirective(operationMapper.toGraphQLDirective(directive, buildContext)));

        GraphQLObjectType type = typeBuilder.build();
        interfaces.forEach(inter -> buildContext.typeRegistry.registerCovariantType(inter.getName(), javaType, type));
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType, buildContext.messageBundle));

        InputFieldBuilderParams params = InputFieldBuilderParams.builder()
                .withType(javaType)
                .withEnvironment(buildContext.globalEnvironment)
                .build();
        buildContext.inputFieldBuilders.getInputFields(params).forEach(field -> typeBuilder.field(operationMapper.toGraphQLInputField(field, buildContext)));
        if (ClassUtils.isAbstract(javaType)) {
            createInputDisambiguatorField(javaType, buildContext).ifPresent(typeBuilder::field);
        }

        typeBuilder.withDirective(Directives.mappedType(javaType));
        buildContext.directiveBuilder.buildInputObjectTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                typeBuilder.withDirective(operationMapper.toGraphQLDirective(directive, buildContext)));

        return typeBuilder.build();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    protected List<GraphQLFieldDefinition> getFields(AnnotatedType javaType, BuildContext buildContext, OperationMapper operationMapper) {
        List<GraphQLFieldDefinition> fields = buildContext.operationRegistry.getChildQueries(javaType).stream()
                .map(childQuery -> operationMapper.toGraphQLField(childQuery, buildContext))
                .collect(Collectors.toList());
        return sortFields(fields, buildContext.typeInfoGenerator.getFieldOrder(javaType, buildContext.messageBundle));
    }

    @SuppressWarnings("WeakerAccess")
    protected List<GraphQLOutputType> getInterfaces(AnnotatedType javaType,
                                                    List<GraphQLFieldDefinition> fields, BuildContext buildContext, OperationMapper operationMapper) {

        List<GraphQLOutputType> interfaces = new ArrayList<>();
        if (buildContext.relayMappingConfig.inferNodeInterface && fields.stream().anyMatch(GraphQLUtils::isRelayId)) {
            interfaces.add(buildContext.node);
        }
        buildContext.interfaceStrategy.getInterfaces(javaType).forEach(
                inter -> interfaces.add(operationMapper.toGraphQLType(inter, buildContext)));

        return interfaces;
    }

    private static List<GraphQLFieldDefinition> sortFields(List<GraphQLFieldDefinition> fields, String[] specifiedFieldOrder) {
        Map<String, GraphQLFieldDefinition> fieldMap = new TreeMap<>();
        for (GraphQLFieldDefinition field : fields) {
            fieldMap.put(field.getName(), field);
        }
        List<GraphQLFieldDefinition> result = new ArrayList<>();
        for (String name : specifiedFieldOrder) {
            if (fieldMap.containsKey(name)) {
                result.add(fieldMap.remove(name));
            }
        }
        result.addAll(fieldMap.values());
        return result;
    }

    @SuppressWarnings("WeakerAccess")
    protected Optional<GraphQLInputObjectField> createInputDisambiguatorField(AnnotatedType javaType, BuildContext buildContext) {
        Class<?> raw = ClassUtils.getRawType(javaType.getType());
        String typeName = buildContext.typeInfoGenerator.generateTypeName(GenericTypeReflector.annotate(raw), buildContext.messageBundle) + "TypeDisambiguator";
        GraphQLInputType fieldType = null;
        if (buildContext.typeCache.contains(typeName)) {
            fieldType = new GraphQLTypeReference(typeName);
        } else {
            List<AnnotatedType> impls = buildContext.abstractInputHandler.findConcreteSubTypes(raw, buildContext).stream()
                    .map(GenericTypeReflector::annotate)
                    .collect(Collectors.toList());
            if (impls.size() > 1) {
                buildContext.typeCache.register(typeName);
                GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum()
                        .name(typeName)
                        .description("Input type discriminator");
                impls.stream()
                        .map(t -> buildContext.typeInfoGenerator.generateTypeName(t, buildContext.messageBundle))
                        .forEach(builder::value);
                fieldType = builder.build();
            }
        }
        return Optional.ofNullable(fieldType).map(type -> newInputObjectField()
                .name(ValueMapper.TYPE_METADATA_FIELD_NAME)
                .type(type)
                .build());
    }
}
