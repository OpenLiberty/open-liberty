/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.Transaction.JTA.HeuristicHazardException;

/**
 *
 */
@Component(service = RemoteTransactionController.class)
public class RemoteTransactionControllerService implements RemoteTransactionController {

    private static final TraceComponent tc = Tr.register(RemoteTransactionControllerService.class);

    private static final ThreadLocal<UOWCoordinator> _threadUOWCoord = new ThreadLocal<UOWCoordinator>();
    private static final ThreadLocal<DistributableTransaction> _threadImportedTran = new ThreadLocal<DistributableTransaction>();

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

        //The first thing we need to do is distinguish between local and remote invocations. If the UOW currently associated
        //with the thread is a global txn then we know the invocation must be local. The other case is where the remote servers
        //web container has begun a local txn in the absence of the global context. We do know that there was a global txn on 
        //invocation else there wouldn't even be a cc available.
        final UOWCoordinator uowCoord = _uowc.getUOWCoord();
        _threadUOWCoord.set(uowCoord);

        if (uowCoord != null && uowCoord.isGlobal()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "The Web Service request is a local invocation");

            ((DistributableTransaction) uowCoord).resumeAssociation();
            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "The Web Service request is a remote invocation");

        try {
            TMHelper.checkTMState();
        } catch (NotSupportedException e) {
            final SystemException se = new SystemException();
            se.initCause(e);
            throw se;
        }

        //The web container will start a local tran as there will not be a global context
        //on thread. We need to suspend this before attaching the global tran
        final LocalTransactionCoordinator ltc = LocalTranCurrentSet.instance().suspend();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Suspended LTC", ltc);

        boolean createdNewTransaction = false;

        DistributableTransaction tx = getTransactionForID(globalId);

        if (tx == null) {
            tx = new EmbeddableTransactionImpl(expires, globalId);
            createdNewTransaction = true;

            new TransactionWrapper((EmbeddableTransactionImpl) tx);
        }

        _threadImportedTran.set(tx);

        // Add association to police single thread per tran
        tx.addAssociation();

        // Resume transaction on this thread
        try {
            _tm.resume((Transaction) tx);
        } catch (Exception e) {
            SystemException se = new SystemException();
            se.initCause(e);
            throw se;
        }

        return createdNewTransaction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.tx.remote.RemoteTransactionController#unimportTransaction(java.lang.String)
     */
    @Override
    public void unimportTransaction(String globalId) throws SystemException {

        DistributableTransaction tx = _threadImportedTran.get();

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

        final UOWCoordinator suspendedUOW = _threadUOWCoord.get();
        // ? Should null be forced back ? - shouldn't happen anyway, LTC must have been on the thread
        if (suspendedUOW != null && !suspendedUOW.isGlobal()) {
            LocalTranCurrentSet.instance().resume((LocalTransactionCoordinator) suspendedUOW);
        }

        _threadUOWCoord.set(null);
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

        // TODO TransactionWrapper stuff

        // Suspend transaction association as we are going off server
        ((DistributableTransaction) uowCoord).suspendAssociation();

        return ((DistributableTransaction) uowCoord).getGlobalId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.tx.remote.RemoteTransactionController#unexportTransaction(java.lang.String)
     */
    @Override
    public void unexportTransaction(String globalId) throws SystemException {

        // look up the tran or just get the one off the thread

        // blow up if there's no tran
        final UOWCoordinator uowCoord = _uowc.getUOWCoord();
        if (uowCoord == null || !uowCoord.isGlobal()) {
            throw new SystemException("No global transaction");
        }

        // Resume transaction association as we are coming back on server
        ((DistributableTransaction) uowCoord).resumeAssociation();
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
            // Stop anyone else shifting the ground beneath us
            tx.addAssociation();

            try {
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
            } finally {
                tx.removeAssociation();
            }
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
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Non matching GlobalId: " + ((DistributableTransaction) tx).getGlobalId());
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
}