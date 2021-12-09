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
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

@Stateless
public class NpTimedObjectTimerBean implements NpTimedObjectTimerLocal, TimedObject {
    private final static String CLASSNAME = NpTimedObjectTimerBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    // Constants: Bean transaction attribute types
    // The bean can be deployed as the following 4 different transaction attribute types
    public static final String TX_BMT = "BMT";
    public static final String TX_REQUIRED = "REQUIRED";
    public static final String TX_REQUIRESNEW = "REQUIRESNEW";
    public static final String TX_NOTSUPPORTED = "NOTSUPPORTED";
    public static final String keyTimerCommitBMT = "keyTimerCommitBMT";
    public static final String keyTimerCommitRequiresNew = "keyTimerCommitRequiresNew";
    public static final String keyTimerRollbackBMT = "keyTimerRollbackBMT";
    public static final String keyTimerRollbackRequiresNew = "keyTimerRollbackRequiresNew";
    public static final String keyTimerCommitNotSupported = "keyTimerCommitNotSupported";

    private static final long LATCH_AWAIT_TIME = 2 * 60 * 1000;
    public static final int COMMIT_PROCESSING_FUDGE = 250; // ms
    public static final int TIMER_PRECISION = 400;
    public static final int RETRY_INTERVAL = TIMER_PRECISION;
    public static final int DEFAULT_EXPIRATION = 800;
    public static final int SHORT_EXPIRATION = 200;
    public static final long CREATE_AND_CANCEL_BMT_EXPIRATION = 1500;
    public final static int MAX_RETRIES = 2;
    public static final String keyTimerNoCommit = "keyTimerNoCommit";

    // The time for the container to run post invoke processing for @Timeout.
    // Should be used after a Timer has triggered a CountDownLatch to insure
    // the @Timeout method, including the transaction, has completed and thus
    // updated (or even removed) the timer.
    private static final long POST_INVOKE_DELAY = 700;

    private static final String svJndi_SF_BEAN = "java:global/NpTimersApp/NpTimersEJB/TxSyncSFBean";
    private static final Set<TxSyncSFBean> svSFBeans = new HashSet<TxSyncSFBean>();

    @Resource
    private SessionContext ivContext;

    // These fields hold the test results for EJB callback methods

    private static final String infoCreateCancel = "CreateCancel";
    private static volatile boolean svTimerCreateCancelInvoked;
    private static final String infoRollbackCancel = "RollbackCancel";
    private static volatile boolean svTimerRollbackCancelInvoked;
    private static final String infoRollbackCreate = "RollbackCreate";
    private static volatile boolean svTimerRollbackCreateInvoked;
    private static final String infoEjbTimeoutCommit = "ejbTimeoutCommit";
    private static final String infoEjbTimeoutRollback = "ejbTimeoutRollback";
    private static final String infoTxExpiration = "TxExpiration";
    private static volatile boolean svTimerTxExpirationInvoked;
    private static volatile boolean svRollbackCalledOnceBMT;
    private static volatile boolean svRollbackCalledOnceRequiresNew;

    private static final String infoNoCommit = "NoCommit";

    public static final String infoAccessAfterCancel = "AccessAfterCancel";
    public static final String infoAccessAfterCancelFromTimeout = "AccessAfterCancelFromTimeout";

    private static CountDownLatch infoRollbackCancelLatch;
    private static CountDownLatch infoTxExpirationLatch;
    private static CountDownLatch svTimerLatch;
    private static CountDownLatch svTimerStartedLatch;
    private static CountDownLatch svTimerCancelLatch;
    private static CountDownLatch svTimerRunLatch;

    private static Timer tmpTimer = null;
    // for passing a cancelled Timer object across methods

    public static Timer svAccessAfterCreateTimer = null;
    public static Object svAccessAfterCreateResult = null;

    /**
     * Obtain a new instance of TxSyncSFBean and cache it so it
     * may be cleaned up after the test.
     */
    private TxSyncSFBean lookupSFBean() {
        try {
            TxSyncSFBean sfBean = (TxSyncSFBean) (new InitialContext()).lookup(svJndi_SF_BEAN);
            svSFBeans.add(sfBean);
            return sfBean;
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Failed looking up " + svJndi_SF_BEAN, ex);
        }
    }

    /**
     * Create and cancel a timer. Both transaction of creation and cancellation are
     * committed. The timer should not be available after its cancellation.
     * This method is tested with Tx attributes BMT, required, and Not supported. <p>
     *
     * This is the preparation part.
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx01} <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    @Override
    public void prepCreateAndCancel(String txType) {
        // Initialize static attributes

        svTimerCreateCancelInvoked = false;

        TimerService ts = ivContext.getTimerService();

        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "prepCreateAndCancel(" + txType + ")");
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify the cancelled timer cannot be found in the timer service.");

        if (txType.equals("BMT")) {
            UserTransaction tx1 = ivContext.getUserTransaction();
            try {
                tx1.begin();
                timer = TimerHelper.createTimer(ts, CREATE_AND_CANCEL_BMT_EXPIRATION, null, infoCreateCancel, false, null);
                tx1.commit();

                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer created successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepCreatAndCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer in prepCreateAndCancel()" + e);
            }

            UserTransaction tx2 = ivContext.getUserTransaction();
            try {
                tx2.begin();
                timer.cancel();
                tx2.commit();

                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer cancelled successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when cancelling timer in prepCreatAndCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when cancelling timer in prepCreatAndCancel()" + e);
            }
        } else { // CMT
            try {
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoCreateCancel, false, null);
                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer created successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepCreatAndCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer in prepCreateAndCancel()" + e);
            }

            try {
                timer.cancel();
                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer created successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when cancelling timer in prepCreatAndCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when cancelling timer in prepCreatAndCancel()" + e);
            }
        }

        tmpTimer = timer;
    }

    /**
     * Create and cancel a timer. Both transaction of creation and cancellation are committed. The
     * timer should not be available after its cancellation. <p>
     * This method is tested with Tx attributes BMT, required, and Not supported. <p>
     *
     * This is the result collection part.
     */
    @Override
    public void testCreateAndCancel() {
        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "testCreateAndCancel()");
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify the cancelled timer cannot be found in the timer service.");

        try {
            timer = TimerHelper.getTimerWithMatchingInfo(ts, infoCreateCancel);

            if (timer == null) {
                svLogger.logp(Level.INFO, CLASSNAME, "", "The timer should not be found.");
            } else {
                fail("The cancelled timer is still active and is found.");
            }
        } catch (Throwable th) {
            svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when searching for timer in testCreateAndCancel(). ");
            th.printStackTrace(System.out);
            fail("Unexpected exception from testNoCancel()" + th);
        }

        timer = tmpTimer;
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify that the correct exception is thrown when the cancelled timer is accessed.");

        try {
            timer.getInfo();
            fail("Timer.getInfo() should not work - " + "expected NoSuchObjectLocalException " + NL);
        } catch (NoSuchObjectLocalException nso) {
            svLogger.logp(Level.INFO, CLASSNAME, "", "Caught expected exception : " + nso + " " + NL);
        } catch (Throwable th) {
            svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when getting timer's info in testCreateAndCancel(). ");
            th.printStackTrace(System.out);
            fail("Unexpected exception from getInfo()" + th);
        }

        // check if the ejbTimeout() is invoked.
        assertTrue("The timer should not be expired.", !svTimerCreateCancelInvoked);
    }

    /**
     * Create a timer in a rolled back transaction. The timer should not be created <p>
     * This method is tested with Tx attributes BMT, Required
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    @Override
    public void prepRollbackCreate(String txType) {
        // Initialize static attributes
        svTimerRollbackCreateInvoked = false;

        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "prepRollbackCreate(" + txType + ")");
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify the cancelled timer cannot be found in the timer service.");

        if (txType.equals("BMT")) {
            UserTransaction tx1 = ivContext.getUserTransaction();
            try {
                tx1.begin();
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoRollbackCreate, false, null);
                tx1.rollback();
                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer creation transaction is rolled back successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepRollbackCreate(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for Tx commit test" + e);
            }
        } else { // CMT
            try {
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoRollbackCreate, false, null);
                ivContext.setRollbackOnly();
                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer creation transaction is rolled back successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepRollbackCreate(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for Tx commit test" + e);
            }
        }

        tmpTimer = timer; // pass the timer object through the static var
    }

    /**
     * Create a timer in a rolled back transaction. The timer should not be created <p>
     * This method is tested with Tx attributes BMT, Required
     */
    @Override
    public void testRollbackCreate() {
        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "testRollbackCreate()");
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify the cancelled timer cannot be found in the timer service.");

        try {
            timer = TimerHelper.getTimerWithMatchingInfo(ts, infoRollbackCreate);

            if (timer == null) {
                svLogger.logp(Level.INFO, CLASSNAME, "", "Transaction is rolled back. The timer should not be found.");
            } else {
                fail("Transaction is rolled back. The timer should not be found.");
            }
        } catch (Throwable th) {
            svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when searching for the timer in TimerService. ");
            th.printStackTrace(System.out);
            fail("Unexpected exception from testNoCreate()" + th);
        }

        timer = tmpTimer;
        try {
            timer.getInfo();
            fail("Timer.getInfo() should not work - " + "expected NoSuchObjectLocalException " + NL);
        } catch (NoSuchObjectLocalException nso) {
            svLogger.logp(Level.INFO, CLASSNAME, "", "Caught expected exception : " + nso + " " + NL);
        } catch (Throwable th) {
            fail("Unexpected exception from getInfo()" + th);
            svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when getting timer's info in testRollbackCreate(). ");
            th.printStackTrace(System.out);
        }

        // check if the ejbTimeout() is invoked.
        assertTrue("The timer should not be expired.", !svTimerRollbackCreateInvoked);
    }

    /**
     * Cancel a timer in a rolled back transaction. The timer should be still active. <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    @Override
    public void prep1RollbackCancel(String txType) {
        // Initialize static attributes
        svTimerRollbackCancelInvoked = false;

        TimerService ts = ivContext.getTimerService();

        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "prep1RollbackCancel(" + txType + ")");
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify the cancelled timer cannot be found in the timer service.");

        if (txType.equals("BMT")) {
            UserTransaction tx1 = ivContext.getUserTransaction();
            try {
                tx1.begin();
                svTimerRunLatch = new CountDownLatch(1);
                infoRollbackCancelLatch = new CountDownLatch(1);
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoRollbackCancel, false, null);
                tx1.commit();

                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer created successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prep1RollbackCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for RollbackCancel test" + e);
            }
        } else { // CMT
            try {
                svTimerRunLatch = new CountDownLatch(1);
                infoRollbackCancelLatch = new CountDownLatch(1);
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoRollbackCancel, false, null);

                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer created successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prep1RollbackCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for RollbackCancel test" + e);
            }
        }

        assertNotNull("Timer was not created", timer); //verify Timer was properly created
    }

    /**
     * Cancel a timer in a rolled back transaction. The timer should be still active. <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    @Override
    public void prep2RollbackCancel(String txType) {
        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "prep2RollbackCancel(" + txType + ")");
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify the cancelled timer cannot be found in the timer service.");

        timer = TimerHelper.getTimerWithMatchingInfo(ts, infoRollbackCancel);

        if (timer == null) {
            fail("The timer should be found in prep2RollbackCancel().");
            return;
        } else {
            svLogger.logp(Level.INFO, CLASSNAME, "", "The timer is found in prep2RollbackCancel().");
        }

        if (txType.equals("BMT")) {
            UserTransaction tx2 = ivContext.getUserTransaction();
            try {
                tx2.begin();
                timer.cancel();
                tx2.rollback();

                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer cancellation transaction rolled back successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when cancelling timer in prep2RollbackCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when calling timer.cancel()" + e);
            }
        } else { // CMT
            try {
                timer.cancel();
                ivContext.setRollbackOnly();
                svLogger.logp(Level.INFO, CLASSNAME, "", "Timer cancellation transaction rolled back successfully.");
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when cancelling timer in prep2RollbackCancel(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when calling timer.cancel()" + e);
            }
        }

        // finished trying to cancel timer... now allow the timer to run
        svTimerRunLatch.countDown();

        return;
    }

    /**
     * Cancel a timer in a rolled back transaction. The timer should be still active. <p>
     */
    @Override
    public void testRollbackCancel() {
        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "testRollbackCancel()");

        try {
            svLogger.logp(Level.INFO, "", "", "Wait enough time (timer expiration + precision) of the timer");
            infoRollbackCancelLatch.await(LATCH_AWAIT_TIME, TimeUnit.MILLISECONDS);

            // Notified when timer is running... now allow it to commit....
            FATHelper.sleep(COMMIT_PROCESSING_FUDGE);

            timer = TimerHelper.getTimerWithMatchingInfo(ts, infoRollbackCancel);
        } catch (Throwable th) {
            svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when searching timer in testRollbackCancel(). ");
            th.printStackTrace(System.out);
            fail("Unexpected exception from testRollbackCancel()" + th);
        }

        // check if the ejbTimeout() is invoked.
        assertTrue("The timer should have been expired and invoked ejbTimeout().", svTimerRollbackCancelInvoked);

        assertNull("Timer was still found in timer service", timer); //verify Timer was removed after timing out
    }

    @Override
    public void cancelAllTimers() {
        TimerService ts = ivContext.getTimerService();

        svLogger.logp(Level.INFO, "", "", "Calling cancelAllTimers() ... ");

        try {
            Collection<Timer> timers = ts.getTimers();
            for (Timer t : timers) {
                svLogger.logp(Level.INFO, "", "", "Cancelling timer with info : " + t.getInfo());
                t.cancel();
            }
        } catch (Throwable th) {
            svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when canceling timer in cancelAllTimers(). ");
            th.printStackTrace(System.out);
        }

        // Also remove any TxSyncSFBeans that may have been used
        svLogger.info("Removing " + svSFBeans.size() + " TxSyncSFBeans");
        for (TxSyncSFBean sfBean : svSFBeans) {
            try {
                sfBean.remove();
            } catch (Throwable ex) {
                // ignore; bean may have been destroyed on rollback
                svLogger.info("Failed to remove TxSyncBean " + sfBean + ", " + ex);
            }
        }
        svSFBeans.clear();
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     **/
    @Override
    public void ejbTimeout(Timer timer) {
        svLogger.logp(Level.INFO, "", "", "ejbTimeout is called ...");
        String info = (String) timer.getInfo();

        if (infoNoCommit.equals(info)) {
            ejbTimeoutNoCommit(timer);
            svTimerLatch.countDown();
            return;
        }

        // Every timer should have not null info. Boolean variables are used to
        // track whether a timer is invoked. Repeating timers are cancelled after
        // the 2nd expiration.

        svTimerRollbackCancelInvoked = false;
        svTimerCreateCancelInvoked = false;
        svTimerRollbackCreateInvoked = false;
        svTimerTxExpirationInvoked = false;

        if (info == null) {
            svLogger.logp(Level.INFO, "", "", "The timer's info is null. Testing in ejbTimeout aborted.");

        } else if (info.equals(infoRollbackCancel)) {
            // Wait to finish running the timer until the test is ready...
            try {
                svTimerRunLatch.await(LATCH_AWAIT_TIME, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught waiting for test to release timer to run.");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught waiting for test to release timer to run:" + e);
            }
            svTimerRollbackCancelInvoked = true;
            infoRollbackCancelLatch.countDown();
        } else if (info.equals(infoCreateCancel)) {
            svTimerCreateCancelInvoked = true;

        } else if (info.equals(infoRollbackCreate)) {
            svTimerRollbackCreateInvoked = true;

        } else if (info.equals(infoTxExpiration)) {
            svTimerTxExpirationInvoked = true;
            infoTxExpirationLatch.countDown();
        } else if (info.startsWith(infoEjbTimeoutCommit)) {
            if (info.endsWith(TX_BMT)) {
                testTxForBMTCommit();
            } else if (info.endsWith(TX_REQUIRESNEW)) {
                testTxForRequiresNew();
            } else {
                testTxForNotSupported();
            }
            svTimerLatch.countDown();
        } else if (info.startsWith(infoEjbTimeoutRollback)) {
            if (info.endsWith(TX_BMT)) {
                if (!svRollbackCalledOnceBMT) {
                    testTxForBMTRollback();
                    svRollbackCalledOnceBMT = true;
                } else {
                    testTxForBMTCommit();
                }
            } else if (info.endsWith(TX_REQUIRESNEW)) {
                if (!svRollbackCalledOnceRequiresNew) {
                    testTxForRequiresNewRollback();
                    svRollbackCalledOnceRequiresNew = true;
                } else {
                    testTxForRequiresNew();
                }
            }
            svTimerLatch.countDown();
        } else if (info.equals(infoAccessAfterCancel)) {
            testAccessAfterCancel(info, timer);
            svTimerLatch.countDown();
        } else if (info.equals(infoAccessAfterCancelFromTimeout)) {
            testAccessAfterCancelFromTimeout(info, timer);
            svTimerLatch.countDown();
        }

        // For debug, just print out that the timer expired, and the name of the
        // test it will execute.
        svLogger.logp(Level.INFO, "", "", "Timer expired: " + info);
    }

    /**
     * Test the behavior of the ejbTimeout method of a BMT Stateless session bean. The user
     * transaction in the method is open (not committed). The transaction is supposed to be rolled
     * back by the ejb container. <p>
     *
     * @param timer EJB Timer that has triggered the call to ejbTimeout
     **/
    public void ejbTimeoutNoCommit(Timer timer) {
        String info = (String) timer.getInfo();
        TxSyncSFBean sfBean = lookupSFBean();

        // Verify the info of the timer is as expected.
        assertTrue("Verifying the timer's info.", info.equals(infoNoCommit));

        UserTransaction tmpTx = ivContext.getUserTransaction();
        try {
            tmpTx.begin();
            sfBean.setInfo(info);
        } catch (Throwable t) {
            svLogger.info("Unexpected exception in ejbTimeoutNoCommit :" + t);
            t.printStackTrace(System.out);
            fail("Unexpected exception in ejbTimeoutNoCommit :" + t);
        }

        // For debug, just print out that the timer expired, and the name of the
        // test it will execute.
        svLogger.logp(Level.INFO, "", "", "Timer expired: " + info);
    }

    /**
     * Test the behavior of the ejbTimeout method of a CMT Stateless session bean. The user
     * transaction in the method is committed. <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    @Override
    public void prepEjbTimeoutCommitTest(String txType) {
        // Initialize static attributes
        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "prepEjbTimeoutCommitTest(" + txType + ")");

        // This test will wait for the timer to expire once
        svTimerLatch = new CountDownLatch(1);
        TxSyncSFBean.svInfo.clear();

        if (txType.equals(TX_BMT)) {
            UserTransaction tx1 = ivContext.getUserTransaction();
            try {
                tx1.begin();
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoEjbTimeoutCommit + "-" + txType, false, null);
                tx1.commit();
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepEjbTimeoutCommitTest(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for Tx commit test" + e);
            }
        } else { // CMT
            try {
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoEjbTimeoutCommit + "-" + txType, false, null);
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepEjbTimeoutCommitTest(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for Tx commit test" + e);
            }
        }

        assertNotNull("Timer was not created", timer); //verify Timer was properly created
    }

    /**
     * Test the behavior of the ejbTimeout method of a CMT Stateless session bean. The user
     * transaction in the method is rollback. <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    @Override
    public void prepEjbTimeoutRollbackTest(String txType) {
        // Initialize static attributes
        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "prepEjbTimeoutRollbackTest(" + txType + ")");

        // This test will wait for the timer to expire twice
        svTimerLatch = new CountDownLatch(2);
        TxSyncSFBean.svInfo.clear();

        if (txType.equals(TX_BMT)) {
            svRollbackCalledOnceBMT = false;
            UserTransaction tx1 = ivContext.getUserTransaction();
            try {
                tx1.begin();
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoEjbTimeoutRollback + "-" + txType, false, null);
                tx1.commit();
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepEjbTimeoutCommitTest(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for Tx commit test" + e);
            }
        } else { // CMT
            svRollbackCalledOnceRequiresNew = false;
            try {
                timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoEjbTimeoutRollback + "-" + txType, false, null);
            } catch (Throwable e) {
                svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepEjbTimeoutCommitTest(). ");
                e.printStackTrace(System.out);
                fail("Unexpected exception caught when creating timer for Tx commit test" + e);
            }
        }

        assertNotNull("Timer was not created", timer); //verify Timer was properly created
    }

    /**
     * Test method invoked by the ejbTimeout with Tx attribute Not Supported
     */
    private void testTxForBMTCommit() {
        TxSyncSFBean sfBean = lookupSFBean();

        UserTransaction tmpTx = ivContext.getUserTransaction();
        try {
            tmpTx.begin();
            sfBean.setInfo(TX_BMT);
            tmpTx.commit();
        } catch (Throwable t) {
            svLogger.info("Unexpected exception in testTxForBMTCommit :" + t);
            t.printStackTrace(System.out);
            fail("Unexpected exception in testTxForBMTCommit :" + t);
        }
    }

    /**
     * Test method invoked by the ejbTimeout with Tx attribute BMT
     */
    private void testTxForBMTRollback() {
        TxSyncSFBean sfBean = lookupSFBean();
        UserTransaction tmpTx = ivContext.getUserTransaction();
        try {
            tmpTx.begin();
            sfBean.setInfo(TX_BMT);
            tmpTx.setRollbackOnly();
            svLogger.logp(Level.INFO, CLASSNAME, "", "Transaction is rollback successfully.");
        } catch (Throwable t) {
            svLogger.info("Unexpected exception in testTxForBMTRollback :" + t);
            t.printStackTrace(System.out);
            fail("Unexpected exception in testTxForBMTRollback :" + t);
        }

    }

    /**
     * Test method invoked by the ejbTimeout with Tx attribute RequiresNew
     */
    private void testTxForRequiresNew() {
        TxSyncSFBean sfBean = lookupSFBean();
        sfBean.setInfo(TX_REQUIRESNEW);
    }

    /**
     * Test method invoked by the ejbTimeout with Tx attribute RequiresNew
     */
    private void testTxForRequiresNewRollback() {
        TxSyncSFBean sfBean = lookupSFBean();

        try {
            sfBean.setInfo(TX_BMT);
            ivContext.setRollbackOnly();
            svLogger.logp(Level.INFO, CLASSNAME, "", "Transaction is rollback successfully.");
        } catch (Throwable t) {
            svLogger.info("Unexpected exception in testTxForRequiresNewRollback :" + t);
            t.printStackTrace(System.out);
            fail("Unexpected exception in testTxForRequiresNewRollback :" + t);
        }
    }

    /**
     * Test method invoked by the ejbTimeout with Tx attribute Not Supported
     */
    private void testTxForNotSupported() {
        TxSyncSFBean sfBean = lookupSFBean();
        sfBean.setInfo(TX_NOTSUPPORTED);
    }

    /**
     * If a timer is created in a transaction. The timer will not be expired if the transaction has
     * not been committed. The timer's expiration should be counted from the time the timer is
     * created instead of the timer the transaction is committed.
     */
    @Override
    public void testTxExpirationBMT() {
        // Initialize static attributes

        svTimerTxExpirationInvoked = false;

        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.logp(Level.INFO, CLASSNAME, "", "testTxExpirationBMT()");
        svLogger.logp(Level.INFO, CLASSNAME, "", "Verify the cancelled timer cannot be found in the timer service.");

        UserTransaction tx1 = ivContext.getUserTransaction();
        try {
            svLogger.logp(Level.INFO, "", "", " Transaction start ... ");
            tx1.begin();
            infoTxExpirationLatch = new CountDownLatch(1);
            timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoTxExpiration, false, null);

            // wait enough time (timer expiration + pollinterval) of the timer

            svLogger.logp(Level.INFO, "", "", "Wait enough time (timer expiration + precision) of the timer");
            FATHelper.sleep(DEFAULT_EXPIRATION + TIMER_PRECISION);

            assertFalse("Transaction has not been commited. Timer shouldn't expired.", svTimerTxExpirationInvoked);

            // Commit the transaction
            svLogger.logp(Level.INFO, "", "", "Transaction commit() is called ... ");
            tx1.commit();

            // sleep for double pollinterval to check if the timer has expired.
            svLogger.logp(Level.INFO, "", "", "Sleep for retry interval to check if the timer has expired for BMT.");
            infoTxExpirationLatch.await(LATCH_AWAIT_TIME, TimeUnit.MILLISECONDS);

            assertTrue("Transaction has been commited. Timer should have expired.", svTimerTxExpirationInvoked);
        } catch (Throwable e) {
            svLogger.logp(Level.INFO, "", "", "Unexpected exception caught when creating timer in prepRollbackCreate(). ");
            e.printStackTrace(System.out);
            fail("Unexpected exception caught when creating timer for Tx commit test" + e);
        }

        assertNotNull("Timer was not created", timer); //verify Timer was properly created

    }

    /**
     * If a timer is created in a transaction. The timer will not be expired if the transaction has
     * not been committed. The timer's expiration should be counted from the time the timer is
     * created instead of the timer the transaction is committed.
     *
     * @param txType Transaction Type - TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    @Override
    public void prepTxExpirationCMT(String txType) {
        // Initialize static attributes
        svTimerTxExpirationInvoked = false;

        TimerService ts = ivContext.getTimerService();
        Timer timer = null;

        svLogger.info("prepTxExpirationCMT(" + txType + ")");

        try {
            // Wait less time if we know it should run right away...
            long expiration = txType.equalsIgnoreCase(TX_NOTSUPPORTED) ? SHORT_EXPIRATION : DEFAULT_EXPIRATION;

            svLogger.info(" Creating Timer ... ");
            infoTxExpirationLatch = new CountDownLatch(1);
            timer = TimerHelper.createTimer(ts, expiration, null, infoTxExpiration, false, null);

            if (!txType.equalsIgnoreCase(TX_NOTSUPPORTED)) {
                // The timer should not expire no matter how long we wait, so let's just sleep the normal amount of time.
                svLogger.info("Wait enough time (timer expiration + precision) for the timer to expire, but it shouldn't.");
                FATHelper.sleep(expiration + TIMER_PRECISION);
                assertFalse("Transaction has not been committed. Timer shouldn't have expired.", svTimerTxExpirationInvoked);
            } else {
                // We expect the timer can expire while we're still in this method, so let's wait around for it.
                svLogger.info("Wait for the timer to expire.");
                infoTxExpirationLatch.await(LATCH_AWAIT_TIME, TimeUnit.MILLISECONDS);
                assertTrue("Transaction with NotSupported attribute has been committed. Timer should expire.", svTimerTxExpirationInvoked);
            }
        } catch (Throwable e) {
            svLogger.info("Unexpected exception caught when creating timer in prepRollbackCreate(). ");
            e.printStackTrace(System.out);
            fail("Unexpected exception caught when creating timer for Tx commit test" + e);
        }

        tmpTimer = timer; // pass the timer object through the static var
    }

    /**
     * If a timer is created in a transaction. The timer will not be expired if the transaction has
     * not been committed. The timer's expiration should be counted from the time the timer is
     * created instead of the timer the transaction is committed.
     */
    @Override
    public void testTxExpirationCMT() {
        try {
            svLogger.info("Calling testTxExpirationCMT() ...");

            // sleep for double pollinterval to check if the timer has expired.
            svLogger.info("Sleep for retry interval to check if the timer has expired for CMT");
            infoTxExpirationLatch.await(LATCH_AWAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            svLogger.info("Unexpected exception caught when creating timer in prepRollbackCreate(). ");
            e.printStackTrace(System.out);
            fail("Unexpected exception caught when creating timer for Tx commit test" + e);
        }

        assertTrue("Transaction has been committed. Timer should have expired.", svTimerTxExpirationInvoked);
    }

    /**
     * Test the behavior of the ejbTimeout method of a BMT Stateless session bean. The user
     * transaction in the method is open (not committed). The transaction is supposed to be rolled
     * back by the ejb container. <p>
     */
    @Override
    public void prepNoCommitTest() {

        // This test will wait for the timer to expire twice
        svTimerLatch = new CountDownLatch(2);
        TxSyncSFBean.svInfo.clear();

        TimerService ts = ivContext.getTimerService();

        Timer timer = null;

        UserTransaction tx1 = ivContext.getUserTransaction();
        try {
            tx1.begin();
            timer = TimerHelper.createTimer(ts, (long) DEFAULT_EXPIRATION, null, infoNoCommit, false, null);
            assertNotNull(timer);
            tx1.commit();
        } catch (Throwable e) {
            String msg = "Unexpected exception caught when creating timer for Tx no-commit test.";
            svLogger.logp(Level.SEVERE, "", "", msg, e);
            fail(msg + e);
        }
    }

    @Override
    public void prepAccessAfterCancelTest(String info) {
        TimerService ts = ivContext.getTimerService();

        svAccessAfterCreateTimer = null;
        svAccessAfterCreateResult = null;

        svTimerLatch = new CountDownLatch(1);
        if (infoAccessAfterCancel.equals(info)) {
            svTimerStartedLatch = new CountDownLatch(1);
            svTimerCancelLatch = new CountDownLatch(1);
        }

        svLogger.info("prepAccessAfterCancelTest : creating a Timer to expire immediately");
        svAccessAfterCreateTimer = TimerHelper.createTimer(ts, (long) 0, null, info, false, null);
    }

    @Override
    public CountDownLatch cancelAccessAfterCancelTimer() {
        try {
            svLogger.info("cancelAccessAfterCancelTimer : waiting for timer to start");
            svTimerStartedLatch.await(LATCH_AWAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }
        svLogger.info("cancelAccessAfterCancelTimer : cancelling timer");
        svAccessAfterCreateTimer.cancel();
        return svTimerCancelLatch;
    }

    private void testAccessAfterCancel(String info, Timer timer) {
        try {
            svTimerStartedLatch.countDown();
            svLogger.info("testAccessAfterCancel: waiting for cancel to complete");
            svTimerCancelLatch.await(LATCH_AWAIT_TIME, TimeUnit.MILLISECONDS);

            svLogger.info("testAccessAfterCancel: attempting timer access from timeout");
            assertEquals("Timer.getInfo() returned unexpected value", info, timer.getInfo());
            timer.getNextTimeout();
            assertFalse("Timer is persistent, should be non-persistent", timer.isPersistent());
            timer.cancel();

            // Now that cancel has also been called from this timeout; verify the timer
            // methods now throw a NoSuchObjectLocalException
            try {
                Object timerInfo = timer.getInfo();
                fail("Timer.getInfo() returned unexpected value : " + timerInfo);
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            try {
                Date next = timer.getNextTimeout();
                fail("Timer.getNextTimeout() returned unexpected value : " + next);
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            try {
                boolean persistent = timer.isPersistent();
                fail("Timer.isPersistent() returned unexpected value : " + persistent);
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            try {
                timer.cancel();
                fail("Timer.persistent() returned without exception");
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            svAccessAfterCreateResult = info;
        } catch (Throwable ex) {
            svLogger.info("Unexpected Exception : " + ex);
            svAccessAfterCreateResult = ex;
        }
    }

    private void testAccessAfterCancelFromTimeout(String info, Timer timer) {
        try {
            svLogger.info("testAccessAfterCancel: cancelling timer");
            timer.cancel();

            svLogger.info("testAccessAfterCancel: attempting timer access from timeout");
            try {
                Object timerInfo = timer.getInfo();
                fail("Timer.getInfo() returned unexpected value : " + timerInfo);
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            try {
                Date next = timer.getNextTimeout();
                fail("Timer.getNextTimeout() returned unexpected value : " + next);
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            try {
                boolean persistent = timer.isPersistent();
                fail("Timer.isPersistent() returned unexpected value : " + persistent);
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            try {
                timer.cancel();
                fail("Timer.persistent() returned without exception");
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Caught excpected exception : " + ex);
            }

            svAccessAfterCreateResult = info;
        } catch (Throwable ex) {
            svLogger.info("Unexpected Exception : " + ex);
            svAccessAfterCreateResult = ex;
        }
    }

    @Override
    public void waitForTimer(long maxWaitTime) {
        try {
            svTimerLatch.await(maxWaitTime, TimeUnit.MILLISECONDS);
            if (svTimerLatch.getCount() == 0) {
                FATHelper.sleep(POST_INVOKE_DELAY); // wait for timer method postInvoke to complete
            }
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }
    }

}
