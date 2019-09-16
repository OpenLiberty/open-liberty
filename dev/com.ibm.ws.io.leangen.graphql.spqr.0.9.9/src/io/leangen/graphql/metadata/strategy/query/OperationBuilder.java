package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface OperationBuilder {

    Operation buildQuery(Type context, List<Resolver> resolvers, GlobalEnvironment environment);
    Operation buildMutation(Type context, List<Resolver> resolvers, GlobalEnvironment environment);
    Operation buildSubscription(Type context, List<Resolver> resolvers, GlobalEnvironment environment);
}
