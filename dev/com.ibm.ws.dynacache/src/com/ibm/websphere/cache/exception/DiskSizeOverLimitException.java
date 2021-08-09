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
 * Signals that either (1) there is no disk space available or (2) the disk cache size in GB is over 
 * the configured "diskCacheSizeInGB" limit when writing the cache entry to the disk cache.
 */
public class DiskSizeOverLimitException extends DynamicCacheException {

    private static final long serialVersionUID = -8048186487020737461L;

    /**
     * Constructs a DiskSizeOverLimitException with the specified detail message.
     */
    public DiskSizeOverLimitException(String message) {
        super(message);
    }

}


