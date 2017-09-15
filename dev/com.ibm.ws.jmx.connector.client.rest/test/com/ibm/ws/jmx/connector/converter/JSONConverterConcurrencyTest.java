/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
//@Ignore("under development")
public class JSONConverterConcurrencyTest {
    // 1000 iterations should be sufficient to drive out any concurrency problems
    private static final int DEFAULT_NUM_ITERATIONS = 1000;
    private CountDownLatch threadLatch; // for coordinating the test case thread and the spawned threads
    private Throwable throwable = null; // for recording anything caught in spawned threads

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void conversionSingleThreaded() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, JSONConverter.getConverter()));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void conversionDoubleThreaded() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, JSONConverter.getConverter()));
        exercisers.add(new ConcurrencyExerciser("t2", DEFAULT_NUM_ITERATIONS, JSONConverter.getConverter()));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void conversionTripleThreaded() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, JSONConverter.getConverter()));
        exercisers.add(new ConcurrencyExerciser("t2", DEFAULT_NUM_ITERATIONS, JSONConverter.getConverter()));
        exercisers.add(new ConcurrencyExerciser("t3", DEFAULT_NUM_ITERATIONS, JSONConverter.getConverter()));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void conversionSingleThreadedSameInstance() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        JSONConverter converter = JSONConverter.getConverter();
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, converter));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Ignore("This test is disabled and left behind to document a failure scenario" +
            "The JSONConverter object is not implicitly thread safe, rather the" +
            "contract to obtain a unique JSONConverter instance is, and therefore" +
            "that behaviour must be followed to ensure thread safety. See JavaDoc")
    @Test
    public void conversionDoubleThreadedSameInstance() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        JSONConverter converter = JSONConverter.getConverter();
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, converter));
        exercisers.add(new ConcurrencyExerciser("t2", DEFAULT_NUM_ITERATIONS, converter));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Ignore("This test is disabled and left behind to document a failure scenario" +
            "The JSONConverter object is not implicitly thread safe, rather the" +
            "contract to obtain a unique JSONConverter instance is, and therefore" +
            "that behaviour must be followed to ensure thread safety. See JavaDoc")
    @Test
    public void conversionTripleThreadedSameInstance() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        JSONConverter converter = JSONConverter.getConverter();
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, converter));
        exercisers.add(new ConcurrencyExerciser("t2", DEFAULT_NUM_ITERATIONS, converter));
        exercisers.add(new ConcurrencyExerciser("t3", DEFAULT_NUM_ITERATIONS, converter));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void conversionSingleThreadedGrabsInstance() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, null));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void conversionDoubleThreadedGrabsInstance() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, null));
        exercisers.add(new ConcurrencyExerciser("t2", DEFAULT_NUM_ITERATIONS, null));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.converter.JSONConverter#writeStringArray(java.io.OutputStream, java.lang.String[])}.
     */
    @Test
    public void conversionTripleThreadedGrabsInstance() throws Throwable {
        List<ConcurrencyExerciser> exercisers = new ArrayList<ConcurrencyExerciser>(); //exercisers used in the test
        exercisers.add(new ConcurrencyExerciser("t1", DEFAULT_NUM_ITERATIONS, null));
        exercisers.add(new ConcurrencyExerciser("t2", DEFAULT_NUM_ITERATIONS, null));
        exercisers.add(new ConcurrencyExerciser("t3", DEFAULT_NUM_ITERATIONS, null));

        threadLatch = new CountDownLatch(exercisers.size());

        // Start all the exercisers
        for (ConcurrencyExerciser exerciser : exercisers) {
            new Thread(exerciser).start();
        }

        // Wait for threads to complete
        assertTrue("Threads timed out, took more than 10 minutes to complete the exercise",
                   threadLatch.await(10, TimeUnit.MINUTES));
        if (throwable != null) {
            throw throwable;
        }
    }

    // A runnable class that exercises a repository client to a specified server
    private class ConcurrencyExerciser implements Runnable {
        private final String thread;
        private final int iterationLimit;
        private final JSONConverter converter;

        public ConcurrencyExerciser(String thread, int iterationLimit, JSONConverter converter) {
            this.thread = thread;
            this.iterationLimit = iterationLimit;
            this.converter = converter;
        }

        public void run() {
            // exercise the client
            int limit = iterationLimit;
            // Iterate until the count is up, OR a Throwable has been caught
            while (limit-- > 0 && (throwable == null)) {
                try {
                    JSONConverter myConverter = converter;
                    if (converter == null) {
                        myConverter = JSONConverter.getConverter();
                    }
                    exercise(myConverter);
                    if (converter == null) {
                        JSONConverter.returnConverter(myConverter);
                    }
                } catch (Throwable t) {
                    setThrowable(t);
                    break; // If we've failed, just stop running
                }
            }

            // Signal that we are done
            threadLatch.countDown();
        }

        private void exercise(JSONConverter converter) throws Exception {
            OutputStream out = new ByteArrayOutputStream();

            Map<Object, Object> map = new HashMap<Object, Object>();
            map.put(1, "a");
            map.put("b", 2);
            map.put(0L, 1);

            converter.writePOJO(out, map);
            Object pojo = converter.readPOJO(new ByteArrayInputStream(out.toString().getBytes()));
            assertEquals("FAIL: conversion of the multiple element map failed", map, pojo);
        }

        /**
         * Set the first Throwable. If it has already been set, do nothing.
         * 
         * @param t
         */
        private synchronized void setThrowable(Throwable t) {
            System.out.println("setThrowable in " + thread);
            if (throwable == null) {
                throwable = t;
            }
        }
    }

}
