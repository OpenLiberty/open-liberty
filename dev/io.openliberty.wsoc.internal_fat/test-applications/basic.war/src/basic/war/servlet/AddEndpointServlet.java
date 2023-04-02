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
package basic.war.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.DeploymentException;

import com.ibm.websphere.wsoc.WsWsocServerContainer;
import basic.war.EchoServerEP;

/**
 * Servlet for testing removed restriction when adding an endpoint in Websocket 2.1 with addEndpoint methods in ServerContainer https://github.com/jakartaee/websocket/issues/211
 * As stated in the specification before 2.1, "These methods are only operational during the application deployment phase of an application. Specifically, as soon as any of the
 * server endpoints within the application have accepted an opening handshake request, the APIs may not longer be used. This restriction may 
 * be relaxed in a future version."
 * 
 * Before 2.1, endpoints can only registered during the deployment of the web application. Once a websocket request enters the webcontainer,
 * no more endpoints could be added with the API as stated in the spec "are only operational during the application deployment phase of an application. 
 * Specifically, as soon as any of the server endpoints within the application have accepted an opening handshake request, the apis may not
 * longer be used. This restriction may be relaxed in a future version.". This restriction was removed in EE10 WebSocket 2.1
 */
public class AddEndpointServlet extends HttpServlet {


    /**
     * Before 2.1, endpoints can only registered during the during the initialization phase of the application. Once a websocket request enters the webcontainer,
     * no more endpoints could be added with the addEndpoint methods. This restriction was removed in EE10 WebSocket 2.1
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        // Get websocket container from servlet context
        ServerContainer container = (ServerContainer) req.getServletContext().getAttribute("javax.websocket.server.ServerContainer");

        if (container instanceof WsWsocServerContainer) {
            WsWsocServerContainer ws = (WsWsocServerContainer) container;
            try{
                // Upgrade request to websocket to hit the webcontainer and set restrictions. In essence, move out of web application deployment phase 
                ws.doUpgrade(req, resp, ServerEndpointConfig.Builder.create(EchoServerEP.class, "/echo").build(), new HashMap<String, String>());
                // Add a new endpoint to test restrictions
                ws.addEndpoint(ServerEndpointConfig.Builder.create(EchoServerEP.class, "/newEchoEndpointAdded").build());
            } catch(DeploymentException ex){
                // Do nothing
            }
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
