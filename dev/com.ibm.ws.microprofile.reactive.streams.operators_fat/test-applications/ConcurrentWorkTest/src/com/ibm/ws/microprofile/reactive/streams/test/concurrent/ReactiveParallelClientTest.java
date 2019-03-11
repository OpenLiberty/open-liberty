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
import java.util.stream.IntStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import componenttest.topology.utils.FATServletClient;

@Path("/parallel")
public class ReactiveParallelClientTest {

    private static final int RS_ELEMENTS_IN_STREAM = 5;
    private static final int DATA_ITEMS_SENT_DOWN_STREAM = 10;
    private static int instanceId = 0;

    /**
     * This test is a simple reactive stream simulation that does some simple processing
     * in a reactive stream before returning an answer.
     *
     * @param message
     * @return
     */
    @GET
    public String restEndpoint(@QueryParam("message") String message) {
        System.out.println(message + " received on thread " + Thread.currentThread().getName());
        singleStreamWorkload(RS_ELEMENTS_IN_STREAM, DATA_ITEMS_SENT_DOWN_STREAM);
        return FATServletClient.SUCCESS;
    }

    /**
     * This method can be used to simulate a small stream run
     *
     * @param rsProcessorsInStream    the length of the pipeline is altered by adding this many Processors
     * @param dataItemsSentDownStream once the stream is running this many onData's will be sent down it before it completes
     */
    public <T> void singleStreamWorkload(int rsProcessorsInStream, int dataItemsSentDownStream) {

        // start of stream
        PublisherBuilder<Integer> streamHead = ReactiveStreams.fromIterable(() -> {
            return IntStream.rangeClosed(0, dataItemsSentDownStream).boxed().iterator();
        });

        // hops (Processors) in the middle
        for (int pipeElement = 0; pipeElement < rsProcessorsInStream; pipeElement++) {
            ProcessorBuilder<Integer, Integer> mapProc = ReactiveStreams.<Integer> builder().map(j -> methodCalledFromProcessor(j));
            streamHead = streamHead.via(mapProc);
        }

        // end of stream
        TestSubscriber streamSink = new TestSubscriber("RPC" + instanceId++);

        // run the stream
        streamHead.to(streamSink).run();

        // block until the stream is finished
        waitForCompletion(streamSink);

        // check the results
        ArrayList<Integer> results = streamSink.getResults();
        String actualResults = Arrays.deepToString(results.toArray());
        assertEquals(expectedResults(), actualResults);

    }

    /**
     * Create the expected result string programmatically
     *
     * @return
     */
    private String expectedResults() {
        String result = "[1000";
        for (int i = 1; i <= DATA_ITEMS_SENT_DOWN_STREAM; i++) {
            result = result + ", " + (i + (RS_ELEMENTS_IN_STREAM * 100));
        }
        result = result + "]";
        return result;
    }

    /**
     * Waits for a stream to be terminated
     *
     * @param streams subscriber
     */
    private void waitForCompletion(TestSubscriber streamSink) {
        while (!streamSink.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Each processor changes the data so we can tell from what the Subscriber sees if all were called.
     *
     * @param j the data sent down the stream
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
