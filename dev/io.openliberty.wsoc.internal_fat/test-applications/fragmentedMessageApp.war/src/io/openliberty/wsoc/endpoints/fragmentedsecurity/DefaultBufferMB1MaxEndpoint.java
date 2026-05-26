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
        LOG.info("DefaultBufferMB1MaxEndpoint opened");
        LOG.info("Using default maxBinaryMessageBufferSize: " + session.getMaxBinaryMessageBufferSize());
        LOG.info("Using default maxTextMessageBufferSize: " + session.getMaxTextMessageBufferSize());
        LOG.info("maxMessageSize set to: " + MAX_MESSAGE_SIZE);
    }
    
    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public void onMessage(ByteBuffer message, Session session) {
        LOG.info("DefaultBufferMB1MaxEndpoint received message: " + message.remaining() + " bytes");
        
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
        LOG.severe("DefaultBufferMB1MaxEndpoint error: " + throwable.getMessage());
    }
}
