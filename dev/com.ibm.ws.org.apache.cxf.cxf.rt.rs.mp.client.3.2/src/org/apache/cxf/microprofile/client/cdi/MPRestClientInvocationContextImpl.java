/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.cxf.microprofile.client.cdi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.interceptor.InvocationContext;


class MPRestClientInvocationContextImpl implements InvocationContext {

    private final Object target;

    private final Method method;

    private Object[] args;

    private int index = 0;

    private final List<InterceptorInvoker> interceptorInvokers;

    private final Map<String, Object> contextData = new HashMap<>();

    private final Callable<Object> callable;
    /**
     * @param target
     * @param method
     * @param args
     * @param interceptorInvokers
     */
    MPRestClientInvocationContextImpl(Object target, Method method, Object[] args, List<InterceptorInvoker> interceptorInvokers, Callable<Object> callable) {
        this.target = target;
        this.method = method;
        this.args = args == null ? new Object[] {} : args;
        this.interceptorInvokers = interceptorInvokers;
        this.callable = callable;
    }

    boolean hasNextInterceptor() {
        return index < interceptorInvokers.size();
    }

    protected Object invokeNextInterceptor() throws Exception {
        int oldIndex = index;
        try {
            // Note that some FaultTolerance interceptors can cause
            // some interesting behaviors if they are invoked before
            // other interceptors. The CDIInterceptorWrapperImpl
            // intentionally orders the FT interceptor last to
            // avoid these side effects.
            return interceptorInvokers.get(index++).invoke(this);
        } finally {
            index = oldIndex;
        }
    }

    protected Object interceptorChainCompleted() throws Exception {
        return callable.call();
    }

    @Override
    public Object proceed() throws Exception {
            if (hasNextInterceptor()) {
                return invokeNextInterceptor();
            } else {
                return interceptorChainCompleted();
            }
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Constructor<?> getConstructor() {
        return null;
    }

    @Override
    public Object[] getParameters() throws IllegalStateException {
        return args;
    }

    @Override
    public void setParameters(Object[] params) throws IllegalStateException, IllegalArgumentException {
        this.args = params;
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;//Collections.emptyMap();
    }

    @Override
    public Object getTimer() {
        return null;
    }
}
