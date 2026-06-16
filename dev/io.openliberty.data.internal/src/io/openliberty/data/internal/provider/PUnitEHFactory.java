/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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
package io.openliberty.data.internal.provider;

import static io.openliberty.data.internal.cdi.DataExtension.exc;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.sql.DataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.DataProvider;
import io.openliberty.data.internal.EntityHandlerFactory;
import jakarta.data.exceptions.DataException;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

/**
 * This factory is used when the repository dataStore is configure to be a
 * persistence unit name (Data 1.1+) or
 * persistence unit reference JNDI name.
 */
public class PUnitEHFactory extends EntityHandlerFactory {
    private static final TraceComponent tc = Tr.register(PUnitEHFactory.class);

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
     * @param entityTypes           entity classes as known by the user, not generated.
     * @throws Exception if an error occurs.
     */
    public PUnitEHFactory(DataProvider provider,
                          ClassLoader repositoryClassLoader,
                          Set<Class<?>> repositoryInterfaces,
                          EntityManagerFactory emf,
                          String persistenceUnitRef,
                          Set<Class<?>> entityTypes) throws Exception {
        super(provider, //
              repositoryClassLoader, //
              repositoryInterfaces, //
              persistenceUnitRef);
        this.emf = emf;

        collectEntityInfo(entityTypes, null);
    }

    @Override
    @Trivial
    public AutoCloseable createEntityAgent() {
        AutoCloseable agent;
        // TODO Persistence 4.0 API
        // agent = emf.getEntityAgent();

        try {
            // Because persistence-4.0 is unavailable and persistence-3.2 must
            // be used in the meantime, emf is a
            // com.ibm.ws.jpa.container.v32.JPAEMFactoryV32, which does not
            // have the createEntityAgent method.
            // A workaround is to unwrap it.
            EntityManagerFactory factory = //
                            emf.unwrap(EntityManagerFactory.class);

            agent = (AutoCloseable) factory.getClass() //
                            .getMethod("openStatelessSession") // TODO "createEntityAgent") //
                            .invoke(factory);
        } catch (IllegalAccessException | NoSuchMethodException x) {
            throw new RuntimeException(x); // should be impossible
        } catch (InvocationTargetException x) {
            throw new DataException(x.getCause());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "createEntityAgent: " + agent);
        return agent;
    }

    @Override
    @Trivial
    public EntityManager createEntityManager() {
        EntityManager em = emf.createEntityManager();
        em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "createEntityManager: " + em);
        return em;
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

    /**
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     */
    @Override
    @Trivial
    public void introspect(PrintWriter writer, String indent) {
        super.introspect(writer, indent);
        writer.println(indent + "  EntityManagerFactory: " + emf);
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(27 + dataStore.length()) //
                        .append(getClass().getSimpleName()).append('@') //
                        .append(Integer.toHexString(hashCode())) //
                        .append(":").append(dataStore) //
                        .toString();
    }
}
