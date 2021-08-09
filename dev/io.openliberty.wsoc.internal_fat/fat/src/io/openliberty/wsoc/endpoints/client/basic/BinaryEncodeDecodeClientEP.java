/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 * Client EP for WebSocket Binary decoder and encoder
 * 
 * @author Rashmi Hunt
 */
public class BinaryEncodeDecodeClientEP implements TestHelper {

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(BinaryEncodeDecodeClientEP.class.getName());

    @ClientEndpoint
    public static class ByteBufferTest extends BinaryEncodeDecodeClientEP {

        public ByteBuffer[] _data = {};
        public int _counter = 0;

        public ByteBufferTest(ByteBuffer[] data) {
            _data = data;
        }

        @OnMessage
        public String echoData(ByteBuffer data) {
            _wtr.addMessage(new String(data.array()));
            LOG.log(Level.INFO, "In client EP. Result from server ", new String(data.array()));
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                _wtr.addMessage(data);
                ByteBuffer bb = _data[_counter++];
                return new String(bb.array());
            }

            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            LOG.log(Level.INFO, "In client EP. onOpen()");
            try {
                ByteBuffer bb = _data[_counter++];
                sess.getBasicRemote().sendBinary(bb);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);
            }
        }
    }

    @ClientEndpoint
    public static class BinaryStreamTest extends BinaryEncodeDecodeClientEP {

        public InputStream _data;
        public int _counter = 0;

        public BinaryStreamTest(InputStream data) {
            _data = data;
        }

        @OnMessage
        public String echoData(String data) {
            _wtr.addMessage(data);
            LOG.log(Level.INFO, "In client EP. Result from server " + data);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                _wtr.addMessage(data);
                return data;
            }

            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            LOG.log(Level.INFO, "In client EP. onOpen()");
            try {
                sess.getBasicRemote().sendObject(_data);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

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
