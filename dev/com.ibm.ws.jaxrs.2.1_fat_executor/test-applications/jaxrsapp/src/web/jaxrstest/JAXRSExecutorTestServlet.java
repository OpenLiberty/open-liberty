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
package web.jaxrstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JAXRSExecutorTestServlet")
public class JAXRSExecutorTestServlet extends FATServlet {
    private static final Logger _log = Logger.getLogger(JAXRSExecutorTestServlet.class.getName());
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    @Resource(name = "java:comp/env/concurrent/scheduledExecutorRef", lookup = "concurrent/scheduledExecutor")
    private ManagedScheduledExecutorService scheduledExecutor;

    // Use JAX-RS client submit with an invocation callback, where the client builder is supplied with
    // a ManagedExecutorService. Verify that the callback runs with access to the java:comp namespace
    // of the servlet because it is running on a ManagedExecutorService thread.
    @Test
    public void testClientBuilderSubmitInvocationCallbackViaManagedExecutor(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String m = "testClientBuilderSubmitInvocationCallbackViaManagedExecutor";
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";

        // Prove that the resource is available to the servlet thread,
        Object scheduledExecutorRef = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
        assertNotNull(scheduledExecutorRef);
        _log.info(m + " scheduledExecutorRef=" + scheduledExecutorRef);
        _log.info(m + " scheduledExecutor=" + scheduledExecutor);

        ClientBuilder clientBuilder = ClientBuilder.newBuilder().connectTimeout(1, TimeUnit.HOURS).readTimeout(1, TimeUnit.HOURS);
        Client client = clientBuilder.executorService(scheduledExecutor).build();

        final ConcurrentLinkedQueue<Object> result = new ConcurrentLinkedQueue<Object>();
        Future<String> f = client.target(uri).request("text/plain").buildGet().submit(new InvocationCallback<String>() {
            @Override
            public void completed(String response) {
                _log.info(m + " Response from inside InvocationCallback: " + response);
                result.add(response);
                String threadName = Thread.currentThread().getName();
                _log.info(m + " Thread name inside InvocationCallback: " + threadName);
                result.add(threadName);
                try {
                    result.add(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));
                } catch (NamingException x) {
                    result.add(x);
                }
            }

            @Override
            public void failed(Throwable x) {
                result.add(x);
            }
        });
        assertEquals("test123", f.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        client.close();

        Object r = result.poll();
        assertNotNull(r);
        if (r instanceof Throwable)
            throw new Exception("InvocationCallback received failure. See cause.", (Throwable) r);
        assertEquals("test123", r);

        assertNotNull(r = result.poll());
        String callbackThreadName = (String) r;
        assertTrue(callbackThreadName, callbackThreadName.startsWith("Default Executor-thread-"));

        assertNotNull(r = result.poll());
        if (r instanceof ScheduledExecutorService)
            ; // test passes, servlet's java:comp should be available to callback thread
        else if (r instanceof Throwable)
            throw new Exception("Unexpected failure for InvocationCallback lookup attempt. See cause.", (Throwable) r);
    }

    // Use JAX-RS client rx invoker, where the client builder does not specify an executor and
    // defaults to the Liberty global thread pool.  Verify that the CompletionStage function
    // which runs asynchronously after completion, runs on the Liberty global thread pool.
    @Test
    public void testCompletionStageRxInvokerAsynchronousFunction(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/post";

        assertNotNull(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.build();
        final Object[] results = new Object[3];
        CompletionStage<?> stage = client.target(uri).request("text/plain").rx().post(Entity.text(123456), String.class).thenAcceptAsync(o -> {
            String threadName = Thread.currentThread().getName();
            Object lookupResult;
            try {
                lookupResult = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
            } catch (NamingException x) {
                lookupResult = x;
            }
            results[0] = o;
            results[1] = threadName;
            results[2] = lookupResult;
        });

        assertNull(stage.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        String resultString = Arrays.toString(results);

        assertEquals("test456", results[0]);

        String executionThreadName = (String) results[1];
        // On JDK 8, it will not use the Liberty executor now.  Depending on ForkJoin pool
        // parallelism settings it could be the ForkJoin pool or it could spawn a Thread.
        if (System.getProperty("java.specification.version").startsWith("1.")) {
            assertFalse(resultString, executionThreadName.startsWith("Default Executor-thread-"));
        } else {
            assertTrue(resultString, executionThreadName.startsWith("Default Executor-thread-"));
        }
        assertTrue(resultString, results[2] instanceof NamingException);
    }

    // Use JAX-RS client rx invoker, where the client builder is supplied with one ManagedExecutorService,
    // but additional completion stages are added with a different ManagedExecutorService instance.
    // Verify that thread context is propagated between the executors by verifying that the async operations
    // have access to the java:comp namespace of the servlet.
    @Test
    public void testCompletionStageRxInvokerSynchronousFunctionSwitchManagedExecutors(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String m = "testCompletionStageRxInvokerSynchronousFunctionSwitchManagedExecutors";
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";

        // Confirm the lookup works from current thread
        assertNotNull(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));

        final LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        BiFunction<String, Throwable, String> function = (result, error) -> {
            if (result != null)
                results.add(result);
            if (error != null)
                results.add(error);
            String threadName = Thread.currentThread().getName();
            results.add(threadName);
            _log.info(m + " currentThread=" + threadName);
            new Exception("handleAsync is running on " + threadName).printStackTrace(System.out);
            try {
                results.add(InitialContext.doLookup("java:comp/env/concurrent/scheduledExecutorRef"));
            } catch (NamingException x) {
                results.add(x);
            }
            return result;
        };

        ExecutorService executor = InitialContext.doLookup("java:comp/DefaultManagedExecutorService");

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.executorService(executor).build();
        client.target(uri)
                        .request("text/plain")
                        .rx()
                        .method(HttpMethod.GET, String.class)
                        .handleAsync(function, scheduledExecutor)
                        .handleAsync(function, scheduledExecutor)
                        .handleAsync(function);

        Object result;
        assertEquals("test123", results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue((result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)).toString(), result.toString().startsWith("Default Executor-thread-"));
        assertTrue((result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)).toString(), result instanceof ScheduledExecutorService);
        assertEquals("test123", results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue((result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)).toString(), result.toString().startsWith("Default Executor-thread-"));
        assertTrue((result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)).toString(), result instanceof ScheduledExecutorService);
        assertEquals("test123", results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));        
        assertTrue((result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)).toString(), result.toString().startsWith("Default Executor-thread-"));
        assertTrue((result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS)).toString(), result instanceof ScheduledExecutorService);
    }

    // Use JAX-RS client rx invoker, where the client builder is supplied with a ManagedExecutorService.
    // Verify that the CompletionStage function which runs asynchronously after completion has access to
    // the java:comp namespace of the servlet because it is running on a ManagedExecutorService thread.
    @Test
    public void testCompletionStageRxInvokerAsynchronousFunctionViaManagedExecutor(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String m = "testCompletionStageRxInvokerAsynchronousFunctionViaManagedExecutor";
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/post";

        assertNotNull(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.executorService(scheduledExecutor).build();
        CompletionStage<?> stage = client.target(uri).request("text/plain").rx().post(Entity.text(112233), String.class).thenApplyAsync(o -> {
            String threadName = Thread.currentThread().getName();
            Object lookupResult;
            try {
                lookupResult = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
            } catch (NamingException x) {
                lookupResult = x;
            }
            return new Object[] { o, threadName, lookupResult };
        });

        Object[] results = (Object[]) stage.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        String resultString = Arrays.toString(results);
        _log.info(m + " resultString=" + resultString);

        assertEquals("test456", results[0]);

        String executionThreadName = (String) results[1];
        assertTrue(resultString, executionThreadName.startsWith("Default Executor-thread-")); 
        assertTrue(resultString, results[2] instanceof ScheduledExecutorService); 
    }

    // Use JAX-RS client rx invoker, where the client builder is not supplied with any ExecutorService.
    // Verify that the CompletionStage function which runs synchronously after completion lacks access to
    // the java:comp namespace of the servlet because it is running on a plain Liberty executor thread
    // (as opposed to a ManagedExecutorService thread).
    @Test
    public void testCompletionStageRxInvokerSynchronousFunction(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";

        final String submitterThreadName = Thread.currentThread().getName();
        assertNotNull(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.build();
        CompletionStage<?> stage = client.target(uri).request("text/plain").rx().get(String.class);
        // Thread.sleep(2000); // uncomment to reproduce behavior where .thenApply executes synchronously on submitter thread
        stage = stage.thenApply(o -> {
            String threadName = Thread.currentThread().getName();
            new Exception("thenApply is running on " + threadName).printStackTrace(System.out);
            Object lookupResult;
            try {
                lookupResult = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
            } catch (NamingException x) {
                lookupResult = x;
            }
            return new Object[] { o, threadName, lookupResult };
        });

        Object[] results = (Object[]) stage.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        String resultString = Arrays.toString(results);

        assertEquals("test123", results[0]);

        String executionThreadName = (String) results[1];
        assertTrue(resultString, executionThreadName.startsWith("Default Executor-thread-"));

        if (submitterThreadName.equals(executionThreadName)) {
            System.out.println("*** Unable to properly complete test because CompletionStage ran on submitter thread.");
            assertTrue(resultString, results[2] instanceof ScheduledExecutorService);
        } else
            assertTrue(resultString, results[2] instanceof NamingException);
    }

    // Use JAX-RS client rx invoker, where the client builder is supplied with a ManagedExecutorService.
    // Verify that the CompletionStage function which runs synchronously after completion has access to
    // the java:comp namespace of the servlet because it is running on a ManagedExecutorService thread.
    @Test
    public void testCompletionStageRxInvokerSynchronousFunctionViaManagedExecutor(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";

        assertNotNull(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.executorService(scheduledExecutor).build();
        CompletionStage<?> stage = client.target(uri).request("text/plain").rx().method(HttpMethod.GET, String.class).thenApply(o -> {
            String threadName = Thread.currentThread().getName();
            Object lookupResult;
            try {
                lookupResult = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
            } catch (NamingException x) {
                lookupResult = x;
            }
            return new Object[] { o, threadName, lookupResult };
        });

        Object[] results = (Object[]) stage.toCompletableFuture().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        String resultString = Arrays.toString(results);

        assertEquals("test123", results[0]);

        String executionThreadName = (String) results[1];
        assertTrue(resultString, executionThreadName.startsWith("Default Executor-thread-"));

        assertTrue(resultString, results[2] instanceof ScheduledExecutorService);
    }

    // TODO Once CXF is updated to new level, also test .rx(org.apache.cxf.jaxrs.rx.client.ObservableRxInvoker.class)

    // Basic test using JAX-RS client async() to run a request asynchronously,
    // which by default should run on the Liberty executor.
    @Test
    public void testClientBuilderAsync(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.build();
        Future<String> f = client.target(uri).request("text/plain").async().get(String.class);
        assertEquals("test123", f.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        client.close();
    }

    // Basic test using JAX-RS client submit to submit a request for asynchronous execution,
    // which by default should run on the Liberty executor.
    @Test
    public void testClientBuilderSubmit(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.build();

        Future<String> f = client.target(uri).request("text/plain").buildGet().submit(String.class);
        assertEquals("test123", f.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        client.close();
    }

    // Basic test using JAX-RS client submit with an invocation callback, which checks that the
    // callback runs without access to the java:comp namespace of the servlet because it is
    // running on a plain Liberty executor thread (as opposed to a ManagedExecutorService thread).
    @Test
    public void testClientBuilderSubmitInvocationCallback(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";

        assertNotNull(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.build();

        final ConcurrentLinkedQueue<Object> result = new ConcurrentLinkedQueue<Object>();
        Future<String> f = client.target(uri).request("text/plain").buildGet().submit(new InvocationCallback<String>() {
            @Override
            public void completed(String response) {
                result.add(response);
                result.add(Thread.currentThread().getName());
                try {
                    result.add(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));
                } catch (NamingException x) {
                    result.add(x); // expected because java:comp should be unavailable on callback thread
                }
            }

            @Override
            public void failed(Throwable x) {
                result.add(x);
            }
        });
        assertEquals("test123", f.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        client.close();

        Object r = result.poll();
        assertNotNull(r);
        if (r instanceof Throwable)
            throw new Exception("InvocationCallback received failure. See cause.", (Throwable) r);
        assertEquals("test123", r);

        assertNotNull(r = result.poll());
        String callbackThreadName = (String) r;
        assertTrue(callbackThreadName, callbackThreadName.startsWith("Default Executor-thread-"));

        assertNotNull(r = result.poll());
        if (r instanceof NamingException)
            ; // test passes, servlet's java:comp should be unavailable to callback thread
        else if (r instanceof Throwable)
            throw new Exception("Unxpected failure for InvocationCallback lookup attempt. See cause.", (Throwable) r);
        else
            fail("Should not be able to look up servlet's java:comp from InvocationCallback thread: " + r);
    }

    @Test
    public void testClientBuilderAsyncResponseOnThreadFromSpecifiedExecutorService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/jaxrsapp/testapp/test/info";
        ClientBuilder builder = ClientBuilder.newBuilder()
                                             .executorService(Executors.newFixedThreadPool(1, new ThreadFactory(){

                                        @Override
                                        public Thread newThread(Runnable r) {
                                            Thread t = new Thread(r);
                                            t.setDaemon(true);
                                            t.setName("MyThread");
                                            return t;
                                        }}))
                                             .register(new ClientResponseFilter(){

            @Override
            public void filter(ClientRequestContext reqCtx, ClientResponseContext respCtx) throws IOException {
                respCtx.getHeaders().putSingle("ThreadName", Thread.currentThread().getName());
                
            }});
        Client client = builder.build();
        Future<Response> f = client.target(uri).request("text/plain").async().get();
        Response r = f.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("MyThread", r.getHeaderString("ThreadName"));
        client.close();
    }
}

