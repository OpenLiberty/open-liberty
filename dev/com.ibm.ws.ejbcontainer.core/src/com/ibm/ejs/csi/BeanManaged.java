/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import java.util.Hashtable;

import javax.transaction.Transaction;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager.InactivityTimer;
import com.ibm.ws.tx.embeddable.NativeJDBCDriverHelper;

/**
 * <code> BeanManaged </code> implements TX_BEAN_MANAGED semantics.
 * If the bean is called under a client initiated global tx, this
 * tx is suspended, and then resumed at postInvoke. For R5.0 TD,
 * a change was made so that the container begins a local
 * transaction on preInvoke. The local tx will be suspended by the
 * transaction manager if the bean code begins a global tx during
 * the dispatched method request. The transaction manager will
 * resume the local transaction again when the bean code commits
 * or rolls back the global tx it started. The container ends the
 * local tx at postInvoke. <p>
 * 
 * One special case exists for stateful session beans. They may
 * begin a global tx in the bean method, and not commit it until
 * a later method call. In this case the container needs to
 * suspend the global tx on postInvoke and resume it whenever
 * the bean is dispatched again (i.e. subsequent preInvoke). In
 * IBM we refer to this as a "sticky" global tx. In this case
 * the sticky global tx is resumed at preInvoke instead of
 * beginning a local tx. <p>
 **/

final class BeanManaged extends TranStrategy
{
    private static final TraceComponent tc =
                    Tr.register(BeanManaged.class, "EJBContainer",
                                "com.ibm.ejs.container.container"); // d123896

    private Hashtable<EJBKey, Transaction> suspendedBeans = new Hashtable<EJBKey, Transaction>();

    EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();

    BeanManaged(TransactionControlImpl txCtrl)
    {
        super(txCtrl);
    }

    /**
    *
    **/
    @Override
    TxCookieImpl preInvoke(EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean entryEnabled =
                        TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (entryEnabled) {
            Tr.entry(tc, "preInvoke");
        }
        Transaction control = null; // LIDB1673.2.1.5

        TxCookieImpl returnCookie = null;

        //---------------------------------------
        // Suspend any client initiated global tx
        //---------------------------------------

        Transaction suspended = null; // LIDB1673.2.1.5

        // Suspend a local tran, if it exists (null is returned if not).
        // This should only be required if a global tran is not present,
        // but this code tolerates scenarios where applications use
        // internals to begin/resume global trans on threads that already
        // have a local transaction.                              LIDB2446 PI10351
        LocalTransactionCoordinator savedLocalTx = suspendLocalTx();

        if (globalTxExists(false))
        {
            suspended = suspendGlobalTx(TransactionControlImpl.TIMEOUT_CLOCK_STOP);
        }

        //------------------------------------------------------------
        // If method is being invoked on a stateful session bean then
        // must resume any suspended transaction instance associated
        // with this stateful session bean (i.e. sticky global tx)
        //------------------------------------------------------------

        control = suspendedBeans.remove(key); // LIDB1673.2.1.5
        if (control != null)
        {
            // Have to stop timer before attempting resume          // LIBDB1673.24
            tm.stopInactivityTimer(control);

            // if timeout has already occured, resume will fail     // LIBDB1673.24
            // note the resumeGlobalTx throws only CSIException
            // d174385----------->
            try
            {
                resumeGlobalTx(control, TransactionControlImpl.TIMEOUT_CLOCK_STOP);
            } catch (CSIException ivtxn)
            {
                try
                {
                    if (suspended != null)
                    {
                        resumeGlobalTx(suspended,
                                       TransactionControlImpl.TIMEOUT_CLOCK_STOP);//d174358
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Resumed suspended global tran after " +
                                         "resume of sticky tran failed"); // d174358
                    }
                    else // LIDB2446 check for suspended ltc here and resume
                    if (savedLocalTx != null)
                    {
                        resumeLocalTx(savedLocalTx);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Resume of suspended local tx after resume of sticky tran failed");
                    }
                } catch (Throwable t)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Resume of suspended global tran after " +
                                     "resume of sticky tran failed"); // d174358

                    if (!(t instanceof CSIException))
                        FFDCFilter.processException(ivtxn, CLASS_NAME + ".preinvoke",
                                                    "139", this); // d174358
                }
                //<---------------d174385

                throw new CSITransactionRolledbackException("Failed to resume transaction, possibly rolled back", ivtxn);
            }

            returnCookie = new TxCookieImpl(false, false, this, suspended);
        }
        else
        {
            //-----------------------------------------------------------
            // Othewise, begin a local tx now until the bean code decides
            // to start a global tx (new for R5.0 TD).  This is only done
            // for EJB 2.0 or newer beans to assure no behavioral changes
            // are introduced for older Java EE 1.2 applications (i.e EJB 1.0
            // or EJB 1.1 beans) except for z/OS which always has either   //LIDB2775-107.1
            // a local or global tran active.                              //LIDB2775-107.1
            //-----------------------------------------------------------

            final int EJBVersion = methodInfo.getBeanMetaData().getEJBModuleVersion();

            // Is this an EJB 2.0 or newer component?
            if ((EJBVersion >= BeanMetaData.J2EE_EJB_VERSION_2_0) // d174083 LIDB2775-107.1
                || (com.ibm.ws.Transaction.TxProperties.LTC_ALWAYS_REQUIRED)) //LIDB2775-107.1
            {
                returnCookie = beginLocalTx(key, methodInfo, suspended); // LIDB2446
            }
            else
            {
                returnCookie = new TxCookieImpl(false, false, this, suspended);
            }
        }

        if (entryEnabled) {
            Tr.exit(tc, "preInvoke");
        }

        // LIDB2446 add suspended ltc to cookie
        returnCookie.suspendedLocalTx = savedLocalTx;

        return (returnCookie);

    } // preInvoke

    /**
    *
    **/
    @Override
    void postInvoke(EJBKey key, TxCookieImpl txCookie, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean entryEnabled =
                        TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (entryEnabled) {
            Tr.entry(tc, "postInvoke");
        }

        Transaction control; // LIDB1673.2.1.5

        //----------------------------------------------------------
        // If bean is a stateful session bean and transaction has
        // not committed then suspend the current transaction and
        // associate it with the stateful session bean (ie. sticky
        // global tx).  Otherwise, any global tx started by the
        // bean must have completed by now.
        //----------------------------------------------------------

        if (globalTxExists(false))
        {
            if (txCookie.methodInfo.isStatefulSessionBean())
            {
                // Control point on application thread for effecting rollback
                // in the case of timeout conditions         // LIDB2669.2.5 d171654
                CSITransactionRolledbackException rollbe = null; // d174385
                try // LIDB2669.2.5
                { // LIDB2669.2.5
                    txCtrl.completeTxTimeout(); // LIDB2669.2.5
                } // LIDB2669.2.5
                catch (CSITransactionRolledbackException rbe) // LIDB2669.2.5
                { // LIDB2669.2.5
                    // Finish off the rollback and throw CSIException // LIDB2669.2.5
                    rollback(false, key, methodInfo); // LIDB2669.2.5 d174358
                    // throw rbe;                                     // LIDB2669.2.5
                    rollbe = rbe;
                } // LIDB2669.2.5

                control = suspendGlobalTx(TransactionControlImpl.TIMEOUT_CLOCK_START);

                // Place the suspended global tran in the list of suspended
                // beans list so it may be resumed later.                    d190186
                suspendedBeans.put(key, control);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Added to suspendedBeans list"); // LIBDB1673.24

                // start inactivity timer                            // LIBDB1673.24
                if (tm.startInactivityTimer(control, new InactivityAlarm(control)))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Inactivity timer started");
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Failed to start inactivity timer: transaction " +
                                     "not active or never times out"); // d190186
                }

                // Notify DB2 that the next request in-tran may occur on a
                // different thread.                                       d241820.1
                if (EJSPlatformHelper.isZOS())
                {
                    NativeJDBCDriverHelper.threadSwitch();
                }

                //d174358------->
                if (rollbe != null)
                {
                    throw rollbe;
                }
                //d174358------->
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                {
                    // d135218 - fixed msg below
                    Tr.event(tc, "Illegal Bean Managed Transaction: " +
                                 "Only stateful session bean initiated transactions " +
                                 "may span method requests.");
                }
                rollback(true, key, methodInfo);
                throw new CSITransactionRolledbackException();
            }

        }
        else
        {
            //-------------------------------------------------------------
            // Complete local tran here since super.postInvoke only completes
            // local tran started by preInvoke.  The local tran may not have
            // started during preInvoke.  This is the special case where a
            // Bean Managed transaction is a "sticky" global tran that is
            // completed by business method calling either commit or rollback
            // on UserTransaction.  When that happens, the transaction service
            // will create a local tran after completing the sticky global tran.
            // This only applies to EJB 2.0 or newer components so Java EE 1.2
            // applications (i.e. EJB 1.0 or EJB 1.1 beans) will not
            // experience a behavioral change.
            // zOS behaves like EJB2.0 in that there is always a tran active.  //LIDB2775-107.1
            //-------------------------------------------------------------

            final int EJBVersion = methodInfo.getBeanMetaData().getEJBModuleVersion();
            // d120623 d120792

            // Is this an EJB 2.0 or newer component?
            if ((EJBVersion >= BeanMetaData.J2EE_EJB_VERSION_2_0) //d174083 LIDB2775-107.1
                || (com.ibm.ws.Transaction.TxProperties.LTC_ALWAYS_REQUIRED)) //LIDB2775-107.1
            {
                // EJB 2.0 or greater bean.
                //d123896 - start of change.
                if (txCtrl.getRollbackOnly()) {
                    rollback(true, key, methodInfo);
                } else {
                    commit(key, methodInfo);
                }
                //d123896 - end of change.
            }
        }

        if (entryEnabled) {
            Tr.exit(tc, "postInvoke");
        }

    } //postInvoke

    /*
     * This method handles exceptions for TX_BEAN_MANAGED method calls.
     * Most of the work it can delegate to its parent implementation,
     * however, cases 2 and 3 (see TranStrategy.handleException())
     * must be handled here.
     */
    @Override
    public void handleException(EJBKey key, TxCookieImpl txCookie,
                                ExceptionType exType, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        //d174358---------->
        // For TX_BEAN_MANAGED beans unchecked exceptions result in
        // the transaction being rolled back, but the client should
        // always receive RemoteException, not TransactionRolledbackException

        if (exType == ExceptionType.UNCHECKED_EXCEPTION &&
            globalTxExists(false) && !txCookie.beginner)
        {
            //If an unchecked exception is thrown, we can never access the bean again
            // rollback tran
            if (txCookie.methodInfo.isStatefulSessionBean())
            {
                rollback(true, key, methodInfo); //LIDB2669.2.5//d174358
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                {
                    //d135218 - fixed msg below
                    Tr.event(tc, "Illegal Bean Managed Transaction: " +
                                 "Only stateful session bean initiated transactions " +
                                 "may span method requests.");
                }
                rollback(true, key, methodInfo);
                throw new CSITransactionRolledbackException();

            }//<----------d174358

            // By returning, we'll end up throwing the exception which
            // caused this rollback to the client

            return;
        }

        super.handleException(key, txCookie, exType, methodInfo);

    } // handleException

    //LIBDB1673.24 start
    /**
     * The InactivityAlarm is a private class which is used to handle the problem
     * of sticky BMT trans which timeout. Because of the nature of sticky BMT trans,
     * the tran service can not detect whether one has timed out. Therefore an Inactivity
     * alarm is created whenever a sticky global tran is suspspended. Likewise it is
     * cancelled when resumed. The alarm is a subclass of the tx service
     * InactivityTimer, and when the alarm goes off, the tran is resumed, and then
     * rolled back.
     */
    // d171654
    private class InactivityAlarm implements InactivityTimer
    {
        Transaction _control;

        //d171654 removed key
        /*
         * d171654
         * The InactivitiyAlarm constructor takes a control object
         * The control object is then used to resume the global tran and the tran can then
         * be rolled back
         * 
         * @param Transaction control used to resume the global tran and roll it back
         */
        public InactivityAlarm(Transaction control) //d171654 removed key
        {
            //d171654 removed key
            _control = control;
        }

        /**
         * When the alarm goes off, the global transaction is resumed and rolled back
         * No exceptions are thrown in the event of failure. Note that this is
         * syncrhonized with stopInactivityTimer, so it will comnplete and
         * stopInactivityTimer will block
         */
        // d171654
        public void alarm()
        {
            final boolean entryEnabled =
                            TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
            if (entryEnabled) {
                Tr.entry(tc, "InactivityAlarm.alarm");
            }

            try
            {
                tm.resume(_control);
                tm.rollback(); //d174358 should we even do this and not suspend it?
                //<----------d174358
            } catch (Exception ex)
            {
                FFDCFilter.processException(ex, CLASS_NAME + ".alarm", "374", this); // d174358
                // if the resume fails the transaction has already completed.
                // ignore exception
            }

            if (entryEnabled) {
                Tr.exit(tc, "InactivityAlarm.alarm");
            }
        }

    }

    //LIBDB1673.24 end

    /**
     * Return boolean true if there is an active BeanManaged Transaction
     * currently associated with the calling thread.
     */
    //167937 - added entire method
    @Override
    boolean isBmtActive()
    {
        try
        {
            return globalTxExists(false);
        } catch (CSIException e)
        {
            // FFDCFilter.processException(e, CLASS_NAME + ".isBmtActive", "276", this);
            return false;
        }
    }

    private static final String CLASS_NAME = "com.ibm.ejs.csi.BeanManaged";//d174358

} // BeanManaged
