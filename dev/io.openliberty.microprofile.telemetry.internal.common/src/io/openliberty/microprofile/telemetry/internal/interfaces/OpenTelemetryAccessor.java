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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.telemetry.internal.common.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.OpenTelemetryInfoFactory;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class OpenTelemetryAccessor {

    private static final TraceComponent tc = Tr.register(OpenTelemetryAccessor.class);

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    /**
     * Gets or creates the instance of OpenTelemetry associated with this application and returns it wrapped inside an OpenTelemetryInfo.
     *
     * @return An instance of OpenTelemetryInfo containing the instance of OpenTelemetry associated with this application. This instance will be a no-op OpenTelemetry if telemetry is disabled or the application has shut down.  
     */
    public static OpenTelemetryInfo getOpenTelemetryInfo() {
        return OpenTelemetryInfoFactory.getOpenTelemetryInfo(ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName());
    }

    /**
     * Gets or creates a tracer instance from the TracerProvider for the OpenTelemetry instance associated with this application.
     * 
     * @return An tracer instance from the instance of OpenTelemetry associated with this application. This instance will be a no-op if telemetry is disabled or the application has shut down.
     */
    public static Tracer getTracer() {
        return OpenTelemetryInfoFactory.getTracer(ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName());
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

}