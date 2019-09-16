package io.leangen.graphql.module;

import io.leangen.graphql.GraphQLSchemaGenerator;

public interface Module {

    void setUp(SetupContext context);

    interface SetupContext {
        GraphQLSchemaGenerator getSchemaGenerator();
    }
}
