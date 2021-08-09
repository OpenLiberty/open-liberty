/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.internal.encoder.Base64Coder;
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
    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA";
    private static final String KEY_SEPARATOR = ":";
    private static MessageDigest CLONEABLE_MESSAGE_DIGEST = null;

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

    /**
     * Use clone() to get a new instance as its approximately 50% faster (as
     * seen in empirical testing), if we can. Worst case scenario is we will
     * create a new one each time.
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    @Trivial
    @FFDCIgnore(CloneNotSupportedException.class)
    private static MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        // If we've never been asked for a MessageDigest, create the parent of
        // our clones. This is not thread safe, but it does not really need to
        // be, since we're just establishing the parent. If we incur the cost
        // of creating two clones the first time through, that's really not
        // worth synchronizing this whole method.
        if (CLONEABLE_MESSAGE_DIGEST == null) {
            CLONEABLE_MESSAGE_DIGEST = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        }

        // Try to clone the parent. If we can't, then we'll ignore the FFDC and create a
        // new instance. If the clone fails, which is REALLY unlikely, as we
        // know the SHA MessageDigest is cloneable on IBM and Sun JDKs
        //
        try {
            return (MessageDigest) CLONEABLE_MESSAGE_DIGEST.clone();
        } catch (CloneNotSupportedException cnse) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "CloneNotSupportedException caught while trying to clone MessageDigest with algorithm " + MESSAGE_DIGEST_ALGORITHM
                             + ". This is pretty unlikely, and we need to get details about the JDK which is in use.",
                         cnse);
            }
            return MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        }
    }

    private static String getHashedPassword(@Sensitive String password) throws NoSuchAlgorithmException {
        String hashedPassword = null;
        if (password != null) {
            MessageDigest messageDigest = getMessageDigest();
            hashedPassword = Base64Coder.base64EncodeToString(messageDigest.digest(Base64Coder.getBytes(password)));
        }
        return hashedPassword;
    }

}
