/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.cache;

/**
 * The configuration used by the authentication cache service.
 */
public interface AuthCacheConfig {

    /**
     * Gets the cache initial size.
     * 
     * @return
     */
    int getInitialSize();

    /**
     * Gets the cache max size.
     * 
     * @return
     */
    int getMaxSize();

    /**
     * Gets the cache timeout in seconds.
     * 
     * @return
     */
    long getTimeout();

    /**
     * Indicates if lookup by userid and hashed password is allowed.
     * 
     * @return
     */
    boolean isBasicAuthLookupAllowed();

}
