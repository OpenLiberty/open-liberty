package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public interface OperationNameGenerator {

    String generateQueryName(OperationNameGeneratorParams<?> params);

    String generateMutationName(OperationNameGeneratorParams<Method> params);

    String generateSubscriptionName(OperationNameGeneratorParams<Method> params);
}