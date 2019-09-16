package io.leangen.graphql;

import io.leangen.graphql.execution.GlobalEnvironment;

public class ExtendedGeneratorConfiguration extends GeneratorConfiguration {

    public final GlobalEnvironment environment;

    ExtendedGeneratorConfiguration(GeneratorConfiguration config, GlobalEnvironment environment) {
        super(config.interfaceMappingStrategy, config.scalarDeserializationStrategy, config.typeTransformer, config.basePackages, config.javaDeprecationConfig);
        this.environment = environment;
    }
}
