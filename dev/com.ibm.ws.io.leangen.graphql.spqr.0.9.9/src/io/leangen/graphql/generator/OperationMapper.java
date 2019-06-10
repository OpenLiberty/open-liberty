package io.leangen.graphql.generator;

import graphql.execution.batched.BatchedDataFetcher;
import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import graphql.schema.PropertyDataFetcher;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.execution.ContextWrapper;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.OperationExecutor;
import io.leangen.graphql.execution.ResolverInterceptorFactory;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.Directive;
import io.leangen.graphql.metadata.DirectiveArgument;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.exceptions.MappingException;
import io.leangen.graphql.metadata.strategy.query.DirectiveBuilderParams;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.Directives;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Urls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static io.leangen.graphql.util.GraphQLUtils.CLIENT_MUTATION_ID;

/**
 * <p>Drives the work of mapping Java structures into their GraphQL representations.</p>
 * While the task of mapping types is delegated to instances of {@link io.leangen.graphql.generator.mapping.TypeMapper},
 * selection of mappers, construction and attachment of resolvers (modeled by {@link DataFetcher}s
 * in <a href=https://github.com/graphql-java/graphql-java>graphql-java</a>), and other universal tasks are encapsulated
 * in this class.
 */
public class OperationMapper {

    private List<GraphQLFieldDefinition> queries; //The list of all mapped queries
    private List<GraphQLFieldDefinition> mutations; //The list of all mapped mutations
    private List<GraphQLFieldDefinition> subscriptions; //The list of all mapped subscriptions
    private List<GraphQLDirective> directives; //The list of all added mapped directives

    private static final Logger log = LoggerFactory.getLogger(OperationMapper.class);

    /**
     *
     * @param buildContext The shared context containing all the global information needed for mapping
     */
    public OperationMapper(BuildContext buildContext) {
        this.queries = generateQueries(buildContext);
        this.mutations = generateMutations(buildContext);
        this.subscriptions = generateSubscriptions(buildContext);
        this.directives = generateDirectives(buildContext);
    }

    /**
     * Generates {@link GraphQLFieldDefinition}s representing all top-level queries (with their types, arguments and sub-queries)
     * fully mapped. This is the entry point into the query-mapping logic.
     *
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return A list of {@link GraphQLFieldDefinition}s representing all top-level queries
     */
    private List<GraphQLFieldDefinition> generateQueries(BuildContext buildContext) {
        List<Operation> rootQueries = new ArrayList<>(buildContext.operationRegistry.getRootQueries());
        List<GraphQLFieldDefinition> queries = rootQueries.stream()
                .map(query -> toGraphQLField(query, buildContext))
                .collect(Collectors.toList());

        buildContext.resolveTypeReferences();
        //Add support for the Relay Node query only if an explicit one isn't already provided and Relay-enabled resolvers exist
        if (rootQueries.stream().noneMatch(query -> query.getName().equals(GraphQLUtils.NODE))) {
            Map<String, String> nodeQueriesByType = getNodeQueriesByType(rootQueries, queries, buildContext.typeRegistry, buildContext.node, buildContext);
            if (!nodeQueriesByType.isEmpty()) {
                queries.add(buildContext.relay.nodeField(buildContext.node, createNodeResolver(nodeQueriesByType, buildContext.relay)));
            }
        }
        return queries;
    }

    /**
     * Generates {@link GraphQLFieldDefinition}s representing all mutations (with their types, arguments and sub-queries)
     * fully mapped. By the GraphQL spec, all mutations are top-level only.
     * This is the entry point into the mutation-mapping logic.
     *
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return A list of {@link GraphQLFieldDefinition}s representing all mutations
     */
    private List<GraphQLFieldDefinition> generateMutations(BuildContext buildContext) {
        Collection<Operation> mutations = buildContext.operationRegistry.getMutations();
        return mutations.stream()
                .map(mutation -> buildContext.relayMappingConfig.relayCompliantMutations
                        ? toRelayMutation(toGraphQLField(mutation, buildContext), buildContext.relayMappingConfig)
                        : toGraphQLField(mutation, buildContext))
                .collect(Collectors.toList());
    }

    private List<GraphQLFieldDefinition> generateSubscriptions(BuildContext buildContext) {
        return buildContext.operationRegistry.getSubscriptions().stream()
                .map(subscription -> toGraphQLField(subscription, buildContext))
                .collect(Collectors.toList());
    }

    private List<GraphQLDirective> generateDirectives(BuildContext buildContext) {
        return buildContext.additionalDirectives.stream()
                .map(directiveType -> buildContext.directiveBuilder.buildClientDirective(directiveType, buildContext.directiveBuilderParams()))
                .map(directive -> toGraphQLDirective(directive, buildContext))
                .collect(Collectors.toList());
    }

    /**
     * Maps a single operation to a GraphQL output field (as queries in GraphQL are nothing but fields of the root operation type).
     *
     * @param operation The operation to map to a GraphQL output field
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL output field representing the given operation
     */
    public GraphQLFieldDefinition toGraphQLField(Operation operation, BuildContext buildContext) {
        GraphQLOutputType type = toGraphQLType(operation.getJavaType(), buildContext);
        GraphQLFieldDefinition.Builder fieldBuilder = newFieldDefinition()
                .name(operation.getName())
                .description(operation.getDescription())
                .deprecate(operation.getDeprecationReason())
                .type(type)
                .withDirective(Directives.mappedOperation(operation))
                .withDirectives(toGraphQLDirectives(operation.getTypedElement(), buildContext.directiveBuilder::buildFieldDefinitionDirectives, buildContext));

        List<GraphQLArgument> arguments = operation.getArguments().stream()
                .filter(OperationArgument::isMappable)
                .map(argument -> toGraphQLArgument(argument, buildContext))
                .collect(Collectors.toList());
        fieldBuilder.argument(arguments);
        if (GraphQLUtils.isRelayConnectionType(type) && buildContext.relayMappingConfig.strictConnectionSpec) {
            validateConnectionSpecCompliance(operation.getName(), arguments, buildContext.relay);
        }

        Stream<AnnotatedType> inputTypes = operation.getArguments().stream()
                .filter(OperationArgument::isMappable)
                .map(OperationArgument::getJavaType);
        ValueMapper valueMapper = buildContext.createValueMapper(inputTypes);
        fieldBuilder.dataFetcher(createResolver(operation, valueMapper, buildContext.globalEnvironment, buildContext.interceptorFactory));

        return buildContext.transformers.transform(fieldBuilder.build(), operation, this, buildContext);
    }

    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext) {
        return toGraphQLType(javaType, new HashSet<>(), buildContext);
    }

    /**
     * Maps a Java type to a GraphQL output type. Delegates most of the work to applicable
     * {@link io.leangen.graphql.generator.mapping.TypeMapper}s.
     * <p>See {@link TypeMapper#toGraphQLType(AnnotatedType, OperationMapper, java.util.Set, BuildContext)}</p>
     *
     * @param javaType The Java type that is to be mapped to a GraphQL output type
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL output type corresponding to the given Java type
     */
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        GraphQLOutputType type = buildContext.typeMappers.getTypeMapper(javaType, mappersToSkip).toGraphQLType(javaType, this, mappersToSkip, buildContext);
        log(buildContext.validator.checkUniqueness(type, javaType));
        buildContext.typeCache.completeType(type);
        return type;
    }

    /**
     * Maps a single field/property to a GraphQL input field.
     *
     * @param inputField The field/property to map to a GraphQL input field
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL input field representing the given field/property
     */
    public GraphQLInputObjectField toGraphQLInputField(InputField inputField, BuildContext buildContext) {
        GraphQLInputObjectField.Builder builder = newInputObjectField()
                .name(inputField.getName())
                .description(inputField.getDescription())
                .type(toGraphQLInputType(inputField.getJavaType(), buildContext))
                .withDirective(Directives.mappedInputField(inputField))
                .withDirectives(toGraphQLDirectives(inputField.getTypedElement(), buildContext.directiveBuilder::buildInputFieldDefinitionDirectives, buildContext))
                .defaultValue(inputField.getDefaultValue());
        return buildContext.transformers.transform(builder.build(), inputField, this, buildContext);
    }

    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext) {
        return toGraphQLInputType(javaType, new HashSet<>(), buildContext);
    }

    /**
     * Maps a Java type to a GraphQL input type. Delegates most of the work to applicable
     * {@link io.leangen.graphql.generator.mapping.TypeMapper}s.
     * <p>See {@link TypeMapper#toGraphQLInputType(AnnotatedType, OperationMapper, java.util.Set, BuildContext)}</p>
     *
     * @param javaType The Java type that is to be mapped to a GraphQL input type
     * @param buildContext The shared context containing all the global information needed for mapping
     *
     * @return GraphQL input type corresponding to the given Java type
     */
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        GraphQLInputType type = buildContext.typeMappers.getTypeMapper(javaType, mappersToSkip).toGraphQLInputType(javaType, this, mappersToSkip, buildContext);
        log(buildContext.validator.checkUniqueness(type, javaType));
        return type;
    }

    private GraphQLArgument toGraphQLArgument(OperationArgument operationArgument, BuildContext buildContext) {
        GraphQLArgument argument = newArgument()
                .name(operationArgument.getName())
                .description(operationArgument.getDescription())
                .type(toGraphQLInputType(operationArgument.getJavaType(), buildContext))
                .defaultValue(operationArgument.getDefaultValue())
                .withDirectives(toGraphQLDirectives(operationArgument.getTypedElement(), buildContext.directiveBuilder::buildArgumentDefinitionDirectives, buildContext))
                .build();
        return buildContext.transformers.transform(argument, operationArgument, this, buildContext);
    }

    private GraphQLDirective[] toGraphQLDirectives(TypedElement element, BiFunction<AnnotatedElement, DirectiveBuilderParams, List<Directive>> directiveBuilder, BuildContext buildContext) {
        return element.getElements().stream()
                .flatMap(el -> directiveBuilder.apply(el, buildContext.directiveBuilderParams()).stream())
                .map(directive -> toGraphQLDirective(directive, buildContext))
                .toArray(GraphQLDirective[]::new);
    }

    public GraphQLDirective toGraphQLDirective(Directive directive, BuildContext buildContext) {
        GraphQLDirective.Builder builder = GraphQLDirective.newDirective()
                .name(directive.getName())
                .description(directive.getDescription())
                .validLocations(directive.getLocations());
        directive.getArguments().forEach(arg -> builder.argument(toGraphQLArgument(arg, buildContext)));
        return buildContext.transformers.transform(builder.build(), directive, this, buildContext);
    }

    private GraphQLArgument toGraphQLArgument(DirectiveArgument directiveArgument, BuildContext buildContext) {
        GraphQLArgument argument = newArgument()
                .name(directiveArgument.getName())
                .description(directiveArgument.getDescription())
                .type(toGraphQLInputType(directiveArgument.getJavaType(), buildContext))
                .value(directiveArgument.getValue())
                .defaultValue(directiveArgument.getDefaultValue())
                .withDirectives(toGraphQLDirectives(directiveArgument.getTypedElement(), buildContext.directiveBuilder::buildArgumentDefinitionDirectives, buildContext))
                .build();
        return buildContext.transformers.transform(argument, directiveArgument, this, buildContext);
    }

    private GraphQLFieldDefinition toRelayMutation(GraphQLFieldDefinition mutation, RelayMappingConfig relayMappingConfig) {

        List<GraphQLFieldDefinition> outputFields;
        if (mutation.getType() instanceof GraphQLObjectType) {
            outputFields = ((GraphQLObjectType) mutation.getType()).getFieldDefinitions();
        } else {
            outputFields = new ArrayList<>();
            outputFields.add(GraphQLFieldDefinition.newFieldDefinition()
                    .name(relayMappingConfig.wrapperFieldName)
                    .description(relayMappingConfig.wrapperFieldDescription)
                    .type(mutation.getType())
                    .dataFetcher(DataFetchingEnvironment::getSource)
                    .build());
        }
        List<GraphQLInputObjectField> inputFields = mutation.getArguments().stream()
                .map(arg -> GraphQLInputObjectField.newInputObjectField()
                        .name(arg.getName())
                        .description(arg.getDescription())
                        .type(arg.getType())
                        .defaultValue(arg.getDefaultValue())
                        .build())
                .collect(Collectors.toList());
        GraphQLInputObjectType inputObjectType = newInputObject()
                .name(mutation.getName() + "Input")
                .field(newInputObjectField()
                        .name(CLIENT_MUTATION_ID)
                        .type(new GraphQLNonNull(GraphQLString)))
                .fields(inputFields)
                .build();
        GraphQLObjectType outputType = newObject()
                .name(mutation.getName() + "Payload")
                .field(newFieldDefinition()
                        .name(CLIENT_MUTATION_ID)
                        .type(new GraphQLNonNull(GraphQLString))
                        .dataFetcher(env -> env.getContext() instanceof ContextWrapper
                                ? ((ContextWrapper) env.getContext()).getClientMutationId()
                                : new PropertyDataFetcher(CLIENT_MUTATION_ID)))
                .fields(outputFields)
                .build();

        return newFieldDefinition()
                .name(mutation.getName())
                .type(outputType)
                .argument(newArgument()
                        .name("input")
                        .type(new GraphQLNonNull(inputObjectType)))
                .dataFetcher(env -> {
                    DataFetchingEnvironment innerEnv = new RelayDataFetchingEnvironmentDecorator(env);
                    if (env.getContext() instanceof ContextWrapper) {
                        ContextWrapper context = env.getContext();
                        context.setClientMutationId(innerEnv.getArgument(CLIENT_MUTATION_ID));
                    }
                    return mutation.getDataFetcher().get(innerEnv);
                })
                .build();
    }

    /**
     * Creates a generic resolver for the given operation.
     * @implSpec This resolver simply invokes {@link OperationExecutor#execute(DataFetchingEnvironment)}
     *
     * @param operation The operation for which the resolver is being created
     * @param valueMapper Mapper to be used to deserialize raw argument values
     * @param globalEnvironment The shared context containing all the global information needed for operation resolution
     * @param interceptorFactory A factory producing interceptors applicable to the given {@code operation}'s resolvers
     *
     * @return The resolver for the given operation
     */
    private DataFetcher createResolver(Operation operation, ValueMapper valueMapper, GlobalEnvironment globalEnvironment, ResolverInterceptorFactory interceptorFactory) {
        if (operation.isBatched()) {
            return (BatchedDataFetcher) environment -> new OperationExecutor(operation, valueMapper, globalEnvironment, interceptorFactory).execute(environment);
        }
        return new OperationExecutor(operation, valueMapper, globalEnvironment, interceptorFactory)::execute;
    }

    /**
     * Creates a resolver for the <em>node</em> query as defined by the Relay GraphQL spec.
     * <p>This query only takes a singe argument called "id" of type String, and returns the object implementing the
     * <em>Node</em> interface to which the given id corresponds.</p>
     *
     * @param nodeQueriesByType A map of all queries whose return types implement the <em>Node</em> interface, keyed
     *                          by their corresponding GraphQL type name
     * @param relay Relay helper
     *
     * @return The node query resolver
     */
    private DataFetcher createNodeResolver(Map<String, String> nodeQueriesByType, Relay relay) {
        return env -> {
            String typeName;
            try {
                typeName = relay.fromGlobalId(env.getArgument(GraphQLId.RELAY_ID_FIELD_NAME)).getType();
            } catch (Exception e) {
                throw new IllegalArgumentException(env.getArgument(GraphQLId.RELAY_ID_FIELD_NAME) + " is not a valid Relay node ID");
            }
            if (!nodeQueriesByType.containsKey(typeName)) {
                throw new IllegalArgumentException(typeName + " is not a Relay node type or no registered query can fetch it by ID");
            }
            return env.getGraphQLSchema().getQueryType().getFieldDefinition(nodeQueriesByType.get(typeName)).getDataFetcher().get(env);
        };
    }

    private Map<String, String> getNodeQueriesByType(List<Operation> queries,
                                                     List<GraphQLFieldDefinition> graphQlQueries,
                                                     TypeRegistry typeRegistry, GraphQLInterfaceType node, BuildContext buildContext) {

        Map<String, String> nodeQueriesByType = new HashMap<>();

        for (int i = 0; i < queries.size(); i++) {
            Operation query = queries.get(i);
            GraphQLFieldDefinition graphQlQuery = graphQlQueries.get(i);

            if (graphQlQuery.getArgument(GraphQLId.RELAY_ID_FIELD_NAME) != null
                    && GraphQLUtils.isRelayId(graphQlQuery.getArgument(GraphQLId.RELAY_ID_FIELD_NAME))
                    && query.getResolver(GraphQLId.RELAY_ID_FIELD_NAME) != null) {

                GraphQLType unwrappedQueryType = GraphQLUtils.unwrapNonNull(graphQlQuery.getType());
                unwrappedQueryType = buildContext.typeCache.resolveType(unwrappedQueryType.getName());
                if (unwrappedQueryType instanceof GraphQLObjectType
                        && ((GraphQLObjectType) unwrappedQueryType).getInterfaces().contains(node)) {
                    nodeQueriesByType.put(unwrappedQueryType.getName(), query.getName());
                } else if (unwrappedQueryType instanceof GraphQLInterfaceType) {
                    typeRegistry.getOutputTypes(unwrappedQueryType.getName()).stream()
                            .map(MappedType::getAsObjectType)
                            .filter(implementation -> implementation.getInterfaces().contains(node))
                            .forEach(nodeType -> nodeQueriesByType.putIfAbsent(nodeType.getName(), query.getName()));  //never override more precise resolvers
                } else if (unwrappedQueryType instanceof GraphQLUnionType) {
                    typeRegistry.getOutputTypes(unwrappedQueryType.getName()).stream()
                            .map(MappedType::getAsObjectType)
                            .filter(implementation -> implementation.getInterfaces().contains(node))
                            .filter(Directives::isMappedType)
                            // only register the possible types that can actually be returned from the primary resolver
                            // for interface-unions it is all the possible types but, for inline unions, only one (right?) possible type can match
                            .filter(implementation -> GenericTypeReflector.isSuperType(query.getResolver(GraphQLId.RELAY_ID_FIELD_NAME).getReturnType().getType(), Directives.getMappedType(implementation).getType()))
                            .forEach(nodeType -> nodeQueriesByType.putIfAbsent(nodeType.getName(), query.getName())); //never override more precise resolvers
                }
            }
        }
        return nodeQueriesByType;
    }

    private void log(Validator.ValidationResult result) {
        if (!result.isValid()) {
            log.warn(result.getMessage());
        }
    }

    private void validateConnectionSpecCompliance(String operationName, List<GraphQLArgument> arguments, Relay relay) {
        String errorMessageTemplate = "Operation '" + operationName + "' is incompatible with the Relay Connection spec due to %s. " +
                "If this is intentional, disable strict compliance checking. " +
                "For details and solutions see " + Urls.Errors.RELAY_CONNECTION_SPEC_VIOLATION;

        boolean forwardPageSupported = relay.getForwardPaginationConnectionFieldArguments().stream().allMatch(
                specArg -> arguments.stream().anyMatch(arg -> arg.getName().equals(specArg.getName())));
        boolean backwardPageSupported = relay.getBackwardPaginationConnectionFieldArguments().stream().allMatch(
                specArg -> arguments.stream().anyMatch(arg -> arg.getName().equals(specArg.getName())));

        if (!forwardPageSupported && !backwardPageSupported) {
            throw new MappingException(String.format(errorMessageTemplate, "required arguments missing"));
        }

        if (relay.getConnectionFieldArguments().stream().anyMatch(specArg -> arguments.stream().anyMatch(
                arg -> arg.getName().equals(specArg.getName())
                        && !GraphQLUtils.unwrap(arg.getType()).getName().equals(specArg.getType().getName())))) {
            throw new MappingException(String.format(errorMessageTemplate, "argument type mismatch"));
        }
    }

    /**
     * Fetches all the mapped GraphQL fields representing top-level queries, ready to be attached to the root query type.
     *
     * @return A list of GraphQL fields representing top-level queries
     */
    public List<GraphQLFieldDefinition> getQueries() {
        return queries;
    }

    /**
     * Fetches all the mapped GraphQL fields representing mutations, ready to be attached to the root mutation type.
     *
     * @return A list of GraphQL fields representing mutations
     */
    public List<GraphQLFieldDefinition> getMutations() {
        return mutations;
    }

    public List<GraphQLFieldDefinition> getSubscriptions() {
        return subscriptions;
    }

    public List<GraphQLDirective> getDirectives() {
        return directives;
    }
}
