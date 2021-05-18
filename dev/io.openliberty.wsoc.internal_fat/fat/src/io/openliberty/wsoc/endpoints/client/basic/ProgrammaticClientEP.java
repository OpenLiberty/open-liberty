/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;

/**
 *
 */
public abstract class ProgrammaticClientEP extends Endpoint implements TestHelper {

    private static final Logger LOG = Logger.getLogger(ProgrammaticClientEP.class.getName());

    /**
     * 
     */
    public ProgrammaticClientEP() {
        // TODO Auto-generated constructor stub
    }

    public WsocTestContext _wtr = null;

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

    public static class TextTest extends ProgrammaticClientEP {
        private String[] _data = {};
        private int _counter = 1;

        public TextTest(String[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    _wtr.addMessage(text);
                    if (_wtr.limitReached()) {
                        _wtr.terminateClient();
                    }
                    else {
                        try {
                            String s = _data[_counter++];
                            LOG.info("Sending message: " + s);
                            sess.getBasicRemote().sendText(s);
                        } catch (Exception e) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", e);
                        }
                    }
                }
            });
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    public static class PartialTextTest extends ProgrammaticClientEP {
        private String[] _data = {};
        private int _counter = 1;

        public PartialTextTest(String[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    if (text.compareTo(Constants.PING_PONG_FROM_SERVER_MSG) == 0) {
                        // strings match, don't count or send this extra message
                        // add it at the end, since is can come at anytime during the sequence of the other messages.
                        _wtr.addAsLastMessage(text);
                        return;
                    }

                    _wtr.addMessage(text);

                    if (_wtr.limitReached()) {
                        _wtr.terminateClient();
                    }
                    else {
                        try {
                            String toSend = _data[_counter++];
                            String x = null;
                            // send the string 1 char at a time
                            for (int i = 0; i < toSend.length(); i++) {
                                x = toSend.substring(i, i + 1);
                                if (i + 1 < toSend.length()) {
                                    sess.getBasicRemote().sendText(x, false);
                                } else {
                                    sess.getBasicRemote().sendText(x, true);
                                }
                                Thread.sleep(250);
                            }
                        } catch (Exception e) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", e);
                        }
                    }
                }
            });
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    public static class ProgrammaticPartialTextTest extends ProgrammaticClientEP {
        private String[] _data = {};
        private int _counter = 1;

        public ProgrammaticPartialTextTest(String[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.addMessageHandler(new MessageHandler.Partial<String>() {
                String echoMessage = "";

                @Override
                public void onMessage(String text, boolean isLast) {

                    echoMessage = echoMessage + text;
                    if (isLast) {
                        _wtr.addMessage(echoMessage);
                        echoMessage = "";

                        if (_wtr.limitReached()) {
                            _wtr.terminateClient();
                        }
                        else {
                            try {

                                String toSend = _data[_counter++];
                                String x = null;
                                // send the string 1 char at a time
                                for (int i = 0; i < toSend.length(); i++) {
                                    x = toSend.substring(i, i + 1);
                                    if (i + 1 < toSend.length()) {
                                        sess.getBasicRemote().sendText(x, false);
                                    } else {
                                        sess.getBasicRemote().sendText(x, true);
                                    }
                                    Thread.sleep(250);
                                }
                            } catch (Exception e) {
                                _wtr.addExceptionAndTerminate("Error publishing msg", e);
                            }
                        }
                    }
                }

            });
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    public static class PartialBinaryTest extends ProgrammaticClientEP {
        private ByteBuffer[] _data = {};
        private int _counter = 1;

        public PartialBinaryTest(ByteBuffer[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                @Override
                public void onMessage(ByteBuffer text) {

                    String s = "buffer implmentation does not support arrays";
                    if (text.hasArray()) {
                        byte[] t = text.array();
                        s = new String(t);
                    }

                    _wtr.addMessage(s);
                    if (_wtr.limitReached()) {
                        _wtr.terminateClient();
                    }
                    else {
                        try {
                            ByteBuffer toSend = _data[_counter++];
                            byte[] ba = new byte[1];
                            ByteBuffer x = null;
                            int length = toSend.limit() - toSend.position();
                            // send the byte buffer 1 byte at a time
                            for (int i = 0; i < length; i++) {
                                ba[0] = toSend.get();
                                x = ByteBuffer.wrap(ba);
                                if (i + 1 < length) {
                                    sess.getBasicRemote().sendBinary(x, false);
                                } else {
                                    sess.getBasicRemote().sendBinary(x, true);
                                }
                                Thread.sleep(250);
                            }
                        } catch (Exception e) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", e);
                        }
                    }
                }
            });
            try {
                sess.getBasicRemote().sendBinary(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    public static class CloseTest extends ProgrammaticClientEP {
        private String[] _data = {};

        public CloseTest(String[] data) {
            _data = data;
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            _wtr.addMessage(closeReason.getCloseCode().getCode() + ":" + closeReason.getReasonPhrase());
            _wtr.setClosedAlready(true);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
         */
        @Override
        public void onOpen(Session sess, EndpointConfig arg1) {
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }

        }

    }

    public static class ReaderTest extends ProgrammaticClientEP {
        private String[] _data = {};
        private int _counter = 1;

        public ReaderTest(String[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;

            session.addMessageHandler(new MessageHandler.Whole<Reader>() {

                @Override
                public void onMessage(Reader msg) {

                    try {
                        _wtr.addMessage(Utils.getReaderText(msg));
                        if (_wtr.limitReached()) {
                            _wtr.terminateClient();
                        }
                        else {
                            String s = _data[_counter++];
                            sess.getBasicRemote().sendText(s);
                        }
                    } catch (Exception e) {
                        _wtr.addExceptionAndTerminate("Error publishing msg", e);
                    }
                }
            });
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                e.printStackTrace();
                //   _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    public static class InputStreamTest extends ProgrammaticClientEP {
        public byte[][] _data = {};
        public int _counter = 1;

        public InputStreamTest(byte[][] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.addMessageHandler(new MessageHandler.Whole<InputStream>() {

                @Override
                public void onMessage(InputStream stream) {

                    try {
                        _wtr.addMessage(Utils.getInputStreamData(stream));
                        if (_wtr.limitReached()) {
                            _wtr.terminateClient();
                        }
                        else {
                            byte[] ba = _data[_counter++];
                            sess.getBasicRemote().sendBinary(ByteBuffer.wrap(ba));
                        }
                    } catch (Exception e) {
                        _wtr.addExceptionAndTerminate("Error publishing msg", e);
                    }
                }
            });
            try {
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(_data[0]));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    public static class ByteBufferTest extends ProgrammaticClientEP {
        private ByteBuffer[] _data = {};
        private int _counter = 1;

        public ByteBufferTest(ByteBuffer[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.setMaxBinaryMessageBufferSize(70000);
            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                @Override
                public void onMessage(ByteBuffer buf) {
                    _wtr.addMessage(buf);
                    if (_wtr.limitReached()) {
                        _wtr.terminateClient();
                    }
                    else {

                        try {
                            ByteBuffer bb = _data[_counter++];
                            sess.getBasicRemote().sendBinary(bb);
                        } catch (Exception e) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", e);
                        }
                    }
                }
            });
            try {
                sess.getBasicRemote().sendBinary(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);
            }
        }

    }

    public static class ByteArrayTest extends ProgrammaticClientEP {

        public byte[][] _data = {};
        public int _counter = 1;

        public ByteArrayTest(byte[][] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            session.addMessageHandler(new MessageHandler.Whole<byte[]>() {

                @Override
                public void onMessage(byte[] retData) {
                    _wtr.addMessage(retData);
                    if (_wtr.limitReached()) {
                        _wtr.terminateClient();
                    }
                    else {

                        try {
                            byte[] ba = _data[_counter++];
                            sess.getBasicRemote().sendBinary(ByteBuffer.wrap(ba));
                        } catch (Exception e) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", e);
                        }
                    }
                }
            });

            try {
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(_data[0]));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    // TODO - write this using decoders and encoders
    public static class CodingTest extends ProgrammaticClientEP {
        private String[] _data = {};
        private int _counter = 1;

        public CodingTest(String[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {

            final Session sess = session;
            // session.addMessageHandler(new MessageHandler.Whole<FormatOne>() {
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String val) {
                    _wtr.addMessage(val);
                    if (_wtr.limitReached()) {
                        _wtr.terminateClient();
                    }
                    else {

                        try {
                            String s = _data[_counter++];
                            sess.getBasicRemote().sendText(s);
                        } catch (Exception e) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", e);
                        }
                    }
                }
            });
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    public static class PartialTextTestWithPong extends ProgrammaticClientEP {
        private String[] _data = {};
        private int _counter = 1;
        boolean pingSent = false;

        public PartialTextTestWithPong(String[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session, EndpointConfig arg1) {
            final Session sess = session;

            session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
                @Override
                public void onMessage(PongMessage msg) {

                    ByteBuffer buf = msg.getApplicationData();
                    int length = buf.limit() - buf.position();
                    byte[] ba = new byte[length];
                    buf.get(ba, 0, length);
                    String s = new String(ba);
                    _wtr.addMessage(s);

                }
            });

            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    _wtr.addMessage(text);
                    if (_wtr.limitReached()) {
                        _wtr.terminateClient();
                    }
                    else {
                        try {
                            String toSend = _data[_counter++];
                            String x = null;
                            // send the string 1 char at a time
                            for (int i = 0; i < toSend.length(); i++) {
                                x = toSend.substring(i, i + 1);
                                if (i + 1 < toSend.length()) {
                                    sess.getBasicRemote().sendText(x, false);

                                    if ((i == 2) && (pingSent == false)) {
                                        // send a ping in the middle of our fist partial message
                                        Thread.sleep(100);
                                        String s2 = "This is a ping pong message";
                                        byte[] ba2 = s2.getBytes();
                                        ByteBuffer b2 = ByteBuffer.wrap(ba2);
                                        pingSent = true;
                                        sess.getBasicRemote().sendPing(b2);
                                        Thread.sleep(3000);
                                    }
                                } else {
                                    sess.getBasicRemote().sendText(x, true);
                                }
                                Thread.sleep(100);
                            }
                        } catch (Exception e) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", e);
                        }
                    }
                }
            });
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    public static class CloseTestOnOpen extends ProgrammaticClientEP {
        private String[] _data = {};

        public CloseTestOnOpen(String[] data) {
            _data = data;
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            _wtr.addMessage(closeReason.getCloseCode().getCode() + ":" + closeReason.getReasonPhrase());
            _wtr.setClosedAlready(true);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
         */
        @Override
        public void onOpen(Session sess, EndpointConfig arg1) {}

    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        try {
            session.close();
        } catch (IOException e) {
            _wtr.addExceptionAndTerminate("Error closing session", e);
        }
    }

    @Override
    public void onError(Session session, java.lang.Throwable throwable) {

        _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
    }

}
