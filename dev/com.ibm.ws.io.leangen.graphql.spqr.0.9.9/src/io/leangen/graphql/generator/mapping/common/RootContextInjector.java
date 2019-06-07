package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.annotations.Context;
import io.leangen.graphql.execution.ContextWrapper;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RootContextInjector implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        String injectionExpression = params.getParameter().getAnnotation(Context.class).value();
        ResolutionEnvironment env = params.getResolutionEnvironment();
        Object rootContext = env.rootContext instanceof ContextWrapper
                ? ((ContextWrapper) env.rootContext).getContext()
                : env.rootContext;
        return injectionExpression.isEmpty() ? rootContext : extract(rootContext, injectionExpression);
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return parameter != null && parameter.isAnnotationPresent(Context.class);
    }
    
    private Object extract(Object input, String expression) {
        if (input instanceof Map) {
            return ((Map) input).get(expression);
        }
        return ClassUtils.getFieldValue(input, expression);
    }
}
