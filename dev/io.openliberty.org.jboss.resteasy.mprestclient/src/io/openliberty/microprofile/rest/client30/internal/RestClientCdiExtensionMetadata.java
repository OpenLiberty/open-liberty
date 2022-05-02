/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.rest.client30.internal;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.spi.Extension;

import org.jboss.resteasy.microprofile.client.RestClientExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Registers the RestEasy MP Rest Client extension
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class RestClientCdiExtensionMetadata implements CDIExtensionMetadata, CDIExtensionMetadataInternal {

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(RestClientExtension.class);
    }

    @Override
    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        Set<Class<? extends Annotation>> BDAs = new HashSet<Class<? extends Annotation>>();
        BDAs.add(RegisterRestClient.class);
        return BDAs;
    }

    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

}
