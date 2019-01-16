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
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.DataSlice;

/**
 * This test is designed to ensure that multiple threads can work on an item
 * stream. A variable number of 'putting' and 'getting threads' may be
 * started. The getting threads will remove items which have been added most recently.
 * 
 * 
 * The test will run in default mode with no arguments but the following may be
 * supplied as command-line arguments to vary the test conditions:-
 * 
 * maxItems= Maximum number of items to be written by each thread (0 = infinite)
 * putThreads= Number of threads to add items to stream
 * getThreads= Number of threads to remove items from stream
 * itemSize= Size of item to be read/written
 * store= always | maybe | never (Alias 'storage=' may be used instead)
 * putInterval= Interval in ms between writes
 * getInterval= Interval in ms between reads
 * progress Show progress (one dot '.' per item removed from stream)
 * 
 * These are supplied as command line parameters, eg.
 * java com.ibm....StressTest putthreads=10 store=always itemsize=4096 progress
 * 
 * @author corrigk
 */
public class StressTest extends MessageStoreTestCase {
    // Maximum number of items to write to a stream - set to 0 to write continously.
    // Override with "maxitems=" argument.
    int maxItems = 500;

    // Interval (milliseconds) between successive writes - set to 0 to write as fast as possible
    // Override with "putinterval=" argument.
    int putInterval = 0;

    // Interval (milliseconds) between successive attempts to remove an item from the stream
    // Override with "getinterval=" argument.
    int getInterval = 0;

    // Number of threads - can be overriden by "putThreads=" and "getThreads=" arguments.
    int putThreads = 5;
    int getThreads = 1;

    // Storage strategy = STORE_ALWAYS, STORE_NEVER, STORE_MAYBE. Can be overridden
    // by "store=" or "storage=" argument.
    int storageStrategy = ItemStream.STORE_ALWAYS;

    // Item size. Can be overridden by "size=" argument.
    int itemSize = 1000;

    // Switch to show progress indicator(s)
    boolean progress = false;

    boolean readerMayEnd = false;
    boolean drainStream = false;
    int threadsRunning = 0;
    private final Object itemLock = new Object();

    StressItem lastItem = null; // The last item written 

    Filter filter = new Filter()
    {
        @Override
        public boolean filterMatches(AbstractItem item) throws MessageStoreException {
            return true;
        }
    };

    int itemsRemoved = 0;
    int deltaIn = 0;
    int deltaOut = 0;

    public StressTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        StressTest test = new StressTest("testStressTestWriteItems");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testStressTestWriteItems() {

        print("---------- StressTest starting --------");
        MessageStore ms = null;
        try {
            ms = createAndStartMessageStore(true, PERSISTENCE);
            Transaction tran = ms.getTransactionFactory().createAutoCommitTransaction();
            PersistentItemStream is = new PersistentItemStream();
            ms.add(is, tran);

            // Start threads(s) to write items to the stream
            MyWriter threadWriter[] = new MyWriter[putThreads];
            print("Starting " + putThreads + " writers");

            for (int i = 0; i < putThreads; i++) {
                threadWriter[i] = new MyWriter(is, ms);
                new Thread(threadWriter[i]).start();
                threadsRunning++;
            }
            print("Starting " + getThreads + " readers");
            // Start thread(s) to remove items from the stream
            MyReader threadReader[] = new MyReader[getThreads];
            for (int i = 0; i < getThreads; i++) {
                threadReader[i] = new MyReader(is, ms);
                new Thread(threadReader[i]).start();
            }

            // Wait for the writer threads to finish							
            while (threadsRunning > 0) {
                Thread.sleep(1000);
                long siz = is.getStatistics().getTotalItemCount();
                print("Stream size=" + siz + " (+" + deltaIn + ") (-" + deltaOut + ")");
                deltaIn = 0;
                deltaOut = 0;
            }
            print("Writers ended. " + itemsRemoved + " items removed. Stream size=" + is.getStatistics().getTotalItemCount());

            // Now drain the stream 
            if (getThreads == 0) {
                print("No readers, unable to drain stream");
            } else {
                drainStream = true;
                long remaining = is.getStatistics().getTotalItemCount();
                while (remaining > 0) {
                    print(is.getStatistics().getTotalItemCount() + " items remaining on stream");
                    Thread.sleep(800);
                    long rem = is.getStatistics().getTotalItemCount();
                    if (rem >= remaining) {
                        print("ERROR Unable to drain stream");
                        rem = 0; // Force exit
                    }
                    remaining = rem;
                }
                readerMayEnd = true;
                Thread.sleep(100);
            }
            print("---------- StressTest ended ----------");
        } catch (Exception e) {
            e.printStackTrace();
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

    public static void main(String args[]) {

        StressTest t = new StressTest("WriteItems")
        {
            @Override
            public void runTest()
            {
                testStressTestWriteItems();
            }
        };

        for (int i = 0; i < args.length; i++) {
            int j = args[i].indexOf('=');
            String parm = null;
            String value = null;

            if (j > 0) {
                parm = args[i].substring(0, j).toLowerCase();
                value = args[i].substring(j + 1);
            } else {
                parm = args[i].toLowerCase();
            }

            if (parm.equals("putthreads")) {
                t.putThreads = Integer.parseInt(value);
            }

            if (parm.equals("getthreads")) {
                t.getThreads = Integer.parseInt(value);
            }

            if (parm.equals("size")) {
                t.itemSize = Integer.parseInt(value);
            }

            if (parm.equals("maxitems")) {
                t.maxItems = Integer.parseInt(value);
            }

            if (parm.equals("putinterval")) {
                t.putInterval = Integer.parseInt(value);
            }

            if (parm.equals("getinterval")) {
                t.getInterval = Integer.parseInt(value);
            }

            if (parm.equals("progress")) {
                t.progress = true;
            }

            if (parm.equals("store") || parm.equals("storage")) {
                if (value.toLowerCase().equals("never")) {
                    t.storageStrategy = ItemStream.STORE_NEVER;
                } else if (value.toLowerCase().equals("maybe")) {
                    t.storageStrategy = ItemStream.STORE_MAYBE;
                } else if (value.toLowerCase().equals("always")) {
                    t.storageStrategy = ItemStream.STORE_ALWAYS;
                } else {
                    print("ERROR: Invalid value for store= argument");
                    return;
                }
            }
        }

        print("---------- StressTest arguments ----------");
        print("PutThreads=" + t.putThreads);
        print("GetThreads=" + t.getThreads);
        print("ItemSize=" + t.itemSize);
        print("MaxItems=" + t.maxItems);
        print("StorageStrategy=");
        switch (t.storageStrategy) {
            case ItemStream.STORE_MAYBE:
                print("MAYBE");
                break;
            case ItemStream.STORE_ALWAYS:
                print("ALWAYS");
                break;
            case ItemStream.STORE_NEVER:
                print("NEVER");
                break;
            default:
                print("** Invalid **");
                break;
        }
        print("GetInterval=" + t.getInterval);
        print("PutInterval=" + t.putInterval);
        if (t.progress) {
            print("Progress=ON");
        } else {
            print("Progress=OFF");
        }

        t.run();
    }

    class MyReader implements Runnable {
        //
        // This thread will remove an item from the stream at intervals 
        //
        PersistentItemStream is = null;
        Transaction tran = null;

        MyReader(PersistentItemStream is, MessageStore ms) {
            this.is = is;
            this.tran = ms.getTransactionFactory().createAutoCommitTransaction();
        }

        StressItem filteredItem = null; //The item to be removed

        Filter filterItem = new Filter()
        {
            @Override
            public boolean filterMatches(AbstractItem item) throws MessageStoreException {
                if (item.equals(filteredItem))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        };

        @Override
        public void run() {
            while (!readerMayEnd) {
                try {
                    Thread.sleep(getInterval);
                    synchronized (itemLock) {
                        if (lastItem != null) {
                            filteredItem = lastItem;
                            lastItem = null;
                        }
                    }
                    if (drainStream || (filteredItem != null)) {
                        Date date = new Date();
                        long start = date.getTime();
                        Item item2 = null;

                        if (drainStream) {
                            item2 = is.removeFirstMatchingItem(filter, tran);
                        } else {
                            item2 = is.removeFirstMatchingItem(filterItem, tran);
                            // We can only verify that the item was removed if the storage strategy
                            // is ALWAYS. Otherwise, the MS may have already removed (thrown away)
                            // the item.
                            if (storageStrategy == ItemStream.STORE_ALWAYS) {
                                assertNotNull("Item not found", item2);
                                assertEquivalent(filteredItem, item2);
                            }
                            filteredItem = null;
                        }
                        date = new Date();
                        long duration = date.getTime() - start;
                        if (duration >= 500) { // Report excessive duration
                            print("(" + duration + ")");
                        }
                        if (item2 != null) {
                            itemsRemoved++;
                            deltaOut++;

                            if (progress) {
                                print(".");
                                System.out.flush();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            print("Reader ended");
        }
    }

    class MyWriter implements Runnable {
        //
        // This thread will write items to a stream at intervals.
        //
        PersistentItemStream is = null;
        Transaction tran = null;
        int count = 0;
        boolean counting = false;

        MyWriter(PersistentItemStream is, MessageStore ms) {

            this.is = is;
            this.tran = ms.getTransactionFactory().createAutoCommitTransaction();

            if (maxItems > 0) {
                counting = true;
            }
        }

        @Override
        public void run() {

            while (!counting || count < maxItems) {
                try {
                    if (putInterval > 0) {
                        Thread.sleep(putInterval);
                    }
                    StressItem item = new StressItem();
                    is.addItem(item, tran);
                    count++;
                    deltaIn++;
                    // Save the last item written, so that it can be removed  
                    // by one of the reader threads.
                    synchronized (itemLock) {
                        if (lastItem == null) {
                            lastItem = item;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            print("Finished writing (" + count + ") items to stream");
            threadsRunning--;
        }
    }

    public class StressItem extends Item {
        public StressItem() {
            super();
        }

        @Override
        public int getStorageStrategy() {
            return storageStrategy;
        }

        @Override
        public List<DataSlice> getPersistentData() {
            List<DataSlice> list = new ArrayList<DataSlice>(1);
            list.add(new DataSlice(new byte[itemSize]));
            return list;
        }

        @Override
        public void restore(final List<DataSlice> dataSlices) {
            DataSlice slice = dataSlices.get(0);
            assertEquals("Incorrect data length", itemSize, slice.getLength());
        }
    }
}
