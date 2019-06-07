package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.Utils;

import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public class DefaultOperationNameGenerator implements OperationNameGenerator {

    private OperationNameGenerator delegate = new PropertyOperationNameGenerator();

    @Override
    public String generateQueryName(OperationNameGeneratorParams<?> params) {
        return Utils.coalesce(delegate.generateQueryName(params), params.getElement().getName());
    }

    @Override
    public String generateMutationName(OperationNameGeneratorParams<Method> params) {
        return Utils.coalesce(delegate.generateMutationName(params), params.getElement().getName());
    }

    @Override
    public String generateSubscriptionName(OperationNameGeneratorParams<Method> params) {
        return Utils.coalesce(delegate.generateSubscriptionName(params), params.getElement().getName());
    }

    public DefaultOperationNameGenerator withDelegate(OperationNameGenerator delegate) {
        this.delegate = delegate;
        return this;
    }
}
