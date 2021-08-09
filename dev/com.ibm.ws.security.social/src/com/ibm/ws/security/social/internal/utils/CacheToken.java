/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Cache token containing the access token and the social login configuration id.
 */
public class CacheToken {

    private final String accessToken;
    private final String socialLoginConfigurationId;
    private String idToken = null;

    /**
     * @param accessToken
     * @param socialLoginConfigurationId
     */
    public CacheToken(@Sensitive String accessToken, String socialLoginConfigurationId) {
        this.accessToken = accessToken;
        this.socialLoginConfigurationId = socialLoginConfigurationId;
    }

    /**
     * @return access token.
     */
    @Sensitive
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @return social login configuration id.
     */
    public String getSocialLoginConfigurationId() {
        return socialLoginConfigurationId;
    }

    /**
     * @param id_token
     */
    public void setIdToken(@Sensitive String idToken) {
        this.idToken = idToken;
    }

    /**
     * @return id token.
     */
    @Sensitive
    public String getIdToken() {
        return idToken;
    }

}
