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
package com.ibm.ws.sib.msgstore.test.statemodel;
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

import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * These tests all use ONE item (at a time) and ONE item stream (at a time). They
 * push the item through all possible state changes and check the features of the
 * state model (ie previous state, valid transitions).
 * 
 * 
 * >>> add non-locking-cursor tests
 * add locking-cursor tests
 * add persistent-locking-cursor tests
 * expiry tests
 * update tests
 * 
 * author: Phill van Leersum
 */
public class StateModelTest extends MessageStoreTestCase {
    MessageStore messageStore = null;
    boolean traceon = false; // Turn trace on/off 

    public StateModelTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        StateModelTest test = new StateModelTest("testStateModel");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public final void testStateModel() {
        ItemStream itemStream = null;
        try {
            print("PersistenceManager used: " + PERSISTENCE);
            messageStore = createAndStartMessageStore(true, PERSISTENCE);

            itemStream = new ItemStream();
            ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
            messageStore.add(itemStream, localtran);
            localtran.commit();

            _testAddLockedRemove(itemStream);
            itemStream.empty();
            _testAddFindRemove(itemStream);
            itemStream.empty();
            _testAddRollback(itemStream);
            itemStream.empty();
            _testRemoveFirst(itemStream);
            itemStream.empty();
            _testRemoveLocked(itemStream);
            itemStream.empty();
            _testRemoveLockedRollback(itemStream);
            itemStream.empty();
            _testRemovePersistentLocked(itemStream);
            itemStream.empty();
            _testUnlock(itemStream);
            itemStream.empty();
            _testUnlockPersistentlyLocked(itemStream);
            itemStream.empty();
            _testAddLockedExpireUnlock(itemStream); // Defect 219384
            itemStream.empty();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed: " + e);
        } finally {
            if (null != messageStore) {
                stopMessageStore(messageStore);
            }
        }
    }

    private final void _assertBecomeAvailableCount(Item item, int i) {
    //assertEquals(i, item.getBecomeAvailableCount());
    }

    /**
     * <ul>
     * <li>put item into adding state using local transaction.
     * <li>show item is in adding state</li>
     * <li>Commit local transation</li>
     * <li>show item is available</li>
     * <li>FindFirst.</li>
     * <li>Show item is still available.</li>
     * <li>Remove item (local transaction)</li>
     * <li>Show item is removingDirect</li>
     * <li>Abort transaction</li>
     * <li>Show item is available.</li>
     * <li>FindFirst.</li>
     * <li>Show item is still available.</li>
     * <li>Remove item (local transaction)</li>
     * <li>Show item is removingDirect</li>
     * <li>Commit transaction</li>
     * <li>Show item is not in store.</li>
     * </ul>
     */
    private final void _testAddFindRemove(ItemStream itemStream) throws Exception {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        Item item = new Item();
        assertNotStored(item, itemStream);
        itemStream.addItem(item, localtran);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertNotStoredDuringAdd(item, itemStream);
        _assertBecomeAvailableCount(item, 0);

        localtran.commit();
        _assertBecomeAvailableCount(item, 0);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertAvailable(item, itemStream);
        Item foundItem = (Item) itemStream.findFirstMatchingItem(null);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, foundItem.getLockID());
        assertRemovingDirect(foundItem, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.rollback();
        _assertBecomeAvailableCount(item, 0);
        assertAvailable(item, itemStream);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, foundItem.getLockID());
        assertRemovingDirect(foundItem, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.commit();
        assertNotStored(foundItem, itemStream);
    }

    /**
     * <ul>
     * <li>put item into addingLocked state using local transaction.
     * <li>show item is in addingLocked state</li>
     * <li>Commit local transation</li>
     * <li>show item is locked</li>
     * <li>Remove item (local transaction)</li>
     * <li>Show item is removingLocked</li>
     * <li>Commit transaction</li>
     * <li>Show item is not in store.</li>
     * </ul>
     */
    private final void _testAddLockedRemove(ItemStream itemStream) throws Exception {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        Item item = new Item();
        assertNotStored(item, itemStream);
        final long LOCK_ID = 12345678L;
        itemStream.addItem(item, LOCK_ID, localtran);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertNotStoredDuringAdd(item, itemStream);
        _assertBecomeAvailableCount(item, 0);

        localtran.commit();
        _assertBecomeAvailableCount(item, 0);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertLocked(item, itemStream);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        item.remove(localtran, LOCK_ID);
        assertRemoving(item, itemStream);
        localtran.commit();
        assertNotStored(item, itemStream);
    }

    /**
     * <ul>
     * <li>defect 219384</li>
     * <li>put item into addingLocked state using local transaction.
     * <li>show item is in addingLocked state</li>
     * <li>Commit local transation</li>
     * <li>show item is locked</li>
     * <li>Wait for item to expire</li>
     * <li>Attempt to unlock the item</li>
     * </ul>
     */
    private final void _testAddLockedExpireUnlock(ItemStream itemStream) throws Exception {
        /*
         * if (traceon)
         * {
         * turnOnTrace();
         * String str = "com.ibm.ws.sib.msgstore.expiry.Expirer=all=enabled";
         * configureTrace(str);
         * try
         * {
         * ManagerAdmin.setTraceOutputToFile("statemodeltest.trace", 10000000, 1, str);
         * }
         * catch (Exception e)
         * {
         * e.printStackTrace();
         * }
         * }
         */
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        Item item = new Item("Item01", 500);
        assertNotStored(item, itemStream);
        final long LOCK_ID = 12345678L;
        itemStream.addItem(item, LOCK_ID, localtran);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertNotStoredDuringAdd(item, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.commit();
        _assertBecomeAvailableCount(item, 0);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertLocked(item, itemStream);
        Thread.sleep(1000); // Let the item expire while locked
        // Now try to unlock it, this will cause NPE described in 219384
        int unlock1 = item.guessUnlockCount();
        item.unlock(LOCK_ID);
        int unlock2 = item.guessUnlockCount();
        // Should be a diff of 1 
        assertEquals("Incorrect unlock count", 1, (unlock2 - unlock1));

        Thread.sleep(1000); // Give expiry a chance to finish 
    }

    /**
     * <ul>
     * <li>put item into adding state using local transaction.</li>
     * <li>show that item is in adding state</li>
     * <li>Rollback local transation</li>
     * <li>show item is not stored</li>
     * </ul>
     */
    private final void _testAddRollback(ItemStream itemStream) throws Exception {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        Item item = new Item();
        assertNotStored(item, itemStream);
        itemStream.addItem(item, localtran);
        assertNotStoredDuringAdd(item, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.rollback();
        assertNotStored(item, itemStream);
    }

    /**
     * <ul>
     * <li>Put an item.</li>
     * <li>Show item is available.</li>
     * <li>Remove item (local transaction)</li>
     * <li>Show item is removingDirect</li>
     * <li>Abort transaction</li>
     * <li>Show item is available.</li>
     * <li>Remove item (local transaction)</li>
     * <li>Show item is removingDirect</li>
     * <li>Commit transaction</li>
     * <li>Show item is not in store.</li>
     * </ul>
     */
    private final void _testRemoveFirst(ItemStream itemStream) throws Exception {
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        Item item = new Item();
        itemStream.addItem(item, autoTran);
        _assertBecomeAvailableCount(item, 0);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        Item foundItem = (Item) itemStream.removeFirstMatchingItem(null, localtran);
        assertRemovingDirect(foundItem, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.rollback();
        assertAvailable(item, itemStream);

        _assertBecomeAvailableCount(item, 2);

        assertAvailable(foundItem, itemStream);
        // update and rollback
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.requestUpdate(localtran);
        assertUpdating(foundItem, itemStream);
        _assertBecomeAvailableCount(item, 2);
        localtran.rollback();
        _assertBecomeAvailableCount(item, 3);

        // update and commit
        assertAvailable(foundItem, itemStream);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.requestUpdate(localtran);
        assertUpdating(foundItem, itemStream);
        _assertBecomeAvailableCount(item, 3);
        localtran.commit();
        _assertBecomeAvailableCount(item, 4);

        assertAvailable(item, itemStream);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, foundItem.getLockID());
        assertRemovingDirect(foundItem, itemStream);
        _assertBecomeAvailableCount(item, 4);
        localtran.commit();
        assertNotStored(foundItem, itemStream);

    }

    /**
     * Test that a locked item is removed correctly.
     * Check that the locked count is decremented - the defect that
     * caused this test to be written.
     * <ul>
     * <li>Add an item {assert available}</li>
     * <li>Lock it {assert locked}</li>
     * <li>Remove the item {assert removed}</li>
     * </ul>
     */
    private final void _testRemoveLocked(ItemStream itemStream) throws Exception {

        Item item = new Item();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.commit();
        _assertBecomeAvailableCount(item, 1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        Item foundItem = (Item) cursor.next();
        _assertBecomeAvailableCount(item, 1);
        assertLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, cursor.getLockID());
        _assertBecomeAvailableCount(item, 1);
        assertRemoving(foundItem, itemStream);
        localtran.rollback();
        _assertBecomeAvailableCount(item, 1);
        assertLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, cursor.getLockID());
        _assertBecomeAvailableCount(item, 1);
        assertRemoving(foundItem, itemStream);
        localtran.commit();

        assertNotStored(foundItem, itemStream);

    }

    /**
     * Test that a locked item remove is rolled back correctly.
     * Check that the locked count is decremented - the defect that
     * caused this test to be written.
     * <ul>
     * <li>Add an item {assert available}</li>
     * <li>Lock it {assert locked}</li>
     * <li>Remove the item under a local transactio {assert removing locked}</li>
     * <li>Rollback the local transaction {assert locked}</li>
     * <li>Persist the lock under a local tran, roll back {assert locked}</li>
     * <li>Persist the lock under a local tran, commit {assert persistently locked}</li>
     * <li>Remove the item under a local transactio {assert removing locked persistent}</li>
     * <li>Rollback the local transaction {assert persistently locked}</li>
     * </ul>
     */
    private final void _testRemoveLockedRollback(ItemStream itemStream) throws Exception {

        Item item = new Item();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.commit();
        _assertBecomeAvailableCount(item, 1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        Item foundItem = (Item) cursor.next();
        _assertBecomeAvailableCount(item, 1);
        assertLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, cursor.getLockID());
        _assertBecomeAvailableCount(item, 1);
        assertRemoving(foundItem, itemStream);
        localtran.rollback();

        assertLocked(foundItem, itemStream);
        _assertBecomeAvailableCount(item, 1);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.persistLock(localtran);
        _assertBecomeAvailableCount(item, 1);
        localtran.rollback();
        _assertBecomeAvailableCount(item, 1);
        assertLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.persistLock(localtran);
        localtran.commit();
        _assertBecomeAvailableCount(item, 1);
        assertPersistentlyLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, cursor.getLockID());
        assertRemoving(foundItem, itemStream);
        localtran.rollback();
        _assertBecomeAvailableCount(item, 1);
        assertPersistentlyLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, cursor.getLockID());
        assertRemoving(foundItem, itemStream);
        localtran.commit();

        assertNotStored(foundItem, itemStream);
    }

    /**
     * Test that a persistently locked item is removed correctly.
     * Check that the locked count is decremented - the defect that
     * caused this test to be written.
     * <ul>
     * <li>Add an item {assert available}</li>
     * <li>Lock it {assert locked}</li>
     * <li>Persist the lock {assert locked persistent}</li>
     * <li>Remove the item {assert removed}</li>
     * </ul>
     */
    private final void _testRemovePersistentLocked(ItemStream itemStream) throws Exception {
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        Item item = new Item();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.commit();
        _assertBecomeAvailableCount(item, 1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        Item foundItem = (Item) cursor.next();
        _assertBecomeAvailableCount(item, 1);
        assertLocked(foundItem, itemStream);

        foundItem.persistLock(autoTran);
        _assertBecomeAvailableCount(item, 1);
        assertPersistentlyLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        _assertBecomeAvailableCount(item, 1);
        foundItem.remove(localtran, cursor.getLockID());
        _assertBecomeAvailableCount(item, 1);
        assertRemoving(foundItem, itemStream);
        localtran.commit();

        assertNotStored(foundItem, itemStream);

    }

    /**
     * Test that a locked item is unlocked correctly.
     * Check that the locked count is decremented - the defect that
     * caused this test to be written.
     * <ul>
     * <li>Add an item {assert available}</li>
     * <li>Lock it {assert locked}</li>
     * <li>Unlock it</li>
     * </ul>
     */
    private final void _testUnlock(ItemStream itemStream) throws Exception {
        Item item = new Item();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertEquals("Incorrect unlock count", 0, item.guessUnlockCount());
        assertAdding(item, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.commit();
        assertEquals("Incorrect unlock count", 0, item.guessUnlockCount());
        _assertBecomeAvailableCount(item, 1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        Item foundItem = (Item) cursor.next();
        assertEquals("Incorrect unlock count", 0, item.guessUnlockCount());
        _assertBecomeAvailableCount(item, 1);
        assertLocked(foundItem, itemStream);

        item.unlock(cursor.getLockID());
        assertEquals("Incorrect unlock count", 1, item.guessUnlockCount());
        _assertBecomeAvailableCount(item, 2);
        assertAvailable(foundItem, itemStream);
    }

    /**
     * Test that a locked item is unlocked correctly.
     * Check that the locked count is decremented - the defect that
     * caused this test to be written.
     * <ul>
     * <li>Add an item {assert available}</li>
     * <li>Lock it {assert locked}</li>
     * <li>Unlock it</li>
     * </ul>
     */
    private final void _testUnlockPersistentlyLocked(ItemStream itemStream) throws Exception {
        Item item = new Item();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        _assertBecomeAvailableCount(item, 0);
        localtran.commit();
        _assertBecomeAvailableCount(item, 1);
        assertEquals("Incorrect unlock count", 0, item.guessUnlockCount());

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        Item foundItem = (Item) cursor.next();
        _assertBecomeAvailableCount(item, 1);
        assertEquals("Incorrect unlock count", 0, item.guessUnlockCount());
        assertLocked(foundItem, itemStream);

        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        foundItem.persistLock(autoTran);
        _assertBecomeAvailableCount(item, 1);
        assertEquals("Incorrect unlock count", 0, item.guessUnlockCount());
        assertPersistentlyLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        item.unlock(cursor.getLockID(), localtran);
        _assertBecomeAvailableCount(item, 1);
        localtran.rollback();
        _assertBecomeAvailableCount(item, 1);
        assertEquals("Incorrect unlock count", 0, item.guessUnlockCount());
        assertPersistentlyLocked(foundItem, itemStream);

        item.unlock(cursor.getLockID(), autoTran);
        _assertBecomeAvailableCount(item, 2);
        assertEquals("Incorrect unlock count", 1, item.guessUnlockCount());
        assertAvailable(foundItem, itemStream);
    }

    private final void assertAdding(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isAdding());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertAvailable(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isAvailable());
        assertEquals("should be one item by cursor", 1, sizeByCursor(itemStream));
        assertEquals("should be no unavailable items", 0, itemStream.getStatistics().getUnavailableItemCount());
        assertEquals("should be one available item", 1, itemStream.getStatistics().getAvailableItemCount());
        assertNotNull("Should be one findable item", itemStream.findFirstMatchingItem(null));
    }

    //	private final void assertExpiring(Item item, ItemStream itemStream) throws MessageStoreException {
    //		assertTrue(item.isExpiring());
    //		assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
    //		assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
    //		assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
    //		assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    //	}

    private final void assertLocked(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isLocked());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertNotStored(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(!item.isInStore());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be no unavailable items", 0, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertNotStoredDuringAdd(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isAdding());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable items", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertPersistentlyLocked(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isLocked());
        assertTrue(item.isPersistentlyLocked());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertRemoving(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isRemoving());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertRemovingDirect(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isRemoving());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    //	private final void assertStored(Item item, ItemStream itemStream) {
    //		assertTrue(item.isStored();
    //	}

    private final void assertUpdating(Item item, ItemStream itemStream) throws MessageStoreException {
        assertTrue(item.isUpdating());
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final int sizeByCursor(ItemStream itemStream) {
        int size = 0;
        try {
            NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
            while (null != cursor.next()) {
                size++;
            }
        } catch (MessageStoreException e) {
            fail(e.toString());
        }
        return size;
    }
}
