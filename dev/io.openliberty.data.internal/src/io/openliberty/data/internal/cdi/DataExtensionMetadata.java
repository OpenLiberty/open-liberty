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
package io.openliberty.data.internal.cdi;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.data.internal.DataProvider;
import jakarta.data.Entities;
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
    DataProvider noSQLDataProvider;

    @Reference(name = "PersistenceDataProvider", cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, target = "(id=unbound)")
    DataProvider persistenceDataProvider;

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
     * @return the provider
     */
    DataProvider getProvider(Class<?> entityClass) {
        DataProvider provider = persistenceDataProvider;
        for (Annotation anno : entityClass.getAnnotations()) {
            String annoClassName = anno.annotationType().getName();
            if ("jakarta.persistence.Entity".equals(annoClassName)) {
                break;
            }
            if ("jakarta.nosql.mapping.Entity".equals(annoClassName)) {
                provider = noSQLDataProvider;
                break;
            }
        }
        if (provider == null) {
            provider = noSQLDataProvider;
            if (provider == null)
                throw new IllegalStateException("Jakarta Data requires either Jakarta Persistence or Jakarta NoSQL"); // TODO
        }
        return provider;
    }
}