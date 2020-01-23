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
 * An OAuth consent implementation used for storing or retrieving entries from an <code>OAuthStore</code> implementation.
 */
public class OAuthConsent {

    private final String clientId;
    private final String user;
    private final String scope;
    private final String resource;
    private final String providerId;
    private final long expires;
    private final String consentProperties;

    /**
     * Constructs a new <code>OAuthConsent</code> data transfer object.
     */
    public OAuthConsent(String clientId, String user, String scope, String resource, String providerId, long expires, String consentProperties) {
        this.clientId = clientId;
        this.user = user;
        this.scope = scope;
        this.resource = resource;
        this.providerId = providerId;
        this.expires = expires;
        this.consentProperties = consentProperties;
    }

    /**
     * Gets the client id.
     *
     * @return the id of the client granted consent to access the resource
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the user name.
     *
     * @return the user that gave consent
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the scope.
     *
     * @return the scope values the user consented to
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the resource.
     *
     * @return the resource the client was granted consent to
     */
    public String getResource() {
        return resource;
    }

    /**
     * Gets the OAuth provider id.
     *
     * @return the id of the OAuth provider from which consent was given
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Gets the time the consent expires.
     *
     * @return the timestamp in milliseconds since the epoch when this consent expires
     */
    public long getExpires() {
        return expires;
    }

    /**
     * Gets the consent properties as a JSON string.
     *
     * @return the JSON string with the consent properties
     */
    public String getConsentProperties() {
        return consentProperties;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj instanceof OAuthConsent == false) {
            return false;
        }

        OAuthConsent other = (OAuthConsent) obj;

        if (!clientId.equals(other.clientId) || !user.equals(other.user) || !scope.equals(other.scope) ||
                !providerId.equals(other.providerId) || expires != other.expires ||
                !consentProperties.equals(other.consentProperties)) {
            return false;
        }

        if (resource != null) {
            if (resource.equals(other.resource) == false) {
                return false;
            }
        } else if (other.resource != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        Long hashes = (long) (clientId.hashCode() + user.hashCode() + scope.hashCode() + providerId.hashCode() + Long.valueOf(expires).hashCode() + consentProperties.hashCode());

        if (resource != null) {
            hashes = hashes + resource.hashCode();
        }

        return hashes.hashCode(); // TODO: Use a Message Digest to create hash code.
    }

}
