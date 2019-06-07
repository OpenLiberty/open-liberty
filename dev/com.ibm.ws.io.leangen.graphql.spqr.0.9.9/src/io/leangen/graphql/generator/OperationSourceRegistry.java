package io.leangen.graphql.generator;

import io.leangen.geantyref.AnnotatedTypeMap;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Created by bojan.tomic on 7/12/16.
 */
public class OperationSourceRegistry {

    private final Collection<ResolverBuilder> topLevelResolverBuilders = new LinkedHashSet<>();
    private final Collection<ResolverBuilder> nestedResolverBuilders = new LinkedHashSet<>();
    private final Collection<OperationSource> operationSources = new HashSet<>();
    private final Map<AnnotatedType, OperationSource> nestedOperationSources = new AnnotatedTypeMap<>();

    public void registerOperationSource(Object querySourceBean, AnnotatedType beanType) {
        this.operationSources.add(new OperationSource(querySourceBean, beanType, topLevelResolverBuilders));
    }

    public void registerOperationSource(Object querySourceBean, AnnotatedType beanType, Collection<ResolverBuilder> extractors) {
        this.operationSources.add(new OperationSource(querySourceBean, beanType, extractors.isEmpty() ? topLevelResolverBuilders : extractors));
    }

    public void registerOperationSource(AnnotatedType serviceType) {
        this.operationSources.add(new OperationSource(serviceType, topLevelResolverBuilders));
    }
    
    public void registerOperationSource(AnnotatedType serviceType, Collection<ResolverBuilder> strategies) {
        this.operationSources.add(new OperationSource(serviceType, strategies));
    }
    
    public void registerNestedOperationSource(AnnotatedType domainType) {
        this.nestedOperationSources.put(domainType, new OperationSource(domainType, nestedResolverBuilders));
    }

    public void registerNestedOperationSource(AnnotatedType domainType, Collection<ResolverBuilder> extractors) {
        this.nestedOperationSources.put(domainType, new OperationSource(domainType, extractors.isEmpty() ? nestedResolverBuilders : extractors));
    }

    public void registerGlobalResolverBuilders(Collection<ResolverBuilder> resolverDetectionStrategies) {
        this.topLevelResolverBuilders.addAll(resolverDetectionStrategies);
    }
    
    public void registerGlobalNestedResolverBuilders(Collection<ResolverBuilder> resolverDetectionStrategies) {
        this.nestedResolverBuilders.addAll(resolverDetectionStrategies);
    }

    public OperationSource nestedSourceForType(AnnotatedType domainType) {
        return nestedOperationSources.computeIfAbsent(domainType, javaType -> new OperationSource(javaType, nestedResolverBuilders));
    }

    public Collection<OperationSource> getOperationSources() {
        return operationSources;
    }

    public Collection<OperationSource> getNestedOperationSources() {
        return nestedOperationSources.values();
    }

    public boolean isEmpty() {
        return operationSources.isEmpty();
    }
}
