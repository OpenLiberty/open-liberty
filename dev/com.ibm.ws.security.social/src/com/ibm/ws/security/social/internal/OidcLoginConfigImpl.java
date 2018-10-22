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

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.osgi.service.cm.Configuration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.config.DiscoveryConfigUtils;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;

/**
 * This class was derived from GoogleLoginConfigImpl, it's purpose is to provide common superclass
 * and a metatype element for other OIDC based social services that will not use the Google metatype defaults.
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
    
    public static final String KEY_USERINFO_ENDPOINT = "userInfoEndpoint";
    private String userInfoEndpoint = null;
    public static final String KEY_USERINFO_ENDPOINT_ENABLED = "userInfoEndpointEnabled";
    private boolean userInfoEndpointEnabled = false;
    
    public static final String KEY_DISCOVERY_ENDPOINT = "discoveryEndpoint";
    private String discoveryEndpointUrl = null;
    private JSONObject discoveryjson = null;
    private boolean discovery = false;
           
    public static final String OPDISCOVERY_AUTHZ_EP_URL = "authorization_endpoint";
    public static final String OPDISCOVERY_TOKEN_EP_URL = "token_endpoint";
    public static final String OPDISCOVERY_INTROSPECTION_EP_URL = "introspection_endpoint";
    public static final String OPDISCOVERY_JWKS_EP_URL = "jwks_uri";
    public static final String OPDISCOVERY_USERINFO_EP_URL = "userinfo_endpoint";
    public static final String OPDISCOVERY_ISSUER = "issuer";
    public static final String OPDISCOVERY_TOKEN_EP_AUTH = "token_endpoint_auth_methods_supported";
    public static final String OPDISCOVERY_SCOPES = "scopes_supported";
    public static final String OPDISCOVERY_IDTOKEN_SIGN_ALG = "id_token_signing_alg_values_supported";
    
    
    public static final String KEY_JWK_CLIENT_ID = "jwkClientId";
    public static final String KEY_JWK_CLIENT_SECRET = "jwkClientSecret";
    private String jwkClientId = null;
    private String jwkClientSecret = null;

    public static final String KEY_RESPONSE_MODE = "responseMode";
    private String responseMode = null;

    public static final String KEY_NONCE_ENABLED = "nonceEnabled";

    public static final String KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT = "includeCustomCacheKeyInSubject";
    private boolean includeCustomCacheKeyInSubject = true;
    
    public static final String KEY_AUTHZ_PARAM = "authzParameter";
    public static final String KEY_TOKEN_PARAM = "tokenParameter";
    public static final String KEY_USERINFO_PARAM = "userinfoParameter";
    public static final String KEY_JWK_PARAM = "jwkParameter";
    public static final String KEY_PARAM_NAME = "name";
    public static final String KEY_PARAM_VALUE = "value";
    private HashMap<String, String> authzRequestParamMap;
    private HashMap<String, String> tokenRequestParamMap;
    private HashMap<String, String> userinfoRequestParamMap;
    private HashMap<String, String> jwkRequestParamMap;
    
    HttpUtils httputils = new HttpUtils();
    DiscoveryConfigUtils discoveryUtil;

    @Override
    protected void setRequiredConfigAttributes(Map<String, Object> props) {
        this.clientId = getRequiredConfigAttribute(props, KEY_clientId);
        this.clientSecret = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_clientSecret);
    }

    @Override
    protected void setOptionalConfigAttributes(Map<String, Object> props) throws SocialLoginException {
    	this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
    	this.discoveryEndpointUrl = configUtils.getConfigAttribute(props, KEY_DISCOVERY_ENDPOINT);
    	jwkClientId = configUtils.getConfigAttribute(props, KEY_JWK_CLIENT_ID);
        jwkClientSecret = configUtils.processProtectedString(props, KEY_JWK_CLIENT_SECRET);
        this.userInfoEndpointEnabled = configUtils.getBooleanConfigAttribute(props, KEY_USERINFO_ENDPOINT_ENABLED, this.userInfoEndpointEnabled);
    	if (discoveryEndpointUrl != null) {
    		discoveryUtil = new DiscoveryConfigUtils(getId());
            discovery = handleDiscoveryEndpoint(discoveryEndpointUrl);
        }
		if (!discovery) {
			this.userInfoEndpoint = configUtils.getConfigAttribute(props, KEY_USERINFO_ENDPOINT);		
			this.authorizationEndpoint = configUtils.getConfigAttribute(props, KEY_authorizationEndpoint);
			this.tokenEndpoint = configUtils.getConfigAttribute(props, KEY_tokenEndpoint);
			this.jwksUri = configUtils.getConfigAttribute(props, KEY_jwksUri);
			this.issuer = configUtils.getConfigAttribute(props, KEY_ISSUER);
		} else {
			discoveryUtil.logDiscoveryWarning(props);
		}
       
        this.scope = configUtils.getConfigAttribute(props, KEY_scope);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.mapToUserRegistry = configUtils.getBooleanConfigAttribute(props, KEY_mapToUserRegistry, this.mapToUserRegistry); 
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.trustAliasName = configUtils.getConfigAttribute(props, KEY_TRUSTED_ALIAS);
        this.isClientSideRedirectSupported = configUtils.getBooleanConfigAttribute(props, KEY_isClientSideRedirectSupported, this.isClientSideRedirectSupported);
        this.displayName = configUtils.getConfigAttribute(props, KEY_displayName);
        this.website = configUtils.getConfigAttribute(props, KEY_website);
        
        this.realmNameAttribute = configUtils.getConfigAttribute(props, KEY_realmNameAttribute);
        this.groupNameAttribute = configUtils.getConfigAttribute(props, KEY_groupNameAttribute);
        this.userUniqueIdAttribute = configUtils.getConfigAttribute(props, KEY_userUniqueIdAttribute);
        this.clockSkewMsec = configUtils.getIntegerConfigAttribute(props, KEY_CLOCKSKEW, this.clockSkewMsec);
        this.signatureAlgorithm = configUtils.getConfigAttribute(props, KEY_SIGNATURE_ALGORITHM);
        this.tokenEndpointAuthMethod = configUtils.getConfigAttribute(props, KEY_tokenEndpointAuthMethod);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
        this.hostNameVerificationEnabled = configUtils.getBooleanConfigAttribute(props, CFG_KEY_HOST_NAME_VERIFICATION_ENABLED, this.hostNameVerificationEnabled);
        this.responseType = configUtils.getConfigAttribute(props, KEY_responseType);
        this.responseMode = configUtils.getConfigAttribute(props, KEY_RESPONSE_MODE);
        this.nonce = configUtils.getBooleanConfigAttribute(props, KEY_NONCE_ENABLED, this.nonce);
        this.realmName = configUtils.getConfigAttribute(props, KEY_realmName);
        this.includeCustomCacheKeyInSubject = configUtils.getBooleanConfigAttribute(props, KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT, this.includeCustomCacheKeyInSubject);
        this.resource = configUtils.getConfigAttribute(props, KEY_resource);
        
        String[] authzReqParams = configUtils.trim((String[]) props.get(KEY_AUTHZ_PARAM));
        if (authzReqParams != null && authzReqParams.length > 0) {
            authzRequestParamMap = new HashMap<String, String>();
            authzRequestParamMap = handleCustomRequestParameters(authzRequestParamMap, authzReqParams);       
        }
        
        String[] tokenReqParams = configUtils.trim((String[]) props.get(KEY_TOKEN_PARAM));
        if (tokenReqParams != null && tokenReqParams.length > 0) {
            tokenRequestParamMap = new HashMap<String, String>();
            tokenRequestParamMap = handleCustomRequestParameters(tokenRequestParamMap, tokenReqParams);       
        }
        
        String[] userinfoReqParams = configUtils.trim((String[]) props.get(KEY_USERINFO_PARAM));
        if (userinfoReqParams != null && userinfoReqParams.length > 0) {
            userinfoRequestParamMap = new HashMap<String, String>();
            userinfoRequestParamMap = handleCustomRequestParameters(userinfoRequestParamMap, userinfoReqParams);       
        }
        
        String[] jwkReqParams = configUtils.trim((String[]) props.get(KEY_JWK_PARAM));
        if (jwkReqParams != null && jwkReqParams.length > 0) {
            jwkRequestParamMap = new HashMap<String, String>();
            jwkRequestParamMap = handleCustomRequestParameters(jwkRequestParamMap, jwkReqParams);       
        }
        
        if (discovery) {
        	DiscoveryConfigUtils discoveryConfigUtil = new DiscoveryConfigUtils(getId(), this.discoveryjson, this.signatureAlgorithm, this.tokenEndpointAuthMethod, this.scope);
            //adjustSignatureAlgorithm();
        	discoveryConfigUtil.adjustTokenEndpointAuthMethod();
        	discoveryConfigUtil.adjustScopes();
        }
        
    }
    
    boolean handleDiscoveryEndpoint(String discoveryUrl) {

        String jsonString = null;

        boolean valid = false;

        try {
        	if (!isValidDiscoveryUrl(discoveryUrl)) {
                Tr.error(tc,  "OIDC_CLIENT_DISCOVERY_SSL_ERROR", discoveryUrl);
                return false;
            }
        	
            SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
            jsonString = httputils.getHttpRequest(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled, null, null);  // do not need to add basic auth header       
            if (jsonString != null) {
                parseJsonResponse(jsonString);
                if (this.discoveryjson != null) {
                    valid = discoverEndpointUrls(this.discoveryjson);
                }
            }

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to get successful discovery response : ", e.getCause());
            }
        } 
      
        if (!valid) {
            Tr.error(tc,  "OIDC_CLIENT_DISCOVERY_SSL_ERROR", getId(), discoveryUrl);
        }
        return valid;
    }
    
    /**
     * @param json
     */
    boolean discoverEndpointUrls(JSONObject json) {
  
        this.authorizationEndpoint = discoveryUtil.discoverOPConfigSingleValue(json.get(OPDISCOVERY_AUTHZ_EP_URL));
        this.tokenEndpoint = discoveryUtil.discoverOPConfigSingleValue(json.get(OPDISCOVERY_TOKEN_EP_URL));
        this.jwksUri = discoveryUtil.discoverOPConfigSingleValue(json.get(OPDISCOVERY_JWKS_EP_URL));
        this.userInfoEndpoint = discoveryUtil.discoverOPConfigSingleValue(json.get(OPDISCOVERY_USERINFO_EP_URL));
        this.issuer = discoveryUtil.discoverOPConfigSingleValue(json.get(OPDISCOVERY_ISSUER));
        //handleValidationEndpoint(json);
        if (invalidEndpoints() || invalidIssuer()) {
            return false;
        }
        return true;
    }
    
    /**
     * @return
     */
    private boolean invalidIssuer() {
        return this.issuer == null;
    }

    /**
     * @return
     */
    private boolean invalidEndpoints() {
        //TODO check other information also and make sure that we have valid values
        return (this.authorizationEndpoint == null && this.tokenEndpoint == null);
    }   
       
    /**
     * @param jsonString
     * @return
     */
    protected void parseJsonResponse(String jsonString) {
        try {
            this.discoveryjson = JSONObject.parse(jsonString);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception parsing JSON string [" + jsonString + "]: " + e.getMessage());
            }
        }
    }

	/**
     * @param discoveryUrl
     * @return
     */
    private boolean isValidDiscoveryUrl(String discoveryUrl) {
        return discoveryUrl != null && discoveryUrl.startsWith("https");
    }

    @Override
    protected void initializeMembersAfterConfigAttributesPopulated(Map<String, Object> props) throws SocialLoginException {
        // OIDC configs do not use userApi, so this method overrides the version in Oauth2LoginConfigImpl to remove that step
        initializeJwt(props);
        resetLazyInitializedMembers();
        setGrantType();
    }

    @Override
    protected void resetLazyInitializedMembers() {
        super.resetLazyInitializedMembers();

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
            Tr.debug(tc, KEY_USERINFO_ENDPOINT + " = " + userInfoEndpoint);
            Tr.debug(tc, KEY_USERINFO_ENDPOINT_ENABLED + " = " + userInfoEndpointEnabled);
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
            Tr.debug(tc, KEY_responseType + " = " + responseType);
            Tr.debug(tc, KEY_RESPONSE_MODE + " = " + responseMode);
            Tr.debug(tc, KEY_realmName + " = " + realmName);
            Tr.debug(tc, KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT + " = " + includeCustomCacheKeyInSubject);
            Tr.debug(tc, KEY_resource + " = " + resource);
        }
    }
    
    @Override
    public boolean isUserInfoEnabled(){
        return this.userInfoEndpointEnabled;
    }
    
    @Override
    public String getUserInfoEndpointUrl(){
        return this.userInfoEndpoint;
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
    public String getResponseMode() {
        return responseMode;
    }

    public boolean includeCustomCacheKeyInSubject() {
        return includeCustomCacheKeyInSubject;
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
    
    /**
     * @param authzRequestParamMap2
     * @param authzReqParams 
     * @return 
     */
    private HashMap<String, String> handleCustomRequestParameters(HashMap<String, String> paramMap, String[] reqParams) {

        for (String reqParameter : reqParams) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "custom authz request param");
            }        
            
            Configuration config = getCustomConfiguration(reqParameter);
                        
            String paramName = null;
            String paramValue = null;
            if (config != null && config.getProperties() != null) {
                paramName = configUtils.trim((String) config.getProperties().get(KEY_PARAM_NAME));
                paramValue = configUtils.trim((String) config.getProperties().get(KEY_PARAM_VALUE));
                if (paramName != null && paramValue != null) {
                    paramMap.put(paramName,  paramValue);
                }
            }
        }
        return paramMap;
    }

	@Override
	public String getDiscoveryEndpointUrl() {
		return this.discoveryEndpointUrl;
	}

	@Override
	public HashMap<String, String> getAuthzRequestParams() {
		return this.authzRequestParamMap;
	}

	@Override
	public HashMap<String, String> getTokenRequestParams() {
		return this.tokenRequestParamMap;
	}

	@Override
	public HashMap<String, String> getUserinfoRequestParams() {
		return this.userinfoRequestParamMap;
	}

	@Override
	public HashMap<String, String> getJwkRequestParams() {
		return this.jwkRequestParamMap;
	}

}
