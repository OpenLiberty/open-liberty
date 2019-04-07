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
 * ------------- -------- -------- ------------------------------------------
 * 188050.1      10/02/04  gareth   SpecJAppServer2003 Optimization
 * 186657.1      24/05/04  gareth   Per-work-item error checking.
 * SIB0003.ms.13 26/08/05  schofiel 1PC optimisation only works for data store not file store 
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase          
 * ============================================================================
 */

import javax.transaction.Status;
import javax.transaction.Synchronization;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransactionSynchronization;
import com.ibm.ws.sib.msgstore.transactions.impl.MSTransactionFactory;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

public class MSDelegatingSynchronizationBlackBoxTest extends MessageStoreTestCase {

    public MSDelegatingSynchronizationBlackBoxTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return new TestSuite(MSDelegatingSynchronizationBlackBoxTest.class);
    }

    public void testMSDelegatingSynchronizationBlackBox() {
        print("******** MSDelegatingSynchronizationBlackBox *********");
        print("*                                                    *");

        // Need to create all of the objects we are going 
        // to use for the following tests.
        MSDelegatingLocalTransaction localTran = null;
        Synchronization sync = null;
        MSTransactionFactory tranFactory = null;
        ExternalLocalTransaction extLocalTran = null;

        // Create the DelegatingTran - this should FAIL because it's not a Synchronization
        try {
            localTran = new MSDelegatingLocalTransaction(null, new NullPersistenceManager(this), 0);
            sync = (Synchronization) localTran;

            print("* Create of LocalTran not Synchronization  - FAILED  *");
            fail("DelegatingLocalTransaction is not supposed to implement Synchronization!");
        } catch (ClassCastException cce) {
            print("* Create of LocalTran not Synchronization  - SUCCESS *");
        } catch (Exception e) {
            print("* Create of LocalTran not Synchronization  - FAILED  *");
            fail("Exception thrown during DelegatingSynchronization create!");
        }

        // Create the DelegatingTranSynchronization - this should SUCCEED
        try {
            localTran = new MSDelegatingLocalTransactionSynchronization(null, new NullPersistenceManager(this), 0);
            sync = (Synchronization) localTran;

            print("* Create of 1st DelegatingSynchronization  - SUCCESS *");
        } catch (Exception e) {
            print("* Create of 1st DelegatingSynchronization  - FAILED  *");
            fail("Exception thrown during DelegatingSynchronization create!");
        }

        if (localTran != null) {
            //Register a Callback, this should SUCCEED
            localTran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");

            // Get the transaction state, this should be STATE_ACTIVE
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ACTIVE) {
                // Succeeded as expected.
                print("* Synchronization State is STATE_ACTIVE    - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Synchronization State is STATE_ACTIVE    - FAILED  *");
                fail("getTransactionState(BEFORE_COMMIT) failed!");
            }

            // Register a WorkList, this should SUCCEED
            try {
                localTran.addWork(new NullWorkItem(this));

                print("* Register WorkItem with Synchronization   - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register WorkItem with Synchronization   - FAILED  *");
                fail("WorkItem failed to register with Synchronization!");
            }

            // Call beforeCompletion() on the Synchronization - this should SUCCEED
            try {
                print("* Call beforeCompletion() on Sync:                   *");

                sync.beforeCompletion();

                print("* Call beforeCompletion() on Sync          - SUCCESS *");
            } catch (Exception e) {
                print("* Call beforeCompletion() on Sync          - FAILED  *");
                fail("Exception thrown during DelegatingSynchronization beforeCompletion!");
            }

            // Call afterCompletion() on the Synchronization - this should SUCCEED
            try {
                print("* Call afterCompletion(COMMIT) on Sync:              *");

                sync.afterCompletion(Status.STATUS_COMMITTED);

                print("* Call afterCompletion(COMMIT) on Sync     - SUCCESS *");
            } catch (Exception e) {
                print("* Call afterCompletion(COMMIT) on Sync     - FAILED  *");
                fail("Exception thrown during DelegatingSynchronization afterCompletion!");
            }

            // Get the transaction state, this should be STATE_COMMITTED
            state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_COMMITTED) {
                // Succeeded as expected.
                print("* Synchronization State is STATE_COMMITTED - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Synchronization State is STATE_COMMITTED - FAILED  *");
                fail("getTransactionState(AFTER_COMMIT) failed!");
            }
        }

        // Create a new Synchronization - this should SUCCEED
        try {
            localTran = new MSDelegatingLocalTransactionSynchronization(null, new NullPersistenceManager(this), 0);
            sync = (Synchronization) localTran;

            print("* Create of 2nd DelegatingSynchronization  - SUCCESS *");
        } catch (Exception e) {
            print("* Create of 2nd DelegatingSynchronization  - FAILED  *");
            fail("Exception thrown during DelegatingSynchronization create!");
        }

        if (localTran != null) {
            //Register a Callback, this should SUCCEED
            localTran.registerCallback(new NullTransactionCallback(this));
            print("* Registration of Callback                 - SUCCESS *");

            // Get the transaction state, this should be STATE_ACTIVE
            TransactionState state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ACTIVE) {
                // Succeeded as expected.
                print("* Synchronization State is STATE_ACTIVE    - SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Synchronization State is STATE_ACTIVE    - FAILED  *");
                fail("getTransactionState(BEFORE_ROLLBACK) failed!");
            }

            // Register a WorkList, this should SUCCEED
            try {
                localTran.addWork(new NullWorkItem(this));

                print("* Register WorkItem with Synchronization   - SUCCESS *");
            } catch (MessageStoreException mse) {
                print("* Register WorkItem with Synchronization   - FAILED  *");
                fail("WorkItem failed to register with Synchronization!");
            }

            // Call afterCompletion() on the Synchronization - this should SUCCEED
            try {
                print("* Call afterCompletion(ROLLBACK) on Sync:            *");

                sync.afterCompletion(Status.STATUS_ROLLEDBACK);

                print("* Call afterCompletion(ROLLBACK) on Sync   - SUCCESS *");
            } catch (Exception e) {
                print("* Call afterCompletion(ROLLBACK) on Sync   - FAILED  *");
                fail("Exception thrown during DelegatingSynchronization afterCompletion!");
            }

            // Get the transaction state, this should be STATE_ROLLEDBACK
            state = ((PersistentTransaction) localTran).getTransactionState();

            if (state == TransactionState.STATE_ROLLEDBACK) {
                // Succeeded as expected.
                print("* Synchronization State is STATE_ROLLEDBACK- SUCCESS *");
            } else {
                // If we get this far then we're broken.
                print("* Synchronization State is STATE_ROLLEDBACK- FAILED  *");
                fail("getTransactionState(AFTER_ROLLBACK) failed!");
            }
        }

        // Ensure that a persistence manager which cannot support 1PC optimisation prevents
        // the transaction factory from creating local transactions which implement Synchronization
        try {
            tranFactory = new MSTransactionFactory(null, new NullPersistenceManager(this, false));
            extLocalTran = tranFactory.createLocalTransaction();
            sync = (Synchronization) extLocalTran;

            print("* Factory LocalTran not Synchronization    - FAILED  *");
            fail("PersistenceManager which does not support 1PC optimisation should give LocalTran which does not implement Synchronization!");
        } catch (ClassCastException cce) {
            print("* Factory LocalTran not Synchronization    - SUCCESS *");
        } catch (Exception e) {
            print("* Factory LocalTran not Synchronization    - FAILED  *");
            fail("Exception thrown during Factory creation of local transaction not Synchronization!");
        }

        // Ensure that a persistence manager which can support 1PC optimisation gets
        // the transaction factory to create local transactions which implement Synchronization
        try {
            tranFactory = new MSTransactionFactory(null, new NullPersistenceManager(this, true));
            extLocalTran = tranFactory.createLocalTransaction();
            sync = (Synchronization) extLocalTran;

            print("* Factory LocalTransaction Synchronization - SUCCESS *");
        } catch (ClassCastException cce) {
            print("* Factory LocalTransaction Synchronization - FAILED  *");
            fail("PersistenceManager which does not support 1PC optimisation should give LocalTran which does not implement Synchronization!");
        } catch (Exception e) {
            print("* Factory LocalTransaction Synchronization - FAILED  *");
            fail("Exception thrown during Factory creation of local transaction Synchronization!");
        }

        print("*                                                    *");
        print("******** MSDelegatingSynchronizationBlackBox *********");
    }
}
