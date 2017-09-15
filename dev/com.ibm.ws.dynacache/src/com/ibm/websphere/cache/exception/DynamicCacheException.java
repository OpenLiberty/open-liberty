/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache.exception;

/**
 * Signals that a generic cache exception has occurred. This class is the base class for 
 * the specific exceptions thrown by Dynamic cache. If a DynamicCacheException occurs while
 * writing the cache entry to disk cache, the cache entry and its related entries are removed
 * from the memory and disk cache. 
 */
public abstract class DynamicCacheException extends Exception {

    private static final long serialVersionUID = -4998690760275949098L;

    /**
     * Constructs a DynamicCacheException with the specified detail message.
     */
    public DynamicCacheException(String message) {
        super(message);
    }

}


