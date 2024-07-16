/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.grpc.fat.streaming.service.StreamReply;
import com.ibm.ws.grpc.fat.streaming.service.StreamRequest;
import com.ibm.ws.grpc.fat.streaming.service.StreamingServiceGrpc;
import com.ibm.ws.grpc.fat.streaming.service.StreamingServiceGrpc.StreamingServiceStub;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 *
 */
@RunWith(FATRunner.class)
public class StreamingTests extends FATServletClient {

    protected static final Class<?> c = StreamingTests.class;
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(c.getName());

    ManagedChannel streamingChannel;
    private StreamingServiceStub streamingServiceStub;

    @Rule
    public TestName name = new TestName();

    @Server("StreamingServer")
    public static LibertyServer streamingServer;

    @BeforeClass
    public static void setUp() throws Exception {

        streamingServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        ShrinkHelper.defaultDropinApp(streamingServer, "StreamingService.war",
                                      "com.ibm.ws.grpc.fat.streaming.service",
                                      "com.ibm.ws.grpc.fat.streaming");

        streamingServer.startServer(StreamingTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        streamingServer.stopServer();
    }

    private void startStreamingService(String address, int port) {
        LOG.info("Connecting to StreamingService gRPC service at " + address + ":" + port);
        streamingChannel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();
        streamingServiceStub = StreamingServiceGrpc.newStub(streamingChannel);
    }

    private void stopStreamingService() {
        GrpcTestUtils.stopGrpcService(streamingChannel);
    }

    public String replyAfterClientStream = "Null";

    /**
     * Tests a single grpc service
     *
     * @throws Exception
     *
     **/
    @Test
    public void testClientStreamGrpcService() throws Exception {

        // TODO Change this to check for the actual grpc service started message, once it's implemented
        // Check to make sure the service has started
        // CWWKZ0001I: Application ... started in 0.802 seconds.
        assertNotNull(streamingServer.waitForStringInLog("CWWKZ0001I"));

        // Start up the client connection and send a request
        startStreamingService(streamingServer.getHostname(), streamingServer.getHttpDefaultPort());

        // client streaming test logic

        // This if for sending a stream of data to the server and then get a single reply
        StreamObserver<StreamRequest> clientStreamX = streamingServiceStub.clientStream(new StreamObserver<StreamReply>() {
            @Override
            public void onNext(StreamReply response) {
                // response from server
                // called only once
                replyAfterClientStream = response.toString();
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                // called after onNext
            }
        });

        // client streaming
        int numberOfMessages = 200;
        int timeBetweenMessagesMsec = 0;
        StreamRequest nextRequest = null;

        String nextMessage = null;
        String firstMessage = "This is the first Message...";
        String lastMessage = "This is the last Message";

        String s5chars = "12345";
        String s50chars = "12345678901234567890123456789012345678901234567890";
        String s500chars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
        String s5000chars = s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars;

        for (int i = 1; i <= numberOfMessages; i++) {

            if (i == 1) {
                nextMessage = firstMessage;
            } else if (i == numberOfMessages) {
                nextMessage = lastMessage;
            } else {
                nextMessage = "--Message " + i + " of " + numberOfMessages + " left client at time: " + System.currentTimeMillis() + "--";
                nextMessage = nextMessage + s50chars;
            }

            nextRequest = StreamRequest.newBuilder().setMessage(nextMessage).build();
            // LOG.info("Client sending/onNext: " + nextMessage);
            clientStreamX.onNext(nextRequest);
            try {
                if (timeBetweenMessagesMsec > 0) {
                    Thread.sleep(timeBetweenMessagesMsec);
                }
            } catch (Exception x) {
                // do nothing
            }
        }

        // wait to send onCompleted for now
        try {
            Thread.sleep(500);
        } catch (Exception x) {
            // do nothing
        }
        LOG.info("Client calling onCompleted");
        clientStreamX.onCompleted();

        // wait for the response from server
        try {
            Thread.sleep(1000);
        } catch (Exception x) {
            // do nothing
        }

        // test that this is what was expected:
        LOG.info("reply message was: " + replyAfterClientStream);
        int i1 = replyAfterClientStream.indexOf(firstMessage);
        int i2 = replyAfterClientStream.indexOf(lastMessage);
        LOG.info("firstMessage index at: " + i1 + " lastMessage index at: " + i2);

        assertTrue((i1 >= 0 && i2 >= 0));

        stopStreamingService();

    }

//  StreamObserver<StreamRequest> clientStreamX = streamingServiceBlockingStub.clientStream(new ClientStreamClass());
//    class ClientStreamClass implements StreamObserver<StreamReply> {
//        @Override
//        public void onNext(StreamReply response) {
//            // response from server
//            // called only once
//            replyAfterClientStream = response.toString();
//        }
//
//        @Override
//        public void onError(Throwable t) {
//
//        }
//
//        @Override
//        public void onCompleted() {
//            // called after onNext
//        }
//    }

}