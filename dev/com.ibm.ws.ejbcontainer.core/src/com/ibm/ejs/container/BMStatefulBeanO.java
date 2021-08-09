/*******************************************************************************
 * Copyright (c) 1998, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;

import com.ibm.ejs.container.CallbackContextHelper.Tx;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A <code>BMStatefulBeanO</code> manages the lifecycle of a
 * single stateful session enterprise bean instance with bean-managed
 * transactions and provides the session context implementation for
 * the session bean. <p>
 */

public class BMStatefulBeanO extends StatefulBeanO {

    private static final TraceComponent tc = Tr.register(BMStatefulBeanO.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Create new <code>BMStatefulBeanO</code>. <p>
     */
    // d367572 - changed signature and rewrote entire method.
    public BMStatefulBeanO(EJSContainer c, EJSHome h)
    {
        super(c, h);
    } // BMStatefulBeanO

    /**
     * Enlist this <code>SessionBeanO</code> instance in the
     * given transaction. <p>
     *
     * @param tx the <code>ContainerTx</code> this instance is being
     *            enlisted in <p>
     */
    @Override
    public final synchronized boolean enlist(ContainerTx tx) // d114677
    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "enlist: " + getStateName(state),
                     new Object[] { tx, currentTx }); //123293

        boolean result = false;

        switch (state)
        {
            case METHOD_READY:

                // The only way we could be enlisted in this state is at
                // activation; it is not possible for tx to be a global
                // transaction; we do not care about enlistments for
                // non-global transactions

                // LIDB1181.23.5.1
                // A new possibility exists now for the create case of a BMT bean.
                // The home of this BMT session bean will also be configured as
                // a BMT bean too.  This means that during the Home create method
                // the container will have put a local transaction context (LTC)
                // on the thread.  During postCreate the BMStatefulBeanO enlist
                // will be invoked and a containerTx will already exist
                // (ie. passed in ContainerTx is not null any more)

                if (currentTx != null) {
                    if (isTraceOn && tc.isEntryEnabled()) // d144064
                        Tr.exit(tc, "enlist: unexpected non-null currentTx"); //123293
                    throw new BeanNotReentrantException(getStateName(state) +
                                                        ": enlist: unexpected non-null currentTx"); //123293  //PQ99986
                }

                // Note, for BM stateful bean, the transaction may have
                // already been rolled back as a result of a prior business
                // method being called and an unchecked exception occurring.
                // when that happens, the next business method call will
                // eventually get to this code with tx set to null.
                // for that case, we simply skip doing the enlist and
                // return boolean false to caller so that the business
                // method gets dispatched.  Needed to pass CTS testcase
                // which is checking for the TransactionRolledBackException
                // occurring when client tries to commit a UserTransaction
                // that was already rolled back as a result of the
                // unchecked exception that previously occurred.
                if (tx != null) //123393
                {
                    result = tx.enlist(this);
                    currentTx = tx;
                    setState(TX_METHOD_READY);
                }

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: " + result + ": " + getStateName(state));

                return result;

            case TX_METHOD_READY:

                // The only way we could be enlisted in this state is at
                // activation; tx must be the same as currentTx, since
                // TransactionControl should have resumed our transaction
                if (tx != currentTx)
                {
                    if (isTraceOn && tc.isEntryEnabled()) // d144064
                        Tr.exit(tc, "enlist: unexpected non-null currentTx"); // 123293
                    String expected = "METHOD_READY"; // 123293
                    if (currentTx == null) // 123293
                    {
                        // This should never happened. Resume did not occur?
                        expected = "MAYBE_RESUME_DID_NOT_OCCUR"; // 123293
                    }
                    throw new InvalidBeanOStateException(getStateName(state)
                                    , expected); // 123293
                }

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: false");

                return false;

            case IN_METHOD:

                // We've got two possibilities: either the bean has just begun
                // a transaction or we're attempting to activate for a re-entrant
                // call from a different thread (which is an error); in the case of
                // a re-entrant call, we must catch it here (cannot defer to
                // preinvoke) since the other thread may change the state of the
                // bean before preInvoke would be called in this thread.

                // LIDB1181.23.5.1
                // A new possibility exists now for the create case of a BMT bean.
                // The home of this BMT session bean will also be configured as
                // a BMT bean too.  This means that during the Home create method
                // the container will have put a local transaction context (LTC)
                // on the thread.  During postCreate the BMStatefulBeanO enlist
                // will be invoked and a containerTx will already exist
                // (ie. passed in ContainerTx is not null any more)

                if (currentTx != null)
                {
                    if (isTraceOn && tc.isEntryEnabled()) // d144064
                        Tr.exit(tc, "enlist: unexpected non-null currentTx"); //123293
                    throw new InvalidBeanOStateException(getStateName(state)
                                    , "TX_METHOD_READY"); //123293
                }

                if (tx == null) // d149781
                {
                    // The only way for the bean to be in the IN_METHOD state with
                    // no transaction is if a different thread is already using it.
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "enlist: reentrant call from different thread");
                    throw new BeanNotReentrantException(StateStrs[state] +
                                                        ": wrong thread");
                }

                result = tx.enlist(this);
                currentTx = tx;

                setState(TX_IN_METHOD);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: " + result + ": " + getStateName(state));

                return result;

            case CREATING: // PQ56091
            case ACTIVATING: // d159252
            case PASSIVATING: // d159152

                // For all of these states, the EJB Specification allows the use of
                // UserTransaction methods.  However, it is not appropriate to
                // enlist this bean at this time (enlist will be called later
                // for the CREATING and ACTIVATING scenarios).

                if (currentTx != null) {
                    if (isTraceOn && tc.isEntryEnabled()) // d144064
                        Tr.exit(tc, "enlist: unexpected non-null currentTx"); //1233293
                    throw new InvalidBeanOStateException(getStateName(state)
                                    , "TX_METHOD_READY"); //123293
                }
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: false - unexpected state!");
                return false;

            case TX_IN_METHOD:
            case COMMITTING_IN_METHOD:
            case COMMITTING_OUTSIDE_METHOD:

                // This bean is already in a transaction - and any of these states
                // could be seen by a different transaction calling a method on it,
                // or they could also be due to a customer coded callback to the same
                // bean.
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: not re-entrant");
                if (currentTx != tx)
                    throw new BeanNotReentrantException(StateStrs[state] +
                                                        ": wrong transaction");

                // This isn't a different thread/tran, but it is a reentrant call,
                // which must be caught here rather than preInvoke to insure the
                // extra cache pins are undone in the activation strategy.     d159152
                throw new BeanNotReentrantException(getStateName(state));

            case REMOVING:

                // Note: prior to the access timeout support added in EJB 3.1, there
                // were issues for the REMOVING state where multiple threads might
                // attempt to enter the bean concurrently, but now that the bean is
                // properly locked, it is safe to just not enlist the bean and thus
                // support UserTransaction methods in ejbRemove/PreDestroy per spec,
                // though if there is a transaction then verify this is the EJB 3.x
                // remove scenario.

                if (currentTx != null && currentTx.ivRemoveBeanO != this) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "enlist: unexpected non-null currentTx");
                    throw new InvalidBeanOStateException(getStateName(state), "TX_METHOD_READY");
                }
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: false - not enlisted for REMOVING state");
                return false;

            default:
                // Any other state is a programming error... there should be no
                // way to encounter these states even from another thread/transaction.
                if (isTraceOn && tc.isEntryEnabled()) // d144064
                    Tr.exit(tc, "enlist: invalid state: " + getStateName(state)); //123293
                throw new InvalidBeanOStateException(getStateName(state),
                                "METHOD_READY | " +
                                                "TX_METHOD_READY | " +
                                                "IN_METHOD");

        }

    } // enlist

    /**
     * Inform this <code>SessionBeanO</code> that a method
     * invocation has completed on its associated enterprise bean. <p>
     *
     * @param id an <code>int</code> indicating which method is being
     *            invoked on this <code>BeanO</code> <p>
     *
     * @param s the <code>EJSDeployedSupport</code> instance passed to
     *            both pre and postInvoke <p>
     */
    //LIDB2018-1 - added method.
    @Override
    public synchronized void postInvoke(int id, EJSDeployedSupport s) throws RemoteException
    {
        // Do the normal postinvoke processing first.
        super.postInvoke(id, s);

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "postInvoke");
        }

        // Now do failover processing if necessary. That is,
        // if we have a sticky BMT with SFSB failover enabled,
        // then create failover property to indicate we have a sticky BMT.
        // If sticky BMT had completed, then remove failover property.
        // This is done so that we can throw an exception if failover
        // is attempted before sticky BMT completes.

        //d204278.2 - remove try/catch block.
        if (ivSfFailoverClient != null)
        {
            if (ivContainerTx == null)
            {
                ivContainerTx = container.getCurrentContainerTx();
            }

            // Determine if where to do sticky BMT checking or
            // sticky BMAS checking (sticky BM ActivitySession).
            ContainerAS containerAS = ivContainerTx.getContainerAS();
            if (containerAS == null)
            {
                // There is no ActivitySession, so check for
                // a sticky BMT.
                if (ivContainerTx.isBmtActive(s.methodInfo))
                {
                    ivSfFailoverClient.stickyUOW(beanId, true);
                }
                else
                {
                    ivSfFailoverClient.stickyUOW(beanId, false);
                }
            }
            else
            {
                // There is an ActivitySession, so check for
                // a sticky BMAS.
                if (containerAS.isBmasActive(s.methodInfo))
                {
                    ivSfFailoverClient.stickyUOW(beanId, true);
                }
                else
                {
                    ivSfFailoverClient.stickyUOW(beanId, false);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "postInvoke");
        }
    }

    /**
     * Inform this <code>SessionBeanO</code> that the transaction
     * it was enlisted with has committed. <p>
     */
    @Override
    public final synchronized void commit(ContainerTx tx)
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "commit: " + getStateName(state),
                     new Object[] { tx, currentTx }); //123293

        if (removed) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "commit: removed");
            return;
        }

        switch (state)
        {
            case COMMITTING_OUTSIDE_METHOD:
                setState(METHOD_READY);
                break;

            case COMMITTING_IN_METHOD:
                setState(IN_METHOD);
                break;

            default:
                if (isTraceOn && tc.isDebugEnabled()) // d144064
                    Tr.debug(tc, "invalid state: " + state); //123293
                throw new InvalidBeanOStateException(StateStrs[state],
                                "COMMITTING_IN_METHOD | " +
                                                "COMMITTING_OUTSIDE_METHOD");
        }

        if (tx.ivRemoveBeanO == this)
        {
            completeRemoveMethod(tx); // d647928
        }

        // currentTx is now reset in atCommit/atRollback AFTER the final pin
        // is undone.                                                      d258770
        // currentTx = null;                                            // PQ99986

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "commit: " + getStateName(state));
    } // commit

    /**
     * Inform this <code>SessionBeanO</code> that the transaction
     * it was enlisted with has rolled back. <p>
     */
    @Override
    public final synchronized void rollback(ContainerTx tx)
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "rollback: " + getStateName(state),
                     new Object[] { tx, currentTx }); //123293

        if (removed) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "rollback: removed");
            return;
        }

        switch (state) {
            case TX_METHOD_READY:
            case COMMITTING_OUTSIDE_METHOD:
                setState(METHOD_READY);
                break;

            case COMMITTING_IN_METHOD:
            case TX_IN_METHOD: // In case of an async rollback
            case REMOVING: // d159152
                setState(IN_METHOD);
                break;

            default:
                if (isTraceOn && tc.isDebugEnabled()) // d144064
                    Tr.debug(tc, "invalid state: " + state); //123293
                throw new InvalidBeanOStateException(StateStrs[state],
                                "TX_METHOD_READY | " +
                                                "COMMITTING_IN_METHOD | " +
                                                "COMMITTING_OUTSIDE_METHOD");

        }

        if (tx.ivRemoveBeanO == this)
        {
            completeRemoveMethod(tx); // d647928
        }

        // currentTx is now reset in atCommit/atRollback AFTER the final pin
        // is undone.                                                      d258770
        // currentTx = null;                                            // PQ99986

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "rollback: " + getStateName(state));
    } // rollback

    /**
     * Ensure that this <code>StatefulBeanO</code> is prepared
     * for transaction completion. <p>
     */
    @Override
    public final synchronized void beforeCompletion()
                    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "beforeCompletion: " + getStateName(state), currentTx); //123293

        if (removed) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "beforeCompletion: removed");
            return;
        }

        switch (state)
        {
            case TX_METHOD_READY:
                setState(COMMITTING_OUTSIDE_METHOD);
                break;

            case TX_IN_METHOD:
                setState(COMMITTING_IN_METHOD);
                break;

            // Could happen during async rollback
            case IN_METHOD:
            case METHOD_READY:
            case DESTROYED:
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "beforeCompletion: asynch rollback: " + getStateName(state));
                return;

            default:
                if (isTraceOn && tc.isDebugEnabled()) // d144064
                    Tr.debug(tc, "invalid state: " + state); //123293
                throw new InvalidBeanOStateException(StateStrs[state],
                                "TX_IN_METHOD | " +
                                                "TX_METHOD_READY");

        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "beforeCompletion: " + getStateName(state));

    } // beforeCompletion

    /**
     * setRollbackOnly - This method is illegal for bean managed stateful
     * session beans
     */
    @Override
    public void setRollbackOnly()
    {
        throw new IllegalStateException();
    }

    /**
     * getRollbackOnly - This method is illegal for bean managed stateful
     * session beans
     */
    @Override
    public boolean getRollbackOnly()
    {
        throw new IllegalStateException();
    }

    @Override
    protected void beginLifecycleCallback(int methodId,
                                          CallbackContextHelper contextHelper,
                                          CallbackContextHelper.Contexts pushContexts) throws CSIException {
        // The EJB specification requires UserTransaction methods work in the lifecycle
        // methods of bean managed transaction stateful beans, so the following special
        // processing is required:

        // When ejbPassivate/PrePassivate and ejbActivate/PostActivate are called,
        // there will not be a transaction active (global or local), so regardless
        // of the module version, always begin an LTC. The CompatLTC setting would
        // normally be used, but then an LTC would not be started for 2.x modules,
        // and so UserTransaction methods would not work.
        if (methodId == LifecycleInterceptorWrapper.MID_PRE_PASSIVATE ||
            methodId == LifecycleInterceptorWrapper.MID_POST_ACTIVATE) {
            contextHelper.begin(Tx.LTC, pushContexts);
        }

        // For EJB 2.x beans, ejbRemove will be called in the context of an existing
        // LTC and the bean will be enlisted in it, so suspending that and starting
        // a new LTC will cause problems; the bean cannot be enlisted in two transactions
        // concurrently. EJB 3.x style beans with PreDestroy don't have this problem,
        // because any transaction that may have been active for the remove method would
        // have been completed by now. The EJB 2.x scenario could be fixed similarly,
        // but for compatibility purposes, we will instead just call ejbRemove in the
        // current LTC context as has been done in the past, regardless of module level.
        // EJB 2.x style remove calls will have currentTx set as may EJB 3.x, but
        // ivRemoveBeanO will be set for EJB 3.x, so that is what is used to determine
        // whether or not to avoid a new LTC.
        else if (methodId == LifecycleInterceptorWrapper.MID_PRE_DESTROY &&
                 currentTx != null && currentTx.ivRemoveBeanO != this) {
            contextHelper.begin(null, pushContexts);
        }

        // For all other scenarios, use the defaults for all stateful beans
        else {
            super.beginLifecycleCallback(methodId, contextHelper, pushContexts);
        }
    }

    /**
     * Used by {@link lock} to determine if the bean is in a state that
     * makes it eligible for use on the current thread, running with
     * the specified transaction. <p>
     *
     * Overridden to support concurrent access of stateful beans with
     * bean managed transactions when sticky global transactions are
     * involved. <p>
     *
     * @return true when the bean state makes it eligible for use on
     *         the current thread.
     */
    // d671368
    @Override
    boolean eligibleForLock(EJSDeployedSupport methodContext, ContainerTx tx) // d671368
    {
        // If the bean is enlisted in a global transaction, and was in a
        // method on a concurrent thread at the time this thread attempted
        // to call a method... then the current thread was unaware of the
        // global transaction and started a local transaction. For this
        // scenario, the local tran should be ignored when determining
        // if the bean is eligible to be locked. The local tran will be
        // 'completed' and the global tran resumed after the lock has
        // been acquired... during 'enlist'.
        if (state == TX_METHOD_READY &&
            currentTx != null && currentTx.isTransactionGlobal() &&
            !tx.isTransactionGlobal())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "eligibleForLock : ignoring local tran : " + tx);

            if (super.eligibleForLock(methodContext, currentTx)) // d671368.1 d704504
            {
                // Attempt to transition thread from local to sticky global tran.
                // Lock the bean only if successful.                       d671368.1
                return container.transitionToStickyGlobalTran(beanId, tx, currentTx);
            }

            return false; // d671368.1
        }

        // Not the sticky global tran scenario... so perform normal checking.
        return super.eligibleForLock(methodContext, tx); // d704504
    }

    /**
     * Used by {@link unlock} to determine if the bean is in a state that
     * makes it eligible for use on other threads. <p>
     *
     * @return true when the bean state makes it eligible for use on
     *         other threads.
     */
    // d650932
    @Override
    protected boolean eligibleForUnlock()
    {
        // For bean managed stateful beans, the only states where the
        // bean is eligible for use on another thread are METHOD_READY,
        // TX_METHOD_READY and DESTROYED.
        //
        // When the bean is being created, no states are valid, as other
        // threads cannot even be aware of the bean. While in a method,
        // no other thread can access the bean instance, as it is in use.
        // When the bean is finally transitioned out of a method, it must
        // be placed in TX_METHOD_READY, METHOD_READY (Once) or DESTROYED
        // (Tran)... and will only be available at that time.
        //
        // If the bean is still IN_METHOD, then UserTransaction.begin
        // has been called, and the bean is being transitioned from
        // one transaction to another... so don't release the bean
        // to other threads.                                            d648183
        //
        // Note that TX_METHOD_READY is different for bean managed, because
        // the transaction is 'sticky', and may be resumed under a different
        // thread.
        return (state == METHOD_READY ||
                state == TX_METHOD_READY || state == DESTROYED);
    }
}
