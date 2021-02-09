/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.JAXRS21ClientLTPA.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

/**
 *
 */
@WebServlet("/JAXRS21ClientTestServlet")
public class JAXRS21ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String moduleName = "jaxrs21clientltpa";

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
            m.put("context", req.getContextPath());

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }

    public void testClientLtpaHander_ClientNoTokenWithSSL(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientLtpaHander_ClientNoTokenWithSSL: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "true");
        WebTarget t = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource").path("echo").path(param.get("param"));
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<String> completionStage = completionStageRxInvoker.get(String.class);
        CompletableFuture<String> completableFuture = completionStage.toCompletableFuture();
        try {
            String response = completableFuture.get();
            ret.append(response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testClientLtpaHander_ClientNoTokenWithSSLDefault(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientLtpaHander_ClientNoTokenWithSSLDefault: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "true");
        WebTarget t = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource").path("echo").path(param.get("param"));
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<String> completionStage = completionStageRxInvoker.get(String.class);
        CompletableFuture<String> completableFuture = completionStage.toCompletableFuture();
        try {
            String response = completableFuture.get();
            ret.append(response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

}
