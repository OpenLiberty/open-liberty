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

package com.ibm.ws.ejbcontainer.timer.persistent.core.web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatelessHome;
import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatelessObject;
import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatelessTimedHome;
import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatelessTimedObject;

import componenttest.app.FATServlet;

/**
 * Test to exercise the EJB Container Timer Service using Stateless Session
 * beans. Verifies the "Allowed Operations" table in the EJB specification
 * for Stateless Session beans.
 */
@WebServlet("/TimerSLOperationsServlet")
@SuppressWarnings("serial")
public class TimerSLOperationsServlet extends FATServlet {

    private static final Logger svLogger = Logger.getLogger(TimerSLOperationsServlet.class.getName());
    private static final long MAX_TIMER_WAIT = 2 * 60 * 1000;

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that does not implement the TimedObject interface. <p>
     *
     * This test will confirm the following :
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
    @Test
    public void testSLTimerServiceNotTimedObject() throws Exception {
        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessHome SLHome = (StatelessHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateless");

        // Create a bean to execute the test.
        StatelessObject sl = SLHome.create();

        // --------------------------------------------------------------------
        // Execute the test by calling a method on the bean... append results
        // --------------------------------------------------------------------

        svLogger.info("Enter EJB method : testTimerService()");
        sl.testTimerService();

        svLogger.info("Exit EJB method : testTimerService()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sl.remove();
    }

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateless Session bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSLTimerServiceSetSessionContext() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - setSessionContext may not be called directly....
        // --------------------------------------------------------------------

        svLogger.info("Enter EJB method : setSessionContext()");

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        slt.verifySetSessionContextResults();

        svLogger.info("Exit EJB method : setSessionContext()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        slt.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbCreate on a
     * Stateless Session bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSLTimerServiceEJBCreate() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - ejbCreate may not be called directly....
        // --------------------------------------------------------------------

        svLogger.info("Enter EJB method : ejbCreate()");

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        slt.verifyEjbCreateResults();

        svLogger.info("Exit EJB method : ejbCreate()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        slt.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbRemove on a
     * Stateless Session bean that implements the TimedObject interface. <p>
     */
    @Test
    public void testSLTimerServiceEJBRemove() throws Exception {
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");
        StatelessTimedObject slt = SLTHome.create();
        slt.recursiveCall(501); // overflows the default pool size
        slt.verifyEjbRemoveResults();
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer(duration, null) works
     * <li> TimerService.createTimer(duration, info) works
     * <li> TimerService.createTimer(duration, interval, info) works
     * <li> TimerService.createTimer(date, info) works
     * <li> TimerService.createTimer(date, interval, info) works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> TimerService.getTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * <li> ejbTimeout is executed for valid Timers
     * <li> NoSuchObjectLocalException occurs accessing expired timer
     * <li> TimerService.getTimers() does not return expired Timers
     * <li> Timer.getNextTimeout() on repeating Timer works
     * <li> ejbTimeout is executed multiple times for repeating Timers
     * <li> NoSuchObjectLocalException occurs accessing self cancelled timer
     * <li> TimerService.getTimers() returns empty collection after all Timers
     * have expired or been cancelled.
     * </ol>
     */
    @Test
    public void testSLTimerServiceBeanMethod() throws Exception {
        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        // --------------------------------------------------------------------
        // Execute the test by calling a method on the bean... append results
        // --------------------------------------------------------------------

        svLogger.info("Enter EJB method : testTimerService(1)");

        slt.testTimerServicePhase1();

        svLogger.info("Exit EJB method : testTimerService(1)");

        svLogger.info("Enter EJB method : testTimerService(2)");

        slt.testTimerServicePhase2();

        svLogger.info("Exit EJB method : testTimerService(2)");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        slt.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer(duration, null) works
     * <li> TimerService.createTimer(duration, info) works
     * <li> TimerService.createTimer(duration, interval, info) works
     * <li> TimerService.createTimer(date, info) works
     * <li> TimerService.createTimer(date, interval, info) works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> TimerService.getTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * <li> ejbTimeout is executed for valid Timers
     * <li> NoSuchObjectLocalException occurs accessing expired timer
     * <li> TimerService.getTimers() does not return expired Timers
     * <li> Timer.getNextTimeout() on repeating Timer works
     * <li> ejbTimeout is executed multiple times for repeating Timers
     * <li> NoSuchObjectLocalException occurs accessing self cancelled timer
     * <li> TimerService.getTimers() returns empty collection after all Timers
     * have expired or been cancelled.
     * </ol>
     */
    @Test
    public void testSLTimerServiceEJBTimeout() throws Exception {
        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean... append results
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testTimerService");

        svLogger.info("Waiting for timer to expire ... ");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Enter EJB method : ejbTimeout()");

        slt.verifyEjbTimeoutResults();

        svLogger.info("Exit EJB method : ejbTimeout()");

        svLogger.info("Enter EJB method : testTimerService(2)");

        slt.testTimerServicePhase2();

        svLogger.info("Exit EJB method : testTimerService(2)");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        slt.remove();
    }

    /**
     * Test SessionContext method access from ejbTimeout on a CMT Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> SessionContext.getEJBObject() works
     * <li> SessionContext.getEJBLocalObject() works
     * <li> SessionContext.getMessageContext() fails with IllegalStateException
     * <li> EJBContext.getEJBHome() works
     * <li> EJBContext.getEJBLocalHome() works
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getRollbackOnly() - false works
     * <li> EJBContext.setRollbackOnly() works
     * <li> EJBContext.getRollbackOnly() - true works
     * <li> ejbTimeout is executed again when setRollbackOnly called.
     * </ol>
     */
    @Test
    public void testSLTimerServiceEJBTimeoutSessionContextCMT() throws Exception {
        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean... append results
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testContextMethods-CMT");

        svLogger.info("Waiting for timer to expire ...");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Enter EJB method : ejbTimeout()");
        slt.verifyEjbTimeoutResults();

        svLogger.info("Exit EJB method : ejbTimeout()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        slt.remove();
    }

    /**
     * Test SessionContext method access from ejbTimeout on a BMT Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> SessionContext.getEJBObject() works
     * <li> SessionContext.getEJBLocalObject() works
     * <li> SessionContext.getMessageContext() fails with IllegalStateException
     * <li> EJBContext.getEJBHome() works
     * <li> EJBContext.getEJBLocalHome() works
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getUserTransaction() works
     * <li> UserTransaction.begin() works
     * <li> EJBContext.getRollbackOnly() fails
     * <li> EJBContext.setRollbackOnly() fails
     * <li> UserTransaction.commit() works
     * </ol>
     */
    @Test
    public void testSLTimerServiceEJBTimeoutSessionContextBMT() throws Exception {
        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimedBMT");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean... append results
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testContextMethods-BMT");
        svLogger.info("Waiting for timer to expire ...");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Enter EJB method : ejbTimeout()");
        slt.verifyEjbTimeoutResults();

        svLogger.info("Exit EJB method : ejbTimeout()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        slt.remove();
    }

    /**
     * Test TimerService.createTimer() IllegalArgumentExceptions from
     * a method on a Stateless Session bean that implements the TimedObject
     * interface. <p>
     *
     * This test will confirm an IllegalArgumentException for the following :
     * <ol>
     * <li> TimerService.createTimer(duration, null)
     * - where duration is negative
     * <li> TimerService.createTimer(duration, interval, info)
     * - where duration is negative
     * <li> TimerService.createTimer(duration, interval, info)
     * - where interval is negative
     * <li> TimerService.createTimer(date, info)
     * - where date is null
     * <li> TimerService.createTimer(date, info)
     * - where date.getTime() is negative
     * <li> TimerService.createTimer(date, interval, info)
     * - where date is null
     * <li> TimerService.createTimer(date, interval, info)
     * - where date.getTime() is negative
     * <li> TimerService.createTimer(date, interval, info)
     * - where interval is negative
     * </ol>
     */
    @Test
    public void testSLTimerServiceInvalidTimerServiceArguments() throws Exception {
        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatelessTimedHome SLTHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean to execute the test.
        StatelessTimedObject slt = SLTHome.create();

        // --------------------------------------------------------------------
        // Execute the test by calling a method on the bean... append results
        // --------------------------------------------------------------------

        svLogger.info("Enter EJB method : testCreateTimerExceptions()");
        slt.testCreateTimerExceptions();

        svLogger.info("Exit EJB method : testCreateTimerExceptions()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        slt.remove();
    }
}
