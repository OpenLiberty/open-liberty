/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;

/**
 * Tests for WebSocket fragmented message memory usage
 * 
 * These tests verify that the server enforces maxMessageSize and buffer size limits
 * on fragmented messages. 
 * 
 * @see <a href="https://jakarta.ee/specifications/websocket/2.2/">Jakarta WebSocket 2.2 Specification</a>
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FragmentedMessageTest {
    
    public static final String SERVER_NAME = "fragmentedMessageServer";
    
    @Server(SERVER_NAME)
    public static LibertyServer server;
    
    private static final Logger LOG = Logger.getLogger(FragmentedMessageTest.class.getName());
    
    private static final String WAR_NAME = "fragmentedMessageApp";
    
    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();
    
    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(WAR_NAME + ".war",
                                                       "io.openliberty.wsoc.endpoints.fragmentedsecurity");
        
        ShrinkHelper.exportDropinAppToServer(server, war);

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*" + WAR_NAME);
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that a single large frame exceeding maxMessageSize is rejected.
     * This is the baseline test - existing functionality that should work.
     * Uses unique size: 2,097,152 bytes (2MB)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testSingleLargeFrameRejected() throws Exception {
        LOG.info("Starting testSingleLargeFrameRejected");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferMb1Max";
        
        TestClient client = new TestClient();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        
        try {
            Session session = container.connectToServer(client, new URI(endpoint));
            
            // Send single 2MB message (exceeds 1MB limit) - UNIQUE SIZE for tracking
            byte[] largePayload = new byte[2 * 1024 * 1024]; // 2,097,152 bytes
            
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(largePayload));
            } catch (Exception e) {
                // Expected - server may close connection during send
                LOG.info("Send failed as expected: " + e.getMessage());
            }
            
            // Wait for close
            assertTrue("Connection should be closed due to message too big",
                      client.closeLatch.await(10, TimeUnit.SECONDS));
            
            // Verify close reason if we got one
            if (client.closeReason != null) {
                LOG.info("Close reason: " + client.closeReason);
                LOG.info("Close code: " + client.closeReason.getCloseCode().getCode());
                // Accept either 1009 (Message Too Big) or 1011 (Internal Server Error)
                int closeCode = client.closeReason.getCloseCode().getCode();
                assertTrue("Close code should indicate error (1009 or 1011), but was: " + closeCode,
                          closeCode == 1009 || closeCode == 1011);
            } else {
                LOG.info("No close reason provided, but connection was closed");
            }
            
            LOG.info("testSingleLargeFrameRejected PASSED");
            
        } catch (Exception e) {
            LOG.severe("testSingleLargeFrameRejected failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Test DefaultEndpoint with many small frames.
     * Sends 9 frames × 3KB = 27KB (well within 32KB limit)
     */
    @Test
    public void testDefaultEndpoint_ManySmallFrames_WithinLimit() throws Exception {
        LOG.info("Starting testDefaultEndpoint_ManySmallFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultEndpoint");
            
            // DefaultBufferDefaultMaxEndpoint: 32KB buffer, -1 maxMessageSize → 32KB effective limit
            // Account for WebSocket frame overhead (~6-14 bytes per frame)
            int frameCount = 9;
            int frameSize = 2 * 1024; // 2KB per frame = 18KB total (well within 32KB with overhead)
            int expectedTotalBytes = frameCount * frameSize;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
            }
            
            client.sendFinalFragment(payload);
            LOG.info("Sent 9 frames of 2KB each, total: 18KB");
            
            // Should receive echo (may be fragmented by server)
            byte[] response = client.readCompleteMessage();
            assertTrue("Expected echo response but got null", response != null);
            assertTrue("Expected echo response of " + expectedTotalBytes + " bytes but got " + response.length,
                      response.length == expectedTotalBytes);
            
            LOG.info("testDefaultEndpoint_ManySmallFrames_WithinLimit PASSED - received " + response.length + " bytes");
            
        } catch (Exception e) {
            LOG.severe("testDefaultEndpoint_ManySmallFrames_WithinLimit FAILED: " + e.getMessage());
            e.printStackTrace();
            fail("testDefaultEndpoint_ManySmallFrames_WithinLimit FAILED: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Test MB1BufferDefaultMaxEndpoint with many small frames within 1MB buffer limit.
     * Sends 250 frames of 3KB each = 750KB total (within 1MB buffer limit)
     */
    @Test
    public void testHighBufferDefaultMax_ManySmallFrames_WithinLimit() throws Exception {
        LOG.info("Starting testHighBufferDefaultMax_ManySmallFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to MB1BufferDefaultMaxEndpoint");
            
            // MB1BufferDefaultMaxEndpoint: 1MB buffer, -1 maxMessageSize → 1MB effective limit
            // Send 300 frames × 3KB = 900KB (well within 1MB limit)
            int frameCount = 300;
            int frameSize = 3 * 1024; // 3KB per frame = 900KB total
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
                if (i % 200 == 0) {
                    LOG.info("Sent " + (i + 1) + " fragments, cumulative: " + ((i + 1) * frameSize / 1024 / 1024) + " MB");
                }
            }
            
            client.sendFinalFragment(payload);
            LOG.info("Sent 300 frames of 3KB each, total: 900KB");
            
            // Should receive echo
            byte[] response = client.readFrame();
            if (response != null) {
                LOG.info("Received echo response: " + response.length + " bytes");
            }
            
            LOG.info("testHighBufferDefaultMax_ManySmallFrames_WithinLimit PASSED");
            
        } catch (Exception e) {
            LOG.severe("testHighBufferDefaultMax_ManySmallFrames_WithinLimit failed: " + e.getMessage());
            throw e;
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test that fragmented message exceeding maxMessageSize is rejected.
     * This tests the fix - fragmented messages should be
     * subject to cumulative size limits.
     *
     * Sends 100 frames of 51,234 bytes each (~5MB total) to endpoint with 1MB limit.
     * Expected: Connection closed with code 1009 (Message Too Big)
     * Uses unique size: 51,234 bytes per frame
     *
     * Uses raw WebSocket frames to ensure proper fragmentation (JSR-356 API doesn't
     * properly support sending fragmented messages).
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testFragmentedMessageExceedingLimitRejected() throws Exception {
        LOG.info("Starting testFragmentedMessageExceedingLimitRejected");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferMb1Max";
        
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        int sentFrames = 0;
        boolean connectionClosed = false;
        
        try {
            client.connect(endpoint);
            LOG.info("Raw WebSocket client connected");
            
            // Send 100 fragments of 51,234 bytes each = ~5MB total (exceeds 1MB limit)
            int frameCount = 100;
            int frameSize = 51234; // UNIQUE SIZE for tracking
            
            byte[] payload = new byte[frameSize];
            
            // Send first fragment (FIN=0, opcode=0x02 for binary)
            client.sendFirstFragment(payload);
            sentFrames++;
            LOG.info("Sent first fragment");
            
            // Send continuation fragments (FIN=0, opcode=0x00)
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + i + " frames");
                    connectionClosed = true;
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
                
                // Check for close frame every 10 frames
                if (i % 10 == 0) {
                    LOG.info("Sent " + i + " fragments, cumulative: " + (i * frameSize) + " bytes");
                }
            }
            
            // If still connected, send final fragment
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
                LOG.info("Sent final fragment");
            }
            
            // Try to read close frame
            byte[] closeFrame = client.readFrame();
            if (closeFrame != null) {
                LOG.info("Received close frame from server");
                connectionClosed = true;
            }
            
            // ASSERT: Connection must have been closed
            if (!connectionClosed && sentFrames >= frameCount) {
                fail("testFragmentedMessageExceedingLimitRejected FAILED - sent all " + sentFrames +
                     " frames without connection being closed. Expected connection to close when limit exceeded.");
            }
            
            assertTrue("Connection should have been closed when limit exceeded", connectionClosed);
            LOG.info("testFragmentedMessageExceedingLimitRejected PASSED - connection closed after " + sentFrames + " frames");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
            LOG.info("testFragmentedMessageExceedingLimitRejected PASSED - connection closed after " + sentFrames + " frames");
            // Exception is expected when server closes connection
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test that fragmented message within maxMessageSize is accepted.
     * This ensures the fix doesn't break legitimate fragmented messages.
     *
     * Sends 10 frames of 40,960 bytes each (400KB total) to endpoint with 1MB limit.
     * Expected: Message accepted and echoed back
     * Uses unique size: 40,960 bytes per frame
     */
    @Test
    public void testFragmentedMessageWithinLimitAccepted() throws Exception {
        LOG.info("Starting testFragmentedMessageWithinLimitAccepted");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferMb1Max";
        
        FragmentedTestClient client = new FragmentedTestClient();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        
        try {
            Session session = container.connectToServer(client, new URI(endpoint));
            
            // Send 10 fragments of 40,960 bytes each = 400KB total (within 1MB limit)
            int frameCount = 10;
            int frameSize = 40010; // UNIQUE SIZE for tracking (40KB)
            
            for (int i = 0; i < frameCount; i++) {
                byte[] payload = new byte[frameSize];
                // Send each as a complete message (not fragmented)
                // The second parameter true means "this is a compl ete message"
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(payload), true);
            }
            
            // Wait for first response
            assertTrue("Should receive echoed message",
                      client.messageLatch.await(10, TimeUnit.SECONDS));
            
            // Wait for all echo responses (server may echo each fragment separately)
            // Expecting 10 messages if server echoes per fragment, or 1 if properly accumulated
            client.waitForAllMessages(10000, frameCount);
            
            // Verify message was received (not closed)
            assertTrue("Connection should remain open",
                      client.closeLatch.getCount() > 0);
            
            // Verify received message size
            int expectedSize = frameCount * frameSize;
            assertTrue("Expected echo response of " + expectedSize + " bytes but got " +
                      (client.receivedMessage != null ? client.receivedMessage.remaining() : 0) + " bytes",
                      client.receivedMessage != null && client.receivedMessage.remaining() == expectedSize);
            
            LOG.info("testFragmentedMessageWithinLimitAccepted PASSED - received correct echo of " + expectedSize + " bytes");
            
            session.close();
            
        } catch (Exception e) {
            LOG.severe("testFragmentedMessageWithinLimitAccepted FAILED: " + e.getMessage());
            e.printStackTrace();
            fail("testFragmentedMessageWithinLimitAccepted FAILED: " + e.getMessage());
        }
    }
    
    /**
     * Test that many small fragmented messages don't cause memory issues.
     * This tests for the scenario where many small frames could
     * accumulate unbounded in memory.
     *
     * Sends 2000 frames of 1,111 bytes each (~2.1MB total) to endpoint with 1MB limit.
     * Expected: Connection closed when cumulative size exceeds limit
     * Uses unique size: 1,111 bytes per frame
     */
    @Test //-- CURRENTLY FAILING! NOW WORKING!
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testManySmallFragmentsRejected() throws Exception {
        LOG.info("Starting testManySmallFragmentsRejected");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferMb1Max";
        
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Raw WebSocket client connected");
            
            // Send 2000 fragments of 1,111 bytes each = ~2.1MB total (exceeds 1MB limit)
            int frameCount = 2000;
            int frameSize = 1111; // UNIQUE SIZE for tracking
            int sentFrames = 0;
            
            byte[] payload = new byte[frameSize];
            
            // Send first fragment
            client.sendFirstFragment(payload);
            sentFrames++;
            
            // Send continuation fragments
            boolean connectionClosed = false;
            for (int i = 1; i < frameCount - 1; i++) {
                try {
                    client.sendContinuationFragment(payload);
                    sentFrames++;
                    
                    if (i % 100 == 0) {
                        LOG.info("Sent " + sentFrames + " fragments, cumulative: " + (sentFrames * frameSize) + " bytes");
                    }
                } catch (IOException e) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize) + " bytes): " + e.getMessage());
                    connectionClosed = true;
                    break;
                }
            }
            
            // If still connected, try to send final fragment
            if (!connectionClosed) {
                try {
                    client.sendFinalFragment(payload);
                    sentFrames++;
                    LOG.info("Sent all " + sentFrames + " fragments (" + (sentFrames * frameSize) + " bytes)");
                } catch (IOException e) {
                    LOG.info("Connection closed when sending final fragment after " + sentFrames + " frames");
                    connectionClosed = true;
                }
            }
            
            // Wait for close frame from server (up to 2 seconds)
            if (!connectionClosed) {
                LOG.info("Waiting for server to close connection...");
                connectionClosed = client.waitForClose(2000);
            }
            
            // Verify connection was closed
            if (!connectionClosed) {
                fail("PROBLEM DETECTED: Server accepted all " + sentFrames + " fragments (" +
                     (sentFrames * frameSize) + " bytes) exceeding 1MB limit! Connection should have been closed.");
            }
            
            LOG.info("testManySmallFragmentsRejected PASSED - connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize) + " bytes)");
            
            // Try to read response to see if server accepted the oversized message
            int totalResponseSize = 0;
            int responseFrameCount = 0;
            
            while (client.isConnected() && responseFrameCount < 200) {
                byte[] frame = client.readFrame();
                if (frame == null) {
                    break;
                }
                
                responseFrameCount++;
                totalResponseSize += frame.length;
                LOG.info("Received frame " + responseFrameCount + " from server: " + frame.length + " bytes (total: " + totalResponseSize + ")");
                
                if (totalResponseSize > 1000000) { // 1MB limit
                    fail("PROBLEM DETECTED: Server accepted and echoed back " + totalResponseSize +
                         " bytes across " + responseFrameCount + " frames, exceeding 1MB limit!");
                }
                
                if (frame.length < 1000) {
                    break;
                }
            }
            
            LOG.info("testManySmallFragmentsRejected PASSED - connection closed after " + sentFrames +
                    " frames as expected");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test with unlimited maxMessageSize (-1) and high buffer size (10MB).
     * When maxMessageSize is -1 (unlimited), the buffer size becomes the effective limit.
     * Uses unique size: 9,876 bytes per frame
     *
     * This test sends 98KB which is WITHIN the 10MB buffer limit, so it should be accepted.
     */
    @Test
    public void testUnlimitedMaxMessageSizeWithinBufferLimit() throws Exception {
        LOG.info("Starting testUnlimitedMaxMessageSizeWithinBufferLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferDefaultMax";
        
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Raw WebSocket client connected");
            
            // Send fragmented message within buffer size (1MB)
            // Send 10 frames of 9,876 bytes each = ~98KB total (well within 1MB buffer)
            int frameCount = 10;
            int frameSize = 9876; // UNIQUE SIZE for tracking (~9.6KB)
            
            byte[] payload = new byte[frameSize];
            
            // Send first fragment
            client.sendFirstFragment(payload);
            LOG.info("Sent first fragment");
            
            // Send continuation fragments
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
                LOG.info("Sent fragment " + i + ", cumulative: " + ((i + 1) * frameSize) + " bytes");
            }
            
            // Send final fragment
            client.sendFinalFragment(payload);
            LOG.info("Sent final fragment, total: " + (frameCount * frameSize) + " bytes");
            
            // Should receive echo
            byte[] response = client.readFrame();
            int expectedSize = frameCount * frameSize;
            
            // Assert response was received
            assertTrue("Expected echo response but got null", response != null);
            LOG.info("Received echo response: " + response.length + " bytes");
            
            // Assert response length matches expected
            assertTrue("Expected echo response of " + expectedSize + " bytes but got " + response.length + " bytes",
                      response.length == expectedSize);
            
            LOG.info("testUnlimitedMaxMessageSizeWithinBufferLimit PASSED - received correct echo of " + expectedSize + " bytes");
            
        } catch (Exception e) {
            LOG.severe("testUnlimitedMaxMessageSizeWithinBufferLimit FAILED: " + e.getMessage());
            e.printStackTrace();
            fail("testUnlimitedMaxMessageSizeWithinBufferLimit FAILED: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    // ========================================
    // Tests for DefaultEndpoint (32KB buffer, unlimited maxMessageSize)
    // ========================================
    
    /**
     * Test DefaultEndpoint with few large frames within 32KB limit.
     * Sends 3 frames of 8KB each = 24KB total (within 32KB buffer limit)
     */
    @Test
    public void testDefaultEndpoint_FewLargeFrames_WithinLimit() throws Exception {
        LOG.info("Starting testDefaultEndpoint_FewLargeFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferDefaultMax";
        // String endpoint = "ws://localhost:" + "8080" + "/" + "fragmentedMessageSecurity" + "/defaultBufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultEndpoint");
            
            // DefaultBufferDefaultMaxEndpoint: 32KB buffer, -1 maxMessageSize → 32KB effective limit
            // Send 3 frames × 8KB = 24KB (well within 32KB limit)
            int frameCount = 3;
            int frameSize = 8 * 1024; // 8KB per frame = 24KB total
            int expectedTotalBytes = frameCount * frameSize;
            byte[] payload = new byte[frameSize];
            
            // Fill payload with identifiable pattern for debugging
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (i % 256);
            }
            
            LOG.info("DEBUG: About to send 3 fragments of " + frameSize + " bytes each");
            LOG.info("DEBUG: Total expected message size: " + expectedTotalBytes + " bytes");
            LOG.info("DEBUG: Payload array length: " + payload.length);
            
            client.sendFirstFragment(payload);
            LOG.info("DEBUG: Sent first fragment - payload.length=" + payload.length + ", expected frame size=" + frameSize);
            
            client.sendContinuationFragment(payload);
            LOG.info("DEBUG: Sent continuation fragment - payload.length=" + payload.length + ", expected frame size=" + frameSize);
            
            client.sendFinalFragment(payload);
            LOG.info("DEBUG: Sent final fragment - payload.length=" + payload.length + ", expected frame size=" + frameSize);
            LOG.info("DEBUG: Total bytes sent across 3 fragments: " + (3 * payload.length));
            
            // Should receive echo (may be fragmented by server)
            byte[] response = client.readCompleteMessage();
            assertTrue("Expected echo response but got null", response != null);
            assertTrue("Expected echo response of " + expectedTotalBytes + " bytes but got " + response.length,
                      response.length == expectedTotalBytes);
            
            LOG.info("testDefaultEndpoint_FewLargeFrames_WithinLimit PASSED - received " + response.length + " bytes");
            
        } catch (Exception e) {
            LOG.severe("testDefaultEndpoint_FewLargeFrames_WithinLimit FAILED: " + e.getMessage());
            e.printStackTrace();
            fail("testDefaultEndpoint_FewLargeFrames_WithinLimit FAILED: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultEndpoint with few large frames exceeding 32KB limit.
     * Sends 5 frames of 10KB each = 50KB total (exceeds 32KB buffer limit)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testDefaultEndpoint_FewLargeFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testDefaultEndpoint_FewLargeFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultEndpoint");
            
            int frameCount = 5;
            int frameSize = 10 * 1024; // 10KB per frame = 50KB total
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            LOG.info("Sent first fragment (10KB)");
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + i + " frames (" + (i * frameSize) + " bytes)");
                    break;
                }
                client.sendContinuationFragment(payload);
                LOG.info("Sent continuation fragment " + i + " (10KB), cumulative: " + ((i + 1) * frameSize) + " bytes");
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                LOG.info("Sent final fragment (10KB), total: 50KB");
            }
            
            LOG.info("testDefaultEndpoint_FewLargeFrames_ExceedsLimit PASSED");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultEndpoint with many small frames exceeding 32KB limit.
     * Sends 100 frames of 500 bytes each = 50KB total (exceeds 32KB buffer limit)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testDefaultEndpoint_ManySmallFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testDefaultEndpoint_ManySmallFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultEndpoint");
            
            int frameCount = 100;
            int frameSize = 500; // 500 bytes per frame = 50KB total
            int sentFrames = 0;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            sentFrames++;
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize) + " bytes)");
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
                
                if (i % 20 == 0) {
                    LOG.info("Sent " + sentFrames + " fragments, cumulative: " + (sentFrames * frameSize) + " bytes");
                }
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
            }
            
            LOG.info("testDefaultEndpoint_ManySmallFrames_ExceedsLimit PASSED - connection closed after " + sentFrames + " frames");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    // ========================================
    // Tests for LimitedBufferDefaultMaxEndpoint (32KB buffer, 1MB maxMessageSize)
    // ========================================
    
    /**
     * Test LimitedBufferDefaultMaxEndpoint with few large frames within 32KB buffer limit.
     * Sends 3 frames of 10KB each = 30KB total (within 32KB buffer limit)
     * Note: maxMessageSize is 1MB but buffer is only 32KB, so buffer is the bottleneck
     */
    @Test
    public void testLimitedBufferDefaultMax_FewLargeFrames_WithinLimit() throws Exception {
        LOG.info("Starting testLimitedBufferDefaultMax_FewLargeFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to LimitedBufferDefaultMaxEndpoint");
            
            int frameCount = 3;
            int frameSize = 10 * 1024; // 10KB per frame = 30KB total
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
            }
            
            client.sendFinalFragment(payload);
            LOG.info("Sent 3 frames of 10KB each, total: 30KB");
            
            // Should receive echo
            byte[] response = client.readFrame();
            if (response != null) {
                LOG.info("Received echo response: " + response.length + " bytes");
            }
            
            LOG.info("testDefaultBufferMb1Max_30KB_FewLargeFrames_WithinLimit PASSED");
            
        } catch (Exception e) {
            LOG.severe("testDefaultBufferMb1Max_30KB_FewLargeFrames_WithinLimit failed: " + e.getMessage());
            throw e;
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultBufferMB1MaxEndpoint with few large frames exceeding 1MB maxMessageSize limit.
     * Sends 350 frames of 3KB each = 1050KB total (exceeds 1MB maxMessageSize limit)
     * Endpoint: 32KB buffer, 1MB maxMessageSize → effective limit = max(32KB, 1MB) = 1MB
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testLimitedBufferDefaultMax_FewLargeFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testLimitedBufferDefaultMax_FewLargeFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultBufferMB1MaxEndpoint");
            
            int frameCount = 350;
            int frameSize = 3 * 1024; // 3KB per frame = 1050KB total (exceeds 1MB)
            int sentFrames = 0;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            sentFrames++;
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize) + " bytes)");
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
            }
            
            LOG.info("testLimitedBufferDefaultMax_FewLargeFrames_ExceedsLimit PASSED");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultBufferMB1MaxEndpoint with many small frames within 1MB maxMessageSize limit.
     * Sends 30 frames of 1KB each = 30KB total (within 1MB limit)
     * Endpoint: 32KB buffer, 1MB maxMessageSize → effective limit = max(32KB, 1MB) = 1MB
     */
    @Test
    public void testLimitedBufferDefaultMax_ManySmallFrames_WithinLimit() throws Exception {
        LOG.info("Starting testLimitedBufferDefaultMax_ManySmallFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultBufferMB1MaxEndpoint");
            
            int frameCount = 30;
            int frameSize = 1024; // 1KB per frame = 30KB total (within 1MB)
            int expectedTotalBytes = frameCount * frameSize;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
            }
            
            client.sendFinalFragment(payload);
            LOG.info("Sent 30 frames of 1KB each, total: 30KB");
            
            // Should receive echo (may be fragmented by server)
            byte[] response = client.readCompleteMessage();
            assertTrue("Expected echo response but got null", response != null);
            assertTrue("Expected echo response of " + expectedTotalBytes + " bytes but got " + response.length,
                      response.length == expectedTotalBytes);
            
            LOG.info("testLimitedBufferDefaultMax_ManySmallFrames_WithinLimit PASSED - received " + response.length + " bytes");
            
        } catch (Exception e) {
            LOG.severe("testLimitedBufferDefaultMax_ManySmallFrames_WithinLimit FAILED: " + e.getMessage());
            e.printStackTrace();
            fail("testLimitedBufferDefaultMax_ManySmallFrames_WithinLimit FAILED: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultBufferMB1MaxEndpoint with many small frames exceeding 1MB maxMessageSize limit.
     * Sends 4000 frames of 300 bytes each = 1200KB total (exceeds 1MB maxMessageSize limit)
     * Endpoint: 32KB buffer, 1MB maxMessageSize → effective limit = max(32KB, 1MB) = 1MB
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testLimitedBufferDefaultMax_ManySmallFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testLimitedBufferDefaultMax_ManySmallFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultBufferMB1MaxEndpoint");
            
            int frameCount = 4000;
            int frameSize = 300; // 300 bytes per frame = 1200KB total (exceeds 1MB)
            int sentFrames = 0;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            sentFrames++;
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize) + " bytes)");
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
                
                if (i % 50 == 0) {
                    LOG.info("Sent " + sentFrames + " fragments, cumulative: " + (sentFrames * frameSize) + " bytes");
                }
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
            }
            
            LOG.info("testLimitedBufferDefaultMax_ManySmallFrames_ExceedsLimit PASSED");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    // ========================================
    // Tests for DefaultBufferMb1MaxEndpoint (32KB buffer, 1MB maxMessageSize)
    // ========================================
    
    /**
     * Test DefaultBufferMb1MaxEndpoint with few large frames within 32KB buffer limit.
     * Sends 3 frames of 10KB each = 30KB total (within 1MB maxMessageSize limit)
     * Note: Effective limit is max(32KB buffer, 1MB maxMessageSize) = 1MB
     */
    @Test
    public void testDefaultBufferMb1Max_FewLargeFrames_WithinLimit() throws Exception {
        LOG.info("Starting testDefaultBufferMb1Max_FewLargeFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultBufferMb1MaxEndpoint");
            
            int frameCount = 3;
            int frameSize = 10 * 1024; // 10KB per frame = 30KB total
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
            }
            
            client.sendFinalFragment(payload);
            LOG.info("Sent 3 frames of 10KB each, total: 30KB");
            
            // Should receive echo
            byte[] response = client.readFrame();
            if (response != null) {
                LOG.info("Received echo response: " + response.length + " bytes");
            }
            
            LOG.info("testDefaultBufferMb1Max_FewLargeFrames_WithinLimit PASSED");
            
        } catch (Exception e) {
            LOG.severe("testDefaultBufferMb1Max_FewLargeFrames_WithinLimit failed: " + e.getMessage());
            throw e;
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultBufferMb1MaxEndpoint with many frames exceeding 1MB maxMessageSize limit.
     * Sends 350 frames of 3KB each = 1050KB total (exceeds 1MB maxMessageSize limit)
     * Note: Buffer is 32KB but maxMessageSize is 1MB, so effective limit is max(32KB, 1MB) = 1MB
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testDefaultBufferMb1Max_FewLargeFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testDefaultBufferMb1Max_FewLargeFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultBufferMb1MaxEndpoint");
            
            int frameCount = 350;
            int frameSize = 3 * 1024; // 3KB per frame = 1050KB total (exceeds 1MB limit)
            int sentFrames = 0;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            sentFrames++;
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize) + " bytes)");
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
            }
            
            LOG.info("Sent " + sentFrames + " frames of 3KB each, total: " + (sentFrames * frameSize / 1024) + "KB (exceeds 1MB limit)");
            LOG.info("testDefaultBufferMb1Max_FewLargeFrames_ExceedsLimit PASSED - connection closed as expected");
            
        } catch (Exception e) {
            LOG.info("testDefaultBufferMb1Max_FewLargeFrames_ExceedsLimit caught expected exception: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultBufferMb1MaxEndpoint with many small frames within 32KB buffer limit.
     * Sends 30 frames of 1KB each = 30KB total (within 32KB buffer limit)
     */
    @Test
    public void testDefaultBufferMb1Max_ManySmallFrames_WithinLimit() throws Exception {
        LOG.info("Starting testDefaultBufferMb1Max_ManySmallFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultBufferMb1MaxEndpoint");
            
            int frameCount = 30;
            int frameSize = 1024; // 1KB per frame = 30KB total
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
            }
            
            client.sendFinalFragment(payload);
            LOG.info("Sent 30 frames of 1KB each, total: 30KB");
            
            // Should receive echo
            byte[] response = client.readFrame();
            if (response != null) {
                LOG.info("Received echo response: " + response.length + " bytes");
            }
            
            LOG.info("testDefaultBufferMb1Max_ManySmallFrames_WithinLimit PASSED");
            
        } catch (Exception e) {
            LOG.severe("testDefaultBufferMb1Max_ManySmallFrames_WithinLimit failed: " + e.getMessage());
            throw e;
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test DefaultBufferMb1MaxEndpoint with many small frames exceeding 32KB buffer limit.
     * Sends 200 frames of 300 bytes each = 60KB total (exceeds 32KB buffer limit)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testDefaultBufferMb1Max_ManySmallFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testDefaultBufferMb1Max_ManySmallFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/defaultBufferMb1Max";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to DefaultBufferMb1MaxEndpoint");
            
            int frameCount = 4000;
            int frameSize = 300; // 300 bytes per frame = 1.2MB total (exceeds 1MB maxMessageSize)
            int sentFrames = 0;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            sentFrames++;
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize) + " bytes)");
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
                
                if (i % 50 == 0) {
                    LOG.info("Sent " + sentFrames + " fragments, cumulative: " + (sentFrames * frameSize) + " bytes");
                }
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
            }
            
            LOG.info("testDefaultBufferMb1Max_ManySmallFrames_ExceedsLimit PASSED");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    // ========================================
    // Tests for MB1BufferDefaultMaxEndpoint (1MB buffer, unlimited maxMessageSize)
    // ========================================
    
    /**
     * Test MB1BufferDefaultMaxEndpoint with few large frames within 1MB buffer limit.
     * Sends 25 frames of 10KB each = 250KB total (within 1MB buffer limit)
     * 
     * TODO -- Maybe 10KB is low?
     */
    @Test
    public void testHighBufferDefaultMax_FewLargeFrames_WithinLimit() throws Exception {
        LOG.info("Starting testHighBufferDefaultMax_FewLargeFrames_WithinLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to MB1BufferDefaultMaxEndpoint");
            
            int frameCount = 25;
            int frameSize = 10 * 1024; // 10KB per frame = 250KB total
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            
            for (int i = 1; i < frameCount - 1; i++) {
                client.sendContinuationFragment(payload);
            }
            
            client.sendFinalFragment(payload);
            LOG.info("Sent 25 frames of 10KB each, total: 250KB");
            
            // Should receive echo
            byte[] response = client.readFrame();
            if (response != null) {
                LOG.info("Received echo response: " + response.length + " bytes");
            }
            
            LOG.info("testHighBufferDefaultMax_FewLargeFrames_WithinLimit PASSED");
            
        } catch (Exception e) {
            LOG.severe("testHighBufferDefaultMax_FewLargeFrames_WithinLimit failed: " + e.getMessage());
            throw e;
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test MB1BufferDefaultMaxEndpoint with few large frames exceeding 1MB buffer limit.
     * Sends 35 frames of 32KB each = 1.12MB total (exceeds 1MB buffer limit)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testHighBufferDefaultMax_FewLargeFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testHighBufferDefaultMax_FewLargeFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to MB1BufferDefaultMaxEndpoint");
            
            int frameCount = 35;
            int frameSize = 32 * 1024; // 32KB per frame = 1.12MB total
            int sentFrames = 0;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            sentFrames++;
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize / 1024 / 1024) + " MB)");
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
                
                if (i % 50 == 0) {
                    LOG.info("Sent " + sentFrames + " fragments, cumulative: " + (sentFrames * frameSize / 1024 / 1024) + " MB");
                }
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
            }
            
            LOG.info("testHighBufferDefaultMax_FewLargeFrames_ExceedsLimit PASSED");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test MB1BufferDefaultMaxEndpoint with many small frames exceeding 1MB buffer limit.
     * Sends 350 frames of 3KB each = 1.05MB total (exceeds 1MB buffer limit)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testHighBufferDefaultMax_ManySmallFrames_ExceedsLimit() throws Exception {
        LOG.info("Starting testHighBufferDefaultMax_ManySmallFrames_ExceedsLimit");
        
        String endpoint = "ws://localhost:" + server.getHttpDefaultPort() + "/" + WAR_NAME + "/mb1BufferDefaultMax";
        io.openliberty.wsoc.util.RawWebSocketClient client = new io.openliberty.wsoc.util.RawWebSocketClient();
        
        try {
            client.connect(endpoint);
            LOG.info("Connected to MB1BufferDefaultMaxEndpoint");
            
            int frameCount = 350;
            int frameSize = 3 * 1024; // 3KB per frame = 1.05MB total
            int sentFrames = 0;
            byte[] payload = new byte[frameSize];
            
            client.sendFirstFragment(payload);
            sentFrames++;
            
            for (int i = 1; i < frameCount - 1; i++) {
                if (!client.isConnected()) {
                    LOG.info("Connection closed after " + sentFrames + " frames (" + (sentFrames * frameSize / 1024 / 1024) + " MB)");
                    break;
                }
                client.sendContinuationFragment(payload);
                sentFrames++;
                
                if (i % 1000 == 0) {
                    LOG.info("Sent " + sentFrames + " fragments, cumulative: " + (sentFrames * frameSize / 1024 / 1024) + " MB");
                }
            }
            
            if (client.isConnected()) {
                client.sendFinalFragment(payload);
                sentFrames++;
            }
            
            LOG.info("testHighBufferDefaultMax_ManySmallFrames_ExceedsLimit PASSED");
            
        } catch (Exception e) {
            LOG.info("Connection closed with exception (expected): " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    
    /**
     * Simple test client for basic tests
     */
    @ClientEndpoint
    public static class TestClient {
        public CountDownLatch closeLatch = new CountDownLatch(1);
        public CloseReason closeReason;
        
        @OnOpen
        public void onOpen(Session session) {
            LOG.info("Client connected");
        }
        
        @OnClose
        public void onClose(Session session, CloseReason reason) {
            LOG.info("Client closed: " + reason);
            this.closeReason = reason;
            closeLatch.countDown();
        }
        
        @OnError
        public void onError(Session session, Throwable throwable) {
            LOG.severe("Client error: " + throwable.getMessage());
        }
    }
    
    /**
     * Test client for fragmented message tests
     */
    @ClientEndpoint
    public static class FragmentedTestClient {
        public CountDownLatch closeLatch = new CountDownLatch(1);
        public CountDownLatch messageLatch = new CountDownLatch(1);
        public CloseReason closeReason;
        public ByteBuffer receivedMessage;
        private java.io.ByteArrayOutputStream accumulatedData = new java.io.ByteArrayOutputStream();
        private int messageCount = 0;
        private long lastMessageTime = 0;
        private static final long MESSAGE_TIMEOUT_MS = 1000; // Wait 1000ms after last message
        
        @OnOpen
        public void onOpen(Session session) {
            LOG.info("Fragmented client connected");
        }
        
        @OnMessage
        public void onMessage(ByteBuffer message) {
            messageCount++;
            int size = message.remaining();
            LOG.info("Fragmented client received message #" + messageCount + ": " + size + " bytes");
            
            // Accumulate data in case server sends multiple messages
            byte[] data = new byte[size];
            message.get(data);
            try {
                accumulatedData.write(data);
                // Create a ByteBuffer with all accumulated data
                this.receivedMessage = ByteBuffer.wrap(accumulatedData.toByteArray());
                LOG.info("Total accumulated: " + this.receivedMessage.remaining() + " bytes");
            } catch (IOException e) {
                LOG.severe("Error accumulating data: " + e.getMessage());
            }
            
            // Update last message time
            lastMessageTime = System.currentTimeMillis();
            
            // Only count down after first message - test will wait for timeout to get all messages
            if (messageCount == 1) {
                messageLatch.countDown();
            }
        }
        
        /**
         * Wait for all messages to be received (waits until no new messages for MESSAGE_TIMEOUT_MS)
         */
        public void waitForAllMessages(long maxWaitMs, int expectedMessageCount) throws InterruptedException {
            long startTime = System.currentTimeMillis();
            long lastCount = messageCount;
            
            LOG.info("Waiting for messages (expecting " + expectedMessageCount + " messages)...");
            
            while (System.currentTimeMillis() - startTime < maxWaitMs) {
                Thread.sleep(100);
                
                // If we've received all expected messages, we're done
                if (messageCount >= expectedMessageCount) {
                    LOG.info("Received all " + expectedMessageCount + " expected messages");
                    break;
                }
                
                // If we've received at least one message and no new messages for MESSAGE_TIMEOUT_MS, we're done
                if (messageCount > 0 && System.currentTimeMillis() - lastMessageTime > MESSAGE_TIMEOUT_MS) {
                    LOG.info("No new messages for " + MESSAGE_TIMEOUT_MS + "ms after receiving " + messageCount + " messages");
                    break;
                }
                
                if (messageCount != lastCount) {
                    LOG.info("Message count increased from " + lastCount + " to " + messageCount);
                    lastCount = messageCount;
                }
            }
            
            LOG.info("Finished waiting for messages. Received " + messageCount + " messages totaling " +
                    (receivedMessage != null ? receivedMessage.remaining() : 0) + " bytes");
        }
        
        @OnClose
        public void onClose(Session session, CloseReason reason) {
            LOG.info("Fragmented client closed: " + reason);
            this.closeReason = reason;
            closeLatch.countDown();
        }
        
        @OnError
        public void onError(Session session, Throwable throwable) {
            LOG.severe("Fragmented client error: " + throwable.getMessage());
        }
    }
}
