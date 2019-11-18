/*
 * Copyright (c) 2017-2019 Contributors to the Eclipse Foundation
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
 * Defines a circuit breaker policy to an individual method or a class.
 * <p>
 * A circuit breaker aims to prevent further damage by not executing functionality that is doomed to fail.
 * After a failure situation has been detected, circuit breakers prevent methods from being executed and
 * instead throw exceptions immediately. After a certain delay or wait time, the functionality is attempted to
 * be executed again.
 * <p>
 * A circuit breaker can be in one of the following states:
 * <ul>
 * <li>
 * <i>Closed:</i> In normal operation, the circuit is closed. If a failure occurs, the Circuit Breaker records the event.
 * In closed state the {@code requestVolumeThreshold} and {@code failureRatio} parameters may be configured in order to specify
 * the conditions under which the breaker will transition the circuit to open. If the failure conditions are met, the circuit
 * will be opened.
 * </li>
 * <li>
 * <i>Open:</i> When the circuit is open, calls to the service operating under the circuit breaker will fail immediately.
 * A delay may be configured for the circuit breaker. After the specified delay, the circuit transitions to half-open state.
 * </li>
 * <li>
 * <i>Half-open:</i> In half-open state, trial executions of the service are allowed. By default one trial call to the
 * service is permitted. If the call fails, the circuit will return to open state. The {@code successThreshold} parameter allows the
 * configuration of the number of trial executions that must succeed before the circuit can be closed. After the specified
 * number of successful executions, the circuit will be closed. If a failure occurs before the successThreshold is reached
 * the circuit will transition to open.
 * </li>
 * </ul>
 * Circuit state transitions will reset the circuit breaker's records.
 * <p>
 * When a method returns a result, the following rules are applied to determine whether the result is a success or a failure:
 * <ul>
 * <li>If the method does not throw a {@link Throwable}, it is considered a success
 * <li>Otherwise, if the thrown object is assignable to any value in the {@link #skipOn()} parameter, is is considered a success
 * <li>Otherwise, if the thrown object is assignable to any value in the {@link #failOn()} parameter, it is considered a failure
 * <li>Otherwise it is considered a success
 * </ul>
 * If a method throws a {@link Throwable} which is not an {@link Error} or {@link Exception}, non-portable behavior results.
 *
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CircuitBreaker {

    /**
     * The list of exception types which should be considered failures
     * <p>
     * Note that if a method throws a {@link Throwable} which is not an {@link Error} or {@link Exception}, non-portable behavior
     * results.
     *
     * @return the exception types which should be considered failures
     */
    @Nonbinding
    Class<? extends Throwable>[] failOn() default {Throwable.class};
    
    /**
     * The list of exception types which should not be considered failures
     * <p>
     * This list takes priority over the types listed in {@link #failOn}
     * <p>
     * Note that if a method throws a {@link Throwable} which is not an {@link Error} or {@link Exception}, non-portable behavior results.
     *
     * @return the exception types which should not be considered failures
     */
    @Nonbinding
    Class<? extends Throwable>[] skipOn() default {};


    /**
     * The delay after which an open circuit will transitions to half-open state.
     * <p>
     * The amount of delay is taken from this delay value and the {@code delayUnit}, and defaults to five seconds. The
     * value must be greater than or equal to {@code 0}. {@code 0} means no delay.
     *
     * @return The delay time after which an open circuit transitions to half-open state
     */
    @Nonbinding
    long delay() default 5000;

    /**
     * The unit of the delay after which an open circuit will transitions to half-open state.
     *
     * @return The unit of the delay
     * @see CircuitBreaker#delay()
     */
    @Nonbinding
    ChronoUnit delayUnit() default ChronoUnit.MILLIS;

    /**
     * The number of consecutive requests in a rolling window.
     * <p>
     * The circuit breaker will trip if the number of failures exceed the {@code failureRatio} within the rolling window
     * of consecutive requests. The value must be greater than or equal to {@code 1}.
     *
     * @return The number of the consecutive requests in a rolling window
     */
    @Nonbinding
    int requestVolumeThreshold() default 20;

    /**
     * The ratio of failures within the rolling window that will trip the circuit to open.
     * <p>
     * The circuit breaker will trip if the number of failures exceed the {@code failureRatio} within the rolling window
     * of consecutive requests. For example, if the {@code requestVolumeThreshold} is {@code 20} and {@code failureRatio}
     * is {@code .50}, ten or more failures in 20 consecutive requests will trigger the circuit to open. The value must
     * be between {@code 0} and {@code 1} inclusive.
     *
     * @return The failure ratio threshold
     */
    @Nonbinding
    double failureRatio() default .50;

    /**
     * The number of successful executions, before a half-open circuit is closed again.
     * <p>
     * A half-open circuit will be closed once {@code successThreshold} executions were made without failures.
     * If a failure occurs while in half-open state the circuit is immediately opened again. The value must be greater
     * than or equal to {@code 1}.
     *
     * @return The success threshold to fully close the circuit
     */
    @Nonbinding
    int successThreshold() default 1;

}
