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
 * ------------- -------- -------- --------------------------------------------
 * 168081        03/09/03  gareth   GlobalTransaction Support (Local Clients)
 * 181930        17/11/03  gareth   XA Recovery Support
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import javax.transaction.xa.XAResource;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingXAResource;

public class MSDelegatingXAResourcePutGetTest extends MessageStoreTestCase {
    public MSDelegatingXAResourcePutGetTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        MSDelegatingXAResourcePutGetTest test = new MSDelegatingXAResourcePutGetTest("testMSDelegatingXAResourcePutGet");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testMSDelegatingXAResourcePutGet() {
        print("************ MSDelegatingXAResourcePutGet ************");
        print("*                                                    *");

        if (PERSISTENCE != null) {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        MSAutoCommitTransaction auto = null;
        MSDelegatingXAResource global = null;
        ItemStream root = null;
        MessageStore MS = null;
        NullXid xid1 = new NullXid("XID1");
        NullXid xid2 = new NullXid("XID2");
        NullXid xid3 = new NullXid("XID3");
        NullXid xid4 = new NullXid("XID4");

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
        print("* Create AutoTransaction and XAResource:             *");

        auto = (MSAutoCommitTransaction) MS.getTransactionFactory().createAutoCommitTransaction();
        global = (MSDelegatingXAResource) MS.getTransactionFactory().createXAResource();

        if ((auto != null) && (global != null)) {
            StringBuffer out1 = new StringBuffer(auto.toString());
            int length1 = out1.length();

            print("* - " + out1.substring(length1 - 48) + " *");

            StringBuffer out2 = new StringBuffer(global.toString());
            int length2 = out2.length();

            print("* - " + out2.substring(length2 - 48) + " *");

            print("* Create AutoTransaction and XAResource:   - SUCCESS *");
        } else {
            print("* Create AutoTransaction and XAResource:   - FAILED  *");
            fail("Auto and XAResource create failed!");
        }

        // Try to add an Item to the ItemStream, this should SUCCEED
        try {
            print("* Item Put Using AutoTransaction:                    *");

            Item putItem = new NullPersistentItem("MSDelegatingXAResourcePutGet1");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, auto);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put Using AutoTransaction           - SUCCESS *");
        } catch (Exception e) {
            print("* Item Put Using AutoTransaction           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst adding an Item: " + e.getMessage());
        }

        // Associate the XAResource, this should SUCCEED
        try {
            global.start(xid1, XAResource.TMNOFLAGS);
            global.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource (XID1)              - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource (XID1)              - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during association of XAResource: " + e.getMessage());
        }

        // Try to add an Item to the ItemStream, this should SUCCEED
        try {
            print("* Item Put:                                          *");

            Item putItem = new NullPersistentItem("MSDelegatingXAResourcePutGet2");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, global);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put                                 - SUCCESS *");
        } catch (Exception e) {
            print("* Item Put                                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst adding an Item: " + e.getMessage());
        }

        // Try to get an Item from the ItemStream, this should SUCCEED
        try {
            print("* Item Get:                                          *");

            Item gotItem = root.removeFirstMatchingItem(null, global);
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

        // Disassociate the XAResource, this should SUCCEED
        try {
            global.end(xid1, XAResource.TMSUCCESS);

            print("* Disassociate XAResource (XID1)           - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource (XID1)           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Commit the XAResource using 1PC, this should SUCCEED
        try {
            print("* Commit XAResource (XID1,1PC):                      *");

            global.commit(xid1, true);

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Commit XAResource (XID1,1PC)             - SUCCESS *");
        } catch (Exception e) {
            print("* Commit XAResource (XID1,1PC)             - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource commit: " + e.getMessage());
        }

        // Associate the XAResource with XID2, this should SUCCEED
        try {
            global.start(xid2, XAResource.TMNOFLAGS);
            global.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource (XID2)              - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource (XID2)              - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Try to add an Item to the ItemStream, this should SUCCEED
        try {
            print("* Item Put:                                          *");

            Item putItem = new NullPersistentItem("MSDelegatingXAResourcePutGet3");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, global);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put                                 - SUCCESS *");
        } catch (Exception e) {
            print("* Item Put                                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst adding an Item: " + e.getMessage());
        }

        // Try to get an Item from the ItemStream, this should SUCCEED
        try {
            print("* Item Get:                                          *");

            Item gotItem = root.removeFirstMatchingItem(null, global);
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

        // Disassociate the XAResource, this should SUCCEED
        try {
            global.end(xid2, XAResource.TMSUCCESS);

            print("* Disassociate XAResource (XID2)           - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource (XID2)           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Prepare the XAResource, this should SUCCEED
        try {
            print("* Prepare XAResource (XID2):                         *");

            global.prepare(xid2);

            print("* Prepare XAResource (XID2)                - SUCCESS *");
        } catch (Exception e) {
            print("* Prepare XAResource (XID2)                - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Commit the XAResource, this should SUCCEED
        try {
            print("* Commit XAResource (XID2,2PC):                      *");

            global.commit(xid2, false);

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Commit XAResource (XID2,2PC)             - SUCCESS *");
        } catch (Exception e) {
            print("* Commit XAResource (XID2,2PC)             - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource commit: " + e.getMessage());
        }

        // Associate the XAResource with XID3, this should SUCCEED
        try {
            global.start(xid3, XAResource.TMNOFLAGS);
            global.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource (XID3)              - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource (XID3)              - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Try to add an Item to the ItemStream, this should SUCCEED
        try {
            print("* Item Put:                                          *");

            Item putItem = new NullPersistentItem("MSDelegatingXAResourcePutGet4");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, global);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put                                 - SUCCESS *");
        } catch (Exception e) {
            print("* Item Put                                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst adding an Item: " + e.getMessage());
        }

        // Try to get an Item from the ItemStream, this should SUCCEED
        try {
            print("* Item Get:                                          *");

            Item gotItem = root.removeFirstMatchingItem(null, global);
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

        // Disassociate the XAResource, this should SUCCEED
        try {
            global.end(xid3, XAResource.TMFAIL);

            print("* Disassociate XAResource (XID3)           - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource (XID3)           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Rollback the XAResource, this should SUCCEED
        try {
            print("* Rollback XAResource (XID3):                        *");

            global.rollback(xid3);

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Rollback XAResource (XID3)               - SUCCESS *");
        } catch (Exception e) {
            print("* Rollback XAResource (XID3)               - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource commit: " + e.getMessage());
        }

        // Associate the XAResource with XID3, this should SUCCEED
        try {
            global.start(xid4, XAResource.TMNOFLAGS);
            global.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource (XID4)              - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource (XID4)              - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Try to add an Item to the ItemStream, this should SUCCEED
        try {
            print("* Item Put:                                          *");

            Item putItem = new NullPersistentItem("MSDelegatingXAResourcePutGet5");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, global);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put                                 - SUCCESS *");
        } catch (Exception e) {
            print("* Item Put                                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown whilst adding an Item: " + e.getMessage());
        }

        // Try to get an Item from the ItemStream, this should SUCCEED
        try {
            print("* Item Get:                                          *");

            Item gotItem = root.removeFirstMatchingItem(null, global);
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

        // Disassociate the XAResource, this should SUCCEED
        try {
            global.end(xid4, XAResource.TMFAIL);

            print("* Disassociate XAResource (XID4)           - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource (XID4)           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Prepare the XAResource, this should SUCCEED
        try {
            print("* Prepare XAResource (XID4):                         *");

            global.prepare(xid4);

            print("* Prepare XAResource (XID4)                - SUCCESS *");
        } catch (Exception e) {
            print("* Prepare XAResource (XID4)                - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Rollback the XAResource, this should SUCCEED
        try {
            print("* Rollback XAResource (XID4,2PC):                    *");

            global.rollback(xid4);

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Rollback XAResource (XID4,2PC)           - SUCCESS *");
        } catch (Exception e) {
            print("* Rollback XAResource (XID4,2PC)           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource commit: " + e.getMessage());
        }

        if (MS != null) {
            stopMessageStore(MS);
        }

        print("*                                                    *");
        print("************ MSDelegatingXAResourcePutGet ************");
    }
}
