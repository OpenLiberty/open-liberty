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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.telemetry.internal.common.info.ErrorOpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.openliberty.microprofile.telemetry.internal.common.helpers.OSGIHelpers;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class OpenTelemetryProducer {

    private static final TraceComponent tc = Tr.register(OpenTelemetryProducer.class);

    private ApplicationMetaData metaData = null;

    @PostConstruct
    private void init() {
        metaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData().getApplicationMetaData();
    }

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    /**
     * Gets or creates the instance of OpenTelemetry associated with this application and returns it wrapped inside an instance of OpenTelemetryInfo.
     *
     * @return An instance of OpenTelemetryInfo containing the instance of OpenTelemetry associated with this application. This instance will be a no-op OpenTelemetry if telemetry
     *         is disabled or the application has shut down.
     */
    @ApplicationScoped
    @Produces
    public OpenTelemetryInfo getOpenTelemetryInfo() {
        try {
            OpenTelemetryInfoFactory factory = OSGIHelpers.getService(OpenTelemetryInfoFactory.class, OpenTelemetryProducer.class);
            return factory.getOpenTelemetryInfo(metaData);
        } catch (Exception e) {
            return new ErrorOpenTelemetryInfo();
        }
    }

    /**
     * Gets or creates a tracer instance from the TracerProvider for the OpenTelemetry instance associated with this application.
     *
     * @return An tracer instance from the instance of OpenTelemetry associated with this application. This instance will be a no-op if telemetry is disabled or the application has
     *         shut down.
     */
    @Produces
    public Tracer getTracer() {
        return getOpenTelemetryInfo().getTracer();
    }

    /**
     * Acquires a proxy for Spans
     *
     * @return Returns a SpanProxy that will redirect all methods to the Span associated with the current context
     */
    @Produces
    @ApplicationScoped
    public Span getSpan() {
        return new SpanProxy();
    }

    /**
     * Acquires a proxy for Baggage
     *
     * @return Returns a BaggageProxy that will redirect all methods to the Baggage associated with the current context
     */
    @Produces
    @ApplicationScoped
    public Baggage getBaggage() {
        return new BaggageProxy();
    }

    /**
     * Gets or creates the instance of OpenTelemetry associated with this application.
     *
     * @return An instance of OpenTelemetryInfo containing the instance of OpenTelemetry associated with this application. This instance will be a no-op OpenTelemetry if telemetry
     *         is disabled or the application has shut down.
     */
    @ApplicationScoped
    @Produces
    public OpenTelemetry getOpenTelemetry(OpenTelemetryInfo openTelemetryInfo) {
        return openTelemetryInfo.getOpenTelemetry();
    }
}
