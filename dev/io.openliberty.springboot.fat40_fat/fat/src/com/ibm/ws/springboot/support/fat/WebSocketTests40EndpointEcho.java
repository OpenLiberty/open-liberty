/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Client Endpoint for WebSocketTests30
 */
@ClientEndpoint
public class WebSocketTests40EndpointEcho {

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
