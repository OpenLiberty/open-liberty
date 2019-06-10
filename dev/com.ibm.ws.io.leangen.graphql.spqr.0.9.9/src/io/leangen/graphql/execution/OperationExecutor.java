package io.leangen.graphql.execution;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ConverterRegistry;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.leangen.graphql.util.GraphQLUtils.CLIENT_MUTATION_ID;

/**
 * Created by bojan.tomic on 1/29/17.
 */
public class OperationExecutor {

    private final Operation operation;
    private final ValueMapper valueMapper;
    private final GlobalEnvironment globalEnvironment;
    private final ConverterRegistry converterRegistry;
    private final Map<Resolver, List<ResolverInterceptor>> interceptors;

    public OperationExecutor(Operation operation, ValueMapper valueMapper, GlobalEnvironment globalEnvironment, ResolverInterceptorFactory interceptorFactory) {
        this.operation = operation;
        this.valueMapper = valueMapper;
        this.globalEnvironment = globalEnvironment;
        this.converterRegistry = filterConverters(globalEnvironment.converters);
        this.interceptors = operation.getResolvers().stream().distinct().collect(Collectors.toMap(Function.identity(),
                res -> interceptorFactory.getInterceptors(new ResolverInterceptorFactoryParams(res))));
    }

    public Object execute(DataFetchingEnvironment env) throws Exception {
        Resolver resolver;
        if (env.getContext() instanceof ContextWrapper) {
            ContextWrapper context = env.getContext();
            if (env.getArgument(CLIENT_MUTATION_ID) != null) {
                context.setClientMutationId(env.getArgument(CLIENT_MUTATION_ID));
            }
        }

        Map<String, Object> arguments = env.getArguments();
        resolver = this.operation.getApplicableResolver(arguments.keySet());
        if (resolver == null) {
            throw new GraphQLException("Resolver for operation " + operation.getName() + " accepting arguments: "
                    + arguments.keySet() + " not implemented");
        }
        ResolutionEnvironment resolutionEnvironment = new ResolutionEnvironment(env, this.valueMapper, this.globalEnvironment, this.converterRegistry);
        try {
            Object result = execute(resolver, resolutionEnvironment, arguments);
            return resolutionEnvironment.convertOutput(result, resolver.getReturnType());
        } catch (ReflectiveOperationException e) {
            sneakyThrow(unwrap(e));
        }
        return null; //never happens, needed because of sneakyThrow
    }

    /**
     * Prepares input arguments by calling respective {@link ArgumentInjector}s
     * and invokes the underlying resolver method/field
     *
     * @param resolver The resolver to be invoked once the arguments are prepared
     * @param resolutionEnvironment An object containing all contextual information needed during operation resolution
     * @param rawArguments Raw input arguments provided by the client
     *
     * @return The result returned by the underlying method/field, potentially proxied and wrapped
     *
     * @throws Exception If the invocation of the underlying method/field or any of the interceptors throws
     */
    private Object execute(Resolver resolver, ResolutionEnvironment resolutionEnvironment, Map<String, Object> rawArguments)
            throws Exception {

        int queryArgumentsCount = resolver.getArguments().size();

        Object[] args = new Object[queryArgumentsCount];
        for (int i = 0; i < queryArgumentsCount; i++) {
            OperationArgument argDescriptor =  resolver.getArguments().get(i);
            Object rawArgValue = rawArguments.get(argDescriptor.getName());

            args[i] = resolutionEnvironment.getInputValue(rawArgValue, argDescriptor);
        }
        InvocationContext invocationContext = new InvocationContext(operation, resolver, resolutionEnvironment, args);
        Queue<ResolverInterceptor> interceptors = new LinkedList<>(this.interceptors.get(resolver));
        interceptors.add((ctx, cont) -> resolver.resolve(ctx.getResolutionEnvironment().context, ctx.getArguments()));
        return execute(invocationContext, interceptors);
    }

    private Object execute(InvocationContext context, Queue<ResolverInterceptor> interceptors) throws Exception {
        return interceptors.remove().aroundInvoke(context, (ctx) -> execute(ctx, interceptors));
    }

    private ConverterRegistry filterConverters(ConverterRegistry converters) {
        return operation.getResolvers().stream()
                .allMatch(res -> globalEnvironment.converters.getOutputConverter(res.getReturnType()) == null)
                ? new ConverterRegistry(converters.getInputConverters(), Collections.emptyList())
                : converters;
    }

    private Throwable unwrap(ReflectiveOperationException e) {
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            return cause;
        }
        return e;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
