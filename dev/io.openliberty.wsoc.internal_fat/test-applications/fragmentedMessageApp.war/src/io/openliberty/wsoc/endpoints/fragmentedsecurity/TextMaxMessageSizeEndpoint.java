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
 * WebSocket endpoint with maxMessageSize applied to text message handler.
 * Tests that buffer size is synced when maxMessageSize is set on @OnMessage for text.
 */
@ServerEndpoint(value = "/textMaxMessageSize")
public class TextMaxMessageSizeEndpoint {
    
    private static final Logger LOG = Logger.getLogger(TextMaxMessageSizeEndpoint.class.getName());
    private static final int MAX_TEXT_MESSAGE_SIZE = 512 * 1024; // 512KB
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("TextMaxMessageSizeEndpoint opened");
        System.out.println("Default maxBinaryMessageBufferSize: " + session.getMaxBinaryMessageBufferSize());
        System.out.println("Default maxTextMessageBufferSize: " + session.getMaxTextMessageBufferSize());
        System.out.println("maxMessageSize for text handler set to: " + MAX_TEXT_MESSAGE_SIZE);
    }
    
    @OnMessage
    public void onMessage(ByteBuffer message, Session session) {
        System.out.println("TextMaxMessageSizeEndpoint received binary message: " + message.remaining() + " bytes");
        
        try {
            // Echo the message back
            session.getBasicRemote().sendBinary(message);
            System.out.println("Echoed binary message back to client");
        } catch (IOException e) {
            LOG.severe("Error echoing binary message: " + e.getMessage());
        }
    }
    
    @OnMessage(maxMessageSize = MAX_TEXT_MESSAGE_SIZE)
    public void onTextMessage(String message, Session session) {
        System.out.println("TextMaxMessageSizeEndpoint received text message: " + message);
        
        try {
            if ("GET_BUFFER_SIZE".equals(message)) {
                int binaryBufferSize = session.getMaxBinaryMessageBufferSize();
                int textBufferSize = session.getMaxTextMessageBufferSize();
                
                System.out.println("Current buffer sizes - Binary: " + binaryBufferSize +
                                   ", Text: " + textBufferSize);
                
                // Send text buffer size info back to client
                String response = "BUFFER_SIZE:TEXT:" + textBufferSize;
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
        LOG.severe("TextMaxMessageSizeEndpoint error: " + throwable.getMessage());
    }
}