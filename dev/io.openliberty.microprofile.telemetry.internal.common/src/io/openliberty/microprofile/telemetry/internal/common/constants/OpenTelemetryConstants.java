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

}
