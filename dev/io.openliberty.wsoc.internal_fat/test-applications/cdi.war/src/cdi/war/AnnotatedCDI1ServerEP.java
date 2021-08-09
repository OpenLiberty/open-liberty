/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.war;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

// JavaScript to access from a WebSocket capable browser would be:  ws://<Host Name>:<port>/<Context-Root>/SimpleAnnotatedCDI1

@ServerEndpoint(value = "/SimpleAnnotatedCDI1")
public class AnnotatedCDI1ServerEP {
    Session currentSession = null;
    int count = 0;

    static String interceptorMessage = "no message yet";

    public AnnotatedCDI1ServerEP() {}

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        currentSession = session;
    }

    @OnMessage
    @InterceptMeBinding
    public void receiveMessageCDI(String message) {

        try {
            currentSession.getBasicRemote().sendText(interceptorMessage);
        } catch (IOException e) {
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

    }

    @OnError
    public void onError(Session session, Throwable t) {}

}
