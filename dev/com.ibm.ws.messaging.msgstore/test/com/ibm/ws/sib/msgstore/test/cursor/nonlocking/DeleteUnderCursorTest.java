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
package com.ibm.ws.sib.msgstore.test.cursor.nonlocking;
/*
 * Change activity:
 *
 * Reason          Date     Origin   Description
 * ------------- --------  -------- -------------------------------------------
 *               26/06/03  drphill  Original
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * Testcases are almost duplicated because we use either an autoCommit 
 * transaction or a local transaction. When we use a local transaction
 * we complicate things by doing a rollback.
 * 
 * There are variants on get:
 * <ul> 
 *  <li>remove/find
 *   <ul> 
 *    <li>Remove - uses removeFirstMatching</li>
 *    <li>Find - uses findFirstMatching followed by a remove
 *        directly on the found item.</li>
 *   </ul>
 *  </li>
 *  <li>Skipping/nonSkipped</li>
 *   <ul>
 *    <li>NonSkipped - use no filter (null), so every item is returned in order.</li>
 *    <li>Use a filter that matches items whose id is a multiple of three, then
 *        a filter matching even ids, then a filter that matches all ids.</li>
 *   </ul>
 * </ul>  
 *
 * <p> We also do some tests upon constraints on put/get:
 * <ul>
 *  <li>Cannot put item into item stream until item stream is avaialble
 *      unless the puts are under same transaction.</li>
 *  <li>Cannot delete non-empty item stream, unless the items in the
 *      stream are being deleted under the same transaction as the 
 *      stream.</li>
 * </ul> 
 * </p>
 * 
 * <p>We also do some tests where we mark each item as we match it.
 * This allows us to check that cursors correctly traverse unlocked items.</p>
 */
public class DeleteUnderCursorTest extends MessageStoreTestCase
{
    private MessageStore messageStore;

    public DeleteUnderCursorTest(String arg0)
    {
        super(arg0);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        DeleteUnderCursorTest test = new DeleteUnderCursorTest("testDeleteUnderCursor");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public final void testDeleteUnderCursor()
    {
        ItemStream itemStream = null;
        try
        {
            print("PersistenceManager used: "+PERSISTENCE);
            messageStore = createAndStartMessageStore(true, PERSISTENCE);

            itemStream = new ItemStream();
            ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
            messageStore.add(itemStream, localtran);
            localtran.commit();

            deleteLast(itemStream);
            itemStream.empty();
            deleteMiddle(itemStream);
            itemStream.empty();
            deleteAllWhileInMiddle(itemStream);
            itemStream.empty();
            deleteEndWhileInMiddle(itemStream);
            itemStream.empty();
            deleteAllWhileAtEnd(itemStream);
            itemStream.empty();
            deleteEndWhileAtEnd(itemStream);
            itemStream.empty();
            ignoreRollback1(itemStream);
            itemStream.empty();
            ignoreRollback2(itemStream);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail("failed: " + e);
        }
        finally
        {
            if (null != messageStore)
            {
                stopMessageStore(messageStore);
            }
        }
    }

    /**
     * Delete the last element while a cursor is pointing to it.  Show
     * that the cursor behaves correctly: returns null until new 
     * item added.
     */
    private final void deleteLast(ItemStream itemStream) throws Exception 
    {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();

        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the last item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
        Item lastItem = items[items.length - 1];
        Item item = (Item) cursor.next();
        while (lastItem != item)
        {
            item = (Item) cursor.next();
        }

        // delete the last item and add another
        int lastIndex = items.length - 1;
        items[lastIndex].remove(autoTransaction, items[lastIndex].getLockID());
        items[lastIndex] = new Item();
        itemStream.addItem(items[lastIndex], autoTransaction);
        lastItem = items[lastIndex];

        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, lastItem);
        // check that we see end of list
        //item = (Item) cursor.next();
        //assertNull(item);

        // remove all items and check we get null
        for (int i = 0; i < items.length; i++)
        {
            items[i].remove(autoTransaction, items[i].getLockID());
        }
        item = (Item) cursor.next();
        assertNull(item);
    }

    /**
     * Delete an item not-at-end while a cursor is looking at it.
     * Show that the cursor behaves as required (sees next item correctly.
     * @param itemStream
     * @throws Exception
     */
    private final void deleteMiddle(ItemStream itemStream) throws Exception {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the middle item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);

        Item item = (Item) cursor.next();
        while (item != items[2])
        {
            item = (Item) cursor.next();
        }
        // delete middle item
        items[2].remove(autoTransaction, items[2].getLockID());

        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, items[3]);

    }

    /** Delete all the items while a cursor is pointing at one in
     * the middle.
     * Show that the cursor behaves correctly - returns null until
     * a new item is added.
     * @param itemStream
     * @throws Exception
     */
    private final void deleteAllWhileInMiddle(ItemStream itemStream) throws Exception {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the middle item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);

        Item item = (Item) cursor.next();
        while (item != items[2])
        {
            item = (Item) cursor.next();
        }
        // delete middle item

        for (int i = 0; i < items.length; i++)
        {
            items[i].remove(autoTransaction, items[i].getLockID());
        }

        // check that we see stream as empty
        item = (Item) cursor.next();
        assertNull(item);

        Item newItem = new Item();
        itemStream.addItem(newItem, autoTransaction);
        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, newItem);
    }

    /** Delete the last items (but not the first) while a cursor 
     * is pointing at one in the middle. (ie at first one deleted).
     * Show that the cursor behaves correctly - returns null until
     * a new item is added.
     * @param itemStream
     * @throws Exception
     */
    private final void deleteEndWhileInMiddle(ItemStream itemStream) throws Exception {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the middle item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);

        Item item = (Item) cursor.next();
        while (item != items[2])
        {
            item = (Item) cursor.next();
        }

        for (int i = 2; i < items.length; i++)
        {
            items[i].remove(autoTransaction, items[i].getLockID());
        }

        // check that we see stream as empty
        item = (Item) cursor.next();
        assertNull(item);

        Item newItem = new Item();
        itemStream.addItem(newItem, autoTransaction);
        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, newItem);

    }

    /** Delete the last items (but not the first) while a cursor 
     * is pointing the last one.
     * Show that the cursor behaves correctly - returns null until
     * a new item is added.
     * @param itemStream
     * @throws Exception
     */
    private final void deleteEndWhileAtEnd(ItemStream itemStream) throws Exception {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the last item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);

        Item item = (Item) cursor.next();
        while (item != items[items.length - 1])
        {
            item = (Item) cursor.next();
        }

        for (int i = 2; i < items.length; i++)
        {
            items[i].remove(autoTransaction, items[i].getLockID());
        }

        // check that we see stream as empty
        item = (Item) cursor.next();
        assertNull(item);

        Item newItem = new Item();
        itemStream.addItem(newItem, autoTransaction);
        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, newItem);
    }

    /** Delete all the items while a cursor is pointing at the
     * last one.
     * Show that the cursor behaves correctly - returns null until
     * a new item is added.
     * @param itemStream
     * @throws Exception
     */
    private final void deleteAllWhileAtEnd(ItemStream itemStream) throws Exception {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the last item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);

        Item item = (Item) cursor.next();
        while (item != items[items.length - 1])
        {
            item = (Item) cursor.next();
        }

        for (int i = 0; i < items.length; i++)
        {
            items[i].remove(autoTransaction, items[i].getLockID());
        }

        // check that we see stream as empty
        item = (Item) cursor.next();
        assertNull(item);

        Item newItem = new Item();
        itemStream.addItem(newItem, autoTransaction);
        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, newItem);
    }

    /** 
     * 1) add five items (0..4)
     * 2) progress cursor to item 3
     * 3) delete items 2,3,4 under a transaction2
     * 4) delete item2 under LocalTransaction1, but do not commit
     * 5) rollback localTransaction1
     * 6) add item 5
     * 7) next on cursor should be item 5
     */
    private final void ignoreRollback1(ItemStream itemStream) throws Exception {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the middle item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);

        Item item = (Item) cursor.next();
        while (item != items[3])
        {
            item = (Item) cursor.next();
        }


        for (int i = 2; i < items.length; i++)
        {
            items[i].remove(autoTransaction, items[i].getLockID());
        }

        ExternalLocalTransaction localTransaction = messageStore.getTransactionFactory().createLocalTransaction();
        items[1].remove(localTransaction, items[1].getLockID());
        localTransaction.rollback();

        Item newItem = new Item();
        itemStream.addItem(newItem, autoTransaction);
        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, newItem);
    }

    /** 
     * 1) add five items (0..4)
     * 2) progress cursor to item 3
     * 3) delete item1 under LocalTransaction1, but do not commit
     * 4) delete items 2,3,4 under a transaction2
     * 5) rollback localTransaction1
     * 6) add item 5
     * 7) next on cursor should be item 5
     */
    private final void ignoreRollback2(ItemStream itemStream) throws Exception {
        Transaction autoTransaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        // add some items to stream
        Item[] items = new Item[5];
        for (int i = 0; i < items.length; i++)
        {
            items[i] = new Item();
            itemStream.addItem(items[i], autoTransaction);
        }

        // create a cursor and point it to the middle item
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);

        Item item = (Item) cursor.next();
        while (item != items[3])
        {
            item = (Item) cursor.next();
        }

        ExternalLocalTransaction localTransaction = messageStore.getTransactionFactory().createLocalTransaction();
        items[1].remove(localTransaction, items[1].getLockID());

        for (int i = 2; i < items.length; i++)
        {
            items[i].remove(autoTransaction, items[i].getLockID());
        }

        localTransaction.rollback();

        Item newItem = new Item();
        itemStream.addItem(newItem, autoTransaction);
        // check that we see the new item
        item = (Item) cursor.next();
        assertEquals(item, newItem);

    }
}
