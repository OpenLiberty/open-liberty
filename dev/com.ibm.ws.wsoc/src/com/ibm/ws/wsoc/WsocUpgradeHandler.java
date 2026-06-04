/*******************************************************************************
 * Copyright 2017, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc;

import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;

import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.transport.access.TransportConnectionUpgrade;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.ws.wsoc.external.SessionExt;
import com.ibm.ws.wsoc.external.WebSocketContainerExt;
import com.ibm.ws.wsoc.external.WebSocketFactory;

public class WsocUpgradeHandler implements HttpUpgradeHandler, TransportConnectionUpgrade {

    Endpoint endpoint = null;
    EndpointConfig endpointConfig = null;
    ParametersOfInterest things = null;
    SessionImpl sessionImpl = null;
    SessionExt sessionExt = null;
    WebSocketContainerExt container = null;

    private static final String HTTP_WEBSOCKET_BUFFER_SIZE_STATE_MAP_KEY = "com.ibm.ws.http.channel.internal.inbound.HttpInboundLink.websocketBufferSize";

    public WsocUpgradeHandler() {

    }

    public void initialize(Endpoint _ep, EndpointConfig _epc, WebSocketContainerExt _container) {
        endpoint = _ep;
        endpointConfig = _epc;
        container = _container;

    }

    @Override
    public void destroy() {
        if (sessionImpl != null) {
            sessionImpl.internalDestory();
        }
        endpoint = null;
        sessionImpl = null;
    }

    @Override
    public void init(WebConnection wc) {
        // we expect user's of our code to use the access service.
    }

    @Override
    public void init(TransportConnectionAccess access) {
        if (access == null) {
            return;
        }

        initializeContainerDefaultsFromInboundHttpConfig(access);

        // a new websocket session is ready to start up
        // SessionImpl is our internal view of this session and sessionExt is the customer facing external view of this session.
        // not very clean to have them both know about each other, should clean this up later if possible
        sessionImpl = new SessionImpl();

        WebSocketFactory webSocketFactory = WebSocketVersionServiceManager.getWebSocketFactory();
        SessionExt sessionExt = webSocketFactory.getWebSocketSession();

        sessionExt.initialize(sessionImpl);
        sessionImpl.initialize(endpoint, endpointConfig, access, sessionExt, container);

        sessionImpl.setParametersOfInterest(things);
        sessionImpl.setPathParameters();
        sessionImpl.signalAppOnOpen();

        // release ref to the endpoint
        endpoint = null;
    }

    private void initializeContainerDefaultsFromInboundHttpConfig(TransportConnectionAccess access) {
        if (container == null) {
            return;
        }

        VirtualConnection virtualConnection = access.getVirtualConnection();
        if (virtualConnection == null) {
            return;
        }

        Object webSocketBufferSize = virtualConnection.getStateMap().get(HTTP_WEBSOCKET_BUFFER_SIZE_STATE_MAP_KEY);
        if (!(webSocketBufferSize instanceof Number)) {
            return;
        }

        int configuredBufferSize = ((Number) webSocketBufferSize).intValue();

        container.initializeDefaultsFromConfiguredBufferSize(configuredBufferSize);
    }

    public void setParametersOfInterest(ParametersOfInterest value) {
        things = value;
    }

}
