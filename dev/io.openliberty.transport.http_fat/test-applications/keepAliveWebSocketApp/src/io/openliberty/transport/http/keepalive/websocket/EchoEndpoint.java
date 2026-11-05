/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.keepalive.websocket;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint that echoes every text message back to the client.
 *
 * <p>The echo lets KeepAliveWebSocketTest prove the WebSocket connection is
 * alive after the handshake: it sends one frame and expects the same text back.
 * If the server had closed the socket, the echo never arrives.
 */
@ServerEndpoint("/echo")
public class EchoEndpoint {

    @OnMessage
    public String echo(String message) {
        return message;
    }
}
