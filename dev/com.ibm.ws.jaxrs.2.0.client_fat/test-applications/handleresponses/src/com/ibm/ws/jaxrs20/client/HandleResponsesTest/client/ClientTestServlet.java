/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.HandleResponsesTest.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String moduleName = "handleresponses";

    private static final String getRemoteUri(Map<String, String> param, String resourcePath) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        return "http://" + serverIP + ":" + serverPort + "/" + moduleName + "/resource" + resourcePath;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            m.put("serverIP", req.getLocalAddr());
            m.put("serverPort", String.valueOf(req.getLocalPort()));

            StringBuilder ret = new StringBuilder();
            ret.append(testM.invoke(this, m));
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(); // print to the logs too since the test client only reads the first line of the pw output
            if (e instanceof InvocationTargetException) {
                e.getCause().printStackTrace(pw);
            } else {
                e.printStackTrace(pw);
            }
        }
    }

    public String testEmpty202Response(Map<String, String> param) {

        Response r = ClientBuilder.newClient().target(getRemoteUri(param, "/202/empty")).request().get();
        int status = r.getStatus();
        if (status != 202) {
            return "Unexpected status code: " + status;
        }
        String entity = null;
        try {
            entity = r.readEntity(String.class);
            return "Did not throw expected IllegalStateException";
        } catch (IllegalStateException expected) {
            entity = null;
        } catch (Throwable t) {
            t.printStackTrace();
            return "Caught unexpected exception: " + t;
        }
        return "OK";
    }

    public String testNonEmpty202Response(Map<String, String> param) {

        Response r = ClientBuilder.newClient().target(getRemoteUri(param, "/202/echo/GrandCanyon")).request().get();
        int status = r.getStatus();
        if (status != 202) {
            return "Unexpected status code: " + status;
        }
        String entity = r.readEntity(String.class);
        if (!"202GrandCanyon".equals(entity)) {
            return "Unexpected entity (expected \"202GrandCanyon\"): " + entity;
        }
        return "OK";
    }

}
