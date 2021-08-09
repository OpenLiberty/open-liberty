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
 * 168080        10/06/03  gareth   Local Transaction Support (Local Clients)
 * 181930        17/11/03  gareth   XA Recovery Support
 * 186657.1      24/05/04  gareth   Per-work-item error checking.
 * 214276        09/07/04  gareth   Add begin method to LocalTran
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

public class MSDelegatingLocalTransactionBlackBoxTest extends MessageStoreTestCase {

    public MSDelegatingLocalTransactionBlackBoxTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return new TestSuite(MSDelegatingLocalTransactionBlackBoxTest.class);
    }

    public void testMSDelegatingLocalTransactionBlackBox() {
        print("******** MSDelegatingLocalTransactionBlackBox ********");
        print("*                                                    *");

        // Need to create all of the objects we are going 
        // to use for the following tests.
        MSDelegatingLocalTransaction localTran = null;

        // Create the DelegatingTran - this should SUCCEED
        try {
            localTran = new MSDelegatingLocalTransaction(null, new NullPersistenceManager(this), 0);

            print("* Create of 1st DelegatingLocalTransaction - SUCCESS *");
        } catch (Exception e) {
            print("* Create of 1st DelegatingLocalTransaction - FAILED  *");
            fail("Exception thrown during DelegatingLocalTransaction create!");
        }

        if (localTran != null) {
            //Register a Callback, this should SUCCEED
            localTran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");

            // Get the transaction state, this should be STATE_ACTIVE
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ACTIVE) {
                // Succeeded as expected.
                print("* Transaction State is STATE_ACTIVE        - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_ACTIVE        - FAILED  *");
                fail("getTransactionState(BEFORE_COMMIT) failed!");
            }

            // Attempt to register a null WorkList, this should FAIL.
            try {
                localTran.addWork(null);

                // If we get this far then we're broken.
                print("* Exception handling on addWork(null)      - FAILED  *");
                fail("addWork(null) failed to throw Exception!");
            } catch (MessageStoreException e) {
                // Failed as expected.
                print("* Exception handling on addWork(null)      - SUCCESS *");
            }

            // Register a Transactable and PersistenceManager, this should SUCCEED
            try {
                localTran.addWork(new NullWorkItem(this));

                print("* Register WorkList with LocalTran         - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register WorkList with LocalTran         - FAILED  *");
                fail("WorkList failed to register with LocalTran!");
            }

            // Begin a new DelegatingTran - this should FAIL
            try {
                localTran.begin();

                print("* Exception handling on begin()            - FAILED  *");
                fail("Exception not thrown during DelegatingLocalTransaction begin!");
            } catch (SIIncorrectCallException pe) {
                print("* Exception handling on begin()            - SUCCESS *");
            } catch (Exception e) {
                print("* Exception handling on begin()            - FAILED  *");
                fail("Unexpected exception thrown during DelegatingLocalTransaction create!");
            }

            // Commit the DelegatingTran - this should SUCCEED
            try {
                print("* Commit DelegatingLocalTransaction:                 *");

                localTran.commit();

                print("* Commit DelegatingLocalTransaction        - SUCCESS *");
            } catch (Exception e) {
                print("* Commit DelegatingLocalTransaction        - FAILED  *");
                fail("Exception thrown during DelegatingLocalTransaction commit!");
            }

            // Get the transaction state, this should be STATE_COMMITTED
            state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_COMMITTED) {
                // Succeeded as expected.
                print("* Transaction State is STATE_COMMITTED     - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_COMMITTED     - FAILED  *");
                fail("getTransactionState(AFTER_COMMIT) failed!");
            }

            // Attempt to commit after completion, this should FAIL.
            try {
                localTran.commit();

                // If we get this far then we're broken.
                print("* Exception handling on commit()           - FAILED  *");
                fail("Commit on completed transaction failed to throw Exception!");
            } catch (SIIncorrectCallException sie) {
                // Failed as expected.
                print("* Exception handling on commit()           - SUCCESS *");
            } catch (Exception e) {
                print("* Exception handling on commit()           - FAILED  *");
                fail("Unexpected exception caught during transaction commit!");
            }

            // Attempt to rollback, this should FAIL.
            try {
                localTran.rollback();

                // If we get this far then we're broken.
                print("* Exception handling on rollback()         - FAILED  *");
                fail("Rollback on completed transaction failed to throw Exception!");
            } catch (SIIncorrectCallException sie) {
                // Failed as expected.
                print("* Exception handling on rollback()         - SUCCESS *");
            } catch (Exception e) {
                print("* Exception handling on rollback()         - FAILED  *");
                fail("Unexpected exception caught during transaction rollback!");
            }

            // Attempt to register a Transactable, this should FAIL.
            try {
                localTran.addWork(new NullWorkItem(this));

                // If we get this far then we're broken.
                print("* Exception handling on addWork()          - FAILED  *");
                fail("Register failed to throw Exception!");
            } catch (MessageStoreException e) {
                // Failed as expected.
                print("* Exception handling on addWork()          - SUCCESS *");
            }

            // Get the transaction id.
            print("* Get Transaction Id                                 *");

            StringBuffer line = new StringBuffer("* - Id:                                              *");
            String idStr = localTran.getPersistentTranId().toString();
            line.replace(8, 8 + idStr.length(), idStr);

            print(line.toString());
            print("* Get Transaction Id                       - SUCCESS *");
        }

        // Begin a new DelegatingTran - this should SUCCEED
        try {
            localTran.begin();

            print("* Begin new DelegatingLocalTransaction     - SUCCESS *");
        } catch (Exception e) {
            print("* Begin new DelegatingLocalTransaction     - FAILED  *");
            fail("Exception thrown during DelegatingLocalTransaction create!");
        }

        if (localTran != null) {
            //Register a Callback, this should SUCCEED
            localTran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");

            // Get the transaction state, this should be STATE_ACTIVE
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ACTIVE) {
                // Succeeded as expected.
                print("* Transaction State is STATE_ACTIVE        - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_ACTIVE        - FAILED  *");
                fail("getTransactionState(BEFORE_ROLLBACK) failed!");
            }

            // Register a WorkList, this should SUCCEED
            try {
                localTran.addWork(new NullWorkItem(this));

                print("* Register WorkItem with LocalTran         - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register WorkItem with LocalTran         - FAILED  *");
                fail("WorkItem failed to register with LocalTran!");
            }

            // Rollback the DelegatingTran - this should SUCCEED
            try {
                print("* Rollback DelegatingLocalTransaction:               *");

                localTran.rollback();

                print("* Rollback DelegatingLocalTransaction      - SUCCESS *");
            } catch (Exception e) {
                print("* Rollback DelegatingLocalTransaction      - FAILED  *");
                fail("Exception thrown during DelegatingLocalTransaction rollback!");
            }

            // Get the transaction state, this should be STATE_ROLLEDBACK
            state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ROLLEDBACK) {
                // Succeeded as expected.
                print("* Transaction State is STATE_ROLLEDBACK    - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_ROLLEDBACK    - FAILED  *");
                fail("getTransactionState(AFTER_ROLLBACK) failed!");
            }

            // Get the transaction type, this should be TX_LOCAL
            int type = localTran.getTransactionType();

            if (type == PersistentTransaction.TX_LOCAL) {
                // Succeeded as expected.
                print("* Transaction Type is TX_LOCAL             - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction Type is TX_LOCAL             - FAILED  *");
                fail("getTransactionType failed!");
            }

            // Get the transaction id.
            print("* Get Transaction Id                                 *");

            StringBuffer line = new StringBuffer("* - Id:                                              *");
            String idStr = localTran.getPersistentTranId().toString();
            line.replace(8, 8 + idStr.length(), idStr);

            print(line.toString());
            print("* Get Transaction Id                       - SUCCESS *");
        }

        print("*                                                    *");
        print("******** MSDelegatingLocalTransactionBlackBox ********");
    }
}
