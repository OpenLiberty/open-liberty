/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.operations.ejb;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;

import org.junit.Assert;

/**
 * Bean implementation for a basic Stateless Session bean that does not
 * implement a timeout callback method. It contains methods to test
 * TimerService access.
 **/
@Stateless
public class StatelessBean implements StatelessLocal {
    private static final Logger logger = Logger.getLogger(StatelessBean.class.getName());

    @Resource
    private SessionContext context;

    @EJB(beanName = "StatelessTimedBean")
    StatelessTimedLocal slBean;

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that does not implement a timeout method. <p>
     *
     * This test method will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() works
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    @Override
    public void testTimerService() {

        TimerService ts = null;
        Timer timer = null;
        String timerInfo = null;
        TimerHandle timerHandle = null;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() returns a valid TimerService
        // -----------------------------------------------------------------------
        logger.info("testTimerService: Calling getTimerService()");
        ts = context.getTimerService();
        Assert.assertNotNull("1 ---> Failed to get TimerService", ts);

        // -----------------------------------------------------------------------
        // 2 - Verify TimerService.createSingleActionTimer() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling TimerService.createSingleActionTimer()");
            TimerConfig timerConfig = new TimerConfig("NoTimeoutBean", false);
            timer = ts.createSingleActionTimer(0, timerConfig);
            Assert.fail("2 ---> createTimer should have failed! " + timer);
        } catch (IllegalStateException ise) {
            logger.info("2 ---> Caught expected exception : " + ise);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify TimerService.getTimers() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling getTimers()");
            Collection<Timer> timers = ts.getTimers();
            Assert.fail("3 ---> getTimers should have failed! " + timers);
        } catch (IllegalStateException ise) {
            logger.info("3 ---> Caught expected exception : " + ise);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may be performed.
        // - must get a Timer from a different bean, since this is not TimedObject
        // -----------------------------------------------------------------------
        logger.info("Creating a timer for StatelessTimed Bean ...");
        timerInfo = "StatelessBean:" + System.currentTimeMillis();
        timer = slBean.createTimer(timerInfo);

        // -------------------------------------------------------------------
        // 4 - Verify TimerService.getAllTimers() returns all created Timers
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling getAllTimers()");
        Collection<Timer> timers = ts.getAllTimers();

        // Print out the results for debug purposes...
        Object[] timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            logger.info("  returned : " + timersArray[i]);
        }

        Assert.assertEquals("4 ---> getTimers returned 1 Timer", 1, timers.size());
        // Make sure it is the correct timer...
        if (!timers.contains(timer))
            Assert.fail("4 ---> Timer not returned: " + timer);

        // -----------------------------------------------------------------------
        // 5 - Verify Timer.getTimeRemaining() works
        // -----------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.getTimeRemaining()");
        long remaining = timer.getTimeRemaining();
        Assert.assertTrue("5 ---> Timer.getTimeRemaining() worked: " + remaining,
                          remaining >= 1 && remaining <= StatelessTimedLocal.DEFAULT_EXPIRATION);

        // -----------------------------------------------------------------------
        // 6 - Verify Timer.getInfo() works
        // -----------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.getInfo()");
        Object curInfo = timer.getInfo();
        Assert.assertEquals("6 ---> Timer.getInfo() worked: " + curInfo +
                            "", timerInfo, curInfo);

        // -----------------------------------------------------------------------
        // 7 - Verify Timer.getHandle() fails with IllegalStateException
        // -----------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.getHandle()");
        try {
            timerHandle = timer.getHandle();
            fail("7 ---> Timer.getHandle() worked: " + timerHandle);
        } catch (IllegalStateException ex) {
            logger.info("7 ---> Caught expected exception : " + ex);
        }

        // -----------------------------------------------------------------------
        // 8 - Verify Timer.cancel() works
        // -----------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.cancel()");
        timer.cancel();
        logger.info("8 ---> Timer.cancel() worked");

        // -----------------------------------------------------------------------
        // 9 - Verify NoSuchObjectLocalException occurs accessing canceled timer
        // -----------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling Timer.getInfo() on cancelled Timer");
            curInfo = timer.getInfo();
            fail("9 ---> Timer.getInfo() worked - expected NoSuchObjectLocalException : " + curInfo);
        } catch (NoSuchObjectLocalException nso) {
            logger.info("9 ---> Caught expected exception " + nso);
        }
    }
}
