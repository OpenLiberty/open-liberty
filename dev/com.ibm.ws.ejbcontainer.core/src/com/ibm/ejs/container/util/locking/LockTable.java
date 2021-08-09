/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util.locking;

/**
 * <code>LockTable</code> implements a hash-based collection of locks.
 * Locks are indexed by a key, which may be any <code>Object</code> which
 * implements <code>hashCode()</code>. Two keys which have the same hash
 * value will access the same lock in the table.
 */
public final class LockTable
{
    /**
     * Array of lock objects
     */
    private final Object[] locks;

    /**
     * Create a new instance of <code>LockTable</code>, with the specified
     * number of locks.
     * <p>
     * 
     * @param size The number of locks desired in the table
     */
    public LockTable(int size)
    {
        locks = new Object[size];
    }

    /**
     * Return the lock which is associated with the specified key. Uses
     * <code>hashCode()</code> to index into the table of locks. Keys
     * which hash identically will share a single lock.
     * <p>
     * 
     * @param key The key for which the lock is desired
     */
    public Object getLock(Object key)
    {
        int index = (key.hashCode() & 0x7FFFFFFF) % locks.length;

        // Double-checked locking.  Safe since Object has no state.
        Object lock = locks[index];
        if (lock == null)
        {
            synchronized (this)
            {
                lock = locks[index];
                if (lock == null)
                {
                    lock = new Object();
                    locks[index] = lock;
                }
            }
        }

        return lock;
    }
}
