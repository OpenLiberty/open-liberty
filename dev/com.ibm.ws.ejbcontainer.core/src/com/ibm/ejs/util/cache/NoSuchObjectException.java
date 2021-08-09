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
package com.ibm.ejs.util.cache;

/**
 * <code>Cache</code> throws a <code>NoSuchObjectException</code> when an
 * operation which requires that the target object exist in the cache is
 * attempted but the target cannot be found in the cache.
 * <p>
 * 
 * @see Cache
 * @see Cache#pin
 * @see Cache#unpin
 * 
 */

public class NoSuchObjectException extends RuntimeException
{
    private static final long serialVersionUID = 8692192410150034609L;

    /**
     * Constructs a <code>NoSuchObjectException</code> object,
     * identifying the object which is not in the cache
     * <p>
     * 
     * @param key The key of the object
     * 
     */

    NoSuchObjectException(Object key)
    {
        super(key.toString() + " could not be found in the cache");
    }
}
