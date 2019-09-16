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

    /**  */
    private static final int MINUTES_10 = 10;
    /**  */
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int PROCESSORS_TO_ADD = 5;
    private static final int PROCESSOR_PROCESSING_DURATION = 10;
    private static final int PROCESSOR_INCREMENT = 100;
    private static final int STREAM_DATA_ITEMS = 10;
    private static final int TERMINATION_POLL_DELAY = 100;
    /**  */
    private static final int POLLS_PER_SECOND = 1000 / TERMINATION_POLL_DELAY;
    private static final String FAILURE = "FAILURE";

    // We identify streams using a static counter incremented each time we get a new stream
    private static int streamInstanceNumber = 0;

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
        // Add so many processors in them middle and send so much data down the stream
        singleStreamWorkload(PROCESSORS_TO_ADD, STREAM_DATA_ITEMS);
        // singleStreamWorkload will wait for all completions and throw an assertion
        // if there expected and actual results do no match
        return FATServletClient.SUCCESS;

    }

    /**
     * This method can be used to simulate a small stream run. A number of data items are
     * sent down the stream that has a variable number of Processors. Each Processor changes
     * the data (increments it) so that we can tell the data visited each stream element.
     *
     * @param rsProcessorsInStream    the length of the pipeline is altered by adding this many Processors
     * @param dataItemsSentDownStream once the stream is running this many onData's will be sent down it before it completes
     */
    public <T> boolean singleStreamWorkload(int rsProcessorsInStream, int dataItemsSentDownStream) {

        // start of stream
        PublisherBuilder<Integer> streamHead = ReactiveStreams.fromIterable(() -> {
            // Publish a sequence of Integers 0,1,2,... down the stream
            return IntStream.rangeClosed(0, dataItemsSentDownStream).boxed().iterator();
        });

        // hops (Processors) in the middle
        for (int pipeElement = 0; pipeElement < rsProcessorsInStream; pipeElement++) {

            // Create a Processor that calls out to "methodCalledFromProcessor"
            ProcessorBuilder<Integer, Integer> mapProc = ReactiveStreams.<Integer> builder().map(j -> methodCalledFromProcessor(j));
            // Add the Processor onto the stream
            streamHead = streamHead.via(mapProc);
        }

        // Cap the end of the stream with an individually identified Subscriber
        TestSubscriber streamSink = new TestSubscriber("RPC" + streamInstanceNumber++);

        // Run the stream
        streamHead.to(streamSink).run();

        // Wait until the stream is finished - takes ~ 20 seconds for whole testcase usually
        waitForCompletionInServer(streamSink);

        // Check the results
        ArrayList<Integer> results = streamSink.getResults();
        String actualResults = Arrays.deepToString(results.toArray());
        assertEquals(expectedResults(), actualResults);

        // We will not reach here unless the assert of the result's contents was good
        return true;
    }

    /**
     * Create the expected result string programmatically
     *
     * @return
     */
    private String expectedResults() {

        String result = "";

        for (int dataItemIndex = 0; dataItemIndex <= STREAM_DATA_ITEMS; dataItemIndex++) {

            // Add the separator between the numbers
            if (dataItemIndex == 0) {
                result = result + "[";
            } else {
                result = result + ", ";
            }

            // Each processor increments the result on top of a base based on the index of the data item
            result = result + (dataItemIndex + (PROCESSORS_TO_ADD * PROCESSOR_INCREMENT));
        }

        // Add the end marker
        result = result + "]";
        return result;
    }

    /**
     * Waits for a stream to be terminated
     *
     * @param streams subscriber
     */
    private void waitForCompletionInServer(TestSubscriber streamSink) {
        int limit = 0;
        while (!streamSink.isTerminated()) {
            try {
                Thread.sleep(TERMINATION_POLL_DELAY);

                // Timeout if needed
                if (limit++ > (POLLS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_10)) {
                    assertEquals(streamSink.isTerminated(), true); // Probably false
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Each processor changes the data so we can tell from what the Subscriber sees if all were called.
     *
     * @param streamDataValue the data sent down the stream
     * @return the streamDataValue incremented by PROCESSOR_INCREMENT
     * @throws InterruptedException
     */
    private Integer methodCalledFromProcessor(Integer streamDataValue) {
        try {
            System.out.println("Processor running on thread " + Thread.currentThread().getName() + " sees " + streamDataValue);
            Thread.sleep(PROCESSOR_PROCESSING_DURATION);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return streamDataValue + PROCESSOR_INCREMENT;
    }

}
