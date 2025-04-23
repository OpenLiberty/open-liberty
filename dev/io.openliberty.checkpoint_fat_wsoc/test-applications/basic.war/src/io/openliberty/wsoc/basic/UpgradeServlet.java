/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.basic;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import com.ibm.websphere.wsoc.WsWsocServerContainer;
import javax.websocket.server.ServerEndpointConfig;

public class UpgradeServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        ServerContainer container = (ServerContainer) req.getServletContext().getAttribute("javax.websocket.server.ServerContainer");

        if (container instanceof WsWsocServerContainer) {
            WsWsocServerContainer ws = (WsWsocServerContainer) container;
            try {
                // Upgrade request to websocket to hit the webcontainer and set restrictions. In essence, move out of web application deployment phase 
                ws.doUpgrade(req, resp, ServerEndpointConfig.Builder.create(EchoServerEP.class, "/echo").build(), new HashMap<String, String>());
                // Add a new endpoint to test restrictions
                // ws.addEndpoint(ServerEndpointConfig.Builder.create(EchoServerEP.class, "/newEchoEndpointAdded").build());
            } catch (Exception ex) {
                // Do nothing
            }
        }

    }

}
