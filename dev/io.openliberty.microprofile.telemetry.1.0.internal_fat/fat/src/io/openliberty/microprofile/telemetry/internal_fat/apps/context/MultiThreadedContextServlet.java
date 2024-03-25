/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.context;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/testContext")
@ApplicationScoped
public class MultiThreadedContextServlet extends FATServlet {

    private static final String MY_KEY_1 = "myKey1";
    private static final String MY_KEY_2 = "myKey2";
    private static final String MY_VALUE_1 = "myValue1";
    private static final String MY_VALUE_2 = "myValue2";
    private static final String MY_VALUE_3 = "myValue3";

    @Resource
    ManagedExecutorService managedExecutorService;

    @Test
    public void testContextTaskWrapping() throws InterruptedException, ExecutionException, TimeoutException {
        //add some baggage to the current context
        try (Scope s = addBaggage(MY_KEY_1, MY_VALUE_1)) {

            // execute my async sub-task
            ExecutorService executorService = Context.taskWrapping(managedExecutorService);
            Future<Map<String, String>> future = executorService.submit(new GetBaggageValueCallable(MY_KEY_1));
            Map<String, String> baggageValues = future.get(5, TimeUnit.SECONDS);
            assertEquals(MY_VALUE_1, baggageValues.get(MY_KEY_1));

            //check that the baggage was not modified by the sub-task
            assertCurrentBaggage(MY_KEY_1, MY_VALUE_1);
        }
    }

    @Test
    public void testCurrentContextWrap() throws InterruptedException, ExecutionException, TimeoutException {
        //add some baggage to the current context
        try (Scope s = addBaggage(MY_KEY_1, MY_VALUE_1)) {

            // execute my async sub-task
            Context context = Context.current();
            ExecutorService executorService = context.wrap(managedExecutorService);
            Future<Map<String, String>> future = executorService.submit(new GetBaggageValueCallable(MY_KEY_1));
            Map<String, String> baggageValues = future.get(5, TimeUnit.SECONDS);
            assertEquals(MY_VALUE_1, baggageValues.get(MY_KEY_1));

            //check that the baggage was not modified by the sub-task
            assertCurrentBaggage(MY_KEY_1, MY_VALUE_1);
        }
    }

    @Test
    public void testNewContextWrap() throws InterruptedException, ExecutionException, TimeoutException {
        //add some baggage to the current context
        try (Scope s = addBaggage(MY_KEY_1, MY_VALUE_1)) {

            //create a new context with a different value in (should not contain MY_KEY_1)
            Context context = newContextWithBaggage(MY_KEY_2, MY_VALUE_2);
            // execute my async sub-task
            ExecutorService executorService = context.wrap(managedExecutorService);
            Future<Map<String, String>> future = executorService.submit(new GetBaggageValueCallable(MY_KEY_1, MY_KEY_2));
            Map<String, String> baggageValues = future.get(5, TimeUnit.SECONDS);
            //check that the value for MY_KEY_1 was null (from the new context)
            assertEquals(null, baggageValues.get(MY_KEY_1));
            //check that the value for MY_KEY_1 was MY_VALUE_2 (from the new context)
            assertEquals(MY_VALUE_2, baggageValues.get(MY_KEY_2));

            //check that the baggage of the current context was not modified by the sub-task
            assertCurrentBaggage(MY_KEY_1, MY_VALUE_1);
        }
    }

    @Test
    public void testNewContextWrapCallable() throws InterruptedException, ExecutionException, TimeoutException {
        //add some baggage to the current context
        try (Scope s = addBaggage(MY_KEY_1, MY_VALUE_1)) {

            //create a new context with a different value in (should not contain MY_KEY_1)
            Context context = newContextWithBaggage(MY_KEY_2, MY_VALUE_2);

            //only wrap this one callable
            Callable<Map<String, String>> callable = new GetBaggageValueCallable(MY_KEY_1, MY_KEY_2);
            callable = context.wrap(callable);
            // execute my async sub-task
            Future<Map<String, String>> future = managedExecutorService.submit(callable);
            Map<String, String> baggageValues = future.get(5, TimeUnit.SECONDS);
            //check that the value for MY_KEY_1 was null (from the new context)
            assertEquals(null, baggageValues.get(MY_KEY_1));
            //check that the value for MY_KEY_1 was MY_VALUE_2 (from the new context)
            assertEquals(MY_VALUE_2, baggageValues.get(MY_KEY_2));

            //check that the baggage of the current context was not modified by the sub-task
            assertCurrentBaggage(MY_KEY_1, MY_VALUE_1);
        }
    }

    //check that the current context contains baggage with a certain value
    private static void assertCurrentBaggage(String key, String expectedValue) {
        Baggage baggage = Baggage.current();
        String value = baggage.getEntryValue(key);
        assertEquals(expectedValue, value);
    }

    //create a new current context by adding some baggage to the existing current context
    private static Scope addBaggage(String key, String value) {
        BaggageBuilder builder = Baggage.builder();
        builder.put(key, value);
        Baggage baggage = builder.build();
        return baggage.makeCurrent();
    }

    //create a new context by adding some baggage to the root context
    private static Context newContextWithBaggage(String key, String value) {
        BaggageBuilder builder = Baggage.builder();
        builder.put(key, value);
        Baggage baggage = builder.build();
        Context newContext = Context.root().with(baggage);
        return newContext;
    }

    //A Callable to get a String value from the Baggage of the current context for the thread
    private static class GetBaggageValueCallable implements Callable<Map<String, String>> {

        private final String[] keys;

        GetBaggageValueCallable(String... keys) {
            this.keys = keys;
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, String> call() throws Exception {
            Map<String, String> results = new HashMap<>();

            //get the baggage and check the contents
            Baggage baggage = Baggage.current();
            for (String key : keys) {
                String value = baggage.getEntryValue(key);
                if (value != null) {
                    results.put(key, value);
                }

                //create a new baggage and make a new current context with it
                //this new context should not affect the original one
                try (Scope s = addBaggage(key, MY_VALUE_3)) {

                    //check that the new baggage is in the current context
                    assertCurrentBaggage(key, MY_VALUE_3);
                }
            }
            return results;
        }

    }

}
