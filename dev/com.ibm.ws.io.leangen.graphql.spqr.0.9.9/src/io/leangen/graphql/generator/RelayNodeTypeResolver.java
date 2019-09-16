package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.util.GraphQLUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
class RelayNodeTypeResolver extends DelegatingTypeResolver {

    RelayNodeTypeResolver(TypeRegistry typeRegistry, TypeInfoGenerator typeInfoGenerator, MessageBundle messageBundle) {
        super(GraphQLUtils.NODE, typeRegistry, typeInfoGenerator, messageBundle);
    }
}
