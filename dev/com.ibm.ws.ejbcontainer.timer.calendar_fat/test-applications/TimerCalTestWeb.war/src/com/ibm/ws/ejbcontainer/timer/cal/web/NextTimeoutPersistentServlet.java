/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.cal.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.cal.ejb.NextTimeoutPersistIntf;

import componenttest.custom.junit.runner.Mode;

@WebServlet("/NextTimeoutPersistentServlet")
@SuppressWarnings("serial")
public class NextTimeoutPersistentServlet extends AbstractServlet {

    private static final long MAX_WAIT_TIME = 4 * 60 * 1000;

    @EJB
    protected NextTimeoutPersistIntf ivBean;

    private final static String CLASSNAME = NextTimeoutPersistentServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @Override
    public void cleanup() throws Exception {
        if (ivBean != null) {
            ivBean.clearAllTimers();
        }
    }

    @Test
    public void testYearPersistent_NextTimeout() {

        try {
            ivBean.testYear();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testMonthPersistent_NextTimeout() {

        try {
            ivBean.testMonth();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testDayOfMonthPersistent_NextTimeout() {

        try {
            ivBean.testDayOfMonth();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testDayOfWeekPersistent_NextTimeout() {

        try {
            ivBean.testDayOfWeek();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testHourPersistent_NextTimeout() {

        try {
            ivBean.testHour();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testMinutePersistent_NextTimeout() {

        try {
            ivBean.testMinute();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testSecondPersistent_NextTimeout() {

        try {
            ivBean.testSecond();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testTimezonePersistent_NextTimeout() {

        try {

            // Try with timer running on either side of Central timezone
            ivBean.testTimezone("America/Dawson"); // UTC-8
            ivBean.testTimezone("America/Halifax"); // UTC-4

            // Try with timer running on other side of the prime meridian
            ivBean.testTimezone("Europe/Vienna");

            // Try with timer running in an off-hour timezone near Central timezone
            ivBean.testTimezone("America/Caracas"); // UTC-5:30

            // Try with timer running on other side of the prime meridian, AND an off-hour zone
            ivBean.testTimezone("Asia/Kabul"); // UTC+4:30

            ivBean.testTimezone("Asia/Kolkata"); // UTC+5:30

        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testStartGEendPersistent_NextTimeout() {

        try {
            ivBean.testStartGEend();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testStartLTendPersistent_NextTimeout() {

        try {
            ivBean.testStartLTend();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testStartPersistent_NextTimeout() {

        try {
            ivBean.testStart();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testGetSchedulePersistent_NextTimeout() {

        try {
            ivBean.testGetSchedule();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testGetNextTimeoutWithActualExpirationPersistent() {

        Date scheduledEnd = new Date(System.currentTimeMillis() + 2 * 60 * 1000); // 2 minutes
        ScheduleExpression se = new ScheduleExpression();
        se.hour("*");
        se.minute("*");
        se.end(scheduledEnd);

        svLogger.info("Creating timer with scheduled end = " + scheduledEnd);
        String info = "verify last timeout with minute(*) and end 2 min later";
        Date firstTimeout = ivBean.createCalTimer(se, info); // (non-persistent)

        long actualEnd = roundToNextSecond(scheduledEnd); // rounded up to next second
        long nextExpectedTimeout = firstTimeout.getTime() + 60 * 1000;
        long numExpectedTimeouts = ((actualEnd - firstTimeout.getTime()) / (60 * 1000)) + 1;

        svLogger.info("First timeout = " + firstTimeout + ", expected timeouts = " + numExpectedTimeouts);

        while (numExpectedTimeouts > 0) {

            svLogger.info("waiting for timer expiration; up to " + MAX_WAIT_TIME + "ms");
            ivBean.waitForTimer(MAX_WAIT_TIME);

            Date nextTimeout = ivBean.getNextTimeoutFromExpiration();
            String failure = ivBean.getNextTimeoutFailureFromExpiration();

            svLogger.info(numExpectedTimeouts + ": nextTimeout=" + nextTimeout + ", failure=" + failure);

            if (numExpectedTimeouts > 1) {
                assertEquals("timer.getNextTimeout returned unexpected value", nextExpectedTimeout, nextTimeout.getTime());
                assertNull("timer.getNextTimeout() threw unexpected exception", failure);
                nextExpectedTimeout += 60 * 1000;
            } else {
                assertNull("timer.getNextTimeout returned unexpected value", nextTimeout);
                assertEquals("timer.getNextTimeout() threw unexpected exception", "NoMoreTimeoutsException", failure);

                // With no more timeouts, timer is considered "expired".  Verify that getSchedule() gives the NSOLE:
                assertEquals("Expected NoSuchObjectLocalException.", "NoSuchObjectLocalException", ivBean.getScheduleString());
            }
            --numExpectedTimeouts;
        }
    }

    /**
     * Round the specified Date up to the next second.
     *
     * This code was copied from ScheduleExpressionParser.
     */
    private long roundToNextSecond(Date date) {

        long value = date.getTime();
        if (value > 0) {
            long remainder = value % 1000;
            if (remainder != 0) {
                // Protect against overflow.
                long newValue = value - remainder + 1000;
                value = newValue > 0 || value < 0 ? newValue : Long.MAX_VALUE;
            }
        }

        return value;
    }

}
