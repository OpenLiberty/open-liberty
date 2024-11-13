/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.spi;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class OpenTelemetryAccessor {
    private static final TraceComponent tc = Tr.register(OpenTelemetryAccessor.class);

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    /**
     * Gets or creates the instance of OpenTelemetry associated with this application and returns it wrapped inside an OpenTelemetryInfo.
     * <p>
     * If OpenTelemetry has a runtime instance this will be returned for all applications. If it does not, it will use the application metadata
     * from the current thread to find the instance of OpenTelemetry associated with this application. If there is no metadata on the thread,
     * or if the application has shut down, it will return an OpenTelemetryInfo containing a no-op OpenTelemetry object.
     *
     * @return An instance of OpenTelemetryInfo containing the instance of OpenTelemetry associated with this application.
     */
    public static OpenTelemetryInfo getOpenTelemetryInfo() {
        return io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor.getOpenTelemetryInfo();
    }
}
