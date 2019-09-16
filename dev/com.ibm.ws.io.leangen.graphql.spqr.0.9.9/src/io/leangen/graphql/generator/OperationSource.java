package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;

import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;

/**
 * Created by bojan.tomic on 7/10/16.
 */
public class OperationSource {

    private final Object serviceSingleton;
    private final AnnotatedType javaType;
    private final Collection<ResolverBuilder> resolverBuilders;

    OperationSource(Object serviceSingleton, AnnotatedType javaType, Collection<ResolverBuilder> resolverBuilders) {
        this.serviceSingleton = serviceSingleton;
        this.javaType = javaType;
        this.resolverBuilders = resolverBuilders;
    }

    OperationSource(AnnotatedType javaType, Collection<ResolverBuilder> resolverBuilders) {
        this(null, javaType, resolverBuilders);
    }

    Object getServiceSingleton() {
        return serviceSingleton;
    }

    AnnotatedType getJavaType() {
        return javaType;
    }

    Collection<ResolverBuilder> getResolverBuilders() {
        return resolverBuilders;
    }
}
