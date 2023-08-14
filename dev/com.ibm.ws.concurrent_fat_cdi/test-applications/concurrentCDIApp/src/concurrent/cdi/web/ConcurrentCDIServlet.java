/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

@ContextServiceDefinition(name = "java:global/concurrent/with-app-context",
                          propagated = APPLICATION, cleared = ALL_REMAINING)
@ContextServiceDefinition(name = "java:global/concurrent/without-app-context",
                          cleared = APPLICATION, propagated = ALL_REMAINING)
@SuppressWarnings("serial")
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
    @Named("java:global/concurrent/with-app-context")
    ContextService withAppContext;

    @Inject
    @Named("java:global/concurrent/without-app-context")
    ContextService withoutAppContext;

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
     * Inject default instance of ContextService and use it.
     */
    @Test
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
     * matches the configuration that the qualifier points to.
     */
    @Test
    public void testInjectContextServiceQualified() throws Exception {
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
     * Inject default instance of ManagedExecutorService and use it.
     */
    @Test
    public void testInjectManagedExecutorServiceDefaultInstance() throws Exception {
        assertNotNull(defaultManagedExecutor);

        // Requires the application's context (to look up a java:comp name)
        Callable<?> task = () -> InitialContext.doLookup("java:comp/env/entry2");
        Future<?> future = defaultManagedExecutor.submit(task);

        Object result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals("value2", result);
    }

    /**
     * Inject default instance of ManagedScheduledExecutorService and use it.
     */
    @Test
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
}
