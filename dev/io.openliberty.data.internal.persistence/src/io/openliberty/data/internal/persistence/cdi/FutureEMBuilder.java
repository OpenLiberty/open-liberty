/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.cdi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.provider.PUnitEMBuilder;
import io.openliberty.data.internal.persistence.service.DBStoreEMBuilder;
import jakarta.persistence.EntityManagerFactory;

/**
 * A completable future for an EntityManagerBuilder that can be
 * completed by invoking the createEMBuilder method.
 */
public class FutureEMBuilder extends CompletableFuture<EntityManagerBuilder> {
    private static final TraceComponent tc = Tr.register(FutureEMBuilder.class);

    /**
     * These are present only if needed to disambiguate a JNDI name
     * that is in java:app, java:module, or java:comp
     */
    private final String application, module, component;

    /**
     * The configured dataStore value of the Repository annotation.
     */
    private final String dataStore;

    /**
     * Entity classes as seen by the user, not generated entity classes for records.
     */
    final Set<Class<?>> entityTypes = new HashSet<>();

    /**
     * Metadata identifier for the class loader of the repository interface.
     */
    private final String metadataIdentifier;

    /**
     * OSGi service component that provides the CDI extension for Data.
     */
    private final DataExtensionProvider provider;

    /**
     * The class loader for repository classes.
     */
    private final ClassLoader repositoryClassLoader;

    /**
     * Obtains entity manager instances from a persistence unit reference /
     * EntityManagerFactory.
     *
     * @param provider              OSGi service that provides the CDI extension.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param emf                   entity manager factory.
     * @param dataStore             configured dataStore value of the Repository annotation.
     * @param metaDataIdentifier    metadata identifier for the class loader of the repository interface.
     * @param appModComp            application/module/component in which the repository interface is defined.
     *                                  Module and component might be null or absent.
     */
    FutureEMBuilder(DataExtensionProvider provider,
                    ClassLoader repositoryClassLoader,
                    String dataStore,
                    String metadataIdentifier,
                    String[] appModComp) {
        this.provider = provider;
        this.repositoryClassLoader = repositoryClassLoader;
        this.dataStore = dataStore;
        this.metadataIdentifier = metadataIdentifier;

        boolean javaApp = dataStore.regionMatches(5, "app", 0, 3);
        boolean javaModule = !javaApp && dataStore.regionMatches(5, "module", 0, 6);
        boolean javaComp = !javaApp && !javaModule && dataStore.regionMatches(5, "comp", 0, 4);

        application = appModComp.length > 0 && (javaApp || javaModule || javaComp) ? appModComp[0] : null;
        module = appModComp.length > 1 && (javaModule || javaComp) ? appModComp[1] : null;
        component = appModComp.length > 2 && javaComp ? appModComp[2] : null;
    }

    /**
     * Adds an entity class to be handled.
     * This is the entity class as the user sees it,
     * not a generated entity class for record entities.
     *
     * @param entityClass entity class.
     */
    @Trivial
    void add(Class<?> entityClass) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "add: " + entityClass.getName());

        entityTypes.add(entityClass);
    }

    /**
     * Invoked to complete this future with the resulting EntityManagerBuilder value.
     *
     * @return PUnitEMBuilder (for persistence unit references) or
     *         DBStoreEMBuilder (data sources, databaseStore)
     * @throws Exception if an error occurs.
     */
    @FFDCIgnore(NamingException.class)
    EntityManagerBuilder createEMBuilder() {
        String resourceName = dataStore;
        boolean isJNDIName = resourceName.startsWith("java:");

        ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        ComponentMetaData extMetadata = accessor.getComponentMetaData();
        ComponentMetaData repoMetadata = (ComponentMetaData) provider.metadataIdSvc.getMetaData(metadataIdentifier);
        boolean switchMetadata = repoMetadata != null &&
                                 (extMetadata == null || !extMetadata.getJ2EEName().equals(repoMetadata.getJ2EEName()));
        if (switchMetadata)
            accessor.beginContext(repoMetadata);
        try {
            if (isJNDIName) {
                try {
                    Object resource = InitialContext.doLookup(dataStore);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, dataStore + " is the JNDI name for " + resource);

                    if (resource instanceof EntityManagerFactory)
                        return new PUnitEMBuilder(provider, repositoryClassLoader, (EntityManagerFactory) resource, //
                                        resourceName, metadataIdentifier, application, module, component, entityTypes);

                } catch (NamingException x) {
                }
            } else if (!DataExtension.DEFAULT_DATA_STORE.equals(resourceName)) {
                // Check for resource references and persistence unit references where java:comp/env/ is omitted:
                String javaCompName = "java:comp/env/" + resourceName;
                try {
                    Object resource = InitialContext.doLookup(javaCompName);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, javaCompName + " is the JNDI name for " + resource);

                    if (resource instanceof EntityManagerFactory)
                        return new PUnitEMBuilder(provider, repositoryClassLoader, (EntityManagerFactory) resource, //
                                        javaCompName, metadataIdentifier, application, module, component, entityTypes);

                    if (resource instanceof DataSource) {
                        isJNDIName = true;
                        resourceName = javaCompName;
                    }
                } catch (NamingException x) {
                }
            }
        } finally {
            if (switchMetadata)
                accessor.endContext();
        }

        ComponentMetaData mdata = (ComponentMetaData) provider.metadataIdSvc.getMetaData(metadataIdentifier);
        J2EEName jeeName = mdata == null ? null : mdata.getJ2EEName();
        return new DBStoreEMBuilder(provider, repositoryClassLoader, //
                        resourceName, isJNDIName, metadataIdentifier, //
                        new String[] { jeeName == null ? null : jeeName.getApplication(),
                                       jeeName == null ? null : jeeName.getModule(),
                                       jeeName == null ? null : jeeName.getComponent() }, // TODO clean this up and avoid duplication
                        entityTypes);
    }

    @Override
    @Trivial
    public boolean equals(Object o) {
        FutureEMBuilder b;
        return this == o || o instanceof FutureEMBuilder
                            && dataStore.equals((b = (FutureEMBuilder) o).dataStore)
                            && Objects.equals(application, b.application)
                            && Objects.equals(module, b.module)
                            && Objects.equals(component, b.component)
                            && Objects.equals(repositoryClassLoader, b.repositoryClassLoader);
    }

    @Override
    @Trivial
    public int hashCode() {
        return dataStore.hashCode() +
               (application == null ? 0 : application.hashCode()) +
               (module == null ? 0 : module.hashCode()) +
               (component == null ? 0 : component.hashCode());
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder b = new StringBuilder(27 + dataStore.length() +
                                            (application == null ? 4 : application.length()) +
                                            (module == null ? 4 : module.length()) +
                                            (component == null ? 4 : component.length())) //
                                                            .append("FutureEMBuilder@") //
                                                            .append(Integer.toHexString(hashCode())) //
                                                            .append(":").append(dataStore) //
                                                            .append(' ').append(application) //
                                                            .append('#').append(module) //
                                                            .append('#').append(component);
        return b.toString();
    }
}
