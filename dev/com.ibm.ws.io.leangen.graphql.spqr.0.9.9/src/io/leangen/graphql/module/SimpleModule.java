package io.leangen.graphql.module;

import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface SimpleModule extends Module {

    default List<ResolverBuilder> getResolverBuilders() {
        return Collections.emptyList();
    }

    default List<ResolverBuilder> getNestedResolverBuilders() {
        return Collections.emptyList();
    }

    default List<TypeMapper> getTypeMappers() {
        return Collections.emptyList();
    }

    default List<OutputConverter<?, ?>> getOutputConverters() {
        return Collections.emptyList();
    }

    default List<InputConverter<?, ?>> getInputConverters() {
        return Collections.emptyList();
    }

    default List<ArgumentInjector> getArgumentInjectors() {
        return Collections.emptyList();
    }

    default Optional<TypeInfoGenerator> getTypeInfoGenerator() {
        return Optional.empty();
    }

    @Override
    default void setUp(SetupContext context) {
        if (!getResolverBuilders().isEmpty()) {
            context.getSchemaGenerator().withResolverBuilders(getResolverBuilders().toArray(new ResolverBuilder[0]));
        }
        if (!getNestedResolverBuilders().isEmpty()) {
            context.getSchemaGenerator().withNestedResolverBuilders(getNestedResolverBuilders().toArray(new ResolverBuilder[0]));
        }
        if (!getTypeMappers().isEmpty()) {
            context.getSchemaGenerator().withTypeMappers(getTypeMappers().toArray(new TypeMapper[0]));
        }
        if (!getOutputConverters().isEmpty()) {
            context.getSchemaGenerator().withOutputConverters(getOutputConverters().toArray(new OutputConverter[0]));
        }
        if (!getInputConverters().isEmpty()) {
            context.getSchemaGenerator().withInputConverters(getInputConverters().toArray(new InputConverter[0]));
        }
        if (!getArgumentInjectors().isEmpty()) {
            context.getSchemaGenerator().withArgumentInjectors(getArgumentInjectors().toArray(new ArgumentInjector[0]));
        }
        if (getTypeInfoGenerator().isPresent()) {
            context.getSchemaGenerator().withTypeInfoGenerator(getTypeInfoGenerator().get());
        }
    }
}
