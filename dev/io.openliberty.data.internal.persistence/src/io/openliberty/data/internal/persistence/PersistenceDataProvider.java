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
package io.openliberty.data.internal.persistence;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

import io.openliberty.data.internal.DataProvider;

/**
 * Simulates a provider for relational databases by delegating
 * JPQL queries to the Jakarta Persistence layer.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataProvider.class)
public class PersistenceDataProvider implements DataProvider {

    final ConcurrentHashMap<Class<?>, CompletableFuture<EntityInfo>> entityInfoMap = new ConcurrentHashMap<>();

    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected ExecutorService executor;

    @Reference
    protected LocalTransactionCurrent localTranCurrent;

    @Reference
    protected EmbeddableWebSphereTransactionManager tranMgr;

    @Override
    public <R> R createRepository(Class<R> repositoryInterface, Class<?> entityClass) {
        RepositoryImpl<R, ?> handler = new RepositoryImpl<>(this, repositoryInterface, entityClass);

        return repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                               new Class<?>[] { repositoryInterface },
                                                               handler));
    }

    @Override
    public void entitiesFound(String databaseId, ClassLoader loader, List<Class<?>> entities) {
        executor.submit(new EntityDefiner(this, databaseId, loader, entities));
    }

    @Trivial
    CompletableFuture<EntityInfo> futureEntityInfo(Class<?> entityClass) {
        CompletableFuture<EntityInfo> entityInfoFuture = new CompletableFuture<>();
        CompletableFuture<EntityInfo> alreadyPresent = entityInfoMap.putIfAbsent(entityClass, entityInfoFuture);
        return alreadyPresent == null ? entityInfoFuture : alreadyPresent;
    }
}