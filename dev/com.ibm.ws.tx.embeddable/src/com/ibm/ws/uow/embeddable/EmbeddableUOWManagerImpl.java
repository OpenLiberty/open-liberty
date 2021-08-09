/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.uow.embeddable;

import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.tx.jta.embeddable.impl.EmbeddableUserTransactionImpl;
import com.ibm.tx.jta.impl.UserTransactionImpl;
import com.ibm.tx.ltc.impl.LocalTranCoordImpl;
import com.ibm.tx.ltc.impl.LocalTranCurrentSet;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tx.jta.embeddable.UserTransactionController;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeCallbackManager;
import com.ibm.wsspi.uow.ExtendedUOWAction;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;

public class EmbeddableUOWManagerImpl implements UOWManager, UOWScopeCallback, UserTransactionController {
    private static final TraceComponent tc = Tr.register(EmbeddableUOWManagerImpl.class, TranConstants.TRACE_GROUP, null);

    protected UOWScopeCallbackManager _callbackManager;

    protected UOWCallbackManager _runUnderUOWCallbackManager;

    @Override
    public UOWToken suspend() throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "suspend");

        Transaction transaction = null;
        LocalTransactionCoordinator localTranCoord = null;

        // All of the currently active UOWs must be suspended
        // and a UOWToken created which will allow them to be
        // resumed.

        try {
            transaction = EmbeddableTransactionManagerFactory.getTransactionManager().suspend();
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.suspend", "109", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "Exception caught suspending transaction", e);

            final SystemException se = new SystemException(e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "suspend", se);
            throw se;
        }

        // An LTC and a global transaction cannot exist on the same
        // thread concurrently so only attempt an LTC suspend if
        // a global transaction wasn't suspended above.
        if (transaction == null) {
            localTranCoord = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent().suspend();
        }

        UOWToken uowToken = null;

        if (transaction != null || localTranCoord != null) {
            uowToken = new EmbeddableUOWTokenImpl(transaction, localTranCoord);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "suspend", uowToken);
        return uowToken;
    }

    @Override
    public UOWToken suspendAll() throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "suspendAll");

        // first suspend any tx, activitysessions
        // ... don't go any further if this throws any exceptions
        UOWToken uowToken = suspend();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "suspendAll", uowToken);
        return uowToken;
    }

    @Override
    public void resume(UOWToken uowToken) throws IllegalThreadStateException, IllegalArgumentException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resume", new Object[] { uowToken });

        if (uowToken != null && uowToken instanceof EmbeddableUOWTokenImpl) {
            final EmbeddableUOWTokenImpl uowTokenImpl = (EmbeddableUOWTokenImpl) uowToken;
            final Transaction transaction = uowTokenImpl.getTransaction();

            if (transaction != null) {
                try {
                    EmbeddableTransactionManagerFactory.getTransactionManager().resume(transaction);
                } catch (InvalidTransactionException ite) {
                    FFDCFilter.processException(ite, "com.ibm.ws.uow.UOWManagerImpl.resume", "212", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "InvalidTransactionException caught resuming transaction", ite);

                    final IllegalArgumentException iae = new IllegalArgumentException();
                    iae.initCause(ite);

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "resume", ite);
                    throw iae;
                } catch (IllegalStateException ise) {
                    FFDCFilter.processException(ise, "com.ibm.ws.uow.UOWManagerImpl.resume", "223", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "IllegalStateException caught resuming transaction", ise);

                    final IllegalThreadStateException itse = new IllegalThreadStateException();
                    itse.initCause(itse);

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "resume", itse);
                    throw itse;
                } catch (Exception e) {
                    FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.resume", "234", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Exception caught resuming transaction", e);

                    final SystemException se = new SystemException(e);

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "resume", se);
                    throw se;
                }
            }

            final LocalTransactionCoordinator localTranCoord = uowTokenImpl.getLocalTransactionCoordinator();

            if (localTranCoord != null) {
                try {
                    EmbeddableTransactionManagerFactory.getLocalTransactionCurrent().resume(localTranCoord);
                } catch (IllegalStateException ise) {
                    FFDCFilter.processException(ise, "com.ibm.ws.uow.UOWManagerImpl.resume", "254", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "IllegalStateException caught resuming LTC", ise);

                    // The LTC resume has failed. If we resumed an ActivitySession above it must
                    // be suspended here so that we leave the thread in the same state as it was
                    // prior to the resume call being made.
                    final IllegalThreadStateException itse = new IllegalThreadStateException();
                    itse.initCause(ise);

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "resume", itse);
                    throw itse;
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "resume");
    }

    @Override
    public void resumeAll(UOWToken uowToken) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resumeAll", new Object[] { uowToken });

        if (uowToken != null) {
            resume(uowToken);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "resumeAll");
    }

    @Override
    public synchronized void registerCallback(UOWScopeCallback callback) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerCallback", new Object[] { callback, this });

        if (_callbackManager == null) {
            _callbackManager = new UOWScopeCallbackManager();

            EmbeddableTransactionManagerFactory.getTransactionManager().registerCallback(this);
            EmbeddableTransactionManagerFactory.getLocalTransactionCurrent().registerCallback(this);
        }

        _callbackManager.addCallback(callback);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerCallback");
    }

    @Override
    public synchronized void registerRunUnderUOWCallback(UOWCallback callback) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "registerRunUnderUOWCallback", new Object[] { callback, this });

        if (_runUnderUOWCallbackManager == null) {
            _runUnderUOWCallbackManager = new UOWCallbackManager();
        }

        _runUnderUOWCallbackManager.addCallback(callback);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "registerRunUnderUOWCallback");
    }

    @Override
    public UOWScope getUOWScope() throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getUOWScope", this);

        // The UOWScope that is returned should be the active UOW that
        // is responsible for coordinating any enlisted resources.
        //
        // If there is a local transaction on the thread we must check
        // if it's enlisted with an ActivitySession as a resource. If
        // the local transaction is enlisted as a resource the
        // ActivitySession is coordinating the resources so it must be
        // returned, otherwise the local transaction is returned.
        //
        // In the absence of a local transaction any global transaction
        // that is on the thread will be returned.

        UOWScope uowScope = (UOWScope) EmbeddableTransactionManagerFactory.getLocalTransactionCurrent().getLocalTranCoord();

        if (uowScope == null) {
            // There was no LTC on the thread. Check for a global transaction

            try {
                uowScope = (UOWScope) EmbeddableTransactionManagerFactory.getTransactionManager().getTransaction();
            } catch (javax.transaction.SystemException se) {
                FFDCFilter.processException(se, "com.ibm.ws.uow.UOWManagerImpl.getUOWScope", "364", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "SystemException caught checking for transaction", se);

                final SystemException systemException = new SystemException(se);

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getUOWScope", systemException);
                throw systemException;
            }

            // If there is still no UOWScope on the thread at this stage we're in
            // an unusual state. We know that there is no local or global transaction
            // on the thread as we have checked above and it should be impossible
            // for an ActivitySession to be active on a thread without an LTC or
            // global transaction running beneath it.
            //
            // In practice the above is not 100% true as there are windows created
            // by, for example, the order in which the EJB container's UOW collaborators
            // are run. These windows can cause an ActivitySession to be begun before a
            // global transaction or LTC is begun beneath it. For code to expose this
            // problem it would have to be invoked as part of a callback for the
            // ActivitySession being begun. To avoid such code receiving a null UOWScope
            // when its just been told that there's an ActivitySession on the thread we'll
            // check for an ActivitySession without an LTC or global transaction just in case.
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getUOWScope", uowScope);
        return uowScope;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#runUnderUOW(int, boolean, com.ibm.wsspi.uow.UOWAction)
     */
    @Override
    public void runUnderUOW(int uowType, boolean join, UOWAction uowAction) throws UOWActionException, UOWException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "runUnderUOW", new Object[] { new Integer(uowType), Boolean.valueOf(join), uowAction });

        final UOWScope initialUOWScope;

        try {
            initialUOWScope = getUOWScope();
        } catch (SystemException e) {
            FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.runUnderUOW", "96", this);
            final UOWException uowe = new UOWException(e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "runUnderUOW", e);
            throw uowe;
        }

        UOWToken uowt = null;
        if (initialUOWScope != null && (join == false || uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION || uowType != getUOWType())) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Need to suspend current UOW");

            try {
                uowt = suspend();
            } catch (Throwable t) {
                FFDCFilter.processException(t, "com.ibm.ws.spi.uow.UOWManagerImpl.runUnderUOW", "81", this);
                final UOWException uowe = new UOWException(t);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "runUnderUOW", uowe);
                throw uowe;
            }
        }

        Throwable toThrow = null;
        try {
            try {
                if (getUOWScope() == null) {
                    // Either we just suspended a UOW or there was no
                    // UOW on the thread in the first place
                    toThrow = runUnderNewUOW(uowType, uowAction);
                } else {
                    toThrow = runUnderCurrentUOW(uowAction);
                }
            } catch (SystemException e) {
                FFDCFilter.processException(e, "com.ibm.ws.uow.PreviewUOWManagerImpl.run", "138", this);
                toThrow = new UOWException(e);
            }
        } finally {
            try {
                resume(uowt);
            } catch (Throwable t) {
                FFDCFilter.processException(t, "com.ibm.ws.spi.uow.UOWManagerImpl.runUnderUOW", "107", this);
                if (toThrow == null) {
                    toThrow = new UOWException(t);
                }
            }
        }

        if (toThrow instanceof UOWException) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "runUnderUOW", toThrow);
            throw (UOWException) toThrow;
        }

        if (toThrow instanceof UOWActionException) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "runUnderUOW", toThrow);
            throw (UOWActionException) toThrow;
        }

        if (toThrow instanceof RuntimeException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "runUnderUOW");
            throw (RuntimeException) toThrow;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "runUnderUOW");
    }

    @Override
    public long getUOWExpiration() throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getUOWExpiration", this);

        final long expiration;
        final int uowType = getUOWType();

        switch (uowType) {
            case UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION:
                try {
                    expiration = ((EmbeddableTransactionImpl) getUOWScope()).getExpirationTime();
                } catch (SystemException e) {
                    final IllegalStateException ise = new IllegalStateException(e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "getUOWExpiration", ise);
                    throw ise;
                }
                break;

            default:
                final IllegalStateException ise = new IllegalStateException("Invalid UOW type: " + uowType);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getUOWExpiration", ise);
                throw ise;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getUOWExpiration", expiration);
        return expiration;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#getUOWTimeout()
     */
    @Override
    public int getUOWTimeout() throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getUOWTimeout", this);

        final int timeout;
        final int uowType = getUOWType();

        switch (uowType) {
            case UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION:
                try {
                    timeout = ((EmbeddableTransactionImpl) getUOWScope()).getTimeout();
                } catch (SystemException e) {
                    final IllegalStateException ise = new IllegalStateException(e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "getUOWTimeout", ise);
                    throw ise;
                }
                break;

            default:
                final IllegalStateException ise = new IllegalStateException("Invalid UOW type: " + uowType);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getUOWTimeout", ise);
                throw ise;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getUOWTimeout", new Integer(timeout));
        return timeout;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.uow.UOWManager#setUOWTimeout(int, int)
     */
    @Override
    public void setUOWTimeout(int uowType, int timeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setUOWTimeout", new Object[] { Integer.valueOf(uowType), Integer.valueOf(timeout) });

        switch (uowType) {
            case UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION:
                try {
                    EmbeddableTransactionManagerFactory.getTransactionManager().setTransactionTimeout(timeout);
                } catch (Exception e) {
                    final IllegalArgumentException iae = new IllegalArgumentException(e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "setUOWTimeout", iae);
                    throw iae;
                }
                break;

            default:
                final IllegalArgumentException iae = new IllegalArgumentException("Invalid UOW type: " + uowType);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "setUOWTimeout", iae);
                throw iae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setUOWTimeout");
    }

    @Override
    public long getLocalUOWId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getLocalUOWId", this);

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            throw new IllegalStateException(se);
        }

        if (uowScope == null) {
            throw new IllegalStateException();
        }

        final long localId = uowScope.getLocalId();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getLocalUOWId", new Long(localId));
        return localId;
    }

    @Override
    public Object getResource(Object key) throws NullPointerException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getResource", new Object[] { key, this });

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getResource", "IllegalStateException");
            throw new IllegalStateException(se);
        }

        if (uowScope == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getResource", "IllegalStateException");
            throw new IllegalStateException();
        }

        final Object resource = uowScope.getResource(key);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getResource", resource);
        return resource;
    }

    @Override
    public boolean getRollbackOnly() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getRollbackOnly", this);

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getRollbackOnly", "IllegalStateException");
            throw new IllegalStateException(se);
        }

        if (uowScope == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getRollbackOnly", "IllegalStateException");
            throw new IllegalStateException();
        }

        final boolean rollbackOnly = uowScope.getRollbackOnly();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getRollbackOnly", Boolean.valueOf(rollbackOnly));
        return rollbackOnly;
    }

    @Override
    public int getUOWStatus() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getUOWStatus", this);

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getUOWStatus", "IllegalStateException");
            throw new IllegalStateException(se);
        }

        final int uowStatus;

        if (uowScope == null) {
            uowStatus = UOW_STATUS_NONE;
        } else {
            uowStatus = uowScope.getUOWStatus();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getUOWStatus", getUOWStatusAsString(uowStatus));
        return uowStatus;
    }

    @Override
    public int getUOWType() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getUOWType", this);

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getUOWType", "IllegalStateException");
            throw new IllegalStateException(se);
        }

        if (uowScope == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getUOWType", "IllegalStateException");
            throw new IllegalStateException();
        }

        final int uowType = uowScope.getUOWType();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getUOWType", new Integer(uowType));
        return uowType;
    }

    @Override
    public void putResource(Object key, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "putResource", new Object[] { key, value, this });

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "putResource", "IllegalStateException");
            throw new IllegalStateException(se);
        }

        if (uowScope == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "putResource", "IllegalStateException");
            throw new IllegalStateException();
        }

        uowScope.putResource(key, value);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "putResource");
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "registerInterposedSynchronization", new Object[] { sync, this });

        if (sync == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "registerInterposedSynchronization", "NullPointerException");
            throw new NullPointerException();
        }

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "registerInterposedSynchronization", "IllegalStateException");
            throw new IllegalStateException(se);
        }

        if (uowScope == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "registerInterposedSynchronization", "IllegalStateException");
            throw new IllegalStateException();
        }

        uowScope.registerInterposedSynchronization(sync);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "registerInterposedSynchronization");
    }

    @Override
    public void setRollbackOnly() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setRollbackOnly", this);

        final SynchronizationRegistryUOWScope uowScope;

        try {
            uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
        } catch (SystemException se) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "setRollbackOnly", "IllegalStateException");
            throw new IllegalStateException(se);
        }

        if (uowScope == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "setRollbackOnly", "IllegalStateException");
            throw new IllegalStateException();
        }

        if (uowScope instanceof LocalTranCoordImpl) {
            ((LocalTranCoordImpl) uowScope).setRollbackOnlyFromApplicationCode();
        } else {
            uowScope.setRollbackOnly();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setRollbackOnly");
    }

    @Override
    public void contextChange(int changeType, UOWScope uowScope) throws IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "contextChange", new Object[] { new Integer(changeType), uowScope, this });

        if (changeType == UOWScopeCallback.POST_BEGIN) {
            _callbackManager.notifyCallbacks(changeType, uowScope);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "contextChange");
    }

    /**
     * @param uowType
     * @param uowAction
     * @return
     */
    private Throwable runUnderNewUOW(int uowType, UOWAction uowAction) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "runUnderNewUOW", new Object[] { new Integer(uowType), uowAction, this });

        Throwable toThrow = null;
        final UOWScope uowScope;

        try {
            uowScope = uowBegin(uowType);
        } catch (Throwable t) {
            FFDCFilter.processException(t, "com.ibm.ws.uow.UOWManagerImpl.runUnderNewUOW", "922", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "runUnderNewUOW", t);
            return new UOWException(t);
        }

        try {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Running UOWAction");
            uowAction.run();
        } catch (RuntimeException e) {
            FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.runUnderNewUOW", "934", this);
            setRollbackOnly();
            toThrow = e;
        } catch (Exception e) {
            // No FFDC Code Needed

            // A checked exception may be part of the application's
            // logic so we do not want to output an FFDC here.

            toThrow = new UOWActionException(e);
        } catch (Throwable t) {
            FFDCFilter.processException(t, "com.ibm.ws.uow.UOWManagerImpl.runUnderNewUOW", "944", this);
            setRollbackOnly();
            toThrow = new UOWException(t);
        } finally {
            try {
                uowEnd(uowScope, uowType);
            } catch (Throwable t) {
                FFDCFilter.processException(t, "com.ibm.ws.spi.uow.UOWManagerImpl.runUnderNewUOW", "175", this);
                final UOWException uowe = new UOWException(t);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "runUnderNewUOW", uowe);
                return uowe;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "runUnderNewUOW", toThrow);
        return toThrow;
    }

    /**
     * @param uowType
     * @throws Exception
     * @throws Exception
     */
    protected void uowEnd(UOWScope uowScope, int uowType) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "uowEnd", new Object[] { uowScope, new Integer(uowType), this });

        final UOWCoordinator uow = EmbeddableTransactionManagerFactory.getUOWCurrent().getUOWCoord();

        try {
            if (getRollbackOnly()) {
                boolean timedOut = false;
                int timedOutTime = 0; //PM18033
                boolean subRollback = false;

                if (uowType == UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
                    timedOut = ((EmbeddableTransactionImpl) uowScope).isTimedOut();
                    timedOutTime = ((EmbeddableTransactionImpl) uowScope).getTimeout(); //PM18033
                    subRollback = ((EmbeddableTransactionImpl) uowScope).isSubRollback();
                }

                uowRollback(uowType);

                if (timedOut) {
                    // Throw a rollback exception which the user will
                    // see wrapped in a UOWException
                    final RollbackException rbe = new RollbackException("Global transaction timed out after " + timedOutTime + " seconds"); //PM18033
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "uowEnd", rbe);
                    throw rbe;
                }

                if (subRollback) {
                    // Throw a rollback exception which the user will
                    // see wrapped in a UOWException
                    final RollbackException rbe = new RollbackException("Global transaction rolledback from client inactivity timeout from subordinate");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "uowEnd", rbe);
                    throw rbe;
                }
            } else {
                uowCommit(uowType);
            }
        } finally {
            if (_runUnderUOWCallbackManager != null) {
                _runUnderUOWCallbackManager.notifyCallbacks(UOWCallback.POST_END, uow);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "uowEnd");
    }

    /**
     * @param uowType
     * @throws Exception
     */
    protected void uowCommit(int uowType) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "uowCommit", Integer.valueOf(uowType));

        switch (uowType) {
            case UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION:
                EmbeddableTransactionManagerFactory.getTransactionManager().commit();
                break;

            case UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION:
                LocalTranCurrentSet.instance().end(LocalTransactionCurrent.EndModeCommit);
                break;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "uowCommit");
    }

    /**
     * @param uowType
     * @throws Exception
     */
    protected void uowRollback(int uowType) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "uowRollback", Integer.valueOf(uowType));

        switch (uowType) {
            case UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION:
                EmbeddableTransactionManagerFactory.getTransactionManager().rollback();
                break;

            case UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION:
                LocalTranCurrentSet.instance().end(LocalTransactionCurrent.EndModeRollBack);
                break;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "uowRollback");
    }

    /**
     * @param uowType
     * @return
     * @throws SystemException
     * @throws ActivitySessionAlreadyActiveException
     * @throws TransactionPendingException
     * @throws com.ibm.websphere.ActivitySession.SystemException
     * @throws javax.transaction.NotSupportedException
     */
    protected UOWScope uowBegin(int uowType) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "uowBegin", Integer.valueOf(uowType));

        switch (uowType) {
            case UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION:
                UserTransactionImpl.instance().begin();
                break;

            case UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION:
                LocalTranCurrentSet.instance().begin(false, false, false);
                break;
        }

        final UOWScope uows = getUOWScope();

        if (_runUnderUOWCallbackManager != null) {
            _runUnderUOWCallbackManager.notifyCallbacks(UOWCallback.POST_BEGIN, EmbeddableTransactionManagerFactory.getUOWCurrent().getUOWCoord());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "uowBegin", uows);
        return uows;
    }

    /**
     * @param uowAction
     * @return
     */
    private Throwable runUnderCurrentUOW(UOWAction uowAction) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "runUnderCurrentUOW", new Object[] { uowAction, this });

        Throwable toThrow = null;

        try {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Running UOWAction");
            uowAction.run();
        } catch (RuntimeException e) {
            FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.runUnderCurrentUOW", "1130", this);
            setRollbackOnly();
            toThrow = e;
        } catch (Exception e) {
            // No FFDC Code Needed

            // A checked exception may be part of the application's
            // logic so we do not want to output an FFDC here.
            toThrow = new UOWActionException(e);
        } catch (Throwable t) {
            FFDCFilter.processException(t, "com.ibm.ws.uow.UOWManagerImpl.runUnderCurrentUOW", "1140", this);
            setRollbackOnly();
            toThrow = new UOWException(t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "runUnderCurrentUOW", toThrow);
        return toThrow;
    }

    protected String getUOWStatusAsString(int status) {
        final String uowStatus;

        switch (status) {
            case UOW_STATUS_ACTIVE: {
                uowStatus = "active";
                break;
            }
            case UOW_STATUS_NONE: {
                uowStatus = "none";
                break;
            }
            case UOW_STATUS_COMMITTED: {
                uowStatus = "committed";
                break;
            }
            case UOW_STATUS_ROLLBACKONLY: {
                uowStatus = "rollback only";
                break;
            }
            case UOW_STATUS_ROLLEDBACK: {
                uowStatus = "rolled back";
                break;
            }
            default: {
                uowStatus = "undefined";
                break;
            }
        }

        return uowStatus;
    }

    @Override
    public String getUOWName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getUOWName", this);

        String uowName = null;

        try {
            final SynchronizationRegistryUOWScope uowScope = (SynchronizationRegistryUOWScope) getUOWScope();
            uowName = uowScope.getUOWName();
        } catch (SystemException se) {
            FFDCFilter.processException(se, "com.ibm.ws.uow.UOWManagerImpl.getUOWName", "1311", this);

            // FFDC the exception and return null to the caller.
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getUOWName", uowName);
        return uowName;
    }

    @Override
    public Object runUnderUOW(int uowType, boolean join, ExtendedUOWAction uowAction, Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) throws Exception, UOWException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "runUnderUOW", new Object[] { uowType, join, uowAction, rollbackOn, dontRollbackOn });

        final UOWScope initialUOWScope = getUOWScope();

        UOWToken uowt = null;
        if (initialUOWScope != null && (join == false || uowType == UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION || uowType != getUOWType())) {

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Need to suspend current UOW");

            uowt = suspend();
        }

        Object ret = null;
        try {
            if (getUOWScope() == null) {
                // Either we just suspended a UOW or there was no
                // UOW on the thread in the first place
                ret = runUnderNewUOW(uowType, uowAction, rollbackOn, dontRollbackOn);
            } else {
                ret = runUnderCurrentUOW(uowAction, rollbackOn, dontRollbackOn);
            }
        } finally {
            resume(uowt);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "runUnderUOW", ret);
        return ret;
    }

    /**
     * @param uowType
     * @param uowAction
     * @return
     * @throws Exception
     */
    private Object runUnderNewUOW(int uowType, ExtendedUOWAction uowAction, Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "runUnderNewUOW", new Object[] { uowType, uowAction, this });

        Object ret = null;
        final UOWScope uowScope = uowBegin(uowType);

        try {
            ret = runUnderCurrentUOW(uowAction, rollbackOn, dontRollbackOn);
        } finally {
            try {
                uowEnd(uowScope, uowType);
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.ws.spi.uow.UOWManagerImpl.runUnderNewUOW", "1220", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "runUnderNewUOW");
                throw e;
            } catch (Throwable t) {
                FFDCFilter.processException(t, "com.ibm.ws.spi.uow.UOWManagerImpl.runUnderNewUOW", "1222", this);
                UOWException e = new UOWException(t);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "runUnderNewUOW");
                throw e;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "runUnderNewUOW", ret);
        return ret;
    }

    /**
     * @param uowAction
     * @return
     */
    private Object runUnderCurrentUOW(ExtendedUOWAction uowAction, Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "runUnderCurrentUOW", new Object[] { uowAction, this });

        Object ret = null;

        try {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Running UOWAction");

            ret = uowAction.run();
        } catch (RuntimeException e) {

            if (!in(e, dontRollbackOn)) {
                setRollbackOnly();
                FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.runUnderCurrentUOW", "1130", this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "runUnderCurrentUOW", e);
            throw e;
        } catch (Exception e) {
            // No FFDC Code Needed

            // A checked exception may be part of the application's
            // logic so we do not want to output an FFDC here.

            if (in(e, rollbackOn)) {
                setRollbackOnly();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "runUnderCurrentUOW", e);
            throw e;
        } catch (Throwable t) {
            // We have caught a strange beast. If it is not an Error, pack it in an Error and throw that.
            // Otherwise we can rethrow the Error
            // Also, we'll set rollbackOnly unless this weird thing is in the dontRollbackOn list
            FFDCFilter.processException(t, "com.ibm.ws.uow.UOWManagerImpl.runUnderNewUOW", "1317", this);

            if (!in(t, dontRollbackOn)) {
                setRollbackOnly();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "runUnderCurrentUOW", t);

            if (t instanceof Error) {
                throw (Error) t;
            }

            throw new Error(t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "runUnderCurrentUOW", ret);
        return ret;
    }

    private boolean in(Throwable t, Class<?>[] list) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "in", new Object[] { t, list });

        if (list != null && t != null) {
            for (Class<?> c : list) {
                if (c.isAssignableFrom(t.getClass())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "in", true);
                    return true;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "in", false);
        return false;
    }

    /*
     * These UserTransactionController methods should probably be on some other class but they can stay here for now
     */
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsspi.uow.UserTransactionController#disable()
     */
    @Override
    public boolean isEnabled() {
        return EmbeddableUserTransactionImpl.isEnabled();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsspi.uow.UserTransactionController#enable()
     */
    @Override
    public void setEnabled(boolean enabled) {
        EmbeddableUserTransactionImpl.setEnabled(enabled);
    }

//    /**
//     * @param uowType
//     * @param uowAction
//     * @return
//     * @throws Exception
//     */
//    private Object runUnderNewUOW(int uowType, ExtendedUOWAction uowAction, Class[] rollbackOn, Class[] dontRollbackOn) throws Exception
//    {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//            Tr.entry(tc, "runUnderNewUOW", new Object[] { uowType, uowAction, this });
//
//        Object ret = null;
//        final UOWScope uowScope = uowBegin(uowType);
//
//        try {
//            if (tc.isDebugEnabled())
//                Tr.debug(tc, "Running UOWAction");
//
//            ret = uowAction.run();
//        } catch (RuntimeException e) {
//            FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.runUnderNewUOW", "934", this);
//
//            if (!in(e, dontRollbackOn)) {
//                setRollbackOnly();
//            }
//
//            throw e;
//        } catch (Exception e) {
//            // No FFDC Code Needed
//
//            // A checked exception may be part of the application's
//            // logic so we do not want to output an FFDC here.
//
//            if (in(e, rollbackOn)) {
//                setRollbackOnly();
//            }
//
//            throw e;
//        } catch (Error e) {
//            FFDCFilter.processException(e, "com.ibm.ws.uow.UOWManagerImpl.runUnderNewUOW", "1240", this);
//
//            if (!in(e, dontRollbackOn)) {
//                setRollbackOnly();
//            }
//
//            throw e;
//        } catch (Throwable t) {
//            // We have caught a strange beast, a subclass of Throwable that is neither Exception nor Error.
//            // We can't throw it because it's not declared. We'll pack it in an Error and throw that.
//            // Also, we'll set rollbackOnly like an Error
//            FFDCFilter.processException(t, "com.ibm.ws.uow.UOWManagerImpl.runUnderNewUOW", "1250", this);
//
//            if (!in(t, dontRollbackOn)) {
//                setRollbackOnly();
//            }
//
//            throw new Error(t);
//        } finally {
//            try {
//                uowEnd(uowScope, uowType);
//            } catch (Throwable t) {
//                FFDCFilter.processException(t, "com.ibm.ws.spi.uow.UOWManagerImpl.runUnderNewUOW", "175", this);
//            }
//        }
//
//        if (tc.isEntryEnabled())
//            Tr.exit(tc, "runUnderNewUOW", ret);
//        return ret;
//    }
}