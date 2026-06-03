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
 * WebSocket endpoint with all default settings and both binary and text message handlers.
 * This endpoint has no maxMessageSize specified on any @OnMessage handler.
 * Used to test default buffer behavior with both message types.
 */
@ServerEndpoint(value = "/default")
public class DefaultEndpoint {
    
    private static final Logger LOG = Logger.getLogger(DefaultEndpoint.class.getName());
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("DefaultEndpoint opened");
        System.out.println("Default maxBinaryMessageBufferSize: " + session.getMaxBinaryMessageBufferSize());
        System.out.println("Default maxTextMessageBufferSize: " + session.getMaxTextMessageBufferSize());
        System.out.println("No maxMessageSize specified on any @OnMessage handler");
    }
    
    @OnMessage
    public void onBinaryMessage(ByteBuffer message, Session session) {
        System.out.println("DefaultEndpoint received binary message: " + message.remaining() + " bytes");
        
        try {
            // Echo the message back
            session.getBasicRemote().sendBinary(message);
            System.out.println("Echoed binary message back to client");
        } catch (IOException e) {
            LOG.severe("Error echoing binary message: " + e.getMessage());
        }
    }
    
    @OnMessage
    public void onTextMessage(String message, Session session) {
        System.out.println("DefaultEndpoint received text message: " + message);
        
        try {
            if ("GET_BUFFER_SIZE".equals(message)) {
                int binaryBufferSize = session.getMaxBinaryMessageBufferSize();
                int textBufferSize = session.getMaxTextMessageBufferSize();
                
                System.out.println("Current buffer sizes - Binary: " + binaryBufferSize +
                                   ", Text: " + textBufferSize);
                
                // Send both buffer sizes back to client
                String response = "BUFFER_SIZE:BINARY:" + binaryBufferSize + ":TEXT:" + textBufferSize;
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
        LOG.severe("DefaultEndpoint error: " + throwable.getMessage());
    }
}
