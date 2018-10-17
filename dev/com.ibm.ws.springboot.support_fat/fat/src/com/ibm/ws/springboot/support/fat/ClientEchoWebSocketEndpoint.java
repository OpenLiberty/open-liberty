/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Client Endpoint for WebSocketTests20
 */
@ClientEndpoint
public class ClientEchoWebSocketEndpoint {

    private Session session;
    private volatile String messageFromServer;
    private CountDownLatch latch;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                setMessageFromServer(message);
            }
        });
    }

    private void setMessageFromServer(String message) {
        this.messageFromServer = message;
        latch.countDown();
    }

    public String getMessageFromServer() {
        return this.messageFromServer;
    }

    public void sendMessage(String message, CountDownLatch latch) throws IOException {
        if (this.session != null && this.session.isOpen()) {
            Log.info(getClass(), "sendMessage", message);
            this.latch = latch;
            this.session.getBasicRemote().sendText(message);
        }
    }
}
