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
 * Reason          Date    Origin    Description
 * ------------- -------- --------  -------------------------------------------
 * 184788        09/12/03  gareth   Add transaction state model
 * SIB0003.ms.13 26/08/05  schofiel 1PC optimisation only works for data store not file store           
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistenceManager;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

public class NullPersistenceManager implements PersistenceManager {
    private boolean _supports1PCOptimization = false;

    private final MessageStoreTestCase _test;

    public NullPersistenceManager(MessageStoreTestCase test) {
        _test = test;
    }

    public NullPersistenceManager(MessageStoreTestCase test, boolean supports1PCOptimization) {
        _test = test;
        _supports1PCOptimization = supports1PCOptimization;
    }

    public void beforeCompletion(PersistentTransaction transaction) throws PersistenceException {
        _test.print("* - PM called with beforeCompletion        - SUCCESS *");
    }

    public void prepare(PersistentTransaction transaction) throws PersistenceException {
        _test.print("* - PM called with prepare                 - SUCCESS *");
    }

    public void commit(PersistentTransaction transaction, boolean onePhase) throws PersistenceException {
        _test.print("* - PM called with commit                  - SUCCESS *");
    }

    public void rollback(PersistentTransaction transaction) throws PersistenceException {
        _test.print("* - PM called with rollback                - SUCCESS *");
    }

    public void afterCompletion(PersistentTransaction transaction, boolean committed) {
        _test.print("* - PM called with afterCompletion         - SUCCESS *");
    }

    public boolean supports1PCOptimisation() {
        _test.print("* - PM called with supports1PCOptimisation - SUCCESS *");
        return _supports1PCOptimization;
    }
}
