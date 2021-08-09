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
package context.war;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.Constants;

/**
 *
 */
@ServerEndpoint(value = "/sessionEndpoint")
public class SessionServerEP {

    Session onOpenSession = null;
    String onOpenId = "nothing yet";

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        onOpenSession = session;

        if (session != null) {
            if (session.getId() != null) {
                onOpenId = session.getId();
            } else {
                onOpenId = "null returned by session.getId()";
            }
            Map<String, Object> userMap = onOpenSession.getUserProperties();
            if (userMap != null) {
                userMap.put(Constants.ON_OPEN_ID, onOpenId);
            }
        }
    }

    @OnMessage
    public void receiveMessage(String message, Session messageSession) {

        String status = Constants.SUCCESS;
        String messageToSend = new String("Message Start. ");
        String messageId;

        messageId = messageSession.getId();

        // check that the passed in session id, is the same as the session id in the onOpen session
        if (messageId.compareTo(onOpenId) == 0) {
            messageToSend = messageToSend + "OnOpen and OnMessage Map ID Equal. ";
        } else {
            messageToSend = messageToSend + "OnOpen and OnMessage Not Equal. OnOpen: " + onOpenId + " messageID: " + messageId;
            status = Constants.FAILED;
        }

        // test user properties by seeing if the id matches the one in the user properties set on OnOpen
        Map<String, Object> userMap = messageSession.getUserProperties();
        String mapId = (String) userMap.get(Constants.ON_OPEN_ID);
        if (messageId.compareTo(mapId) == 0) {
            messageToSend = messageToSend + "Map ID Equal. ";
        } else {
            messageToSend = messageToSend + "Map ID Not Equal. mapID: " + mapId + " messageID: " + messageId;
            status = Constants.FAILED;
        }

        // go through the open sessions, looking for the id in one and only one entry
        boolean found = false;
        Set<Session> sessions = messageSession.getOpenSessions();
        for (Session s : sessions) {
            String getId = s.getId();
            String propMapId = (String) (s.getUserProperties().get(Constants.ON_OPEN_ID));

            if ((messageId.compareTo(getId) == 0) && (messageId.compareTo(propMapId) == 0)) {
                if (found == false) {
                    found = true;
                } else {
                    messageToSend = messageToSend + "ID was found more the once in session map";
                    status = Constants.FAILED;
                }
            }
        }

        if (found == false) {
            messageToSend = messageToSend + "ID was not found in session map";
            status = Constants.FAILED;
        }

        messageToSend = messageToSend + "..." + status + "...";

        if (status.compareTo(Constants.SUCCESS) == 0) {
            messageToSend = messageToSend + "with id of: " + messageId;
        }

        try {
            messageSession.getBasicRemote().sendText(messageToSend);
        } catch (IOException e) {
        }

    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

    }

    @OnError
    public void onError(Session session, Throwable t) {

    }

}
