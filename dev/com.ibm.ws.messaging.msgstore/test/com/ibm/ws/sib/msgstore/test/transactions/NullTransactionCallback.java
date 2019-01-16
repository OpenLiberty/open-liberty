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
 * Reason          Date     Origin   Description
 * ------------- --------  -------- -------------------------------------------
 * 184788        09/12/03  gareth   Add transaction state model
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;

public class NullTransactionCallback implements TransactionCallback
{
    private MessageStoreTestCase _test;

    public NullTransactionCallback(MessageStoreTestCase test)
    {
        _test = test;
    }

    public void beforeCompletion(TransactionCommon transaction) 
    {
        _test.print("* - Callback called with beforeCompletion  - SUCCESS *");
    }

    public void afterCompletion(TransactionCommon transaction, boolean committed) 
    {
        _test.print("* - Callback called with afterCompletion   - SUCCESS *");
    }
}

