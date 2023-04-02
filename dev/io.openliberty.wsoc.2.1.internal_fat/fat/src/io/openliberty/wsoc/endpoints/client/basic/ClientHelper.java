/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.common.Constants;

import io.openliberty.wsoc.common.Utils;

/**
 * Creates variosu Client Endpoints used within this FAT
 */
public abstract class ClientHelper extends Endpoint implements TestHelper {

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(ClientHelper.class.getName());


    public static class BasicClientEP extends ClientHelper {

        public String[] _data = {};

        public BasicClientEP(String[] data) {
            _data = data;
        }

        @Override
        public void onOpen(Session session,  EndpointConfig config) {
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                public void onMessage(String text) {
                               _wtr.addMessage(text);
                                 _wtr.terminateClient();
                }

            });
        
            try {
                session.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                //TODO: handle exception
            }
        }

    }

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
