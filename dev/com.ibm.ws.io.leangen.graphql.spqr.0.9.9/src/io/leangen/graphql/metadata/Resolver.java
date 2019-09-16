package io.leangen.graphql.metadata;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.exceptions.MappingException;
import io.leangen.graphql.metadata.execution.Executable;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class representing a single method used to resolve a specific query given specific arguments.
 * A single query can have multiple resolvers, corresponding to different combinations of arguments.
 * This is done mainly to support attaching multiple overloaded methods as resolvers for the same query.
 * Two resolvers of the same query must not accept the same list of argument names.
 *
 * @author bojan.tomic (kaqqao)
 */
public class Resolver {

    private final String operationName;
    private final String operationDescription;
    private final String operationDeprecationReason;
    private final List<OperationArgument> arguments;
    private final AnnotatedType returnType;
    private final Set<OperationArgument> contextArguments;
    private final String complexityExpression;
    private final Executable executable;
    private final boolean batched;

    public Resolver(String operationName, String operationDescription, String operationDeprecationReason, boolean batched,
                    Executable executable, AnnotatedType returnType, List<OperationArgument> arguments, String complexityExpression) {

        Set<OperationArgument> contextArguments = resolveContexts(arguments);
        
        if (batched) {
            validateBatching(executable.toString(), returnType, contextArguments);
        }
        
        this.operationName = validateName(operationName, executable);
        this.operationDescription = operationDescription;
        this.operationDeprecationReason = operationDeprecationReason;
        this.arguments = arguments;
        this.returnType = returnType;
        this.contextArguments = contextArguments;
        this.complexityExpression = complexityExpression;
        this.executable = executable;
        this.batched = batched;
    }

    private String validateName(String operationName, Executable executable) {
        if (Utils.isEmpty(operationName)) {
            throw new MappingException("The operation name for executable " + executable.toString() + " could not be determined");
        }
        return operationName;
    }

    private void validateBatching(String executableSignature, AnnotatedType returnType, Set<OperationArgument> contextArguments) {
        if (contextArguments.isEmpty() || !Stream.concat(contextArguments.stream().map(arg -> arg.getJavaType().getType()), Stream.of(returnType.getType()))
                .allMatch(type -> GenericTypeReflector.isSuperType(List.class, type))) {
            throw new IllegalArgumentException("Resolver method " + executableSignature
                    + " is marked as batched but doesn't return a list or its context argument is not a list");
        }
    }
    
    /**
     * Finds the argument representing the query context (object returned by the parent query), if it exists.
     * Query context arguments potentially exist only for the resolvers of nestable queries.
     * Even then, not all resolvers of such queries necessarily accept a context object.
     *
     * @param arguments All arguments that this resolver accepts
     * @return The arguments representing possible query contexts for this resolver
     * (object returned by the parent query), or null if this resolver doesn't accept a query context
     */
    private Set<OperationArgument> resolveContexts(List<OperationArgument> arguments) {
        return arguments.stream()
                .filter(OperationArgument::isContext)
                .collect(Collectors.toSet());
    }

    /**
     * Calls the underlying resolver  method/field
     *
     * @param source The object on which the method/field is to be called
     * @param args Arguments to the underlying method (empty if the underlying resolver is a field)
     *
     * @return The result returned by the underlying method/field
     *
     * @throws InvocationTargetException If a reflective invocation of the underlying method/field fails
     * @throws IllegalAccessException If a reflective invocation of the underlying method/field is not allowed
     */
    @SuppressWarnings("unchecked")
    public Object resolve(Object source, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return executable.execute(source, args);
    }

    /**
     * Gets the generic Java types of the source objects (object returned by the parent query),
     * if one is accepted by this resolver. Used to decide if this query can be nested inside another.
     *
     * @return The generic Java type of the source object, or null if this resolver does not accept one.
     */
    public Set<Type> getSourceTypes() {
        return contextArguments.stream().map(arg -> arg.getJavaType().getType()).collect(Collectors.toSet());
    }

    public String getOperationName() {
        return operationName;
    }

    public boolean isBatched() {
        return batched;
    }

    public String getOperationDescription() {
        return operationDescription;
    }

    public String getOperationDeprecationReason() {
        return operationDeprecationReason;
    }

    /**
     * Get the fingerprint of this resolver. Fingerprint uniquely identifies a resolver within a query.
     * It is based on the name of the query and all parameters this specific resolver accepts.
     * It is used to decide which resolver to invoke for the query, based on the provided arguments.
     *
     * @return The unique "fingerprint" string identifying this resolver
     */
    public Set<String> getFingerprints() {
        Set<String> fingerprints = new HashSet<>(contextArguments.size() + 1);
        contextArguments.forEach(context -> fingerprints.add(fingerprint(context)));
        fingerprints.add(fingerprint(null));
        return fingerprints;
    }

    public List<OperationArgument> getArguments() {
        return arguments;
    }

    public AnnotatedType getReturnType() {
        return returnType;
    }

    public String getComplexityExpression() {
        return complexityExpression;
    }

    public Executable getExecutable() {
        return executable;
    }

    private String fingerprint(OperationArgument ignoredResolverSource) {
        StringBuilder fingerprint = new StringBuilder();
        arguments.stream()
                .filter(arg -> arg != ignoredResolverSource)
                .filter(OperationArgument::isMappable)
                .map(OperationArgument::getName)
                .sorted()
                .forEach(fingerprint::append);
        return fingerprint.toString();
    }

    @Override
    public boolean equals(Object that) {
        return this == that || (that instanceof Resolver && this.executable.equals(((Resolver) that).executable));
    }

    @Override
    public int hashCode() {
        return executable.hashCode();
    }

    @Override
    public String toString() {
        return executable.toString();
    }
}
