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

import jakarta.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.data.Data;
import io.openliberty.data.Entities;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = CDIExtensionMetadata.class) // TODO property to identify class?
public class DataExtensionMetadata implements CDIExtensionMetadata {

    @Override
    public Set<Class<?>> getBeanClasses() {
        return Set.of(TemplateProducer.class);
    }

    @Override
    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        return Set.of(Data.class, Entities.class);
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(DataExtension.class);
    }
}