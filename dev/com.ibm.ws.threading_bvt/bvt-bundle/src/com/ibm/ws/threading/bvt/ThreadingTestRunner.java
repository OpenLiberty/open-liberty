/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.bvt;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;

@SuppressWarnings("serial")
public class ThreadingTestRunner extends HttpServlet {
    private HttpService http;

    public void setHttp(HttpService http) {
        this.http = http;
    }

    public void unsetHttp(HttpService http) {}

    private ScheduledExecutorService scheduledExecutorService;

    public void setScheduledExecutorService(ScheduledExecutorService executor) {
        this.scheduledExecutorService = executor;
    }

    public void unsetScheduledExecutorService(ScheduledExecutorService executor) {}

    private ScheduledExecutorService nondeferrableScheduledExecutorService;

    public void setNondeferrableScheduledExecutorService(ScheduledExecutorService executor) {
        this.nondeferrableScheduledExecutorService = executor;
    }

    public void unsetNondeferrableScheduledExecutorService(ScheduledExecutorService executor) {}

    private ScheduledExecutorService deferrableScheduledExecutorService;

    public void setDeferrableScheduledExecutorService(ScheduledExecutorService executor) {
        this.deferrableScheduledExecutorService = executor;
    }

    public void unsetDeferrableScheduledExecutorService(ScheduledExecutorService executor) {}

    public void activate() throws Exception {
        http.registerServlet("/com.ibm.ws.threading.bvt", this, null, null);

        // Output test.console indicator to allow the test client to proceed.
        System.out.println("ThreadingTestRunner has completed activation.");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String test = req.getParameter("test");
        System.out.println(test);
        try {
            getClass().getMethod(test).invoke(this);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new ServletException(t);
        }
    }

    private static class CurrentTimeMillisCallable implements Callable<Long> {
        private final String name;

        CurrentTimeMillisCallable(String name) {
            this.name = name;
        }

        @Override
        public Long call() {
            long now = System.currentTimeMillis();
            System.out.println(name + ": called at " + now);
            return now;
        }
    }

    private static class ScheduledExecutorServiceTester {
        private final ScheduledExecutorService executor;
        private final boolean deferrable;

        private final Future<Long> immediateFuture;
        private final Future<Long> delayedFuture;

        ScheduledExecutorServiceTester(String name, ScheduledExecutorService executor, boolean deferrable) {
            this.executor = executor;
            this.deferrable = deferrable;
            immediateFuture = executor.schedule(new CurrentTimeMillisCallable(name + " immediate"), 0, TimeUnit.SECONDS);
            delayedFuture = executor.schedule(new CurrentTimeMillisCallable(name + " delayed"), 5, TimeUnit.SECONDS);
        }

        public void test() throws InterruptedException, ExecutionException {
            long immediate = immediateFuture.get();
            long delayed = delayedFuture.get();

            // The execution was deferred if both tasks ran at "the same" time or
            // if the second was clearly deferred.
            long diff = delayed - immediate;
            boolean deferred = diff < 1000 || diff > 10000;

            if (deferrable != deferred) {
                throw new IllegalStateException("executor=" + executor + ", immediate=" + immediate + ", delayed=" + delayed + ", diff=" + diff);
            }
        }
    }

    public void testScheduledExecutorService() throws Exception {
        // Schedule all at once to run the tests in parallel.
        ScheduledExecutorServiceTester tester = new ScheduledExecutorServiceTester("default", scheduledExecutorService, false);
        ScheduledExecutorServiceTester nondeferrableTester = new ScheduledExecutorServiceTester("nondeferrable", nondeferrableScheduledExecutorService, false);
        ScheduledExecutorServiceTester deferrableTester = new ScheduledExecutorServiceTester("deferrable", deferrableScheduledExecutorService, true);

        tester.test();
        nondeferrableTester.test();
        deferrableTester.test();
    }
}
