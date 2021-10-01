/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.callback.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 4523370851550171777L;
    private static final String moduleName = "jaxrs20clientcallback";

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
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }

    public void testClientAPIInsideInvocationCallback(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        String res = null;
        try {
            res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/rest/get?server=" + serverIP + "&port=" + serverPort)
                            .request()
                            .get(String.class);
        } catch (Exception e) {
            res = "[Proxy Error]:" + e.toString();
        } finally {
            c.close();
            ret.append(res);
        }
    }

    public void testCanReadEntityAndConsumeInvocationCallbackWithoutBuffering_Response(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        Future<Response> future = null;
        AtomicReference<String> invCallbackResult = new AtomicReference<>("uninvoked");
        InvocationCallback<Response> callback = new InvocationCallback<Response>() {

            @Override
            public void completed(Response r) {
                // note that we don't actually read the response here - we do that with the Future<Response> later in the code
                invCallbackResult.set("completed");
            }

            @Override
            public void failed(Throwable t) {
                t.printStackTrace();
                invCallbackResult.set(t.toString());
            }
            
        };
        try {
            future = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/rest/hello")
                      .request()
                      .async()
                      .get(callback);
            String res = future.get(2, TimeUnit.MINUTES).readEntity(String.class);
            ret.append(invCallbackResult.get()).append(" ").append(res);
        } catch (Exception e) {
            e.printStackTrace();
            ret.append("[Error]:" + e.toString());
        } finally {
            c.close();
        }
    }

    public void testCanReadEntityAndConsumeInvocationCallbackWithoutBuffering_String(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        Future<String> future = null;
        AtomicReference<String> invCallbackResult = new AtomicReference<>("uninvoked");
        InvocationCallback<String> callback = new InvocationCallback<String>() {

            @Override
            public void completed(String s) {
                invCallbackResult.set(s);
            }

            @Override
            public void failed(Throwable t) {
                t.printStackTrace();
                invCallbackResult.set(t.toString());
            }
            
        };
        try {
            future = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/rest/hello")
                      .request()
                      .async()
                      .get(callback);
            String res = future.get(2, TimeUnit.MINUTES);
            ret.append(invCallbackResult.get()).append(" ").append(res);
        } catch (Exception e) {
            e.printStackTrace();
            ret.append("[Error]:" + e.toString());
        } finally {
            c.close();
        }
    }
}