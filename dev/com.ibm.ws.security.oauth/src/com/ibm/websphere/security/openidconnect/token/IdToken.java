/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.openidconnect.token;

import java.util.List;
import java.util.Map;

import com.ibm.websphere.security.WSSecurityException;

public interface IdToken {

    /**
     * Returns the JWT ID of the IdToken
     * This is not required.
     * Case sensitive
     * (key-id jti)
     *
     * @return JWT ID
     */
    public String getJwtId();

    /**
     * Returns the raw Id of the IdToken
     * This is not required.
     *
     * @return raw Id token
     */
    public String getRawIdToken() throws WSSecurityException;

    /**
     * returns the Type of IdToken, such as: Bearer
     * This is not required.
     * Case sensitive
     * (key-id typ)
     *
     * @return Token Type
     */
    public String getType();

    /**
     * returns Issuer Identifier for the Issuer of the Response
     * This is required
     * Case sensitive
     * (claim iss)
     *
     * @return Issuer
     */
    public String getIssuer();

    /**
     * returns  the Subject Identifier
     * This is required.
     * Case sensitive
     * (claim sub)
     *
     * @return subject Id
     */
    public String getSubject();

    /**
     * returns the audience(s)
     * This is required
     * Case sensitive
     * (claim aud)
     *
     * @return audience(s)
     */
    public List<String> getAudience();

    /**
     * returns the client Id
     * This is not required
     * (key-id azp2)
     *
     * @return Client Id
     */
    public String getClientId();

    /**
     * return the expiration time of the Id Token
     * The time is represented as the number of seconds from 1970-01-01T0:0:0:0Z
     * This is required
     * (claim exp)
     *
     * @return Expiration time in seconds
     */
    public long getExpirationTimeSeconds();

    /**
     * return the time which Id Token is not valid before it
     * The time is represented as the number of seconds from 1970-01-01T0:0:0:0Z
     * This is not required
     * (key-id nbf)
     *
     * @return Not Before Time in seconds
     */
    public long getNotBeforeTimeSeconds();

    /**
     * returns the issued time of Id Token
     * This is required
     * (claim iat)
     *
     * @return the issued time in seconds
     */
    public long getIssuedAtTimeSeconds();

    /**
     * returns The authorization time of the Id Token
     * This is not required
     * (key-id auth_time)
     *
     * @return The authorization time in seconds
     */
    public long getAuthorizationTimeSeconds(); //

    /**
     * returns the value of nonce
     * This value is optional usually.
     *
     * But it's required when the request of a RP Client provides a nonce.
     * In this case, the value of nonce must be the same as the one that
     * the RP Client provides.
     *
     * case sensitive
     * (claim nonce)
     *
     * @return the value of nonce
     */
    public String getNonce();

    /**
     * returns the Hash code of access token
     * This is optional
     * (claim at_hash)
     *
     *  @return the Hash code of the access token
     */
    public String getAccessTokenHash(); //

    /**
     * returns Authentication Context Class Reference
     * This is optional
     * (claim acr)
     *
     * @return Authentication Context Class Reference
     */
    public String getClassReference(); //

    /**
     * returns Authentication Methods References
     * This is optional
     * (claim amr)
     *
     * @return Authentication Methods References
     */
    public List<String> getMethodsReferences(); //

    /**
     * Authorized Party
     * This is optional
     * Case sensitive
     * (claim azp)
     *
     * @return Authorized Party
     */
    public String getAuthorizedParty();

    /**
     * Using the key to get its value
     *
     * @param key - the claim or key-id
     * @return The value
     */
    public Object getClaim(String key);

    /**
     * get all the claims in the payload of Id Token
     *
     * @return all the claims in the payload of Id Token
     */
    public Map<String, Object> getAllClaims();

    /**
     * Get the access token
     *
     * @return the access token string
     */
    public String getAccessToken();

    /**
     * Get the refresh token
     *
     * @return the refresh token string if exists, otherwise return null
     */
    public String getRefreshToken();

    /**
     * @return all the claims in Json format
     */
    public String getAllClaimsAsJson();

}
