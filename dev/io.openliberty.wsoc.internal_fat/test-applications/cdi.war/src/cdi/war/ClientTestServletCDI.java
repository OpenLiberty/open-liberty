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
package cdi.war;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

/**
 *
 */
public class ClientTestServletCDI extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 8917212525932407301L;

    public static AtomicInteger waitForMessage = new AtomicInteger(0);
    public static String clientEPTestMessage = null;

    public static ClientEndpointConfig getDefaultConfig() {
        Builder b = ClientEndpointConfig.Builder.create();
        return b.build();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {

        PrintWriter pw = res.getWriter();
        String testName = req.getParameter("testname");
        String host = (req.getParameter("host") != null) ? req.getParameter("host") : req.getServerName();
        int port = (req.getParameter("port") != null) ? Integer.valueOf(req.getParameter("port")) : req.getServerPort();
        res.setStatus(200);

        Builder b = ClientEndpointConfig.Builder.create();
        ClientEndpointConfig cec = b.build();

        WebSocketContainer c = ContainerProvider.getWebSocketContainer();

        String serverEP = "ws://" + host + ":" + port + "/cdi/simpleServerEndpoint";
        URI uriServerEP = null;

        String ret = serverEP;
        boolean done = false;
        clientEPTestMessage = "new message";
        waitForMessage.set(0);

        try {
            if (testName.compareTo("testClientCDIOne") == 0) {
                uriServerEP = new URI(String.format(serverEP));
                c.connectToServer(AnnotatedCDIClientEP.class, uriServerEP);
            }
            if (testName.compareTo("testClientCDITwo") == 0) {
                uriServerEP = new URI(String.format(serverEP));
                c.connectToServer(ExtendedCDIClientEP.class, cec, uriServerEP);
            }
        } catch (URISyntaxException e) {
            ret = ret + " oops, FAILED. caught URISyntaxException: " + e;
        } catch (DeploymentException e) {
            ret = ret + " oops, FAILED. caught DeploymentException: " + e;
        }

        if (!done) {
            int count = 0;
            while (waitForMessage.intValue() == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                count++;
                if (count > 10) {
                    break;
                }
            }

            ret = clientEPTestMessage + " ret: " + ret;
        }

        pw.println(ret);
        pw.close();

    }
}
