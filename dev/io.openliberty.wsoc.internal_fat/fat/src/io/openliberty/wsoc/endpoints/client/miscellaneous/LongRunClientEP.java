/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.miscellaneous;

import java.io.IOException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.endpoints.client.basic.MultiClientEP;

/**
 *
 */
public class LongRunClientEP implements TestHelper {

    public WsocTestContext _wtr = null;

    @ClientEndpoint
    public static class SimpleReceiverTest extends LongRunClientEP {

        @OnMessage
        public void echoText(String data) {

            _wtr.addMessage(data);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        @OnOpen
        public void onOpen(Session sess) {
            _wtr.connected();
        }
    }

    @ClientEndpoint
    public static class NoPublishNoReceiveTest extends MultiClientEP {

        @OnOpen
        public void onOpen(Session sess) {
            _wtr.connected();

        }

    }

    @ClientEndpoint
    public static class SimplePublisherTest extends LongRunClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public SimplePublisherTest(String[] data) {
            _data = data;
        }

        @OnOpen
        public void onOpen(Session sess) {
            _wtr.connected();
            // try {
            // sess.getBasicRemote().sendText("FIRST MSG");
            // } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            //     e.printStackTrace();
            // }
//            try {
//                for (String sendText : _data) {
//                    System.out.println("PUBLISHING MSG");
//                    sess.getBasicRemote().sendText(sendText);
//                }
//                //String s = _data[_counter++];              
//                //sess.getBasicRemote().sendText(s);
//            } catch (Exception e) {
//                _wtr.addExceptionAndTerminate("Error publishing initial message", e);
//
//            }
        }

    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

        try {
            session.close();
        } catch (IOException e) {
            _wtr.addExceptionAndTerminate("Error closing session", e);
        }

    }

    @OnError
    public void onError(Session session, java.lang.Throwable throwable) {

        _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
    }

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

}
