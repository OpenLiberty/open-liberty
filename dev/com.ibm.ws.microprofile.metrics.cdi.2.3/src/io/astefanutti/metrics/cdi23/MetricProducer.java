/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package io.astefanutti.metrics.cdi23;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.ws.microprofile.metrics.cdi23.helper.Utils;

@Alternative
@Dependent
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
/* package-private */ public class MetricProducer {

    @Produces
    private static Counter counter(InjectionPoint ip, MetricRegistry registry, MetricName metricName, MetricsExtension extension) {
        Metadata metadata = metricName.metadataOf(ip, Counter.class);
        String[] tags = metricName.tagOf(ip);
        MetricID mid = new MetricID(metadata.getName(), Utils.tagsToTags(tags));
        extension.addMetricID(mid);

        return registry.counter(metadata, Utils.tagsToTags(tags));
    }

    @Produces
    private static ConcurrentGauge concurrentGauge(InjectionPoint ip, MetricRegistry registry, MetricName metricName, MetricsExtension extension) {
        Metadata metadata = metricName.metadataOf(ip, Counter.class);
        String[] tags = metricName.tagOf(ip);
        MetricID mid = new MetricID(metadata.getName(), Utils.tagsToTags(tags));
        extension.addMetricID(mid);

        return registry.concurrentGauge(metadata, Utils.tagsToTags(tags));
    }

    @Produces
    private static <T> Gauge<T> gauge(final InjectionPoint ip, final MetricRegistry registry, final MetricName metricName) {
        // A forwarding Gauge must be returned as the Gauge creation happens when the declaring bean gets instantiated and the corresponding Gauge can be injected before which leads to producing a null value
        return new Gauge<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T getValue() {
                // TODO: better error report when the gauge doesn't exist
                MetricID tempMId = new MetricID(metricName.of(ip), Utils.tagsToTags(metricName.tagOf(ip)));
                return ((Gauge<T>) registry.getGauges().get(tempMId)).getValue();
            }
        };
    }

    @Produces
    private static Histogram histogram(InjectionPoint ip, MetricRegistry registry, MetricName metricName, MetricsExtension extension) {
        Metadata metadata = metricName.metadataOf(ip, Histogram.class);
        String tags[] = metricName.tagOf(ip);
        MetricID mid = new MetricID(metadata.getName(), Utils.tagsToTags(tags));
        extension.addMetricID(mid);
        return registry.histogram(metadata, Utils.tagsToTags(tags));
    }

    @Produces
    private static Meter meter(InjectionPoint ip, MetricRegistry registry, MetricName metricName, MetricsExtension extension) {
        Metadata metadata = metricName.metadataOf(ip, Meter.class);
        String[] tags = metricName.tagOf(ip);
        MetricID mid = new MetricID(metadata.getName(), Utils.tagsToTags(tags));
        extension.addMetricID(mid);
        return registry.meter(metadata, Utils.tagsToTags(tags));
    }

    @Produces
    private static Timer timer(InjectionPoint ip, MetricRegistry registry, MetricName metricName, MetricsExtension extension) {
        Metadata metadata = metricName.metadataOf(ip, Timer.class);
        String[] tags = metricName.tagOf(ip);
        MetricID mid = new MetricID(metadata.getName(), Utils.tagsToTags(tags));
        extension.addMetricID(mid);
        return registry.timer(metadata, Utils.tagsToTags(tags));
    }

    @Produces
    private static SimpleTimer simpleTimer(InjectionPoint ip, MetricRegistry registry, MetricName metricName, MetricsExtension extension) {
        Metadata metadata = metricName.metadataOf(ip, SimpleTimer.class);
        String[] tags = metricName.tagOf(ip);
        MetricID mid = new MetricID(metadata.getName(), Utils.tagsToTags(tags));
        extension.addMetricID(mid);
        return registry.simpleTimer(metadata, Utils.tagsToTags(tags));
    }
}