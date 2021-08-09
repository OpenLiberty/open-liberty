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
 * Thrown when the object manager detects an attempt to unlock or replace
 * a managed object using a transaction that did not lock it.
 */
public final class InvalidTransactionException extends ObjectManagerException
{
    private static final long serialVersionUID = 7418328647854498853L;

    /**
     * @param ManagedObject being unlocked or replaced.
     * @param IinternalTransaction requesting the unlock.
     * @param TransactionLock the lock currently held.
     */
    protected InvalidTransactionException(ManagedObject source
                                          , InternalTransaction internalTransaction
                                          , TransactionLock transactionLock)
    {
        super(source,
              InvalidTransactionException.class,
              new Object[] { source, internalTransaction, transactionLock });

    } //InvalidTransactionException(). 

} // class InvalidTransactionException.
