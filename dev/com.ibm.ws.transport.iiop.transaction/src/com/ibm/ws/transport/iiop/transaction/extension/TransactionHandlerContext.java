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
package com.ibm.ws.transport.iiop.transaction.extension;

import javax.transaction.TransactionManager;

import com.ibm.tx.remote.RemoteTransactionController;

/**
 * Internal Liberty extension interface: context object passed to
 * {@link TransactionProtocolProvider} methods.
 *
 * <p>Provides access to the Liberty transaction services that providers need
 * to export and import transactions. Providers receive this context as a
 * method parameter; they never construct it.
 *
 * <p>The concrete implementation is internal to Open Liberty and is not
 * part of any published API or SPI contract.
 */
public interface TransactionHandlerContext {

    /**
     * Returns the RemoteTransactionController service.
     * Used to import/export transactions and look up transactions by global ID.
     */
    RemoteTransactionController getRemoteTransactionController();

    /**
     * Returns the JTA TransactionManager.
     * Used for suspend, resume, and current-transaction operations.
     */
    TransactionManager getTransactionManager();
}
