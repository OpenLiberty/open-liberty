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
import javax.transaction.Status;
import javax.transaction.UserTransaction;

public class JTATransactionJacket implements TransactionJacket {
    private UserTransaction ut;
    private boolean appManaged = false;

    public JTATransactionJacket(UserTransaction ut) {
        this.ut = ut;
    }

    public JTATransactionJacket(UserTransaction ut, boolean applicationManaged) {
        this.ut = ut;
        this.appManaged = applicationManaged;
    }

    @Override
    public void beginTransaction() {
        try {
            System.out.println("Beginning Global Transaction ...");
            ut.begin();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public void commitTransaction() {
        try {
            System.out.println("Committing Global Transaction ...");
            ut.commit();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public void rollbackTransaction() {
        try {
            System.out.println("Rolling Back Global Transaction ...");
            ut.rollback();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public void markTransactionForRollback() {
        try {
            System.out.println("Marking Global Transaction for Rollback ...");
            ut.setRollbackOnly();
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public boolean isTransactionMarkedForRollback() {
        try {
            int status = ut.getStatus();
            if (status == Status.STATUS_MARKED_ROLLBACK)
                return true;
            else
                return false;
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public boolean isTransactionActive() {
        try {
            int status = ut.getStatus();
            if (status == Status.STATUS_NO_TRANSACTION || status == Status.STATUS_UNKNOWN)
                return false;
            else
                return true;
        } catch (Throwable t) {
            throw new TransactionJacketException(t);
        }
    }

    @Override
    public boolean isEntityManagerTransactionJacket() {
        return false;
    }

    @Override
    public boolean isJTAUserTransactionJacket() {
        return true;
    }

    @Override
    public boolean isApplicationManaged() {
        return appManaged;
    }

    @Override
    public void joinTransaction(EntityManager em) {
        if (appManaged) {
            em.joinTransaction();
        }
    }

}