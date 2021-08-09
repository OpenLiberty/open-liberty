/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.metrics11.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;

import io.astefanutti.metrics.cdi.MetricName;
import io.astefanutti.metrics.cdi.MetricResolver;
import io.astefanutti.metrics.cdi.MetricsExtension;

@ApplicationScoped
@Specializes
public class MetricResolver11 extends MetricResolver {

    private final MetricRegistry registry;

    @Inject
    public MetricResolver11(MetricsExtension extension, MetricName metricName, MetricRegistry registry) {
        this.extension = extension;
        this.metricName = metricName;
        this.registry = registry;
    }

    @Override
    protected <E extends Member & AnnotatedElement, T extends Annotation> Of<T> elementResolverOf(E element, Class<T> metric) {
        Of<T> of = super.elementResolverOf(element, metric);
        if (of instanceof DoesHaveMetric) {
            of.metadata().setReusable(getReusable(of.metricAnnotation()));
            checkReusable(of);
        }
        return of;
    }

    @Override
    protected <E extends Member & AnnotatedElement, T extends Annotation> Of<T> beanResolverOf(E element, Class<T> metric, Class<?> bean) {
        Of<T> of = super.beanResolverOf(element, metric, bean);
        if (of instanceof DoesHaveMetric) {
            of.metadata().setReusable(getReusable(of.metricAnnotation()));
            checkReusable(of);
        }
        return of;
    }

    private boolean getReusable(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).reusable();
        else if (Gauge.class.isInstance(annotation))
            return false;
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).reusable();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).reusable();
        else
            throw new IllegalArgumentException("Unsupported Metrics forMethod [" + annotation.getClass().getName() + "]");
    }

    /**
     * Checks whether the metric should be re-usable
     */
    private <T extends Annotation> boolean checkReusable(MetricResolver.Of<T> of) {
        String name = of.metadata().getName();
        // If the metric has been registered before (eg. metrics found in RequestScoped beans),
        // we don't need to worry about re-usable
        if (!of.isInitialDiscovery()) {
            return true;
        }

        Metadata existingMetadata = registry.getMetadata().get(name);
        if (existingMetadata != null && (existingMetadata.isReusable() == false || of.metadata().isReusable() == false)) {
            throw new IllegalArgumentException("Cannot reuse metric for " + of.metricName());
        }
        return true;

    }
}
