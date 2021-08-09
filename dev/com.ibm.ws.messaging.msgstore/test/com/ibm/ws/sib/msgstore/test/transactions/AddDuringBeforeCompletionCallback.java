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

import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;

public class AddDuringBeforeCompletionCallback implements TransactionCallback {
    private final MessageStoreTestCase _test;

    public AddDuringBeforeCompletionCallback(MessageStoreTestCase test) {
        _test = test;
    }

    public void beforeCompletion(TransactionCommon transaction) {
        _test.print("* - Callback called with beforeCompletion:           *");

        try {
            ((PersistentTransaction) transaction).addWork(new NullWorkItem(_test));

            // If we are using an MSAutoCommitTransaction
            // then adding more work at this point shouldn't 
            // have succeeded.
            if (transaction instanceof MSAutoCommitTransaction) {
                _test.print("*   - Item added to AutoCommitTransaction! - FAILED  *");
                _test.fail("Add of further work was allowed by AutoCommitTransaction!");
            } else {
                _test.print("*   - Item added to Transaction            - SUCCESS *");
            }
        } catch (Exception e) {
            // If we are using an MSAutoCommitTransaction
            // then we should expect to fail.
            if (transaction instanceof MSAutoCommitTransaction) {
                _test.print("*   - Item add failed on AutoCommitTran    - SUCCESS *");
            } else {
                _test.print("*   - Item added to Transaction            - FAILED  *");
                _test.fail("Add of further work failed! " + e.getMessage());
            }
        }

        _test.print("* - Callback called with beforeCompletion  - SUCCESS *");
    }

    public void afterCompletion(TransactionCommon transaction, boolean committed) {
        _test.print("* - Callback called with afterCompletion   - SUCCESS *");
    }
}
