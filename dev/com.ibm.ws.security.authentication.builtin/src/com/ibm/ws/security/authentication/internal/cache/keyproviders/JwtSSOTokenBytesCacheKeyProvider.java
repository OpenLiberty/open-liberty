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

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.internal.SSOTokenHelper;
import com.ibm.wsspi.security.token.SingleSignonToken;

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
        String base64EncodedSSOTokenBytes = null;
        SingleSignonToken ssoToken = AccessController.doPrivileged(new PrivilegedAction<SingleSignonToken>() {

            @Override
            public SingleSignonToken run() {
                return SSOTokenHelper.getSSOToken(subject);
            }
        });
        if (ssoToken != null) {
            base64EncodedSSOTokenBytes = Base64Coder.toString(Base64Coder.base64Encode(ssoToken.getBytes()));
        }
        return base64EncodedSSOTokenBytes;
    }
}
