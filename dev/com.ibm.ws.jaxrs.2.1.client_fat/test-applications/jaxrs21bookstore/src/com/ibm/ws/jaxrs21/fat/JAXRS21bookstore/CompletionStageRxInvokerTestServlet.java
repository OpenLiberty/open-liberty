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
package com.ibm.ws.jaxrs21.fat.JAXRS21bookstore;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 *
 */
@WebServlet("/CompletionStageRxInvokerTestServlet")
public class CompletionStageRxInvokerTestServlet extends HttpServlet {

    private static final long serialVersionUID = 2880606295862546001L;
    private static final long TIMEOUT = 5000;
    private static final long SLEEP = 20000;
    private static final long clientBuilderTimeout = 15000;

    private static final boolean isZOS() {
        String osName = System.getProperty("os.name");
        if (osName.contains("OS/390") || osName.contains("z/OS") || osName.contains("zOS")) {
            return true;
        }
        return false;
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

    public void testCompletionStageRxInvoker_get1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget1");
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        try {
            Response response = completableFuture.get();
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_get2WithClass(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get2WithClass: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget2");
        Builder builder = t.request();
        builder.accept("application/xml");
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<JAXRS21Book> completionStage = completionStageRxInvoker.get(JAXRS21Book.class);
        CompletableFuture<JAXRS21Book> completableFuture = completionStage.toCompletableFuture();
        try {
            JAXRS21Book response = completableFuture.get();
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_get3WithGenericType(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get3WithGenericType: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget3");
        Builder builder = t.request();
        builder.accept("application/xml");
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        GenericType<List<JAXRS21Book>> genericResponseType = new GenericType<List<JAXRS21Book>>() {
        };
        CompletionStage<List<JAXRS21Book>> completionStage = completionStageRxInvoker.get(genericResponseType);
        CompletableFuture<List<JAXRS21Book>> completableFuture = completionStage.toCompletableFuture();
        try {
            List<JAXRS21Book> response = completableFuture.get();
            ret.append(response != null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_get4WithExecutorService(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get4WithExecutorService: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget1");
        Builder builder = t.request();
        builder.accept("application/xml");
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        try {
            Response response = completableFuture.get();
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_get5WithThenCombine(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get5WithThenCombine1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget1");
        Builder builder1 = t1.request();
        CompletionStageRxInvoker completionStageRxInvoker1 = builder1.rx();
        CompletionStage<Response> completionStage1 = completionStageRxInvoker1.get();
        CompletableFuture<Response> completableFuture1 = completionStage1.toCompletableFuture();

        try {
            Response response1 = completableFuture1.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get5WithThenCombine2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget2");
        Builder builder2 = t2.request();
        builder2.accept("application/xml");
        CompletionStageRxInvoker completionStageRxInvoker2 = builder2.rx();
        CompletionStage<JAXRS21Book> completionStage2 = completionStageRxInvoker2.get(JAXRS21Book.class);
        CompletableFuture<JAXRS21Book> completableFuture2 = completionStage2.toCompletableFuture();
        String book2 = null;

        try {
            JAXRS21Book response2 = completableFuture2.get();
            book2 = response2.getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        CompletionStage<JAXRS21Book> completionStage = completionStage1.thenCombine(completionStage2, this::returnBook);
        CompletableFuture<JAXRS21Book> completableFuture = completionStage.toCompletableFuture();

        try {
            JAXRS21Book response = completableFuture.get();
            if (!(response.getName().equals(book2))) {
                throw new RuntimeException("testCompletionStageRxInvoker_get5WithThenCombine: incorrect book name");
            }
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c1.close();
        c2.close();
    }

    public void testCompletionStageRxInvoker_post1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get4WithExecutorService: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost1");
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        JAXRS21Book book = new JAXRS21Book("Test book", 100);
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(book));
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        try {
            Response response = completableFuture.get();
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_post2WithClass(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get4WithExecutorService: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost2");
        Builder builder = t.request();
        builder.accept("application/xml");
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        JAXRS21Book book = new JAXRS21Book("Test book2", 101);
        CompletionStage<JAXRS21Book> completionStage = completionStageRxInvoker.post(Entity.xml(book), JAXRS21Book.class);
        CompletableFuture<JAXRS21Book> completableFuture = completionStage.toCompletableFuture();
        try {
            JAXRS21Book response = completableFuture.get();
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_post3WithGenericType(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_get4WithExecutorService: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost3");
        Builder builder = t.request();
        builder.accept("application/xml");
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        GenericType<List<JAXRS21Book>> genericResponseType = new GenericType<List<JAXRS21Book>>() {
        };
        JAXRS21Book book = new JAXRS21Book("Test book3", 102);
        CompletionStage<List<JAXRS21Book>> completionStage = completionStageRxInvoker.post(Entity.xml(book), genericResponseType);
        CompletableFuture<List<JAXRS21Book>> completableFuture = completionStage.toCompletableFuture();
        try {
            List<JAXRS21Book> response = completableFuture.get();
            ret.append(response.get(response.size() - 1).getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_post4WithExecutorService(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_post4WithExecutorService: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost1");
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        JAXRS21Book book = new JAXRS21Book("Test book4", 103);
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(book));
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        try {
            Response response = completableFuture.get();
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_post5WithThenCombine(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_post5WithThenCombine1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost1");
        Builder builder1 = t1.request();
        CompletionStageRxInvoker completionStageRxInvoker1 = builder1.rx();
        JAXRS21Book book1 = new JAXRS21Book("Test book5", 104);
        CompletionStage<Response> completionStage1 = completionStageRxInvoker1.post(Entity.xml(book1));
        CompletableFuture<Response> completableFuture1 = completionStage1.toCompletableFuture();

        try {
            Response response1 = completableFuture1.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testCompletionStageRxInvoker_post5WithThenCombine2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost2");
        Builder builder2 = t2.request();
        builder2.accept("application/xml");
        CompletionStageRxInvoker completionStageRxInvoker2 = builder2.rx();
        JAXRS21Book book2 = new JAXRS21Book("Test book6", 105);
        CompletionStage<JAXRS21Book> completionStage2 = completionStageRxInvoker2.post(Entity.xml(book2), JAXRS21Book.class);
        CompletableFuture<JAXRS21Book> completableFuture2 = completionStage2.toCompletableFuture();

        try {
            JAXRS21Book response2 = completableFuture2.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        CompletionStage<JAXRS21Book> completionStage = completionStage1.thenCombine(completionStage2, this::returnBook);
        CompletableFuture<JAXRS21Book> completableFuture = completionStage.toCompletableFuture();
        try {
            JAXRS21Book response = completableFuture.get();
            if (!(response.getName().equals(book2.getName()))) {
                throw new RuntimeException("testCompletionStageRxInvoker_get5WithThenCombine: incorrect book name");
            }
            ret.append(response.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        c1.close();
        c2.close();
    }

    public void testCompletionStageRxInvoker_getCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/" + SLEEP);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_getCbReceiveTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get elapsed time " + elapsed);

        c.close();
    }

    public void testCompletionStageRxInvoker_getIbmReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/" + SLEEP);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_getIbmReceiveTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get elapsed time " + elapsed);

        c.close();
    }

    public void testCompletionStageRxInvoker_getIbmOverridesCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(clientBuilderTimeout, TimeUnit.MILLISECONDS);
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/" + SLEEP);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_getIbmOverridesCbReceiveTimeout with TIMEOUT " + TIMEOUT + " and clientBuilderTimeout " + clientBuilderTimeout + " completableFuture.get elapsed time " + elapsed);

        if (elapsed >= clientBuilderTimeout)  {
            ret.setLength(0);
            ret.append("Failure used clientBuilderTimeout ").append(clientBuilderTimeout).append(" instead of IBM timeout ").append(TIMEOUT).append(" as the elapsed time was  ").append(elapsed);
            System.out.println("testCompletionStageRxInvoker_getIbmOverridesCbReceiveTimeout " + ret);
        }
        c.close();
    }

    public void testCompletionStageRxInvoker_getCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
        } else {
            //Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a timeout
            target = "http://localhost:23/blah";
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target(target);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        long startTime = System.currentTimeMillis();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_getCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completionStageRxInvoker.get elapsed time " + elapsed);
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime2 = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testCompletionStageRxInvoker_getCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get() elapsed2 time " + elapsed2);

        c.close();
    }

    public void testCompletionStageRxInvoker_getIbmConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
        } else {
            //Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a timeout
            target = "http://localhost:23/blah";
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target(target);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        long startTime = System.currentTimeMillis();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_getIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " completionStageRxInvoker.get elapsed time " + elapsed);
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime2 = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testCompletionStageRxInvoker_getIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get() elapsed2 time " + elapsed2);

        c.close();
    }

    public void testCompletionStageRxInvoker_getIbmOverridesCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
        } else {
            //Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a timeout
            target = "http://localhost:23/blah";
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(clientBuilderTimeout, TimeUnit.MILLISECONDS);
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target(target);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        long startTime = System.currentTimeMillis();
        CompletionStage<Response> completionStage = completionStageRxInvoker.get();
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_getIbmOverridesCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completionStageRxInvoker.get elapsed time " + elapsed);
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime2 = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testCompletionStageRxInvoker_getIbmOverridesCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get() elapsed2 time " + elapsed2);

        if (elapsed > clientBuilderTimeout ) {
            ret.setLength(0);
            ret.append("Failure used clientBuilderTimeout ").append(clientBuilderTimeout).append(" instead of IBM timeout ").append(TIMEOUT).append(" as the elapsed time was  ").append(elapsed);
            System.out.println("testCompletionStageRxInvoker_getIbmOverridesCbConnectionTimeout " + ret);
        }

        c.close();
    }


    public void testCompletionStageRxInvoker_postCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/post/" + SLEEP);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_postCbReceiveTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get elapsed time " + elapsed);

        c.close();
    }

    public void testCompletionStageRxInvoker_postIbmReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/post/" + SLEEP);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_postIbmReceiveTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get elapsed time " + elapsed);

        c.close();
    }

    public void testCompletionStageRxInvoker_postIbmOverridesCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(clientBuilderTimeout, TimeUnit.MILLISECONDS);
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/post/" + SLEEP);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
                e.printStackTrace();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_postIbmOverridesCbReceiveTimeout with TIMEOUT " + TIMEOUT + " and clientBuilderTimeout " + clientBuilderTimeout + " completableFuture.get elapsed time " + elapsed);

        if (elapsed >= clientBuilderTimeout)  {
            ret.setLength(0);
            ret.append("Failure used clientBuilderTimeout ").append(clientBuilderTimeout).append(" instead of IBM timeout ").append(TIMEOUT).append(" as the elapsed time was  ").append(elapsed);
            System.out.println("testCompletionStageRxInvoker_postIbmOverridesCbReceiveTimeout " + ret);
        }

        c.close();
    }

    public void testCompletionStageRxInvoker_postCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
        } else {
            //Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a timeout
            target = "http://localhost:23/blah";
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target(target);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        long startTime = System.currentTimeMillis();
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_postCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completionStageRxInvoker.post elapsed time " + elapsed);
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime2 = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
            }
            e.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testCompletionStageRxInvoker_postCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get elapsed2 time " + elapsed2);

        c.close();
    }

    public void testCompletionStageRxInvoker_postIbmConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
        } else {
            //Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a timeout
            target = "http://localhost:23/blah";
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target(target);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        long startTime = System.currentTimeMillis();
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_postIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " completionStageRxInvoker.post elapsed time " + elapsed);
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime2 = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
            }
            e.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testCompletionStageRxInvoker_postIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get elapsed2 time " + elapsed2);

        c.close();
    }

    public void testCompletionStageRxInvoker_postIbmOverridesCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
        } else {
            //Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a timeout
            target = "http://localhost:23/blah";
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        WebTarget t = c.target(target);
        Builder builder = t.request();
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();
        long startTime = System.currentTimeMillis();
        CompletionStage<Response> completionStage = completionStageRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testCompletionStageRxInvoker_postIbmOverridesCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completionStageRxInvoker.post elapsed time " + elapsed);
        CompletableFuture<Response> completableFuture = completionStage.toCompletableFuture();
        long startTime2 = System.currentTimeMillis();

        try {
            Response response = completableFuture.get();
            // Did not time out as expected
            ret.append(response.readEntity(String.class));
        } catch (InterruptedException e) {
            ret.append("InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().toString().contains("ProcessingException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("ExecutionException");
            }
            e.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testCompletionStageRxInvoker_postIbmOverridesCbConnectionTimeout with TIMEOUT " + TIMEOUT + " completableFuture.get elapsed2 time " + elapsed2);

        if (elapsed > clientBuilderTimeout ) {
            ret.setLength(0);
            ret.append("Failure used clientBuilderTimeout ").append(clientBuilderTimeout).append(" instead of IBM timeout ").append(TIMEOUT).append(" as the elapsed time was  ").append(elapsed);
            System.out.println("testCompletionStageRxInvoker_postIbmOverridesCbConnectionTimeout " + ret);
        }

        c.close();
    }

    private JAXRS21Book returnBook(Response response, JAXRS21Book book) {
        // System.out.println("returnBook: " + book.getName());
        return book;
    }
}
