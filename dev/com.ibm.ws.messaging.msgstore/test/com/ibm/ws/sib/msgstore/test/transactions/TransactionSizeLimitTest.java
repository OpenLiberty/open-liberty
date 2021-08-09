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
 * Reason        Date     Origin     Description
 * ------------- -------- --------- -------------------------------------------
 * 199334.1      27/05/04  gareth   Add transaction size counter
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.ExternalXAResource;
import com.ibm.ws.sib.msgstore.transactions.impl.MSTransactionFactory;

public class TransactionSizeLimitTest extends MessageStoreTestCase {
    public TransactionSizeLimitTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        TransactionSizeLimitTest test = new TransactionSizeLimitTest("testTransactionSizeLimit");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testTransactionSizeLimit() {
        print("************** TransactionSizeLimitTest **************");
        print("*                                                    *");

        if (PERSISTENCE != null) {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        MessageStoreImpl MS = null;
        MSTransactionFactory factory = null;
        ExternalLocalTransaction localTran = null;
        ExternalXAResource xaRes = null;

        // Try to create an ItemStream, this should SUCCEED
        try {
            MS = (MessageStoreImpl) createAndStartMessageStore(true, PERSISTENCE);

            print("* Start MessageStore                       - SUCCESS *");
        } catch (Exception e) {
            print("* Start MessageStore                       - FAILED  *");
            fail("Exception thrown during MS.start()!");
        }

        // Create a TransactionFactory, this should SUCCEED
        try {
            factory = new MSTransactionFactory(MS, new NullPersistenceManager(this));

            print("* Create TransactionFactory                - SUCCESS *");
        } catch (Exception e) {
            print("* Create TransactionFactory                - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during creation of TranFactory: " + e.getMessage());
        }

        // Set the limit to 2, this should SUCCEED
        try {
            factory.setMaximumTransactionSize(2);

            print("* Set transaction size limit to 2          - SUCCESS *");
        } catch (Exception e) {
            print("* Set transaction size limit to 2          - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown setting transaction size limit: " + e.getMessage());
        }

        // Create a LocalTransaction, this should SUCCEED
        try {
            localTran = factory.createLocalTransaction();

            print("* Create LocalTransaction                  - SUCCESS *");
        } catch (Exception e) {
            print("* Create LocalTransaction                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during creation of LocalTran: " + e.getMessage());
        }

        // Increment the tran for the first time, this should SUCCEED
        try {
            localTran.incrementCurrentSize();

            print("* Increment current tran size (1)          - SUCCESS *");
        } catch (Exception e) {
            print("* Increment current tran size (1)          - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown incrementing current tran size (1): " + e.getMessage());
        }

        // Increment the tran for the second time, this should SUCCEED
        try {
            localTran.incrementCurrentSize();

            print("* Increment current tran size (2)          - SUCCESS *");
        } catch (Exception e) {
            print("* Increment current tran size (2)          - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown incrementing current tran size (2): " + e.getMessage());
        }

        // Increment the tran for the third time, this should FAIL
        try {
            localTran.incrementCurrentSize();

            print("* Increment current tran size (3)          - FAILED *");
            fail("Exception not thrown incrementing current tran size!");
        } catch (SIResourceException tmse) {
            print("* Increment current tran size (3)          - SUCCESS *");
        } catch (Exception e) {
            print("* Increment current tran size (3)          - FAILED *");
            e.printStackTrace(System.err);
            fail("Unknown exception thrown incrementing current tran size (3): " + e.getMessage());
        }

        // Create an XAResource, this should SUCCEED
        try {
            xaRes = factory.createXAResource();

            print("* Create XAResource                        - SUCCESS *");
        } catch (Exception e) {
            print("* Create XAResource                        - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during creation of XAResource: " + e.getMessage());
        }

        // Start an association, this should SUCCEED
        try {
            xaRes.start(new NullXid("1"), 0);

            print("* Start XAResource                         - SUCCESS *");
        } catch (Exception e) {
            print("* Start XAResource                         - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown calling start on XAResource: " + e.getMessage());
        }

        // Increment the tran for the first time, this should SUCCEED
        try {
            xaRes.incrementCurrentSize();

            print("* Increment current tran size (1)          - SUCCESS *");
        } catch (Exception e) {
            print("* Increment current tran size (1)          - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown incrementing current tran size (1): " + e.getMessage());
        }

        // Increment the tran for the second time, this should SUCCEED
        try {
            xaRes.incrementCurrentSize();

            print("* Increment current tran size (2)          - SUCCESS *");
        } catch (Exception e) {
            print("* Increment current tran size (2)          - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown incrementing current tran size (2): " + e.getMessage());
        }

        // Increment the tran for the third time, this should FAIL
        try {
            xaRes.incrementCurrentSize();

            print("* Increment current tran size (3)          - FAILED *");
            fail("Exception not thrown incrementing current tran size!");
        } catch (SIResourceException tmse) {
            print("* Increment current tran size (3)          - SUCCESS *");
        } catch (Exception e) {
            print("* Increment current tran size (3)          - FAILED *");
            e.printStackTrace(System.err);
            fail("Unknown exception thrown incrementing current tran size (3): " + e.getMessage());
        }

        if (MS != null) {
            stopMessageStore(MS);
        }

        print("*                                                    *");
        print("************** TransactionSizeLimitTest **************");
    }
}
