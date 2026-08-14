/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.websocket.app;

import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint that uses CDI method parameter injection
 */
@ServerEndpoint("/testWebSocket")
public class TestWebSocketEndpoint {

    private InjectedBean injectedBean;
    private String injectionResult = "NOT_INJECTED";
    private Exception injectionException = null;

    /**
     * CDI method parameter injection - the method writes stack trace to sysout
     */
    @Inject
    public void setInjectedBean(InjectedBean bean) {
        System.out.println("TestWebSocketEndpoint.setInjectedBean() called with bean: " + bean);
        
        // Write stack trace to sysout as required
        Thread.currentThread().dumpStack();
        
        try {
            this.injectedBean = bean;
            if (bean != null) {
                injectionResult = bean.getMessage();
                System.out.println("CDI method parameter injection successful: " + injectionResult);
            } else {
                injectionResult = "INJECTION_FAILED_NULL";
                System.out.println("CDI method parameter injection failed: bean is null");
            }
        } catch (Exception e) {
            injectionException = e;
            injectionResult = "INJECTION_FAILED_EXCEPTION";
            System.out.println("CDI method parameter injection failed with exception: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("TestWebSocketEndpoint.onOpen() called");
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        System.out.println("TestWebSocketEndpoint.onMessage() received: " + message);
        
        if ("getStatus".equals(message)) {
            if (injectionException != null) {
                return "ERROR: " + injectionException.getMessage();
            }
            return injectionResult;
        }
        
        return "Unknown command: " + message;
    }

    public String getInjectionResult() {
        return injectionResult;
    }
}

// Made with Bob
