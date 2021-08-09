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
 * 184788        09/12/03  gareth   Implement transaction state model
 * 186657.1      24/05/04  gareth   Per-work-item error checking.
 * 214276        09/07/04  gareth   Add begin method to LocalTran
 * 316887        03/02/06  gareth   Modify exception handling 
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

public class MSDelegatingLocalTransactionBreakerTest extends MessageStoreTestCase {
    public MSDelegatingLocalTransactionBreakerTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite() {
        return new TestSuite(MSDelegatingLocalTransactionBreakerTest.class);
    }

    public void testMSDelegatingLocalTransactionBreaker() {
        print("******** MSDelegatingLocalTransactionBreaker *********");
        print("*                                                    *");

        // Need to create all of the objects we are going 
        // to use for the following tests.
        MSDelegatingLocalTransaction localTran = null;

        // Create the DelegatingTran - this should SUCCEED
        try {
            localTran = new MSDelegatingLocalTransaction(null, new NullPersistenceManager(this), 0);

            print("* Create of DelegatingLocalTransaction     - SUCCESS *");
        } catch (Exception e) {
            print("* Create of DelegatingLocalTransaction     - FAILED  *");
            fail("Exception thrown during DelegatingLocalTransaction create!");
        }

        if (localTran != null) {
            //Register a Callback, this should SUCCEED
            localTran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");

            // Register a BrokenWorkList, this should SUCCEED
            try {
                localTran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_PRE_COMMIT));

                print("* Register BrokenWorkList(PRE_COMMIT)      - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register BrokenWorkList(PRE_COMMIT)      - FAILED  *");
                fail("addWork(BREAK_AT_PRE_COMMIT) failed!");
            }

            // Commit the DelegatingTran - this should FAIL
            try {
                print("* Commit DelegatingLocalTransaction:                 *");

                localTran.commit();

                print("* Commit DelegatingLocalTransaction        - FAILED  *");
                fail("No Exception thrown during DelegatingLocalTransaction commit!");
            } catch (SIRollbackException rbe) {
                print("* Commit DelegatingLocalTransaction        - SUCCESS *");
            } catch (Exception e) {
                print("* Commit DelegatingLocalTransaction        - FAILED  *");
                fail("Unexpected Exception thrown during DelegatingLocalTransaction commit!");
            }

            // Get the transaction state, this should be STATE_COMMITTED
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ROLLEDBACK) {
                // Succeeded as expected.
                print("* Transaction State is STATE_ROLLEDBACK    - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_ROLLEDBACK    - FAILED  *");
                fail("getTransactionState(AFTER_BREAK_AT_PRE_COMMIT) failed!");
            }
        }

        // Create the DelegatingTran - this should SUCCEED
        try {
            localTran = new MSDelegatingLocalTransaction(null, new NullPersistenceManager(this), 0);

            print("* Create of DelegatingLocalTransaction     - SUCCESS *");
        } catch (Exception e) {
            print("* Create of DelegatingLocalTransaction     - FAILED  *");
            fail("Exception thrown during DelegatingLocalTransaction create!");
        }

        if (localTran != null) {
            //Register a Callback, this should SUCCEED
            localTran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");

            // Register a BrokenWorkList, this should SUCCEED
            try {
                localTran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_COMMIT));

                print("* Register BrokenWorkList(COMMIT)          - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register BrokenWorkList(COMMIT)          - FAILED  *");
                fail("addWork(BREAK_AT_COMMIT) failed!");
            }

            // Commit the DelegatingTran - this should FAIL
            try {
                print("* Commit DelegatingLocalTransaction:                 *");

                localTran.commit();

                print("* Commit DelegatingLocalTransaction        - FAILED  *");
                fail("No Exception thrown during DelegatingLocalTransaction commit!");
            } catch (SIErrorException rbe) {
                print("* Commit DelegatingLocalTransaction        - SUCCESS *");
            } catch (Exception e) {
                print("* Commit DelegatingLocalTransaction        - FAILED  *");
                fail("Unexpected Exception thrown during DelegatingLocalTransaction commit!");
            }

            // Get the transaction state, this should be STATE_COMMITTED
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_COMMITTING_1PC) {
                // Succeeded as expected.
                print("* Transaction State is STATE_COMMITTING_1PC- SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_COMMITTING_1PC- FAILED  *");
                fail("getTransactionState(AFTER_BREAK_AT_COMMIT) failed!");
            }
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

            // Register a BrokenWorkList, this should SUCCEED
            try {
                localTran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_ROLLBACK));

                print("* Register BrokenWorkList(ROLLBACK)        - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register BrokenWorkList(ROLLBACK)        - FAILED  *");
                fail("addWork(BREAK_AT_ROLLBACK) failed!");
            }

            // Commit the DelegatingTran - this should FAIL
            try {
                print("* Rollback DelegatingLocalTransaction:               *");

                localTran.rollback();

                print("* Rollback DelegatingLocalTransaction      - FAILED  *");
                fail("No Exception thrown during DelegatingLocalTransaction commit!");
            } catch (SIResourceException se) {
                print("* Rollback DelegatingLocalTransaction      - SUCCESS *");
            } catch (Exception e) {
                print("* Rollback DelegatingLocalTransaction      - FAILED  *");
                fail("Unexpected Exception thrown during DelegatingLocalTransaction rollback!");
            }

            // Get the transaction state, this should be STATE_ROLLINGBACK
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ROLLINGBACK) {
                // Succeeded as expected.
                print("* Transaction State is STATE_ROLLINGBACK   - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_ROLLINGBACK   - FAILED  *");
                fail("getTransactionState(AFTER_BREAK_AT_ROLLBACK) failed!");
            }
        }

        // Create the DelegatingTran - this should SUCCEED
        try {
            localTran = new MSDelegatingLocalTransaction(null, new BrokenPersistenceManager(this, BrokenPersistenceManager.BREAK_AT_COMMIT), 0);

            print("* Register BrokenPersistenceManager(COMMIT)- SUCCESS *");

            print("* Create of DelegatingLocalTransaction     - SUCCESS *");
        } catch (Exception e) {
            print("* Create of DelegatingLocalTransaction     - FAILED  *");
            fail("Exception thrown during DelegatingLocalTransaction create!");
        }

        if (localTran != null) {
            //Register a Callback, this should SUCCEED
            localTran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");

            // Register a WorkItem, this should SUCCEED
            try {
                localTran.addWork(new NullWorkItem(this));

                print("* Register WorkItem with LocalTran         - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register WorkItem with LocalTran         - FAILED  *");
                fail("addWork() failed!");
            }

            // Commit the DelegatingTran - this should FAIL
            try {
                print("* Commit DelegatingLocalTransaction:                 *");

                localTran.commit();

                print("* Commit DelegatingLocalTransaction        - FAILED  *");
                fail("No Exception thrown during DelegatingLocalTransaction commit!");
            } catch (SIRollbackException se) {
                print("* Commit DelegatingLocalTransaction        - SUCCESS *");
            } catch (Exception e) {
                print("* Commit DelegatingLocalTransaction        - FAILED  *");
                fail("Unexpected Exception thrown during DelegatingLocalTransaction commit!");
            }

            // Get the transaction state, this should be STATE_COMMITTED
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ROLLEDBACK) {
                // Succeeded as expected.
                print("* Transaction State is STATE_ROLLEDBACK    - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Transaction State is STATE_ROLLEDBACK    - FAILED  *");
                fail("getTransactionState(AFTER_PM_BREAK_AT_COMMIT) failed!");
            }
        }

        print("*                                                    *");
        print("******** MSDelegatingLocalTransactionBreaker *********");
    }
}
