/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    private final AuthCacheConfig config;
    private final Subject subject;
    private String userid;
    private String password;
    private X509Certificate[] certChain = null;

    /**
     * @param config
     * @param cacheObject
     */
    public CacheContext(AuthCacheConfig config, Subject subject) {
        this.config = config;
        this.subject = subject;
    }

    /**
     * @param cacheObject
     * @param userid
     * @param password
     */
    public CacheContext(AuthCacheConfig config, Subject subject, String userid, @Sensitive String password) {
        this(config, subject);
        this.userid = userid;
        this.password = password;
    }

    public CacheContext(AuthCacheConfig config, Subject subject, X509Certificate[] certChain) {
        this.config = config;
        this.subject = subject;
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
        return subject;
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
