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

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;

import javax.ejb.Timer;
import javax.ejb.TimerService;

/**
 * Local interface for a basic Stateless Session bean that implements a
 * timeout method. It contains methods to test TimerService access.
 **/
public interface StatelessTimedLocal {

    public static final long DEFAULT_EXPIRATION = 60 * 1000;
    public static final long EXPIRATION = 4 * 1000;
    public static final long INTERVAL = 4 * 1000;
    public static final long MAX_TIMER_WAIT = 3 * 60 * 1000;
    public static final long TIMER_PRECISION = 900;

    /**
     * Verifies the results of testing performed in {@link StatelessTimedBean#setSessionContext setSessionContext()}. <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifySetSessionContextResults();

    /**
     * Verifies the results of testing performed in {@link StatelessTimedBean#ejbCreate PostConstruct}. <p>
     *
     * Since PostConstruct may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyPostConstructResults();

    /**
     * Verifies the results of testing performed in {@link StatelessTimedBean#ejbRemove PreDestroy}. <p>
     *
     * Since PreDestroy may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyPreDestroyResults();

    /**
     * Verifies the results of testing performed in {@link StatelessTimedBean#ejbTimeout ejbTimeout()}. <p>
     *
     * Since ejbTimeout may not be called directly, the results are
     * stored in a static variable for later verification. <p>
     **/
    public void verifyEjbTimeoutResults();

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements a timeout callback method. <p>
     *
     * This test is executed in 2 phases. The first phase will create
     * multiple Timers, and the second phase will confirm that they are
     * executed correctly. This cannot all be done in one method call,
     * as the Timers will not execute until after their creation has
     * been committed (at the end of phase 1 - and the return from the
     * method call). <p>
     *
     * This test method will confirm the following in Phase 1:
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createSingleActionTimer with null info works
     * <li> TimerService.createSingleActionTimer with info works
     * <li> TimerService.createIntervalTimer with info works
     * <li> TimerService.createSingleActionTimer with date and info works
     * <li> TimerService.createIntervalTimer with date and info works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> TimerService.getTimers() returns all created Timers
     * <li> TimerService.getAllTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * </ol> <p>
     */
    public void testTimerServicePhase1();

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements a timeout callback method. <p>
     *
     * This test is executed in 2 phases. The first phase will create
     * multiple Timers, and the second phase will confirm that they are
     * executed correctly. This cannot all be done in one method call,
     * as the Timers will not execute until after their creation has
     * been committed (at the end of phase 1 - and the return from the
     * method call). <p>
     *
     * This test method will confirm the following in Phase 2 :
     * <ol start=17>
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
    public void testTimerServicePhase2();

    /**
     * Utility method to create a Timer. This method is for use
     * by tests that are testing the expiration of a Timer and execution
     * of the ejbTimeout method. <p>
     *
     * @param duration duration of the Timer to create
     * @param info info parameter passed through to the createTimer call
     *
     * @return CountDownLatch that can be used wait for the timer to run.
     **/
    public CountDownLatch createTimer(long duration, Serializable info);

    /**
     * Utility method that may be used to obtain a TimerService reference
     * external to an EJB. Used for testing access to TimerService methods
     * external to a bean. <p>
     **/
    public TimerService getTimerService();

    /**
     * Utility method that may be used to create a Timer when a Timer is
     * required to perform a test, but cannot be created directly by
     * the bean performing the test. For example, if the bean performing
     * the test does not implement a timeout callback method. <p>
     *
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with 1 minute duration and specified info.
     **/
    public Timer createTimer(Serializable info);

    /**
     * Recursive call to trigger creation of multiple bean instances.
     */
    public void recursiveCall(int depth);

    public void clearAllTimers();
}
