/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.crypto.MessageDigestUtils;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.cache.AuthCacheConfig;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Provides a basic authentication cache keys containing the realm, userid, and hashed password.
 */
public class BasicAuthCacheKeyProvider implements CacheKeyProvider {

    private static final TraceComponent tc = Tr.register(BasicAuthCacheKeyProvider.class);
    private static final String KEY_SEPARATOR = ":";

    private static final String SHA_512 = "SHA-512";

    /** {@inheritDoc} */
    @Override
    public Object provideKey(CacheContext cacheContext) {
        Set<Object> keys = null;
        if (isPossibleToCreateAnyKey(cacheContext)) {
            keys = new HashSet<Object>();
            String hashedPassword = createHashedPassword(cacheContext);
            addKeysFromContext(keys, cacheContext, hashedPassword);
            addKeysFromWSCredential(keys, cacheContext, hashedPassword);
        } else {
            keys = Collections.emptySet();
        }
        return keys;
    }

    private boolean isPossibleToCreateAnyKey(CacheContext cacheContext) {
        SubjectHelper subjectHelper = new SubjectHelper();
        String customCacheKey = null;
        Hashtable<String, ?> hashtable = subjectHelper.getHashtableFromSubject((cacheContext.getSubject()), new String[] { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY });
        if (hashtable != null) {
            customCacheKey = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        }
        if (customCacheKey != null)
            return false;
        else
            return cacheContext.getUserid() != null || subjectHelper.getWSCredential(cacheContext.getSubject()) != null;
    }

    @FFDCIgnore(NoSuchAlgorithmException.class)
    private String createHashedPassword(CacheContext cacheContext) {
        String hashedPassword = null;
        try {
            AuthCacheConfig config = cacheContext.getAuthCacheConfig();
            String password = cacheContext.getPassword();
            if (config.isBasicAuthLookupAllowed() && password != null) {
                hashedPassword = getHashedPassword(password);
            }
        } catch (NoSuchAlgorithmException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem creating the hashed password.", e);
            }
        }
        return hashedPassword;
    }

    private void addKeysFromContext(Set<Object> keys, CacheContext cacheContext, @Sensitive String hashedPassword) {
        try {
            SubjectHelper subjectHelper = new SubjectHelper();
            String realm = subjectHelper.getRealm(cacheContext.getSubject());
            String userid = cacheContext.getUserid();
            addKeys(keys, realm, userid, hashedPassword);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem creating the cache key.", e);
            }
        }
    }

    private void addKeysFromWSCredential(Set<Object> keys, CacheContext cacheContext, @Sensitive String hashedPassword) {
        SubjectHelper subjectHelper = new SubjectHelper();
        WSCredential wsCredential = subjectHelper.getWSCredential(cacheContext.getSubject());
        if (wsCredential != null) {
            try {
                String realm = wsCredential.getRealmName();
                String securityName = wsCredential.getSecurityName();
                String uniqueSecurityName = wsCredential.getUniqueSecurityName();
                addKeys(keys, realm, securityName, hashedPassword);
                addKeys(keys, realm, uniqueSecurityName, hashedPassword);
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "There was a problem creating the password based cache keys from the WSCredential.", e);
                }
            }
        }
    }

    private void addKeys(Set<Object> keys, String realm, String userid, @Sensitive String hashedPassword) {
        String keyWithoutPassword = createLookupKey(realm, userid);
        if (keyWithoutPassword != null) {
            if (hashedPassword == null) {
                addKey(keys, keyWithoutPassword);
            } else {
                String keyWithPassword = keyWithoutPassword + KEY_SEPARATOR + hashedPassword;
                addKey(keys, keyWithPassword);
            }
        }
    }

    private void addKey(Set<Object> keys, String cacheKey) {
        if (cacheKey != null) {
            keys.add(cacheKey);
        }
    }

    /**
     * Creates a key to be used with the AuthCacheService.
     * The parameters must not be null, otherwise a null key is returned.
     *
     * @param realm
     * @param userid
     * @return
     */
    public static String createLookupKey(String realm, String userid) {
        String key = null;
        if (realm != null && userid != null) {
            key = realm + KEY_SEPARATOR + userid;
        }
        return key;
    }

    /**
     * Creates a lookup key to be used with the AuthCacheService.
     * The parameters must not be null, otherwise a null key is returned.
     *
     * @param realm
     * @param userid
     * @param password
     * @return
     */
    @FFDCIgnore(NoSuchAlgorithmException.class)
    public static String createLookupKey(String realm, String userid, @Sensitive String password) {
        String lookupKey = null;
        if (realm != null && userid != null && password != null) {
            try {
                String hashedPassword = getHashedPassword(password);
                lookupKey = realm + KEY_SEPARATOR + userid + KEY_SEPARATOR + hashedPassword;
            } catch (NoSuchAlgorithmException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "There was a problem creating the lookup key.", e);
                }
            }
        }
        return lookupKey;
    }

    private static String getHashedPassword(@Sensitive String password) throws NoSuchAlgorithmException {
        return password == null ? null : MessageDigestUtils.getHashedValue(password, SHA_512);
    }

}
