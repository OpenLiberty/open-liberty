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
 * WebSocket endpoint with 1MB maxMessageSize limit & 1MB buffer limit.
 * Used to test that fragmented messages exceeding this limit are rejected.
 */
@ServerEndpoint(value = "/mb1BufferMb1Max")
public class MB1BufferMB1MaxEndpoint {
    
    private static final Logger LOG = Logger.getLogger(MB1BufferMB1MaxEndpoint.class.getName());
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB
    
    @OnOpen
    public void onOpen(Session session) {
        LOG.info("MB1BufferMB1MaxEndpoint opened");
        
        // Set maxMessageSize to 1MB
        session.setMaxBinaryMessageBufferSize(MAX_MESSAGE_SIZE);
        session.setMaxTextMessageBufferSize(MAX_MESSAGE_SIZE);
        
        LOG.info("Set maxBinaryMessageBufferSize to: " + MAX_MESSAGE_SIZE);
        LOG.info("Set maxTextMessageBufferSize to: " + MAX_MESSAGE_SIZE);
    }
    
    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public void onMessage(ByteBuffer message, Session session) {
        LOG.info("MB1BufferMB1MaxEndpoint received message: " + message.remaining() + " bytes");
        
        try {
            // Echo the message back
            session.getBasicRemote().sendBinary(message);
            LOG.info("Echoed message back to client");
        } catch (IOException e) {
            LOG.severe("Error echoing message: " + e.getMessage());
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.severe("MB1BufferMB1MaxEndpoint error: " + throwable.getMessage());
    }
}

