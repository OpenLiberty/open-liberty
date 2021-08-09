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
 * Signals that the disk offload feature for the cache instance is not enabled to perform this operation.
 *
 */
public class DiskOffloadNotEnabledException extends DynamicCacheException {

    private static final long serialVersionUID = -5082196191715410157L;

    /**
     * Constructs a DiskOffloadNotEnabledException with the specified
     * detail message.
     */
    public DiskOffloadNotEnabledException(String message) {
        super(message);
    }

}


