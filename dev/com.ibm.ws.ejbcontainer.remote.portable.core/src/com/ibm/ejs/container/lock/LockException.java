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

import com.ibm.ejs.container.ContainerException;

/**
 * A <code>LockException</code> represents the root of all exceptions
 * raised by the <code>LockManager</code>. <p>
 */

public class LockException
                extends ContainerException
{
    private static final long serialVersionUID = 932849371367133006L;

    /**
     * Create a new <code>LockException</code> instance. <p>
     */

    public LockException() {
        super();
    } // LockException

    public LockException(String s, Throwable ex) {
        super(s, ex);
    } // LockException

} // LockException
