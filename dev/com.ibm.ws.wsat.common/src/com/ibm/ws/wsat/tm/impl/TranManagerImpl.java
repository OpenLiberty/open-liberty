/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.tm.impl;

import java.io.Serializable;

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.tx.remote.Vote;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.JTA.HeuristicHazardException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.uow.UOWManager;

/**
 * This class contains our own WS-AT specific wrapper around the various
 * transaction manager interfaces. Mostly these are simple wrappers around the
 * RemoteTransactionController.
 */
public class TranManagerImpl {

    private static final String CLASS_NAME = TranManagerImpl.class.getName();
    private static final TraceComponent TC = Tr.register(TranManagerImpl.class);

    private static final TranManagerImpl INSTANCE = new TranManagerImpl();

    private TransactionManager localTranMgr;
    private RemoteTransactionController remoteTranMgr;
    private UOWManager uowManager;
    private TransactionSynchronizationRegistry syncRegistry;
    private ClassLoadingService clService;

    public static TranManagerImpl getInstance() {
        return INSTANCE;
    }

    public synchronized TransactionManager getLocalTranMgr() {
        if (localTranMgr == null) {
            localTranMgr = getService(TransactionManager.class);
        }
        return localTranMgr;
    }

    public synchronized RemoteTransactionController getRemoteTranMgr() {
        if (remoteTranMgr == null) {
            remoteTranMgr = getService(RemoteTransactionController.class);
        }
        return remoteTranMgr;
    }

    public synchronized UOWManager getUOWManager() {
        if (uowManager == null) {
            uowManager = getService(UOWManager.class);
        }
        return uowManager;
    }

    public synchronized TransactionSynchronizationRegistry getTranSyncRegistry() {
        if (syncRegistry == null) {
            syncRegistry = getService(TransactionSynchronizationRegistry.class);
        }
        return syncRegistry;
    }

    public synchronized ClassLoadingService getClassLoadingService() {
        if (clService == null) {
            clService = getService(ClassLoadingService.class);
        }
        return clService;
    }

    private <T> T getService(Class<T> service) {
        T impl = null;
        BundleContext context = FrameworkUtil.getBundle(service).getBundleContext();
        ServiceReference<T> ref = context.getServiceReference(service);
        if (ref != null) {
            impl = context.getService(ref);
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unable to locate service: {0}", service);
            }
            //throw new WSATException("Cannot locate service " + service);
        }
        return impl;
    }

    /*
     * Return true if a local transaction is active on the current thread. Will return
     * false if no transaction, or any kind of error prevents us from knowing.
     */
    public boolean isTranActive() {
        boolean active = false;
        try {
            int status = getLocalTranMgr().getStatus();
            if (status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK) {
                active = true;
            }
        } catch (SystemException e) {
        }
        return active;
    }

    /*
     * Return the local JTA transaction instance
     */
    public Transaction getTransaction() throws WSATException {
        Transaction txTran = null;
        try {
            getLocalTranMgr().getTransaction();
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
        return txTran;
    }

    /*
     * Flag local transaction as failed
     */
    public void markRollback() throws WSATException {
        try {
            getLocalTranMgr().setRollbackOnly();
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
    }

    /*
     * Return the expiry time (in milliseconds) for the current transaction.
     * This is the actual time left before the transaction expires. Returns a
     * very large number if no timeout is set.
     */
    public long getTimeout() throws IllegalStateException, WSATException {
        long expireTime = getUOWManager().getUOWExpiration();
        if (expireTime != 0l)
            return expireTime - System.currentTimeMillis();
        else
            return Long.MAX_VALUE / 4l;
    }

    /*
     * Import/export transactions between systems
     */
    public String getGlobalId() throws WSATException {
        String globalId = null;
        try {
            globalId = getRemoteTranMgr().getGlobalId();
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
        return globalId;
    }

    /*
     * Import/export transactions between systems
     */
    public String exportTransaction() throws WSATException {
        String globalId = null;
        try {
            globalId = getRemoteTranMgr().exportTransaction();
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
        return globalId;
    }

    public void unexportTransaction(String globalId) throws WSATException {
        try {
            getRemoteTranMgr().unexportTransaction(globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
    }

    public void setRollbackOnly(String globalId) throws WSATException {
        try {
            getRemoteTranMgr().setRollbackOnly(globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
    }

    public boolean importTransaction(String globalId, int timeout) throws WSATException {
        boolean imported = false;
        try {
            imported = getRemoteTranMgr().importTransaction(globalId, timeout);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
        return imported;
    }

    public void unimportTransaction(String globalId) throws WSATException {
        try {
            getRemoteTranMgr().unimportTransaction(globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
    }

    /*
     * Enlist resources for 2PC processing
     */
    public boolean registerParticipant(String globalId, WSATParticipant participant) throws WSATException {
        boolean result = false;
        try {
            Serializable key = ParticipantFactoryService.serialize(participant);
            result = getRemoteTranMgr().registerRemoteParticipant(Constants.WS_FACTORY_PART_FILTER, key, globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
        return result;
    }

    /*
     * Enlist for potential participant recovery
     */
    public boolean registerCoordinator(String globalId, WSATCoordinator coordinator) throws WSATException {
        boolean result = false;
        try {
            Serializable key = ParticipantFactoryService.serialize(coordinator);
            result = getRemoteTranMgr().registerRecoveryCoordinator(Constants.WS_FACTORY_COORD_FILTER, key, globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
        return result;
    }

    /*
     * Process 2PC for a local transaction
     */
    @FFDCIgnore(SystemException.class)
    public Vote prepareTransaction(String globalId) throws WSATException {
        try {
            return getRemoteTranMgr().prepare(globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        } catch (HeuristicHazardException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        } catch (HeuristicMixedException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        } catch (RollbackException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        }
    }

    @FFDCIgnore(SystemException.class)
    public void commitTransaction(String globalId) throws WSATException {
        try {
            getRemoteTranMgr().commit(globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        } catch (HeuristicHazardException e) {
            // Don't support heuristics yet
        } catch (HeuristicRollbackException e) {
            // Don't support heuristics yet
        } catch (HeuristicMixedException e) {
            // Don't support heuristics yet
        }
    }

    @FFDCIgnore(SystemException.class)
    public void rollbackTransaction(String globalId) throws WSATException {
        try {
            getRemoteTranMgr().rollback(globalId);
        } catch (SystemException e) {
            throw new WSATException(Tr.formatMessage(TC, "TRAN_MGR_ERROR_CWLIB0205"), e);
        } catch (HeuristicHazardException e) {
            // Don't support heuristics yet
        } catch (HeuristicCommitException e) {
            // Don't support heuristics yet
        } catch (HeuristicMixedException e) {
            // Don't support heuristics yet
        }
    }

    public boolean replayCompletion(String globalId) {
        return getRemoteTranMgr().replayCompletion(globalId);
    }

    /*
     * Register with the synchronization registry (to detect transaction end)
     */
    public void registerTranSync(Synchronization tranSync) {
        getTranSyncRegistry().registerInterposedSynchronization(tranSync);
    }

    /*
     * Class loading services
     */
    public ClassLoader getThreadClassLoader(Class<?> cl) {
        return getClassLoadingService().createThreadContextClassLoader(cl.getClassLoader());
    }

    public void destroyThreadClassLoader(ClassLoader loader) {
        getClassLoadingService().destroyThreadContextClassLoader(loader);
    }
}
