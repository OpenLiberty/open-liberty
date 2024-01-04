/*
 * Copyright (c) 2016-2023 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
// Original file source: https://github.com/eclipse/microprofile-telemetry/blob/main/tracing/tck/src/main/java/org/eclipse/microprofile/telemetry/tracing/tck/exporter/InMemorySpanExporter.java

package io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Assert;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemorySpanExporter implements SpanExporter {
    private static final Logger LOGGER = Logger.getLogger(InMemorySpanExporter.class.getName());

    private boolean isStopped = false;
    // Static to allow multi-app testing
    private static final List<SpanData> finishedSpanItems = new CopyOnWriteArrayList<>();
    private static final Set<String> failedTestTraceIds = Collections.synchronizedSet(new HashSet<>());

    /**
     * Retrieve a list of finished span items with the same traceId as {@code span}
     * <p>
     * Asserts that the expected number of spans are received within the timeout
     *
     * @param spanCount the number of traces to wait for before returning
     * @param span      the span with the traceId to look for
     * @return the list of spans
     */
    public List<SpanData> getFinishedSpanItems(int spanCount, Span span) {
        return getFinishedSpanItems(spanCount, span.getSpanContext().getTraceId());
    }

    /**
     * Retrieve a list of finished span items for a given traceId
     * <p>
     * Asserts that the expected number of spans are received within the timeout
     *
     * @param spanCount the number of traces to wait for before returning
     * @param traceId   the traceId to look for
     * @return the list of spans
     */
    public List<SpanData> getFinishedSpanItems(int spanCount, String traceId) {
        // Wait for the right number of spans to arrive
        waitFor(Duration.ofSeconds(12), () -> getSpansForTraceId(traceId).size() >= spanCount);

        // Actually get the spans and check there are the right number
        List<SpanData> spans = getSpansForTraceId(traceId);
        LOGGER.info("Retrieved traces for " + traceId + ", expected: " + spanCount + " found: " + spans.size());

        // If we do not have the right number, report the traceId as belonging to a failed test and
        // create a good failure message
        if (spans.size() != spanCount) {
            addFailedTestTraceId(traceId);
            String foundSpans = spans.stream().map(Object::toString).collect(Collectors.joining("\n"));
            fail("Expected " + spanCount + " traces but found " + spans.size() + "\n" + foundSpans);
        }

        // Remove the found spans from the list of finished spans so that we can log any unclaimed spans at shutdown
        finishedSpanItems.removeAll(spans);

        // Sort and return the spans
        return sortByParentage(spans);
    }

    private List<SpanData> getSpansForTraceId(String traceId) {
        return finishedSpanItems.stream()
                        .filter(s -> s.getTraceId().equals(traceId))
                        .collect(toList());
    }

    /**
     * Wait up to {@code timeout} for {@code condition} to return {@code true}
     * <p>
     * This method will not throw an exception if {@code condition} never returns {@code true}. Callers should assert the condition after this method returns and create an
     * appropriate error message.
     *
     * @param timeout   the timeout to wait
     * @param condition the condition to check
     */
    private static void waitFor(Duration timeout, BooleanSupplier condition) {
        long timeoutNanos = timeout.toNanos();
        long startNanos = System.nanoTime();
        while ((System.nanoTime() - startNanos) < timeoutNanos) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Create a new list which contains the contents of {@code spans} sorted so that all spans appear before their children in the list
     *
     * @param spans a list of spans
     * @return a new list of spans sorted by parentage
     */
    private List<SpanData> sortByParentage(List<SpanData> spans) {
        Set<String> allSpanIds = spans.stream().map(SpanData::getSpanId).collect(toSet());

        ArrayList<SpanData> result = new ArrayList<>();

        // Start with all root spans and spans whose parents are not present in the list
        // For each, add it to the list then recursively add its children
        spans.stream().filter(s -> !s.getParentSpanContext().isValid() || !allSpanIds.contains(s.getParentSpanId()))
                        .sorted((a, b) -> Long.compare(a.getStartEpochNanos(), b.getStartEpochNanos()))
                        .forEach(s -> {
                            result.add(s);
                            addChildren(s, spans, result);
                        });

        return result;
    }

    private void addChildren(SpanData parent, List<SpanData> source, List<SpanData> destination) {
        source.stream().filter(s -> parent.getSpanId().equals(s.getParentSpanId()))
                        .sorted((a, b) -> Long.compare(a.getStartEpochNanos(), b.getStartEpochNanos()))
                        .forEach(s -> {
                            destination.add(s);
                            addChildren(s, source, destination);
                        });
    }

    /**
     * Register that a traceId belongs to a test which has failed
     * <p>
     * If a test fails, we can expect that it may not claim all of its spans. When we check for remaining spans at shutdown, we will ignore any with this traceId since there's no
     * point logging a failure twice.
     *
     * @param traceId the traceId associated with a failed test
     */
    public void addFailedTestTraceId(String traceId) {
        failedTestTraceIds.add(traceId);
    }

    /**
     * Careful when retrieving the list of finished spans. There is a chance when the response is already sent to the
     * client and the server still writing the end of the spans. This means that a response is available to assert from
     * the test side but not all spans may be available yet. For this reason, this method requires the number of
     * expected spans.
     *
     * @deprecated Use {@link #getFinishedSpanItems(int, Span)} or {@link #getFinishedSpanItems(int, String)} instead.
     *             They allow us to avoid mixing up spans from different tests without having to sleep between tests.
     */
    @Deprecated
    public List<SpanData> getFinishedSpanItems(int spanCount) {
        assertSpanCount(spanCount);
        List<SpanData> results = finishedSpanItems.stream().sorted(comparingLong(SpanData::getStartEpochNanos))
                        .collect(Collectors.toList());
        finishedSpanItems.removeAll(results);
        return results;
    }

    @Deprecated
    public void assertSpanCount(int spanCount) {
        int retries = 120;
        while (retries > 0 && finishedSpanItems.size() != spanCount) {
            try {
                retries--;
                Thread.sleep(100);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Assert.assertEquals(spanCount, finishedSpanItems.size());
    }

    /**
     * @deprecated No longer any need to reset. Any left over spans will now be ignored by {@code getFinishedSpanItems} and reported at app shutdown.
     */
    @Deprecated
    public void reset() {
        LOGGER.info("reset method called");
        finishedSpanItems.clear();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        LOGGER.info("export method called, exporter = " + System.identityHashCode(this) + ", collection: " + System.identityHashCode(finishedSpanItems));
        if (isStopped) {
            return CompletableResultCode.ofFailure();
        }

        List<SpanData> lSpans = new ArrayList<SpanData>(spans);
        Iterator<SpanData> iter = lSpans.listIterator();
        while (iter.hasNext()) {
            SpanData span = iter.next();
            /*
             * Remove readspans and FAT servlet spans.
             *
             * REGEX breakdown:
             * Match start of line.
             * Match 0 or 1 of the string "GET " (note whitespace) using a non-capturing group.
             * Match a literal "/"
             * Match anything
             * Match a literal /
             * match the string "test"
             * Match anything until the end of the line.
             */
            if (span.getName().contains("readspans") || span.getName().matches("^(?:GET )?/.*/test.*$")) {
                iter.remove();
            }
        }

        if (!lSpans.isEmpty()) { //this will be empty after a call to readSpans
            StringBuilder sb = new StringBuilder();
            sb.append("----------------- list of spans (filtered but unordered, ordering will be based on parentage) ---------- ");
            for (SpanData spanData : lSpans) {
                sb.append(System.lineSeparator() + spanData.toString() + System.lineSeparator());
            }
            LOGGER.info(sb.toString());
        }

        finishedSpanItems.addAll(lSpans);
        LOGGER.info("There are now " + finishedSpanItems.size() + " items");

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        LOGGER.info("shutdown method called");

        // Sleep a while to allow any async work which may still be going on to export spans
        try {
            Thread.sleep(Duration.ofSeconds(3).toMillis());
        } catch (InterruptedException e) {
            // Ignore here, still want to do the check
            LOGGER.severe("TEST9999E: Shutdown check for unclaimed spans interrupted");
            e.printStackTrace();
        }

        // Search for and report any exported spans which were not retrieved by a test
        List<SpanData> unclaimedSpans = finishedSpanItems.stream()
                        .filter(s -> !failedTestTraceIds.contains(s.getTraceId()))
                        .collect(Collectors.toList());

        if (!unclaimedSpans.isEmpty()) {
            String unclaimedNames = unclaimedSpans.stream()
                            .map(SpanData::getName)
                            .collect(Collectors.joining(", "));
            LOGGER.severe("TEST9999E: Found " + unclaimedSpans.size() + " unexpected spans at shutdown, full data in log: " + unclaimedNames);
            for (SpanData span : unclaimedSpans) {
                LOGGER.severe(span.toString());
            }
        }

        isStopped = true;
        return CompletableResultCode.ofSuccess();
    }
}
