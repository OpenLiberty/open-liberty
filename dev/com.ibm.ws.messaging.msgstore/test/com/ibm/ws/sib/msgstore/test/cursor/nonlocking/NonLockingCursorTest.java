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

import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * These tests do browse operations.  Effectively put 100 items
 * with priorities 0,1,2,3,4,5,6,7,8,9,0,1,2,3........ and then get them
 * again.
 * 
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
public class NonLockingCursorTest extends MessageStoreTestCase
{
    private long _baseTime;
    private MessageStore messageStore;

    public NonLockingCursorTest(String arg0)
    {
        super(arg0);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        NonLockingCursorTest test = new NonLockingCursorTest("testNonLockingCursor");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public final void testNonLockingCursor()
    {
        ItemStream itemStream = null;
        _baseTime = System.currentTimeMillis();
        try
        {
            print("PersistenceManager used: "+PERSISTENCE);
            _report("testNonLockingCursor starting messageStore (" + _baseTime + ")");
            messageStore = createAndStartMessageStore(true, PERSISTENCE);

            _report("testNonLockingCursor creating itemStream (" + (System.currentTimeMillis() - _baseTime) + ")");
            itemStream = new ItemStream();
            ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
            messageStore.add(itemStream, localtran);
            localtran.commit();

            findAndRemoveAuto(itemStream);
            findAndRemoveLocal(itemStream);

            findAndRemoveAutoSkipping(itemStream);
            findAndRemoveLocalSkipping(itemStream);

            findAndRemoveLocalInterleaved(itemStream);

            appendRemoveAuto(itemStream);
            appendRemoveLocal(itemStream);
            appendRemoveLocalSkipping(itemStream);

            appendNonSkipping(itemStream);
            itemStream.empty();

            appendNonSkippingMark(itemStream);
            itemStream.empty();
            appendSkippingMark(itemStream);
            itemStream.empty();
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
        _report("testNonLockingCursor finished (" + (System.currentTimeMillis() - _baseTime) + ")");
    }


    private final void _addItems(ItemStream itemStream) throws Exception 
    {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        for (int i = 0; i < 10; i++)
        {
            for (int j = 0; j < 10; j++)
            {
                Item item = new Item(j);
                itemStream.addItem(item, localtran);
            }
        }
        localtran.commit();
    }
    private final void _appendItems(ItemStream itemStream) throws Exception {
        ExternalLocalTransaction t = messageStore.getTransactionFactory().createLocalTransaction();
        for (int i = 0; i < 10; i++)
        {
            for (int j = 0; j < 10; j++)
            {
                Item item = new Item(j);
                itemStream.addItem(item, t);
            }
        }
        t.commit();
    }

    /** Wrapper for assertion to make sure I get it the right way round!
     * @param i
     */
    private final void _assertAll(int i)
    {
        assertEquals(100, i);
    }
    private final int _find(NonLockingCursor cursor) throws MessageStoreException {
        int count = 0;
        Item oldItem = (Item) cursor.next();
        while (null != oldItem)
        {
            count++;
            Item newItem = (Item) cursor.next();
            if (null != newItem)
            {
                assertTrue(newItem.isAfter(oldItem));
            }
            oldItem = newItem;
        }
        return count;
    }
    private final int _findAndMark(NonLockingCursor cursor) throws MessageStoreException {
        int count = 0;
        Item oldItem = (Item) cursor.next();
        while (null != oldItem)
        {
            count++;
            oldItem.setMarked(true);
            Item newItem = (Item) cursor.next();
            if (null != newItem)
            {
                assertTrue(newItem.isAfter(oldItem));
            }
            oldItem = newItem;
        }
        return count;
    }

    private final int _nonSkippingFindAndRemove(ItemStream itemStream, Transaction transaction)
    throws MessageStoreException {
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
        return _nonSkippingFindAndRemove(transaction, cursor);
    }

    private final int _nonSkippingFindAndRemove(Transaction transaction, NonLockingCursor cursor)
    throws MessageStoreException {
        _report("........nonSkippingFind");
        int count = 0;
        Item oldItem = (Item) cursor.next();
        while (null != oldItem)
        {
            count++;
            oldItem.remove(transaction, oldItem.getLockID());
            Item newItem = (Item) cursor.next();
            if (null != newItem)
            {
                assertTrue(newItem.isAfter(oldItem));
            }
            oldItem = newItem;
        }
        return count;
    }

    private final void _report(String s)
    {
        print(s);
    }

    private final NonLockingCursor _skippingCursor(int match, ItemStream itemStream) throws MessageStoreException {
        Filter filter = new SkippingFilter(match);
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(filter);
        return cursor;
    }

    private final int _skippingFindAndRemove(int match, ItemStream itemStream, Transaction transaction)
    throws MessageStoreException {
        NonLockingCursor cursor = _skippingCursor(match, itemStream);
        int count = _skippingFindAndRemove(transaction, cursor);
        _report("............Skipping=" + match + ", count=" + count);
        return count;
    }

    private final int _skippingFindAndRemove(ItemStream itemStream, Transaction transaction)
    throws MessageStoreException {
        _report("........skippingFind");

        NonLockingCursor cursor = _skippingCursor(1, itemStream);
        int count1 = _skippingFindAndRemove(transaction, cursor);
        cursor = _skippingCursor(1, itemStream);
        int count2 = _skippingFindAndRemove(transaction, cursor);
        cursor = _skippingCursor(1, itemStream);
        int count3 = _skippingFindAndRemove(transaction, cursor);

        int count = count3 + count2 + count1;
        _report("........skippingFind = " + count);
        return count;
    }

    private final int _skippingFindAndRemove(Transaction transaction, NonLockingCursor cursor)
    throws MessageStoreException {
        int count = 0;
        Item oldItem = (Item) cursor.next();
        while (null != oldItem)
        {
            count++;
            oldItem.remove(transaction, oldItem.getLockID());
            Item newItem = (Item) cursor.next();
            if (null != newItem)
            {
                assertTrue(newItem.isAfter(oldItem));
            }
            oldItem = newItem;
        }
        return count;
    }

    private final void appendNonSkipping(ItemStream itemStream) throws Exception {
        _report("....appendRemoveAuto (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);

        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
        int count = _find(cursor);
        _assertAll(count);

        _appendItems(itemStream);

        count = _findAndMark(cursor);
        _assertAll(count);
    }

    private final void appendNonSkippingMark(ItemStream itemStream) throws Exception {
        _report("....appendRemoveAuto (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);

        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
        int count = _findAndMark(cursor);
        _assertAll(count);

        _appendItems(itemStream);

        count = _findAndMark(cursor);
        _assertAll(count);
    }

    /** Show that we can add more items and they still come out 
     * in the correct order
     * @param itemStream
     * @throws Exception
     */
    private final void appendRemoveAuto(ItemStream itemStream) throws Exception {
        _report("....appendRemoveAuto (" + (System.currentTimeMillis() - _baseTime) + ")");
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        _addItems(itemStream);

        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
        int count = _nonSkippingFindAndRemove(autoTran, cursor);
        _assertAll(count);

        _appendItems(itemStream);

        count = _nonSkippingFindAndRemove(autoTran, cursor);
        _assertAll(count);
    }

    /** Show that we can add more items and they still come out 
     * in the correct order
     * @param itemStream
     * @throws Exception
     */
    private final void appendRemoveLocal(ItemStream itemStream) throws Exception {
        _report("....appendRemoveLocal (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);

        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
        ExternalLocalTransaction localTran = messageStore.getTransactionFactory().createLocalTransaction();
        int count = _nonSkippingFindAndRemove(localTran, cursor);
        _assertAll(count);

        _appendItems(itemStream);

        count = _nonSkippingFindAndRemove(localTran, cursor);
        localTran.commit();
        _assertAll(count);
    }

    /** Show that we can add more items and they still come out 
     * in the correct order
     * @param itemStream
     * @throws Exception
     */
    private final void appendRemoveLocalSkipping(ItemStream itemStream) throws Exception {
        _report("....appendRemoveLocalSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);

        NonLockingCursor cursor1 = _skippingCursor(1, itemStream);
        NonLockingCursor cursor2 = _skippingCursor(2, itemStream);
        NonLockingCursor cursor3 = _skippingCursor(3, itemStream);

        ExternalLocalTransaction localTran = messageStore.getTransactionFactory().createLocalTransaction();

        int count = _skippingFindAndRemove(localTran, cursor3);
        assertTrue(count < 100);
        count += _skippingFindAndRemove(localTran, cursor2);
        assertTrue(count < 100);
        count += _skippingFindAndRemove(localTran, cursor1);
        _assertAll(count);

        _appendItems(itemStream);

        count = _skippingFindAndRemove(localTran, cursor3);
        assertTrue(count < 100);
        count += _skippingFindAndRemove(localTran, cursor2);
        assertTrue(count < 100);
        count += _skippingFindAndRemove(localTran, cursor1);

        _assertAll(count);

        localTran.commit();

    }
    private final void appendSkippingMark(ItemStream itemStream) throws Exception {
        _report("....appendRemoveLocalSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);

        NonLockingCursor cursor1 = itemStream.newNonLockingItemCursor(new SkippingFilter(1));
        NonLockingCursor cursor2 = itemStream.newNonLockingItemCursor(new SkippingFilter(2));
        NonLockingCursor cursor3 = itemStream.newNonLockingItemCursor(new SkippingFilter(3));

        int count = _findAndMark(cursor3);
        assertTrue(count < 100);
        count += _findAndMark(cursor2);
        assertTrue(count < 100);
        count += _findAndMark(cursor1);
        _assertAll(count);

        _appendItems(itemStream);

        count = _findAndMark(cursor3);
        assertTrue(count < 100);
        count += _findAndMark(cursor2);
        assertTrue(count < 100);
        count += _findAndMark(cursor1);

        _assertAll(count);

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
    /**
     * Delete the last element while a cursor is pointing to it.  Show
     * that the cursor behaves correctly: returns null until new 
     * item added.
     */
    private final void deleteLast(ItemStream itemStream) throws Exception {
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

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under an auto Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under auto transaction, by finding then removing them, show 
     * that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void findAndRemoveAuto(ItemStream itemStream) throws Exception {
        _report("....findAuto (" + (System.currentTimeMillis() - _baseTime) + ")");
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        _addItems(itemStream);
        int count = _nonSkippingFindAndRemove(itemStream, autoTran);
        _assertAll(count);
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under an auto Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under auto transaction, by finding then removing them, show 
     * that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void findAndRemoveAutoSkipping(ItemStream itemStream) throws Exception {
        _report("....findAutoSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        _addItems(itemStream);

        int count = _skippingFindAndRemove(itemStream, autoTran);
        _assertAll(count);
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under a local Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under local transaction, by finding then removing them, show 
     * that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void findAndRemoveLocal(ItemStream itemStream) throws Exception {
        _report("....findLocal (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        int count = _nonSkippingFindAndRemove(itemStream, localtran);
        _assertAll(count);
        localtran.rollback();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        count = _nonSkippingFindAndRemove(itemStream, localtran);
        _assertAll(count);
        localtran.commit();

    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under a local Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>remove the items with some locked</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void findAndRemoveLocalInterleaved(ItemStream itemStream) throws Exception {
        _report("....removeLocalInterleaved (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);

        ExternalLocalTransaction localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        int count3 = _skippingFindAndRemove(3, itemStream, localtran3);
        ExternalLocalTransaction localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        int count2 = _skippingFindAndRemove(2, itemStream, localtran2);
        ExternalLocalTransaction localtran1 = messageStore.getTransactionFactory().createLocalTransaction();
        int count1 = _skippingFindAndRemove(1, itemStream, localtran1);
        _assertAll(count3 + count2 + count1);

        localtran3.rollback();
        localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        count3 = _skippingFindAndRemove(3, itemStream, localtran3);
        localtran2.rollback();
        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        count2 = _skippingFindAndRemove(2, itemStream, localtran2);
        localtran1.rollback();
        localtran1 = messageStore.getTransactionFactory().createLocalTransaction();
        count1 = _skippingFindAndRemove(1, itemStream, localtran1);
        _assertAll(count3 + count2 + count1);

        localtran3.rollback();
        localtran2.rollback();
        localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        count3 = _skippingFindAndRemove(3, itemStream, localtran3);
        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        count2 = _skippingFindAndRemove(2, itemStream, localtran2);
        _assertAll(count3 + count2 + count1);

        localtran3.commit();
        localtran2.commit();
        localtran1.commit();
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under a local Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under local transaction, by finding then removing them, show 
     * that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void findAndRemoveLocalSkipping(ItemStream itemStream) throws Exception {
        _report("....findLocalSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        _addItems(itemStream);
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        int count = _skippingFindAndRemove(itemStream, localtran);
        _assertAll(count);
        localtran.rollback();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        count = _skippingFindAndRemove(itemStream, localtran);
        _assertAll(count);
        localtran.commit();

    }
}
