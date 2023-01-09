package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.Serializable;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.util.TxTMHelper;
import com.ibm.tx.util.TMHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.Transaction.JTA.JTAResource;
import com.ibm.ws.Transaction.JTS.Configuration;
import com.ibm.wsspi.tx.UOWEventListener;

/**
 * A singleton class that delegates method calls to a
 * thread local transaction manager.
 * The class is also responsible for the registration
 * of two varieties of callbacks: context change
 * callbacks and unit of work callbacks.
 */
public class TranManagerSet implements ExtendedTransactionManager, UOWCurrent {
    private static final TraceComponent tc = Tr.register(TranManagerSet.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected static ExtendedTransactionManager _instance;

    protected volatile boolean _replayComplete;

    protected volatile boolean _quiesced;

    protected static ThreadLocal<?> _thread;

    protected TranManagerSet() {
    }

    public synchronized static ExtendedTransactionManager instance() {
        if (_instance == null) {
            _instance = new TranManagerSet();
            _thread = new ThreadLocal<TranManagerImpl>() {
                @Override
                public TranManagerImpl initialValue() {
                    return new TranManagerImpl();
                }
            };

        }

        return _instance;
    }

    protected TranManagerImpl self() {
        return (TranManagerImpl) _thread.get();
    }

    public void cleanup() {
        if (_thread != null)
            _thread.remove();
    }

    @Override
    public void begin() throws NotSupportedException, SystemException /* @512190C */
    {
        TMHelper.checkTMState();

        self().begin();
    }

    @Override
    public void begin(int timeout) throws NotSupportedException, SystemException {
        TMHelper.checkTMState();

        self().begin(timeout);
    }

    /**
     * Used by UserTransaction to create a transaction
     */
    public void beginUserTran() throws NotSupportedException, SystemException /* @512190C */
    {
        TMHelper.checkTMState();

        self().beginUserTran();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        self().commit();
    }

    @Override
    public int getStatus() {
        return self().getStatus();
    }

    @Override
    public Transaction getTransaction() {
        return self().getTransaction();
    }

    public TransactionImpl getTransactionImpl() {
        return self().getTransactionImpl();
    }

    @Override
    public void resume(Transaction tran) throws InvalidTransactionException, IllegalStateException {
        self().resume(tran);
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        self().rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        self().setRollbackOnly();
    }

    @Override
    public void setTransactionTimeout(int timeout) throws SystemException {
        self().setTransactionTimeout(timeout);
    }

    @Override
    public Transaction suspend() {
        return self().suspend();
    }

    @Override
    public boolean delist(XAResource xaRes, int flag) {
        return self().delist(xaRes, flag);
    }

    public void replayComplete(boolean localFailureScope) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "replayComplete", localFailureScope);

        if (localFailureScope) {
            TMHelper.asynchRecoveryProcessingComplete(null);
        }

        _replayComplete = true;
    }

    public boolean isReplayComplete() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isReplayComplete", _replayComplete);
        return _replayComplete;
    }

    @Override
    public int registerResourceInfo(String xaResFactoryClassName, Serializable xaResInfo) {
        return registerResourceInfo(xaResFactoryClassName, xaResInfo, JTAResource.DEFAULT_COMMIT_PRIORITY);
    }

    @Override
    public int registerResourceInfo(String xaResFactoryClassName, Serializable xaResInfo, int priority) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerResourceInfo", new Object[] { xaResFactoryClassName, xaResInfo, priority, this });

        try {
            TMHelper.checkTMState(); // start the TX service if not already
        } catch (NotSupportedException ex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "registerResourceInfo: checkTMState failed: ", ex);
        }

        int index = -1;
        if (xaResFactoryClassName != null && xaResFactoryClassName.length() != 0 && TxTMHelper.ready()) {
            final PartnerLogTable plt = Configuration.getFailureScopeController().getPartnerLogTable();

            if (plt != null) // ie if the core TM is not stopped
            {
                final XARecoveryWrapper xaWrapper = new XARecoveryWrapper(xaResFactoryClassName, xaResInfo, null, priority);

                // Ensure this wrapper is in the cache
                final PartnerLogData pld = plt.findEntry(xaWrapper);

                // Could have been set terminating if this is an unregistered activationspec
                pld._terminating = false;

                index = pld.getIndex();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerResourceInfo", index);
        return index;
    }

    /* Called by MessageEndpointHandler */
    @Override
    public boolean enlist(XAResource xaRes, int recoveryId) throws RollbackException, IllegalStateException, SystemException {
        return self().enlist(xaRes, recoveryId);
    }

    public PartnerLogData registerJCAProvider(String providerId) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerJCAProvider", providerId);

        final PartnerLogData pld;

        final JCARecoveryWrapper jcarw = new JCARecoveryWrapper(providerId);

        // Ensure this wrapper is in the cache
        final PartnerLogTable plt = Configuration.getFailureScopeController().getPartnerLogTable();
        pld = plt.findEntry(jcarw);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerJCAProvider", pld);
        return pld;
    }

    /**
     *
     */
    public void quiesce() {
        _quiesced = true;
    }

    /**
     * @return
     */
    public boolean isQuiesced() {
        return _quiesced;
    }

    //
    // UOWCurrent interface
    //
    @Override
    public UOWCoordinator getUOWCoord() {
        return self().getUOWCoord();
    }

    @Override
    public int getUOWType() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getUOWType");

        final UOWCoordinator uowCoord = getUOWCoord();

        int result = UOWCurrent.UOW_NONE;

        if (uowCoord != null) {
            result = uowCoord.isGlobal() ? UOWCurrent.UOW_GLOBAL : UOWCurrent.UOW_LOCAL;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getUOWType", result);
        return result;
    }

    @Override
    public void registerLTCCallback(UOWCallback callback) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerLTCCallback", callback);

        // No support for LTCCallbacks ... provided by derived classes

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerLTCCallback");
    }

    @Override
    public void setUOWEventListener(UOWEventListener el) {
        self().setUOWEventListener(el);
    }

    @Override
    public void unsetUOWEventListener(UOWEventListener el) {
        self().unsetUOWEventListener(el);
    }
}