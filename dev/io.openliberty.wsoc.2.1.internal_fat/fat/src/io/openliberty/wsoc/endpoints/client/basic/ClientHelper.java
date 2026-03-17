/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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

import java.util.logging.Logger;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

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
