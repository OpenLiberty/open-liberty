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
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *                 26/06/03 drphill  Original
 * 295531          07/11/05 schofiel Redundant return value for eventPrecommitAdd
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.StreamIsFull;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * These tests all use ONE item (at a time) and ONE item stream (at a time).  They
 * push the item through all possible state changes and check the features of the 
 * state model (ie previous state, valid transitions).  
 *
 *
 * >>>> add non-locking-cursor tests
 *      add locking-cursor tests
 *      add persistent-locking-cursor tests
 *      expiry tests
 *      update tests
 * 
 *  author: Phill van Leersum
 */
public class ItemStateModelTest extends MessageStoreTestCase
{
    private static final String EVENT_MADE_AVAILABLE = "madeAvailable";

    private static final String EVENT_POSTCOMMIT_ADD = "postCommitAdd";
    private static final String EVENT_POSTCOMMIT_REMOVE = "postCommitRemove";
    private static final String EVENT_POSTCOMMIT_UPDATE = "postCommitUpdate";

    private static final String EVENT_POSTROLLBACK_ADD = "postRollbackAdd";
    private static final String EVENT_POSTROLLBACK_REMOVE = "postRollbackRemove";
    private static final String EVENT_POSTROLLBACK_UPDATE = "postRollbackUpdate";
    private static final String EVENT_PRECOMMIT_ADD = "preCommitAdd";
    private static final String EVENT_PRECOMMIT_REMOVE = "preCommitRemove";
    private static final String EVENT_PRECOMMIT_UPDATE = "preCommitUpdate";

    /* placeholder for any event resulting in notInStore state */
    private static final String EVENT_UNSTORED = "unstored";


    public ItemStateModelTest(String arg0)
    {
        super(arg0);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        ItemStateModelTest test = new ItemStateModelTest("testItemStateModel");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public void testItemStateModel()
    {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream itemStream = createNonPersistentRootItemStream(messageStore);
        try
        {
            _testAddFindRemove(messageStore, itemStream);
            _testAddRollback(messageStore, itemStream);
            _testRemoveFirst(messageStore, itemStream);
            _testRemoveLocked(messageStore, itemStream);
            _testRemovePersistentLocked(messageStore, itemStream);
            _testUnlock(messageStore, itemStream);
            _testUnlockPersistentlyLocked(messageStore, itemStream);
        }
        catch (Exception e)
        {
            fail(e.toString());
        }
        finally
        {
            stopMessageStore(messageStore);
        }
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
    private void _testAddFindRemove(MessageStore messageStore, ItemStream itemStream)
    throws
    MessageStoreException,
    OutOfCacheSpace,
    StreamIsFull,
    SIRollbackException,
    SIConnectionLostException,
    SIIncorrectCallException,
    SIResourceException,
    SIErrorException {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        StateTestItem item = new StateTestItem();
        assertNotStored(item, itemStream);
        itemStream.addItem(item, localtran);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertNotStoredDuringAdd(item, itemStream);
        item.assertBecomeAvailableCount(0);
        localtran.commit();
        item.assertBecomeAvailableCount(1);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());
        assertAvailable(item, itemStream);
        StateTestItem foundItem = (StateTestItem) itemStream.findFirstMatchingItem(null);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, foundItem.getLockID());
        assertRemovingDirect(foundItem, itemStream);
        item.assertBecomeAvailableCount(1);
        localtran.rollback();
        item.assertBecomeAvailableCount(2);
        assertAvailable(item, itemStream);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, foundItem.getLockID());
        assertRemovingDirect(foundItem, itemStream);
        item.assertBecomeAvailableCount(2);
        localtran.commit();
        assertNotStored(foundItem, itemStream);
    }

    /**
     * <ul> 
     * <li>put item into adding state using local transaction.</li>
     * <li>show that item is in adding state</li>
     * <li>Rollback local transation</li>
     * <li>show item is not stored</li>
     * </ul> 
     */
    private void _testAddRollback(MessageStore messageStore, ItemStream itemStream)
    throws
    MessageStoreException,
    OutOfCacheSpace,
    StreamIsFull,
    SIConnectionLostException,
    SIIncorrectCallException,
    SIResourceException,
    SIErrorException {
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        StateTestItem item = new StateTestItem();
        assertNotStored(item, itemStream);
        itemStream.addItem(item, localtran);
        assertNotStoredDuringAdd(item, itemStream);
        item.assertBecomeAvailableCount(0);
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
    private void _testRemoveFirst(MessageStore messageStore, ItemStream itemStream)
    throws
    OutOfCacheSpace,
    StreamIsFull,
    MessageStoreException,
    SIConnectionLostException,
    SIIncorrectCallException,
    SIResourceException,
    SIErrorException {
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        StateTestItem item = new StateTestItem();
        itemStream.addItem(item, autoTran);
        item.assertBecomeAvailableCount(1);
        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        StateTestItem foundItem = (StateTestItem) itemStream.removeFirstMatchingItem(null, localtran);
        assertRemovingDirect(foundItem, itemStream);
        item.assertBecomeAvailableCount(1);
        localtran.rollback();
        item.assertAvailable();
        item.resetEventForAvailable();

        item.assertBecomeAvailableCount(2);

        assertAvailable(foundItem, itemStream);
        // update and rollback
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.requestUpdate(localtran);
        assertUpdating(foundItem, itemStream);
        item.assertBecomeAvailableCount(2);
        localtran.rollback();
        item.assertBecomeAvailableCount(2);

        // update and commit
        assertAvailable(foundItem, itemStream);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.requestUpdate(localtran);
        assertUpdating(foundItem, itemStream);
        item.assertBecomeAvailableCount(2);
        localtran.commit();
        item.assertBecomeAvailableCount(2);

        assertAvailable(item, itemStream);
        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, foundItem.getLockID());
        assertRemovingDirect(foundItem, itemStream);
        item.assertBecomeAvailableCount(2);
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
    private void _testRemoveLocked(MessageStore messageStore, ItemStream itemStream)
    throws
    OutOfCacheSpace,
    StreamIsFull,
    MessageStoreException,
    SIRollbackException,
    SIConnectionLostException,
    SIIncorrectCallException,
    SIResourceException,
    SIErrorException {
        StateTestItem item = new StateTestItem();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        item.assertBecomeAvailableCount(0);
        localtran.commit();
        item.assertBecomeAvailableCount(1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        StateTestItem foundItem = (StateTestItem) cursor.next();
        item.assertBecomeAvailableCount(1);
        assertLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        foundItem.remove(localtran, cursor.getLockID());
        item.assertBecomeAvailableCount(1);
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
    private void _testRemovePersistentLocked(MessageStore messageStore, ItemStream itemStream)
    throws
    OutOfCacheSpace,
    StreamIsFull,
    MessageStoreException,
    SIRollbackException,
    SIConnectionLostException,
    SIIncorrectCallException,
    SIResourceException,
    SIErrorException {
        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();

        StateTestItem item = new StateTestItem();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        item.assertBecomeAvailableCount(0);
        localtran.commit();
        item.assertBecomeAvailableCount(1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        StateTestItem foundItem = (StateTestItem) cursor.next();
        item.assertBecomeAvailableCount(1);
        assertLocked(foundItem, itemStream);

        foundItem.persistLock(autoTran);
        item.assertBecomeAvailableCount(1);
        assertPersistentlyLocked(foundItem, itemStream);

        localtran = messageStore.getTransactionFactory().createLocalTransaction();
        item.assertBecomeAvailableCount(1);
        foundItem.remove(localtran, cursor.getLockID());
        item.assertBecomeAvailableCount(1);
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
    private void _testUnlock(MessageStore messageStore, ItemStream itemStream)
    throws
    OutOfCacheSpace,
    StreamIsFull,
    MessageStoreException,
    SIRollbackException,
    SIConnectionLostException,
    SIIncorrectCallException,
    SIResourceException,
    SIErrorException {
        StateTestItem item = new StateTestItem();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        item.assertBecomeAvailableCount(0);
        localtran.commit();
        item.assertBecomeAvailableCount(1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        StateTestItem foundItem = (StateTestItem) cursor.next();
        item.assertBecomeAvailableCount(1);
        assertLocked(foundItem, itemStream);

        item.unlock(cursor.getLockID());
        item.assertBecomeAvailableCount(2);
        assertAvailable(foundItem, itemStream);
        item.remove();
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
    private void _testUnlockPersistentlyLocked(MessageStore messageStore, ItemStream itemStream)
    throws
    OutOfCacheSpace,
    StreamIsFull,
    MessageStoreException,
    SIRollbackException,
    SIConnectionLostException,
    SIIncorrectCallException,
    SIResourceException,
    SIErrorException {
        StateTestItem item = new StateTestItem();
        ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
        itemStream.addItem(item, localtran);
        assertAdding(item, itemStream);
        item.assertBecomeAvailableCount(0);
        localtran.commit();
        item.assertBecomeAvailableCount(1);

        assertEquals("incorrect size", 1, itemStream.getStatistics().getTotalItemCount());

        LockingCursor cursor = itemStream.newLockingItemCursor(null);
        StateTestItem foundItem = (StateTestItem) cursor.next();
        item.assertBecomeAvailableCount(1);
        assertLocked(foundItem, itemStream);

        Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
        foundItem.persistLock(autoTran);
        item.assertBecomeAvailableCount(1);
        assertPersistentlyLocked(foundItem, itemStream);

        item.unlock(cursor.getLockID(), autoTran);
        item.assertBecomeAvailableCount(2);
        assertAvailable(foundItem, itemStream);
        item.remove();
    }

    private final void assertAdding(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
        item.assertAdding();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertAvailable(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
        item.assertAvailable();
        assertEquals("should be one item by cursor", 1, sizeByCursor(itemStream));
        assertEquals("should be no unavailable items", 0, itemStream.getStatistics().getUnavailableItemCount());
        assertEquals("should be one available item", 1, itemStream.getStatistics().getAvailableItemCount());
        assertNotNull("Should be one findable item", itemStream.findFirstMatchingItem(null));
    }

    //	private final void assertExpiring(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
    //		item.assertExpiring();
    //		assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
    //		assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
    //		assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
    //		assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    //	}

    private final void assertLocked(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
        item.assertLocked();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertNotStored(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
        item.assertNotStored();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be no unavailable items", 0, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertNotStoredDuringAdd(StateTestItem item, ItemStream itemStream)
    throws MessageStoreException {
        item.assertAdding();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable items", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertPersistentlyLocked(StateTestItem item, ItemStream itemStream)
    throws MessageStoreException {
        item.assertLocked();
        item.assertPersistentlyLocked();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertRemoving(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
        item.assertRemoving();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final void assertRemovingDirect(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
        item.assertRemoving();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    //	private final void assertStored(StateTestItem item, ItemStream itemStream) {
    //		item.assertStored();
    //	}

    private final void assertUpdating(StateTestItem item, ItemStream itemStream) throws MessageStoreException {
        item.assertUpdating();
        assertEquals("should be empty by cursor", 0, sizeByCursor(itemStream));
        assertEquals("should be no available items", 0, itemStream.getStatistics().getAvailableItemCount());
        assertEquals("should be one unavailable item", 1, itemStream.getStatistics().getUnavailableItemCount());
        assertNull("Should be no findable item", itemStream.findFirstMatchingItem(null));
    }

    private final int sizeByCursor(ItemStream itemStream)
    {
        int size = 0;
        try
        {
            NonLockingCursor cursor = itemStream.newNonLockingItemCursor(null);
            while (null != cursor.next())
            {
                size++;
            }
        }
        catch (MessageStoreException e)
        {
            fail(e.toString());
        }
        return size;
    }

    public static class StateTestItem extends Item
    {
        private int _becomeAvailableCount = 0;
        private String _previousEvent = EVENT_UNSTORED;

        public StateTestItem()
        {
            super();
        }

        final void assertAdding()
        {
            assertTrue("Item should believe it is adding", isAdding());
        }

        final void assertAvailable()
        {
            assertTrue("Item should believe it is available", isAvailable());
        }
        final void assertBecomeAvailableCount(int i)
        {
            assertEquals("Incorrect available count", i, _becomeAvailableCount);
        }

        final void assertExpiring()
        {
            assertTrue("Item should believe it is expiring", isExpiring());
        }

        final void assertLocked()
        {
            assertTrue("Item should believe it is locked", isLocked());
        }

        final void assertNotStored()
        {
            assertTrue("Item should not believe it is in store", !isInStore());
        }

        final void assertPersistentlyLocked()
        {
            assertTrue("Item should believe it is persistently locked", isPersistentlyLocked());
        }

        final void assertRemoving()
        {
            assertTrue("Item should believe it is removing", isRemoving());
        }

        final void assertRemovingDirect()
        {
            assertTrue("Item should believe it is removing", isRemoving());
        }

        final void assertStored()
        {
            assertTrue("Item should believe it is stored", isInStore());
        }

        final void assertUpdating()
        {
            assertTrue("Item should believe it is updating", isUpdating());
        }

        public void eventPostCommitAdd(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPostCommitAdd(transaction);
            assertEquals("previous event !", EVENT_PRECOMMIT_ADD, _previousEvent);
            _previousEvent = EVENT_POSTCOMMIT_ADD;
            assertAvailable();
            resetEventForAvailable();
            _becomeAvailableCount++;
        }

        public void eventPostCommitRemove(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPostCommitRemove(transaction);
            assertEquals("previous event !", EVENT_PRECOMMIT_REMOVE, _previousEvent);
            _previousEvent = EVENT_POSTCOMMIT_REMOVE;
            assertNotStored();
            resetEventForUnstored();
        }

        public void eventPostCommitUpdate(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPostCommitUpdate(transaction);
            assertEquals("previous event !", EVENT_PRECOMMIT_UPDATE, _previousEvent);
            _previousEvent = EVENT_POSTCOMMIT_UPDATE;
            assertAvailable();
        }

        public void eventPostRollbackAdd(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPostRollbackAdd(transaction);
            if (EVENT_UNSTORED == _previousEvent || EVENT_PRECOMMIT_ADD == _previousEvent)
            {
            }
            else
            {
                fail("previous event! " + _previousEvent);
            }
            _previousEvent = EVENT_POSTROLLBACK_ADD;
            assertNotStored();
            resetEventForUnstored();
        }

        public void eventPostRollbackRemove(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPostRollbackRemove(transaction);
            if (EVENT_MADE_AVAILABLE == _previousEvent || EVENT_PRECOMMIT_REMOVE == _previousEvent)
            {
            }
            else
            {
                fail("previous event! " + _previousEvent);
            }
            _becomeAvailableCount++;
            _previousEvent = EVENT_POSTROLLBACK_REMOVE;
        }

        /* (non-Javadoc)
         * @see com.ibm.ws.sib.msgstore.AbstractItem#eventPostRollbackUpdate(com.ibm.ws.sib.msgstore.Transaction)
         */
        public void eventPostRollbackUpdate(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPostRollbackUpdate(transaction);
            if (EVENT_MADE_AVAILABLE == _previousEvent || EVENT_PRECOMMIT_UPDATE == _previousEvent)
            {
            }
            else
            {
                fail("previous event! " + _previousEvent);
            }
            _previousEvent = EVENT_POSTROLLBACK_UPDATE;
            assertAvailable();
            resetEventForAvailable();
        }

        public void eventPrecommitAdd(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPrecommitAdd(transaction);
            assertEquals("previous event !", EVENT_UNSTORED, _previousEvent);
            _previousEvent = EVENT_PRECOMMIT_ADD;
            assertAdding();
        }

        public void eventPrecommitRemove(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPrecommitRemove(transaction);
            _previousEvent = EVENT_PRECOMMIT_REMOVE;
            assertRemoving();
        }

        public void eventPrecommitUpdate(Transaction transaction) throws SevereMessageStoreException
        {
            super.eventPrecommitUpdate(transaction);
            _previousEvent = EVENT_PRECOMMIT_UPDATE;
            assertUpdating();
        }

        /* (non-Javadoc)
         * @see com.ibm.ws.sib.msgstore.AbstractItem#eventUnlocked()
         */
        public void eventUnlocked() throws SevereMessageStoreException
        {
            super.eventUnlocked();
            _becomeAvailableCount++;
        }

        final void remove() throws MessageStoreException {
            MessageStore messageStore = getOwningMessageStore();
            Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
            remove(autoTran, getLockID());

        }

        final void resetAvailableCount()
        {
            _becomeAvailableCount = 0;
        }

        //        public void eventUnlocked()
        //        {
        //            super.eventUnlocked();
        //            assertAvailable();
        //        }

        final void resetEventForAvailable()
        {
            // event needs to be reset as a result of finally reaching
            // available, and no more events expected in this sequence
            if (EVENT_POSTROLLBACK_REMOVE == _previousEvent
                || EVENT_POSTROLLBACK_UPDATE == _previousEvent
                || EVENT_POSTCOMMIT_UPDATE == _previousEvent
                || EVENT_POSTCOMMIT_ADD == _previousEvent)
            {
                _previousEvent = EVENT_MADE_AVAILABLE;
            }
            else
            {
                fail("last event! " + _previousEvent);
            }
        }

        final void resetEventForUnstored()
        {
            // event needs to be reset as a result of finally reaching
            // available, and no more events expected in this sequence
            if (EVENT_POSTROLLBACK_ADD == _previousEvent || EVENT_POSTCOMMIT_REMOVE == _previousEvent)
            {
                _previousEvent = EVENT_UNSTORED;
            }
            else
            {
                fail("last event! " + _previousEvent);
            }
        }
    }
}
