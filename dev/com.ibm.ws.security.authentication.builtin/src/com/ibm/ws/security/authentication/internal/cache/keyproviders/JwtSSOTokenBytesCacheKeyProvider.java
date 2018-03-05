/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import javax.security.auth.Subject;

import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;

/**
 * Provides the SSO token bytes as the cache key.
 */
public class JwtSSOTokenBytesCacheKeyProvider implements CacheKeyProvider {

    /** {@inheritDoc} */
    @Override
    public Object provideKey(CacheContext context) {
        return getJwtSSOTokenBytes(context.getSubject());
    }

    private String getJwtSSOTokenBytes(final Subject subject) {
        String jwtSSOTokenCacheKey = "s3eDVtuRqj7kIXsMUnLPDUtrUHPqtAHhAxwWOwTIUtc=";
        //TODO call aruna code to get the cache key
        return jwtSSOTokenCacheKey;
//        return null;
    }
}
