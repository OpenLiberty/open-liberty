/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.lra.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRACDIExtension;
import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantResource;
import io.narayana.lra.filter.ServerLRAFilter;
import io.openliberty.cdi.spi.CDIExtensionMetadata;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class CdiExtensionComponent implements CDIExtensionMetadata {

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(LRACDIExtension.class);
    }

    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> beans = new HashSet<>();
        beans.add(ServerLRAFilter.class);
        beans.add(LRAParticipantResource.class);
        return beans;
    }
}
