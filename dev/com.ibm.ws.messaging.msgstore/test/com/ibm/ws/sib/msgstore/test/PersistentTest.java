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
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * These tests are designed to Add and remove items to and from persistent
 * item streams. The message store is stopped and restarted during each test.
 * 
 * ---------------------- Summary -------------------------
 * 01 Add and remove ONE item using local transaction
 * 02 Add and remove ONE item using AutoCommit transaction
 * 03 Add and remove MANY items using local transaction
 * 04 Add and remove MANY items using AutoCommit transaction
 * 
 * @author drphill
 * 
 *         <p>.</p>
 */
public class PersistentTest extends MessageStoreTestCase {
    private static final int MANY = 100;

    public PersistentTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        // Commenting out this temporarily - Kavitha

        /*
         * PersistentTest test = new PersistentTest("testOneAutoTx");
         * test.setPersistence(persistence);
         * suite.addTest(test);
         */

        PersistentTest test = new PersistentTest("testOneLocalTx");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new PersistentTest("testManyAutoTx");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new PersistentTest("testManyLocalTx");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    // Test simple put and get of one item using autocommit transaction
/*
 * public void testOneAutoTx() {
 * MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
 * ItemStream rootItemStream = createPersistentRootItemStream(messageStore);
 * try {
 * long initialSize = rootItemStream.getStatistics().getTotalItemCount();
 * 
 * Transaction transaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
 * 
 * Item putItem = new Item();
 * rootItemStream.addItem(putItem, transaction);
 * long finalSize = rootItemStream.getStatistics().getTotalItemCount();
 * 
 * assertEquals(finalSize, initialSize + 1);
 * 
 * // recycle the message store
 * stopMessageStore(messageStore);
 * messageStore = createAndStartMessageStore(false, PERSISTENCE);
 * transaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
 * 
 * // reget the itemStream
 * rootItemStream = messageStore.findFirstMatching(null);
 * assertNotNull("Item stream  not found", rootItemStream);
 * 
 * Item gotItem = rootItemStream.removeFirstMatchingItem(null, transaction);
 * 
 * assertNotNull(gotItem);
 * assertEquivalent(putItem, gotItem);
 * 
 * finalSize = rootItemStream.getStatistics().getTotalItemCount();
 * assertEquals(finalSize, initialSize);
 * } catch (Exception e) {
 * e.printStackTrace();
 * fail(e.toString());
 * }
 * stopMessageStore(messageStore);
 * }
 */

    /**
     * Test simple put and get of one item using local transaction
     */
    public void testOneLocalTx() {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream rootItemStream = createPersistentRootItemStream(messageStore);
        try {
            long initialSize = rootItemStream.getStatistics().getTotalItemCount();
            ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

            Item putItem = new PersistentItem();

            rootItemStream.addItem(putItem, uncotran);
            uncotran.commit();

            long finalSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize + 1);

            uncotran = messageStore.getTransactionFactory().createLocalTransaction();

            Item gotItem = rootItemStream.removeFirstMatchingItem(null, uncotran);
            assertNotNull(gotItem);

            assertEquivalent(putItem, gotItem);
            uncotran.commit();

            finalSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize);
        } catch (Exception e) {
            fail(e.toString());
        }
        stopMessageStore(messageStore);
    }

    /**
     * Test simple put and get of many items using autoCommit transaction
     */
    public void testManyAutoTx() {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream rootItemStream = createPersistentRootItemStream(messageStore);
        try {
            Item[] putItems = new Item[MANY];
            Item[] gotItems = new Item[MANY];
            for (int i = 0; i < putItems.length; i++) {
                putItems[i] = new PersistentItem();
            }
            long initialSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(initialSize, 0);

            Transaction transaction = messageStore.getTransactionFactory().createAutoCommitTransaction();

            for (int i = 0; i < putItems.length; i++) {
                rootItemStream.addItem(putItems[i], transaction);
            }

            long finalSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize + MANY);

            int gotCount = 0;
            Item gotItem = rootItemStream.removeFirstMatchingItem(null, transaction);
            while (null != gotItem) {
                if (gotCount < MANY) {
                    gotItems[gotCount] = gotItem;
                } else {
                    fail("too many items");
                }
                gotCount++;
                gotItem = rootItemStream.removeFirstMatchingItem(null, transaction);
            }

            for (int i = 0; i < putItems.length; i++) {
                assertEquivalent(putItems[i], gotItems[i]);
            }

            finalSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize);

        } catch (Exception e) {
            fail(e.toString());
        }
        stopMessageStore(messageStore);
    }

    /**
     * Test simple put and get of many items using local transaction
     */
    public void testManyLocalTx() {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream rootItemStream = createPersistentRootItemStream(messageStore);
        try {
            Item[] putItems = new Item[MANY];
            Item[] gotItems = new Item[MANY];
            for (int i = 0; i < putItems.length; i++) {
                putItems[i] = new PersistentItem();
            }
            long initialSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(initialSize, 0);
            ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

            for (int i = 0; i < putItems.length; i++) {
                rootItemStream.addItem(putItems[i], uncotran);
            }
            uncotran.commit();

            long finalSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize + MANY);

            uncotran = messageStore.getTransactionFactory().createLocalTransaction();

            int gotCount = 0;
            Item gotItem = rootItemStream.removeFirstMatchingItem(null, uncotran);
            while (null != gotItem) {
                if (gotCount < MANY) {
                    gotItems[gotCount] = gotItem;
                } else {
                    fail("too many items");
                }
                gotCount++;
                gotItem = rootItemStream.removeFirstMatchingItem(null, uncotran);
            }
            uncotran.commit();

            for (int i = 0; i < putItems.length; i++) {
                assertEquivalent(putItems[i], gotItems[i]);
            }

            finalSize = rootItemStream.getStatistics().getTotalItemCount();
            assertEquals(finalSize, initialSize);

        } catch (Exception e) {
            fail(e.toString());
        }
        stopMessageStore(messageStore);
    }
}
