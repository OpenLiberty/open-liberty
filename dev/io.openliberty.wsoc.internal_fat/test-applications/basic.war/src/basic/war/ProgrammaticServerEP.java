/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnError;
import javax.websocket.PongMessage;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;

import basic.war.AnnotatedPartialServerEP.AnnotatedPartialTextTest;
import basic.war.coding.FormatOne;

/**
 *
 */
public abstract class ProgrammaticServerEP extends Endpoint {

    private static final Logger LOG = Logger.getLogger(ProgrammaticServerEP.class.getName());

    public static class TextEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    try {
                        session.getBasicRemote().sendText(text);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class AsyncTextEndpoint extends ProgrammaticServerEP {
        volatile int echoHitCount = 0;
        volatile int resultHitCount = 0;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            echoHitCount = 0;
            resultHitCount = 0;

            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    echoHitCount++;
                    int iterationCounter = 0;
                    int iterationCountLimit = 20; // five seconds

                    // if we enter here before we see the result for the last entry, we need to wait.  Can't do two async writes in a row.
                    // wait for 5 seconds for condition to be met, then just go on anyways.
                    while (echoHitCount - resultHitCount > 1) {
                        try {
                            iterationCounter++;
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                        }

                        if (iterationCounter == iterationCountLimit) {
                            return; // test will fail since we are not sending back data
                        }
                    }

                    session.getAsyncRemote().sendText(text, new SendHandler() {
                        @Override
                        public void onResult(SendResult result) {
                            resultHitCount++;
                            boolean ok = result.isOK();
                            if (!ok) {
                                // TODO - fail the test somehow?
                            }

                        }

                    });
                }
            });
        }
    }

    public static class ReaderEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<Reader>() {

                @Override
                public void onMessage(Reader reader) {

                    try {
                        session.getBasicRemote().sendText(Utils.getReaderText(reader));
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class InputStreamEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<InputStream>() {

                @Override
                public void onMessage(InputStream stream) {

                    try {
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(Utils.getInputStreamData(stream)));
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class ByteBufferEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.setMaxBinaryMessageBufferSize(70000);
            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                @Override
                public void onMessage(ByteBuffer buffer) {

                    try {
                        session.getBasicRemote().sendBinary(buffer);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class ByteArrayEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<byte[]>() {

                @Override
                public void onMessage(byte[] data) {

                    try {

                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class CodingEndpoint extends ProgrammaticServerEP {

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<FormatOne>() {

                @Override
                public void onMessage(FormatOne formatOne) {

                    try {

                        session.getBasicRemote().sendObject(formatOne);
                    } catch (Exception ex) {
                        Logger.getLogger(CodingEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class PingPongEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {

                @Override
                public void onMessage(PongMessage msg) {

                    try {

                        ByteBuffer retBuf = msg.getApplicationData();
                        retBuf.rewind();
                        session.getBasicRemote().sendPong(retBuf);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                @Override
                public void onMessage(ByteBuffer buffer) {

                    try {
                        buffer.rewind();
                        session.getBasicRemote().sendPing(buffer);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }

    }

    public static class PartialTextEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Partial<String>() {
                String echoMessage = "";

                @Override
                public void onMessage(String text, boolean isLast) {

                    if (isLast == false) {
                        echoMessage = echoMessage + text;
                    } else {

                        try {
                            echoMessage = echoMessage + text;
                            session.getBasicRemote().sendText(echoMessage, true);
                            echoMessage = "";
                        } catch (Exception ex) {
                            Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        }
    }

    public static class PartialSenderWholeReceiverEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                int messageCount = 0;

                @Override
                public void onMessage(String text) {

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
            });
        }
    }

    public static class PartialTextEndpoint2 extends ProgrammaticServerEP {

        int messageCount = 0;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Partial<String>() {
                String echoMessage = "";

                @Override
                public void onMessage(String text, boolean isLast) {

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
                                Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
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
                                // session.getBasicRemote().sendText(echoMessage, true);
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
                                Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } else {
                        // read in next message, and send back as we read it in.
                        try {
                            session.getBasicRemote().sendText(text, isLast);
                        } catch (Exception ex) {
                            Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            });
        }
    }

    public static class PartialTextWithSendingEmbeddedPingEndpoint extends ProgrammaticServerEP {

        int messageCount = 0;
        boolean pingSent = false;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
                @Override
                public void onMessage(PongMessage msg) {
                    try {
                        ByteBuffer buf = msg.getApplicationData();
                        int length = buf.limit() - buf.position();
                        if (length > 0) {
                            byte[] ba = new byte[length];
                            buf.get(ba, 0, length);
                            String s = new String(ba);

                        }

                        session.getBasicRemote().sendText(Constants.PING_PONG_FROM_SERVER_MSG);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            session.addMessageHandler(new MessageHandler.Partial<String>() {
                String echoMessage = "";

                @Override
                public void onMessage(String text, boolean isLast) {

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
                                Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
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
                                // session.getBasicRemote().sendText(echoMessage, true);
                                for (int i = 0; i < echoMessage.length(); i++) {
                                    x = echoMessage.substring(i, i + 1);

                                    if (i == 1) {
                                        // send a ping in the "middle" of sending this partial message
                                        String s = "Ping from Server Test";
                                        byte[] ba = s.getBytes();
                                        ByteBuffer buf = ByteBuffer.wrap(ba);
                                        session.getBasicRemote().sendPing(buf);
                                    }

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
                                Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } else {
                        // read in next message, and send back as we read it in.
                        try {
                            session.getBasicRemote().sendText(text, isLast);
                        } catch (Exception ex) {
                            Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            });
        }
    }

    public static class CloseEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {
                    String[] vals = text.split(":");

                    try {
                        // CloseCode = CloseCodes.
                        session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(Integer.valueOf(vals[0])), vals[1]));

                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

        }

        @Override
        public void onClose(Session session, CloseReason reason) {

            // Shouldn't usually call session.close in onClose, but there is a loop condition that this might catch.
            try {
                session.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
                e.printStackTrace();
            }

        }
    }

    public static class CloseEndpointOnOpen extends ProgrammaticServerEP {
        Session ses = null;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            ses = session;
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(Integer.valueOf("1001")), "THIS IS A TEST CLOSING STATUS FROM onOPEN"));

            } catch (Exception ex) {
                Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        public void onMessage(String text) {
            String[] vals = text.split(":");

            try {
                ses.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(Integer.valueOf("9999")), "SHOULD NOT GET HERE"));

            } catch (Exception ex) {
                Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
            }

            return;
        }

        @Override
        public void onClose(Session session, CloseReason reason) {

            // Shouldn't usually call session.close in onClose, but there is a loop condition that this might catch.
            try {
                session.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
                e.printStackTrace();
            }

        }
    }

    public static class OnErrorEndpoint extends ProgrammaticServerEP {
        public static String onErrorParamValue = null;

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {
                    if (text.equals("FirstMessage"))
                        throw new NullPointerException("First Message Error");
                    try {
                        if (text.equals("SecondMessage")) {
                            sess.getBasicRemote().sendText(onErrorParamValue);
                        } else
                            sess.getBasicRemote().sendText("");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        @OnError
        public void onError(final Session session, Throwable error) {
            try {
                if (session != null && error != null) { //if the session is declared, runtime should be passing them in. Throwable is a mandatory parameter
                    if (error.getMessage().equals("First Message Error")) {
                        onErrorParamValue = "Test success";
                        session.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class SessionPathParamEndpoint extends ProgrammaticServerEP {
        private static String pathparamResult;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            String keys = Arrays.toString(session.getPathParameters().keySet().toArray());
            String values = Arrays.toString(session.getPathParameters().values().toArray());
            pathparamResult = keys + values;
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    try {
                        session.getBasicRemote().sendText(pathparamResult);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class MaxMessageSizeInSession extends ProgrammaticServerEP {
        Session ses = null;
        int messageNumber = 0;
        String ret = new String("Message Start: ");

        // this needs to match what we define it to be in our production implementation of the message size

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            ses = session;

            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                @Override
                public void onMessage(ByteBuffer buffer) {
                    messageNumber++;

                    LOG.info("MaxMessageSizeInSession: onMessage entered: " + messageNumber);

                    if (messageNumber == 1) {
                        int maxTextSize = ses.getMaxTextMessageBufferSize();
                        int maxBinarySize = ses.getMaxBinaryMessageBufferSize();

                        try {
                            ret = ret + "Default, sesion.Text, session.Binary sizes: " + Constants.UNLIMITED_MAX_MSG_SIZE + " , " + maxTextSize + " , " + maxBinarySize;
                            if (maxTextSize != Constants.UNLIMITED_MAX_MSG_SIZE || maxBinarySize != Constants.UNLIMITED_MAX_MSG_SIZE) {
                                ret = ret + " " + Constants.FAILED;
                                ses.getBasicRemote().sendText(ret);
                            } else {
                                ret = Constants.SUCCESS;
                                ses.getBasicRemote().sendText(ret);
                            }
                        } catch (IOException e) {
                        }
                    }

                    if (messageNumber == 2) {
                        // this should be of  Constants.TEST_MAX_MSG_SIZE
                        int messageSize = buffer.limit() - buffer.position();

                        // set the next max message size to new size
                        ses.setMaxBinaryMessageBufferSize((int) Constants.TEST_MAX_MSG_SIZE / 2);

                        try {
                            if (messageSize != Constants.TEST_MAX_MSG_SIZE) {
                                ret = ret + "messageSize was not: " + Constants.TEST_MAX_MSG_SIZE + " but instead was: " + messageSize + " " + Constants.FAILED;
                                ret = ret + " " + Constants.FAILED;
                                ses.getBasicRemote().sendText(ret);
                            } else {
                                ret = Constants.SUCCESS;
                                ses.getBasicRemote().sendText(ret);
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

        }

    }

    public static class TextEndpointQueryParms extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            LOG.info("TextEndpointQueryParms: OnOpen entered");
            Exception eFailed = null;

            URI uri = session.getRequestURI();
            if (uri != null) {
                Map<String, List<String>> params = session.getRequestParameterMap();
                if (params != null) {
                    List<String> str = params.get("testMe");
                    if (str != null) {
                        if (str.indexOf("HI") != 0) {
                            LOG.info("Error: query string parms: " + str);
                            eFailed = new Exception("parms incorrect in parameter map");
                        } else {
                            LOG.info("server side found correct parameter value: " + str);
                        }
                    } else {
                        LOG.info("Error: null parms in parameter map");
                        eFailed = new Exception("missing parms in parameter map");
                    }
                } else {
                    LOG.info("Error: getRequestParameterMap is null");
                    eFailed = new Exception("parameter map is null");
                }

                String query = session.getQueryString();
                if (query != null) {
                    if (query.indexOf("testMe=HI") == -1) {
                        LOG.info("Error: getQueryString is wrong: " + query);
                        eFailed = new Exception("bad query string");
                    } else {
                        LOG.info("server side found correct QueryString: " + query);
                    }
                } else {
                    LOG.info("Error: query string is null");
                    eFailed = new Exception("null query string");
                }
            } else {
                LOG.info("Error: uri is null");
                eFailed = new Exception("uri is null");
            }

            try {
                if (eFailed != null) {
                    session.getBasicRemote().sendText(Constants.FAILED + " server side failed with exception: " + eFailed.getMessage());
                } else {
                    session.getBasicRemote().sendText(Constants.SUCCESS);
                }
            } catch (IOException e) {
            }

            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    try {
                        session.getBasicRemote().sendText(text);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpointQueryParms.class.getName()).log(Level.SEVERE, null, ex);
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
