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
package com.ibm.ws.sib.msgstore.test.cache;

/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * SIB0112le.ms.1  07/02/07 gareth   Add restoreData() method to Item
 * 668676.1        12/11/10 skavitha restoreData method takes boolean parameter
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransaction;

public class RestoreDataFromPersistenceTest extends MessageStoreTestCase {
    public RestoreDataFromPersistenceTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        RestoreDataFromPersistenceTest test = new RestoreDataFromPersistenceTest("testRestoreDataFromPersistence");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testRestoreDataFromPersistence() {
        print("|-----------------------------------------------------");
        print("| RestoreDataFromPersistence:");
        print("|----------------------------");
        print("|");

        if (PERSISTENCE != null) {
            print("| PersistenceManager Used:");
            print("| - " + PERSISTENCE);
        }

        Transaction auto = null;
        MSDelegatingLocalTransaction local = null;

        ItemStream root = null;
        MessageStore MS = null;

        RestoreDataItem message = null;

        long before, after;

        try {

            // Start the MessageStore, this should SUCCEED
            try {
                MS = createAndStartMessageStore(true, PERSISTENCE);

                local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();

                print("| Started MessageStore");
            } catch (Exception e) {
                print("| Start MessageStore   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during MessageStore.start(): " + e.getMessage());
            }

            // Try to create an ItemStream, this should SUCCEED
            try {
                print("| Create Root ItemStream:");

                root = createPersistentRootItemStream(MS);

                before = root.getStatistics().getTotalItemCount();

                print("| - Root ItemStream size: " + before);
                print("| Created Root ItemStream");
            } catch (Exception e) {
                print("| Create Root ItemStream   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during root ItemStream create: " + e.getMessage());
            }

            // Try to add an Item to the ItemStream, this should SUCCEED
            try {
                print("| Put Message:");

                message = new RestoreDataItem("RestoreDataFromPersistence");

                print("| - " + message);

                root.addItem(message, local);
                after = root.getStatistics().getTotalItemCount();

                print("| - Root ItemStream size: " + after);
                print("| Put Message");
            } catch (Exception e) {
                print("| Put Message   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown whilst adding an Item: " + e.getMessage());
            }

            // Commit the transaction, this should SUCCEED
            try {
                local.commit();

                print("| Commit local transaction");
            } catch (Exception e) {
                print("| Commit local transaction   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during commit of transaction: " + e.getMessage());
            }

            // Release the Items data
            message.releaseData();
            print("| Data released from Item:");
            print("| - " + message);

            try {
                print("| Restore Item data:");

                message.restoreData(true);

                print("| - " + message);
            } catch (Exception e) {
                print("| Restore Item data   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown restoring Item's data: " + e.getMessage());
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: " + t.getMessage());
        } finally {
            if (MS != null) {
                stopMessageStore(MS);

                print("| Stopped MessageStore");
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }
}
