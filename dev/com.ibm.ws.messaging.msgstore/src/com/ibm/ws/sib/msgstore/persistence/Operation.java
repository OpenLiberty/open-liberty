package com.ibm.ws.sib.msgstore.persistence;
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

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

/**
 * These are operations that must be provided by the Cache layer to the
 * Persistence layer.
 */
public interface Operation 
{
    /**
     * Returns a {@link Persistable} object for processing.
     * 
     * @return a {@link Persistable} object
     */
    public Persistable getPersistable();

    /**
     * Returns the approximate in memory size of {@link Persistable}.
     * 
     * @param tranState the state of the transaction associated with the operation
     * @return the size
     */
    public int getPersistableInMemorySizeApproximation(TransactionState tranState);
    
    /**
     * This is the mechanism via which a {@link Persistable} that carries the data
     * related to a write operation is passed to the Persistence layer for processing.
     * 
     * @param bc        the {@link BatchingContext} via which the {@link Persistable} will be serialized to the database.
     * @param tranState the state of the transaction associated with the operation
     */
    public void persist(BatchingContext bc, TransactionState tranState);
    
    /**
     * The data in a {@link Persistable} is copied in order to preserve its state
     * in case it may be overwritten. A copy is not required provided that the data
     * is immutable.
     *
     */
    public void copyDataIfVulnerable() throws PersistenceException, SevereMessageStoreException;
    
    /**
     * Ensures that the data in a {@link Persistable} is available for writing by the
     * {@link Operation} irrespective of timing considerations with respect to the
     * actual time of the write.
     *
     */
    public void ensureDataAvailable() throws PersistenceException, SevereMessageStoreException;
    
    /**
     * Indicates whether the operation to be performed will create a persistent
     * representation in the database.
     * 
     * @return <code>true</code> if it is a create (insert) operation, <code>false</code> otherwise.
     */
    public boolean isCreateOfPersistentRepresentation();
    
    /**
     * Indicates whether the operation to be performed will delete a persistent
     * representation in the database.
     * 
     * @return <code>true</code> if it is a delete operation, <code>false</code> otherwise.
     */
    public boolean isDeleteOfPersistentRepresentation(); 
}
