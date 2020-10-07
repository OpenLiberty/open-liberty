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
package com.ibm.oauth.core.api.oauth20.token;

import java.util.Map;

/**
 * This interface represents an OAuth 2.0 token.
 */
public interface OAuth20Token {

    /**
     * @return the unique ID to identify an OAuth 2.0 token in the token cache.
     */
    public String getId();

    /**
     * @return the componentId to which this token applies
     */
    public String getComponentId();

    /**
     * @return the token type. Possible values are:
     *         <ul>
     *         <li>access_token</li>
     *         <li>id_token</li>
     *         <li>authorization_grant</li>
     *         </ul>
     */
    public String getType();

    /**
     * @return the authorization grant type or access token type. Possible
     *         values are dependent upon the return of getType()
     *
     *         When getType() is access_token, possible values for subType are:
     *         <ul>
     *         <li>Bearer</li>
     *         </ul>
     *
     *         When getType() is authorization_grant, possible values for
     *         subType are:
     *         <ul>
     *         <li>authorization_code</li>
     *         <li>refresh_token</li>
     *         </ul>
     */
    public String getSubType();

    /**
     * @return The time stamp in milliseconds since the epoch when this token was
     *         created. This can be used along with the lifetime to calculate an
     *         expiration time.
     */
    public long getCreatedAt();

    /**
     * @return the lifetime of this token in seconds.
     */
    public int getLifetimeSeconds();

    /**
     * @return TRUE if this token should be cached, FALSE otherwise. Tokens
     *         which completely self-contained be their "getTokenString()"
     *         method need not be cached. Opaque bearer tokens which return a
     *         random key as the token string should always return false.
     */
    public boolean isPersistent();

    /**
     * @return the token string that is returned to the OAuth 2.0 client, unless
     *         the token is an encoded self-describing data structure and does
     *         not need too be cached, this method should return the same as the
     *         getId() method
     */
    public String getTokenString();

    /**
     * @return the id of the client that this token was created for
     */
    public String getClientId();

    /**
     * @return the name of the user who authorized this token (if the token was
     *         authorized to access a clients resources i.e client credentials
     *         flow, this method should return the client id)
     */
    public String getUsername();

    /**
     * @return an array of scope values that this token was approved for (if
     *         applicable)
     */
    public String[] getScope();

    /**
     * @return the redirect uri associated with this token (if applicable)
     */
    public String getRedirectUri();

    /**
     * @return the state id of this token. Tokens that are derivatives from
     *         another token (i.e. from a refresh token or authorization_code)
     *         must maintain the same stateId as the parent token.
     */
    public String getStateId();

    /**
     * @return an array of property names which can be used in calls to
     *         getExtensionProperty() to obtain extension property values for a
     *         particular token. Different extension properties are be set by
     *         different token spec implementations. The Bearer token spec does
     *         not use extension properties.
     */
    public String[] getExtensionPropertyNames();

    /**
     * @return an array of properties for a given property name. Different
     *         extension properties are be set by different token spec
     *         implementations. The Bearer token spec does not use extension
     *         properties.
     */
    public String[] getExtensionProperty(String propertyName);

    /**
     * When getType() is access_token returns the type of grant that generated the
     * access token, otherwise returns null.
     *
     * Valid grant type values for an access token are:
     *    authorization_code, implicit, resource_owner, client_credential, app_password, app_token
     */
    public String getGrantType();

    /**
     * @return the extensionProperties
     */
    public Map<String, String[]> getExtensionProperties();

    /**
     * Unused.  Obtain this info from the grantType.
     * If used, might return "app_password" or "app_token" if this token is to be used as one of those, otherwise return null.
     */
    public String getUsedFor();

    /**
     * In the case of an app-password or app-token, return the clientIds of the clients that will consume this token, or null otherwise.
     * This is not the same as getClientId, which returns the id of the client that -created- this token.
     * @return
     */
    public String[] getUsedBy();

    /**
     * In the case of an authorization grant code, return code_challenge if exists, or null
     * @return
     */
    public String getCodeChallenge();

    /**
     * In the case of authorization grant code, return code_challenge_method if exists, or null.
     *
     * @return
     */
    public String getCodeChallengeMethod();

    /**
     * In the case of an app-password or app-token, return the name of the application that this token is associated with.
     * Otherwise return null.
     * @return
     */
    public String getAppName();

    /**
     * Set the time the token was last retrieved from the in-memory cache or db.
     * Sets it to the value of System.currentTimeMillis.
     */
    public void setLastAccess();

    /**
     * @return the last time this token was last retrieved from the token cache.  Same format as System.currentTimeMillis.
     */
    public long getLastAccess();

}
