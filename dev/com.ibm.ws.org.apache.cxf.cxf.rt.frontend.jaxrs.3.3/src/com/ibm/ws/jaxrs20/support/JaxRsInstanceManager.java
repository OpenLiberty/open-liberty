/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.ws.jaxrs20.Destroyable;

/**
 *
 */
public class JaxRsInstanceManager {

    private final ClassLoader classLoader;

    private final List<InstanceInterceptor> interceptors;

    private static final InstanceInterceptor[] EMPTY_INTERCEPTOR_ARRAY = new InstanceInterceptor[0];

    public JaxRsInstanceManager(ClassLoader classLoader, InstanceInterceptor... interceptors) {
        this.classLoader = classLoader;
        this.interceptors = new CopyOnWriteArrayList<InstanceInterceptor>(interceptors);
    }

    public Object createInstance(String className) throws InstantiationException, IllegalAccessException, InterceptException, ClassNotFoundException {
        return createInstance(classLoader.loadClass(className));
    }

    public Object createInstance(String className, InstanceInterceptor... addintionalInterceptors) throws InstantiationException, IllegalAccessException, InterceptException, ClassNotFoundException {
        return createInstance(classLoader.loadClass(className), addintionalInterceptors);
    }

    public <T> T createInstance(Class<T> cls) throws IllegalAccessException, InstantiationException, InterceptException {
        return createInstance(cls, EMPTY_INTERCEPTOR_ARRAY);
    }

    public boolean addInterceptor(InstanceInterceptor interceptor) {
        return interceptors.add(interceptor);
    }

    public boolean removeInterceptor(InstanceInterceptor interceptor) {
        return interceptors.remove(interceptor);
    }

    public InstanceInterceptor getInterceptor(String className) {
        for (InstanceInterceptor interceptor : this.interceptors) {
            if (interceptor.getClass().getName().equals(className)) {
                return interceptor;
            }
        }
        return null;
    }

    public <T> T createInstance(Class<T> cls, InstanceInterceptor... addintionalInterceptors) throws IllegalAccessException, InstantiationException, InterceptException {
        T instance = cls.newInstance();
        if (interceptors.size() == 0 && addintionalInterceptors.length == 0) {
            return instance;
        }

        InterceptorContext ctx = createInterceptorContext(instance);

        for (InstanceInterceptor interceptor : addintionalInterceptors) {
            interceptor.postNewInstance(ctx);
        }

        for (InstanceInterceptor interceptor : interceptors) {
            interceptor.postNewInstance(ctx);
        }

        for (InstanceInterceptor interceptor : addintionalInterceptors) {
            interceptor.postInjectInstance(ctx);
        }

        for (InstanceInterceptor interceptor : interceptors) {
            interceptor.postInjectInstance(ctx);
        }
        return instance;
    }

    /**
     * Using the method to construct the instance context
     * 
     * @param instance
     * @return
     */
    protected InterceptorContext createInterceptorContext(Object instance) {
        return new InterceptorContext(instance);
    }

    public void destroyInstance(Object instance) throws InterceptException {
        if (interceptors.size() > 0) {
            InterceptorContext ctx = createInterceptorContext(instance);
            for (InstanceInterceptor interceptor : interceptors) {
                interceptor.preDestroyInstance(ctx);
            }
        }

        if (instance instanceof Destroyable) {
            ((Destroyable) instance).destroy();
        }
    }

    public static class InterceptorContext {

        private final Object instance;

        public InterceptorContext(Object instance) {
            this.instance = instance;
        }

        /**
         * @return the instance
         */
        public Object getInstance() {
            return instance;
        }

    }

    /**
     * Using the interface to intercept the phase when creating the instance
     */
    public static interface InstanceInterceptor {
        /**
         * Execute the method after the clazz.newInstance()
         * 
         * @param ctx
         */
        public void postNewInstance(InterceptorContext ctx) throws InterceptException;

        /**
         * Execute the method after injecting the instance
         * 
         * @param ctx
         */
        public void postInjectInstance(InterceptorContext ctx);

        /**
         * Execute the method before destroying the instance
         * 
         * @param ctx
         */
        public void preDestroyInstance(InterceptorContext ctx) throws InterceptException;
    }

    public static class InterceptException extends Exception {

        /**
         * @param message
         * @param cause
         */
        public InterceptException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * @param message
         */
        public InterceptException(String message) {
            super(message);
        }

        /**
         * @param cause
         */
        public InterceptException(Throwable cause) {
            super(cause);
        }
    }
}
