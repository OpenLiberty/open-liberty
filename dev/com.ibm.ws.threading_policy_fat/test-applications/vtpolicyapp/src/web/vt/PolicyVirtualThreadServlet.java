/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package web.vt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyExecutor.MaxPolicy;
import com.ibm.ws.threading.PolicyExecutorProvider;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PolicyVirtualThreadServlet")
public class PolicyVirtualThreadServlet extends HttpServlet {
    // Maximum number of nanoseconds to wait for a task to complete
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(lookup = "test/TestPolicyExecutorProvider")
    private PolicyExecutorProvider provider;

    // Executor that can be used when tests don't want to tie up policy executor threads to perform concurrent test logic
    private ExecutorService testThreads;

    @Override
    public void destroy() {
        testThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) {
        testThreads = Executors.newFixedThreadPool(20);
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
     * Invokes thread.isVirtual();
     */
    static boolean isVirtual(Thread thread) throws Exception {
        return (Boolean) Thread.class.getMethod("isVirtual").invoke(thread);
    }

    /**
     * Tests that max concurrency is enforced when running on virtual threads.
     * Verify the names of the virtual threads are unique, include the identifier for the policy executor,
     * and end with the thread number.
     */
    public void testMaxConcurrencyWithVirtualThreads() throws Exception {
        Map<String, Object> config = new TreeMap<>();
        config.put("max", 3);
        config.put("maxPolicy", MaxPolicy.strict.name());
        config.put("virtual", true);
        // defaults:
        config.put("expedite", 0);
        config.put("maxWaitForEnqueue", 0L);
        config.put("runIfQueueFull", false);

        PolicyExecutor executor = provider.create("testMaxConcurrencyWithVirtualThreads");
        executor.updateConfig(config);

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch threeTasksStarted = new CountDownLatch(3);
        CountDownLatch fourTasksStarted = new CountDownLatch(4);

        CurrentThreadTask task1 = new CurrentThreadTask(blocker, threeTasksStarted, fourTasksStarted);
        CurrentThreadTask task2 = new CurrentThreadTask(blocker, threeTasksStarted, fourTasksStarted);
        CurrentThreadTask task3 = new CurrentThreadTask(blocker, threeTasksStarted, fourTasksStarted);
        CurrentThreadTask task4 = new CurrentThreadTask(blocker, threeTasksStarted, fourTasksStarted);

        Future<Thread> future1 = executor.submit(task1);
        Future<Thread> future2 = executor.submit(task2);
        Future<Thread> future3 = executor.submit(task3);
        Future<Thread> future4 = executor.submit(task4);

        assertEquals(true, threeTasksStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(false, fourTasksStarted.await(200, TimeUnit.MILLISECONDS));

        blocker.countDown();

        assertEquals(true, fourTasksStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        Thread thread1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        Thread thread2 = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        Thread thread3 = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        Thread thread4 = future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(true, isVirtual(thread1));
        assertEquals(true, isVirtual(thread2));
        assertEquals(true, isVirtual(thread3));
        assertEquals(true, isVirtual(thread4));

        assertEquals(thread1.getName(), true, thread1.getName().startsWith("PolicyExecutorProvider-testMaxConcurrencyWithVirtualThreads-"));
        assertEquals(thread2.getName(), true, thread2.getName().startsWith("PolicyExecutorProvider-testMaxConcurrencyWithVirtualThreads-"));
        assertEquals(thread3.getName(), true, thread3.getName().startsWith("PolicyExecutorProvider-testMaxConcurrencyWithVirtualThreads-"));
        assertEquals(thread4.getName(), true, thread4.getName().startsWith("PolicyExecutorProvider-testMaxConcurrencyWithVirtualThreads-"));

        Set<String> threadNames = new TreeSet<>();
        threadNames.add(thread1.getName());
        threadNames.add(thread2.getName());
        threadNames.add(thread3.getName());
        threadNames.add(thread4.getName());

        assertEquals(threadNames.toString(), 4, threadNames.size());

        char threadNum1 = thread1.getName().charAt(thread1.getName().length() - 1);
        char threadNum2 = thread2.getName().charAt(thread2.getName().length() - 1);
        char threadNum3 = thread3.getName().charAt(thread3.getName().length() - 1);
        char threadNum4 = thread4.getName().charAt(thread4.getName().length() - 1);

        assertEquals(true, threadNum1 >= '1' && threadNum1 <= '4');
        assertEquals(true, threadNum2 >= '1' && threadNum2 <= '4');
        assertEquals(true, threadNum3 >= '1' && threadNum3 <= '4');
        assertEquals(true, threadNum4 >= '1' && threadNum4 <= '4');

        executor.shutdownNow();
    }
}
