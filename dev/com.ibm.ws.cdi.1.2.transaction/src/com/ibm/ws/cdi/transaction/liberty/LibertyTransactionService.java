/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.transaction.liberty;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.weld.transaction.spi.TransactionServices;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.tx.jta.impl.RegisteredSyncs;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * DS to provide transaction services to cdi; shared across all threads and applications
 */
@Component(
                property = { "service.vendor=IBM" })
public class LibertyTransactionService implements TransactionServices {

    private static final TraceComponent TC = Tr.register(LibertyTransactionService.class);

    private final AtomicServiceReference<UserTransaction> userTransaction = new AtomicServiceReference<UserTransaction>("userTransaction");
    private final AtomicServiceReference<TransactionManager> transactionManager = new AtomicServiceReference<TransactionManager>("transactionManager");

    /**
     * Called by DS to activate this service
     * 
     * @param compcontext the context of this component
     */
    protected void activate(ComponentContext compcontext) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Activating " + this.getClass().getName());
        }
        this.userTransaction.activate(compcontext);
        this.transactionManager.activate(compcontext);
    }

    /**
     * Called by DS to deactivate this service
     * 
     * @param compcontext the context of this component
     */
    protected void deactivate(ComponentContext compcontext) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Deactivating " + this.getClass().getName());
        }
        this.userTransaction.deactivate(compcontext);
        this.transactionManager.deactivate(compcontext);
    }

    /**
     * Called by DS to set the service reference
     * 
     * @param ref the reference from DS
     */
    @Reference(name = "userTransaction", service = UserTransaction.class, policy = ReferencePolicy.DYNAMIC)
    protected void setUserTransaction(ServiceReference<UserTransaction> ref) {
        this.userTransaction.setReference(ref);
    }

    /**
     * Called by DS to remove the service reference
     * 
     * @param ref the reference from DS
     */
    protected void unsetUserTransaction(ServiceReference<UserTransaction> ref) {
        this.userTransaction.unsetReference(ref);
    }

    /**
     * Called by DS to set the service reference
     * 
     * @param ref the reference from DS
     */
    @Reference(name = "transactionManager", service = TransactionManager.class, policy = ReferencePolicy.DYNAMIC)
    protected void setTransactionManager(ServiceReference<TransactionManager> ref) {
        this.transactionManager.setReference(ref);
    }

    /**
     * Called by DS to remove the service reference
     * 
     * @param ref the reference from DS
     */
    protected void unsetTransactionManager(ServiceReference<TransactionManager> ref) {
        this.transactionManager.unsetReference(ref);
    }

    public TransactionManager getTransactionManager() {
        TransactionManager transactionManager = this.transactionManager.getService();
        if (transactionManager == null && TC.isDebugEnabled()) {
            Tr.debug(TC, "Returning a null TransactionManager because the TransactionManager service is not currently unavailable. Is the transaction feature enabled?");
        }
        return transactionManager;
    }

    public Transaction getTransaction() {
        TransactionManager transactionManager = this.getTransactionManager();
        if (transactionManager == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Returning a null Transaction because the TransactionManager service is not currently unavailable. Is the transaction feature enabled?");
            }
            return null;
        }
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Returning a null Transaction: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public UserTransaction getUserTransaction() {
        UserTransaction utx = this.userTransaction.getService();
        if (utx == null && TC.isDebugEnabled()) {
            Tr.debug(TC, "Returning a null UserTransaction because the UserTransaction service is not currently unavailable. Is the transaction feature enabled?");
        }
        return utx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.weld.bootstrap.api.Service#cleanup()
     */
    @Override
    public void cleanup() {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "cleaning up " + this.getClass().getName());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.weld.transaction.spi.TransactionServices#isTransactionActive()
     */
    @Override
    public boolean isTransactionActive() {

        if (this.getTransactionManager() != null)
            try {
                if (this.getTransactionManager().getStatus() == Status.STATUS_ACTIVE)
                    return true;

            } catch (SystemException se) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Returning isTransactionActive() false: " + se.getMessage());
                }
                return false;
            }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.weld.transaction.spi.TransactionServices#registerSynchronization(javax.transaction.Synchronization)
     */
    @Override
    public void registerSynchronization(Synchronization sync) {

        TransactionManager transactionManager = this.getTransactionManager();
        try {
            Transaction tran = transactionManager.getTransaction();
            if (tran instanceof TransactionImpl) {
                ((TransactionImpl) tran).registerSynchronization(sync, RegisteredSyncs.SYNC_TIER_OUTER);
            } else {
                tran.registerSynchronization(sync);
            }
        } catch (SystemException se) {
            FFDCFilter.processException(se, this.getClass().getName() + ".registerSynchronization", "177", this);
        } catch (RollbackException re) {

            FFDCFilter.processException(re, this.getClass().getName() + ".registerSynchronization", "180", this);
        } catch (IllegalStateException ie) {
            FFDCFilter.processException(ie, this.getClass().getName() + ".registerSynchronization", "182", this);
        }

    }

}
