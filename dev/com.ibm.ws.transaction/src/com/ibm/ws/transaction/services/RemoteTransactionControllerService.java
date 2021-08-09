/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import java.io.Serializable;

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.tx.jta.embeddable.impl.WSATRecoveryCoordinator;
import com.ibm.tx.jta.impl.LocalTIDTable;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.ltc.impl.LocalTranCurrentSet;
import com.ibm.tx.remote.DistributableTransaction;
import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.tx.remote.TransactionWrapper;
import com.ibm.tx.remote.Vote;
import com.ibm.tx.util.TMHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.JTA.HeuristicHazardException;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;

/**
 *
 */
@Component(service = RemoteTransactionController.class)
public class RemoteTransactionControllerService implements RemoteTransactionController {

    private static final TraceComponent tc = Tr.register(RemoteTransactionControllerService.class);

    private final ThreadLocal<LocalTransactionCoordinator> _suspendedLTC = new ThreadLocal<LocalTransactionCoordinator>();
    private final ThreadLocal<DistributableTransaction> _threadImportedTran = new ThreadLocal<DistributableTransaction>();

    private UOWCurrent _uowc;

    private TransactionManager _tm;

    @Reference
    protected void setUOWCurrent(UOWCurrent uowc) {
        _uowc = uowc;
    }

    @Reference
    protected void setTransactionManager(TransactionManager tm) {
        _tm = tm;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#importTransaction(java.lang.String)
     */
    @Override
    public boolean importTransaction(String globalId, int expires) throws SystemException {

        // Make sure TM is open for business
        try {
            TMHelper.checkTMState();
        } catch (NotSupportedException e) {
            final SystemException se = new SystemException();
            se.initCause(e);
            throw se;
        }

        //The next thing we need to do is distinguish between local and remote invocations. If the UOW currently associated
        //with the thread is a global txn then we know the invocation must be local.
        final UOWCoordinator uowCoord = _uowc.getUOWCoord();

        if (uowCoord != null && uowCoord.isGlobal()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "The Web Service request is a local invocation");

            ((DistributableTransaction) uowCoord).resumeAssociation();
            return false;
        }

        LocalTransactionCoordinator ltc;
        _suspendedLTC.set(ltc = LocalTranCurrentSet.instance().suspend());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Suspended LTC", ltc);

        // Still could be local
        DistributableTransaction tx = getTransactionForID(globalId);
        if (tx != null) {
            // We know about this transaction already so at least we know we don't need to register

            try {
                _tm.resume((Transaction) tx);
            } catch (Exception e) {
                SystemException se = new SystemException();
                se.initCause(e);
                throw se;
            }

            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "The Web Service request is a remote invocation");

        _threadImportedTran.set(tx = new EmbeddableTransactionImpl(expires, globalId));
        // Add association to police single thread per tran
        tx.addAssociation();
        new TransactionWrapper((EmbeddableTransactionImpl) tx);

        // Resume transaction on this thread
        try {
            _tm.resume((Transaction) tx);
        } catch (Exception e) {
            SystemException se = new SystemException();
            se.initCause(e);
            throw se;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#unimportTransaction(java.lang.String)
     */
    @Override
    public void unimportTransaction(String globalId) throws SystemException {

        DistributableTransaction tx = getTransactionForID(globalId);

        // In the case of the in-process http channel, where no WS-AT
        // context is being propagated, we get the situation where there is a global tran
        // on the thread at this point but it is not imported
        if (tx != null) {
            if (!globalId.equals(tx.getGlobalId())) {
                // ???
                final SystemException e = new SystemException();
                throw e;
            }

            final Transaction suspendedTx = _tm.suspend();
            // If zOS and isSupportWSAT=false then there is no tx to suspend
            if ((suspendedTx != null) && (!tx.equals(suspendedTx))) {
                // Something is badly wrong
                final SystemException e = new SystemException();
                throw e;
            }
        }

        _threadImportedTran.set(null);

        // d238113 - check for null although with defect 235471.2 we should not get this problem
        if (tx != null) {
            tx.removeAssociation();
        }

        final LocalTransactionCoordinator ltc = _suspendedLTC.get();
        if (ltc != null) {
            LocalTranCurrentSet.instance().resume(ltc);
        }

        _suspendedLTC.set(null);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#exportTransaction()
     */
    @Override
    public String exportTransaction() throws SystemException {

        // blow up if there's no tran
        final UOWCoordinator uowCoord = _uowc.getUOWCoord();
        if (uowCoord == null || !uowCoord.isGlobal()) {
            throw new SystemException("No global transaction");
        }

        _tm.suspend();

        return ((DistributableTransaction) uowCoord).getGlobalId();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#unexportTransaction(java.lang.String)
     */
    @Override
    public void unexportTransaction(String globalId) throws SystemException {

        final DistributableTransaction tx = getTransactionForID(globalId);

        try {
            _tm.resume((Transaction) tx);
        } catch (Exception e) {
            SystemException se = new SystemException();
            se.initCause(e);
            throw se;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#registerRemoteParticipant(java.lang.String, java.io.Serializable, java.lang.String)
     */
    @Override
    public boolean registerRemoteParticipant(String xaResFactoryFilter, Serializable xaResInfo, String globalId) throws SystemException {

        boolean retval = true; // set to false if InvalidState

        final DistributableTransaction tx = getTransactionForID(globalId);

        if (tx != null) {
            // Check the transaction state and action as appropriate
            switch (tx.getStatus()) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_MARKED_ROLLBACK:
                    tx.enlistAsyncResource(xaResFactoryFilter, xaResInfo, tx.getXid());
                    break;

                case Status.STATUS_PREPARING:
                    tx.setRollbackOnly();
                    // NOTE no break

                default:
                    retval = false; // caller sends InvalidState
                    break;

            } // end switch
        } else {
            retval = false;
        }

        return retval;
    }

    /**
     * @param xid
     * @return
     */
    private DistributableTransaction getTransactionForID(String globalId) {

        for (TransactionImpl tx : LocalTIDTable.getAllTransactions()) {
            if (globalId.equals(((DistributableTransaction) tx).getGlobalId())) {
                return (DistributableTransaction) tx;
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#registerRecoveryCoordinator(java.lang.String, java.io.Serializable, java.lang.String)
     */
    @Override
    public boolean registerRecoveryCoordinator(String recoveryCoordinatorFactoryFilter, Serializable recoveryCoordinatorInfo, String globalId) throws SystemException {

        final DistributableTransaction tx = getTransactionForID(globalId);

        WSATRecoveryCoordinator rc = new WSATRecoveryCoordinator(recoveryCoordinatorFactoryFilter, recoveryCoordinatorInfo, globalId);

        tx.setWSATRecoveryCoordinator(rc);

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#prepare(java.lang.String)
     */
    @Override
    public Vote prepare(String globalId) throws SystemException, HeuristicHazardException, HeuristicMixedException, RollbackException {

        final TransactionWrapper t = TransactionWrapper.getTransactionWrapper(globalId);

        if (t != null) {
            return t.prepare();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "No matching TransactionWrapper: " + globalId);
            throw new SystemException();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#commit(java.lang.String)
     */
    @Override
    public void commit(String globalId) throws SystemException, HeuristicHazardException, HeuristicRollbackException, HeuristicMixedException {

        final TransactionWrapper t = TransactionWrapper.getTransactionWrapper(globalId);

        if (t != null) {
            t.commit();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "No matching TransactionWrapper: " + globalId);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#rollback(java.lang.String)
     */
    @Override
    public void rollback(String globalId) throws HeuristicHazardException, HeuristicCommitException, HeuristicMixedException, SystemException {

        final TransactionWrapper t = TransactionWrapper.getTransactionWrapper(globalId);

        if (t != null) {
            t.rollback();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "No matching TransactionWrapper: " + globalId);
            throw new SystemException();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#setRollbackOnly(java.lang.String)
     */
    @Override
    public void setRollbackOnly(String globalId) throws SystemException {

        final DistributableTransaction tx = getTransactionForID(globalId);

        if (tx != null) {
            tx.setRollbackOnly();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.RemoteTransactionController#replayCompletion(java.lang.String)
     */
    @Override
    public boolean replayCompletion(String globalId) {
        final DistributableTransaction tx = getTransactionForID(globalId);

        if (tx != null) {
            tx.replayCompletion();
            return true;
        }

        return false;

    }

    @Override
    public String getGlobalId() throws SystemException {

        // blow up if there's no global tran
        final UOWCoordinator uowCoord = _uowc.getUOWCoord();
        if (uowCoord == null || !uowCoord.isGlobal()) {
            throw new SystemException("No global transaction");
        }

        return ((DistributableTransaction) uowCoord).getGlobalId();
    }
}
