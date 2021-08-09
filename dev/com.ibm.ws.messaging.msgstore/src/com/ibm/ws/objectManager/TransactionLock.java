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
 * @author Andrew_Banks
 *         Created on 29-Jul-2005
 * 
 *         Indicates if a ManagedObject is locked t a transaction and whether the lock is active.
 *         By having a single lock Object we can release it for all ManagedObjects simultaneoulsly.
 */
class TransactionLock
{
    private InternalTransaction lockingTransaction;
    private long unlockSequence = 0;

    protected TransactionLock(InternalTransaction internalTransaction)
    {
        lockingTransaction = internalTransaction;
    } // TransactionLock().

    /**
     * Releases the lock on the transaction.
     * 
     * @param objectManagerState within which the transaction is unlocked.
     */
    protected final void unLock(ObjectManagerState objectManagerState)
    {
        synchronized (objectManagerState.transactionUnlockSequenceLock) {
            unlockSequence = objectManagerState.getNewGlobalTransactionUnlockSequence();
            lockingTransaction = null;
        } // synchronized.
    }

    /**
     * @return boolean true if the transactionLock is locked to a transaction.
     */
    protected boolean isLocked()
    {
        return (lockingTransaction != null);
    }

    /**
     * @param unlockPoint the point after which the lock must have been held to return true.
     * @return boolean true if the transactionLock was locked, to a transaction
     *         at or after the unlockPoint.
     */
    protected boolean wasLocked(long unlockPoint)
    {
        // The ordering of this test is important because the unlockSequence is set 
        // first and then lockingTransaction is set to null. If the compiler (or jit)
        // were to reorder the test then we might get the wrong results.

        return (lockingTransaction != null
        || unlockPoint < unlockSequence);
    }

    /**
     * @return Returns the lockingTransaction or null if currently unlocked.
     */
    protected InternalTransaction getLockingTransaction()
    {
        return lockingTransaction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        if (lockingTransaction == null)
            return new String("TransactionLock"
                              + "(null)");
        else
            return new String("TransactonLock"
                              + "(" + lockingTransaction + ")");
    } // toString().

} // class TransactoinLock.
