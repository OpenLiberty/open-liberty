package io.leangen.graphql.execution;

public interface ResolverInterceptor {

    Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception;

    interface Continuation {
        Object proceed(InvocationContext context) throws Exception;
    }
}
