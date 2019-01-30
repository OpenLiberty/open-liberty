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
 * F 170900        11/07/03 corrigk  Original
 * 272110          10/05/05 schofiel 602:SVT: Malformed messages bring down the AppServer
 * 295531          07/11/05 schofiel Redundant return value for eventPrecommitAdd
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * ============================================================================
 */

import java.util.List;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.DataSlice;

/**
 * These tests are designed to ensure that the events triggered upon Items
 * are working as designed. This will test the following events:-
 * 
 * eventPreCommittingAdd
 * eventCommittedAdd
 * eventpreCommittingRemove
 * eventCommittedRemove
 * eventRolledbackAdd
 * eventRolledbackRemove
 * 
 * @author corrigk
 */
public class EventsTest extends MessageStoreTestCase {
    private static final String STATE_NOTSTORED = "not stored";
    private static final String STATE_ADDING = "adding";

    private static final String STATE_ADDING_DATA_GIVEN = "adding, data given";
    private static final String STATE_AVAILABLE = "available";

    private static final String STATE_REMOVING = "removing";
    private static final String STATE_REMOVED = "removed";

    private static final String STATE_UNADDED = "unadded";
    private static final String STATE_UNREMOVED = "unremoved";

    public EventsTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        EventsTest test = new EventsTest("testEventsLocal");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new EventsTest("testEventsAuto");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    //----------------------------------------------------------------------------
    // 01 - Add and Remove of Items to/from an ItemStream using local transaction.	
    //----------------------------------------------------------------------------
    public void testEventsLocal() {
        MessageStore ms = null;
        try {
            ms = createAndStartMessageStore(true, PERSISTENCE);
            ExternalLocalTransaction uncotran = ms.getTransactionFactory().createLocalTransaction();

            PersistentItemStream is = new PersistentItemStream();

            ms.add(is, uncotran);
            uncotran.commit();

            uncotran = ms.getTransactionFactory().createLocalTransaction();

            TestEventItem item1 = new TestEventItem();
            TestEventItem item2 = new TestEventItem();

            assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

            // Add several items.
            is.addItem(item1, uncotran);
            is.addItem(item2, uncotran);

            uncotran.commit();

            assertEquals("Stream size incorrect", 2, is.getStatistics().getTotalItemCount());
            assertEquals("Incorrect state", STATE_AVAILABLE, item1.getState());
            assertEquals("Incorrect state", STATE_AVAILABLE, item2.getState());

            uncotran = ms.getTransactionFactory().createLocalTransaction();

            //	Now retrieve objects. Should get objects back in FIFO order.			
            TestEventItem i1 = (TestEventItem) is.removeFirstMatchingItem(filter, uncotran);
            TestEventItem i2 = (TestEventItem) is.removeFirstMatchingItem(filter, uncotran);

            uncotran.commit();

            // Should leave the stream empty
            assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());
            assertEquivalent(i1, item1);
            assertEquivalent(i2, item2);
            assertEquals("Incorrect state", STATE_REMOVED, i1.getState());
            assertEquals("Incorrect state", STATE_REMOVED, i2.getState());

            item1.setState(STATE_NOTSTORED);
            item2.setState(STATE_NOTSTORED);

            uncotran = ms.getTransactionFactory().createLocalTransaction();

            // Add the items back in and check rolledbackRemove event.
            is.addItem(item1, uncotran);
            is.addItem(item2, uncotran);

            uncotran.commit();
            assertEquals("Stream size incorrect", 2, is.getStatistics().getTotalItemCount());

            uncotran = ms.getTransactionFactory().createLocalTransaction();

            //	Now retrieve objects. Should get objects back in FIFO order.			
            i1 = (TestEventItem) is.removeFirstMatchingItem(filter, uncotran);
            i2 = (TestEventItem) is.removeFirstMatchingItem(filter, uncotran);

            uncotran.rollback();

            // Should leave the stream size as it was
            assertEquals("Stream size incorrect", 2, is.getStatistics().getTotalItemCount());
            assertEquivalent(i1, item1);
            assertEquivalent(i2, item2);
            assertEquals("Incorrect state", STATE_UNREMOVED, i1.getState());
            assertEquals("Incorrect state", STATE_UNREMOVED, i2.getState());

            // Add the items back in and check rolledbackAdd event.

            uncotran = ms.getTransactionFactory().createLocalTransaction();

            TestEventItem item3 = new TestEventItem();
            is.addItem(item3, uncotran);
            assertEquals("Incorrect state", STATE_NOTSTORED, item3.getState()); // Not committed yet

            uncotran.rollback();
            assertEquals("Incorrect state", STATE_UNADDED, item3.getState());

        } catch (Exception e) {
            fail(e.toString());
        } finally {
            try {
                stopMessageStore(ms);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }
    }

    //----------------------------------------------------------------------------
    // 02 - Add and Remove of Items to/from an ItemStream using an autoCommit transaction.	
    //----------------------------------------------------------------------------
    public void testEventsAuto() {
        MessageStore ms = null;
        try {
            ms = createAndStartMessageStore(true, PERSISTENCE);
            Transaction tran = ms.getTransactionFactory().createAutoCommitTransaction();
            PersistentItemStream is = new PersistentItemStream();

            ms.add(is, tran);

            TestEventItem item1 = new TestEventItem();
            TestEventItem item2 = new TestEventItem();

            assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());

            // Add several items.
            is.addItem(item1, tran);

            assertEquals("Incorrect state", STATE_AVAILABLE, item1.getState());
            assertEquals("Incorrect state", STATE_NOTSTORED, item2.getState());

            is.addItem(item2, tran);

            assertEquals("Incorrect state", STATE_AVAILABLE, item1.getState());
            assertEquals("Incorrect state", STATE_AVAILABLE, item2.getState());

            assertEquals("Stream size incorrect", 2, is.getStatistics().getTotalItemCount());

            //	Now retrieve objects. Should get objects back in FIFO order.	
            TestEventItem i1 = (TestEventItem) is.removeFirstMatchingItem(filter, tran);
            TestEventItem i2 = (TestEventItem) is.removeFirstMatchingItem(filter, tran);

            // Should leave the stream empty
            assertEquals("Stream size non zero", 0, is.getStatistics().getTotalItemCount());
            assertEquivalent(i1, item1);
            assertEquivalent(i2, item2);
            assertEquals("Incorrect state", STATE_REMOVED, i1.getState());
            assertEquals("Incorrect state", STATE_REMOVED, i2.getState());

        } catch (Exception e) {
            fail(e.toString());
        } finally {
            try {
                stopMessageStore(ms);
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    public static class TestEventItem extends PersistentItem {
        /**
         * @see com.ibm.ws.sib.msgstore.Item#itemGetStorageStrategy()
         */
        public TestEventItem() {
            super();
        }

        String state = STATE_NOTSTORED; // Used to monitor the sequence of events

        void setState(String i) {
            state = i;
        }

        String getState() {
            return state;
        }

        @Override
        public void eventPrecommitAdd(Transaction tran) throws SevereMessageStoreException {
            super.eventPrecommitAdd(tran);
            assertEquals("Incorrect state", STATE_NOTSTORED, state);
            state = STATE_ADDING;
        }

        // Although these tests do not return any data to be serialized, we override
        // this method to check/update the state to verify that it was called at
        // the correct time in relation to the other (event) callbacks.
        @Override
        public List<DataSlice> getPersistentData() {
            assertEquals("Incorrect state", STATE_ADDING, state);
            state = STATE_ADDING_DATA_GIVEN;
            return null;
        }

        @Override
        public void eventPostCommitAdd(Transaction transaction) throws SevereMessageStoreException {
            super.eventPostCommitAdd(transaction);
            assertEquals("Incorrect state", STATE_ADDING_DATA_GIVEN, state);
            state = STATE_AVAILABLE;
        }

        @Override
        public void eventPrecommitRemove(Transaction tran) throws SevereMessageStoreException {
            super.eventPrecommitRemove(tran);
            assertEquals("Incorrect state", STATE_AVAILABLE, state);
            state = STATE_REMOVING;
        }

        @Override
        public void eventPostCommitRemove(Transaction tran) throws SevereMessageStoreException {
            super.eventPostCommitRemove(tran);
            assertEquals("Incorrect state", STATE_REMOVING, state);
            state = STATE_REMOVED;
        }

        @Override
        public void eventPostRollbackAdd(Transaction tran) throws SevereMessageStoreException {
            super.eventPostRollbackAdd(tran);
            assertEquals("Incorrect state", STATE_NOTSTORED, state);
            state = STATE_UNADDED;
        }

        @Override
        public void eventPostRollbackRemove(Transaction tran) throws SevereMessageStoreException {
            super.eventPostRollbackRemove(tran);
            assertEquals("Incorrect state", STATE_AVAILABLE, state);
            state = STATE_UNREMOVED;
        }
    }

    Filter filter = new Filter()
    {
        public boolean filterMatches(AbstractItem item) throws MessageStoreException
        {
            return true;
        }
    };
}
