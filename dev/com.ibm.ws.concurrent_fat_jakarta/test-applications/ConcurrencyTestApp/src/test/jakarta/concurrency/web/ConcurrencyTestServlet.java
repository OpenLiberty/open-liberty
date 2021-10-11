/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.concurrency.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrencyTestServlet extends FATServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(name = "java:module/env/concurrent/threadFactoryRef")
    ManagedThreadFactory defaultThreadFactory;

    // TODO @Resource(lookup = "java:comp/DefaultManagedThreadFactory")
    ForkJoinWorkerThreadFactory forkJoinThreadFactory;

    @Override
    public void init() throws ServletException {
        // TODO remove this temporary code once the 3.0 ManagedThreadFactory class
        // is available which implements ForkJoinWorkerThreadFactory
        forkJoinThreadFactory = new ForkJoinWorkerThreadFactory() {
            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                try {
                    java.lang.reflect.Method newThread = defaultThreadFactory.getClass()
                                    .getMethod("newThread", ForkJoinPool.class);
                    newThread.setAccessible(true);
                    return (ForkJoinWorkerThread) newThread.invoke(defaultThreadFactory, pool);
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        };
    }

    /**
     * Verify that a parallel stream can run on a ForkJoinPool that uses a ManagedThreadFactory
     * to create its ForkJoinWorkerThreads, and that those threads run with the application
     * component context of the the application that looked up or injected the ManagedThreadFactory.
     * Verify this by attempting a resource reference lookup from the parallel stream operations.
     */
    @Test
    public void testParallelStreamRunsOnManagedThreadFactory() throws Exception {
        String curThreadName = Thread.currentThread().getName();
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

        ForkJoinPool pool = new ForkJoinPool(3, forkJoinThreadFactory, null, false);
        try {
            pool.submit(() -> {
                Arrays.asList(1, 2, 3).parallelStream().forEach(i -> {
                    try {
                        // Perform a resource reference lookup to demonstrate that the
                        // application component's context is established on the ForkJoinWorkerThread,
                        Object lookedUp = InitialContext.doLookup("java:module/env/concurrent/threadFactoryRef");
                        results.add(Thread.currentThread().getName() + " (" + i + ") " + lookedUp);
                    } catch (NamingException x) {
                        results.add(x);
                    }
                });
            });

            Object result;
            assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (result instanceof Exception) {
                throw new AssertionError("Failure on parallel stream thread", (Exception) result);
            } else {
                String s = (String) result;
                assertTrue("Current: " + curThreadName + " vs " + s, !s.startsWith(curThreadName));
                assertTrue(s, s.contains(" (1) ") || s.contains(" (2) ") || s.contains(" (3) "));
            }

            assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (result instanceof Exception) {
                throw new AssertionError("Failure on parallel stream thread", (Exception) result);
            } else {
                String s = (String) result;
                assertTrue("Current: " + curThreadName + " vs " + s, !s.startsWith(curThreadName));
                assertTrue(s, s.contains(" (1) ") || s.contains(" (2) ") || s.contains(" (3) "));
            }

            assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            if (result instanceof Exception) {
                throw new AssertionError("Failure on parallel stream thread", (Exception) result);
            } else {
                String s = (String) result;
                assertTrue("Current: " + curThreadName + " vs " + s, !s.startsWith(curThreadName));
                assertTrue(s, s.contains(" (1) ") || s.contains(" (2) ") || s.contains(" (3) "));
            }
        } finally {
            pool.shutdown();
        }
    }
}
