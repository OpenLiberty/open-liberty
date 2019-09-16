package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnumMapToObjectTypeAdapter<E extends Enum<E>, V> extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> implements InputConverter<EnumMap<E, V>, Map<String, V>> {

    private final EnumMapper enumMapper;

    public EnumMapToObjectTypeAdapter(EnumMapper enumMapper) {
        this.enumMapper = enumMapper;
    }


    @Override
    protected GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));

        Enum<E>[] keys = ClassUtils.<E>getRawType(getElementType(javaType, 0).getType()).getEnumConstants();
        Arrays.stream(keys).forEach(enumValue -> builder.field(GraphQLFieldDefinition.newFieldDefinition()
                .name(enumMapper.getValueName(enumValue, buildContext.messageBundle))
                .description(enumMapper.getValueDescription(enumValue, buildContext.messageBundle))
                .deprecate(enumMapper.getValueDeprecationReason(enumValue, buildContext.messageBundle))
                .type(operationMapper.toGraphQLType(getElementType(javaType, 1), buildContext))
                .dataFetcher(env -> ((Map)env.getSource()).get(enumValue))
                .build()));
        return builder.build();
    }

    @Override
    protected GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType, buildContext.messageBundle));

        Enum[] keys = (Enum[]) ClassUtils.getRawType(getElementType(javaType, 0).getType()).getEnumConstants();
        Arrays.stream(keys).forEach(enumValue -> builder.field(GraphQLInputObjectField.newInputObjectField()
                .name(enumMapper.getValueName(enumValue, buildContext.messageBundle))
                .description(enumMapper.getValueDescription(enumValue, buildContext.messageBundle))
                .type(operationMapper.toGraphQLInputType(getElementType(javaType, 1), buildContext))
                .build()));
        return builder.build();
    }

    @Override
    public EnumMap<E, V> convertInput(Map<String, V> substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        Map<String, E> values = Arrays.stream((ClassUtils.<E>getRawType(getElementType(type, 0).getType()).getEnumConstants()))
                .collect(Collectors.toMap(e -> (enumMapper.getValueName(e, environment.messageBundle)), Function.identity()));
        Map<E, V> m = substitute.entrySet().stream().collect(Collectors.toMap(e -> values.get(e.getKey()), Map.Entry::getValue));
        return new EnumMap<>(m);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(EnumMap.class, type.getType());
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType keyType = getElementType(original, 0);
        AnnotatedType valueType = getElementType(original, 1);
        return TypeFactory.parameterizedAnnotatedClass(Map.class, original.getAnnotations(),  GenericTypeReflector.annotate(String.class, keyType.getAnnotations()), valueType);
    }

    private AnnotatedType getElementType(AnnotatedType javaType, int index) {
        return GenericTypeReflector.getTypeParameter(javaType, Map.class.getTypeParameters()[index]);
    }
}
