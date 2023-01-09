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
package com.ibm.ws.wsoc.servercontainer.v10;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.wsoc.WsWsocServerContainer;
import com.ibm.ws.wsoc.servercontainer.ServerContainerExt;

public class ServerContainerExt10 extends ServerContainerExt implements WsWsocServerContainer {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.wsoc.WsWsocServerContainer#upgrade(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.websocket.EndpointConfig,
     * java.lang.String)
     */
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
