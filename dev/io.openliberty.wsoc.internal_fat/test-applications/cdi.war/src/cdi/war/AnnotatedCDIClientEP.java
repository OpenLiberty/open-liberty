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
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 *
 */
@ClientEndpoint
public class AnnotatedCDIClientEP {

    Session session = null;

    public @Inject
    @Named("Producer1")
    StringBuffer N1;

    @OnOpen
    public void onOpen(Session sess) {
        session = sess;
        try {
            session.getBasicRemote().sendText("from clientEPAnnotated");
        } catch (IOException e) {
        }
    }

    @OnMessage
    public void message(String input) {

        if (input.compareTo(SimpleServerEP.sseRet) == 0) {
            String s = null;
            if (N1 == null) {
                s = "FailedTest: injection failed.";
            } else {
                s = N1.toString();
                if (s.indexOf(":ProducerBean Field Injected") == 0) {
                    s = "SuccessfulTest " + s;
                } else {
                    s = "FailedTest" + s;
                }
            }
            ClientTestServletCDI.clientEPTestMessage = s + " input from serverEP: " + input;
        } else {
            ClientTestServletCDI.clientEPTestMessage = "FailedTest input from serverEP: " + input;
        }
        ClientTestServletCDI.waitForMessage.incrementAndGet();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {}

    @OnError
    public void onError(Session session, Throwable t) {}

}
