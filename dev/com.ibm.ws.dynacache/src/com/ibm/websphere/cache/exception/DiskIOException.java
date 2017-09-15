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
 * Signals that a disk IO exception has occurred when writing the cache entry to the disk cache.
 * When this exception is encountered, the Dynamic cache will automatically turn off the 
 * disk offload feature.
 */
public class DiskIOException extends DynamicCacheException {

    private static final long serialVersionUID = 1390857807469146766L;

    /**
     * Constructs a DiskIOException with the specified detail message.
     */
    public DiskIOException(String message) {
        super(message);
    }

}


