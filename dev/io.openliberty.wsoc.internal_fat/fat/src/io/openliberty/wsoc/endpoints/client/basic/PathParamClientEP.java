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

import java.io.IOException;
import java.io.Reader;
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
import io.openliberty.wsoc.common.Utils;

/**
 *
 */
public class PathParamClientEP implements TestHelper {

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(PathParamClientEP.class.getName());

    @ClientEndpoint
    public static class TextTest extends PathParamClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TextTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(String data) {
            LOG.info("TextTest.PathParamClientEP.echoText() " + data);

            _wtr.addMessage(data);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                return _data[_counter++];
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                LOG.info("TextTest.PathParamClientEP.onOpen() ");
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class TestShort extends PathParamClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestShort(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(Short data) {
            LOG.info("TextShort.PathParamClientEP.echoText() " + data);

            _wtr.addMessage(data.toString());

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                return _data[_counter++];
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                LOG.info("TextTest.PathParamClientEP.onOpen() ");
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }
    }

    @ClientEndpoint
    public static class ReaderTest extends PathParamClientEP {

        public String[] _data = {};
        public int _counter = 1;

        public ReaderTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(Reader data) {

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                try {
                    _wtr.addMessage(Utils.getReaderText(data));
                } catch (Exception e) {
                    _wtr.addExceptionAndTerminate("Error publishing msg", e);
                }
                return _data[_counter++];
            }

            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
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
