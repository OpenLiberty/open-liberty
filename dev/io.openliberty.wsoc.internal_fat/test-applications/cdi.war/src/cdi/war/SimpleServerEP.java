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
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
@ServerEndpoint(value = "/simpleServerEndpoint")
public class SimpleServerEP {

    Session session = null;

    static public String sseRet = "Hi from simpleServerEndpoint";

    @OnOpen
    public void onOpen(Session sess) {
        session = sess;
    }

    @OnMessage
    public void message(String input) {
        try {
            session.getBasicRemote().sendText(sseRet);
        } catch (IOException e) {
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {}

    @OnError
    public void onError(Session session, Throwable t) {}

}
