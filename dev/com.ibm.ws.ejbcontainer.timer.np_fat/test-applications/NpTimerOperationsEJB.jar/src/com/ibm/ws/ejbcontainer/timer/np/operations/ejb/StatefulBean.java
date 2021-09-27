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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;

import org.junit.Assert;

/**
 * Bean implementation for a basic Stateful Session bean. It contains
 * methods to test TimerService access.
 **/
@Stateful
public class StatefulBean implements StatefulLocal {

    private static final Logger logger = Logger.getLogger(StatefulBean.class.getName());

    private SessionContext context;

    protected String message;

    // For testing passivation/activation of Timer/TimerHandle
    private boolean timerPassivated, timerActivated;
    private Timer savedTimer;

    // These fields hold the test results for EJB callback methods
    private Object setSessionContextResults;
    private Object postConstructResults;
    private Object ejbActivateResults;
    private Object ejbPassivateResults;
    private Object afterBeginResults;
    private Object beforeCompletionResults;
    private Object afterCompletionResults;
    private Object timerPassivationResults;
    private static Object PreDestroyResults = null;
    public static boolean TestPostConstruct = false;

    @EJB(beanName = "StatelessTimedBean")
    private StatelessTimedLocal slBean;

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #verifySetSessionContextResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol> <p>
     *
     * @param sc session context provided by container.
     */
    @Resource
    public void setSessionContext(SessionContext sc) {

        context = sc;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("setSessionContext: Calling getTimerService()");
            context.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            logger.log(Level.INFO, "1 ---> Caught expected exception", ise);
            setSessionContextResults = true;
        } catch (Throwable th) {
            setSessionContextResults = th;
        }
    }

    private void verifyResults(Object results) {

        if (results instanceof Throwable) {
            throw new Error((Throwable) results);
        }

        Assert.assertEquals(true, results);
    }

    /**
     * Verifies the results of testing performed in {@link #setSessionContext setSessionContext()}. <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    @Override
    public void verifySetSessionContextResults() {
        verifyResults(setSessionContextResults);
    }

    /**
     * Test getTimerService()/TimerService access from PostConstruct, PreDestroy,
     * ejbActivate, and ejbPassivate on a Stateful Session bean. <p>
     *
     * Since these methods have the same behavior in regards to TimerService
     * access, this internal method has been provided for all of them. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol> <p>
     */
    private void testNoTimerAccess() throws Exception {

        Timer timer = null;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info(message + ": Calling getTimerService()");
            context.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            logger.log(Level.INFO, "1 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may not be performed.
        // - must get a Timer from a different bean, since this is not TimedObject
        // -----------------------------------------------------------------------
        // For afterCompletion testing, timer was pre-created in beforeCompletion
        if (message != null && message.equals("afterCompletion"))
            timer = savedTimer;
        else
            timer = createTimer();

        // -----------------------------------------------------------------------
        // 2 - Verify Timer.getTimeRemaining() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info(message + ": Calling Timer.getTimeRemaining()");
            long remaining = timer.getTimeRemaining();
            Assert.fail("2 ---> getTimeRemaining should have failed! " + remaining);
        } catch (IllegalStateException ise) {
            logger.log(Level.INFO, "2 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify Timer.getInfo() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info(message + ": Calling Timer.getInfo()");
            Object curInfo = timer.getInfo();
            Assert.fail("3 ---> getInfo should have failed! " + curInfo);
        } catch (IllegalStateException ise) {
            logger.log(Level.INFO, "3 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 4 - Verify Timer.getHandle() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info(message + ": Calling Timer.getHandle()");
            timer.getHandle();
            Assert.fail("4 ---> getHandle should have failed!");
        } catch (IllegalStateException ise) {
            logger.log(Level.INFO, "4 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify Timer.cancel() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info(message + ": Calling Timer.cancel()");
            timer.cancel();
            Assert.fail("5 ---> cancel should have failed!");
        } catch (IllegalStateException ise) {
            logger.log(Level.INFO, "5 ---> Caught expected exception", ise);
        }
    }

    private Object testNoTimerAccessIndirect() {

        try {
            testNoTimerAccess();
            return true;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "test failure", t);
            return t;
        }
    }

    /**
     * Test getTimerService()/TimerService access from PostConstruct on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #verifyPostConstructResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol> <p>
     */
    @PostConstruct
    public void postConstruct() throws CreateException {

        if (TestPostConstruct) {
            postConstructResults = testNoTimerAccessIndirect();
        }
    }

    /**
     * Verifies the results of testing performed in {@link #postConstruct postConstruct()}. <p>
     *
     * Since postConstruct may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    @Override
    public void verifyPostConstructResults() {
        verifyResults(postConstructResults);
    }

    /**
     * Test getTimerService()/TimerService access from PreDestroy on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container removes a Stateful bean, the results will be stored
     * in an static variable. The test may then extract the results
     * ({@link #verifyPreDestroyResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol> <p>
     */
    @PreDestroy
    public void preDestroy() {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (message != null && message.equals("preDestroy"))
            PreDestroyResults = testNoTimerAccessIndirect();
    }

    /**
     * Verifies the results of testing performed in {@link #preDestroy preDestroy()}. <p>
     *
     * Since PreDestroy may not be called directly, the results are
     * stored in an static variable for later verification. <p>
     **/
    @Override
    public void verifyPreDestroyResults() {
        verifyResults(PreDestroyResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbActivate on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container accesses a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #verifyEjbActivateResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol> <p>
     */
    @PostActivate
    public void ejbActivate() {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (message != null && message.equals("ejbActivate")) {
            ejbActivateResults = testNoTimerAccessIndirect();
            message += ":done"; // perform test only 1 time
        }

        // If testing that a Timer (and TimerHandle) may be passivated as
        // part of the beans state, then turn on the booleans indicating
        // if the state was set properly after activation.
        else if (message != null && message.equals("TimerPassivation")) {
            if (savedTimer != null && timerPassivationResults != null) {
                timerActivated = true;
                logger.info("ejbActivate: Timer is set");
            }
        }
    }

    /**
     * Verifies the results of testing performed in {@link #ejbActivate ejbActivate()}. <p>
     *
     * Since ejbActivate may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    @Override
    public void verifyEjbActivateResults() {
        verifyResults(ejbActivateResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbPassivate on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container accesses a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #verifyEjbPassivateResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol> <p>
     */
    @PrePassivate
    public void ejbPassivate() {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (message != null && message.equals("ejbPassivate")) {
            ejbPassivateResults = testNoTimerAccessIndirect();
            message += ":done"; // perform test only 1 time
        }

        // If testing that a Timer (and TimerHandle) may be passivated as
        // part of the beans state, then turn on the booleans indicating
        // if the state was set properly prior to passivation.
        else if (message != null && message.equals("TimerPassivation")) {
            if (savedTimer != null && timerPassivationResults != null) {
                timerPassivated = true;
                logger.info("ejbPassivate: Timer is set");
            }
        }
    }

    /**
     * Verifies the results of testing performed in {@link #ejbPassivate ejbPassivate()}. <p>
     *
     * Since ejbPassivate may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    @Override
    public void verifyEjbPassivateResults() {
        verifyResults(ejbPassivateResults);
    }

    /**
     * Test getTimerService()/TimerService access from business method,
     * afterBegin, and beforeCompletion on a Stateful Session bean. <p>
     *
     * Since these methods have the same behavior in regards to TimerService
     * access, this method has been provided for all of them. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol> <p>
     */
    @Override
    public void testTimerAccess() {

        Timer timer = null;
        TimerHandle timerHandle = null;
        String timerInfo = "StatelessBean:StatefulBean." + message;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testTimerAccess: Calling getTimerService()");
            context.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            logger.log(Level.INFO, "1 ---> Caught expected exception", ise);
        }

        // Obtain a TimerService from another bean that supports timers.
        TimerService ts = slBean.getTimerService();

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
        // 4 - Verify TimerService.getAllTimers() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling getAllTimers()");
            Collection<Timer> timers = ts.getAllTimers();
            Assert.fail("4 ---> getAllTimers should have failed! " + timers);
        } catch (IllegalStateException ise) {
            logger.info("4 ---> Caught expected exception : " + ise);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may be performed.
        // - must get a Timer from a different bean, since this is not TimedObject
        // -----------------------------------------------------------------------
        timer = createTimer();

        // -----------------------------------------------------------------------
        // 5 - Verify Timer.getTimeRemaining() works
        // -----------------------------------------------------------------------
        logger.info("testTimerAccess: Calling Timer.getTimeRemaining()");
        long remaining = timer.getTimeRemaining();
        Assert.assertTrue("5 ---> Timer.getTimeRemaining() worked: " + remaining,
                          remaining >= 1 && remaining <= StatelessTimedLocal.DEFAULT_EXPIRATION);

        // -----------------------------------------------------------------------
        // 6 - Verify Timer.getInfo() works
        // -----------------------------------------------------------------------
        logger.info("testTimerAccess: Calling Timer.getInfo()");
        Object curInfo = timer.getInfo();
        Assert.assertEquals("6 ---> Timer.getInfo() worked: " + curInfo +
                            "", timerInfo, curInfo);

        // -----------------------------------------------------------------------
        // 7 - Verify Timer.getHandle() fails with IllegalStateException
        // -----------------------------------------------------------------------
        logger.info("testTimerAccess(" + message + "): Calling Timer.getHandle()");
        try {
            timerHandle = timer.getHandle();
            fail("7 ---> Timer.getHandle() worked: " + timerHandle);
        } catch (IllegalStateException ex) {
            logger.info("7 ---> Caught expected exception : " + ex);
        }

        // -----------------------------------------------------------------------
        // 8 - Verify Timer.cancel() works
        // -----------------------------------------------------------------------
        logger.info("testTimerAccess: Calling Timer.cancel()");
        timer.cancel();

        // -----------------------------------------------------------------------
        // 9 - Verify NoSuchObjectLocalException occurs accessing canceled timer
        // -----------------------------------------------------------------------
        try {
            logger.info("testTimerAccess: Calling Timer.getInfo() " +
                        "on cancelled Timer");
            timer.getInfo();
            Assert.fail("9 ---> Timer.getInfo() worked - " +
                        "expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            logger.log(Level.INFO, "9 ---> Caught expected exception", nso);
        }
    }

    private Object testTimerAccessIndirect() {

        try {
            testTimerAccess();
            return true;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "test failure", t);
            return t;
        }
    }

    /**
     * Test getTimerService()/TimerService access from afterBegin on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container accesses a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #verifyAfterBeginResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol> <p>
     */
    @AfterBegin
    public void afterBegin() {

        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (message != null && message.equals("afterBegin")) {
            afterBeginResults = testTimerAccessIndirect();
            message += ":done"; // perform test only 1 time
        }
    }

    /**
     * Verifies the results of testing performed in {@link #afterBegin afterBegin()}. <p>
     *
     * Since afterBegin may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    @Override
    public void verifyAfterBeginResults() {
        verifyResults(afterBeginResults);
    }

    /**
     * Test getTimerService()/TimerService access from beforeCompletion on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container accesses a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #verifyBeforeCompletionResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol> <p>
     */
    @BeforeCompletion
    public void beforeCompletion() {

        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (message != null && message.equals("beforeCompletion")) {
            beforeCompletionResults = testTimerAccessIndirect();
            message += ":done"; // perform test only 1 time
        }

        // If afterCompletion is the message, then create a Timer in an
        // instance variable for use in afterCompletion, since afterCompletion
        // does not have EJB access, and therefore cannot create a Timer.
        else if (message != null && message.equals("afterCompletion")) {
            if (afterCompletionResults == null) {
                try {
                    savedTimer = createTimer();
                    afterCompletionResults = true;
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "test failure", t);
                    afterCompletionResults = t;
                }
            }
        }

        // If TimerPassivation is the message, then create a Timer in an
        // instance variable to test that a Timer (and TimerHandle) may
        // be 'passivated' as part of a Stateful beans state.
        else if (message != null && message.equals("TimerPassivation")) {
            if (timerPassivationResults == null) {
                try {
                    savedTimer = createTimer();
                    timerPassivationResults = true;
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "test failure", t);
                    timerPassivationResults = t;
                }
            }
        }
    }

    /**
     * Verifies the results of testing performed in {@link #beforeCompletion beforeCompletion()}. <p>
     *
     * Since beforeCompletion may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    @Override
    public void verifyBeforeCompletionResults() {
        verifyResults(beforeCompletionResults);
    }

    /**
     * Test getTimerService()/TimerService access from afterCompletion on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #verifyAfterCompletionResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol> <p>
     *
     * @param flag true/false if committed/rolledback.
     */
    @AfterCompletion
    public void afterCompletion(boolean flag) {

        // If this methods name was passed on the create, then perform the
        // test...
        if (message != null && message.equals("afterCompletion")) {
            if (afterCompletionResults instanceof Boolean) {
                afterCompletionResults = testNoTimerAccessIndirect();
                message += ":done"; // perform test only 1 time
                savedTimer = null;
            }
        }
    }

    /**
     * Verifies the results of testing performed in {@link #afterCompletion afterCompletion()}. <p>
     *
     * Since afterCompletion may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    @Override
    public void verifyAfterCompletionResults() {
        verifyResults(afterCompletionResults);
    }

    /**
     * Internal method for creating a Timer that may be used for testing
     * Timer method access. <p>
     *
     * @return a Timer for a Stateless Session bean.
     **/
    private Timer createTimer() {

        logger.info("Creating a timer for StatelessTimed Bean ...");
        String timerInfo = "StatelessBean:StatefulBean." + message;
        Timer timer = slBean.createTimer(timerInfo);
        logger.info("Timer Created: " + timerInfo);

        return timer;
    }

    /**
     * Test that a Timer and TimerHandle will be serialized by the EJB Container
     * as part of the state of a Stateful Session bean. <p>
     *
     * A Timer and TimerHandle will be set as instance data during
     * beforeCompletion. They will be passivated after beforeCompletion,
     * and then activated when this method is called. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Timer is serialized successfully.
     * <li> TimerHandle is serialized successfully.
     * </ol>
     */
    @Override
    public void verifyTimerPassivationResults() {

        verifyResults(timerPassivationResults);

        // --------------------------------------------------------------------
        // 1 - Verify Timer is serialized successfully
        // --------------------------------------------------------------------
        if (timerPassivated && timerActivated && savedTimer != null) {
            logger.info("1 ---> Timer serialized successfully");
        } else {
            Assert.fail("1 ---> Timer not serialized : " +
                        "Timer = " + savedTimer + ", " +
                        "passivated = " + timerPassivated + ", " +
                        "activated = " + timerActivated);
        }

        message += ":done"; // perform test only 1 time
        savedTimer = null;
    }

    @Override
    @Remove
    public void remove() {
        logger.info(getClass().getSimpleName() + ".remove(");
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
