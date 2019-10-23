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
package io.astefanutti.metrics.cdi;

import static io.astefanutti.metrics.cdi.MetricsParameter.useAbsoluteName;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;

@Vetoed
/* package-private */ public class SeMetricName implements MetricName {

    private final Set<MetricsParameter> parameters;

    public SeMetricName(Set<MetricsParameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String of(InjectionPoint ip) {
        Annotated annotated = ip.getAnnotated();
        if (annotated instanceof AnnotatedMember)
            return of((AnnotatedMember<?>) annotated);
        else if (annotated instanceof AnnotatedParameter)
            return of((AnnotatedParameter<?>) annotated);
        else
            throw new IllegalArgumentException("Unable to retrieve metric name for injection point [" + ip + "], only members and parameters are supported");
    }

    @Override
    public String of(AnnotatedMember<?> member) {
        if (member.isAnnotationPresent(Metric.class)) {
            Metric metric = member.getAnnotation(Metric.class);
            String name = (metric.name().isEmpty()) ? member.getJavaMember().getName() : of(metric.name());
            return metric.absolute() | parameters.contains(useAbsoluteName) ? name : MetricRegistry.name(member.getJavaMember().getDeclaringClass(), name);
        } else {
            return parameters.contains(useAbsoluteName) ? member.getJavaMember().getName() : MetricRegistry.name(member.getJavaMember().getDeclaringClass(),
                                                                                                                 member.getJavaMember().getName());
        }
    }

    @Override
    public String of(String attribute) {
        return attribute;
    }

    private String of(AnnotatedParameter<?> parameter) {
        if (parameter.isAnnotationPresent(Metric.class)) {
            Metric metric = parameter.getAnnotation(Metric.class);
            String name = (metric.name().isEmpty()) ? getParameterName(parameter) : of(metric.name());
            return metric.absolute() | parameters.contains(useAbsoluteName) ? name : MetricRegistry.name(parameter.getDeclaringCallable().getJavaMember().getDeclaringClass(),
                                                                                                         name);
        } else {
            return parameters.contains(useAbsoluteName) ? getParameterName(parameter) : MetricRegistry.name(parameter.getDeclaringCallable().getJavaMember().getDeclaringClass(),
                                                                                                            getParameterName(parameter));
        }
    }

    // Let's rely on reflection to retrieve the parameter name until Java 8 is required.
    // To be refactored eventually when CDI SPI integrate JEP-118.
    // See http://openjdk.java.net/jeps/118
    // And http://docs.oracle.com/javase/tutorial/reflect/member/methodparameterreflection.html
    // TODO: move into a separate metric name strategy
    private String getParameterName(AnnotatedParameter<?> parameter) {
        try {
            Method method = Method.class.getMethod("getParameters");
            Object[] parameters = (Object[]) method.invoke(parameter.getDeclaringCallable().getJavaMember());
            Object param = parameters[parameter.getPosition()];
            Class<?> Parameter = Class.forName("java.lang.reflect.Parameter");
            if ((Boolean) Parameter.getMethod("isNamePresent").invoke(param))
                return (String) Parameter.getMethod("getName").invoke(param);
            else
                throw new UnsupportedOperationException("Unable to retrieve name for parameter [" + parameter
                                                        + "], activate the -parameters compiler argument or annotate the injected parameter with the @Metric annotation");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException cause) {
            throw new UnsupportedOperationException("Unable to retrieve name for parameter [" + parameter
                                                    + "], @Metric annotation on injected parameter is required before Java 8");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Metadata metadataOf(InjectionPoint ip, Class<?> type) {
        Annotated annotated = ip.getAnnotated();
        String name = of(ip);
        return metadataOf(annotated, name, type);
    }

    @Override
    public String[] tagOf(InjectionPoint ip) {
        Annotated annotated = ip.getAnnotated();
        return tagOf(annotated);
    }

    /** {@inheritDoc} */
    @Override
    public Metadata metadataOf(AnnotatedMember<?> member) {
        // getBaseType is a reflection proxy class, we can't directly compare the classes.
        // If type can be determined more efficiently, we can remove the overloaded methods that pass in class
        String typeName = member.getBaseType().getTypeName();
        if (typeName.startsWith(Gauge.class.getName())) {
            return metadataOf(member, Gauge.class);
        } else if (typeName.startsWith(Counter.class.getName())) {
            return metadataOf(member, Counter.class);
        } else if (typeName.startsWith(Meter.class.getName())) {
            return metadataOf(member, Meter.class);
        } else if (typeName.startsWith(Histogram.class.getName())) {
            return metadataOf(member, Histogram.class);
        } else if (typeName.startsWith(Timer.class.getName())) {
            return metadataOf(member, Timer.class);
        }
        return null;
    }

    public Metadata metadataOf(Annotated annotated, String name, Class<?> type) {

        MetadataBuilder mdb = Metadata.builder().withName(name).withType(MetricType.from(type));
        if (annotated.isAnnotationPresent(Metric.class)) {
            Metric metric = annotated.getAnnotation(Metric.class);

            String val = null;
            mdb = ((val = metadataValueOf(metric.description())) == null) ? mdb : mdb.withDescription(val);
            mdb = ((val = metadataValueOf(metric.displayName())) == null) ? mdb : mdb.withDisplayName(val);
            mdb = ((val = metadataValueOf(metric.unit())) == null) ? mdb : mdb.withUnit(val);

        }
        return mdb.build();
    }

    public String[] tagOf(Annotated annotated) {
        String[] returnStringArray = null;
        if (annotated.isAnnotationPresent(Metric.class)) {
            Metric metric = annotated.getAnnotation(Metric.class);
            returnStringArray = metric.tags();
        }
        return returnStringArray;
    }

    /**
     * Since annotations have the default value of "", convert it to null.
     *
     * @param value
     * @return The value of the metadata field if present, null otherwise
     */
    private String metadataValueOf(String value) {
        if (value == null || value.trim().isEmpty())
            return null;
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Metadata metadataOf(AnnotatedMember<?> member, Class<?> type) {
        String name = of(member);
        return metadataOf(member, name, type);
    }

    @Override
    public String[] tagOf(AnnotatedMember<?> member) {
        return tagOf((Annotated) member);
    }
}
