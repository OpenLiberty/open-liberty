package io.leangen.graphql.metadata.execution;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/5/16.
 */
public class SingletonMethodInvoker extends MethodInvoker {

    private Object target;

    public SingletonMethodInvoker(Object target, Method resolverMethod, AnnotatedType enclosingType) {
        super(resolverMethod, enclosingType);
        this.target = target;
    }

    @Override
    public Object execute(Object target, Object[] arguments) throws InvocationTargetException, IllegalAccessException {
        return delegate.invoke(this.target, arguments);
    }
}
