/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.cdi.extensions;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import jakarta.enterprise.inject.spi.Extension;

@Component(service = CDIExtensionMetadata.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class JakartaSecurity40CDIExtensionMetadata implements CDIExtensionMetadata, CDIExtensionMetadataInternal {

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(JakartaSecurity40CDIExtension.class);
        return extensions;
    }

    @Override
    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        Set<Class<? extends Annotation>> BDAs = new HashSet<Class<? extends Annotation>>();
        return BDAs;
    }

    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

}
