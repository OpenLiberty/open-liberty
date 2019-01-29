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
 * Reason          Date    Origin       Description
 * --------------- ------  --------     ---------------------------------------
 *                 Mar 24, 2003 van Leersum  Original
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * These tests add and remove one/many items to/from item streams using
 * both local and autoCommit transactions.
 * 
 * ---------------- Summary -------------------------
 * 01  Add and remove ONE item using autoCommit transaction
 * 02  Add and remove ONE item using local transaction
 * 03  Add and remove MANY items using AutoCommit transaction
 * 04  Add and remove MANY items using local transaction
 * 
 * @author drphill
 *
 * <p>.</p>
 */
public class GetCursorAndRemoveTest extends MessageStoreTestCase
{
    public GetCursorAndRemoveTest(String name)
    {
        super(name);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        GetCursorAndRemoveTest test = new GetCursorAndRemoveTest("testGetCursorAndRemove");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    /**
     * Test simple put and get of one item using autocommit transaction
     */
    public void testGetCursorAndRemove()
    {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream itemStream = createNonPersistentRootItemStream(messageStore);
        try
        {
            long initialSize = itemStream.getStatistics().getTotalItemCount();

            Transaction transaction = messageStore.getTransactionFactory().createAutoCommitTransaction();

            Item putItem = new Item();
            itemStream.addItem(putItem, transaction);
            long finalSize = itemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize + 1);

            LockingCursor cursor = itemStream.newLockingItemCursor(null);

            AbstractItem gotItem = cursor.next();
            assertNotNull(gotItem);
            assertEquals(putItem, gotItem);

            gotItem.remove(transaction, gotItem.getLockID());

            finalSize = itemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize);
        }
        catch (Exception e)
        {
            fail(e.toString());
        }
        stopMessageStore(messageStore);
    }

}
