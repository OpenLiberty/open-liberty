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
package com.ibm.ws.sib.transactions;

import com.ibm.websphere.sib.exception.SIResourceException;

/**
 * Common transaction functionality.  All transaction instances passed to the Core SPI should
 * implement this interface.
 */
public interface TransactionCommon
{
    /**
     * @return The unique transaction ID
     */
    public PersistentTranId getPersistentTranId();
       
    /**
     * @return True if this transaction object currently represents a
     * transaction being tracked by the transaction manager. 
     */
    public boolean isAlive();
       
    /**
     * @return True if (and only if) the transaction is auto commit.
     */
    public boolean isAutoCommit();
       
    /**
     * @return True if (and only if) the transaction has (or can have)
     * subordinates.
     */
    public boolean hasSubordinates();
      
    /**
     * @param callback Registers a transaction lifecycle callback with
     * the transaction.  This will be notified prior to the transaction
     * been completed and after completion of the transaction.
     */
    public void registerCallback(TransactionCallback callback);
       
    /**
     * Increments the current size of the transaction.
     * @throws SIResourceException Thrown if too many operations are
     * performed by the transaction.
     */
    public void incrementCurrentSize() throws SIResourceException;
}
