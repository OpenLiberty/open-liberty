/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.fat.defaultBuckets;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 *
 */
@ApplicationScoped
@Path("/test")
public class MetricsResource {

    @Inject
    MetricRegistry metricRegistry;

    @GET
    @Path("/histogram")
    public String histogram() throws InterruptedException {
        Histogram histogram = metricRegistry.histogram("testHistogram");
        return "Histogram  registered";
    }

    @GET
    @Path("/timer")
    public String timer() throws InterruptedException {
        Timer timer = metricRegistry.timer("testTimer");
        return "Timer  registered";
    }

    @GET
    @Path("/histogramMinMax")
    public String histogramMinMax() throws InterruptedException {
        Histogram histogram = metricRegistry.histogram("testHistogramMinMax");
        return "Histogram  registered";
    }

    @GET
    @Path("/timerMinMax")
    public String timerMinMax() throws InterruptedException {
        Timer timer = metricRegistry.timer("testTimerMinMax");
        return "Timer  registered";
    }

    @GET
    @Path("/histogramBadEnableConfig")
    public String badEnableconfig() throws InterruptedException {
        Histogram histogram = metricRegistry.histogram("histogram.bad.enable.config");
        return "Histogram registered";
    }

    @GET
    @Path("/timerBadMaxGoodMinConfig")
    public String timerBadMaxGoodMinConfig() throws InterruptedException {
        Timer timer = metricRegistry.timer("timer.bad.max.good.min.config");
        return "Timer registered";
    }

    @GET
    @Path("/timerBadMinGoodMaxConfig")
    public String timerBadMinGoodMaxConfig() throws InterruptedException {
        Timer timer = metricRegistry.timer("timer.bad.min.good.max.config");
        return "Timer registered";
    }

    @GET
    @Path("/histogramBadMinGoodMaxConfig")
    public String histogramBadMinGoodMaxConfig() throws InterruptedException {
        Histogram histogram = metricRegistry.histogram("histogram.bad.min.good.max.config");
        return "Histogram registered";
    }

    @GET
    @Path("/histogramBadMaxGoodMinConfig")
    public String histogramBadMaxGoodMinConfig() throws InterruptedException {
        Histogram histogram = metricRegistry.histogram("histogram.bad.max.good.min.config");
        return "Histogram registered";
    }

    @GET
    @Path("/timerGoodMinGoodMaxConfig")
    public String timerGoodMinGoodMaxConfig() throws InterruptedException {
        Timer timer = metricRegistry.timer("timer.good.min.good.max.config");
        return "Timer registered";
    }

    @GET
    @Path("/histogramGoodMaxGoodMinConfig")
    public String histogramGoodMaxGoodMinConfig() throws InterruptedException {
        Histogram histogram = metricRegistry.histogram("histogram.good.max.good.min.config");
        return "Histogram registered";
    }
}
