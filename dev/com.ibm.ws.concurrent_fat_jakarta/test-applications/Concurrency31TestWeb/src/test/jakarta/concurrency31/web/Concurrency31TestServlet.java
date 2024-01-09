/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.concurrency31.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.context.list.ListContext;
import test.context.location.ZipCode;
import test.context.timing.Timestamp;

@SuppressWarnings("serial")
@WebServlet("/*")
public class Concurrency31TestServlet extends FATServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(name = "java:module/concurrent/virtual-thread-factory", lookup = "concurrent/temp-virtual-thread-factory")
    ManagedThreadFactory tempThreadFactory;

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    /**
     * TODO Use ManagedThreadFactoryDefinition with virtual=true to request virtual threads once implemented.
     * It can also define custom context to propagate to the managed virtual thread.
     */
    @Test
    public void testVirtualThreadFactory() throws Exception {
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();
        Runnable action = () -> {
            try {
                results.add(InitialContext.doLookup("java:module/concurrent/virtual-thread-factory"));
            } catch (Throwable x) {
                results.add(x);
            }
        };

        ManagedThreadFactory threadFactory = InitialContext.doLookup("java:module/concurrent/virtual-thread-factory");
        Thread thread1 = threadFactory.newThread(action);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread1));
        thread1.start();

        Thread thread2 = threadFactory.newThread(action);
        assertEquals(Boolean.TRUE, Thread.class.getMethod("isVirtual").invoke(thread2));
        thread2.start();

        Object result;
        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on one of the virtual threads.", (Throwable) result);

        assertNotNull(result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (result instanceof Throwable)
            throw new AssertionError("An error occurred on the other virtual thread.", (Throwable) result);
    }
}
