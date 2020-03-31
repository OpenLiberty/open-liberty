/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.osgi.framework.ServiceReference;
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
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.SocialConfigUtils;
import com.ibm.ws.security.social.tai.SocialLoginTAI;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(name = "com.ibm.ws.security.social.okdServiceLogin", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = SocialLoginConfig.class, property = { "service.vendor=IBM", "type=okdServiceLogin" })
public class OkdServiceLoginImpl implements SocialLoginConfig {
    public static final TraceComponent tc = Tr.register(OkdServiceLoginImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_UNIQUE_ID = "id";
    private String uniqueId;

    public static final String KEY_userValidationApi = "userValidationApi";
    private String userValidationApi;

    public static final String KEY_apiResponseCacheTime = "apiResponseCacheTime";
    private long apiResponseCacheTime = 1000 * 60 * 10;

    public static final String KEY_authFilterRef = "authFilterRef";
    private String authFilterRef;

    public static final String KEY_realmName = "realmName";
    private String realmName;

    public static final String KEY_sslRef = "sslRef";
    private String sslRef;

    private UserApiConfig[] userApiConfigs = null;
    private AuthenticationFilter authFilter = null;
    private SSLContext sslContext = null;
    private SSLSocketFactory sslSocketFactory = null;

    final AtomicServiceReference<SocialLoginService> socialLoginServiceRef = new AtomicServiceReference<SocialLoginService>(Oauth2LoginConfigImpl.KEY_SOCIAL_LOGIN_SERVICE);

    private CommonConfigUtils configUtils = new CommonConfigUtils();
    private SocialConfigUtils socialConfigUtils = new SocialConfigUtils();

    @Reference(service = SocialLoginService.class, name = Oauth2LoginConfigImpl.KEY_SOCIAL_LOGIN_SERVICE, cardinality = ReferenceCardinality.MANDATORY)
    protected void setSocialLoginService(ServiceReference<SocialLoginService> ref) {
        this.socialLoginServiceRef.setReference(ref);
    }

    protected void unsetSocialLoginService(ServiceReference<SocialLoginService> ref) {
        this.socialLoginServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        this.socialLoginServiceRef.activate(cc);
        initProps(cc, props);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_PROCESSED", uniqueId);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        initProps(cc, props);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        this.socialLoginServiceRef.deactivate(cc);
        Tr.info(tc, "SOCIAL_LOGIN_CONFIG_DEACTIVATED", uniqueId);
    }

    private void initProps(ComponentContext cc, Map<String, Object> props) {
        uniqueId = configUtils.getConfigAttribute(props, KEY_UNIQUE_ID);
        userValidationApi = configUtils.getConfigAttribute(props, KEY_userValidationApi);
        apiResponseCacheTime = configUtils.getLongConfigAttribute(props, KEY_apiResponseCacheTime, apiResponseCacheTime);
        authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        realmName = configUtils.getConfigAttribute(props, KEY_realmName);
        sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);

        initializeMembersAfterConfigAttributesPopulated(props);
    }

    private void initializeMembersAfterConfigAttributesPopulated(Map<String, Object> props) {
        initializeUserApiConfigs();
        resetLazyInitializedMembers();
    }

    private void initializeUserApiConfigs() {
        UserApiConfig[] results = new UserApiConfig[1];
        results[0] = new UserApiConfigImpl(userValidationApi);
        userApiConfigs = results;
    }

    protected void resetLazyInitializedMembers() {
        // Lazy re-initialize of variables
        authFilter = null;
        sslContext = null;
        sslSocketFactory = null;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getClientId() {
        return null;
    }

    @Override
    public String getClientSecret() {
        return null;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return null;
    }

    @Override
    public String getTokenEndpoint() {
        return null;
    }

    @Override
    public UserApiConfig[] getUserApis() {
        if (this.userApiConfigs == null) {
            return null;
        }
        return this.userApiConfigs.clone();
    }

    @Override
    public String getUserApi() {
        return userValidationApi;
    }

    @Override
    public String getUserApiResponseIdentifier() {
        return null;
    }

    @Override
    public Cache getSocialLoginCookieCache() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getWebsite() {
        return null;
    }

    @Override
    public String getSslRef() {
        return sslRef;
    }

    @Override
    public AuthenticationFilter getAuthFilter() {
        if (this.authFilter == null) {
            this.authFilter = SocialLoginTAI.getAuthFilter(this.authFilterRef);
        }
        return this.authFilter;
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() throws SocialLoginException {
        this.sslSocketFactory = socialConfigUtils.getSSLSocketFactory(uniqueId, sslContext, socialLoginServiceRef, sslRef);
        return this.sslSocketFactory;
    }

    @Override
    public HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException {
        return null;
    }

    @Override
    public String getScope() {
        return null;
    }

    @Override
    public String getResponseType() {
        return null;
    }

    @Override
    public String getGrantType() {
        return null;
    }

    @Override
    public boolean createNonce() {
        return false;
    }

    @Override
    public String getResource() {
        return null;
    }

    @Override
    public boolean isClientSideRedirectSupported() {
        return false;
    }

    @Override
    public String getTokenEndpointAuthMethod() {
        return null;
    }

    @Override
    public String getRedirectToRPHostAndPort() {
        return null;
    }

    @Override
    public String getJwksUri() {
        return null;
    }

    @Override
    public String getRealmName() {
        return realmName;
    }

    @Override
    public String getRealmNameAttribute() {
        return null;
    }

    @Override
    public String getUserNameAttribute() {
        // TODO - This is hard-coded to reflect the v1.User schema definition
        return "name";
    }

    @Override
    public String getGroupNameAttribute() {
        // TODO - This is hard-coded to reflect the v1.User schema definition
        return "groups";
    }

    @Override
    public String getUserUniqueIdAttribute() {
        return null;
    }

    @Override
    public boolean getMapToUserRegistry() {
        return false;
    }

    @Override
    public String getJwtRef() {
        return null;
    }

    @Override
    public String[] getJwtClaims() {
        return null;
    }

    @Override
    public String getRequestTokenUrl() {
        return null;
    }

    @Override
    public PublicKey getPublicKey() throws SocialLoginException {
        return null;
    }

    @Override
    public PrivateKey getPrivateKey() throws SocialLoginException {
        return null;
    }

    @Override
    public String getAlgorithm() {
        return null;
    }

    @Override
    public boolean getUserApiNeedsSpecialHeader() {
        return false;
    }

    @Override
    public String getResponseMode() {
        return null;
    }

    @Override
    public boolean getUseSystemPropertiesForHttpClientConnections() {
        return false;
    }

    @Override
    public String getUserApiType() {
        return null;
    }

    @Override
    public String getUserApiToken() {
        return null;
    }

    @Override
    public long getApiResponseCacheTime() {
        return apiResponseCacheTime;
    }

    @Override
    public boolean isAccessTokenRequired() {
        return true;
    }

    @Override
    public boolean isAccessTokenSupported() {
        return false;
    }

    @Override
    public String getAccessTokenHeaderName() {
        return null;
    }

}
