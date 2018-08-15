/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
