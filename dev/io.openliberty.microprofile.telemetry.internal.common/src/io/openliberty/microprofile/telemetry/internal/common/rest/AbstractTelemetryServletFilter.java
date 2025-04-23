/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.rest;

/**
 *
 */
public class AbstractTelemetryServletFilter {

    public static final String SPAN_CONTEXT = "otel.span.http.context";
    public static final String SPAN_PARENT_CONTEXT = "otel.span.http.parentContext";
    public static final String SPAN_SCOPE = "otel.span.http.scope";
    public static final String ENV_DISABLE_HTTP_TRACING_PROPERTY = "OTEL_TRACE_HTTP_DISABLED";
    public static final String CONFIG_DISABLE_HTTP_TRACING_PROPERTY = "otel.trace.http.disabled";
    public static final String ACCESS_TRACE_HEADER_NAME = "io.openliberty.trace";

}
