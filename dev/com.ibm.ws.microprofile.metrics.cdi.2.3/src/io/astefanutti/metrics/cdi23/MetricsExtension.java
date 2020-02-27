/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.metrics.cdi.binding.MetricsBinding;
import com.ibm.ws.microprofile.metrics.cdi.decorator.AnnotatedTypeDecorator;
import com.ibm.ws.microprofile.metrics.cdi23.helper.Utils;
import com.ibm.ws.microprofile.metrics.cdi23.producer.MetricRegistryFactory;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

@Component(service = WebSphereCDIExtension.class, immediate = true)
public class MetricsExtension implements Extension, WebSphereCDIExtension {
    protected static final AnnotationLiteral<Nonbinding> NON_BINDING = new AnnotationLiteral<Nonbinding>() {
    };

    protected static final AnnotationLiteral<InterceptorBinding> INTERCEPTOR_BINDING = new AnnotationLiteral<InterceptorBinding>() {
    };

    protected static final AnnotationLiteral<MetricsBinding> METRICS_BINDING = new AnnotationLiteral<MetricsBinding>() {
    };

    protected static final AnnotationLiteral<Default> DEFAULT = new AnnotationLiteral<Default>() {
    };

    protected final Map<Bean<?>, AnnotatedMember<?>> metrics = new HashMap<>();

    protected final Set<MetricID> metricIDs = Collections.synchronizedSortedSet(new TreeSet<MetricID>());

    /**
     * Stores the member/annotation that were intercepted to their metric name.
     */
    protected final Map<Member, Map<Annotation, String>> memberMap = Collections.synchronizedMap(new HashMap<Member, Map<Annotation, String>>());

    /**
     * Stores the CDI bean classes that were already visited, so we can skip re-registering them
     */
    private final Set<Class<?>> beansVisited = Collections.synchronizedSet(new HashSet<Class<?>>());

    @Reference
    public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        MetricRegistryFactory.SHARED_METRIC_REGISTRIES = sharedMetricRegistry;
    }

    protected <X> void metricsAnnotations(@Observes @WithAnnotations({ Counted.class, Gauge.class, Metered.class, Timed.class,
                                                                       ConcurrentGauge.class, SimplyTimed.class }) ProcessAnnotatedType<X> pat) {
        pat.setAnnotatedType(new AnnotatedTypeDecorator<>(pat.getAnnotatedType(), METRICS_BINDING));
    }

    protected void metricProducerField(@Observes ProcessProducerField<? extends Metric, ?> ppf) {
        metrics.put(ppf.getBean(), ppf.getAnnotatedProducerField());
    }

    protected void metricProducerMethod(@Observes ProcessProducerMethod<? extends Metric, ?> ppm) {
        // Skip the Metrics CDI alternatives
        if (!ppm.getBean().getBeanClass().equals(MetricProducer.class)) {
            metrics.put(ppm.getBean(), ppm.getAnnotatedProducerMethod());
        }
    }

    protected void defaultMetricRegistry(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        if (manager.getBeans(MetricRegistry.class).isEmpty())
            abd.addBean(new MetricRegistryBean(manager));
    }

    @SuppressWarnings("unchecked")
    protected <T extends Metric> void configuration(@Observes AfterDeploymentValidation adv, BeanManager manager) {

        MetricRegistry registry = getReference(manager, MetricRegistry.class);

        MetricName name = getReference(manager, MetricName.class);
        for (Map.Entry<Bean<?>, AnnotatedMember<?>> bean : metrics.entrySet()) {
            // TODO: add MetricSet metrics into the metric registry
            if (
            // skip non @Default beans
            !bean.getKey().getQualifiers().contains(DEFAULT)
                // skip producer methods with injection point
                || hasInjectionPoints(bean.getValue()))
                continue;
            Metadata metadata = name.metadataOf(bean.getValue());
            String[] tags = name.tagOf(bean.getValue());

            registry.register(metadata, (Metric) getReference(manager, bean.getValue().getBaseType(), bean.getKey()), Utils.tagsToTags(tags));

            MetricID mid = new MetricID(metadata.getName(), Utils.tagsToTags(tags));
            addMetricID(mid);
        }

        //Clear the collected metric producers
        metrics.clear();
    }

    protected void beforeShutdown(@Observes BeforeShutdown shutdown) {
        MetricRegistry registry = MetricRegistryFactory.getApplicationRegistry();
        // Unregister metrics
        for (MetricID mid : metricIDs) {
            registry.remove(mid);
        }
    }

    protected static <T> T getReference(BeanManager manager, Class<T> type) {
        return getReference(manager, type, manager.resolve(manager.getBeans(type)));
    }

    @SuppressWarnings("unchecked")
    protected static <T> T getReference(BeanManager manager, Type type, Bean<?> bean) {
        return (T) manager.getReference(bean, type, manager.createCreationalContext(bean));
    }

    protected static boolean hasInjectionPoints(AnnotatedMember<?> member) {
        if (!(member instanceof AnnotatedMethod))
            return false;
        AnnotatedMethod<?> method = (AnnotatedMethod<?>) member;
        for (AnnotatedParameter<?> parameter : method.getParameters()) {
            if (parameter.getBaseType().equals(InjectionPoint.class))
                return true;
        }
        return false;
    }

    public String getMetricNameForMember(Member member, Annotation annotation) {
        Map<Annotation, String> map = memberMap.get(member);
        if (map == null)
            return null;
        return map.get(annotation);
    }

    public void addMetricID(Member member, Annotation annotation, MetricID mid) {
        Map<Annotation, String> map = memberMap.get(member);
        String name = mid.getName();
        if (map == null) {
            map = Collections.synchronizedMap(new HashMap<Annotation, String>());
            memberMap.put(member, map);
        }
        map.put(annotation, name);
        metricIDs.add(mid);
    }

    public void addMetricID(MetricID mid) {
        metricIDs.add(mid);
    }

    public Set<Class<?>> getBeansVisited() {
        return beansVisited;
    }

}
