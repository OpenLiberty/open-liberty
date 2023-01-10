/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package cditx.war;

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

// JavaScript to access from a WebSocket capable browser would be:  ws://<Host Name>:<port>/<Context-Root>/EPTxMandatoryFieldInjectionCDI12

@ServerEndpoint(value = "/EPTxMandatoryFieldInjectionCDI12")
public class AnnotatedTxMandatoryFieldInjectionServerCDI12EP {
    Session currentSession = null;

    String responseMessage = "no message yet";

    public @Inject SimpleWebSocketBean bean;

    public @Inject @Named("Producer1") StringBuffer N1;

    public @Inject CounterTxMandatoryApplicationScoped txAppScopedCounter;
    private int depBeanCounter = 0;
    private int xCounter = 0;

    boolean appScopeMethodInjectionOK = false;

    // SessionScope not supported by WELD/CDI1.2

    // cdi constructor injection
    @Inject
    public AnnotatedTxMandatoryFieldInjectionServerCDI12EP(CounterDependentScoped input) {
        if (input != null) {
            xCounter = input.getNext();
        }
    }

    // Websocket spec requires a 0-arg constructor
    public AnnotatedTxMandatoryFieldInjectionServerCDI12EP() {

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
        System.out.println("TxMandatory: into onOpen >");
        currentSession = session;
        try {
            txAppScopedCounter.getNext();
        } catch (javax.transaction.TransactionalException ex) {
            System.out.println("TxMandatory: onOpen caught exc: " + ex + "as expected");
        }
        // we should be able to use injected stuff here
        responseMessage = N1.toString();
        System.out.println("TxMandatory: exit onOpen < " + responseMessage);
    }

    @OnMessage
    public void receiveMessageCDI(String message) {
        System.out.println("TxMandatory: into receiveMessageCDI >");

        try {
            txAppScopedCounter.getNext();
        } catch (javax.transaction.TransactionalException ex) {
            System.out.println("TxMandatory: receiveMessageCDI caught exc: " + ex + "as expected");
        }

        // and use injected stuff here
        responseMessage = responseMessage + bean.getResponse();

        // so final string we will send should be (assuming no one willy nilly has changed the values)
        // :ProducerBean Field Injected:SimpleWebSocketBean Inside Contructor:SimpleWebSocketBean Inside PostContruct:SimpleWebSocketBean Inside getResponse

        try {
            currentSession.getBasicRemote().sendText(responseMessage);
        } catch (IOException e) {
        }
        System.out.println("TxMandatory: exit receiveMessage < " + responseMessage);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {}

    @OnError
    public void onError(Session session, Throwable t) {}

}
