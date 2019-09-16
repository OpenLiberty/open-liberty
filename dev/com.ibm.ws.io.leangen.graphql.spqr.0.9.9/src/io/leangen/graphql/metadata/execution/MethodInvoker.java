package io.leangen.graphql.metadata.execution;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Created by bojan.tomic on 7/20/16.
 */
public class MethodInvoker extends Executable<Method> {

    private AnnotatedType enclosingType;
    private AnnotatedType returnType;

    public MethodInvoker(Method resolverMethod, AnnotatedType enclosingType) {
        this.delegate = resolverMethod;
        this.enclosingType = enclosingType;
        this.returnType = resolveReturnType(enclosingType);
    }

    @Override
    public Object execute(Object target, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return delegate.invoke(target, args);
    }

    @Override
    public AnnotatedType getReturnType() {
        return returnType;
    }


    private AnnotatedType resolveReturnType(AnnotatedType enclosingType) {
        return ClassUtils.getReturnType(delegate, enclosingType);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.reflect.Executable#getParameterCount
     */
    @Override
    public int getParameterCount() {
        return delegate.getParameterCount();
    }

    @Override
    public AnnotatedType[] getAnnotatedParameterTypes() {
        return ClassUtils.getParameterTypes(delegate, enclosingType);
    }

    @Override
    public Parameter[] getParameters() {
        return delegate.getParameters();
    }
}
