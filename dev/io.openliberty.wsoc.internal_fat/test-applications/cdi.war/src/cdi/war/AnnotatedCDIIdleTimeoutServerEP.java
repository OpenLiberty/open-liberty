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
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/annotatedIdleTimeoutCDI")
public class AnnotatedCDIIdleTimeoutServerEP {
    static int count = 0;

    static String responseMessage = "nothing";

    public @Inject
    @Named("Producer1")
    StringBuffer N1;

    public @Inject
    @Named("Producer1")
    StringBuffer N2;

    public @Inject
    CounterApplicationScoped2 appScopedCounter2;

    public AnnotatedCDIIdleTimeoutServerEP() {

    }

    @OnOpen
    public void onOpen(final Session session) {

        try {
            // we should be able to use injected stuff here
            if (count == 0) {
                //set idle timeout as 15 seconds
                session.setMaxIdleTimeout(15000);
                responseMessage = N1.toString();
                count++;
                appScopedCounter2.getNext();
                responseMessage += appScopedCounter2.getCounter();

            } else {
                //set idle timeout as 15 seconds
                session.setMaxIdleTimeout(3000);
            }

            session.getBasicRemote().sendText(String.valueOf(responseMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        // test that the idle timeout thread that calls onClose has the CDI contexts available.

        // we should be able to use injected stuff here
        String responseMessage2 = N2.toString();
        appScopedCounter2.getNext();

        responseMessage += ":OnClose Called" + responseMessage2;
        responseMessage += appScopedCounter2.getCounter();
    }

    @OnError
    public void onError(Session session, Throwable t) {

    }

}
