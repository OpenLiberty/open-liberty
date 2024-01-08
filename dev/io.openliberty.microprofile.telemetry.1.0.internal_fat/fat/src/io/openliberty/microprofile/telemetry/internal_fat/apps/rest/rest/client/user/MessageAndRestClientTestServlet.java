/*******************************************************************************
 * Copyright (c) 2019 ,2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.rest.rest.client.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import junit.framework.Assert;

@WebServlet("/testMessageAndRestClientTestServlet")
public class MessageAndRestClientTestServlet extends FATServlet {

    private static final long MAX_WAIT = 40000; //millis
    private static final long WAIT_INTERVAL = 50; //millis

    private static List<Span> recordedSpans = new ArrayList<Span>();

    @Inject
    private SimpleReactiveMessagingBean reactiveBean;

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Test
    public void testSpanInBothJaxCallerAndCalled() throws Exception {
        //Test that the reactive messaging part of the app worked as expected, no point checking the telemetry part works if it did not.
        List<String> reactiveMessagingResults = getReactiveMessagingResultsWithWaitIfNeeded(3);
        assertThat(reactiveMessagingResults, not(contains("LENGTH 8", "LENGTH 9!")));
        assertThat(reactiveMessagingResults, contains("LENGTH 10!", "LENGTH 11!!", "LENGTH 12!!!"));

        //Now onto the telemetry

        SpanDataMatcher spanInsideClientMatcher = SpanDataMatcher.hasName("SimpleReactiveMessagingBean.toUpperCase");

        SpanDataMatcher spanInsideJaxServiceMatcher = SpanDataMatcher.hasName("RestClientThatCapitalizes.jaxServiceThatCapitalizes");

        //Each message will end up with four spans being registered. And there are five messages.
        assertTrue(recordedSpans.size() == 5);
        for (int i = 0; i < 5; i++) {
            List<SpanData> results = new ArrayList(inMemorySpanExporter.getFinishedSpanItems(4, recordedSpans.get(i)));

            assertThat("Failed to find spans for index " + i,
                results,
                allOf(hasItem(spanInsideClientMatcher),
                    hasItem(spanInsideJaxServiceMatcher)));
        }
    }

    private List<String> getReactiveMessagingResultsWithWaitIfNeeded(int expectedResultCount) throws Exception {
        List<String> results = reactiveBean.getResults();

        if (results.size() >= expectedResultCount) {
            assertEquals(expectedResultCount, results.size());
            return results;
        }

        long maxNanos = System.nanoTime() + millisToNanos(MAX_WAIT);
        //Reactive messaging will populate this on another thread
        synchronized (results) {
            while (results.size() < expectedResultCount && nanoTimeRemaining(maxNanos)) {
                results.wait(WAIT_INTERVAL);
            }
            
            if (results.size() < expectedResultCount) {
                throw new TimeoutException("Timed out waiting for reactive messaging to produce results");
            }
        }

        assertEquals(expectedResultCount, results.size());
        return results;
    }

    private static final long millisToNanos(long millis) {
        return millis * 1000 * 1000;
    }

    private static final boolean nanoTimeRemaining(long maxNanos) {
        return (maxNanos - System.nanoTime()) > 0;
    }

    public static void recordSpan(Span span) {
        recordedSpans.add(span);
    }

}
