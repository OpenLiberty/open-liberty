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
 * The Retry annotation to define the number of the retries and the fallback method on reaching the
 * retry counts. Any invalid config value causes 
 * {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException}.
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author John Ament
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE})
@InterceptorBinding
public @interface Retry {

    /**
     * @return The max number of retries. -1 means retry forever. The value must be greater than or equal to -1.
     *
     */
    @Nonbinding
    int maxRetries() default 3;

    /**
     * The delay between retries. Defaults to 0. The value must be greater than or equal to 0.
     * @return the delay time
     */
    @Nonbinding
    long delay() default 0;

    /**
     *
     * @return the delay unit
     */
    @Nonbinding
    ChronoUnit delayUnit() default ChronoUnit.MILLIS;

    /**
     * The max duration. The max duration must be greater than the delay duration if set. 0 means not set.
     * @return the maximum duration to perform retries for.
     */
    @Nonbinding
    long maxDuration() default 180000;

    /**
     *
     * @return the duration unit
     */
    @Nonbinding
    ChronoUnit durationUnit() default ChronoUnit.MILLIS;

    /**
     * Set the jitter to randomly vary retry delays for. The value must be greater than or equals to 0. 
     * 0 means not set.
     * @return the jitter that randomly vary retry delays by. e.g. a jitter of 200 milliseconds
     * will randomly add between -200 and 200 milliseconds to each retry delay.
     */
    @Nonbinding
    long jitter() default 200;

    /**
     *
     * @return the jitter delay unit.
     */
    @Nonbinding
    ChronoUnit jitterDelayUnit() default ChronoUnit.MILLIS;


    /**
     *
     * @return Specify the failure to retry on
     */
    @Nonbinding
    Class<? extends Throwable>[] retryOn() default { Exception.class };

    /**
     *
     * @return Specify the failure to abort on
     */
    @Nonbinding
    Class<? extends Throwable>[] abortOn() default {};

}
