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
 * A <code>LockProxy</code> may be used by the <code>LockManager</code>
 * as a placeholder for a lock that is acuired/released by a single
 * <code>Locker</code> at a time. <p>
 */

public interface LockProxy {

    /**
     * Returns true iff this <code>LockProxy</code> instance is actually
     * a full heavy-weight lock instance (as opposed to a placeholder). <p>
     */

    public boolean isLock();

} // LockProxy

