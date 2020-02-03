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
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.Arrays;

import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;

/**
 * OIDC Discovery Service Bean
 *
 * Properties based off Draft 21:
 * http://openid.bitbucket.org/openid-connect-discovery-1_0.html
 *
 * Note that several properties have been commented out because they are currently not being utilized.
 */
public abstract class OIDCAbstractDiscoveryModel {
    private String issuer;
    private String authorization_endpoint;
    private String token_endpoint;
    private String jwks_uri;
    private String[] response_types_supported;
    private String[] subject_types_supported;
    private String[] id_token_signing_alg_values_supported;
    private String userinfo_endpoint;
    private String registration_endpoint;
    private String[] scopes_supported;
    private String[] claims_supported;
    private String[] response_modes_supported;
    private String[] grant_types_supported;
    private String[] token_endpoint_auth_methods_supported;
    private String[] display_values_supported;
    private String[] claim_types_supported;
    private boolean claims_parameter_supported;
    private boolean request_parameter_supported;
    private boolean request_uri_parameter_supported;
    private boolean require_request_uri_registration;
    private String check_session_iframe;
    private String end_session_endpoint;
    private String revocation_endpoint;
    private String app_passwords_endpoint;
    private String app_tokens_endpoint;
    private String personal_token_mgmt_endpoint;
    private String users_token_mgmt_endpoint;
    private String client_mgmt_endpoint;
    private String[] code_challenge_methods_supported;

    /**
     * OIDC Properties not utilized in implementation
     **/
    //private String[] acr_values_supported;
    //private String[] id_token_encryption_alg_values_supported;
    //private String[] id_token_encryption_enc_values_supported;
    //private String[] userinfo_signing_alg_values_supported;
    //private String[] userinfo_encryption_alg_values_supported;
    //private String[] userinfo_encryption_enc_values_supported;
    //private String[] request_object_signing_alg_values_supported;
    //private String[] request_object_encryption_alg_values_supported;
    //private String[] request_object_encryption_enc_values_supported;
    //private String[] token_endpoint_auth_signing_alg_values_supported;
    //private String service_documentation;
    //private String[] claims_locales_supported;
    //private String[] ui_locales_supported;
    //private String op_policy_uri;
    //private String op_tos_uri;

    protected OIDCAbstractDiscoveryModel() {
    }

    /**
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * @param issuer the issuer to set
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * @return the authorizationEndpoint
     */
    public String getAuthorizationEndpoint() {
        return authorization_endpoint;
    }

    /**
     * @param authorizationEndpoint the authorizationEndpoint to set
     */
    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorization_endpoint = authorizationEndpoint;
    }

    /**
     * @return the tokenEndpoint
     */
    public String getTokenEndpoint() {
        return token_endpoint;
    }

    /**
     * @param tokenEndpoint the tokenEndpoint to set
     */
    public void setTokenEndpoint(String tokenEndpoint) {
        this.token_endpoint = tokenEndpoint;
    }

    /**
     * @return the jwks_uri
     */
    public String getJwks_uri() {
        return jwks_uri;
    }

    /**
     * @param jwks_uri the jwks_uri to set
     */
    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    /**
     * @return the responseTypesSupported
     */
    public String[] getResponseTypesSupported() {
        return defensiveCopy(response_types_supported);
    }

    /**
     * @param responseTypesSupported the responseTypesSupported to set
     */
    public void setResponseTypesSupported(String[] responseTypesSupported) {
        this.response_types_supported = defensiveCopy(responseTypesSupported);
    }

    /**
     * @return the subjectTypesSupported
     */
    public String[] getSubjectTypesSupported() {
        return defensiveCopy(subject_types_supported);
    }

    /**
     * @param subjectTypesSupported the subjectTypesSupported to set
     */
    public void setSubjectTypesSupported(String[] subjectTypesSupported) {
        this.subject_types_supported = defensiveCopy(subjectTypesSupported);
    }

    /**
     * @return the idTokenSigningAlgValuesSupported
     */
    public String[] getIdTokenSigningAlgValuesSupported() {
        return defensiveCopy(id_token_signing_alg_values_supported);
    }

    /**
     * @param idTokenSigningAlgValuesSupported the idTokenSigningAlgValuesSupported to set
     */
    public void setIdTokenSigningAlgValuesSupported(String[] idTokenSigningAlgValuesSupported) {
        this.id_token_signing_alg_values_supported = defensiveCopy(idTokenSigningAlgValuesSupported);
    }

    /**
     * @return the userinfoEndpoint
     */
    public String getUserinfoEndpoint() {
        return userinfo_endpoint;
    }

    /**
     * @param userinfoEndpoint the userinfoEndpoint to set
     */
    public void setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfo_endpoint = userinfoEndpoint;
    }

    /**
     * @return the registrationEndpoint
     */
    public String getRegistrationEndpoint() {
        return registration_endpoint;
    }

    /**
     * @param registrationEndpoint the registrationEndpoint to set
     */
    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registration_endpoint = registrationEndpoint;
    }

    /**
     * @return the scopesSupported
     */
    public String[] getScopesSupported() {
        return defensiveCopy(scopes_supported);
    }

    /**
     * @param scopesSupported the scopesSupported to set
     */
    public void setScopesSupported(String[] scopesSupported) {
        this.scopes_supported = defensiveCopy(scopesSupported);
    }

    /**
     * @return the claimsSupported
     */
    public String[] getClaimsSupported() {
        return defensiveCopy(claims_supported);
    }

    /**
     * @param claimsSupported the claimsSupported to set
     */
    public void setClaimsSupported(String[] claimsSupported) {
        this.claims_supported = defensiveCopy(claimsSupported);
    }

    /**
     * @return the responseModesSupported
     */
    public String[] getResponseModesSupported() {
        return defensiveCopy(response_modes_supported);
    }

    /**
     * @param responseModesSupported the responseModesSupported to set
     */
    public void setResponseModesSupported(String[] responseModesSupported) {
        this.response_modes_supported = defensiveCopy(responseModesSupported);
    }

    /**
     * @return the grantTypesSupported
     */
    public String[] getGrantTypesSupported() {
        return defensiveCopy(grant_types_supported);
    }

    /**
     * @param grantTypesSupported the grantTypesSupported to set
     */
    public void setGrantTypesSupported(String[] grantTypesSupported) {
        this.grant_types_supported = defensiveCopy(grantTypesSupported);
    }

    /**
     * @return the tokenEndpointAuthMethodsSupported
     */
    public String[] getTokenEndpointAuthMethodsSupported() {
        return defensiveCopy(token_endpoint_auth_methods_supported);
    }

    /**
     * @param tokenEndpointAuthMethodsSupported the tokenEndpointAuthMethodsSupported to set
     */
    public void setTokenEndpointAuthMethodsSupported(String[] tokenEndpointAuthMethodsSupported) {
        this.token_endpoint_auth_methods_supported = defensiveCopy(tokenEndpointAuthMethodsSupported);
    }

    /**
     * @return the displayValuesSupported
     */
    public String[] getDisplayValuesSupported() {
        return defensiveCopy(display_values_supported);
    }

    /**
     * @param displayValuesSupported the displayValuesSupported to set
     */
    public void setDisplayValuesSupported(String[] displayValuesSupported) {
        this.display_values_supported = defensiveCopy(displayValuesSupported);
    }

    /**
     * @return the claimTypesSupported
     */
    public String[] getClaimTypesSupported() {
        return defensiveCopy(claim_types_supported);
    }

    /**
     * @param claimTypesSupported the claimTypesSupported to set
     */
    public void setClaimTypesSupported(String[] claimTypesSupported) {
        this.claim_types_supported = defensiveCopy(claimTypesSupported);
    }

    /**
     * @return the claimsParameterSupported
     */
    public boolean isClaimsParameterSupported() {
        return claims_parameter_supported;
    }

    /**
     * @param claimsParameterSupported the claimsParameterSupported to set
     */
    public void setClaimsParameterSupported(boolean claimsParameterSupported) {
        this.claims_parameter_supported = claimsParameterSupported;
    }

    /**
     * @return the requestParameterSupported
     */
    public boolean isRequestParameterSupported() {
        return request_parameter_supported;
    }

    /**
     * @param requestParameterSupported the requestParameterSupported to set
     */
    public void setRequestParameterSupported(boolean requestParameterSupported) {
        this.request_parameter_supported = requestParameterSupported;
    }

    /**
     * @return the requestUriParameterSupported
     */
    public boolean isRequestUriParameterSupported() {
        return request_uri_parameter_supported;
    }

    /**
     * @param requestUriParameterSupported the requestUriParameterSupported to set
     */
    public void setRequestUriParameterSupported(boolean requestUriParameterSupported) {
        this.request_uri_parameter_supported = requestUriParameterSupported;
    }

    /**
     * @return the requireRequestUriRegistration
     */
    public boolean isRequireRequestUriRegistration() {
        return require_request_uri_registration;
    }

    /**
     * @param requireRequestUriRegistration the requireRequestUriRegistration to set
     */
    public void setRequireRequestUriRegistration(boolean requireRequestUriRegistration) {
        this.require_request_uri_registration = requireRequestUriRegistration;
    }

    /**
     * @return the checkSessionIframe
     */
    public String getCheckSessionIframe() {
        return check_session_iframe;
    }

    /**
     * @param checkSessionIframe the checkSessionIframe to set
     */
    public void setCheckSessionIframe(String checkSessionIframe) {
        this.check_session_iframe = checkSessionIframe;
    }

    /**
     * @return the endSessionEndpoint
     */
    public String getEndSessionEndpoint() {
        return end_session_endpoint;
    }

    /**
     * @param endSessionEndpoint the endSessionEndpoint to set
     */
    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.end_session_endpoint = endSessionEndpoint;
    }

    /**
     * @return the revocationEndpoint
     */
    public String getRevocationEndpoint() {
        return revocation_endpoint;
    }

    /**
     * @param revocationEndpoint the revocationEndpoint to set
     */
    public void setRevocationEndpoint(String revocationEndpoint) {
        this.revocation_endpoint = revocationEndpoint;
    }

    /**
     * @return the appPasswordsEndpoint
     */
    public String getAppPasswordsEndpoint() {
        return app_passwords_endpoint;
    }

    /**
     * @param appPasswordsEndpoint the appPasswordsEndpoint to set
     */
    public void setAppPasswordsEndpoint(String appPasswordsEndpoint) {
        this.app_passwords_endpoint = appPasswordsEndpoint;
    }

    /**
     * @return the appTokensEndpoint
     */
    public String getAppTokensEndpoint() {
        return app_tokens_endpoint;
    }

    /**
     * @param appTokensEndpoint the appTokensEndpoint to set
     */
    public void setAppTokensEndpoint(String appTokensEndpoint) {
        this.app_tokens_endpoint = appTokensEndpoint;
    }

    /**
     * @return the personalTokenMgmtEndpoint
     */
    public String getPersonalTokenMgmtEndpoint() {
        return personal_token_mgmt_endpoint;
    }

    /**
     * @param personalTokenMgmtEndpoint the personalTokenMgmtEndpoint to set
     */
    public void setPersonalTokenMgmtEndpoint(String personalTokenMgmtEndpoint) {
        this.personal_token_mgmt_endpoint = personalTokenMgmtEndpoint;
    }

    /**
     * @return the usersTokenMgmtEndpoint
     */
    public String getUsersTokenMgmtEndpoint() {
        return users_token_mgmt_endpoint;
    }

    /**
     * @param usersTokenMgmtEndpoint the usersTokenMgmtEndpoint to set
     */
    public void setUsersTokenMgmtEndpoint(String usersTokenMgmtEndpoint) {
        this.users_token_mgmt_endpoint = usersTokenMgmtEndpoint;
    }

    /**
     * @return the clientMgmtEndpoint
     */
    public String getClientMgmtEndpoint() {
        return client_mgmt_endpoint;
    }

    /**
     * @param clientMgmtEndpoint the clientMgmtEndpoint to set
     */
    public void setClientMgmtEndpoint(String clientMgmtEndpoint) {
        this.client_mgmt_endpoint = clientMgmtEndpoint;
    }

    /**
     * @return the pkceCodeChallengeMethodsSupported
     */
    public String[] getPkceCodeChallengeMethodsSupported() {
        return defensiveCopy(code_challenge_methods_supported);
    }

    /**
     * @param pkceCodeChallengeMethodsSupported the pkceCodeChallengeMethodsSupported to set
     */
    public void setPkceCodeChallengeMethodsSupported(String[] pkceCodeChallengeMethodsSupported) {
        this.code_challenge_methods_supported = defensiveCopy(pkceCodeChallengeMethodsSupported);
    }

    private String[] defensiveCopy(String[] strArr) {
        return Arrays.copyOf(strArr, strArr.length);
    }

    public String toJSONString() {
        return JsonTokenUtil.toJsonFromObj(this);
    }
}
