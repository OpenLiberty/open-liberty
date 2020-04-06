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
package com.ibm.ws.security.oauth20.api;

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;

public interface OidcOAuth20Client extends OAuth20Client {
    public long getClientIdIssuedAt();

    /**
     * @return the clientSecretExpiresAt
     */
    public long getClientSecretExpiresAt();

    public String getRegistrationClientUri();

    public void setRegistrationClientUri(String value);

    /**
     * @return the tokenEndpointAuthMethod
     */
    public String getTokenEndpointAuthMethod();

    /**
     * @return the scope
     */
    public String getScope();

    /**
     * @return the grantTypes
     */
    public JsonArray getGrantTypes();

    /**
     * @return the responseTypes
     */
    public JsonArray getResponseTypes();

    /**
     * @return the applicationType
     */
    public String getApplicationType();

    /**
     * @return the subjectType
     */
    public String getSubjectType();

    /**
     * @return the postLogoutRedirectUris
     */
    public JsonArray getPostLogoutRedirectUris();

    /**
     * @return the preAuthorizedScope
     */
    public String getPreAuthorizedScope();

    /**
     * @return the introspectTokens
     */
    public boolean isIntrospectTokens();

    /**
     * @return the trustedUriPrefixes
     */
    public JsonArray getTrustedUriPrefixes();

    /**
     * @return the resourceIds
     */
    public JsonArray getResourceIds();

    /**
     * @return the functionalUserId
     */
    public String getFunctionalUserId();

    /**
     * @return the functionalUserGroupIds
     */
    public JsonArray getFunctionalUserGroupIds();

    public boolean isAppPasswordAllowed();

    public boolean isAppTokenAllowed();
    
    public boolean isProofKeyForCodeExchangeEnabled();
    
    public boolean isPublicClient();

}
