/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
@ServerEndpoint(value = "/multiText")
public class MultiServerEP {

    private Session _curSess = null;

    @OnMessage
    public String echoText(String text) {

        return text;

        /*
         * getOpenSessions currently not implemented, try again later.
         * Set<Session> sessions = _curSess.getOpenSessions();
         * for (Session sess : sessions) {
         * try {
         * System.out.println("PRINTING IT");
         * sess.getBasicRemote().sendText(text);
         * } catch (IOException e) {
         * e.printStackTrace();
         * }
         * }
         * return text;
         */
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {

        _curSess = session;
    }

}
