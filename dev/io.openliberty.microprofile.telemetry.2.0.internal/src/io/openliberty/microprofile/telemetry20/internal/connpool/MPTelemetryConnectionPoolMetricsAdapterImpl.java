/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.connpool;

import java.time.Duration;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.connectionpool.monitor.metrics.ConnectionPoolMetricAdapter;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.context.Context;

@Component(service = { ConnectionPoolMetricAdapter.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPTelemetryConnectionPoolMetricsAdapterImpl implements ConnectionPoolMetricAdapter {

    private static final TraceComponent tc = Tr.register(MPTelemetryConnectionPoolMetricsAdapterImpl.class);

    private static final String INSTR_SCOPE = "io.openliberty.monitor.metrics";

    private static final double NANO_CONVERSION = 0.000000001;

    public void updateHistogramMetric(String metricName, String description, String poolName, Duration duration) {

        OpenTelemetry otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo().getOpenTelemetry();

        /*
         * AppName is retrived through component metadata.
         * Since we're running this through method call from the probed methods.
         * We "should" always return an appname
         *
         */
        if (otelInstance == null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                         String.format("Unable to resolve an OpenTelemetry instance for the ConnectionPool [%s]", poolName));
            }
            //do nothing - return
            return;

        }

        //Use boundaries specified by Otel DB metrics  semantic convention
        DoubleHistogram dHistogram = otelInstance.getMeterProvider().get(INSTR_SCOPE).histogramBuilder(metricName)
                        .setUnit(OpenTelemetryConstants.OTEL_SECONDS_UNIT)
                        .setDescription(description)
                        .setExplicitBucketBoundariesAdvice(List.of(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0)).build();

        Context ctx = Context.current();

        double seconds = duration.toNanos() * NANO_CONVERSION;

        dHistogram.record(seconds, Attributes.builder().put(OpenTelemetryConstants.NAME_SPACE_PREFIX + OpenTelemetryConstants.DATASOURCE_ATTRIBUTE, poolName).build(), ctx);

    }

    /** {@inheritDoc} */
    @Override
    public void updateWaitTimeMetrics(String poolName, Duration duration) {
        updateHistogramMetric(OpenTelemetryConstants.NAME_SPACE_PREFIX + OpenTelemetryConstants.WAIT_TIME_NAME, OpenTelemetryConstants.WAIT_TIME_DESC, poolName, duration);
    }

    /** {@inheritDoc} */
    @Override
    public void updateInUseTimeMetrics(String poolName, Duration duration) {
        updateHistogramMetric(OpenTelemetryConstants.NAME_SPACE_PREFIX + OpenTelemetryConstants.IN_USE_TIME_NAME, OpenTelemetryConstants.IN_USE_TIME_DESC, poolName, duration);
    }

}
