/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import javax.interceptor.InvocationContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.microprofile.telemetry.internal.common.info.ErrorOpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfo;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class OpenTelemetryAccessor {

    private static final TraceComponent tc = Tr.register(OpenTelemetryAccessor.class);
    private static final ServiceCaller<OpenTelemetryInfoFactory> openTelemetryInfoFactoryService = new ServiceCaller<OpenTelemetryInfoFactory>(OpenTelemetryAccessor.class, OpenTelemetryInfoFactory.class);
    private static final ServiceCaller<CDIService> cdiService = new ServiceCaller<CDIService>(OpenTelemetryAccessor.class, CDIService.class);

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    /**
     * Gets or creates the instance of OpenTelemetry associated with this application and returns it wrapped inside an OpenTelemetryInfo.
     *
     * @return An instance of OpenTelemetryInfo containing the instance of OpenTelemetry associated with this application. This instance will be a no-op OpenTelemetry if telemetry
     *         is disabled or the application has shut down.
     */
    public static OpenTelemetryInfo getOpenTelemetryInfo() {
        Optional<OpenTelemetryInfo> openTelemetryInfo = openTelemetryInfoFactoryService.call((factory) -> {
            return factory.getOpenTelemetryInfo();
        });
        return openTelemetryInfo.orElseGet(ErrorOpenTelemetryInfo::new);
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
     * Returns all interceptor bindings which apply to the current invocation or lifecycle event by calling CDIService.getInterceptorBindingsFromInvocationContext().
     *
     * @return a set of interceptor bindings which apply to the current invocation or lifecycle event. This will include all interceptor bindings that apply, not just those that
     *         were used to bind the current interceptor.
     * @throws IllegalArgumentException if InvocationContext is not an instance of org.jboss.weld.interceptor.proxy.AbstractInvocationContext;
     */
    public static Set<Annotation> getInterceptorBindingsFromInvocationContext(final InvocationContext context) {
        Optional<Set<Annotation>> bindings = cdiService.call((service) -> {
            return service.getInterceptorBindingsFromInvocationContext(context);
        });
        return bindings.orElseThrow(() -> new IllegalStateException("Unable to get CDIService"));
    }

}
