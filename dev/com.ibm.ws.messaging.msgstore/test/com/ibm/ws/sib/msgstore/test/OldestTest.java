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
package com.ibm.ws.sib.msgstore.test;
/*
 * Change activity:
 *
 * Reason          Date    Origin       Description
 * --------------- ------  --------     ---------------------------------------
 *                 Mar 24, 2003 van Leersum  Original
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * Add some items to a stream and check that we correctly return the oldest.
 * Use different priorities, and continually remove the oldest. 
 * @author drphill
 *
 * <p>.</p>
 */
public class OldestTest extends MessageStoreTestCase
{
    public OldestTest(String name)
    {
        super(name);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        OldestTest test = new OldestTest("testOldest");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    /**
     * Test simple put and get of one item using autocommit transaction
     */
    public void testOldest()
    {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream itemStream = createNonPersistentRootItemStream(messageStore);
        try
        {
            TestItem[] items = new TestItem[50];
            for (int i = 0; i < items.length; i++)
            {
                items[i] = new TestItem();
            }
            Transaction transaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
            for (int i = 0; i < items.length; i++)
            {
                itemStream.addItem(items[i], transaction);
            }
            for (int i = 0; i < 10; i++)
            {
                final Item item = itemStream.findOldestItem();
                item.remove(transaction, item.getLockID());
            }
        }
        catch (Exception e)
        {
            fail(e.toString());
        }
        stopMessageStore(messageStore);
    }

    public static final class TestItem extends Item
    {
        private static int Counter = 0;
        private int counter = Counter++;
        /**
         * Explicit default constructor 
         */
        public TestItem() {}

        public int getPriority()
        {
            return counter % 10;
        }

    }
}
