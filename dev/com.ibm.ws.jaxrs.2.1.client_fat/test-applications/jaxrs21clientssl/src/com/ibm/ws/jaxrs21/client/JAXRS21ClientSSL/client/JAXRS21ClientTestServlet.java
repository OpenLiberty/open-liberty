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
package com.ibm.ws.jaxrs21.client.JAXRS21ClientSSL.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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

    private static final String moduleName = "jaxrs21clientssl";

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

    public void testClientBasicSSL_ClientBuilder(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSL_ClientBuilder: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        cb.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        Client c = cb.build();
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

    public void testClientBasicSSLDefault_ClientBuilder(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSLDefault_ClientBuilder: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
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

    public void testClientBasicSSL_Client(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSL_Client: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
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

    public void testClientBasicSSLDefault_Client(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSLDefault_Client: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
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

    public void testClientBasicSSL_WebTarget(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSL_WebTarget: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);

        Client c = cb.build();
        WebTarget wt = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource");
        wt.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        wt = wt.path("echo").path(param.get("param"));
        Builder builder = wt.request();
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

    public void testClientBasicSSLDefault_WebTarget(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSLDefault_WebTarget: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget wt = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource");
        wt = wt.path("echo").path(param.get("param"));
        Builder builder = wt.request();
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

    public void testClientBasicSSL_CustomizedSSLContext(Map<String, String> param, StringBuilder ret) {
        String res = "";

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSL_CustomizedSSLContext: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);

        // set a invalid Customized SSL context, then the access should fail as using Customized SSL context instead of
        // Liberty SSL config "mySSLConfig"
        KeyStore ts;
        try {
            ts = KeyStore.getInstance("jceks");
        } catch (KeyStoreException e1) {
            ret.append("new KeyStore fails");
            return;
        }
        String keyStorePath = this.getServletContext().getRealPath("/") + "/clientInvalidTrust.jks";

        try {
            ts.load(new FileInputStream(keyStorePath), "passw0rd".toCharArray());
        } catch (NoSuchAlgorithmException e1) {
            ret.append("load KeyStore1 " + keyStorePath + " fails");
            return;
        } catch (CertificateException e1) {
            ret.append("load KeyStore2 " + keyStorePath + " fails");
            return;
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            ret.append("load KeyStore3 " + keyStorePath + " fails");
            return;
        } catch (IOException e1) {
            ret.append("load KeyStore4 " + keyStorePath + " fails");
            return;
        }

        cb.trustStore(ts);
        cb.keyStore(ts, "passw0rd");

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");

        try {
            WebTarget wt = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource");
            wt = wt.path("echo").path(param.get("param"));
            Builder builder = wt.request();
            CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
            CompletionStage<String> completionStage = completionStageRxInvoker.get(String.class);
            CompletableFuture<String> completableFuture = completionStage.toCompletableFuture();
            res = completableFuture.get();
        } catch (Exception e) {
            res = e.getMessage();
        } finally {
            c.close();
            ret.append(res);
        }

    }

    public void testClientBasicSSL_InvalidSSLRef(Map<String, String> param, StringBuilder ret) {
        String res = "";

        String serverIP = param.get("hostname");
        String serverPort = param.get("secport");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testClientBasicSSL_InvalidSSLRef: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();

        try {
            WebTarget wt = c.target("https://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource");
            wt.property("com.ibm.ws.jaxrs.client.ssl.config", "invalidSSLConfig");
            wt = wt.path("echo").path(param.get("param"));
            Builder builder = wt.request();
            CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
            CompletionStage<String> completionStage = completionStageRxInvoker.get(String.class);
            CompletableFuture<String> completableFuture = completionStage.toCompletableFuture();
            res = completableFuture.get();
        } catch (Exception e) {
            res = e.getMessage();
        } finally {
            c.close();
            ret.append(res);
        }

    }
}
