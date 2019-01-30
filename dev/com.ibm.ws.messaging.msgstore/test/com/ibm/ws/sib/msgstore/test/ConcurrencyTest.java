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
 * Reason          Date   Origin   Description
 * ------------- -------- -------- --------------------------------------------
 * 170900        11/07/03 corrigk  Original
 * 341158        13/03/06 gareth   Make better use of LoggingTestCase
 * 467410        12/11/07 gareth   Change NonPersistent tests to use STORE_MAYBE
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.test.persistence.StoreMaybeItem;
import com.ibm.ws.sib.msgstore.test.persistence.StoreMaybeItemStream;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * These tests are designed to ensure that items can be written to and
 * read from item streams concurrently.
 * 
 * @author corrigk
 */
public class ConcurrencyTest extends MessageStoreTestCase {
    static final Filter TRUE_FILTER = new Filter()
    {
        public boolean filterMatches(AbstractItem item) throws MessageStoreException
        {
            return true;
        }
    };

    int itemsRead = 0;

    int itemsWritten = 0;

    public ConcurrencyTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        ConcurrencyTest test = new ConcurrencyTest("testNonpersistentTrueFilter");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ConcurrencyTest("testPersistentTrueFilter");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ConcurrencyTest("testNonpersistentNullFilter");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ConcurrencyTest("testPersistentNullFilter");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    //----------------------------------------------------------------------------
    // 01 - Add and Remove of Items to/from an ItemStream by concurrent threads.	
    // We write an initial set of (50) items to the stream. Then start thread
    // to read all items from the stream until it is empty.  Once thread has started,
    // put another 50 items
    //----------------------------------------------------------------------------
    public void testNonpersistentTrueFilter() {
        MessageStore ms = null;
        ItemStream is = null;
        Transaction tran = null;

        print("PersistenceManager used: " + PERSISTENCE);
        ms = createAndStartMessageStore(true, PERSISTENCE);

        try {
            tran = ms.getTransactionFactory().createAutoCommitTransaction();
            is = new StoreMaybeItemStream();
            ms.add(is, tran);
        } catch (MessageStoreException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        // Write initial set of items to the stream		
        _writeNonpersistent(tran, is, 50);
        blankLine();

        // Start a new thread to read items from the stream
        tran = ms.getTransactionFactory().createAutoCommitTransaction();

        MyReader reader = new MyReader(this, is, TRUE_FILTER, ms);
        Thread thread = new Thread(reader);
        thread.start();

        _writeNonpersistent(tran, is, 50);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        reader.stop();
        try {
            thread.join();
            if (ms != null) {
                stopMessageStore(ms);
            }
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        print("\nStream empty - test ended. Items written=" + itemsWritten + " Items read=" + itemsRead);
        assertEquals("Items written not same as read", itemsRead, itemsWritten);

    }

    //----------------------------------------------------------------------------
    // 02 - Add and Remove of Items to/from an ItemStream by concurrent threads.	
    // We write an initial set of (50) items to the stream. Then start thread
    // to read all items from the stream until it is empty.  Once thread has started,
    // put another 50 items
    //----------------------------------------------------------------------------
    public void testPersistentTrueFilter() {
        MessageStore ms = null;
        Transaction tran = null;
        ItemStream is = null;

        print("PersistenceManager used: " + PERSISTENCE);
        ms = createAndStartMessageStore(true, PERSISTENCE);

        try {
            is = new PersistentItemStream();
            tran = ms.getTransactionFactory().createAutoCommitTransaction();
            ms.add(is, tran);
        } catch (MessageStoreException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        // Write initial set of items to the stream		
        _writePersistent(tran, is, 50);
        blankLine();

        // Start a new thread to read items from the stream
        tran = ms.getTransactionFactory().createAutoCommitTransaction();

        MyReader reader = new MyReader(this, is, TRUE_FILTER, ms);
        Thread thread = new Thread(reader);
        thread.start();

        _writePersistent(tran, is, 50);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        reader.stop();
        try {
            thread.join();
            if (ms != null) {
                stopMessageStore(ms);
            }
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        print("\nStream empty - test ended. Items written=" + itemsWritten + " Items read=" + itemsRead);
        assertEquals("Items written not same as read", itemsRead, itemsWritten);

    }

    //----------------------------------------------------------------------------
    // 03 - Add and Remove of Items to/from an ItemStream by concurrent threads.	
    // We write an initial set of (50) items to the stream. Then start thread
    // to read all items from the stream until it is empty.  Once thread has started,
    // put another 50 items
    //----------------------------------------------------------------------------
    public void testNonpersistentNullFilter() {
        MessageStore ms = null;
        Transaction tran = null;
        ItemStream is = null;

        print("PersistenceManager used: " + PERSISTENCE);
        ms = createAndStartMessageStore(true, PERSISTENCE);

        try {
            is = new StoreMaybeItemStream();
            tran = ms.getTransactionFactory().createAutoCommitTransaction();
            ms.add(is, tran);
        } catch (MessageStoreException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        // Write initial set of items to the stream		
        _writeNonpersistent(tran, is, 50);
        blankLine();

        // Start a new thread to read items from the stream
        tran = ms.getTransactionFactory().createAutoCommitTransaction();

        MyReader reader = new MyReader(this, is, null, ms);
        Thread thread = new Thread(reader);
        thread.start();

        _writeNonpersistent(tran, is, 50);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        reader.stop();
        try {
            thread.join();
            if (ms != null) {
                stopMessageStore(ms);
            }
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        print("\nStream empty - test ended. Items written=" + itemsWritten + " Items read=" + itemsRead);
        assertEquals("Items written not same as read", itemsRead, itemsWritten);

    }

    //----------------------------------------------------------------------------
    // 04 - Add and Remove of Items to/from an ItemStream by concurrent threads.	
    // We write an initial set of (50) items to the stream. Then start thread
    // to read all items from the stream until it is empty.  Once thread has started,
    // put another 50 items
    //----------------------------------------------------------------------------
    public void testPersistentNullFilter() {
        MessageStore ms = null;
        Transaction tran = null;

        print("PersistenceManager used: " + PERSISTENCE);
        ms = createAndStartMessageStore(true, PERSISTENCE);

        ItemStream is = null;
        try {
            is = new PersistentItemStream();
            tran = ms.getTransactionFactory().createAutoCommitTransaction();
            ms.add(is, tran);
        } catch (MessageStoreException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        // Write initial set of items to the stream		
        _writePersistent(tran, is, 50);
        blankLine();

        // Start a new thread to read items from the stream
        tran = ms.getTransactionFactory().createAutoCommitTransaction();

        MyReader reader = new MyReader(this, is, null, ms);
        Thread thread = new Thread(reader);
        thread.start();

        _writePersistent(tran, is, 50);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        reader.stop();
        try {
            thread.join();
            if (ms != null) {
                stopMessageStore(ms);
            }
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        print("\nStream empty - test ended. Items written=" + itemsWritten + " Items read=" + itemsRead);
        assertEquals("Items written not same as read", itemsRead, itemsWritten);

    }

    private void _writeNonpersistent(Transaction tran, ItemStream is, int count) {
        try {
            for (int i = 0; i < count; i++) {
                // Defect 467410
                // Change to use STORE_MAYBE as this provides more consistent 
                // behaviour and guarantees no loss of messages before restart.
                is.addItem(new StoreMaybeItem(), tran);
                itemsWritten++;
                print("+"); // ... to signify that an item has been added to the stream
                Thread.yield(); // stop the loop being too tight
            }
        } catch (MessageStoreException e) {
            fail(e.toString());
        }
    }

    private void _writePersistent(Transaction tran, ItemStream is, int count) {
        try {
            for (int i = 0; i < count; i++) {
                is.addItem(new PersistentItem(), tran);
                itemsWritten++;
                print("+"); // ... to signify that an item has been added to the stream
            }
        } catch (MessageStoreException e) {
            fail(e.toString());
        }
    }

    public class MyReader implements Runnable {
        // This thread will read items from the stream until .?.
        private final ItemStream _itemStream;
        private volatile boolean _keepRunning = true;
        private Transaction _transaction = null;
        private final Filter _filter;
        private final MessageStoreTestCase _test;

        MyReader(MessageStoreTestCase test, ItemStream is, Filter filter, MessageStore ms) {
            _test = test;
            _itemStream = is;
            _transaction = ms.getTransactionFactory().createAutoCommitTransaction();
            _filter = filter;
        }

        public void run() {
            StringBuffer removes;

            while (_keepRunning) {
                try {
                    Item item = _itemStream.removeFirstMatchingItem(_filter, _transaction);
                    if (item != null) {
                        _test.print("");

                        removes = new StringBuffer();
                        while (item != null) {
                            removes.append("-");// ... to signify that an item has been removed from the stream
                            itemsRead++;
                            item = _itemStream.removeFirstMatchingItem(_filter, _transaction);
                        }
                        _test.print(removes.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public final void stop() {
            _keepRunning = false;
        }
    }
}
