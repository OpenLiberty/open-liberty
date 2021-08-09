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
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 416338          27/02/07 gareth   Add BasePerformanceTest to SniffBucket
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.test.transactions.NullPersistentItem;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransaction;

public class BasePerformanceTest extends MessageStoreTestCase {
    public BasePerformanceTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite(BasePerformanceTest.class);

        return suite;
    }

/*
 * public void testBasePerformanceDatastore() {
 * print("|-----------------------------------------------------");
 * print("| DatastoreBasePerformance:");
 * print("|--------------------------");
 * print("|");
 * 
 * print("| PersistenceManager Used:");
 * print("| - " + DATABASE_PERSISTENCE);
 * print("|");
 * 
 * MSDelegatingLocalTransaction local = null;
 * 
 * ItemStream root = null;
 * MessageStore MS = null;
 * NullPersistentItem message = null;
 * 
 * long before, after, average;
 * 
 * // Start the MessageStore, this should SUCCEED
 * try {
 * print("| Test Start MS -> Put Message -> Get Message -> Stop MS");
 * 
 * before = 0;
 * 
 * for (int i = 0; i < 11; i++) {
 * // If this is the second run then start the timer
 * // this will give us one run to get the classes
 * // loaded and hopefully give us a more accurate
 * // average.
 * if (i == 1) {
 * before = System.currentTimeMillis();
 * }
 * 
 * MS = createAndStartMessageStore(true, DATABASE_PERSISTENCE);
 * 
 * local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();
 * 
 * root = createPersistentRootItemStream(MS);
 * 
 * message = new NullPersistentItem("TestPutGet");
 * 
 * root.addItem(message, local);
 * 
 * local.commit();
 * 
 * local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();
 * 
 * message = (NullPersistentItem) root.removeFirstMatchingItem(null, local);
 * 
 * local.commit();
 * 
 * stopMessageStore(MS);
 * 
 * MS = null;
 * }
 * 
 * after = System.currentTimeMillis();
 * 
 * average = (after - before) / 10;
 * 
 * print("|");
 * print("| Expected time to run test");
 * print("|----------------------------------------");
 * print("| Thinkpad T40 1x1.5Ghz PM 2048MB: 6600ms");
 * print("| NetVista M42 1x2.3Ghz P4 1024MB: 4600ms");
 * print("| NetVista M52 2x3.3Ghz P4 2548MB: 2600ms");
 * print("|----------------------------------------");
 * print("|               Actual time taken: " + average + "ms");
 * } catch (Throwable t) {
 * t.printStackTrace(System.err);
 * fail("Exception thrown during test: " + t.getMessage());
 * } finally {
 * if (MS != null) {
 * stopMessageStore(MS);
 * }
 * 
 * print("|");
 * print("|------------------------ END ------------------------");
 * }
 * }
 */
    public void testBasePerformanceFilestore() {
        print("|-----------------------------------------------------");
        print("| FilestoreBasePerformance:");
        print("|--------------------------");
        print("|");

        print("| PersistenceManager Used:");
        print("| - " + OBJECTMANAGER_PERSISTENCE);
        print("|");

        MSDelegatingLocalTransaction local = null;

        ItemStream root = null;
        MessageStore MS = null;
        NullPersistentItem message = null;

        long before, after, average;

        // Start the MessageStore, this should SUCCEED
        try {
            print("| Test Start MS -> Put Message -> Get Message -> Stop MS");

            before = 0;

            for (int i = 0; i < 11; i++) {
                // If this is the second run then start the timer
                // this will give us one run to get the classes
                // loaded and hopefully give us a more accurate 
                // average.
                if (i == 1) {
                    before = System.currentTimeMillis();
                }

                MS = createAndStartMessageStore(true, OBJECTMANAGER_PERSISTENCE);

                local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();

                root = createPersistentRootItemStream(MS);

                message = new NullPersistentItem("TestPutGet");

                root.addItem(message, local);

                local.commit();

                local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();

                message = (NullPersistentItem) root.removeFirstMatchingItem(null, local);

                local.commit();

                stopMessageStore(MS);

                MS = null;
            }

            after = System.currentTimeMillis();

            average = (after - before) / 10;

            print("|");
            print("| Expected time to run test");
            print("|---------------------------------------");
            print("| Thinkpad T40 1x1.5Ghz PM 2048MB: 560ms");
            print("| NetVista M42 1x2.3Ghz P4 1024MB: 520ms");
            print("| NetVista M52 2x3.3Ghz P4 2548MB: 340ms");
            print("|---------------------------------------");
            print("|               Actual time taken: " + average + "ms");
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: " + t.getMessage());
        } finally {
            if (MS != null) {
                stopMessageStore(MS);
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }
}
