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
package io.openliberty.wsoc.servercontainer21;

import java.io.IOException;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.wsoc.WsWsocServerContainer;
import com.ibm.ws.wsoc.servercontainer.ServerContainerExt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

public class ServerContainerExt21 extends ServerContainerExt implements ServerContainer, WsWsocServerContainer {

    private static final TraceComponent tc = Tr.register(ServerContainerExt.class);

    /*
     * Since Websocket 2.1
     */
    @Override
    public void upgradeHttpToWebSocket(Object httpServletRequest, Object httpServletResponse, ServerEndpointConfig sec,
                                       Map<String, String> pathParameters) throws IOException, DeploymentException {

        try {
            doUpgrade((HttpServletRequest) httpServletRequest, (HttpServletResponse) httpServletResponse, sec, pathParameters);
        } catch (ServletException ex) {
            String msg = Tr.formatMessage(tc, "upgradeHttpToWebSocket.failed", ex.getMessage());
            Tr.error(tc, "upgradeHttpToWebSocket.failed");
            throw new DeploymentException(msg, ex);
        }

    }

    /*
     * Kept for WebSocket 2.1, but will be removed from the next release.
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.wsoc.WsWsocServerContainer#upgrade(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.websocket.EndpointConfig,
     * java.lang.String)
     */
    @Deprecated
    @Override
    public void doUpgrade(HttpServletRequest request, HttpServletResponse response, ServerEndpointConfig endpointConfig,
                          Map<String, String> pathParams) throws ServletException, IOException {

        wsocUpgradeHandler.handleRequest(request, response, endpointConfig, pathParams, true);
        if (!response.isCommitted()) {
            response.getOutputStream().close();
        }

    }

    @Override
    public void addEndpoint(Class<?> endpointClass) throws DeploymentException {
        super.addEndpoint(endpointClass);
    }

    @Override
    public void addEndpoint(ServerEndpointConfig serverConfig) throws DeploymentException {
        super.addEndpoint(serverConfig);
    };

}
