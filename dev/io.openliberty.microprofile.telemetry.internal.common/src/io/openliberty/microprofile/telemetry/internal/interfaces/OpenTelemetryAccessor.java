/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.interfaces;

import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;

import io.openliberty.microprofile.telemetry.internal.common.helpers.OSGIHelpers;
import io.openliberty.microprofile.telemetry.internal.common.info.ErrorOpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfo;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@Component(immediate = true)
public class OpenTelemetryAccessor {

    private static final TraceComponent tc = Tr.register(OpenTelemetryAccessor.class);

    private static volatile Optional<OpenTelemetryAccessor> instance = Optional.empty();

    @Reference
    private CDIService cdiService;

    @Activate
    protected void activate() {
        instance = Optional.of(this);
    }

    @Deactivate
    protected void deactivate() {
        if (instance.isPresent() && instance.get() == this) {
            instance = Optional.empty();
        }
    }

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    /**
     * Gets or creates the instance of OpenTelemetry associated with this application and returns it wrapped inside an OpenTelemetryInfo.
     *
     * @return An instance of OpenTelemetryInfo containing the instance of OpenTelemetry associated with this application. This instance will be a no-op OpenTelemetry if telemetry
     *         is disabled or the application has shut down.
     */
    public static OpenTelemetryInfo getOpenTelemetryInfo() {
        try {
            OpenTelemetryInfoFactory factory = OSGIHelpers.getService(OpenTelemetryInfoFactory.class, OpenTelemetryAccessor.class);
            return factory.getOpenTelemetryInfo();
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
    public static Tracer getTracer() {
        return getOpenTelemetryInfo().getTracer();
    }

    /**
     * Acquires the span for the current Context
     *
     * @return Returns the Span from the current Context, falling back to a default, no-op Span if there is no span in the current context.
     */
    public static Span getSpan() {
        return Span.current();
    }

    /**
     * Acquires the Baggage for the current Context
     *
     * @return Returns Baggage from the current Context, falling back to empty Baggage if none is in the current Context.
     */
    public static Baggage getBaggage() {
        return Baggage.current();
    }

    /**
     * Gets the CDIService service
     *
     * @return the current CDIService instance
     */
    public static CDIService getCdiService() {
        return instance.map(i -> i.cdiService)
                       .orElseThrow(() -> new IllegalStateException("Unable to get CDIService"));
    }

}