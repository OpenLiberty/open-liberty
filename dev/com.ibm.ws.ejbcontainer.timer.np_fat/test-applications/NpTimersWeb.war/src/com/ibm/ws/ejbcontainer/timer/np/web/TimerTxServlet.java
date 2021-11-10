/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.web;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.ejb.NpTimedObjectTimerBean;
import com.ibm.ws.ejbcontainer.timer.np.ejb.NpTimedObjectTimerLocal;
import com.ibm.ws.ejbcontainer.timer.np.ejb.TxSyncSFBean;

import componenttest.annotation.ExpectedFFDC;

/**
 * <dl>
 * <dt><b>Test Name:</b> TimerTxTest .
 *
 * <dt><b>Test Author:</b> Andy McCright - based on code by Sean Zhou <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Verify the transaction quality of Timer Service and the behavior
 * of non-persistent Timer objects. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testCreateAndCancelATimerInACommittedTran
 * <li>testCreateATimerInARolledBackTran
 * <li>testCancelATimerInARolledBackTran
 * <li>testEjbTimeoutNeverCommits()
 * <li>testCommittedEjbTimeoutTran
 * <li>testTimoutOccursAtCreateNotAtTxCommit()
 * <li>testRolledBackEjbTimeoutTran()
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@WebServlet("/TimerTxServlet")
@SuppressWarnings("serial")
public class TimerTxServlet extends AbstractServlet {

    private final static String TEST_CASE_NAME = TimerTxServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(TEST_CASE_NAME);

    private final static String NL = System.getProperty("line.separator");

    private static final long WAIT_FOR_TIMER_RUN = 60 * 1000;

    /** Jndi Names of the Bean Homes to use for the test. **/
    private final static String svJndi_SLBMT = "java:app/NpTimersEJB/NpTimerBeanBMT";
    private final static String svJndi_SLRequiresNew = "java:app/NpTimersEJB/NpTimerBeanRequiresNew";
    private final static String svJndi_SLRequired = "java:app/NpTimersEJB/NpTimerBeanRequired";
    private final static String svJndi_SLNotSupported = "java:app/NpTimersEJB/NpTimerBeanNotSupported";

    private NpTimedObjectTimerLocal ivSLBMTBean;
    private NpTimedObjectTimerLocal ivSLRequiresNewBean;
    private NpTimedObjectTimerLocal ivSLRequiredBean;
    private NpTimedObjectTimerLocal ivSLNotSupportedBean;

    @Override
    protected void clearAllTimers() {
        svLogger.info("> clearAllTimers");

        if (ivSLBMTBean != null) {
            ivSLBMTBean.cancelAllTimers();
            ivSLBMTBean = null;
        }
        if (ivSLRequiresNewBean != null) {
            ivSLRequiresNewBean.cancelAllTimers();
            ivSLRequiresNewBean = null;
        }
        if (ivSLRequiredBean != null) {
            ivSLRequiredBean.cancelAllTimers();
            ivSLRequiredBean = null;
        }
        if (ivSLNotSupportedBean != null) {
            ivSLNotSupportedBean.cancelAllTimers();
            ivSLNotSupportedBean = null;
        }

        svLogger.info("< clearAllTimers");
    }

    /**
     * Looks up the requested bean and caches it for test cleanup.
     */
    private NpTimedObjectTimerLocal lookupBean(String beanJndi) throws Exception {
        Context context = new InitialContext();
        NpTimedObjectTimerLocal bean = (NpTimedObjectTimerLocal) context.lookup(beanJndi);

        if (beanJndi == svJndi_SLBMT) {
            ivSLBMTBean = bean;
        } else if (beanJndi == svJndi_SLRequiresNew) {
            ivSLRequiresNewBean = bean;
        } else if (beanJndi == svJndi_SLRequired) {
            ivSLRequiredBean = bean;
        } else if (beanJndi == svJndi_SLNotSupported) {
            ivSLNotSupportedBean = bean;
        } else {
            throw new EJBException("Bean not supported : " + beanJndi);
        }
        return bean;
    }

    /**
     * --------------------------------------------------------------------
     * Test the scenario of creating and canceling a Timer in a committed
     * transaction.
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * NpTimerBeanRequired
     * NpTimerBeanNotSupported
     */
    @Test
    public void testCreateAndCancelATimerInACommittedTran() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------
        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);

        svLogger.info("Calling SLTimedForTxObject.prepCreateAndCancel() ..." + NL);
        try {
            timerBean.prepCreateAndCancel(NpTimedObjectTimerBean.TX_BMT);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked 'DEFAULT_EXPIRATION * 2' ..." + NL);
        FATHelper.sleep(NpTimedObjectTimerBean.CREATE_AND_CANCEL_BMT_EXPIRATION + NpTimedObjectTimerBean.TIMER_PRECISION);
        svLogger.info("Calling SLTimedForTxObject.testCreateAndCancel() ..." + NL);

        try {
            timerBean.testCreateAndCancel();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean to test it.
        // The target method's Tx attribute is 'requiresNew'
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean (requiresNew) to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequiresNew);

        svLogger.info("Calling SLTimedForTxHome.prepCreateAndCancel() ... " + NL);
        try {
            timerBean.prepCreateAndCancel(NpTimedObjectTimerBean.TX_REQUIRESNEW);
        } catch (EJBException ex) {
            if (ex.getCause() instanceof Error) {
                throw (Error) ex.getCause();
            } else {
                throw ex;
            }
        }

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked 'DEFAULT_EXPIRATION * 2' ..." + NL);
        FATHelper.sleep(NpTimedObjectTimerBean.DEFAULT_EXPIRATION + NpTimedObjectTimerBean.TIMER_PRECISION);
        svLogger.info("Calling SLTimedForTxHome.testCreateAndCancel() ..." + NL);
        try {
            timerBean.testCreateAndCancel();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean to test it.
        // The target method's Tx attribute is 'required'
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean (required) to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequired);

        svLogger.info("Calling SLTimedForTxHome.prepCreateAndCancel() ... " + NL);
        try {
            timerBean.prepCreateAndCancel(NpTimedObjectTimerBean.TX_REQUIRED);
        } catch (EJBException ex) {
            if (ex.getCause() instanceof Error) {
                throw (Error) ex.getCause();
            } else {
                throw ex;
            }
        }

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked 'DEFAULT_EXPIRATION * 2' ..." + NL);
        FATHelper.sleep(NpTimedObjectTimerBean.DEFAULT_EXPIRATION + NpTimedObjectTimerBean.TIMER_PRECISION);
        svLogger.info("Calling SLTimedForTxHome.testCreateAndCancel() ..." + NL);
        try {
            timerBean.testCreateAndCancel();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean to test it.
        // The target method's Tx attribute is 'NotSupported'
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean (NotSupported) to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLNotSupported);

        svLogger.info("Calling SLTimedForTxHome.prepCreateAndCancel() ... " + NL);
        try {
            timerBean.prepCreateAndCancel(NpTimedObjectTimerBean.TX_NOTSUPPORTED);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked 'DEFAULT_EXPIRATION * 2' ..." + NL);
        FATHelper.sleep(NpTimedObjectTimerBean.DEFAULT_EXPIRATION + NpTimedObjectTimerBean.TIMER_PRECISION);
        svLogger.info("Calling SLTimedForTxHome.testCreateAndCancel() ..." + NL);
        try {
            timerBean.testCreateAndCancel();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        svLogger.info("<<<--------------------------------------------------");
        svLogger.info("<<< testCreateAndCancelATimerInACommittedTran() -----");
        svLogger.info("<<<--------------------------------------------------" + NL);
    }

    /**
     * Test the scenario of creating a Timer in a rolled back transaction
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * NpTimerBeanRequired
     */
    @Test
    public void testCreateATimerInARolledBackTran() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);

        svLogger.info("Calling SLTimedForTxHome.prepRollbackCreate() ..." + NL);
        try {
            timerBean.prepRollbackCreate(NpTimedObjectTimerBean.TX_BMT);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked 'DEFAULT_EXPIRATION * 2' ..." + NL);
        FATHelper.sleep(NpTimedObjectTimerBean.DEFAULT_EXPIRATION + NpTimedObjectTimerBean.TIMER_PRECISION);
        svLogger.info("Calling SLTimedForTxHome.testRollbackCreate() ..." + NL);
        try {
            timerBean.testRollbackCreate();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean to test it.
        // The target method's Tx attribute is 'requiresNew'
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean (requiresNew) to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequiresNew);

        svLogger.info("Calling prepRollbackCreate() ..." + NL);
        try {
            timerBean.prepRollbackCreate(NpTimedObjectTimerBean.TX_REQUIRESNEW);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked 'DEFAULT_EXPIRATION * 2' ..." + NL);
        FATHelper.sleep(NpTimedObjectTimerBean.DEFAULT_EXPIRATION + NpTimedObjectTimerBean.TIMER_PRECISION);
        svLogger.info("Calling testRollbackCreate() ..." + NL);
        try {
            timerBean.testRollbackCreate();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean to test it.
        // The target method's Tx attribute is 'required'
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean (required) to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequired);

        svLogger.info("Calling prepRollbackCreate() ..." + NL);
        try {
            timerBean.prepRollbackCreate(NpTimedObjectTimerBean.TX_REQUIRED);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked 'DEFAULT_EXPIRATION * 2' ..." + NL);
        FATHelper.sleep(NpTimedObjectTimerBean.DEFAULT_EXPIRATION + NpTimedObjectTimerBean.TIMER_PRECISION);
        svLogger.info("Calling testRollbackCreate() ..." + NL);
        try {
            timerBean.testRollbackCreate();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        svLogger.info("<<<--------------------------------------------------");
        svLogger.info("<<< testCreateATimerInARolledBackTran() -------------");
        svLogger.info("<<<--------------------------------------------------" + NL);
    }

    /**
     * Test the scenario of canceling a Timer in a rolled back transaction
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * NpTimerBeanRequired
     */
    @Test
    public void testCancelATimerInARolledBackTran() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);

        svLogger.info("Calling prep1RollbackCancel() ..." + NL);
        try {
            timerBean.prep1RollbackCancel(NpTimedObjectTimerBean.TX_BMT);
            svLogger.info("Calling prep2RollbackCancel() ..." + NL);
            timerBean.prep2RollbackCancel(NpTimedObjectTimerBean.TX_BMT);
            timerBean.testRollbackCancel();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean to test it.
        // The target method's Tx attribute is 'requiresNew'
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean (requiresNew) to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequiresNew);

        try {
            svLogger.info("Calling prep1RollbackCancel() ..." + NL);
            timerBean.prep1RollbackCancel(NpTimedObjectTimerBean.TX_REQUIRESNEW);
            svLogger.info("Calling prep2RollbackCancel() ..." + NL);
            timerBean.prep2RollbackCancel(NpTimedObjectTimerBean.TX_REQUIRESNEW);

            svLogger.info("Calling testRollbackCancel() ..." + NL);
            timerBean.testRollbackCancel();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean to test it.
        // The target method's Tx attribute is 'required'
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean (required) to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequired);

        try {
            svLogger.info("Calling prep1RollbackCancel() ..." + NL);
            timerBean.prep1RollbackCancel(NpTimedObjectTimerBean.TX_REQUIRED);
            svLogger.info("Calling prep2RollbackCancel() ..." + NL);
            timerBean.prep2RollbackCancel(NpTimedObjectTimerBean.TX_REQUIRED);
            svLogger.info("Calling testRollbackCancel() ..." + NL);
            timerBean.testRollbackCancel();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        svLogger.info("<<<--------------------------------------------------");
        svLogger.info("<<< testCancelATimerInARolledBackTran() -------------");
        svLogger.info("<<<--------------------------------------------------" + NL);
    }

    /**
     * Test for a not committed transaction inside the ejbTimeout method of a BMT
     * Timed Stateless session bean. The transaction is supposed to rollback.
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * TxSyncSFBean - used to confirm rollback with @AfterCompletion
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRolledbackException")
    public void testEjbTimeoutNeverCommits() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);
        timerBean.prepNoCommitTest();

        // Sleep for enough time to ensure the timer has expired and retried.
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked and retried");
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);

        svLogger.info("Checking SF Bean state was rolled back ...");
        assertEquals("Unexpected SF Bean State", 4, TxSyncSFBean.svInfo.size());
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(0));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_COMPLETION_ROLLBACK, TxSyncSFBean.svInfo.get(1));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(2));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_COMPLETION_ROLLBACK, TxSyncSFBean.svInfo.get(3));
    }

    /**
     * Test for a committed transaction inside the ejbTimeout method of a BMT & CMT
     * Timed Stateless session bean. A Stateful bean is updated in the ejbTimeout method.
     * The test will verify the Stateful bean AfterCompletion is called properly.
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * NpTimerBeanNotSupported
     * TxSyncSFBean - used to confirm commit with @AfterCompletion
     */
    @Test
    public void testCommittedEjbTimeoutTran() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);

        svLogger.info("Calling prepEjbTimeoutCommitTest() ... ");
        timerBean.prepEjbTimeoutCommitTest(NpTimedObjectTimerBean.TX_BMT);

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked ..." + NL);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        svLogger.info("Calling getTestResults() ...");

        svLogger.info("Checking SF Bean state was committed ...");
        assertEquals("Unexpected SF Bean State", 3, TxSyncSFBean.svInfo.size());
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(0));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.BEFORE_COMPLETION, TxSyncSFBean.svInfo.get(1));
        assertEquals("Unexpected SF Bean State", NpTimedObjectTimerBean.TX_BMT, TxSyncSFBean.svInfo.get(2));

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean of RequiresNew Tx attribute to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequiresNew);

        svLogger.info("Calling prepEjbTimeoutCommitTest() ... ");
        timerBean.prepEjbTimeoutCommitTest(NpTimedObjectTimerBean.TX_REQUIRESNEW);

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked ..." + NL);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);

        svLogger.info("Checking SF Bean state was committed ...");
        assertEquals("Unexpected SF Bean State", 3, TxSyncSFBean.svInfo.size());
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(0));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.BEFORE_COMPLETION, TxSyncSFBean.svInfo.get(1));
        assertEquals("Unexpected SF Bean State", NpTimedObjectTimerBean.TX_REQUIRESNEW, TxSyncSFBean.svInfo.get(2));

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean of Not Supported Tx attribute to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLNotSupported);

        svLogger.info("Calling prepEjbTimeoutCommitTest() ... ");
        timerBean.prepEjbTimeoutCommitTest(NpTimedObjectTimerBean.TX_NOTSUPPORTED);

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked ..." + NL);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);

        svLogger.info("Checking SF Bean state was not in a global transaction ...");
        assertEquals("Unexpected SF Bean State", 0, TxSyncSFBean.svInfo.size());
    }

    /**
     * If a timer is created in a transaction. The timer will not be expired if the transaction has
     * not been committed. The timer's expiration should be counted from the time the timer is
     * created instead of the timer the transaction is committed.
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * NpTimerBeanRequired
     * NpTimerBeanNotSupported
     */
    @Test
    public void testTimeoutOccursAtCreateNotAtTxCommit() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        try {
            // --------------------------------------------------------------------
            // Create a BMT Stateless Bean to test it
            // --------------------------------------------------------------------

            svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
            timerBean = lookupBean(svJndi_SLBMT);

            // For BMT, prep and test are merged into one method. Test results are
            // returned directly.

            svLogger.info("Calling testTxExpirationBMT() ...");
            timerBean.testTxExpirationBMT();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        try {
            // --------------------------------------------------------------------
            // Create a CMT RequiresNew Stateless Bean to test it
            // --------------------------------------------------------------------

            svLogger.info("Creating a CMT RequiresNew Stateless Bean to execute test ..." + NL);
            timerBean = lookupBean(svJndi_SLRequiresNew);

            svLogger.info("Calling prepTxExpirationCMT() ...");
            timerBean.prepTxExpirationCMT(NpTimedObjectTimerBean.TX_REQUIRESNEW);

            // not need to Thread.sleep at the test client. Sleep time is added in the bean methods.
            svLogger.info("Calling testTxExpirationCMT() ...");
            timerBean.testTxExpirationCMT();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        try {
            // --------------------------------------------------------------------
            // Create a CMT Required Stateless Bean to test it
            // --------------------------------------------------------------------

            svLogger.info("Creating a CMT Required Stateless Bean to execute test ..." + NL);
            timerBean = lookupBean(svJndi_SLRequired);

            svLogger.info("Calling prepTxExpirationCMT() ...");
            timerBean.prepTxExpirationCMT(NpTimedObjectTimerBean.TX_REQUIRED);

            // not need to Thread.sleep at the test client. Sleep time is added in the bean methods.
            svLogger.info("Calling testTxExpirationCMT() ...");
            timerBean.testTxExpirationCMT();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        try {
            // --------------------------------------------------------------------
            // Create a CMT NotSupported Stateless Bean to test it
            // --------------------------------------------------------------------

            svLogger.info("Creating a CMT NotSupported Stateless Bean to execute test ..." + NL);
            timerBean = lookupBean(svJndi_SLNotSupported);

            svLogger.info("Calling prepTxExpirationCMT() ...");
            timerBean.prepTxExpirationCMT(NpTimedObjectTimerBean.TX_NOTSUPPORTED);

            // not need to wait at the test client. Sleep time is added in the bean methods.
            svLogger.info("Calling testTxExpirationCMT() ...");
            timerBean.testTxExpirationCMT();
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    /**
     * Test for a rollback transaction inside the ejbTimeout method of a BMT & CMT
     * Timed Stateless session bean followed by successful completion on retry.
     * A Stateful bean is updated in the ejbTimeout method. The test will verify
     * the Stateful bean AfterCompletion is called properly.
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * TxSyncSFBean - used to confirm commit with @AfterCompletion
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRolledbackException")
    public void testRolledBackEjbTimeoutTran() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);

        svLogger.info("Calling prepEjbTimeoutRollbackTest() ... ");
        timerBean.prepEjbTimeoutRollbackTest(NpTimedObjectTimerBean.TX_BMT);

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked ..." + NL);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);

        svLogger.info("Checking SF Bean state was rolledback then committed ...");
        assertEquals("Unexpected SF Bean State", 5, TxSyncSFBean.svInfo.size());
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(0));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_COMPLETION_ROLLBACK, TxSyncSFBean.svInfo.get(1));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(2));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.BEFORE_COMPLETION, TxSyncSFBean.svInfo.get(3));
        assertEquals("Unexpected SF Bean State", NpTimedObjectTimerBean.TX_BMT, TxSyncSFBean.svInfo.get(4));

        // --------------------------------------------------------------------
        // Create a CMT Stateless Bean of RequiresNew Tx attribute to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequiresNew);

        svLogger.info("Calling prepEjbTimeoutRollbackTest() ... ");
        timerBean.prepEjbTimeoutRollbackTest(NpTimedObjectTimerBean.TX_REQUIRESNEW);

        // sleep enough time to check if ejbTimeout is invoked
        svLogger.info("Sleep enough time to check if ejbTimeout is invoked ..." + NL);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);

        svLogger.info("Checking SF Bean state was rolledback then committed ...");
        assertEquals("Unexpected SF Bean State", 5, TxSyncSFBean.svInfo.size());
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(0));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_COMPLETION_ROLLBACK, TxSyncSFBean.svInfo.get(1));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.AFTER_BEGIN, TxSyncSFBean.svInfo.get(2));
        assertEquals("Unexpected SF Bean State", TxSyncSFBean.BEFORE_COMPLETION, TxSyncSFBean.svInfo.get(3));
        assertEquals("Unexpected SF Bean State", NpTimedObjectTimerBean.TX_REQUIRESNEW, TxSyncSFBean.svInfo.get(4));
    }

    /**
     * Test the scenario of canceling a timer from another thread while the
     * timeout method is running and verify the timeout method may still
     * access the timer APIs (like Timer.getInfo).
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * NpTimerBeanRequired
     * NpTimerBeanNotSupported
     */
    @Test
    public void testTimerAccessFromTimeoutAfterCancel() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancel);
        timerBean.cancelAccessAfterCancelTimer().countDown();
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancel, NpTimedObjectTimerBean.svAccessAfterCreateResult);

        // --------------------------------------------------------------------
        // Create a CMT RequiresNew Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT RequiresNew Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequiresNew);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancel);
        timerBean.cancelAccessAfterCancelTimer().countDown();
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancel, NpTimedObjectTimerBean.svAccessAfterCreateResult);

        // --------------------------------------------------------------------
        // Create a CMT Required Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Required Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequired);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancel);
        timerBean.cancelAccessAfterCancelTimer().countDown();
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancel, NpTimedObjectTimerBean.svAccessAfterCreateResult);

        // --------------------------------------------------------------------
        // Create a CMT NotSupported Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT NotSupported Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLNotSupported);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancel);
        timerBean.cancelAccessAfterCancelTimer().countDown();
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancel, NpTimedObjectTimerBean.svAccessAfterCreateResult);
    }

    /**
     * Test the scenario of canceling a timer from the timeout method and verify
     * the timeout method may no longer access the timer APIs (like Timer.getInfo).
     *
     * <b>Beans for test</b>:
     * NpTimerBeanBMT
     * NpTimerBeanRequiresNew
     * NpTimerBeanRequired
     * NpTimerBeanNotSupported
     */
    @Test
    public void testTimerAccessFromTimeoutAfterCancelFromTimeout() throws Exception {
        NpTimedObjectTimerLocal timerBean;

        // --------------------------------------------------------------------
        // Create a BMT Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a BMT Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLBMT);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout, NpTimedObjectTimerBean.svAccessAfterCreateResult);

        // --------------------------------------------------------------------
        // Create a CMT RequiresNew Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT RequiresNew Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequiresNew);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout, NpTimedObjectTimerBean.svAccessAfterCreateResult);

        // --------------------------------------------------------------------
        // Create a CMT Required Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT Required Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLRequired);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout, NpTimedObjectTimerBean.svAccessAfterCreateResult);

        // --------------------------------------------------------------------
        // Create a CMT NotSupported Stateless Bean to test it
        // --------------------------------------------------------------------

        svLogger.info("Creating a CMT NotSupported Stateless Bean to execute test ..." + NL);
        timerBean = lookupBean(svJndi_SLNotSupported);

        svLogger.info("Calling prepAccessAfterCancelTest() ... ");
        timerBean.prepAccessAfterCancelTest(NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout);
        timerBean.waitForTimer(WAIT_FOR_TIMER_RUN);
        assertEquals("Unexpected timer result", NpTimedObjectTimerBean.infoAccessAfterCancelFromTimeout, NpTimedObjectTimerBean.svAccessAfterCreateResult);
    }
}
