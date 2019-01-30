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
package com.ibm.ws.sib.msgstore.test.putget;
/*
 * Change activity:
 *
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *                 26/06/03 drphill  Original
 * 341158          13/03/06 gareth   Make better use of LoggingTestCase
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;                         
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.StreamIsFull;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * These tests do simple put/get operations.  Effectively put 100 items
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
 */
public class PutGetTest extends MessageStoreTestCase
{
    private MessageStore messageStore = null;

    // A general purpose filter which returns everything
    private Filter filter = new Filter()
    {
        public boolean filterMatches(AbstractItem item) throws MessageStoreException {
            return true;
        }
    };

    private long _baseTime;

    private ItemStream is = null;

    public PutGetTest(String arg0)
    {
        super(arg0);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        PutGetTest test = new PutGetTest("testPutGet");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public final void testPutGet()
    {
        ItemStream itemStream = null;
        _baseTime = System.currentTimeMillis();
        try
        {
            print("PersistenceManager used: "+PERSISTENCE);
            _report("TestPutGet starting messageStore (" + _baseTime + ")");
            messageStore = createAndStartMessageStore(true, PERSISTENCE);

            // the following require no stream present
            _testFindStreamFromStoreAfterRollback();
            _testRemStreamFromStoreAfterRollback();

            is = new ItemStream();
            messageStore.add(is, messageStore.getTransactionFactory().createAutoCommitTransaction());

            _testFindStreamFromItem();
            is.empty();
            _testAddRemItemsToStream();
            is.empty();
            _testAddFindItemsToStream();
            is.empty();
            _testAddRemStreamToStore();
            is.empty();
            _testAddFindStreamToStore();
            is.empty();
            _testAddRemItemsToStreamLocal();
            is.empty();
            _testAddFindItemsToStreamLocal();
            is.empty();
            _testAddRemStreamToStoreLocal();
            is.empty();
            _testAddFindStreamToStoreLocal();
            is.empty();
            _testRemItemsFromStreamAfterRollback();
            is.empty();
            _testFindItemsFromStreamAfterRollback();

            _report("TestPutGet creating itemStream (" + (System.currentTimeMillis() - _baseTime) + ")");
            itemStream = new ItemStream();
            ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
            messageStore.add(itemStream, localtran);
            localtran.commit();

            removeAuto(itemStream);
            removeLocal(itemStream);

            findAuto(itemStream);
            findLocal(itemStream);

            removeAutoSkipping(itemStream);
            removeLocalSkipping(itemStream);

            findAutoSkipping(itemStream);
            findLocalSkipping(itemStream);

            removeLocalInterleaved(itemStream);
            findLocalInterleaved(itemStream);

            addRemoveConstraints(itemStream);

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
        _report("TestPutGet finished (" + (System.currentTimeMillis() - _baseTime) + ")");
    }

    private final Statistics _addItems(ItemStream itemStream, Transaction autoTran) throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException 
    {
        _report("........addItems");
        Item.resetCounter();
        Statistics stats = itemStream.getStatistics();
        _assertNone(stats.getTotalItemCount());
        for (int i = 0; i < 10; i++)
        {
            for (int j = 0; j < 10; j++)
            {
                Item item = new Item(j);
                itemStream.addItem(item, autoTran);
            }
        }
        _assertAll(stats.getTotalItemCount());
        return stats;
    }

    private final Statistics _addItemsLocal(ItemStream itemStream) throws Exception {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        Statistics stats = _addItems(itemStream, localtran);
        _assertAll(stats.getAddingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.commit();
        _assertAll(stats.getTotalItemCount());
        _assertNone(stats.getAddingItemCount());
        _assertAll(stats.getAvailableItemCount());
        return stats;
    }

    /** Wrapper for assertion to make sure I get it the right way round!
     * @param i
     */
    private final void _assertAll(long i)
    {
        assertEquals(100, i);
    }

    /** Wrapper for assertion to make sure I get it the right way round!
     * @param i
     */
    private final void _assertNone(long i)
    {
        assertEquals(0, i);
    }

    private final int _nonSkippingFind(ItemStream itemStream, Transaction transaction) throws MessageStoreException {
        _report("........nonSkippingFind");
        int count = 0;
        Item oldItem = (Item) itemStream.findFirstMatchingItem(null);
        while (null != oldItem)
        {
            count++;
            oldItem.remove(transaction, oldItem.getLockID());
            Item newItem = (Item) itemStream.findFirstMatchingItem(null);
            if (null != newItem)
            {
                assertTrue(newItem.isAfter(oldItem));
            }
            oldItem = newItem;
        }
        return count;
    }

    private final int _nonSkippingRemove(ItemStream itemStream, Transaction transaction) throws MessageStoreException {
        _report("........nonSkippingRemove");
        int count = 0;
        Item oldItem = (Item) itemStream.removeFirstMatchingItem(null, transaction);
        while (null != oldItem)
        {
            count++;
            Item newItem = (Item) itemStream.removeFirstMatchingItem(null, transaction);
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

    private final int _skippingFind(int match, ItemStream itemStream, Transaction transaction)
    throws MessageStoreException {
        int count = 0;
        Filter filter = new SkippingFilter(match);
        Item oldItem = (Item) itemStream.findFirstMatchingItem(filter);
        while (null != oldItem)
        {
            count++;
            oldItem.remove(transaction, oldItem.getLockID());
            Item newItem = (Item) itemStream.findFirstMatchingItem(filter);
            if (null != newItem)
            {
                assertTrue(newItem.isAfter(oldItem));
            }
            oldItem = newItem;
        }
        _report("............Skipping=" + match + ", count=" + count);
        return count;
    }

    private final int _skippingFind(ItemStream itemStream, Transaction transaction) throws MessageStoreException {
        _report("........skippingFind");
        int count3 = _skippingFind(3, itemStream, transaction);
        int count2 = _skippingFind(2, itemStream, transaction);
        int count1 = _skippingFind(1, itemStream, transaction);
        int count = count3 + count2 + count1;
        _report("........skippingFind = " + count);
        return count;
    }

    private final int _skippingRemove(int match, ItemStream itemStream, Transaction transaction)
    throws MessageStoreException {
        int count = 0;
        Filter filter = new SkippingFilter(match);
        Item oldItem = (Item) itemStream.removeFirstMatchingItem(filter, transaction);
        while (null != oldItem)
        {
            count++;
            Item newItem = (Item) itemStream.removeFirstMatchingItem(filter, transaction);
            if (null != newItem)
            {
                assertTrue(newItem.isAfter(oldItem));
            }
            oldItem = newItem;
        }
        _report("............Skipping=" + match + ", count=" + count);
        return count;
    }

    private final int _skippingRemove(ItemStream itemStream, Transaction transaction) throws MessageStoreException {
        _report("........skippingRemove");
        int count3 = _skippingRemove(3, itemStream, transaction);
        int count2 = _skippingRemove(2, itemStream, transaction);
        int count1 = _skippingRemove(1, itemStream, transaction);
        int count = count3 + count2 + count1;
        _report("........skippingRemove = " + count);
        return count;
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
    private final void findAuto(ItemStream itemStream) throws Exception {
        _report("....findAuto (" + (System.currentTimeMillis() - _baseTime) + ")");
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        Statistics stats = _addItems(itemStream, autoTran);
        _assertNone(stats.getAddingItemCount());
        _assertAll(stats.getAvailableItemCount());

        int count = _nonSkippingFind(itemStream, autoTran);
        _assertAll(count);
        _assertNone(stats.getTotalItemCount());
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
    private final void findAutoSkipping(ItemStream itemStream) throws Exception {
        _report("....findAutoSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        Statistics stats = _addItems(itemStream, autoTran);
        _assertNone(stats.getAddingItemCount());
        _assertAll(stats.getAvailableItemCount());

        int count = _skippingFind(itemStream, autoTran);
        _assertAll(count);
        _assertNone(stats.getTotalItemCount());
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
    private final void findLocal(ItemStream itemStream) throws Exception {
        _report("....findLocal (" + (System.currentTimeMillis() - _baseTime) + ")");
        Statistics stats = _addItemsLocal(itemStream);
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        int count = _nonSkippingFind(itemStream, localtran);
        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.rollback();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        count = _nonSkippingFind(itemStream, localtran);
        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.commit();

        _assertNone(stats.getTotalItemCount());
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under a local Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>remove the items with some locked</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void findLocalInterleaved(ItemStream itemStream) throws Exception {
        _report("....removeLocalInterleaved (" + (System.currentTimeMillis() - _baseTime) + ")");
        Statistics stats = _addItemsLocal(itemStream);

        ExternalLocalTransaction localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        int count3 = _skippingFind(3, itemStream, localtran3);
        ExternalLocalTransaction localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        int count2 = _skippingFind(2, itemStream, localtran2);
        ExternalLocalTransaction localtran1 = messageStore.getTransactionFactory().createLocalTransaction();
        int count1 = _skippingFind(1, itemStream, localtran1);
        _assertAll(count3 + count2 + count1);

        localtran3.rollback();
        localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        count3 = _skippingFind(3, itemStream, localtran3);
        localtran2.rollback();
        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        count2 = _skippingFind(2, itemStream, localtran2);
        localtran1.rollback();
        localtran1 = messageStore.getTransactionFactory().createLocalTransaction();
        count1 = _skippingFind(1, itemStream, localtran1);
        _assertAll(count3 + count2 + count1);

        localtran3.rollback();
        localtran2.rollback();
        localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        count3 = _skippingFind(3, itemStream, localtran3);
        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        count2 = _skippingFind(2, itemStream, localtran2);
        _assertAll(count3 + count2 + count1);

        localtran3.commit();
        localtran2.commit();
        localtran1.commit();
        _assertNone(stats.getTotalItemCount());
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
    private final void findLocalSkipping(ItemStream itemStream) throws Exception {
        _report("....findLocalSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        Statistics stats = _addItemsLocal(itemStream);
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        int count = _skippingFind(itemStream, localtran);
        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.rollback();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        count = _skippingFind(itemStream, localtran);
        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.commit();

        _assertNone(stats.getTotalItemCount());
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under an auto Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under auto transaction, show that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void removeAuto(ItemStream itemStream) throws Exception {
        _report("....removeAuto (" + (System.currentTimeMillis() - _baseTime) + ")");
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        Statistics stats = _addItems(itemStream, autoTran);
        _assertNone(stats.getAddingItemCount());
        _assertAll(stats.getAvailableItemCount());

        int count = _nonSkippingRemove(itemStream, autoTran);
        _assertAll(count);
        _assertNone(stats.getTotalItemCount());
    }
    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under an auto Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under auto transaction, show that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void removeAutoSkipping(ItemStream itemStream) throws Exception {
        _report("....removeAutoSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        Statistics stats = _addItems(itemStream, autoTran);
        _assertNone(stats.getAddingItemCount());
        _assertAll(stats.getAvailableItemCount());

        int count = _skippingRemove(itemStream, autoTran);
        _assertAll(count);
        _assertNone(stats.getTotalItemCount());
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under a local Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under local transaction, show that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void removeLocal(ItemStream itemStream) throws Exception {
        _report("....removeLocal (" + (System.currentTimeMillis() - _baseTime) + ")");
        Statistics stats = _addItemsLocal(itemStream);
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        int count = _nonSkippingRemove(itemStream, localtran);
        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.rollback();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        count = _nonSkippingRemove(itemStream, localtran);
        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.commit();

        _assertNone(stats.getTotalItemCount());
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under a local Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>remove the items with some locked</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void removeLocalInterleaved(ItemStream itemStream) throws Exception {
        _report("....removeLocalInterleaved (" + (System.currentTimeMillis() - _baseTime) + ")");
        Statistics stats = _addItemsLocal(itemStream);

        ExternalLocalTransaction localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        int count3 = _skippingRemove(3, itemStream, localtran3);
        ExternalLocalTransaction localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        int count2 = _skippingRemove(2, itemStream, localtran2);
        ExternalLocalTransaction localtran1 = messageStore.getTransactionFactory().createLocalTransaction();
        int count1 = _skippingRemove(1, itemStream, localtran1);
        _assertAll(count3 + count2 + count1);

        localtran3.rollback();
        localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        count3 = _skippingRemove(3, itemStream, localtran3);
        localtran2.rollback();
        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        count2 = _skippingRemove(2, itemStream, localtran2);
        localtran1.rollback();
        localtran1 = messageStore.getTransactionFactory().createLocalTransaction();
        count1 = _skippingRemove(1, itemStream, localtran1);
        _assertAll(count3 + count2 + count1);

        localtran3.rollback();
        localtran2.rollback();
        localtran3 = messageStore.getTransactionFactory().createLocalTransaction();
        count3 = _skippingRemove(3, itemStream, localtran3);
        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        count2 = _skippingRemove(2, itemStream, localtran2);
        _assertAll(count3 + count2 + count1);

        localtran3.commit();
        localtran2.commit();
        localtran1.commit();
        _assertNone(stats.getTotalItemCount());
    }

    private final void addRemoveConstraints(ItemStream itemStream) throws Exception {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        ItemStream innerStream = new ItemStream();
        Item innerItem = new Item();

        boolean excepted = false;
        try
        {
            excepted = false;
            innerStream.addItem(innerItem, localtran);
            fail();
        }
        catch (Exception e)
        {
            localtran.rollback();
            excepted = true;
        }
        assertTrue(excepted);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        ExternalLocalTransaction localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItemStream(innerStream, localtran);

        try
        {
            innerStream.addItem(innerItem, localtran2);
            excepted = false;
            localtran2.commit();
            fail(); // should fail as different transactions
        }
        catch (Exception e)
        {
            excepted = true;
            localtran2.rollback();
        }
        assertTrue(excepted);

        // can add in same transaction

        // Problem using item which failed to add previously. Although the previous
        // add failed, we have left the membership hooked up so addItem now thinks
        // the item is already added to a stream.
        // Workaround - make a new one for now.
        innerItem = new Item(); 

        innerStream.addItem(innerItem, localtran);
        localtran.commit();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        //itemStream.xmlRequestWriteOnSystemOut();
        innerStream.remove(localtran, innerStream.getLockID());
        try
        {
            excepted = false;
            localtran.commit();
            fail(); // should fail as not empty
        }
        catch (Exception e)
        {
            excepted = true;
        }
        assertTrue(excepted);
        //itemStream.xmlRequestWriteOnSystemOut();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.removeFirstMatchingItemStream(null, localtran);
        try
        {
            excepted = false;
            localtran.commit();
            fail(); // should fail as not empty
        }
        catch (Exception e)
        {
            excepted = true;
        }
        assertTrue(excepted);

        // attempt to remove item under diffrent transaction
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.removeFirstMatchingItemStream(null, localtran);

        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        innerItem.remove(localtran2, innerItem.getLockID());
        try
        {
            excepted = false;
            localtran2.commit();
            fail(); // should fail as parent is deleting under different transaction
        }
        catch (Exception e)
        {
            excepted = true;
        }
        assertTrue(excepted);

        localtran2 = messageStore.getTransactionFactory().createLocalTransaction();
        innerStream.removeFirstMatchingItem(null, localtran2);
        try
        {
            excepted = false;
            localtran2.commit();
            fail(); // should fail as parent is deleting under different transaction
        }
        catch (Exception e)
        {
            excepted = true;
        }
        assertTrue(excepted);

        innerStream.removeFirstMatchingItem(null, localtran);
        localtran.commit();
    }

    /**
     * <ul> 
     * <li>Put 100 items with alternating priorities under a local Transaction</li>
     * <li>Show 100 items in correct state</li>
     * <li>Get all items under local transaction, show that they are in order</li>
     * <li>Show item stream is empty</li>
     * </ul> 
     */
    private final void removeLocalSkipping(ItemStream itemStream) throws Exception {
        _report("....removeLocalSkipping (" + (System.currentTimeMillis() - _baseTime) + ")");
        Statistics stats = _addItemsLocal(itemStream);
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        int count = _skippingRemove(itemStream, localtran);

        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.rollback();

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        count = _skippingRemove(itemStream, localtran);

        _assertAll(count);
        _assertAll(stats.getTotalItemCount());
        _assertAll(stats.getRemovingItemCount());
        _assertNone(stats.getAvailableItemCount());
        localtran.commit();

        _assertNone(stats.getTotalItemCount());
    }

    //----------------------------------------------------------------------------
    // 01 - Add and Remove Items to/from an ItemStream using auto commit.   
    //----------------------------------------------------------------------------
    private final void _testAddRemItemsToStream() throws Exception {
        Transaction tran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        // Add several items.
        is.addItem(item1, tran);
        is.addItem(item2, tran);
        is.addItem(item3, tran);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        //  Now retrieve objects. Should get objects back in FIFO order.            
        Item i1 = (Item) is.removeFirstMatchingItem(filter, tran);
        Item i2 = (Item) is.removeFirstMatchingItem(filter, tran);
        Item i3 = (Item) is.removeFirstMatchingItem(filter, tran);
        Item i4 = (Item) is.removeFirstMatchingItem(filter, tran); // Missing

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());
    }

    //----------------------------------------------------------------------------
    // 02 - Add and Find Items to/from an ItemStream using auto commit. 
    //----------------------------------------------------------------------------
    private final void _testAddFindItemsToStream() throws Exception {
        Transaction tran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        // Add several items.
        is.addItem(item1, tran);
        is.addItem(item2, tran);
        is.addItem(item3, tran);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        //  Now retrieve objects. Should get item1 back each time.          
        Item i1 = (Item) is.findFirstMatchingItem(filter);
        Item i2 = (Item) is.findFirstMatchingItem(filter);
        Item i3 = (Item) is.findFirstMatchingItem(filter);

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item1);
        assertEquivalent(i3, item1);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        // Setup a new filter to return only a specific item.   
        final Item item3b = item3;
        Filter filter2 = new Filter()
        {
            public boolean filterMatches(AbstractItem item) throws MessageStoreException {
                if (item.equals(item3b))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        };

        //  Now retrieve objects again. Using filter2, should get item3 back each time.         
        i1 = (Item) is.findFirstMatchingItem(filter2);
        i2 = (Item) is.findFirstMatchingItem(filter2);
        i3 = (Item) is.findFirstMatchingItem(filter2);

        assertEquivalent(i1, item3);
        assertEquivalent(i2, item3);
        assertEquivalent(i3, item3);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        // Now, lets just check that all three objects are still there
        // by doing a destructive read. Should get objects back in FIFO order.          
        i1 = (Item) (is.removeFirstMatchingItem(filter, tran));
        i2 = (Item) (is.removeFirstMatchingItem(filter, tran));
        i3 = (Item) (is.removeFirstMatchingItem(filter, tran));
        Item i4 = (Item) (is.removeFirstMatchingItem(filter, tran)); // Missing

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());
    }

    //----------------------------------------------------------------------------
    // 03 - Add and Remove ItemStream to/from a MessageStore using auto commit.
    //----------------------------------------------------------------------------  
    private final void _testAddRemStreamToStore() throws Exception {
        Transaction tran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        // Add several items to the stream.
        is.addItem(item1, tran);
        is.addItem(item2, tran);
        is.addItem(item3, tran);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        // Get a new stream from the message store.
        ItemStream is2 = (ItemStream) messageStore.findFirstMatching(filter);

        // Retrieve objects from the new stream. Should get same object back each time.                 
        Item i1 = (Item) (is2.findFirstMatchingItem(filter));
        Item i2 = (Item) (is2.findFirstMatchingItem(filter));
        Item i3 = (Item) (is2.findFirstMatchingItem(filter));

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item1);
        assertEquivalent(i3, item1);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());
        assertEquals("Stream size incorrect", 3, is2.getStatistics().getTotalItemCount());

        // Now, check that all three objects are in the new stream
        // by doing a destructive read. Should get objects back in FIFO order.          
        i1 = (Item) (is2.removeFirstMatchingItem(filter, tran));
        i2 = (Item) (is2.removeFirstMatchingItem(filter, tran));
        i3 = (Item) (is2.removeFirstMatchingItem(filter, tran));
        Item i4 = (Item) (is2.removeFirstMatchingItem(filter, tran)); // Missing

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);

        assertEquals("Stream size non zero", 0, is2.getStatistics().getTotalItemCount());

        // Finally, check that the InputStream has gone
        //ItemStream is3 = (ItemStream) messageStore.removeFirstMatching(filter, tran);
        //assertEquivalent(is3, is);
    }

    //----------------------------------------------------------------------------
    // 04 - Add and Find ItemStream to/from a MessageStore using auto commit.
    //----------------------------------------------------------------------------  
    private final void _testAddFindStreamToStore() throws Exception {
        Transaction tran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        // Add several items to the stream.
        is.addItem(item1, tran);
        is.addItem(item2, tran);
        is.addItem(item3, tran);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        // Get a new stream from the message store.
        ItemStream is2 = (ItemStream) messageStore.findFirstMatching(filter);

        // Retrieve objects from the new stream. Should get same object back each time.                 
        Item i1 = (Item) (is2.findFirstMatchingItem(filter));
        Item i2 = (Item) (is2.findFirstMatchingItem(filter));
        Item i3 = (Item) (is2.findFirstMatchingItem(filter));

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item1);
        assertEquivalent(i3, item1);

        assertEquals("Stream size incorrect", 3, is2.getStatistics().getTotalItemCount());

        // Now, check that all three objects are in the new stream
        // by doing a destructive read. Should get objects back in FIFO order.          
        i1 = (Item) (is2.removeFirstMatchingItem(filter, tran));
        i2 = (Item) (is2.removeFirstMatchingItem(filter, tran));
        i3 = (Item) (is2.removeFirstMatchingItem(filter, tran));
        Item i4 = (Item) (is2.removeFirstMatchingItem(filter, tran)); // Missing

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);

        assertEquals("Stream size non zero", 0, is2.getStatistics().getTotalItemCount());
    }

    //----------------------------------------------------------------------------
    // 05 - Add and Remove Items to/from an ItemStream using local transaction. 
    //----------------------------------------------------------------------------
    private final void _testAddRemItemsToStreamLocal() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        // Add several items.
        is.addItem(item1, uncotran);
        is.addItem(item2, uncotran);
        is.addItem(item3, uncotran);

        uncotran.commit();

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        //  Now retrieve objects. Should get objects back in FIFO order.            
        Item i1 = (Item) is.removeFirstMatchingItem(filter, uncotran);
        Item i2 = (Item) is.removeFirstMatchingItem(filter, uncotran);
        Item i3 = (Item) is.removeFirstMatchingItem(filter, uncotran);
        Item i4 = (Item) is.removeFirstMatchingItem(filter, uncotran); // Missing

        uncotran.commit();

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);
    }

    //----------------------------------------------------------------------------
    // 06 - Add and Find Items to/from an ItemStream using local transaction.   
    //----------------------------------------------------------------------------
    private final void _testAddFindItemsToStreamLocal() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        // Add several items.
        is.addItem(item1, uncotran);
        is.addItem(item2, uncotran);
        is.addItem(item3, uncotran);

        uncotran.commit();
        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        //  Now retrieve objects. Should get item1 back each time.          
        Item i1 = (Item) is.findFirstMatchingItem(filter);
        Item i2 = (Item) is.findFirstMatchingItem(filter);
        Item i3 = (Item) is.findFirstMatchingItem(filter);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item1);
        assertEquivalent(i3, item1);

        // Setup a new filter to return only a specific item.   
        final Item item3b = item3;
        Filter filter2 = new Filter()
        {
            public boolean filterMatches(AbstractItem item) throws MessageStoreException {
                if (item.equals(item3b))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        };

        //  Now retrieve objects again. Using filter2, should get item3 back each time.         
        i1 = (Item) is.findFirstMatchingItem(filter2);
        i2 = (Item) is.findFirstMatchingItem(filter2);
        i3 = (Item) is.findFirstMatchingItem(filter2);

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        assertEquivalent(i1, item3);
        assertEquivalent(i2, item3);
        assertEquivalent(i3, item3);

        // Now, lets just check that all three objects are still there
        // by doing a destructive read. Should get objects back in FIFO order.          
        i1 = (Item) (is.removeFirstMatchingItem(filter, uncotran));
        i2 = (Item) (is.removeFirstMatchingItem(filter, uncotran));
        i3 = (Item) (is.removeFirstMatchingItem(filter, uncotran));
        Item i4 = (Item) (is.removeFirstMatchingItem(filter, uncotran)); // Missing

        uncotran.commit();

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);
        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());
    }

    //----------------------------------------------------------------------------
    // 07 - Add and Remove ItemStream to/from a MessageStore using local transaction.
    //----------------------------------------------------------------------------  
    private final void _testAddRemStreamToStoreLocal() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        // Add several items to the stream.
        is.addItem(item1, uncotran);
        is.addItem(item2, uncotran);
        is.addItem(item3, uncotran);

        uncotran.commit();
        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        // Get a new stream from the message store.
        ItemStream is2 = (ItemStream) messageStore.findFirstMatching(filter);

        // Retrieve objects from the new stream. Should get same object back each time.                 
        Item i1 = (Item) (is2.findFirstMatchingItem(filter));
        Item i2 = (Item) (is2.findFirstMatchingItem(filter));
        Item i3 = (Item) (is2.findFirstMatchingItem(filter));

        uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item1);
        assertEquivalent(i3, item1);

        // Now, check that all three objects are in the new stream
        // by doing a destructive read. Should get objects back in FIFO order.          
        i1 = (Item) (is2.removeFirstMatchingItem(filter, uncotran));
        i2 = (Item) (is2.removeFirstMatchingItem(filter, uncotran));
        i3 = (Item) (is2.removeFirstMatchingItem(filter, uncotran));
        Item i4 = (Item) (is2.removeFirstMatchingItem(filter, uncotran)); // Missing

        uncotran.commit();
        assertEquals("Stream size non zero", 0, is2.getStatistics().getTotalItemCount());

        uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);

        // Finally, check that the InputStream has gone
        ItemStream is3 = (ItemStream) messageStore.removeFirstMatching(filter, uncotran);

        uncotran.rollback();

        assertEquivalent(is3, is);
    }

    //----------------------------------------------------------------------------
    // 08 - Add and Find ItemStream to/from a MessageStore using local transaction.
    //----------------------------------------------------------------------------  
    private final void _testAddFindStreamToStoreLocal() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        // Add several items to the stream.
        is.addItem(item1, uncotran);
        is.addItem(item2, uncotran);
        is.addItem(item3, uncotran);

        uncotran.commit();
        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        // Get a new stream from the message store.
        ItemStream is2 = (ItemStream) messageStore.findFirstMatching(filter);

        // Retrieve objects from the new stream. Should get same object back each time.                 
        Item i1 = (Item) (is2.findFirstMatchingItem(filter));
        Item i2 = (Item) (is2.findFirstMatchingItem(filter));
        Item i3 = (Item) (is2.findFirstMatchingItem(filter));

        assertEquals("Stream size incorrect", 3, is2.getStatistics().getTotalItemCount());

        uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item1);
        assertEquivalent(i3, item1);

        // Now, check that all three objects are in the new stream
        // by doing a destructive read. Should get objects back in FIFO order.          
        i1 = (Item) (is2.removeFirstMatchingItem(filter, uncotran));
        i2 = (Item) (is2.removeFirstMatchingItem(filter, uncotran));
        i3 = (Item) (is2.removeFirstMatchingItem(filter, uncotran));
        Item i4 = (Item) (is2.removeFirstMatchingItem(filter, uncotran)); // Missing

        uncotran.commit();
        assertEquals("Stream size non zero", 0, is2.getStatistics().getTotalItemCount());

        assertEquivalent(i1, item1);
        assertEquivalent(i2, item2);
        assertEquivalent(i3, item3);
        assertNull("Item 4 found, should not exist", i4);
    }

    //----------------------------------------------------------------------------
    // 09 - Add and Remove Items to/from an ItemStream using local transaction.
    //      The transaction is rolled back after add, so the items should be absent.    
    //----------------------------------------------------------------------------
    private final void _testRemItemsFromStreamAfterRollback() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        // Add several items.
        is.addItem(item1, uncotran);
        is.addItem(item2, uncotran);
        is.addItem(item3, uncotran);

        uncotran.rollback();
        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        //  Now attempt to retrieve objects. Should be missing.         
        Item i1 = (Item) is.removeFirstMatchingItem(filter, uncotran); // Missing
        Item i2 = (Item) is.removeFirstMatchingItem(filter, uncotran); // Missing

        uncotran.commit();

        assertNull("Item 1 found, should not exist", i1);
        assertNull("Item 2 found, should not exist", i2);
    }

    //----------------------------------------------------------------------------
    // 10 - Add and Find Items to/from an ItemStream using local transaction.   
    //      The transaction is rolled back after add, so the items should be absent.    
    //----------------------------------------------------------------------------
    private final void _testFindItemsFromStreamAfterRollback() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();

        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        // Add several items.
        is.addItem(item1, uncotran);
        is.addItem(item2, uncotran);
        is.addItem(item3, uncotran);

        uncotran.rollback();
        assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

        //  Now attempt to retrieve objects. Should be missing.         
        Item i1 = (Item) is.findFirstMatchingItem(filter);
        Item i2 = (Item) is.findFirstMatchingItem(filter);

        assertNull("Item 1 found, should not exist", i1);
        assertNull("Item 2 found, should not exist", i2);

        // Setup a new filter to return only a specific item.   
        final Item item3b = item3;
        Filter filter2 = new Filter()
        {
            public boolean filterMatches(AbstractItem item) throws MessageStoreException {
                if (item.equals(item3b))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        };

        //  Now attempt to retrieve specific object. Should be missing.         
        i1 = (Item) is.findFirstMatchingItem(filter2);
        i2 = (Item) is.findFirstMatchingItem(filter2);

        assertNull("Item 3 found, should not exist", i1);
        assertNull("Item 3 found, should not exist", i2);
    }

    //----------------------------------------------------------------------------
    // 11 - Add and Remove ItemStream to/from a MessageStore using local transaction.
    //      The transaction is rolled back after add, so the items should be absent.    
    //----------------------------------------------------------------------------  
    private final void _testRemStreamFromStoreAfterRollback() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();
        ItemStream is = new ItemStream();

        messageStore.add(is, uncotran);

        uncotran.rollback();

        uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        // Attempt to get the stream from the message store.
        ItemStream is2 = (ItemStream) messageStore.removeFirstMatching(filter, uncotran);

        uncotran.commit();

        assertNull("ItemStream found, should not exist", is2);
    }

    //----------------------------------------------------------------------------
    // 12 - Add and Find ItemStream to/from a MessageStore using local transaction.
    //      The transaction is rolled back after add, so the items should be absent.    
    //----------------------------------------------------------------------------  
    private final void _testFindStreamFromStoreAfterRollback() throws Exception {
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();
        ItemStream is = new ItemStream();

        messageStore.add(is, uncotran);

        uncotran.rollback();

        // Attempt to get a new stream from the message store.
        ItemStream is2 = (ItemStream) messageStore.findFirstMatching(filter);

        assertNull("ItemStream found, should not exist", is2);

    }

    // to aid tests that check that the back reference from an item to its
    // item stream is correct, we examine a given item stream to see that it
    // contains the correct items
    private final AbstractItem _checkItemStream(final ItemStream itemStream, final Item[] itemsItShouldContain)
    throws MessageStoreException {
        int count = 0;
        NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
        AbstractItem gotItem = cursor.next();
        AbstractItem lastItem = gotItem;
        while (null != gotItem)
        {
            lastItem = gotItem;
            if (count < itemsItShouldContain.length)
            {
                assertEquivalent(itemsItShouldContain[count], gotItem);
            }
            else
            {
                fail("too many items in stream");
            }
            count++;
            gotItem = cursor.next();
        }
        return lastItem;
    }

    // to aid tests that check that the back reference from an item to its
    // item stream is correct, we examine a given item stream to see that it
    // contains the correct items
    private final void _checkItemStreamDestructively(final ItemStream itemStream, final Item[] itemsItShouldContain)
    throws Exception {
        int count = 0;
        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        // And get the other items
        Item gotItem = (Item) itemStream.removeFirstMatchingItem(filter, uncotran);
        while (null != gotItem)
        {
            if (count < itemsItShouldContain.length)
            {
                assertEquivalent(itemsItShouldContain[count], gotItem);
            }
            else
            {
                fail("too many items in stream");
            }
            count++;
            gotItem = (Item) itemStream.removeFirstMatchingItem(filter, uncotran);
        }

        uncotran.commit();
    }

    //----------------------------------------------------------------------------
    // 13 - Add some items to a stream. Each should hold a reference to the stream.
    // Obtain the stream from each item and check that it (the stream) contains     
    // the other item(s).
    //----------------------------------------------------------------------------
    private final void _testFindStreamFromItem() throws Exception {
        Item[] realItems = new Item[] { new Item(), new Item(), new Item(),};

        ExternalLocalTransaction uncotran = messageStore.getTransactionFactory().createLocalTransaction();

        // Add several items to the stream.
        for (int i = 0; i < realItems.length; i++)
        {
            is.addItem(realItems[i], uncotran);
        }

        uncotran.commit();

        assertEquals("Stream size incorrect", 3, is.getStatistics().getTotalItemCount());

        // Attempt to get the stream from the message store.
        ItemStream is2 = (ItemStream) messageStore.findFirstMatching(filter);

        _checkItemStream(is2, realItems);

        Item item4 = (Item) is2.findFirstMatchingItem(filter);

        // Now, get the stream again from the item
        ItemStream is3 = (ItemStream) item4.getItemStream();

        assertEquals("Stream mismatch", is, is3);
        assertEquals("Stream size incorrect", 3, is3.getStatistics().getTotalItemCount());

        _checkItemStream(is3, realItems);

        ItemStream is4 = (ItemStream) messageStore.findFirstMatching(filter);
        Item lastItem = (Item) _checkItemStream(is4, realItems);

        // Now, get the stream again from the item
        ItemStream is5 = (ItemStream) lastItem.getItemStream();

        assertEquals("Stream mismatch", is, is5);
        assertEquals("Stream size incorrect", 3, is5.getStatistics().getTotalItemCount());

        _checkItemStreamDestructively(is5, realItems);

        // All streams should now be empty
        assertEquals("Stream not empty", 0, is.getStatistics().getTotalItemCount());
        assertEquals("Stream not empty", 0, is2.getStatistics().getTotalItemCount());
        assertEquals("Stream not empty", 0, is3.getStatistics().getTotalItemCount());
        assertEquals("Stream not empty", 0, is4.getStatistics().getTotalItemCount());
        assertEquals("Stream not empty", 0, is5.getStatistics().getTotalItemCount());
    }
}
