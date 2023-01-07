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
package io.openliberty.microprofile.telemetry.internal.utils;

public class TestConstants {

    /**
     * Environment variable to disable/enable OTel
     */
    public static final String ENV_OTEL_SDK_DISABLED = "OTEL_SDK_DISABLED";

    /**
     * Environment variable to set the service name
     */
    public static final String ENV_OTEL_SERVICE_NAME = "OTEL_SERVICE_NAME";

    /**
     * Environment variable to set the trace exporter
     */
    public static final String ENV_OTEL_TRACES_EXPORTER = "OTEL_TRACES_EXPORTER";

    /**
     * Environment variable to set the OTLP exporter endpoint
     */
    public static final String ENV_OTEL_EXPORTER_OTLP_ENDPOINT = "OTEL_EXPORTER_OTLP_ENDPOINT";

    /**
     * Environment variable to set the legacy Jaeger exporter endpoint
     */
    public static final String ENV_OTEL_EXPORTER_JAEGER_ENDPOINT = "OTEL_EXPORTER_JAEGER_ENDPOINT";

    /**
     * Environment variable to set the Zipkin exporter endpoint
     */
    public static final String ENV_OTEL_EXPORTER_ZIPKIN_ENDPOINT = "OTEL_EXPORTER_ZIPKIN_ENDPOINT";

    /**
     * Environment variable to set the maximum time to wait before sending a batch of traces to the trace server
     */
    public static final String ENV_OTEL_BSP_SCHEDULE_DELAY = "OTEL_BSP_SCHEDULE_DELAY";

    /**
     * The TraceID value returned when there is no active trace
     */
    public static final String NULL_TRACE_ID = "00000000000000000000000000000000";

    /*
     * Private constructor, no instances
     */
    private TestConstants() {}
}
