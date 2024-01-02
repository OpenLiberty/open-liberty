/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package test.concurrency.schedasync.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class SchedAsyncTestServlet extends FATServlet {
    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Inject
    private SchedAsyncAppScopedBean bean;

    private static CompletableFuture<Integer> cfEveryFiveSeconds3Times;

    /**
     * Nanoseconds at which the init method was invoked on this servlet.
     */
    private static long init_ns;

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        init_ns = System.nanoTime();

        cfEveryFiveSeconds3Times = bean.everyFiveSeconds(new AtomicInteger(3));
    }

    /**
     * An asynchronous methods that is scheduled to run every 5 seconds for 3 executions
     * and then complete must run exactly 3 times.
     */
    @Test
    public void testEveryFiveSeconds3Times() throws Exception {
        assertEquals(Integer.valueOf(0), cfEveryFiveSeconds3Times.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        long elapsed = System.nanoTime() - init_ns;
        if (elapsed < TimeUnit.SECONDS.toNanos(10L))
            fail("A task that runs every 5 seconds must not complete 3 executions in under 10 seconds. Elapsed nanoseconds: " + elapsed);
    }

}
