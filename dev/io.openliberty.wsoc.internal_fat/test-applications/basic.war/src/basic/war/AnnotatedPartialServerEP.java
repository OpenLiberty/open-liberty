/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
public class AnnotatedPartialServerEP {

    @ServerEndpoint(value = "/annotatedPartialByteBuffer")
    public static class AnnotatedPartialByteBufferTest extends AnnotatedPartialServerEP {

        int messageCount = 0;
        ByteBuffer echoMessage = ByteBuffer.allocate(1024);

        @OnMessage
        public void echoText(ByteBuffer inBuf, boolean isLast, Session session) {
            if (messageCount == 0) {
                // read in first message, and send back response in one whole message
                // put/add input buffer to buffer to send back
                echoMessage.put(inBuf);
                if (isLast == true) {
                    try {
                        // flip buffer so we can now send it
                        echoMessage.flip();
                        session.getBasicRemote().sendBinary(echoMessage, true);
                        echoMessage = ByteBuffer.allocate(1024);
                        messageCount++;
                    } catch (Exception ex) {
                        Logger.getLogger(AnnotatedPartialByteBufferTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else if (messageCount == 1) {
                // read in second message, and send back response in one partial message at a time
                echoMessage.put(inBuf);
                if (isLast == true) {
                    try {
                        byte[] ba = new byte[1];
                        ByteBuffer x = null;
                        echoMessage.flip();
                        int length = echoMessage.limit() - echoMessage.position();
                        for (int i = 0; i < length; i++) {
                            ba[0] = echoMessage.get();
                            x = ByteBuffer.wrap(ba);
                            if (i + 1 < length) {
                                session.getBasicRemote().sendBinary(x, false);
                            } else {
                                session.getBasicRemote().sendBinary(x, true);
                            }
                            Thread.sleep(250);
                        }

                        echoMessage = ByteBuffer.allocate(1024);
                        messageCount++;

                    } catch (Exception ex) {
                        Logger.getLogger(AnnotatedPartialByteBufferTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                // read in next message and send back as received with respect to partial
                try {
                    session.getBasicRemote().sendBinary(inBuf, isLast);
                } catch (Exception ex) {
                    Logger.getLogger(AnnotatedPartialByteBufferTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    @ServerEndpoint(value = "/annotatedPartialByteArray")
    public static class AnnotatedPartialByteArrayTest extends AnnotatedPartialServerEP {

        int MAX_MESSAGE_SIZE = 1024;
        int messageCount = 0;
        byte[] echoMessage = new byte[MAX_MESSAGE_SIZE];
        int currentIndex = 0;

        @OnMessage
        public void echoText(byte[] inBuf, boolean isLast, Session session) {

            // copy message into a byte array for current or later use
            for (int i = 0; i < inBuf.length; i++) {
                echoMessage[currentIndex] = inBuf[i];
                currentIndex++;
            }

            if (messageCount == 0) {
                // read in first message, and send back response in one whole message
                // put/add input buffer to buffer to send back
                if (isLast == true) {
                    try {
                        ByteBuffer sendBuffer = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
                        sendBuffer.put(echoMessage, 0, currentIndex);
                        sendBuffer.position(0);
                        sendBuffer.limit(currentIndex);

                        session.getBasicRemote().sendBinary(sendBuffer, true);

                        echoMessage = new byte[MAX_MESSAGE_SIZE];
                        messageCount++;
                        currentIndex = 0;
                    } catch (Exception ex) {
                        Logger.getLogger(AnnotatedPartialByteArrayTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else if (messageCount == 1) {
                // read in second message, and send back response in one partial message at a time
                if (isLast == true) {
                    try {
                        byte[] ba = new byte[1];
                        ByteBuffer x = null;
                        int length = currentIndex;
                        for (int i = 0; i < length; i++) {
                            ba[0] = echoMessage[i];
                            x = ByteBuffer.wrap(ba);
                            if (i + 1 < length) {
                                session.getBasicRemote().sendBinary(x, false);
                            } else {
                                session.getBasicRemote().sendBinary(x, true);
                            }
                            Thread.sleep(250);
                        }

                        echoMessage = new byte[MAX_MESSAGE_SIZE];
                        messageCount++;
                        currentIndex = 0;

                    } catch (Exception ex) {
                        Logger.getLogger(AnnotatedPartialByteArrayTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                // read in next message and send back as received with respect to partial
                try {
                    ByteBuffer sendBuffer = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

                    sendBuffer.put(echoMessage, 0, currentIndex);
                    sendBuffer.position(0);
                    sendBuffer.limit(currentIndex);
                    session.getBasicRemote().sendBinary(sendBuffer, isLast);

                    echoMessage = new byte[MAX_MESSAGE_SIZE];
                    messageCount++;
                    currentIndex = 0;

                } catch (Exception ex) {
                    Logger.getLogger(AnnotatedPartialByteArrayTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @ServerEndpoint(value = "/annotatedPartialText")
    public static class AnnotatedPartialTextTest extends AnnotatedPartialServerEP {

        int messageCount = 0;
        String echoMessage = "";

        @OnMessage
        public void echoText(String text, boolean isLast, Session session) {

            if (messageCount == 0) {
                // read in first message, and send back response in one whole message
                if (isLast == false) {
                    echoMessage = echoMessage + text;
                } else {
                    try {
                        echoMessage = echoMessage + text;
                        session.getBasicRemote().sendText(echoMessage, true);
                        echoMessage = "";
                        messageCount++;
                    } catch (Exception ex) {
                        Logger.getLogger(AnnotatedPartialTextTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else if (messageCount == 1) {
                // read in second message, and send back response in one partial message at a time
                if (isLast == false) {
                    echoMessage = echoMessage + text;
                } else {
                    try {
                        echoMessage = echoMessage + text;
                        String x = null;
                        for (int i = 0; i < echoMessage.length(); i++) {
                            x = echoMessage.substring(i, i + 1);
                            if (i + 1 < echoMessage.length()) {
                                session.getBasicRemote().sendText(x, false);
                            } else {
                                session.getBasicRemote().sendText(x, true);
                            }
                            Thread.sleep(250);
                        }

                        echoMessage = "";
                        messageCount++;

                    } catch (Exception ex) {
                        Logger.getLogger(AnnotatedPartialTextTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                // read in next message, and send back as we read it in.
                try {
                    session.getBasicRemote().sendText(text, isLast);
                } catch (Exception ex) {
                    Logger.getLogger(AnnotatedPartialTextTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) {
            try {
                java.lang.Thread.sleep(250);
            } catch (Exception e) {
            }
        }
    }

    @ServerEndpoint(value = "/annotatedPartialSenderText")
    public static class AnnotatedPartialSenderWholeReceiverTest extends AnnotatedPartialServerEP {

        int messageCount = 0;

        @OnMessage
        public void echoText(String text, Session session) {

            if (messageCount == 0) {
                // read in first message, and send back response in one whole message
                try {
                    session.getBasicRemote().sendText(text, true);
                    messageCount++;

                } catch (Exception ex) {
                    Logger.getLogger(AnnotatedPartialTextTest.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else if (messageCount >= 1) {
                // read in second message, and send back response in one partial message at a time

                try {
                    String x = null;
                    for (int i = 0; i < text.length(); i++) {
                        x = text.substring(i, i + 1);
                        if (i + 1 < text.length()) {
                            session.getBasicRemote().sendText(x, false);
                        } else {
                            session.getBasicRemote().sendText(x, true);
                        }
                        Thread.sleep(250);
                    }

                    messageCount++;

                } catch (Exception ex) {
                    Logger.getLogger(AnnotatedPartialTextTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) {
            try {
                java.lang.Thread.sleep(250);
            } catch (Exception e) {
            }
        }
    }

}
