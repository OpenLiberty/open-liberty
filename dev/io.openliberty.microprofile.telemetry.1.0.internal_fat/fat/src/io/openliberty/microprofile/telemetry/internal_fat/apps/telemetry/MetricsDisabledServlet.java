/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/testMetricsDisabled")
@ApplicationScoped
public class MetricsDisabledServlet extends FATServlet {

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    @SkipForRepeat({TelemetryActions.MP61_MPTEL20_ID, TelemetryActions.MP60_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_ID, TelemetryActions.MP41_MPTEL20_ID,  TelemetryActions.MP14_MPTEL20_ID}) //Metrics is not disabled for MpTelemetry-2.0
    public void testMetricsDisabledServlet() {
        //metricReaders should not contain an
        //The exporter should be set to `none` despite having `otel.metrics.exporter=otlp` in microprofile-config.properties
        assertThat(openTelemetry.toString(), containsString("metricReaders=[]"));
    }
}