package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.annotations.types.Interface;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.util.Directives;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InterfaceMapper extends CachingMapper<GraphQLInterfaceType, GraphQLInputObjectType> {

    private final InterfaceMappingStrategy interfaceStrategy;
    private final ObjectTypeMapper objectTypeMapper;

    public InterfaceMapper(InterfaceMappingStrategy interfaceStrategy, ObjectTypeMapper objectTypeMapper) {
        this.interfaceStrategy = Objects.requireNonNull(interfaceStrategy);
        this.objectTypeMapper = Objects.requireNonNull(objectTypeMapper);
    }

    @Override
    public GraphQLInterfaceType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInterfaceType.Builder typeBuilder = newInterface()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));

        List<GraphQLFieldDefinition> fields = objectTypeMapper.getFields(javaType, buildContext, operationMapper);
        fields.forEach(typeBuilder::field);

        typeBuilder.typeResolver(buildContext.typeResolver);
        typeBuilder.withDirective(Directives.mappedType(javaType));
        buildContext.directiveBuilder.buildInterfaceTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                typeBuilder.withDirective(operationMapper.toGraphQLDirective(directive, buildContext)));
        GraphQLInterfaceType type = typeBuilder.build();

        registerImplementations(javaType, type, operationMapper, buildContext);
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return objectTypeMapper.toGraphQLInputType(typeName, javaType, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }

    private void registerImplementations(AnnotatedType javaType, GraphQLInterfaceType type, OperationMapper operationMapper, BuildContext buildContext) {
        if (isImplementationAutoDiscoveryEnabled(javaType)) {
            buildContext.implDiscoveryStrategy.findImplementations(javaType, getScanPackages(javaType), buildContext).forEach(impl ->
                    getImplementingType(impl, operationMapper, buildContext)
                            .ifPresent(implType -> buildContext.typeRegistry.registerDiscoveredCovariantType(type.getName(), impl, implType)));
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean isImplementationAutoDiscoveryEnabled(AnnotatedType javaType) {
        return javaType.isAnnotationPresent(Interface.class) && javaType.getAnnotation(Interface.class).implementationAutoDiscovery();
    }

    @SuppressWarnings("WeakerAccess")
    protected String[] getScanPackages(AnnotatedType javaType) {
        return javaType.isAnnotationPresent(Interface.class) ? javaType.getAnnotation(Interface.class).scanPackages() : Utils.emptyArray();
    }

    private Optional<GraphQLObjectType> getImplementingType(AnnotatedType implType, OperationMapper operationMapper, BuildContext buildContext) {
        return Optional.of(implType)
                .filter(impl -> !interfaceStrategy.supports(impl))
                .map(impl -> operationMapper.toGraphQLType(impl, buildContext))
                .filter(impl -> impl instanceof GraphQLObjectType)
                .map(impl -> (GraphQLObjectType) impl);
    }
}
