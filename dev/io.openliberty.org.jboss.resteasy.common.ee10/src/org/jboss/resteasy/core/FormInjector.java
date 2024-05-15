/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2024 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.core;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FormInjector implements ValueInjector {
//   private Class type; // Liberty Change
    private ConstructorInjector constructorInjector;
    private PropertyInjector propertyInjector;

//   @SuppressWarnings(value = "unchecked") // Liberty Change
    public FormInjector(final Class<?> type, final ResteasyProviderFactory factory) {
//      this.type = type; // Liberty Change
        Constructor<?> constructor; // Liberty Change

        try {
            constructor = type.getDeclaredConstructor();
            //Liberty change - doPriv - requires above changes for proper constructor scope
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                constructor.setAccessible(true);
                return true;
            });;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(Messages.MESSAGES.unableToInstantiateForm());
        }

        constructorInjector = factory.getInjectorFactory().createConstructor(constructor, factory);
        propertyInjector = factory.getInjectorFactory().createPropertyInjector(type, factory);

    }

    @Override
    public Object inject(boolean unwrapAsync) {
        throw new IllegalStateException(Messages.MESSAGES.cannotInjectIntoForm());
    }

    @Override
    public Object inject(HttpRequest request, HttpResponse response, boolean unwrapAsync) {
        Object obj = constructorInjector.construct(unwrapAsync);
        if (obj instanceof CompletionStage) {
            @SuppressWarnings("unchecked")
            CompletionStage<Object> stage = (CompletionStage<Object>) obj;
            return stage.thenCompose(target -> {
                CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, target, unwrapAsync);
                return propertyStage == null ? CompletableFuture.completedFuture(target)
                        : propertyStage
                                .thenApply(v -> target);
            });
        }
        CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, obj, unwrapAsync);
        return propertyStage == null ? obj : propertyStage.thenApply(v -> obj);

    }
}
