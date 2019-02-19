package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * A resolver builder that exposes all public getter methods
 */
public class BeanResolverBuilder extends PublicResolverBuilder {

    public BeanResolverBuilder(String... basePackages) {
        super(basePackages);
        this.operationNameGenerator = new PropertyOperationNameGenerator();
    }

    @Override
    protected boolean isQuery(Method method) {
        return super.isQuery(method) && ClassUtils.isGetter(method);
    }

    @Override
    protected boolean isMutation(Method method) {
        return ClassUtils.isSetter(method);
    }
}
