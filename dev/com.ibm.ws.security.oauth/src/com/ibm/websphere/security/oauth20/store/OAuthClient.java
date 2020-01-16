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
 * An OAuth client implementation used for storing or retrieving entries from an <code>OAuthStore</code> implementation.
 */
public class OAuthClient {

    private final String providerId;
    private final String clientId;
    private final String clientSecret;
    private final String displayName;
    private final boolean enabled;
    private final String clientMetadata;

    /**
     * Constructs a new <code>OAuthClient</code> data transfer object.
     */
    public OAuthClient(String providerId, String clientId, String clientSecret, String displayName, boolean enabled, String clientMetadata) {
        this.providerId = providerId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.displayName = displayName;
        this.enabled = enabled;
        this.clientMetadata = clientMetadata;
    }

    /**
     * Gets the OAuth provider id.
     *
     * @return the id of the OAuth provider this client is registered with
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Gets the client id.
     *
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the encoded client secret.
     *
     * @return the client secret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns whether this client can participate in an OAuth flow or not.
     *
     * @return true when the client is allowed to participate in an OAuth flow with the OAuth provider it is registered with
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the meta data as a JSON string.
     *
     * @return the JSON string with the client meta data
     */
    public String getClientMetadata() {
        return clientMetadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj instanceof OAuthClient == false) {
            return false;
        }

        OAuthClient other = (OAuthClient) obj;

        if (!providerId.equals(other.providerId)) {
            return false;
        }

        if (!clientId.equals(other.clientId)) {
            return false;
        }

        if (clientSecret != null) {
            if (clientSecret.equals(other.clientSecret) == false) {
                return false;
            }
        } else if (other.clientSecret != null) {
            return false;
        }

        if (displayName != null) {
            if (displayName.equals(other.displayName) == false) {
                return false;
            }
        } else if (other.displayName != null) {
            return false;
        }

        if (enabled != other.enabled) {
            return false;
        }

        if (clientMetadata != null) {
            if (clientMetadata.equals(other.clientMetadata) == false) {
                return false;
            }
        } else if (other.clientMetadata != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        Long hashes = (long) (providerId.hashCode() + clientId.hashCode()) + Boolean.valueOf(enabled).hashCode();
        if (clientSecret != null) {
            hashes = hashes + clientSecret.hashCode();
        }
        if (displayName != null) {
            hashes = hashes + displayName.hashCode();
        }
        if (clientMetadata != null) {
            hashes = hashes + clientMetadata.hashCode();
        }

        return hashes.hashCode(); // TODO: Use a Message Digest to create hash code.
    }

}
