/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;

/**
 * This class was derived from GoogleLoginConfigImpl, it's purpose is to provide common superclass
 * and a metatype element for other OIDC based social services that will not use the Google metatypedefaults.
 *
 * It provides two services:
 * . One is for the oidcConfig which extends from the generic OAuth2LoginConfig
 * . The other is for JwtConsumerConfig. This make oidcLogin does not need to define an additional jJwtConsumerConfig
 * .. So, we can reuse the jwksUri and sslRef defined in the oidcLogin.
 */
@Component(name = "com.ibm.ws.security.social.oidclogin", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = { SocialLoginConfig.class, JwtConsumerConfig.class }, property = { "service.vendor=IBM", "type=oidcLogin" })
public class OidcLoginConfigImpl extends Oauth2LoginConfigImpl implements JwtConsumerConfig, ConvergedClientConfig {
    public static final TraceComponent tc = Tr.register(OidcLoginConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    ConsumerUtils consumerUtils = null; // lazy init

    JWKSet jwkSet = null; // lazy init. This makes sure one jwkSet per a jwtConsumerConfiguration

    public static final String KEY_ISSUER = "issuer";
    String issuer = null;

    public static final String KEY_SIGNATURE_ALGORITHM = "signatureAlgorithm";
    String signatureAlgorithm = null;

    public static final String KEY_CLOCKSKEW = "clockSkew";
    int clockSkewMsec = 0;

    public static final String CFG_KEY_HOST_NAME_VERIFICATION_ENABLED = "hostNameVerificationEnabled";
    private boolean hostNameVerificationEnabled = true;

    public static final String KEY_TRUSTED_ALIAS = "trustAliasName";
    private String trustAliasName = null;

    @Override
    protected void setRequiredConfigAttributes(Map<String, Object> props) {
        this.clientId = getRequiredConfigAttribute(props, KEY_clientId);
        this.clientSecret = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_clientSecret);
    }

    @Override
    protected void setOptionalConfigAttributes(Map<String, Object> props) throws SocialLoginException {
        this.authorizationEndpoint = configUtils.getConfigAttribute(props, KEY_authorizationEndpoint);
        this.tokenEndpoint = configUtils.getConfigAttribute(props, KEY_tokenEndpoint);
        this.jwksUri = configUtils.getConfigAttribute(props, KEY_jwksUri);
        this.scope = configUtils.getConfigAttribute(props, KEY_scope);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.mapToUserRegistry = configUtils.getBooleanConfigAttribute(props, KEY_mapToUserRegistry, this.mapToUserRegistry);
        this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.trustAliasName = configUtils.getConfigAttribute(props, KEY_TRUSTED_ALIAS);
        this.isClientSideRedirectSupported = configUtils.getBooleanConfigAttribute(props, KEY_isClientSideRedirectSupported, this.isClientSideRedirectSupported);
        this.displayName = configUtils.getConfigAttribute(props, KEY_displayName);
        this.website = configUtils.getConfigAttribute(props, KEY_website);
        this.issuer = configUtils.getConfigAttribute(props, KEY_ISSUER);
        this.realmNameAttribute = configUtils.getConfigAttribute(props, KEY_realmNameAttribute);
        this.groupNameAttribute = configUtils.getConfigAttribute(props, KEY_groupNameAttribute);
        this.userUniqueIdAttribute = configUtils.getConfigAttribute(props, KEY_userUniqueIdAttribute);
        this.clockSkewMsec = configUtils.getIntegerConfigAttribute(props, KEY_CLOCKSKEW, this.clockSkewMsec);
        this.signatureAlgorithm = configUtils.getConfigAttribute(props, KEY_SIGNATURE_ALGORITHM);
        this.tokenEndpointAuthMethod = configUtils.getConfigAttribute(props, KEY_tokenEndpointAuthMethod);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
        this.hostNameVerificationEnabled = configUtils.getBooleanConfigAttribute(props, CFG_KEY_HOST_NAME_VERIFICATION_ENABLED, this.hostNameVerificationEnabled);
        this.nonce = true;
    }

    @Override
    protected void initializeMembersAfterConfigAttributesPopulated(Map<String, Object> props) throws SocialLoginException {
        // OIDC configs do not use userApi, so this method overrides the version in Oauth2LoginConfigImpl to remove that step
        initializeJwt(props);
        resetLazyInitializedMembers();
    }

    @Override
    protected void resetLazyInitializedMembers() {
        super.resetLazyInitializedMembers();

        this.responseType = ClientConstants.CODE;
        this.jwkSet = null; // the jwkEndpoint may have been changed during dynamic update
        this.consumerUtils = null; // the parameters in consumerUtils may have been changed during dynamic changing
    }

    @Override
    protected void debug() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "" + this);
            Tr.debug(tc, KEY_clientId + " = " + clientId);
            Tr.debug(tc, KEY_clientSecret + " is null = " + (clientSecret == null));
            Tr.debug(tc, KEY_authorizationEndpoint + " = " + authorizationEndpoint);
            Tr.debug(tc, KEY_tokenEndpoint + " = " + tokenEndpoint);
            Tr.debug(tc, KEY_jwksUri + " = " + jwksUri);
            Tr.debug(tc, KEY_scope + " = " + scope);
            Tr.debug(tc, KEY_userNameAttribute + " = " + userNameAttribute);
            Tr.debug(tc, KEY_mapToUserRegistry + " = " + mapToUserRegistry);
            Tr.debug(tc, KEY_sslRef + " = " + sslRef);
            Tr.debug(tc, KEY_authFilterRef + " = " + authFilterRef);
            Tr.debug(tc, KEY_TRUSTED_ALIAS + " = " + trustAliasName);
            Tr.debug(tc, CFG_KEY_jwtRef + " = " + jwtRef);
            Tr.debug(tc, CFG_KEY_jwtClaims + " = " + ((jwtClaims == null) ? null : Arrays.toString(jwtClaims)));
            Tr.debug(tc, KEY_isClientSideRedirectSupported + " = " + isClientSideRedirectSupported);
            Tr.debug(tc, KEY_displayName + " = " + displayName);
            Tr.debug(tc, KEY_website + " = " + website);
            Tr.debug(tc, KEY_ISSUER + " = " + issuer);
            Tr.debug(tc, KEY_realmNameAttribute + " = " + realmNameAttribute);
            Tr.debug(tc, KEY_groupNameAttribute + " = " + groupNameAttribute);
            Tr.debug(tc, KEY_userUniqueIdAttribute + " = " + userUniqueIdAttribute);
            Tr.debug(tc, KEY_CLOCKSKEW + " = " + clockSkewMsec);
            Tr.debug(tc, KEY_SIGNATURE_ALGORITHM + " = " + signatureAlgorithm);
            Tr.debug(tc, KEY_tokenEndpointAuthMethod + " = " + tokenEndpointAuthMethod);
            Tr.debug(tc, KEY_redirectToRPHostAndPort + " = " + redirectToRPHostAndPort);
            Tr.debug(tc, CFG_KEY_HOST_NAME_VERIFICATION_ENABLED + " = " + hostNameVerificationEnabled);
            Tr.debug(tc, KEY_nonce + " = " + nonce);
        }
    }

    @Override
    public boolean isHostNameVerificationEnabled() {
        return this.hostNameVerificationEnabled;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmNameAttribute() {
        return this.realmNameAttribute;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return getUniqueId();
    }

    /** {@inheritDoc} */
    @Override
    public String getIssuer() {
        if (issuer == null || issuer.length() == 0) {
            // calculate it from the token endpoint, if we can.
            if (tokenEndpoint != null && tokenEndpoint.length() > "http://".length()) {
                String computedIssuer = null;
                if (tokenEndpoint.toLowerCase().startsWith("http")) {
                    int lastpos = tokenEndpoint.lastIndexOf("/");
                    if (lastpos > "http://".length()) {
                        //  if token endpoint is https://abc.com/123/token, issuer is https://abc.com/123
                        computedIssuer = tokenEndpoint.substring(0, lastpos);
                    } else {
                        // Token endpoint value has no other '/' characters after the URL scheme
                        computedIssuer = tokenEndpoint;
                    }
                    return computedIssuer;
                } else {
                    // Token endpoint must not be a valid HTTP or HTTPS URL, so return whatever the issuer was set to originally
                    return issuer;
                }
            }
        }
        // couldn't compute it, or didn't need to.
        return issuer;
    }

    /** {@inheritDoc} */
    @Override
    @Sensitive
    public String getSharedKey() {
        //return null;
        return clientSecret;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAudiences() { // TODO needed for verifying the ID_TOKEN
        List<String> audiences = new ArrayList<String>();
        String clientId = getClientId();
        if (clientId != null) {
            audiences.add(clientId);
        }
        return audiences;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidationRequired() { // TODO may need to be set from configuration
        // 241159 return jwksUri != null;
        return false; // oidc jose4jUtil always does validation, so no need to do it again in the social code.
    }

    /** {@inheritDoc} */
    @Override
    public String getSignatureAlgorithm() {
        return this.signatureAlgorithm;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(SocialLoginException.class)
    public String getTrustStoreRef() {
        if (this.sslRefInfo == null) {
            SocialLoginService service = socialLoginServiceRef.getService();
            if (service == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Social login service is not available");
                }
                return null;
            }
            sslRefInfo = createSslRefInfoImpl(service);
        }
        try {
            return sslRefInfo.getTrustStoreName();
        } catch (SocialLoginException e) {
            // TODO - NLS message?
            e.logErrorMessage();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTrustedAlias() {
        return trustAliasName;
    }

    /** {@inheritDoc} */
    @Override
    public long getClockSkew() {
        return this.clockSkewMsec;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getJwkEnabled() {
        return jwksUri != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getJwkEndpointUrl() {
        return jwksUri;
    }

    /** {@inheritDoc} */
    @Override
    public ConsumerUtils getConsumerUtils() {
        if (consumerUtils == null) { // lazy init
            SocialLoginService socialLoginService = socialLoginServiceRef.getService();
            if (socialLoginService != null) {
                consumerUtils = new ConsumerUtils(socialLoginService.getKeyStoreServiceRef());
            } else {
                Tr.warning(tc, "SERVICE_NOT_FOUND_JWT_CONSUMER_NOT_AVAILABLE", new Object[] { uniqueId });
            }
        }
        return consumerUtils;
    }

    /** {@inheritDoc} */
    @Override
    public JWKSet getJwkSet() {
        if (jwkSet == null) { // lazy initialization
            jwkSet = new JWKSet();
        }
        return jwkSet;
    }

    @Override
    public boolean getTokenReuse() {
        // The common JWT code is not allowed to reuse JWTs. This could be revisited later as a potential config option.
        return false;
    }

    @Override
    protected SslRefInfoImpl createSslRefInfoImpl(SocialLoginService socialLoginService) {
        return new SslRefInfoImpl(socialLoginService.getSslSupport(), socialLoginService.getKeyStoreServiceRef(), sslRef, trustAliasName);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSocial() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public OidcClientConfig getOidcClientConfig() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getInboundPropagation() {
        return "none";
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAccessTokenInLtpaCookie() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthnSessionDisabled_propagation() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getClockSkewInSeconds() {
        return getClockSkew() / 1000;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthorizationEndpointUrl() {
        return getAuthorizationEndpoint();
    }

    /** {@inheritDoc} */
    @Override
    public boolean createSession() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public long getAuthenticationTimeLimitInSeconds() {
        // TODO Auto-generated method stub
        return 420;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHttpsRequired() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isClientSideRedirect() {
        return isClientSideRedirectSupported();
    }

    /** {@inheritDoc} */
    @Override
    public String getContextPath() {
        return getContextRoot();
    }

    /** {@inheritDoc} */
    @Override
    public String getTokenEndpointUrl() {
        return getTokenEndpoint();
    }

    /** {@inheritDoc} */
    @Override
    public String getSSLConfigurationName() {
        return getSslRef();
    }

    /** {@inheritDoc} */
    @Override
    public String getRedirectUrlFromServerToClient() {
        // TODO Auto-generated method stub
        return getRedirectToRPHostAndPort();
    }

    /** {@inheritDoc} */
    @Override
    public String getRedirectUrlWithJunctionPath(String redirect_url) {
        return redirect_url;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthContextClassReference() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getGrantType() {
        // TODO Auto-generated method stub
        return "authorization_code";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNonceEnabled() {
        return createNonce();
    }

    /** {@inheritDoc} */
    @Override
    public String getPrompt() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getResources() {
        // TODO Auto-generated method stub
        String resource = getResource();
        if (resource == null) {
            return null;
        }
        return resource.split(" ");
    }

    /** {@inheritDoc} */
    @Override
    public String getOidcClientCookieName() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getIssuerIdentifier() {
        return getIssuer();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getUseAccessTokenAsIdToken() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMapIdentityToRegistryUser() {
        return getMapToUserRegistry();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIncludeCustomCacheKeyInSubject() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIncludeIdTokenInSubject() {
        // TODO Auto-generated method stub
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDisableLtpaCookie() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupIdentifier() {
        // TODO Auto-generated method stub
        return getGroupNameAttribute();
    }

    /** {@inheritDoc} */
    @Override
    public String getUserIdentifier() {
        // TODO Auto-generated method stub
        return getUserNameAttribute();
    }

    /** {@inheritDoc} */
    @Override
    public String getUserIdentityToCreateSubject() {
        // TODO Auto-generated method stub
        return getUserNameAttribute();
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmIdentifier() {
        // TODO Auto-generated method stub
        return getRealmNameAttribute();
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueUserIdentifier() {
        // TODO Auto-generated method stub
        return getUserUniqueIdAttribute();
    }

    /** {@inheritDoc} */
    @Override
    public String getJsonWebKey() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowedAllAudiences() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean disableIssChecking() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getJwkClientId() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getJwkClientSecret() {
        // TODO Auto-generated method stub
        return null;
    }

}
