/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package asyncEventsApp.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/asyncevents")
public class AsyncEventsServlet extends FATServlet {

    @Inject
    Event<CakeArrival> cakeEvent;

    private static final long serialVersionUID = 8549700799591343964L;

    /**
     * Test the javax.enterprise.event.Event.fire() method.
     *
     * This test exercises existing CDI synchronous event handling behaviour. The method fires a Synchronous event which is observed by
     * the syncCakeObserver. We confirm that the event has been handled.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSyncObserver() throws Exception {

        long myTid = Thread.currentThread().getId();
        CakeArrival newCake = new CakeArrival();
        cakeEvent.fire(newCake);

        List<CakeReport> cakeReportList = newCake.getCakeReports();
        CakeReport cakeReport = null;
        if (!cakeReportList.isEmpty()) {
            if (cakeReportList.size() == 1)
                cakeReport = cakeReportList.get(0);
            else
                fail("Unexpected number of cake reports - " + cakeReportList.size());
        }

        assertNotNull("No cake report from sync observer", cakeReport);
        assertTrue("Unexpected cake observer - " + cakeReport.getCakeObserver(), cakeReport.getCakeObserver().equals("syncCakeObserver"));
    }

    /**
     * Test the javax.enterprise.event.Event.fireAsync() introduced in CDI2.0.
     *
     * This method fires an Async event which is observed by the asyncCakeObserver. We confirm that the event has been
     * handled and that the handling was done on a different thread.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testAsyncObserver() throws Exception {
        long myTid = Thread.currentThread().getId();
        CakeArrival newCake = new CakeArrival();
        CompletionStage<CakeArrival> stage = cakeEvent.fireAsync(newCake);
        CompletableFuture<CakeArrival> future = stage.toCompletableFuture();

        // Set a (very) large timeout to be sure that something is wrong as opposed to slow
        CakeArrival futureCake = future.get(60000, TimeUnit.MILLISECONDS);

        List<CakeReport> cakeReportList = futureCake.getCakeReports();
        CakeReport cakeReport = null;
        if (!cakeReportList.isEmpty()) {
            if (cakeReportList.size() == 1)
                cakeReport = cakeReportList.get(0);
            else
                fail("Unexpected number of cake reports - " + cakeReportList.size());
        }

        assertNotNull("No cake report from async observer", cakeReport);
        assertTrue("Unexpected cake observer - " + cakeReport.getCakeObserver(), cakeReport.getCakeObserver().equals("asyncCakeObserver"));
        assertFalse("async thread id is not different", myTid == cakeReport.getTid());
    }

    /**
     * Test use of Liberty ScheduledExecutorService
     *
     * This invocation of fireAsync will throw an exception if no Liberty ScheduledExecutorService is set.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testGetTimerExecutor() throws Exception {
        CakeArrival newCake = new CakeArrival();
        cakeEvent.fireAsync(newCake, NotificationOptions.of("weld.async.notification.timeout", 1000));
    }
}