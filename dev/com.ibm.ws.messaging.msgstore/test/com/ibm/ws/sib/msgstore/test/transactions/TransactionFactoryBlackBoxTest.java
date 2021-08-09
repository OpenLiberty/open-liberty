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
 * 168080        10/06/03  gareth   Local Transaction Support (Local Clients)
 * 168081        17/09/03  gareth   GlobalTransaction Support (Local Clients)
 * 184121        12/01/04  gareth   Remove IsolatedLocalTransaction support
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import javax.transaction.xa.XAResource;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;

public class TransactionFactoryBlackBoxTest extends MessageStoreTestCase
{
    private MessageStore MS;

    public TransactionFactoryBlackBoxTest(String name)
    {
        super(name);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        TransactionFactoryBlackBoxTest test = new TransactionFactoryBlackBoxTest("testTransactionFactoryBlackBox");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testTransactionFactoryBlackBox()
    {
        print("************* TransactionFactoryBlackBox *************");
        print("*                                                    *");

        if (PERSISTENCE != null)
        {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        // Try to create an ItemStream, this should SUCCEED
        try
        {
            print("* Create MessageStore:                               *");

              MS = createAndStartMessageStore(true, PERSISTENCE);

            print("* Create MessageStore                      - SUCCESS *");
        }
        catch (Exception e)
        {
            print("* Create MessageStore                      - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during MessageStore create: "+e.getMessage());
        }


        // Try to create an AutoCommitTransaction, this should SUCCEED
        print("* Create AutoCommitTransaction:                      *");

        ExternalAutoCommitTransaction tran = MS.getTransactionFactory().createAutoCommitTransaction();

        if (tran != null)
        {
            StringBuffer out = new StringBuffer(tran.toString());
            int       length = out.length();

            print("* - "+out.substring(length - 48)+" *");

            print("* Create AutoCommitTransaction             - SUCCESS *");
        }
        else
        {
            print("* Create AutoCommitTransaction             - FAILED  *");
            fail("AutoCommitTransaction create failed!");
        }

        
        // Try to create an LocalResource, this should SUCCEED
        print("* Create LocalTransaction:                           *");

        ExternalLocalTransaction local = MS.getTransactionFactory().createLocalTransaction();

        if (local != null)
        {
            StringBuffer out = new StringBuffer(local.toString());
            int       length = out.length();

            print("* - "+out.substring(length - 48)+" *");

            print("* Create LocalTransaction                  - SUCCESS *");
        }
        else
        {
            print("* Create LocalTransaction                  - FAILED  *");
            fail("LocalTransaction create failed!");
        }

        
        // Try to create an XAResource, this should SUCCEED
        print("* Create XAResource:                                 *");

        XAResource xa = MS.getTransactionFactory().createXAResource();

        if (xa != null)
        {
            StringBuffer out = new StringBuffer(xa.toString());
            int       length = out.length();

            print("* - "+out.substring(length - 48)+" *");

            print("* Create XAResource                        - SUCCESS *");
        }
        else
        {
            print("* Create XAResource                        - FAILED  *");
            fail("XAResource create failed!");
        }

        
        if (MS != null)
        {
            stopMessageStore(MS);
        }
        
        print("*                                                    *");
        print("************* TransactionFactoryBlackBox *************");
    }
}
