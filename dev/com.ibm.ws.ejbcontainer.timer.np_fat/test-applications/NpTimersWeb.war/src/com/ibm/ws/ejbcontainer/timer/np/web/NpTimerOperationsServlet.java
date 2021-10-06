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
package com.ibm.ws.ejbcontainer.timer.np.web;

import static com.ibm.ws.ejbcontainer.timer.np.operations.ejb.StatelessTimedLocal.MAX_TIMER_WAIT;
import static javax.ejb.TransactionManagementType.BEAN;
import static javax.ejb.TransactionManagementType.CONTAINER;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.np.operations.ejb.SingletonTimedLocal;
import com.ibm.ws.ejbcontainer.timer.np.operations.ejb.StatefulBean;
import com.ibm.ws.ejbcontainer.timer.np.operations.ejb.StatefulLocal;
import com.ibm.ws.ejbcontainer.timer.np.operations.ejb.StatelessLocal;
import com.ibm.ws.ejbcontainer.timer.np.operations.ejb.StatelessTimedLocal;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;

/**
 * Test to exercise the EJB Container Non-Persistent Timer support using session
 * beans. Verifies the "Allowed Operations" tables in the EJB specification for
 * session bean types. <p>
 *
 * The following scenarios are covered for each session bean type:
 *
 * <ul>
 * <li>TimerService access in bean that does not implement a timeout method.
 * <li>TimerService access in setSessionContext.
 * <li>TimerService access in PostCreate.
 * <li>TimerService access in business method.
 * <li>TimerService access in ejbTimeout.
 * <li>SessionContext access in ejbTimeout - CMT.
 * <li>SessionContext access in ejbTimeout - BMT.
 * <li>createTimer() IllegalArgumentExceptions.
 * </ul>
 *
 * Also covered is the ability to access a Timer outside of a bean.
 */
@WebServlet("/NpTimerOperationsServlet")
@SuppressWarnings("serial")
public class NpTimerOperationsServlet extends AbstractServlet {
    private static final Logger svLogger = Logger.getLogger(NpTimerOperationsServlet.class.getName());

    @EJB
    StatelessLocal slbean;

    @EJB(beanName = "StatelessTimedBean")
    StatelessTimedLocal sltbean;

    @EJB(beanName = "StatelessTimedBMTBean")
    StatelessTimedLocal sltbmtbean;

    @EJB(beanName = "SingletonTimedBean")
    SingletonTimedLocal sgtbean;

    @EJB(beanName = "SingletonTimedBMTBean")
    SingletonTimedLocal sgtbmtbean;

    private <T> T lookupBean(Class<T> intf, TransactionManagementType txType) throws NamingException {

        if (intf == StatefulLocal.class && txType == CONTAINER) {
            return intf.cast(new InitialContext().lookup("java:app/NpTimerOperationsEJB/StatefulBean"));
        }
        if (intf == StatelessLocal.class && txType == CONTAINER) {
            return intf.cast(slbean);
        }
        if (intf == StatelessTimedLocal.class && txType == CONTAINER) {
            return intf.cast(sltbean);
        }
        if (intf == StatelessTimedLocal.class && txType == BEAN) {
            return intf.cast(sltbmtbean);
        }
        if (intf == SingletonTimedLocal.class && txType == CONTAINER) {
            return intf.cast(sgtbean);
        }
        if (intf == SingletonTimedLocal.class && txType == BEAN) {
            return intf.cast(sgtbmtbean);
        }
        throw new IllegalArgumentException("Unsupported bean type.");
    }

    /**
     * Test Timer method access from a servlet. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    @Test
    public void testTimerAccessFromServlet() throws Exception {

        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);
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
                          remaining >= 1 && remaining <= StatelessTimedLocal.DEFAULT_EXPIRATION);

        // -------------------------------------------------------------------
        // 5 - Verify Timer.getInfo() returning serializable works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.getInfo()");
        Object timerInfo = timer.getInfo();
        Assert.assertEquals("5 ---> Timer.getInfo() worked: " + timerInfo,
                            "Servlet", timerInfo);

        // -------------------------------------------------------------------
        // 6 - Verify Timer.getHandle() fails with IllegalStateException
        // -------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling Timer.getHandle()");
            TimerHandle timerHandle = timer.getHandle();
            fail("6 ---> Successfully called Timer.getHandle() for non-persistent timer : " + timerHandle);
        } catch (IllegalStateException ex) {
            svLogger.info("6 ---> Caught expected exception from getHandle : " + ex);
        }

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

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulSetSessionContext() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - setSessionContext may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.verifySetSessionContextResults();
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from PostConstruct on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulPostConstruct() throws Exception {

        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - PostConstruct may not be called directly....
        // --------------------------------------------------------------------
        StatefulBean.TestPostConstruct = true;
        try {
            StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
            sf.verifyPostConstructResults();
            sf.remove();
        } finally {
            StatefulBean.TestPostConstruct = false;
        }
    }

    /**
     * Test getTimerService()/TimerService access from PreDestroy on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulPreDestroy() throws Exception {

        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - PreDestroy may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("preDestroy");

        // Now remove the bean to execute preDestroy....
        sf.remove();

        // Create another bean to extract the results...
        sf = lookupBean(StatefulLocal.class, CONTAINER);

        // --------------------------------------------------------------------
        // Verify the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyPreDestroyResults();

        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbActivate on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulEjbActivate() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - ejbActivate may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("ejbActivate");
        sf.verifyEjbActivateResults();
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbPassivate on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulEjbPassivate() throws Exception {

        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - ejbPassivate may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("ejbPassivate");
        sf.verifyEjbPassivateResults();
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from a business method on a
     * Stateful Session bean. <p>
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
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulBeanMethod() throws Exception {
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("testTimerAccess");
        sf.testTimerAccess();
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from afterBegin on a
     * Stateful Session bean. <p>
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
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulAfterBegin() throws Exception {

        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - afterBegin may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("afterBegin");
        sf.verifyAfterBeginResults();
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from beforeCompletion on a
     * Stateful Session bean. <p>
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
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulBeforeCompletion() throws Exception {

        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - beforeCompletion may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("beforeCompletion");

        // --------------------------------------------------------------------
        // Call any method to force a transaction and beforeCompletion.
        // --------------------------------------------------------------------
        sf.getMessage();

        // --------------------------------------------------------------------
        // Verify the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyBeforeCompletionResults();

        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from afterCompletion on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulAfterCompletion() throws Exception {

        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - afterCompletion may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("afterCompletion");

        // --------------------------------------------------------------------
        // Call any method to force a transaction and afterCompletion.
        // --------------------------------------------------------------------
        sf.getMessage();

        // --------------------------------------------------------------------
        // Verify the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyAfterCompletionResults();

        sf.remove();
    }

    /**
     * Test that a Timer will be serialized by the EJB Container
     * as part of the state of a Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Timer is serialized successfully.
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatefulSerialization() throws Exception {

        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - passivation/activation may not be called directly....
        // --------------------------------------------------------------------
        StatefulLocal sf = lookupBean(StatefulLocal.class, CONTAINER);
        sf.setMessage("TimerPassivation");

        // --------------------------------------------------------------------
        // Call any method to force a transaction and beforeCompletion, which
        // will create a Timer to be passivated....
        // --------------------------------------------------------------------
        sf.getMessage();

        // --------------------------------------------------------------------
        // Verify the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyTimerPassivationResults();

        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that does not implement a timeout callback method. <p>
     *
     * This test will confirm the following :
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
    @Test
    public void testTimerAccessFromStatelessNotTimedObject() throws Exception {
        StatelessLocal sl = lookupBean(StatelessLocal.class, CONTAINER);
        sl.testTimerService();
    }

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateless Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatelessSetSessionContext() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - setSessionContext may not be called directly....
        // --------------------------------------------------------------------
        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);
        slt.verifySetSessionContextResults();
    }

    /**
     * Test getTimerService()/TimerService access from PostConstruct on a
     * Stateless Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatelessPostConstruct() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - PostConstruct may not be called directly....
        // --------------------------------------------------------------------
        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);
        slt.verifySetSessionContextResults();
        slt.verifyPostConstructResults();
    }

    /**
     * Test getTimerService()/TimerService access from PreDestroy on a
     * Stateless Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createSingleActionTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> TimerService.getAllTimers() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromStatelessPreDestroy() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating multiple instances of the bean...
        // - PreDestroy may not be called directly....
        // --------------------------------------------------------------------
        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);
        slt.recursiveCall(501); // overflows the default pool size
        slt.verifySetSessionContextResults();
        slt.verifyPreDestroyResults();
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
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
    @Mode(Mode.TestMode.FULL)
    public void testTimerAccessFromStatelessBeanMethod() throws Exception {
        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);

        // --------------------------------------------------------------------
        // Execute the test by calling methods on the bean...
        // --------------------------------------------------------------------
        slt.testTimerServicePhase1();
        slt.testTimerServicePhase2();
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Stateless
     * Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
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
    public void testTimerAccessFromStatelessTimeout() throws Exception {
        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean...
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testTimerService");

        svLogger.info("Wait up to " + MAX_TIMER_WAIT + "ms for the timer to fire...");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        // verify the results from phase 1, and then call phase 2 to see if timers ran
        slt.verifyEjbTimeoutResults();
        slt.testTimerServicePhase2();
    }

    /**
     * Test SessionContext method access from ejbTimeout on a CMT Stateless
     * Session bean that implements a timeout callback method. <p>
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
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRolledbackException")
    public void testSessionContextAccessFromStatelessTimeoutCMT() throws Exception {

        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean...
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testContextMethods-CMT");

        svLogger.info("Wait up to " + MAX_TIMER_WAIT + "ms for the timer to fire...");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        // verify the results from ejbTimeout
        slt.verifyEjbTimeoutResults();
    }

    /**
     * Test SessionContext method access from ejbTimeout on a BMT Stateless
     * Session bean that implements a timeout callback method. <p>
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
    public void testSessionContextAccessFromStatelessTimeoutBMT() throws Exception {

        StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, BEAN);

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean... append results
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testContextMethods-BMT");

        svLogger.info("Wait up to " + MAX_TIMER_WAIT + "ms for the timer to fire...");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        // verify the results from ejbTimeout
        slt.verifyEjbTimeoutResults();
    }

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Singleton Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testTimerAccessFromSingletonSetSessionContext() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - setSessionContext may not be called directly....
        // --------------------------------------------------------------------
        SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, CONTAINER);
        slt.verifySetSessionContextResults();
    }

    /**
     * Test getTimerService()/TimerService access from PostConstruct on a
     * Singleton Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
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
     * </ol>
     */
    @Test
    public void testTimerAccessFromSingletonPostConstruct() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean...
        // - PostConstruct may not be called directly....
        // --------------------------------------------------------------------
        SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, CONTAINER);
        slt.verifySetSessionContextResults();
        slt.verifyPostConstructResults();
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Singleton
     * Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
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
    @Mode(Mode.TestMode.FULL)
    public void testTimerAccessFromSingletonBeanMethod() throws Exception {
        SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, CONTAINER);

        // --------------------------------------------------------------------
        // Execute the test by calling methods on the bean...
        // --------------------------------------------------------------------
        slt.testTimerServicePhase1();
        slt.testTimerServicePhase2();
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Singleton
     * Session bean that implements a timeout callback method. <p>
     *
     * This test will confirm the following :
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
    @Mode(Mode.TestMode.FULL)
    public void testTimerAccessFromSingletonTimeout() throws Exception {
        SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, CONTAINER);

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean...
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testTimerService");

        svLogger.info("Wait up to " + SingletonTimedLocal.MAX_TIMER_WAIT + "ms for the timer to fire...");
        timerLatch.await(SingletonTimedLocal.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        // verify the results from phase 1, and then call phase 2 to see if timers ran
        slt.verifyEjbTimeoutResults();
        slt.testTimerServicePhase2();
    }

    /**
     * Test SessionContext method access from ejbTimeout on a CMT Singleton
     * Session bean that implements a timeout callback method. <p>
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
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRolledbackException")
    public void testSessionContextAccessFromSingletonTimeoutCMT() throws Exception {

        SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, CONTAINER);

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean...
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testContextMethods-CMT");

        svLogger.info("Wait up to " + SingletonTimedLocal.MAX_TIMER_WAIT + "ms for the timer to fire...");
        timerLatch.await(SingletonTimedLocal.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        // verify the results from ejbTimeout
        slt.verifyEjbTimeoutResults();
    }

    /**
     * Test SessionContext method access from ejbTimeout on a BMT Singleton
     * Session bean that implements a timeout callback method. <p>
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
    public void testSessionContextAccessFromSingletonTimeoutBMT() throws Exception {

        SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, BEAN);

        // --------------------------------------------------------------------
        // Execute the test by creating a timer for the bean... append results
        // - ejbTimeout may not be called directly, so wait for it to expire
        // --------------------------------------------------------------------

        svLogger.info("Creating a Timer to test access in ejbTimeout ...");
        CountDownLatch timerLatch = slt.createTimer(0, "testContextMethods-BMT");

        svLogger.info("Wait up to " + SingletonTimedLocal.MAX_TIMER_WAIT + "ms for the timer to fire...");
        timerLatch.await(SingletonTimedLocal.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        // verify the results from ejbTimeout
        slt.verifyEjbTimeoutResults();
    }

    @Override
    protected void clearAllTimers() {
        try {
            StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, CONTAINER);
            slt.clearAllTimers();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
        try {
            StatelessTimedLocal slt = lookupBean(StatelessTimedLocal.class, BEAN);
            slt.clearAllTimers();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
        try {
            SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, CONTAINER);
            slt.clearAllTimers();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
        try {
            SingletonTimedLocal slt = lookupBean(SingletonTimedLocal.class, BEAN);
            slt.clearAllTimers();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
    }
}
