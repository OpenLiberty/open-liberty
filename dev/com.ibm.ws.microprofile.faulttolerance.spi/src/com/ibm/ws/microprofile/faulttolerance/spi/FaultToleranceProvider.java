/*
 * Copyright (c) 2016,2017 Contributors to the Eclipse Foundation
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

public final class FaultToleranceProvider {
    private static final FaultToleranceProviderResolver INSTANCE = FaultToleranceProviderResolver.instance();

    public static RetryPolicy newRetryPolicy() {
        return INSTANCE.newRetryPolicy();
    }

    public static CircuitBreakerPolicy newCircuitBreakerPolicy() {
        return INSTANCE.newCircuitBreakerPolicy();
    }

    public static BulkheadPolicy newBulkheadPolicy() {
        return INSTANCE.newBulkheadPolicy();
    }

    public static FallbackPolicy newFallbackPolicy() {
        return INSTANCE.newFallbackPolicy();
    }

    public static TimeoutPolicy newTimeoutPolicy() {
        return INSTANCE.newTimeoutPolicy();
    }

    public static <T, R> ExecutorBuilder<T, R> newExecutionBuilder() {
        return INSTANCE.newExecutionBuilder();
    }
}
