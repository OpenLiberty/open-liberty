/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.concurrent.sim.zos.context.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.concurrent.cache.TestCache;
import test.concurrent.sim.context.zos.wlm.Enclave;

@ContextServiceDefinition(name = "java:app/concurrent/ThreadNameContext",
                          propagated = { "SyncToOSThread", SECURITY, APPLICATION })

@ContextServiceDefinition(name = "java:module/concurrent/zosWLMContext",
                          propagated = { "Classification" },
                          cleared = { TRANSACTION, "SyncToOSThread", SECURITY },
                          unchanged = ALL_REMAINING)

@SuppressWarnings("serial")
@WebServlet("/*")
public class SimZOSContextTestServlet extends FATServlet {

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
     * Configure to clear SyncToOSThread context and verify that the fake
     * context type that we are using to simulate it is cleared from the thread.
     */
    @Test
    public void testClearSimulatedSyncToOSThreadContext() throws Exception {
        // Instead of testing the real SyncToOSThread context behavior,
        // the fake context provider defaults/propagates the thread name.
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/zosWLMContext");

        String originalName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("testClearSimulatedSyncToOSThreadContext");

            Supplier<String> threadNameSupplier = contextSvc.contextualSupplier(() -> Thread.currentThread().getName());

            assertEquals("Unnamed Thread", threadNameSupplier.get());

            assertEquals("testClearSimulatedSyncToOSThreadContext", Thread.currentThread().getName());
        } finally {
            Thread.currentThread().setName(originalName);
        }
    }

    /**
     * Configure to clear the fake z/OS WLM context and verify that it is cleared from the thread.
     */
    @Test
    public void testClearSimulatedZOSWLMContext() throws Exception {
        // Instead of testing the real z/OS WLM context behavior,
        // the fake context provider updates the state of a mock Enclave class.
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/ThreadNameContext");

        String originalName = Thread.currentThread().getName();
        try {
            Enclave.setTransactionClass("TX_CLASS_1");

            Supplier<String> txClassSupplier = contextSvc.contextualSupplier(Enclave::getTransactionClass);

            assertEquals("ASYNCBN", txClassSupplier.get());

            assertEquals("TX_CLASS_1", Enclave.getTransactionClass());
        } finally {
            Enclave.clear();
        }
    }

    /**
     * First part of test case for deactivate of a managedThreadFactory implicitly interrupting
     * a managed ForkJoinWorkerThread that the managed thread factory created.
     * This part of the test case starts the thread and ensures it is running.
     */
    public void testInterruptOnDeactivate_threadStart() throws Exception {
        ManagedThreadFactory threadFactory = InitialContext.doLookup("concurrent/testInterruptOnDeactivate-threadFactory");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(1);

        ForkJoinPool pool = new ForkJoinPool(2, threadFactory, null, false);
        pool.submit(() -> {
            ForkJoinTask<Integer> task = pool.submit(new RecursiveTask<Integer>() {
                @Override
                protected Integer compute() {
                    running.countDown();
                    try {
                        return blocker.await(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS) ? 1 : 0;
                    } catch (InterruptedException x) {
                        // expected
                        return 100;
                    }
                }
            });
            TestCache.instance.put("testInterruptOnDeactivate-pool", pool);
            TestCache.instance.put("testInterruptOnDeactivate-task", task);
        });

        assertTrue(running.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Second part of test case for deactivate of a managedThreadFactory implicitly interrupting
     * a managed ForkJoinWorkerThread that the managed thread factory created.
     * This part of the test case waits for the thread to be interrupted.
     */
    public void testInterruptOnDeactivate_waitForInterrupt() throws Exception {
        @SuppressWarnings("unchecked")
        ForkJoinTask<Integer> task = (ForkJoinTask<Integer>) TestCache.instance.get("testInterruptOnDeactivate-task");
        ForkJoinPool pool = (ForkJoinPool) TestCache.instance.get("testInterruptOnDeactivate-pool");

        try {
            assertEquals(Integer.valueOf(100), task.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            pool.shutdown();
        }
    }

    /**
     * Configure to propagate SyncToOSThread context and verify that the fake
     * context type that we are using to simulate it propagates to the thread.
     */
    @Test
    public void testPropagateSimulatedSyncToOSThreadContext() throws Exception {
        // Instead of testing the real SyncToOSThread context behavior,
        // the fake context provider propagates the thread name.
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/ThreadNameContext");

        String originalName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("testPropagateSimulatedSyncToOSThreadContext-A");

            Supplier<String> threadNameSupplier = contextSvc.contextualSupplier(() -> Thread.currentThread().getName());

            Thread.currentThread().setName("testPropagateSimulatedSyncToOSThreadContext-B");

            assertEquals("testPropagateSimulatedSyncToOSThreadContext-A", threadNameSupplier.get());

            assertEquals("testPropagateSimulatedSyncToOSThreadContext-B", Thread.currentThread().getName());

            Future<String> future = unmanagedThreads.submit(threadNameSupplier::get);
            assertEquals("testPropagateSimulatedSyncToOSThreadContext-A", future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            Thread.currentThread().setName(originalName);
        }
    }

    /**
     * Configure to propagate zosWLMContext and verify that the fake
     * context type that we are using to simulate it propagates to the thread.
     */
    @Test
    public void testPropagateSimulatedZOSWLMContext() throws Exception {
        // Instead of testing the real z/OS WLM context behavior,
        // the fake context provider updates the state of a mock Enclave class.
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/zosWLMContext");

        String originalName = Thread.currentThread().getName();
        try {
            Enclave.setTransactionClass("TX_CLASS_A");

            Supplier<String> txClassSupplier = contextSvc.contextualSupplier(Enclave::getTransactionClass);

            Enclave.setTransactionClass("TX_CLASS_B");

            assertEquals("TX_CLASS_A", txClassSupplier.get());

            assertEquals("TX_CLASS_B", Enclave.getTransactionClass());

            // Propagate the absence of context:

            Enclave.clear();

            txClassSupplier = contextSvc.contextualSupplier(Enclave::getTransactionClass);

            Enclave.setTransactionClass("TX_CLASS_C");

            assertEquals(null, txClassSupplier.get());

            assertEquals("TX_CLASS_C", Enclave.getTransactionClass());

            // Long running task:

            @SuppressWarnings("unchecked")
            Supplier<String> longRunningTxSupplier = contextSvc.createContextualProxy(Enclave::getTransactionClass,
                                                                                      Collections.singletonMap(ManagedTask.LONGRUNNING_HINT, "true"),
                                                                                      Supplier.class);
            assertEquals("ASYNCDMN", longRunningTxSupplier.get());

            assertEquals("TX_CLASS_C", Enclave.getTransactionClass());
        } finally {
            Enclave.clear();
        }
    }

    /**
     * Configure vendor properties for PropagateOrNew and with different default transaction
     * classes for zosWLMContext and verify that the fake context type that we are using to simulate it
     * propagates or creates the new context on the thread.
     */
    // TODO If the spec ever adds a way for vendor properties to be supplied to context types, this test could help provide coverage.
    // @Test
    public void testVendorPropertiesSimulatedZOSWLMContext() throws Exception {
        // Instead of testing the real z/OS WLM context behavior,
        // the fake context provider updates the state of a mock Enclave class.
        ContextService contextSvc = InitialContext.doLookup("java:comp/concurrent/zosWLMContextPropagateOrNew");

        String originalName = Thread.currentThread().getName();
        try {
            Enclave.setTransactionClass("TX_CLASS_D");

            Supplier<String> txClassSupplier = contextSvc.contextualSupplier(Enclave::getTransactionClass);

            Enclave.setTransactionClass("TX_CLASS_E");

            assertEquals("TX_CLASS_D", txClassSupplier.get());

            assertEquals("TX_CLASS_E", Enclave.getTransactionClass());

            // Propagate the absence of context:

            Enclave.clear();

            txClassSupplier = contextSvc.contextualSupplier(Enclave::getTransactionClass);

            Enclave.setTransactionClass("TX_CLASS_F");

            assertEquals("DEFAULT_TX", txClassSupplier.get());

            assertEquals("TX_CLASS_F", Enclave.getTransactionClass());

            // Long running task:

            @SuppressWarnings("unchecked")
            Supplier<String> longRunningTxSupplier = contextSvc.createContextualProxy(Enclave::getTransactionClass,
                                                                                      Collections.singletonMap(ManagedTask.LONGRUNNING_HINT, "true"),
                                                                                      Supplier.class);
            assertEquals("DAEMON_TX", longRunningTxSupplier.get());

            assertEquals("TX_CLASS_F", Enclave.getTransactionClass());
        } finally {
            Enclave.clear();
        }
    }
}
