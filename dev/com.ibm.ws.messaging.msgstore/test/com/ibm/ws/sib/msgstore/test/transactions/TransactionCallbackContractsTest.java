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
 *               24/11/04  gareth   Test transaction callback contracts
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSTransactionFactory;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.transactions.AutoCommitTransaction;
import com.ibm.ws.sib.transactions.LocalTransaction;

public class TransactionCallbackContractsTest extends MessageStoreTestCase {
    public TransactionCallbackContractsTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        TransactionCallbackContractsTest test = new TransactionCallbackContractsTest("testTransactionCallbackContracts");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testTransactionCallbackContracts() {
        print("********** TransactionCallbackContractsTest **********");
        print("*                                                    *");

        if (PERSISTENCE != null) {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        MessageStoreImpl MS = null;
        MSTransactionFactory factory = null;
        AutoCommitTransaction autoCommit = null;
        LocalTransaction localTran = null;

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

        // Create a LocalTransaction, this should SUCCEED
        try {
            localTran = factory.createLocalTransaction();

            print("* Create LocalTransaction                  - SUCCESS *");
        } catch (Exception e) {
            print("* Create LocalTransaction                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during creation of LocalTran: " + e.getMessage());
        }

        // Enlist our special callback in the tran, this should SUCCEED
        try {
            localTran.registerCallback(new AddDuringBeforeCompletionCallback(this));

            print("* Register callback                        - SUCCESS *");
        } catch (Exception e) {
            print("* Register callback                        - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown registering AddDuringBeforeCompletionCallback: " + e.getMessage());
        }

        // Add our special item to the tran, this should SUCCEED
        try {
            ((PersistentTransaction) localTran).addWork(new AddDuringPreCommitWorkItem(this));

            print("* Add callback Item                        - SUCCESS *");
        } catch (Exception e) {
            print("* Add callback Item                        - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown adding AddDuringPreCommitWorkItem: " + e.getMessage());
        }

        // Commit the transaction, this should SUCCEED
        try {
            print("* Commit transaction:                                *");

            localTran.commit();

            print("* Commit transaction                       - SUCCESS *");
        } catch (Exception e) {
            print("* Commit transaction                       - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown committing transaction: " + e.getMessage());
        }

        // Create a LocalTransaction, this should SUCCEED
        try {
            autoCommit = factory.createAutoCommitTransaction();

            print("* Create AutoCommitTransaction             - SUCCESS *");
        } catch (Exception e) {
            print("* Create AutoCommitTransaction             - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during creation of AutoCommitTran: " + e.getMessage());
        }

        // Enlist our special callback in the tran, this should SUCCEED
        try {
            autoCommit.registerCallback(new AddDuringBeforeCompletionCallback(this));

            print("* Register callback                        - SUCCESS *");
        } catch (Exception e) {
            print("* Register callback                        - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown registering AddDuringBeforeCompletionCallback: " + e.getMessage());
        }

        // Add our special item to the tran, this should SUCCEED but add of second Item should FAIL
        try {
            print("* Add callback Item (Commit Tran):                   *");

            ((PersistentTransaction) autoCommit).addWork(new AddDuringPreCommitWorkItem(this));

            print("* Add callback Item (Commit Tran)          - SUCCESS *");
        } catch (Exception e) {
            print("* Add callback Item (Commit Tran)          - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown adding AddDuringPreCommitWorkItem: " + e.getMessage());
        }

        if (MS != null) {
            stopMessageStore(MS);
        }

        print("*                                                    *");
        print("********** TransactionCallbackContractsTest **********");
    }
}
