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
package io.openliberty.microprofile.telemetry.internal.common.info;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/**
 * This produces an OpenTelemetry wrapper with info to state whether it is enabled or disabled
 */
public class OpenTelemetryInfoImpl implements OpenTelemetryInfo {

    private static final TraceComponent tc = Tr.register(OpenTelemetryInfoImpl.class);
    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";

    private final boolean enabled;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    /**
     * @param enabled
     * @param openTelemetry
     */
    public OpenTelemetryInfoImpl(boolean enabled, OpenTelemetry openTelemetry, String appName) {
        super();
        this.enabled = enabled;
        this.openTelemetry = openTelemetry;
        tracer = openTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    /**
     * No-args constructor for CDI
     */
    public OpenTelemetryInfoImpl() {
        this.enabled = false;
        this.openTelemetry = null;
        tracer = null;
    }

    /**
     * @return the enabled
     */
    @Override
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * @return the openTelemetry
     */
    @Override
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    /**
     * @return the Tracer
     */
    @Override
    public Tracer getTracer() {
        return tracer;
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        try {
            if (AgentDetection.isAgentActive()) {
                return;
            }

            if (openTelemetry instanceof OpenTelemetrySdk) {
                OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetry;
                List<CompletableResultCode> results = new ArrayList<>();

                SdkTracerProvider tracerProvider = sdk.getSdkTracerProvider();
                if (tracerProvider != null) {
                    results.add(tracerProvider.shutdown());
                }

                SdkMeterProvider meterProvider = sdk.getSdkMeterProvider();
                if (meterProvider != null) {
                    results.add(meterProvider.shutdown());
                }

                SdkLoggerProvider loggerProvider = sdk.getSdkLoggerProvider();
                if (loggerProvider != null) {
                    results.add(loggerProvider.shutdown());
                }

                CompletableResultCode.ofAll(results).join(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }

    }

}