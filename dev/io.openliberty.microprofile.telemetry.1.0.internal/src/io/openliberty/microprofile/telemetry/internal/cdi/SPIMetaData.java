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

package io.openliberty.microprofile.telemetry.internal.cdi;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.telemetry.internal.cdi.WithSpanInterceptor;
import io.openliberty.microprofile.telemetry.internal.cdi.OpenTelemetryProducer;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;

@Component(service = CDIExtensionMetadata.class, configurationPolicy = IGNORE)
public class SPIMetaData implements CDIExtensionMetadata, CDIExtensionMetadataInternal { 

    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> beans = new HashSet<Class<?>>();
        beans.add(OpenTelemetryProducer.class);
        beans.add(WithSpanInterceptor.class);
        return beans;
    }

    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

}