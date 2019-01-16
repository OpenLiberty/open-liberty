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
package com.ibm.ws.sib.msgstore.test.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 *  Not a test in itself - just uses hierarchy for assert statements
 * @author DrPhill
 *
 */
public class PersistentArrangement
{
    private final long _addingItemID;
    private final long _removingItemID;
    private final long _lockedItemID;

    public static final class PersistentItem extends Item
    {
        public PersistentItem()
        {
            super();
        }
        public int getStorageStrategy()
        {
            return STORE_ALWAYS;
        }
    }

    public PersistentArrangement(ItemStream itemStream, Transaction transaction, MessageStore messageStore) throws MessageStoreException 
    {
        //super("PersistentArrangement");
        ExternalAutoCommitTransaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        Item item;
        // create an item in the deleting state
        item = new PersistentItem();
        itemStream.addItem(item, autoTransaction);
        item.remove(transaction, item.getLockID());
        _removingItemID = item.getID();
        // create an item in the deletingLocked state
        item = new PersistentItem();
        itemStream.addItem(item, autoTransaction);
        item.lockItemIfAvailable(1234556789);
        item.persistLock(autoTransaction);
        item.remove(transaction, item.getLockID());
        _lockedItemID = item.getID();
        // create an item in the adding state
        item = new PersistentItem();
        itemStream.addItem(item, transaction);
        _addingItemID = item.getID();
    }

    public final void assertCommitted(ItemStream itemStream, MessageStore messageStore) throws MessageStoreException 
    {
        ExternalAutoCommitTransaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();

        // find the adding item added
        Item item = itemStream.findFirstMatchingItem(null);
        assertNotNull(item);
        assertEquals(item.getID(), _addingItemID);
        // remove the added item
        item = itemStream.removeFirstMatchingItem(null, autoTransaction);

        item = itemStream.removeFirstMatchingItem(null, autoTransaction);
        assertNull(item); // no more items expected

        AbstractItem found = itemStream.findById(_removingItemID);
        assertNull(found);

        found = itemStream.findById(_lockedItemID);
        assertNull(found);
    }

    public final void assertRolledback(ItemStream itemStream, MessageStore messageStore) throws MessageStoreException 
    {
        ExternalAutoCommitTransaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();

        // find the removing item replaced
        Item item = itemStream.findFirstMatchingItem(null);
        assertNotNull(item);
        assertEquals(item.getID(), _removingItemID);
        // replace the removing item
        item = itemStream.removeFirstMatchingItem(null, autoTransaction);

        item = itemStream.removeFirstMatchingItem(null, autoTransaction);
        assertNull(item); // no more items expected

        AbstractItem found = itemStream.findById(_addingItemID);
        assertNull(found);

        found = itemStream.findById(_lockedItemID);
        assertNotNull(found);
        found.unlock(found.getLockID(), autoTransaction);
        item = itemStream.removeFirstMatchingItem(null, autoTransaction);
        assertNotNull(item);
    }

}
