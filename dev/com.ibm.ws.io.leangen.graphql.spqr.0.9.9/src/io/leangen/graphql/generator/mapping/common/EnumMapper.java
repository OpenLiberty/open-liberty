package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import io.leangen.graphql.annotations.EnumValue;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.JavaDeprecationMappingConfig;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.ReservedStrings;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class EnumMapper extends CachingMapper<GraphQLEnumType, GraphQLEnumType> {

    private final JavaDeprecationMappingConfig javaDeprecationConfig;

    public EnumMapper(JavaDeprecationMappingConfig javaDeprecationConfig) {
        this.javaDeprecationConfig = javaDeprecationConfig;
    }

    @Override
    public GraphQLEnumType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));
        buildContext.directiveBuilder.buildEnumTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                enumBuilder.withDirective(operationMapper.toGraphQLDirective(directive, buildContext)));
        addOptions(enumBuilder, javaType, operationMapper, buildContext);
        return enumBuilder.build();
    }

    @Override
    public GraphQLEnumType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return toGraphQLType(typeName, javaType, operationMapper, buildContext);
    }

    private void addOptions(GraphQLEnumType.Builder enumBuilder, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        MessageBundle messageBundle = buildContext.messageBundle;
        sortEnumValues((Enum[]) ClassUtils.getRawType(javaType.getType()).getEnumConstants(), buildContext.typeInfoGenerator.getFieldOrder(javaType, messageBundle), messageBundle).stream()
                .map(enumConst -> (Enum<?>) enumConst)
                .forEach(enumConst -> enumBuilder.value(GraphQLEnumValueDefinition.newEnumValueDefinition()
                        .name(getValueName(enumConst, messageBundle))
                        .value(enumConst)
                        .description(getValueDescription(enumConst, messageBundle))
                        .deprecationReason(getValueDeprecationReason(enumConst, messageBundle))
                        .withDirectives(getValueDirectives(enumConst, operationMapper, buildContext))
                        .build()));
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueName(Enum<?> value, MessageBundle messageBundle) {
        EnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(EnumValue.class);
        return annotation != null && !annotation.value().isEmpty() ? messageBundle.interpolate(annotation.value()) : value.name();
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueDescription(Enum<?> value, MessageBundle messageBundle) {
        EnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(EnumValue.class);
        return annotation != null ? messageBundle.interpolate(annotation.description()) : null;
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueDeprecationReason(Enum<?> value, MessageBundle messageBundle) {
        io.leangen.graphql.annotations.Deprecated annotation = ClassUtils.getEnumConstantField(value).getAnnotation(io.leangen.graphql.annotations.Deprecated.class);
        if (annotation != null) {
            return ReservedStrings.decode(messageBundle.interpolate(annotation.value()));
        }
        Deprecated deprecated = ClassUtils.getEnumConstantField(value).getAnnotation(Deprecated.class);
        return javaDeprecationConfig.enabled && deprecated != null ? javaDeprecationConfig.deprecationReason : null;
    }

    protected GraphQLDirective[] getValueDirectives(Enum<?> value, OperationMapper operationMapper, BuildContext buildContext) {
        return buildContext.directiveBuilder.buildEnumValueDirectives(value, buildContext.directiveBuilderParams()).stream()
                .map(directive -> operationMapper.toGraphQLDirective(directive, buildContext))
                .toArray(GraphQLDirective[]::new);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isEnum();
    }

    private List<Enum> sortEnumValues(Enum[] values, String[] order, MessageBundle messageBundle) {
        Map<String, Enum> fieldMap = new TreeMap<>();
        for (Enum value : values) {
            fieldMap.put(getValueName(value, messageBundle), value);
        }
        List<Enum> result = new ArrayList<>();
        for (String name : order) {
            if (fieldMap.containsKey(name)) {
                result.add(fieldMap.remove(name));
            }
        }
        result.addAll(fieldMap.values());
        return result;
    }
}
