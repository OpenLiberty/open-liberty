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
/*
 * Change activity:
 *
 * Reason          Date        Origin       Description
 * --------------- ----------  -----------  --------------------------------------------
 *                 27/10/2003  van Leersum  Original
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.putget;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * @author DrPhill
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public final class ItemStream extends com.ibm.ws.sib.msgstore.ItemStream {

    private int _storageStrategy = STORE_NEVER;

    /**
     * 
     */
    public ItemStream() {
        super();
    }

    /**
     * Iterate over the items in the stream and remove them singly
     * using an auto commit transaction.  Do not remove streams.
     * @throws MessageStoreException
     */
    private final void _emptyAuto() throws MessageStoreException {
        Transaction transaction = getOwningMessageStore().getTransactionFactory().createAutoCommitTransaction();
        LockingCursor cursor = newLockingItemCursor(null);
        long lockID = cursor.getLockID();
        AbstractItem item = (AbstractItem) cursor.next();
        try {
            while (null != item) {
                Transaction transaction1 = transaction;
                item.remove(transaction1, cursor.getLockID());
                item = (AbstractItem) cursor.next();
            }
        } catch (Exception e) {
            // unlock the locked item - otherwise it is never accessible until restart
            if (null != item) {
                item.unlock(lockID);
            }
            throw new MessageStoreException(e);
        }
    }

    /** Iterate across contents of stream, removing the items that are not themselves
     * streams.  Remove the items in batches determined by batchsize.
     * @param batchSize
     * @throws Exception
     */
    private final void _emptyLocal(final int batchSize) throws Exception {
        ExternalLocalTransaction uncotran = getOwningMessageStore().getTransactionFactory().createLocalTransaction();

        LockingCursor cursor = newLockingItemCursor(null);
        long lockID = cursor.getLockID();

        int batchCount = 0; // keep track of the number in batch so far
        // keep track of the items locked so far, so we can unlock them after
        // an exception.
        AbstractItem[] lockedLinks = new AbstractItem[batchSize];

        AbstractItem item = (AbstractItem) cursor.next();
        try {
            while (null != item) {
                lockedLinks[batchCount++] = item;
                if (batchSize <= batchCount) {
                    // we have reached batch size, so commit the current transaction
                    // and create a new one.
                    uncotran.commit();
                    batchCount = 0;
                    lockedLinks = new AbstractItem[batchSize];
                    uncotran = getOwningMessageStore().getTransactionFactory().createLocalTransaction();
                }
                item.remove(uncotran, cursor.getLockID());
                item = (AbstractItem) cursor.next();
            }
            uncotran.commit();
        } catch (Exception e) {
            uncotran.rollback();
            // unlock the locked items - otherwise they are never accessible until restart
            for (int i = 0; i < lockedLinks.length; i++) {
                lockedLinks[i].unlock(lockID);
            }
            throw e;
        }
    }

    /**
     * Attempt to empty the receiver by deleting all available items.  
     * This call will not remove ItemStreams, ReferenceStreams or unavailable items.
     * The caller should ascertain the emptiness of the stream
     * after the call has been made.
     * @throws MessageStoreException
     */
    public final void empty() throws MessageStoreException {
        int DEFAULT_BATCH_SIZE = 100;
        try {
            _emptyLocal(DEFAULT_BATCH_SIZE);
        } catch (Exception e) {
            // could be because we have not enough resource for the batch size,
            // - try again with a smaller batch size (one!)
            _emptyAuto();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getStorageStrategy()
     */
    public final int getStorageStrategy() {
        return _storageStrategy;
    }

    public final void setStoreAlways() {
        _storageStrategy = STORE_ALWAYS;
    }

}
