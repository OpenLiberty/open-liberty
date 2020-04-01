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

package com.ibm.ws.ejbcontainer.timer.persistent.restart.missed.web;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.timer.persistent.restart.missed.ejb.RestartMissedTimerAction;

import componenttest.app.FATServlet;

@WebServlet("/RestartMissedTimerActionServlet")
@SuppressWarnings("serial")
public class RestartMissedTimerActionServlet extends FATServlet {

    private static final Logger logger = Logger.getLogger(RestartMissedTimerActionServlet.class.getName());

    @EJB
    RestartMissedTimerAction timerBean;

    /**
     * Create an interval timer in preparation for running a test that will restart the server.
     */
    public void createIntervalTimer() throws Exception {
        logger.info("createIntervalTimer: creating interval timer");
        timerBean.createIntervalTimer(false);
    }

    /**
     * Create an interval timer in preparation for running a test that will restart the server
     * and wait for the time to expire and run once time.
     */
    public void createIntervalTimerAndWaitForExpiration() throws Exception {
        logger.info("createIntervalTimer: creating interval timer");
        timerBean.createIntervalTimer(true);
    }

    /**
     * Cancel all timers in preparation (or cleanup) for running a test that will restart the server.
     */
    public void cancelAllTimers() throws Exception {
        logger.info("cancelAllTimers: cancelling all timers");
        timerBean.cancelAllTimers();
    }

    /**
     * Test Persistent Timer missed action default behavior across server restart when failover has not been enabled.
     * The default behavior without failover should be ALL. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a server restart that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    public void testMissedTimerActionDefaultNoFailoverRestart() throws Exception {
        // Default when no failover is ALL
        testMissedTimerAction("testMissedTimerActionDefaultNoFailoverRestart", "ALL");
    }

    /**
     * Test Persistent Timer missed action default behavior across server restart when failover has been enabled.
     * The default behavior with failover should be ONCE. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a server restart.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    public void testMissedTimerActionDefaultWithFailoverRestart() throws Exception {
        // Default when failover is enabled is ONCE
        testMissedTimerAction("testMissedTimerActionDefaultWithFailoverRestart", "ONCE");
    }

    /**
     * Test Persistent Timer missed action "ALL" behavior across server restart when failover has been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a server restart that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    public void testMissedTimerActionAllWithFailoverRestart() throws Exception {
        testMissedTimerAction("testMissedTimerActionAllWithFailoverRestart", "ALL");
    }

    /**
     * Test Persistent Timer missed action "ONCE" behavior across server restart when failover has not been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a server restart.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    public void testMissedTimerActionOnceNoFailoverRestart() throws Exception {
        testMissedTimerAction("testMissedTimerActionOnceNoFailoverRestart", "ONCE");
    }

    private void testMissedTimerAction(String testName, String missedTimerAction) throws Exception {
        try {
            logger.info(testName + ": verifying timer results");
            timerBean.verifyMissedTimerAction(missedTimerAction);
        } catch (EJBException ejbex) {
            // Unwrap any junit assertion errors to make it clearer what failed.
            Throwable rootex = ejbex.getCause();
            if (rootex instanceof Exception && rootex.getCause() != null) {
                rootex = rootex.getCause();
            }
            if (rootex instanceof AssertionError) {
                throw (AssertionError) rootex;
            }
            throw ejbex;
        } finally {
            timerBean.cancelAllTimers();
        }
    }

}
