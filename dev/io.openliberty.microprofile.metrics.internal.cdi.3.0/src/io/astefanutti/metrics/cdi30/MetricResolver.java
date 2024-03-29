/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
package io.astefanutti.metrics.cdi30;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;

import io.openliberty.microprofile.metrics.internal.cdi30.helper.Utils;

@ApplicationScoped
public class MetricResolver {

    @Inject
    private MetricRegistry registry;

    @Inject
    protected MetricsExtension extension;

    @Inject
    protected MetricName metricName;

    @Inject
    protected BeanManager beanManager;

    public <E extends Member & AnnotatedElement> Of<Counted> counted(Class<?> topClass, E element) {
        return resolverOf(topClass, element, Counted.class);
    }

    public <E extends Member & AnnotatedElement> Of<ConcurrentGauge> concurentGauged(Class<?> topClass, E element) {
        return resolverOf(topClass, element, ConcurrentGauge.class);
    }

    public Of<Gauge> gauge(Class<?> topClass, Method method) {
        return resolverOf(topClass, method, Gauge.class);
    }

    public <E extends Member & AnnotatedElement> Of<Metered> metered(Class<?> topClass, E element) {
        return resolverOf(topClass, element, Metered.class);
    }

    public <E extends Member & AnnotatedElement> Of<Timed> timed(Class<?> bean, E element) {
        return resolverOf(bean, element, Timed.class);
    }

    public <E extends Member & AnnotatedElement> Of<SimplyTimed> simplyTimed(Class<?> bean, E element) {
        return resolverOf(bean, element, SimplyTimed.class);
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> Of<T> resolverOf(Class<?> bean, E element, Class<T> metric) {
        if (element.isAnnotationPresent(metric))
            return elementResolverOf(element, metric);
        else
            return beanResolverOf(element, metric, bean);
    }

    protected <E extends Member & AnnotatedElement, T extends Annotation> Of<T> elementResolverOf(E element, Class<T> metric) {
        T annotation = element.getAnnotation(metric);

        // See if we have the name cached
        String name = extension.getMetricNameForMember(element, annotation);

        boolean initialDiscovery = false;
        if (name == null) {
            name = metricName(element, metric, metricName(annotation), isMetricAbsolute(annotation));
            initialDiscovery = true;
        }

        MetadataBuilder mdb = Metadata.builder().withName(name).withType(this.getType(annotation)).withUnit(this.getUnit(annotation)).withDescription(this.getDescription(annotation)).withDisplayName(this.getDisplayname(annotation));

        String[] tags = this.getTags(annotation);

        Of<T> of = new DoesHaveMetric<>(annotation, name, mdb.build(), initialDiscovery, tags);
        checkReusable(of);
        return of;
    }

    protected <T extends Annotation> boolean hasMetricAnnotationBean(Class<T> metric, Class<?> bean) {
        if (bean.isAnnotationPresent(metric))
            return true;

        /*
         * Go through the annotations available in this class to see if a sterotype is present
         * that contains the metric annotation we are testing for
         */
        Annotation[] beanAnotations = bean.getAnnotations();
        for (Annotation annotation : beanAnotations) {

            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (beanManager.isStereotype(annotationType) && annotationType.isAnnotationPresent(metric)) {
                return true;
            }
        }

        return false;
    }

    protected <T extends Annotation> T getAnnotationBean(Class<T> metric, Class<?> bean) {
        T annotationObj = bean.getAnnotation(metric);
        if (annotationObj != null)
            return annotationObj;

        /*
         * Go through the annotations available in this class to see if a sterotype is present
         * that contains the metric annotation we are testing for and retrieves the Annotation
         */

        Annotation[] beanAnotations = bean.getAnnotations();
        for (Annotation annotation : beanAnotations) {

            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (beanManager.isStereotype(annotationType) && annotationType.isAnnotationPresent(metric)) {
                annotationObj = annotationType.getAnnotation(metric);
                if (annotationObj != null) {
                    return annotationObj;
                }
            }
        }

        return null;
    }

    protected <E extends Member & AnnotatedElement, T extends Annotation> Of<T> beanResolverOf(E element, Class<T> metric, Class<?> bean) {

        if (hasMetricAnnotationBean(metric, bean)) {
            T annotation = getAnnotationBean(metric, bean);

            // See if we have the name cached
            String name = extension.getMetricNameForMember(element, annotation);

            boolean initialDiscovery = false;
            if (name == null) {
                name = metricName(bean, element, metric, metricName(annotation), isMetricAbsolute(annotation));
                initialDiscovery = true;
            }

            MetadataBuilder mdb = Metadata.builder().withName(name).withType(this.getType(annotation)).withUnit(this.getUnit(annotation)).withDescription(this.getDescription(annotation)).withDisplayName(this.getDisplayname(annotation));
            String[] tags = this.getTags(annotation);

            Of<T> of = new DoesHaveMetric<>(annotation, name, mdb.build(), initialDiscovery, tags);
            checkReusable(of);

            return of;
        } else if (bean.getSuperclass() != null) {
            return beanResolverOf(element, metric, bean.getSuperclass());
        }
        return new DoesNotHaveMetric<>();

    }

    // TODO: should be grouped with the metric name strategy
    private <E extends Member & AnnotatedElement> String metricName(E element, Class<? extends Annotation> type, String name, boolean absolute) {
        String metric = name.isEmpty() ? defaultName(element, type) : metricName.of(name);
        return absolute ? metric : MetricRegistry.name(element.getDeclaringClass(), metric);
    }

    private <E extends Member & AnnotatedElement> String metricName(Class<?> bean, E element, Class<? extends Annotation> type, String name, boolean absolute) {
        String metric = name.isEmpty() ? bean.getSimpleName() : metricName.of(name);
        return absolute ? MetricRegistry.name(metric, defaultName(element, type)) : MetricRegistry.name(bean.getPackage().getName(), metric, defaultName(element, type));
    }

    private <E extends Member & AnnotatedElement> String defaultName(E element, Class<? extends Annotation> type) {
        return memberName(element);
    }

    // While the Member Javadoc states that the getName method should returns
    // the simple name of the underlying member or constructor, the FQN is returned
    // for constructors. See JDK-6294399:
    // http://bugs.java.com/view_bug.do?bug_id=6294399
    private String memberName(Member member) {
        if (member instanceof Constructor)
            return member.getDeclaringClass().getSimpleName();
        else
            return member.getName();
    }

    private String metricName(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).name();
        else if (ConcurrentGauge.class.isInstance(annotation))
            return ((ConcurrentGauge) annotation).name();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).name();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).name();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).name();
        else if (SimplyTimed.class.isInstance(annotation))
            return ((SimplyTimed) annotation).name();
        else
            throwIAEUnsupportedMetric(annotation);
        return null;
    }

    private boolean isMetricAbsolute(Annotation annotation) {

        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).absolute();
        else if (ConcurrentGauge.class.isInstance(annotation))
            return ((ConcurrentGauge) annotation).absolute();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).absolute();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).absolute();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).absolute();
        else if (SimplyTimed.class.isInstance(annotation))
            return ((SimplyTimed) annotation).absolute();
        else
            throwIAEUnsupportedMetric(annotation);
        return false;
    }

    private String[] getTags(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).tags();
        else if (ConcurrentGauge.class.isInstance(annotation))
            return ((ConcurrentGauge) annotation).tags();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).tags();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).tags();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).tags();
        else if (SimplyTimed.class.isInstance(annotation))
            return ((SimplyTimed) annotation).tags();
        else
            throwIAEUnsupportedMetric(annotation);
        return null;

    }

    private String getDisplayname(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).displayName();
        else if (ConcurrentGauge.class.isInstance(annotation))
            return ((ConcurrentGauge) annotation).displayName();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).displayName();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).displayName();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).displayName();
        else if (SimplyTimed.class.isInstance(annotation))
            return ((SimplyTimed) annotation).displayName();
        else
            throwIAEUnsupportedMetric(annotation);
        return null;

    }

    private String getDescription(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).description();
        else if (ConcurrentGauge.class.isInstance(annotation))
            return ((ConcurrentGauge) annotation).description();
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).description();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).description();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).description();
        else if (SimplyTimed.class.isInstance(annotation))
            return ((SimplyTimed) annotation).description();
        else
            throwIAEUnsupportedMetric(annotation);
        return null;
    }

    private MetricType getType(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return MetricType.COUNTER;
        else if (ConcurrentGauge.class.isInstance(annotation))
            return MetricType.CONCURRENT_GAUGE;
        else if (Gauge.class.isInstance(annotation))
            return MetricType.GAUGE;
        else if (Metered.class.isInstance(annotation))
            return MetricType.METERED;
        else if (Timed.class.isInstance(annotation))
            return MetricType.TIMER;
        else if (SimplyTimed.class.isInstance(annotation))
            return MetricType.SIMPLE_TIMER;
        else
            throwIAEUnsupportedMetric(annotation);
        return null;
    }

    private String getUnit(Annotation annotation) {
        if (Counted.class.isInstance(annotation))
            return ((Counted) annotation).unit();
        else if (ConcurrentGauge.class.isInstance(annotation))
            return MetricUnits.NONE;
        else if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).unit();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).unit();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).unit();
        else if (SimplyTimed.class.isInstance(annotation))
            return ((SimplyTimed) annotation).unit();
        else
            throwIAEUnsupportedMetric(annotation);
        return null;
    }

    private void throwIAEUnsupportedMetric(Annotation annotation) throws IllegalArgumentException {
        /*
         * Try and Catch to force an FFDC since the DefaultExceptionMapper is catching exceptions
         * which does not result in FFDCs. Caught Exception generates an FFDC. The subsequent
         * rethrow is caught by the DefaultExceptionMapper where it is printed out.
         */
        try {
            throw new IllegalArgumentException("Unsupported Metrics for Method [" + annotation.getClass().getName() + "]");
        } catch (IllegalArgumentException exception) {
            throw exception;
        }
    }

    /**
     * Checks whether the metric should be re-usable
     */
    private <T extends Annotation> boolean checkReusable(MetricResolver.Of<T> of) {

        // If the metric has been registered before (eg. metrics found in RequestScoped beans),
        // we don't need to worry about re-usable
        if (!of.isInitialDiscovery()) {
            return true;
        }

        String name = of.metadata().getName();
        String[] tags = of.tags();
        MetricID metricID = new MetricID(name, Utils.tagsToTags(tags));

        Metadata existingMetadata = registry.getMetadata(name);

        /*
         * If the current metadata does not equal an existing metadata (of matching name).. - Then we can NOT re-use.
         * Type check is conducted when registering/retrieving metric.
         */
        if ((existingMetadata != null && !existingMetadata.equals(of.metadata()))) {
            /*
             * Try and Catch to force an FFDC since the DefaultExceptionMapper is catching exceptions
             * which does not result in FFDCs.
             */
            try {
                throw new IllegalArgumentException("Cannot reuse metric with MetricID " + metricID.toString() + "with Metadata " + of.metadata().toString()
                                                   + ". There already exists a Metadata for this metric name with different values: " + existingMetadata.toString());
            } catch (IllegalArgumentException exception) {
                throw exception;
            }
        }
        return true;

    }

    public interface Of<T extends Annotation> {

        boolean isPresent();

        String metricName();

        String[] tags();

        Metadata metadata();

        T metricAnnotation();

        boolean isInitialDiscovery();
    }

    protected static final class DoesHaveMetric<T extends Annotation> implements Of<T> {

        private final T annotation;

        private final String name;

        /**
         * private final String[] tags;
         *
         * private final String displayName;
         *
         * private final String mbean;
         *
         * private final
         **/

        private final Metadata metadata;

        private final boolean initialDiscovery;

        private final String[] tags;

        private DoesHaveMetric(T annotation, String name, Metadata metadata, boolean initialDiscovery, String[] tags) {
            this.annotation = annotation;
            this.name = name;
            this.metadata = metadata;
            this.initialDiscovery = initialDiscovery;
            this.tags = tags;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public String metricName() {
            return name;
        }

        @Override
        public T metricAnnotation() {
            return annotation;
        }

        @Override
        public Metadata metadata() {
            return this.metadata;
        }

        @Override
        public boolean isInitialDiscovery() {
            return initialDiscovery;
        }

        @Override
        public String[] tags() {
            return tags;
        }
    }

    @Vetoed
    protected static final class DoesNotHaveMetric<T extends Annotation> implements Of<T> {

        private DoesNotHaveMetric() {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public String metricName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T metricAnnotation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Metadata metadata() {
            return null;
        }

        @Override
        public boolean isInitialDiscovery() {
            return false;
        }

        @Override
        public String[] tags() {
            return null;
        }
    }
}
