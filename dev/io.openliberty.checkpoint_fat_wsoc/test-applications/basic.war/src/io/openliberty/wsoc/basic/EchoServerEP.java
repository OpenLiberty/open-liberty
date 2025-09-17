/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.basic;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import javax.websocket.server.ServerEndpoint;

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
        System.out.println("EchoServerEP#onMsg: " + msg);
        session.getAsyncRemote().sendText("got your message " + msg);
    }

}
