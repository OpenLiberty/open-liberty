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
 * Reason        Date      Origin   Description
 * ------------- -------- -------- --------------------------------------------
 * 168092                  gareth   Original
 * 168080        24/07/03  gareth   LocalTransaction Support (Local Clients) 
 * 181930        17/11/03  gareth   XA Recovery Support
 * 184788        05/12/03  gareth   Implement transaction state model
 * 222613        09/08/04  gareth   Standardize tranid generation
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

public class AutoCommitBlackBoxTest extends MessageStoreTestCase {

    public AutoCommitBlackBoxTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return new TestSuite(AutoCommitBlackBoxTest.class);
    }

    public void testAutoCommitBlackBox() {
        print("***************** AutoCommitBlackBox *****************");
        print("*                                                    *");

        MSAutoCommitTransaction autoCommit = new MSAutoCommitTransaction(null, new NullPersistenceManager(this), 0);

        autoCommit.registerCallback(new NullTransactionCallback(this));
        print("* Registration of Callback                 - SUCCESS *");

        // Get the transaction state, this should be STATE_ACTIVE
        TransactionState state = ((PersistentTransaction) autoCommit).getTransactionState();

        if (state == TransactionState.STATE_ACTIVE) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ACTIVE        - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ACTIVE        - FAILED  *");
            fail("getTransactionState failed!");
        }

        // Attempt to register a null WorkList, this should FAIL.
        try {
            autoCommit.addWork(null);

            // If we get this far then we're broken.
            print("* Exception handling on addWork(null)      - FAILED  *");
            fail("addWork failed to throw Exception!");
        } catch (MessageStoreException e) {
            // Failed as expected.
            print("* Exception handling on addWork(null)      - SUCCESS *");
        }

        // Get the transaction id.
        print("* Get Transaction Id                                 *");

        StringBuffer line = new StringBuffer("* - Id:                                              *");
        String idStr = autoCommit.getPersistentTranId().toString();
        line.replace(8, 8 + idStr.length(), idStr);

        print(line.toString());
        print("* Get Transaction Id                       - SUCCESS *");

        // Attempt to register a WorkList, this should Succeed.
        try {
            print("* Registration of WorkList:                          *");

            autoCommit.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Registration of WorkList                 - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Registration of WorkList                 - FAILED  *");
            fail("Register of WorkList failed!");
        }

        // Get the transaction state, this should still be STATE_ACTIVE
        state = ((PersistentTransaction) autoCommit).getTransactionState();

        if (state == TransactionState.STATE_ACTIVE) {
            // Succeeded as expected.
            print("* Transaction State is still STATE_ACTIVE  - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is still STATE_ACTIVE  - FAILED  *");
            fail("Transaction not ready for re-use!");
        }

        // Get the transaction type, this should be TX_AUTO_COMMIT
        int type = ((PersistentTransaction) autoCommit).getTransactionType();

        if (type == PersistentTransaction.TX_AUTO_COMMIT) {
            // Succeeded as expected.
            print("* Transaction Type is TX_AUTO_COMMIT       - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction Type is TX_AUTO_COMMIT       - FAILED  *");
            fail("getTransactionType failed!");
        }

        // Get the transaction id.
        print("* Get Transaction Id                                 *");

        line = new StringBuffer("* - Id:                                              *");
        idStr = autoCommit.getPersistentTranId().toString();
        line.replace(8, 8 + idStr.length(), idStr);

        print(line.toString());
        print("* Get Transaction Id                       - SUCCESS *");

        print("*                                                    *");
        print("***************** AutoCommitBlackBox *****************");
    }
}
