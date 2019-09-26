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

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;

import javax.ejb.EJBLocalObject;
import javax.ejb.Timer;
import javax.ejb.TimerService;

/**
 * Remote interface for a basic Stateless Session that implements the
 * TimedObject interface. It contains methods to test TimerService access.
 **/
public interface StatelessTimedObject extends EJBLocalObject {

    /**
     * Utility method that may be used to create a Timer when a Timer is
     * required to perform a test, but cannot be created directly by
     * the bean performing the test. For example, if the bean performing
     * the test does not implement the TimedObject interface. <p>
     *
     * Local interface only! <p>
     *
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with 1 minutie duration and specified info.
     **/
    public Timer createTimer(Serializable info);

    /**
     * Returns the results of testing performed in StatelessTimedBean.setSessionContext(). <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     *
     * @return Results of testing performed in setSesionContext method.
     **/
    public void verifySetSessionContextResults();

    /**
     * Returns the results of testing performed in StatelessTimedBean.ejbCreate(). <p>
     *
     * Since ejbCreate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbCreate method.
     **/
    public void verifyEjbCreateResults();

    /**
     * Returns the results of testing performed in StatelessTimedBean.ejbTimeout()}. <p>
     *
     * Since ejbTimeout may not be called directly, the results are
     * stored in a static variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbTimeout method.
     **/
    public void verifyEjbTimeoutResults();

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test is executed in 2 phases. The first phase will create
     * multiple Timers, and the second phase will confirm that they are
     * executed correctly. This cannot all be done in one method call,
     * as the Timers will not execute until after thier creation has
     * been committed (at the end of phase 1 - and the return from the
     * method call). <p>
     *
     * This test method will confirm the following in Phase 1:
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.create(duration, null) works
     * <li> TimerService.create(duration, info) works
     * <li> TimerService.create(duration, interval, info) works
     * <li> TimerService.create(date, info) works
     * <li> TimerService.create(date, interval, info) works
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
     * </ol> <p>
     *
     * This test method will confirm the following in Phase 2 :
     * <ol start=18>
     * <li> ejbTimeout is executed for single event Timers & not cancelled Timers
     * <li> NoSuchObjectLocalException occurs accessing expired timer
     * <li> TimerService.getTimers() does not return expired Timers
     * <li> Timer.getNextTimeout() on repeating Timer works
     * <li> ejbTimeout is executed multiple times for repeating Timers
     * <li> NoSuchObjectLocalException occurs accessing self cancelled timer
     * <li> TimerService.getTimers() returns empty collection after all Timers
     * have expired or been cancelled.
     * </ol>
     */
    public void testTimerServicePhase1();

    public void testTimerServicePhase2();

    /**
     * Test TimerService.createTimer() IllegalArgumentExceptions from
     * a method on a Stateless Session bean that implements the TimedObject
     * interface. <p>
     *
     * This test method will confirm an IllegalArgumentException for the following :
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
    public void testCreateTimerExceptions();

    /**
     * Utility method to create a Timer remotely. This method is for use
     * by tests that are testing the expiration of a Timer and execution
     * of the ejbTimeout method. <p>
     *
     * @param duration duration of the Timer to create
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with the duration and info specified.
     **/
    public CountDownLatch createTimer(long duration, Serializable info);

    public void recursiveCall(int depth);

    public void verifyEjbRemoveResults();

    /**
     * Utility method that may be used to obtain a TimerService reference
     * external to an EJB. Used for testing access to TimerService methods
     * external to a bean. <p>
     **/
    public TimerService getTimerService();

}
