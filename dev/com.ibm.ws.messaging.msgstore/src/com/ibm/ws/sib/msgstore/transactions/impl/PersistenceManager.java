package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.sib.msgstore.*;

/**
 * This interface provides the methods used by the transaction layer to carry 
 * out all of its persistent work.
 */
public interface PersistenceManager
{
    /**
     * Returns whether 1PC optimisation is supported by this PersistenceManager. This is only
     * the case for a PersistenceManager which uses the Relational Resource Adapter to obtain
     * database connections enlisted with the Transaction Service.
     *
     * @return whether 1PC optimisation is supported by this PersistenceManager
     */
    public boolean supports1PCOptimisation();

    /**
     * This method will be called when we are being optimized to
     * use the same database connection as the EJB PersistenceManager
     * and will signify to the PersistenceManager implementation that
     * an enlistable/shareable connection must be used.
     * 
     * @param transaction
     *               The transaction whose members need persisting to the datastore.
     * 
     * @exception PersistenceException
     *                   Thrown if an error occurs whilst persisting the data
     */
    public void beforeCompletion(PersistentTransaction transaction) throws PersistenceException,SevereMessageStoreException;

    /**
     * This method is only called when the MessageStore is involved in an
     * XA global transaction and causes the flushing of the transactions
     * contents to the persistent datastore.
     * 
     * @param transaction
     *               The transaction whose members need persisting to the datastore.
     * 
     * @exception PersistenceException
     *                   Thrown if an error occurs whilst persisting the data
     */
    public void prepare(PersistentTransaction transaction) throws PersistenceException,SevereMessageStoreException;


    /**
     * This method has two uses:
     * <BR>one-phase-commit - All work needed to persist the contents of the transaction to 
     * the datastore is carried out.
     * <BR>two-phase-commit - Completes the work started in the prepare stage by clearing the 
     * transactions indoubt state in the datastore.
     * 
     * @param transaction
     *                 The transaction whose members need persisting to the datastore.
     * @param onePhase Whether this commit call is:
     *                 <UL>
     *                 <LI>true - The single phase of a one-phase-commit transaction</LI>
     *                 <LI>false - The second phase of a two-phase-commit transaction</LI>
     *                 </UL>
     * 
     * @exception PersistenceException
     *                   Thrown if an error occurs whilst persisting the data
     */
    public void commit(PersistentTransaction transaction, boolean onePhase) throws PersistenceException,SevereMessageStoreException;


    /**
     * Revert the datastore to the state it was in before the work involved in this
     * transaction was carried out. This method will only be called if previous work 
     * has been carried out in the prepare stage of a two-phase transaction.
     * 
     * @param transaction
     *               The transaction whose members need persisting to the datastore.
     * 
     * @exception PersistenceException
     *                   Thrown if an error occurs whilst persisting the data
     */
    public void rollback(PersistentTransaction transaction) throws PersistenceException,SevereMessageStoreException;


    /**
     * This method is called after completion of the transaction has occurred in
     * the optimization case in order to carry out any neccessary clean-up.
     * 
     * @param transaction
     *                  The transaction that needs cleaning up.
     * @param committed The completion direction of the transaction:
     *                  <UL>
     *                  <LI>true - Committed</LI>
     *                  <LI>false - Rolled-back</LI>
     *                  </UL>
     */
    public void afterCompletion(PersistentTransaction transaction, boolean committed) throws SevereMessageStoreException;
}
