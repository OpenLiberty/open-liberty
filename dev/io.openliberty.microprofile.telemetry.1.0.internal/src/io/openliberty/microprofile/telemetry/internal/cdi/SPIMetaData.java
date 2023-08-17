/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package io.openliberty.microprofile.telemetry.internal.cdi;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.microprofile.telemetry.internal.helper.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.rest.TelemetryClientFilter;
import io.openliberty.microprofile.telemetry.internal.rest.TelemetryContainerFilter;
import io.openliberty.microprofile.telemetry.internal.rest.TelemetryServletFilter;

@Component(service = CDIExtensionMetadata.class, configurationPolicy = IGNORE)
public class SPIMetaData implements CDIExtensionMetadata, CDIExtensionMetadataInternal {

    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> beans = new HashSet<Class<?>>();
        beans.add(OpenTelemetryProducer.class);
        if (!AgentDetection.isAgentActive()) {
            beans.add(WithSpanInterceptor.class);
        }
        beans.add(TelemetryClientFilter.class);
        beans.add(TelemetryContainerFilter.class);
        beans.add(TelemetryServletFilter.class);
        return beans;
    }

    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

}