/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.timer.auto.np.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.Test;

import io.openliberty.ejbcontainer.fat.timer.auto.np.ejb.AutoCreatedTimerABean;
import io.openliberty.ejbcontainer.fat.timer.auto.np.ejb.AutoCreatedTimerDriver;
import io.openliberty.ejbcontainer.fat.timer.auto.np.ejb.AutoCreatedTimerDriverBean;
import io.openliberty.ejbcontainer.fat.timer.auto.np.ejb.AutoCreatedTimerMBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/AutoCreatedNPTimerServlet")
@SuppressWarnings({ "unchecked", "serial" })
public class AutoCreatedNPTimerServlet extends AbstractServlet {
    private static final String CLASS_NAME = AutoCreatedNPTimerServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    private AutoCreatedTimerDriver ivTestDriver;

    @Override
    public void setup() throws Exception {
        svLogger.entering(CLASS_NAME, "setUp");

        // Wait for some of the timers to expire a few times
        AutoCreatedTimerDriver driverBean = getDriverBean();
        driverBean.setup();

        svLogger.exiting(CLASS_NAME, "setUp");
    }

    @Override
    public void cleanup() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();
        driverBean.clearAllTimers();
    }

    private boolean verifyAttemptsIsAcceptable(int actualAttempts, int minimumAcceptableAttempts) {
        if (actualAttempts < minimumAcceptableAttempts) {
            log("The actual number of attempts **" + actualAttempts + "** was less than the minimum required attempts **" + minimumAcceptableAttempts + "**");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Looks up the bean that is used as the driver for these tests.
     *
     * The tests are physically run inside of an AppClient JVM, and so they need help to communicate with the server. The client side test method looks
     * up this bean and gives it directions, and then the 'driver' bean tells the various 'worker' beans to do the needed things (like create timers).
     */
    private AutoCreatedTimerDriver getDriverBean() throws Exception {
        return ivTestDriver;
    }

    /**
     * Writes log messages to both SystemOut and the test logs.
     *
     * Log messages that get written to the svLogger end up getting dumped into the 'results' output directory for the test. However, this data doesn't
     * appear to get physically written out to the disk until the testing has fully completed.
     *
     * These timer tests take a long time due to all of the delays built into them, and I want to be able to follow along with the progress in 'real time',
     * instead of being in the dark until everything is done. Thus, I'm writing out log messages to SystemOut as well, because that data will get sent to the
     * client log in 'real time'.
     */
    @Override
    public void log(String message) {
        System.out.println(message);
        svLogger.info(message);
        super.log(message);
    }

    private boolean verifyNextTimeoutsInterval(ArrayList<Long> nextTimeouts, long firstOffset, long interval) {
        if (nextTimeouts == null) {
            svLogger.info("The next timeout list for the timer was null.");
            return false;
        }

        Long previousTimeout = null;
        for (Long nextTimeout : nextTimeouts) {
            if (previousTimeout == null) {
                if ((nextTimeout - firstOffset) % interval != 0) {
                    svLogger.info("next timeout not scheduled on correct boundary : timeout=" + nextTimeout + ", firstOffset=" + firstOffset + ", interval=" + interval);
                    svLogger.info("next timeouts=" + nextTimeouts);
                    return false;
                }
            } else if (nextTimeout - previousTimeout != interval) {
                svLogger.info("next timeout not scheduled at correct interval : previous=" + previousTimeout + ", next=" + nextTimeout + ", interval=" + interval);
                svLogger.info("next timeouts=" + nextTimeouts);
                return false;
            }
            previousTimeout = nextTimeout;
        }
        return true;
    }

    // -----------------------------------------------------------------------------------------
    // Note about validating the actual time of the timeouts:
    //
    // Unfortunately, it looks like its not possible to ensure the exact second that timeouts
    // will occur. Most of the time, for most of the tests, the timeouts *do* occur at the exact
    // second they were configured for. For example, if we configure a timer to go off at 30
    // seconds past the minute, in most cases it does exactly that.
    //
    // However, some of the time in some of the tests, we don't get the exact second for a number
    // of reasons...sometimes the timer starts to go off in the correct second, but by the time
    // we actually get into the timeout method and record the timestamp the clock has already
    // rolled over into the next second...or the exact second that the timer is suppose to fire
    // just so happens to come around after the timer has been created, but before the
    // application is completely started, and so the timer tries to fire and we block it, and
    // the timer keeps retrying itself until the application is actually up, and when the
    // application does come up and the timer is allowed to fire its now past the exact second
    // it was suppose to go off at, and so according to the timestamp it fired at the 'wrong'
    // time (even though our behavior was actually correct).
    //
    // Worst of all, these 'false failures' are random....they will occur in a method in one run
    // (because the timing of the application startup was unlucky) and then not occur in the
    // next run (because the timing of the application startup was lucky), etc.
    //
    // The net is that if we check for an exact second on the timeout, we'll likely be forever
    // chasing false failures in the automated moonstone testing that aren't actually real
    // problems (but rather, the result of unlucky server startup timing), and which we'll
    // waste a bunch of time trying unsuccessfully to reproduce them in our own environment.
    //
    // So, to prevent this, for the tests with timeouts that are expected to occur, the tests
    // will just verify the timeout occurred (and not worry about exactly when occurred) and
    // also collect and verify that the next scheduled timeout(s) is/are correct.
    // -----------------------------------------------------------------------------------------

    /**
     * Verifies that you can have multiple Schedule and Timeout annotation on the same method, and that all get respected.
     */
    @Test
    public void testMultipleAtScheduleWithTimeout() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        driverBean.driveCreationOfProgramaticTimer();
        driverBean.waitForProgramaticTimer(6000);

        Properties firstProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.FIRST_SCHEDULE);
        int count1 = ((Integer) firstProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts1 = (ArrayList<Long>) firstProps.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        Properties secondProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.SECOND_SCHEDULE);
        int count2 = ((Integer) secondProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts2 = (ArrayList<Long>) secondProps.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        Properties thirdProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.THIRD_SCHEDULE);
        int count3 = ((Integer) thirdProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts3 = (ArrayList<Long>) thirdProps.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        Properties fourthProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.PROGRAMATIC_TIMEOUT);
        int count4 = ((Integer) fourthProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();

        boolean validTimeoutCount1 = verifyAttemptsIsAcceptable(count1, 2);
        boolean validNextTimeouts1 = verifyNextTimeoutsInterval(nextTimeouts1, 20 * 1000, 60 * 1000);

        boolean validTimeoutCount2 = verifyAttemptsIsAcceptable(count2, 2);
        boolean validNextTimeouts2 = verifyNextTimeoutsInterval(nextTimeouts2, 10 * 1000, 60 * 1000);

        boolean validTimeoutCount3 = verifyAttemptsIsAcceptable(count3, 2);
        boolean validNextTimeouts3 = verifyNextTimeoutsInterval(nextTimeouts3, 40 * 1000, 60 * 1000);

        assertTrue("There were not enough timeouts for the 1st @Schedule.", validTimeoutCount1);
        assertTrue("There were not enough timeouts for the 2nd @Schedule.", validTimeoutCount2);
        assertTrue("There were not enough timeouts for the 3rd @Schedule.", validTimeoutCount3);
        assertEquals("There were not enough timeouts for the programatic timer.", 1, count4);
        assertTrue("The next scheduled timeout values for the 1st @Schedule were not correct.", validNextTimeouts1);
        assertTrue("The next scheduled timeout values for the 2st @Schedule were not correct.", validNextTimeouts2);
        assertTrue("The next scheduled timeout values for the 3st @Schedule were not correct.", validNextTimeouts3);
    }

    /**
     * Verifies that a timer stanza in xml overrides a Schedule annotation in the code.
     *
     */
    @Test
    public void testXMLOverridesAtSchedule() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties propsANO = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.FIRST_OVERRIDE_ANNOTATION);
        Properties propsXML = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.FIRST_OVERRIDE_XML);

        boolean noAnnotation = (propsANO == null) ? true : false;
        boolean yesXML = (propsXML != null) ? true : false;

        assertTrue("The timer defined via annoation existed, but it should have been overriden by the timer defined via xml.", noAnnotation);
        assertTrue("The timer defined via xml did not exist, but it should have.", yesXML);
    }

    /**
     * Verifies that a timer stanza in xml overrides multiple Schedule annotations in the code.
     *
     */
    @Test
    public void testXMLOverridesMultipleAtSchedule() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties propsAno1 = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.SECOND_OVERRIDE_ANNOTATION);
        Properties propsAno2 = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.THIRD_OVERRIDE_ANNOTATION);
        Properties propsXML = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.SECOND_OVERRIDE_XML);

        boolean noAnnotation1 = (propsAno1 == null) ? true : false;
        boolean noAnnotation2 = (propsAno2 == null) ? true : false;
        boolean yesXML = (propsXML != null) ? true : false;

        assertTrue("The first timer defined via annoation existed, but it should have been overriden by the timer defined via xml.", noAnnotation1);
        assertTrue("The second timer defined via annoation existed, but it should have been overriden by the timer defined via xml.", noAnnotation2);
        assertTrue("The timer defined via xml did not exist, but it should have.", yesXML);
    }

}
