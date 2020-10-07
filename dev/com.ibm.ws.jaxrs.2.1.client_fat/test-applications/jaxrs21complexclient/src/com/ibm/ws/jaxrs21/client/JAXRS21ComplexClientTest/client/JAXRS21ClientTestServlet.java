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
package com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
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
import javax.ws.rs.core.Response;

/**
 *
 */
@WebServlet("/JAXRS21ClientTestServlet")
public class JAXRS21ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String moduleName = "jaxrs21complexclient";

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
            e.printStackTrace(); // print to server logs
            e.printStackTrace(pw); // print back in the response to the web client
        }
    }

    /**
     * Test 14 Client Filter Test
     *
     * @param param
     * @param ret
     */
    public void testNew2WebTargetsRequestFilterForRx1(Map<String, String> param, StringBuilder ret) {
        final String serverIP = param.get("serverIP");
        final String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        final Client c = cb.build();

        ExecutorService executor1 = Executors.newSingleThreadExecutor();
        Callable<String> callable1 = new Callable<String>() {
            @Override
            public String call() {

                WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource").register(JAXRS21ClientRequestFilter1.class);
                t1.path("echo1").path("test1").request().get(String.class);
                String result1 = c.getConfiguration().getProperties().toString();
                System.out.println("callable1: result1: " + result1);

                return result1;
            }
        };

        String result1 = null;
        try {
            result1 = executor1.submit(callable1).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        ExecutorService executor2 = Executors.newSingleThreadExecutor();
        Callable<String> callable2 = new Callable<String>() {
            @Override
            public String call() {

                WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource").register(JAXRS21ClientRequestFilter2.class);
                t2.path("echo2").path("test2").request().get(String.class);
                String result2 = c.getConfiguration().getProperties().toString();
                System.out.println("callable2: result2: " + result2);

                return result2;
            }
        };

        String result2 = null;
        try {
            result2 = executor2.submit(callable2).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        c.close();
        ret.append(result1 + "," + result2);
        System.out.println("ret: " + ret);
    }

    public void testNew2WebTargetsRequestFilterForRx2(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testNew2WebTargetsRequestFilterForRx2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);

        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource").register(JAXRS21ClientRequestFilter1.class);
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource").register(JAXRS21ClientRequestFilter2.class);

        CompletableFuture<String> completableFuture1 = t1.path("echo1").path("test1").request().rx().get(String.class).toCompletableFuture();

        try {
            completableFuture1.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        String result1 = removeExecutorServicefromMap(new HashMap<String, Object>(c.getConfiguration().getProperties()));

        CompletableFuture<String> completableFuture2 = t2.path("echo2").path("test2").request().rx().get(String.class).toCompletableFuture();
        try {
            completableFuture2.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        String result2 = removeExecutorServicefromMap(new HashMap<String, Object>(c.getConfiguration().getProperties()));
        c.close();
        ret.append(result1 + "," + result2);
    }

    public void testNew2ResponseFilterForRx(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testNew2ResponseFilterForRx1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        c1.register(JAXRS21ClientResponseFilter1.class, 200);
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource");

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testNew2ResponseFilterForRx2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource").register(JAXRS21ClientResponseFilter2.class, 100);

        t1 = t1.path("echo1").path("test1");
        Builder builder1 = t1.request();
        CompletionStageRxInvoker completionStageRxInvoker1 = builder1.rx();
        CompletionStage<Response> completionStage1 = completionStageRxInvoker1.get(Response.class);
        CompletableFuture<Response> completableFuture1 = completionStage1.toCompletableFuture();
        Response response1 = null;

        try {
            response1 = completableFuture1.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        t2 = t2.path("echo2").path("test2");
        Builder builder2 = t2.request();
        CompletionStageRxInvoker completionStageRxInvoker2 = builder2.rx();
        CompletionStage<Response> completionStage2 = completionStageRxInvoker2.get(Response.class);
        CompletableFuture<Response> completableFuture2 = completionStage2.toCompletableFuture();
        Response response2 = null;

        try {
            response2 = completableFuture2.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("config: " + c2.getConfiguration().getProperties());
        c1.close();
        c2.close();
        ret.append(response1.getStatus() + "," + response2.getStatus());
    }

    public void testNew2MixFilterForRx(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testNew2MixFilterForRx1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        c1.register(JAXRS21ClientResponseFilter1.class);
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource").register(JAXRS21ClientRequestFilter1.class);
        t1 = t1.path("echo1").path("test1");
        Builder builder1 = t1.request();
        CompletionStageRxInvoker completionStageRxInvoker1 = builder1.rx();
        CompletionStage<Response> completionStage1 = completionStageRxInvoker1.get(Response.class);
        CompletableFuture<Response> completableFuture1 = completionStage1.toCompletableFuture();
        Response response1 = null;

        try {
            response1 = completableFuture1.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        String result1 = removeExecutorServicefromMap(new HashMap<String, Object>(c1.getConfiguration().getProperties()));

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testNew2MixFilterForRx2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/JAXRS21ComplexClientTest/JAXRS21ComplexResource").register(JAXRS21ClientResponseFilter2.class)
                .register(JAXRS21ClientRequestFilter2.class);
        t2 = t2.path("echo2").path("test2");
        Builder builder2 = t2.request();
        CompletionStageRxInvoker completionStageRxInvoker2 = builder2.rx();
        CompletionStage<Response> completionStage2 = completionStageRxInvoker2.get(Response.class);
        CompletableFuture<Response> completableFuture2 = completionStage2.toCompletableFuture();
        Response response2 = null;

        try {
            response2 = completableFuture2.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        String result2 = removeExecutorServicefromMap(new HashMap<String, Object>(c2.getConfiguration().getProperties()));

        c1.close();
        c2.close();
        ret.append(response1.getStatus() + "," + result1 + "," + response2.getStatus() + "," + result2);
    }

    /**
     * Test: Test the new Reactive client by simply changing the use of async to rx
     *
     * Expected Results: Exception is thrown
     */

    public void testThrowsExceptionForRx(Map<String, String> param, StringBuilder ret) {

        String url = "http://justforcts.test:6789/resource/delete";
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        CompletionStageRxInvoker completionStageRxInvoker = target.request().rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.delete();
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        try {
            completableFuture.get();
            ret.append(false + "");
            throw new Exception("ExecutionException has not been thrown");
        } catch (ExecutionException e) {
            ret.append(true + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String removeExecutorServicefromMap(Map<String, Object> properties) {

        // System.out.println("removeExecutorServicefromMap: Entry: properties: " + properties.toString());

        Iterator<String> iterator = properties.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.contains("executorService")) {
                iterator.remove();
            }
        }

        // System.out.println("removeExecutorServicefromMap: Exit: properties: " + properties.toString());

        return properties.toString();
    }

    private Response returnResponse(Response response1, Response response2) {

        return response1;
    }

}
