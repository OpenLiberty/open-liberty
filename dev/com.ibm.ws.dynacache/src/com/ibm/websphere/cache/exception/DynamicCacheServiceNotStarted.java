/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
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
 * The Dynamic cache service has not started. The servlet or object caching operation will be aborted.
 */
public class DynamicCacheServiceNotStarted extends DynamicCacheException {

    static final long serialVersionUID = -8035956532047048631L;
    
    /**
     * Constructs a DynamicCacheServiceNotStarting with the specified detail message.
     */
    public DynamicCacheServiceNotStarted(String message) {
        super(message);
    }

}
