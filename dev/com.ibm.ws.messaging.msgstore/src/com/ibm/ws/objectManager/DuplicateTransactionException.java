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
 * Thrown when an attempt is made to register or free a transaction which has same logical unit of work identifier as
 * one that is already registered or free.
 */
public final class DuplicateTransactionException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 8325741467489066039L;

    /**
     * Constructor
     * 
     * @param Object
     *            source of the exception.
     * @param IntenalTransaction
     *            being registered or freed.
     * @param InternalTransaction
     *            already registered or free.
     */
    protected DuplicateTransactionException(ObjectManagerState source,
                                            InternalTransaction newInternalTransaction,
                                            InternalTransaction existingInternalTransaction)
    {
        super(source,
              DuplicateTransactionException.class,
              new Object[] { source,
                            newInternalTransaction,
                            existingInternalTransaction });

    } // DuplicateTransactionException().
} // class DuplicateTransactionException.
