/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

// JavaScript to access from a WebSocket capable browser would be:  ws://<Host Name>:<port>/<Context-Root>/EPFieldInjectionCDI12

@ServerEndpoint(value = "/EPFieldInjectionCDI12")
public class AnnotatedFieldInjectionServerCDI12EP {
    Session currentSession = null;

    String responseMessage = "no message yet";

    public @Inject
    SimpleWebSocketBean bean;

    public @Inject
    @Named("Producer1")
    StringBuffer N1;

    private int depBeanCounter = 0;
    private int xCounter = 0;

    boolean appScopeMethodInjectionOK = false;

    // SessionScope not supported by WELD/CDI1.2

    // cdi constructor injection
    @Inject
    public AnnotatedFieldInjectionServerCDI12EP(CounterDependentScoped input) {
        if (input != null) {
            xCounter = input.getNext();
        }
    }

    // Websocket spec requires a 0-arg constructor
    public AnnotatedFieldInjectionServerCDI12EP() {

    }

    // Method Injected bean using Dependent Scope
    @Inject
    public void setMethodBeanDep(CounterDependentScoped depBean) {
        depBeanCounter = depBean.getNext();
    }

    // Method Injected bean Application Scope
    @Inject
    public void setMethodBeanApp(CounterApplicationScoped appBean) {
        appScopeMethodInjectionOK = false;
        // just make sure it isn't null, don't inc the counter and mess up other tests
        if ((appBean != null) && (appBean instanceof Counter)) {
            appScopeMethodInjectionOK = true;
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        currentSession = session;

        // we should be able to use injected stuff here
        responseMessage = N1.toString();

    }

    @OnMessage
    public void receiveMessageCDI(String message) {

        String s = null;
        if (depBeanCounter != 1) {
            s = "Error, injected method counter was not 1 it was: " + depBeanCounter;
        } else if (xCounter != 1) {
            s = "Error, consturctor injection counter was not 1 it was: " + xCounter;
        } else if (!appScopeMethodInjectionOK) {
            s = "Error, appScopeMethodInjection failed";
        }

        if (s == null) {
            // and use injected stuff here
            responseMessage = responseMessage + bean.getResponse();
        } else {
            responseMessage = responseMessage + s;
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
