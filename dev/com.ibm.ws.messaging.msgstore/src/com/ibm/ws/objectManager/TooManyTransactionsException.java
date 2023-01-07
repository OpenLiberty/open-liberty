package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

/**
 * Thrown when we try to create more than the defined maximum number of Transactions.
 * 
 * @param ObjectManagerState throwing the exception.
 * @param long the defined maximumAvailableTransactions.
 */
public final class TooManyTransactionsException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 8298589467480019051L;

    protected TooManyTransactionsException(ObjectManagerState objectManagerState
                                           , long maximumAvailableTransactions)
    {
        super(objectManagerState,
              TooManyTransactionsException.class,
              new Object[] { new Long(maximumAvailableTransactions) });
    } // TooManyTransactionsException().
} // class TooManyTransactionsException.