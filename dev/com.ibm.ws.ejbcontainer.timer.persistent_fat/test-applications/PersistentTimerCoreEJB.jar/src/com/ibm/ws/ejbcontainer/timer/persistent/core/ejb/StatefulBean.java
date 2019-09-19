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

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.naming.InitialContext;

import org.junit.Assert;

/**
 * Bean implementation for a basic Stateful Session bean. It contains
 * methods to test TimerService access.
 **/
@SuppressWarnings("serial")
public class StatefulBean implements SessionBean, SessionSynchronization {
    private static final Logger svLogger = Logger.getLogger(StatefulBean.class.getName());

    private SessionContext ivContext;

    protected String ivMessage;

    // For testing passivation/activation of Timer/TimerHandle
    private boolean ivTimerPassivated, ivTimerActivated;
    private boolean ivHandlePassivated, ivHandleActivated;
    private Timer ivTimer;
    private TimerHandle ivTimerHandle;

    // These fields hold the test results for EJB callback methods
    private Object ivSetSessionContextResults;
    private Object ivEjbCreateResults;
    private Object ivEjbActivateResults;
    private Object ivEjbPassivateResults;
    private Object ivAfterBeginResults;
    private Object ivBeforeCompletionResults;
    private Object ivAfterCompletionResults;
    private Object ivTimerPassivationResults;
    private static Object svEjbRemoveResults = null;

    /** Required default constructor. **/
    public StatefulBean() {
    }

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #getSetSessionContextResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol> <p>
     *
     * @param sc session context provided by container.
     */
    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info("setSessionContext: Calling getTimerService()");
            ivContext.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "1 ---> Caught expected exception", ise);
            ivSetSessionContextResults = true;
        } catch (Throwable th) {
            ivSetSessionContextResults = th;
        }
    }

    private void verifyResults(Object results) {
        if (results instanceof Throwable) {
            throw new Error((Throwable) results);
        }

        Assert.assertEquals(true, results);
    }

    /**
     * Returns the results of testing performed in {@link #setSessionContext setSessionContext()}. <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in setSesionContext method.
     **/
    public void verifySetSessionContextResults() {
        verifyResults(ivSetSessionContextResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbCreate, ejbRemove,
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
            svLogger.info(ivMessage + ": Calling getTimerService()");
            ivContext.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "1 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may not be performed.
        // - must get a Timer from a different bean, since this is not TimedObject
        // -----------------------------------------------------------------------
        // For afterCompletion testing, timer was pre-created in beforeCompletion
        if (ivMessage != null && ivMessage.equals("afterCompletion"))
            timer = ivTimer;
        else
            timer = createTimer();

        // -----------------------------------------------------------------------
        // 2 - Verify Timer.getTimeRemaining() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info(ivMessage + ": Calling Timer.getTimeRemaining()");
            long remaining = timer.getTimeRemaining();
            Assert.fail("2 ---> getTimeRemaining should have failed! " + remaining);
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "2 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify Timer.getInfo() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info(ivMessage + ": Calling Timer.getInfo()");
            Object curInfo = timer.getInfo();
            Assert.fail("3 ---> getInfo should have failed! " + curInfo);
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "3 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 4 - Verify Timer.getHandle() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info(ivMessage + ": Calling Timer.getHandle()");
            timer.getHandle();
            Assert.fail("4 ---> getHandle should have failed!");
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "4 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify Timer.cancel() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info(ivMessage + ": Calling Timer.cancel()");
            timer.cancel();
            Assert.fail("5 ---> cancel should have failed!");
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "5 ---> Caught expected exception", ise);
        }
    }

    private Object testNoTimerAccessIndirect() {
        try {
            testNoTimerAccess();
            return true;
        } catch (Throwable t) {
            svLogger.log(Level.SEVERE, "test failure", t);
            return t;
        }
    }

    /**
     * Test getTimerService()/TimerService access from ejbCreate on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #getEjbCreateResults}). <p>
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
    public void ejbCreate() throws CreateException {
        ivMessage = "StatefulBean - default message";
    }

    public void ejbCreate(String message) throws CreateException {
        ivMessage = message;

        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (ivMessage != null && ivMessage.equals("ejbCreate")) {
            ivEjbCreateResults = testNoTimerAccessIndirect();
        }
    }

    /**
     * Returns the results of testing performed in {@link #ejbCreate ejbCreate()}. <p>
     *
     * Since ejbCreate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbCreate method.
     **/
    public void verifyEjbCreateResults() {
        verifyResults(ivEjbCreateResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbRemove on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container removes a Stateful bean, the results will be stored
     * in an static variable. The test may then extract the results
     * ({@link #getEjbRemoveResults}). <p>
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
    @Override
    public void ejbRemove() {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (ivMessage != null && ivMessage.equals("ejbRemove"))
            svEjbRemoveResults = testNoTimerAccessIndirect();
    }

    /**
     * Returns the results of testing performed in {@link #ejbRemove ejbRemove()}. <p>
     *
     * Since ejbRemove may not be called directly, the results are
     * stored in an static variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbRemove method.
     **/
    public void verifyEjbRemoveResults() {
        verifyResults(svEjbRemoveResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbActivate on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container accesses a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #getEjbActivateResults}). <p>
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
    @Override
    public void ejbActivate() {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (ivMessage != null && ivMessage.equals("ejbActivate")) {
            ivEjbActivateResults = testNoTimerAccessIndirect();

            ivMessage += ":done"; // perform test only 1 time
        }

        // If testing that a Timer (and TimerHandle) may be passivated as
        // part of the beans state, then turn on the booleans indicating
        // if the state was set properly after activation.
        else if (ivMessage != null && ivMessage.equals("TimerPassivation")) {
            if (ivTimer != null && ivTimerPassivationResults != null) {
                ivTimerActivated = true;
                svLogger.info("ejbActivate: Timer is set");
            }
            if (ivTimerHandle != null && ivTimerPassivationResults != null) {
                ivHandleActivated = true;
                svLogger.info("ejbActivate: TimerHandle is set ");
            }
        }
    }

    /**
     * Returns the results of testing performed in {@link #ejbActivate ejbActivate()}. <p>
     *
     * Since ejbActivate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbActivate method.
     **/
    public void verifyEjbActivateResults() {
        verifyResults(ivEjbActivateResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbPassivate on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container accesses a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #getEjbPassivateResults}). <p>
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
    @Override
    public void ejbPassivate() {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (ivMessage != null && ivMessage.equals("ejbPassivate")) {
            ivEjbPassivateResults = testNoTimerAccessIndirect();

            ivMessage += ":done"; // perform test only 1 time
        }

        // If testing that a Timer (and TimerHandle) may be passivated as
        // part of the beans state, then turn on the booleans indicating
        // if the state was set properly prior to passivation.
        else if (ivMessage != null && ivMessage.equals("TimerPassivation")) {
            if (ivTimer != null && ivTimerPassivationResults != null) {
                ivTimerPassivated = true;
                svLogger.info("ejbPassivate: Timer is set");
            }
            if (ivTimerHandle != null && ivTimerPassivationResults != null) {
                ivHandlePassivated = true;
                svLogger.info("ejbPassivate: TimerHandle is set ");
            }
        }
    }

    /**
     * Returns the results of testing performed in {@link #ejbPassivate ejbPassivate()}. <p>
     *
     * Since ejbPassivate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbPassivate method.
     **/
    public void verifyEjbPassivateResults() {
        verifyResults(ivEjbPassivateResults);
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
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol> <p>
     */
    public void testTimerAccess() throws Exception {
        Timer timer = null;
        Timer retTimer = null;
        TimerHandle timerHandle = null;
        String timerInfo = "StatelessBean:StatefulBean." + ivMessage;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info(ivMessage + ": Calling getTimerService()");
            ivContext.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "1 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may be performed.
        // - must get a Timer from a different bean, since this is not TimedObject
        // -----------------------------------------------------------------------
        timer = createTimer();

        // -----------------------------------------------------------------------
        // 2 - Verify Timer.getTimeRemaining() works
        // -----------------------------------------------------------------------
        {
            svLogger.info(ivMessage + ": Calling Timer.getTimeRemaining()");
            long remaining = timer.getTimeRemaining();
            Assert.assertTrue("2 ---> Timer.getTimeRemaining() worked: " + remaining,
                              remaining >= 1 && remaining <= 60000);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify Timer.getInfo() works
        // -----------------------------------------------------------------------
        {
            svLogger.info(ivMessage + ": Calling Timer.getInfo()");
            Object curInfo = timer.getInfo();
            Assert.assertEquals("3 ---> Timer.getInfo() worked: " + curInfo +
                                "", timerInfo, curInfo);
        }

        // -----------------------------------------------------------------------
        // 4 - Verify Timer.getHandle() works
        // -----------------------------------------------------------------------
        {
            svLogger.info(ivMessage + ": Calling Timer.getHandle()");
            timerHandle = timer.getHandle();
            Assert.assertNotNull("4 ---> Timer.getHandle() worked: " + timerHandle,
                                 timerHandle);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify TimerHandle.getTimer() works
        // -----------------------------------------------------------------------
        svLogger.info(ivMessage + ": Calling TimerHandle.getTimer()");
        retTimer = timerHandle.getTimer();
        Assert.assertNotNull("5 ---> TimerHandle.getTimer() worked: " +
                             retTimer, retTimer);

        // -----------------------------------------------------------------------
        // 6 - Verify Timer.equals() works
        // -----------------------------------------------------------------------
        svLogger.info(ivMessage + ": Calling Timer.equals()");
        Assert.assertEquals("6 ---> Timer.equals() worked",
                            timer, retTimer);

        // -----------------------------------------------------------------------
        // 7 - Verify Timer.cancel() works
        // -----------------------------------------------------------------------
        svLogger.info(ivMessage + ": Calling Timer.cancel()");
        timer.cancel();
        svLogger.info("7 ---> Timer.cancel() worked");

        // -----------------------------------------------------------------------
        // 8 - Verify NoSuchObjectLocalException occurs accessing canceled timer
        // -----------------------------------------------------------------------
        try {
            svLogger.info(ivMessage + ": Calling Timer.getInfo() " +
                          "on cancelled Timer");
            timer.getInfo();
            Assert.fail("8 ---> Timer.getInfo() worked - " +
                        "expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            svLogger.log(Level.INFO, "8 ---> Caught expected exception", nso);
        }
    }

    private Object testTimerAccessIndirect() {
        try {
            testTimerAccess();
            return true;
        } catch (Throwable t) {
            svLogger.log(Level.SEVERE, "test failure", t);
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
     * ({@link #getAfterBeginResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol> <p>
     */
    @Override
    public void afterBegin() throws EJBException, RemoteException {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (ivMessage != null && ivMessage.equals("afterBegin")) {
            ivAfterBeginResults = testTimerAccessIndirect();

            ivMessage += ":done"; // perform test only 1 time
        }
    }

    /**
     * Returns the results of testing performed in {@link #afterBegin afterBegin()}. <p>
     *
     * Since afterBegin may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in afterBegin method.
     **/
    public void verifyAfterBeginResults() {
        verifyResults(ivAfterBeginResults);
    }

    /**
     * Test getTimerService()/TimerService access from beforeCompletion on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container accesses a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #getBeforeCompletionResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol> <p>
     */
    @Override
    public void beforeCompletion() throws EJBException, RemoteException {
        // If this methods name was passed on the create, then perform the
        // test... call an internal method used by like EJB callback methods.
        if (ivMessage != null && ivMessage.equals("beforeCompletion")) {
            ivBeforeCompletionResults = testTimerAccessIndirect();

            ivMessage += ":done"; // perform test only 1 time
        }

        // If afterCompletion is the message, then create a Timer in an
        // instance variable for use in afterCompletion, since afterCompletion
        // does not have EJB access, and therefore cannot create a Timer.
        else if (ivMessage != null && ivMessage.equals("afterCompletion")) {
            if (ivAfterCompletionResults == null) {
                try {
                    ivTimer = createTimer();
                    ivAfterCompletionResults = true;
                } catch (Throwable t) {
                    svLogger.log(Level.SEVERE, "test failure", t);
                    ivAfterCompletionResults = t;
                }
            }
        }

        // If TimerPassivation is the message, then create a Timer in an
        // instance variable to test that a Timer (and TimerHandle) may
        // be 'passivated' as part of a Stateful beans state.
        else if (ivMessage != null && ivMessage.equals("TimerPassivation")) {
            if (ivTimerPassivationResults == null) {
                try {
                    ivTimer = createTimer();
                    ivTimerHandle = ivTimer.getHandle();
                    ivTimerPassivationResults = true;
                } catch (Throwable t) {
                    svLogger.log(Level.SEVERE, "test failure", t);
                    ivTimerPassivationResults = t;
                }
            }
        }
    }

    /**
     * Returns the results of testing performed in {@link #beforeCompletion beforeCompletion()}. <p>
     *
     * Since beforeCompletion may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in beforeCompletion method.
     **/
    public void verifyBeforeCompletionResults() {
        verifyResults(ivBeforeCompletionResults);
    }

    /**
     * Test getTimerService()/TimerService access from afterCompletion on a
     * Stateful Session bean. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateful bean, the results will be stored
     * in an instance variable. The test may then extract the results
     * ({@link #getAfterCompletionResults}). <p>
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
    @Override
    public void afterCompletion(boolean flag) throws EJBException, RemoteException {
        // If this methods name was passed on the create, then perform the
        // test...
        if (ivMessage != null && ivMessage.equals("afterCompletion")) {
            if (ivAfterCompletionResults instanceof Boolean) {
                ivAfterCompletionResults = testNoTimerAccessIndirect();

                ivMessage += ":done"; // perform test only 1 time
                ivTimer = null;
            }
        }
    }

    /**
     * Returns the results of testing performed in {@link #afterCompletion afterCompletion()}. <p>
     *
     * Since afterCompletion may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in afterCompletion method.
     **/
    public void verifyAfterCompletionResults() {
        verifyResults(ivAfterCompletionResults);
    }

    /**
     * Internal method for creating a Timer that may be used for testing
     * Timer method access. <p>
     *
     * @param results Object to record results of Timer creation
     *
     * @return a Timer for a Stateless Session bean.
     **/
    private Timer createTimer() throws Exception {
        Timer timer = null;

        // --------------------------------------------------------------------
        // Locate Home, create a bean, and use the bean to create a Timer
        // --------------------------------------------------------------------
        StatelessTimedHome SLTLHome = (StatelessTimedHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/StatelessTimed");

        // Create a bean that may be used to create a timer.
        svLogger.info("Creating a bean to get a timer from ...");
        StatelessTimedObject sltl = SLTLHome.create();

        svLogger.info("Creating a timer for StatelessTimed Bean ...");
        String timerInfo = "StatelessBean:StatefulBean." + ivMessage;
        timer = sltl.createTimer(timerInfo);
        svLogger.info("Timer Created: " + timerInfo);

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
     *
     * @return Results of testing performed during passivation/activation.
     */
    public void verifyTimerPassivationResults() {
        verifyResults(ivTimerPassivationResults);

        // --------------------------------------------------------------------
        // 1 - Verify Timer is serialized successfully
        // --------------------------------------------------------------------
        if (ivTimerPassivated && ivTimerActivated && ivTimer != null) {
            svLogger.info("1 ---> Timer serialized successfully");
        } else {
            Assert.fail("1 ---> Timer not serialized : " +
                        "Timer = " + ivTimer + ", " +
                        "passivated = " + ivTimerPassivated + ", " +
                        "activated = " + ivTimerActivated);
        }

        // --------------------------------------------------------------------
        // 2 - Verify TimerHandle is serialized successfully
        // --------------------------------------------------------------------
        if (ivHandlePassivated && ivHandleActivated && ivTimerHandle != null) {
            svLogger.info("2 ---> TimerHandle serialized successfully");
        } else {
            Assert.fail("2 ---> TimerHandle not serialized : " +
                        "TimerHandle = " + ivTimerHandle + ", " +
                        "passivated = " + ivHandlePassivated + ", " +
                        "activated = " + ivHandleActivated);
        }

        ivMessage += ":done"; // perform test only 1 time
        ivTimer = null;
        ivTimerHandle = null;
    }

    public void setMessage(String message) {
        ivMessage = message;
    }

    public String getMessage() {
        return ivMessage;
    }

}
