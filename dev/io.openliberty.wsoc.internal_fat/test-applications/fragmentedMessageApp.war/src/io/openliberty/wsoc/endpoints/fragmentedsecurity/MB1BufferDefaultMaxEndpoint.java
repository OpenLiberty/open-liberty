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
 * WebSocket endpoint with 1MB buffer size and default maxMessageSize.
 */
@ServerEndpoint(value = "/mb1BufferDefaultMax")
public class MB1BufferDefaultMaxEndpoint {
    
    private static final Logger LOG = Logger.getLogger(MB1BufferDefaultMaxEndpoint.class.getName());
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("MB1BufferDefaultMaxEndpoint opened (1MB buffer)");
        
        // Set 1MB buffer sizes
        session.setMaxBinaryMessageBufferSize(BUFFER_SIZE);
        session.setMaxTextMessageBufferSize(BUFFER_SIZE);
        
        System.out.println("Set maxBinaryMessageBufferSize to: " + BUFFER_SIZE + " (1MB)");
        System.out.println("Set maxTextMessageBufferSize to: " + BUFFER_SIZE + " (1MB)");
        System.out.println("maxMessageSize: unlimited (no @OnMessage maxMessageSize set)");
    }
    
    @OnMessage
    public void onMessage(ByteBuffer message, Session session) {
        System.out.println("MB1BufferDefaultMaxEndpoint received message: " + message.remaining() + " bytes");
        
        try {
            // Echo the message back
            session.getBasicRemote().sendBinary(message);
            System.out.println("Echoed message back to client");
        } catch (IOException e) {
            LOG.severe("Error echoing message: " + e.getMessage());
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.severe("MB1BufferDefaultMaxEndpoint error: " + throwable.getMessage());
    }
}
