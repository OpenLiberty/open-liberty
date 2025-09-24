/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// https://github.com/resteasy/resteasy-microprofile/blob/3.0.0.Final/rest-client-base/src/main/java/org/jboss/resteasy/microprofile/client/ProxyInvocationHandler.java
// https://repo.maven.apache.org/maven2/org/jboss/resteasy/microprofile/microprofile-rest-client-base/3.0.0.Final/microprofile-rest-client-base-3.0.0.Final-sources.jar
/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.microprofile.client;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.microprofile.client.header.ClientHeaderFillingException;
import org.jboss.resteasy.microprofile.client.header.ClientHeaderProviders;

public class ProxyInvocationHandler implements InvocationHandler {

    private static final Logger LOGGER = Logger.getLogger(ProxyInvocationHandler.class);
    public static final Type[] NO_TYPES = {};

    private final Object target;

    private final Set<Object> providerInstances;

    private final ResteasyClient client;

    private final AtomicBoolean closed;

    public ProxyInvocationHandler(final Class<?> restClientInterface,
            final Object target,
            final Set<Object> providerInstances,
            final ResteasyClient client) {
        this.target = target;
        this.providerInstances = providerInstances;
        this.client = client;
        this.closed = new AtomicBoolean();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (RestClientProxy.class.equals(method.getDeclaringClass())) {
            return invokeRestClientProxyMethod(method);
        }
        // Autocloseable/Closeable
        if (method.getName().equals("close") && (args == null || args.length == 0)) {
            close();
            return null;
        }
        // Check if this proxy is closed or the client itself is closed. The client may be closed if this proxy was a
        // sub-resource and the resource client itself was closed.
        if (closed.get() || client.isClosed()) {
            closed.set(true);
            throw new IllegalStateException("RestClientProxy is closed");
        }

        boolean replacementNeeded = false;
        Object[] argsReplacement = args != null ? new Object[args.length] : null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        if (args != null) {
            for (Object p : providerInstances) {
                if (p instanceof ParamConverterProvider) {

                    int index = 0;
                    for (Object arg : args) {
                        // ParamConverter's are not allowed to be passed null values. If we have a null value do not process
                        // it through the provider.
                        if (arg == null) {
                            continue;
                        }

                        if (parameterAnnotations[index].length > 0) { // does a parameter converter apply?
                            ParamConverter<?> converter = ((ParamConverterProvider) p).getConverter(arg.getClass(), null,
                                    parameterAnnotations[index]);
                            if (converter != null) {
                                Type[] genericTypes = getGenericTypes(converter.getClass());
                                if (genericTypes.length == 1) {

                                    // minimum supported types
                                    switch (genericTypes[0].getTypeName()) {
                                        case "java.lang.String":
                                            @SuppressWarnings("unchecked")
                                            ParamConverter<String> stringConverter = (ParamConverter<String>) converter;
                                            argsReplacement[index] = stringConverter.toString((String) arg);
                                            replacementNeeded = true;
                                            break;
                                        case "java.lang.Integer":
                                            @SuppressWarnings("unchecked")
                                            ParamConverter<Integer> intConverter = (ParamConverter<Integer>) converter;
                                            argsReplacement[index] = intConverter.toString((Integer) arg);
                                            replacementNeeded = true;
                                            break;
                                        case "java.lang.Boolean":
                                            @SuppressWarnings("unchecked")
                                            ParamConverter<Boolean> boolConverter = (ParamConverter<Boolean>) converter;
                                            argsReplacement[index] = boolConverter.toString((Boolean) arg);
                                            replacementNeeded = true;
                                            break;
                                        default:
                                            continue;
                                    }
                                }
                            }
                        } else {
                            argsReplacement[index] = arg;
                        }
                        index++;
                    }
                }
            }
        }

        if (replacementNeeded) {
            args = argsReplacement;
        }

        try {
            final Object result = method.invoke(target, args);
            final Class<?> returnType = method.getReturnType();
            // Check if this is a sub-resource. A sub-resource must be an interface.
            if (returnType.isInterface()) {
                final Annotation[] annotations = method.getDeclaredAnnotations();
                boolean hasPath = false;
                boolean hasHttpMethod = false;
                // Check the annotations. If the method has one of the @HttpMethod annotations, we will just use the
                // current method. If it only has a @Path, then we need to create a proxy for the return type.
                for (Annotation annotation : annotations) {
                    final Class<?> type = annotation.annotationType();
                    if (type.equals(Path.class)) {
                        hasPath = true;
                    } else if (type.getDeclaredAnnotation(HttpMethod.class) != null) {
                        hasHttpMethod = true;
                    }
                }
                if (!hasHttpMethod && hasPath) {
                    // Create a proxy of the return type re-using the providers and client, but do not add the required
                    // interfaces for the sub-resource.
                    return createProxy(returnType, result, false, providerInstances, client, getBeanManager());
                }
            }
            return result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CompletionException) {
                cause = cause.getCause();
            }
            if (cause instanceof ExceptionMapping.HandlerException) {
                ((ExceptionMapping.HandlerException) cause).mapException(method);
                // no applicable exception mapper found or applicable mapper returned null
                return null;
            }
            if (cause instanceof ResponseProcessingException) {
                ResponseProcessingException rpe = (ResponseProcessingException) cause;
                cause = rpe.getCause();
                if (cause instanceof RuntimeException) {
                    throw cause;
                }
            } else {
                if (cause instanceof ProcessingException &&
                        cause.getCause() instanceof ClientHeaderFillingException) {
                    throw cause.getCause().getCause();
                }
                if (cause instanceof RuntimeException) {
                    throw cause;
                }
            }
            throw e;
        }
    }

    /**
     * Creates a proxy for the interface. The proxy will implement the interfaces {@link RestClientProxy} and
     * {@link Closeable}.
     *
     * @param resourceInterface the resource interface to create the proxy for
     * @param target            the target object for the proxy
     * @param providers         the providers for the client
     * @param client            the client to use
     * @param beanManager       the bean manager used to register {@linkplain ClientHeaderProviders client header providers}
     * @return the new proxy
     */
    static Object createProxy(final Class<?> resourceInterface, final Object target, final Set<Object> providers,
            final ResteasyClient client, final BeanManager beanManager) {
        return createProxy(resourceInterface, target, true, providers, client, beanManager);
    }

    /**
     * Creates a proxy for the interface.
     * <p>
     * If {@code addExtendedInterfaces} is set to {@code true}, the proxy will implement the interfaces
     * {@link RestClientProxy} and {@link Closeable}.
     * </p>
     *
     * @param resourceInterface     the resource interface to create the proxy for
     * @param target                the target object for the proxy
     * @param addExtendedInterfaces {@code true} if the proxy should also implement {@link RestClientProxy} and
     *                              {@link Closeable}
     * @param providers             the providers for the client
     * @param client                the client to use
     * @param beanManager           the bean manager used to register {@linkplain ClientHeaderProviders client header providers}
     * @return the new proxy
     */
    static Object createProxy(final Class<?> resourceInterface, final Object target, final boolean addExtendedInterfaces,
            final Set<Object> providers, final ResteasyClient client, final BeanManager beanManager) {
        final Class<?>[] interfaces;
        if (addExtendedInterfaces) {
            interfaces = new Class<?>[3];
            interfaces[1] = RestClientProxy.class;
            interfaces[2] = Closeable.class;
        } else {
            interfaces = new Class[1];
        }
        interfaces[0] = resourceInterface;
        final Object proxy = Proxy.newProxyInstance(getClassLoader(resourceInterface), interfaces,
                new ProxyInvocationHandler(resourceInterface, target, Set.copyOf(providers), client));
        // ClientHeaderProviders.registerForClass(resourceInterface, proxy, beanManager); // Liberty change since done in LibertyRestClientBuilderImpl
        return proxy;
    }

    private Object invokeRestClientProxyMethod(final Method method) {
        switch (method.getName()) {
            case "getClient":
                return client;
            case "close":
                close();
                return null;
            default:
                throw new IllegalStateException("Unsupported RestClientProxy method: " + method);
        }
    }

    private void close() {
        if (closed.compareAndSet(false, true)) {
            client.close();
        }
    }

    private Type[] getGenericTypes(Class<?> aClass) {
        Type[] genericInterfaces = aClass.getGenericInterfaces();
        Type[] genericTypes = NO_TYPES;
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
            }
        }
        return genericTypes;
    }

    private static ClassLoader getClassLoader(final Class<?> type) {
        if (System.getSecurityManager() == null) {
            return type.getClassLoader();
        }
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) type::getClassLoader);
    }

    private static BeanManager getBeanManager() {
        try {
            CDI<Object> current = CDI.current();
            return current != null ? current.getBeanManager() : null;
        } catch (IllegalStateException e) {
            LOGGER.debug("CDI container is not available", e);
            return null;
        }
    }
}
