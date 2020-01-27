/*
 **********************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *               2010-2013 Coda Hale, Yammer.com
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************************/
package org.eclipse.microprofile.metrics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * An annotation for marking a method or field as a gauge.
 * The metric will be registered in the application MetricRegistry.
 * 
 * <p>
 * Given a method annotated with {@literal @}Gauge like this:
 * </p>
 * <pre><code>
 *     {@literal @}Gauge(name = "queueSize")
 *     public int getQueueSize() {
 *         return queue.size;
 *     }
 * </code></pre>
 * A gauge with the fully qualified class name + {@code queueSize} will be created which uses the
 * annotated method's return value as its value.
 * 
 * <p>
 * Given a field annotated with {@literal @}Gauge like this:
 * </p>
 * <pre><code>
 *     {@literal @}Gauge
 *     long value;
 * </code></pre>
 * A gauge with the fully qualified class name + {@code value} will be created which uses the
 * annotated field value as its value.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface Gauge {

    /**
     * The name of the gauge.
     * @return The name of the gauge.
     */
    @Nonbinding
    String name() default "";

    /**
     * The tags of the gauge.
     * @return The tags of the gauge. Each {@code String} tag must be in the form of 'key=value'. If the input is empty or does
     * not contain a '=' sign, the entry is ignored.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String[] tags() default {};

    /**
     * Denotes whether to use the absolute name or use the default given name relative to the annotated class.
     * @return If {@code true}, use the given name as an absolute name. If {@code false} (default), use the given name
     * relative to the annotated class.
     */
    @Nonbinding
    boolean absolute() default false;
    
    
    /**
     * The human readable display name of the gauge.
     * @return The display name of the gauge.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String displayName() default "";
    
    /**
     * The description of the gauge.
     * @return The description of the gauge.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String description() default "";
    
    
    /**
     * The unit of the gauge.
     * @return (Required) The unit of the gauge.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     * @see org.eclipse.microprofile.metrics.MetricUnits
     */
    @Nonbinding
    String unit();

}
