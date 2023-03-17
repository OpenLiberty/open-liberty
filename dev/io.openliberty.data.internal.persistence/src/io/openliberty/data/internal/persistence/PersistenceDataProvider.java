/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

import io.openliberty.data.internal.LibertyDataProvider;
import jakarta.data.Template;
import jakarta.persistence.Entity;

/**
 * Simulates a provider for relational databases by delegating
 * JPQL queries to the Jakarta Persistence layer.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = LibertyDataProvider.class)
public class PersistenceDataProvider implements LibertyDataProvider {

    private static final Set<Class<? extends Annotation>> ENTITY_ANNO_TYPES = Set.of(Entity.class);

    final ConcurrentHashMap<Class<?>, CompletableFuture<EntityInfo>> entityInfoMap = new ConcurrentHashMap<>();

    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected ExecutorService executor;

    @Reference
    protected LocalTransactionCurrent localTranCurrent;

    @Reference
    protected EmbeddableWebSphereTransactionManager tranMgr;

    @Override
    public void entitiesFound(String databaseId, ClassLoader loader, List<Class<?>> entities) {
        executor.submit(new EntityDefiner(this, databaseId, loader, entities));
    }

    @Override
    public <R> R getRepository(Class<R> repositoryInterface) {
        Class<?> entityClass = LibertyDataProvider.entityClass.get();

        RepositoryImpl<R, ?> handler = new RepositoryImpl<>(this, repositoryInterface, entityClass);

        return repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                               new Class<?>[] { repositoryInterface },
                                                               handler));
    }

    @Override
    public Template getTemplate() {
        return new TemplateImpl(this);
    }

    @Override
    public String name() {
        return "Open Liberty Data Provider";
    }

    @Override
    public void repositoryBeanDisposed(Object repository) {
        RepositoryImpl<?, ?> handler = (RepositoryImpl<?, ?>) Proxy.getInvocationHandler(repository);
        handler.isDisposed.set(true);
    }

    @Override
    public Set<Class<? extends Annotation>> supportedEntityAnnotations() {
        return ENTITY_ANNO_TYPES;
    }
}