/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/{String-var}")
public class URIOverlappingServerEP {
    Session currentSession = null;

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        currentSession = session;
    }

    @OnMessage
    public String receiveMessage(String message) {
        return message;
    }

}