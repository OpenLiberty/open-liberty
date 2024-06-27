/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.common.constants;

public class OpenTelemetryConstants {

    public static final String ENV_DISABLE_PROPERTY = "OTEL_SDK_DISABLED";
    public static final String CONFIG_DISABLE_PROPERTY = "otel.sdk.disabled";
    public static final String ENV_METRICS_EXPORTER_PROPERTY = "OTEL_METRICS_EXPORTER";
    public static final String CONFIG_METRICS_EXPORTER_PROPERTY = "otel.metrics.exporter";
    public static final String ENV_LOGS_EXPORTER_PROPERTY = "OTEL_LOGS_EXPORTER";
    public static final String CONFIG_LOGS_EXPORTER_PROPERTY = "otel.logs.exporter";
    public static final String SERVICE_NAME_PROPERTY = "otel.service.name";
    public static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";
    public static final String OTEL_RUNTIME_INSTANCE_NAME = "io.openliberty.microprofile.telemetry.runtime";

    //HTTP Metric attributes
    public static final String ERROR_TYPE = "error.type";
    public static final String HTTP_REQUEST_METHOD = "http.request.method";
    public static final String HTTP_RESPONSE_STATUS_CODE = "http.response.status_code";
    public static final String HTTP_ROUTE = "http.route";
    public static final String NETWORK_PROTOCOL_NAME = "network.protocol.name";
    public static final String NETWORK_PROTOCOL_VERSION = "network.protocol.version";
    public static final String SERVER_ADDRESS = "server.address";
    public static final String SERVER_PORT = "server.port";
    public static final String URL_SCHEME = "url.scheme";

    //HTTP Metric name + desc
    public static final String HTTP_SERVER_REQUEST_DURATION_NAME = "http.server.request.duration";
    public static final String HTTP_SERVER_REQUEST_DURATION_DESC = "Duration of HTTP server requests.";

    //HTTP Metric units
    public static final String OTEL_SECONDS_UNIT = "s";
}
