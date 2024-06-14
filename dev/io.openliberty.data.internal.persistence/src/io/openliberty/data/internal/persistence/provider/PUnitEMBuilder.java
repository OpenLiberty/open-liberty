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

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;

import javax.sql.DataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.EntityInfo;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.cdi.DataExtensionProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

/**
 * This builder is used when a persistence unit reference JNDI name is configured as the repository dataStore.
 */
public class PUnitEMBuilder extends EntityManagerBuilder {
    private static final TraceComponent tc = Tr.register(PUnitEMBuilder.class);

    /**
     * These are present only if needed to disambiguate a persistence unit reference JNDI name
     * that is in java:app, java:module, or java:comp
     */
    private final String application, module, component;

    private final EntityManagerFactory emf;

    private final String persistenceUnitRef;

    // TODO this should be removed because the spec did not add the ability to use
    // a resource access method with a qualifier to configure the EntityManager.
    public PUnitEMBuilder(DataExtensionProvider provider,
                          ClassLoader repositoryClassLoader,
                          EntityManagerFactory emf,
                          Set<Class<?>> entityTypes) {
        super(provider, repositoryClassLoader);
        this.emf = emf;
        this.persistenceUnitRef = emf.toString();

        this.application = null;
        this.module = null;
        this.component = null;

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
     * @param application           application in which the repository interface is defined.
     * @param module                module in which the repository interface is defined. Null if not in a module.
     * @param component             component in which the repository interface is defined. Null if not in a component or in web module.
     * @param entityTypes           entity classes as known by the user, not generated.
     */
    public PUnitEMBuilder(DataExtensionProvider provider,
                          ClassLoader repositoryClassLoader,
                          EntityManagerFactory emf,
                          String persistenceUnitRef,
                          String metadataIdentifier,
                          String application,
                          String module,
                          String component,
                          Set<Class<?>> entityTypes) {
        super(provider, repositoryClassLoader);
        this.emf = emf;
        this.persistenceUnitRef = persistenceUnitRef;
        this.application = application;
        this.module = module;
        this.component = component;

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

    @Override
    @Trivial
    public boolean equals(Object o) {
        PUnitEMBuilder b;
        return this == o || o instanceof PUnitEMBuilder
                            && persistenceUnitRef.equals((b = (PUnitEMBuilder) o).persistenceUnitRef)
                            && Objects.equals(application, b.application)
                            && Objects.equals(module, b.module)
                            && Objects.equals(component, b.component)
                            && Objects.equals(getRepositoryClassLoader(), b.getRepositoryClassLoader());
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
    public int hashCode() {
        return persistenceUnitRef.hashCode() +
               (application == null ? 0 : application.hashCode()) +
               (module == null ? 0 : module.hashCode()) +
               (component == null ? 0 : component.hashCode());

    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(27 + persistenceUnitRef.length() +
                                 (application == null ? 4 : application.length()) +
                                 (module == null ? 4 : module.length()) +
                                 (component == null ? 4 : component.length())) //
                                                 .append("PUnitEMBuilder@") //
                                                 .append(Integer.toHexString(hashCode())) //
                                                 .append(":").append(persistenceUnitRef) //
                                                 .append(' ').append(application) //
                                                 .append('#').append(module) //
                                                 .append('#').append(component) //
                                                 .toString();
    }
}
