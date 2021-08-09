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
package com.ibm.ws.sib.msgstore.persistence;

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.transactions.PersistentTranId;

/**
 * The batching context allows objects to be grouped together so that they can be
 * processed in a single batch. Two types of objects are supported, {@link Persistable}
 * objects and {@link PersistentTranId} objects. Processing, as far as the persistence
 * layer is concerned, means that the objects are serialized to the persistent store,
 * e.g. a RDBMS.
 * 
 * @author pradine
 */
public interface BatchingContext {
    /**
     * Set the maximum number of {@link Persistable} objects that can be added
     * to the batch.
     * 
     * @param capacity the number of {@link Persistable} objects.
     */
    public void setCapacity(int capacity);
    
    /**
     * Returns the maximum number of {@link Persistable} objects that can be added
     * to the batch.
     * 
     * @return the number of {@link Persistable} objects.
     */
    public int getCapacity();

    /**
     * Indicate whether JDBC connections will be enlisted in WAS transactions,
     * or not.
     * 
     * @param useEnlistedConnections <code>true</code> to enlist connections,
     * <code>false</code> otherwise.
     */
    public void setUseEnlistedConnections(boolean useEnlistedConnections);

    /**
     * Add a {@link Persistable} that performs the insert operation.
     * 
     * @param tuple the {@link Persistable}
     */
    public void insert(Persistable tuple);
    
    /**
     * Add a {@link Persistable} that performs the update binary data operation.
     * It also has the side effect of updating the size of the data.
     * 
     * @param tuple the {@link Persistable}
     */
    public void updateDataAndSize(Persistable tuple);
    
    /**
     * Add a {@link Persistable} that performs the update lock id operation.
     * 
     * @param tuple the {@link Persistable} 
     */
    public void updateLockIDOnly(Persistable tuple);
    
    /**
     * Add a {@link Persistable} that performs the update redelivery count operation.
     * 
     * @param tuple the {@link Persistable} 
     */
    public void updateRedeliveredCountOnly(Persistable tuple);
 
    /**
     * Add a {@link Persistable} that performs the update the logical delete
     * flag operation. The {@link PersistentTranId} is also set at the same
     * time as this operation is performed during 2PC processing.
     * 
     * @param tuple the {@link Persistable}
     */   
    public void updateLogicalDeleteAndXID(Persistable tuple);
    
    /**
     * Add a {@link Persistable} that performs the delete operation.
     * 
     * @param tuple
     */
    public void delete(Persistable tuple);
    
    /**
     * Add an in-doubt record to the Transaction log
     * 
     * @param xid the {@link PersistentTranId}
     */
    public void addIndoubtXID(PersistentTranId xid);
    
    /**
     * Change the state of an in-doubt record to committed.
     * 
     * @param xid the {@link PersistentTranId}
     */
    public void updateXIDToCommitted(PersistentTranId xid);
    
    /**
     * Change the state of an in-doubt record to rolledback.
     * 
     * @param xid the {@link PersistentTranId}
     */
    public void updateXIDToRolledback(PersistentTranId xid);
    
    /**
     * Delete a record from the Transaction log
     * 
     * @param xid the {@link PersistentTranId}
     */
    public void deleteXID(PersistentTranId xid);
    
    /**
     * Commit the entire batch to the database. No updates will
     * have been performed on the database prior to this method
     * being called.
     *
     */
    public void executeBatch() throws PersistenceException;
    
    /**
     * Delete all objects that have already been added to the batch.
     * No updates are performed on the database.
     * 
     */
    public void clear();
}
