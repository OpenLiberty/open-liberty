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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import junit.framework.Assert;

@WebServlet("/testMessageAndRestClientTestServlet")
public class MessageAndRestClientTestServlet extends FATServlet {

    private static final long MAX_WAIT = 40000; //millis
    private static final long WAIT_INTERVAL = 50; //millis

    private static Map<Span, Integer> spanAndExpectedNumber = new HashMap<Span, Integer>();

    @Inject
    private SimpleReactiveMessagingBean reactiveBean;

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Test
    public void testReactiveMessagingThatCallsJaxClient() throws InterruptedException {
        List<String> results = getResultsWithWaitIfNeeded();

        assertEquals(3, results.size());
        assertEquals(3, results.size());
        assertFalse(results.contains("LENGTH 8"));
        assertFalse(results.contains("LENGTH 9!"));
        assertTrue(results.contains("LENGTH 10!"));
        assertTrue(results.contains("LENGTH 11!!"));
        assertTrue(results.contains("LENGTH 12!!!"));
    }

    @Test
    public void testSpanInBothJaxCallerAndCalled() throws InterruptedException {
        getResultsWithWaitIfNeeded();//we just want to delay until the results are in.
        List<List<SpanData>> listOfListOfResults = new ArrayList<List<SpanData>>(5);

        SpanDataMatcher spanInsideClientMatcher = SpanDataMatcher.hasName("SimpleReactiveMessagingBean.toUpperCase");

        SpanDataMatcher spanInsideJaxServiceMatcher = SpanDataMatcher.hasName("RestClientThatCapitalizes.jaxServiceThatCapitalizes");

        //Each message will end up with four spans being registered. And there are five messages.
        for (Span s : spanAndExpectedNumber.keySet()) {
            listOfListOfResults.add(new ArrayList(inMemorySpanExporter.getFinishedSpanItems(spanAndExpectedNumber.get(s), s)));
        }

        String failMessage = "";

        //We don't really need to test this five times, but keeping all five
        //makes the test app more authentic and since we have them.
        for (int index = 0; index < listOfListOfResults.size(); index++) {
            boolean foundSpanCallingJax = false;
            boolean foundSpanInsideJax = false;

            for (SpanData sd : listOfListOfResults.get(index)) {
                if (spanInsideClientMatcher.matches(sd)) {
                    foundSpanCallingJax = true;
                }

                if (spanInsideJaxServiceMatcher.matches(sd)) {
                    foundSpanInsideJax = true;
                }

                if (foundSpanCallingJax && foundSpanInsideJax) {
                    break;
                }
            }
            if (!foundSpanCallingJax || !foundSpanInsideJax) {
                StringBuilder sb = new StringBuilder();
                sb.append("failed on index: " + index);
                if (!foundSpanCallingJax) {
                    sb.append(" Did not find a Span from SimpleReactiveMessagingBean");
                }
                if (!foundSpanInsideJax) {
                    sb.append(" Did not find a Span from RestClientThatCapitalizes");
                }
                sb.append(System.lineSeparator());
                failMessage = failMessage + sb.toString();
            }
        }

        if (!failMessage.isEmpty()) {
            Assert.fail(failMessage);
        }
    }

    private List<String> getResultsWithWaitIfNeeded() throws InterruptedException {
        List<String> results = reactiveBean.getResults();

        if (results.size() < 3) {
            return results;
        }

        long maxNanos = System.nanoTime() + millisToNanos(MAX_WAIT);
        //Reactive messaging will populate this on another thread
        synchronized (results) {
            while (results.size() < 3 && nanoTimeRemaining(maxNanos)) {
                results.wait(WAIT_INTERVAL);
            }
        }
        return results;
    }

    private static final long millisToNanos(long millis) {
        return millis * 1000 * 1000;
    }

    private static final boolean nanoTimeRemaining(long maxNanos) {
        return (maxNanos - System.nanoTime()) > 0;
    }

    public static void recordSpan(Span span, Integer expectedNumber) {
        spanAndExpectedNumber.put(span, expectedNumber);
    }

}
