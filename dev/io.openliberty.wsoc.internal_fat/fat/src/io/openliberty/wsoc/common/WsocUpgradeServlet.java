/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.wsoc.WsWsocServerContainer;

/**
 *
 */
public class WsocUpgradeServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(WsocUpgradeServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

        String path = req.getServletPath();
        String pathInfo = req.getPathInfo();
        if (!pathInfo.endsWith("/")) {

            pathInfo = pathInfo + "/";
        }

        LOG.info("Request PathInfo: " + pathInfo);

        boolean useConfig = false;
        String endpointClass = "";
        String endpointConfig = "";

        String tokens[] = pathInfo.split("/");
        if (tokens.length <= 1) {
            sendError(resp, 500, "This test classes requires additional path params indicating endpoitnconfig or endpoint class to use.", null);
            return;

        }
        else if (tokens.length == 2) {
            endpointClass = tokens[1];
        }
        else if (tokens.length > 2) {
            String type = tokens[1];
            if ("EndpointConfig".equals(tokens[1])) {
                useConfig = true;
                endpointConfig = tokens[2];
            }
            else {
                endpointClass = tokens[2];
            }

        }

        LOG.info("Using EndpointConfig: " + useConfig + " EndpointConfig class: " + endpointConfig + " Endpoint class" + endpointClass);
        ServerEndpointConfig sec = null;
        if (useConfig) {
            try {
                Class<?> theclass = Class.forName(endpointConfig);
                sec = (ServerEndpointConfig) theclass.newInstance();

            } catch (Exception e) {
                sendError(resp, 500, "Unable to create class " + endpointConfig, e);
                e.printStackTrace();
                return;
            }
        }
        else {
            try {
                Class<?> theclass = Class.forName(endpointClass);
                sec = ServerEndpointConfig.Builder.create(theclass, path).build();

            } catch (Exception e) {
                sendError(resp, 500, "Unable to create class " + endpointClass, e);
                e.printStackTrace();
                return;
            }

        }

        ServerContainer container = (ServerContainer) req.getServletContext().getAttribute("javax.websocket.server.ServerContainer");
        if (container instanceof WsWsocServerContainer) {
            WsWsocServerContainer ws = (WsWsocServerContainer) container;
            ws.doUpgrade(req, resp, sec, null);

        }

    }

    private void sendError(HttpServletResponse resp, int code, String message, Exception e) throws IOException {
        LOG.warning("Error in WsocUpgradeServlet:  " + message);
        if (e != null) {
            e.printStackTrace();
        }
        resp.setStatus(code);
        Writer wr = resp.getWriter();
        wr.write(message);
        wr.close();
    }
}
