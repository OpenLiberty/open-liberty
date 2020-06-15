/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013, 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import basic.war.ProgrammaticServerEP.PingPongEndpoint;

//TODO - move this class into AnnotatedEndpoint once we get onMessage session argument passed in.
@ServerEndpoint(value = "/annotatedPong/{boolean-var}")
public class PongServerEP extends AnnotatedServerEP {

    private Session _curSess = null;

    @OnMessage
    public void pongMessage(Session session, PongMessage msg, @PathParam("boolean-var") boolean booleanVar) { //session, msg and @PathParam canb be any index

        try {
            ByteBuffer buf = msg.getApplicationData();
            byte[] data = new byte[buf.limit()];
            buf.get(data, 0, buf.limit());
            if (session != null && booleanVar) { //session should not be null && test is passing true for booleanVar in the URI
                _curSess.getBasicRemote().sendPong(ByteBuffer.wrap(data));
            } else {
                String result = "FAILED";
                _curSess.getBasicRemote().sendPong(ByteBuffer.wrap(result.getBytes("UTF-16LE")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return;
    }

    @OnMessage
    public void onMessage(byte[] msg) {
        try {

            // as per a TCK test, make sure invalid Pings are not sent
            String bad = null;
            // first send a ping, async (which should attempted sync by the api) using a message that is too big, and verify that a IAE exception thrown
            String s126 = "1........10........20........30........40........50........60........70........80........90........100.......110.......120...6";
            ByteBuffer b126 = ByteBuffer.wrap(s126.getBytes());

            boolean okSoFar = false;
            try {
                _curSess.getAsyncRemote().sendPing(b126);
                bad = "darn, API didn't throw a too ping IAE exception";
            } catch (IllegalArgumentException e) {
                // we should get here
                okSoFar = true;
            } catch (Throwable t) {
                Logger.getLogger(PingPongEndpoint.class.getName()).log(Level.SEVERE, null, t);
                bad = "caught wrong exception on too big of ping";
            }

            if (!okSoFar) {
                _curSess.getBasicRemote().sendPong(ByteBuffer.wrap(bad.getBytes()));
            } else {
                // !! don't send ping, because the jetty client has a bug, and it will call the Pong message handler.
                _curSess.getBasicRemote().sendPong(ByteBuffer.wrap(msg));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //this is also a positive test which shows all 3 message types, ping, binary and text
    @OnMessage
    public String onMessageText(String msg) {
        try {
            return msg;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {

        _curSess = session;
    }

}