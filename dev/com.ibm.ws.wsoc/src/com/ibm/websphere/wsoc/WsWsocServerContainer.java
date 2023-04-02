/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.websphere.wsoc;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

/**
 * This interface provides WebSphere specific extensions to WebSocket ServerContainer support.
 * 
 * Example:
 * <pre>
 * <code>
 *     ServerContainer container = (ServerContainer) httpServletRequest.getServletContext().getAttribute("javax.websocket.server.ServerContainer");
 *     if (container instanceof WsWsocServerContainer) {
 *         WsWsocServerContainer ws = (WsWsocServerContainer) container;
 *         ...
 *     }
 * </code>
 * </pre>
 * 
 * @deprecated Since websocket-2.1 - This SPI is no longer necessary: the jakarta.websocket.server.ServerContainer now has upgradeHttpToWebSocket as part of the WebSocket 2.1 API.
 */
@Deprecated
public interface WsWsocServerContainer extends ServerContainer {

    /**
     * Performs a WebSocket upgrade on provided HttpServletRequest and HttpServletResponse with the specified ServerEndpointConfig. After a call to doUpgrade, the servlet response
     * is committed and you will be unable to write additional data or change the response code.
     * 
     * 
     * @param req -
     * @param resp -
     * @param serverEndpointConfig - server endpoint config object representing a WebSocket endpoint - either programmatic or annotated.
     * @param pathParams - additional parameters that will be made availble thorugh wsoc Session.getRequestParameterMap
     * 
     * 
     * @throws ServletException
     * @throws IOException
     * @deprecated Since websocket-2.1 - Use jakarta.websocket.server.ServerContainer#upgradeHttpToWebSocket(Object httpServletRequest, Object httpServletResponse, ServerEndpointConfig sec, Map<String,String> pathParameters) instead.
     */
    @Deprecated
    public void doUpgrade(HttpServletRequest req, HttpServletResponse resp, ServerEndpointConfig sec, Map<String, String> pathParams) throws ServletException, IOException;

}
