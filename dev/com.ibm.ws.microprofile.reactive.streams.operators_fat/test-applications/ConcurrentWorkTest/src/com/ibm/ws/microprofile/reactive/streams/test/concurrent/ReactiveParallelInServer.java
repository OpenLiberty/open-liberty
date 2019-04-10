/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.concurrent;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import componenttest.topology.utils.FATServletClient;

/**
 * This class provides an endpoint for a test that does some
 * reactive streams FAT tests parallel in the server.
 */
@ApplicationScoped
@Path("/parallelInServer")
public class ReactiveParallelInServer {

    /**  */
    private static final int PARALLELISM = 10;
    /**  */
    private static final int PROC_INCREMENT = 100;
    /**  */
    private static final int PIPE_LENGTH = 5;
    /**  */
    private static final int FOUNDATION = 1000000;
    /**  */
    private static final int ELEMENTS = 10;
    @Resource
    private ManagedExecutorService mes; //Injected by the container

    @GET
    public String restEndpoint(@QueryParam("message") String message) {

        System.out.println(message + " received on thread " + Thread.currentThread().getName());

        // Set up the parallel tasks we create 3 * those that can be run on a physical core
        int count = PARALLELISM;
        System.out.println(" Going to run " + count + " in parallel");
        Collection<Callable<String>> tasks = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            final int j = i;
            Callable<String> callableTask = () -> {
                String msg = "Task run on thread " + Thread.currentThread().getName();
                System.out.println(msg);
                multiElementStreamTest(j);
                return msg;
            };
            tasks.add(callableTask);
        }

        // Run the multiStream tests in parallel
        try {
            List<Future<String>> futures = mes.invokeAll(tasks);
            waitForFutures(futures);
            return FATServletClient.SUCCESS;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }

    /**
     * This method will not return until all the futures it is passed complete
     */
    static void waitForFutures(List<Future<String>> futures) {
        futures.parallelStream().forEach(f -> {
            try {
                f.get(1, TimeUnit.MINUTES);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * This test will drive some workload down a stream. The parallel'ization
     * is done before calling this method.
     *
     * @param i
     */
    public <T> void multiElementStreamTest(Integer i) {

        int pipeLength = PIPE_LENGTH;
        int base = i * FOUNDATION;
        PublisherBuilder<Integer> stream = ReactiveStreams.fromIterable(() -> {
            return IntStream.rangeClosed(0 + base, ELEMENTS + base).boxed().iterator();
        });

        TestSubscriber subscriber = new TestSubscriber("mst");

        // Set up the stages of the stream
        for (int pipeElement = 0; pipeElement < pipeLength; pipeElement++) {
            ProcessorBuilder<Integer, Integer> mapProc = ReactiveStreams.<Integer> builder().map(j -> methodCalledFromProcessor(j));
            stream = stream.via(mapProc);
        }

        // Set up a subscriber and run the stream
        stream.to(subscriber).run();

        // Wait for the stream to end
        while (!subscriber.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Integer> actualResults = subscriber.getResults();
        System.out.println(Arrays.deepToString(actualResults.toArray()) + " results received on thread " + Thread.currentThread().getName());
        assertEquals(expectedResults(PIPE_LENGTH, base, ELEMENTS), "" + actualResults);
    }

    /**
     * @return
     */
    private Object expectedResults(int pipeLength, int base, int elements) {
        String result = "[";

        result = result + (base + (PIPE_LENGTH * PROC_INCREMENT));
        int floor = base;
        for (int i = 1; i <= elements; i++) {
            result = result + ", " + (floor + (i + (PIPE_LENGTH * PROC_INCREMENT)));
        }
        result = result + "]";

        return result;
    }

    /**
     * @param j
     * @return
     * @throws InterruptedException
     */
    private Integer methodCalledFromProcessor(Integer j) {
        try {
            System.out.println("Processor running on thread " + Thread.currentThread().getName() + " sees " + j);
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return j + PROC_INCREMENT;
    }

}
