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

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/alternate")
public class AlternateEndpoint {

    public static Session currentSession;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        currentSession = session;
        System.out.println("Alternate SOCKET OPENED");
    }

    @OnMessage
    public void onMessage(String message) throws IOException, InterruptedException {
        System.out.println("message received alternate Server: " + message);
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
        System.out.println("closing alternate socket endpoint");
    }

    public static void send(String message) throws IOException {
        currentSession.getBasicRemote().sendText(message);
    }

}