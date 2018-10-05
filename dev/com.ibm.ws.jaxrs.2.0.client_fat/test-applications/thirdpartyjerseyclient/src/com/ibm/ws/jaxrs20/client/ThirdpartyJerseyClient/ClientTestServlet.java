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
package com.ibm.ws.jaxrs20.client.ThirdpartyJerseyClient;

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
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

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
            m.put("context", req.getContextPath());

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }

    public void testNewClientBuilder_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        ret.append("OK");
    }

    public void testNewClient_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        c.close();
        ret.append("OK");
    }

    public void testNewWebTarget_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        c.close();
        ret.append("OK");
    }

    public void testNewInvocationBuilder_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        c.close();
        ret.append("OK");
    }

    public void testNewInvocation_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        Invocation iv = ib.buildGet();
        c.close();
        ret.append("OK");
    }

    public void testFlowProgram_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        String res = c.target("http://" + serverIP + ":" + serverPort + "/" + param.get("context")
                              + "/Test/BasicResource").path("echo").path(param.get("param")).request().get(String.class);
        c.close();
        ret.append(res);
    }
}
