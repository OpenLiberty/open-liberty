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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * A simple endpoint that accumulates all parital messages sent to it.
 * Used for manual testing. 
 */
@ServerEndpoint(value = "/unlimitedAccumulator")
public class UnlimitedAccumulatorEndpoint {
    
    private static final Logger LOG = Logger.getLogger(UnlimitedAccumulatorEndpoint.class.getName());
    
    // Thread-safe: Each session gets its own list
    private static final String FRAGMENTS_KEY = "fragments";
    private static final String TOTAL_SIZE_KEY = "totalSize";
    private int  totalSize = 0;
    @OnOpen
    public void onOpen(Session session) {
        LOG.info("UnlimitedAccumulatorEndpoint opened");
        
        // Do NOT set buffer sizes - use defaults
        LOG.info("Default maxBinaryMessageBufferSize: " + session.getMaxBinaryMessageBufferSize());
        LOG.info("Default maxTextMessageBufferSize: " + session.getMaxTextMessageBufferSize());
        
    }
    
    /**
     * Partial message handler - receives fragments WITHOUT checking size.
     * This tests if the WebSocket layer enforces cumulative limits.
     */
    @OnMessage
    @SuppressWarnings("unchecked")
    public void onMessage(ByteBuffer message, boolean last, Session session) {        
        int fragmentSize = message.remaining();
        totalSize += fragmentSize;
        
        LOG.info("UnlimitedAccumulatorEndpoint received fragment: " + fragmentSize +
                 " bytes, last=" + last + ", total accumulated: " + totalSize + " bytes");
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.severe("UnlimitedAccumulatorEndpoint error: " + throwable.getMessage());
        throwable.printStackTrace();
    }
}
