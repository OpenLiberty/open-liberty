/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.testtooling.tranjacket;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

public final class EMTransactionJacket implements TransactionJacket {
    private EntityTransaction et;

    public EMTransactionJacket(EntityTransaction et) {
        this.et = et;
    }

    @Override
    public void beginTransaction() {
        try {
            System.out.println("Beginning EntityTransaction ...");
            et.begin();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public void commitTransaction() {
        try {
            System.out.println("Committing EntityTransaction ...");
            et.commit();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public void rollbackTransaction() {
        try {
            System.out.println("Rolling Back EntityTransaction ...");
            et.rollback();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public void markTransactionForRollback() {
        try {
            System.out.println("Marking EntityTransaction for Rollback ...");
            et.setRollbackOnly();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public boolean isTransactionMarkedForRollback() {
        try {
            return et.getRollbackOnly();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public boolean isTransactionActive() {
        try {
            return et.isActive();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public boolean isEntityManagerTransactionJacket() {
        return true;
    }

    @Override
    public boolean isJTAUserTransactionJacket() {
        return false;
    }

    @Override
    public boolean isApplicationManaged() {
        return false;
    }

    @Override
    public void joinTransaction(EntityManager em) {}
}