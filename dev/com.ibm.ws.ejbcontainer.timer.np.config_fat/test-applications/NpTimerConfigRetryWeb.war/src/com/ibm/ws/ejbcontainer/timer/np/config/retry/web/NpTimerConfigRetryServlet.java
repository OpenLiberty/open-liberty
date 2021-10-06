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
package com.ibm.ws.ejbcontainer.timer.np.config.retry.web;

import static com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb.TimerRetryDriverBean.TIMER_INTERVAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb.TimerRetryDriver;
import com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb.TimerRetryDriverBean;

import componenttest.app.FATServlet;

@WebServlet("/NpTimerConfigRetryServlet")
@SuppressWarnings("serial")
public class NpTimerConfigRetryServlet extends FATServlet {
    private static final String CLASS_NAME = NpTimerConfigRetryServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final int TIMER_DELAY = 2500; // 602131
    private static final long NO_CANCEL_DELAY = 0;

    @EJB
    TimerRetryDriver driverBean;

    /**
     * Determines if the time span between two attempts is acceptable...was it less than some amount.
     *
     * @param timestampForFirstAttempt
     * @param timestampForSecondAttempt
     * @return
     */
    private boolean verifyImmediateRetryAcceptable(long timestampForFirstAttempt, long timestampForSecondAttempt) {
        long difference = timestampForSecondAttempt - timestampForFirstAttempt;

        if (difference < TIMER_DELAY) {
            svLogger.info("Initial attempt occurred at **" + timestampForFirstAttempt + "**, and retry occurred at **" + timestampForSecondAttempt + "**, which is " +
                          "a difference of **" + difference + "**, which falls inside of the acceptable range of **" + TIMER_DELAY + "**");
            return true;
        } else {
            svLogger.info("Initial attempt occurred at **" + timestampForFirstAttempt + "**, and retry occurred at **" + timestampForSecondAttempt + "**, which is " +
                          "a difference of **" + difference + "**, which falls outside of the acceptable range of **" + TIMER_DELAY + "**");
            return false;
        }
    }

    /**
     * Determines if the time span between two attempts is acceptable....was it greater than some minimum amount, and also less than some maximum amount.
     *
     * @param timestampForFirstAttempt
     * @param timestampForSecondAttempt
     * @param minimumDifference
     * @return
     */
    private boolean verifyRetryIntervalAcceptable(long timestampForFirstAttempt, long timestampForSecondAttempt, long minimumDifference) {
        long difference = timestampForSecondAttempt - timestampForFirstAttempt;
        // 500 ms fudge factor for Windows time math and preInvoke delays
        long maxDifference = minimumDifference + TIMER_DELAY + 500;
        minimumDifference = minimumDifference - 500;

        if (difference < minimumDifference) {
            svLogger.info("Initial attempt occurred at **" + timestampForFirstAttempt + "**, and retry occurred at **" + timestampForSecondAttempt + "**, which is " +
                          "a difference of **" + difference + "**, which is less than the minimum acceptable difference of **" + minimumDifference
                          + "**.  In other words, the retry " +
                          "happened too quickly.");
            return false;
        } else if (difference > maxDifference) {
            svLogger.info("Initial attempt occurred at **" + timestampForFirstAttempt + "**, and retry occurred at **" + timestampForSecondAttempt + "**, which is " +
                          "a difference of **" + difference + "**, which is greater than the maximum acceptable difference of **" + maxDifference
                          + "**.  In other words, the retry " +
                          "waited too long.");
            return false;
        } else {
            svLogger.info("Initial attempt occurred at **" + timestampForFirstAttempt + "**, and retry occurred at **" + timestampForSecondAttempt + "**, which is " +
                          "a difference of **" + difference + "**, which is greater than the minimum difference of **" + minimumDifference + "**, and less than " +
                          "the maximum difference of **" + maxDifference + "**.  In other words, the retry happened in the expected window of time.");
            return true;
        }
    }

    /**
     * Purpose:
     * 1) We get an automatic retry
     * 2) It happens immediately (ie, it doesn't wait for the scheduled retry interval)
     *
     */
    @Test
    public void testForceImmediateRetry() throws Exception {
        //Expected behavior:
        //    Initial timeout should fail.
        //
        //    Retry should occur immediately, even though we have a few second timeout interval configured.
        //
        //    No additional retries should occur, because the first immediate retry should succeed.
        //
        //    Note that the 'count' variable tracked in the 'worked' bean includes the original attempt, so the value of the count will always be
        //    1 + number-of-retries

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, RETRY_COUNT_NORMAL, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("MHD: Calling my bean to force retry scenario...");
        driverBean.forceOneFailure("testForceImmediateRetry");

        svLogger.info("Waiting for timer to fail and immediate retry to occur...");
        driverBean.waitForTimersAndCancel(NO_CANCEL_DELAY);

        svLogger.info("MHD: Validating results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        boolean validInterval = verifyImmediateRetryAcceptable(timestamps.get(0).longValue(), timestamps.get(1).longValue());

        assertEquals("Attempt count of **" + count + "** was not the expected value of 2.", 2, count);
        assertTrue("Immediate retry interval was not acceptable.", validInterval);
    }

    /**
     * Purpose:
     * 1) Verify that we get the correct number of retries.
     * 2) Verify that all retries (after the first, immediate retry) wait for the configured interval
     *
     */
    @Test
    public void testRetryCountAndIntervalHonored() throws Exception {
        //Expected behavior:
        //    Initial attempt should fail.
        //
        //    First retry should occur, immediately, and fail.
        //
        //    Second retry should occur, after the configured delay, and fail.
        //
        //    Third retry should occur, after the configured delay, and fail.
        //
        //    Fourth retry should occur, after the configured delay, and fail.
        //
        //    No additional retries should occur, because we've now hit our configured limit of 4.

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, RETRY_COUNT_NORMAL, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("Calling BeanB to force repeated retries, until the retry limit is reached...");
        driverBean.forceEverythingToFail("testRetryCountAndIntervalHonored", 4);

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(1000 + TIMER_DELAY);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        assertEquals("Attempt count **" + count + "** was not the expected value of 5.", 5, count);
        int failcount = 0;
        boolean validIntervalForFirstRetry = false, validIntervalForSecondRetry = false, validIntervalForThirdRetry = false, validIntervalForFourthRetry = false;
        if (verifyImmediateRetryAcceptable(timestamps.get(0).longValue(), timestamps.get(1).longValue()))
            validIntervalForFirstRetry = true;
        else
            failcount++;
        if (verifyRetryIntervalAcceptable(timestamps.get(1).longValue(), timestamps.get(2).longValue(), 1000))
            validIntervalForSecondRetry = true;
        else
            failcount++;
        if (verifyRetryIntervalAcceptable(timestamps.get(2).longValue(), timestamps.get(3).longValue(), 1000))
            validIntervalForThirdRetry = true;
        else
            failcount++;
        if (verifyRetryIntervalAcceptable(timestamps.get(3).longValue(), timestamps.get(4).longValue(), 1000))
            validIntervalForFourthRetry = true;
        else
            failcount++;
        // tolerate 1 fail as it was probably a server hiccup
        if (failcount > 1) {
            assertTrue("Immediate retry interval was not acceptable.", validIntervalForFirstRetry);
            assertTrue("First delayed retry interval was not acceptable.", validIntervalForSecondRetry);
            assertTrue("Second delayed retry interval was not acceptable.", validIntervalForThirdRetry);
            assertTrue("Third delayed retry interval was not acceptable.", validIntervalForFourthRetry);
        }
    }

    /**
     * Purpose:
     * 1) Verify that when a timer is invoked as part of a retry sequence, and is successful, that the retryCount is reset, so the next regularly scheduled
     * timer invocation also gets the benefit of the full retry sequence.
     *
     */
    @Test
    public void testRetryLimitGetsResetUponSuccess() throws Exception {
        //Expected behavior:
        //    Initial attempt fails.
        //
        //    First retry occurs, immediately, and fails.
        //
        //    Second retry occurs, after the configured delay, and succeeds.  This resets the retryCounter maintained by the EJBcontianer.
        //
        //    Another initial attempt fails.
        //
        //    First retry occurs, immediately, and fails.
        //
        //    Second retry occurs, after the configured delay, and succeeds.  If the EJBContainers retry counter had not been reset, then this would
        //    be our 4th retry attempt (2 retries for the first timeout, plus the immediate one for this second timeout), and since our limit is 3,
        //    we never would have attempted this second retry for the second invocation.

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, RETRY_COUNT_NORMAL, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();
        boolean validIntervalForFirstRetryInvocation2 = false;
        boolean validIntervalForSecondRetryInvocation2 = false;
        int tries = 0;

        // Try this multiple times because a VM machine blip can cause a retry to not be in acceptable time period.
        while ((validIntervalForFirstRetryInvocation2 && validIntervalForSecondRetryInvocation2) == false &&
               tries < 5) {
            svLogger.info("testRetryLimitGetsResetUponSuccess try number " + ++tries);

            svLogger.info("Calling BeanC to force retries, until we get a success...");
            driverBean.forceTwoFailures("testRetryLimitGetsResetUponSuccess");

            svLogger.info("Waiting for timer to fail and expected retries to occur...");
            driverBean.waitForTimersAndCancel(NO_CANCEL_DELAY);

            svLogger.info("Calling BeanC a 2nd time to force more retries, again until we get a success...");
            driverBean.forceTwoFailures("testRetryLimitGetsResetUponSuccess");

            svLogger.info("Waiting for timer to fail and expected retries to occur...");
            driverBean.waitForTimersAndCancel(NO_CANCEL_DELAY);

            svLogger.info("Waking up and getting results...");
            Properties props = driverBean.getResults();
            int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
            @SuppressWarnings("unchecked")
            ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
            assertEquals("Attempt count **" + count + "** was not the expected value of 3.", 3, count);
            validIntervalForFirstRetryInvocation2 = verifyImmediateRetryAcceptable(timestamps.get(0).longValue(), timestamps.get(1).longValue());
            validIntervalForSecondRetryInvocation2 = verifyRetryIntervalAcceptable(timestamps.get(1).longValue(), timestamps.get(2).longValue(), 1000);
        }
        assertTrue("Immediate retry interval was not acceptable for the second invocation after " + tries + " tries", validIntervalForFirstRetryInvocation2);
        assertTrue("First delayed retry interval was not acceptable for the second invocation after " + tries + " tries", validIntervalForSecondRetryInvocation2);
    }

    /**
     * Purpose:
     * 1) Verify that we do not get any retry (not even the required automatic one) when the user configured to have zero retries.
     *
     * @throws Exception
     */
    @Test
    public void testConfiguredForZeroRetries() throws Exception {
        //Expected behavior:
        //    Initial attempt fails.
        //
        //    There is no retry - not even the first immediate one that the spec requires - because the the retrycount is explicitly configured to be 0.

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, RETRY_COUNT_ZERO, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("Calling BeanB to force retry, which should get skipped because we explicitly configured to never retry...");
        driverBean.forceEverythingToFail("testConfiguredForZeroRetries", 0);

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(1000 + TIMER_DELAY);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        assertEquals("Attempt count **" + count + "** was not the expected value of 1.", 1, count);
        assertEquals("Timestamps size **" + timestamps.size() + "** was not the expected value of 1.", 1, timestamps.size());

        boolean timerExists = ((Boolean) props.get(TimerRetryDriverBean.TIMER_EXISTS)).booleanValue();
        assertFalse("Single Action Timer should not have existed after max retries", timerExists);
    }

    /**
     * Purpose:
     * 1) Verify that an interval timer that hits max retries will continue on and fire next interval.
     *
     * @throws Exception
     */
    @Test
    public void testConfiguredForZeroRetriesTimerReschedules() throws Exception {

        //Expected behavior:
        //    Initial attempt fails.
        //
        //    There is no retry - not even the first immediate one that the spec requires - because the the retrycount is explicitly configured to be 0.
        //
        //    It's an interval timer so it moves on to nextTimeout
        //
        //    That fails too

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, RETRY_COUNT_ZERO, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("Calling BeanE to force retry, which should get skipped because we explicitly configured to never retry...");
        driverBean.forceEverythingToFailIntervalTimer("testConfiguredForZeroRetriesTimerReschedules", 2);

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(0);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        boolean timerExists = ((Boolean) props.get(TimerRetryDriverBean.TIMER_EXISTS)).booleanValue();
        assertEquals("Timeout count **" + count + "** was not the expected value of 2. Interval Timer failing at max retries is not rescheduling!", 2, count);
        assertEquals("Timestamps size **" + timestamps.size() + "** was not the expected value of 2.", 2, timestamps.size());
        assertTrue("Timer did not exist after max retries", timerExists);
    }

    /**
     * Purpose:
     * 1) Verify that an interval timer that hits max retries will have it's retry count reset for next interval.
     *
     * @throws Exception
     */
    @Test
    public void testConfiguredForTwoRetriesTimerReschedules() throws Exception {

        //Expected behavior:
        //    Initial attempt fails.
        //
        //    retries once, still fails and retry count is 2
        //
        //    It's an interval timer so it moves on to nextTimeout
        //
        //    That fails twice too, confirming try count is reset between timeout attempts

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, RETRY_COUNT_ZERO, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("Calling BeanB to force retry, which should get skipped because we explicitly configured to never retry...");
        driverBean.forceEverythingToFailIntervalTimer("testConfiguredForZeroRetriesTimerReschedules", 6);

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(0);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        boolean timerExists = ((Boolean) props.get(TimerRetryDriverBean.TIMER_EXISTS)).booleanValue();
        assertEquals("Timeout count **" + count + "** was not the expected value of 6. Interval Timer not resetting number of retries", 6, count);
        assertEquals("Timestamps size **" + timestamps.size() + "** was not the expected value of 6.", 6, timestamps.size());
        assertTrue("Timer did not exist after max retries", timerExists);
    }

    /**
     * Purpose:
     * 1) Verify that an Calendar timer that hits max retries will have it's retry count reset for next interval.
     *
     * @throws Exception
     */
    @Test
    public void testConfiguredForTwoRetriesTimerReschedulesCalendar() throws Exception {

        //Expected behavior:
        //    Initial attempt fails.
        //
        //    retries once, still fails and retry count is 2
        //
        //    It's an calendar timer so it moves on to nextTimeout
        //
        //    That fails twice too, confirming try count is reset between timeout attempts

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, RETRY_COUNT_ZERO, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("Calling BeanB to force retry, which should get skipped because we explicitly configured to never retry...");
        driverBean.forceEverythingToFailCalendarTimer("testConfiguredForTwoRetriesTimerReschedulesCalendar", 6);

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(0);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        boolean timerExists = ((Boolean) props.get(TimerRetryDriverBean.TIMER_EXISTS)).booleanValue();
        assertEquals("Timeout count **" + count + "** was not the expected value of 6. Interval Timer not resetting number of retries", 6, count);
        assertEquals("Timestamps size **" + timestamps.size() + "** was not the expected value of 6.", 6, timestamps.size());
        assertTrue("Timer did not exist after max retries", timerExists);
    }

    /**
     * Purpose:
     * 1) Verify that we retry endlessly when the user omits the retry count configuration attribute.
     *
     * @throws Exception
     */
    @Test
    public void testConfiguredForEndlessRetry() throws Exception {
        //Expected behavior:
        //    Initial attempt fails.
        //
        //    We retry endlessly, on the configured interval, because the retryCount is omitted from the configuration.

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_NORMAL, null, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();
        svLogger.info("Calling BeanB to force retry, which should happen continuously, because retry count was omitted...");
        driverBean.forceEverythingToFail("testConfiguredForEndlessRetry", 10);

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(NO_CANCEL_DELAY);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        svLogger.info("Got attempt count of **" + count + "**");
        assertTrue("We did not see continous retries, which should have occurred because the retry count was omitted.", count >= 10);

        boolean timerExists = ((Boolean) props.get(TimerRetryDriverBean.TIMER_EXISTS)).booleanValue();
        assertTrue("Single Action Timer did not exist even though we had endless retries", timerExists);
    }

    /**
     * Purpose:
     * 1) Verify that we attempt all retries immediately, because the user configured an interval of zero.
     *
     * @throws Exception
     */
    @Test
    public void testConfiguredForImmediateRetry() throws Exception {
        //Expected behavior:
        //    Initial attempt fails.
        //
        //    First retry occurs, immediately.
        //
        //    Second retry occurs, immediately, because the retryInterval is explicitly set to 0.

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_ZERO, RETRY_COUNT_NORMAL, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();
        int retryCount = 10;

        // Run the entire test twice if needed because first result can be cratered by a server hiccup
        boolean retryTest = false;

        do {
            svLogger.info("Calling BeanB to force retrys, which should all happen immediately, because retry interval was zero...");
            driverBean.forceEverythingToFail("testConfiguredForImmediateRetry", retryCount);

            svLogger.info("Waiting for timer to fail and expected retries to occur...");
            driverBean.waitForTimersAndCancel(1000 + TIMER_DELAY);

            svLogger.info("Waking up and checking results...");
            Properties props = driverBean.getResults();
            @SuppressWarnings("unchecked")
            ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
            svLogger.info("timestamps: " + timestamps.toString());

            int numOfFails = 0;
            for (int i = 0; i < retryCount - 1; i++) {
                if (!verifyImmediateRetryAcceptable(timestamps.get(i).longValue(), timestamps.get(i + 1).longValue())) {
                    numOfFails++;
                }
            }
            // 1 bad interval is acceptable due to a server hiccup
            if (numOfFails > 1) {
                // Ran the test twice and still failed, fail the test
                if (retryTest)
                    fail("Immediate retry interval execution was not acceptable " + numOfFails + " times on second run, failing.");
                else {
                    svLogger.info("Immediate retry interval execution was not acceptable " + numOfFails + " times on first run, retrying.");
                    retryTest = true;
                }
            } else {
                retryTest = false;
            }

            boolean timerExists = ((Boolean) props.get(TimerRetryDriverBean.TIMER_EXISTS)).booleanValue();
            assertFalse("Single Action Timer should not have existed after max retries", timerExists);

            if (retryTest) {
                //Sleep 20 seconds to try to get past server hiccup then retry the test
                FATHelper.sleep(20000);
            }

        } while (retryTest == true);
    }

    /**
     * Purpose:
     * 1) Verify that we attempt all retries (after the first immediate, automatic retry) on 5 minute intervals, because that is the default interval
     * that the user should get when the interval attribute is omitted from the configuration.
     *
     * @throws Exception
     */
    @Test
    public void testConfiguredForDefaultRetryInterval() throws Exception {
        //Expected behavior:
        //    Initial attempt fails.
        //
        //    First retry occurs, immediately.
        //
        //    Second retry occurs, after a 5 minute interval, because the user omitted the retryInterval from the configuration, which makes us default to
        //    using a 5 minute interval.

        // ensureServerIsConfiguredCorrectly(svCell, null, RETRY_COUNT_NORMAL, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("Calling BeanC to force retrys, which should happen after 5 minutes, because the retry interval was omitted...");
        driverBean.forceTwoFailures("testConfiguredForDefaultRetryInterval");

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(NO_CANCEL_DELAY);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        assertEquals("Attempt count **" + count + "** was not the expected value of 3.", 3, count);
        boolean validIntervalForFirstRetry = verifyImmediateRetryAcceptable(timestamps.get(0).longValue(), timestamps.get(1).longValue());
        boolean validIntervalForSecondRetry = verifyRetryIntervalAcceptable(timestamps.get(1).longValue(), timestamps.get(2).longValue(), 300000);
        assertTrue("Immediate retry interval was not acceptable.", validIntervalForFirstRetry);
        assertTrue("First delayed retry interval was not acceptable.", validIntervalForSecondRetry);
    }

    /**
     * Purpose:
     * 1) Verify that when retry attempts and regularly scheduled attempts overlap, that we handle this correctly.
     *
     * @throws Exception
     */
    @Test
    public void testOverlappingRetriesAndRegularlyScheduled() throws Exception {
        //We are trying to verify the following 'rules' are obeyed:
        //    1) All normally scheduled executions that are missed because we are waiting on a retry from a previously
        //       failed execution get "made up"...ie, they are eventually run (as oppossed to never getting run)
        //
        //    2) All of these "make ups" occur as soon as possible (as oppossed to running them at the configured interval
        //       that we normally wait for between normally scheduled executions)
        //
        //    3) After completing the "make ups", we resuming the normally scheduled exections based on the original
        //      schedule (as oppossed to re-calculating the scheduled based on the current time).
        //
        //
        //So, the execution flow in this test is:
        //    We have a configured retry interval of 10 seconds.
        //
        //    We are configured to repeat every 4.5 seconds.
        //
        //Attempt         Moment in Time        Difference from previous attempt
        //0 (initial)          1            na
        //1 (1st retry...immediate)            2            <2
        //2 (2nd retry...delayed 10 sec)     12           10
        //3 (1st makeup...immediate)           13                             <2
        //4 (2nd makeup...immediate)           14                             <2
        //5 (13.5 second normal)           14.5         13.5 from initial attempt
        //6 (18 second normal)                 19                             > scheduled nextTimeout
        //
        //
        //So, in words, the flow is this:
        //    We do our intial attempt and fail, then immeidately retry and fail again, and then wait 10 seconds...while waiting,
        //    two normally scheduled executions are skipped (4.5 & 9)...then our 10 second wait interval is done, and we wake up and
        //    retry again and pass...and now we "make up" the two skipped executions as soon as we can...which should take us
        //    right up to the next normally scheduled execution at the 13.5 second mark...so we do that one at the 13.5 second
        //    mark as normally scheduled. By the time the 18 second mark comes around, we should be back on schedule and that timeout
        //    should occur 4.5 seconds after the 13.5 second timeout.

        // ensureServerIsConfiguredCorrectly(svCell, RETRY_INTERVAL_LONG, RETRY_COUNT_NORMAL, null, TIMER_THREADS_NORMAL, USE_TIMER_MANAGER, secure);

        // TimerRetryDriver driverBean = getDriverBean();

        svLogger.info("Calling BeanD to create situation where retries and regularly scheduled timeouts overlap...");
        driverBean.forceRetrysAndRegularSchedulesToOverlap("testOverlappingRetriesAndRegularlyScheduled", 7);

        svLogger.info("Waiting for timer to fail and expected retries to occur...");
        driverBean.waitForTimersAndCancel(NO_CANCEL_DELAY);

        svLogger.info("Waking up and checking results...");
        Properties props = driverBean.getResults();
        int count = ((Integer) props.get(TimerRetryDriverBean.COUNT_KEY)).intValue();
        long scheduledStartTime = ((Long) props.get(TimerRetryDriverBean.SCHEDULED_START_TIME_KEY)).longValue();
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) props.get(TimerRetryDriverBean.TIMESTAMP_KEY);
        @SuppressWarnings("unchecked")
        ArrayList<Long> nextTimes = (ArrayList<Long>) props.get(TimerRetryDriverBean.NEXTTIMEOUT_KEY);
        assertEquals("Attempt count **" + count + "** was not the expected value of 7.", 7, count);
        boolean validIntervalForImmediateRetry = verifyImmediateRetryAcceptable(timestamps.get(0).longValue(), timestamps.get(1).longValue());
        boolean validIntervalForDelayedRetry = verifyRetryIntervalAcceptable(timestamps.get(1).longValue(), timestamps.get(2).longValue(), 10000);
        boolean validIntervalForFirstMakeUp = verifyImmediateRetryAcceptable(timestamps.get(2).longValue(), timestamps.get(3).longValue());
        boolean validIntervalForSecondMakeUp = verifyImmediateRetryAcceptable(timestamps.get(3).longValue(), timestamps.get(4).longValue());

        // Under normal timing, the next 2 timers should fire on time, but check that
        // the earlier times havn't been delayed so much they've pushed out past the
        // next timer as well, causing it to also be a make up.
        boolean nextTimeoutIsMakeUp = (scheduledStartTime + 3 * TIMER_INTERVAL) <= timestamps.get(4).longValue();
        boolean validIntervalForFirstNormal = (nextTimeoutIsMakeUp ? verifyImmediateRetryAcceptable(timestamps.get(4).longValue(),
                                                                                                    timestamps.get(5).longValue()) : verifyRetryIntervalAcceptable(scheduledStartTime,
                                                                                                                                                                   timestamps.get(5).longValue(),
                                                                                                                                                                   3 * TIMER_INTERVAL));

        // Note, if running on time the checking uses 'nextTime' of prior timer which should be
        // the actual time; thus 0 difference. A fudge factor is included in the method.
        nextTimeoutIsMakeUp = (scheduledStartTime + 4 * TIMER_INTERVAL) <= timestamps.get(5).longValue();
        boolean validIntervalForSecondNormal = (nextTimeoutIsMakeUp ? verifyImmediateRetryAcceptable(timestamps.get(5).longValue(),
                                                                                                     timestamps.get(6).longValue()) : verifyRetryIntervalAcceptable(nextTimes.get(5).longValue(),
                                                                                                                                                                    timestamps.get(6).longValue(),
                                                                                                                                                                    0));

        assertTrue("Immediate retry interval was not acceptable.", validIntervalForImmediateRetry);
        assertTrue("Delayed retry interval was not acceptable.", validIntervalForDelayedRetry);
        assertTrue("First makeup interval was not acceptable.", validIntervalForFirstMakeUp);
        assertTrue("Second makeup interval was not acceptable.", validIntervalForSecondMakeUp);
        assertTrue("First normally scheduled interval was not acceptable.", validIntervalForFirstNormal);
        assertTrue("NextTimeout scheduled correctly: " + (nextTimes.get(5).longValue() - nextTimes.get(4).longValue()),
                   (nextTimes.get(5).longValue() - nextTimes.get(4).longValue()) == 4500);
        assertTrue("Second normally scheduled interval was not acceptable.", validIntervalForSecondNormal);
    }
}
