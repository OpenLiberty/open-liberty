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
import java.util.concurrent.CountDownLatch;
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
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.rx2.client.FlowableRxInvoker;
import org.apache.cxf.jaxrs.rx2.client.FlowableRxInvokerProvider;
import org.apache.cxf.jaxrs.rx2.client.ObservableRxInvoker;
import org.apache.cxf.jaxrs.rx2.client.ObservableRxInvokerProvider;

import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 *
 */
@WebServlet("/CXFRxInvokerTestServlet")
public class CXFRxInvokerTestServlet extends HttpServlet {

    private static final long serialVersionUID = 2880606295862546001L;
    private static final long TIMEOUT = 5000;
    private static final long SLEEP = 20000;
    private static final int basicTimeout = 30;
    private static final int complexTimeout = 35;
    private static final int messageTimeout = 70;
    private static final int zTimeout = 70;

    private static final boolean isZOS() {
        String osName = System.getProperty("os.name");
        if (osName.contains("OS/390") || osName.contains("z/OS") || osName.contains("zOS")) {
            return true;
        }
        return false;
    }

    private static final boolean isRestful30() {
        try {
            jakarta.ws.rs.core.Application app = new jakarta.ws.rs.core.Application();
            return true;
        } catch (NoClassDefFoundError ignore) {
            return false;
        }
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

    public void testObservableRxInvoker_get1(Map<String, String> param, StringBuilder ret) {

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testObservableRxInvoker_get1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget1");
        Builder builder = t.request();
        Observable<Response> observable = builder.rx(ObservableRxInvoker.class).get(Response.class);

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_get1: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(complexTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_get1: Response took too long. Waited " + complexTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.readEntity(String.class));
        c.close();
    }

    public void testFlowableRxInvoker_get1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testFlowableRxInvoker_get1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget1");
        Builder builder = t.request();
        Flowable<Response> flowable = builder.rx(FlowableRxInvoker.class).get(Response.class);

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_get1: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_get1: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.readEntity(String.class));
        c.close();
    }

    public void testObservableRxInvoker_get2WithClass(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testObservableRxInvoker_get2WithClass: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget2");
        Builder builder = t.request();
        builder.accept("application/xml");
        Observable<JAXRS21Book> observable = builder.rx(ObservableRxInvoker.class).get(JAXRS21Book.class);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_get2WithClass: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_get2WithClass: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c.close();
    }

    public void testFlowableRxInvoker_get2WithClass(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testFlowableRxInvoker_get2WithClass: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget2");
        Builder builder = t.request();
        builder.accept("application/xml");
        Flowable<JAXRS21Book> flowable = builder.rx(FlowableRxInvoker.class).get(JAXRS21Book.class);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_get2WithClass: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_get2WithClass: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c.close();
    }

    public void testObservableRxInvoker_get3WithGenericType(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testObservableRxInvoker_get3WithGenericType: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget3");
        Builder builder = t.request();
        builder.accept("application/xml");
        GenericType<List<JAXRS21Book>> genericResponseType = new GenericType<List<JAXRS21Book>>() {
        };
        Observable<List<JAXRS21Book>> observable = builder.rx(ObservableRxInvoker.class).get(genericResponseType);

        final Holder<List<JAXRS21Book>> holder = new Holder<List<JAXRS21Book>>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_get3WithGenericType: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_get3WithGenericType: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        List<JAXRS21Book> response = holder.value;
        ret.append(response != null);
        c.close();
    }

    public void testFlowableRxInvoker_get3WithGenericType(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testFlowableRxInvoker_get3WithGenericType: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget3");
        Builder builder = t.request();
        builder.accept("application/xml");
        GenericType<List<JAXRS21Book>> genericResponseType = new GenericType<List<JAXRS21Book>>() {
        };
        Flowable<List<JAXRS21Book>> flowable = builder.rx(FlowableRxInvoker.class).get(genericResponseType);

        final Holder<List<JAXRS21Book>> holder = new Holder<List<JAXRS21Book>>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_get3WithGenericType: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_get3WithGenericType: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        List<JAXRS21Book> response = holder.value;
        ret.append(response != null);
        c.close();
    }

    public void testObservableRxInvoker_get5WithZip(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testObservableRxInvoker_get5WithZip1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        c1.register(ObservableRxInvokerProvider.class);
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget1");
        Builder builder1 = t1.request();
        Observable<Response> observable1 = builder1.rx(ObservableRxInvoker.class).get(Response.class);

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testObservableRxInvoker_get5WithZip2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        c2.register(ObservableRxInvokerProvider.class);
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget2");
        Builder builder2 = t2.request();
        builder2.accept("application/xml");
        Observable<JAXRS21Book> observable2 = builder2.rx(ObservableRxInvoker.class).get(JAXRS21Book.class);

        // Use .zip here to execute returnBook after observable1 and observable2 are complete
        Observable<JAXRS21Book> observableZip = Observable.zip(observable1, observable2, this::returnBook);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observableZip.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_get5WithZip: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(complexTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_get5WithZip: Response took too long. Waited " + complexTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c1.close();
        c2.close();
    }

    public void testFlowableRxInvoker_get5WithZip(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testFlowableRxInvoker_get5WithZip1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        c1.register(FlowableRxInvokerProvider.class);
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget1");
        Builder builder1 = t1.request();

        Flowable<Response> flowable1 = builder1.rx(FlowableRxInvoker.class).get(Response.class);

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testFlowableRxInvoker_get5WithZip2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        c2.register(FlowableRxInvokerProvider.class);
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxget2");
        Builder builder2 = t2.request();
        builder2.accept("application/xml");
        Flowable<JAXRS21Book> flowable2 = builder2.rx(FlowableRxInvoker.class).get(JAXRS21Book.class);

        // Use .zip here to execute returnBook after flowable1 and flowable2 are complete
        Flowable<JAXRS21Book> observableZip = Flowable.zip(flowable1, flowable2, this::returnBook);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observableZip.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_get5WithZip: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(complexTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_get5WithZip: Response took too long. Waited " + complexTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c1.close();
        c2.close();
    }

    public void testObservableRxInvoker_post1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testObservableRxInvoker_post1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost1");
        Builder builder = t.request();
        JAXRS21Book book = new JAXRS21Book("Test book", 100);
        Observable<Response> observable = builder.rx(ObservableRxInvoker.class).post(Entity.xml(book));

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_post1: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_post1: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.readEntity(String.class));
        c.close();
    }

    public void testFlowableRxInvoker_post1(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testFlowableRxInvoker_post1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost1");
        Builder builder = t.request();
        JAXRS21Book book = new JAXRS21Book("Test book", 100);
        Flowable<Response> flowable = builder.rx(FlowableRxInvoker.class).post(Entity.xml(book));

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_post1: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_post1: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.readEntity(String.class));
        c.close();
    }

    public void testObservableRxInvoker_post2WithClass(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testObservableRxInvoker_post2WithClass: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost2");
        Builder builder = t.request();
        builder.accept("application/xml");
        JAXRS21Book book = new JAXRS21Book("Test book2", 101);
        Observable<JAXRS21Book> observable = builder.rx(ObservableRxInvoker.class).post(Entity.xml(book), JAXRS21Book.class);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_post2WithClass: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_post2WithClass: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c.close();
    }

    public void testFlowableRxInvoker_post2WithClass(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testFlowableRxInvoker_post2WithClass: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost2");
        Builder builder = t.request();
        builder.accept("application/xml");
        JAXRS21Book book = new JAXRS21Book("Test book2", 101);
        Flowable<JAXRS21Book> flowable = builder.rx(FlowableRxInvoker.class).post(Entity.xml(book), JAXRS21Book.class);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_post2WithClass: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_post2WithClass: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c.close();
    }

    public void testObservableRxInvoker_post3WithGenericType(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testObservableRxInvoker_post3WithGenericType: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost3");
        Builder builder = t.request();
        builder.accept("application/xml");
        GenericType<List<JAXRS21Book>> genericResponseType = new GenericType<List<JAXRS21Book>>() {
        };
        JAXRS21Book book = new JAXRS21Book("Test book3", 102);
        Observable<List<JAXRS21Book>> observable = builder.rx(ObservableRxInvoker.class).post(Entity.xml(book), genericResponseType);

        final Holder<List<JAXRS21Book>> holder = new Holder<List<JAXRS21Book>>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_post3WithGenericType: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_post3WithGenericType: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        List<JAXRS21Book> response = holder.value;
        ret.append(response.get(response.size() - 1).getName());
        c.close();
    }

    public void testFlowableRxInvoker_post3WithGenericType(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String threadName = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory = Executors.defaultThreadFactory();

        Thread jaxrs21Thread = jaxrs21ThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName))) {
                    throw new RuntimeException("testFlowableRxInvoker_post3WithGenericType: incorrect thread name");
                }
            }
        });

        jaxrs21Thread.setName(threadName);
        ExecutorService executorService = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory);
        ClientBuilder cb = ClientBuilder.newBuilder().executorService(executorService);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost3");
        Builder builder = t.request();
        builder.accept("application/xml");
        GenericType<List<JAXRS21Book>> genericResponseType = new GenericType<List<JAXRS21Book>>() {
        };
        JAXRS21Book book = new JAXRS21Book("Test book3", 102);
        Flowable<List<JAXRS21Book>> flowable = builder.rx(FlowableRxInvoker.class).post(Entity.xml(book), genericResponseType);

        final Holder<List<JAXRS21Book>> holder = new Holder<List<JAXRS21Book>>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_post3WithGenericType: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(basicTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_post3WithGenericType: Response took too long. Waited " + basicTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        List<JAXRS21Book> response = holder.value;
        ret.append(response.get(response.size() - 1).getName());
        c.close();
    }

    public void testObservableRxInvoker_post5WithZip(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testObservableRxInvoker_post5WithZip1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        c1.register(ObservableRxInvokerProvider.class);
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost1");
        Builder builder1 = t1.request();
        JAXRS21Book book1 = new JAXRS21Book("Test book5", 104);
        Observable<Response> observable1 = builder1.rx(ObservableRxInvoker.class).post(Entity.xml(book1));

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testObservableRxInvoker_post5WithZip2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        c2.register(ObservableRxInvokerProvider.class);
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost2");
        Builder builder2 = t2.request();
        builder2.accept("application/xml");
        JAXRS21Book book2 = new JAXRS21Book("Test book6", 105);
        Observable<JAXRS21Book> observable2 = builder2.rx(ObservableRxInvoker.class).post(Entity.xml(book2), JAXRS21Book.class);

        // Use .zip here to execute returnBook after observable1 and observable2 are complete
        Observable<JAXRS21Book> observableZip = Observable.zip(observable1, observable2, this::returnBook);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observableZip.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testObservableRxInvoker_post5WithZip: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(complexTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_post5WithZip: Response took too long. Waited " + complexTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c1.close();
        c2.close();
    }

    public void testFlowableRxInvoker_post5WithZip(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testFlowableRxInvoker_post5WithZip1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        Client c1 = cb1.build();
        c1.register(FlowableRxInvokerProvider.class);
        WebTarget t1 = c1.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost1");
        Builder builder1 = t1.request();
        JAXRS21Book book1 = new JAXRS21Book("Test book5", 104);
        Flowable<Response> flowable1 = builder1.rx(FlowableRxInvoker.class).post(Entity.xml(book1));

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testFlowableRxInvoker_post5WithZip2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        Client c2 = cb2.build();
        c2.register(FlowableRxInvokerProvider.class);
        WebTarget t2 = c2.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/rxpost2");
        Builder builder2 = t2.request();
        builder2.accept("application/xml");
        JAXRS21Book book2 = new JAXRS21Book("Test book6", 105);
        Flowable<JAXRS21Book> flowable2 = builder2.rx(FlowableRxInvoker.class).post(Entity.xml(book2), JAXRS21Book.class);

        // Use .zip here to execute returnBook after flowable1 and flowable2 are complete
        Flowable<JAXRS21Book> flowableZip = Flowable.zip(flowable1, flowable2, this::returnBook);

        final Holder<JAXRS21Book> holder = new Holder<JAXRS21Book>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowableZip.subscribe(v -> {
            holder.value = v; // OnNext
            countDownLatch.countDown();
        }, throwable -> {
            throw new RuntimeException("testFlowableRxInvoker_post5WithZip: onError " + throwable.getStackTrace()); // OnError
        });

        try {
            if (!(countDownLatch.await(complexTimeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_post5WithZip: Response took too long. Waited " + complexTimeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        ret.append(holder.value.getName());
        c1.close();
        c2.close();
    }

    public void testObservableRxInvokerOnError(Map<String, String> param, StringBuilder ret) {

        String url = "http://justforcts.test:6789/resource/delete";
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target(url);
        Builder builder = t.request();
        ObservableRxInvoker ObservableRxInvoker = builder.rx(ObservableRxInvoker.class);
        Observable<Response> observable = ObservableRxInvoker.delete();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            ret.append("true"); // OnError
            countDownLatch.countDown();
        }, () -> ret.append("false") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvokerOnError: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        c.close();
    }

    public void testFlowableRxInvokerOnError(Map<String, String> param, StringBuilder ret) {

        String url = "http://justforcts.test:6789/resource/delete";
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target(url);
        Builder builder = t.request();
        FlowableRxInvoker FlowableRxInvoker = builder.rx(FlowableRxInvoker.class);
        Flowable<Response> flowable = FlowableRxInvoker.delete();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            ret.append("true"); // OnError
            countDownLatch.countDown();
        }, () -> ret.append("false") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvokerOnError: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        c.close();
    }

    public void testObservableRxInvoker_getCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/" + SLEEP);
        Builder builder = t.request();
        Observable<Response> observable = builder.rx(ObservableRxInvoker.class).get(Response.class);
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_getCbReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_getCbReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testObservableRxInvoker_getIbmReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/" + SLEEP);
        Builder builder = t.request();
        Observable<Response> observable = builder.rx(ObservableRxInvoker.class).get(Response.class);
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_getIbmReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_getIbmReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testFlowableRxInvoker_getCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/" + SLEEP);
        Builder builder = t.request();
        Flowable<Response> flowable = builder.rx(FlowableRxInvoker.class).get(Response.class);
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_getCbReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_getCbReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testFlowableRxInvoker_getIbmReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/" + SLEEP);
        Builder builder = t.request();
        Flowable<Response> flowable = builder.rx(FlowableRxInvoker.class).get(Response.class);
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_getIbmReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_getIbmReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testObservableRxInvoker_getCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testObservableRxInvoker_getCbConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        ObservableRxInvoker observableRxInvoker = builder.rx(ObservableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Observable<Response> observable = observableRxInvoker.get();
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_getCbConnectionTimeout with TIMEOUT " + TIMEOUT + " observableRxInvoker.get elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_getCbConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testObservableRxInvoker_getCbConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    public void testObservableRxInvoker_getIbmConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testObservableRxInvoker_getIbmConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        ObservableRxInvoker observableRxInvoker = builder.rx(ObservableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Observable<Response> observable = observableRxInvoker.get();
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_getIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " observableRxInvoker.get elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_getIbmConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testObservableRxInvoker_getIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    public void testFlowableRxInvoker_getCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testFlowableRxInvoker_getCbConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        FlowableRxInvoker flowableRxInvoker = builder.rx(FlowableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Flowable<Response> flowable = flowableRxInvoker.get();
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_getCbConnectionTimeout with TIMEOUT " + TIMEOUT + " flowableRxInvoker.get elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_getCbConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testFlowableRxInvoker_getCbConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    public void testFlowableRxInvoker_getIbmConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testFlowableRxInvoker_getIbmConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        FlowableRxInvoker flowableRxInvoker = builder.rx(FlowableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Flowable<Response> flowable = flowableRxInvoker.get();
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_getIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " flowableRxInvoker.get elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_getIbmConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testFlowableRxInvoker_getIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    public void testObservableRxInvoker_postCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/post/" + SLEEP);
        Builder builder = t.request();
        Observable<Response> observable = builder.rx(ObservableRxInvoker.class).post(Entity.xml(Long.toString(SLEEP)));
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_postCbReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_postCbReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testObservableRxInvoker_postIbmReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/post/" + SLEEP);
        Builder builder = t.request();
        Observable<Response> observable = builder.rx(ObservableRxInvoker.class).post(Entity.xml(Long.toString(SLEEP)));
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_postIbmReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_postIbmReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testFlowableRxInvoker_postCbReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.readTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/post/" + SLEEP);
        Builder builder = t.request();
        Flowable<Response> flowable = builder.rx(FlowableRxInvoker.class).post(Entity.xml(Long.toString(SLEEP)));
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_postCbReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_postCbReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testFlowableRxInvoker_postIbmReceiveTimeout(Map<String, String> param, StringBuilder ret) {
        long timeout = messageTimeout;

        if (isZOS()) {
            timeout = zTimeout;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/jaxrs21bookstore/JAXRS21bookstore2/post/" + SLEEP);
        Builder builder = t.request();
        Flowable<Response> flowable = builder.rx(FlowableRxInvoker.class).post(Entity.xml(Long.toString(SLEEP)));
        long startTime = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getMessage().contains("SocketTimeoutException")) { // OnError
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_postIbmReceiveTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_postIbmReceiveTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed time " + elapsed);

        c.close();
    }

    public void testObservableRxInvoker_postCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testObservableRxInvoker_postCbConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        ObservableRxInvoker observableRxInvoker = builder.rx(ObservableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Observable<Response> observable = observableRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_postCbConnectionTimeout with TIMEOUT " + TIMEOUT + " observableRxInvoker.post elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_postCbConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testObservableRxInvoker_postCbConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    public void testObservableRxInvoker_postIbmConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testObservableRxInvoker_postIbmConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(ObservableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        ObservableRxInvoker observableRxInvoker = builder.rx(ObservableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Observable<Response> observable = observableRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testObservableRxInvoker_postIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " observableRxInvoker.post elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        observable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testObservableRxInvoker_postIbmConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testObservableRxInvoker_postIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    public void testFlowableRxInvoker_postCbConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testFlowableRxInvoker_postCbConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        FlowableRxInvoker flowableRxInvoker = builder.rx(FlowableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Flowable<Response> flowable = flowableRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_postCbConnectionTimeout with TIMEOUT " + TIMEOUT + " flowableRxInvoker.post elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_postCbConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testFlowableRxInvoker_postCbConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    public void testFlowableRxInvoker_postIbmConnectionTimeout(Map<String, String> param, StringBuilder ret) {
        String target = null;
        long timeout = messageTimeout;

        if (isZOS()) {
            // https://stackoverflow.com/a/904609/6575578
            target = "http://example.com:81";
            timeout = zTimeout;
        } else {
            // Connect to telnet port - which should be disabled on all non-Z test machines - so we should expect a
            // timeout
            target = "http://localhost:23/blah";
        }

        if (isRestful30()) {
            timeout = timeout * 2;
            System.out.println("testFlowableRxInvoker_postIbmConnectionTimeout with timeout " + timeout);
        }

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", TIMEOUT);
        Client c = cb.build();
        c.register(FlowableRxInvokerProvider.class);
        WebTarget t = c.target(target);
        Builder builder = t.request();
        FlowableRxInvoker flowableRxInvoker = builder.rx(FlowableRxInvoker.class);
        long startTime = System.currentTimeMillis();
        Flowable<Response> flowable = flowableRxInvoker.post(Entity.xml(Long.toString(SLEEP)));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("testFlowableRxInvoker_postIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " flowableRxInvoker.post elapsed time " + elapsed);
        long startTime2 = System.currentTimeMillis();

        final Holder<Response> holder = new Holder<Response>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        flowable.subscribe(v -> {
            holder.value = v; // OnNext
        }, throwable -> {
            if (throwable.getCause().toString().contains("ConnectException") || // OnError
            throwable.getCause().toString().contains("SocketTimeoutException")) {
                ret.append("Timeout as expected");
            } else {
                ret.append("throwable");
                throwable.printStackTrace();
            }
            countDownLatch.countDown();
        }, () -> ret.append("OnCompleted") // OnCompleted
        );

        try {
            if (!(countDownLatch.await(timeout, TimeUnit.SECONDS))) {
                throw new RuntimeException("testFlowableRxInvoker_postIbmConnectionTimeout: Response took too long. Waited " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        long elapsed2 = System.currentTimeMillis() - startTime2;
        System.out.println("testFlowableRxInvoker_postIbmConnectionTimeout with TIMEOUT " + TIMEOUT + " OnError elapsed2 time " + elapsed2);

        c.close();
    }

    private JAXRS21Book returnBook(Response response, JAXRS21Book book) {
        // System.out.println("returnBook: " + book.getName());
        return book;
    }

    private class Holder<T> {
        public volatile T value;
    }
}
