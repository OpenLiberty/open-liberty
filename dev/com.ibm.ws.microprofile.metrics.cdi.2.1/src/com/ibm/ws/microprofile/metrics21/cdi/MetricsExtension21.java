package com.ibm.ws.microprofile.metrics21.cdi;
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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import io.astefanutti.metrics.cdi.AnnotatedTypeDecorator;
import io.astefanutti.metrics.cdi.MetricProducer;
import io.astefanutti.metrics.cdi.MetricsExtension;

@Component(service = WebSphereCDIExtension.class, immediate = true)
@Specializes
@Alternative
public class MetricsExtension21 extends MetricsExtension implements Extension, WebSphereCDIExtension {
    protected static final AnnotationLiteral<MetricsBinding21> METRICS_BINDING21 = new AnnotationLiteral<MetricsBinding21>() {
    };

    @Override
    protected <X> void metricsAnnotations(@Observes @WithAnnotations({ Counted.class, Gauge.class, Metered.class, Timed.class,
                                                                       ConcurrentGauge.class }) ProcessAnnotatedType<X> pat) {

        pat.setAnnotatedType(new AnnotatedTypeDecorator<>(pat.getAnnotatedType(), METRICS_BINDING21));
    }

    /**
     * Ignores metric producer class
     */
    private void vetoMetricProducer(@Observes ProcessAnnotatedType<MetricProducer> pat) {
        pat.veto();
    }

    @Override
    protected void metricProducerMethod(@Observes ProcessProducerMethod<? extends Metric, ?> ppm) {
        // Skip the Metrics CDI alternatives
        if (!ppm.getBean().getBeanClass().equals(MetricProducer21.class)) {
            metrics.put(ppm.getBean(), ppm.getAnnotatedProducerMethod());
        }
    }

}
