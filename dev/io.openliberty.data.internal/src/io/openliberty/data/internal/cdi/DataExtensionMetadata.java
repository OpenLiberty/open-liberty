/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.data.internal.cdi;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.data.internal.LibertyDataProvider;
import jakarta.data.Entities;
import jakarta.data.provider.DataProvider;
import jakarta.data.repository.Repository;
import jakarta.enterprise.inject.spi.Extension;

@Component(configurationPid = "io.openliberty.data.internal.cdi.DataExtensionMetadata",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { CDIExtensionMetadata.class, DataExtensionMetadata.class },
           immediate = true)
public class DataExtensionMetadata implements CDIExtensionMetadata {
    private static final Set<Class<?>> beanClasses = Set.of(TemplateProducer.class);
    private static final Set<Class<? extends Annotation>> beanDefiningAnnos = Set.of(Entities.class, Repository.class);
    private static final Set<Class<? extends Extension>> extensions = Collections.singleton(DataExtension.class);

    @Reference(name = "NoSQLDataProvider", cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, target = "(id=unbound)")
    LibertyDataProvider noSQLDataProvider;

    @Reference(name = "PersistenceDataProvider", cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, target = "(id=unbound)")
    LibertyDataProvider persistenceDataProvider;

    @Override
    //@Trivial
    public Set<Class<?>> getBeanClasses() {
        return beanClasses;
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        return beanDefiningAnnos;
    }

    @Override
    @Trivial
    public Set<Class<? extends Extension>> getExtensions() {
        return extensions;
    }

    /**
     * Get the provider of data repositories for the specified entity class.
     *
     * @param entityClass
     * @param classLoader
     * @return the provider
     */
    DataProvider getProvider(Class<?> entityClass, ClassLoader classLoader) {
        Annotation[] entityClassAnnos = entityClass.getAnnotations();
        DataProvider dataProvider = null;

        for (DataProvider provider : ServiceLoader.load(DataProvider.class, classLoader)) {
            Set<Class<? extends Annotation>> supportedEntityAnnos = provider.supportedEntityAnnotations();
            for (Annotation anno : entityClassAnnos) {
                if (supportedEntityAnnos.contains(anno.annotationType())) {
                    dataProvider = provider;
                    break;
                }
            }
            if (dataProvider != null)
                break;
        }

        if (dataProvider == null) {
            for (Annotation anno : entityClassAnnos) {
                String annoClassName = anno.annotationType().getName();
                if (persistenceDataProvider != null && "jakarta.persistence.Entity".equals(annoClassName)) {
                    dataProvider = persistenceDataProvider;
                    break;
                }
                // TODO noSQLDataProvider should be removed in favor of general use of ServiceLoader once Jakarta NoSQL supplies a DataProvider
                if (noSQLDataProvider != null && "jakarta.nosql.mapping.Entity".equals(annoClassName)) {
                    dataProvider = noSQLDataProvider;
                    break;
                }
            }

            if (dataProvider == null) {
                dataProvider = persistenceDataProvider == null ? noSQLDataProvider : persistenceDataProvider;
                if (dataProvider == null)
                    throw new IllegalStateException("Jakarta Data requires either Jakarta Persistence or Jakarta NoSQL"); // TODO NLS
            }
        }

        return dataProvider;
    }
}