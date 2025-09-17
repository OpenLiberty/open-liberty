/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;

import com.ibm.websphere.simplicity.log.Log;

/**
 * A wrapper for the liberty-properties-decrypter external test service.
 *
 * This wrapper is used by other external test services to decrypt sensitive properties at test runtime.
 */
public class ExternalTestServiceDecrypter {

    private static final Class<?> c = ExternalTestServiceDecrypter.class;

    // Encrypted prefix to distinguish encrypted vs unencrypted properties
    private static final String ENCRYPTED_PREFIX = "encrypted!";

    // Property for GHE access token
    private static final String PROP_ACCESS_TOKEN = "global.ghe.access.token";

    // The wrapped decrypted service instance stored in a concurrent hash map for lazily-synchronized initialization
    private static final ConcurrentHashMap<String, ExternalTestService> DECRYPTER_SERVICE = new ConcurrentHashMap<>();

    // The service key used to maintain a singleton instance of the decrypter service in the ConcurrentHashMap
    private static final String SERVICE_KEY = "SINGLETON";

    // A global retry policy for getting and replacing decrypter services
    // Max wait time will be 144 seconds
    private static final RetryPolicy DECRYPTER_SERVICE_RETRY = new FibonacciRetryPolicy(Duration.ofSeconds(1), 11);

    private ExternalTestServiceDecrypter() {
        //private constructor - use decrypt method
    }

    /**
     * Function that finds a new decrypter service
     */
    private static final Function<String, ExternalTestService> INITALIZER = key -> {
        ExternalTestService instance = null;
        while (instance == null) {
            try {
                instance = ExternalTestService.getService("liberty-properties-decrypter");
            } catch (Exception e) {
                if (DECRYPTER_SERVICE_RETRY.retryable("INITALIZER", e.getMessage())) { // Wait and continue
                    continue;
                } else {
                    throw new RuntimeException("Could not get a liberty-properties-decrypter service", e);
                }
            }
        }
        return instance;
    };

    /**
     * Function that replaces an existing decrypter service, that has been reported unhealthy
     */
    private static final BiFunction<String, ExternalTestService, ExternalTestService> RE_INITALIZER = (key, current) -> {
        ExternalTestService instance = null;
        while (instance == null) {
            try {
                instance = ExternalTestService.getService("liberty-properties-decrypter");
            } catch (Exception e) {
                if (DECRYPTER_SERVICE_RETRY.retryable("RE_INITALIZER", e.getMessage())) { // Wait and continue
                    continue;
                } else {
                    throw new RuntimeException("Could not get a liberty-properties-decrypter service", e);
                }
            }
        }
        return instance;
    };

    /**
     * Uses the decrypter external test service to decrypt the provided property
     *
     * @param  property              the property to be decrypted
     * @return                       returns the original property if the property was not encrypted,
     *                               otherwise returns the decrypted property.
     *
     * @throws IllegalStateException if the access token was not set, or if the decypter service rejected the token
     * @throws NullPointerException  if the decrypter service was was unreachable after a series of retries
     */
    public static String decrypt(String property) {
        final String m = "decrypt";

        // Do not decrypt property if it is not encrypted
        if (!property.startsWith(ENCRYPTED_PREFIX)) {
            return property;
        }

        // Ensure access token is available
        final String accessToken = findAccessToken();

        // Get a decrypter service, new or existing
        ExternalTestService decrypter = DECRYPTER_SERVICE.computeIfAbsent(SERVICE_KEY, INITALIZER);

        // Set retry policy for decrypting a property, to account for occasional network issues.
        final RetryPolicy retry = new CounterRetryPolicy(5);

        String decrypted = null;
        while (decrypted == null) {
            // Create request to decrypter service
            final String URL = "https://" + decrypter.getAddress() + ":" + decrypter.getPort() + "/decrypt?value=" + property + "&access_token=" + accessToken;
            final HttpsRequest request = new HttpsRequest(URL)
                            .allowInsecure()
                            .timeout(10000)
                            .expectCode(HttpsURLConnection.HTTP_OK)
                            .expectCode(HttpsURLConnection.HTTP_UNAUTHORIZED)
                            .expectCode(HttpsURLConnection.HTTP_FORBIDDEN)
                            .silent();

            try {
                decrypted = request.run(String.class);
            } catch (Exception e) {

                // The decrypter threw an unexpected exception, mark it unhealthy
                ExternalTestServiceReporter.reportUnhealthy(decrypter, e);

                //Should we retry
                if (retry.retryable(m, e.toString())) {
                    // If so, then compute a new decrypter, then
                    // continue on to try again using a different decrypter service.
                    decrypter = DECRYPTER_SERVICE.compute(SERVICE_KEY, RE_INITALIZER);
                    continue;
                } else {
                    // If not, remove the unhealthy decrypter, then
                    // break out of the while loop
                    DECRYPTER_SERVICE.remove(SERVICE_KEY);
                    decrypter = null;
                    break;
                }
            }

            // Handle expected response codes
            switch (request.getResponseCode()) {
                case HttpsURLConnection.HTTP_UNAUTHORIZED:
                    throw new IllegalStateException(PROP_ACCESS_TOKEN
                                                    + " is not recognized by github.ibm.com, see "
                                                    + "https://github.ibm.com/websphere/WS-CD-Open/wiki/Automated-Tests#running-fats-that-use-secure-properties-locally "
                                                    + "for more info");
                case HttpsURLConnection.HTTP_FORBIDDEN:
                    throw new IllegalStateException(PROP_ACCESS_TOKEN
                                                    + " is not able to be access organisation data, Access Token requires read:org permission, see "
                                                    + "https://github.ibm.com/websphere/WS-CD-Open/wiki/Automated-Tests#running-fats-that-use-secure-properties-locally "
                                                    + "for more info");
                case HttpsURLConnection.HTTP_OK:
                    //Do nothing
            }
        }

        return Objects.requireNonNull(decrypted, "Unable to decrypt the provided property. See earlier logs for failed decryption attempts.");
    }

    /**
     * Helper method that searches for the access token system property
     *
     * @throws IllegalStateException if no access token was set
     */
    private static String findAccessToken() {
        // Identify Access Token
        String accessToken = System.getProperty(PROP_ACCESS_TOKEN);
        if (accessToken == null) {
            throw new IllegalStateException("Missing Property called: '" + PROP_ACCESS_TOKEN
                                            + "', this property is needed to decrypt secure properties, see "
                                            + "https://github.ibm.com/websphere/WS-CD-Open/wiki/Automated-Tests#running-fats-that-use-secure-properties-locally "
                                            + "for more info");
        }
        return accessToken;
    }

    /**
     * Retry policy interface
     */
    private interface RetryPolicy {

        /**
         * @param  method calling method
         * @param  reason reason for the retury
         * @return        true if the operation should be retried; false otherwise.
         */
        boolean retryable(String method, String reason);

    }

    /**
     * Retry with increasing waits based on the fibonacci sequence until
     * a maximum number of retries have elapsed.
     */
    private static class FibonacciRetryPolicy implements RetryPolicy {

        // Track fibonacci numbers
        private int previous = 1;
        private int current = 1;

        // Track number of sleeps
        private final AtomicInteger counter = new AtomicInteger(0);

        // Track sleep duration, count
        private final Duration sleep;
        private final int maxRetries;

        /**
         * Construct retry policy
         *
         * @param sleep      the scale in which the increasing wait is calculated
         * @param maxRetries how many times we will allow this policy to retry
         */
        public FibonacciRetryPolicy(Duration sleep, int maxRetries) {
            Objects.requireNonNull(sleep);
            this.sleep = sleep;
            this.maxRetries = maxRetries;
        }

        @Override
        public boolean retryable(String method, String reason) {
            if (counter.getAndIncrement() >= maxRetries) {
                Log.info(c, method, "Cannot allow retry(" + counter.get() + "), instead allow failure due to: " + reason);
                return false;
            }

            long sleepTime = sleep.toMillis() * current;

            Log.info(c, method, "Allow retry(" + counter.get() + ") after sleeping for " + sleepTime + "ms.  Retry called because: " + reason);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // Thread was interrupted, ignore it and continue on...
            }

            // Calculate next fibbonnaci numbers
            int temp = current;
            current = previous + current;
            previous = temp;

            return true;
        }
    }

    private static class CounterRetryPolicy implements RetryPolicy {
        // Track number of retries
        private final AtomicInteger counter = new AtomicInteger(0);

        private final int maxRetries;

        public CounterRetryPolicy(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public boolean retryable(String method, String reason) {
            if (counter.getAndIncrement() >= maxRetries) {
                Log.info(c, method, "Cannot allow retry(" + counter.get() + "), instead allow failure due to: " + reason);
                return false;
            }

            Log.info(c, method, "Allow retry(" + counter.get() + ").  Retry called because: " + reason);
            return true;
        }
    }
}
