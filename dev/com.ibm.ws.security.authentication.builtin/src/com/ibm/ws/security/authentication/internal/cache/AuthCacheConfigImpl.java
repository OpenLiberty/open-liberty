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
package com.ibm.ws.security.authentication.internal.cache;

import com.ibm.ws.security.authentication.cache.AuthCacheConfig;

/**
 * The configuration used by the AuthCacheServiceImpl.
 */
public class AuthCacheConfigImpl implements AuthCacheConfig {

    private final int initialSize;
    private final int maxSize;
    private final long timeout;
    private final boolean allowBasicAuthLookup;

    /**
     * @param initialSize
     * @param maxSize
     * @param timeout
     * @param allowBasicAuthLookup
     */
    public AuthCacheConfigImpl(int initialSize, int maxSize, long timeout, boolean allowBasicAuthLookup) {
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.allowBasicAuthLookup = allowBasicAuthLookup;
    }

    /** {@inheritDoc} */
    @Override
    public int getInitialSize() {
        return initialSize;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxSize() {
        return maxSize;
    }

    /** {@inheritDoc} */
    @Override
    public long getTimeout() {
        return timeout;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBasicAuthLookupAllowed() {
        return allowBasicAuthLookup;
    }

}
