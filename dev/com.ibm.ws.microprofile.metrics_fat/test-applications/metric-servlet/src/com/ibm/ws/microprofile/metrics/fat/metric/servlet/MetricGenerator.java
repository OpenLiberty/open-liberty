/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.fat.metric.servlet;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;

@ApplicationScoped
public class MetricGenerator {

    @Inject
    MetricRegistry metricRegistryInstance;

    private final int testGaugeVal1 = 55555555;
    private final int testGaugeVal2 = 10101010;
    private final int testGaugeVal3 = 998989881;

    @PostConstruct
    private void init() {
        Counter c = metricRegistryInstance.counter("testCounter");
        int i = 0;
        while (i < 10) {
            c.inc();
            i++;
        }
    }

    @Timed(name = "doSomethingTimed", absolute = true)
    public void doSomethingWithTimed() {
        int i = 0;
        while (i < 10000) {
            if (metricRegistryInstance instanceof MetricRegistry)
                ;
            i++;
        }
    }

    @Counted(name = "doSomething", absolute = true, monotonic = true)
    public void doSomething() {
        System.out.println("Doing something");
    }

    @Gauge(unit = MetricUnits.NONE, name = "testGaugeOne", absolute = true, description = "testGaugeOne returns five")
    public int getTestGaugeOne() {
        return testGaugeVal1;
    }

    @Produces
    @Metric(name = "testGaugeTwo")
    @ApplicationScoped
    protected org.eclipse.microprofile.metrics.Gauge<Integer> createHitPercentage() {
        return new org.eclipse.microprofile.metrics.Gauge<Integer>() {

            @Override
            public Integer getValue() {
                return testGaugeVal2;
            }
        };
    }

    @Produces
    @Metric(name = "testGaugeThree")
    @ApplicationScoped
    org.eclipse.microprofile.metrics.Gauge<Integer> ninerNiner = new org.eclipse.microprofile.metrics.Gauge<Integer>() {

        @Override
        public Integer getValue() {
            return testGaugeVal3;
        }
    };

}
