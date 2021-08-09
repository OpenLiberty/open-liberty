/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.oauth20.store;

/**
 * An OAuth token implementation used for storing or retrieving entries from an <code>OAuthStore</code> implementation.
 */
public class OAuthToken {

    private final String lookupKey;
    private final String uniqueId;
    private final String providerId;
    private final String type;
    private final String subType;
    private final long createdAt;
    private final int lifetimeInSeconds;
    private final long expires;
    private final String tokenString;
    private final String clientId;
    private final String username;
    private final String scope;
    private final String redirectUri;
    private final String stateId;
    private final String tokenProperties;

    // TODO: Determine if tokenString, stateId, and tokenProperties need to be annotated with @Sensitive
    /**
     * Constructs a new <code>OAuthToken</code> data transfer object.
     */
    public OAuthToken(String lookupKey, String uniqueId, String providerId, String type, String subType, long createdAt, int lifetimeInSeconds, long expires, String tokenString, String clientId, String username, String scope, String redirectUri, String stateId, String tokenProperties) {
        this.lookupKey = lookupKey;
        this.uniqueId = uniqueId;
        this.providerId = providerId;
        this.type = type;
        this.subType = subType;
        this.createdAt = createdAt;
        this.lifetimeInSeconds = lifetimeInSeconds;
        this.expires = expires;
        this.tokenString = tokenString;
        this.clientId = clientId;
        this.username = username;
        this.scope = scope;
        this.redirectUri = redirectUri;
        this.stateId = stateId;
        this.tokenProperties = tokenProperties;
    }

    /**
     * Gets the lookup key.
     *
     * @return the lookup key to be used to store the entry in the <code>OAuthStore</code>
     */
    public String getLookupKey() {
        return lookupKey;
    }

    /**
     * Gets the unique id.
     *
     * @return the unique id
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the OAuth provider id. This id can be used in combination with the lookup key to store the tokens per OAuth provider.
     *
     * @return the id of the OAuth provider that issued the token
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Gets the token type.
     *
     * @return the token type, "AUTHORIZATION_GRANT" or "ACCESS_TOKEN" for example
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the token sub type.
     *
     * @return the authorization grant type or access token type, "Code" or "Bearer" for example
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Gets the time the token was created at.
     *
     * @return the timestamp in milliseconds since the epoch when this token was created
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the token lifetime.
     *
     * @return the lifetime in seconds of this token
     */
    public int getLifetimeInSeconds() {
        return lifetimeInSeconds;
    }

    /**
     * Gets the time the token expires.
     *
     * @return the timestamp in milliseconds since the epoch when this token expires
     */
    public long getExpires() {
        return expires;
    }

    /**
     * Gets the encoded token string.
     *
     * @return the token string
     */
    public String getTokenString() {
        return tokenString;
    }

    /**
     * Gets the client id.
     *
     * @return the id of the client the token was issued to
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the user name.
     *
     * @return the user the token was issued for
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the token scope.
     *
     * @return the scope values that this token was approved for
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the redirect URI.
     *
     * @return the redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Gets the state id.
     *
     * @return the state id
     */
    public String getStateId() {
        return stateId;
    }

    /**
     * Gets the token extension properties as a JSON string.
     *
     * @return the JSON string with the token extension properties
     */
    public String getTokenProperties() {
        return tokenProperties;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj instanceof OAuthToken == false) {
            return false;
        }

        OAuthToken other = (OAuthToken) obj;

        if (!lookupKey.equals(other.lookupKey) || !uniqueId.equals(other.uniqueId) || !providerId.equals(other.providerId) ||
                !type.equals(other.type) || createdAt != other.createdAt || lifetimeInSeconds != other.lifetimeInSeconds ||
                expires != other.expires || !clientId.equals(other.clientId) || !username.equals(other.username)) {
            return false;
        }

        if (subType != null) {
            if (subType.equals(other.subType) == false) {
                return false;
            }
        } else if (other.subType != null) {
            return false;
        }

        if (tokenString != null) {
            if (tokenString.equals(other.tokenString) == false) {
                return false;
            }
        } else if (other.tokenString != null) {
            return false;
        }

        if (scope != null) {
            if (scope.equals(other.scope) == false) {
                return false;
            }
        } else if (other.scope != null) {
            return false;
        }

        if (redirectUri != null) {
            if (redirectUri.equals(other.redirectUri) == false) {
                return false;
            }
        } else if (other.redirectUri != null) {
            return false;
        }

        if (stateId != null) {
            if (stateId.equals(other.stateId) == false) {
                return false;
            }
        } else if (other.stateId != null) {
            return false;
        }

        if (tokenProperties != null) {
            if (tokenProperties.equals(other.tokenProperties) == false) {
                return false;
            }
        } else if (other.tokenProperties != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        Long hashes = (long) (lookupKey.hashCode() + uniqueId.hashCode() + providerId.hashCode() + type.hashCode() +
                Long.valueOf(createdAt).hashCode() + Integer.valueOf(lifetimeInSeconds).hashCode() + Long.valueOf(expires).hashCode() +
                clientId.hashCode()) + username.hashCode();

        if (subType != null) {
            hashes = hashes + subType.hashCode();
        }

        if (tokenString != null) {
            hashes = hashes + tokenString.hashCode();
        }

        if (scope != null) {
            hashes = hashes + scope.hashCode();
        }

        if (redirectUri != null) {
            hashes = hashes + redirectUri.hashCode();
        }

        if (stateId != null) {
            hashes = hashes + stateId.hashCode();
        }

        return hashes.hashCode(); // TODO: Use a Message Digest to create hash code.
    }

}
