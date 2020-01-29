/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import com.google.gson.JsonArray;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class OidcBaseClient extends BaseClient implements Serializable, OidcOAuth20Client {

    public static final String SN_CLIENT_ID_ISSUED_AT = OIDCConstants.OIDC_CLIENTREG_ISSUED_AT;
    public static final String SN_REGISTRATION_CLIENT_URI = OIDCConstants.OIDC_CLIENTREG_REGISTRATION_CLIENT_URI;
    public static final String SN_CLIENT_SECRET_EXPIRES_AT = OIDCConstants.OIDC_CLIENTREG_SECRET_EXPIRES_AT;
    public static final String SN_TOKEN_ENDPOINT_AUTH_METHOD = OIDCConstants.OIDC_CLIENTREG_TOKEN_EP_AUTH_METH;
    public static final String SN_GRANT_TYPES = "grant_types";
    public static final String SN_RESPONSE_TYPES = "response_types";
    public static final String SN_APPLICATION_TYPE = OIDCConstants.OIDC_CLIENTREG_APP_TYPE;
    public static final String SN_SUBJECT_TYPE = OIDCConstants.OIDC_CLIENTREG_SUB_TYPE;
    public static final String SN_POST_LOGOUT_REDIRECT_URIS = OIDCConstants.OIDC_CLIENTREG_POST_LOGOUT_URIS;
    public static final String SN_PREAUTHORIZED_SCOPE = OIDCConstants.OIDC_CLIENTREG_PREAUTHORIZED_SCOPE;
    public static final String SN_INTROSPECT_TOKENS = "introspect_tokens";
    public static final String SN_TRUSTED_URI_PREFIXES = OIDCConstants.JSA_CLIENTREG_TRUSTED_URI_PREFIXES;
    public static final String SN_RESOURCE_IDS = "resource_ids";
    public static final String SN_FUNCTIONAL_USER_ID = "functional_user_id";
    public static final String SN_FUNCTIONAL_USER_GROUP_IDS = OIDCConstants.JSA_CLIENTREG_FUNCTIONAL_USER_GROUP_IDS;

    private static final long serialVersionUID = -2407700528170555986L;

    // these affect the names used for json<-->java transformations in database backed clients,
    // so are like an API. Don't change them without careful thought.
    @Expose
    @SerializedName(SN_CLIENT_ID_ISSUED_AT)
    private long clientIdIssuedAt = 0;

    @Expose
    @SerializedName(SN_REGISTRATION_CLIENT_URI)
    private String registrationClientUri = "";

    @Expose
    @SerializedName(SN_CLIENT_SECRET_EXPIRES_AT)
    private long clientSecretExpiresAt = 0;

    @Expose
    @SerializedName(SN_TOKEN_ENDPOINT_AUTH_METHOD)
    private String tokenEndpointAuthMethod = "";

    @Expose
    private String scope = "";

    @Expose
    @SerializedName(SN_GRANT_TYPES)
    private JsonArray grantTypes = new JsonArray();

    @Expose
    @SerializedName(SN_RESPONSE_TYPES)
    private JsonArray responseTypes = new JsonArray();

    @Expose
    @SerializedName(SN_APPLICATION_TYPE)
    private String applicationType = "";

    @Expose
    @SerializedName(SN_SUBJECT_TYPE)
    private String subjectType = "";

    @Expose
    @SerializedName(SN_POST_LOGOUT_REDIRECT_URIS)
    private JsonArray postLogoutRedirectUris = new JsonArray();

    @Expose
    @SerializedName(SN_PREAUTHORIZED_SCOPE)
    private String preAuthorizedScope = "";

    @Expose
    @SerializedName(SN_INTROSPECT_TOKENS)
    private boolean introspectTokens = false;

    @Expose
    @SerializedName(SN_TRUSTED_URI_PREFIXES)
    private JsonArray trustedUriPrefixes = new JsonArray();

    @Expose
    @SerializedName(SN_RESOURCE_IDS)
    private JsonArray resourceIds = new JsonArray(); // To be handle in DB

    @Expose
    @SerializedName(SN_FUNCTIONAL_USER_ID)
    private String functionalUserId = "";

    @Expose
    @SerializedName(SN_FUNCTIONAL_USER_GROUP_IDS)
    private JsonArray functionalUserGroupIds = new JsonArray();

    @Expose
    private boolean proofKeyForCodeExchange = false;
    
    @Expose
    private boolean publicClient = false;
    
    @Expose
    private boolean appPasswordAllowed = false;

    @Expose
    private boolean appTokenAllowed = false;

    @Expose
    @SerializedName(OAuth20Constants.SALT)
    private String salt = null;

    @Expose
    @SerializedName(OAuth20Constants.HASH_ALGORITHM)
    private String algorithm = null;

    @Expose
    @SerializedName(OAuth20Constants.HASH_ITERATIONS)
    private int iterations = HashSecretUtils.DEFAULT_ITERATIONS;

    @Expose
    @SerializedName(OAuth20Constants.HASH_LENGTH)
    private int length = HashSecretUtils.DEFAULT_KEYSIZE;

    public OidcBaseClient(String clientId,
            @Sensitive String clientSecret,
            JsonArray redirectUris,
            String clientName,
            String componentId,
            boolean isEnabled) {
        super(componentId,
                clientId,
                clientSecret,
                clientName,
                redirectUris,
                isEnabled);
    }

    /**
     * @return the clientIdIssuedAt
     */
    @Override
    public long getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    /**
     * @param clientIdIssuedAt the clientIdIssuedAt to set
     */
    @Trivial
    public void setClientIdIssuedAt(long clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    @Override
    public String getRegistrationClientUri() {
        return this.registrationClientUri;
    }

    @Override
    @Trivial
    public void setRegistrationClientUri(String registrationClientUri) {
        this.registrationClientUri = registrationClientUri;
    }

    /**
     * @return the clientSecretExpiresAt
     */
    @Override
    public long getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    /**
     * @param clientSecretExpiresAt the clientSecretExpiresAt to set
     */
    @Trivial
    public void setClientSecretExpiresAt(long clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    /**
     * @return the tokenEndpointAuthMethod
     */
    @Override
    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    /**
     * @param tokenEndpointAuthMethod the tokenEndpointAuthMethod to set
     */
    @Trivial
    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    /**
     * @return the scope
     */
    @Override
    public String getScope() {
        return scope;
    }

    /**
     * @param scope the scope to set
     */
    @Trivial
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * @return the grantTypes
     */
    @Override
    public JsonArray getGrantTypes() {
        return grantTypes;
    }

    /**
     * @param grantTypes the grantTypes to set
     */
    @Trivial
    public void setGrantTypes(JsonArray grantTypes) {
        this.grantTypes = grantTypes;
    }

    /**
     * @return the responseTypes
     */
    @Override
    public JsonArray getResponseTypes() {
        return responseTypes;
    }

    /**
     * @param responseTypes the responseTypes to set
     */
    @Trivial
    public void setResponseTypes(JsonArray responseTypes) {
        this.responseTypes = responseTypes;
    }

    /**
     * @return the applicationType
     */
    @Override
    public String getApplicationType() {
        return applicationType;
    }

    /**
     * @param applicationType the applicationType to set
     */
    @Trivial
    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    /**
     * @return the subjectType
     */
    @Override
    public String getSubjectType() {
        return subjectType;
    }

    /**
     * @param subjectType the subjectType to set
     */
    @Trivial
    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    /**
     * @return the postLogoutRedirectUris
     */
    @Override
    public JsonArray getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    /**
     * @param postLogoutRedirectUris the postLogoutRedirectUris to set
     */
    @Trivial
    public void setPostLogoutRedirectUris(JsonArray postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    /**
     * @return the preAuthorizedScope
     */
    @Override
    public String getPreAuthorizedScope() {
        return preAuthorizedScope;
    }

    /**
     * @param preAuthorizedScope the preAuthorizedScope to set
     */
    @Trivial
    public void setPreAuthorizedScope(String preAuthorizedScope) {
        this.preAuthorizedScope = preAuthorizedScope;
    }

    /**
     * @return the introspectTokens
     */
    @Override
    public boolean isIntrospectTokens() {
        return introspectTokens;
    }

    /**
     * @param introspectTokens the introspectTokens to set
     */
    @Trivial
    public void setIntrospectTokens(boolean introspectTokens) {
        this.introspectTokens = introspectTokens;
    }

    /**
     * @return the trustedUriPrefixes
     */
    @Override
    public JsonArray getTrustedUriPrefixes() {
        return trustedUriPrefixes;
    }

    /**
     * @param trustedUriPrefixes the trustedUriPrefixes to set
     */
    @Trivial
    public void setTrustedUriPrefixes(JsonArray trustedUriPrefixes) {
        this.trustedUriPrefixes = trustedUriPrefixes;
    }

    /**
     * @return the functionalUserId
     */
    @Override
    public String getFunctionalUserId() {
        return functionalUserId;
    }

    /**
     * @param functionalUserId the functionalUserId to set
     */
    @Trivial
    public void setFunctionalUserId(String functionalUserId) {
        this.functionalUserId = functionalUserId;
    }

    /**
     * @return the functionalUserGroupIds
     */
    @Override
    public JsonArray getFunctionalUserGroupIds() {
        return functionalUserGroupIds;
    }

    /**
     * @param functionalUserGroupIds the functionalUserGroupsIds to set
     */
    @Trivial
    public void setFunctionalUserGroupIds(JsonArray functionalUserGroupIds) {
        this.functionalUserGroupIds = functionalUserGroupIds;
    }

    @Override
    public boolean isAppPasswordAllowed() {
        return appPasswordAllowed;
    }

    @Trivial
    public void setAppPasswordAllowed(boolean appPasswordAllowed) {
        this.appPasswordAllowed = appPasswordAllowed;
    }

    @Override
    public boolean isAppTokenAllowed() {
        return appTokenAllowed;
    }

    @Trivial
    public void setAppTokenAllowed(boolean appTokenAllowed) {
        this.appTokenAllowed = appTokenAllowed;
    }

    @Override
    public boolean isProofKeyForCodeExchangeEnabled() {
        return proofKeyForCodeExchange;
    }

    @Trivial
    public void setProofKeyForCodeExchange(boolean proofKeyForCodeExchange) {
        this.proofKeyForCodeExchange = proofKeyForCodeExchange;
    }

    @Override
    public boolean isPublicClient() {
        return publicClient;
    }

    @Trivial
    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    @Trivial
    public OidcBaseClient getDeepCopy() {
        // RTC246290
        // OidcBaseClient deepCopy = OidcOAuth20Util.GSON_RAWEST.fromJson(OidcOAuth20Util.getJsonObj(this), OidcBaseClient.class);
        OidcBaseClient dc = new OidcBaseClient(this._clientId,
                this._clientSecret,
                copyArray(this._redirectURIs),
                this._clientName,
                this._componentId,
                this._isEnabled);

        dc.setApplicationType(this.applicationType);
        dc.setClientIdIssuedAt(this.clientIdIssuedAt);
        dc.setClientSecretExpiresAt(this.clientSecretExpiresAt);
        dc.setFunctionalUserGroupIds((copyArray(this.functionalUserGroupIds)));
        dc.setFunctionalUserId(this.functionalUserId);
        dc.setGrantTypes((copyArray(this.grantTypes)));
        dc.setIntrospectTokens(this.introspectTokens);
        dc.setPostLogoutRedirectUris((copyArray(postLogoutRedirectUris)));
        dc.setPreAuthorizedScope(this.preAuthorizedScope);
        dc.setRegistrationClientUri(this.registrationClientUri);
        dc.setResourceIds((copyArray(this.resourceIds)));
        dc.setResponseTypes((copyArray(this.responseTypes)));
        dc.setScope(this.scope);
        dc.setSubjectType(this.subjectType);
        dc.setTokenEndpointAuthMethod(this.tokenEndpointAuthMethod);
        dc.setTrustedUriPrefixes((copyArray(this.trustedUriPrefixes)));
        dc.setAllowRegexpRedirects(this.getAllowRegexpRedirects());
        dc.setAppPasswordAllowed(this.isAppPasswordAllowed());
        dc.setAppTokenAllowed(this.isAppTokenAllowed());
        dc.setProofKeyForCodeExchange(this.isProofKeyForCodeExchangeEnabled());
        dc.setPublicClient(this.isPublicClient());
        dc.setSalt(this.getSalt());
        dc.setAlgorithm(this.getAlgorithm());
        dc.setIterations(this.getIterations());
        dc.setLength(this.getLength());
        return dc;
    }

    @Trivial
    private JsonArray copyArray(JsonArray ar) {
        JsonArray ja = new JsonArray();
        if (ar != null) {
            ja.addAll(ar);
        }
        return ja;
    }

    /** {@inheritDoc} */
    @Override
    public JsonArray getResourceIds() {
        return resourceIds;
    }

    /**
     * @param trustedUriPrefixes the trustedUriPrefixes to set
     */
    @Trivial
    public void setResourceIds(JsonArray resourceIds) {
        this.resourceIds = resourceIds;
    }

    public void setSalt(@Sensitive String s) {
        salt = s;
    }

    @Sensitive
    public String getSalt() {
        return salt;
    }

    public void setAlgorithm(String s) {
        algorithm = s;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setIterations(int s) {
        iterations = s;
    }

    public int getIterations() {
        return iterations;
    }

    public void setLength(int s) {
        length = s;
    }

    public int getLength() {
        return length;
    }
}
