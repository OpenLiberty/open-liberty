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
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.repository.Repository;

/**
 * Makes Jakarta Data's Repository annotation into a bean defining annotation.
 */
@Component(configurationPid = "io.openliberty.data.internal.cdi.BeanDefiningAnnotationMetadata",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { CDIExtensionMetadata.class, BeanDefiningAnnotationMetadata.class },
           immediate = true)
public class BeanDefiningAnnotationMetadata implements CDIExtensionMetadata {
    private static final Set<Class<? extends Annotation>> beanDefiningAnnos = Set.of(Repository.class, StaticMetamodel.class);

    @Override
    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        return beanDefiningAnnos;
    }
}