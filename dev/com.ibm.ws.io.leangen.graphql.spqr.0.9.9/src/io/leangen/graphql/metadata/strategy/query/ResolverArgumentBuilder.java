package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.OperationArgument;

import java.util.List;

/**
 * Created by bojan.tomic on 7/17/16.
 */
public interface ResolverArgumentBuilder {

    List<OperationArgument> buildResolverArguments(ArgumentBuilderParams params);
}
