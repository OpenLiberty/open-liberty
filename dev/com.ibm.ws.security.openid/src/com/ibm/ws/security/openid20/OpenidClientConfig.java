/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20;

import java.util.List;

import com.ibm.ws.security.openid20.internal.UserInfo;

/**
 * Process the openID entry in the server.xml file
 */

public interface OpenidClientConfig {
    /**
     * @return the allowOnlyTrustedProvider
     */
    public boolean getAllowStateless();

    /**
     * @return the nonceValidTime
     */
    public long getNonceValidTime();

    /**
     * @return the maxSize
     */
    public int getMaxDiscoveryCacheSize();

    /**
     * @return the nonceValidTime
     */
    public int getMaxAssociationAttemps();

    /**
     * @return the sessionEncryptionType
     */
    public String getSessionEncryptionType();

    /**
     * @return the signatureAlgorithm
     */
    public String getSignatureAlgorithm();

    /**
     * @return the sslRef
     */
    public String getSslRef();

    /**
     * @return the userInfo
     */
    public List<UserInfo> getUserInfo();

    /**
     * @return the failedAssocExpire
     */
    public long getFailedAssocExpire();

    /**
     * @return the connectTimeout
     */
    public long getConnectTimeout();

    /**
     * @return the socketTimeout
     */
    public long getSocketTimeout();

    /**
     * @return the hostNameVerificationEnabled
     */
    public boolean isHostNameVerificationEnabled();

    /**
     * @return the httpsRequired
     */
    public boolean ishttpsRequired();

    /**
     * @return the checkImmediate
     */
    public boolean isCheckImmediate();

    /**
     * @return the mapIdentityToRegistryUser
     */
    public boolean isMapIdentityToRegistryUser();

    /**
     * @return the useClientIdentity
     */
    public boolean isUseClientIdentity();

    /**
     * @return the searchNumberOfUserInfoToMap
     */
    public int getSearchNumberOfUserInfoToMap();

    /**
     * @return maxDiscoverRetry
     */
    public int getMaxDiscoverRetry();

    /**
     * @return groupIdentifier
     */
    public String getGroupIdentifier();

    /**
     * @return realmIdentifier
     */
    public String getRealmIdentifier();

    /**
     * @return encoding
     */
    public String getCharacterEncoding();

    /**
     * @return includeUserInfoInSubject
     */
    public boolean isIncludeUserInfoInSubject();

    /**
     * @return includeCustomCacheKeyInSubject
     */
    public boolean isIncludeCustomCacheKeyInSubject();

    /**
     * @return opRealm
     */

    /**
     * This method is specific for Openid TAI
     * 
     * @return openid_identifier
     */
    public String getProviderIdentifier();

    /**
     * @return
     */
    public boolean allowBasicAuthentication();

    public boolean isTryOpenIDIfBasicAuthFails();

    public String getAuthFilterId();

}
