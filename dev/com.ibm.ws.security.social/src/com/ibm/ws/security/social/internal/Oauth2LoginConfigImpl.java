/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.SslRefInfo;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.Cache;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialHashUtils;
import com.ibm.ws.security.social.tai.SocialLoginTAI;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.ssl.SSLSupport;

@Component(name = "com.ibm.ws.security.social.oauth2login", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = SocialLoginConfig.class, property = { "service.vendor=IBM", "type=oauth2Login" })
public class Oauth2LoginConfigImpl implements SocialLoginConfig {
    public static final TraceComponent tc = Tr.register(Oauth2LoginConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected final boolean IS_REQUIRED = true;
    protected final boolean IS_NOT_REQUIRED = false;

    protected static final String KEY_UNIQUE_ID = "id";
    protected String uniqueId = null;
    protected Cache cache = null;

    public static final String KEY_clientId = "clientId";
    protected String clientId = null;

    public static final String KEY_clientSecret = "clientSecret";
    @Sensitive
    protected String clientSecret = null;

    public static final String KEY_displayName = "displayName";
    protected String displayName = null;

    public static final String KEY_website = "website";
    protected String website = null;

    public static final String KEY_authorizationEndpoint = "authorizationEndpoint";
    protected String authorizationEndpoint = null;

    public static final String KEY_tokenEndpoint = "tokenEndpoint";
    protected String tokenEndpoint = null;

    public static final String KEY_userApi = "userApi";
    protected String userApi = null;
    protected String[] userApis = null;

    public static final String KEY_authFilterRef = "authFilterRef";
    protected String authFilterRef;
    protected String authFilterId;
    protected AuthenticationFilter authFilter = null;
    protected SSLContext sslContext = null;
    protected SSLSocketFactory sslSocketFactory = null;

    public static final String KEY_sslRef = "sslRef";
    protected String sslRef;

    public static final String KEY_keyAliasName = "keyAliasName";
    protected String keyAliasName;

    protected String algorithm = "AES";

    public static final String KEY_scope = "scope";
    protected String scope = null;

    public static final String KEY_responseType = "responseType";
    protected String responseType = null;

    protected String grantType = null;

    public static final String KEY_nonce = "nonce";
    protected boolean nonce = false;

    public static final String KEY_resource = "resource";
    protected String resource = null;

    public static final String KEY_isClientSideRedirectSupported = "isClientSideRedirectSupported";
    protected boolean isClientSideRedirectSupported = true;

    public static final String KEY_tokenEndpointAuthMethod = "tokenEndpointAuthMethod";
    protected String tokenEndpointAuthMethod = null;

    public static final String KEY_userApiNeedsSpecialHeader = "userApiNeedsSpecialHeader";
    protected boolean userApiNeedsSpecialHeader = false;

    public static final String KEY_redirectToRPHostAndPort = "redirectToRPHostAndPort";
    protected String redirectToRPHostAndPort = null;

    protected UserApiConfig[] userApiConfigs = null;

    protected String userApiResponseIdentifier = null;

    protected SslRefInfo sslRefInfo = null;

    public static final String KEY_jwksUri = "jwksUri";
    protected String jwksUri = null;

    public static final String KEY_realmName = "realmName";
    protected String realmName = null;

    public static final String KEY_realmNameAttribute = "realmNameAttribute";
    protected String realmNameAttribute = null;

    public static final String KEY_userNameAttribute = "userNameAttribute";
    protected String userNameAttribute = null;

    public static final String KEY_groupNameAttribute = "groupNameAttribute";
    protected String groupNameAttribute = null;

    public static final String KEY_userUniqueIdAttribute = "userUniqueIdAttribute";
    protected String userUniqueIdAttribute = null;

    public static final String KEY_mapToUserRegistry = "mapToUserRegistry";
    protected boolean mapToUserRegistry = false;
    public static final String KEY_requestTokenUrl = "requestTokenUrl";
    protected String requestTokenUrl = null;

    public static final String CFG_KEY_jwt = "jwt";
    public static final String CFG_KEY_jwtRef = "builder";
    public static final String CFG_KEY_jwtClaims = "claims";
    protected String jwtRef = null;
    protected String[] jwtClaims;
    public static final String DEFAULT_JWT_BUILDER = "defaultJWT";

    static final String KEY_SOCIAL_LOGIN_SERVICE = "socialLoginService";

    public static final String DEFAULT_CONTEXT_ROOT = "/ibm/api/social-login";
    static String contextRoot = DEFAULT_CONTEXT_ROOT;

    public static final String KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS = "useSystemPropertiesForHttpClientConnections";
    protected boolean useSystemPropertiesForHttpClientConnections = false;

    // OpenShift-specific configuration
    public static final String KEY_k8sTokenReviewEndpoint = "k8sTokenReviewEndpoint";
    protected String k8sTokenReviewEndpoint = null;
    public static final String KEY_serviceAccountTokenForK8sTokenreview = "serviceAccountTokenForK8sTokenreview";
    protected String serviceAccountTokenForK8sTokenreview = null;
    public static final String KEY_useAccessTokenFromRequest = "useAccessTokenFromRequest";
    protected String useAccessTokenFromRequest = null;
    public static final String KEY_tokenHeaderName = "tokenHeaderName";
    protected String tokenHeaderName = null;

    protected CommonConfigUtils configUtils = new CommonConfigUtils();

    final AtomicServiceReference<SocialLoginService> socialLoginServiceRef = new AtomicServiceReference<SocialLoginService>(KEY_SOCIAL_LOGIN_SERVICE);

    private String bundleLocation;

    @Reference(service = SocialLoginService.class, name = KEY_SOCIAL_LOGIN_SERVICE, cardinality = ReferenceCardinality.MANDATORY)
    protected void setSocialLoginService(ServiceReference<SocialLoginService> ref) {
        this.socialLoginServiceRef.setReference(ref);
    }

    public static String getContextRoot() {
        return contextRoot;
    }

    // called by SocialLoginWebappConfigImpl to set context root from server.xml
    public static void setContextRoot(String ctx) {
        contextRoot = ctx;
    }

    protected void unsetSocialLoginService(ServiceReference<SocialLoginService> ref) {
        this.socialLoginServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) throws SocialLoginException {
        this.socialLoginServiceRef.activate(cc);
        this.bundleLocation = cc.getBundleContext().getBundle().getLocation();
        uniqueId = configUtils.getConfigAttribute(props, KEY_UNIQUE_ID);
        initProps(cc, props);

        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_PROCESSED", uniqueId);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) throws SocialLoginException {
        initProps(cc, props);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        this.socialLoginServiceRef.deactivate(cc);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_DEACTIVATED", uniqueId);
    }

    public void initProps(ComponentContext cc, Map<String, Object> props) throws SocialLoginException {
        setRequiredConfigAttributes(props);
        setOptionalConfigAttributes(props);
        initializeMembersAfterConfigAttributesPopulated(props);
        debug();
    }

    protected void setRequiredConfigAttributes(Map<String, Object> props) {
        this.clientId = getRequiredConfigAttribute(props, KEY_clientId);
        this.clientSecret = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_clientSecret);
        this.authorizationEndpoint = getRequiredConfigAttribute(props, KEY_authorizationEndpoint);
        this.scope = getRequiredConfigAttribute(props, KEY_scope);
    }

    protected void setOptionalConfigAttributes(Map<String, Object> props) throws SocialLoginException {
        this.useSystemPropertiesForHttpClientConnections = configUtils.getBooleanConfigAttribute(props, KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, false);
        this.displayName = configUtils.getConfigAttribute(props, KEY_displayName);
        this.website = configUtils.getConfigAttribute(props, KEY_website);
        this.tokenEndpoint = configUtils.getConfigAttribute(props, KEY_tokenEndpoint);
        this.jwksUri = configUtils.getConfigAttribute(props, KEY_jwksUri);
        this.responseType = configUtils.getConfigAttributeWithDefaultValue(props, KEY_responseType, ClientConstants.CODE);
        this.tokenEndpointAuthMethod = configUtils.getConfigAttributeWithDefaultValue(props, KEY_tokenEndpointAuthMethod, ClientConstants.METHOD_client_secret_post);
        this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.userApi = configUtils.getConfigAttribute(props, KEY_userApi);
        this.realmName = configUtils.getConfigAttribute(props, KEY_realmName);
        this.realmNameAttribute = configUtils.getConfigAttribute(props, KEY_realmNameAttribute);
        this.groupNameAttribute = configUtils.getConfigAttribute(props, KEY_groupNameAttribute);
        this.userUniqueIdAttribute = configUtils.getConfigAttribute(props, KEY_userUniqueIdAttribute);
        this.mapToUserRegistry = configUtils.getBooleanConfigAttribute(props, KEY_mapToUserRegistry, this.mapToUserRegistry);
        this.isClientSideRedirectSupported = configUtils.getBooleanConfigAttribute(props, KEY_isClientSideRedirectSupported, this.isClientSideRedirectSupported);
        this.nonce = configUtils.getBooleanConfigAttribute(props, KEY_nonce, this.nonce);
        this.userApiNeedsSpecialHeader = configUtils.getBooleanConfigAttribute(props, KEY_userApiNeedsSpecialHeader, this.userApiNeedsSpecialHeader);

        this.k8sTokenReviewEndpoint = configUtils.getConfigAttribute(props, KEY_k8sTokenReviewEndpoint);
        if (k8sTokenReviewEndpoint != null && !k8sTokenReviewEndpoint.isEmpty()) {
            // A service account token is required if the Kubernetes TokenReview API will be used to retrieve user info
            this.serviceAccountTokenForK8sTokenreview = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_serviceAccountTokenForK8sTokenreview);
            this.userApi = k8sTokenReviewEndpoint;
            //            this.userNameAttribute = "username";
            //            this.groupNameAttribute = "groups";
        } else {
            this.serviceAccountTokenForK8sTokenreview = configUtils.processProtectedString(props, KEY_serviceAccountTokenForK8sTokenreview);
        }
        this.useAccessTokenFromRequest = configUtils.getConfigAttribute(props, KEY_useAccessTokenFromRequest);
        this.tokenHeaderName = configUtils.getConfigAttribute(props, KEY_tokenHeaderName);
    }

    protected void initializeMembersAfterConfigAttributesPopulated(Map<String, Object> props) throws SocialLoginException {
        initializeUserApiConfigs();
        initializeJwt(props);
        resetLazyInitializedMembers();
        setGrantType();
    }

    protected void initializeUserApiConfigs() throws SocialLoginException {
        this.userApiConfigs = initUserApiConfigs(this.userApi);
    }

    protected Configuration getCustomConfiguration(String customParam) {
        if (this.socialLoginServiceRef.getService() != null) {
            try {
                return this.socialLoginServiceRef.getService().getConfigAdmin().getConfiguration(customParam, "");
            } catch (IOException e) {
            }
        }
        return null;
    }

    protected void initializeJwt(Map<String, Object> props) {
        Configuration jwtConfig = null;
        if (this.socialLoginServiceRef.getService() != null) {
            jwtConfig = handleJwtElement(props, this.socialLoginServiceRef.getService().getConfigAdmin());
        }
        if (jwtConfig != null) {
            Dictionary<String, Object> jwtProps = jwtConfig.getProperties();
            if (jwtProps != null) {
                this.jwtRef = CommonConfigUtils.trim((String) jwtProps.get(CFG_KEY_jwtRef));
                this.jwtClaims = CommonConfigUtils.trim((String[]) jwtProps.get(CFG_KEY_jwtClaims));
            }
        }
    }

    protected Configuration handleJwtElement(Map<String, Object> props, ConfigurationAdmin configurationAdmin) {
        String jwt = configUtils.getConfigAttribute(props, CFG_KEY_jwt);
        Configuration config = null;
        if (jwt != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "jwt element exists");
            }
            try {
                if (configurationAdmin != null) {
                    config = configurationAdmin.getConfiguration(jwt, bundleLocation);
                }
            } catch (IOException e) {

            }
        }
        return config;
    }

    protected void resetLazyInitializedMembers() {
        // Lazy re-initialize of variables
        this.userApis = null;
        this.sslRefInfo = null;
        this.authFilter = null;
        this.sslContext = null;
        this.sslSocketFactory = null;
    }

    protected void setGrantType() {
        grantType = ClientConstants.AUTHORIZATION_CODE;
        if (responseType != null && responseType.contains(ClientConstants.TOKEN)) {
            grantType = ClientConstants.IMPLICIT;
        }
    }

    protected String getRequiredConfigAttribute(Map<String, Object> props, String key) {
        String value = configUtils.getConfigAttribute(props, key);
        if (value == null) {
            logErrorForMissingRequiredAttribute(key);
        }
        return value;
    }

    @Sensitive
    protected String getRequiredSerializableProtectedStringConfigAttribute(Map<String, Object> props, String key) {
        String result = SocialHashUtils.decodeString((SerializableProtectedString) props.get(key));
        if (result == null) {
            logErrorForMissingRequiredAttribute(key);
        }
        return result;
    }

    void logErrorForMissingRequiredAttribute(String key) {
        Tr.error(tc, "CONFIG_REQUIRED_ATTRIBUTE_NULL", new Object[] { key, uniqueId });
    }

    /**
     */
    protected String defaultJwtBuilder() {
        return DEFAULT_JWT_BUILDER;
    }

    protected void debug() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "" + this);
            Tr.debug(tc, KEY_clientId + " = " + clientId);
            Tr.debug(tc, KEY_clientSecret + " is null = " + (clientSecret == null));
            Tr.debug(tc, KEY_displayName + " = " + displayName);
            Tr.debug(tc, KEY_website + " = " + website);
            Tr.debug(tc, KEY_authorizationEndpoint + " = " + authorizationEndpoint);
            Tr.debug(tc, KEY_tokenEndpoint + " = " + tokenEndpoint);
            Tr.debug(tc, KEY_jwksUri + " = " + jwksUri);
            Tr.debug(tc, KEY_responseType + " = " + responseType);
            Tr.debug(tc, KEY_tokenEndpointAuthMethod + " = " + tokenEndpointAuthMethod);
            Tr.debug(tc, KEY_sslRef + " = " + sslRef);
            Tr.debug(tc, KEY_scope + " = " + scope);
            Tr.debug(tc, KEY_authFilterRef + " = " + authFilterRef);
            Tr.debug(tc, KEY_redirectToRPHostAndPort + " = " + redirectToRPHostAndPort);
            Tr.debug(tc, KEY_userNameAttribute + " = " + userNameAttribute);
            Tr.debug(tc, KEY_userApi + " = " + userApi);
            Tr.debug(tc, "userApiConfigs = " + (userApiConfigs == null ? "null" : userApiConfigs.length));
            Tr.debug(tc, KEY_realmName + " = " + realmName);
            Tr.debug(tc, KEY_realmNameAttribute + " = " + realmNameAttribute);
            Tr.debug(tc, KEY_groupNameAttribute + " = " + groupNameAttribute);
            Tr.debug(tc, KEY_userUniqueIdAttribute + " = " + userUniqueIdAttribute);
            Tr.debug(tc, KEY_mapToUserRegistry + " = " + mapToUserRegistry);
            Tr.debug(tc, CFG_KEY_jwtRef + " = " + jwtRef);
            Tr.debug(tc, CFG_KEY_jwtClaims + " = " + ((jwtClaims == null) ? null : Arrays.toString(jwtClaims)));
            Tr.debug(tc, KEY_isClientSideRedirectSupported + " = " + isClientSideRedirectSupported);
            Tr.debug(tc, KEY_nonce + " = " + nonce);
            Tr.debug(tc, KEY_userApiNeedsSpecialHeader + " = " + userApiNeedsSpecialHeader);
        }
    }

    /**
     * @param userApis
     * @return
     * @throws SocialLoginException
     */
    UserApiConfig[] initUserApiConfigs(String userApi) throws SocialLoginException {
        if (userApi != null) {
            UserApiConfig[] results = new UserApiConfig[1];
            results[0] = new UserApiConfigImpl(userApi);
            return results;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public AuthenticationFilter getAuthFilter() {
        if (this.authFilter == null) {
            this.authFilter = SocialLoginTAI.getAuthFilter(this.authFilterRef);
        }
        return this.authFilter;
    }

    /** {@inheritDoc} */
    @Override
    public String getClientId() {
        return this.clientId;
    }

    /** {@inheritDoc} */
    @Override
    @Sensitive
    public String getClientSecret() {
        return this.clientSecret;
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    /** {@inheritDoc} */
    @Override
    public String getWebsite() {
        return this.website;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthorizationEndpoint() {
        return this.authorizationEndpoint;
    }

    /** {@inheritDoc} */
    @Override
    public String getTokenEndpoint() {
        return this.tokenEndpoint;
    }

    /** {@inheritDoc} */
    @Override
    public UserApiConfig[] getUserApis() {
        if (this.userApiConfigs == null) {
            return null;
        }
        return this.userApiConfigs.clone();
    }

    /**
     * @param userApis
     * @return
     * @throws SocialLoginException
     */
    @Override
    public String getUserApi() {
        return this.userApi;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibm.ws.security.socialmedia.SocialLoginService#getSocialLoginCookieCache
     * (java.lang.String)
     */
    @Override
    public Cache getSocialLoginCookieCache() {
        if (cache == null) {
            cache = new Cache(0, 0);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "socialLoginCockieCache cache:" + cache);
        }
        return cache;
    }

    /** {@inheritDoc} */
    @Override
    public String getSslRef() {
        return this.sslRef;
    }

    @Override
    public String getScope() {
        return this.scope;
    }

    @Override
    public String getResponseType() {
        return this.responseType;
    }

    @Override
    public String getGrantType() {
        return this.grantType;
    }

    @Override
    public boolean createNonce() {
        return this.nonce;
    }

    @Override
    public String getResource() {
        return this.resource;
    }

    @Override
    public boolean isClientSideRedirectSupported() {
        return isClientSideRedirectSupported;
    }

    @Override
    public String getTokenEndpointAuthMethod() {
        return this.tokenEndpointAuthMethod;
    }

    @Override
    public String getRedirectToRPHostAndPort() {
        return this.redirectToRPHostAndPort;
    }

    @Override
    public HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException {
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
        return sslRefInfo.getPublicKeys();
    }

    @Override
    public SSLContext getSSLContext() throws SocialLoginException {
        if (this.sslContext == null) {
            SocialLoginService service = socialLoginServiceRef.getService();
            if (service == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Social login service is not available");
                }
                return null;
            }
            SSLSupport sslSupport = service.getSslSupport();
            if (sslSupport == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL support could not be found for social login service");
                }
                return null;
            }
            try {
                JSSEHelper jsseHelper = sslSupport.getJSSEHelper();
                if (jsseHelper != null) {
                    sslContext = jsseHelper.getSSLContext(sslRef, null, null, true);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "sslContext (" + sslRef + ") get: " + sslContext);
                        // Properties sslProps =
                        // jsseHelper.getProperties(sslRef);
                    }
                }
            } catch (Exception e) {
                throw new SocialLoginException("FAILED_TO_GET_SSL_CONTEXT", e, new Object[] { uniqueId, e.getLocalizedMessage() });
            }
        }

        return this.sslContext;
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() throws SocialLoginException {
        if (this.sslContext == null) {
            SocialLoginService service = socialLoginServiceRef.getService();
            if (service == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Social login service is not available");
                }
                return null;
            }
            SSLSupport sslSupport = service.getSslSupport();
            if (sslSupport == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL support could not be found for social login service");
                }
                return null;
            }
            try {
                sslSocketFactory = sslSupport.getSSLSocketFactory(sslRef);
                JSSEHelper jsseHelper = sslSupport.getJSSEHelper();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "sslSocketFactory (" + sslRef + ") get: " + sslSocketFactory);
                }
            } catch (Exception e) {
                throw new SocialLoginException("FAILED_TO_GET_SSL_CONTEXT", e, new Object[] { uniqueId, e.getLocalizedMessage() });
            }
        }

        return this.sslSocketFactory;
    }

    /** {@inheritDoc} */
    @Override
    public String getJwksUri() {
        return this.jwksUri;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmName() {
        return this.realmName;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmNameAttribute() {
        return this.realmNameAttribute;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserNameAttribute() {
        return this.userNameAttribute;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupNameAttribute() {
        return this.groupNameAttribute;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserUniqueIdAttribute() {
        return this.userUniqueIdAttribute;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getMapToUserRegistry() {
        return this.mapToUserRegistry;
    }

    /** {@inheritDoc} */
    @Override
    public String getJwtRef() {
        return this.jwtRef;
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
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * {@inheritDoc}
     *
     * @throws SocialLoginException
     */
    @Override
    public PublicKey getPublicKey() throws SocialLoginException {
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
        return sslRefInfo.getPublicKey();
    }

    /**
     * {@inheritDoc}
     *
     * @throws SocialLoginException
     */
    @Override
    public PrivateKey getPrivateKey() throws SocialLoginException {
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
        return sslRefInfo.getPrivateKey();
    }

    /** {@inheritDoc} */
    @Override
    public String getRequestTokenUrl() {
        return this.requestTokenUrl;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserApiResponseIdentifier() {
        return userApiResponseIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getUserApiNeedsSpecialHeader() {
        return userApiNeedsSpecialHeader;
    }

    protected SslRefInfoImpl createSslRefInfoImpl(SocialLoginService socialLoginService) {
        return new SslRefInfoImpl(socialLoginService.getSslSupport(), socialLoginService.getKeyStoreServiceRef(), sslRef, keyAliasName);
    }

    @Override
    public String getResponseMode() {
        return null;
    }

    public boolean getUseSystemPropertiesForHttpClientConnections() {
        return useSystemPropertiesForHttpClientConnections;
    }

    public String getK8sTokenReviewEndpoint() {
        return k8sTokenReviewEndpoint;
    }

    @Sensitive
    public String getServiceAccountTokenForK8sTokenreview() {
        return serviceAccountTokenForK8sTokenreview;
    }

    public String getUseAccessTokenFromRequest() {
        return useAccessTokenFromRequest;
    }

    public String getTokenHeaderName() {
        return tokenHeaderName;
    }

}
