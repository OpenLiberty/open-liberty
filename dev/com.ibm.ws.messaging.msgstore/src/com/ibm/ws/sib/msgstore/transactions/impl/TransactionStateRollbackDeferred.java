package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

public class TransactionStateRollbackDeferred implements TransactionState
{
    private static final TransactionStateRollbackDeferred _instance = new TransactionStateRollbackDeferred();

    private static final String _toString = "TransactionStateRollbackDeferred";

    static TransactionState instance()
    {
        return _instance;
    }

    /**
     * private constructor so state can only
     * be accessed via instance method.
     */
    private TransactionStateRollbackDeferred() {}

    public String toString()
    {
        return _toString;
    }
}

