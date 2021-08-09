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

public class TransactionStateRollbackExpected implements TransactionState
{
    /** A state to represent the XA specs idle state before transitioning 
     * to rolled back.  This state is required when we fail to prepare, and
     * we fail to rollback (facilitating XA_RB response to prepare).  RM_FAIL
     * is returned instead, and we are simply in a state where we are waiting
     * for a rollback.  Rollback is retried and on the first rollback call,
     * state transitions to RollingBack.
     */
     
    
    private static final TransactionStateRollbackExpected _instance = new TransactionStateRollbackExpected();

    private static final String _toString = "TransactionStateRollbackExpected";

    static TransactionState instance()
    {
        return _instance;
    }

    /**
     * private constructor so state can only 
     * be accessed via instance method.
     */
    private TransactionStateRollbackExpected() {}

    public String toString()
    {
        return _toString;
    }
}
