/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jboss.resteasy.microprofile.client.ot;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import io.openliberty.checkpoint.spi.CheckpointHook;

/**
 * A handler only used if we are doing a checkpoint
 */
public class LibertyProxyInvocationHandlerCheckpoint implements InvocationHandler, CheckpointHook{
    final AtomicReference<LibertyProxyInvocationHandler> delegate = new AtomicReference<>();

    final LibertyRestClientBuilderImpl libertyBuilder;
    final ResteasyClientBuilder resteasyClientBuilder;
    final Class<?> aClass;
    final ClassLoader classLoader;
    final BeanManager beanManager;
    final Map<Method, List<InterceptorInvoker>> interceptorInvokers;
    LibertyProxyInvocationHandlerCheckpoint(final LibertyRestClientBuilderImpl libertyBuilder, final ResteasyClientBuilder resteasyClientBuilder, final Class<?> aClass, final ClassLoader classLoader, final BeanManager beanManager, final Map<Method, List<InterceptorInvoker>> interceptorInvokers) {
        this.libertyBuilder = libertyBuilder;
        this.resteasyClientBuilder = resteasyClientBuilder;
        this.aClass = aClass;
        this.classLoader = classLoader;
        this.beanManager = beanManager;
        this.interceptorInvokers = interceptorInvokers;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return delegate.updateAndGet(new UnaryOperator<LibertyProxyInvocationHandler>() {
            @Override
            public LibertyProxyInvocationHandler apply(LibertyProxyInvocationHandler t) {
                if (t != null) {
                    return t;
                }
                return libertyBuilder.createLibertyProxyInvocationHandler(resteasyClientBuilder, aClass, classLoader, beanManager, interceptorInvokers);
            }
        }).invoke(proxy, method, args);
    }

}
