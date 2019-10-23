/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.persistent.core.web;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.logging.Logger;

import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatelessTimedBean;
import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatelessTimedHome;
import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatelessTimedObject;

import componenttest.app.FATServlet;

@WebServlet("/TimerAccessOperationsServlet")
@SuppressWarnings("serial")
public class TimerAccessOperationsServlet extends FATServlet {

    private static final Logger svLogger = Logger.getLogger(TimerAccessOperationsServlet.class.getName());

    /**
     * Test Persistent Timer method access from a servlet. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    @Test
    public void testTimerAccessFromServlet() throws Exception {

        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        TimerService ts = slt.getTimerService();

        // -------------------------------------------------------------------
        // 1 - Verify TimerService.createSingleActionTimer fails
        // -------------------------------------------------------------------
        svLogger.info("testTimerAccessFromServlet: calling TimerService.createSingleActionTimer");
        try {
            TimerConfig timerConfig = new TimerConfig("Servlet", false);
            Timer timer = ts.createSingleActionTimer(0, timerConfig);
            fail("1 ---> Successfully created timer from servlet : " + timer);
        } catch (IllegalStateException ex) {
            svLogger.info("1 ---> Caught expected exception creating timer : " + ex);
        }

        // -------------------------------------------------------------------
        // 2 - Verify TimerService.getTimers fails
        // -------------------------------------------------------------------
        svLogger.info("testTimerAccessFromServlet: calling TimerService.getTimers");
        try {
            Collection<Timer> timers = ts.getTimers();
            fail("2 ---> Successfully called TimerService.getTimers() from servlet : " + timers);
        } catch (IllegalStateException ex) {
            svLogger.info("2 ---> Caught expected exception from getTimers : " + ex);
        }

        // -------------------------------------------------------------------
        // 3 - Verify TimerService.getAllTimers fails
        // -------------------------------------------------------------------
        svLogger.info("testTimerAccessFromServlet: calling TimerService.getAllTimers");
        try {
            Collection<Timer> timers = ts.getAllTimers();
            fail("3 ---> Successfully called TimerService.getAllTimers() from servlet : " + timers);
        } catch (IllegalStateException ex) {
            svLogger.info("3 ---> Caught expected exception from getAllTimers : " + ex);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may be performed.
        // - must get a Timer from a bean, since this is not Timer bean
        // -----------------------------------------------------------------------
        svLogger.info("testTimerService: Creating timer for StatelessTimedBean");
        Timer timer = slt.createTimer("Servlet");

        // -------------------------------------------------------------------
        // 4 - Verify Timer.getTimeRemaining() on single event Timer works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.getTimeRemaining()");
        long remaining = timer.getTimeRemaining();
        Assert.assertTrue("4 ---> Timer.getTimeRemaining() worked: " + remaining,
                          remaining >= 1 && remaining <= StatelessTimedBean.DEFAULT_EXPIRATION);

        // -------------------------------------------------------------------
        // 5 - Verify Timer.getInfo() returning serializable works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.getInfo()");
        Object timerInfo = timer.getInfo();
        Assert.assertEquals("5 ---> Timer.getInfo() worked: " + timerInfo,
                            "Servlet", timerInfo);

        // -------------------------------------------------------------------
        // 6 - Verify Timer.getHandle() works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.getHandle()");
        Assert.assertNotNull("6 ---> Timer.getHandle() did not work", timer.getHandle());

        // -------------------------------------------------------------------
        // 7 - Verify Timer.cancel() works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.cancel()");
        timer.cancel();
        svLogger.info("7 ---> Timer.cancel() worked");

        // -------------------------------------------------------------------
        // 8 - Verify NoSuchObjectLocalException occurs accessing canceled
        //     timer
        // -------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling Timer.getInfo() on cancelled Timer");
            timerInfo = timer.getInfo();
            fail("8 ---> Timer.getInfo() worked - expected NoSuchObjectLocalException : " + timerInfo);
        } catch (NoSuchObjectLocalException nso) {
            svLogger.info("8 ---> Caught expected exception : " + nso);
        }

    }
}
