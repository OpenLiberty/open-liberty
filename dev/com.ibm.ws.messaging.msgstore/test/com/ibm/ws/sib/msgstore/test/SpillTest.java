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
 * Reason          Date        Origin       Description
 * --------------- ----------  -----------  --------------------------------------------
 *                 27/10/2003  van Leersum  Original
 * 291186          08/11/2005  schofiel     Test case logical error fixed                
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

public class SpillTest extends MessageStoreTestCase implements MessageStoreConstants {
    public SpillTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        SpillTest test = new SpillTest("testSpill");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public void testSpill() {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);

        //  turnOnTrace();
        //  configureTrace("com.ibm.ws.sib.msgstore.cache.statemodel.ListStatistics=all=enabled");
        try {

            _cycle(messageStore, 20, 2);
            print("\n------------------------------------\n");
            _cycle(messageStore, 40, 4);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
        stopMessageStore(messageStore);
    }

    private void _cycle(MessageStore messageStore, int upper, int lower) throws MessageStoreException {
        System.setProperty(STANDARD_PROPERTY_PREFIX + PROP_SPILL_UPPER_LIMIT, Integer.toString(upper));
        System.setProperty(STANDARD_PROPERTY_PREFIX + PROP_SPILL_LOWER_LIMIT, Integer.toString(lower));

        Transaction transaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        ItemStream itemStream = new ItemStream();
        messageStore.add(itemStream, transaction);
        assertFalse("should not be spilling before addition of items", itemStream.isSpilling());

        itemStream.add(upper * 2);
        assertTrue("should be spilling after addition of items", itemStream.isSpilling());

        itemStream.empty();
        assertFalse("should not be spilling after removal of items", itemStream.isSpilling());

        itemStream.add(upper * 2);
        assertTrue("should be spilling after addition of items", itemStream.isSpilling());

        itemStream.empty();
        assertFalse("should not be spilling after removal of items", itemStream.isSpilling());
    }

    public static class Item extends com.ibm.ws.sib.msgstore.Item {
        @Override
        public int getStorageStrategy() {
            return STORE_MAYBE;
        }
    }

    public static class ItemStream extends com.ibm.ws.sib.msgstore.ItemStream {
        @Override
        public int getStorageStrategy() {
            return STORE_MAYBE;
        }

        public final void empty() throws MessageStoreException {
            Transaction transaction = getOwningMessageStore().getTransactionFactory().createAutoCommitTransaction();
            while (null != removeFirstMatchingItem(null, transaction));
        }

        public final void add(int count) throws MessageStoreException {
            Transaction transaction = getOwningMessageStore().getTransactionFactory().createAutoCommitTransaction();
            for (int i = 0; i < count; i++) {
                addItem(new Item(), transaction);
            }
        }
    }
}
