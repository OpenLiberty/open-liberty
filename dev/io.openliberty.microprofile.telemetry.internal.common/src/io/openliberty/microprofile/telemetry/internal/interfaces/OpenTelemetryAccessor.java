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
    public static OpenTelemetryInfo getOpenTelemetryInfo() {
        return OpenTelemetryInfoFactory.getOpenTelemetryInfo(ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName());
    }

    public static Tracer getTracer() {
        return OpenTelemetryInfoFactory.getTracer(ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName());
    }

    public static Span getSpan() {
        return OpenTelemetryInfoFactory.getSpan();
    }

    public static Baggage getBaggage() {
        return OpenTelemetryInfoFactory.getBaggage();
    }

}