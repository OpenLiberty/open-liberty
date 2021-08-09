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

import java.util.concurrent.TimeUnit;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet("/*")
public class PersistentDemoTimersServlet extends FATServlet {
    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;

    @EJB
    private AutomaticDatabase autoTimerDatabase;

    @EJB
    private AutomaticMemory autoTimerMemory;

    @EJB
    private AutomaticIO autoTimerIO;

    /**
     * Verify that an automatic persistent timer is running multiple times
     * This timer runs every other second
     */
    @Test
    public void testRepeatingAutomaticPersistentTimerMemory() throws Exception {
        final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(10);

        int count = autoTimerMemory.getRunCount();
        for (long start = System.nanoTime(); count < 3 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = autoTimerMemory.getRunCount();

        if (count < 3)
            throw new Exception("Expecting EJB timer to run at least 3 times. Instead count was: " + count);

        autoTimerMemory.cancel();
    }

    /**
     * Verify that an automatic persistent timer is running multiple times
     * This timer runs every 30 seconds
     */
    @Test
    @Mode(TestMode.FULL)
    public void testRepeatingAutomaticPersistentTimerDatabase() throws Exception {
        final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

        int count = autoTimerDatabase.getRunCount();
        for (long start = System.nanoTime(); count < 3 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = autoTimerDatabase.getRunCount();

        if (count < 3)
            throw new Exception("Expecting EJB timer to run at least 3 times. Instead count was: " + count);

        autoTimerDatabase.cancel();
    }

    /**
     * Verify that an automatic persistent timer is running multiple times
     * This timer runs every minute
     */
    @Test
    @Mode(TestMode.FULL)
    public void testRepeatingAutomaticPersistentTimerIO() throws Exception {
        final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(4);

        int count = autoTimerIO.getRunCount();
        for (long start = System.nanoTime(); count < 3 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL)) {
            count = autoTimerIO.getRunCount();
        }

        if (count < 3)
            throw new Exception("Expecting EJB timer to run at least 3 times. Instead count was: " + count);

        autoTimerIO.cancel();
    }
}
