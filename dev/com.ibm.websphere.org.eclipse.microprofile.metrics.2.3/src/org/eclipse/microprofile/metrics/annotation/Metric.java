/*
 **********************************************************************
 * Copyright (c) 2017, 2019 Contributors to the Eclipse Foundation
 *               2012 Ryan W Tenney (ryan@10e.us)
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

import org.eclipse.microprofile.metrics.MetricUnits;

/**
 * An annotation requesting that a metric be injected or registered.
 * The metric will be registered in the application MetricRegistry.
 *
 * Given an injected field annotated with {@literal @}Metric like this:
 * <pre><code>
 *     {@literal @}Inject
 *     {@literal @}Metric(name="histogram")
 *     public Histogram histogram;
 * </code></pre>
 * A meter of the field's type will be created and injected into managed objects.
 * It will be up to the user to interact with the metric. This annotation
 * can be used on fields of type Meter, Timer, SimpleTimer, Counter, and Histogram.
 * <p>
 * This may also be used to register a metric.
 * </p>
 * <pre><code>
 *     {@literal @}Produces
 *     {@literal @}Metric(name="hitPercentage")
 *     {@literal @}ApplicationScoped
 *     Gauge&lt;Double&gt; hitPercentage = new Gauge&lt;Double&gt;() {
 * 
 *       {@literal @}Override
 *       public Double getValue() {
 *           return hits / total;
 *       }
 *     };
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface Metric {

    /**
     * The name of the metric.
     * @return The name of the metric.
     */
    @Nonbinding
    String name() default "";

    /**
     * The tags of the metric.
     * @return The tags of the metric. Each {@code String} tag must be in the form of 'key=value'. If the input is empty or does
     * not contain a '=' sign, the entry is ignored.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String[] tags() default {};

    /**
     * Denotes whether to use the absolute name or use the default given name relative to the annotated class.
     * @return If {@code true}, use the given name as an absolute name. If {@code false} (default),
     * use the given name relative to the annotated class.
     */
    @Nonbinding
    boolean absolute() default false;

    /**
     * The display name of the metric.
     * @return The display name of the metric.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String displayName() default "";
    
    /**
     * The description of the metric.
     * @return The description of the metric.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String description() default "";
    
    /**
     * The unit of the metric.
     * @return The unit of the metric. By default, the value is {@link MetricUnits#NONE}.
     * 
     * @see org.eclipse.microprofile.metrics.Metadata
     * @see org.eclipse.microprofile.metrics.MetricUnits
     */
    @Nonbinding
    String unit() default MetricUnits.NONE;

}
