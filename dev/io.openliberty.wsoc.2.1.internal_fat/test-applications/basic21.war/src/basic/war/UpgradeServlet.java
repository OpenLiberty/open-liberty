
/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import jakarta.websocket.DeploymentException;

/**
 * Upgrade to ws using the Websocket 2.1 ServerContainer#upgradeHttpToWebSocket method
 */
public class UpgradeServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

        ServerContainer container = (ServerContainer) req.getServletContext().getAttribute("jakarta.websocket.server.ServerContainer");
        try{
            // echo endpoint is used into another test, so I am reusing it here. 
            container.upgradeHttpToWebSocket(req, resp, ServerEndpointConfig.Builder.create(EchoServerEP.class, "/echo").build(), new HashMap<String, String>());
        } catch(DeploymentException ex){
            
        }

    }

}
