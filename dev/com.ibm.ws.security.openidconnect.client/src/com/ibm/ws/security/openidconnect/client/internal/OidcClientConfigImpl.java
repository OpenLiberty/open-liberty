/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import java.io.IOException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.common.config.DiscoveryConfigUtils;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.HashUtils;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.ws.security.openidconnect.common.ConfigUtils;
import com.ibm.ws.security.openidconnect.common.OidcCommonClientRequest;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * Process the OpenID Connect client entry in the server.xml file
 */
@Component(configurationPid = "com.ibm.ws.security.openidconnect.client.oidcClientConfig", configurationPolicy = ConfigurationPolicy.REQUIRE, service = OidcClientConfig.class, property = { "service.vendor=IBM" })
public class OidcClientConfigImpl implements OidcClientConfig {

    private static final TraceComponent tc = Tr.register(OidcClientConfigImpl.class);

    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected final AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);

    public static final String CFG_KEY_ID = "id";
    public static final String CFG_KEY_GRANT_TYPE = "grantType";
    public static final String CFG_KEY_RESPONSE_TYPE = "responseType";
    public static final String CFG_KEY_SCOPE = "scope";
    public static final String CFG_KEY_CLIENT_ID = "clientId";
    public static final String CFG_KEY_CLIENT_SECRET = "clientSecret";
    public static final String CFG_KEY_REDIRECT_TO_RP_HOST_AND_PORT = "redirectToRPHostAndPort";
    public static final String CFG_KEY_USER_IDENTIFIER = "userIdentifier";
    public static final String CFG_KEY_INTROSPECTION_TOKEN_TYPE_HINT = "introspectionTokenTypeHint";
    public static final String CFG_KEY_GROUP_IDENTIFIER = "groupIdentifier";
    public static final String CFG_KEY_REALM_IDENTIFIER = "realmIdentifier";
    public static final String CFG_KEY_REALM_NAME = "realmName";
    public static final String CFG_KEY_UNIQUE_USER_IDENTIFIER = "uniqueUserIdentifier";
    public static final String CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD = "tokenEndpointAuthMethod";
    public static final String CFG_KEY_USER_IDENTITY_TO_CREATE_SUBJECT = "userIdentityToCreateSubject";
    public static final String CFG_KEY_MAP_IDENTITY_TO_REGISTRY_USER = "mapIdentityToRegistryUser";
    public static final String CFG_KEY_OidcclientRequestParameterSupported = "oidcclientRequestParameterSupported";
    public static final String CFG_KEY_VALIDATE_ACCESS_TOKEN_LOCALLY = "validateAccessTokenLocally";
    public static final String CFG_KEY_SHARED_KEY = "sharedKey";
    public static final String CFG_KEY_TRUST_ALIAS_NAME = "trustAliasName";
    public static final String CFG_KEY_HTTPS_REQUIRED = "httpsRequired";
    public static final String CFG_KEY_CLIENTSIDE_REDIRECT = "isClientSideRedirectSupported";
    public static final String CFG_KEY_disableLtpaCookie = "disableLtpaCookie";
    public static final String CFG_KEY_NONCE_ENABLED = "nonceEnabled";
    public static final String CFG_KEY_SSL_REF = "sslRef";
    public static final String CFG_KEY_SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String CFG_KEY_CLOCK_SKEW = "clockSkew";
    public static final String CFG_KEY_AUTHENTICATION_TIME_LIMIT = "authenticationTimeLimit";
    public static final String CFG_KEY_DISCOVERY_ENDPOINT_URL = "discoveryEndpointUrl";
    public static final String CFG_KEY_AUTHORIZATION_ENDPOINT_URL = "authorizationEndpointUrl";
    public static final String CFG_KEY_TOKEN_ENDPOINT_URL = "tokenEndpointUrl";
    public static final String CFG_KEY_USERINFO_ENDPOINT_URL = "userInfoEndpointUrl";
    public static final String CFG_KEY_VALIDATION_ENDPOINT_URL = "validationEndpointUrl";
    public static final String CFG_KEY_DISABLE_ISS_CHECKING = "disableIssChecking";
    public static final String CFG_KEY_INITIAL_STATE_CACHE_CAPACITY = "initialStateCacheCapacity";
    public static final String CFG_KEY_AUTO_AUTHORIZE_PARAM = "autoAuthorizeParam";
    public static final String CFG_KEY_ISSUER_IDENTIFIER = "issuerIdentifier";
    public static final String CFG_KEY_TRUSTSTORE_REF = "trustStoreRef";
    public static final String CFG_KEY_HOST_NAME_VERIFICATION_ENABLED = "hostNameVerificationEnabled";
    public static final String CFG_KEY_INCLUDE_ID_TOKEN_IN_SUBJECT = "includeIdTokenInSubject";
    public static final String CFG_KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT = "includeCustomCacheKeyInSubject";
    public static final String CFG_KEY_ALLOW_CUSTOM_CACHE_KEY = "allowCustomCacheKey";
    public static final String CFG_KEY_AUTH_CONTEXT_CLASS_REFERENCE = "authContextClassReference";
    public static final String CFG_KEY_AUTH_FILTER_REF = "authFilterRef";
    public static final String CFG_KEY_JSON_WEB_KEY = "jsonWebKey";
    public static final String CFG_KEY_JWK_ENDPOINT_URL = "jwkEndpointUrl";
    public static final String CFG_KEY_JWK_CLIENT_ID = "jwkClientId";
    public static final String CFG_KEY_JWK_CLIENT_SECRET = "jwkClientSecret";
    public static final String CFG_KEY_PROMPT = "prompt";
    public static final String CFG_KEY_AUDIENCES = "audiences";
    public static final String CFG_KEY_RESOURCES = "resource";
    public static final String CFG_KEY_CREATE_SESSION = "createSession";
    public static final String CFG_KEY_INBOUND_PROPAGATION = "inboundPropagation";
    public static final String CFG_KEY_VALIDATION_METHOD = "validationMethod";
    public static final String CFG_KEY_JWT_ACCESS_TOKEN_REMOTE_VALIDATION = "jwtAccessTokenRemoteValidation";
    public static final String CFG_KEY_HEADER_NAME = "headerName";
    public static final String CFG_KEY_propagation_authnSessionDisabled = "authnSessionDisabled";
    public static final String CFG_KEY_reAuthnOnAccessTokenExpire = "reAuthnOnAccessTokenExpire";
    public static final String CFG_KEY_reAuthnCushionMilliseconds = "reAuthnCushion";
    public static final String CFG_KEY_jwt = "jwt";
    public static final String CFG_KEY_jwtRef = "builder";
    public static final String CFG_KEY_jwtClaims = "claims";
    public static final String CFG_KEY_AUTHZ_PARAM = "authzParameter";
    public static final String CFG_KEY_TOKEN_PARAM = "tokenParameter";
    public static final String CFG_KEY_USERINFO_PARAM = "userinfoParameter";
    public static final String CFG_KEY_JWK_PARAM = "jwkParameter";
    public static final String CFG_KEY_PARAM_NAME = "name";
    public static final String CFG_KEY_PARAM_VALUE = "value";
    public static final String CFG_KEY_JUNCTION_PATH = "redirectJunctionPath";
    public static final String CFG_KEY_accessTokenInLtpaCookie = "accessTokenInLtpaCookie";
    public static final String CFG_KEY_USE_ACCESS_TOKEN_AS_ID_TOKEN = "useAccessTokenAsIdToken";
    public static final String CFG_KEY_USERINFO_ENDPOINT_ENABLED = "userInfoEndpointEnabled";
    public static final String CFG_KEY_DISCOVERY_POLLING_RATE = "discoveryPollingRate";
    public static final String CFG_KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS = "useSystemPropertiesForHttpClientConnections";
    public static final String CFG_KEY_FORWARD_LOGIN_PARAMETER = "forwardLoginParameter";
    public static final String CFG_KEY_REQUIRE_EXP_CLAIM = "requireExpClaimForIntrospection";
    public static final String CFG_KEY_REQUIRE_IAT_CLAIM = "requireIatClaimForIntrospection";
    public static final String CFG_KEY_KEY_MANAGEMENT_KEY_ALIAS = "keyManagementKeyAlias";
    public static final String CFG_KEY_ACCESS_TOKEN_CACHE_ENABLED = "accessTokenCacheEnabled";
    public static final String CFG_KEY_ACCESS_TOKEN_CACHE_TIMEOUT = "accessTokenCacheTimeout";

    public static final String OPDISCOVERY_AUTHZ_EP_URL = "authorization_endpoint";
    public static final String OPDISCOVERY_TOKEN_EP_URL = "token_endpoint";
    public static final String OPDISCOVERY_INTROSPECTION_EP_URL = "introspection_endpoint";
    public static final String OPDISCOVERY_JWKS_EP_URL = "jwks_uri";
    public static final String OPDISCOVERY_USERINFO_EP_URL = "userinfo_endpoint";
    public static final String OPDISCOVERY_ISSUER = "issuer";
    public static final String OPDISCOVERY_TOKEN_EP_AUTH = "token_endpoint_auth_methods_supported";
    public static final String OPDISCOVERY_SCOPES = "scopes_supported";
    public static final String OPDISCOVERY_IDTOKEN_SIGN_ALG = "id_token_signing_alg_values_supported";
    public static final String CFG_KEY_TOKEN_REUSE = "tokenReuse";

    static String contextPath = "/oidcclient";

    static final String COMMA = ",";
    static final String BLANK = "";

    public static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);
    public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);
    static final String KEY_LOCATION_ADMIN = "locationAdmin";
    static final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    private String id;
    private String grantType;
    private String responseType; // when the responseType is "code", we change
                                 // the getGrantType to "authorization_code"
    private String scope; // scopes separated by space
    private String clientId;
    private String clientSecret;
    private String redirectToRPHostAndPort;
    private String userIdentifier;
    private String introspectionTokenTypeHint;
    private String groupIdentifier;
    private String realmIdentifier;
    private String realmName;
    private String uniqueUserIdentifier;
    private String tokenEndpointAuthMethod;
    private String userIdentityToCreateSubject;
    private boolean mapIdentityToRegistryUser;
    private boolean oidcclientRequestParameterSupported;
    private boolean validateAccessTokenLocally;
    private boolean disableLtpaCookie = false; // default
    private String sharedKey;
    private String trustAliasName;
    private boolean httpsRequired;
    private boolean clientSideRedirect;
    private boolean nonceEnabled;
    private String sslRef;
    private String sslConfigurationName;
    private String signatureAlgorithm;
    private long clockSkew;
    private long clockSkewInSeconds;
    private long authenticationTimeLimitInSeconds;
    private String discoveryEndpointUrl;
    private String authorizationEndpointUrl;
    private String tokenEndpointUrl;
    private String userInfoEndpointUrl;
    private boolean userInfoEndpointEnabled;
    private String validationEndpointUrl;
    private int initialStateCacheCapacity;
    private String issuerIdentifier;
    private String trustStoreRef;
    private boolean hostNameVerificationEnabled;
    private boolean includeIdTokenInSubject;
    private boolean includeCustomCacheKeyInSubject;
    private boolean allowCustomCacheKey;
    private String authenticationContextClassReferenceValue; // acr_values separated by space
    private String authFilterRef;
    private String authFilterId;
    private String jsonWebKey;
    private String jwkEndpointUrl;
    private String jwkClientId;
    private String jwkClientSecret;
    private String jwtRef;
    private String[] jwtClaims;
    private JWKSet jwkset;
    private String prompt;
    private boolean createSession;
    private String inboundPropagation;
    private String validationMethod;
    private String jwtAccessTokenRemoteValidation;
    private String headerName;
    private boolean disableIssChecking;
    private String[] audiences;
    private boolean allAudiences = false;
    private String[] resources;
    private boolean useAccessTokenAsIdToken;
    private List<String> forwardLoginParameter;
    private boolean requireExpClaimForIntrospection = true;
    private boolean requireIatClaimForIntrospection = true;
    private String keyManagementKeyAlias;
    private boolean accessTokenCacheEnabled = true;
    private long accessTokenCacheTimeout = 1000 * 60 * 5;

    private String oidcClientCookieName;
    private boolean authnSessionDisabled;
    boolean goodConfig = true;

    private boolean reAuthnOnAccessTokenExpire;

    private long reAuthnCushionMilliseconds;

    private String redirectJunctionPath;

    private boolean accessTokenInLtpaCookie = false; // default
    private JSONObject discoveryjson = null;
    private String discoveryDocumentHash = null;
    private long discoveryPollingRate = 5 * 60 * 1000; // 5 minutes in milliseconds
    private long nextDiscoveryTime;
    private boolean discovery = false;

    private HashMap<String, String> authzRequestParamMap;
    private HashMap<String, String> tokenRequestParamMap;
    private HashMap<String, String> userinfoRequestParamMap;
    private HashMap<String, String> jwkRequestParamMap;

    private final CommonConfigUtils configUtils = new CommonConfigUtils();
    private final ConfigUtils oidcConfigUtils = new ConfigUtils(configAdminRef);
    private final DiscoveryConfigUtils discoveryUtils = new DiscoveryConfigUtils();
    private ConsumerUtils consumerUtils = null;

    private SingleTableCache cache = null;

    private boolean useSystemPropertiesForHttpClientConnections = false;
    private boolean tokenReuse = false;

    // see defect 218708
    static String firstRandom = OidcUtil.generateRandom(32);

    public OidcClientConfigImpl() {
    }

    @Reference(name = KEY_CONFIGURATION_ADMIN, service = ConfigurationAdmin.class, policy = ReferencePolicy.DYNAMIC)
    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.setReference(ref);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.unsetReference(ref);
    }

    @Reference(name = KEY_KEYSTORE_SERVICE, service = KeyStoreService.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.setReference(ref);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.unsetReference(ref);
    }

    @Reference(name = KEY_LOCATION_ADMIN, service = WsLocationAdmin.class)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    @Reference(service = SSLSupport.class, name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "updatedtSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.unsetReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        configAdminRef.activate(cc);
        sslSupportRef.activate(cc);
        keyStoreServiceRef.activate(cc);
        locationAdminRef.activate(cc);
        processConfigProps(props);
        if (isValidConfig())
            Tr.info(tc, "OIDC_CLIENT_CONFIG_PROCESSED", getId());
    }

    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        processConfigProps(props);
        if (isValidConfig())
            Tr.info(tc, "OIDC_CLIENT_CONFIG_MODIFIED", getId());
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {
        configAdminRef.deactivate(cc);
        sslSupportRef.deactivate(cc);
        keyStoreServiceRef.deactivate(cc);
        locationAdminRef.deactivate(cc);
        consumerUtils = null;
    }

    private void processConfigProps(Map<String, Object> props) {
        oidcClientCookieName = null; // reset the session variables

        if (props == null || props.isEmpty())
            return;
        id = (String) props.get(CFG_KEY_ID);
        grantType = (String) props.get(CFG_KEY_GRANT_TYPE);
        responseType = trimIt((String) props.get(CFG_KEY_RESPONSE_TYPE));
        // clarify the grantType and responseType
        if (responseType != null) {
            if (ClientConstants.CODE.equals(responseType)) {
                // when the responseType is "code", we change the getGrantType
                // to "authorization_code"
                grantType = ClientConstants.AUTHORIZATION_CODE;
            } else if (responseType.contains(ClientConstants.TOKEN)) {
                grantType = ClientConstants.IMPLICIT;
            }
        } else {
            if (ClientConstants.CODE.equals(grantType)) {
                responseType = ClientConstants.CODE; // "code"
            } else if (ClientConstants.IMPLICIT.equals(grantType)) {
                responseType = ClientConstants.ID_TOKEN_TOKEN; // "id_token token"
            }
        }
        useSystemPropertiesForHttpClientConnections = configUtils.getBooleanConfigAttribute(props, CFG_KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, useSystemPropertiesForHttpClientConnections);
        scope = (String) props.get(CFG_KEY_SCOPE);
        clientId = trimIt((String) props.get(CFG_KEY_CLIENT_ID));
        clientSecret = processProtectedString(props, CFG_KEY_CLIENT_SECRET);
        redirectToRPHostAndPort = trimIt((String) props.get(CFG_KEY_REDIRECT_TO_RP_HOST_AND_PORT));
        redirectJunctionPath = trimIt((String) props.get(CFG_KEY_JUNCTION_PATH));
        if (redirectJunctionPath != null) {
            if (!redirectJunctionPath.startsWith("/")) {
                redirectJunctionPath = "/" + redirectJunctionPath;
            }
            if (redirectJunctionPath.endsWith("/")) {
                redirectJunctionPath = redirectJunctionPath.substring(0, redirectJunctionPath.length() - 1);
            }
        }
        userIdentifier = trimIt((String) props.get(CFG_KEY_USER_IDENTIFIER));
        introspectionTokenTypeHint = trimIt((String) props.get(CFG_KEY_INTROSPECTION_TOKEN_TYPE_HINT));
        groupIdentifier = trimIt((String) props.get(CFG_KEY_GROUP_IDENTIFIER));
        realmIdentifier = trimIt((String) props.get(CFG_KEY_REALM_IDENTIFIER));
        realmName = trimIt((String) props.get(CFG_KEY_REALM_NAME));
        uniqueUserIdentifier = trimIt((String) props.get(CFG_KEY_UNIQUE_USER_IDENTIFIER));
        tokenEndpointAuthMethod = trimIt((String) props.get(CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD));

        userIdentityToCreateSubject = trimIt((String) props.get(CFG_KEY_USER_IDENTITY_TO_CREATE_SUBJECT));
        checkForValidValue(userIdentityToCreateSubject);
        mapIdentityToRegistryUser = (Boolean) props.get(CFG_KEY_MAP_IDENTITY_TO_REGISTRY_USER);
        oidcclientRequestParameterSupported = (Boolean) props.get(CFG_KEY_OidcclientRequestParameterSupported);
        validateAccessTokenLocally = (Boolean) props.get(CFG_KEY_VALIDATE_ACCESS_TOKEN_LOCALLY);
        disableLtpaCookie = (Boolean) props.get(CFG_KEY_disableLtpaCookie);
        sharedKey = processProtectedString(props, CFG_KEY_SHARED_KEY);// (String)
                                                                      // props.get(CFG_KEY_SHARED_KEY);
        trustAliasName = trimIt((String) props.get(CFG_KEY_TRUST_ALIAS_NAME));
        httpsRequired = (Boolean) props.get(CFG_KEY_HTTPS_REQUIRED);
        clientSideRedirect = (Boolean) props.get(CFG_KEY_CLIENTSIDE_REDIRECT);
        nonceEnabled = (Boolean) props.get(CFG_KEY_NONCE_ENABLED);
        sslRef = trimIt((String) props.get(CFG_KEY_SSL_REF));
        // sslConfigurationName = getSSLConfigurationName(sslRef);
        sslConfigurationName = sslRef;
        signatureAlgorithm = trimIt((String) props.get(CFG_KEY_SIGNATURE_ALGORITHM));
        if (ClientConstants.ALGORITHM_NONE.equals(signatureAlgorithm)) {
            // 220146
            Tr.warning(tc, "OIDC_CLIENT_NONE_ALG", new Object[] { id, signatureAlgorithm });
        }
        clockSkew = (Long) props.get(CFG_KEY_CLOCK_SKEW);
        clockSkewInSeconds = clockSkew / 1000; // Duration types are always in milliseconds, convert to seconds.
        authenticationTimeLimitInSeconds = (Long) props.get(CFG_KEY_AUTHENTICATION_TIME_LIMIT) / 1000;
        validationMethod = trimIt((String) props.get(CFG_KEY_VALIDATION_METHOD));
        jwtAccessTokenRemoteValidation = configUtils.getConfigAttribute(props, CFG_KEY_JWT_ACCESS_TOKEN_REMOTE_VALIDATION);
        userInfoEndpointEnabled = (Boolean) props.get(CFG_KEY_USERINFO_ENDPOINT_ENABLED);
        discoveryEndpointUrl = trimIt((String) props.get(CFG_KEY_DISCOVERY_ENDPOINT_URL));
        discoveryPollingRate = (Long) props.get(CFG_KEY_DISCOVERY_POLLING_RATE);
        discovery = false;
        discoveryjson = null;
        if (discoveryEndpointUrl != null) {
            discovery = handleDiscoveryEndpoint(discoveryEndpointUrl);
            if (discovery) {
                logDiscoveryWarning(props);
            } else {
                reConfigEndpointsAfterDiscoveryFailure();
            }
        } else {
            authorizationEndpointUrl = trimIt((String) props.get(CFG_KEY_AUTHORIZATION_ENDPOINT_URL));
            tokenEndpointUrl = trimIt((String) props.get(CFG_KEY_TOKEN_ENDPOINT_URL));
            userInfoEndpointUrl = trimIt((String) props.get(CFG_KEY_USERINFO_ENDPOINT_URL));
            jwkEndpointUrl = trimIt((String) props.get(CFG_KEY_JWK_ENDPOINT_URL));
            validationEndpointUrl = trimIt((String) props.get(CFG_KEY_VALIDATION_ENDPOINT_URL));
            issuerIdentifier = trimIt((String) props.get(CFG_KEY_ISSUER_IDENTIFIER));
        }

        initialStateCacheCapacity = (Integer) props.get(CFG_KEY_INITIAL_STATE_CACHE_CAPACITY);
        trustStoreRef = trimIt((String) props.get(CFG_KEY_TRUSTSTORE_REF));
        hostNameVerificationEnabled = (Boolean) props.get(CFG_KEY_HOST_NAME_VERIFICATION_ENABLED);
        includeIdTokenInSubject = (Boolean) props.get(CFG_KEY_INCLUDE_ID_TOKEN_IN_SUBJECT);
        includeCustomCacheKeyInSubject = (Boolean) props.get(CFG_KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT);
        allowCustomCacheKey = (Boolean) props.get(CFG_KEY_ALLOW_CUSTOM_CACHE_KEY);
        authenticationContextClassReferenceValue = trimIt((String) props.get(CFG_KEY_AUTH_CONTEXT_CLASS_REFERENCE));
        if (authenticationContextClassReferenceValue == null)
            authenticationContextClassReferenceValue = "";
        authFilterRef = trimIt((String) props.get(CFG_KEY_AUTH_FILTER_REF));
        authFilterId = getAuthFilterId(authFilterRef);
        jsonWebKey = trimIt((String) props.get(CFG_KEY_JSON_WEB_KEY));

        jwkClientId = trimIt((String) props.get(CFG_KEY_JWK_CLIENT_ID));
        jwkClientSecret = processProtectedString(props, CFG_KEY_JWK_CLIENT_SECRET);
        jwkset = new JWKSet();
        prompt = trimIt((String) props.get(CFG_KEY_PROMPT));
        createSession = (Boolean) props.get(CFG_KEY_CREATE_SESSION);
        inboundPropagation = trimIt((String) props.get(CFG_KEY_INBOUND_PROPAGATION));

        audiences = trimIt((String[]) props.get(CFG_KEY_AUDIENCES));
        allAudiences = false;
        if (audiences != null) {
            for (int iI = 0; iI < audiences.length; iI++) {
                if (OidcCommonClientRequest.ALL_AUDIENCES.equals(audiences[iI])) {
                    allAudiences = true;
                    break;
                }
            }
        }
        String jwt = trimIt((String) props.get(CFG_KEY_jwt));
        if (jwt != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "jwt element exists");
            }
            Configuration config = null;
            try {
                config = configAdminRef.getService().getConfiguration(jwt, "");
            } catch (IOException e) {

            }
            if (config != null && config.getProperties() != null) {
                jwtRef = trimIt((String) config.getProperties().get(CFG_KEY_jwtRef));
                jwtClaims = trimIt((String[]) config.getProperties().get(CFG_KEY_jwtClaims));
            }
        }

        authzRequestParamMap = populateCustomRequestParameterMap(props, CFG_KEY_AUTHZ_PARAM);
        tokenRequestParamMap = populateCustomRequestParameterMap(props, CFG_KEY_TOKEN_PARAM);
        userinfoRequestParamMap = populateCustomRequestParameterMap(props, CFG_KEY_USERINFO_PARAM);
        jwkRequestParamMap = populateCustomRequestParameterMap(props, CFG_KEY_JWK_PARAM);

        resources = trimIt((String[]) props.get(CFG_KEY_RESOURCES));
        headerName = trimIt((String) props.get(CFG_KEY_HEADER_NAME));
        authnSessionDisabled = (Boolean) props.get(CFG_KEY_propagation_authnSessionDisabled);
        reAuthnOnAccessTokenExpire = (Boolean) props.get(CFG_KEY_reAuthnOnAccessTokenExpire);
        reAuthnCushionMilliseconds = (Long) props.get(CFG_KEY_reAuthnCushionMilliseconds);
        disableIssChecking = (Boolean) props.get(CFG_KEY_DISABLE_ISS_CHECKING);
        goodConfig = true; // default, of course, true

        accessTokenInLtpaCookie = (Boolean) props.get(CFG_KEY_accessTokenInLtpaCookie);
        useAccessTokenAsIdToken = configUtils.getBooleanConfigAttribute(props, CFG_KEY_USE_ACCESS_TOKEN_AS_ID_TOKEN, useAccessTokenAsIdToken);
        tokenReuse = configUtils.getBooleanConfigAttribute(props, CFG_KEY_TOKEN_REUSE, tokenReuse);
        forwardLoginParameter = oidcConfigUtils.readAndSanitizeForwardLoginParameter(props, id, CFG_KEY_FORWARD_LOGIN_PARAMETER);
        requireExpClaimForIntrospection = configUtils.getBooleanConfigAttribute(props, CFG_KEY_REQUIRE_EXP_CLAIM, requireExpClaimForIntrospection);
        requireIatClaimForIntrospection = configUtils.getBooleanConfigAttribute(props, CFG_KEY_REQUIRE_IAT_CLAIM, requireIatClaimForIntrospection);
        keyManagementKeyAlias = configUtils.getConfigAttribute(props, CFG_KEY_KEY_MANAGEMENT_KEY_ALIAS);
        accessTokenCacheEnabled = configUtils.getBooleanConfigAttribute(props, CFG_KEY_ACCESS_TOKEN_CACHE_ENABLED, accessTokenCacheEnabled);
        accessTokenCacheTimeout = configUtils.getLongConfigAttribute(props, CFG_KEY_ACCESS_TOKEN_CACHE_TIMEOUT, accessTokenCacheTimeout);
        // TODO - 3Q16: Check the validationEndpointUrl to make sure it is valid
        // before continuing to process this config
        // checkValidationEndpointUrl();

        // validateAuthzTokenEndpoints(); //TODO: update tests to expect the error if the validation here fails

        if (discovery) {
            logDiscoveryMessage("OIDC_CLIENT_DISCOVERY_COMPLETE");
        }

        consumerUtils = new ConsumerUtils(keyStoreServiceRef);
        if (accessTokenCacheEnabled) {
            initializeAccessTokenCache();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id: " + id);
            Tr.debug(tc, "grantType: " + grantType);
            Tr.debug(tc, "responseType:" + responseType);
            Tr.debug(tc, "scope: " + scope);
            Tr.debug(tc, "clientId: " + clientId);
            Tr.debug(tc, "redirectToRPHostAndPort: " + redirectToRPHostAndPort);
            Tr.debug(tc, "userIdentifier: " + userIdentifier);
            Tr.debug(tc, "introspectionTokenTypeHint: " + introspectionTokenTypeHint);
            Tr.debug(tc, "groupIdentifier: " + groupIdentifier);
            Tr.debug(tc, "realmIdentifier: " + realmIdentifier);
            Tr.debug(tc, "realmName: " + realmName);
            Tr.debug(tc, "uniqueUserIdentifier: " + uniqueUserIdentifier);
            Tr.debug(tc, "tokenEndpointAuthMethod: " + tokenEndpointAuthMethod);
            Tr.debug(tc, "userIdentityToCreateSubject: " + userIdentityToCreateSubject);
            Tr.debug(tc, "mapIdentityToRegistryUser: " + mapIdentityToRegistryUser);
            Tr.debug(tc, "oidcclientRequestParameterSupported: " + oidcclientRequestParameterSupported);
            Tr.debug(tc, "validateAccessTokenLocally: " + validateAccessTokenLocally);
            Tr.debug(tc, "disableLtpaCookie:" + disableLtpaCookie);
            Tr.debug(tc, "trustAliasName: " + trustAliasName);
            Tr.debug(tc, "httpsRequired: " + httpsRequired);
            Tr.debug(tc, "isClientSideRedirectSupported: " + clientSideRedirect);
            Tr.debug(tc, "nonceEnabled: " + nonceEnabled);
            Tr.debug(tc, "sslRef: " + sslRef);
            Tr.debug(tc, "signatureAlgorithm: " + signatureAlgorithm);
            Tr.debug(tc, "clockSkew: " + clockSkewInSeconds);
            Tr.debug(tc, "discoveryEndpointUrl: " + discoveryEndpointUrl);
            Tr.debug(tc, "discoveryPollingRate: " + discoveryPollingRate);
            Tr.debug(tc, "authorizationEndpointUrl: " + authorizationEndpointUrl);
            Tr.debug(tc, "tokenEndpointUrl: " + tokenEndpointUrl);
            Tr.debug(tc, "userinfoEndpointUrl: " + userInfoEndpointUrl);
            Tr.debug(tc, "userInfoEndpointEnabled: " + userInfoEndpointEnabled);
            Tr.debug(tc, "validationEndpointUrl: " + validationEndpointUrl);
            Tr.debug(tc, "initialStateCacheCapacity: " + initialStateCacheCapacity);
            Tr.debug(tc, "issuerIdentifier: " + issuerIdentifier);
            Tr.debug(tc, "trustStoreRef: " + trustStoreRef);
            Tr.debug(tc, "hostNameVerificationEnabled: " + hostNameVerificationEnabled);
            Tr.debug(tc, "includeIdTokenInSubject: " + includeIdTokenInSubject);
            Tr.debug(tc, "includeCustomCacheKeyInSubject: " + includeCustomCacheKeyInSubject);
            Tr.debug(tc, "authContextClassReference: " + authenticationContextClassReferenceValue);
            Tr.debug(tc, "authFilterRef: " + authFilterRef);
            Tr.debug(tc, "authFilterId: " + authFilterId);
            Tr.debug(tc, "jsonWebKey: " + jsonWebKey);
            Tr.debug(tc, "jwkEndpointUrl: " + jwkEndpointUrl);
            Tr.debug(tc, "jwkClientIdentifier: " + jwkClientId);
            Tr.debug(tc, "prompt: " + prompt);
            Tr.debug(tc, "createSession: " + createSession);
            Tr.debug(tc, "inboundPropagation: " + inboundPropagation);
            Tr.debug(tc, "validationMethod: " + validationMethod);
            Tr.debug(tc, "jwtAccessTokenRemoteValidation: " + jwtAccessTokenRemoteValidation);
            Tr.debug(tc, "headerName: " + headerName);
            Tr.debug(tc, "authnSessionDisabled:" + authnSessionDisabled);
            Tr.debug(tc, "disableIssChecking:" + disableIssChecking);
            Tr.debug(tc, "jwt builder:" + jwtRef);
            Tr.debug(tc, "redirectJunctionPath:" + redirectJunctionPath);
            Tr.debug(tc, "accessTokenInLtpaCookie:" + accessTokenInLtpaCookie);
            Tr.debug(tc, "useAccessTokenAsIdToken:" + useAccessTokenAsIdToken);
            Tr.debug(tc, "tokenReuse:" + tokenReuse);
            Tr.debug(tc, "forwardLoginParameter:" + forwardLoginParameter);
            Tr.debug(tc, "accessTokenCacheEnabled:" + accessTokenCacheEnabled);
            Tr.debug(tc, "accessTokenCacheTimeout:" + accessTokenCacheTimeout);
        }
    }

    private void initializeAccessTokenCache() {
        if (cache == null) {
            cache = new SingleTableCache(500, accessTokenCacheTimeout);
        } else {
            cache.rescheduleCleanup(accessTokenCacheTimeout);
        }
    }

    private HashMap<String, String> populateCustomRequestParameterMap(Map<String, Object> configProps, String configAttributeName) {
        HashMap<String, String> customRequestParameterMap = new HashMap<String, String>();
        String[] customRequestParameterElements = configUtils.getStringArrayConfigAttribute(configProps, configAttributeName);
        if (customRequestParameterElements != null && customRequestParameterElements.length > 0) {
            populateCustomRequestParameterMap(customRequestParameterMap, customRequestParameterElements);
        }
        return customRequestParameterMap;
    }

    private void populateCustomRequestParameterMap(HashMap<String, String> paramMapToPopulate, String[] configuredCustomRequestParams) {
        ConfigurationAdmin configAdmin = configAdminRef.getService();
        if (configAdmin == null) {
            return;
        }
        oidcConfigUtils.populateCustomRequestParameterMap(configAdmin, paramMapToPopulate, configuredCustomRequestParams, CFG_KEY_PARAM_NAME, CFG_KEY_PARAM_VALUE);
    }

    private void validateAuthzTokenEndpoints() {
        if (this.tokenEndpointUrl == null) {
            logConfigError("CONFIG_REQUIRED_ATTRIBUTE_NULL", CFG_KEY_TOKEN_ENDPOINT_URL);
        }
        if (this.authorizationEndpointUrl == null && this.getGrantType() != ClientConstants.IMPLICIT) {
            logConfigError("CONFIG_REQUIRED_ATTRIBUTE_NULL", CFG_KEY_AUTHORIZATION_ENDPOINT_URL);
        }
    }

    /**
     * @param key
     * @param attrib
     */
    private void logConfigError(String key, String attrib) {
        Tr.error(tc, key, attrib);

    }

    /**
     *
     */
    private void reConfigEndpointsAfterDiscoveryFailure() {
        authorizationEndpointUrl = null;
        tokenEndpointUrl = null;
        userInfoEndpointUrl = null;
        jwkEndpointUrl = null;
        validationEndpointUrl = null;
        issuerIdentifier = null;
        this.discoveryDocumentHash = null;
    }

    /**
     * @param string
     */
    private void logDiscoveryMessage(String key) {
        Tr.info(tc, key, getId(), getDiscoveryEndpointUrl());
    }

    // @Override
    @Override
    public boolean getUseSystemPropertiesForHttpClientConnections() {
        return useSystemPropertiesForHttpClientConnections;
    }

    //@Override
    public boolean isDiscoveryInUse() {
        return isValidDiscoveryUrl(this.discoveryEndpointUrl);
    }

    /**
     * @param props
     */
    private void logDiscoveryWarning(Map<String, Object> props) {
        String endpoints = "";
        String ep = null;
        if ((ep = trimIt((String) props.get(CFG_KEY_AUTHORIZATION_ENDPOINT_URL))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, CFG_KEY_AUTHORIZATION_ENDPOINT_URL);
        }
        if ((ep = trimIt((String) props.get(CFG_KEY_TOKEN_ENDPOINT_URL))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, CFG_KEY_TOKEN_ENDPOINT_URL);
        }
        if ((ep = trimIt((String) props.get(CFG_KEY_USERINFO_ENDPOINT_URL))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, CFG_KEY_USERINFO_ENDPOINT_URL);
        }
        if ((ep = trimIt((String) props.get(CFG_KEY_JWK_ENDPOINT_URL))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, CFG_KEY_JWK_ENDPOINT_URL);
        }
        if ((ep = trimIt((String) props.get(CFG_KEY_VALIDATION_ENDPOINT_URL))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, CFG_KEY_VALIDATION_ENDPOINT_URL);
        }
        if (!endpoints.isEmpty()) {
            logWarning("OIDC_CLIENT_DISCOVERY_OVERRIDE_EP", endpoints);
        }

        if ((ep = trimIt((String) props.get(CFG_KEY_ISSUER_IDENTIFIER))) != null) {
            logWarning("OIDC_CLIENT_DISCOVERY_OVERRIDE_ISSUER", CFG_KEY_ISSUER_IDENTIFIER);
        }

    }

    /**
     * @param endpoints
     */
    private void logWarning(String key, String endpoints) {

        Tr.warning(tc, key, CFG_KEY_DISCOVERY_ENDPOINT_URL, endpoints, getId());

    }

    /**
     * @param endpoints
     * @param ep
     * @return
     */
    private String buildDiscoveryWarning(String endpoints, String ep) {
        return endpoints.concat(ep).concat(", ");
    }

    /**
     *
     */
    void adjustScopes() {
        ArrayList<String> discoveryScopes = discoveryUtils.discoverOPConfig(discoveryjson.get(OPDISCOVERY_SCOPES));
        if (isRPUsingDefault("scope") && !opHasRPDefault("scope", discoveryScopes)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "See if we need to adjusted the scopes. The original is : " + this.scope);
            }
            String supported = rpSupportsOPConfig("scope", discoveryScopes);
            if (supported != null) {
                Tr.info(tc, "OIDC_CLIENT_DISCOVERY_OVERRIDE_DEFAULT", this.scope, CFG_KEY_SCOPE, supported, getId());
                this.scope = supported;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The adjusted value is : " + this.scope);
                }
            }
        }
    }

    void adjustTokenEndpointAuthMethod() {
        ArrayList<String> discoveryTokenepAuthMethod = discoveryUtils.discoverOPConfig(discoveryjson.get(OPDISCOVERY_TOKEN_EP_AUTH));
        if (isRPUsingDefault("authMethod") && !opHasRPDefault("authMethod", discoveryTokenepAuthMethod)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "See if we need to adjusted the token endpoint authmethod. The original is : " + tokenEndpointAuthMethod);
            }
            String supported = rpSupportsOPConfig("authMethod", discoveryTokenepAuthMethod);
            if (supported != null) {
                Tr.info(tc, "OIDC_CLIENT_DISCOVERY_OVERRIDE_DEFAULT", this.tokenEndpointAuthMethod, CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD, supported, getId());
                this.tokenEndpointAuthMethod = supported;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The adjusted value is : " + tokenEndpointAuthMethod);
                }
            }
        }
    }

    void adjustSignatureAlgorithm() {

        ArrayList<String> discoverySigAlgorithm = discoveryUtils.discoverOPConfig(discoveryjson.get(OPDISCOVERY_IDTOKEN_SIGN_ALG));
        if (isRPUsingDefault("alg") && !opHasRPDefault("alg", discoverySigAlgorithm)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "See if we need to Adjust the signature algorithm. The original value is : " + signatureAlgorithm);
            }
            String supported = rpSupportsOPConfig("alg", discoverySigAlgorithm);
            if (supported != null) {
                Tr.info(tc, "OIDC_CLIENT_DISCOVERY_OVERRIDE_DEFAULT", this.signatureAlgorithm, CFG_KEY_SIGNATURE_ALGORITHM, supported, getId());
                this.signatureAlgorithm = supported;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The adjusted value is : " + signatureAlgorithm);
                }
            }
        }
    }

    /**
     * @param discoveryTokenepAuthMethod
     * @return
     */
    private String rpSupportsOPConfig(String key, ArrayList<String> values) {

        String rpSupportedSignatureAlgorithms = "HS256 RS256";
        String rpSupportedTokenEndpointAuthMethods = "post basic";
        String rpSupportedScopes = "openid profile";

        if ("alg".equals(key) && values != null) {
            for (String value : values) {
                if (rpSupportedSignatureAlgorithms.contains(value)) {
                    return value;
                }
            }
        }

        if ("authMethod".equals(key) && values != null) {
            for (String value : values) {
                value = matchingRPValue(value);
                if (rpSupportedTokenEndpointAuthMethods.contains(value)) {
                    return value;
                }
            }
        }

        if ("scope".equals(key) && values != null) {
            String scopes = null;
            for (String value : values) {
                if (rpSupportedScopes.contains(value)) {
                    if (scopes == null) {
                        scopes = value;
                    } else {
                        scopes = scopes + " " + value;
                    }
                }
            }
            return scopes;
        }
        return null;
    }

    /**
     * @param value
     * @return
     */
    private String matchingRPValue(String value) {
        if ("client_secret_post".equals(value)) {
            return "post";
        } else if ("client_secret_basic".equals(value)) {
            return "basic";
        }
        return value;
    }

    /**
     * @param string
     * @return
     */
    private boolean opHasRPDefault(String key, ArrayList<String> opconfig) {

        if ("authMethod".equals(key)) {
            return discoveryUtils.matches("client_secret_post", opconfig);
        } else if ("alg".equals(key)) {
            return discoveryUtils.matches("HS256", opconfig);
        } else if ("scope".equals(key)) {
            return discoveryUtils.matches("openid", opconfig) && discoveryUtils.matches("profile", opconfig);
        }
        return false;
    }

    /**
     * @param string
     * @return
     */
    private boolean isRPUsingDefault(String key) {
        if ("authMethod".equals(key)) {
            return discoveryUtils.matches("post", this.tokenEndpointAuthMethod);
        } else if ("alg".equals(key)) {
            return discoveryUtils.matches("HS256", this.signatureAlgorithm);
        } else if ("scope".equals(key)) {
            return (matchesMultipleValues("openid profile", this.scope));
        }
        return false;
    }

    /**
     * @param string
     * @param scope2
     * @return
     */
    private boolean matchesMultipleValues(String rpdefault, String rpconfig) {
        String[] configuredScope = rpconfig.split(" ");
        if (configuredScope.length != 2) {
            return false;
        }
        for (String scope : configuredScope) {
            if (!rpdefault.contains(scope)) {
                return false;
            }
        }
        return true;
    }

    @FFDCIgnore({ SSLException.class })
    public boolean handleDiscoveryEndpoint(String discoveryUrl) {

        String jsonString = null;

        boolean valid = false;

        if (!isValidDiscoveryUrl(discoveryUrl)) {
            Tr.error(tc, "OIDC_CLIENT_DISCOVERY_SSL_ERROR", getId(), discoveryUrl);
            return false;
        }
        try {
            setNextDiscoveryTime(); //
            SSLSocketFactory sslSocketFactory = getSSLSocketFactory(discoveryUrl, sslConfigurationName, sslSupportRef.getService());
            HttpClient client = createHTTPClient(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled);
            jsonString = getHTTPRequestAsString(client, discoveryUrl);
            if (jsonString != null) {
                parseJsonResponse(jsonString);
                if (this.discoveryjson != null) {
                    valid = discoverEndpointUrls(this.discoveryjson);
                }
            }

        } catch (SSLException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to get successful discovery response : ", e.getCause());
            }

        } catch (Exception e) {
            // could be ignored
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to get successful discovery response : ", e.getCause());
            }
        }

        if (!valid) {
            Tr.error(tc, "OIDC_CLIENT_DISCOVERY_SSL_ERROR", getId(), discoveryUrl);
        }
        return valid;
    }

    /**
     * @param discoveryUrl
     * @return
     */
    private boolean isValidDiscoveryUrl(String discoveryUrl) {
        return discoveryUrl != null && discoveryUrl.startsWith("https");
    }

    /**
     * @param json
     */
    boolean discoverEndpointUrls(JSONObject json) {

        if (calculateDiscoveryDocumentHash(json)) {
            this.authorizationEndpointUrl = discoveryUtils.discoverOPConfigSingleValue(json.get(OPDISCOVERY_AUTHZ_EP_URL));
            this.tokenEndpointUrl = discoveryUtils.discoverOPConfigSingleValue(json.get(OPDISCOVERY_TOKEN_EP_URL));
            this.jwkEndpointUrl = discoveryUtils.discoverOPConfigSingleValue(json.get(OPDISCOVERY_JWKS_EP_URL));
            this.userInfoEndpointUrl = discoveryUtils.discoverOPConfigSingleValue(json.get(OPDISCOVERY_USERINFO_EP_URL));
            this.issuerIdentifier = discoveryUtils.discoverOPConfigSingleValue(json.get(OPDISCOVERY_ISSUER));
            handleValidationEndpoint(json);
            if (invalidEndpoints() || invalidIssuer()) {
                return false;
            }
            adjustSignatureAlgorithm();
            adjustTokenEndpointAuthMethod();
            adjustScopes();
        }

        return true;
    }

    /**
     *
     */
    //@Override //TODO:
    public void setNextDiscoveryTime() {
        this.nextDiscoveryTime = System.currentTimeMillis() + discoveryPollingRate;
    }

    //@Override //TODO:
    public long getNextDiscoveryTime() {
        return this.nextDiscoveryTime;
    }

    /**
     * @param json
     */
    private boolean calculateDiscoveryDocumentHash(JSONObject json) {
        String latestDiscoveryHash = HashUtils.digest(json.toString());
        boolean updated = false;
        if (this.discoveryDocumentHash == null || !this.discoveryDocumentHash.equals(latestDiscoveryHash)) {
            if (this.discoveryDocumentHash != null) {
                logDiscoveryMessage("OIDC_CLIENT_DISCOVERY_UPDATED_CONFIG");
            }
            updated = true;
            this.discoveryDocumentHash = latestDiscoveryHash;
        } else {
            logDiscoveryMessage("OIDC_CLIENT_DISCOVERY_NOT_UPDATED_CONFIG");
        }
        return updated;
    }

    //@Override //TODO:
    public String getDiscoveryDocumentHash() {
        return this.discoveryDocumentHash;
    }

    /**
     * @return
     */
    private boolean invalidIssuer() {
        // TODO Auto-generated method stub
        return this.issuerIdentifier == null;
    }

    /**
     * @return
     */
    private boolean invalidEndpoints() {
        //TODO check other information also and make sure that we have valid values
        return (this.authorizationEndpointUrl == null && this.tokenEndpointUrl == null);
    }

    /**
     * @param json
     */
    private void handleValidationEndpoint(JSONObject json) {

        if (isIntrospectionValidation()) {
            this.validationEndpointUrl = discoveryUtils.discoverOPConfigSingleValue(json.get(OPDISCOVERY_INTROSPECTION_EP_URL));
        } else {
            this.validationEndpointUrl = discoveryUtils.discoverOPConfigSingleValue(json.get(OPDISCOVERY_USERINFO_EP_URL));
        }
    }

    /**
     * @return
     */
    private boolean isIntrospectionValidation() {
        return "introspect".equals(this.validationMethod);
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
                Tr.debug(tc, "Caught exception parsing JSON string [" + jsonString + "]: " + e);
            }
        }
    }

    @FFDCIgnore({ Exception.class })
    protected String getHTTPRequestAsString(HttpClient httpClient, String url) throws Exception {

        String json = null;
        try {
            HttpGet request = new HttpGet(url);
            request.addHeader("content-type", "application/json");
            HttpResponse result = null;
            try {
                result = httpClient.execute(request);
            } catch (IOException ioex) {
                logErrorMessage(url, 0, "IOException: " + ioex.getMessage() + " " + ioex.getCause());
                throw ioex;
            }
            StatusLine statusLine = result.getStatusLine();
            int iStatusCode = statusLine.getStatusCode();
            if (iStatusCode == 200) {
                json = EntityUtils.toString(result.getEntity(), "UTF-8");
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Response: ", json);
                }
                if (json == null || json.isEmpty()) { // NO json response returned
                    throw new Exception(logErrorMessage(url, iStatusCode, json));
                }
            } else {
                String errMsg = statusLine.getReasonPhrase();
                // String errMsg = EntityUtils.toString(result.getEntity(), "UTF-8");
                // error in getting the discovery response
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "status:" + iStatusCode + " errorMsg:" + errMsg);
                }
                throw new Exception(logErrorMessage(url, iStatusCode, errMsg));
            }
        } catch (Exception e) {
            throw e;
        }

        return json;
    }

    private String logErrorMessage(String url, int iStatusCode, String errMsg) {

        String defaultMessage = "Error processing discovery request";

        String message = TraceNLS.getFormattedMessage(getClass(),
                "com.ibm.ws.security.openidconnect.client.internal.resources.OidcClientMessages", "OIDC_CLIENT_DISC_RESPONSE_ERROR",
                new Object[] { url, Integer.valueOf(iStatusCode), errMsg }, defaultMessage);
        ;
        Tr.error(tc, message, new Object[0]);
        return message;
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification) {

        HttpClient client = null;
        boolean addBasicAuthHeader = false;

        //        if (jwkClientId != null && jwkClientSecret != null) {
        //            addBasicAuthHeader = true;
        //        }

        BasicCredentialsProvider credentialsProvider = null;
        if (addBasicAuthHeader) {
            credentialsProvider = createCredentialsProvider();
        }

        client = createHttpClient(url.startsWith("https:"), isHostnameVerification, sslSocketFactory, addBasicAuthHeader, credentialsProvider);
        return client;

    }

    private HttpClient createHttpClient(boolean isSecure, boolean isHostnameVerification, SSLSocketFactory sslSocketFactory, boolean addBasicAuthHeader, BasicCredentialsProvider credentialsProvider) {

        HttpClient client = null;
        if (isSecure) {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new NoopHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new DefaultHostnameVerifier());
            }
            if (addBasicAuthHeader) {
                client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).setSSLSocketFactory(connectionFactory).build();
            } else {
                client = HttpClientBuilder.create().setSSLSocketFactory(connectionFactory).build();
            }
        } else {
            if (addBasicAuthHeader) {
                client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
            } else {
                client = HttpClientBuilder.create().build();
            }
        }
        return client;
    }

    private BasicCredentialsProvider createCredentialsProvider() {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(jwkClientId, jwkClientSecret));
        return credentialsProvider;
    }

    @FFDCIgnore({ javax.net.ssl.SSLException.class })
    protected SSLSocketFactory getSSLSocketFactory(String requestUrl, String sslConfigurationName,
            SSLSupport sslSupport) throws SSLException {
        SSLSocketFactory sslSocketFactory = null;

        try {
            if (sslSupport != null) {
                sslSocketFactory = sslSupport.getSSLSocketFactory(sslConfigurationName);
            }

        } catch (javax.net.ssl.SSLException e) {
            throw new SSLException(e.getMessage());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sslSocketFactory (" + ") get: " + sslSocketFactory);
        }

        if (sslSocketFactory == null) {
            throw new SSLException(Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL",
                    new Object[] { "Null ssl socket factory", getId() }));
        }
        return sslSocketFactory;
    }

    /**
     * @param userIdentityToCreateSubject
     */
    private void checkForValidValue(String userIdentityToCreateSubject) {
        // TODO Auto-generated method stub
        // <AD id="userIdentityToCreateSubject"
        // name="%userIdentityToCreateSubject"
        // description="%userIdentityToCreateSubject.desc" required="true"
        // type="String" default="sub" />
        // Don't allow this attribute to have an empty or null since we have a
        // default value associated with it in the metatype.
        // Default to "sub" in this case
        if (userIdentityToCreateSubject == null || userIdentityToCreateSubject.isEmpty()) {
            this.userIdentityToCreateSubject = ClientConstants.SUB;
        }

    }

    /**
     * Verify that validationEndpointUrl is non-null, begins with "http", and
     * contains "/". If inboundPropagation="required" and the URL does not meet
     * the requirements, this config is considered bad. If
     * inboundPropagation="supported" and the URL does not meet the
     * requirements, set inboundPropagation to "none".
     */
    private void checkValidationEndpointUrl() {
        if (validationEndpointUrl == null || // it can not be null
                (!validationEndpointUrl.startsWith("http")) || // it has to
                                                               // starts http
                (validationEndpointUrl.indexOf("/") < 0)) { // no "/"

            // Inbound propagation requires a valid validationEndpointUrl;
            // either fall back to inboundPropagation="none" or consider this a
            // bad config
            if (ClientConstants.PROPAGATION_REQUIRED.equalsIgnoreCase(inboundPropagation)) {
                goodConfig = false;
                // BAD_INBOUND_PRPAGATION_REQUIRED=CWWKS1732E: The OpenID
                // Connect client [{0}] configuration is disabled because the
                // validationEndpointUrl [{1}] is not properly set and
                // inboundPropagation is "required".
                Tr.error(tc, "BAD_INBOUND_PRPAGATION_REQUIRED", getId(), validationEndpointUrl);
            } else if (ClientConstants.PROPAGATION_SUPPORTED.equalsIgnoreCase(inboundPropagation)) {
                // Behave as if inboundPropagation="none"
                inboundPropagation = ClientConstants.PROPAGATION_NONE;
                // BAD_INBOUND_PRPAGATION_SUPPORTED=CWWKS1733W: The
                // validationEndpointUrl [{0}] is not properly set, the OpenID
                // Connect client [{1}] will act as if its inboundPropagation is
                // "none".
                Tr.warning(tc, "BAD_INBOUND_PRPAGATION_SUPPORTED", validationEndpointUrl, getId());
            }
        }
    }

    // private String getSSLConfigurationName(String sslRef) {
    // String sslConfigurationName = null;
    // if (sslRef != null) {
    // Configuration config = null;
    // ConfigurationAdmin configAdmin = configAdminRef.getService();
    // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    // Tr.debug(tc, "ConfigurationAdmin: " + configAdmin);
    // }
    // if( configAdmin != null ){
    // try {
    // config = configAdmin.getConfiguration(sslRef, null);
    // Dictionary<String, Object> props = config.getProperties();
    // if (props != null) {
    // sslConfigurationName = (String) props.get(CFG_KEY_ID);
    // }
    // } catch (IOException e) {
    // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    // Tr.debug(tc, "Invalid sslRef configuration", e.getMessage());
    // }
    // }
    // }
    // }
    // return sslConfigurationName;
    // }

    @Sensitive
    private String processProtectedString(Map<String, Object> props, String cfgKey) {
        String secret;
        Object o = props.get(cfgKey);
        if (o != null) {
            if (o instanceof SerializableProtectedString) {
                secret = new String(((SerializableProtectedString) o).getChars());
            } else {
                secret = (String) o;
            }
        } else {
            secret = null;
        }
        // decode
        secret = PasswordUtil.passwordDecode(secret);
        return secret;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String getGrantType() {
        return grantType;
    }

    /** {@inheritDoc} */
    @Override
    public String getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    @Override
    public String getClientId() {
        return clientId;
    }

    /** {@inheritDoc} */
    @Override
    @Sensitive
    public String getClientSecret() {
        return clientSecret;
    }

    /** {@inheritDoc} */
    @Override
    public String getRedirectUrlFromServerToClient() {
        return new OIDCClientAuthenticatorUtil().getRedirectUrlFromServerToClient(getId(), getContextPath(), redirectToRPHostAndPort);
    }

    /** {@inheritDoc} */
    @Override
    public String getRedirectUrlWithJunctionPath(String redirectURL) {
        if (redirectJunctionPath != null && redirectJunctionPath.length() > 0
                && redirectURL != null && redirectURL.length() > 0) {
            // find first / after hostname, insert junction after that.
            int hostnameloc = redirectURL.indexOf("//");
            int pathBegin = redirectURL.indexOf("/", hostnameloc + 2);
            redirectURL = redirectURL.substring(0, pathBegin) + redirectJunctionPath +
                    redirectURL.substring(pathBegin);
        }
        return redirectURL;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmIdentifier() {
        return realmIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmName() {
        return realmName;
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueUserIdentifier() {
        return uniqueUserIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserIdentityToCreateSubject() {
        return userIdentityToCreateSubject;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMapIdentityToRegistryUser() {
        return mapIdentityToRegistryUser;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidateAccessTokenLocally() {
        return validateAccessTokenLocally;
    }

    /** {@inheritDoc} */
    @Override
    @Sensitive
    public String getSharedKey() {
        if (sharedKey != null)
            return sharedKey;
        else
            return clientSecret;
    }

    /** {@inheritDoc} */
    @Override
    public String getTrustAliasName() {
        return trustAliasName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHttpsRequired() {
        return httpsRequired;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClientSideRedirect() {
        return clientSideRedirect;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNonceEnabled() {
        return nonceEnabled;
    }

    /** {@inheritDoc} */
    @Override
    public String getSslRef() {
        return sslRef;
    }

    /** {@inheritDoc} */
    @Override
    public String getSSLConfigurationName() {
        // if( sslConfigurationName == null){
        // if( sslRef != null){
        // //sslConfigurationName = getSSLConfigurationName(sslRef);
        // return sslRef;
        // }
        // }
        return sslConfigurationName;
    }

    /** {@inheritDoc} */
    @Override
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /** {@inheritDoc} */
    @Override
    public long getClockSkewInSeconds() {
        return clockSkewInSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public long getAuthenticationTimeLimitInSeconds() {
        return authenticationTimeLimitInSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthorizationEndpointUrl() {
        return authorizationEndpointUrl;
    }

    /** {@inheritDoc} */
    @Override
    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    /** {@inheritDoc} */
    @Override
    public String getValidationEndpointUrl() {
        return validationEndpointUrl;
    }

    /** {@inheritDoc} */
    @Override
    public int getInitialStateCacheCapacity() {
        return initialStateCacheCapacity;
    }

    /** {@inheritDoc} */
    @Override
    public String getIssuerIdentifier() {
        return issuerIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public String getTrustStoreRef() {
        return trustStoreRef;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CertificateException
     * @throws KeyStoreException
     */
    @Override
    public PublicKey getPublicKey() throws KeyStoreException, CertificateException {
        KeyStoreService keyStoreService = keyStoreServiceRef.getService();
        return keyStoreService.getCertificateFromKeyStore(trustStoreRef, trustAliasName).getPublicKey();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHostNameVerificationEnabled() {
        return hostNameVerificationEnabled;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIncludeIdTokenInSubject() {
        return includeIdTokenInSubject;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIncludeCustomCacheKeyInSubject() {
        if (!includeCustomCacheKeyInSubject || !allowCustomCacheKey) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthContextClassReference() {
        return authenticationContextClassReferenceValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthFilterId() {
        return authFilterId;
    }

    private String getAuthFilterId(String authFilterRef) {
        if (authFilterRef == null || authFilterRef.isEmpty())
            return null;
        Configuration config = null;
        ConfigurationAdmin configAdmin = configAdminRef.getService();
        try {
            if (configAdmin != null)
                config = configAdmin.getConfiguration(authFilterRef, null);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid authFilterRef configuration", e);
            }
            return null;
        }
        if (config == null)
            return null;
        Dictionary<String, Object> props = config.getProperties();
        if (props == null)
            return null;
        String id = (String) props.get(CFG_KEY_ID);
        return id;
    }

    @Override
    public String getJwkEndpointUrl() {
        return this.jwkEndpointUrl;
    }

    @Override
    public JWKSet getJwkSet() {
        return this.jwkset;
    }

    @Override
    public String getJsonWebKey() {
        return this.jsonWebKey;
    }

    @Override
    public String getPrompt() {
        return this.prompt;
    }

    /** {@inheritDoc} */
    @Override
    public boolean createSession() {
        return createSession;
    }

    @Override
    public String getInboundPropagation() {
        return this.inboundPropagation;
    }

    @Override
    public String getValidationMethod() {
        return this.validationMethod;
    }

    @Override
    public String getJwtAccessTokenRemoteValidation() {
        return this.jwtAccessTokenRemoteValidation;
    }

    // This is either null or not_empty_string
    @Override
    public String getHeaderName() {
        return this.headerName;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserIdentifier() {
        return userIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public String getIntrospectionTokenTypeHint() {
        return introspectionTokenTypeHint;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isDisableLtpaCookie()
     */
    @Override
    public boolean isDisableLtpaCookie() {
        return disableLtpaCookie;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibm.ws.security.saml.SsoConfig#getSpCookieName(com.ibm.wsspi.kernel
     * .service.location.WsLocationAdmin)
     */
    @Override
    public String getOidcClientCookieName() {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        if (oidcClientCookieName == null || oidcClientCookieName.isEmpty()) {
            String cookieLongName = "";
            if (locationAdmin != null) {
                String usrLocation = locationAdmin.resolveString(ClientConstants.WLP_USER_DIR).replace('\\', '/');
                String slash = usrLocation.endsWith("/") ? "" : "/";
                // using the unique id instead of clientId
                cookieLongName = FileInfo.getHostName() + "_" + usrLocation + slash + "servers/" + locationAdmin.getServerName() + "/oidcclient/" + getId();
            } else {
                Tr.error(tc, "OSGI_SERVICE_ERROR", "WsLocationAdmin");
                cookieLongName = clientId;
            }
            oidcClientCookieName = ClientConstants.COOKIE_NAME_OIDC_CLIENT_PREFIX + hash(cookieLongName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cookieHashName: " + oidcClientCookieName + " cookieLongName: " + cookieLongName);
            }
        }
        return oidcClientCookieName;
    }

    public static String hash(String stringToEncrypt) {
        int hashCode = stringToEncrypt.hashCode();
        if (hashCode < 0) {
            hashCode = hashCode * -1;
            return "n" + hashCode;
        } else {
            return "p" + hashCode;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthnSessionDisabled_propagation() {
        return authnSessionDisabled;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidConfig() {
        return goodConfig;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReAuthnOnAccessTokenExpire() {
        return reAuthnOnAccessTokenExpire;
    }

    /** {@inheritDoc} */
    @Override
    public long getReAuthnCushion() {
        return reAuthnCushionMilliseconds;
    }

    /** {@inheritDoc} */
    @Override
    public boolean disableIssChecking() {
        return disableIssChecking;
    }

    String trimIt(String str) {
        if (str == null)
            return null;
        str = str.trim();
        if (str.isEmpty())
            return null;
        return str;
    }

    /**
     * @param strings
     * @return
     */
    String[] trimIt(String[] strings) {
        if (strings == null || strings.length == 0)
            return null;
        String[] results = new String[strings.length];
        int iCnt = 0;
        for (int iI = 0; iI < strings.length; iI++) {
            String result = trimIt(strings[iI]);
            if (result != null) {
                results[iCnt++] = result;
            }
        }
        if (iCnt == strings.length) {
            return results;
        } else if (iCnt > 0) {
            String[] newResults = new String[iCnt];
            System.arraycopy(results, 0, newResults, 0, iCnt);
            return newResults;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAudiences() {
        if (audiences != null) {
            List<String> audList = new ArrayList<String>();
            for (String aud : audiences) {
                audList.add(aud);
            }
            return audList;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowedAllAudiences() {
        return allAudiences;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getResources() {
        if (resources != null) {
            return resources.clone();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getResponseType() {
        return responseType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOidcclientRequestParameterSupported() {
        return oidcclientRequestParameterSupported;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    public static void setContextPath(String ctx) {
        contextPath = ctx;
    }

    @Override
    public String jwtRef() {
        return jwtRef;
    }

    @Override
    public String[] getJwtClaims() {
        if (jwtClaims != null) {
            return jwtClaims.clone();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getJwkClientId() {
        return jwkClientId;
    }

    /** {@inheritDoc} */
    @Override
    @Sensitive
    public String getJwkClientSecret() {
        return jwkClientSecret;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAccessTokenInLtpaCookie() {
        return accessTokenInLtpaCookie;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getTokenReuse() {
        return tokenReuse;
    }

    @Override
    public boolean getUseAccessTokenAsIdToken() {
        return useAccessTokenAsIdToken;
    }

    @Override
    public List<String> getForwardLoginParameter() {
        if (forwardLoginParameter != null) {
            return new ArrayList<String>(forwardLoginParameter);
        } else {
            return null;
        }
    }

    @Override
    public boolean isSocial() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public OidcClientConfig getOidcClientConfig() {
        return this;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("Id: " + id);
        sb.append(" clientId: " + clientId);
        sb.append(" grantType: " + grantType);
        sb.append(" responseType: " + responseType);
        sb.append(" scope: " + scope);
        sb.append(" redirectToRPHostAndPort: " + redirectToRPHostAndPort);
        sb.append(" issuerIdentifier: " + issuerIdentifier);
        sb.append(" tokenEndpointUrl: " + tokenEndpointUrl);
        sb.append(" userInfoEndpointUrl: " + userInfoEndpointUrl);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean isUserInfoEnabled() {
        return userInfoEndpointEnabled;
    }

    @Override
    public String getUserInfoEndpointUrl() {
        return userInfoEndpointUrl;
    }

    //@Override //TODO:
    @Override
    public String getDiscoveryEndpointUrl() {
        return discoveryEndpointUrl;
    }

    //@Override //TODO:
    @Override
    public HashMap<String, String> getAuthzRequestParams() {
        return authzRequestParamMap;
    }

    //@Override //TODO:
    @Override
    public HashMap<String, String> getTokenRequestParams() {
        return tokenRequestParamMap;
    }

    //@Override //TODO:
    @Override
    public HashMap<String, String> getUserinfoRequestParams() {
        return userinfoRequestParamMap;
    }

    //@Override //TODO:
    @Override
    public HashMap<String, String> getJwkRequestParams() {
        return jwkRequestParamMap;
    }

    @Override
    public boolean requireExpClaimForIntrospection() {
        return requireExpClaimForIntrospection;
    }

    @Override
    public boolean requireIatClaimForIntrospection() {
        return requireIatClaimForIntrospection;
    }

    @Override
    public String getKeyManagementKeyAlias() {
        return keyManagementKeyAlias;
    }

    @Override
    public boolean getAccessTokenCacheEnabled() {
        return accessTokenCacheEnabled;
    }

    @Override
    public long getAccessTokenCacheTimeout() {
        return accessTokenCacheTimeout;
    }

    @Override
    @Sensitive
    public Key getJweDecryptionKey() throws GeneralSecurityException {
        String keyAlias = getKeyManagementKeyAlias();
        if (keyAlias != null) {
            String keyStoreRef = getKeyStoreRef();
            return JwtUtils.getPrivateKey(keyAlias, keyStoreRef);
        }
        return null;
    }

    @Override
    public String getIssuer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean ignoreAudClaimIfNotConfigured() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getKeyStoreRef() {
        String keyStoreName = null;
        String sslRef = getSslRef();
        if (sslRef == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "sslRef not configured");
            }
            return null;
        }
        Properties sslConfigProps = getSslConfigProperties(sslRef);
        if (sslConfigProps != null) {
            keyStoreName = sslConfigProps.getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_KEY_STORE_NAME);
        }
        return keyStoreName;
    }

    @Trivial
    @FFDCIgnore(Exception.class)
    Properties getSslConfigProperties(String sslRef) {
        SSLSupport sslSupportService = sslSupportRef.getService();
        if (sslSupportService == null) {
            return null;
        }
        Properties sslConfigProps;
        try {
            final Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
            sslConfigProps = (Properties) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return sslSupportService.getJSSEHelper().getProperties(sslRef, connectionInfo, null, true);
                }
            });
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting SSL properties: " + e);
            }
            return null;
        }
        return sslConfigProps;
    }

    @Override
    public String getTrustedAlias() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getClockSkew() {
        return clockSkew;
    }

    @Override
    public boolean getJwkEnabled() {
        return false;
    }

    @Override
    public ConsumerUtils getConsumerUtils() {
        return consumerUtils;
    }

    @Override
    public boolean isValidationRequired() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> getAMRClaim() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SingleTableCache getCache() {
        return cache;
    }

}
