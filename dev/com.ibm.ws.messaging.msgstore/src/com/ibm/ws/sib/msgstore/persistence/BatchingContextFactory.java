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

import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

/**
 * Instances of this interface provide factory methods for creation of instances of
 * {@link BatchingContext}. This makes it easier to plug in alternative batching contexts. 
 */
public interface BatchingContextFactory
{
    /**
     * Returns a new {@link BatchingContext}
     * 
     * @return a new {@link BatchingContext}
     */
    public BatchingContext createBatchingContext();
    
    /**
     * Returns a new {@link BatchingContext}
     * 
     * @param capacity the maximum number of objects to be stored in the {@link BatchingContext}
     * @return a new {@link BatchingContext}
     */
    public BatchingContext createBatchingContext(int capacity);

    /**
     * Returns a new {@link BatchingContext}
     * 
     * @param capacity the maximum number of objects to be stored in the {@link BatchingContext}
     * @param useEnlistedConnections <code>true</code> to enlist connections, <code>false</code> otherwise.
     * @return a new {@link BatchingContext}
     */
    public BatchingContext createBatchingContext(int capacity, boolean useEnlistedConnections);
    
    /**
     * Returns the {@link BatchingContext} associated with a transaction
     *
     * @param transaction the transaction
     * @param capacity the maximum number of objects to be stored in the {@link BatchingContext}
     * @return a {@link BatchingContext}
     */
    public BatchingContext getBatchingContext(PersistentTransaction transaction, int capacity);
    
    /**
     * Returns the {@link BatchingContext} associated with a transaction
     * 
     * @param transaction the transaction
     * @param capacity the maximum number of objects to be stored in the {@link BatchingContext}
     * @param useEnlistedConnections <code>true</code> to enlist connections, <code>false</code> otherwise.
     * @return a {@link BatchingContext}
     */
    public BatchingContext getBatchingContext(PersistentTransaction transaction, int capacity, boolean useEnlistedConnections);    
}
