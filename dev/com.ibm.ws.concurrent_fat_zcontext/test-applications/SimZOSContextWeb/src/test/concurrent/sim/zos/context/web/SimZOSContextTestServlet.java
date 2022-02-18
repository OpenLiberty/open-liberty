/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.sim.zos.context.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;

@ContextServiceDefinition(name = "java:app/concurrent/ThreadNameContext",
                          propagated = { "SyncToOSThread", SECURITY, APPLICATION })

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
}
