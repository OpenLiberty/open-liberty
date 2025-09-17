/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.concurrent.Callable;

import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;

/**
 * Wraps CDI interceptors into calls to the {@code ProxyInvocationHandler#invoke} method.
 */
public class LibertyProxyInvocationHandler implements InvocationHandler {

    private final Map<Method, List<InterceptorInvoker>> interceptorInvokers;
    private final InvocationHandler delegateHandler;

    LibertyProxyInvocationHandler(final Class<?> restClientInterface,
                                  final Object target,
                                  final Set<Object> providerInstances,
                                  final ResteasyClient client,
                                  final BeanManager beanManager,
                                  final Map<Method, List<InterceptorInvoker>> interceptorInvokers,
                                  final ClassLoader classLoader) {
        this.interceptorInvokers = interceptorInvokers;
        this.delegateHandler = new DefaultMethodInvocationHandler(restClientInterface, target, providerInstances, client, beanManager, classLoader);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<InterceptorInvoker> invokers = interceptorInvokers.get(method);
        if (invokers == null || invokers.isEmpty()) {
            return delegateHandler.invoke(proxy, method, args);
        }
        return new MPRestClientInvocationContextImpl(proxy, method, args, invokers, new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                try {
                    return delegateHandler.invoke(proxy, method, args);
                } catch (Exception ex) {
                    throw ex;
                } catch (Throwable t) {
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    throw (RuntimeException) t;
                }
            }
        }).proceed();
    }
}
