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

    @Resource
    private ManagedExecutorService mes; //Injected by the container

    @GET
    public String restEndpoint(@QueryParam("message") String message) {

        System.out.println(message + " received on thread " + Thread.currentThread().getName());

        // Set up the parallel tasks we create 3 * thyose that can be run on a physical core
        int count = 3 * Runtime.getRuntime().availableProcessors();
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

        int pipeLength = 10;
        int base = i * 1000000;
        PublisherBuilder<Integer> stream = ReactiveStreams.fromIterable(() -> {
            return IntStream.rangeClosed(0 + base, 99 + base).boxed().iterator();
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
        assertEquals(expectedResults(), actualResults);
    }

    /**
     * @return
     */
    private Object expectedResults() {
        return "[15001000, 15001001, 15001002, 15001003, 15001004, 15001005, 15001006, 15001007, 15001008, 15001009, 15001010, 15001011, 15001012, 15001013, 15001014, 15001015, 15001016, 15001017, 15001018, 15001019, 15001020, 15001021, 15001022, 15001023, 15001024, 15001025, 15001026, 15001027, 15001028, 15001029, 15001030, 15001031, 15001032, 15001033, 15001034, 15001035, 15001036, 15001037, 15001038, 15001039, 15001040, 15001041, 15001042, 15001043, 15001044, 15001045, 15001046, 15001047, 15001048, 15001049, 15001050, 15001051, 15001052, 15001053, 15001054, 15001055, 15001056, 15001057, 15001058, 15001059, 15001060, 15001061, 15001062, 15001063, 15001064, 15001065, 15001066, 15001067, 15001068, 15001069, 15001070, 15001071, 15001072, 15001073, 15001074, 15001075, 15001076, 15001077, 15001078, 15001079, 15001080, 15001081, 15001082, 15001083, 15001084, 15001085, 15001086, 15001087, 15001088, 15001089, 15001090, 15001091, 15001092, 15001093, 15001094, 15001095, 15001096, 15001097, 15001098, 15001099]";
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
        return j + 100;
    }

}
