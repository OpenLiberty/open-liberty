/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

/**
 * <code>IllegalOperationException</code> is thrown by <code>Cache</code> when
 * an operation is invoked with a key for an object which is currenly not in
 * the correct state. Typically this occurs if the object is pinned when it
 * should not be, or vice-versa.
 * <p>
 * 
 * @see Cache
 * @see Cache#remove
 * @see Cache#unpin
 * 
 */

public class IllegalOperationException
                extends com.ibm.websphere.csi.IllegalOperationException
{
    private static final long serialVersionUID = 838891005517005185L;

    /**
     * Constructs an <code>IllegalOperationException</code> identifying
     * the specified key as the target object in an operation which cannot
     * be performed on that object while it is in its current state.
     * <p>
     * 
     * @param key The key of the target object
     * 
     */

    IllegalOperationException(Object key, int count)
    {
        super(key.toString() + " is currently pinned: " + count);
    }
}
