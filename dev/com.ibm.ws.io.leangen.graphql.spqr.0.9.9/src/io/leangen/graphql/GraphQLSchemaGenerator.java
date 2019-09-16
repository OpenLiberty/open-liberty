package io.leangen.graphql;

import graphql.relay.Relay;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.AnnotatedTypeSet;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolverInterceptor;
import io.leangen.graphql.execution.ResolverInterceptorFactory;
import io.leangen.graphql.execution.ResolverInterceptorFactoryParams;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.InputFieldBuilderRegistry;
import io.leangen.graphql.generator.JavaDeprecationMappingConfig;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.OperationRegistry;
import io.leangen.graphql.generator.OperationSource;
import io.leangen.graphql.generator.OperationSourceRegistry;
import io.leangen.graphql.generator.RelayMappingConfig;
import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRegistry;
import io.leangen.graphql.generator.mapping.BaseTypeSynonymComparator;
import io.leangen.graphql.generator.mapping.ConverterRegistry;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.SchemaTransformer;
import io.leangen.graphql.generator.mapping.SchemaTransformerRegistry;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMapperRegistry;
import io.leangen.graphql.generator.mapping.common.AnnotationMapper;
import io.leangen.graphql.generator.mapping.common.ArrayAdapter;
import io.leangen.graphql.generator.mapping.common.CollectionOutputConverter;
import io.leangen.graphql.generator.mapping.common.ContextInjector;
import io.leangen.graphql.generator.mapping.common.DirectiveValueDeserializer;
import io.leangen.graphql.generator.mapping.common.EnumMapToObjectTypeAdapter;
import io.leangen.graphql.generator.mapping.common.EnumMapper;
import io.leangen.graphql.generator.mapping.common.EnvironmentInjector;
import io.leangen.graphql.generator.mapping.common.IdAdapter;
import io.leangen.graphql.generator.mapping.common.IdMapper;
import io.leangen.graphql.generator.mapping.common.InputValueDeserializer;
import io.leangen.graphql.generator.mapping.common.InterfaceMapper;
import io.leangen.graphql.generator.mapping.common.IterableAdapter;
import io.leangen.graphql.generator.mapping.common.ListMapper;
import io.leangen.graphql.generator.mapping.common.NonNullMapper;
import io.leangen.graphql.generator.mapping.common.ObjectScalarMapper;
import io.leangen.graphql.generator.mapping.common.ObjectTypeMapper;
import io.leangen.graphql.generator.mapping.common.OptionalAdapter;
import io.leangen.graphql.generator.mapping.common.OptionalDoubleAdapter;
import io.leangen.graphql.generator.mapping.common.OptionalIntAdapter;
import io.leangen.graphql.generator.mapping.common.OptionalLongAdapter;
import io.leangen.graphql.generator.mapping.common.PageMapper;
import io.leangen.graphql.generator.mapping.common.RootContextInjector;
import io.leangen.graphql.generator.mapping.common.ScalarMapper;
import io.leangen.graphql.generator.mapping.common.StreamToCollectionTypeAdapter;
import io.leangen.graphql.generator.mapping.common.UnionInlineMapper;
import io.leangen.graphql.generator.mapping.common.UnionTypeMapper;
import io.leangen.graphql.generator.mapping.common.VoidToBooleanTypeAdapter;
import io.leangen.graphql.generator.mapping.core.CompletableFutureMapper;
import io.leangen.graphql.generator.mapping.core.DataFetcherResultMapper;
import io.leangen.graphql.generator.mapping.core.PublisherAdapter;
import io.leangen.graphql.generator.mapping.strategy.AbstractInputHandler;
import io.leangen.graphql.generator.mapping.strategy.AnnotatedInterfaceStrategy;
import io.leangen.graphql.generator.mapping.strategy.AutoScanAbstractInputHandler;
import io.leangen.graphql.generator.mapping.strategy.DefaultImplementationDiscoveryStrategy;
import io.leangen.graphql.generator.mapping.strategy.ImplementationDiscoveryStrategy;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.generator.mapping.strategy.NoOpAbstractInputHandler;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.DelegatingMessageBundle;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.query.AnnotatedDirectiveBuilder;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.DefaultOperationBuilder;
import io.leangen.graphql.metadata.strategy.query.DirectiveBuilder;
import io.leangen.graphql.metadata.strategy.query.OperationBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.AnnotationInputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.ScalarDeserializationStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.module.Module;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Defaults;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Urls;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.schema.GraphQLObjectType.newObject;
import static java.util.Collections.addAll;

/**
 * <p>This class is the main entry point to the library. It is used to generate a GraphQL schema by analyzing the registered classes
 * and exposing the chosen methods as GraphQL queries or mutations. The process of choosing the methods to expose is delegated
 * to {@link ResolverBuilder} instances, and a different set of builders can be attached to each registered class.
 * One such coupling of a registered class and a set of builders is modeled by an instance of {@link OperationSource}.
 * Methods of the {@code with*OperationSource} family are used to register sources to be analyzed.</p>
 * <p>Builders can also be registered globally (to be used when none are provided explicitly) via {@link #withResolverBuilders(ResolverBuilder...)}.
 * The process of mapping the Java methods to GraphQL queries/mutations will also transparently map all encountered Java types
 * to corresponding GraphQL types. The entire mapping process is handled by an instance {@link OperationMapper} where actual type
 * mapping is delegated to different instances of {@link TypeMapper}.</p>
 * <p>To customize the mapping process, clients can registers their own {@link TypeMapper}s using {@link #withTypeMappers(TypeMapper...)}.
 * Runtime conversion between values provided by the GraphQL client and those expected by Java code might be needed.
 * This is handled by {@link InputConverter} instances.</p>
 * <p>Similarly, the conversion between values returned by Java code and those expected by the GraphQL client (if needed) is
 * handled by {@link OutputConverter} instances.
 * Custom implementations of both {@link InputConverter} and {@link OutputConverter} can be provided using
 * {@link #withInputConverters(InputConverter[])} and {@link #withOutputConverters(OutputConverter[])} respectively.</p>
 *
 * <p><b>Example:</b></p>
 *
 * <pre>
 * {@code
 * UserService userService = new UserService(); //could also be injected by a framework
 * GraphQLSchema schema = new GraphQLSchemaGenerator()
 *      .withOperationsFromSingletons(userService) //register an operations source and use the default strategy
 *      .withNestedResolverBuildersForType(User.class, new BeanResolverBuilder()) //customize how queries are extracted from User.class
 *      .generate();
 * GraphQL graphQL = new GraphQL(schema);
 *
 * //keep the reference to GraphQL instance and execute queries against it.
 * //this query selects a user by ID and requests name and regDate fields only
 * ExecutionResult result = graphQL.execute(
 * "{ user (id: 123) {
 *      name,
 *      regDate
 *  }}");
 *  }
 * </pre>
 */
@SuppressWarnings("WeakerAccess")
public class GraphQLSchemaGenerator {

    private InterfaceMappingStrategy interfaceStrategy = new AnnotatedInterfaceStrategy(true);
    private ScalarDeserializationStrategy scalarStrategy;
    private AbstractInputHandler abstractInputHandler = new NoOpAbstractInputHandler();
    private OperationBuilder operationBuilder = new DefaultOperationBuilder(DefaultOperationBuilder.TypeInference.NONE);
    private DirectiveBuilder directiveBuilder = new AnnotatedDirectiveBuilder();
    private ValueMapperFactory valueMapperFactory;
    private InclusionStrategy inclusionStrategy;
    private ImplementationDiscoveryStrategy implDiscoveryStrategy = new DefaultImplementationDiscoveryStrategy();
    private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
    private TypeTransformer typeTransformer = new DefaultTypeTransformer(false, false);
    private GlobalEnvironment environment;
    private String[] basePackages = Utils.emptyArray();
    private DelegatingMessageBundle messageBundle = new DelegatingMessageBundle();
    private List<TypeMapper> typeMappers;
    private List<SchemaTransformer> transformers;
    private Comparator<AnnotatedType> typeComparator;
    private List<InputFieldBuilder> inputFieldBuilders;
    private ResolverInterceptorFactory interceptorFactory;
    private JavaDeprecationMappingConfig javaDeprecationConfig = new JavaDeprecationMappingConfig(true, "Deprecated");
    private final OperationSourceRegistry operationSourceRegistry = new OperationSourceRegistry();
    private final List<ExtensionProvider<GeneratorConfiguration, TypeMapper>> typeMapperProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, SchemaTransformer>> schemaTransformerProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, InputConverter>> inputConverterProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, OutputConverter>> outputConverterProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ArgumentInjector>> argumentInjectorProviders = new ArrayList<>();
    private final List<ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder>> inputFieldBuilderProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ResolverBuilder>> resolverBuilderProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ResolverBuilder>> nestedResolverBuilderProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, Module>> moduleProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory>> interceptorFactoryProviders = new ArrayList<>();
    private final Collection<GraphQLSchemaProcessor> processors = new HashSet<>();
    private final RelayMappingConfig relayMappingConfig = new RelayMappingConfig();
    private final Map<String, GraphQLDirective> additionalDirectives = new HashMap<>();
    private final List<AnnotatedType> additionalDirectiveTypes = new ArrayList<>();
    private final Map<String, GraphQLType> additionalTypes = new HashMap<>();
    private final Set<Comparator<AnnotatedType>> typeComparators = new HashSet<>();

    private final String queryRoot;
    private final String mutationRoot;
    private final String subscriptionRoot;
    private final String queryRootDescription;
    private final String mutationRootDescription;
    private final String subscriptionRootDescription;

    /**
     * Default constructor
     */
    public GraphQLSchemaGenerator() {
        this("Query", "Mutation", "Subscription");
    }

    /**
     * Constructor which allows to customize names of root types.
     * @param queryRoot name of query root type
     * @param mutationRoot name of mutation root type
     * @param subscriptionRoot name of subscription root type
     */
    public GraphQLSchemaGenerator(String queryRoot, String mutationRoot, String subscriptionRoot) {
        this(queryRoot, "Query root", mutationRoot, "Mutation root", subscriptionRoot, "Subscription root");
    }

    /**
     * Constructor which allows to customize names of root types.
     * @param queryRoot name of query root type
     * @param mutationRoot name of mutation root type
     * @param subscriptionRoot name of subscription root type
     */
    public GraphQLSchemaGenerator(String queryRoot, String queryRootDescription, String mutationRoot, String mutationRootDescription, String subscriptionRoot, String subscriptionRootDescription) {
        this.queryRoot = queryRoot;
        this.mutationRoot = mutationRoot;
        this.subscriptionRoot = subscriptionRoot;
        this.queryRootDescription = queryRootDescription;
        this.mutationRootDescription = mutationRootDescription;
        this.subscriptionRootDescription = subscriptionRootDescription;
    }

    /**
     * Register {@code serviceSingleton} as a singleton {@link OperationSource},
     * with its class (obtained via {@link Object#getClass()}) as its runtime type and with the globally registered
     * {@link ResolverBuilder}s.
     * All query/mutation methods discovered by analyzing the {@code serviceSingleton}'s type will be later,
     * in query resolution time, invoked on this specific instance (hence the 'singleton' in the method name).
     * Instances of stateless service classes are commonly registered this way.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton) {
        checkType(serviceSingleton.getClass());
        return withOperationsFromSingleton(serviceSingleton, serviceSingleton.getClass());
    }

    /**
     * Register {@code serviceSingleton} as a singleton {@link OperationSource},
     * with {@code beanType} as its runtime type and with the globally registered {@link ResolverBuilder}s.
     * <p>See {@link #withOperationsFromSingleton(Object)}</p>
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}). This is commonly the case when the class is generic
     *                 or when the instance has been proxied by a framework.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, Type beanType) {
        return withOperationsFromSingleton(serviceSingleton, GenericTypeReflector.annotate(beanType));
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, Type)}, except that an {@link AnnotatedType} is used as
     * {@code serviceSingleton}'s runtime type. Needed when type annotations such as {@link GraphQLNonNull}
     * not directly declared on the class should be captured.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}) and when annotations on the type should be kept.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get an {@link AnnotatedType} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, AnnotatedType beanType) {
        checkType(beanType);
        this.operationSourceRegistry.registerOperationSource(serviceSingleton, beanType);
        return this;
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object)} except that custom {@link ResolverBuilder}s will be used
     * to look through {@code beanType} for methods to be exposed.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param builders Custom strategy to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, ResolverBuilder... builders) {
        return withOperationsFromSingleton(serviceSingleton, serviceSingleton.getClass(), builders);
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, Type)} except that custom {@link ResolverBuilder}s will be used
     * to look through {@code beanType} for methods to be exposed.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}). This is commonly the case when the class is generic
     *                 or when the instance has been proxied by a framework.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param builders Custom strategy to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, Type beanType, ResolverBuilder... builders) {
        checkType(beanType);
        return withOperationsFromSingleton(serviceSingleton, GenericTypeReflector.annotate(beanType), builders);
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, AnnotatedType)} except that custom {@link ResolverBuilder}s will be used
     * to look through {@code beanType} for methods to be exposed.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}) and when annotations on the type should be kept.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get an {@link AnnotatedType} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param builders Custom builders to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, AnnotatedType beanType, ResolverBuilder... builders) {
        checkType(beanType);
        this.operationSourceRegistry.registerOperationSource(serviceSingleton, beanType, Arrays.asList(builders));
        return this;
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object)} except that multiple beans can be registered at the same time.
     *
     * @param serviceSingletons Singleton beans whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingletons(Object... serviceSingletons) {
        Arrays.stream(serviceSingletons).forEach(this::withOperationsFromSingleton);
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromType(Type serviceType) {
        return this.withOperationsFromType(GenericTypeReflector.annotate(serviceType));
    }

    public GraphQLSchemaGenerator withOperationsFromType(Type serviceType, ResolverBuilder... builders) {
        return this.withOperationsFromType(GenericTypeReflector.annotate(serviceType), builders);
    }

    public GraphQLSchemaGenerator withOperationsFromTypes(Type... serviceType) {
        Arrays.stream(serviceType).forEach(this::withOperationsFromType);
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromType(AnnotatedType serviceType) {
        checkType(serviceType);
        this.operationSourceRegistry.registerOperationSource(serviceType);
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromType(AnnotatedType serviceType, ResolverBuilder... builders) {
        checkType(serviceType);
        this.operationSourceRegistry.registerOperationSource(serviceType, Arrays.asList(builders));
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromTypes(AnnotatedType... serviceType) {
        Arrays.stream(serviceType).forEach(this::withOperationsFromType);
        return this;
    }

    /**
     * Register a type to be scanned for exposed methods, using the globally registered builders.
     * This is not normally required as domain types will be discovered dynamically and globally registered builders
     * will be used anyway. Only needed when no exposed method refers to this domain type directly
     * (relying exclusively on interfaces or super-types instead) and the type should still be mapped and listed in the resulting schema.
     *
     * @param types The domain types that are to be scanned for query/mutation methods
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedOperationsFromTypes(Type... types) {
        Arrays.stream(types).forEach(this::withNestedResolverBuildersForType);
        return this;
    }

    /**
     * The same as {@link #withNestedOperationsFromTypes(Type...)} except that an {@link AnnotatedType} is used,
     * so any extra annotations on the type (not only those directly on the class) are kept.
     *
     * @param types The domain types that are to be scanned for query/mutation methods
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedOperationsFromTypes(AnnotatedType... types) {
        Arrays.stream(types).forEach(this::withNestedResolverBuildersForType);
        return this;
    }

    /**
     * Register {@code querySourceType} type to be scanned for exposed methods, using the provided {@link ResolverBuilder}s.
     * Domain types are discovered dynamically, when referred to by an exposed method (either as its parameter type or return type).
     * This method gives a way to customize how the discovered domain type will be analyzed.
     *
     * @param querySourceType The domain type that is to be scanned for query/mutation methods
     * @param resolverBuilders Custom resolverBuilders to use when analyzing {@code querySourceType} type
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedResolverBuildersForType(Type querySourceType, ResolverBuilder... resolverBuilders) {
        return withNestedResolverBuildersForType(GenericTypeReflector.annotate(querySourceType), resolverBuilders);
    }

    /**
     * Same as {@link #withNestedResolverBuildersForType(Type, ResolverBuilder...)} except that an {@link AnnotatedType} is used
     * so any extra annotations on the type (not only those directly on the class) are kept.
     *
     * @param querySourceType The annotated domain type that is to be scanned for query/mutation methods
     * @param resolverBuilders Custom resolverBuilders to use when analyzing {@code querySourceType} type
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedResolverBuildersForType(AnnotatedType querySourceType, ResolverBuilder... resolverBuilders) {
        this.operationSourceRegistry.registerNestedOperationSource(querySourceType, Arrays.asList(resolverBuilders));
        return this;
    }

    /**
     * Globally registers {@link ResolverBuilder}s to be used for sources that don't have explicitly assigned builders.
     *
     * @param resolverBuilders builders to be globally registered
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withResolverBuilders(ResolverBuilder... resolverBuilders) {
        return withResolverBuilders((config, defaults) -> Arrays.asList(resolverBuilders));
    }

    public GraphQLSchemaGenerator withResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider) {
        this.resolverBuilderProviders.add(provider);
        return this;
    }

    /**
     * Globally registers {@link ResolverBuilder}s to be used for sources that don't have explicitly assigned builders.
     *
     * @param resolverBuilders builders to be globally registered
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedResolverBuilders(ResolverBuilder... resolverBuilders) {
        return withNestedResolverBuilders((config, defaults) -> Arrays.asList(resolverBuilders));
    }

    public GraphQLSchemaGenerator withNestedResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider) {
        this.nestedResolverBuilderProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withInputFieldBuilders(InputFieldBuilder... inputFieldBuilders) {
        return withInputFieldBuilders((env, defaults) -> defaults.prepend(inputFieldBuilders));
    }

    public GraphQLSchemaGenerator withInputFieldBuilders(ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder> provider) {
        this.inputFieldBuilderProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withAbstractInputTypeResolution() {
        this.abstractInputHandler = new AutoScanAbstractInputHandler();
        return this;
    }

    public GraphQLSchemaGenerator withAbstractInputHandler(AbstractInputHandler abstractInputHandler) {
        this.abstractInputHandler = abstractInputHandler;
        return this;
    }

    public GraphQLSchemaGenerator withBasePackages(String... basePackages) {
        this.basePackages = Utils.emptyIfNull(basePackages);
        return this;
    }

    public GraphQLSchemaGenerator withStringInterpolation(MessageBundle... messageBundles) {
        this.messageBundle.withBundles(messageBundles);
        return this;
    }

    public GraphQLSchemaGenerator withJavaDeprecationRespected(boolean respectJavaDeprecation) {
        this.javaDeprecationConfig = new JavaDeprecationMappingConfig(respectJavaDeprecation, javaDeprecationConfig.deprecationReason);
        return this;
    }

    public GraphQLSchemaGenerator withJavaDeprecationReason(String deprecationReason) {
        this.javaDeprecationConfig = new JavaDeprecationMappingConfig(javaDeprecationConfig.enabled, deprecationReason);
        return this;
    }

    public GraphQLSchemaGenerator withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator) {
        this.typeInfoGenerator = typeInfoGenerator;
        return this;
    }

    public GraphQLSchemaGenerator withValueMapperFactory(ValueMapperFactory valueMapperFactory) {
        this.valueMapperFactory = valueMapperFactory;
        return this;
    }

    public GraphQLSchemaGenerator withInterfaceMappingStrategy(InterfaceMappingStrategy interfaceStrategy) {
        this.interfaceStrategy = interfaceStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withScalarDeserializationStrategy(ScalarDeserializationStrategy scalarStrategy) {
        this.scalarStrategy = scalarStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withInclusionStrategy(InclusionStrategy inclusionStrategy) {
        this.inclusionStrategy = inclusionStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withImplementationDiscoveryStrategy(ImplementationDiscoveryStrategy implDiscoveryStrategy) {
        this.implDiscoveryStrategy = implDiscoveryStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withTypeTransformer(TypeTransformer transformer) {
        this.typeTransformer = transformer;
        return this;
    }

    /**
     * Registers custom {@link TypeMapper}s to be used for mapping Java type to GraphQL types.
     * <p><b>Ordering of mappers is strictly important as the first {@link TypeMapper} that supports the given Java type
     * will be used for mapping it.</b></p>
     * <p>See {@link TypeMapper#supports(AnnotatedType)}</p>
     *
     * @param typeMappers Custom type mappers to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withTypeMappers(TypeMapper... typeMappers) {
        return withTypeMappers((conf, current) -> current.insertAfterOrAppend(IdAdapter.class, typeMappers));
    }

    public GraphQLSchemaGenerator withTypeMappersPrepended(TypeMapper... typeMappers) {
        this.typeMapperProviders.add(0, (conf, current) -> current.insertAfterOrAppend(IdAdapter.class, typeMappers));
        return this;
    }

    /**
     * Registers custom {@link TypeMapper}s to be used for mapping Java type to GraphQL types.
     * <p><b>Ordering of mappers is strictly important as the first {@link TypeMapper} that supports the given Java type
     * will be used for mapping it.</b></p>
     * <p>See {@link TypeMapper#supports(AnnotatedType)}</p>
     *
     * @param provider Provides the customized list of TypeMappers to use
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withTypeMappers(ExtensionProvider<GeneratorConfiguration, TypeMapper> provider) {
        this.typeMapperProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withSchemaTransformers(SchemaTransformer... transformers) {
        return withSchemaTransformers((conf, current) -> current.append(transformers));
    }

    public GraphQLSchemaGenerator withSchemaTransformers(ExtensionProvider<GeneratorConfiguration, SchemaTransformer> provider) {
        this.schemaTransformerProviders.add(provider);
        return this;
    }

    /**
     * Registers custom {@link InputConverter}s to be used for converting values provided by the GraphQL client
     * into those expected by the corresponding Java method. Only needed in some specific cases when usual deserialization
     * isn't enough, for example, when a client-provided {@link java.util.List} should be repackaged into a {@link java.util.Map},
     * which is normally done because GraphQL type system has no direct support for maps.
     * <p><b>Ordering of converters is strictly important as the first {@link InputConverter} that supports the given Java type
     * will be used for converting it.</b></p>
     * <p>See {@link InputConverter#supports(AnnotatedType)}</p>
     *
     * @param inputConverters Custom input converters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withInputConverters(InputConverter<?,?>... inputConverters) {
        return withInputConverters((config, current) -> current.insert(0, inputConverters));
    }

    public GraphQLSchemaGenerator withInputConvertersPrepended(InputConverter<?,?>... inputConverters) {
        this.inputConverterProviders.add(0, (config, current) -> current.insert(0, inputConverters));
        return this;
    }

    public GraphQLSchemaGenerator withInputConverters(ExtensionProvider<GeneratorConfiguration, InputConverter> provider) {
        this.inputConverterProviders.add(provider);
        return this;
    }

    /**
     * Registers custom {@link OutputConverter}s to be used for converting values returned by the exposed Java method
     * into those expected by the GraphQL client. Only needed in some specific cases when usual serialization isn't enough,
     * for example, when an instance of {@link java.util.Map} should be repackaged into a {@link java.util.List}, which
     * is normally done because GraphQL type system has no direct support for maps.
     * <p><b>Ordering of converters is strictly important as the first {@link OutputConverter} that supports the given Java type
     * will be used for converting it.</b></p>
     * <p>See {@link OutputConverter#supports(AnnotatedType)}</p>
     *
     * @param outputConverters Custom output converters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOutputConverters(OutputConverter<?,?>... outputConverters) {
        return withOutputConverters((config, current) -> current.insertAfterOrAppend(IdAdapter.class, outputConverters));
    }

    public GraphQLSchemaGenerator withOutputConvertersPrepended(OutputConverter<?,?>... outputConverters) {
        this.outputConverterProviders.add(0, (config, current) -> current.insertAfterOrAppend(IdAdapter.class, outputConverters));
        return this;
    }

    public GraphQLSchemaGenerator withOutputConverters(ExtensionProvider<GeneratorConfiguration, OutputConverter> provider) {
        this.outputConverterProviders.add(provider);
        return this;
    }

    /**
     * Type adapters (instances of {@link AbstractTypeAdapter}) are both type mappers and bi-directional converters,
     * implementing {@link TypeMapper}, {@link InputConverter} and {@link OutputConverter}.
     * They're used in the same way as mappers/converters individually, and exist solely because it can sometimes
     * be convenient to group the logic for mapping and converting to/from the same Java type in one place.
     * For example, because GraphQL type system has no notion of maps, {@link java.util.Map}s require special logic
     * both when mapping them to a GraphQL type and when converting them before and after invoking a Java method.
     * For this reason, all code dealing with translating {@link java.util.Map}s is kept in one place in
     * {@link io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter}.
     * <p><b>Ordering of mappers/converters is strictly important as the first one supporting the given Java type
     * will be used to map/convert it.</b></p>
     * <p>See {@link #withTypeMappers(ExtensionProvider)}</p>
     * <p>See {@link #withInputConverters(ExtensionProvider)}</p>
     * <p>See {@link #withOutputConverters(ExtensionProvider)}</p>
     *
     * @param typeAdapters Custom type adapters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withTypeAdapters(AbstractTypeAdapter<?,?>... typeAdapters) {
        withInputConverters(typeAdapters);
        withOutputConverters(typeAdapters);
        return withTypeMappers((conf, defaults) -> defaults.insertAfter(ScalarMapper.class, typeAdapters));
    }

    public GraphQLSchemaGenerator withArgumentInjectors(ArgumentInjector... argumentInjectors) {
        return withArgumentInjectors((config, current) -> current.insert(0, argumentInjectors));
    }

    public GraphQLSchemaGenerator withArgumentInjectors(ExtensionProvider<GeneratorConfiguration, ArgumentInjector> provider) {
        this.argumentInjectorProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withModules(Module... modules) {
        return withModules((config, current) -> current.append(modules));
    }

    public GraphQLSchemaGenerator withModules(ExtensionProvider<GeneratorConfiguration, Module> provider) {
        this.moduleProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withResolverInterceptors(ResolverInterceptor... interceptors) {
        return withResolverInterceptorFactories((config, current) -> current.append(new GlobalResolverInterceptorFactory(Arrays.asList(interceptors))));
    }

    public GraphQLSchemaGenerator withResolverInterceptorFactories(ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory> provider) {
        this.interceptorFactoryProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withAdditionalTypes(Collection<GraphQLType> additionalTypes) {
        additionalTypes.stream()
                .filter(this::isRealType)
                .forEach(type -> {
                    if (this.additionalTypes.put(type.getName(), type) != null) {
                        throw new ConfigurationException("Type name collision: multiple registered additional types are named '" + type.getName() + "'");
                    }
                });
        return this;
    }

    public GraphQLSchemaGenerator withAdditionalDirectives(Type... additionalDirectives) {
        return withAdditionalDirectives(
                Arrays.stream(additionalDirectives).map(GenericTypeReflector::annotate).toArray(AnnotatedType[]::new));
    }

    public GraphQLSchemaGenerator withAdditionalDirectives(AnnotatedType... additionalDirectives) {
        Collections.addAll(this.additionalDirectiveTypes, additionalDirectives);
        return this;
    }

    public GraphQLSchemaGenerator withAdditionalDirectives(GraphQLDirective... additionalDirectives) {
        Arrays.stream(additionalDirectives)
                .forEach(directive -> {
                    if (this.additionalDirectives.put(directive.getName(), directive) != null) {
                        throw new ConfigurationException("Directive name collision: multiple registered additional directives are named '" + directive.getName() + "'");
                    }
                });
        return this;
    }

    public GraphQLSchemaGenerator withTypeSynonymGroup(Type... synonyms) {
        this.typeComparators.add(new BaseTypeSynonymComparator(synonyms));
        return this;
    }

    public GraphQLSchemaGenerator withTypeSynonymGroup(AnnotatedType... synonyms) {
        Set<AnnotatedType> synonymGroup = new AnnotatedTypeSet<>();
        Collections.addAll(synonymGroup, synonyms);
        this.typeComparators.add((t1, t2) -> synonymGroup.contains(t1) && synonymGroup.contains(t2) ? 0 : -1);
        return this;
    }

    public GraphQLSchemaGenerator withTypeComparator(Comparator<AnnotatedType> comparator) {
        this.typeComparators.add(comparator);
        return this;
    }

    public GraphQLSchemaGenerator withOperationBuilder(OperationBuilder operationBuilder) {
        this.operationBuilder = operationBuilder;
        return this;
    }

    public GraphQLSchemaGenerator withDirectiveBuilder(DirectiveBuilder directiveBuilder) {
        this.directiveBuilder = directiveBuilder;
        return this;
    }

    /**
     * Sets a flag that all mutations should be mapped in a Relay-compliant way,
     * using the default name and description for output wrapper fields.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayCompliantMutations() {
        return withRelayCompliantMutations("result", "Mutation result");
    }

    /**
     * Sets a flag signifying that all mutations should be mapped in a Relay-compliant way,
     * using the default name and description for output wrapper fields.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayCompliantMutations(String wrapperFieldName, String wrapperFieldDescription) {
        this.relayMappingConfig.relayCompliantMutations = true;
        this.relayMappingConfig.wrapperFieldName = wrapperFieldName;
        this.relayMappingConfig.wrapperFieldDescription = wrapperFieldDescription;
        return this;
    }

    /**
     * Sets the flag controlling whether the Node interface (as defined by the Relay spec) should be automatically
     * inferred for types that have an ID field.
     *
     * @param enabled Whether the inference should be enabled
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayNodeInterfaceInference(boolean enabled) {
        this.relayMappingConfig.inferNodeInterface = enabled;
        return this;
    }

    /**
     * Removes the requirement on queries returning a Connection to comply with the Relay Connection spec
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayConnectionCheckRelaxed() {
        this.relayMappingConfig.strictConnectionSpec = false;
        return this;
    }

    /**
     * Registers custom schema processors that can perform arbitrary transformations on the schema just before it is built.
     *
     * @param processors Custom processors to call right before the GraphQL schema is built
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withSchemaProcessors(GraphQLSchemaProcessor... processors) {
        addAll(this.processors, processors);
        return this;
    }

    /**
     * Sets the default values for all settings not configured explicitly,
     * ensuring the builder is in a valid state
     */
    private void init() {
        GeneratorConfiguration configuration = new GeneratorConfiguration(interfaceStrategy, scalarStrategy, typeTransformer, basePackages, javaDeprecationConfig);
        if (operationSourceRegistry.isEmpty()) {
            throw new IllegalStateException("At least one top-level operation source must be registered");
        }

        if (inclusionStrategy == null) {
            inclusionStrategy = new DefaultInclusionStrategy(basePackages);
        }
        ValueMapperFactory internalValueMapperFactory = valueMapperFactory != null
                ? valueMapperFactory
                : Defaults.valueMapperFactory(typeInfoGenerator);
        if (scalarStrategy == null) {
            if (internalValueMapperFactory instanceof ScalarDeserializationStrategy) {
                scalarStrategy = (ScalarDeserializationStrategy) internalValueMapperFactory;
            } else {
                scalarStrategy = (ScalarDeserializationStrategy) Defaults.valueMapperFactory(typeInfoGenerator);
            }
        }

        List<Module> modules = Defaults.modules();
        for (ExtensionProvider<GeneratorConfiguration, Module> provider : moduleProviders) {
            modules = provider.getExtensions(configuration, new ExtensionList<>(modules));
        }
        checkForDuplicates("modules", modules);
        modules.forEach(module -> module.setUp(() -> this));

        List<ResolverBuilder> resolverBuilders = Collections.singletonList(new AnnotatedResolverBuilder());
        for (ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider : resolverBuilderProviders) {
            resolverBuilders = provider.getExtensions(configuration, new ExtensionList<>(resolverBuilders));
        }
        checkForEmptyOrDuplicates("resolver builders", resolverBuilders);
        operationSourceRegistry.registerGlobalResolverBuilders(resolverBuilders);

        List<ResolverBuilder> nestedResolverBuilders = Arrays.asList(
                new AnnotatedResolverBuilder(),
                new BeanResolverBuilder(basePackages).withJavaDeprecation(javaDeprecationConfig));
        for (ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider : nestedResolverBuilderProviders) {
            nestedResolverBuilders = provider.getExtensions(configuration, new ExtensionList<>(nestedResolverBuilders));
        }
        checkForEmptyOrDuplicates("nested resolver builders", nestedResolverBuilders);
        operationSourceRegistry.registerGlobalNestedResolverBuilders(nestedResolverBuilders);

        ObjectTypeMapper objectTypeMapper = new ObjectTypeMapper();
        PublisherAdapter publisherAdapter = new PublisherAdapter();
        EnumMapper enumMapper = new EnumMapper(javaDeprecationConfig);
        typeMappers = Arrays.asList(
                new NonNullMapper(), new IdMapper(), new IdAdapter(), new ScalarMapper(), new CompletableFutureMapper(),
                publisherAdapter, new AnnotationMapper(), new OptionalIntAdapter(), new OptionalLongAdapter(), new OptionalDoubleAdapter(),
                enumMapper, new ArrayAdapter(), new UnionTypeMapper(), new UnionInlineMapper(),
                new StreamToCollectionTypeAdapter(), new DataFetcherResultMapper(), new VoidToBooleanTypeAdapter(),
                new ListMapper(), new IterableAdapter<>(), new PageMapper(), new OptionalAdapter(), new EnumMapToObjectTypeAdapter(enumMapper),
                new ObjectScalarMapper(), new InterfaceMapper(interfaceStrategy, objectTypeMapper), objectTypeMapper);
        for (ExtensionProvider<GeneratorConfiguration, TypeMapper> provider : typeMapperProviders) {
            typeMappers = provider.getExtensions(configuration, new ExtensionList<>(typeMappers));
        }
        checkForEmptyOrDuplicates("type mappers", typeMappers);

        transformers = Arrays.asList(new NonNullMapper(), publisherAdapter);
        for (ExtensionProvider<GeneratorConfiguration, SchemaTransformer> provider : schemaTransformerProviders) {
            transformers = provider.getExtensions(configuration, new ExtensionList<>(transformers));
        }
        checkForEmptyOrDuplicates("schema transformers", transformers);

        List<OutputConverter> outputConverters = Arrays.asList(
                new IdAdapter(), new VoidToBooleanTypeAdapter(), new ArrayAdapter(), new CollectionOutputConverter(),
                new OptionalIntAdapter(), new OptionalLongAdapter(), new OptionalDoubleAdapter(), new OptionalAdapter(),
                new StreamToCollectionTypeAdapter(), publisherAdapter);
        for (ExtensionProvider<GeneratorConfiguration, OutputConverter> provider : outputConverterProviders) {
            outputConverters = provider.getExtensions(configuration, new ExtensionList<>(outputConverters));
        }
        checkForDuplicates("output converters", outputConverters);

        List<InputConverter> inputConverters = Arrays.asList(
                new StreamToCollectionTypeAdapter(), new IterableAdapter<>(), new EnumMapToObjectTypeAdapter(enumMapper));
        for (ExtensionProvider<GeneratorConfiguration, InputConverter> provider : inputConverterProviders) {
            inputConverters = provider.getExtensions(configuration, new ExtensionList<>(inputConverters));
        }
        checkForDuplicates("input converters", inputConverters);

        List<ArgumentInjector> argumentInjectors = Arrays.asList(
                new IdAdapter(), new RootContextInjector(), new ContextInjector(),
                new EnvironmentInjector(), new DirectiveValueDeserializer(), new InputValueDeserializer());
        for (ExtensionProvider<GeneratorConfiguration, ArgumentInjector> provider : argumentInjectorProviders) {
            argumentInjectors = provider.getExtensions(configuration, new ExtensionList<>(argumentInjectors));
        }
        checkForDuplicates("argument injectors", argumentInjectors);

        List<ResolverInterceptorFactory> interceptorFactories = Collections.emptyList();
        for (ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory> provider : this.interceptorFactoryProviders) {
            interceptorFactories = provider.getExtensions(configuration, new ExtensionList<>(interceptorFactories));
        }
        interceptorFactory = new DelegatingResolverInterceptorFactory(interceptorFactories);

        environment = new GlobalEnvironment(messageBundle, new Relay(), new TypeRegistry(additionalTypes.values()),
                new ConverterRegistry(inputConverters, outputConverters), new ArgumentInjectorRegistry(argumentInjectors),
                typeTransformer, inclusionStrategy, typeInfoGenerator);
        ExtendedGeneratorConfiguration extendedConfig = new ExtendedGeneratorConfiguration(configuration, environment);
        valueMapperFactory = new MemoizedValueMapperFactory(environment, internalValueMapperFactory);
        ValueMapper def = valueMapperFactory.getValueMapper(Collections.emptyMap(), environment);

        InputFieldBuilder defaultInputFieldBuilder;
        if (def instanceof InputFieldBuilder) {
            defaultInputFieldBuilder = (InputFieldBuilder) def;
        } else {
            defaultInputFieldBuilder = (InputFieldBuilder) Defaults.valueMapperFactory(typeInfoGenerator).getValueMapper(Collections.emptyMap(), environment);
        }
        inputFieldBuilders = Arrays.asList(new AnnotationInputFieldBuilder(), defaultInputFieldBuilder);
        for (ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder> provider : this.inputFieldBuilderProviders) {
            inputFieldBuilders = provider.getExtensions(extendedConfig, new ExtensionList<>(inputFieldBuilders));
        }
        checkForEmptyOrDuplicates("input field builders", inputFieldBuilders);

        Type annotatedTypeComparator = TypeFactory.parameterizedClass(Comparator.class, AnnotatedType.class);
        //noinspection unchecked
        typeMappers.stream()
                .filter(mapper -> GenericTypeReflector.isSuperType(annotatedTypeComparator, mapper.getClass()))
                .forEach(mapper -> typeComparators.add((Comparator<AnnotatedType>) mapper));
        typeComparator = (t1, t2) -> typeComparators.stream().anyMatch(comparator -> comparator.compare(t1, t2) == 0) ? 0 : -1;
    }

    /**
     * Generates a GraphQL schema based on the results of analysis of the registered sources. All exposed methods will be mapped
     * as queries or mutations and all Java types referred to by those methods will be mapped to corresponding GraphQL types.
     * Such schema can then be used to construct {@link graphql.GraphQL} instances. See the example in the description of this class.
     *
     * @return A GraphQL schema
     */
    public GraphQLSchema generate() {
        init();

        BuildContext buildContext = new BuildContext(
                basePackages, environment, new OperationRegistry(operationSourceRegistry, operationBuilder, inclusionStrategy,
                typeTransformer, basePackages, environment), new TypeMapperRegistry(typeMappers),
                new SchemaTransformerRegistry(transformers), valueMapperFactory, typeInfoGenerator, messageBundle, interfaceStrategy,
                scalarStrategy, typeTransformer, abstractInputHandler, new InputFieldBuilderRegistry(inputFieldBuilders),
                interceptorFactory, directiveBuilder, inclusionStrategy, relayMappingConfig, additionalTypes(), additionalDirectiveTypes,
                typeComparator, implDiscoveryStrategy);
        OperationMapper operationMapper = new OperationMapper(buildContext);

        GraphQLSchema.Builder builder = GraphQLSchema.newSchema();
        builder.query(newObject()
                .name(messageBundle.interpolate(queryRoot))
                .description(messageBundle.interpolate(queryRootDescription))
                .fields(operationMapper.getQueries())
                .build());

        List<GraphQLFieldDefinition> mutations = operationMapper.getMutations();
        if (!mutations.isEmpty()) {
            builder.mutation(newObject()
                    .name(messageBundle.interpolate(mutationRoot))
                    .description(messageBundle.interpolate(mutationRootDescription))
                    .fields(mutations)
                    .build());
        }

        List<GraphQLFieldDefinition> subscriptions = operationMapper.getSubscriptions();
        if (!subscriptions.isEmpty()) {
            builder.subscription(newObject()
                    .name(messageBundle.interpolate(subscriptionRoot))
                    .description(messageBundle.interpolate(subscriptionRootDescription))
                    .fields(subscriptions)
                    .build());
        }

        Set<GraphQLType> additional = new HashSet<>(additionalTypes.values());
        additional.addAll(buildContext.typeRegistry.getDiscoveredTypes());
        builder.additionalTypes(additional);

        builder.additionalDirectives(new HashSet<>(additionalDirectives.values()));
        builder.additionalDirectives(new HashSet<>(operationMapper.getDirectives()));

        applyProcessors(builder, buildContext);
        buildContext.executePostBuildHooks();
        return builder.build();
    }

    private void applyProcessors(GraphQLSchema.Builder builder, BuildContext buildContext) {
        for (GraphQLSchemaProcessor processor : processors) {
            processor.process(builder, buildContext);
        }
    }

    private boolean isRealType(GraphQLType type) {
        // Reject introspection types
        return !(GraphQLUtils.isIntrospectionType(type)
                // Reject quasi-types
                || type instanceof GraphQLTypeReference
                || type instanceof GraphQLArgument
                || type instanceof GraphQLDirective
                // Reject root types
                || type.getName().equals(messageBundle.interpolate(queryRoot))
                || type.getName().equals(messageBundle.interpolate(mutationRoot))
                || type.getName().equals(messageBundle.interpolate(subscriptionRoot)));
    }

    private Collection<GraphQLType> additionalTypes() {
        Set<GraphQLType> additional = Stream.concat(
                additionalTypes.values().stream(),
                additionalDirectives.values().stream().flatMap(directive -> directive.getArguments().stream().map(GraphQLArgument::getType)))
                .filter(this::isRealType)
                .collect(Collectors.toSet());
        additional.stream().collect(Collectors.groupingBy(GraphQLType::getName)).forEach((name, types) -> {
            if (types.stream().anyMatch(type -> types.stream().anyMatch(t -> !t.equals(type)))) {
                throw new ConfigurationException("Type name collision: multiple registered additional types are named '" + name + "'");
            }
        });
        return additional;
    }

    private void checkType(Type type) {
        if (type == null) {
            throw new TypeMappingException();
        }
        Class<?> clazz = ClassUtils.getRawType(type);
        if (ClassUtils.isProxy(clazz)) {
            throw new TypeMappingException("The registered object of type " + clazz.getName() +
                    " appears to be a dynamically generated proxy, so its type can not be reliably determined." +
                    " Provide the type explicitly when registering the bean." +
                    " For details and solutions see " + Urls.Errors.DYNAMIC_PROXIES);
        }
        if (ClassUtils.isMissingTypeParameters(type)) {
            throw new TypeMappingException("The registered object is of generic type " + type.getTypeName() + "." +
                    " Provide the full type explicitly when registering the bean." +
                    " For details and solutions see " + Urls.Errors.TOP_LEVEL_GENERICS);
        }
    }

    private void checkType(AnnotatedType type) {
        if (type == null) {
            throw new TypeMappingException();
        }
        checkType(type.getType());
    }

    private void checkForEmptyOrDuplicates(String extensionType, List<?> extensions) {
        if (extensions.isEmpty()) {
            throw new ConfigurationException("No " + extensionType + "SimpleFieldValidation registered");
        }
        checkForDuplicates(extensionType, extensions);
    }

    private void checkForDuplicates(String extensionType, List<?> extensions) {
        Set<Class<?>> classes = new HashSet<>();
        extensions.stream()
                .map(Object::getClass)
                .forEach(clazz -> {
                    if (!classes.add(clazz)) {
                        throw new ConfigurationException("Multiple " + extensionType + " of type " + clazz.getName() + " registered");
                    }
                });
    }

    private static class MemoizedValueMapperFactory implements ValueMapperFactory {

        private final ValueMapper defaultValueMapper;
        private final ValueMapperFactory delegate;

        public MemoizedValueMapperFactory(GlobalEnvironment environment, ValueMapperFactory delegate) {
            this.defaultValueMapper = delegate.getValueMapper(Collections.emptyMap(), environment);
            this.delegate = delegate;
        }

        @Override
        public ValueMapper getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
            if (concreteSubTypes.isEmpty() || concreteSubTypes.values().stream().allMatch(List::isEmpty)) {
                return this.defaultValueMapper;
            }
            return delegate.getValueMapper(concreteSubTypes, environment);
        }
    }

    private static class GlobalResolverInterceptorFactory implements ResolverInterceptorFactory {

        private final List<ResolverInterceptor> interceptors;

        private GlobalResolverInterceptorFactory(List<ResolverInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        @Override
        public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
            return interceptors;
        }
    }

    private static class DelegatingResolverInterceptorFactory implements ResolverInterceptorFactory {

        private final List<ResolverInterceptorFactory> delegates;

        private DelegatingResolverInterceptorFactory(List<ResolverInterceptorFactory> delegates) {
            this.delegates = delegates;
        }

        @Override
        public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
            return delegates.stream()
                    .flatMap(delegate -> delegate.getInterceptors(params).stream())
                    .collect(Collectors.toList());
        }
    }
}
