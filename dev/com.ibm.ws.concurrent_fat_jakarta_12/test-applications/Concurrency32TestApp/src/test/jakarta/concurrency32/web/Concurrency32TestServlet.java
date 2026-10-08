/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.concurrency32.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;
import test.context.locale.LocaleContext;

@ManagedScheduledExecutorDefinition
/**/ (name = "java:module/concurrent/custom-context-scheduler",
      context = "java:app/concurrent/appdd/locale-context-only",
      maxAsync = 2,
      virtual = false)
@SuppressWarnings("serial")
@WebServlet("/*")
public class Concurrency32TestServlet extends FATServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    // Futures to cancel between tests, to reduce the chance of failures in
    // one test method interfering with others
    private final Set<Future<?>> cancelAfterTest = Collections //
                    .newSetFromMap(new ConcurrentHashMap<Future<?>, Boolean>());

    @Resource(lookup = "java:module/concurrent/custom-context-scheduler")
    ManagedScheduledExecutorService customContextScheduler;

    /**
     * Cancel futures that are still incomplete when tests methods end, in order to prevent long-running tasks
     * and queued tasks from interfering with subsequent tests.
     * Ideally, there should never be any futures left incomplete after successful execution of a test method,
     * and this should only be needed for when test methods fail and cannot easily clean up.
     */
    @After
    void cancelFuturesAfterTest() {
        int count = 0;
        for (Iterator<Future<?>> it = cancelAfterTest.iterator(); it.hasNext();) {
            Future<?> f = it.next();
            if (!f.isDone() && f.cancel(true))
                count++;
        }
        if (count > 0)
            System.out.println("Canceled " + count + " futures after previous test");
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    /**
     * Verify that context is propagated or cleared per the context-service
     * definition in application.xml.
     */
    @Test
    public void testTasksRunWithContext() throws Exception {
        assertEquals("value1",
                     InitialContext.doLookup("java:comp/env/TestEntry"));

        ScheduledFuture<Locale> future1;

        // Custom context Locale must be propagated
        LocaleContext.set(Locale.GERMAN);
        try {
            future1 = customContextScheduler.schedule(LocaleContext::get,
                                                      100,
                                                      TimeUnit.MILLISECONDS);
            cancelAfterTest.add(future1);
        } finally {
            LocaleContext.remove();
        }

        // Application context must be cleared, causing the java:comp lookup to fail
        Future<String> future2 = //
                        customContextScheduler.submit(() //
                        -> InitialContext.doLookup("java:comp/env/TestEntry"));
        cancelAfterTest.add(future2);

        assertEquals(Locale.GERMAN,
                     future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        try {
            String value = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Application context should be cleared. Instead found: " + value);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NamingException)
                ; // expected
            else
                throw x;
        }
    }

}
