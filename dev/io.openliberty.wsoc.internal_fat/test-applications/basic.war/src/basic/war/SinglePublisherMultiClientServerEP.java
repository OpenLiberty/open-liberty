/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

//TODO - move this class into AnnotatedEndpoint once we get onMessage session argument passed in.
@ServerEndpoint(value = "/singlePubMultiReceive")
public class SinglePublisherMultiClientServerEP extends AnnotatedServerEP {

    private Session _curSess = null;
    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

    @OnMessage
    public void echoText(String val) {

        synchronized (clients) {
            // Iterate over the connected sessions
            // and broadcast the received message
            for (Session client : clients) {
                if (!client.equals(_curSess)) {
                    try {
                        client.getBasicRemote().sendText(val);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        clients.remove(session);
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {

        // last onOpen call is assumed from the "publish" connection (previous calls are assumed from the "receiver" connections)
        _curSess = session;
        // pass in "session" here, not _curSess to avoid race conditions
        clients.add(session);
    }
}