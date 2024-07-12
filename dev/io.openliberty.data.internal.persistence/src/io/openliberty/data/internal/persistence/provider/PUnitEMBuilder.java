/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.provider;

import java.util.Set;
import java.util.concurrent.CompletionException;

import javax.sql.DataSource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.EntityInfo;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

/**
 * This builder is used when a persistence unit reference JNDI name is configured as the repository dataStore.
 */
public class PUnitEMBuilder extends EntityManagerBuilder {

    private final EntityManagerFactory emf;

    private final String persistenceUnitRef;

    // TODO this should be removed because the spec did not add the ability to use
    // a resource access method with a qualifier to configure the EntityManager.
    public PUnitEMBuilder(DataProvider provider,
                          ClassLoader repositoryClassLoader,
                          EntityManagerFactory emf,
                          Set<Class<?>> entityTypes) {
        super(provider, repositoryClassLoader);
        this.emf = emf;
        this.persistenceUnitRef = emf.toString();

        try {
            collectEntityInfo(entityTypes);
        } catch (RuntimeException x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw x;
        } catch (Exception x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw new RuntimeException(x);
        } catch (Error x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw x;
        }
    }

    /**
     * Obtains entity manager instances from a persistence unit reference /
     * EntityManagerFactory.
     *
     * @param provider              OSGi service that provides the CDI extension.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param emf                   entity manager factory.
     * @param pesistenceUnitRef     persistence unit reference.
     * @param metaDataIdentifier    metadata identifier for the class loader of the repository interface.
     * @param entityTypes           entity classes as known by the user, not generated.
     */
    public PUnitEMBuilder(DataProvider provider,
                          ClassLoader repositoryClassLoader,
                          EntityManagerFactory emf,
                          String persistenceUnitRef,
                          String metadataIdentifier,
                          Set<Class<?>> entityTypes) {
        super(provider, repositoryClassLoader);
        this.emf = emf;
        this.persistenceUnitRef = persistenceUnitRef;

        try {
            collectEntityInfo(entityTypes);
        } catch (RuntimeException x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw x;
        } catch (Exception x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw new CompletionException(x);
        } catch (Error x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw x;
        }
    }

    @Override
    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    @FFDCIgnore(PersistenceException.class)
    @Override
    public DataSource getDataSource() {
        try {
            return emf.unwrap(DataSource.class);
        } catch (PersistenceException x) {
            try {
                EntityManager em = emf.createEntityManager();
                return em.unwrap(DataSource.class);
            } catch (PersistenceException xx) {
                throw new UnsupportedOperationException("DataSource and Connection resources are not available" +
                                                        " from the EntityManagerFactory or EntityManager of the" +
                                                        " Jakarta Persistence provider.", x); // TODO NLS
            }
        }
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(27 + persistenceUnitRef.length()) //
                        .append("PUnitEMBuilder@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append(":").append(persistenceUnitRef) //
                        .toString();
    }
}
