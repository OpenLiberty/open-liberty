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

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * This builder is used when a persistence unit reference JNDI name is configured as the repository dataStore.
 */
public class PUnitEMBuilder extends EntityManagerBuilder {
    /**
     * These are present only if needed to disambiguate a persistence unit reference JNDI name
     * that is in java:app, java:module, or java:comp
     */
    private final String application, module, component;

    private final EntityManagerFactory emf;

    private final String persistenceUnitRef;

    public PUnitEMBuilder(EntityManagerFactory emf, ClassLoader repositoryClassLoader) {
        super(repositoryClassLoader);
        this.emf = emf;
        this.persistenceUnitRef = emf.toString();

        this.application = null;
        this.module = null;
        this.component = null;

        // TODO For EntityManagerFactory managed by Open Liberty, the persistence unit and app/module/component are known
        // Example of emf.toString():
        // com.ibm.ws.jpa.container.v31.JPAEMFactoryV31@ed2fe703[PuId=DataStoreTestApp#DataStoreTestWeb.war#MyPersistenceUnit,
        // DataStoreTestApp#DataStoreTestWeb.war, org.eclipse.persistence.internal.jpa.EntityManagerFactoryImpl@3708cabf]
    }

    public PUnitEMBuilder(EntityManagerFactory emf, String persistenceUnitRef, ClassLoader repositoryClassLoader) {
        super(repositoryClassLoader);
        this.emf = emf;
        this.persistenceUnitRef = persistenceUnitRef;

        boolean javaApp = persistenceUnitRef.regionMatches(5, "app", 0, 3);
        boolean javaModule = !javaApp && persistenceUnitRef.regionMatches(5, "module", 0, 6);
        boolean javaComp = !javaApp && !javaModule && persistenceUnitRef.regionMatches(5, "comp", 0, 4);

        // TODO it might not be predictable which module this thread runs from. If so, module and component cannot be used.
        if (javaApp || javaModule || javaComp) {
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            J2EEName jeeName = cData == null ? null : cData.getJ2EEName();

            application = jeeName == null ? null : jeeName.getApplication();
            module = jeeName == null || javaApp ? null : jeeName.getModule();
            component = jeeName == null || javaApp || javaModule ? null : jeeName.getComponent();
            // TODO how do we know if running in the web module so component should be omitted? Can we look for a ModuleMetadata subclass?
        } else {
            application = null;
            module = null;
            component = null;
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

    @Override
    @Trivial
    public int hashCode() {
        return persistenceUnitRef.hashCode();
    }

    @Override
    @Trivial
    protected void initialize() throws Exception {
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
