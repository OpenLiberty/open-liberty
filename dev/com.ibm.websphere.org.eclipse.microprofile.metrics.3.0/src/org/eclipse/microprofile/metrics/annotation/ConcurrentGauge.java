/*
 * ********************************************************************
 *  Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 *  See the NOTICES file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ********************************************************************
 *
 */
package org.eclipse.microprofile.metrics.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import org.eclipse.microprofile.metrics.MetricUnits;

/**
 * An annotation for marking a method, constructor, or class as concurrent gauged.
 * The metric will be registered in the application MetricRegistry.
 *
 * A concurrent gauge has the following semantics:
 * <ul>
 *   <li>Upon entering the marked item, the value is increased.</li>
 *   <li>Upon exiting the marked item, the value is decreased.</li>
 * </ul>
 *
 * <p>
 * Given a method annotated with {@literal @}ConcurrentGauge like this:
 * </p>
 * <pre><code>
 *     {@literal @}ConcurrentGauge(name = "fancyName")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * A concurrent gauge with the fully qualified class name + {@code fancyName} will be created and each time the
 * {@code #fancyName(String)} method is invoked, the gauge will be updated.
 * Similarly, the same applies for a constructor annotated with {@literal @}ConcurrentGauge.
 *
 * <p>
 * Given a class annotated with {@literal @}ConcurrentGauge like this:
 * </p>
 * <pre><code>
 *     {@literal @}ConcurrentGauge
 *     public class CGaugedBean {
 *         public void cGaugedMethod1() {}
 *         public void cGaugedMethod2() {}
 *     }
 * </code></pre>
 *
 * A counter for the defining class will be created for each of the constructors/methods.
 * Each time the constructor/method is invoked, the respective gauge will be updated.
 *
 * This annotation will throw an IllegalStateException if the constructor/method is invoked, but the metric no
 * longer exists in the MetricRegistry.
 *
 *
 * @since 2.0
 */
@Inherited
@Documented
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD })
public @interface ConcurrentGauge {

    /**
     * The name of the concurrent gauge.
     * @return The name of the concurrent gauge.
     */
    @Nonbinding
    String name() default "";

    /**
     * The tags of the concurrent gauge.
     * @return The tags of the concurrent gauge. Each {@code String} tag must be in the form of 'key=value'. If the input is empty or does
     * not contain a '=' sign, the entry is ignored.
     *
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String[] tags() default {};

    /**
     * Denotes whether to use the absolute name or use the default given name relative to the annotated class.
     * @return If {@code true}, use the given name as an absolute name. If {@code false} (default), use the given name
     * relative to the annotated class. When annotating a class, this must be {@code false}.
     */
    @Nonbinding
    boolean absolute() default false;


    /**
     * The display name of the concurrent gauge.
     * @return The display name of the concurrent gauge.
     *
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String displayName() default "";

    /**
     * The description of the concurrent gauge.
     * @return The description of the concurrent gauge.
     *
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String description() default "";

    /**
     * The unit of the concurrent gauge.
     * @return The unit of the concurrent gauge. By default, the value is {@link MetricUnits#NONE}.
     *
     * @see org.eclipse.microprofile.metrics.Metadata
     * @see org.eclipse.microprofile.metrics.MetricUnits
     */
    @Nonbinding
    String unit() default MetricUnits.NONE;

}
