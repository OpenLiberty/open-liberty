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
package com.ibm.ws.sib.msgstore.test.persistence.objectManager;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- -------------------------------------------
 * SIB0112i.ms.1   23/02/07 gareth   Changes to handling of STORE_MAYBE Items
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * ============================================================================
 */

import javax.transaction.xa.*;

import com.ibm.ws.sib.msgstore.*;
import com.ibm.ws.sib.msgstore.transactions.ExternalXAResource;

import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.test.persistence.StoreMaybeItem;
import com.ibm.ws.sib.msgstore.test.transactions.LongXid;

import junit.framework.TestSuite;

public class StoreMaybeTwoPhaseCommitTest extends MessageStoreTestCase
{
    public StoreMaybeTwoPhaseCommitTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    /**
     * This is a Object Manager specific test so no persistence
     * choice is provided.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(StoreMaybeTwoPhaseCommitTest.class);

        return suite;
    }

    public void testStoreMaybeTwoPhasePutGet()
    {
        print("|-----------------------------------------------------");
        print("| StoreMaybeTwoPhasePutGet:");
        print("|--------------------------");
        print("|");

        MessageStore MS = null;
        Xid xid = null;

        try
        {
            // Start the MS to give us a handle to use, this should SUCCEED
            try
            {
                MS = createAndStartMessageStore(true, OBJECTMANAGER_PERSISTENCE);

                print("| Created a MessageStore");
            }
            catch (Exception e)
            {
                print("| Create a MessageStore   !!!FAILED!!!");
                fail("Exception thrown during MessageStore create!");
            }

            long before, after = 0;
            ItemStream root = null;
            // Try to create an ItemStream, this should SUCCEED
            try
            {
                print("| Create Root ItemStream:");

                root = createPersistentRootItemStream(MS);

                before = root.getStatistics().getTotalItemCount();

                print("| - Root ItemStream size: " + before);
            }
            catch (Exception e)
            {
                print("| Create Root ItemStream   !!!FAILED!!!");
                fail("Exception thrown during root ItemStream create!");
            }

            // Try to create an AutoCommitTransaction, this should SUCCEED
            ExternalXAResource tran = MS.getTransactionFactory().createXAResource();
            print("| Created XAResource");


            // Try to add 10000 STORE_MAYBE Items to the ItemStream, this should SUCCEED
            try
            {

                for (int i=0; i<10000; i++)
                {
                    // Use LongXid here as it randomly generates
                    // it's transaction id components.
                    xid = new LongXid();

                    // Start association
                    tran.start(xid, XAResource.TMNOFLAGS);

                    // Put Item
                    Item putItem = new StoreMaybeItem();
                    root.addItem(putItem, tran);                    

                    // End association
                    tran.end(xid, XAResource.TMSUCCESS);

                    // Prepare
                    tran.prepare(xid);

                    // Commit 2PC
                    tran.commit(xid, false);
                }

                print("| Put 10000 Items:");

                after = root.getStatistics().getTotalItemCount();

                print("| - Root ItemStream size: " + after);

                if (after != 10000)
                {
                    print("| Put 10000 Items   !!!FAILED!!!");
                    fail("Failed to put all 10000 STORE_MAYBE Items!");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                print("| Put 10000 Items   !!!FAILED!!!");
                fail("Unexpected exception thrown whilst adding an Item!");
            }

            // Try to remove 10000 STORE_MAYBE Items from the ItemStream, this should SUCCEED
            try
            {
                Item getItem = null;
                int getCount = 0;

                for (int i=0; i<10000; i++)
                {
                    // Use LongXid here as it randomly generates
                    // it's transaction id components.
                    xid = new LongXid();

                    // Start association
                    tran.start(xid, XAResource.TMNOFLAGS);

                    // Get Item
                    getItem = root.removeFirstMatchingItem(null, tran);

                    // End association
                    tran.end(xid, XAResource.TMSUCCESS);

                    // Prepare
                    tran.prepare(xid);

                    // Commit 2PC
                    tran.commit(xid, false);

                    if (getItem != null && getItem instanceof StoreMaybeItem)
                    {
                        getCount++;
                    }
                }

                if (getCount == 10000)
                {
                    print("| Got 10000 Items");
                }
                else
                {
                    print("| Get 10000 Items   !!!FAILED!!!");
                    fail("Failed to get all 10000 STORE_MAYBE Items!");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                print("| Get 10000 Items   !!!FAILED!!!");
                fail("Unexpected exception thrown whilst getting an Item!");
            }

            try
            {
                print("| ItemStream empty after gets:");

                after = root.getStatistics().getTotalItemCount();

                print("| - Root ItemStream size: " + after);

                if (after != 0)
                {
                    print("| ItemStream empty after gets   !!!FAILED!!!");
                    fail("STORE_MAYBE items found after gets!");
                }
            }
            catch (SevereMessageStoreException smse)
            {
            	smse.printStackTrace();
            	fail("Severe exception caught getting size of root ItemStream!");
            }
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
}

