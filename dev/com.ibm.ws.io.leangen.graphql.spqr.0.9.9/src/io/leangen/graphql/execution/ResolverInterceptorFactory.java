package io.leangen.graphql.execution;

import java.util.List;

public interface ResolverInterceptorFactory {

    List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params);
}
