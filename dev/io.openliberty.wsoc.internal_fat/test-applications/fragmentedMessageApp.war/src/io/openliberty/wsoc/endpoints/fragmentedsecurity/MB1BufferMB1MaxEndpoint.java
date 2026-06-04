/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.fragmentedsecurity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint with 1MB maxMessageSize limit & 1MB buffer limit.
 * Used to test that fragmented messages exceeding this limit are rejected.
 */
@ServerEndpoint(value = "/mb1BufferMb1Max")
public class MB1BufferMB1MaxEndpoint {
    
    private static final Logger LOG = Logger.getLogger(MB1BufferMB1MaxEndpoint.class.getName());
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("MB1BufferMB1MaxEndpoint opened");
        
        // Set maxMessageSize to 1MB
        session.setMaxBinaryMessageBufferSize(MAX_MESSAGE_SIZE);
        session.setMaxTextMessageBufferSize(MAX_MESSAGE_SIZE);
        
        System.out.println("Set maxBinaryMessageBufferSize to: " + MAX_MESSAGE_SIZE);
        System.out.println("Set maxTextMessageBufferSize to: " + MAX_MESSAGE_SIZE);
    }
    
    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public void onMessage(ByteBuffer message, Session session) {
        System.out.println("MB1BufferMB1MaxEndpoint received message: " + message.remaining() + " bytes");
        
        try {
            // Echo the message back
            session.getBasicRemote().sendBinary(message);
            System.out.println("Echoed message back to client");
        } catch (IOException e) {
            LOG.severe("Error echoing message: " + e.getMessage());
        }
    }
    
    @OnMessage
    public void onTextMessage(String message, Session session) {
        System.out.println("MB1BufferMB1MaxEndpoint received text message: " + message);
        
        try {
            // Handle buffer size query
            if ("GET_BUFFER_SIZE".equals(message)) {
                int binaryBufferSize = session.getMaxBinaryMessageBufferSize();
                int textBufferSize = session.getMaxTextMessageBufferSize();
                
                System.out.println("Current buffer sizes - Binary: " + binaryBufferSize +
                                   ", Text: " + textBufferSize);
                
                // Send buffer size info back to client
                String response = "BUFFER_SIZE:BINARY:" + binaryBufferSize;
                session.getBasicRemote().sendText(response);
                System.out.println("Sent buffer size response: " + response);
            } else {
                // Echo other text messages
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            LOG.severe("Error handling text message: " + e.getMessage());
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.severe("MB1BufferMB1MaxEndpoint error: " + throwable.getMessage());
    }
}

