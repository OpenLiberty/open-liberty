/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

import javax.transaction.Synchronization;
import javax.transaction.UserTransaction;

import org.jboss.weld.transaction.spi.TransactionServices;

import com.ibm.ws.cdi.internal.interfaces.TransactionService;

/**
 *
 */
public class TransactionServicesImpl implements TransactionServices {

    private final TransactionService transactionService;

    /**
     * @param transactionService
     */
    public TransactionServicesImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() {
        transactionService.cleanup();
    }

    /** {@inheritDoc} */
    @Override
    public UserTransaction getUserTransaction() {
        return transactionService.getUserTransaction();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTransactionActive() {
        return transactionService.isTransactionActive();
    }

    /** {@inheritDoc} */
    @Override
    public void registerSynchronization(Synchronization sync) {
        transactionService.registerSynchronization(sync);
    }

}
