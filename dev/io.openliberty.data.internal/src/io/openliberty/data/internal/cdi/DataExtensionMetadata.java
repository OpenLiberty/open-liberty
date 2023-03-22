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
import java.util.Arrays;
import java.util.Collections;
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
import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.Repository;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;

@Component(configurationPid = "io.openliberty.data.internal.cdi.DataExtensionMetadata",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { CDIExtensionMetadata.class, DataExtensionMetadata.class },
           immediate = true)
public class DataExtensionMetadata implements CDIExtensionMetadata {
    private static final Set<Class<?>> beanClasses = Set.of(TemplateProducer.class);
    private static final Set<Class<? extends Annotation>> beanDefiningAnnos = Set.of(Entities.class, Repository.class);
    private static final Set<Class<? extends Extension>> extensions = Collections.singleton(DataExtension.class);

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
     * Get the provider of data repositories for the specified entity class
     * if the provider supports the type of entity annotations observed on
     * the class, or if the entity class has no entity annotations and the
     * provider supports unannotated entity classes.
     *
     * @param entityClass
     * @param repositoryType
     * @return the provider. Null to ignore this entity class type.
     */
    LibertyDataProvider getProvider(Class<?> entityClass, AnnotatedType<?> repositoryType) {
        Annotation[] entityClassAnnos = entityClass.getAnnotations();
        LibertyDataProvider provider = null;

        boolean hasEntityAnnos = false;
        for (Annotation anno : entityClassAnnos) {
            String annoClassName = anno.annotationType().getName();
            if ("jakarta.persistence.Entity".equals(annoClassName)) {
                hasEntityAnnos = true;
                provider = persistenceDataProvider;
                break;
            } else if (anno.annotationType().getSimpleName().indexOf("Entity") >= 0) {
                hasEntityAnnos = true;
            }
        }

        if (provider == null)
            if (hasEntityAnnos) {
                Repository repository = repositoryType.getAnnotation(Repository.class);
                if (!Repository.ANY_PROVIDER.equals(repository.provider()))
                    throw new MappingException("Open Liberty's built-in Jakarta Data provider cannot provide the " +
                                               repositoryType.getJavaClass().getName() + " repository because the repository's " +
                                               entityClass.getName() + " entity class includes an unrecognized entity annotation. " +
                                               " The following annotations are found on the entity class: " + Arrays.toString(entityClassAnnos) +
                                               ". Supported entity annotations are: " + "jakarta.persistence.Entity."); // TODO NLS
            } else {
                provider = persistenceDataProvider;
            }

        return provider;
    }
}