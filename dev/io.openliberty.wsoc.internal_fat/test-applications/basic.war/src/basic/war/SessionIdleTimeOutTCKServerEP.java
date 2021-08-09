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
package basic.war;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
@ServerEndpoint(value = "/annotatedIdleTimeoutTCK")
public class SessionIdleTimeOutTCKServerEP {

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        if (session != null && ec != null) {
            //set idle timeout as 15 seconds
            session.setMaxIdleTimeout(15000);
        }
    }

    @OnMessage
    public void echoText(String msg) {
        // TCK just wants to sit on the thread until the timeout fires
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 19000) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
            }
        }

        return;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {}
}
