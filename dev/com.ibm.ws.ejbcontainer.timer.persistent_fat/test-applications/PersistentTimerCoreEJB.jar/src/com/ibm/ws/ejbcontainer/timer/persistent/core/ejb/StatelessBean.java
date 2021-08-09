/*******************************************************************************
 * Copyright (c) 2003, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.persistent.core.ejb;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.naming.InitialContext;

import org.junit.Assert;

/**
 * Bean implementation for a basic Stateless Session bean that does not
 * implement the TimedObject interface. It contains methods to test
 * TimerService access.
 **/
@SuppressWarnings("serial")
public class StatelessBean implements SessionBean {
    private static final Logger svLogger = Logger.getLogger(StatelessBean.class.getName());

    private SessionContext ivContext;

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that does not implement the TimedObject interface. <p>
     *
     * This test method will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    public void testTimerService() throws Exception {
        TimerService ts = null;
        Timer timer = null;
        String timerInfo = null;
        Timer retTimer = null;
        TimerHandle timerHandle = null;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() returns a valid TimerService
        // -----------------------------------------------------------------------
        svLogger.info("testTimerService: Calling getTimerService()");
        ts = ivContext.getTimerService();
        Assert.assertNotNull("testTimerService() ---> Got TimerService", ts);

        // -----------------------------------------------------------------------
        // 2 - Verify TimerService.createTimer() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling TimerService.createTimer()");
            timer = ts.createTimer(60000, (java.io.Serializable) null);
            Assert.fail("testTimerService() ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "testTimerService() ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify TimerService.getTimers() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling getTimers()");
            Collection<Timer> timers = ts.getTimers();
            Assert.fail("testTimerService() ---> getTimers should have failed! " + timers);
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "testTimerService() ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may be performed.
        // - must get a Timer from a different bean, since this is not TimedObject
        // -----------------------------------------------------------------------
        {
            // --------------------------------------------------------------------
            // Locate Home, create a bean, and use the bean to create a Timer
            // --------------------------------------------------------------------
            StatelessTimedHome SLTLHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

            // Create a bean that may be used to create a timer.
            svLogger.info("Creating a bean to get a timer from ...");
            StatelessTimedObject sltl = SLTLHome.create();

            svLogger.info("Creating a timer for StatelessTimed Bean ...");
            timerInfo = "StatelessBean:" + System.currentTimeMillis();
            timer = sltl.createTimer(timerInfo);
        }

        // -----------------------------------------------------------------------
        // 4 - Verify Timer.getTimeRemaining() works
        // -----------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling Timer.getTimeRemaining()");
            long remaining = timer.getTimeRemaining();
            Assert.assertTrue("testTimerService() ---> Timer.getTimeRemaining() worked: " + remaining,
                              remaining >= 1 && remaining <= 60000);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify Timer.getInfo() works
        // -----------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling Timer.getInfo()");
            Object curInfo = timer.getInfo();
            Assert.assertEquals("testTimerService() ---> Timer.getInfo() worked: " + curInfo +
                                "", timerInfo, curInfo);
        }

        // -----------------------------------------------------------------------
        // 6 - Verify Timer.getHandle() works
        // -----------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.getHandle()");
        timerHandle = timer.getHandle();
        Assert.assertNotNull("testTimerService() ---> Timer.getHandle() worked: " + timerHandle +
                             "", timerHandle);

        // -----------------------------------------------------------------------
        // 7 - Verify TimerHandle.getTimer() works
        // -----------------------------------------------------------------------
        svLogger.info("testTimerService: Calling TimerHandle.getTimer()");
        retTimer = timerHandle.getTimer();
        Assert.assertNotNull("testTimerService() ---> TimerHandle.getTimer() worked: " +
                             retTimer, retTimer);

        // -----------------------------------------------------------------------
        // 8 - Verify Timer.equals() works
        // -----------------------------------------------------------------------
        // (It uses a timer set in test 7)
        svLogger.info("testTimerService: Calling Timer.equals()");
        Assert.assertEquals("testTimerService() ---> Timer.equals() worked",
                            timer, retTimer);

        // -----------------------------------------------------------------------
        // 9 - Verify Timer.cancel() works
        // -----------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.cancel()");
        timer.cancel();
        svLogger.info("testTimerService() ---> Timer.cancel() worked");

        // -----------------------------------------------------------------------
        // 10 - Verify NoSuchObjectLocalException occurs accessing canceled timer
        // -----------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling Timer.getInfo() " +
                          "on cancelled Timer");
            timer.getInfo();
            Assert.fail("testTimerService() --> Timer.getInfo() worked - " +
                        "expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            svLogger.log(Level.INFO, "testTimerService() --> Caught expected exception", nso);
        }
    }

    public StatelessBean() {
    }

    public void ejbCreate() throws CreateException {
    }

    @Override
    public void ejbRemove() {
    }

    @Override
    public void ejbActivate() {
    }

    @Override
    public void ejbPassivate() {
    }

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }
}
