/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.cache.AuthCacheConfig;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.utility.SubjectHelper;

/**
 * Provides a basic authentication cache keys containing the realm, userid, and hashed password.
 */
public class BasicAuthCacheKeyProvider implements CacheKeyProvider {

    private static final TraceComponent tc = Tr.register(BasicAuthCacheKeyProvider.class);
    private static final String DEFAULT_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String KEY_SEPARATOR = ":";
    private static String SEED_VALUE = String.valueOf(System.nanoTime());

    /** {@inheritDoc} */
    @Override
    public Object provideKey(CacheContext cacheContext) {
        if (isPossibleToCreateAnyKey(cacheContext)) {
            Set<Object> keys = new HashSet<Object>();
            String hashedPassword = createHashedPassword(cacheContext);
            addKeysFromContext(keys, cacheContext, hashedPassword);
            addKeysFromWSCredential(keys, cacheContext, hashedPassword);
            return keys;
        } else {
            return Collections.emptySet();
        }
    }

    private boolean isPossibleToCreateAnyKey(CacheContext cacheContext) {
        SubjectHelper subjectHelper = new SubjectHelper();
        return cacheContext.getUserid() != null || subjectHelper.getWSCredential(cacheContext.getSubject()) != null;
    }

    private String createHashedPassword(CacheContext cacheContext) {
        String hashedPassword = null;
        try {
            AuthCacheConfig config = cacheContext.getAuthCacheConfig();
            String password = cacheContext.getPassword();
            if (config.isBasicAuthLookupAllowed() && password != null) {
                String userId = cacheContext.getUserid();
                hashedPassword = getHashedPassword(userId, password, SEED_VALUE);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
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
        addKey(keys, keyWithoutPassword);
        if (keyWithoutPassword != null && hashedPassword != null) {
            String keyWithPassword = keyWithoutPassword + KEY_SEPARATOR + hashedPassword;
            addKey(keys, keyWithPassword);
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
    public static String createLookupKey(String realm, String userid, @Sensitive String password) {
        String lookupKey = null;
        if (realm != null && userid != null && password != null) {
            try {
                String hashedPassword = getHashedPassword(userid, password, SEED_VALUE);
                lookupKey = realm + KEY_SEPARATOR + userid + KEY_SEPARATOR + hashedPassword;
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "There was a problem creating the lookup key.", e);
                }
            }
        }
        return lookupKey;
    }

    /**
     * hashing the password
     *
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     **/
    protected static String getHashedPassword(String userId, @Sensitive String password, String seedValue) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(DEFAULT_ALGORITHM);
        PBEKeySpec ks = new PBEKeySpec(password.toCharArray(), Base64Coder.getBytes(userId + seedValue + password), userId.length() * 128 + password.length() * 11, 256);
        return Base64Coder.base64EncodeToString(skf.generateSecret(ks).getEncoded());
    }

    // this is for UT
    protected String getSeedValue() {
        return SEED_VALUE;
    }
}
