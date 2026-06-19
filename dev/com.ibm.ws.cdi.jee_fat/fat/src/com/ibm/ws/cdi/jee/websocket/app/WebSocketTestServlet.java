/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.websocket.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/testServlet")
public class WebSocketTestServlet extends FATServlet {

    @Test
    public void testWebSocketCDIMethodParameterInjection() throws Exception {
        // Get the WebSocket URL
        String host = System.getProperty("liberty.test.hostname", "localhost");
        String port = System.getProperty("liberty.test.port", "8010");
        String contextRoot = System.getProperty("liberty.test.context.root", "webSocketCDIApp");
        
        String wsUrl = "ws://" + host + ":" + port + "/" + contextRoot + "/testWebSocket";
        System.out.println("Connecting to WebSocket at: " + wsUrl);
        
        TestClient client = new TestClient();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        
        Session session = null;
        try {
            session = container.connectToServer(client, new URI(wsUrl));
            
            // Wait for connection to be established
            assertTrue("WebSocket connection not opened", client.openLatch.await(10, TimeUnit.SECONDS));
            
            // Send message to get injection status
            session.getBasicRemote().sendText("getStatus");
            
            // Wait for response
            assertTrue("No response received from WebSocket", client.messageLatch.await(10, TimeUnit.SECONDS));
            
            // Check if injection was successful
            String response = client.lastMessage;
            assertNotNull("Response should not be null", response);
            System.out.println("Received response: " + response);
            
            // Verify successful injection
            assertTrue("Expected successful injection message but got: " + response, 
                       response.contains("CDI Bean successfully injected"));
            
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Simple WebSocket client for testing
     */
    @ClientEndpoint
    public static class TestClient {
        public CountDownLatch openLatch = new CountDownLatch(1);
        public CountDownLatch messageLatch = new CountDownLatch(1);
        public String lastMessage = null;

        @OnOpen
        public void onOpen(Session session) {
            System.out.println("Client connected to WebSocket");
            openLatch.countDown();
        }

        @OnMessage
        public void onMessage(String message) {
            System.out.println("Client received message: " + message);
            lastMessage = message;
            messageLatch.countDown();
        }

        @OnClose
        public void onClose(CloseReason reason) {
            System.out.println("Client connection closed: " + reason);
        }
    }
}

// Made with Bob
