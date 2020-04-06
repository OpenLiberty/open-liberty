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

package com.ibm.ws.ejbcontainer.timer.persistent.missed.web;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.timer.persistent.missed.ejb.MissedTimerAction;

import componenttest.app.FATServlet;

@WebServlet("/MissedTimerActionServlet")
@SuppressWarnings("serial")
public class MissedTimerActionServlet extends FATServlet {

    private static final Logger svLogger = Logger.getLogger(MissedTimerActionServlet.class.getName());

    @EJB
    MissedTimerAction timerBean;

    /**
     * Test Persistent Timer missed action default behavior when failover has not been enabled.
     * The default behavior without failover should be ALL. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    public void testMissedTimerActionDefaultNoFailover() throws Exception {
        // Default when no failover is ALL
        testMissedTimerAction("testMissedTimerActionDefaultNoFailover", "ALL");
    }

    /**
     * Test Persistent Timer missed action default behavior when failover has been enabled.
     * The default behavior with failover should be ONCE. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    public void testMissedTimerActionDefaultWithFailover() throws Exception {
        // Default when failover is enabled is ONCE
        testMissedTimerAction("testMissedTimerActionDefaultWithFailover", "ONCE");
    }

    /**
     * Test Persistent Timer missed action "ALL" behavior when failover has not been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    public void testMissedTimerActionAllNoFailover() throws Exception {
        testMissedTimerAction("testMissedTimerActionAllNoFailover", "ALL");
    }

    /**
     * Test Persistent Timer missed action "ALL" behavior when failover has been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    public void testMissedTimerActionAllWithFailover() throws Exception {
        testMissedTimerAction("testMissedTimerActionAllWithFailover", "ALL");
    }

    /**
     * Test Persistent Timer missed action "ONCE" behavior when failover has not been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    public void testMissedTimerActionOnceNoFailover() throws Exception {
        testMissedTimerAction("testMissedTimerActionOnceNoFailover", "ONCE");
    }

    /**
     * Test Persistent Timer missed action "ONCE" behavior when failover has been enabled. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    public void testMissedTimerActionOnceWithFailover() throws Exception {
        testMissedTimerAction("testMissedTimerActionOnceWithFailover", "ONCE");
    }

    /**
     * Test Persistent Timer missed action "Once" (mixed case) behavior when failover has not been enabled.
     * The value is case insensitive and will be treated as "ONCE". <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will skip expirations missed because of a delay.
     * <li> Timer.getNextTimeout() will return values in the future; skipping missed expirations.
     * </ol>
     */
    public void testMissedTimerActionMixedCaseNoFailover() throws Exception {
        testMissedTimerAction("testMissedTimerActionMixedCaseNoFailover", "ONCE");
    }

    /**
     * Test Persistent Timer missed action "Blah" (bad value) behavior when failover has not been enabled.
     * A warning will be logged, and the value will be treated as unspecified, so default to "ALL". <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Interval timer will run all expirations despite a delay that causes several to be missed.
     * <li> Timer.getNextTimeout() will return values in the past for missed expirations.
     * </ol>
     */
    public void testMissedTimerActionBadValueNoFailover() throws Exception {
        testMissedTimerAction("testMissedTimerActionBadValueNoFailover", "ALL");
    }

    private void testMissedTimerAction(String testName, String missedTimerAction) throws Exception {
        svLogger.info(testName + ": creating interval timer");
        timerBean.createIntervalTimer();

        try {
            svLogger.info(testName + ": verifying timer results");
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
