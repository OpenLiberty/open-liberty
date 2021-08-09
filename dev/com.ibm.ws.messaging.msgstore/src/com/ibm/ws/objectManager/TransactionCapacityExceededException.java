package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Thrown if we try to start more transactions than the ObjectManager is currently capable of managing.
 * This is usuallybcause the ObjectManager has reduced the number of transactions it can start
 * because it needs to make sure checkpoints are completed before the log file fills.
 * 
 * @param ObjectManagerState throwing the exception.
 * @param long the current number of active Transactions.
 * @param long the current number of Transactions the ObjectManager can start.
 */
public final class TransactionCapacityExceededException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -5330604766707787610L;

    protected TransactionCapacityExceededException(ObjectManagerState source,
                                                   long totalTransactions,
                                                   long currentMaximumActiveTransactions)
    {
        super(source,
              TransactionCapacityExceededException.class,
              new Object[] { new Long(totalTransactions),
                            new Long(currentMaximumActiveTransactions) });
    } // TransactionCapacityExceededException().
} // class TransactionCapacityExceededException.
