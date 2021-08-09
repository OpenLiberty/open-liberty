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
package com.ibm.ws.sib.msgstore.test.transactions;

/*
 * Change activity:
 *
 * Reason          Date    Origin   Description
 * ------------- -------- -------- ---------------------------------------
 * 168080        16/07/03  gareth   Local Transaction Support (Local Clients)
 * 168081        02/09/03  gareth   GlobalTransaction Support (Local Clients)
 * 181930        17/11/03  gareth   XA Recovery Support
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransaction;

public class MSDelegatingLocalTransactionPutGetTest extends MessageStoreTestCase {
    private static boolean TEST_ALL_PERSISTENCE_MANAGERS;

    public MSDelegatingLocalTransactionPutGetTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        MSDelegatingLocalTransactionPutGetTest test = new MSDelegatingLocalTransactionPutGetTest("testMSDelegatingLocalTransactionPutGet");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testMSDelegatingLocalTransactionPutGet() {
        print("********* MSDelegatingLocalTransactionPutGet *********");
        print("*                                                    *");

        if (PERSISTENCE != null) {
            print("* PersistenceManager Used:                 - SUCCESS *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        ExternalAutoCommitTransaction auto = null;
        MSDelegatingLocalTransaction local = null;
        ItemStream root = null;
        MessageStore MS = null;

        long before, after;

        // Try to create an ItemStream, this should SUCCEED
        try {
            print("* Create Root ItemStream:                            *");

            MS = createAndStartMessageStore(true, PERSISTENCE);
            root = createPersistentRootItemStream(MS);

            before = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + before + "                          *");
            print("* Create Root ItemStream                   - SUCCESS *");
        } catch (Exception e) {
            print("* Create Root ItemStream                   - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during root ItemStream create: " + e.getMessage());
        }

        // Try to create an Auto and LocalTransaction, this should SUCCEED
        print("* Create Auto and LocalTransaction:                  *");

        auto = MS.getTransactionFactory().createAutoCommitTransaction();
        local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();

        if ((auto != null) && (local != null)) {
            local.registerCallback(new NullTransactionCallback(this));

            StringBuffer out1 = new StringBuffer(auto.toString());
            int length1 = out1.length();

            print("* - " + out1.substring(length1 - 48) + " *");

            StringBuffer out2 = new StringBuffer(local.toString());
            int length2 = out2.length();

            print("* - " + out2.substring(length2 - 48) + " *");

            print("* Create Auto and LocalTransaction         - SUCCESS *");
        } else {
            print("* Create Auto and LocalTransaction         - FAILED  *");
            fail("Auto and LocalTransaction create failed!");
        }

        // Try to add an Item to the ItemStream, this should SUCCEED
        try {
            print("* Item Put 1:                                        *");

            Item putItem = new NullPersistentItem("MSDelegatingLocalTransactionPutGet1");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, local);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put 1                               - SUCCESS *");
        } catch (Exception e) {
            print("* Item Put 1                               - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst adding an Item: " + e.getMessage());
        }

        // Try to add an Item to the ItemStream, this should SUCCEED
        try {
            print("* Item Put 2:                                        *");

            Item putItem = new NullPersistentItem("MSDelegatingLocalTransactionPutGet2");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, local);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put 2                               - SUCCESS *");
        } catch (Exception e) {
            print("* Item Put 2                               - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst adding an Item: " + e.getMessage());
        }

        // Commit the LocalTransaction, this should SUCCEED
        try {
            print("* Commit LocalTransaction:                           *");

            local.commit();

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Commit LocalTransaction                  - SUCCESS *");
        } catch (Exception e) {
            print("* Commit LocalTransaction                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during LocalTransaction commit: " + e.getMessage());
        }

        // Create a new LocalTransaction, this should SUCCEED
        local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();

        if (local != null) {
            local.registerCallback(new NullTransactionCallback(this));
            print("* Create LocalTransaction                  - SUCCESS *");
        } else {
            print("* Create LocalTransaction                  - FAILED  *");
            fail("LocalTransaction create failed!");
        }

        // Try to get an Item from the ItemStream, this should SUCCEED
        try {
            print("* Item Get:                                          *");

            Item gotItem = root.removeFirstMatchingItem(null, local);
            StringBuffer out = new StringBuffer(((Object) gotItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Get                                 - SUCCESS *");
        } catch (Exception e) {
            print("* Item Get                                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst getting an Item: " + e.getMessage());
        }

        // Commit the LocalTransaction, this should SUCCEED
        try {
            print("* Commit LocalTransaction:                           *");

            local.commit();

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Commit LocalTransaction                  - SUCCESS *");
        } catch (Exception e) {
            print("* Commit LocalTransaction                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during LocalTransaction commit: " + e.getMessage());
        }

        // Create a new LocalTransaction, this should SUCCEED
        local = (MSDelegatingLocalTransaction) MS.getTransactionFactory().createLocalTransaction();

        if (local != null) {
            local.registerCallback(new NullTransactionCallback(this));
            print("* Create LocalTransaction                  - SUCCESS *");
        } else {
            print("* Create LocalTransaction                  - FAILED  *");
            fail("LocalTransaction create failed!");
        }

        // Try to get an Item from the ItemStream, this should SUCCEED
        try {
            print("* Item Get:                                          *");

            Item gotItem = root.removeFirstMatchingItem(null, local);
            StringBuffer out = new StringBuffer(((Object) gotItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Get                                 - SUCCESS *");
        } catch (Exception e) {
            print("* Item Get                                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst getting an Item: " + e.getMessage());
        }

        // Rollback the LocalTransaction, this should SUCCEED
        try {
            print("* Rollback LocalTransaction:                         *");

            local.rollback();

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Rollback LocalTransaction                - SUCCESS *");
        } catch (Exception e) {
            print("* Rollback LocalTransaction                - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during LocalTransaction rollback: " + e.getMessage());
        }

        if (MS != null) {
            stopMessageStore(MS);
        }

        print("*                                                    *");
        print("********* MSDelegatingLocalTransactionPutGet *********");
    }
}
