/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 * WebSocket endpoint with default buffer size and 1 MB maxMessageSize.
 */
@ServerEndpoint(value = "/defaultBufferMb1Max")
public class DefaultBufferMB1MaxEndpoint {
    
    private static final Logger LOG = Logger.getLogger(DefaultBufferMB1MaxEndpoint.class.getName());
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("DefaultBufferMB1MaxEndpoint opened");
        System.out.println("Using default maxBinaryMessageBufferSize: " + session.getMaxBinaryMessageBufferSize());
        System.out.println("Using default maxTextMessageBufferSize: " + session.getMaxTextMessageBufferSize());
        System.out.println("maxMessageSize set to: " + MAX_MESSAGE_SIZE);
    }
    
    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public void onMessage(ByteBuffer message, Session session) {
        System.out.println("DefaultBufferMB1MaxEndpoint received message: " + message.remaining() + " bytes");
        
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
        System.out.println("DefaultBufferMB1MaxEndpoint received text message: " + message);
        
        try {
            if ("GET_BUFFER_SIZE".equals(message)) {
                int binaryBufferSize = session.getMaxBinaryMessageBufferSize();
                int textBufferSize = session.getMaxTextMessageBufferSize();
                
                System.out.println("Current buffer sizes - Binary: " + binaryBufferSize +
                                   ", Text: " + textBufferSize);
                
                String response = "BUFFER_SIZE:BINARY:" + binaryBufferSize;
                session.getBasicRemote().sendText(response);
                System.out.println("Sent buffer size response: " + response);
            } else {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            LOG.severe("Error handling text message: " + e.getMessage());
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.severe("DefaultBufferMB1MaxEndpoint error: " + throwable.getMessage());
    }
}
