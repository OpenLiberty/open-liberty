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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.oauth20.error.impl.BrowserAndServerLogMessage;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.AbstractOidcEndpointServices;
import com.ibm.ws.security.oauth20.web.TraceConstants;

/**
 * Client Registration utilizes this class to perform parameter validation of property values
 */
public class OidcBaseClientValidator {
    protected static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages";
    private static final String[] illegalChars = new String[] { "<", ">" };

    private final OidcBaseClient client; // Defensive copy of OidcBaseClient reference used in constructor
    private static TraceComponent tc = Tr.register(OidcBaseClientValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private OidcBaseClientValidator(OidcBaseClient client) {
        this.client = client.getDeepCopy();

    }

    public static OidcBaseClientValidator getInstance(OidcBaseClient client) {
        // This method could be called from various callers (CachedDBOidcClientProvider, RegistrationEndpointServices, etc)
        // and the existing client name may or may not be decoded.
        return new OidcBaseClientValidator(clientGetClientName(client));
    }

    public static OidcBaseClient clientGetClientName(OidcBaseClient client) {
        try {
            if (client.getClientName() != null) {
                client.setClientName(URLDecoder.decode(client.getClientName(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException ex) {
            // keep the existing client name
        }
        return client;
    }

    /**
     * Method will perform pre-condition checking of Client Registration parameter
     * values meant for creation or update of client.
     *
     * @return copy of approved client, with some fields normalized
     * @throws OidcServerException
     */
    public OidcBaseClient validateCreateUpdate() throws OidcServerException {
        detectIllegalChars();
        validateAppType();

        validateResponseTypes();

        Set<String> grantTypes = validateGrantTypes();

        // response_types and grant_types need to match
        validateResponseAndGrantMatch(grantTypes);

        validateRedirectUris();

        // scope (space separated, no defaults will register)
        validateScopes();

        validateSujectType();

        validateTokenEndpointAuthMethod();

        validatePostLogoutRedirectUris();

        validatePreAuthorizedScopes();

        validateTrustedUriPrefixes();

        validateFunctionalUserGroupIds();

        validateOutputParameters();

        return this.client;
    }

    /**
     * check for disallowed characters that could allow javascript to be fed back to ui.
     */
    private void detectIllegalChars() throws OidcServerException {
        detectIllegalCharacters(client.getClientId());
        detectIllegalCharacters(client.getClientSecret());
        detectIllegalCharacters(client.getRedirectUris());
        detectIllegalCharacters(client.getClientName());
        detectIllegalCharacters(client.getPostLogoutRedirectUris());
        detectIllegalCharacters(client.getPreAuthorizedScope());
        detectIllegalCharacters(client.getFunctionalUserId());
        detectIllegalCharacters(client.getFunctionalUserGroupIds());
    }

    // detect illegal chars
    private void detectIllegalCharacters(@Sensitive String s) throws OidcServerException {
        if (s == null || s.length() == 0) {
            return;
        }
        for (int i = 0; i < illegalChars.length; i++) {
            if (s.contains(illegalChars[i])) {
                throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_ILLEGAL_CHAR", new Object[] { illegalChars[i] }), OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    // detect illegal chars
    private void detectIllegalCharacters(@Sensitive JsonArray a) throws OidcServerException {
        detectIllegalCharacters(a.toString());
    }

    /**
     * Method will return raw values of client, but with defaults set for
     * omitted fields
     *
     * @return copy of client passed in, but with default fields set where appropriate
     */
    public OidcBaseClient setDefaultsForOmitted() {
        // Default is 'web'
        setDefaultAppType();

        // Default is 'code'
        setDefaultResponseType();

        // Default is 'authorization_code'
        setDefaultGrantType();

        // Default is 'client_secret_basic'
        setDefaultTokenEndpointAuthMethod();

        // If redirectUris, postLogoutRedirectUri, trustedUriPrefixes is null, set to new JsonArray()
        setDefaultJsonArrayForNullUris();

        return this.client;
    }

    /**
    public OidcBaseClient validate() throws OidcServerException {
        return validateCommons(false);
    }
    
    public OidcBaseClient validateAndSetDefaultsOnErrors() {
        try {
            return validateCommons(true);
        } catch (OidcServerException e) {
        } //This will not occur
    
        return null; //This will not occur
    }
    
    private OidcBaseClient validateCommons(boolean setDefaultsOnError) throws OidcServerException {
        //application_type - defaults to web if omitted
        try {
            validateAppType();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                this.client.setApplicationType(OIDCConstants.OIDC_CLIENTREG_PARM_WEB);
            } else {
                throw e;
            }
        }
    
        try {
            validateResponseTypes();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Set to blank JsonArray because setResponseAndGrant will set defaults
                this.client.setResponseTypes(new JsonArray());
            } else {
                throw e;
            }
        }
    
        Set<String> grantTypes = new HashSet<String>();
        try {
            grantTypes = validateGrantTypes();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Set to blank JsonArray because setResponseAndGrant will set defaults
                this.client.setGrantTypes(new JsonArray());
            } else {
                throw e;
            }
        }
    
        try {
            //response_types and grant_types need to match
            validateResponseAndGrantMatch(grantTypes);
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Set default values if all else fails
                this.client.setResponseTypes(OidcOAuth20Util.initJsonArray(OIDCConstants.OIDC_DISC_RESP_TYPES_SUPP_CODE));
                this.client.setGrantTypes(OidcOAuth20Util.initJsonArray(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
            } else {
                throw e;
            }
        }
    
        try {
            validateRedirectUris();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Return empty array if issue found
                this.client.setRedirectUris(new JsonArray());
            } else {
                throw e;
            }
        }
    
        try {
            //scope (space separated, if omitted can register default scope)
            validateScopes();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Omit if issues is found
                this.client.setScope(null);
            } else {
                throw e;
            }
        }
    
        try {
            validateSujectType();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Omit if issues are found
                this.client.setSubjectType(null);
            } else {
                throw e;
            }
        }
    
        try {
            //token_endpoint_auth_method - if omitted, defaults to client_secret_basic
            validateTokenEndpointAuthMethod();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Set default
                this.client.setTokenEndpointAuthMethod(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_BASIC);
            } else {
                throw e;
            }
        }
    
        try {
            validatePostLogoutRedirectUris();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Return empty array if issue found
                this.client.setPostLogoutRedirectUris(new JsonArray());
            } else {
                throw e;
            }
        }
    
        try {
            validatePreAuthorizedScopes();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Omit if issues is found
                this.client.setPreAuthorizedScope(null);
            } else {
                throw e;
            }
        }
    
        try {
            validateTrustedUriPrefixes();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Return empty array if issue found
                this.client.setTrustedUriPrefixes(new JsonArray());
            } else {
                throw e;
            }
        }
    
        try {
            validateOutputParameters();
        } catch (OidcServerException e) {
            if (setDefaultsOnError) {
                //Set Defaults for Output Params?
                if (this.client.getClientSecretExpiresAt() < 0) {
                    this.client.setClientSecretExpiresAt(0);
                }
                if (this.client.getClientIdIssuedAt() < 0) {
                    this.client.setClientIdIssuedAt(0);
                }
    
            } else {
                throw e;
            }
        }
    
        return this.client;
    }
    **/

    protected void setDefaultAppType() {
        String appType = client.getApplicationType();
        if (OidcOAuth20Util.isNullEmpty(appType)) {
            client.setApplicationType(OIDCConstants.OIDC_CLIENTREG_PARM_WEB);
        }
    }

    protected void setDefaultResponseType() {
        JsonArray responseTypes = client.getResponseTypes();
        if (OidcOAuth20Util.isNullEmpty(responseTypes)) {
            client.setResponseTypes(OidcOAuth20Util.initJsonArray(OIDCConstants.OIDC_DISC_RESP_TYPES_SUPP_CODE));
        }
    }

    protected void setDefaultGrantType() {
        JsonArray grantTypes = client.getGrantTypes();
        if (OidcOAuth20Util.isNullEmpty(grantTypes)) {
            client.setGrantTypes(OidcOAuth20Util.initJsonArray(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
        }
    }

    protected void setDefaultTokenEndpointAuthMethod() {
        String tokenEndpointAuthMethod = client.getTokenEndpointAuthMethod();
        if (OidcOAuth20Util.isNullEmpty(tokenEndpointAuthMethod)) {
            client.setTokenEndpointAuthMethod(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_BASIC);
        }
    }

    protected void setDefaultJsonArrayForNullUris() {
        if (client.getRedirectUris() == null) {
            client.setRedirectUris(new JsonArray());
        }

        if (client.getPostLogoutRedirectUris() == null) {
            client.setPostLogoutRedirectUris(new JsonArray());
        }

        if (client.getTrustedUriPrefixes() == null) {
            client.setTrustedUriPrefixes(new JsonArray());
        }
    }

    protected void validateAppType() throws OidcServerException {
        String appType = client.getApplicationType();

        if (!OidcOAuth20Util.isNullEmpty(appType) &&
                !appType.equals(OIDCConstants.OIDC_CLIENTREG_PARM_NATIVE) &&
                !appType.equals(OIDCConstants.OIDC_CLIENTREG_PARM_WEB)) {
            throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_NOT_SUPPORTED", new Object[] { appType, OIDCConstants.OIDC_CLIENTREG_APP_TYPE }), OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    protected void validateResponseTypes() throws OidcServerException {
        JsonArray responseTypes = client.getResponseTypes();

        if (!OidcOAuth20Util.isNullEmpty(responseTypes)) {

            Set<String> dupeCheckerSet = new HashSet<String>();

            for (JsonElement response : responseTypes) {
                if (!OIDCConstants.OIDC_SUPP_RESP_TYPES_SET.contains(response.getAsString())) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_NOT_SUPPORTED", new Object[] { response.getAsString(), OAuth20Constants.RESPONSE_TYPE }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }

                if (!dupeCheckerSet.add(response.getAsString())) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_DUPE", new Object[] { response.getAsString(), OAuth20Constants.RESPONSE_TYPE }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }
    }

    protected Set<String> validateGrantTypes() throws OidcServerException {
        Set<String> grantTypeSet = new HashSet<String>();

        JsonArray grantTypes = client.getGrantTypes();

        if (!OidcOAuth20Util.isNullEmpty(grantTypes)) {
            for (JsonElement grant : grantTypes) {
                if (!OAuth20Constants.ALL_GRANT_TYPES_SET.contains(grant.getAsString())) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_NOT_SUPPORTED", new Object[] { grant.getAsString(), OAuth20Constants.GRANT_TYPE }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }

                if (!grantTypeSet.add(grant.getAsString())) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_DUPE", new Object[] { grant.getAsString(), OAuth20Constants.GRANT_TYPE }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }

        return grantTypeSet;
    }

    protected void validateResponseAndGrantMatch(Set<String> grantTypeSet) throws OidcServerException {
        JsonArray responseTypes = client.getResponseTypes();

        if (!OidcOAuth20Util.isNullEmpty(responseTypes)) {
            for (JsonElement responseType : responseTypes) {
                if (responseType.getAsString().equals(OAuth20Constants.RESPONSE_TYPE_CODE) && !grantTypeSet.contains(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE)) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_GRANT_RESPONSE_VALIDATION", new Object[] { responseType.getAsString(), OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);

                } else if ((responseType.getAsString().equals(OIDCConstants.OIDC_DISC_RESP_TYPES_SUPP_ID_TOKEN_TOKEN) || responseType.getAsString().equals(OIDCConstants.OIDC_DISC_RESP_TYPES_SUPP_TOKEN_ID_TOKEN))
                        && !grantTypeSet.contains(OAuth20Constants.GRANT_TYPE_IMPLICIT)) {

                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_GRANT_RESPONSE_VALIDATION", new Object[] { responseType.getAsString(), OAuth20Constants.GRANT_TYPE_IMPLICIT }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);

                }
            }
        }
    }

    protected void validateRedirectUris() throws OidcServerException {
        JsonArray redirectUris = client.getRedirectUris();

        // Normalize by setting empty array, if null
        if (redirectUris == null) {
            client.setRedirectUris(new JsonArray());
            return;
        }

        if (!OidcOAuth20Util.isNullEmpty(redirectUris)) {

            Set<String> dupeCheckerSet = new HashSet<String>();

            for (JsonElement redirectUriEle : redirectUris) {
                String redirectUriString = redirectUriEle.getAsString();
                URI uri;
                try {
                    uri = new URI(redirectUriEle.getAsString());
                } catch (URISyntaxException e) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_MALFORMED_URI", new Object[] { redirectUriString, OIDCConstants.OIDC_CLIENTREG_REDIRECT_URIS }),
                            OIDCConstants.ERROR_INVALID_REDIRECT_URI, HttpServletResponse.SC_BAD_REQUEST, e);
                }

                if ((client.getApplicationType() == null || client.getApplicationType().equals(OIDCConstants.OIDC_CLIENTREG_PARM_WEB)) && !uri.isAbsolute()) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_NOT_ABSOLUTE_URI", new Object[] { redirectUriString, OIDCConstants.OIDC_CLIENTREG_REDIRECT_URIS }),
                            OIDCConstants.ERROR_INVALID_REDIRECT_URI, HttpServletResponse.SC_BAD_REQUEST);
                }

                if (!dupeCheckerSet.add(redirectUriEle.getAsString())) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_DUPE", new Object[] { redirectUriString, OIDCConstants.OIDC_CLIENTREG_REDIRECT_URIS }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }
    }

    protected void validateScopes() throws OidcServerException {
        if (!OidcOAuth20Util.isNullEmpty(client.getScope())) {
            // TODO: Scope Character Validation http://tools.ietf.org/html/rfc6749#section-3.3
            // TODO: Check for duplicates
        }
    }

    protected void validateSujectType() throws OidcServerException {
        String subjectType = client.getSubjectType();

        // Liberty presently only supports subject_type public and not pairwise
        if (!OidcOAuth20Util.isNullEmpty(subjectType) && !subjectType.equals(OIDCConstants.OIDC_DISC_SUB_TYPES_SUPP_PUBLIC)) {
            throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_NOT_SUPPORTED", new Object[] { subjectType, OIDCConstants.OIDC_CLIENTREG_SUB_TYPE }),
                    OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    protected void validateTokenEndpointAuthMethod() throws OidcServerException {
        String tokenEndpointAuthMethod = client.getTokenEndpointAuthMethod();

        if (!OidcOAuth20Util.isNullEmpty(tokenEndpointAuthMethod) &&
                !tokenEndpointAuthMethod.equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_POST) &&
                !tokenEndpointAuthMethod.equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_BASIC) &&
                // !tokenEndpointAuthMethod.equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_CLIENT_SECRET_JWT) &&
                // !tokenEndpointAuthMethod.equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_PRIVATE_KEY_JWT) &&
                !tokenEndpointAuthMethod.equals(OIDCConstants.OIDC_DISC_TOKEN_EP_AUTH_METH_SUPP_NONE)) {
            throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_NOT_SUPPORTED", new Object[] { tokenEndpointAuthMethod, OIDCConstants.OIDC_CLIENTREG_TOKEN_EP_AUTH_METH }),
                    OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    protected void validatePostLogoutRedirectUris() throws OidcServerException {
        JsonArray postLogoutRedirectUris = client.getPostLogoutRedirectUris();

        // Normalize by setting empty array, if null
        if (postLogoutRedirectUris == null) {
            client.setPostLogoutRedirectUris(new JsonArray());
            return;
        }

        validateUris(postLogoutRedirectUris, OIDCConstants.OIDC_CLIENTREG_POST_LOGOUT_URIS);
    }

    protected void validatePreAuthorizedScopes() throws OidcServerException {
        // Removing condition check that ensures preauthorized scope is a subset of scope, because runtime performs reducing
        /**
        if (!OidcOAuth20Util.isNullEmpty(client.getPreAuthorizedScope()) && OidcOAuth20Util.isNullEmpty(client.getScope())) {
            String errorMsg = "The values for the client registration metadata field \"%s\" should also be specified as values in the client registration metadata field \"scope\".";
            String description = String.format(errorMsg, OIDCConstants.OIDC_CLIENTREG_PREAUTHORIZED_SCOPE);
            throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
        } else if (!OidcOAuth20Util.isNullEmpty(client.getPreAuthorizedScope()) && !OidcOAuth20Util.isNullEmpty(client.getScope())) {
            String errorMsg = "The value \"%s\" for the client registration metadata field \"%s\" should also be specified as a value in the client registration metadata field \"scope\".";
        
            String[] scopeArr = client.getScope().split(" ");
            Set<String> scopeSet = getSetFromArr(scopeArr);
        
            String[] preAuthorizedScopeArr = client.getPreAuthorizedScope().split(" ");
            for (String preAuthorizedScope : preAuthorizedScopeArr) {
                if (!scopeSet.contains(preAuthorizedScope)) {
                    String description = String.format(errorMsg, preAuthorizedScope, OIDCConstants.OIDC_CLIENTREG_PREAUTHORIZED_SCOPE);
                    throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        
        }
        **/

        // TODO: Scope Character Validation http://tools.ietf.org/html/rfc6749#section-3.3
        // TODO: Check for duplicates
    }

    protected void validateTrustedUriPrefixes() throws OidcServerException {
        JsonArray trustedUriPrefixes = client.getTrustedUriPrefixes();

        // Spec requires empty array to be returned if no values specified
        if (trustedUriPrefixes == null) {
            client.setTrustedUriPrefixes(new JsonArray());
            return;
        }

        validateUris(trustedUriPrefixes, OIDCConstants.JSA_CLIENTREG_TRUSTED_URI_PREFIXES);

        // Add terminating slashes to each value
        client.setTrustedUriPrefixes(AbstractOidcEndpointServices.getSlashTerminated(trustedUriPrefixes));
    }

    protected void validateFunctionalUserGroupIds() throws OidcServerException {
        JsonArray functionalUserGroupIds = client.getFunctionalUserGroupIds();

        // Set empty array if none specified
        if (functionalUserGroupIds == null) {
            client.setFunctionalUserGroupIds(new JsonArray());
            return;
        }

        // Ensure no duplicates exist
        Set<String> dupeCheckerSet = new HashSet<String>();
        for (JsonElement groupIdEle : functionalUserGroupIds) {
            String groupIdString = groupIdEle.getAsString();
            if (!dupeCheckerSet.add(groupIdString)) {
                throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_DUPE", new Object[] { groupIdString, "functional_user_groupIds" }),
                        OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    protected void validateOutputParameters() throws OidcServerException {
        if (client.getClientIdIssuedAt() != 0) {
            throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_OUT_NOT_ALLOWED", new Object[] { OIDCConstants.OIDC_CLIENTREG_ISSUED_AT }),
                    OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
        }

        if (client.getClientSecretExpiresAt() != 0) {
            throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_OUT_NOT_ALLOWED", new Object[] { OIDCConstants.OIDC_CLIENTREG_SECRET_EXPIRES_AT }),
                    OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
        }

        if (client.getRegistrationClientUri() != null && !client.getRegistrationClientUri().isEmpty()) {
            throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_OUT_NOT_ALLOWED", new Object[] { OIDCConstants.OIDC_CLIENTREG_REGISTRATION_CLIENT_URI }),
                    OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void validateUris(JsonArray uris, String clientMetadataField) throws OidcServerException {
        if (!OidcOAuth20Util.isNullEmpty(uris)) {

            Set<String> dupeCheckerSet = new HashSet<String>();

            for (JsonElement uriEle : uris) {
                String uriString = uriEle.getAsString();
                URI uri;
                try {
                    uri = new URI(uriString);
                } catch (URISyntaxException e) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_MALFORMED_URI", new Object[] { uriString, clientMetadataField }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST, e);
                }

                if (!uri.isAbsolute()) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_NOT_ABSOLUTE_URI", new Object[] { uriString, clientMetadataField }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }

                if (!dupeCheckerSet.add(uriString)) {
                    throw new OidcServerException(new BrowserAndServerLogMessage(tc, "OAUTH_CLIENT_REGISTRATION_VALUE_DUPE", new Object[] { uriString, clientMetadataField }),
                            OIDCConstants.ERROR_INVALID_CLIENT_METADATA, HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }
    }
}
