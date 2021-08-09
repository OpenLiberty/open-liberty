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
 * Signals that a runtime exception other than a disk IOException has occurred when 
 * writing the cache entry to the disk cache.
 */
public class MiscellaneousException extends DynamicCacheException {

    private static final long serialVersionUID = -733655168072791778L;

    /**
     * Constructs an MiscellaneousException with the specified detail message.
     */
    public MiscellaneousException(String message) {
        super(message);
    }

}


