/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.transaction;

import javax.transaction.TransactionManager;

import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;

/**
 * Internal implementation of {@link TransactionHandlerContext}.
 *
 * <p>Created by {@link TransactionSubsystemFactory} and passed to providers
 * via the extension interface. Never visible to extension consumers — this class is package-private
 * and lives in the non-exported top-level package.
 */
class TransactionHandlerContextImpl implements TransactionHandlerContext {

    private final RemoteTransactionController remoteTransactionController;
    private final TransactionManager transactionManager;

    TransactionHandlerContextImpl(RemoteTransactionController remoteTransactionController,
                                  TransactionManager transactionManager) {
        this.remoteTransactionController = remoteTransactionController;
        this.transactionManager = transactionManager;
    }

    @Override
    public RemoteTransactionController getRemoteTransactionController() {
        return remoteTransactionController;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
}
