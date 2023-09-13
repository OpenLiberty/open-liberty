/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.common.cdi;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.telemetry.internal.common.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.OpenTelemetryInfoFactory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class OpenTelemetryProducer {

    private static final TraceComponent tc = Tr.register(OpenTelemetryProducer.class);

    J2EEName j2EEName = null;

    private J2EEName getJ2EEName() {
        if (j2EEName == null) {
            j2EEName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName();
        }
        return j2EEName;
    }

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    @ApplicationScoped
    @Produces
    public OpenTelemetryInfo getOpenTelemetryInfo() {
        return OpenTelemetryInfoFactory.getOpenTelemetryInfo(getJ2EEName());
    }

    @Produces
    public Tracer getTracer() {
        return OpenTelemetryInfoFactory.getTracer(getJ2EEName());
    }

    @Produces
    @ApplicationScoped
    public Span getSpan() {
        return new SpanProxy();
    }

    @Produces
    @ApplicationScoped
    public Baggage getBaggage() {
        return new BaggageProxy();
    }

    @ApplicationScoped
    @Produces
    public OpenTelemetry getOpenTelemetry(OpenTelemetryInfo openTelemetryInfo) {
        return openTelemetryInfo.getOpenTelemetry();
    }
}
