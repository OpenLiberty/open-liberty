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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Assert;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InMemorySpanExporter implements SpanExporter {
    private static final Logger LOGGER = Logger.getLogger(InMemorySpanExporter.class.getName());

    private boolean isStopped = false;
    // Static to allow multi-app testing
    private static final List<SpanData> finishedSpanItems = new CopyOnWriteArrayList<>();

    /**
     * Careful when retrieving the list of finished spans. There is a chance when the response is already sent to the
     * client and the server still writing the end of the spans. This means that a response is available to assert from
     * the test side but not all spans may be available yet. For this reason, this method requires the number of
     * expected spans.
     */
    public List<SpanData> getFinishedSpanItems(int spanCount) {
        assertSpanCount(spanCount);
        return finishedSpanItems.stream().sorted(comparingLong(SpanData::getStartEpochNanos))
                        .collect(Collectors.toList());
    }

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

    public void reset() {
        LOGGER.info("reset method called");
        finishedSpanItems.clear();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        LOGGER.info("export method called");
        if (isStopped) {
            return CompletableResultCode.ofFailure();
        }

        List<SpanData> lSpans = new ArrayList<SpanData>(spans);
        Iterator<SpanData> iter = lSpans.listIterator();
        while (iter.hasNext()) {
            if (iter.next().getName().contains("readspans")) {
                iter.remove();
            }
        }

        if (!lSpans.isEmpty()) { //this will be empty after a call to readSpans
            StringBuilder sb = new StringBuilder();
            sb.append("----------------- list of spans (filtered but unordered, ordering will be based on the start time) ---------- ");
            for (SpanData spanData : lSpans) {
                sb.append(System.lineSeparator() + spanData.toString() + System.lineSeparator());
            }
            LOGGER.info(sb.toString());
        }

        finishedSpanItems.addAll(lSpans);

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        LOGGER.info("shutdown method called");
        finishedSpanItems.clear();
        isStopped = true;
        return CompletableResultCode.ofSuccess();
    }
}
