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
 * 168080        16/07/03  gareth   Local Transaction Support (Local Clients)
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

public class AutoCommitPutGetTest extends MessageStoreTestCase
{
    public AutoCommitPutGetTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        AutoCommitPutGetTest test = new AutoCommitPutGetTest("testAutoCommitPutGet");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public void testAutoCommitPutGet()
    {
        print("****************** AutoCommitPutGet ******************");
        print("*                                                    *");

        if (PERSISTENCE != null)
        {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        ExternalAutoCommitTransaction tran = null;
        ItemStream  root = null;
        MessageStore  MS = null;

        long before, after;

        // Try to create an ItemStream, this should SUCCEED
        try
        {
            print("* Create Root ItemStream:                            *");

            MS   = createAndStartMessageStore(true, PERSISTENCE);
            root = createPersistentRootItemStream(MS);

            before = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + before + "                          *");
            print("* Create Root ItemStream                   - SUCCESS *");
        }
        catch (Exception e)
        {
            print("* Create Root ItemStream                   - FAILED  *");
            fail("Exception thrown during root ItemStream create!");
        }


        // Try to create an AutoCommitTransaction, this should SUCCEED
        print("* Create AutoCommitTransaction:                      *");

        tran = MS.getTransactionFactory().createAutoCommitTransaction();

        if (tran != null)
        {
            StringBuffer out = new StringBuffer(tran.toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            print("* Create AutoCommitTransaction             - SUCCESS *");

            tran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");
        }
        else
        {
            print("* Create AutoCommitTransaction             - FAILED  *");
            fail("AutoCommitTransaction create failed!");
        }
        

        // Try to add an Item to the ItemStream, this should SUCCEED
        try
        {
            print("* Item Put:                                          *");

            Item putItem = new NullPersistentItem("AutoCommitPutGet");
            StringBuffer out = new StringBuffer(((Object) putItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            root.addItem(putItem, tran);
            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Put                                 - SUCCESS *");
        }
        catch (Exception e)
        {
            print("* Item Put                                 - FAILED  *");
            fail("Exception thrown whilst adding an Item!");
        }


        if (tran != null)
        {
            tran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");
        }
        

        // Try to get an Item from the ItemStream, this should SUCCEED
        try
        {
            print("* Item Get:                                          *");

            Item gotItem = root.removeFirstMatchingItem(null, tran);
            StringBuffer out = new StringBuffer(((Object) gotItem).toString());
            int length = out.length();

            print("* - " + out.substring(length - 48) + " *");

            after = root.getStatistics().getTotalItemCount();

            print("* - Root ItemStream size: " + after + "                          *");
            print("* Item Get                                 - SUCCESS *");
        }
        catch (Exception e)
        {
            print("* Item Get                                 - FAILED  *");
            fail("Exception thrown whilst getting an Item!");
        }


        if (MS != null)
        {
            stopMessageStore(MS);
        }

        print("*                                                    *");
        print("****************** AutoCommitPutGet ******************");
    }
}
