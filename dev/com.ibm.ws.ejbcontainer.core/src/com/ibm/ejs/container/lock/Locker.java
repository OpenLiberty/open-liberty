/*******************************************************************************
 * Copyright (c) 1999 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.lock;

/**
 * A <code>Locker</code> is any object that can obtain locks from the
 * <code>LockManager</code>. <p>
 * 
 * The <code>Locker</code> is the owner (as far as the lock manager is
 * concerened) of any locks acquired from the lock manager. A
 * <code>Locker</code> instance must be provided when acquiring a lock
 * from the <code>LockManager</code> and that same instance must be
 * supplied when releasing the lock or when attempting to convert it
 * from shared to exclusive mode. <p>
 * 
 * The lock manager depends upon being able to determine the identity
 * of <code>Locker</code> instance. For this reason <code>Locker</code>
 * instance must correctly implement the <code>hashCode</code> and
 * <code>equals</code> methods. <p>
 * 
 */

public interface Locker
                extends LockProxy
{
    /**
     * Return the lock mode this locker currently holds the lock
     * identified by lockName in. <p>
     */

    public int getLockMode(Object lockName);

} // Locker

