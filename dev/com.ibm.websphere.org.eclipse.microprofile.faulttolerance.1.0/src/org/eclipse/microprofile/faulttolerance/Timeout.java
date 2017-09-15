/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
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
 */
package org.eclipse.microprofile.faulttolerance;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * The annotation to define a method execution timeout.
 *
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE})
@InterceptorBinding
public @interface Timeout {

    /**
     * The timeout value. The value must be greater than or equal to 0. 0 means no timeout configured.
     * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} occurs.
     * @return the timeout value
     */
    @Nonbinding
    long value() default 1000;

    /**
     *
     * @return the timeout unit
     */
    @Nonbinding
    ChronoUnit unit() default ChronoUnit.MILLIS;

}
