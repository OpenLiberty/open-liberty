/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.common.Constants;

import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.tests.all.TimeOutTest;

/**

 */
public class ServerUserPropertiesClientEP implements TestHelper {

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(ServerUserPropertiesClientEP.class.getName());

    @ClientEndpoint
    public static class UserPropertiesTest extends ServerUserPropertiesClientEP {

        @OnOpen
        public void onOpen(Session sess) {
            try {
                // Make onMessage call on server.
                sess.getBasicRemote().sendText("test");
            } catch (Exception e) {
 
            }
        }

        @OnMessage
        public String onMessage(String data) {
            _wtr.addMessage(data);
            if(_wtr.limitReached()){
                _wtr.terminateClient();
            }
            return null;
        }
    }

    @OnError
    public void onError(Session session, java.lang.Throwable throwable) {
        LOG.warning(throwable.toString());
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

