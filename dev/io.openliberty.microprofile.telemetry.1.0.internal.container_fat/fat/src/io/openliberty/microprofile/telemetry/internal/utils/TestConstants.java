/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import org.testcontainers.utility.DockerImageName;

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

    /**
     * Environment variable to enable TLS, defualts to false.
     */
    public static final String ENV_COLLECTOR_OTLP_GRPC_TLS_ENABLED = "COLLECTOR_OTLP_GRPC_TLS_ENABLED";

    /**
     * Path to the private key for OTLP secure connections
     */
    public static final String ENV_OTEL_EXPORTER_OTLP_CLIENT_KEY = "OTEL_EXPORTER_OTLP_CLIENT_KEY";

    /**
     * Path to the certificate for OTLP secure connections
     */
    public static final String ENV_OTEL_EXPORTER_OTLP_CERTIFICATE = "OTEL_EXPORTER_OTLP_CERTIFICATE";

    /**
     * Path to the client CA for OTLP secure connections
     */
    public static final String ENV_OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE = "OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE";

    /*
     * Private constructor, no instances
     */
    private TestConstants() {}

    //Docker image names. These must be kept in sync with bnd.bnd
    public static final DockerImageName DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR = DockerImageName.parse("otel/opentelemetry-collector:0.74.0");
    public static final DockerImageName DOCKER_IMAGE_ALL_IN_ONE = DockerImageName.parse("jaegertracing/all-in-one:1.54");
    public static final DockerImageName DOCKER_IMAGE_ZIPKIN_SLIM = DockerImageName.parse("openzipkin/zipkin-slim:2.23");


}
