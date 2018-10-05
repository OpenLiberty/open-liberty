/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.LtpaClientTest.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;
    private static final String moduleName = "jaxrs20ltpa";
    String serverPort = "8888";

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
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = (String) itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            m.put("serverIP", req.getLocalAddr());
            m.put("serverPort", String.valueOf(req.getLocalPort()));

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }

    public void testClientWrongLtpaHander_Client(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa", "true");
        String res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/rest/ltpa")
                        .request()
                        .get(String.class);
        c.close();
        ret.append(res);
    }

    public void testClientWrongValueLtpaHander_Client(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "false");
        String res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/rest/ltpa")
                        .request()
                        .get(String.class);
        c.close();
        ret.append(res);
    }

    public void testClientLtpaHander_Client(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "true");

        String res = "please see server log for emplty ltpa token error";
        try {
            res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/rest/ltpa")
                            .request()
                            .get(String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        c.close();
        ret.append(res);
    }
}
