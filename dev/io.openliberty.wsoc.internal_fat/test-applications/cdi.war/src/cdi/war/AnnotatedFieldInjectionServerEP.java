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

import javax.inject.Inject;
import javax.inject.Named;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

// JavaScript to access from a WebSocket capable browser would be:  ws://<Host Name>:<port>/<Context-Root>/EPFieldInjectionCDI

@ServerEndpoint(value = "/EPFieldInjectionCDI")
public class AnnotatedFieldInjectionServerEP {
    Session currentSession = null;

    String responseMessage = "no message yet";

    public @Inject
    SimpleWebSocketBean bean;

    public @Inject
    @Named("Producer1")
    StringBuffer N1;

    public @Inject
    CounterSessionScoped sesScopedCounter;

    public AnnotatedFieldInjectionServerEP() {

    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        currentSession = session;

        // we should be able to use injected stuff here
        responseMessage = N1.toString();

        // verify the session scope will work in onOpen
        sesScopedCounter.getNext();
        sesScopedCounter.getNext();

    }

    @OnMessage
    public void receiveMessageCDI(String message) {

        // and use injected stuff here
        responseMessage = responseMessage + bean.getResponse();

        if (sesScopedCounter.getCounter() != 2) {
            responseMessage = "Session Scope in onOpen FAILED. " + responseMessage;
        }

        // so final string we will send should be (assuming no one willy nilly has changed the values)
        // :ProducerBean Field Injected:SimpleWebSocketBean Inside Contructor:SimpleWebSocketBean Inside PostContruct:SimpleWebSocketBean Inside getResponse

        try {
            currentSession.getBasicRemote().sendText(responseMessage);
        } catch (IOException e) {
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {}

    @OnError
    public void onError(Session session, Throwable t) {}

}
