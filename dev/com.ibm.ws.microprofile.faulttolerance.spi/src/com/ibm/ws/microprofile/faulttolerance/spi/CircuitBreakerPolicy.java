/*
 * Copyright (c) 2017, 2019 Contributors to the Eclipse Foundation
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
package com.ibm.ws.microprofile.faulttolerance.spi;

import java.time.Duration;

/**
 * Define the Circuit Breaker policy
 */
public interface CircuitBreakerPolicy {

    /**
     * Define the failure criteria
     *
     * @return the failure exception
     */
    public Class<? extends Throwable>[] getFailOn();

    @SuppressWarnings("unchecked")
    public void setFailOn(Class<? extends Throwable>... failOn);

    /**
     * Define the skip criteria
     *
     * @return the skip exception
     */
    Class<? extends Throwable>[] getSkipOn();

    @SuppressWarnings("unchecked")
    public void setSkipOn(Class<? extends Throwable>... skipOn);

    /**
     *
     * @return The delay time after the circuit is open
     */
    public Duration getDelay();

    public void setDelay(Duration delay);

    /**
     * The number of consecutive requests in a rolling window
     * that will trip the circuit.
     *
     * @return the number of the consecutive requests in a rolling window
     *
     */
    public int getRequestVolumeThreshold();

    public void setRequestVolumeThreshold(int threshold);

    /**
     * The failure threshold to trigger the circuit to open.
     * e.g. if the requestVolumeThreshold is 20 and failureRation is .50,
     * more than 10 failures in 20 consecutive requests will trigger
     * the circuit to open.
     *
     * @return The failure threshold to open the circuit
     */
    public double getFailureRatio();

    public void setFailureRatio(double ratio);

    /**
     * For an open circuit, after the delay period is reached, once the successThreshold
     * is reached, the circuit is back to close again.
     *
     * @return The success threshold to fully close the circuit
     */
    public int getSuccessThreshold();

    public void setSuccessThreshold(int threshold);

}
