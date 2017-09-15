/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;

/**
 * @param <R>
 *
 */
public class RetryImpl extends net.jodah.failsafe.RetryPolicy {

    public RetryImpl(RetryPolicy policy) {
        super();
        if (policy == null) {
            withMaxRetries(0);
        } else {
            Class<? extends Throwable>[] abortOn = policy.getAbortOn();
            Duration delay = policy.getDelay();
            Duration jitter = policy.getJitter();
            Duration maxDuration = policy.getMaxDuration();
            int maxRetries = policy.getMaxRetries();
            Class<? extends Throwable>[] retryOn = policy.getRetryOn();

            if (abortOn.length > 0) {
                abortOn(abortOn);
            }

            long delayMillis = delay.toMillis();
            long jitterMillis = jitter.toMillis();
            long maxDurationMillis = maxDuration.toMillis();

            if (maxDurationMillis == 0 && jitterMillis > delayMillis) {
                jitterMillis = delayMillis; // Clamp jitter to delay to stop Failsafe sleeping for negative time
            }

            if (delayMillis > 0) {
                withDelay(delayMillis, TimeUnit.MILLISECONDS);
            }

            if (jitterMillis > 0) {
                withJitter(jitterMillis, TimeUnit.MILLISECONDS);
            }

            if (maxDurationMillis > 0) {
                withMaxDuration(maxDurationMillis, TimeUnit.MILLISECONDS);
            }

            // Pass value through directly as Failsafe interprets -1 as retry forever
            withMaxRetries(maxRetries);

            if (retryOn.length > 0) {
                retryOn(retryOn);
            }
        }
    }
}
