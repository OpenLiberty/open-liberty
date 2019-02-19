package io.leangen.graphql.execution;

import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

public class TypeResolutionEnvironment extends graphql.TypeResolutionEnvironment {

    private final TypeRegistry typeRegistry;
    private final TypeInfoGenerator typeInfoGenerator;

    public TypeResolutionEnvironment(graphql.TypeResolutionEnvironment environment,
                                     TypeRegistry typeRegistry,
                                     TypeInfoGenerator typeInfoGenerator) {
        super(environment.getObject(), environment.getArguments(), environment.getField(), environment.getFieldType(), environment.getSchema(), environment.getContext());
        this.typeRegistry = typeRegistry;
        this.typeInfoGenerator = typeInfoGenerator;
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public TypeInfoGenerator getTypeInfoGenerator() {
        return typeInfoGenerator;
    }
}
