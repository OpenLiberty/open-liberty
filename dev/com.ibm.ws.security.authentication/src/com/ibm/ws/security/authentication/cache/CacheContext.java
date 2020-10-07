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

import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * The context information passed to the CacheKeyProvider objects when prompting them for the cache key.
 * This context may be used by the providers when creating the key.
 */
public class CacheContext {

    private final CacheObject cacheObject;
    private final AuthCacheConfig config;
    private String userid;
    private String password;
    private X509Certificate[] certChain = null;

    /**
     * @param config
     * @param cacheObject
     */
    public CacheContext(AuthCacheConfig config, CacheObject cacheObject) {
        this.config = config;
        this.cacheObject = cacheObject;
    }

    /**
     * @param cacheObject
     * @param userid
     * @param password
     */
    public CacheContext(AuthCacheConfig config, CacheObject cacheObject, String userid, @Sensitive String password) {
        this(config, cacheObject);
        this.userid = userid;
        this.password = password;
    }

    public CacheContext(AuthCacheConfig config, CacheObject cacheObject, X509Certificate[] certChain) {
        this.config = config;
        this.cacheObject = cacheObject;
        this.certChain = certChain;
    }

    /**
     * Gets the AuthCacheConfig object.
     *
     * @return
     */
    public AuthCacheConfig getAuthCacheConfig() {
        return config;
    }

    /**
     * Gets the subject being cached.
     *
     * @return
     */
    public Subject getSubject() {
        return cacheObject.getSubject();
    }

    /**
     * Gets the userid currently used.
     *
     * @return
     */
    public String getUserid() {
        return userid;
    }

    /**
     * Gets the password currently used.
     *
     * @return
     */
    @Sensitive
    public String getPassword() {
        return password;
    }

    /**
     * Gets the certificate currently used.
     *
     * @return
     */
    public X509Certificate[] getCertChain() {
        return certChain;
    }

}
