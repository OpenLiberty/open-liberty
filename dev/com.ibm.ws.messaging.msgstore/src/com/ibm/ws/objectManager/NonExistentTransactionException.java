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
 * Thrown when an attempt is made to deregister a transaction which has a logical unit of work identifier that is not
 * registered with the ObjectManager.
 * 
 * @param Object throwing the exception.
 * @param Transaction which was not found to have been registered.
 */
public final class NonExistentTransactionException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 8517624139783773826L;

    protected NonExistentTransactionException(Object source,
                                              InternalTransaction internalTransaction)
    {
        super(source,
              NonExistentTransactionException.class,
              new Object[] { internalTransaction });
    } // NonExistentTransactionException.
} // class NonExistantTransactionException.
