/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package websocket11.war;

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import io.openliberty.wsoc.common.Utils;

/**
 * This test case is exactly like ProgrammaticEndpoint except this uses websocket 1.1 Whole/Partial message handler APIs
 */
public abstract class AddMessageHandlerEndpoint11 extends Endpoint {

    public static class TextEndpoint extends AddMessageHandlerEndpoint11 {
        private static Session sess = null;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            sess = session;
            //uses websocket 1.1 API
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    try {
                        sess.getBasicRemote().sendText(text);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class ReaderEndpoint extends AddMessageHandlerEndpoint11 {
        private Session sess = null;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            sess = session;
            //uses websocket 1.1 API
            session.addMessageHandler(Reader.class, new MessageHandler.Whole<Reader>() {

                @Override
                public void onMessage(Reader reader) {

                    try {
                        sess.getBasicRemote().sendText(Utils.getReaderText(reader));
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class InputStreamEndpoint extends AddMessageHandlerEndpoint11 {
        private Session sess = null;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            sess = session;
            //uses websocket 1.1 API
            session.addMessageHandler(InputStream.class, new MessageHandler.Whole<InputStream>() {

                @Override
                public void onMessage(InputStream stream) {

                    try {
                        sess.getBasicRemote().sendBinary(ByteBuffer.wrap(Utils.getInputStreamData(stream)));
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class PartialTextEndpoint extends AddMessageHandlerEndpoint11 {
        private Session sess = null;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            sess = session;
            //uses websocket 1.1
            session.addMessageHandler(String.class, new MessageHandler.Partial<String>() {
                String echoMessage = "";

                @Override
                public void onMessage(String text, boolean isLast) {

                    if (isLast == false) {
                        echoMessage = echoMessage + text;
                    } else {

                        try {
                            echoMessage = echoMessage + text;
                            sess.getBasicRemote().sendText(echoMessage, true);
                            echoMessage = "";
                        } catch (Exception ex) {
                            Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        }
    }

    public static class PartialSenderWholeReceiverEndpoint extends AddMessageHandlerEndpoint11 {
        private Session sess = null;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            sess = session;
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                int messageCount = 0;

                @Override
                public void onMessage(String text) {

                    if (messageCount == 0) {
                        // read in first message, and send back response in one whole message
                        try {
                            sess.getBasicRemote().sendText(text, true);
                            messageCount++;

                        } catch (Exception ex) {
                            Logger.getLogger(AddMessageHandlerEndpoint11.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    } else if (messageCount >= 1) {
                        // read in second message, and send back response in one partial message at a time

                        try {
                            String x = null;
                            for (int i = 0; i < text.length(); i++) {
                                x = text.substring(i, i + 1);
                                if (i + 1 < text.length()) {
                                    sess.getBasicRemote().sendText(x, false);
                                } else {
                                    sess.getBasicRemote().sendText(x, true);
                                }
                                Thread.sleep(250);
                            }

                            messageCount++;

                        } catch (Exception ex) {
                            Logger.getLogger(AddMessageHandlerEndpoint11.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onClose(Session session, CloseReason reason) {

    }

    @Override
    public void onError(Session session, Throwable thr) {

    }

}
