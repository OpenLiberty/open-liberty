/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import static com.ibm.ejs.container.ContainerProperties.ExcludeNestedExceptions;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionRolledbackException;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.util.FastHashtable;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.websphere.csi.CSIActivitySessionResetException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.csi.TxContextChange;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.traceinfo.ejbcontainer.TETxLifeCycleInfo;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

/**
 * A <code>TransactionControlImpl</code> provides access for
 * the EJB container to transaction service functions. It also
 * performs transaction context management for the container,
 * as required by the EJB spec. <p>
 * 
 * It is responsible for beginning/ending/suspending/resuming transactions
 * based on the transaction attributes of the methods invoked on a bean. <p>
 * 
 * It is not responsible for any portion of the EJB lifecycle, and does
 * not load, lock, or in any way manage EJBs (that's the container's
 * job). <p>
 **/

public class TransactionControlImpl
                implements UOWControl
{
    private static final TraceComponent tc =
                    Tr.register(TransactionControlImpl.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.csi.TransactionControlImpl";

    /**
     * Transaction service instance for global transaction support
     */
    protected EmbeddableWebSphereTransactionManager txService; //LIDB1673.2.1.5

    /**
     * // 131880-6
     * Single UOWCurrent instance // 131880-6
     */
    // 131880-6
    protected UOWCurrent uowCurrent; // 131880-6
                                     // 131880-6
    /**
     * Single LocalTransactionCurrent instance
     */
    protected LocalTransactionCurrent ltcCurrent;

    /**
     * Table of transaction strategies indexed by transaction attribute.
     */
    private TranStrategy[] txStrategies;

    /**
     * <code>UserTransactionImpl</code> instance used by this transaction
     * control for demarcation of global transactions.
     */
    private UserTransaction userTransactionImpl;

    /**
     * Hashtable of "sticky" local TXs (i.e. boundary = ActivitySession, rather
     * than a single method). The table contains only suspended local TXs.
     */
    FastHashtable<EJBKey, LocalTransactionCoordinator> stickyLocalTxTable = new FastHashtable<EJBKey, LocalTransactionCoordinator>(251);

    /*********************************************************************
     * 
     * Constructor(s)
     * 
     *********************************************************************/

    /**
     * Create a new <code>TransactionControlImpl</code> instance
     * configured into the given container. <p>
     */
    public TransactionControlImpl(UserTransaction userTx) // LIDB4171-35.03 F84120
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) { // 173022.3
            Tr.entry(tc, "<init>"); //LIDB4171-35.03
        }

        //LIDB4171-35.03 remove TxService and timeouts
        this.txService = EmbeddableTransactionManagerFactory.getTransactionManager();
        this.ltcCurrent = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        this.uowCurrent = EmbeddableTransactionManagerFactory.getUOWCurrent();

        this.userTransactionImpl = userTx;

        txStrategies = new TranStrategy[TransactionAttribute.getNumAttrs()];

        txStrategies[TransactionAttribute.TX_NOT_SUPPORTED.getValue()] =
                        new NotSupported(this);

        txStrategies[TransactionAttribute.TX_BEAN_MANAGED.getValue()] =
                        new BeanManaged(this);

        txStrategies[TransactionAttribute.TX_REQUIRED.getValue()] =
                        new Required(this);

        txStrategies[TransactionAttribute.TX_SUPPORTS.getValue()] =
                        new Supports(this);

        txStrategies[TransactionAttribute.TX_REQUIRES_NEW.getValue()] =
                        new RequiresNew(this);

        txStrategies[TransactionAttribute.TX_MANDATORY.getValue()] =
                        new Mandatory(this);

        txStrategies[TransactionAttribute.TX_NEVER.getValue()] =
                        new Never(this);

        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.exit(tc, "<init>");
        }

    } // TransactionControlImpl

    /*********************************************************************
     * 
     * The following section contains implementations to satisfy the
     * UOWControl interface.
     * 
     **********************************************************************/

    //LIDB1181.23.5
    /**
     * Perform transaction context management prior to invoking a method
     * with the given transaction attribute. <p>
     * 
     * @param key the <code>EJBKey</code> that identifies the bean
     *            instance the method is being invoked on <p>
     * 
     * @param methodInfo an <code>EJBMethodInfo</code> instance
     *            that described method being invoked <p>
     * 
     * @return the <code>UOWCookie</code> instance corresponding to
     *         the current transaction context; if there is no current
     *         transaction context returns null <p>
     * 
     * @exception CSIException thrown if an error occurs
     *                while performing transaction context management <p>
     */
    public UOWCookie preInvoke(EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.entry(tc, "preInvoke");
        }

        // Control point on application thread for effecting       //LIDB1673.2.1.5
        // rollback in the case of timeout                         //LIDB1673.2.1.5//d171654
        // conditions for transaction associated with the thread   //LIDB1673.2.1.5
        // (i.e. the calling bean)                                 //LIDB1673.2.1.5
        completeTxTimeout(); //LIDB1673.2.1.5

        TranStrategy ts =
                        txStrategies[methodInfo.getTransactionAttribute().getValue()];

        // LIDB1181.23.5
        //-------------------------------------------------------------
        // Suspend any local transaction that exists on entry. It
        // will be resumed on postInvoke.  Suspended coordinator is
        // carried forward to postInvoke in TxCookieImpl.
        //-------------------------------------------------------------

        // LIDB2446: shareable ltc -- suspend as required in individual transtrategies
        // LocalTransactionCoordinator suspendedLocal = suspendLocalTx(); // 131880-6

        //--------------------------------------------------------------------
        // Invoke preInvoke on the configured transaction strategy (REQUIRED,
        // MANDATORY, etc).  This may suspend a Global Tran, start a Local or
        // Global tran, or throw a CSIException (for example if MANDATORY and
        // there is no Global Tran), etc...
        //--------------------------------------------------------------------

        TxCookieImpl txCookie = null;
        try
        {
            txCookie = ts.preInvoke(key, methodInfo);
        } catch (Throwable th)
        {
            FFDCFilter.processException(th, CLASS_NAME + ".preInvoke",
                                        "266", this);
            if (isTraceOn && tc.isEventEnabled()) {
                Tr.event(tc, "Tran Strategy preInvoke failed", th);
            }

            //-----------------------------------------------------------------
            // If preInvoke failed, then postInvoke will not be called (as
            // there is no txCookie to return, so cleanup must be performed
            // here.  The only thing that needs to be done is to resume
            // a Local tran if it was suspended above.                  d123372
            //-----------------------------------------------------------------

            // LIDB2446: shareable ltc ... suspend will happen in transtrategy now
            // if (suspendedLocal != null)
            // {
            //    try
            //    {
            //       resumeLocalTx(suspendedLocal);
            //    }
            //    catch (Throwable ex)
            //    {
            //       FFDCFilter.processException(ex, CLASS_NAME + ".preInvoke",
            //                                   "280", this);
            //       if (isTraceOn && tc.isEventEnabled()) {
            //          Tr.event(tc, "Local tx resume failed", ex);
            //       }
            //    }
            // }

            if (th instanceof CSIException)
                throw (CSIException) th;
            else
                throw new CSIException("Tran Strategy preInvoke failed", th);
        }

        //d135218 - deleted check for suspend of both inbound local tx and
        //          inbound global tx.  By design, we should have a local tx
        //          or a global tx, but never both.

        // Store suspended local tx in cookie so we can resume it in postInvoke
        // LIDB2446: suspended ltc stored by individual transtrategies
        // if (suspendedLocal != null) {
        //    txCookie.suspendedLocalTx = suspendedLocal;
        // }

        txCookie.methodInfo = methodInfo;

        //------------------------------------------------------------------------
        // Now that the TranStrategy has finished, get the resulting
        // Tran Coordinator and cache it in the cookie.  Note that
        // the Coordinator may be null for BMT and EJB 1.0.              d139352-2
        //------------------------------------------------------------------------
        try
        {
            // The EJB Specification seems to imply that after setRollBackOnly()
            // has been called, the customer may elect to continue... even though
            // the tran must eventually rollback... so, do not throw an exception
            // here if the tran has been marked for rollback.               PQ99912
            txCookie.ivCoordinator = getCurrentTransactionalUOW(false); // d166414
        } catch (Throwable throwable)
        {
            FFDCFilter.processException(throwable, CLASS_NAME + ".preInvoke",
                                        "266", this);
            if (isTraceOn && tc.isEventEnabled()) {
                Tr.event(tc, "getCurrentTransactionalUOW failed", throwable);
            }

            //---------------------------------------------------------------------
            // Java EE Compliance - If we are invoking a method on a bean using
            // bean managed transactions, then let the method invocation proceed
            // despite the fact that the associated transaction has rolled back.
            // This allows a client to cleanup UserTransaction state (by calling
            // ut.commit() or ut.rollback() on the already rolled back
            // transaction. The method will continue on in the Txn which has been
            // marked for rollback
            //------------------------------------------------------------------
            boolean ivFailPreinvoke = false;//d171654

            //d174358 remove getValue calls
            if ((throwable instanceof CSITransactionRolledbackException) &&
                (methodInfo.getTransactionAttribute() ==
                TransactionAttribute.TX_BEAN_MANAGED))
            {
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "BMT - allowing method to proceed");
                // 160238-------------->
                // Ignore rolledback only state and get the tx

                try
                {
                    txCookie.ivCoordinator = getCurrentTransactionalUOW(false); // d166414
                } catch (Throwable throwable1)
                {
                    FFDCFilter.processException(throwable, CLASS_NAME + ".preInvoke",
                                                "386", this);

                    if (isTraceOn && tc.isEventEnabled())
                        Tr.event(tc, "BMT - failed trying to get Tran BMT bean in invalid state");
                    ivFailPreinvoke = true;//d171654

                }
                // <--------------160238

                if (isTraceOn && tc.isEntryEnabled()) // d173022.3
                    Tr.exit(tc, "preInvoke (RolledBack) using old stuff : " + txCookie);
                if (ivFailPreinvoke == false)
                    return txCookie;//d171654

            }

            //---------------------------------------------------------------------
            // If getCurrentTransactionalUOW failed, then postInvoke will not be
            // called (as txCookie will not be returned), so cleanup must be
            // performed here.  Since TranStrategy.preInvoke did return a txCookie
            // above, all that needs to be done for cleanup is to call postInvoke
            // with that txCookie.
            //---------------------------------------------------------------------

            try
            {
                postInvoke(key, txCookie, ExceptionType.UNCHECKED_EXCEPTION, methodInfo);
            } catch (Throwable th)
            {
                FFDCFilter.processException(th, CLASS_NAME + ".preInvoke",
                                            "266", this);
                if (isTraceOn && tc.isEventEnabled()) {
                    Tr.event(tc, "postInvoke failed : original exception re-thrown",
                             th);
                }
            }

            if (throwable instanceof CSIException)
                throw (CSIException) throwable;
            else
                throw new CSIException("getCurrentTransactionalUOW failed", throwable);
        }

        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.exit(tc, "preInvoke : " + txCookie);
        }

        return txCookie;

    } // preInvoke

    /**
     * Perform transaction context management after a method has been invoked
     * with the given transaction attribute. <p>
     * 
     * @param key the <code>EJBKey</code> of the bean method has
     *            been invoked on <p>
     * 
     * @param uowCookie the <code>UOWCookie</code> returned by corresponding
     *            preInvoke method <p>
     * 
     * @exception CSIException thrown if an error
     *                occurs while performing transaction context management <p>
     */
    public void postInvoke(EJBKey key, UOWCookie uowCookie,
                           ExceptionType exType, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.entry(tc, "postInvoke");
        }

        final TxCookieImpl txCookieImpl = (TxCookieImpl) uowCookie;

        // LIDB1181.23.5
        //--------------------------------------------------------
        // If the tx cookie is null then preinvoke went seriously
        // wrong. We cannot properly recover from this serious
        // internal error.  Mark any global transaction for
        // rollback.  If there is a local transaction we can end
        // it since it is local to this component instance.
        //--------------------------------------------------------

        if (txCookieImpl == null)
        {
            // Check for local transaction on this thread; if found, end it
            LocalTransactionCoordinator lCoord = getLocalCoord();
            if (lCoord != null) {
                try
                {
                    int resolver = methodInfo.getBeanMetaData()._localTran.getResolver();

                    // Is this an EJB 2.0 or newer component && the container is
                    // responsible for begin / end of connection specific local
                    // transactions (ie. RMLTs)?  For EJB 1.1 beans, local tx
                    // resolver will never be ContainerAtBoundary.  This is
                    // enforced by a check in  BeanMetaData.                  d135218

                    if (resolver == LocalTransactionSettings.RESOLVER_CONTAINER_AT_BOUNDARY)
                    {
                        // Complete the LTC context so enlisted resources will be
                        // rolled back. Container is responsible to automatically
                        // begin / end connections (i.e. local trancation
                        // resolver = containerAtBoundary).

                        lCoord.complete(LocalTransactionCoordinator.EndModeRollBack);

                    } else {

                        // Cleanup the LTC context so resources enlisted for cleanup
                        // will catch any application errors.
                        //  When resolver = application, the application code is
                        // responsible to do begin / end of the connections.

                        lCoord.setRollbackOnly();
                        lCoord.cleanup();
                    }

                } catch (RolledbackException ex) {

                    // do nothing... this is normal on setRollback, followed by cleanup

                    FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke",
                                                "325", this);

                } catch (Throwable ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke",
                                                "330", this);
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Failed to complete local transaction", ex);
                    }
                }
                throw new CSIException("Aborted improperly started local transaction");
            }

            // If there was no local tx on this thread, there should be a global tx;
            // mark it for rollback
            else {
                try {
                    setRollbackOnly();
                } catch (Throwable ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke",
                                                "345", this);
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Failed to mark global transaction for rollback", ex);
                    }
                    // By default - include cause.                            d672063
                    if (ExcludeNestedExceptions) {
                        ex = null;
                    }
                    throw new CSITransactionRolledbackException("Rolled back improperly started global transaction", ex);

                }
                throw new CSITransactionRolledbackException("Rolled back improperly started global transaction");
            }
        }

        final TranStrategy ts = txCookieImpl.txStrategy;

        try {
            if (exType == ExceptionType.NO_EXCEPTION) {
                ts.postInvoke(key, txCookieImpl, methodInfo);
            } else {
                ts.handleException(key, txCookieImpl, exType, methodInfo);
            }
        } finally
        {
            // LIDB1181.23.5
            //---------------------------------------
            // Resume any suspended local transaction
            //---------------------------------------

            LocalTransactionCoordinator suspendedLocalTx = txCookieImpl.suspendedLocalTx;
            if (suspendedLocalTx != null) {
                try {
                    resumeLocalTx(suspendedLocalTx);
                } catch (Exception ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke",
                                                "394", this);
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Local tx resume failed", ex);
                    }
                    throw new CSIException("Local tx resume failed", ex);
                }

            }

            // LIDB1181.23.5
            //---------------------------------------------------------------------
            // Resume any suspended global transaction
            //
            // There should be no global tx to resume if a local was resumed
            // above, but to insure the EJB Container always puts things back
            // the way it found them in preInvoke... at least look and resume
            // a global if it was also suspended. This may occur if an
            // application has used internals to begin/resume a global when
            // a local was already active on the thread.                    PI10351
            //---------------------------------------------------------------------
            Transaction suspendedTx = txCookieImpl.suspendedTx; //LIDB1673.2.1.5

            if (suspendedTx != null) {
                try {
                    resumeGlobalTx(suspendedTx, TIMEOUT_CLOCK_START);
                } catch (Exception ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke",
                                                "414", this);
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Global tx resume failed", ex);
                    }
                    throw new CSIException("Global tx resume failed", ex);
                }
                //d174358.1
            }
        } // finally

        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.exit(tc, "postInvoke");
        }

    } // postInvoke

    /**
     * Obtain UOW identifier which corresponds to whatever transaction
     * context exists on the current thread. This object will be an
     * implementation of one of the following two interfaces. Each of
     * these objects is a type of UOW that could be used to trigger
     * activation / passivation. Also, each of these objects supports
     * registration of a javax.activity.Synchronization.
     * 
     * 1) Transaction (if there is one) OR
     * 2) LocalTransactionCoordinator (if there is one) OR
     * 3) null
     * 
     * Throws CSITransactionRolledBackException if a transaction context
     * exists, but has already been marked RollbackOnly
     * 
     * @param checkMarkedRollback set to true if an exception should be thrown
     *            if the transaction has been marked for
     *            rollback only.
     * 
     * @exception CSITransactionRolledbackException rasied if a transaction
     *                exists on the current thread, but has been marked for
     *                rollbackOnly, is rolling back, or has rolled back. <p>
     */
    // Added checkMarkedRollback parameter.                              d166414
    public SynchronizationRegistryUOWScope getCurrentTransactionalUOW(boolean checkMarkedRollback)
                    throws CSITransactionRolledbackException
    {
        UOWCoordinator coord = uowCurrent.getUOWCoord(); // 131880-6
        if (coord != null)
        {
            if (checkMarkedRollback && coord.getRollbackOnly()) // 131880-6 d173218.1
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Attempting to do work on a tx " +
                                 "that has been marked rollback.");
                }
                throw new CSITransactionRolledbackException("Transaction rolled back");
            }
        } // if coord != null

        return (SynchronizationRegistryUOWScope) coord; // 131880-6

    } // getCurrentTransactionalUOW

    /**
     * Obtain UOW identifier which corresponds to whatever activity
     * session context exists on the current thread. The returned
     * object is null for TranscationControlImpl since there is never
     * an activity session when this collaborator implementation is
     * used.
     * 
     * Throws CSIActivitySessionResetException if an activity session
     * context exists, but has already been marked resetOnly
     * 
     * @param checkMarkedReset set to true if an exception should be thrown
     *            if the activitySession has been marked for
     *            reset only.
     * 
     * @exception CSIActivitySessionResetException rasied if an activitySession
     *                exists on the current thread, but has been marked for
     *                resetOnly. <p>
     */
    // Added checMarkedRest parameter.                                   d348420

    public Object getCurrentSessionalUOW(boolean checkMarkedReset)
                    throws CSIActivitySessionResetException
    {
        return (null);
    }

    /**
     * Get a <code>UserTransaction</code> instance suitable for a BeanO
     * which is handling a TX_BEAN_MANAGED bean to allow demarcation
     * of global transactions.
     */

    public UserTransaction getUserTransaction()
    {
        return userTransactionImpl;
    }

    //LIDB1181.23.5
    /**
     * Marks the current local or global transaction to be rolled back
     */

    public void setRollbackOnly()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.entry(tc, "setRollbackOnly", this);
        }

        LocalTransactionCoordinator lCoord = getLocalCoord();
        if (lCoord != null) {
            lCoord.setRollbackOnly();
        } else {
            try {
                txService.setRollbackOnly(); //LIDB1673.2.1.5
            } catch (Exception e) { //LIDB1673.2.1.5 SystemException

                // Shouldn't get here
                FFDCFilter.processException(e, CLASS_NAME + ".setRollbackOnly",
                                            "556", this);
                throw new IllegalStateException("No active transaction");
            }
        }
        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.exit(tc, "setRollbackOnly");
        }
    }

    //LIDB1181.23.5
    /**
     * Returns true iff the current local or global transaction has been
     * marked rollback only
     */

    public boolean getRollbackOnly()
    {
        LocalTransactionCoordinator lCoord = getLocalCoord();
        if (lCoord != null) {
            return (lCoord.getRollbackOnly());
        } else {

            int status = Status.STATUS_NO_TRANSACTION; //LIDB1673.2.1.5
            try { //LIDB1673.2.1.5
                status = txService.getStatus(); //LIDB1673.2.1.5
            } //LIDB1673.2.1.5
            catch (SystemException e) //LIDB1673.2.1.5
            { //LIDB1673.2.1.5
                FFDCFilter.processException(e, CLASS_NAME + ".getRollbackOnly",
                                            "667", this); //LIDB1673.2.1.5
            } //LIDB1673.2.1.5
            return (status == Status.STATUS_MARKED_ROLLBACK || //LIDB1673.2.1.5
                    status == Status.STATUS_ROLLEDBACK || //LIDB1673.2.1.5
            status == Status.STATUS_ROLLING_BACK); //LIDB1673.2.1.5

        }
    }

    /**
     * Enlist synchronization instance with the current local or global
     * transaction.
     */
    public void enlistWithTransaction(javax.transaction.Synchronization sync)
                    throws CSIException
    {
        SynchronizationRegistryUOWScope uowScope = getCurrentTransactionalUOW(false);
        if (uowScope == null)
        {
            throw new IllegalStateException("No active transaction");
        }

        enlistWithTransaction(uowScope, sync);
    }

    /**
     * Enlist synchronization instance with the specified UOWCoord
     */
    public void enlistWithTransaction(SynchronizationRegistryUOWScope uowCoord,
                                      Synchronization sync)
                    throws CSIException
    {
        try
        {
            if (uowCoord.getUOWType() == UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION)
            {
                ((Transaction) uowCoord).registerSynchronization(sync);
            }
            else
            {
                ((LocalTransactionCoordinator) uowCoord).enlistSynchronization(sync);
            }
        } catch (Exception ex)
        {
            throw new CSIException("Failed to enlist with transaction", ex);
        }
    }

    /**
     * Enlist synchronization instance with the current activity session.
     * This implementation always returns a CSI exception since there is
     * never an activity session when transaction control is being used
     * as the container's UOW control collaborator. <p>
     */
    public void enlistWithSession(Synchronization interestedParty)
                    throws CSIException
    {
        throw new CSIException("ActivitySession should not exist");
    }

    /**
     * Notification from the Container that the current ActivitySession ended.
     * This is used to release the collaborator's references to any sticky local
     * tx contexts (ie. local tx boundary = ActivitySession). This implementation
     * always returns a CSI exception since there is never an activity session
     * when transaction control is being used as the container's UOW control
     * collaborator. <p>
     */

    public void sessionEnded(EJBKey[] EjbKeyArray)
                    throws CSIException
    {
        throw new CSIException("ActivitySession should not exist");
    }

    //d126930.2
    /**
     * Ensure that the local transaction context for the bean instance
     * represented by the given BeanId is active on the thread. With
     * the introduction of ActivitySessions in R5.0 the bean may have
     * a sticky local tx context which needs to be resumed. <p>
     */

    public TxContextChange setupLocalTxContext(EJBKey key) throws CSIException //d174358.1
    {

        LocalTransactionCoordinator suspendedLocalTx = null;
        Transaction suspendedGlobalTx = null; //LIDB1673.2.1.5
        LocalTransactionCoordinator activatedLocalTx = null;
        BeanMetaData bmd = ((BeanId) key).getHome().getBeanMetaData(key);
        int localTxBoundary = bmd._localTran.getBoundary();

        // See if global transaction is active on thread.

        Transaction globalTxCoord = getGlobalCoord(); //LIDB1673.2.1.5

        if (globalTxCoord != null) {

            // If sticky local tx exists for this bean, swap contexts.
            // Otherwise, leave global tx on thread.  Sticky local
            // transctions may only be present if this is an EJB 2.0 bean
            // with local transaction boundary set to ActivitySession.
            // For EJB 1.1, local transaction boundary may never be set to
            // ActivitySession.  This is enforced by a check in BeanMetaData.

            if (localTxBoundary == LocalTransactionSettings.BOUNDARY_ACTIVITY_SESSION) { //d135218

                // see if this bean has a suspended sticky local tx

                LocalTransactionCoordinator stickyLocalTx = stickyLocalTxTable.remove(key);

                // If sticky local tx exists, swap contexts.  The contexts
                // will be swapped back by teardownLocalTxContext.

                if (stickyLocalTx != null) {

                    //174358.1-------------->
                    try { //d174358.1
                        suspendedGlobalTx = suspendGlobalTx(TIMEOUT_CLOCK_STOP);
                    } //d174358.1
                    catch (CSIException csie) //d174358.1
                    { //d174358.1
                        FFDCFilter.processException(csie, CLASS_NAME + ".setupLocalTxContext", "937", this);//d174358.3

                        stickyLocalTxTable.put(key, stickyLocalTx); //d174358.1
                        throw csie; //d174358.1
                    } //d174358.1

                    //<----------174358.1
                    resumeLocalTx(stickyLocalTx);
                    activatedLocalTx = stickyLocalTx;
                }
            }

            // Else, there may be a local transaction context on the thread

        } else {

            // See if there is already a local tx context on the thread

            LocalTransactionCoordinator currentLocalTx = getLocalCoord();

            // See if this bean also has a sticky local tx.  Sticky local
            // transctions may only be present if this is an EJB 2.0 bean
            // with local transaction boundary set to ActivitySession.  For
            // EJB 1.1 beans, local tx boundary may not be set to ActivitySession.
            // This is enforced by a check in BeanMetaData.

            LocalTransactionCoordinator stickyLocalTx = null;

            if (localTxBoundary == LocalTransactionSettings.BOUNDARY_ACTIVITY_SESSION) { //d135218

                stickyLocalTx = stickyLocalTxTable.remove(key);
            }

            if ((currentLocalTx != null) && (stickyLocalTx != null)) {

                // Local tx exists on thread, but bean also has a sticky local tx
                // so swap contexts now.  The contexts will be swapped back by
                // teardownLocalTxContext.

                suspendLocalTx();
                suspendedLocalTx = currentLocalTx;
                resumeLocalTx(stickyLocalTx);
                activatedLocalTx = stickyLocalTx;

                // If sticky local tx exists, but thread has no local tx resume sticky
                // local tx.  The sticky local tx will be suspended again by
                // teardownLocalTxContext.

            } else if ((currentLocalTx == null) && (stickyLocalTx != null)) {

                resumeLocalTx(stickyLocalTx);
                activatedLocalTx = stickyLocalTx;

                // Otherwise, do nothing. The thread may have an existing local tx context
                // or no transaction context at all.

            } else {

                // do nothing
            }

        } // end if no global tx present

        return (new TxContextChange(suspendedLocalTx, suspendedGlobalTx, activatedLocalTx, key));
    }

    //d126930.2
    /**
     * Restore any local transaction context change made by the
     * setupLocalTxContext method. <p>
     */

    public void teardownLocalTxContext(TxContextChange changedContext)
    {

        LocalTransactionCoordinator activatedLocalTx = changedContext.getActivatedLocalTx();

        // If setupLocalTxContext did nothing, teardownLocalContext will do nothing

        if (activatedLocalTx != null) {

            // setupLocalTxContext must have swapped a different local tx context
            // onto thread so suspend it now (ie. make local tx sticky)

            suspendLocalTx();
            stickyLocalTxTable.put(changedContext.getKey(), activatedLocalTx);

            // If setupLocalTxContext suspended a local tx which was already on the thread,
            // resume that context now (ie. swap back the original context)

            LocalTransactionCoordinator previousLocalTx = changedContext.getSuspendedLocalTx();
            if (previousLocalTx != null) {

                resumeLocalTx(previousLocalTx);
            }

            // If setupLocalTxContext suspended a global tx which was already on the thread,
            // resume that context now (ie. swap back the original context)
            Transaction previousGlobalTx = changedContext.getSuspendedGlobalTx(); //LIDB1673.2.1.5
            if (previousGlobalTx != null) {

                try {
                    resumeGlobalTx(previousGlobalTx, TIMEOUT_CLOCK_START);
                } catch (Exception ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".teardownLocalTxContext",
                                                "650", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Global tx resume failed", ex);
                    }
                }
            }

        } // End if setupLocalTxContext did swap contexts

    } // teardownLocalTxContext

    /*********************************************************************
     * 
     * This section contains internal methods which are NOT part of the
     * UOWControl interface.
     * 
     **********************************************************************/

    //d126930.2
    /**
     * Begin a new local trasaction context
     */
    final LocalTransactionCoordinator beginLocalTx()
    {
        LocalTransactionCoordinator lCoord = null;

        try {
            ltcCurrent.begin();
            lCoord = getLocalCoord(); // d175585
            if (TraceComponent.isAnyTracingEnabled()) // d527372
            {
                if (tc.isEventEnabled())
                {
                    if (lCoord != null) {
                        Tr.event(tc, "Began LTC cntxt: tid=" +
                                     Integer.toHexString(lCoord.hashCode()) + "(LTC)");
                    } else {
                        Tr.event(tc, "Began LTC cntxt: " + "null Coordinator!");
                    }
                }
                // d165585 Begins
                if (lCoord != null && TETxLifeCycleInfo.isTraceEnabled()) // d171555
                {
                    TETxLifeCycleInfo.traceLocalTxBegin("" + System.identityHashCode(lCoord), "Begin Local Tx");
                }
                // d165585 Ends
            }
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".beginLocalTx", "737", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Begin local tx failed", ex);
            }

        }
        return (lCoord);

    } // beginLocalTx

    //
    // Inactivity timeout actions for suspend and resume methods below
    //

    static final int TIMEOUT_CLOCK_START = 0;
    static final int TIMEOUT_CLOCK_STOP = 2;

    //LIDB1181.23.5
    /**
     * Suspend the current local transaction. If no local transaction exists
     * on the current thread, do nothing.
     */
    final LocalTransactionCoordinator suspendLocalTx() // 131880-6
    {
        // d173641 Begins
        LocalTransactionCoordinator lCoord = null; // d165585
        if (TraceComponent.isAnyTracingEnabled() && // d527372
            (tc.isEventEnabled() ||
            TETxLifeCycleInfo.isTraceEnabled()))
        {
            lCoord = getLocalCoord();

            if (lCoord != null &&
                TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "Suspending LTC cntxt: tid=" +
                             Integer.toHexString(lCoord.hashCode()) + "(LTC)");
            }
            // 173641 Ends
        }

        // d165585 Begins
        LocalTransactionCoordinator rtnVal = ltcCurrent.suspend(); // 131880-6

        if (lCoord != null &&
            TraceComponent.isAnyTracingEnabled() && // d527372
            TETxLifeCycleInfo.isTraceEnabled()) // d171555
        {
            TETxLifeCycleInfo.traceLocalTxSuspend("" + System.identityHashCode(lCoord), "Suspend Local Tx");
        }
        return rtnVal;
        // d165585 Ends
    }

    //LIDB1181.23.5
    /**
     * Suspend the current global transaction and return the Control instance for
     * it; the inactivity timeout is either started or stopped, according
     * to action. If no global transaction exists on the thread, do nothing;
     * returned Control object will be null is this case
     */
    final Transaction suspendGlobalTx(int action) throws CSIException //LIDB1673.2.1.5 //d174358.1
    {
        Transaction ctrl = null; //LIDB1673.2.1.5
        try { //LIDB1673.2.1.5

            ctrl = txService.suspend(); //LIDB1673.2.1.5
            if (TraceComponent.isAnyTracingEnabled()) // d527372
            {
                if (tc.isEventEnabled()) //LIDB1673.2.1.5
                    Tr.event(tc, "Suspending TX cntxt: " + ctrl); //LIDB1673.2.1.5

                // d165585 Begins
                if (TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
                { // PQ74774
                    String idStr = null; // d171555
                    if (ctrl != null) // d171555
                        idStr = ctrl.toString(); // d171555
                    int idx;
                    idStr = (idStr != null)
                                    ? (((idx = idStr.indexOf("(")) != -1)
                                                    ? idStr.substring(idx + 1, idStr.indexOf(")"))
                                                    : ((idx = idStr.indexOf("tid=")) != -1)
                                                                    ? idStr.substring(idx + 4)
                                                                    : idStr)
                                    : "NoTx";
                    TETxLifeCycleInfo.traceGlobalTxSuspend(idStr, "Suspend Global Tx");
                } // PQ74774
                // d165585 Ends
            }
        } //LIDB1673.2.1.5
        catch (SystemException e) //LIDB1673.2.1.5
        { //LIDB1673.2.1.5
            FFDCFilter.processException(e, CLASS_NAME + ".setRollbackOnly", "770", this); //LIDB1673.2.1.5
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) //LIDB1673.2.1.5
                Tr.event(tc, "Error suspending global tx", e); //LIDB1673.2.1.5
            throw new CSIException("suspend global tx failed", e);//d174358.1
        } //LIDB1673.2.1.5

        if (ctrl != null) {
            int txtype = ((UOWCoordinator) ctrl).getTxType(); //LIDB1673.2.1.5
            // NonInteropControls have no coordinator

            if (txtype == UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL)
                return ctrl; //LIDB1673.2.1.5
        }
        return ctrl;
    }

    //LIDB1181.23.5
    /**
     * Resume the local transaction associated with the given coordinator
     * instance. If the coordinator passed in is null, no local context
     * will be resumed, but a trace message is generated because we do
     * not expect callers to try resuming a null local coordinator object.
     * IllegalStateException is raised if a global transaction context
     * exists on the current thread.
     */

    final void resumeLocalTx(LocalTransactionCoordinator lCoord)
                    throws IllegalStateException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            if (lCoord != null) {
                Tr.event(tc, "Resuming LTC cntxt: tid=" +
                             Integer.toHexString(lCoord.hashCode()) + "(LTC)");
            } else {
                Tr.event(tc, "Resuming LTC cntxt: " + "null Coordinator!");
            }
        }
        ltcCurrent.resume(lCoord);

        // d165585 Begins
        if (TraceComponent.isAnyTracingEnabled() && // d527372
            lCoord != null && TETxLifeCycleInfo.isTraceEnabled()) // d171555
        {
            TETxLifeCycleInfo.traceLocalTxResume("" + System.identityHashCode(lCoord), "Resume Local Tx");
        }
        // d165585 Ends
    }

    //LIDB1181.23.5
    /**
     * Resume the global transaction associated with the given Control instance.
     * The inactivity timeout is either started or stopped, according
     * to action. If InvalidTransactionException is raised by the global tx service
     * it is passed on to the caller of this method.
     */
    final void resumeGlobalTx(Transaction ctrl, int action) //LIDB1673.2.1.5
    throws SystemException, InvalidTransactionException //LIDB1673.2.1.5
    {

        try { //LIDB1673.2.1.5
            txService.resume(ctrl); //LIDB1673.2.1.5
        } //LIDB1673.2.1.5
        catch (SystemException e) //LIDB1673.2.1.5
        { //LIDB1673.2.1.5
            FFDCFilter.processException(e, CLASS_NAME + ".resumeGlobalTx", "814", this); //LIDB1673.2.1.5
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) //LIDB1673.2.1.5
                Tr.event(tc, "Error resuming global tx", e); //LIDB1673.2.1.5
            throw e; //LIDB1673.2.1.5
        } //LIDB1673.2.1.5

        if (TraceComponent.isAnyTracingEnabled()) // d527372
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "Resumed TX cntxt: " + txService.getTransaction()); //LIDB1673.2.1.5

            // d165585 Begins
            if (TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
            { // PQ74774
                String idStr = null; // d171555
                if (ctrl != null) // d171555
                    idStr = ctrl.toString(); // d171555
                int idx;
                idStr = (idStr != null)
                                ? (((idx = idStr.indexOf("(")) != -1)
                                                ? idStr.substring(idx + 1, idStr.indexOf(")"))
                                                : ((idx = idStr.indexOf("tid=")) != -1)
                                                                ? idStr.substring(idx + 4)
                                                                : idStr)
                                : "NoTx";
                TETxLifeCycleInfo.traceGlobalTxResume(idStr, "Resume Global Tx");
            } // PQ74774
            // d165585 Ends
        }

        // NonInteropControls have no coordinator
        if (ctrl != null && ((UOWCoordinator) ctrl).getTxType() == UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL)
            return; //LIDB1673.2.1.5
    }

    //LIDB1181.23.5
    /*
     * Return local transaction coordinator if a local tx is
     * associated with the current thread. The state of the
     * transaction is not considered (i.e. it will return a
     * coordinator even if the transaction is marked for
     * rollback). Returns null if there is no local transaction
     * context on this thread.
     */

    final LocalTransactionCoordinator getLocalCoord()
    {
        return (ltcCurrent.getLocalTranCoord());
    }

    //LIDB1181.23.5
    /*
     * Return global transaction coordinator if a global tx is
     * associated with the current thread. The state of the
     * transaction is not considered (i.e. a coordinator is
     * returned even if the current transaction is marked for
     * rollback). Returns null if there is no global transaction
     * context on this thread.
     */

    final Transaction getGlobalCoord() //LIDB1673.2.1.5
    {
        try {
            Transaction control = txService.getTransaction(); //LIDB1673.2.1.5

            return control; //LIDB1673.2.1.5

        } catch (SystemException ex) { //LIDB1673.2.1.5
            FFDCFilter.processException(ex, CLASS_NAME + ".getGlobalCoord",
                                        "856", this);
            Tr.warning(tc, "TRANSACTION_COORDINATOR_NOT_AVAILABLE_CNTR0022E",
                       new Object[] { ex }); //p111002.5
        }
        return null;
    }

    /**
     * Complete processing of passive transaction timeout.
     * The timer pops on a timeout thread; the transaction is
     * rolled back on the application thread when the container
     * has control. d171654
     * 
     * @exception CSITransactionRolledbackException is thrown in the event of timeout.
     */

    final public void completeTxTimeout() throws //LIDB1673.2.1.5
    CSITransactionRolledbackException //LIDB1673.2.1.5
    { //LIDB1673.2.1.5
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        try
        { //LIDB1673.2.1.5

            if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
                Tr.entry(tc, "completeTxTimeout");// d171654
            }
            txService.completeTxTimeout(); //LIDB1673.2.1.5
        } //LIDB1673.2.1.5
        catch (TransactionRolledbackException e) //LIDB1673.2.1.5
        { //LIDB1673.2.1.5
            FFDCFilter.processException(e, CLASS_NAME + ".completeTxTimeout", "1390", this); //LIDB1673.2.1.5
            if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
                Tr.exit(tc, "completeTxTimeout throwing CSITransactionRolledBackException");// d171654
            }
            throw new CSITransactionRolledbackException("Transaction rolled back", e); //LIDB1673.2.1.5
        }
        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            //LIDB1673.2.1.5
            Tr.exit(tc, "completeTxTimeout exit");// d171654                                             //LIDB2669.2.5
        }
    }

    //LIDB1673.2.1.5

    /**
     * Return boolean true if there is an active BeanManaged Transaction
     * currently associated with the calling thread.
     */
    //167937 - added entire method
    public boolean isBmtActive(EJBMethodInfoImpl methodInfo)
    {
        TranStrategy ts = txStrategies[methodInfo.getTransactionAttribute().getValue()];
        return ts.isBmtActive();
    }

    /**
     * Return boolean true if there is an active BeanManaged ActivitySession
     * currently associated with the calling thread.
     */
    //LIDB2018-1 added entire method
    public boolean isBmasActive(EJBMethodInfoImpl methodInfo)
    {
        // There is never a ActivitySession when TransactionControlImpl
        // object is being used, so return false.
        return false;
    }

    /**
     * This method allows the <code>TransactionControlImpl</code> to suspend the current
     * transaction (local or global), if one should exist. This method will suspend a global
     * or local Tx should one exist. A <code>UOWHandle</code> will be returned as follows:
     * 1) If a global tx was suspended, the handle will contain the global tx.
     * 2) If a local tx was suspended, the handle will contain the LocalTransactionCoordinator
     * which can be used to resume the suspended local tx.
     * 3) If a tx wasn't suspended (i.e. a global or local tx doesn't exist on the thread), a
     * null handle will be returned to indicate this.
     * 
     * @return A <code>UOWHandle</code> will be returned which containes the suspended local
     *         or global transaction if one was suspended, null otherwise.
     */
    //PK15508: Added entire method
    public UOWHandle suspend() throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "suspend");
        UOWHandle toReturn = null;

        //First, look for a global Tx, if non-existant then look for a local Tx.
        //If neither are present, return a null UOWHandle

        //If a global tx coordinator is on the thread, getGlobalCoord() will return it in
        //which case we know there is a global tx on the thread. If not, getGlobalCoord
        //will return null in which case we can then try to see if there is a local tx.
        if (getGlobalCoord() != null) {
            Transaction gtx = suspendGlobalTx(TIMEOUT_CLOCK_STOP);
            toReturn = new UOWHandleImpl(gtx);
        }
        //If a local tx coordinator is on the thread, getLocalCoord will return it.  If not,
        //null will be returned.
        else if (getLocalCoord() != null) {
            LocalTransactionCoordinator ltc = suspendLocalTx();
            toReturn = new UOWHandleImpl(ltc);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "suspend : " + toReturn);
        return toReturn;
    }//suspend

    /**
     * This method will use the <code>UOWHandle</code> passed to it to resume the Tx (if any)
     * that was previously suspended. If the handle is null, nothing is done (a null
     * handle indicates that on the corresponding suspend, there wasn't a Tx to
     * suspend). If the handle contains a suspended global or local Tx, it will be resumed.
     * 
     * @param handle A <code>UOWHandle</code> that contains the suspended transaction.
     *            A null handle means that on the corresponding call to suspend
     *            there was nothing to suspend.
     */
    //PK15508: Added entire method
    public void resume(UOWHandle handle) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resume : " + handle);

        //A null handle indicates that during the execution of the corresponding suspend,
        //there wasn't a Tx to suspend, so do nothing.
        if (handle != null) {
            //Cast the handle to a UOWHandleImpl in order to get the instance
            //variables from it.
            UOWHandleImpl handleImpl = (UOWHandleImpl) handle;

            try {
                //From the handle we can determine if it is a local tx.  If so resume the local
                // tx, otherwise we have a global tx.
                if (handleImpl.suspendedLocalTx != null) {
                    //Get the LocalTransactionCoordinator from the handle, and resume the local
                    //Tx associated with it.
                    resumeLocalTx(handleImpl.suspendedLocalTx);
                }
                else {
                    //Get the suspended global tx from the handle, and resume the global
                    //tx associated with it.
                    resumeGlobalTx(handleImpl.suspendedGlobalTx, TIMEOUT_CLOCK_START);
                }
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".resume", "1491", this);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "resume", "Error resuming tx in TransactionControlImpl: " + t);
                throw new CSIException("Error resuming tx in TransactionControlImpl.", t);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resume");
    } //resume
} // TransactionControlImpl
