/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import java.util.Hashtable;

import javax.security.auth.Subject;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.internal.SSOTokenHelper;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Provides the SSO token bytes as the cache key.
 */
public class SSOTokenBytesCacheKeyProvider implements CacheKeyProvider {
    private static final String[] disableLtpaSSOCache = new String[] { AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE,
                                                                       AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_CACHE };

    /** {@inheritDoc} */
    @Override
    public Object provideKey(CacheContext context) {
        return getSingleSignonTokenBytes(context.getSubject());
    }

    private String getSingleSignonTokenBytes(final Subject subject) {
        String base64EncodedSSOTokenBytes = null;

        if (isDisableLtpaSSOCache(subject))
            return null;

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

    private boolean isDisableLtpaSSOCache(final Subject subject) {
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> hashtable = subjectHelper.getHashtableFromSubject(subject, disableLtpaSSOCache);
        if (hashtable != null) {
            Boolean dlCookie = (Boolean) hashtable.get(AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE);
            Boolean dlCache = (Boolean) hashtable.get(AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_CACHE);
            if (dlCookie != null && dlCache != null &&
                dlCookie.booleanValue() && dlCache.booleanValue()) {
                return true;
            }
        }
        return false;
    }
}
