/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.utils.zipkin;

import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonValue;

import org.hamcrest.Matcher;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.HttpRequest;

/**
 * Client for retrieving spans from the Zipkin trace server
 * <p>
 * This calls the HTTP API documented at https://zipkin.io/zipkin-api/#/
 */
public class ZipkinQueryClient {

    private static final Class<ZipkinQueryClient> c = ZipkinQueryClient.class;

    private final String baseUrl;

    public ZipkinQueryClient(ZipkinContainer container) {
        baseUrl = container.getApiBaseUrl();
    }

    public void dumpTraces() throws Exception {
        HttpRequest req = new HttpRequest(baseUrl + "/traces");
        System.out.println(req.run(String.class));
    }

    /**
     * Wait until the list of spans matching the given traceId meets the waitCondition
     * <p>
     * This should be used when waiting for the expected spans from the server to appear in Jaeger.
     * <p>
     * Example:
     *
     * <pre>
     * client.waitForSpansForTraceId(testTraceId, hasSize(3))
     * </pre>
     *
     * @param traceId the traceId as a string of hex characters
     * @param waitCondition the condition to wait for
     * @return the list of spans
     */
    public List<ZipkinSpan> waitForSpansForTraceId(String traceId, Matcher<? super List<ZipkinSpan>> waitCondition) throws Exception {
        List<ZipkinSpan> result = null;
        Timeout timeout = new Timeout(Duration.ofSeconds(10));
        int retryCount = 0;
        while(retryCount < 3){
            retryCount += 1;
            try {
                while (true) {
                    result = getSpansForTraceId(traceId);

                    if (timeout.isExpired()) {
                        // Time is up,retry
                        Log.info(c, "waitForSpansForTraceId", "Spans did not match: " + result);
                        break;
                    }
                    if (waitCondition.matches(result)) {
                        Log.info(c, "waitForSpansForTraceId", "Waited " + timeout.getTimePassed() + " for spans to arrive");

                        // Wait additional time to allow more spans to arrive which would invalidate the match
                        // E.g. if we're waiting for 2 spans, check that we don't end up with 3 after waiting a while longer
                        Thread.sleep(500);
                        assertThat("Spans did not match after waiting additional time", result, waitCondition);

                        return result;
                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                throw new AssertionError("Iterrupted while waiting for spans", e);
            }
        }
        assertThat("Spans not found within timeout after 3 retries", result, waitCondition);
        return result;
    }

    public List<ZipkinSpan> getSpansForTraceId(String traceId) throws Exception {
        traceId = traceId.toLowerCase();
        String url = baseUrl + "/trace/" + traceId;

        Log.info(c, "getSpansForTraceId", "Fetching traces from " + url);

        JsonArray result;
        try {
            HttpRequest req = new HttpRequest(url);
            result = req.run(JsonArray.class);
            Log.info(c, "getSpansForTraceId", "Finished fetching traces from " + url);
        } catch (Exception e) {
            // run() will throw an exception if the endpoint returns a 404 (i.e. no traces found)
            // Assume any exception means there are no traces found on the server and return an empty list
            Log.info(c, "getSpansForTraceId", "No traces, exception was " + e);
            return Collections.emptyList();
        }

        Log.info(c, "getSpansForTraceId", "Got " + result.size() + " spans for " + traceId);

        return convertToSpans(result);
    }

    private List<ZipkinSpan> convertToSpans(JsonArray jsonArray) {
        List<ZipkinSpan> result = new ArrayList<>();
        for (JsonValue value : jsonArray) {
            result.add(new ZipkinSpan(value.asJsonObject()));
        }
        return result;
    }

    private static class Timeout {
        private final long start;
        private final Duration timeLimit;

        public Timeout(Duration timeout) {
            this.timeLimit = timeout;
            start = System.nanoTime();
        }

        public boolean isExpired() {
            return getTimePassed().compareTo(timeLimit) > 0;
        }

        public Duration getTimePassed() {
            return Duration.ofNanos(System.nanoTime() - start);
        }
    }
}
