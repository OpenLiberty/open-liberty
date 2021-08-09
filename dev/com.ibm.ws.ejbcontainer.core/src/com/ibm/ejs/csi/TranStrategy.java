/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
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
import static com.ibm.websphere.csi.MethodInterface.MESSAGE_LISTENER;
import static com.ibm.websphere.csi.MethodInterface.TIMED_OBJECT;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRequiredException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.traceinfo.ejbcontainer.TETxLifeCycleInfo;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.embeddable.UOWCompensatedException;
import com.ibm.wsspi.uow.UOWManagerFactory;

/**
 * The base class of all <code>TranStrategy</code> implementations. <p>
 * 
 * A <code>TranStratgey</code> provides transaction management for
 * EJBs as specified by one of the transaction attributes in the
 * EJB spec.
 * 
 * There is a single <code>TranStratgey</code> instance per
 * <code>TransactionControlImpl</code>. They are stateless and designed
 * to be reentrant so that multiple concurrent threads do not have
 * to serialize their access. <p>
 **/

abstract class TranStrategy
{
    private static final String CLASS_NAME = TranStrategy.class.getName();

    private static final TraceComponent tc = Tr.register(TranStrategy.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    //d458031: UOWSynch is used to set the JPA task name.
    private static final UOWSynchronizationRegistry svUOWSynchReg =
                    UOWManagerFactory.getUOWManager();

    /**
     * Transaction control instance this <code>TranStrategy</code>
     * instance associated with
     */
    protected TransactionControlImpl txCtrl;

    /**
     * Single LocalTransactionCurrent instance
     */
    protected LocalTransactionCurrent ltcCurrent;

    /**
     * Create new TranStratgey instance.
     */
    protected TranStrategy(TransactionControlImpl txCtrl)
    {
        this.txCtrl = txCtrl;
        this.ltcCurrent = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent(); //LIDB1673.2.1.5
    } // TranStrategy

    // preInvoke - must be overridden by subclasses
    abstract TxCookieImpl preInvoke(EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws CSIException;

    /**
     * Base implementation of postInvoke; commits any local or global
     * transaction which was started during preInvoke, unless
     * setRollbackOnly() was called. If setRollback was called, the
     * transaction is rolled back now.
     */
    void postInvoke(EJBKey key, TxCookieImpl cookie, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.entry(tc, "postInvoke");
        }

        // Any local transaction which exists now was started in preInvoke

        if (cookie.beginner) {
            if (txCtrl.getRollbackOnly())
            {
                // Before rolling back, first determine if the rollback was the
                // result of the transaction timing out. If it did time out, then
                // and exception needs to be thrown.                         PM63801
                CSITransactionRolledbackException timeoutEx = null;
                try
                {
                    txCtrl.completeTxTimeout();
                } catch (CSITransactionRolledbackException ex)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "tran timed out; will throw ex after rollback");
                    timeoutEx = ex;
                }

                rollback(true, key, methodInfo);

                // Because of the asynchronous nature of timeouts, if the tran
                // did timeout, then the timeout exception needs to be thrown
                // regardless of the bean type or setRollbackOnly behavior
                // setting. If the beginner did call setRollbackOnly(), then
                // this exception may still be discarded later.              PM63801
                if (timeoutEx != null)
                {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "postInvoke : " + timeoutEx);
                    throw timeoutEx;
                }

                MethodInterface methodType = methodInfo.getInterfaceType();

                // Transaction has been marked rollbackonly (due to either a
                // timeout or call to setRollbackOnly), and has now been rolled
                // back... so throw an exception to notify the client.
                // Note: this will be caught and discarded if bean called
                // setRollbackOnly().                                   d159491
                // Note: Beginning with EJB 3.0 (CTS) it is required that the
                // bean method return normally regardless of how the tx was
                // marked for rollback, so don't throw the exception.   d461917
                // Also, the exception needs to be thrown for MDBs and timer
                // methods to let the messaging and scheduler services know
                // that the method failed/rolledback.                   d470213
                // Note:  We will now allow EJB 3.0 modules to have the old
                // 2.x SetRollbackOnly behavior, and we will allow EJB 2.x
                // modules to have the new 3.0 SetRollbackOnly behavior.
                // EJB 3.x modules will exhibit the 2.x behavior if the
                // application is listed in the com.ibm.websphere.ejbcontainer.
                // limitSetRollbackOnlyBehaviorToInstanceFor property.
                // Likewise, the EJB 2.x modules will exhibit the 3.x
                // SetRollbackOnly behavior if the application is listed
                // in the com.ibm.websphere.ejbcontainer.
                // extendSetRollbackOnlyBehaviorBeyondInstanceFor property.
                // These properties were analyzed when the EJBModuleMetaData
                // was created and the "ivUseExtendedSetRollbackOnlyBehavior"
                // was set.     //d461917.1
                BeanMetaData bmd = methodInfo.getBeanMetaData();
                EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
                if (isTraceOn && tc.isDebugEnabled()) // d461917.1
                {
                    Tr.debug(tc, "EJBModuleMetaDataImpl.ivUseExtendedSetRollbackOnlyBehavior = "
                                 + mmd.ivUseExtendedSetRollbackOnlyBehavior);
                }
                if (!mmd.ivUseExtendedSetRollbackOnlyBehavior ||
                    methodType == MESSAGE_LISTENER ||
                    methodType == TIMED_OBJECT)
                {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "postInvoke : Transaction marked rollbackonly");
                    throw new CSITransactionRolledbackException("Transaction marked rollbackonly"); // LIDB1673.2.1.5
                }
            }
            else
            {
                commit(key, methodInfo);
            }
        }
        else //LIDB1673.2.1.5
        { //LIDB1673.2.1.5
            // Control point on application thread for effecting       //LIDB1673.2.1.5
            // rollback in the case of timeout                         //LIDB1673.2.1.5//d171654
            // conditions                                              //LIDB1673.2.1.5
            txCtrl.completeTxTimeout(); //LIDB1673.2.1.5
        } //LIDB1673.2.1.5

        if (isTraceOn && tc.isEntryEnabled()) { // d173022.3
            Tr.exit(tc, "postInvoke");
        }
    }

    /*
     * The handleException method is called when a bean method
     * is terminated via an exception. Its responsibility is to
     * determine if the current transaction should be aborted. If
     * the transaction is aborted it throws an exception indicating
     * this fact which should ultimately be propagated to the client
     * of the bean method call. See Section 12 of the 1.0 EJB Spec for
     * more details.
     * 
     * This base implementation of this method handles all exceptions
     * regardless of the transaction attribute for the method.
     */

    void handleException(EJBKey key, TxCookieImpl txCookie,
                         ExceptionType type, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {

        if (type == ExceptionType.CHECKED_EXCEPTION) {

            // Checked exceptions always result in "normal" transaction
            // resolution semantics

            postInvoke(key, txCookie, methodInfo);
            return;
        }

        if (txCookie.beginner) {

            // Unchecked exceptions on container-started transactions
            // result in a rollback; the client will receive the original
            // exception (i.e. some RemoteException)

            rollback(true, key, methodInfo);

            // By returning, we'll end up throwing the exception which
            // caused this rollback to the client

            return;
        }

        // The container did not begin a transaction. If one exsits,
        // it must be client initialed global tx. Mark it for
        // rollback and throw TransactionRolledbackException
        //d174358
        if (globalTxExists(false))
        {

            // Control point on application thread for effecting       //LIDB1673.2.1.5
            // rollback in the case of timeout                        //LIDB1673.2.1.5//d171654
            // conditions.                                             //LIDB1673.2.1.5
            // completeTxTimeout will throw                            //LIDB1673.2.1.5
            // CSITransactionRolledbackException if the tran has timed out //LIDB1673.2.1.5
            txCtrl.completeTxTimeout(); //LIDB1673.2.1.5
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "handleException: marking client transaction rollbackOnly"); //LIDB1673.2.1.5
            }

            rollback(false, key, methodInfo);
            throw new CSITransactionRolledbackException();
        }

        // LIDB2446: could be a shareable ltc which should be marked rollbackonly
        final LocalTransactionCoordinator coord = ltcCurrent.getLocalTranCoord();
        if (coord != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "handleException: marking client LTC rollbackOnly");
            }
            coord.setRollbackOnly();
        }

    } // handleException

    // LIDB1181.23.5
    // d123423
    /*
     * This utility attempts to begin a new local transaction. If
     * the begin fails an exception consistent with the EJB exception
     * conventions is thrown. Beginning with R5.0 (d123423), some
     * EJB 2.0 beans support sticky local TXs for ActivitySessions.
     * If a sticky local TX exists, we resume it rather than starting
     * a new one.
     */

    final TxCookieImpl beginLocalTx(EJBKey ejbKey, EJBMethodInfoImpl methodInfo, Transaction suspendedGlobalTx)
                    throws CSIException
    {
        BeanMetaData bmd = methodInfo.getBeanMetaData();
        final int EJBType = bmd.getEJBComponentType();
        final LocalTransactionSettings ltcSettings = bmd._localTran;
        final boolean ltcBoundaryIsAS = ltcSettings.getBoundary() == LocalTransactionSettings.BOUNDARY_ACTIVITY_SESSION; // d171555.3.2

        boolean suspendedLocalTxFound = false;

        // LIDB2446: reference to local tx suspended during this method
        LocalTransactionCoordinator savedLocalTx = null;
        boolean begunLocalTx = false;

        // If this is an EJB 2.0 (or newer) SFSB or EntityBean which is configured
        // for local transactions with duration longer than method (ie. boundary =
        // ActivitySession, there might be a sticky local TX which we should resume now.
        // Note that EJB 1.1 beans never have local tx boundary = ActivitySession.
        // This is enforced by a check in BeanMetaData.

        if (ltcBoundaryIsAS && //d135218 d171555.3.2
            (EJBType == InternalConstants.TYPE_STATEFUL_SESSION ||
             EJBType == InternalConstants.TYPE_BEAN_MANAGED_ENTITY ||
            EJBType == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY)) {

            LocalTransactionCoordinator suspendedLocalTx = txCtrl.stickyLocalTxTable.remove(ejbKey);

            if (suspendedLocalTx != null) {
                suspendedLocalTxFound = true;
                try
                {
                    // LIDB2446 save any existing local tran prior to resuming sticky ltc
                    savedLocalTx = suspendLocalTx();

                    resumeLocalTx(suspendedLocalTx);

                    // LIDB2446: remember local tx 'started'
                    begunLocalTx = true;

                } catch (Throwable ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".beginLocalTx",
                                                "200", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Local tx resume failed", ex);
                    }

                    // LIDB2446: have to restore any suspended local tx here
                    if (savedLocalTx != null)
                    {
                        try
                        {
                            resumeLocalTx(savedLocalTx);
                        } catch (Throwable ex2) {
                            FFDCFilter.processException(ex2, CLASS_NAME + ".beginLocalTx",
                                                        "212", this);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(tc, "Saved local tx resume failed", ex2);
                            }
                        }
                    }

                    throw new CSIException("Resume local tx failed", ex);
                }

            }

        } // end if EJB 2.0+, SFSB or Entity Bean, with local tx boundary = ActivitySession

        // If we didn't find a sticky local TX to resume, begin a new Local Tx now

        if (!suspendedLocalTxFound) {

            try {
                //PK16329: Begins
                //PK16329: Ooops, prior to PK16329, the begin of an ltc was like this:
                //   ltcCurrent.begin(ltcBoundaryIsAS);
                //It seems someone forgot to get the "unresolved action" and "resolver"
                //from the bean meta data.  Therefore, the transaction guys were always
                //getting the default "unresolved action" and "resolver" even if the user
                //set specific values in these fields.  Lets call the 'begin' that takes
                //these values as parameters:
                //First, get the "unresolved action" as a boolean
                final boolean ltcUnresActionIsCommit = ltcSettings.getUnresolvedAction() == LocalTransactionSettings.UNRESOLVED_COMMIT;

                //Second, get the "resolver" as a boolean
                final boolean ltcResolverIsCAB = ltcSettings.getResolver() == LocalTransactionSettings.RESOLVER_CONTAINER_AT_BOUNDARY;

                // LIDB2446: check for shareable LTC
                final boolean ltcShareable = ltcSettings.isShareable();
                LocalTransactionCoordinator coord = ltcCurrent.getLocalTranCoord();

                if (coord == null)
                {
                    // call the begin method that takes "unresolved action" and "resolver"
                    if (ltcShareable)
                        ltcCurrent.beginShareable(ltcBoundaryIsAS, ltcUnresActionIsCommit, ltcResolverIsCAB);
                    else
                        ltcCurrent.begin(ltcBoundaryIsAS, ltcUnresActionIsCommit, ltcResolverIsCAB); // 118180, 120870.3 d171555.3.2 PK16329
                    //PK16329: Ends

                    svUOWSynchReg.putResource("com.ibm.websphere.profile", // d458031
                                              methodInfo.getJPATaskName()); // d515803
                    begunLocalTx = true;
                }
                else if (coord.isShareable() && ltcShareable)
                {
                    // nothing to do ... this context shared
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "LTC context shared");
                }
                else
                {
                    // suspend current and begin new ltc
                    savedLocalTx = suspendLocalTx();

                    if (ltcShareable)
                        ltcCurrent.beginShareable(ltcBoundaryIsAS, ltcUnresActionIsCommit, ltcResolverIsCAB);
                    else
                        ltcCurrent.begin(ltcBoundaryIsAS, ltcUnresActionIsCommit, ltcResolverIsCAB); // 118180, 120870.3 d171555.3.2 PK16329

                    svUOWSynchReg.putResource("com.ibm.websphere.profile", // d458031
                                              methodInfo.getJPATaskName()); // d515803
                    begunLocalTx = true;
                }

                // d171555.3.2 Begins
                if (TraceComponent.isAnyTracingEnabled() && // d527372
                    (tc.isEventEnabled() ||
                    TETxLifeCycleInfo.isTraceEnabled()))
                {
                    LocalTransactionCoordinator lCoord = txCtrl.getLocalCoord(); // d165585

                    if (tc.isEventEnabled())
                    {
                        if (lCoord != null) {
                            Tr.event(tc, "Began LTC cntxt: tid=" +
                                         Integer.toHexString(lCoord.hashCode()) + "(LTC)");
                        } else {
                            Tr.event(tc, "Began LTC cntxt: " + "null Coordinator!");
                        }
                        if (begunLocalTx)
                        {
                            Tr.event(tc, "Set JPA task name: " + methodInfo.getJPATaskName());
                        }
                    }
                    // d165585 Begins
                    if (lCoord != null && TETxLifeCycleInfo.isTraceEnabled()) // d171555.3.2
                    {
                        TETxLifeCycleInfo.traceLocalTxBegin
                                        ("" + System.identityHashCode(lCoord), "Begin Local Tx");
                    }
                    // d165585 Ends
                }
                // d171555.3.2 Ends
            } catch (Exception ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".beginLocalTx", "217", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Begin local tx failed", ex);
                }

                // LIDB2446: have to restore any suspended local tx here
                if (savedLocalTx != null)
                {
                    try
                    {
                        resumeLocalTx(savedLocalTx);
                    } catch (Throwable ex2) {
                        FFDCFilter.processException(ex2, CLASS_NAME + ".beginLocalTx",
                                                    "242", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "Saved local tx resume failed", ex2);
                        }
                    }
                }

                throw new CSIException("Begin local tx failed", ex);
            }
        }

        // LIDB2446: return cookie
        TxCookieImpl cookie = new TxCookieImpl(begunLocalTx, true, this, suspendedGlobalTx);
        cookie.suspendedLocalTx = savedLocalTx;
        return cookie;
    }

    // d123423
    /**
     * Suspend the current local transaction. If no local transaction exists
     * on the current thread, do nothing.
     */
    final LocalTransactionCoordinator suspendLocalTx()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEventEnabled())
        {
            LocalTransactionCoordinator currentLTC = ltcCurrent.getLocalTranCoord();
            if (currentLTC != null)
            {
                Tr.event(tc, "Suspending LTC cntxt: tid=" + Integer.toHexString(currentLTC.hashCode()) + "(LTC)");
            }
        }

        LocalTransactionCoordinator suspendedLocalTx = ltcCurrent.suspend();

        if (isTraceOn && suspendedLocalTx != null && TETxLifeCycleInfo.isTraceEnabled()) // d171555
        {
            TETxLifeCycleInfo.traceLocalTxSuspend("" + System.identityHashCode(suspendedLocalTx), "Suspend Local Tx");
        }

        return suspendedLocalTx;
    }

    // d123423
    /**
     * Resume the local transaction associated with the given coordinator
     * instance. If the coordinator passed in is null, no local context
     * will be resumed. IllegalStateException is raised if a global
     * transaction context exists on the current thread.
     */

    final void resumeLocalTx(LocalTransactionCoordinator lCoord)
                    throws IllegalStateException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEventEnabled()) {
            if (lCoord != null) {
                Tr.event(tc, "Resuming LTC cntxt: tid=" +
                             Integer.toHexString(lCoord.hashCode()) + "(LTC)");
            } else {
                Tr.event(tc, "Resuming LTC cntxt: " + "null Coordinator!");
            }
        }
        ltcCurrent.resume(lCoord);
        // d165585 Begins
        if (isTraceOn && // d527372
            lCoord != null && TETxLifeCycleInfo.isTraceEnabled()) // d171555
        {
            TETxLifeCycleInfo.traceLocalTxResume("" + System.identityHashCode(lCoord), "Resume Local Tx");
        }
        // d165585 Ends
    }

    //LIDB1181.23.5
    /*
     * This helper method returns true iff there is a global transaction
     * context associated with the current thread.
     * A CSITransactionRequiredException is thrown if there is a non-interoperable
     * transaction on the thread and the caller does not allow this.
     * Callers that do not allow this are the strategies: Required, Supports
     * and Mandatory.
     */
    private EmbeddableWebSphereTransactionManager txManager = EmbeddableTransactionManagerFactory.getTransactionManager(); // d173641

    final boolean globalTxExists(boolean failIfNonInterop)
                    throws CSIException
    {
        Transaction tx = null;

        try
        {
            tx = txManager.getTransaction(); // d173641
        } catch (SystemException se)
        {
            //FFDCFilter.processException(se, CLASS_NAME + ".globalTxExists", "217", this);//d174358.3

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "Could not determine if there is a global tx active");
            }
        }

        if ((failIfNonInterop) && (tx != null) && //LIDB1673.2.1.5
            (((UOWCoordinator) tx).getTxType() == //LIDB1673.2.1.5
            UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL)) //LIDB1673.2.1.5
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Cannot proceed under a non-interoperable transaction context");
            }
            throw new CSITransactionRequiredException("Interoperable global transaction required");
        }

        return (tx != null);//LIDB1673.2.1.5
    }

    // LIDB1181.23.5
    /*
     * This utility attempts to begin a new global transaction. If the
     * begin fails an exception consistent with the EJB exception
     * conventions is thrown.
     */

    final void beginGlobalTx(EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        //d135218 - removed check which assures that EJB 2.0 beans with
        //          sticky local transactions do not try to become involved
        //          in multiple transactions simultaneously.  This is checked
        //          during activation now.

        try {
            // d201891 global tx set time out value is now done by the tx service
            //    during begin processing.
            txCtrl.txService.begin();
            svUOWSynchReg.putResource("com.ibm.websphere.profile", // d458031
                                      methodInfo.getJPATaskName()); // d515803
            if (TraceComponent.isAnyTracingEnabled()) // d527372
            {
                if (tc.isEventEnabled())
                {
                    Tr.event(tc, "Began TX cntxt: " + txCtrl.txService.getTransaction()); //LIDB1673.2.1.5
                    Tr.event(tc, "Set JPA task name: " + methodInfo.getJPATaskName());
                }

                // d165585 Begins
                if (TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
                { // PQ74774
                    String idStr = txCtrl.txService.getTransaction().toString();
                    int idx;
                    idStr = (idStr != null)
                                    ? (((idx = idStr.indexOf("(")) != -1)
                                                    ? idStr.substring(idx + 1, idStr.indexOf(")"))
                                                    : ((idx = idStr.indexOf("tid=")) != -1)
                                                                    ? idStr.substring(idx + 4)
                                                                    : idStr)
                                    : "NoTx";
                    TETxLifeCycleInfo.traceGlobalTxBegin(idStr, "Begin Global Tx");
                } // PQ74774
                // d165585 Ends
            }
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".beginGlobalTx", "243", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Begin global tx failed", ex);
            }
            throw new CSIException("Begin global tx failed", ex);
        }
    }

    //LIDB1181.23.5
    /*
     * This utility suspends the current global transaction and
     * returns the associated control object. If the transaction
     * exists, it is suspended without regard to whether it has
     * been marked for rollback. If the transaction does not exist,
     * the return Control will be null.
     */

    final Transaction suspendGlobalTx(int action) throws CSIException //174358.1 //LIDB1673.2.1.5
    {
        final Transaction result = txCtrl.suspendGlobalTx(action); //LIDB1673.2.1.5

        // d165585 Begins
        if (TraceComponent.isAnyTracingEnabled() && // d527372
            TETxLifeCycleInfo.isTraceEnabled()) // d171555
        {
            TETxLifeCycleInfo.traceGlobalTxSuspend("NoTx", "Suspend Global Tx");
        }
        // d165585 Ends
        return result;

    }

    //LIDB1181.23.5
    /*
     * This utility method attempts to resume the global transaction
     * associated with the input control object. If it fails, an
     * an exception consistent with the EJB exception conventions
     * is thrown. <p>
     */
    final void resumeGlobalTx(Transaction control, int action) //LIDB1673.2.1.5
    throws CSIException
    {
        try {
            txCtrl.resumeGlobalTx(control, action);
            // d165585 Begins
            if (TraceComponent.isAnyTracingEnabled() && // d527372
                TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
            { // PQ74774
                String idStr = txCtrl.txService.getTransaction().toString();
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

        } catch (Exception ex) { //LIDB1673.2.1.5

            FFDCFilter.processException(ex, CLASS_NAME + ".resumeGlobalTx",
                                        "335", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Global tx resume failed", ex);
            }
            throw new CSIException("", ex);
        }
    }

    /**
     * End a local transaction within an activity session. This method should
     * only be called if {@link LocalTransactionCoordinator#isASScoped} returns
     * true.
     */
    // RTC103197
    private void endASLocalTx(EJBKey key,
                              EJBMethodInfoImpl methodInfo,
                              LocalTransactionCoordinator lCoord,
                              boolean rollback)
    {
        // For EJB 2.0 SFSBs or Entity Beans with local tx boundary = ActivitySession,
        // do not end the local tx context now, but suspended it for reuse in
        // susequent methods (i.e. "sticky" local tx).  This is not done for home
        // methods because we have the key of the home rather than the key of the
        // bean instance.
        //
        // In the rollback case, where a bean method has thrown an exception, we
        // do not end the local tx context now.  The ActivitySession should have
        // been marked resetOnly which will eventually casue this local tx to end
        // with rollback.  In the mean time the application may continue to do
        // work on this local context, but it will be futile.
        //
        // Note that EJB 1.1 beans never have local tx boundary = ActivitySession.
        // This is enforced by a check in BeanMetaData.

        int type = methodInfo.getBeanMetaData().getEJBComponentType();
        if (!methodInfo.isHome() && // d126930.3
            type != InternalConstants.TYPE_MESSAGE_DRIVEN)
        {
            if (rollback)
            {
                lCoord.setRollbackOnly(); // d179185
            }

            suspendLocalTx();

            if ((type == InternalConstants.TYPE_STATEFUL_SESSION ||
                 type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY ||
                type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY) &&
                !methodInfo.isBeanRemove()) // d179185, d180809
            {
                txCtrl.stickyLocalTxTable.put(key, lCoord);
            }
        }
    }

    // d121667
    // d123423
    /*
     * This utility attempts to commit the current local or global
     * transaction. If the commit fails, it throws an exception
     * consistent with the EJB exception conventions.
     * 
     * Beginning with R5.0 (d123423), local transactions may just be
     * suspended at postInvoke time. This is to support sticky
     * local TXs for ActivitySessions.
     * 
     * Note that commit must be done through internal TX service
     * interfaces, not the UserTransaction interface which an
     * application would use.
     */

    final void commit(EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws CSITransactionRolledbackException
    {
        LocalTransactionCoordinator lCoord = ltcCurrent.getLocalTranCoord();

        // If local tx exists; process it.  There must not be a global tx.

        if (lCoord != null)
        {
            if (lCoord.isASScoped()) // d130989
            {
                endASLocalTx(key, methodInfo, lCoord, false); // RTC103197
            }
            // Otherwise, sticky local tx not required so end local tx now.
            else
            {
                try {
                    // d171555.3.2 Begins
                    //                     int resolver = (((EJBComponentMetaData)methodInfo.getComponentMetaData()).getLocalTranConfigData()).getValueResolver();
                    //
                    //                     // Is this an EJB 2.0 or newer component && the container is responsible for begin / end
                    //                     // of connection specific local transactions (ie. RMLTs)?  We only need to check
                    //                     // resolver because EJB 1.1 beans never have resolver = ContainerAtBoundary.  This
                    //                     // is enforced by a check in BeanMetaData.
                    //
                    //                     if (resolver == LocalTranConfigData.RESOLVER_CONTAINER_AT_BOUNDARY) {  //d135218
                    //
                    //                        // Complete the LTC context so enlisted resources will be committed.
                    //                        // When Container is responsible to automatically begin / end connections,
                    //                        // (i.e. local trancation resolver = containerAtBoundary) the connection
                    //                        // manager enlists each connection as a resource in the local transaction
                    //                        // context.  Enlist causes LocalTransaction.begin to occur on the
                    //                        // connections.  Here we are "completing" the context which causes
                    //                        // LocalTransaction.commit to occur for the enlisted connections.
                    //
                    //                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    //                           Tr.event(tc, "Completing LTC cntxt: " + System.identityHashCode(lCoord));
                    //                        }
                    //                        lCoord.complete(LocalTransactionCoordinator.EndModeCommit);
                    //                        // d165585 Begins
                    //                        if( TETxLifeCycleInfo.isTraceEnabled() )              // PQ74774
                    //                        {
                    //                            if( lCoord != null )
                    //                            {
                    //                                TETxLifeCycleInfo.traceLocalTxCommit( "" + System.identityHashCode( lCoord ), "Commit Local Tx - complete" );
                    //                            }
                    //                        }
                    //                        // d165585 Ends
                    //
                    //                     } else {
                    //
                    //                        // Cleanup the LTC context so resources enlisted for cleanup will catch any
                    //                        // application errors.  When resolver = application, the application code is
                    //                        // responsible to do begin / end of the connections.  In this case the
                    //                        // connection manager enlists the connections for cleanup in the local
                    //                        // transaction context.  They are delisted iff the application remembers to
                    //                        // end them (ie. commit or rollback).  Here we end the context with
                    //                        // "cleanup" which will cause commit or rollback of any remaining connections
                    //                        // (ie. application forgot to finish them), based on the bean's local
                    //                        // transaction configuration setting for unresolved-action (ie. COMMIT or
                    //                        // ROLLBACK).
                    //
                    //                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    //                           Tr.event(tc, "Ending LTC cntxt: " + System.identityHashCode(lCoord));
                    //                        }
                    //                        lCoord.cleanup();
                    //                        // d165585 Begins
                    //                        if( TETxLifeCycleInfo.isTraceEnabled() )              // PQ74774
                    //                        {
                    //                            if( lCoord != null )
                    //                            {
                    //                                TETxLifeCycleInfo.traceLocalTxCommit( "" + System.identityHashCode( lCoord ), "Commit Local Tx - cleanup" );
                    //                            }
                    //                        }
                    //                        // d165585 Ends
                    //                     }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Completing LTC cntxt: tid=" +
                                     Integer.toHexString(lCoord.hashCode()) + "(LTC)");

                    lCoord.end(LocalTransactionCoordinator.EndModeCommit); // d171555.3.2

                    if (TraceComponent.isAnyTracingEnabled() && // d527372
                        TETxLifeCycleInfo.isTraceEnabled()) // 171555.3.2
                    {
                        TETxLifeCycleInfo.traceLocalTxCommit("" + System.identityHashCode(lCoord), "Commit Local Tx - end");
                    }
                } catch (Exception ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".commit", "277", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Local tx completion failed", ex);
                    }
                    // By default - include cause.               d616849 d672063
                    if (ExcludeNestedExceptions) {
                        ex = null;
                    }
                    throw new CSITransactionRolledbackException("", ex);
                }

            } // end else (ie. sticky local tx not required

        } // end if local tx exists

        // There was no local tx so a global tx must exist.

        else {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Committing TX cntxt: " + txCtrl.txService.getTransaction()); //LIDB1673.2.1.5
                }
                txCtrl.txService.commit(); //LIDB1673.2.1.5

                // d165585 Begins
                if (TraceComponent.isAnyTracingEnabled() && // d527372
                    TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
                {
                    TETxLifeCycleInfo.traceGlobalTxCommit("NoTx", "Commit Global Tx");
                }
            }
            // d165585 Ends
            //445903 start: the call to commit above may throw a HeuristicMixedException.  If we simply catch
            //'Exception' (as in the second catch block below), the HeuristicMixedException is lost/eaten by our
            //Remote or Local mapping strategies.  To make sure the user sees the HeuristicMixedException, we must
            //look for (catch) it and nest it in the CSI exception.
            catch (javax.transaction.HeuristicMixedException hme) {
                FFDCFilter.processException(hme, CLASS_NAME + ".commit", "856", this);
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Global tx commit failed Heuristically", hme);
                }
                throw new CSITransactionRolledbackException("", hme);
            }
            //445903 end
            catch (Exception ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".commit", "294", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Global tx commit failed", ex);
                }
                // By default - include cause.                  d616849 d672063
                if (ExcludeNestedExceptions) {
                    ex = null;
                }
                throw new CSITransactionRolledbackException("", ex);
            }
        }
    }

    // d121667
    // d123423
    /*
     * If the current local or global transaction was started by the
     * Container, roll it back now. Otherwise, we may have a global
     * tx that was started by a client. In this case we only mark
     * it for rollback. This method is only called when a bean
     * method threw an exception.
     * 
     * Beginning with R5.0 (d123423), local transactions may just be
     * suspended at postInvoke time. This is to support sticky
     * local TXs for ActivitySessions.
     * 
     * Note that rollback must be done through internal TX service
     * interfaces, not the UserTransaction interface which an
     * application would use.
     */
    final void rollback(boolean beginner, EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        LocalTransactionCoordinator lCoord = ltcCurrent.getLocalTranCoord();

        // If local tx exists; process it.  There must not be a global tx.
        if (lCoord != null)
        {
            if (lCoord.isASScoped()) // d130989
            {
                endASLocalTx(key, methodInfo, lCoord, true); // RTC103197
            }
            // Otherwise, sticky local tx not required so end local tx now.
            else
            {
                try
                {
                    // d171555.3.2
                    //                     int resolver = (((EJBComponentMetaData)methodInfo.getComponentMetaData()).getLocalTranConfigData()).getValueResolver();
                    //
                    //                     // Is this an EJB 2.0 or newer component && the container is responsible for begin / end
                    //                     // of connection specific local transactions (ie. RMLTs)?  For EJB 1.1 beans local
                    //                     // tx resolver is never set to ContainerAtBoundary.  This is enforced by a
                    //                     // check in BeanMetaData.
                    //
                    //                     if (resolver == LocalTranConfigData.RESOLVER_CONTAINER_AT_BOUNDARY) {    //d135218
                    //
                    //                        // Complete the LTC context so enlisted resources will be rolled back.
                    //                        // When Container is responsible to automatically begin / end connections,
                    //                        // (i.e. local trancation resolver = containerAtBoundary) the connection
                    //                        // manager enlists each connection as a resource in the local transaction
                    //                        // context.  The enlist causes LocalTransaction.begin to occur on the
                    //                        // connection.  Here we are "completing" the context which causes
                    //                        // LocalTransaction.rollback to occur for the enlisted connections.
                    //
                    //                        if (isTraceOn && tc.isEventEnabled()) {
                    //                           Tr.event(tc, "Completing LTC cntxt with rollback due to bean " +
                    //                                    "exception: " + System.identityHashCode(lCoord));
                    //                        }
                    //                        lCoord.complete(LocalTransactionCoordinator.EndModeRollBack);
                    //
                    //                        // d165585 Begins
                    //                        if( TETxLifeCycleInfo.isTraceEnabled() )              // PQ74774
                    //                        {
                    //                            if( lCoord != null )
                    //                            {
                    //                                TETxLifeCycleInfo.traceLocalTxRollback( "" + System.identityHashCode( lCoord ), "Rollback Local Tx - complete" );
                    //                            }
                    //                        }
                    //                        // d165585 Ends
                    //                     } else {
                    //
                    //                        // Cleanup the LTC context so resources enlisted for cleanup will catch any
                    //                        // application errors.  When resolver = application, the application code is
                    //                        // responsible to do begin / end of the connections.  In this case the
                    //                        // connection manager enlists the connections for cleanup in the local
                    //                        // transaction context.  They are delisted iff the application remembers to
                    //                        // end them (ie. commit or rollback).  Here we end the context with
                    //                        // "cleanup" and "force" rollback of any remaining connections.  Rollback
                    //                        // of any remaining connections will occur despite the configuration
                    //                        // setting for unresolved-action (ie. COMMIT or ROLLBACK) because we first
                    //                        // call setRollbackOnly.
                    //
                    //                        if (isTraceOn && tc.isEventEnabled()) {
                    //                            Tr.event(tc, "Ending LTC cntxt with rollback due to bean " +
                    //                                    "exception: " + System.identityHashCode(lCoord));
                    //                        }
                    //                        lCoord.setRollbackOnly();
                    //                        lCoord.cleanup();
                    //
                    //                        // d165585 Begins
                    //                        if( TETxLifeCycleInfo.isTraceEnabled() )              // PQ74774
                    //                        {
                    //                            if( lCoord != null )
                    //                            {
                    //                                TETxLifeCycleInfo.traceLocalTxRollback( "" + System.identityHashCode( lCoord ), "Rollback Local Tx - cleanup" );
                    //                            }
                    //                        }
                    //                        // d165585 Ends
                    //                     } // end else
                    if (isTraceOn && tc.isEventEnabled())
                    {
                        Tr.event(tc, "Completing LTC cntxt with rollback due to bean " +
                                     "exception: tid=" +
                                     Integer.toHexString(lCoord.hashCode()) + "(LTC)");
                    }

                    lCoord.setRollbackOnly();
                    lCoord.end(LocalTransactionCoordinator.EndModeRollBack); // d171555.3.2

                    if (isTraceOn && // d527372
                        TETxLifeCycleInfo.isTraceEnabled()) // d171555.3.2
                    {
                        TETxLifeCycleInfo.traceLocalTxRollback("" + System.identityHashCode(lCoord), "Rollback Local Tx - end");
                    }
                } catch (RolledbackException ex)
                {
                    // do nothing... this is normal on setRollback, followed by cleanup
                    FFDCFilter.processException(ex, CLASS_NAME + ".rollback", "375", this);
                } catch (UOWCompensatedException ex)
                {
                    // UOWCompensatedException is only thrown to insure some
                    // exception is reported when setCompensateOnly has been
                    // called. However, if the method has already reported an
                    // exception, then the UOWCEx should not override it.
                    // Since the compensation code cannot currently tell when
                    // the method reported an exception, it will always throw
                    // the exception, and it will be handled here.           dPM45328
                    EJSDeployedSupport s = EJSContainer.getMethodContext();
                    if (s.getException() == null)
                    {
                        FFDCFilter.processException(ex, CLASS_NAME + ".rollback", "1129", this);
                        String errStr = "Could not complete local tx";
                        if (isTraceOn && tc.isEventEnabled()) {
                            Tr.event(tc, errStr, ex);
                        }
                        throw new CSIException(errStr, ex);
                    }
                    else
                    {
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Ignoring UOWCompensatedException; " +
                                         "another exception already reported : " +
                                         s.getException(), ex);
                        }
                    }
                } catch (Exception ex)
                {
                    FFDCFilter.processException(ex, CLASS_NAME + ".rollback", "378", this);
                    String errStr = "Could not complete local tx";
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, errStr, ex);
                    }
                    throw new CSIException(errStr, ex);
                }
            } // end else (not a sticky local tx)
        } // end if local tx

        // There was no local tx so a global tx must exist
        else
        {
            try
            {
                if (beginner)
                {
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Rolling back TX cntxt due to bean " +
                                     "exception: " + txCtrl.txService.getTransaction()); //LIDB1673.2.1.5
                    }
                    txCtrl.txService.rollback();

                    // d165585 Begins
                    if (isTraceOn && // d527372
                        TETxLifeCycleInfo.isTraceEnabled()) // PQ74774
                    {
                        TETxLifeCycleInfo.traceGlobalTxRollback("NoTx", "Rollback Global Tx");
                    }
                    // d165585 Ends
                }
                else
                {
                    if (isTraceOn && tc.isEventEnabled()) {
                        Tr.event(tc, "Marking TX cntxt for rollback due to bean " +
                                     "exception: " + txCtrl.txService.getTransaction()); //LIDB1673.2.1.5
                    }
                    txCtrl.txService.setRollbackOnly(); //LIDB1673.2.1.5
                }
            } catch (Exception ex)
            {
                FFDCFilter.processException(ex, CLASS_NAME + ".rollback", "405", this);
                String errStr = "Could not roll back global tx";
                if (isTraceOn && tc.isEventEnabled()) {
                    Tr.event(tc, errStr, ex);
                }
                throw new CSIException(errStr, ex);
            }
        }
    } // rollback

    /**
     * Return boolean true if there is an active BeanManaged Transaction
     * currently associated with the calling thread.
     */
    //167937 - added entire method
    boolean isBmtActive()
    {
        return false;
    }

} // TranStrategy
