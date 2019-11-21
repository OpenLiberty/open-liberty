/*
 *******************************************************************************
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
 *******************************************************************************/

package org.eclipse.microprofile.faulttolerance;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * Define bulkhead policy to limit the number of the concurrent calls to an instance.
 * <p>
 * If this is used together with {@code Asynchronous}, it means thread isolation. 
 * 
 * Otherwise, it means semaphore isolation.
 * <ul>
 * <li> Thread isolation - execution happens on a separate thread and the concurrent requests
 * are confined in a fixed number of a thread pool.</li>
 * <li> Semaphore isolation - execution happens on the calling thread and the concurrent requests
 * are constrained by the semaphore count.</li>
 * </ul>
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@InterceptorBinding
public @interface Bulkhead {

    /**
     * Specify the maximum number of concurrent calls to an instance. The value must be greater than 0.
     * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} occurs.
     * @return the limit of the concurrent calls
     */
    @Nonbinding
    int value() default 10;
    /**
     * Specify the waiting task queue. This setting only takes effect on asynchronous invocation, achieved by using {@code Asynchronous}.
     * The value must be greater than 0.
     * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} occurs.
     * @return the waiting queue size
     */
    @Nonbinding
    int waitingTaskQueue() default 10;
}
