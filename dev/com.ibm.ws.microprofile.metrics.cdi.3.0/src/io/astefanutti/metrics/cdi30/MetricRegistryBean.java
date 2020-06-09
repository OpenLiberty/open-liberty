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
package io.astefanutti.metrics.cdi30;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.metrics.MetricRegistry;

import com.ibm.ws.microprofile.metrics23.impl.MetricRegistry23Impl;

public final class MetricRegistryBean implements Bean<MetricRegistry23Impl>, PassivationCapable {

    private final Set<Annotation> qualifiers = new HashSet<>(Arrays.<Annotation> asList(new AnnotationLiteral<Any>() {
    }, new AnnotationLiteral<Default>() {
    }));

    private final Set<Type> types;

    private final InjectionTarget<MetricRegistry23Impl> target;

    public MetricRegistryBean(BeanManager manager) {
        AnnotatedType<MetricRegistry23Impl> annotatedType = manager.createAnnotatedType(MetricRegistry23Impl.class);
        this.types = annotatedType.getTypeClosure();
        this.target = manager.createInjectionTarget(annotatedType);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.unmodifiableSet(qualifiers);
    }

    @Override
    public MetricRegistry23Impl create(CreationalContext<MetricRegistry23Impl> context) {
        MetricRegistry23Impl registry = target.produce(context);
        target.inject(registry, context);
        target.postConstruct(registry);
        context.push(registry);
        return registry;
    }

    @Override
    public void destroy(MetricRegistry23Impl instance, CreationalContext<MetricRegistry23Impl> context) {
        target.preDestroy(instance);
        target.dispose(instance);
        context.release();
    }

    @Override
    public Class<MetricRegistry> getBeanClass() {
        return MetricRegistry.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return "metric-registry";
    }

    @Override
    public String toString() {
        return "Default Metric Registry Bean";
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public String getId() {
        return getClass().getName();
    }
}
