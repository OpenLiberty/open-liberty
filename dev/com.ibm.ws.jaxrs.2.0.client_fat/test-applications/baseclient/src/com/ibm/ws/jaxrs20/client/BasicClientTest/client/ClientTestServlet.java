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
package com.ibm.ws.jaxrs20.client.BasicClientTest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String moduleName = "baseclient";

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
            e.printStackTrace(); // print to the logs too since the test client only reads the first line of the pw output
            if (e instanceof InvocationTargetException) {
                e.getCause().printStackTrace(pw);
            } else {
                e.printStackTrace(pw);
            }
        }
    }

    public void testNewClientBuilder(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        ret.append("OK");
    }

    public void testNewClient(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        c.close();
        ret.append("OK");
    }

    public void testNewWebTarget(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        c.close();
        ret.append("OK");
    }

    public void testNewWebTargetSupportMapType(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        t = t.path("test1/{index}").resolveTemplateFromEncoded("index", 2000);
        c.close();
        ret.append("OK");
    }

    public void testNewInvocationBuilder(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        c.close();
        ret.append("OK");
    }

    public void testNewInvocation(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        Invocation iv = ib.buildGet();
        c.close();
        ret.append("OK");
    }

    public void testDefaultAccept(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        String res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/BasicClientTest/BasicResource")
                        .path("defaultaccept")
                        .request()
                        .get(String.class);
        c.close();
        ret.append(res);
    }

    public void testFlowProgram(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        String res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/BasicClientTest/BasicResource")
                        .path("echo")
                        .path(param.get("param"))
                        .request()
                        .get(String.class);
        c.close();
        ret.append(res);
    }

    /*
     * Test to make sure uri's with query parameters with empty values do not
     * get the '=' character stripped away, aka "?param=" should not become "?param"
     * If the '=' character is stripped, then by default web container will ignore
     * the param entirely
     */
    public void testQueryParam(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        String base = "http://" + serverIP + ":" + serverPort + "/" + moduleName + "/BasicClientTest/BasicResource/query";
        String uri = base + "?param=";
        String uri2 = base + "?param";

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget webTarget = c.target(uri);
        WebTarget webTarget2 = c.target(uri2);

        // verify that the trailing '=' character is not lost when we build the uri
        assertEquals(uri, webTarget.getUri().toString());

        // try again with no '='
        // verify that '=' is not added to the uri
        assertEquals(uri2, webTarget2.getUri().toString());

        // verify that the resource class got a query parameter with an empty value
        if (!sendQueryRequest(webTarget)) {
            fail("Query param ignored for " + webTarget.getUri().toString());
        }

        // we know that CXF removes query parameters with null values
        // which is the case for requests from the same server.
        if (sendQueryRequest(webTarget2)) {
            fail("Behavior change - query param not ignored for " + webTarget2.getUri().toString());
        }

        c.close();
        ret.append("OK");
    }

    private boolean sendQueryRequest(WebTarget webTarget) {
        String res = webTarget.request().get(String.class);
        System.out.println(webTarget.getUri().toString() + " res=" + res);
        if (res.equals("null")) {
            System.out.println(webTarget.getUri().toString() + " - param was ignored by webcontainer");
            return false;
        }
        return true;
    }
}
