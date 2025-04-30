
/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.asynchttpclient.Dsl;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Basic WebSockets tests to ensure the behavior works as expected after a checkpoint restore.
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class BasicTest {

    @Server("basicWsocServer")
    public static LibertyServer server;

    private static final Logger LOG = Logger.getLogger(BasicTest.class.getName());

    private static final String APP_NAME = "basic";

    private static Boolean IS_EXPECTED_RESULT = false;

    private static CountDownLatch latch; // Used to address timing issues between when the message is recived and when the assert is checked

    @BeforeClass
    public static void setUp() throws Exception {

        // Build the war app and add the dependencies
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.wsoc.basic");

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.startServer();
        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDown() throws Exception {

        if (server != null && server.isStarted()) {
            server.stopServer();
        }

    }

    @Before
    public void resetResult() throws Exception {
        IS_EXPECTED_RESULT = false;
        latch = null;
    }

    private WebSocketUpgradeHandler createWebSocketUpgradeHandler(Object expectedResult) {

        latch = new CountDownLatch(1); // forces the asserts in each test to be checked once the message is recieved (via countdown / await)

        WebSocketUpgradeHandler.Builder upgradeHandlerBuilder = new WebSocketUpgradeHandler.Builder();
        WebSocketUpgradeHandler wsHandler = upgradeHandlerBuilder
                        .addWebSocketListener(new WebSocketListener() {
                            @Override
                            public void onOpen(WebSocket websocket) {
                                // WebSocket connection opened
                                LOG.info("Opened Websocket");
                            }

                            @Override
                            public void onClose(WebSocket websocket, int code, String reason) {
                                // WebSocket connection closed
                                LOG.info("Closed Websocket");
                            }

                            @Override
                            public void onError(Throwable t) {
                                // WebSocket connection error
                                LOG.info("Session Error Occurred: " + t);
                            }

                            @Override
                            public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
                                // Log message
                                StringBuilder sb = new StringBuilder();
                                for (byte b : payload) {
                                    sb.append((char) b);
                                }
                                LOG.info("Debugging binary message: " + sb.toString());
                                IS_EXPECTED_RESULT = Arrays.equals(payload, (byte[]) expectedResult);
                                latch.countDown();
                            }

                            @Override
                            public void onTextFrame(String payload, boolean finalFragment, int rsv) {
                                // Log message
                                LOG.info("Debugging text message: " + payload);
                                IS_EXPECTED_RESULT = expectedResult.equals(payload);
                                latch.countDown();
                            }
                        })
                        .build();
        return wsHandler;
    }

    /*
     * Tested the PathParam annotation
     */
    @Test
    public void testAnnotatedByteArray() throws Exception {
        Object expectedResult = "test message".getBytes();

        WebSocketUpgradeHandler wsHandler = createWebSocketUpgradeHandler(expectedResult);

        WebSocket webSocketClient = Dsl.asyncHttpClient()
                        .prepareGet("ws://" +
                                    server.getHostname() + ":" +
                                    server.getHttpDefaultPort() + "/" +
                                    APP_NAME +
                                    "/annotatedByteArray/true")
                        .setRequestTimeout(5000)
                        .execute(wsHandler)
                        .get();

        if (webSocketClient.isOpen()) {
            LOG.info("sending message");
            webSocketClient.sendBinaryFrame("test message".getBytes());
        }
        latch.await(3L, TimeUnit.SECONDS);
        webSocketClient.sendCloseFrame();
        assertTrue("Results do not match! ", IS_EXPECTED_RESULT);
    }

    /*
     * Verifies the decoder annotation is picked up
     */
    @Test
    public void testDecoder() throws Exception {
        Object expectedResult = "[class io.openliberty.wsoc.basic.BinaryStreamDecoder]";

        WebSocketUpgradeHandler wsHandler = createWebSocketUpgradeHandler(expectedResult);
        WebSocket webSocketClient = Dsl.asyncHttpClient()
                        .prepareGet("ws://" +
                                    server.getHostname() + ":" +
                                    server.getHttpDefaultPort() + "/" +
                                    APP_NAME +
                                    "/defaults")
                        .setRequestTimeout(5000)
                        .execute(wsHandler)
                        .get();

        if (webSocketClient.isOpen()) {
            LOG.info("sending message");
            webSocketClient.sendTextFrame("decoders");
        }
        latch.await(3L, TimeUnit.SECONDS);
        webSocketClient.sendCloseFrame();
        assertTrue("Results do not match! ", IS_EXPECTED_RESULT);
    }

    /*
     * Verifies the WsWsocServerContainer#doUprade works (deprecated in 2.1)
     */
    @Test
    public void testUpgrade() throws Exception {
        Object expectedResult = "got your message hello world";

        WebSocketUpgradeHandler wsHandler = createWebSocketUpgradeHandler(expectedResult);
        WebSocket webSocketClient = Dsl.asyncHttpClient()
                        .prepareGet("ws://" +
                                    server.getHostname() + ":" +
                                    server.getHttpDefaultPort() + "/" +
                                    APP_NAME +
                                    "/upgradeEcho")
                        .setRequestTimeout(5000)
                        .execute(wsHandler)
                        .get();

        if (webSocketClient.isOpen()) {
            LOG.info("sending message");
            webSocketClient.sendTextFrame("hello world");
        }
        latch.await(3L, TimeUnit.SECONDS);
        webSocketClient.sendCloseFrame();
        assertTrue("Results do not match! ", IS_EXPECTED_RESULT);
    }

}
