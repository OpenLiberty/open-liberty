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
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.ParamTypeBinaryDecoder;
import io.openliberty.wsoc.common.ParamTypeBinaryEncoder;
import io.openliberty.wsoc.common.ParamTypeTextDecoder;
import io.openliberty.wsoc.common.ParamTypeTextEncoder;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.tests.all.AnnotatedTest;

/**
 *
 */
public class AnnotatedClientEP implements TestHelper {

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(AnnotatedClientEP.class.getName());
    protected boolean EXPECT_TIMEOUT_ERROR = false;
    private final String CLOSE_1006_ERROR_EXCEPTION = "org.eclipse.jetty.websocket.api.ProtocolException: Frame forbidden close status code: 1006";

    @ClientEndpoint
    public static class ByteArrayTest extends AnnotatedClientEP {

        public byte[][] _data = {};
        public int _counter = 0;

        public ByteArrayTest(byte[][] data) {
            _data = data;
        }

        @OnMessage
        public byte[] echoData(byte[] data) {
            _wtr.addMessage(data);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return _data[_counter++];
            }

            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {

                byte[] ba = _data[_counter++];
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(ba));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class ByteBufferTest extends AnnotatedClientEP {

        public ByteBuffer[] _data = {};
        public int _counter = 0;

        public ByteBufferTest(ByteBuffer[] data) {
            _data = data;
        }

        @OnMessage
        public ByteBuffer echoData(ByteBuffer data) {

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                _wtr.addMessage(data);
                return _data[_counter++];
            }

            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                ByteBuffer bb = _data[_counter++];
                sess.getBasicRemote().sendBinary(bb);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class ReaderTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public ReaderTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(Reader data) {

            try {
                _wtr.addMessage(Utils.getReaderText(data));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing msg", e);
            }

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return _data[_counter++];
            }

            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class TextTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TextTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(String data) {

            _wtr.addMessage(data);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return _data[_counter++];
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class DoNothingTest extends AnnotatedClientEP {

        Exception eFailed = null;

        @OnOpen
        public void onOpen(Session sess) {

            LOG.info("DoNothingTest: OnOpen entered");

            URI uri = sess.getRequestURI();
            if (uri != null) {
                Map<String, List<String>> params = sess.getRequestParameterMap();
                if (params != null) {
                    List<String> str = params.get("testMe");
                    if (str != null) {
                        if (str.indexOf("HI") != 0) {
                            LOG.info("Error: query string parms: " + str);
                            eFailed = new Exception("parms incorrect in parameter map");
                        } else {
                            LOG.info("client side found correct parameter value: " + str);
                        }
                    } else {
                        LOG.info("Error: null parms in parameter map");
                        eFailed = new Exception("missing parms in parameter map");
                    }
                } else {
                    LOG.info("Error: getRequestParameterMap is null");
                    eFailed = new Exception("parameter map is null");
                }

                String query = sess.getQueryString();
                if (query != null) {
                    if (query.indexOf("testMe=HI") == -1) {
                        LOG.info("Error: getQueryString is wrong: " + query);
                        eFailed = new Exception("bad query string");
                    } else {
                        LOG.info("client side found correct QueryString: " + query);
                    }
                } else {
                    LOG.info("Error: query string is null");
                    eFailed = new Exception("null query string");
                }
            } else {
                LOG.info("Error: uri is null");
                eFailed = new Exception("uri is null");
            }

        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            LOG.info("onClose received reason phrase: " + closeReason.getReasonPhrase());
        }

        @OnMessage
        public void messageProcessor(String msg, Session sess) {

            // first checked if client onOpen shows success
            if (eFailed != null) {
                AnnotatedTest.connectToClassResult = Constants.FAILED + " " + eFailed.getMessage();
            } else {
                // check if server side also showed success
                if ((msg.indexOf(Constants.SUCCESS) != -1)) {
                    AnnotatedTest.connectToClassResult = Constants.SUCCESS;
                } else {
                    LOG.info(msg);
                    AnnotatedTest.connectToClassResult = Constants.FAILED + ": " + msg;
                }
            }
        }

    }

    @ClientEndpoint
    public static class AsyncTextTest extends AnnotatedClientEP {

        volatile int echoHitCount = 0;
        volatile int resultHitCount = 0;

        public String[] _data = {};
        public int _counter = 0;

        public AsyncTextTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(String msg, Session sess) {

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

            _wtr.addMessage(msg);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {

                String s = _data[_counter++];
                sess.getAsyncRemote().sendText(s, new SendHandler() {
                    @Override
                    public void onResult(SendResult result) {

                        boolean ok = result.isOK();
                        resultHitCount++;
                        if (!ok) {
                            _wtr.addExceptionAndTerminate("Error publishing msg", new IOException("Error publishing msg, async handler is not ok."));
                        }
                    }

                });

            }
        }

        @OnOpen
        public void onOpen(Session sess) {
            echoHitCount = 0;
            resultHitCount = 0;

            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class FutureTextTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public FutureTextTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(String msg, Session sess) {

            _wtr.addMessage(msg);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {

                String s = _data[_counter++];
                Future<Void> future = sess.getAsyncRemote().sendText(s);
                try {
                    future.get();
                } catch (Exception e) {
                    _wtr.addExceptionAndTerminate("Error publishing msg", e);
                }
            }
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                // need to inc counter before sending to avoid race conditions
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class FutureWithReturnTextTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public FutureWithReturnTextTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(String msg, Session sess) {

            // get two message for every one that is sent, so not using echo messages for this test
            _counter++;

            _wtr.addMessage(msg);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class FutureWithReturnByteTest extends AnnotatedClientEP {

        public byte[] _data = {};
        public int _counter = 0;

        public FutureWithReturnByteTest(byte[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(byte b, Session sess) {

            // get two message for every one that is sent, so not using echo messages for this test
            _counter++;

            Byte b2 = new Byte(b);
            String msg = b2.toString();

            _wtr.addMessage(msg);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                byte b = _data[_counter++];
                sess.getBasicRemote().sendObject(b);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class OnMsgVoidReturnTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public OnMsgVoidReturnTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(String data, Session sess) {
            _wtr.addMessage(data);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {

                try {
                    String s = _data[_counter++];
                    sess.getBasicRemote().sendText(s);

                } catch (Exception e) {
                    _wtr.addExceptionAndTerminate("Error publishing msg", e);
                }
            }
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class InputStreamTest extends AnnotatedClientEP {

        public byte[][] _data = {};
        public int _counter = 0;

        public InputStreamTest(byte[][] data) {
            _data = data;
        }

        @OnMessage
        public ByteBuffer echoData(InputStream stream) {

            try {
                _wtr.addMessage(Utils.getInputStreamData(stream));

                if (_wtr.limitReached()) {
                    _wtr.terminateClient();
                } else {
                    byte[] ba = _data[_counter++];
                    return ByteBuffer.wrap(ba);
                }
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                byte[] ba = _data[_counter++];
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(ba));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class BooleanTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public BooleanTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(Session sess, boolean data) {

            _wtr.addMessage(Boolean.valueOf(data).toString());
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                try {
                    String s = _data[_counter++];
                    sess.getBasicRemote().sendObject(s);
                } catch (Exception e) {
                    _wtr.addExceptionAndTerminate("Error sending data", e);
                }
            }

            return;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendObject(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class PingTest extends AnnotatedClientEP {

        public byte[][] _data = {};
        public int _counter = 0;

        public PingTest(byte[][] data) {
            _data = data;
        }

        @OnMessage
        public void echoData(PongMessage msg, Session sess) {
            ByteBuffer retBuf = msg.getApplicationData();
            byte[] retData = new byte[retBuf.limit()];
            retBuf.get(retData, 0, retBuf.limit());

            _wtr.addMessage(retData);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                try {
                    byte[] ba = _data[_counter++];
                    sess.getBasicRemote().sendPing(ByteBuffer.wrap(ba));
                } catch (Exception e) {
                    _wtr.addExceptionAndTerminate("Error publishing initial message", e);

                }
            }

            return;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                byte[] ba = _data[_counter++];
                sess.getBasicRemote().sendPing(ByteBuffer.wrap(ba));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class PingPongTest extends AnnotatedClientEP {

        public byte[][] _data = {};
        public int _counter = 0;

        public PingPongTest(byte[][] data) {
            _data = data;
        }

        @OnMessage
        public void onMessage(PongMessage msg, Session sess) {

            ByteBuffer retBuf = msg.getApplicationData();
            byte[] retData = new byte[retBuf.limit()];
            retBuf.get(retData, 0, retBuf.limit());
            _wtr.addMessage(retData);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                try {
                    byte[] ba = _data[_counter++];
                    sess.getBasicRemote().sendPong(ByteBuffer.wrap(ba));
                } catch (Exception e) {
                    _wtr.addExceptionAndTerminate("Error publishing initial message", e);
                }
            }

            return;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                byte[] ba = _data[_counter++];
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(ba));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class TestDouble extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestDouble(String[] data) {
            _data = data;
        }

        @OnMessage
        public Double echoText(Double data) {

            _wtr.addMessage(data.toString());

            LOG.log(Level.INFO, "data ", data.toString());
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return new Double(_data[_counter++]);
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class TestFloat extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestFloat(String[] data) {
            _data = data;
        }

        @OnMessage
        public Float echoText(Float data) {

            _wtr.addMessage(data.toString());

            LOG.log(Level.INFO, "data ", data.toString());
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return new Float(_data[_counter++]);
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class TestLong extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestLong(String[] data) {
            _data = data;
        }

        @OnMessage
        public Long echoText(Long data) {

            _wtr.addMessage(data.toString());

            LOG.log(Level.INFO, "data ", data.toString());
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return new Long(_data[_counter++]);
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class TestInteger extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestInteger(String[] data) {
            _data = data;
        }

        @OnMessage
        public Integer echoText(Integer data) {

            _wtr.addMessage(data.toString());

            LOG.log(Level.INFO, "data ", data.toString());
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return new Integer(_data[_counter++]);
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class TestShort extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestShort(String[] data) {
            _data = data;
        }

        @OnMessage
        public Short echoText(Short data) {

            _wtr.addMessage(data.toString());

            LOG.log(Level.INFO, "data ", data.toString());
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return new Short(_data[_counter++]);
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class TestPrimitiveShort extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestPrimitiveShort(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(Session sess, short data) {

            _wtr.addMessage(new Short(data).toString());

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                try {
                    String s = _data[_counter++];
                    sess.getBasicRemote().sendObject(new Short(s).shortValue());
                } catch (Exception e) {

                    _wtr.addExceptionAndTerminate("Error sending message", e);
                }
            }
            return;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class TestbyteReturn extends AnnotatedClientEP {

        public byte[][] _data = {};
        public int _counter = 0;

        public TestbyteReturn(byte[][] data) {
            _data = data;
        }

        @OnMessage
        //[rashmi] TODO Even though onMessage() on the server returns just byte not byte[]
        //jetty client is unable to receieve the result as byte, hence the OnMessage parameter
        //here is declared as byte[] for now. Re-visit.
        public void echoData(byte[] data) {
            _wtr.addMessage(data);
            LOG.log(Level.INFO, "added return result " + data);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return;
            }

            return;
        }

        /*
         *
         * public byte[] echoData(byte[] data) {
         * _wtr.addMessage(data);
         * if (_wtr.limitReached()) {
         * _wtr.terminateClient();
         * }
         * else {
         * return _data[_counter++];
         * }
         *
         * return null;
         * }
         */

        @OnOpen
        public void onOpen(Session sess) {
            try {
                byte[] ba = _data[_counter++];
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(ba));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class AnnonotatedPartialTextTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 0;
        String echoMessage = "";

        public AnnonotatedPartialTextTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoText(String text, boolean isLast, Session sess) {

            echoMessage = echoMessage + text;
            if (isLast) {
                _wtr.addMessage(echoMessage);
                echoMessage = "";

                if (_wtr.limitReached()) {
                    _wtr.terminateClient();
                } else {
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

            return;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint(decoders = { ParamTypeTextDecoder.class, ParamTypeBinaryDecoder.class }, encoders = { ParamTypeTextEncoder.class, ParamTypeBinaryEncoder.class })
    public static class ParamTypeCodingTest extends AnnotatedClientEP {

        public String[] _data = {};
        public int _counter = 1;

        public ParamTypeCodingTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoBinary(HashSet<String> data) {

            _wtr.addMessage(ParamTypeBinaryEncoder.quickEncodeBinary(data));
            _wtr.terminateClient();

        }

        @OnMessage
        public void echoText(LinkedList<HashSet<String>> data, Session sess) {

            try {
                _wtr.addMessage(ParamTypeTextEncoder.quickEncode(data));
                HashSet<String> i = ParamTypeBinaryDecoder.quickDecodeBinary(_data[1]);
                sess.getBasicRemote().sendObject(i);

            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing msg", e);
            }
            return;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                sess.getBasicRemote().sendObject(ParamTypeTextDecoder.quickDecode(_data[0]));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class ByteTest extends AnnotatedClientEP {

        public byte _data;
        public int _counter = 0;

        public ByteTest(byte data) {
            _data = data;
        }

        @OnMessage
        public String echoData(byte data) {
            _wtr.addMessage(data);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            } else {
                return String.valueOf(_data);
            }

            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                sess.getBasicRemote().sendObject(_data);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class ByteReturnTest extends AnnotatedClientEP {

        public String _data;
        public int _counter = 0;

        public ByteReturnTest(String data) {
            _data = data;
        }

        @OnMessage
        public void receiveData(byte data) {
            _wtr.addMessage(data);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }

            // if both the client and server endpoint have an onMessage that returns a "byte", then we will be in an infinite loop.
            // So the client side will not return anything, and the server side can test out the return byte code.

        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                sess.getBasicRemote().sendText(_data);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class MaxBinaryMessageSizeTest extends AnnotatedClientEP {

        public byte[][] _data = {};
        public int _counter = 0;

        public MaxBinaryMessageSizeTest(byte[][] data) {
            _data = data;
        }

        @OnMessage
        public void echoData(byte[] data) {
            _wtr.addMessage(data);
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(_data[0]));
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(_data[1]));

            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            _wtr.setClosedAlready(true);
            _wtr.addMessage(closeReason.getCloseCode().getCode());
            _wtr.terminateClient();

        }

    }

    @ClientEndpoint
    public static class MaxTextMessageSizeTest extends AnnotatedClientEP {

        public String[] _data = null;
        public int _counter = 0;

        public MaxTextMessageSizeTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public void echoData(String data) {
            _wtr.addMessage(data);
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                sess.getBasicRemote().sendText(_data[0]);
                sess.getBasicRemote().sendText(_data[1]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            _wtr.setClosedAlready(true);
            _wtr.addMessage(closeReason.getCloseCode().getCode());
            _wtr.terminateClient();

        }

    }

    @ClientEndpoint
    public static class SessionIdleTest extends AnnotatedClientEP {
        public SessionIdleTest() {
            this.EXPECT_TIMEOUT_ERROR = true;
        }

        @OnMessage
        public void echoText(String data) {
            LOG.info("SessionIdleTest onMessage: " + data);
            _wtr.addMessage(data);
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            int closeCode;
            //First look for closecode inside closeReasonPhrase because if server sends a closecode of of 1006, current jetty client
            //implementation converts the closeCode to 1002 and closeReasonPhrase to "Invalid close code: 1006" before calling this
            //onClose() method. Tyrus client does do not this conversion from 1006--> 1002 and TCK test case also expects 1006 from server
            //when idleTimeout occurs at the server
            LOG.info("On close code: " + closeReason.getCloseCode().getCode());
            if (closeReason.getReasonPhrase().contains("1006"))
                closeCode = 1006;
            else
                closeCode = closeReason.getCloseCode().getCode();

            _wtr.addMessage(closeCode);
            _wtr.setClosedAlready(true);
            _wtr.terminateClient();

        }

    }

    @OnError
    public void onError(Session session, java.lang.Throwable throwable) {
        // Client automatically throws an error when a 1006 response is added. Verify if this
        // is what is happening on this client and do not throw error when it does since as seen above
        // on the onClose we expect it to behave this way and need to verify it
        LOG.warning(throwable.toString());
        if (this.EXPECT_TIMEOUT_ERROR && throwable.toString().equals(CLOSE_1006_ERROR_EXCEPTION))
            LOG.info("Skipping error when receiving a 1006 response since it is expected.");
        else
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
