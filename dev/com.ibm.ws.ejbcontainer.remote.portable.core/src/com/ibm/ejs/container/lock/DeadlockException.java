/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
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
 * A <code>DeadlockException</code> is thrown whenever the container's
 * <code>LockManager</code> determines that waiting to acquire a lock
 * would result in a deadlock. <p>
 */

public class DeadlockException
                extends LockException
{
    private static final long serialVersionUID = 6641400854594164270L;

    /**
     * Create a new <code>DeadlockException</code> instance. <p>
     */

    public DeadlockException() {
        super();
    } // DeadlockException

} // DeadlockException
