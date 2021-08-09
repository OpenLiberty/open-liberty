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
package com.ibm.ws.sib.msgstore.test.persistence.dispatcher;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 496154          11/04/08 gareth   Improve spilling performance 
 * ============================================================================
 */

import java.util.Random;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.test.persistence.StoreMaybeItem;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

import junit.framework.TestSuite;

public class SpillingThroughputTest extends MessageStoreTestCase
{
    private ItemStream root = null;
    private MessageStore MS = null;

    // Number of Items to be Put/Get:
    private int itemCount = 10000;
    private int itemSize  = 100;   // 100 bytes


    public SpillingThroughputTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        SpillingThroughputTest test = new SpillingThroughputTest("testSpillingPutsThenGets");
        test.setPersistence(persistence);

        suite.addTest(test);

        test = new SpillingThroughputTest("testSpillingPutsUpdatesAndGets");
        test.setPersistence(persistence);

        suite.addTest(test);

        test = new SpillingThroughputTest("testSpillingMixedPutsAndGets");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testSpillingPutsThenGets()
    {
        print("|-----------------------------------------------------");
        print("| SpillingPutsThenGets:");
        print("|----------------------");
        print("|");

        long   count;
        long   beforeTime;
        long   afterTime;
        double lastLandmark;
        double averageTime;
        Transaction auto = null;

        try
        {
            // Start the MessageStore, this should SUCCEED
            try
            {
                MS = createAndStartMessageStore(true, PERSISTENCE);

                auto = MS.getTransactionFactory().createAutoCommitTransaction();

                print("| Started MessageStore");
                print("| - Persistence manager used: "+PERSISTENCE);
            }
            catch (Exception e)
            {
                print("| Start MessageStore   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during MessageStore.start(): "+e.getMessage());
            }


            // Try to create an ItemStream, this should SUCCEED
            try
            {
                root = createPersistentRootItemStream(MS);

                print("| Created Root ItemStream.");
            }
            catch (Exception e)
            {
                print("| Create Root ItemStream   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during root ItemStream create: "+e.getMessage());
            }

            print("|");

            // Put and get 2000 items to warm up the JVM.
            try
            {
                print("| Warming up JVM");

                for (int i=1;i <= 2000; i++)
                {
                    Item putItem = new StoreMaybeItem(1024); // 1K message

                    root.addItem(putItem, auto);
                }

                for (int i=1;i <= 2000; i++)
                {
                    Item getItem = root.removeFirstMatchingItem(null, auto);
                }

                print("| JVM warmed up");
            }
            catch (Exception e)
            {
                print("| JVM warmup   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst warming up the JVM: "+e.getMessage());
            }

            print("|");
            print("| Item size: "+itemSize+"bytes");
            print("|");

            // Add items to the stream in one big lump.
            try
            {
                lastLandmark = 1000.0;

                print("| Putting "+itemCount+" Items:");

                beforeTime = System.currentTimeMillis();

                for (int i=1;i <= itemCount; i++)
                {
                    Item putItem = new StoreMaybeItem(itemSize);

                    root.addItem(putItem, auto);

                    if (i == lastLandmark)
                    {
                        count = root.getStatistics().getTotalItemCount();

                        afterTime = System.currentTimeMillis();

                        averageTime = (afterTime - beforeTime) / lastLandmark;

                        print("| - Item "+i+": "+putItem+
                            "\n| - Root ItemStream size: "+count+
                            "\n| - Average put time after "+i+" Items: "+averageTime+"ms"+
                            "\n|");

                        lastLandmark += 1000.0;
                    }
                }

                print("| All Items added to stream");
            }
            catch (Exception e)
            {
                print("| Item Put   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst adding an Item: "+e.getMessage());
            }

            print("|");

            try
            {
                Thread.sleep(20000);
            }
            catch (Exception e)
            {
                print("| Wait for 20 seconds   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst waiting for 20 seconds: "+e.getMessage());
            }

            // Get items off the stream in one big lump.
            try
            {
                lastLandmark = 1000.0;

                print("| Getting "+itemCount+" Items:");

                beforeTime = System.currentTimeMillis();

                for (int i=1;i <= itemCount; i++)
                {
                    Item getItem = root.removeFirstMatchingItem(null, auto);

                    if (i == lastLandmark)
                    {
                        count = root.getStatistics().getTotalItemCount();

                        afterTime = System.currentTimeMillis();

                        averageTime = (afterTime - beforeTime) / lastLandmark;

                        print("| - Item "+i+": "+getItem+
                            "\n| - Root ItemStream size: "+count+
                            "\n| - Average get time after "+i+" Items: "+averageTime+"ms"+
                            "\n|");

                        lastLandmark += 1000.0;
                    }
                }

                print("| All Items removed from stream");
            }
            catch (Exception e)
            {
                print("| Item Get   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst getting an Item: "+e.getMessage());
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: "+t.getMessage());
        }
        finally
        {
            if (MS != null)
            {
                stopMessageStore(MS);

                print("| Stopped MessageStore");
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    public void testSpillingPutsUpdatesAndGets()
    {
        print("|-----------------------------------------------------");
        print("| SpillingPutsUpdatesAndGets:");
        print("|----------------------------");
        print("|");

        long   count;
        long   beforeTime;
        long   afterTime;
        double lastLandmark;
        double averageTime;
        Transaction auto = null;

        try
        {
            // Start the MessageStore, this should SUCCEED
            try
            {
                MS = createAndStartMessageStore(true, PERSISTENCE);

                auto = MS.getTransactionFactory().createAutoCommitTransaction();

                print("| Started MessageStore");
                print("| - Persistence manager used: "+PERSISTENCE);
            }
            catch (Exception e)
            {
                print("| Start MessageStore   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during MessageStore.start(): "+e.getMessage());
            }


            // Try to create an ItemStream, this should SUCCEED
            try
            {
                root = createPersistentRootItemStream(MS);

                print("| Created Root ItemStream.");
            }
            catch (Exception e)
            {
                print("| Create Root ItemStream   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during root ItemStream create: "+e.getMessage());
            }

            print("|");

            // Put and get 2000 items to warm up the JVM.
            try
            {
                print("| Warming up JVM");

                for (int i=1;i <= 2000; i++)
                {
                    Item putItem = new StoreMaybeItem(1024); // 1K message

                    root.addItem(putItem, auto);
                }

                for (int i=1;i <= 2000; i++)
                {
                    Item getItem = root.removeFirstMatchingItem(null, auto);
                }

                print("| JVM warmed up");
            }
            catch (Exception e)
            {
                print("| JVM warmup   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst warming up the JVM: "+e.getMessage());
            }

            print("|");
            print("| Item size: "+itemSize+"bytes");
            print("|");

            // Add items to the stream in one big lump.
            try
            {
                lastLandmark = 1000.0;

                print("| Putting "+itemCount+" Items:");

                beforeTime = System.currentTimeMillis();

                for (int i=1;i <= itemCount; i++)
                {
                    Item putItem = new StoreMaybeItem(itemSize);

                    root.addItem(putItem, auto);

                    if (i == lastLandmark)
                    {
                        count = root.getStatistics().getTotalItemCount();

                        afterTime = System.currentTimeMillis();

                        averageTime = (afterTime - beforeTime) / lastLandmark;

                        print("| - Item "+i+": "+putItem+
                            "\n| - Root ItemStream size: "+count+
                            "\n| - Average put time after "+i+" Items: "+averageTime+"ms"+
                            "\n|");

                        lastLandmark += 1000.0;
                    }
                }

                print("| All Items added to stream");
            }
            catch (Exception e)
            {
                print("| Item Put   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst adding an Item: "+e.getMessage());
            }

            print("|");

            // Update a random selection of items.
            try
            {
                print("| Updating random selection of Items:");

                Random updateThis = new Random(System.currentTimeMillis());

                NonLockingCursor cursor = root.newNonLockingItemCursor(null);

                int singleUpdates = 0;
                int doubleUpdates = 0;

                for (int i=1;i <= itemCount; i++)
                {
                    StoreMaybeItem updateItem = (StoreMaybeItem)cursor.next();

                    // Update a random selection of Items
                    if (updateThis.nextBoolean())
                    {
                        updateItem.setPersistentData(new byte[2 * itemSize]);

                        updateItem.requestUpdate(auto);

                        singleUpdates++;

                        // Randomly update some Items twice
                        if (updateThis.nextBoolean())
                        {
                            updateItem.setPersistentData(new byte[3 * itemSize]);

                            updateItem.requestUpdate(auto);

                            doubleUpdates++;
                        }
                    }
                }

                print("| - Single updates made: "+singleUpdates);
                print("| - Double updates made: "+doubleUpdates);
                print("| Updates complete");
            }
            catch (Exception e)
            {
                print("| Item updates   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst updating Items: "+e.getMessage());
            }

            print("|");

            // Get items off the stream in one big lump.
            try
            {
                lastLandmark = 1000.0;

                print("| Getting "+itemCount+" Items:");

                beforeTime = System.currentTimeMillis();

                for (int i=1;i <= itemCount; i++)
                {
                    Item getItem = root.removeFirstMatchingItem(null, auto);

                    if (i == lastLandmark)
                    {
                        count = root.getStatistics().getTotalItemCount();

                        afterTime = System.currentTimeMillis();

                        averageTime = (afterTime - beforeTime) / lastLandmark;

                        print("| - Item "+i+": "+getItem+
                            "\n| - Root ItemStream size: "+count+
                            "\n| - Average get time after "+i+" Items: "+averageTime+"ms"+
                            "\n|");

                        lastLandmark += 1000.0;
                    }
                }

                print("| All Items removed from stream");
            }
            catch (Exception e)
            {
                print("| Item Get   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst getting an Item: "+e.getMessage());
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: "+t.getMessage());
        }
        finally
        {
            if (MS != null)
            {
                stopMessageStore(MS);

                print("| Stopped MessageStore");
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    public void testSpillingMixedPutsAndGets()
    {
        print("|-----------------------------------------------------");
        print("| SpillingMixedPutsAndGets:");
        print("|--------------------------");
        print("|");

        Transaction auto = null;

        try
        {
            // Start the MessageStore, this should SUCCEED
            try
            {
                MS = createAndStartMessageStore(true, PERSISTENCE);

                auto = MS.getTransactionFactory().createAutoCommitTransaction();

                print("| Started MessageStore");
                print("| - Persistence manager used: "+PERSISTENCE);
            }
            catch (Exception e)
            {
                print("| Start MessageStore   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during MessageStore.start(): "+e.getMessage());
            }

            // Try to create an ItemStream, this should SUCCEED
            try
            {

                root = createPersistentRootItemStream(MS);

                print("| Created Root ItemStream.");
            }
            catch (Exception e)
            {
                print("| Create Root ItemStream   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during root ItemStream create: "+e.getMessage());
            }

            print("|");

            // Put and get 2000 items to warm up the JVM.
            try
            {
                print("| Warming up JVM");

                for (int i=1;i <= 2000; i++)
                {
                    Item putItem = new StoreMaybeItem(1024); // 1K message

                    root.addItem(putItem, auto);
                }

                for (int i=1;i <= 2000; i++)
                {
                    Item getItem = root.removeFirstMatchingItem(null, auto);
                }

                print("| JVM warmed up");
            }
            catch (Exception e)
            {
                print("| JVM warmup   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst warming up the JVM: "+e.getMessage());
            }

            print("|");
            print("| Item size: "+itemSize+"bytes");
            print("|");

            PutThread putThread = null;
            GetThread getThread = null;

            try
            {
                putThread = new PutThread();

                putThread.start();

                print("| PutThread started");
            }
            catch (Exception e)
            {
                print("| PutThread start   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst starting PutThread: "+e.getMessage());
            }

            // Need to sleep for a while to allow some messages to build up.
            print("|");
            Thread.sleep(500);
            print("| Sleep 500ms");

            try
            {
                getThread = new GetThread();

                getThread.start();

                print("| GetThread started");
            }
            catch (Exception e)
            {
                print("| GetThread start   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst starting GetThread: "+e.getMessage());
            }


            try
            {
                putThread.join();

                getThread.join();
            }
            catch (Exception e)
            {
                print("| Wait for threads   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst waiting for threads to finish: "+e.getMessage());
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: "+t.getMessage());
        }
        finally
        {
            if (MS != null)
            {
                stopMessageStore(MS);

                print("| Stopped MessageStore");
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    private class PutThread extends Thread
    {
        public void run()
        {
            long   count;
            long   beforeTime;
            long   afterTime;
            double lastLandmark;
            double averageTime;
            Transaction tran;

            // Add items to the stream
            try
            {
                lastLandmark = 1000.0;

                print("| Putting "+itemCount+" Items:");

                beforeTime = System.currentTimeMillis();

                for (int i=1;i <= itemCount; i++)
                {
                    tran = MS.getTransactionFactory().createAutoCommitTransaction();

                    Item putItem = new StoreMaybeItem(itemSize);

                    root.addItem(putItem, tran);

                    if (i == lastLandmark)
                    {
                        count = root.getStatistics().getTotalItemCount();

                        afterTime = System.currentTimeMillis();

                        averageTime = (afterTime - beforeTime) / lastLandmark;

                        print("| - Item "+i+": "+putItem+
                            "\n| - Root ItemStream size: "+count+
                            "\n| - Average put time after "+i+" Items: "+averageTime+"ms"+
                            "\n|");

                        lastLandmark += 1000.0;
                    }

                    // Give getter thread a chance.
                    Thread.yield();
                }

                print("| All Items added to stream");
            }
            catch (Exception e)
            {
                print("| Item Put   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst adding an Item: "+e.getMessage());
            }
        }
    }

    private class GetThread extends Thread
    {
        public void run()
        {
            long   count;
            long   beforeTime;
            long   afterTime;
            double lastLandmark;
            double averageTime;
            Transaction tran;

            // Get items off the stream
            try
            {
                lastLandmark = 1000.0;

                print("| Getting "+itemCount+" Items:");

                beforeTime = System.currentTimeMillis();

                for (int i=1; i <= itemCount; i++)
                {
                    tran = MS.getTransactionFactory().createAutoCommitTransaction();

                    Item getItem = root.removeFirstMatchingItem(null, tran);

                    if (i == lastLandmark)
                    {
                        count = root.getStatistics().getTotalItemCount();

                        afterTime = System.currentTimeMillis();

                        averageTime = (afterTime - beforeTime) / lastLandmark;

                        print("| - Item "+i+": "+getItem+
                            "\n| - Root ItemStream size: "+count+
                            "\n| - Average get time after "+i+" Items: "+averageTime+"ms"+
                            "\n|");

                        lastLandmark += 1000.0;
                    }

                    // Give putter thread a chance.
                    Thread.yield();
                }

                print("| All Items removed from stream");
            }
            catch (Exception e)
            {
                print("| Item Get   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst getting an Item: "+e.getMessage());
            }
        }
    }
}




