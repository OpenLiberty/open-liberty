package io.leangen.graphql.execution;

import io.leangen.graphql.metadata.Resolver;

public class ResolverInterceptorFactoryParams {

    private final Resolver resolver;

    ResolverInterceptorFactoryParams(Resolver resolver) {
        this.resolver = resolver;
    }

    public Resolver getResolver() {
        return resolver;
    }
}
