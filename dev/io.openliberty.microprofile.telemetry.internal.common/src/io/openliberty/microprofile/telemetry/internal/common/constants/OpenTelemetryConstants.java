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

import io.opentelemetry.api.common.AttributeKey;

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

    //HTTP Metric name + desc
    public static final String HTTP_SERVER_REQUEST_DURATION_NAME = "http.server.request.duration";
    public static final String HTTP_SERVER_REQUEST_DURATION_DESC = "Duration of HTTP server requests.";

    //HTTP Metric units
    public static final String OTEL_SECONDS_UNIT = "s";

    //Attribute Keys
    //We're not pulling these from constants in semconv libraries because the import will be version specific
    public static final AttributeKey<String> KEY_SERVICE_NAME = AttributeKey.stringKey("service.name");
    public static final AttributeKey<String> KEY_SERVICE_INSTANCE_ID = AttributeKey.stringKey("service.instance.id");

    //Other OTel standards
    public static final String UNKOWN_SERVICE = "unkown_service";

    //OpenLiberty namespace prefix
    public static final String NAME_SPACE_PREFIX = "io.openliberty.";

    //(Open Liberty) Connection Pool metric name + desc
    public static final String WAIT_TIME_NAME = "connection_pool.connection.wait_time";

    public static final String IN_USE_TIME_NAME = "connection_pool.connection.use_time";

    public static final String DATASOURCE_ATTRIBUTE = "datasource.name";
}
