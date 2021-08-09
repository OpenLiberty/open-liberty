/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore.test;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestSuite;
import test.common.SharedOutputManager;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.CacheStatistics;
import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.ws.sib.utils.DataSlice;

/**
 * Test expiry of Items in the MessageStore.
 */
public class ExpiryTest extends MessageStoreTestCase {
    final long addItems = 1000;
    final long expiryMilliseconds = 10;
    final long streamMaximumOccupancy = 10000;

    int putTasks = 10;
    int getTasks = 2;

    int storageStrategy = ItemStream.STORE_NEVER;
    String[] storageStrategyName = new String[] {"INVALID", "STORE_NEVER", "STORE_MAYBE", "STORE_EVENTUALLY", "STORE_ALWAYS" };

    int itemSize = 10;

    AtomicBoolean readerDraining = new AtomicBoolean(false);
    AtomicLong itemsAdded = new AtomicLong();
    AtomicLong itemsExpired = new AtomicLong();
    AtomicLong itemsRemoved = new AtomicLong();
    final Lock expiryStartedLock = new ReentrantLock();
    boolean expiryStarted = false;
    final Condition expiryStartedCondition = expiryStartedLock.newCondition();

    public ExpiryTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        ExpiryTest test = new ExpiryTest("testStoreNeverItemExpiry");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ExpiryTest("testStoreAlwaysItemExpiry");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new ExpiryTest("testLockedItemExpiry");
        test.setPersistence(persistence);
        suite.addTest(test);
        
        test = new ExpiryTest("testExpirerStartup");
        test.setPersistence(persistence);
        suite.addTest(test);
        
        return suite;
    }

    @Test
    public void testStoreNeverItemExpiry() {
        storageStrategy = AbstractItem.STORE_NEVER;
        itemExpiry();
        long dropped = report();
        assertTrue("Incorrect ItemsAdded, itemsAdded="+itemsAdded.get() , itemsAdded.get() == addItems*putTasks*2);
        // Some items are usually dropped, however, this is not guaranteed because it depends on the behaviour
        // of the garbage collector. Don't make it a hard error if no items are dropped. 
        //assertTrue("No items dropped", dropped > 0);
        if (dropped == 0) getLog().error("No items dropped!");
        assertTrue("No items expired, itemsExpired="+itemsExpired , itemsExpired.get() > 0);
        // Some items are usually removed, however, this is not guaranteed because it is possible that 
        // the items all expire before the ItemReader sees them Don't make it a hard error if no items are removed. 
        //assertTrue("No items removed, itemsRemoved="+itemsRemoved , itemsRemoved.get() > 0);
        if (itemsRemoved.get() == 0) getLog().error("No items removed!");    
    }

    @Test
    public void testStoreAlwaysItemExpiry() {
        storageStrategy = AbstractItem.STORE_ALWAYS;
        itemExpiry();
        long dropped = report();
        assertTrue("Incorrect ItemsAdded, itemsAdded="+itemsAdded , itemsAdded.get() == addItems*putTasks*2);
        assertTrue("Items were dropped dropped="+dropped, dropped == 0);
        assertTrue("No items expired, itemsExpired="+itemsExpired , itemsExpired.get() > 0);
        assertTrue("No items expired, itemsRemoved="+itemsRemoved , itemsRemoved.get() > 0);
    }

    @Test
    public void testLockedItemExpiry() {
        //SharedOutputManager outputMgr = SharedOutputManager.getInstance();
        //outputMgr.trace("com.ibm.ws.sib.*=all:com.ibm.ejs.util.am.*=all");
        //outputMgr.trace("com.ibm.ws.sib.msgstore.expiry.*=all:com.ibm.ejs.util.am.*=all");
        //outputMgr.captureStreams();
        //print("Tracing to: .../com.ibm.ws.messaging.msgstore/build/libs/trace-logs/");
        
        storageStrategy = AbstractItem.STORE_NEVER;
        
        // Provide an executor for theExpirers alarmManager to use, note that the Alarm Manager only uses one thread.
        AlarmManager alarmManager = new TestAlarmManager(Executors.newScheduledThreadPool(10));
        
        MessageStore messageStore = null;
        try {
            // Make the expirer run almost continuously, an expiry interval < 1 means stop the expirer.
            System.setProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_EXPIRY_INTERVAL, Integer.toString(1));
            messageStore = createAndStartMessageStore(true, PERSISTENCE);
            Transaction tran = messageStore.getTransactionFactory().createAutoCommitTransaction();
            ItemStream itemStream = new ItemStream();
            messageStore.add(itemStream, tran);
            
            // Put an item on the ItemStream and the consume it under a transaction, before it expires.
            long expiryMilliseconds = 1000;
            ExpiryTestItem item1 = new ExpiryTestItem(expiryMilliseconds);
            itemStream.addItem(item1, tran);
            ExternalLocalTransaction tran1 = messageStore.getTransactionFactory().createLocalTransaction();           
            Item item = itemStream.removeFirstMatchingItem(alwaysMatchesFilter, tran1);
            assertNotNull("First item not retrieved, itemsExpired="+itemsExpired , item );
           
            // Put another item on the stream. 
            ExpiryTestItem item2 = new ExpiryTestItem(expiryMilliseconds);
            itemStream.addItem(item2, tran);
            
            // Wait for item2 to expire, item1 cannot expire because it is being retrieved in a transaction..
            expiryStartedLock.lock();
            print(new Date()+" Waiting for item2 to expire, itemsExpired="+itemsExpired);
            try {
                if (!expiryStarted) {
                   expiryStartedCondition.await(10, TimeUnit.SECONDS);
                   assertTrue("Expiry not started itemsExpired=" + itemsExpired, expiryStarted);          
                }
                
            } finally {
                expiryStartedLock.unlock();
            }
            
            // Finish retrieving the first item.
            tran1.commit();
            
        } catch (Exception exception) {
            exception.printStackTrace();
            fail(exception.toString());
        } finally {
            // outputMgr.dumpStreams();
            // Make stdout and stderr "normal"
            // outputMgr.restoreStreams();
                           
            try {
                stopMessageStore(messageStore);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }  
        
        // We should see only the first item has expired, the second one was removed before it expired.
        assertTrue("Incorrect items expired, itemsExpired="+itemsExpired , itemsExpired.get() ==1);     
    }
    
    @Test
    public void testExpirerStartup() {
        //SharedOutputManager outputMgr = SharedOutputManager.getInstance();
        //outputMgr.trace("com.ibm.ws.sib.msgstore.expiry.*=all");
        //outputMgr.trace("com.ibm.ws.sib.msgstore.*=all");
        //outputMgr.captureStreams();
        //print("Tracing to: .../com.ibm.ws.messaging.msgstore/build/libs/trace-logs/");
        
        // Provide an executor for theExpirers alarmManager to use, note that the Alarm Manager only uses one thread.
        AlarmManager alarmManager = new TestAlarmManager(Executors.newScheduledThreadPool(10));
        
        MessageStore messageStore = null;
        try {
            long expiryMilliseconds = 1000;
            CountDownLatch expiryFinished = new CountDownLatch(2);
            class StartupTestItem extends Item {
                @Override
                public long getMaximumTimeInStore() {return expiryMilliseconds;}
                @Override
                public int getStorageStrategy() {return AbstractItem.STORE_ALWAYS;}
                @Override
                public boolean canExpireSilently() {return false;}
                @Override
                public void eventExpiryNotification(Transaction transaction) throws SevereMessageStoreException {                    
                    expiryFinished.countDown();
                }
            };
            
            // Make the Expirer run almost continuously, an expiry interval < 1 means stop the expirer.
            System.setProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_EXPIRY_INTERVAL, Integer.toString(1));
            messageStore = createAndStartMessageStore(true, PERSISTENCE);
             
            ItemStream rootItemStream = createPersistentRootItemStream(messageStore);
            
            // Put an item on the ItemStream then stop the message store until after it has expired.            
            Transaction tran = messageStore.getTransactionFactory().createAutoCommitTransaction();
            rootItemStream.addItem(new StartupTestItem(), tran);            
           
            messageStore.expirerStop();
            Thread.sleep(expiryMilliseconds);
            
            messageStore.expirerStart();
            // Put another item on the stream. 
            rootItemStream.addItem(new StartupTestItem(), tran);
            
            // Wait for both items to expire.
            print(new Date()+" Waiting for two items to expire, itemsExpired="+itemsExpired);
            expiryFinished.await(expiryMilliseconds+10, TimeUnit.MILLISECONDS); 
            long itemsRemaining = expiryFinished.getCount();
            assertTrue("ItemsNotExpired != 0 itemsRemaining=" + itemsRemaining, itemsRemaining == 0);      
            
        } catch (Exception exception) {
            exception.printStackTrace();
            fail(exception.toString());
        
        } finally {
            //outputMgr.dumpStreams();
            // Make stdout and stderr "normal"
            //outputMgr.restoreStreams();
            try {
                stopMessageStore(messageStore);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }           
        }  
       
    }
    
    private void itemExpiry() {

        print("---------- "+this.getClass().getSimpleName()+" arguments ----------");
        print("PutThreads=" + putTasks+ " GetThreads=" + getTasks);
        print("ItemSize=" + itemSize);
        print("AddItems=" + addItems);
        print("ExpiryMilliseconds=" + expiryMilliseconds);
        print("StreamMaximumOccupancy=" + streamMaximumOccupancy);
        print("StorageStrategy="+ storageStrategy+ " "+ storageStrategyName[storageStrategy]);

        print("---------- "+this.getClass().getSimpleName()+" starting --------");

        // Provide an executor for theExpirers alarmManager to use, note that the Alarm Manager only uses one thread.
        AlarmManager alarmManager = new TestAlarmManager(Executors.newScheduledThreadPool(10));

        // Create as many threads as needed to run this test without queueing.
        ExecutorService executor = new ForkJoinPool(putTasks+getTasks);

        MessageStore messageStore = null;
        try {
            // Make the expirer run almost continuously, an expiry interval < 1 means stop the expirer.
            System.setProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_EXPIRY_INTERVAL, Integer.toString(1));
            // Make the permanent store large enough to potentially hold all of the items we will produce.
            System.setProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MAXIMUM_SIZE, Integer.toString(100000000));
            messageStore = createAndStartMessageStore(true, PERSISTENCE);
            Transaction tran = messageStore.getTransactionFactory().createAutoCommitTransaction();
            PersistentItemStream itemStream = new PersistentItemStream();
            messageStore.add(itemStream, tran);

            List<Future<String>> activeWriterList = new ArrayList<Future<String>>();
            List<Future<String>> activeReaderList = new ArrayList<Future<String>>();

            print("Starting " + putTasks + " writers");
            for (int i = 0; i < putTasks; i++) {
                Future<String> future = executor.submit(new ItemWriter(itemStream, messageStore, addItems));
                activeWriterList.add(future);
            }

            expiryStartedLock.lock();
            print(new Date()+" Waiting for items to start expiring itemsExpired="+itemsExpired);
            try {
                if (!expiryStarted) {
                   expiryStartedCondition.await(10, TimeUnit.SECONDS);
                   assertTrue("Expiry not started itemsAdded=" + itemsAdded + " itemsExpired=" + itemsExpired, expiryStarted);          
                }
                
            } finally {
                expiryStartedLock.unlock();
            }

            // Encourage the garbage collector to start dropping items.
            System.gc();
            
            print("Starting " + getTasks + " readers");
            for (int i = 0; i < getTasks; i++) {
                Future<String> future = executor.submit(new ItemReader(itemStream, messageStore));
                activeReaderList.add(future);
            }
            print("Starting " + putTasks + " writers");
            for (int i = 0; i < putTasks; i++) {
                Future<String> future = executor.submit(new ItemWriter(itemStream, messageStore, addItems));
                activeWriterList.add(future);
            }

            Statistics statistics = itemStream.getStatistics();
            print("Statistics: TotalItemCount=" + statistics.getTotalItemCount());
            print("            AvailableItemCount=" + statistics.getAvailableItemCount());
            print("            AddingItemCount=" + statistics.getAddingItemCount());
            print("            ExpiringItemCount=" + statistics.getExpiringItemCount());

            print("IsSpilling="+itemStream.isSpilling());
            print("ExpiryIndexSize="+messageStore.getExpiryIndexSize());

            CacheStatistics cacheStatistics;
            if (storageStrategy == AbstractItem.STORE_NEVER)
                cacheStatistics = messageStore.getNonStoredCacheStatistics();
            else
                cacheStatistics = messageStore.getStoredCacheStatistics();
            print("CacheStatistics: CurrentCount="+cacheStatistics.getCurrentCount());
            print("                 CurrentSize="+cacheStatistics.getCurrentSize());
            print("                 TotalCount="+cacheStatistics.getTotalCount());

            // Wait for the active writers to finish.
            Exception writerException = waitFor(activeWriterList);

            readerDraining.set(true);
            print("Readers draining, stream size=" + itemStream.getStatistics().getTotalItemCount());

            // Wait for the active readers to finish.
            Exception readerException = waitFor(activeReaderList);

            if (writerException != null)
                fail("Item writer failed itemsAdded="+ itemsAdded + " exception="+writerException.toString());
            if (readerException != null)
                fail("Item reader failed itemsRemoved=" + itemsRemoved+ " exception="+readerException.toString());

            print("IsSpilling="+itemStream.isSpilling());

            print("----------" + this.getClass().getSimpleName() + " ended ----------");

        } catch (Exception exception) {
            exception.printStackTrace();
            fail(exception.toString());
        } finally {
            executor.shutdown();
            try {
                //messageStore.expirerStop();
                stopMessageStore(messageStore);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }
    }

    private Exception waitFor(List<Future<String>> activeTaskList) {
        Exception taskException = null;
        for(Future<String> future : activeTaskList) {
            try {
                print(new Date()+ ": "+future.get(100, TimeUnit.SECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                StringWriter stringWriter =  new StringWriter();
                exception.printStackTrace(new PrintWriter(stringWriter));
                print(stringWriter.toString());
                taskException = exception;
            }
        }
        return taskException;
    }

    private long report() {
        long added = itemsAdded.get();
        long expired = itemsExpired.get();
        long removed = itemsRemoved.get();
        long dropped = added-expired-removed;
        print("Items added="+added+" expired="+expired+" removed="+removed+" dropped="+dropped);
        return dropped;
    }

    class TestAlarmManager extends AlarmManager {
        TestAlarmManager(ScheduledExecutorService executorService) {
            AlarmManager.executorService=executorService;
        }
    }

    class ItemReader implements Callable<String> {
        final PersistentItemStream itemStream;
        final Transaction tran;
        long removed = 0;

        ItemReader(PersistentItemStream itemStream, MessageStore ms) {
            this.itemStream = itemStream;
            this.tran = ms.getTransactionFactory().createAutoCommitTransaction();
        }

        @Override
        public String call() throws Exception {
            while (!readerDraining.get() || itemStream.getStatistics().getTotalItemCount() > 0) {
                Item item = itemStream.removeFirstMatchingItem(alwaysMatchesFilter, tran);
                if (item != null) {
                    removed++;
                    itemsRemoved.incrementAndGet();
                }
            }

            return (Thread.currentThread().getName() + " Finished reading. This thread removed="+removed+ " total itemsRemoved=" + itemsRemoved + " itemsExpired="+itemsExpired );
        }
    }

    Filter alwaysMatchesFilter = new Filter() {
        @Override
        public boolean filterMatches(AbstractItem item) throws MessageStoreException {
            return true;
        }
    };

    public class ItemWriter implements Callable<String> {
        final PersistentItemStream itemStream;
        final Transaction tran;
        final long itemsToWrite;

        ItemWriter(PersistentItemStream itemStream, MessageStore ms, long itemsToWrite) {
            this.itemStream = itemStream;
            this.tran = ms.getTransactionFactory().createAutoCommitTransaction();
            this.itemsToWrite = itemsToWrite;
        }

        @Override
        public String call() throws Exception {
            for (long itemIndex = 0; itemIndex < itemsToWrite; itemIndex++) {
                ExpiryTestItem item = new ExpiryTestItem(expiryMilliseconds);
                itemStream.addItem(item, tran);
                itemsAdded.incrementAndGet();
                // Uncomment the following to cap the number of items held in the store, waiting to be consumed or expire.
                /*
                while (itemStream.getStatistics().getTotalItemCount() > streamMaximumOccupancy) {
                    print(Thread.currentThread().getName() + " " + itemStream.getStatistics().getTotalItemCount() + "> streamMaximumOccupancy");
                    Thread.sleep(1);
                }
                */
            }
            return (Thread.currentThread().getName() + " Finished. itemsToWrite=" + itemsToWrite + " itemsAdded="+itemsAdded);
        }
    }

    public class ExpiryTestItem extends Item {
        final long expiryMilliseconds;

        public ExpiryTestItem(long expiryMilliseconds) {
            super();
            this.expiryMilliseconds = expiryMilliseconds;
        }

        @Override
        public long getMaximumTimeInStore() {return expiryMilliseconds;}
        @Override
        public int getStorageStrategy() {return storageStrategy;}
        @Override
        public boolean canExpireSilently() {return false;}
        @Override
        public void eventExpiryNotification(Transaction transaction) throws SevereMessageStoreException {
            long items = itemsExpired.incrementAndGet();
            if (items == 1) {
                expiryStartedLock.lock();
                expiryStarted = true;
                try {
                    expiryStartedCondition.signal();
                } finally {
                  expiryStartedLock.unlock();
                }
            }
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
