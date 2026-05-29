/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
        System.out.println("WSOC DEBUG inbound init access=" + access + " container=" + container);
        if (access == null) {
            System.out.println("WSOC DEBUG inbound init returning: access is null");
            return;
        }

        initializeContainerDefaultsFromInboundHttpConfig(access);

        System.out.println("WSOC DEBUG inbound init container defaults after http config binary="
                           + (container == null ? "null-container" : Integer.valueOf(container.getDefaultMaxBinaryMessageBufferSize()))
                           + " text="
                           + (container == null ? "null-container" : Integer.valueOf(container.getDefaultMaxTextMessageBufferSize())));

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

    private static final String HTTP_WEBSOCKET_BUFFER_SIZE_STATE_MAP_KEY = "com.ibm.ws.http.channel.internal.inbound.HttpInboundLink.websocketBufferSize";

    private void initializeContainerDefaultsFromInboundHttpConfig(TransportConnectionAccess access) {
        if (container == null) {
            System.out.println("WSOC DEBUG inbound http config skipped: container is null");
            return;
        }

        VirtualConnection virtualConnection = access.getVirtualConnection();
        System.out.println("WSOC DEBUG inbound virtualConnection=" + virtualConnection);
        if (virtualConnection == null) {
            System.out.println("WSOC DEBUG inbound http config skipped: virtualConnection is null");
            return;
        }

        Object webSocketBufferSize = virtualConnection.getStateMap().get(HTTP_WEBSOCKET_BUFFER_SIZE_STATE_MAP_KEY);
        System.out.println("WSOC DEBUG inbound state map websocketBufferSize=" + webSocketBufferSize
                           + " valueClass=" + (webSocketBufferSize == null ? null : webSocketBufferSize.getClass().getName()));
        if (!(webSocketBufferSize instanceof Number)) {
            System.out.println("WSOC DEBUG inbound http config skipped: state map websocket buffer size is not a Number");
            return;
        }

        long configuredBufferSize = ((Number) webSocketBufferSize).longValue();
        long originalConfiguredBufferSize = configuredBufferSize;
        if (configuredBufferSize > Integer.MAX_VALUE) {
            configuredBufferSize = Integer.MAX_VALUE;
        } else if (configuredBufferSize < Integer.MIN_VALUE) {
            configuredBufferSize = Integer.MIN_VALUE;
        }

        System.out.println("WSOC DEBUG inbound applying websocketBufferSize from state map original="
                           + originalConfiguredBufferSize + " clamped=" + configuredBufferSize);
        container.initializeDefaultsFromConfiguredBufferSize(Integer.valueOf((int) configuredBufferSize));
        System.out.println("WSOC DEBUG inbound applied container defaults binary="
                           + container.getDefaultMaxBinaryMessageBufferSize()
                           + " text=" + container.getDefaultMaxTextMessageBufferSize());
    }

    public void setParametersOfInterest(ParametersOfInterest value) {
        things = value;
    }

}
