/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
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
package com.ibm.ejs.container.lock;

/**
 * A <code>LockReleasedException</code> is thrown whenever a waiting
 * request to acquire a lock managed by the lock manager is interrupted
 * because the lock has been released by another thread. <p>
 */

public class LockReleasedException
                extends LockException
{
    private static final long serialVersionUID = -1316438387962623085L;

    /**
     * Create a new <code>LockReleasedException</code> instance. <p>
     */

    public LockReleasedException() {
        super();
    } // LockReleasedException

} // LockReleasedException
