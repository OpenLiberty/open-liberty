package io.leangen.graphql.metadata.execution;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Created by bojan.tomic on 7/20/16.
 */
public class FieldAccessor extends Executable<Field> {

    private AnnotatedType enclosingType;

    public FieldAccessor(Field field, AnnotatedType enclosingType) {
        this.delegate = field;
        this.enclosingType = enclosingType;
    }

    @Override
    public Object execute(Object target, Object[] args) throws IllegalAccessException {
        return delegate.get(target);
    }

    @Override
    public AnnotatedType getReturnType() {
        return ClassUtils.getFieldType(delegate, enclosingType);
    }

    /**
     * {@inheritDoc}
     * Always 0 for @{FieldAccessor}
     */
    @Override
    public int getParameterCount() {
        return 0;
    }

    @Override
    public AnnotatedType[] getAnnotatedParameterTypes() {
        return new AnnotatedType[0];
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[0];
    }
}
