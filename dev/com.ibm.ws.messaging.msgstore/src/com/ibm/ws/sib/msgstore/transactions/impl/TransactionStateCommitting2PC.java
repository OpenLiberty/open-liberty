package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public class TransactionStateCommitting2PC implements TransactionState
{
    private static final TransactionStateCommitting2PC _instance = new TransactionStateCommitting2PC();

    private static final String _toString = "TransactionStateCommitting2PC";

    static TransactionState instance()
    {
        return _instance;
    }

    /**
     * private constructor so state can only 
     * be accessed via instance method.
     */
    private TransactionStateCommitting2PC() {}

    public String toString()
    {
        return _toString;
    }
}
