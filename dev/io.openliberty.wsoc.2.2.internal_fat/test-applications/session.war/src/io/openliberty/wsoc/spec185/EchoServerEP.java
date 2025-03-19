/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.spec185;

import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;

import jakarta.websocket.server.ServerEndpoint;

/*
 * Echos messages sent to this endpoint.
 * Used for testing GetResultTestSession
 */
@ServerEndpoint(value = "/echo")
public class EchoServerEP {

    @OnOpen
    public void onOpen(final Session session) {

    }

    // asynchronous text message delivery using a callback

    @OnMessage
    public void onMsg(String msg, Session session) {
        // Print out session ID before the message is sent
        System.out.println("MSG SESSION: " + session.getId());
        session.getAsyncRemote().sendText("got your message ", new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                if (result.getSession() != null){
                    // Print out session ID after message is sent to verify it is not null and is the same as pre-message
                    System.out.println("RESULT SESSION: " + result.getSession().getId());
                }
            }
        });
    }

}
