package io.leangen.graphql.metadata.strategy.query;

import graphql.language.OperationDefinition;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.union.Union;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Urls;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
@SuppressWarnings("WeakerAccess")
public class DefaultOperationBuilder implements OperationBuilder {

    private final TypeInference typeInference;

    /**
     * @param typeInference Controls automatic type inference if multiple resolver methods for the same operation return different types,
     * or if different resolver methods specify arguments of the same name but of different types.
     * <p>The inference process selects the most specific common super type of all the detected types.</p>
     * <p>This feature is off by default as it can lead to surprising results when used unconsciously.</p>
     *
     * <p><b>Example:</b></p>
     *
     * <pre>
     * {@code
     * @GraphQLQuery(name = "numbers")
     * public ArrayList<Long> getLongs(String paramOne) {...}
     *
     * @GraphQLQuery(name = "numbers")
     * public LinkedList<Double> getDoubles(String paramTwo) {...}
     * }
     * </pre>
     *
     * The situation shown above would cause an exception without type inference enabled.
     * With type inference, the return type would be treated as {@code AbstractList<Number>} as that
     * is the most specific common super type of the two encountered types.
     */

    public DefaultOperationBuilder(TypeInference typeInference) {
        this.typeInference = typeInference;
    }

    @Override
    public Operation buildQuery(Type contextType, List<Resolver> resolvers, GlobalEnvironment environment) {
        return buildOperation(contextType, resolvers, OperationDefinition.Operation.QUERY, environment);
    }

    @Override
    public Operation buildMutation(Type context, List<Resolver> resolvers, GlobalEnvironment environment) {
        return buildOperation(context, resolvers, OperationDefinition.Operation.MUTATION, environment);
    }

    @Override
    public Operation buildSubscription(Type context, List<Resolver> resolvers, GlobalEnvironment environment) {
        return buildOperation(context, resolvers, OperationDefinition.Operation.SUBSCRIPTION, environment);
    }

    private Operation buildOperation(Type contextType, List<Resolver> resolvers, OperationDefinition.Operation operationType, GlobalEnvironment environment) {
        String name = resolveName(resolvers);
        AnnotatedType javaType = resolveJavaType(name, resolvers, environment.messageBundle);
        List<OperationArgument> arguments = collectArguments(name, resolvers);
        boolean batched = isBatched(resolvers);
        return new Operation(name, javaType, contextType, arguments, resolvers, operationType, batched);
    }

    protected String resolveName(List<Resolver> resolvers) {
        return resolvers.get(0).getOperationName();
    }

    protected AnnotatedType resolveJavaType(String operationName, List<Resolver> resolvers, MessageBundle messageBundle) {
        List<AnnotatedType> returnTypes = resolvers.stream()
                .map(Resolver::getReturnType)
                .collect(Collectors.toList());

        if (resolvers.stream().anyMatch(resolver -> ClassUtils.containsTypeAnnotation(resolver.getReturnType(), GraphQLUnion.class))) {
            return unionize(returnTypes.toArray(new AnnotatedType[0]), messageBundle);
        }

        return resolveJavaType(returnTypes, "Multiple methods detected for operation \"" + operationName + "\" with different return types.");
    }

    //TODO do annotations or overloading decide what arg is required? should that decision be externalized?
    protected List<OperationArgument> collectArguments(String operationName, List<Resolver> resolvers) {
        Map<String, List<OperationArgument>> argumentsByName = resolvers.stream()
                .flatMap(resolver -> resolver.getArguments().stream()) // merge all known args for this query
                .collect(Collectors.groupingBy(OperationArgument::getName));

        String errorPrefixTemplate = "Argument %s of operation \"" + operationName + "\" has different types in different resolver methods.";
        return argumentsByName.keySet().stream()
                .map(argName -> new OperationArgument(
                        resolveJavaType(argumentsByName.get(argName).stream().map(OperationArgument::getJavaType).collect(Collectors.toList()), String.format(errorPrefixTemplate, argName)),
                        argName,
                        argumentsByName.get(argName).stream().map(OperationArgument::getDescription).filter(Objects::nonNull).findFirst().orElse(""),
//						argumentsByName.get(argName).size() == resolvers.size() || argumentsByName.get(argName).stream().anyMatch(OperationArgument::isRequired),
                        argumentsByName.get(argName).stream().map(OperationArgument::getDefaultValue).filter(Objects::nonNull).findFirst().orElse(null),
                        argumentsByName.get(argName).stream().map(OperationArgument::getParameter).filter(Objects::nonNull).collect(Collectors.toList()),
                        argumentsByName.get(argName).stream().anyMatch(OperationArgument::isContext),
                        argumentsByName.get(argName).stream().anyMatch(OperationArgument::isMappable)
                ))
                .collect(Collectors.toList());
    }

    protected boolean isBatched(List<Resolver> resolvers) {
        return resolvers.stream().anyMatch(Resolver::isBatched);
    }

    protected AnnotatedType unionize(AnnotatedType[] types, MessageBundle messageBundle) {
        return Union.unionize(types, messageBundle);
    }

    private AnnotatedType resolveJavaType(List<AnnotatedType> types, String errorPrefix) {
        errorPrefix = errorPrefix + " Types found: " + Arrays.toString(types.stream().map(type -> type.getType().getTypeName()).toArray()) + ". ";
        if (!typeInference.inferTypes && !types.stream().map(AnnotatedType::getType).allMatch(type -> type.equals(types.get(0).getType()))) {
            throw new TypeMappingException(errorPrefix + "If this is intentional, and you wish GraphQL SPQR to infer the most " +
                    "common super type automatically, see " + Urls.Errors.CONFLICTING_RESOLVER_TYPES);
        }
        try {
            return ClassUtils.getCommonSuperType(types, typeInference.allowObject ? GenericTypeReflector.annotate(Object.class) : null);
        } catch (TypeMappingException e) {
            throw new TypeMappingException(errorPrefix, e);
        }
    }

    /**
     * <p>{@code NONE} - No type inference. Results in a {@link TypeMappingException} if multiple different types are encountered.</p>
     * <p>{@code LIMITED} - Automatically infer the common super type. Results in a {@link TypeMappingException} if no common ancestors except
     * {@link Object}, {@link java.io.Serializable}, {@link Cloneable}, {@link Comparable} or {@link java.lang.annotation.Annotation} are found.</p>
     * <p>{@code UNRESTRICTED} - Automatically infer the common super type. Results in {@link Object} if no common ancestors are found.</p>
     */
    public enum TypeInference {
        
        NONE(false, false), LIMITED(true, false), UNLIMITED(true, true);
        
        public final boolean inferTypes;
        public final boolean allowObject;

        TypeInference(boolean inferTypes, boolean allowObject) {
            this.inferTypes = inferTypes;
            this.allowObject = allowObject;
        }
    }
}
