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
package com.ibm.ws.microprofile.metrics.cdi30.interceptors;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.ibm.ws.microprofile.metrics.cdi30.helper.Utils;

import io.astefanutti.metrics.cdi30.MetricResolver;

@Timed
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
public class TimedInterceptor {

    private final Bean<?> bean;

    private final MetricRegistry registry;

    private final MetricResolver resolver;

    @Inject
    private TimedInterceptor(@Intercepted Bean<?> bean, MetricRegistry registry, MetricResolver resolver) {
        this.bean = bean;
        this.registry = registry;
        this.resolver = resolver;
    }

    @AroundConstruct
    private Object timedConstructor(InvocationContext context) throws Exception {
        return timedCallable(context, context.getConstructor());
    }

    @AroundInvoke
    private Object timedMethod(InvocationContext context) throws Exception {
        return timedCallable(context, context.getMethod());
    }

    @AroundTimeout
    private Object timedTimeout(InvocationContext context) throws Exception {
        return timedCallable(context, context.getMethod());
    }

    private <E extends Member & AnnotatedElement> Object timedCallable(InvocationContext context, E element) throws Exception {
        MetricResolver.Of<Timed> timed = resolver.timed(bean.getBeanClass(), element);
        MetricID MetricID = new MetricID(timed.metricName(), Utils.tagsToTags(timed.tags()));
        Timer timer = (Timer) registry.getMetric(MetricID);
        if (timer == null)
            throw new IllegalStateException("No timer with metricID [" + MetricID + "] found in registry [" + registry + "]");

        Timer.Context time = timer.time();
        try {
            return context.proceed();
        } finally {
            time.stop();
        }
    }
}
