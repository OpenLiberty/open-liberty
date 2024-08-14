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
package io.openliberty.microprofile.telemetry.internal.apps.metricTest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;

/**
 * A Restful WS resource which can create spans in various ways which are useful for tests
 * <p>
 * Each method returns the current Trace ID so that tests can query the trace store and ensure the correct spans were created.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class TestResource {

    @Inject
    private Meter sdkMeter;

    private static final String longUpDownCounterName = "testLongUpDownCounter";
    private static final String longCounterName = "testLongCounter";
    private static final String longHistogramName = "testLongHistogram";
    private static final String doubleCounterName = "testDoubleCounter";
    private static final String doubleUpDownCounterName = "testDoubleUpDownCounter";
    private static final String counterDescription = "Testing long up down counter";
    private static final String counterUnit = "Metric Tonnes";
    private static final String histogramDescription = "Testing long histogram";
    private static final String histogramUnit = "Metric Tonnes";

    private static final long LONG_VALUE = 10;
    private static final long LONG_UP_DOWN_VALUE = -10;

    private static final long DOUBLE_VALUE = 20;
    private static final long DOUBLE_UP_DOWN_VALUE = -20;

    //Creates Long Up Down Counter
    @GET
    @Path("/longUpDownCounterCreated")
    public Response longUpDownCounterCreated() {
        LongUpDownCounter longUpDownCounter = sdkMeter
                                                      .upDownCounterBuilder(longUpDownCounterName)
                                                      .setDescription(counterDescription)
                                                      .setUnit(counterUnit)
                                                      .build();
        //Result should total -20
        longUpDownCounter.add(LONG_UP_DOWN_VALUE, Attributes.empty());
        longUpDownCounter.add(LONG_UP_DOWN_VALUE, Attributes.empty());
        return Response.ok("longUpDownCounterCreated").build();
    }

    //Creates Long Counter
    @GET
    @Path("/longCounterCreated")
    public Response longCounterCreated() {
        LongCounter longCounter = sdkMeter
                                          .counterBuilder(longCounterName)
                                          .setDescription(counterDescription)
                                          .setUnit(counterUnit)
                                          .build();
        //Result should total 20
        longCounter.add(LONG_VALUE, Attributes.empty());
        longCounter.add(LONG_VALUE, Attributes.empty());
        return Response.ok("longCounterCreated").build();
    }

    //Creates Long Histogram
    @GET
    @Path("/longHistogramCreated")
    public Response longHistrogramCreated() {
        LongHistogram longHistogram = sdkMeter
                                              .histogramBuilder(longHistogramName)
                                              .ofLongs()
                                              .setDescription(histogramDescription)
                                              .setUnit(histogramUnit)
                                              .build();
        //Sum should be 20
        longHistogram.record(LONG_VALUE, Attributes.empty());
        longHistogram.record(LONG_VALUE, Attributes.empty());
        return Response.ok("longHistogramCreated").build();
    }

    //Creates Double Counter
    @GET
    @Path("/doubleCounterCreated")
    public Response doubleCounterCreated() {
        DoubleCounter doubleCounter = sdkMeter
                                              .counterBuilder(doubleCounterName)
                                              .ofDoubles()
                                              .setDescription(counterDescription)
                                              .setUnit(counterUnit)
                                              .build();
        //Result should total 40
        doubleCounter.add(DOUBLE_VALUE, Attributes.empty());
        doubleCounter.add(DOUBLE_VALUE, Attributes.empty());
        return Response.ok("doubleCounterCreated").build();
    }

    //Creates Double Up Down Counter
    @GET
    @Path("/doubleUpDownCounterCreated")
    public Response doubleUpDownCounterCreated() {
        DoubleUpDownCounter doubleUpDownCounter = sdkMeter
                                                          .upDownCounterBuilder(doubleUpDownCounterName)
                                                          .ofDoubles()
                                                          .setDescription(counterDescription)
                                                          .setUnit(counterUnit)
                                                          .build();
        //Result should total -40
        doubleUpDownCounter.add(DOUBLE_UP_DOWN_VALUE, Attributes.empty());
        doubleUpDownCounter.add(DOUBLE_UP_DOWN_VALUE, Attributes.empty());
        return Response.ok("doubleUpDownCounterCreated").build();
    }

}
