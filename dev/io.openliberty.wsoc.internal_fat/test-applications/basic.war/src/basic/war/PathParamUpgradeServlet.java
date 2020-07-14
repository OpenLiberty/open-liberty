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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;

import com.ibm.websphere.wsoc.WsWsocServerContainer;

/**
 *
 */
public class PathParamUpgradeServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

        Map<String, String> themap = new HashMap<String, String>(5);
        themap.put("TEST1", "TEST1");
        themap.put("TEST2", "TEST2");

        ServerContainer container = (ServerContainer) req.getServletContext().getAttribute("javax.websocket.server.ServerContainer");
        if (container instanceof WsWsocServerContainer) {
            WsWsocServerContainer ws = (WsWsocServerContainer) container;
            ws.doUpgrade(req, resp, new ServletUpgradePathEndpointConfig(), themap);
            // Our response should be committed by now, but we'll try to change it here for one of our tests...
            if (resp.getStatus() != 101) {
                resp.setStatus(500);
            }

        }

    }
}
