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

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.lang.reflect.Method;
import java.util.Set;

import javax.sql.DataSource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

/**
 * This builder is used when a persistence unit reference JNDI name is configured as the repository dataStore.
 */
public class PUnitEMBuilder extends EntityManagerBuilder {

    private final EntityManagerFactory emf;

    /**
     * Obtains entity manager instances from a persistence unit reference /
     * EntityManagerFactory.
     *
     * @param provider              OSGi service that provides the CDI extension.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param repositoryInterfaces  repository interfaces that use the entities.
     * @param emf                   entity manager factory.
     * @param pesistenceUnitRef     persistence unit reference.
     * @param metaDataIdentifier    metadata identifier for the class loader of the repository interface.
     * @param entityTypes           entity classes as known by the user, not generated.
     * @throws Exception if an error occurs.
     */
    public PUnitEMBuilder(DataProvider provider,
                          ClassLoader repositoryClassLoader,
                          Set<Class<?>> repositoryInterfaces,
                          EntityManagerFactory emf,
                          String persistenceUnitRef,
                          String metadataIdentifier,
                          Set<Class<?>> entityTypes) throws Exception {
        super(provider, //
              repositoryClassLoader, //
              repositoryInterfaces, //
              persistenceUnitRef);
        this.emf = emf;

        collectEntityInfo(entityTypes);
    }

    @Override
    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    @FFDCIgnore(PersistenceException.class)
    @Override
    public DataSource getDataSource(Method repoMethod, Class<?> repoInterface) {
        try {
            return emf.unwrap(DataSource.class);
        } catch (PersistenceException x) {
            try {
                EntityManager em = emf.createEntityManager();
                return em.unwrap(DataSource.class);
            } catch (PersistenceException xx) {
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1063.unsupported.resource",
                          repoMethod.getName(),
                          repoInterface.getName(),
                          repoMethod.getReturnType().getName(),
                          DataSource.class.getName());
            }
        }
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(27 + dataStore.length()) //
                        .append("PUnitEMBuilder@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append(":").append(dataStore) //
                        .toString();
    }
}
