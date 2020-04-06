/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.util.JSONUtil;

public class OAuth20TokenImpl implements OAuth20Token, Serializable {

    private static final long serialVersionUID = -276162556269542122L;

    String _uniqueId;
    String _componentId;
    String _type;
    String _subType;
    long _createdAt = 0;
    int _lifetimeSeconds = 0;
    long _lastAccess = 0; // not persisted to db, only meaningful after extraction from db.
    String _tokenString;
    String _clientId;
    String _username;
    String[] _scopes;
    String _redirectUri;
    String _stateId;
    String accessToken;
    String refreshToken;
    Map<String, String[]> _extensionProperties;

    // the following fields are kept in the extensionProperties map:
    // usedFor
    // appName
    // usedBy
    // appId (but there's no getter for it)

    /**
     * The type of grant for an access token
     */
    private String _grantType = null;

    public OAuth20TokenImpl(String uniqueId, String componentId, String type,
            String subType, long createdAt, int lifetime, String tokenString,
            String clientId, String username, String[] scopes,
            String redirectUri, String stateId,
            Map<String, String[]> extensionProperties, String grantType) {
        _uniqueId = uniqueId;
        _componentId = componentId;
        _type = type;
        _subType = subType;
        _createdAt = createdAt;
        _lifetimeSeconds = lifetime;
        _tokenString = tokenString;
        _clientId = clientId;
        _username = username;
        _scopes = scopes;
        _redirectUri = redirectUri;
        _stateId = stateId;
        _grantType = grantType;
        _extensionProperties = extensionProperties;
    }

    public OAuth20TokenImpl(OAuth20Token tokenIn) {
        _uniqueId = tokenIn.getId();
        _componentId = tokenIn.getComponentId();
        _type = tokenIn.getType();
        _subType = tokenIn.getSubType();
        _createdAt = tokenIn.getCreatedAt();
        _lifetimeSeconds = tokenIn.getLifetimeSeconds();
        _tokenString = tokenIn.getTokenString();
        _clientId = tokenIn.getClientId();
        _username = tokenIn.getUsername();
        _scopes = tokenIn.getScope();
        _redirectUri = tokenIn.getRedirectUri();
        _stateId = tokenIn.getStateId();
        _grantType = tokenIn.getGrantType();
        _extensionProperties = tokenIn.getExtensionProperties();
    }

    public void setAccessTokenKey(String access) {
        accessToken = access;
    }

    public String getAccessTokenKey() {
        return accessToken;
    }

    public void setRefreshTokenKey(String refresh) {
        refreshToken = refresh;
    }

    public String getRefreshTokenKey() {
        return refreshToken;
    }

    /**
     * @return the unique key to identity this token in a cache
     */
    @Override
    public String getId() {
        return _uniqueId;
    }

    /**
     * @return the componentId this token applies to
     */
    @Override
    public String getComponentId() {
        return _componentId;
    }

    /**
     * @return the token type i.e. "AUTHORIZATION_GRANT" or "ACCESS_TOKEN"
     */
    @Override
    public String getType() {
        return _type;
    }

    /**
     * @return the authorization grant type or access token type e.g. "Code" or
     *         "Bearer"
     */
    @Override
    public String getSubType() {
        return _subType;
    }

    /**
     * @return The timestamp in milliseconds since the epoch when this token was
     *         created. This can be used along with the lifetime to calculate an
     *         expiry time.
     */
    @Override
    public long getCreatedAt() {
        return _createdAt;
    }

    /**
     * @return the lifetime (seconds) of this token
     */
    @Override
    public int getLifetimeSeconds() {
        return _lifetimeSeconds;
    }

    /**
     * @return TRUE if this token should be cached, FALSE otherwise
     */
    @Override
    public boolean isPersistent() {
        return true;
    }

    /**
     * @return the token string that is returned to the OAuth 2.0 client, unless
     *         the token is an encoded self-describing data structure and does
     *         not need too be cached, this method should return the same as the
     *         getId() method
     */
    @Override
    public String getTokenString() {
        return _tokenString;
    }

    /**
     * @return the id of the client that this token was created for
     */
    @Override
    public String getClientId() {
        return _clientId;
    }

    /**
     * @return the name of the user who authorized this token (if the token was
     *         authorized to access a clients resources i.e client credentials
     *         flow, this method should return the client id)
     */
    @Override
    public String getUsername() {
        return _username;
    }

    /**
     * @return an array of scope values that this token was approved for (if
     *         applicable)
     */
    @Override
    public String[] getScope() {
        return _scopes;
    }

    /**
     * @return the redirect uri associated with this token (if applicable)
     */
    @Override
    public String getRedirectUri() {
        return _redirectUri;
    }

    /**
     * @return the state id of this token. Tokens that are derivatives from
     *         another token (i.e. from a refresh token or authorization_code)
     *         must maintain the same stateId as the parent token.
     */
    @Override
    public String getStateId() {
        return _stateId;
    }

    /**
     * @return an array of property names which can be used in calls to
     *         getExtensionProperty() to obtain extension property values for a
     *         particular token. Different extension properties are be set by
     *         different token spec implementations. The Bearer token spec does
     *         not use extension properties.
     */
    @Override
    public String[] getExtensionPropertyNames() {
        String[] result = null;
        if (_extensionProperties != null && _extensionProperties.size() > 0) {
            Set<String> keys = _extensionProperties.keySet();
            if (keys != null) {
                result = keys.toArray(new String[keys.size()]);
            }
        }
        return result;
    }

    /**
     * @return an array of properties for a given property name. Different
     *         extension properties are be set by different token spec
     *         implementations. The Bearer token spec does not use extension
     *         properties.
     */
    @Override
    public String[] getExtensionProperty(String propertyName) {
        String[] result = null;
        if (_extensionProperties != null && _extensionProperties.size() > 0) {
            result = _extensionProperties.get(propertyName);
        }
        return result;
    }

    /**
    
     */
    @Override
    public String getGrantType() {
        return _grantType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String[]> getExtensionProperties() {
        return _extensionProperties;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("_uniqueId: " + _uniqueId);
        sb.append(" _componentId: " + _componentId);
        sb.append(" _type: " + _type);
        sb.append(" _subType: " + _subType);
        sb.append(" _createdAt: " + _createdAt);
        sb.append(" _lifetime: " + _lifetimeSeconds);
        sb.append(" _tokenString: " + _tokenString);
        sb.append(" _clientId: " + _clientId);
        sb.append(" _username: " + _username);
        sb.append(" _scopes: " + arrayToString(_scopes));
        sb.append(" _redirectUri: " + _redirectUri);
        sb.append(" _stateId: " + _stateId);
        sb.append(" _grantType: " + _grantType);
        sb.append(" _extensionProperties: " + JSONUtil.getJSONStrings(_extensionProperties));
        sb.append("}");
        return sb.toString();
    }

    String arrayToString(String[] strs) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        if (strs != null && strs.length > 0) {
            for (int i = 0; i < strs.length; i++) {
                sb.append(strs[i]);
                if (i < (strs.length - 1)) {
                    sb.append(",");
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getUsedFor() {
        String[] buf = getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.USED_FOR);
        return buf == null ? null : buf[0];
    }

    /** {@inheritDoc} */
    @Override
    public String[] getUsedBy() {
        return getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.USED_BY);
    }

    /** {@inheritDoc} */
    @Override
    public String getAppName() {
        String[] buf = getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.APP_NAME);
        return buf == null ? null : buf[0];
    }

    /** {@inheritDoc} */
    @Override
    public long getLastAccess() {
        return _lastAccess;
    }

    /** {@inheritDoc} */
    @Override
    public void setLastAccess() {
        _lastAccess = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see com.ibm.oauth.core.api.oauth20.token.OAuth20Token#getCodeChallenge()
     */
    @Override
    public String getCodeChallenge() {
        String[] buf = getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.CODE_CHALLENGE);
        return buf == null ? null : buf[0];
    }

    /* (non-Javadoc)
     * @see com.ibm.oauth.core.api.oauth20.token.OAuth20Token#getCodeChallengeMethod()
     */
    @Override
    public String getCodeChallengeMethod() {
        String[] buf = getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.CODE_CHALLENGE_METHOD);
        return buf == null ? null : buf[0];
    }

}
