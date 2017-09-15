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
package com.ibm.ws.microprofile.faulttolerance.spi;

import java.time.Duration;

/**
 * The Retry annotation to define the number of the retries and the fallback method on reaching the
 * retry counts.
 */
public interface RetryPolicy {

    /**
     * @return The max number of retries. -1 means retry forever. If less than -1, an IllegalArgumentException will be thrown.
     *
     */
    public int getMaxRetries();

    public void setMaxRetries(int maxRetries);

    /**
     * The delay between retries. Defaults to 0.
     *
     * @return the delay time
     */
    public Duration getDelay();

    public void setDelay(Duration delay);

    /**
     * @return the maximum duration to perform retries for.
     */
    public Duration getMaxDuration();

    public void setMaxDuration(Duration maxDuration);

    /**
     *
     * @return the jitter that randomly vary retry delays by. e.g. a jitter of 200 milliseconds
     *         will randomly add between -200 and 200 milliseconds to each retry delay.
     */
    public Duration getJitter();

    public void setJitter(Duration jitter);

    /**
     *
     * @return Specify the failure to retry on
     */
    public Class<? extends Throwable>[] getRetryOn();

    @SuppressWarnings("unchecked")
    public void setRetryOn(Class<? extends Throwable>... retryOn);

    /**
     *
     * @return Specify the failure to abort on
     */
    public Class<? extends Throwable>[] getAbortOn();

    @SuppressWarnings("unchecked")
    public void setAbortOn(Class<? extends Throwable>... abortOn);
}
