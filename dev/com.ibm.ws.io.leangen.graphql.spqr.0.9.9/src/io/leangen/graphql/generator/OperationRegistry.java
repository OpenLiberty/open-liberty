package io.leangen.graphql.generator;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.query.OperationBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilderParams;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OperationRegistry {

    private final Set<Operation> queries;
    private final Set<Operation> mutations;
    private final Set<Operation> subscriptions;
    private final OperationSourceRegistry operationSourceRegistry;
    private final OperationBuilder operationBuilder;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final String[] basePackages;
    private final GlobalEnvironment environment;

    public OperationRegistry(OperationSourceRegistry operationSourceRegistry, OperationBuilder operationBuilder,
                             InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, String[] basePackages,
                             GlobalEnvironment environment) {

        this.operationSourceRegistry = operationSourceRegistry;
        this.operationBuilder = operationBuilder;
        this.inclusionStrategy = inclusionStrategy;
        this.typeTransformer = typeTransformer;
        this.basePackages = basePackages;
        this.environment = environment;
        List<Resolver> resolvers = buildQueryResolvers(operationSourceRegistry.getOperationSources());
        List<Resolver> mutationResolvers = buildMutationResolvers(operationSourceRegistry.getOperationSources());
        List<Resolver> subscriptionResolvers = buildSubscriptionResolvers(operationSourceRegistry.getOperationSources());
        queries = buildQueries(resolvers);
        mutations = buildMutations(mutationResolvers);
        subscriptions = buildSubscriptions(subscriptionResolvers);
    }

    private Set<Operation> buildQueries(List<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).values().stream()
                .flatMap(r -> collectContextTypes(r).stream()
                        .map(contextType -> resolversPerContext(contextType, r))
                        .filter(contextual -> !contextual.getValue().isEmpty())
                        .map(contextual -> operationBuilder.buildQuery(contextual.getKey(), contextual.getValue(), environment)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Operation> buildMutations(List<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).values().stream()
                .flatMap(r -> collectContextTypes(r).stream()
                        .map(contextType -> resolversPerContext(contextType, r))
                        .filter(contextual -> !contextual.getValue().isEmpty())
                        .map(contextual -> operationBuilder.buildMutation(contextual.getKey(), contextual.getValue(), environment)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Operation> buildSubscriptions(List<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).values().stream()
                .flatMap(r -> collectContextTypes(r).stream()
                        .map(contextType -> resolversPerContext(contextType, r))
                        .filter(contextual -> !contextual.getValue().isEmpty())
                        .map(contextual -> operationBuilder.buildSubscription(contextual.getKey(), contextual.getValue(), environment)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map.Entry<Type, List<Resolver>> resolversPerContext(Type context, List<Resolver> resolvers) {
        List<Resolver> contextual;
        if (context == null) {
            contextual = resolvers.stream().filter(r -> r.getSourceTypes().isEmpty()).collect(Collectors.toList());
        } else {
            contextual = resolvers.stream().filter(r -> r.getSourceTypes().contains(context)).collect(Collectors.toList());
        }
        return new AbstractMap.SimpleEntry<>(context, contextual);
    }

    private List<Type> collectContextTypes(Collection<Resolver> resolvers) {
        List<Type> contextTypes = resolvers.stream()
                .flatMap(r -> r.getSourceTypes().stream())
                .distinct()
                .collect(Collectors.toList());
        contextTypes.add(null); //root queries have null context
        return contextTypes;
    }

    private Collection<Operation> getAllQueries() {
        return queries;
    }

    Collection<Operation> getRootQueries() {
        return queries.stream().filter(Operation::isRoot).collect(Collectors.toList());
    }

    Collection<Operation> getMutations() {
        return mutations;
    }

    Collection<Operation> getSubscriptions() {
        return subscriptions;
    }

    private Set<Operation> getNestedQueries(AnnotatedType domainType) {
        OperationSource domainSource = operationSourceRegistry.nestedSourceForType(domainType);
        return buildNestedQueries(domainSource);
    }

    public Collection<Operation> getChildQueries(AnnotatedType domainType) {
        Map<String, Operation> children = new HashMap<>();

        Map<String, Operation> nestedQueries = getNestedQueries(domainType).stream().collect(Collectors.toMap(Operation::getName, Function.identity()));
        /*TODO check if any nested query has a @GraphQLContext field of type different then domainType.
        If so, throw an error early, as such an operation will be impossible to invoke, unless they're static!
        Not sure about @RootContext*/
        Map<String, Operation> embeddableQueries = getEmbeddableQueries(domainType.getType()).stream().collect(Collectors.toMap(Operation::getName, Function.identity()));
        children.putAll(nestedQueries);
        children.putAll(embeddableQueries);
        return children.values();
    }

    private Set<Operation> getEmbeddableQueries(Type domainType) {
        return getAllQueries().stream()
                .map(Operation::unbatch)
                .filter(query -> query.isEmbeddableForType(domainType))
                .collect(Collectors.toSet());
    }

    private Set<Operation> buildNestedQueries(OperationSource operationSource) {
        return buildQueries(buildQueryResolvers(Collections.singleton(operationSource)));
    }

    private List<Resolver> buildQueryResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildQueryResolvers(new ResolverBuilderParams(
                        operationSource.getServiceSingleton(), operationSource.getJavaType(), inclusionStrategy, typeTransformer, basePackages, environment))));
    }

    private List<Resolver> buildMutationResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildMutationResolvers(new ResolverBuilderParams(
                        operationSource.getServiceSingleton(), operationSource.getJavaType(), inclusionStrategy, typeTransformer, basePackages, environment))));
    }

    private List<Resolver> buildSubscriptionResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildSubscriptionResolvers(new ResolverBuilderParams(
                        operationSource.getServiceSingleton(), operationSource.getJavaType(), inclusionStrategy, typeTransformer, basePackages, environment))));
    }

    private List<Resolver> buildResolvers(Collection<OperationSource> operationSources, BiFunction<OperationSource, ResolverBuilder, Collection<Resolver>> building) {
        return operationSources.stream()
                .flatMap(operationSource ->
                        operationSource.getResolverBuilders().stream()
                                .flatMap(builder -> building.apply(operationSource, builder).stream())
                                .distinct())
                .collect(Collectors.toList());
    }
}
