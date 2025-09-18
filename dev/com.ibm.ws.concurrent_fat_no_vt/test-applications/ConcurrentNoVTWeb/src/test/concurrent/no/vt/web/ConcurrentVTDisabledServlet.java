/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.concurrent.no.vt.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@ManagedExecutorDefinition //
(name = "java:comp/concurrent/AnnoExecutorToOverride",
 virtual = true)
@ManagedScheduledExecutorDefinition //
(name = "java:app/concurrent/AnnoScheduledExecutorToOverride",
 virtual = true)
@ManagedThreadFactoryDefinition //
(name = "java:module/concurrent/AnnoThreadFactoryToOverride",
 virtual = true)
@WebServlet("/*")
@SuppressWarnings("serial")
public class ConcurrentVTDisabledServlet extends FATServlet {

    @Resource(lookup = "java:comp/concurrent/AnnoExecutorToOverride")
    ManagedExecutorService overriddenManagedExecutorFromAnno;

    @Resource(lookup = "concurrent/ServerXMLExecutorToOverride")
    ManagedExecutorService overriddenManagedExecutorFromServerXML;

    @Resource(lookup = "java:global/concurrent/WebXMLExecutorToOverride")
    ManagedExecutorService overriddenManagedExecutorFromWebXML;

    @Resource(lookup = "java:app/concurrent/AnnoScheduledExecutorToOverride")
    ManagedScheduledExecutorService overriddenManagedScheduledExecutorFromAnno;

    @Resource(lookup = "concurrent/ServerXMLScheduledExecutorToOverride")
    ManagedScheduledExecutorService overriddenManagedScheduledExecutorFromServerXML;

    @Resource(lookup = "java:module/concurrent/WebXMLScheduledExecutorToOverride")
    ManagedScheduledExecutorService overriddenManagedScheduledExecutorFromWebXML;

    @Resource(lookup = "java:module/concurrent/AnnoThreadFactoryToOverride")
    ManagedThreadFactory overriddenThreadFactoryFromAnno;

    @Resource(lookup = "concurrent/ServerXMLThreadFactoryToOverride")
    ManagedThreadFactory overriddenThreadFactoryFromServerXML;

    @Resource(lookup = "java:comp/concurrent/WebXMLThreadFactoryToOverride")
    ManagedThreadFactory overriddenThreadFactoryFromWebXML;

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    private ExecutorService unmanagedThreads;

    @Override
    public void destroy() {
        unmanagedThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        unmanagedThreads = Executors.newFixedThreadPool(5);
    }

    /**
     * Determines if the supplied thread is a virtual thread.
     *
     * @param thread a thread that might be a virtual thread.
     * @return true if the supplied thread is a virtual thread, otherwise false.
     */
    private static boolean isVirtual(Thread thread) {
        if (Runtime.version().feature() == 17)
            return false;
        else
            try {
                return (Boolean) Thread.class.getMethod("isVirtual").invoke(thread);
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
    }

    /**
     * A ManagedExecutorDefinition with virtual=true runs async tasks on
     * platform threads instead of virtual threads when the ThreadTypeOverride SPI
     * instructs Liberty to avoid creating virtual threads. The task runs
     * successfully.
     */
    @Test
    public void testOverrideVirtualManagedExecutorFromAnno() throws Exception {

        String testName = "testOverrideVirtualManagedExecutorFromAnno";
        Future<Thread> future = overriddenManagedExecutorFromAnno
                        .submit(() -> {
                            System.out.println("Task from " + testName);
                            return Thread.currentThread();
                        });

        Thread thread = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(false, isVirtual(thread));
    }

    /**
     * A managedExecutorService defined in server.xml with virtual=true runs async
     * tasks on platform threads instead of virtual threads when the
     * ThreadTypeOverride SPI instructs Liberty to avoid creating virtual threads.
     * The task runs successfully on Java SE 21+, but is rejected on Java SE 17.
     */
    @Test
    public void testOverrideVirtualManagedExecutorFromServerXML() throws Exception {

        String testName = "testOverrideVirtualManagedExecutorFromServerXML";

        Callable<Thread> task = () -> {
            System.out.println("Task from " + testName);
            return Thread.currentThread();
        };

        if (Runtime.version().feature() == 17)
            try {
                overriddenManagedExecutorFromServerXML
                                .invokeAll(List.of(task),
                                           TIMEOUT_NS,
                                           TimeUnit.NANOSECONDS);
                fail("Virtual=true should not be allowed on Java SE 17.");
            } catch (IllegalArgumentException x) {
                if (Runtime.version().feature() == 17)
                    return; // expected
                else
                    throw x;
            }
        else {
            Future<Thread> future = overriddenManagedExecutorFromServerXML
                            .invokeAll(List.of(task),
                                       TIMEOUT_NS,
                                       TimeUnit.NANOSECONDS)
                            .get(0);

            assertEquals(true, future.isDone());

            Thread thread = future.get();

            assertEquals(false, isVirtual(thread));
        }
    }

    /**
     * A managed-executor defined in web.xml with virtual=true runs async tasks on
     * platform threads instead of virtual threads when the ThreadTypeOverride SPI
     * instructs Liberty to avoid creating virtual threads. The task runs
     * successfully.
     */
    @Test
    public void testOverrideVirtualManagedExecutorFromWebXML() throws Exception {

        CompletableFuture<Thread> future = overriddenManagedExecutorFromWebXML
                        .completedFuture("testOverrideVirtualManagedExecutorFromWebXML")
                        .thenApplyAsync(testName -> {
                            System.out.println("Task from " + testName);
                            return Thread.currentThread();
                        });

        Thread thread = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(false, isVirtual(thread));
    }

    /**
     * A ManagedEScheduledxecutorDefinition with virtual=true runs async tasks on
     * platform threads instead of virtual threads when the ThreadTypeOverride SPI
     * instructs Liberty to avoid creating virtual threads. The task runs
     * successfully.
     */
    @Test
    public void testOverrideVirtualManagedScheduledExecutorFromAnno() //
                    throws Exception {

        String name = "testOverrideVirtualManagedScheduledExecutorFromAnno";
        CompletableFuture<Thread> future = //
                        overriddenManagedScheduledExecutorFromAnno
                                        .completedFuture(name)
                                        .thenApplyAsync(testName -> {
                                            System.out.println("Task from " + testName);
                                            return Thread.currentThread();
                                        });

        Thread thread = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(false, isVirtual(thread));
    }

    /**
     * A managedScheduledExecutorService defined in server.xml with virtual=true runs
     * async tasks on platform threads instead of virtual threads when the
     * ThreadTypeOverride SPI instructs Liberty to avoid creating virtual threads.
     * The task runs successfully on Java SE 21+, but is rejected on Java SE 17.
     */
    @Test
    public void testOverrideVirtualManagedScheduledExecutorFromServerXML() //
                    throws Exception {

        String testName = "testOverrideVirtualManagedScheduledExecutorFromServerXML";
        Callable<Thread> task = () -> {
            System.out.println("Task from " + testName);
            return Thread.currentThread();
        };

        if (Runtime.version().feature() == 17)
            try {
                overriddenManagedScheduledExecutorFromServerXML
                                .submit(task);
                fail("Virtual=true should not be allowed on Java SE 17.");
            } catch (IllegalArgumentException x) {
                if (Runtime.version().feature() == 17)
                    return; // expected
                else
                    throw x;
            }
        else {
            Future<Thread> future = //
                            overriddenManagedScheduledExecutorFromServerXML
                                            .submit(task);

            Thread thread = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

            assertEquals(false, isVirtual(thread));
        }
    }

    /**
     * A managed-scheduled-executor defined in web.xml with virtual=true runs
     * async tasks on platform threads instead of virtual threads when the
     * ThreadTypeOverride SPI instructs Liberty to avoid creating virtual threads.
     * The task runs successfully.
     */
    @Test
    public void testOverrideVirtualManagedScheduledExecutorFromWebXML() //
                    throws Exception {

        String testName = "testOverrideVirtualManagedScheduledExecutorFromWebXML";
        Future<Thread> future = //
                        overriddenManagedScheduledExecutorFromWebXML
                                        .schedule(() -> {
                                            System.out.println("Task from " + testName);
                                            return Thread.currentThread();
                                        }, 250, TimeUnit.MILLISECONDS);

        Thread thread = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(false, isVirtual(thread));
    }

    /**
     * A ManagedThreadFactoryDefinition with virtual=true creates a
     * ManagedThreadFactory that creates platform threads instead of virtual threads
     * when the ThreadTypeOverride SPI instructs Liberty to avoid creating virtual
     * threads. The thread runs successfully.
     */
    @Test
    public void testOverrideVirtualThreadFactoryFromAnno() throws Exception {
        CompletableFuture<Thread> threadIsRunning = new CompletableFuture<>();

        Thread thread = overriddenThreadFactoryFromAnno.newThread(() -> {
            System.out.println("Thread from testOverrideVirtualThreadFactoryFromAnno");
            threadIsRunning.complete(Thread.currentThread());
        });

        thread.start();

        assertEquals(false, isVirtual(thread));

        assertEquals(thread, threadIsRunning.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * A managedThreadFactory defined in server.xml with virtual=true creates a
     * ManagedThreadFactory that creates platform threads instead of virtual threads
     * when the ThreadTypeOverride SPI instructs Liberty to avoid creating virtual
     * threads. The thread runs successfully.
     */
    @Test
    public void testOverrideVirtualThreadFactoryFromServerXML() throws Exception {
        CompletableFuture<Thread> threadIsRunning = new CompletableFuture<>();

        Runnable action = () -> {
            System.out.println("Thread from testOverrideVirtualThreadFactoryFromServerXML");
            threadIsRunning.complete(Thread.currentThread());
        };

        if (Runtime.version().feature() == 17) {
            try {
                Thread thread = overriddenThreadFactoryFromServerXML.newThread(action);
                fail("Server configuration should not allow virtual=true on Java 17," +
                     " which has no support for virtual threads.");
            } catch (UnsupportedOperationException x) {
                if (x.getMessage() != null &&
                    x.getMessage().startsWith("CWWKC1121E"))
                    return; // expected
                else
                    throw x;
            }
        } else {
            // Java 21+

            Thread thread = overriddenThreadFactoryFromServerXML.newThread(action);

            thread.start();

            assertEquals(false, isVirtual(thread));

            assertEquals(thread, threadIsRunning.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }
    }

    /**
     * A managed-thread-factory defined in web.xml with virtual=true creates a
     * ManagedThreadFactory that creates platform threads instead of virtual threads
     * when the ThreadTypeOverride SPI instructs Liberty to avoid creating virtual
     * threads. The thread runs successfully.
     */
    @Test
    public void testOverrideVirtualThreadFactoryFromWebXML() throws Exception {
        CompletableFuture<Thread> threadIsRunning = new CompletableFuture<>();

        Thread thread = overriddenThreadFactoryFromWebXML.newThread(() -> {
            System.out.println("Thread from testOverrideVirtualThreadFactoryFromWebXML");
            threadIsRunning.complete(Thread.currentThread());
        });

        thread.start();

        assertEquals(false, isVirtual(thread));

        assertEquals(thread, threadIsRunning.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }
}
