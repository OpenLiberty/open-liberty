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
package com.ibm.ws.sib.msgstore.test.persistence;

/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- -------------------------------------------
 * 355038          23/03/06 gareth   Delete STORE_MAYBE items in batches at startup
 * SIB0112i.ms.1   22/02/07 gareth   Changes to handling of STORE_MAYBE Items
 * 522787          22/05/08 susana   Allow tests to run against DataStore as well as FileStore
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;

public class StoreMaybeCleanupTest extends MessageStoreTestCase {
    public StoreMaybeCleanupTest(String name) {
        super(name);

        //turnOnTrace();
    }

    /**
     */
    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        /*
         * StoreMaybeCleanupTest test = new StoreMaybeCleanupTest("testStoreMaybeItemCleanup");
         * test.setPersistence(persistence);
         * suite.addTest(test);
         */

        StoreMaybeCleanupTest test = new StoreMaybeCleanupTest("testStoreMaybeItemPutGet");
        test.setPersistence(persistence);
        suite.addTest(test);

        /*
         * test = new StoreMaybeCleanupTest("testStoreMaybeStreamCleanup");
         * test.setPersistence(persistence);
         * suite.addTest(test);
         */

        return suite;
    }

    /*
     * public void testStoreMaybeItemCleanup()
     * {
     * print("************* testStoreMaybeItemCleanup **************");
     * print("*                                                    *");
     * 
     * if (PERSISTENCE != null)
     * {
     * print("* PersistenceManager Used:                 - SUCCESS *");
     * 
     * int length = PERSISTENCE.length();
     * 
     * print("* - " + PERSISTENCE.substring(length - 48) + " *");
     * }
     * 
     * MessageStore MS = null;
     * 
     * try
     * {
     * // Start the MS to give us a handle to use, this should SUCCEED
     * try
     * {
     * MS = createAndStartMessageStore(true, PERSISTENCE);
     * 
     * print("* Create a MessageStore                    - SUCCESS *");
     * }
     * catch (Exception e)
     * {
     * print("* Create a MessageStore                    - FAILED  *");
     * fail("Exception thrown during MessageStore create!");
     * }
     * 
     * long before, after = 0;
     * ItemStream root = null;
     * // Try to create an ItemStream, this should SUCCEED
     * try
     * {
     * print("* Create Root ItemStream:                            *");
     * 
     * root = createPersistentRootItemStream(MS);
     * 
     * before = root.getStatistics().getTotalItemCount();
     * 
     * print("* - Root ItemStream size: " + before + "                          *");
     * print("* Create Root ItemStream                   - SUCCESS *");
     * }
     * catch (Exception e)
     * {
     * print("* Create Root ItemStream                   - FAILED  *");
     * fail("Exception thrown during root ItemStream create!");
     * }
     * 
     * // Try to create an AutoCommitTransaction, this should SUCCEED
     * ExternalAutoCommitTransaction tran = MS.getTransactionFactory().createAutoCommitTransaction();
     * print("* Create AutoCommitTransaction             - SUCCESS *");
     * 
     * // Try to add 10000 STORE_MAYBE Items to the ItemStream, this should SUCCEED
     * try
     * {
     * for (int i=0; i<10000; i++)
     * {
     * Item putItem = new StoreMaybeItem();
     * root.addItem(putItem, tran);
     * }
     * 
     * print("* Put 10000 Items:                                   *");
     * 
     * after = root.getStatistics().getTotalItemCount();
     * 
     * print("* - Root ItemStream size: " + after + "                      *");
     * 
     * if (after == 10000)
     * {
     * print("* Put 10000 Items                          - SUCCESS *");
     * }
     * else
     * {
     * print("* Put 10000 Items                          - FAILED  *");
     * fail("Failed to put all 10000 STORE_MAYBE Items!");
     * }
     * }
     * catch (Exception e)
     * {
     * e.printStackTrace();
     * print("* Put 10000 Items                          - FAILED  *");
     * fail("Unexpected exception thrown whilst adding an Item!");
     * }
     * 
     * // Stop the message store, this should SUCCEED.
     * if (MS != null)
     * {
     * stopMessageStore(MS);
     * 
     * print("* Stop MessageStore                        - SUCCESS *");
     * }
     * 
     * // Set the log file sizes and restart the MessageStore, this should SUCCEED.
     * try
     * {
     * MS = createMessageStore(false, PERSISTENCE);
     * 
     * // If using DataStore, ensure that we can support a customer requesting DELETE
     * if (PERSISTENCE == DATABASE_PERSISTENCE) {
     * MS.setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
     * +MessageStoreConstants.PROP_USE_DELETE_INSTEAD_OF_TRUNCATE_AT_STARTUP
     * , "true");
     * }
     * 
     * MS.start();
     * 
     * print("* Restart the MessageStore                 - SUCCESS *");
     * }
     * catch(Exception e)
     * {
     * print("* Restart the MessageStore                 - FAILED  *");
     * fail("Unexpected exception thrown during MessageStore restart!");
     * }
     * 
     * // Find the ItemStream to check, this should SUCCEED
     * try
     * {
     * root = MS.findFirstMatching(null);
     * 
     * print("* Find Root ItemStream                     - SUCCESS *");
     * }
     * catch (Exception e)
     * {
     * print("* Find Root ItemStream                     - FAILED  *");
     * e.printStackTrace(System.err);
     * fail("Exception thrown finding root ItemStream after restart: "+e.getMessage());
     * }
     * 
     * try
     * {
     * print("* ItemStream empty after restart:                    *");
     * 
     * after = root.getStatistics().getTotalItemCount();
     * 
     * print("* - Root ItemStream size: " + after + "                          *");
     * 
     * if (after == 0)
     * {
     * print("* ItemStream empty after restart           - SUCCESS *");
     * }
     * else
     * {
     * print("* ItemStream empty after restart           - FAILED *");
     * fail("STORE_MAYBE items found after restart!");
     * }
     * }
     * catch (SevereMessageStoreException smse)
     * {
     * smse.printStackTrace();
     * fail("Exception caught getting root item stream length!");
     * }
     * }
     * finally
     * {
     * if (MS != null)
     * {
     * stopMessageStore(MS);
     * 
     * print("* Stop MessageStore                        - SUCCESS *");
     * }
     * 
     * print("*                                                    *");
     * print("************* testStoreMaybeItemCleanup **************");
     * }
     * }
     */

    public void testStoreMaybeItemPutGet() {
        print("************* testStoreMaybeItemPutGet ***************");
        print("*                                                    *");

        MessageStore MS = null;

        try {
            // Start the MS to give us a handle to use, this should SUCCEED
            try {
                MS = createAndStartMessageStore(true, PERSISTENCE);

                print("* Create a MessageStore                    - SUCCESS *");
            } catch (Exception e) {
                print("* Create a MessageStore                    - FAILED  *");
                fail("Exception thrown during MessageStore create!");
            }

            long before, after = 0;
            ItemStream root = null;
            // Try to create an ItemStream, this should SUCCEED
            try {
                print("* Create Root ItemStream:                            *");

                root = createPersistentRootItemStream(MS);

                before = root.getStatistics().getTotalItemCount();

                print("* - Root ItemStream size: " + before + "                          *");
                print("* Create Root ItemStream                   - SUCCESS *");
            } catch (Exception e) {
                print("* Create Root ItemStream                   - FAILED  *");
                fail("Exception thrown during root ItemStream create!");
            }

            // Try to create an AutoCommitTransaction, this should SUCCEED
            ExternalAutoCommitTransaction tran = MS.getTransactionFactory().createAutoCommitTransaction();
            print("* Create AutoCommitTransaction             - SUCCESS *");

            // Try to add 10000 STORE_MAYBE Items to the ItemStream, this should SUCCEED
            try {
                for (int i = 0; i < 10000; i++) {
                    Item putItem = new StoreMaybeItem();
                    root.addItem(putItem, tran);
                }

                print("* Put 10000 Items:                                   *");

                after = root.getStatistics().getTotalItemCount();

                print("* - Root ItemStream size: " + after + "                      *");

                if (after == 10000) {
                    print("* Put 10000 Items                          - SUCCESS *");
                } else {
                    print("* Put 10000 Items                          - FAILED  *");
                    fail("Failed to put all 10000 STORE_MAYBE Items!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                print("* Put 10000 Items                          - FAILED  *");
                fail("Unexpected exception thrown whilst adding an Item!");
            }

            // Try to remove 10000 STORE_MAYBE Items from the ItemStream, this should SUCCEED
            try {
                Item getItem = null;
                int getCount = 0;

                for (int i = 0; i < 10000; i++) {
                    getItem = root.removeFirstMatchingItem(null, tran);

                    if (getItem != null && getItem instanceof StoreMaybeItem) {
                        getCount++;
                    }
                }

                if (getCount == 10000) {
                    print("* Get 10000 Items                          - SUCCESS *");
                } else {
                    print("* Get 10000 Items                          - FAILED  *");
                    fail("Failed to get all 10000 STORE_MAYBE Items!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                print("* Get 10000 Items                          - FAILED  *");
                fail("Unexpected exception thrown whilst getting an Item!");
            }

            try {
                print("* ItemStream empty after gets:                       *");

                after = root.getStatistics().getTotalItemCount();

                print("* - Root ItemStream size: " + after + "                          *");

                if (after == 0) {
                    print("* ItemStream empty after gets              - SUCCESS *");
                } else {
                    print("* ItemStream empty after gets              - FAILED *");
                    fail("STORE_MAYBE items found after gets!");
                }
            } catch (SevereMessageStoreException smse) {
                smse.printStackTrace();
                fail("Exception caught getting root item stream length!");
            }
        } finally {
            if (MS != null) {
                stopMessageStore(MS);

                print("* Stop MessageStore                        - SUCCESS *");
            }

            print("*                                                    *");
            print("************* testStoreMaybeItemPutGet ***************");
        }
    }
/*
 * public void testStoreMaybeStreamCleanup()
 * {
 * print("************ testStoreMaybeStreamCleanup *************");
 * print("*                                                    *");
 * 
 * MessageStore MS = null;
 * 
 * try
 * {
 * // Start the MS to give us a handle to use, this should SUCCEED
 * try
 * {
 * MS = createAndStartMessageStore(true, PERSISTENCE);
 * 
 * print("* Create a MessageStore                    - SUCCESS *");
 * }
 * catch (Exception e)
 * {
 * print("* Create a MessageStore                    - FAILED  *");
 * fail("Exception thrown during MessageStore create!");
 * }
 * 
 * long before, after = 0;
 * ItemStream root = null;
 * // Try to create an ItemStream, this should SUCCEED
 * try
 * {
 * print("* Create Root ItemStream:                            *");
 * 
 * root = createPersistentRootItemStream(MS);
 * 
 * before = root.getStatistics().getTotalItemCount();
 * 
 * print("* - Root ItemStream size: " + before + "                          *");
 * print("* Create Root ItemStream                   - SUCCESS *");
 * }
 * catch (Exception e)
 * {
 * print("* Create Root ItemStream                   - FAILED  *");
 * fail("Exception thrown during root ItemStream create!");
 * }
 * 
 * // Try to create an AutoCommitTransaction, this should SUCCEED
 * ExternalAutoCommitTransaction tran = MS.getTransactionFactory().createAutoCommitTransaction();
 * print("* Create AutoCommitTransaction             - SUCCESS *");
 * 
 * // Try to create a STORE_MAYBE ItemStream, this should SUCCEED
 * try
 * {
 * print("* Create STORE_MAYBE ItemStream:                     *");
 * 
 * root.addItemStream(new StoreMaybeItemStream(), tran);
 * 
 * before = root.getStatistics().getTotalItemCount();
 * 
 * print("* - Root ItemStream size: " + before + "                          *");
 * print("* Create STORE_MAYBE ItemStream            - SUCCESS *");
 * }
 * catch (Exception e)
 * {
 * print("* Create STORE_MAYBE ItemStream            - FAILED  *");
 * fail("Exception thrown during STORE_MAYBE ItemStream create!");
 * }
 * 
 * // Stop the message store, this should SUCCEED.
 * if (MS != null)
 * {
 * stopMessageStore(MS);
 * 
 * print("* Stop MessageStore                        - SUCCESS *");
 * }
 * 
 * // Set the log file sizes and restart the MessageStore, this should SUCCEED.
 * try
 * {
 * MS = createAndStartMessageStore(false, PERSISTENCE);
 * 
 * print("* Restart the MessageStore                 - SUCCESS *");
 * }
 * catch(Exception e)
 * {
 * print("* Restart the MessageStore                 - FAILED  *");
 * fail("Unexpected exception thrown during MessageStore restart!");
 * }
 * 
 * // Find the ItemStream to check, this should SUCCEED
 * try
 * {
 * root = MS.findFirstMatching(null);
 * 
 * print("* Find Root ItemStream                     - SUCCESS *");
 * }
 * catch (Exception e)
 * {
 * print("* Find Root ItemStream                     - FAILED  *");
 * e.printStackTrace(System.err);
 * fail("Exception thrown finding root ItemStream after restart: "+e.getMessage());
 * }
 * 
 * try
 * {
 * print("* ItemStream empty after restart:                    *");
 * 
 * after = root.getStatistics().getTotalItemCount();
 * 
 * print("* - Root ItemStream size: " + after + "                          *");
 * 
 * if (after == 0)
 * {
 * print("* ItemStream empty after restart           - SUCCESS *");
 * }
 * else
 * {
 * print("* ItemStream empty after restart           - FAILED *");
 * fail("STORE_MAYBE items found after restart!");
 * }
 * }
 * catch (SevereMessageStoreException smse)
 * {
 * smse.printStackTrace();
 * fail("Exception caught getting root item stream length!");
 * }
 * }
 * finally
 * {
 * if (MS != null)
 * {
 * stopMessageStore(MS);
 * 
 * print("* Stop MessageStore                        - SUCCESS *");
 * }
 * 
 * print("*                                                    *");
 * print("************ testStoreMaybeStreamCleanup *************");
 * }
 * }
 */
}
