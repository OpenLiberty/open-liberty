/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package webSocketTest;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/chat")
public class DefaultEndpoint {

    public static Session currentSession;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        currentSession = session;
        System.out.println("SOCKET OPENED");
    }

    @OnMessage
    public void onMessage(String message) throws IOException, InterruptedException {
        System.out.println("message received Server: " + message);
        Thread.sleep(2500);
        send(message);
    }

    @OnClose
    public void closing() {
        try {
            currentSession.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("closing socket endpoint");
    }

    public static void send(String message) throws IOException {
        currentSession.getBasicRemote().sendText(message);
    }

}