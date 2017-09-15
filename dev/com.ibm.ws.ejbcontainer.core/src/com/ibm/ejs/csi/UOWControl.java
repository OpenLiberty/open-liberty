/*******************************************************************************
 * Copyright (c) 2002, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import java.rmi.RemoteException;

import javax.transaction.Synchronization;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.csi.CSIActivitySessionResetException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.EJBKey;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.csi.TxContextChange;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

/**
 * <code>UOWControl</code> is a dual purpose interface. It isolates the EJB
 * container from all interactions with the activitySession, global transaction,
 * and local transaction services. It also provides collaborator functionality
 * for maintaining activitySession and transaction contexts at preInvoke and
 * postInvoke for dispatch of EJB methods.
 */
public interface UOWControl
{
    /**
     * <code>preInvoke</code> must be called by the container prior
     * to dispatching a method to an EJB instance. It is responsible
     * for ensuring that on return the current activity session context
     * is appropriate for the given <code>ActivitySessionAttribute<code>
     * and the current transaction context is appropriate for the given
     * <code>TransactionAttribute</code>. <p>
     * 
     * @param key an <code>EJBKey</code> instance corresponding to the
     *            bean instance the method call will be delegated to <p>
     * 
     * @param methodInfo the <code>EJBMethodInfo</code> for the
     *            method being invoked <p>
     * 
     * @return <code>UOWCookie</code> instance that container must
     *         guarantee is passed to the corresponding
     *         <code>postInvoke</code>
     * 
     * @exception RemoteException thrown if an error occurs while performing
     *                <code>preInvoke</code> processing; in general, if an exception
     *                is raised then the preinvoke processing for the given method
     *                has failed. The container will not call postInvoke in this case.
     */

    public UOWCookie preInvoke(EJBKey key, EJBMethodInfoImpl methodInfo)
                    throws RemoteException;

    /**
     * <code>postInvoke</code> must be called by the container after
     * dispatching a method to an EJB. It is responsible for ensuring
     * that on return the current activity session and transaction
     * contexts are appropriate. <p>
     * 
     * Every call to <code>postInvoke</code> must be associated with
     * exactly one call to <code>preInvoke</code>, i.e. the container
     * must never call <code>postInvoke</code> without first calling
     * <code>preInvoke</code>. <p>
     * 
     * @param key an <code>EJBKey</code> instance indicating the EJB that
     *            the method was invoked on; must be identical to the
     *            <code>EJBKey</code> instance passed to the corresponding
     *            <code>preInvoke</code> <p>
     * 
     * @param UOWCookie the <code>UOWCookie</code> instance that the
     *            corresponding <code>preInvoke</code> method returned.<p>
     * 
     * @param exType an <code>ExceptionType</code> instance indicating
     *            what type, if any, of exception was raised during method
     *            invocation <p>
     * 
     * @param methodInfo the <code>EJBMethodInfo</code> for the
     *            method invoked <p>
     * 
     * @exception RemoteException thrown if an error occurs while performing
     *                <code>postInvoke</code> processing; in general, if an exception
     *                is raised then the postinvoke processing for the given method
     *                has failed, however, the <code>UOWControl</code> instance
     *                itself must remain usable <p>
     */

    public void postInvoke(EJBKey key, UOWCookie uowCookie,
                           ExceptionType exType, EJBMethodInfoImpl methodInfo)
                    throws RemoteException;

    /**
     * Return true iff the current local or global transaction has been
     * marked for rollback. <p>
     */

    public boolean getRollbackOnly();

    /**
     * Mark the current local or global tranasction for rollback. <p>
     */

    public void setRollbackOnly();

    /**
     * Return a unique <code>Object</code> instance for the transactional unit
     * of work associated with the current thread. This UOW may be a global
     * transaction, or a local transaction, or null (ie. if no tranaction
     * exists). <p>
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
                    throws CSITransactionRolledbackException;

    /**
     * Return a unique <code>Object</code> instance for the sessional unit of
     * work associated with the current thread. This UOW may be an activitySession,
     * or null (ie. if no activitySession exists). <p>
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
                    throws CSIActivitySessionResetException;

    /**
     * Return a <code>UserTransaction</code> instance that may used
     * for demarcating bean-managed global transactions. <p>
     */

    public UserTransaction getUserTransaction();

    /**
     * Enlist the given synchronization instance with the current
     * transactional UOW. <p>
     * 
     * @param interestedParty the <code>Synchronization</code> instance
     *            to enlist with the transaction. <p>
     */

    public void enlistWithTransaction(Synchronization interestedParty)
                    throws CSIException;

    // 131880-6
    public void enlistWithTransaction(SynchronizationRegistryUOWScope uowCoord, Synchronization interestedParty)
                    throws CSIException;

    /**
     * Enlist the given synchronization instance with the current
     * sessional UOW. <p>
     * 
     * @param interestedParty the <code>Synchronization</code> instance
     *            to enlist with the activitySession. <p>
     * 
     */

    public void enlistWithSession(Synchronization interestedParty)
                    throws CSIException;

    /**
     * Notification from the Container that the current ActivitySession ended.
     * This is used to release the collaborator's references to any sticky local
     * tx contexts (ie. local tx boundary = ActivitySession). <p>
     * 
     * @param EjbKeyArray is an array of <code>EJBKey</code> instances, representing
     *            the beans which have been enlisted with the activitySession. Each of
     *            these beans could possibly have a sticky local tx. <p>
     */

    public void sessionEnded(EJBKey[] EjbKeyArray)
                    throws CSIException;

    /**
     * Ensure that the local transaction context for the bean instance
     * represented by the given EJBKey is active on the thread. With
     * the introduction of ActivitySessions in R5.0 the bean may have
     * a sticky local tx context which needs to be resumed. <p>
     * 
     * @param key is the <code>EJBKey</code> for the bean. <p>
     * 
     */

    public TxContextChange setupLocalTxContext(EJBKey key) throws CSIException;

    /**
     * Restore any local transaction context change made by the
     * setupLocalTxContext method. <p>
     * 
     * @param changedContext is the <code>TxContextChange</code> instance
     *            which contains details about any context change made by the
     *            matching setupLocalTxContext method call. <p>
     */

    public void teardownLocalTxContext(TxContextChange changedContext);

    /**
     * Return boolean true if there is an active BeanManaged Transaction
     * currently associated with the calling thread.
     */
    //167937 - added entire method
    public boolean isBmtActive(EJBMethodInfoImpl methodInfo);

    /**
     * Return boolean true if there is an active BeanManaged ActivitySession
     * currently associated with the calling thread.
     */
    //LIDB2018-1 added entire method
    public boolean isBmasActive(EJBMethodInfoImpl methodInfo);

    /**
     * This method allows a UOWControl impl to suspend the current
     * transaction (local or global) or an ActivitySession should one exist.
     * 
     * @return A <code>UOWHandle</code> will be returned which containes the suspended
     *         ActivitySession or transaction if one was suspended, null otherwise.
     */
    //PK15508: added entire method
    public UOWHandle suspend() throws CSIException;

    /**
     * This method allows a UOWControl impl to resume a transaction (local
     * or global) or an ActivitySession that was previously suspended.
     * 
     * @param handle A <code>UOWHandle</code> that contains the suspended ActivitySession or
     *            transaction. A null handle means that on the corresponding call to suspend
     *            there was nothing to suspend.
     */
    //PK15508: added entire method
    public void resume(UOWHandle handle) throws CSIException;

    /**
     * Check if the active transaction has timed out.
     * 
     * @throws CSITransactionRolledbackException if the transaction has timed out
     */
    // F61004.1
    public void completeTxTimeout()
                    throws CSITransactionRolledbackException;
}
