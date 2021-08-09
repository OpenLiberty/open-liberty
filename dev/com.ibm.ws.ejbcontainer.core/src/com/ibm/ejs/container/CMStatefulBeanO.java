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
package com.ibm.ejs.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.ejb.SessionSynchronization;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A <code>CMStatefulBeanO</code> manages the lifecycle of a
 * single stateful session enterprise bean instance with container-managed
 * transactions and provides the session context implementation for the
 * session bean. <p>
 */

public class CMStatefulBeanO
                extends StatefulBeanO
{
    private static final String CLASS_NAME = CMStatefulBeanO.class.getName();
    private static final TraceComponent tc =
                    Tr.register(CMStatefulBeanO.class, "EJBContainer",
                                "com.ibm.ejs.container.container");//121558

    /**
     * Non-null iff associated sessionBean implements the
     * SessionSynchronization interface
     */
    private SessionSynchronization sessionSync = null;

    /**
     * Stateful AfterBegin session synchronization method when bean does
     * not implement the SessionSynchronization interface.
     */
    // F743-25855
    public Method ivAfterBegin = null;

    /**
     * Stateful BeforeCompletion session synchronization method when bean does
     * not implement the SessionSynchronization interface.
     */
    // F743-25855
    public Method ivBeforeCompletion = null;

    /**
     * Stateful AfterCompletion session synchronization method when bean does
     * not implement the SessionSynchronization interface.
     */
    // F743-25855
    public Method ivAfterCompletion = null;

    /**
     * Create new <code>CMStatefulBeanO</code>. <p>
     */
    // d367572 - changed signature and rewrote entire method.
    public CMStatefulBeanO(EJSContainer c, EJSHome h)
    {
        super(c, h);

        // Copied here for runtime performance                          F743-25855
        ivAfterBegin = h.beanMetaData.ivAfterBegin;
        ivBeforeCompletion = h.beanMetaData.ivBeforeCompletion;
        ivAfterCompletion = h.beanMetaData.ivAfterCompletion;
    } // CMStatefulBeanO

    @Override
    public void setEnterpriseBean(Object bean)
    {
        super.setEnterpriseBean(bean);
        if (bean instanceof SessionSynchronization) {
            sessionSync = (SessionSynchronization) bean;
        }
    }

    @Override
    public boolean enlist(ContainerTx tx) // F61004.1
    throws RemoteException
    {
        return enlist(tx, true);
    }

    /**
     * Enlist this <code>SessionBeanO</code> instance in the
     * given transaction. <p>
     *
     * @param tx the <code>ContainerTx</code> this instance is being
     *            enlisted in <p>
     */
    @Override
    public final synchronized boolean enlist(ContainerTx tx, boolean txEnlist) // d114677
    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "enlist: " + getStateName(state), new Object[] { tx, txEnlist });

        switch (state)
        {
            case METHOD_READY:
                // A thread is about to invoke a method in the context
                // of the given transaction

                if (currentTx != null) {
                    throw new BeanNotReentrantException(getStateName(state) + ": Tx != null"); //PQ99986
                }

                if (ivExPcContext != null && tx.isTransactionGlobal()) {
                    container.getEJBRuntime().getEJBJPAContainer().onEnlistCMT(ivExPcContext);
                }

                currentTx = tx;
                final boolean result = !txEnlist || tx.enlist(this); // F61004.1

                // Session Synchronization AfterBegin needs to be called if either
                // the annotation (or XML) was specified or the bean implemented
                // the interface and a global transaction is active.        F743-25855
                if ((ivAfterBegin != null || sessionSync != null) &&
                    tx.isTransactionGlobal())
                {
                    setState(AFTER_BEGIN); // d159152
                    if (ivAfterBegin != null) {
                        invokeSessionSynchMethod(ivAfterBegin, null);
                    } else {
                        sessionSync.afterBegin();
                    }
                }

                setState(TX_METHOD_READY);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: " + result + ": " + getStateName(state));

                return result;

            case TX_METHOD_READY:
                // A thread is about to invoke a method in the context of
                // the given transaction; it must be the same transaction
                // which we're already associated with

                if (currentTx != tx) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "enlist: not re-entrant");
                    throw new BeanNotReentrantException(StateStrs[state] +
                                                        ": wrong transaction"); // d116807
                }

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: false");
                return false;

            case COMMITTING_OUTSIDE_METHOD:
                // Either another thread is currently committing this bean (PQ52534),
                // or current thread is re-enlisting this bean after beforeCompletion
                // has been driven on it (i.e. another bean has called a method
                // on this bean during beforeCompletion processing) (d115597).

                if (currentTx != tx) { // PQ52534
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "enlist: not re-entrant");
                    throw new BeanNotReentrantException(StateStrs[state] +
                                                        ": wrong transaction"); // d116807
                }

                // Although already enlisted, need to call enlist again to make sure
                // it gets added to the end of the list, and processed again.  d115597
                boolean result1 = tx.enlist(this);
                setState(TX_METHOD_READY);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "enlist: " + result1 + ": " + getStateName(state));
                return result1;

            case TX_IN_METHOD:
            case REMOVING: // d159152
            case AFTER_BEGIN: // d159152
            case AFTER_COMPLETION: // d159152
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

            default:
                // Any other state is a programming error... there should be no
                // way to encounter these states even from another thread/transaction.
                throw new InvalidBeanOStateException(getStateName(state),
                                "METHOD_READY | TX_METHOD_READY");

        }
    } // enlist

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
            Tr.entry(tc, "commit: " + getStateName(state), tx);

        if (removed) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "commit: removed");
            return;
        }

        if (tx.ivRemoveBeanO == this)
        {
            completeRemoveMethod(tx); // d647928
        }
        else
        {
            // Perform normal afterCompletion processing.... calling
            // afterCompletion if present.
            setState(COMMITTING_OUTSIDE_METHOD, AFTER_COMPLETION); // d159152

            // Session Synchronization AfterCompletion needs to be called if either
            // the annotation (or XML) was specified or the bean implemented
            // the interface and a global transaction is active.        F743-25855
            if ((ivAfterCompletion != null || sessionSync != null) &&
                tx.isTransactionGlobal())
            {
                EJBThreadData threadData = EJSContainer.getThreadData();

                threadData.pushContexts(this);
                try
                {
                    if (ivAfterCompletion != null) {
                        invokeSessionSynchMethod(ivAfterCompletion, new Object[] { true });
                    } else {
                        sessionSync.afterCompletion(true);
                    }
                } finally
                {
                    threadData.popContexts();
                }
            }

            setState(AFTER_COMPLETION, METHOD_READY); // d159152
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
            Tr.entry(tc, "rollback: " + StateStrs[state], tx);

        if (removed) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "rollback: removed");
            return;
        }

        switch (state)
        {
        // These are the "normal" rollback points
            case COMMITTING_OUTSIDE_METHOD:
            case TX_METHOD_READY:
            case AFTER_BEGIN: // d159152

                setState(METHOD_READY);
                break;

            // We also could be rolledback "unexpectedly" at these points
            case TX_IN_METHOD:
            case REMOVING: // d159152

                setState(IN_METHOD);
                break;

            // Rollback in the DESTROYED state can/should only occur if there
            // was an exception from the customer beforeCompletion method.
            // Otherwise, the bean should have been delisted.                d160910
            case DESTROYED:
                if (ivBeforeCompletion != null || sessionSync != null)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "SessionSynchronization.beforeCompletion exception?");
                }
                else
                {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "rollback: InvalidBeanOStateException: " +
                                    StateStrs[state]);
                    throw new InvalidBeanOStateException(StateStrs[state],
                                    "COMMITTING_OUTSIDE_METHOD | " +
                                                    "TX_METHOD_READY | TX_IN_METHOD");
                }
                break;

            default:
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "rollback: InvalidBeanOStateException: " +
                                StateStrs[state]);
                throw new InvalidBeanOStateException(StateStrs[state],
                                "COMMITTING_OUTSIDE_METHOD | " +
                                                "TX_METHOD_READY | TX_IN_METHOD");

        }

        if (tx.ivRemoveBeanO == this)
        {
            completeRemoveMethod(tx); // d647928
        }
        else
        {
            // Perform normal afterCompletion processing.... calling
            // afterCompletion if present.
            // Save the above determined state and set to AFTER_COMPLETION for
            // the duration of the afterCompletion callback.                d159152
            int savedState = state;
            setState(AFTER_COMPLETION);

            // Session Synchronization AfterCompletion needs to be called if either
            // the annotation (or XML) was specified or the bean implemented
            // the interface and a global transaction is active.        F743-25855
            if ((ivAfterCompletion != null || sessionSync != null) &&
                tx.isTransactionGlobal())
            {
                EJBThreadData threadData = EJSContainer.getThreadData();

                threadData.pushContexts(this);
                try
                {
                    if (ivAfterCompletion != null) {
                        invokeSessionSynchMethod(ivAfterCompletion, new Object[] { false });
                    } else {
                        sessionSync.afterCompletion(false);
                    }
                } finally
                {
                    threadData.popContexts();
                }
            }

            setState(savedState); // d159152
        }

        // currentTx is now reset in atCommit/atRollback AFTER the final pin
        // is undone.                                                      d258770
        // currentTx = null;                                            // PQ99986

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "rollback: " + StateStrs[state]);
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
            Tr.entry(tc, "beforeCompletion: " + StateStrs[state]);

        if (removed) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "beforeCompletion: removed");
            return;
        }

        switch (state)
        {
        // These are the "normal" commit points
            case TX_METHOD_READY:

                setState(COMMITTING_OUTSIDE_METHOD);

                // Session Synchronization BeforeCompletion needs to be called if either
                // the annotation (or XML) was specified or the bean implemented
                // the interface and a global transaction is active.        F743-25855
                if (ivBeforeCompletion != null || sessionSync != null)
                {
                    ContainerTx tx = container.getActiveContainerTx();

                    // SessionSynchronization beforeCompletion is only called for
                    // global transactions that are NOT being rolledback... and
                    // it is skipped if a remove method (@Remove) was called.    390657
                    if ((tx != null) &&
                        (tx.isTransactionGlobal()) &&
                        (!tx.getRollbackOnly()) &&
                        (tx.ivRemoveBeanO != this)) // 390657
                    {
                        EJBThreadData threadData = EJSContainer.getThreadData();

                        threadData.pushContexts(this);
                        try
                        {
                            if (ivBeforeCompletion != null) {
                                invokeSessionSynchMethod(ivBeforeCompletion, null);
                            } else {
                                sessionSync.beforeCompletion();
                            }
                        } finally
                        {
                            threadData.popContexts();
                        }
                    }
                }

                break;

            // Could happen during async rollback (i.e. a second thread is
            // rolling back the tran, as the current thread is committing it.
            case IN_METHOD:
            case METHOD_READY:
            case AFTER_COMPLETION: // d159152
            case DESTROYED:
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "beforeCompletion: asynch rollback: " + getStateName(state));
                return;

            default:

                throw new InvalidBeanOStateException(getStateName(state),
                                "TX_METHOD_READY | TX_IN_METHOD | " +
                                                "METHOD_READY | DESTROYED");

        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "beforeCompletion: " + getStateName(state));
    } // beforeCompletion

    /**
     * getUserTransaction - override the implementation in SessionBeanO
     * and throw an exception. This method call is invalid in a container
     * managed transaction
     */
    @Override
    public synchronized UserTransaction getUserTransaction()
    {
        throw new IllegalStateException("UserTransaction not allowed for Stateful with container managed transactions.");
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
        // For container managed stateful beans, the only states where the
        // bean is eligible for use on another thread are METHOD_READY,
        // TX_METHOD_READY and DESTROYED.
        //
        // When the bean is being created, no states are valid, as other
        // threads cannot even be aware of the bean. While in a method,
        // no other thread can be waiting that is in the same transaction
        // or on the same thread. When the bean is finally transitioned
        // out of a method, it must be placed in TX_METHOD_READY,
        // METHOD_READY (Once) or DESTROYED (Tran)... and will
        // only be available at that time.
        //
        // Previously, TX_METHOD_READY was not included. However, it is
        // required, as remote method calls in the same transaction may
        // occur on different threads.                                     d704504
        return (state == METHOD_READY ||
                state == TX_METHOD_READY || state == DESTROYED);
    }

    /**
     * Utility method to encapsulate all of the exception handling needed
     * when invoking the Session Synchronization methods through reflections. <p>
     *
     * To be consistent with calling the methods through the interface,
     * if an exception is thrown from the method, it will be extracted
     * from the InvocationTargetException and re-thrown from this method.
     * The spec is silent on what may may be on the throws clause, so WAS
     * is limiting it to RuntimeExceptions. <p>
     *
     * @param method session synchronization method to invoke
     * @param parameters optional parameters, if required by the method
     */
    // F743-25855
    private void invokeSessionSynchMethod(Method method, Object[] parameters)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
        {
            StringBuilder traceMsg = new StringBuilder("invoking session synchronization method : ");
            traceMsg.append(method.getName());
            if (parameters != null && parameters.length > 0) {
                traceMsg.append("(").append(parameters[0]).append(")");
            }
            Tr.debug(tc, traceMsg.toString());
        }

        try
        {
            method.invoke(ivEjbInstance, parameters);
        } catch (IllegalArgumentException ex)
        {
            // This should never happen... log useful info and re-throw
            FFDCFilter.processException(ex, CLASS_NAME + ".invokeSessionSynchMethod",
                                        "601", new Object[] { this, method,
                                                             ivEjbInstance, parameters });
            throw ex;
        } catch (IllegalAccessException ex)
        {
            // This should never happen... log useful info and re-throw
            FFDCFilter.processException(ex, CLASS_NAME + ".invokeSessionSynchMethod",
                                        "610", new Object[] { this, method,
                                                             ivEjbInstance, parameters });
            throw ExceptionUtil.EJBException
                            ("Failure invoking session synchronization method " + method, ex);
        } catch (InvocationTargetException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".invokeSessionSynchMethod",
                                        "618", new Object[] { this, method,
                                                             ivEjbInstance, parameters });
            // To make this consistent with the interface, the cause will be
            // extracted and re-thrown; which should be a RuntimeException.
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw ExceptionUtil.EJBException
                            ("Failure invoking session synchronization method " + method, ex);
        }
    }
}
