/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// https://github.com/resteasy/resteasy/blob/4.7.2.Final/resteasy-client-microprofile-base/src/main/java/org/jboss/resteasy/microprofile/client/header/ClientHeaderProviders.java
// https://repo.maven.apache.org/maven2/org/jboss/resteasy/resteasy-client-microprofile-base/4.7.2.Final/resteasy-client-microprofile-base-4.7.2.Final-sources.jar
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

package org.jboss.resteasy.microprofile.client.header;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.jboss.resteasy.cdi.CdiConstructorInjector;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * A storage of {@link ClientHeaderProvider}s
 */
public class ClientHeaderProviders {
    private static final ClientHeadersFactory defaultHeadersFactory = new DefaultClientHeadersFactoryImpl();

    // Liberty change start
    private final Map<Method, ClientHeaderProvider> providersForMethod = new ConcurrentHashMap<>();
    private final Map<Class<?>, ClientHeadersFactory> headerFactoriesForClass = new ConcurrentHashMap<>();
    // Liberty change end

    private static final HeaderFillerFactory fillerFactory;

    /**
     * Get {@link ClientHeaderProvider} for a given method, if exists
     *
     * @param method a method to get the provider for
     * @return the provider responsible for setting the headers
     */
    public Optional<ClientHeaderProvider> getProvider(Method method) { // Liberty change
        return Optional.ofNullable(providersForMethod.get(method));
    }

    /**
     * Get {@link ClientHeadersFactory} for a given class, if exists
     *
     * @param aClass a class to get the ClientHeadersFactory for
     * @return the factory used to adjust the headers
     */
    public Optional<ClientHeadersFactory> getFactory(Class<?> aClass) { // Liberty change
        return Optional.ofNullable(headerFactoriesForClass.get(aClass));
    }

    /**
     * Register, in a static map, {@link ClientHeaderProvider}`s for the given class and all of its methods
     *
     * @param clientClass a class to scan for {@link org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam} and {@link RegisterClientHeaders}
     * @param clientProxy proxy of the clientClass, used to handle the default methods
     *
     * @deprecated use {@link #registerForClass(Class, Object, BeanManager)}
     */
    @Deprecated
    public void registerForClass(Class<?> clientClass, Object clientProxy) { // Liberty change
        registerForClass(clientClass, clientProxy, null);
    }

    /**
     * Register, in a static map, {@link ClientHeaderProvider}`s for the given class and all of its methods
     *
     * @param clientClass a class to scan for {@link org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam} and {@link RegisterClientHeaders}
     * @param clientProxy proxy of the clientClass, used to handle the default methods
     * @param beanManager the bean manager used to construct CDI beans
     */
    public void registerForClass(Class<?> clientClass, Object clientProxy, BeanManager beanManager) { // Liberty change
        Stream.of(clientClass.getMethods())
                .forEach(m -> registerForMethod(m, clientProxy));
        registerHeaderFactory(clientClass, beanManager);
    }

    private void registerHeaderFactory(Class<?> aClass, BeanManager beanManager) { // Liberty change
        RegisterClientHeaders annotation = aClass.getAnnotation(RegisterClientHeaders.class);
        if (annotation != null) {
            Optional<ClientHeadersFactory> clientHeadersFactory = getCustomHeadersFactory(annotation, aClass, beanManager);

            headerFactoriesForClass.put(aClass, clientHeadersFactory.orElse(defaultHeadersFactory));
        }
    }

    private Optional<ClientHeadersFactory> getCustomHeadersFactory(RegisterClientHeaders annotation, Class<?> source, BeanManager beanManager) { //Liberty change
        Class<? extends ClientHeadersFactory> factoryClass = annotation.value();
        try {
            return Optional.of(construct(factoryClass, beanManager));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RestClientDefinitionException(
                    "Failed to instantiate " + factoryClass.getCanonicalName() + ", the client header factory for " + source.getCanonicalName(),
                    e
            );
        }
    }

    private void registerForMethod(Method method, Object clientProxy) { // Liberty change
        ClientHeaderProvider.forMethod(method, clientProxy, fillerFactory).ifPresent(
                provider -> providersForMethod.put(method, provider)
        );
    }

    private ClientHeadersFactory construct(final Class<? extends ClientHeadersFactory> factory, final BeanManager manager) throws IllegalAccessException, InstantiationException { //Liberty change
        if (manager != null) {
            Set<Bean<?>> beans = manager.getBeans(factory);
            if (!beans.isEmpty()) {
                final CdiConstructorInjector injector = new CdiConstructorInjector(factory, (jakarta.enterprise.inject.spi.BeanManager) manager); // Liberty change
                // The CdiConstructorInjector does not use the unwrapAsync value using false has no effect
                return factory.cast(injector.construct(false));
            }
        }
        return factory.newInstance();
    }

    static {
        ServiceLoader<HeaderFillerFactory> fillerFactories = ServiceLoader.load(HeaderFillerFactory.class);
        int highestPrio = Integer.MIN_VALUE;
        HeaderFillerFactory result = null;
        for (HeaderFillerFactory factory : fillerFactories) {
            if (factory.getPriority() > highestPrio) {
                highestPrio = factory.getPriority();
                result = factory;
            }
        }
        if (result == null) {
            throw new java.lang.IllegalStateException("Unable to find a HeaderFillerFactory implementation");
        } else {
            fillerFactory = result;
        }
    }

    public ClientHeaderProviders() { // Liberty change
    }
}
