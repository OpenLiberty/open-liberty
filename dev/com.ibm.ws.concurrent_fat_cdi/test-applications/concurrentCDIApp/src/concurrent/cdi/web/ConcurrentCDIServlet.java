/*******************************************************************************
 * Copyright (c) 2017,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import concurrent.cdi.context.location.Location;

@ContextServiceDefinition(name = "java:app/concurrent/with-app-context",
                          qualifiers = WithAppContext.class,
                          propagated = APPLICATION, cleared = ALL_REMAINING)
@ContextServiceDefinition(name = "java:app/concurrent/with-location-and-tx-context",
                          qualifiers = { WithLocationContext.class, WithTransactionContext.class },
                          propagated = { Location.CONTEXT_NAME, TRANSACTION }, cleared = ALL_REMAINING)
@ContextServiceDefinition(name = "java:app/concurrent/without-app-context",
                          qualifiers = WithoutAppContext.class,
                          cleared = APPLICATION, propagated = ALL_REMAINING)
@ManagedExecutorDefinition(name = "java:app/concurrent/executor-with-app-context",
                           qualifiers = WithAppContext.class,
                           context = "java:app/concurrent/with-app-context")
@ManagedExecutorDefinition(name = "java:app/concurrent/executor-without-app-context",
                           qualifiers = WithoutAppContext.class,
                           context = "java:app/concurrent/without-app-context")
@ManagedScheduledExecutorDefinition(name = "java:app/concurrent/scheduled-executor-with-app-context",
                                    qualifiers = WithAppContext.class,
                                    context = "java:app/concurrent/with-app-context")
@ManagedScheduledExecutorDefinition(name = "java:app/concurrent/scheduled-executor-without-app-context",
                                    qualifiers = WithoutAppContext.class,
                                    context = "java:app/concurrent/without-app-context")
@ManagedThreadFactoryDefinition(name = "java:app/concurrent/thread-factory-with-app-context",
                                qualifiers = WithAppContext.class,
                                context = "java:app/concurrent/with-app-context",
                                priority = 4)
@ManagedThreadFactoryDefinition(name = "java:app/concurrent/thread-factory-with-location-and-tx-context",
                                qualifiers = { WithLocationContext.class, WithTransactionContext.class },
                                context = "java:app/concurrent/with-location-and-tx-context",
                                priority = 7)
@ManagedThreadFactoryDefinition(name = "java:app/concurrent/thread-factory-without-app-context",
                                qualifiers = WithoutAppContext.class,
                                context = "java:app/concurrent/without-app-context",
                                priority = 6)
@SuppressWarnings("serial")
@ApplicationScoped
@WebServlet("/*")
public class ConcurrentCDIServlet extends HttpServlet {

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Inject
    ContextService defaultContextSvc;

    @Inject
    ManagedExecutorService defaultManagedExecutor;

    @Inject
    ManagedScheduledExecutorService defaultManagedScheduledExecutor;

    @Inject
    ManagedThreadFactory defaultManagedThreadFactory;

    @Inject
    @WithAppContext
    ManagedExecutorService executorWithAppContext;

    @Inject
    @WithoutAppContext
    ManagedExecutorService executorWithoutAppContext;

    @Inject
    @WithoutLocationContext
    @WithoutTransactionContext
    ManagedExecutorService executorWithoutLocationAndTxContext;

    @Inject
    @WithAppContext
    ManagedScheduledExecutorService scheduledExecutorWithAppContext;

    @Inject
    @WithoutAppContext
    ManagedScheduledExecutorService scheduledExecutorWithoutAppContext;

    @Inject
    @WithoutLocationContext
    ManagedScheduledExecutorService scheduledExecutorWithoutLocationContext;

    @Inject
    @WithAppContext
    ManagedThreadFactory threadFactoryWithAppContext;

    @Inject
    @WithLocationContext
    ManagedThreadFactory threadFactoryWithLocationContext;

    @Inject
    @WithoutAppContext
    ManagedThreadFactory threadFactoryWithoutAppContext;

    @Inject
    @WithoutTransactionContext
    @WithoutLocationContext
    ManagedThreadFactory threadFactoryWithoutLocationAndTxContext;

    @Inject
    @WithAppContext
    ContextService withAppContext;

    @Inject
    @WithoutAppContext
    ContextService withoutAppContext;

    @Inject
    @WithoutLocationContext
    @WithoutTransactionContext
    ContextService withoutLocationAndTxContext;

    @Inject
    @WithoutLocationContext
    ContextService withoutLocationContext;

    @Resource
    UserTransaction tx;

    private ExecutorService unmanagedThreads;

    @Override
    public void destroy() {
        unmanagedThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        unmanagedThreads = Executors.newFixedThreadPool(5); // TODO switch to virtual threads?
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String method = request.getParameter("testMethod");

        System.out.println(">>> BEGIN: " + method);
        System.out.println("Request URL: " + request.getRequestURL() + '?' + request.getQueryString());
        PrintWriter writer = response.getWriter();
        if (method != null && method.length() > 0) {
            try {
                // Use reflection to try invoking various test method signatures:
                // 1)  method(HttpServletRequest request, HttpServletResponse response)
                // 2)  method()
                // 3)  use custom method invocation by calling invokeTest(method, request, response)
                try {
                    Method mthd = getClass().getMethod(method, HttpServletRequest.class, HttpServletResponse.class);
                    mthd.invoke(this, request, response);
                } catch (NoSuchMethodException nsme) {
                    Method mthd = getClass().getMethod(method, (Class<?>[]) null);
                    mthd.invoke(this);
                }

                writer.println("SUCCESS");
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    t = t.getCause();
                }

                System.out.println("ERROR: " + t);
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                System.err.print(sw);

                writer.println("ERROR: Caught exception attempting to call test method " + method + " on servlet " + getClass().getName());
                t.printStackTrace(writer);
            }
        } else {
            System.out.println("ERROR: expected testMethod parameter");
            writer.println("ERROR: expected testMethod parameter");
        }

        writer.flush();
        writer.close();

        System.out.println("<<< END:   " + method);
    }

    /**
     * Attempt to obtain a ContextService with an unrecognized qualifier.
     */
    public void testContextServiceWithUnrecognizedQualifier() throws Exception {
        assertEquals(false, CDI.current().select(ContextService.class, Unrecognized.Literal.INSTANCE).isResolvable());
    }

    /**
     * Inject default instance of ContextService and use it.
     */
    public void testInjectContextServiceDefaultInstance() throws Exception {
        assertNotNull(defaultContextSvc);

        // Use the ContextService to contextualize a task that require the application's context (to look up a java:comp name)
        Callable<?> task = defaultContextSvc.contextualCallable(() -> InitialContext.doLookup("java:comp/env/entry2"));

        Future<?> future = unmanagedThreads.submit(task);

        Object found = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", found);
    }

    /**
     * Inject qualified instances of ContextService and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are defined
     * on a ContextServiceDefinition annotation.
     */
    public void testInjectContextServiceQualifiedFromAnno() throws Exception {
        assertNotNull(withAppContext);

        Callable<?> task1 = withAppContext.contextualCallable(() -> InitialContext.doLookup("java:comp/env/entry2"));

        Future<?> future1 = unmanagedThreads.submit(task1);

        Object found1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", found1);

        assertNotNull(withoutAppContext);

        Callable<?> task2 = withoutAppContext.contextualCallable(() -> InitialContext.doLookup("java:comp/env/entry2"));

        try {
            Object found2 = task2.call();
            fail("Application context should be cleared, preventing java:comp lookup. Instead found " + found2);
        } catch (NamingException x) {
            // expected
        }
    }

    /**
     * Inject qualified instances of ContextService and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are defined
     * on a context-service element in web.xml.
     */
    public void testInjectContextServiceQualifiedFromWebDD() throws Exception {
        assertNotNull(withoutLocationContext);
        assertNotNull(withoutLocationAndTxContext);

        Location.set("Olmsted County");
        try {
            Supplier<String> supplier1 = withoutLocationContext.contextualSupplier(Location::get);
            assertEquals(null, supplier1.get());
            assertEquals("Olmsted County", Location.get());

            Callable<String> callable1 = withoutLocationContext.contextualCallable(() -> {
                return InitialContext.doLookup("java:comp/env/entry2");
            });

            Future<?> future1 = unmanagedThreads.submit(callable1);

            Object found1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals("value2", found1);

            tx.begin();
            try {
                Supplier<String> supplier2 = withoutLocationAndTxContext.contextualSupplier(Location::get);
                assertEquals(null, supplier2.get());
                assertEquals("Olmsted County", Location.get());

                Callable<String> callable2 = withoutLocationContext.contextualCallable(() -> {
                    return InitialContext.doLookup("java:comp/env/entry2");
                });

                Future<?> future2 = unmanagedThreads.submit(callable2);

                Object found2 = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertEquals("value2", found2);

                Callable<Integer> callable3 = withoutLocationAndTxContext.contextualCallable(() -> tx.getStatus());
                assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), callable3.call());
                assertEquals(Status.STATUS_ACTIVE, tx.getStatus());
            } finally {
                tx.rollback();
            }
        } finally {
            Location.clear();
        }
    }

    /**
     * Inject default instance of ManagedExecutorService and use it.
     */
    public void testInjectManagedExecutorServiceDefaultInstance() throws Exception {
        assertNotNull(defaultManagedExecutor);

        // Requires the application's context (to look up a java:comp name)
        Callable<?> task = () -> InitialContext.doLookup("java:comp/env/entry2");
        Future<?> future = defaultManagedExecutor.submit(task);

        Object result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result);
    }

    /**
     * Inject qualified instances of ManagedExecutorService and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are configured
     * on the ManagedExecutorDefinition annotation.
     */
    public void testInjectManagedExecutorServiceQualifiedFromAnno() throws Exception {
        Callable<?> task = () -> InitialContext.doLookup("java:comp/env/entry2");

        assertNotNull(executorWithAppContext);

        Future<?> future1 = executorWithoutAppContext.submit(task);
        try {
            Object result1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Application context should be cleared, preventing java:comp lookup. Instead found " + result1);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NamingException)
                ; // expected
            else
                throw x;
        }

        assertNotNull(executorWithoutAppContext);

        Future<?> future2 = executorWithAppContext.submit(task);
        Object result2 = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result2);
    }

    /**
     * Inject qualified instances of ManagedExecutorService and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are configured
     * on the managed-executor element in web.xml.
     */
    public void testInjectManagedExecutorServiceQualifiedFromWebDD() throws Exception {
        Callable<Object[]> task = () -> {
            return new Object[] {
                                  Location.get(),
                                  tx.getStatus(),
                                  InitialContext.doLookup("java:comp/env/entry2")
            };
        };

        assertNotNull(executorWithoutLocationAndTxContext);

        tx.begin();
        Location.set("Minnesota");
        try {
            List<Future<Object[]>> results = executorWithoutLocationAndTxContext.invokeAll(Arrays.asList(task, task));

            Object[] r = results.get(0).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(null, r[0]); // cleared location context
            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), r[1]); // cleared transaction context
            assertEquals("value2", r[2]); // propagated transaction context

            r = results.get(0).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(null, r[0]); // cleared location context
            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), r[1]); // cleared transaction context
            assertEquals("value2", r[2]); // propagated transaction context

            assertEquals("Minnesota", Location.get());
            assertEquals(Status.STATUS_ACTIVE, tx.getStatus());
        } finally {
            Location.clear();
            tx.rollback();
        }
    }

    /**
     * Inject default instance of ManagedScheduledExecutorService and use it.
     */
    public void testInjectManagedScheduledExecutorServiceDefaultInstance() throws Exception {
        assertNotNull(defaultManagedScheduledExecutor);

        final AtomicInteger executionCount = new AtomicInteger();
        Future<?> future1 = defaultManagedScheduledExecutor.schedule(() -> executionCount.incrementAndGet(), 30, TimeUnit.MINUTES);

        // Requires the application's context (to look up a java:comp name)
        Callable<?> task2 = () -> InitialContext.doLookup("java:comp/env/entry2");
        Future<?> future2 = defaultManagedScheduledExecutor.schedule(task2, 122, TimeUnit.MILLISECONDS);

        Object result = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result);

        assertEquals(true, future1.cancel(false));
        assertEquals(0, executionCount.get());
    }

    /**
     * Inject qualified instances of ManagedScheduledExecutorService and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are configured on the
     * ManagedScheduledExecutorDefinition annotation.
     */
    public void testInjectManagedScheduledExecutorServiceQualifiedFromAnno() throws Exception {
        Callable<?> task = () -> InitialContext.doLookup("java:comp/env/entry2");

        assertNotNull(scheduledExecutorWithAppContext);
        assertNotNull(scheduledExecutorWithoutAppContext);

        Future<?> future1 = scheduledExecutorWithoutAppContext.schedule(task, 111, TimeUnit.MILLISECONDS);
        Future<?> future2 = scheduledExecutorWithAppContext.schedule(task, 112, TimeUnit.MILLISECONDS);

        try {
            Object result1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Application context should be cleared, preventing java:comp lookup. Instead found " + result1);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NamingException)
                ; // expected
            else
                throw x;
        }

        Object result2 = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result2);
    }

    /**
     * Inject qualified instances of ManagedScheduledExecutorService and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are configured
     * on the managed-scheduled-executor element in web.xml.
     */
    public void testInjectManagedScheduledExecutorServiceQualifiedFromWebDD() throws Exception {
        Callable<Object[]> task = () -> {
            return new Object[] {
                                  Location.get(),
                                  InitialContext.doLookup("java:comp/env/entry2")
            };
        };

        assertNotNull(scheduledExecutorWithoutLocationContext);

        Location.set("2800 37th St NW");
        try {
            ScheduledFuture<Object[]> future = scheduledExecutorWithoutLocationContext.schedule(task, 37, TimeUnit.MILLISECONDS);

            Object[] results = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(null, results[0]); // cleared location context
            assertEquals("value2", results[1]); // propagated transaction context

            assertEquals("2800 37th St NW", Location.get());
        } finally {
            Location.clear();
        }
    }

    /**
     * Inject an instance of the default ManagedThreadFactory resource and use it.
     */
    public void testInjectManagedThreadFactoryDefaultInstance() throws Exception {
        assertNotNull(defaultManagedThreadFactory);

        CompletableFuture<?> future = new CompletableFuture<>();

        // Requires the application's context (to look up a java:comp name)
        Runnable task = () -> {
            try {
                future.complete(InitialContext.doLookup("java:comp/env/entry2"));
            } catch (Throwable x) {
                future.completeExceptionally(x);
            }
        };

        Thread thread = defaultManagedThreadFactory.newThread(task);
        thread.start();

        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());

        Object result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result);
    }

    /**
     * Inject qualified instances of ManagedThreadFactory and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are configured
     * on a ManagedThreadFactoryDefinition annotation.
     */
    public void testInjectManagedThreadFactoryQualifiedFromAnno() throws Exception {
        assertNotNull(threadFactoryWithAppContext);
        assertNotNull(threadFactoryWithoutAppContext);

        CompletableFuture<?> future1 = new CompletableFuture<>();
        CompletableFuture<?> future2 = new CompletableFuture<>();

        // Requires the application's context (to look up a java:comp name)
        Runnable task1 = () -> {
            try {
                future1.complete(InitialContext.doLookup("java:comp/env/entry2"));
            } catch (Throwable x) {
                future1.completeExceptionally(x);
            }
        };

        // Requires the application's context (to look up a java:comp name).
        // Expect an exception because this context should be cleared.
        Runnable task2 = () -> {
            try {
                future2.complete(InitialContext.doLookup("java:comp/env/entry2"));
            } catch (Throwable x) {
                future2.completeExceptionally(x);
            }
        };

        Thread thread1 = threadFactoryWithAppContext.newThread(task1);
        Thread thread2 = threadFactoryWithoutAppContext.newThread(task2);

        thread1.start();
        thread2.start();

        assertEquals(4, thread1.getPriority());
        assertEquals(6, thread2.getPriority());

        Object result1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result1);

        try {
            Object result2 = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Should not be abl to look up java:comp name because application context should be cleared. Found: " + result2);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NamingException)
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Inject qualified instances of ManagedThreadFactory and verify that the behavior of each
     * matches the configuration that the qualifier points to. The qualifiers are configured
     * on a managed-thread-factory element in web.xml.
     */
    public void testInjectManagedThreadFactoryQualifiedFromWebDD() throws Exception {
        assertNotNull(threadFactoryWithoutLocationAndTxContext);

        tx.begin();
        Location.set("St. Paul, MN");
        try {
            CompletableFuture<Object[]> future = new CompletableFuture<>();

            Runnable task = () -> {
                try {
                    future.complete(new Object[] {
                                                   tx.getStatus(),
                                                   Location.get(),
                                                   // Requires the application's context to look up a java:comp name
                                                   InitialContext.doLookup("java:comp/env/entry2"),
                                                   Thread.currentThread().getPriority()
                    });
                } catch (Throwable x) {
                    future.completeExceptionally(x);
                }
            };

            Thread thread = threadFactoryWithoutLocationAndTxContext.newThread(task);

            thread.start();

            assertEquals(3, thread.getPriority());

            Object[] results = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), results[0]);
            assertEquals(null, results[1]);
            assertEquals("value2", results[2]);
            assertEquals(Integer.valueOf(3), results[3]);
        } finally {
            Location.clear();
            tx.rollback();
        }
    }

    /**
     * Verify that a ManagedThreadFactory (regardless of whether it has qualifiers) follows the rule of
     * capturing context at the point when the resource is created. A new instance must be created upon
     * lookup.
     */
    public void testLookUpManagedThreadFactory() throws Exception {
        Location.set("Rochester, MN");
        try {
            ManagedThreadFactory threadFactory1 = InitialContext.doLookup("java:app/concurrent/thread-factory-with-location-and-tx-context");

            Location.set("Byron, MN");

            CompletableFuture<String> future1 = new CompletableFuture<>();

            Thread thread1 = threadFactory1.newThread(() -> future1.complete(Location.get()));
            thread1.start();

            assertEquals(7, thread1.getPriority());

            String result1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals("Rochester, MN", result1);

            assertEquals("Byron, MN", Location.get());
        } finally {
            Location.clear();
        }
    }

    /**
     * Verify that the equals operation of generated instances of qualifier annotations
     * obeys the JavaDoc for Annotation.equals and can be compared with literal instances
     * of the same annotation in either direction.
     */
    public void testQualifierEquals() throws Exception {
        Instance<ContextService> instance;

        Annotation annoWithAppContext = null;
        instance = CDI.current().select(ContextService.class, WithAppContext.Literal.INSTANCE);
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithAppContext.class))
                annoWithAppContext = anno;
        assertNotNull(annoWithAppContext);
        assertEquals(true, WithAppContext.Literal.INSTANCE.equals(annoWithAppContext));
        assertEquals(true, annoWithAppContext.equals(WithAppContext.Literal.INSTANCE));
        assertEquals(true, annoWithAppContext.equals(annoWithAppContext));

        Annotation annoWithLocationContext = null;
        instance = CDI.current().select(ContextService.class, WithLocationContext.Literal.INSTANCE);
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithLocationContext.class))
                annoWithLocationContext = anno;
        assertNotNull(annoWithLocationContext);
        assertEquals(true, WithLocationContext.Literal.INSTANCE.equals(annoWithLocationContext));
        assertEquals(true, annoWithLocationContext.equals(WithLocationContext.Literal.INSTANCE));
        assertEquals(true, annoWithLocationContext.equals(annoWithLocationContext));

        Annotation annoWithoutLocationContext = null;
        instance = CDI.current().select(ContextService.class, WithoutLocationContext.Literal.INSTANCE);
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithoutLocationContext.class))
                annoWithoutLocationContext = anno;
        assertNotNull(annoWithoutLocationContext);
        assertEquals(true, WithoutLocationContext.Literal.INSTANCE.equals(annoWithoutLocationContext));
        assertEquals(true, annoWithoutLocationContext.equals(WithoutLocationContext.Literal.INSTANCE));
        assertEquals(true, annoWithoutLocationContext.equals(annoWithoutLocationContext));

        Annotation annoWithoutTransactionContext = null;
        instance = CDI.current().select(ContextService.class, WithoutTransactionContext.Literal.INSTANCE);
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithoutTransactionContext.class))
                annoWithoutTransactionContext = anno;
        assertNotNull(annoWithoutTransactionContext);
        assertEquals(true, WithoutTransactionContext.Literal.INSTANCE.equals(annoWithoutTransactionContext));
        assertEquals(true, annoWithoutTransactionContext.equals(WithoutTransactionContext.Literal.INSTANCE));
        assertEquals(true, annoWithoutTransactionContext.equals(annoWithoutTransactionContext));

        assertEquals(false, annoWithAppContext.equals(annoWithLocationContext));
        assertEquals(false, annoWithAppContext.equals(annoWithoutLocationContext));
        assertEquals(false, annoWithAppContext.equals(annoWithoutTransactionContext));

        assertEquals(false, annoWithLocationContext.equals(annoWithAppContext));
        assertEquals(false, annoWithLocationContext.equals(annoWithoutLocationContext));
        assertEquals(false, annoWithLocationContext.equals(annoWithoutTransactionContext));

        assertEquals(false, annoWithoutLocationContext.equals(annoWithAppContext));
        assertEquals(false, annoWithoutLocationContext.equals(annoWithLocationContext));
        assertEquals(false, annoWithoutLocationContext.equals(annoWithoutTransactionContext));

        assertEquals(false, annoWithoutTransactionContext.equals(annoWithAppContext));
        assertEquals(false, annoWithoutTransactionContext.equals(annoWithLocationContext));
        assertEquals(false, annoWithoutTransactionContext.equals(annoWithoutLocationContext));

        assertEquals(false, annoWithLocationContext.equals(WithLocationContext.Literal.with(TRANSACTION)));

        assertEquals(false, annoWithoutLocationContext.equals(WithoutLocationContext.Literal.of("A", 12)));

        assertEquals(false, annoWithoutTransactionContext.equals(WithoutTransactionContext.Literal.of("B", 10)));

        // comparison of array valued attributes
        Annotation annoWithTransactionContext = null;
        instance = CDI.current().select(ContextService.class, WithTransactionContext.Literal.INSTANCE);
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithTransactionContext.class))
                annoWithTransactionContext = anno;
        assertNotNull(annoWithTransactionContext);
        assertEquals(true, WithTransactionContext.Literal.INSTANCE.equals(annoWithTransactionContext));
        assertEquals(true, annoWithTransactionContext.equals(WithTransactionContext.Literal.INSTANCE));
        assertEquals(true, annoWithTransactionContext.equals(annoWithTransactionContext));

        WithTransactionContext annoWithDifferentOrderedArray = WithTransactionContext.Literal //
                        .of(new Class<?>[] { short.class, int.class, long.class }, // different order of values
                            new int[] { 216, 713, 745 }); // matches

        assertEquals(false, annoWithTransactionContext.equals(annoWithDifferentOrderedArray));

        WithTransactionContext annoWithOneLessArrayElement = WithTransactionContext.Literal //
                        .of(new Class<?>[] { long.class, int.class, short.class }, // matches
                            new int[] { 216, 713 }); // one less value in array

        assertEquals(false, annoWithTransactionContext.equals(annoWithOneLessArrayElement));
    }

    /**
     * Verify that the hashCode operation of generated instances of qualifier annotations
     * matches the behavior of literal instances of the same annotation.
     */
    public void testQualifierHashCode() throws Exception {
        Instance<ContextService> instance;
        Annotation qualifier;

        instance = CDI.current().select(ContextService.class, WithAppContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithAppContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        assertEquals(WithAppContext.Literal.INSTANCE.hashCode(), qualifier.hashCode());

        instance = CDI.current().select(ContextService.class, WithLocationContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithLocationContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        assertEquals(WithLocationContext.Literal.INSTANCE.hashCode(), qualifier.hashCode());

        instance = CDI.current().select(ContextService.class, WithoutLocationContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithoutLocationContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        assertEquals(WithoutLocationContext.Literal.INSTANCE.hashCode(), qualifier.hashCode());

        instance = CDI.current().select(ContextService.class, WithoutTransactionContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithoutTransactionContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        assertEquals(WithoutTransactionContext.Literal.INSTANCE.hashCode(), qualifier.hashCode());

        instance = CDI.current().select(ContextService.class, WithTransactionContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithTransactionContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        assertEquals(WithTransactionContext.Literal.INSTANCE.hashCode(), qualifier.hashCode());
    }

    /**
     * Verify the toString operation of generated instances of qualifier annotations.
     */
    public void testQualifierToString() throws Exception {
        Instance<ContextService> instance;
        Annotation qualifier;
        String stringValue;

        instance = CDI.current().select(ContextService.class, WithLocationContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithLocationContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        stringValue = qualifier.toString();
        assertEquals(stringValue, true, stringValue.contains(WithLocationContext.class.getName()));
        assertEquals(stringValue, true, stringValue.contains("pairedWith")); // annotation attribute name

        instance = CDI.current().select(ContextService.class, WithoutLocationContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithoutLocationContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        stringValue = qualifier.toString();
        assertEquals(stringValue, true, stringValue.contains(WithoutLocationContext.class.getName()));
        assertEquals(stringValue, true, stringValue.contains("letter")); // annotation attribute name
        assertEquals(stringValue, true, stringValue.contains("A")); // annotation attribute value
        assertEquals(stringValue, true, stringValue.contains("number")); // annotation attribute name
        assertEquals(stringValue, true, stringValue.contains("10")); // annotation attribute value

        instance = CDI.current().select(ContextService.class, WithTransactionContext.Literal.INSTANCE);
        qualifier = null;
        for (Annotation anno : instance.getHandle().getBean().getQualifiers())
            if (anno.annotationType().equals(WithTransactionContext.class))
                qualifier = anno;
        assertNotNull(qualifier);
        stringValue = qualifier.toString();
        assertEquals(stringValue, true, stringValue.contains(WithTransactionContext.class.getName()));
        assertEquals(stringValue, true, stringValue.contains("classes")); // annotation attribute name
        assertEquals(stringValue, true, stringValue.contains("int")); // in annotation attribute value
        assertEquals(stringValue, true, stringValue.contains("long")); // in annotation attribute value
        assertEquals(stringValue, true, stringValue.contains("short")); // in annotation attribute value
        assertEquals(stringValue, true, stringValue.contains("numbers")); // annotation attribute name
        assertEquals(stringValue, true, stringValue.contains("216")); // in annotation attribute value
        assertEquals(stringValue, true, stringValue.contains("713")); // in annotation attribute value
        assertEquals(stringValue, true, stringValue.contains("745")); // in annotation attribute value
    }

    /**
     * Use CDI.current() to select the default instance of ContextService and use it.
     */
    public void testSelectContextServiceDefaultInstance() throws Exception {
        ContextService contextSvc = CDI.current().select(ContextService.class, Default.Literal.INSTANCE).get();

        assertNotNull(contextSvc);

        // Use the ContextService to contextualize a task that require the application's context (to look up a java:comp name)
        Callable<?> task = contextSvc.contextualCallable(() -> InitialContext.doLookup("java:comp/env/entry2"));

        Future<?> future = unmanagedThreads.submit(task);

        Object found = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", found);
    }

    /**
     * Use CDI.current() to select qualified instances of ContextService and verify that the behavior of each
     * matches the configuration that the qualifier points to.
     */
    public void testSelectContextServiceQualified() throws Exception {
        ContextService appContext = CDI.current().select(ContextService.class, WithAppContext.Literal.INSTANCE).get();

        assertNotNull(appContext);

        Callable<?> task1 = appContext.contextualCallable(() -> InitialContext.doLookup("java:comp/env/entry2"));

        Future<?> future1 = unmanagedThreads.submit(task1);

        Object found1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", found1);

        ContextService clearAppContext = CDI.current().select(ContextService.class, WithoutAppContext.Literal.INSTANCE).get();

        assertNotNull(clearAppContext);

        Callable<?> task2 = clearAppContext.contextualCallable(() -> InitialContext.doLookup("java:comp/env/entry2"));

        try {
            Object found2 = task2.call();
            fail("Application context should be cleared, preventing java:comp lookup. Instead found " + found2);
        } catch (NamingException x) {
            // expected
        }
    }

    /**
     * Use CDI.current() to select an instance of the default ManagedThreadFactory instance and use it.
     */
    public void testSelectManagedThreadFactoryDefaultInstance() throws Exception {
        ManagedThreadFactory threadFactory = CDI.current().select(ManagedThreadFactory.class, Default.Literal.INSTANCE).get();

        assertNotNull(threadFactory);

        CompletableFuture<?> future = new CompletableFuture<>();

        // Requires the application's context (to look up a java:comp name)
        Runnable task = () -> {
            try {
                future.complete(InitialContext.doLookup("java:comp/env/entry2"));
            } catch (Throwable x) {
                future.completeExceptionally(new AssertionError("A failure occurred on the new thread.", x));
            }
        };

        Thread thread = threadFactory.newThread(task);
        thread.start();

        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());

        Object result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result);
    }

    /**
     * Verify that a ManagedThreadFactory with qualifiers follows the rule of capturing context only
     * at the point when the resource was created, and does not replace that context with the context that is on
     * the thread when CDI.current().select obtains the bean.
     */
    public void testSelectManagedThreadFactoryQualified() throws Exception {
        // Context is captured when the ManagedThreadFactory is created, not when we first obtain it from CDI,

        Location.set("Rochester, MN");
        try {
            ManagedThreadFactory threadFactory1 = CDI.current().select(ManagedThreadFactory.class, WithLocationContext.Literal.INSTANCE).get();

            CompletableFuture<String> future1 = new CompletableFuture<>();

            Thread thread1 = threadFactory1.newThread(() -> future1.complete(Location.get()));
            thread1.start();

            assertEquals(7, thread1.getPriority());

            String result1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals(null, result1);

            assertEquals("Rochester, MN", Location.get());
        } finally {
            Location.clear();
        }

        // Application context was originally on the thread, so it is propagated,

        ManagedThreadFactory threadFactory2 = CDI.current().select(ManagedThreadFactory.class, WithAppContext.Literal.INSTANCE).get();

        CompletableFuture<String> future2 = new CompletableFuture<>();

        Thread thread2 = threadFactory2.newThread(() -> {
            try {
                future2.complete(InitialContext.doLookup("java:comp/env/entry2"));
            } catch (Throwable x) {
                future2.completeExceptionally(x);
            }
        });
        thread2.start();

        assertEquals(4, thread2.getPriority());

        assertEquals("value2", future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }
}
