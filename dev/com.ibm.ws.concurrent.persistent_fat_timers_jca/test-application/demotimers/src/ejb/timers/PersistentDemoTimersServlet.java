/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb.timers;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class PersistentDemoTimersServlet extends FATServlet {
    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;
    private static final int RUN_COUNT = 2;
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(RUN_COUNT + 1);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @EJB
    private AutomaticDatabase autoTimerDatabase;

    @EJB
    private ScheduledPurge autoTimerPurge;

    @Test
    public void testTimerDatabase() throws InterruptedException, ExecutionException {
        Future<Boolean> databaseFuture = executor.submit(() -> {
            return testRepeatingAutomaticPersistentTimerDatabase();
        });

        Future<Boolean> purgeFuture = executor.submit(() -> {
            return testRepeatingAutomaticPersistentTimerPurge();
        });

        assertTrue("Purge Timer did not return true", purgeFuture.get());
        assertTrue("Database Timer did not return true", databaseFuture.get());
    }

    /**
     * Verify that an automatic persistent timer is running multiple times
     * This timer runs every 30 seconds
     */
    public boolean testRepeatingAutomaticPersistentTimerDatabase() throws Exception {
        boolean passed = false;
        try {
            int count = autoTimerDatabase.getRunCount();
            for (long start = System.nanoTime(); count < Integer.MAX_VALUE && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                count = autoTimerDatabase.getRunCount();

            if (count < RUN_COUNT)
                throw new Exception("Expecting EJB timer " + autoTimerDatabase.getClass().getName() + " to run at least " + RUN_COUNT + " times. Instead count was: " + count);

            passed = true;
        } finally {
            autoTimerDatabase.cancel();
        }
        return passed;
    }

    /**
     * Verify that an automatic persistent purge is running multiple times
     * This timer runs every minute
     */
    public boolean testRepeatingAutomaticPersistentTimerPurge() throws Exception {
        boolean passed = false;
        try {
            int count = autoTimerPurge.getRunCount();
            for (long start = System.nanoTime(); count < RUN_COUNT && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                count = autoTimerPurge.getRunCount();

            if (count < RUN_COUNT)
                throw new Exception("Expecting EJB timer " + autoTimerPurge.getClass().getName() + " to run at least " + RUN_COUNT + " times. Instead count was: " + count);

            passed = true;
        } finally {
            autoTimerPurge.cancel();
        }
        return passed;
    }
}
