/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transactional.web;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.xa.XAException;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

public class CheckedUncheckedTestBean {

    @Transactional(TxType.REQUIRED)
    public void checkedRequired() throws RollbackException {
        tranRollsBack();
    }

    @Transactional(TxType.REQUIRED)
    public void uncheckedRequired() {
        tranRollsBack();
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void checkedRequiresNew() throws RollbackException {
        tranRollsBack();
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void uncheckedRequiresNew() {
        tranRollsBack();
    }

    public void tranRollsBack() {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        Transaction tx = null;
        try {
            tx = tm.getTransaction();
        } catch (SystemException e) {
            e.printStackTrace();
        }

        try {
            tx.enlistResource(new XAResourceImpl());
            tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        } catch (IllegalStateException | RollbackException | SystemException e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public void throwRTE() {
        throw new RuntimeException();
    }
}