/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
