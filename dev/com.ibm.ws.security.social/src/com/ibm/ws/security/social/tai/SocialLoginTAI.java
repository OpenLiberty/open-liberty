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
package com.ibm.ws.security.social.tai;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.SocialLoginWebappConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.OidcLoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.RequestUtil;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.ws.security.social.twitter.TwitterConstants;
import com.ibm.ws.security.social.web.EndpointServices;
import com.ibm.ws.security.social.web.SelectionPageGenerator;
import com.ibm.ws.security.social.web.utils.ObscuredConfigIdManager;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.ssl.SSLSupport;

public class SocialLoginTAI implements TrustAssociationInterceptor, UnprotectedResourceService {

    public static final TraceComponent tc = Tr.register(SocialLoginTAI.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static protected final String KEY_SERVICE_PID = "service.pid";
    static protected final String KEY_PROVIDER_ID = "id";
    static protected final String KEY_ID = "id";

    static final String KEY_LOCATION_ADMIN = "locationAdmin";
    static final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);
    static final String KEY_AUTH_CACHE_SERVICE = "authCacheService";
    static final AtomicServiceReference<AuthCacheService> authCacheServiceRef = new AtomicServiceReference<AuthCacheService>(KEY_AUTH_CACHE_SERVICE);
    static final String KEY_SECURITY_SERVICE = "securityService";
    static final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    static public final String KEY_FILTER = "authFilter";
    static protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);
    static public final String KEY_SOCIAL_LOGIN_CONFIG = "socialLoginConfig";
    protected final static ConcurrentServiceReferenceMap<String, SocialLoginConfig> socialLoginConfigRef = new ConcurrentServiceReferenceMap<String, SocialLoginConfig>(KEY_SOCIAL_LOGIN_CONFIG);
    static final String KEY_SOCIAL_WEB_APP_SERVICE = "socialLoginWebappConfig";
    static final AtomicServiceReference<SocialLoginWebappConfig> socialWebappConfigRef = new AtomicServiceReference<SocialLoginWebappConfig>(KEY_SOCIAL_WEB_APP_SERVICE);

    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);

    static WebProviderAuthenticatorHelper authHelper;
    TAIWebUtils taiWebUtils = new TAIWebUtils();
    TAIRequestHelper taiRequestHelper = new TAIRequestHelper();
    SocialWebUtils webUtils = new SocialWebUtils();
    static ObscuredConfigIdManager configIdManager = new ObscuredConfigIdManager();

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

    public void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    public void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    protected void setAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.putReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setFilter pid:" + pid);
        }
    }

    protected void updatedAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.putReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setFilter pid:" + pid);
        }
    }

    protected void unsetAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (authFilterServiceRef) {
            authFilterServiceRef.removeReference(pid, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetFilter pid:" + pid);
        }
    }

    // Method for unit testing.
    static public AuthenticationFilter getAuthFilter(String pid) {
        return authFilterServiceRef.getService(pid);
    }

    protected void setSocialLoginConfig(ServiceReference<SocialLoginConfig> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (socialLoginConfigRef) {
            socialLoginConfigRef.putReference(id, ref);
        }

        trackSocialLoginId(id);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setSocialLoginConfig id:" + id + " Number of references is now: " + socialLoginConfigRef.size());
        }
    }

    protected void updatedSocialLoginConfig(ServiceReference<SocialLoginConfig> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (socialLoginConfigRef) {
            socialLoginConfigRef.putReference(id, ref);
        }

        trackSocialLoginId(id);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateSocialLoginConfig id:" + id);
        }
    }

    protected void unsetSocialLoginConfig(ServiceReference<SocialLoginConfig> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (socialLoginConfigRef) {
            socialLoginConfigRef.removeReference(id, ref);
        }

        untrackSocialLoginId(id);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetSocialLoginConfig id:" + id);
        }
    }

    void trackSocialLoginId(String id) {
        configIdManager.addId(id);
    }

    void untrackSocialLoginId(String id) {
        configIdManager.removeId(id);
    }

    /**
     * Returns the obscured value that corresponds to the provided ID. Obscured configuration IDs are used in user-facing
     * situations where internal configuration data either must not or should not be exposed.
     */
    public static String getObscuredIdFromConfigId(String configId) {
        return configIdManager.getObscuredIdFromConfigId(configId);
    }

    /**
     * Returns the original configuration ID that corresponds to the provided obscured value. Obscured configuration IDs are used
     * in user-facing situations where internal configuration data either must not or should not be exposed.
     */
    public static String getConfigIdFromObscuredId(String obscuredConfigId) {
        return configIdManager.getConfigIdFromObscuredId(obscuredConfigId);
    }

    public static SocialLoginConfig getSocialLoginConfig(String key) {
        // TODO: Use read/write locks to serialize access when the socialLoginConfigRef is being updated.
        return socialLoginConfigRef.getService(key);
    }

    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    protected void setAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.setReference(reference);
    }

    protected void unsetAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.unsetReference(reference);
    }

    public void setSocialLoginWebappConfig(ServiceReference<SocialLoginWebappConfig> reference) {
        socialWebappConfigRef.setReference(reference);
    }

    public void unsetSocialLoginWebappConfig(ServiceReference<SocialLoginWebappConfig> reference) {
        socialWebappConfigRef.unsetReference(reference);
    }

    public static SocialLoginWebappConfig getSocialLoginWebappConfig() {
        return socialWebappConfigRef.getService();
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        synchronized (authFilterServiceRef) {
            sslSupportRef.activate(cc);
            authFilterServiceRef.activate(cc);
        }

        synchronized (socialLoginConfigRef) {
            socialLoginConfigRef.activate(cc);
        }
        locationAdminRef.activate(cc);
        // TODO The cache service maybe disabled in
        // /com.ibm.ws.security.authentication.builtin/src/com/ibm/ws/security/authentication/internal/AuthenticationServiceImpl.java
        authCacheServiceRef.activate(cc);
        securityServiceRef.activate(cc);
        socialWebappConfigRef.activate(cc);
        authHelper = new WebProviderAuthenticatorHelper(securityServiceRef);

        EndpointServices.setActivatedSocialLoginConfigRef(socialLoginConfigRef);
        EndpointServices.setActivatedSecurityServiceRef(securityServiceRef);

        RequestUtil.setSocialLoginConfigRef(socialLoginConfigRef);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        // Do nothing for now.
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        synchronized (authFilterServiceRef) {
            sslSupportRef.deactivate(cc);
            authFilterServiceRef.deactivate(cc);
        }
        synchronized (socialLoginConfigRef) {
            // 240443 work around small kernel bug.
            // need to remove all references, because if we changed id param, osgi will not remove old one.
            // it will however add everything back in later, so we can remove everything now.
            Iterator<String> keysIt = socialLoginConfigRef.keySet().iterator();
            while (keysIt.hasNext()) {
                String key = keysIt.next();
                ServiceReference<SocialLoginConfig> configref = socialLoginConfigRef.getReference(key);
                socialLoginConfigRef.removeReference(key, configref);
            }
            socialLoginConfigRef.deactivate(cc);
        }
        locationAdminRef.deactivate(cc);
        authCacheServiceRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
        socialWebappConfigRef.deactivate(cc);
    }

    @Override
    public boolean isTargetInterceptor(HttpServletRequest request) throws WebTrustAssociationException {
        SocialTaiRequest socialTaiRequest = taiRequestHelper.createSocialTaiRequestAndSetRequestAttribute(request);
        return taiRequestHelper.requestShouldBeHandledByTAI(request, socialTaiRequest);
    }

    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest request, HttpServletResponse response) throws WebTrustAssociationFailedException {
        TAIResult taiResult = TAIResult.create(HttpServletResponse.SC_FORBIDDEN);

        SocialTaiRequest socialTaiRequest = (SocialTaiRequest) request.getAttribute(Constants.ATTRIBUTE_TAI_REQUEST);
        if (socialTaiRequest == null) {
            // Should not be null
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Request is missing " + Constants.ATTRIBUTE_TAI_REQUEST + " attribute.");
            }
            return taiWebUtils.sendToErrorPage(response, taiResult);
        }
        return getAssociatedConfigAndHandleRequest(request, response, socialTaiRequest, taiResult);
    }

    @FFDCIgnore({ SocialLoginException.class })
    TAIResult getAssociatedConfigAndHandleRequest(HttpServletRequest request, HttpServletResponse response, SocialTaiRequest socialTaiRequest, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
        SocialLoginConfig clientConfig = null;
        try {
            clientConfig = socialTaiRequest.getTheOnlySocialLoginConfig();
        } catch (SocialLoginException e) {
            // Couldn't find a unique social login config to serve this request - redirect to sign in page for user to select
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A unique social login config wasn't found for this request. Exception was " + e.getMessage());
            }
            return displaySocialMediaSelectionPage(request, response, socialTaiRequest);
        }
        return handleRequestBasedOnSocialLoginConfig(request, response, clientConfig, defaultTaiResult);
    }

    TAIResult displaySocialMediaSelectionPage(HttpServletRequest request, HttpServletResponse response, SocialTaiRequest socialTaiRequest) throws WebTrustAssociationFailedException {
        SelectionPageGenerator selectionPageGenerator = getSelectionPageGenerator();
        try {
            selectionPageGenerator.displaySelectionPage(request, response, socialTaiRequest);
        } catch (IOException e) {
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_FORBIDDEN));
        }
        return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
    }

    SelectionPageGenerator getSelectionPageGenerator() {
        return new SelectionPageGenerator();
    }

    TAIResult handleRequestBasedOnSocialLoginConfig(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config, TAIResult defaultTaiResult) throws WebTrustAssociationFailedException {
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Client config for request could not be found. An error must have occurred initializing this request.");
            }
            return taiWebUtils.sendToErrorPage(response, defaultTaiResult);
        }

        // Start social login; remove any cached data from attempting any previous form login
        removeCachedDataFromLocalAuthentication(request, response);

        if (isTwitterConfig(config)) {
            // Twitter doesn't follow the OAuth 2.0 flow, so Twitter requests must be handled differently
            return handleTwitterLoginRequest(request, response, config);
        }
        if (config instanceof OidcLoginConfigImpl) {
            return handleOidc(request, response, (OidcLoginConfigImpl) config);
        }
        return handleOAuthLoginRequest(request, response, config);

    }

    /**
     * Removes any data (original request URL and POST parameters) that was cached from an early local authentication attempt.
     */
    void removeCachedDataFromLocalAuthentication(HttpServletRequest request, HttpServletResponse response) {
        webUtils.removeRequestUrlAndParameters(request, response);
    }

    boolean isTwitterConfig(SocialLoginConfig config) {
        return config.getClass().getName().contains(TwitterConstants.TWITTER_CONFIG_CLASS);
    }

    /** {@inheritDoc} */
    @Override
    public int initialize(Properties props) throws WebTrustAssociationFailedException {
        // Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        // Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        // Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() {
        // Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthenticationRequired(HttpServletRequest request) {
        String ctxPath = request.getContextPath();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Context path:" + ctxPath);
        }
        // return !(KnownSocialLoginUrl.SOCIAL_LOGIN_CONTEXT_PATH.equals(ctxPath));
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout(HttpServletRequest request, HttpServletResponse response, String userName) {
        boolean bSetSubject = false;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "logout() userName:" + userName);
        }

        // Search all service and
        // 1) setSubject if subject match
        // 2) remove the cookie and its cached subject
        synchronized (this.socialLoginConfigRef) {
            Iterator<SocialLoginConfig> services = this.socialLoginConfigRef.getServices();
            SocialLoginConfig socialLoginConfig = null;
            while (services.hasNext()) {
                socialLoginConfig = services.next();
                // TODO remove all the cookies of the subject
            }
        }
        return bSetSubject;
    }

    TAIResult handleOAuthLoginRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
        OAuthLoginFlow oauthLoginFlow = getOAuthLoginFlow();
        return oauthLoginFlow.handleOAuthRequest(request, response, clientConfig);
    }

    OAuthLoginFlow getOAuthLoginFlow() {
        return new OAuthLoginFlow();
    }

    TAIResult handleTwitterLoginRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws WebTrustAssociationFailedException {
        TwitterLoginFlow twitterLoginFlow = getTwitterLoginFlow();
        return twitterLoginFlow.handleTwitterRequest(request, response, config);
    }

    TwitterLoginFlow getTwitterLoginFlow() {
        return new TwitterLoginFlow();
    }

    TAIResult handleOidc(HttpServletRequest request, HttpServletResponse response, OidcLoginConfigImpl clientConfig) throws WebTrustAssociationFailedException {
        if (!isConfigValid(clientConfig)) {
            return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        }
        OIDCClientAuthenticatorUtil oidccau = new OIDCClientAuthenticatorUtil(sslSupportRef.getService());

        // the OidcClientRequest object is used mostly after the authorization code has come back
        // but we cannot easily tell what state we are in here, so create one regardless.
        OidcClientRequest clientRequest = new OidcClientRequest(request, response, clientConfig, null);
        request.setAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST, clientRequest);

        // call the oidc code.
        ProviderAuthenticationResult presult = oidccau.authenticate(request, response, clientConfig);
        discoverOPAgain(presult, clientConfig);
        if (presult.getStatus().compareTo(AuthResult.REDIRECT_TO_PROVIDER) == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.event(tc, "redirecting to provider, javascript redirect supported = " + clientConfig.isClientSideRedirect());
            }
            if (!clientConfig.isClientSideRedirect()) {
                try {
                    response.sendRedirect(presult.getRedirectUrl());
                } catch (IOException e) {
                    // ffdc
                }
            }
            return TAIResult.create(HttpServletResponse.SC_FORBIDDEN); //doing redirect to provider.
        }

        // if we got here, we've been off to get the tokens and might even have a valid one.
        if (presult.getStatus().compareTo(AuthResult.SUCCESS) != 0) {
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }

        // have validated tokens from oidc, create the subject.
        String idToken = (String) presult.getCustomProperties().get(ClientConstants.ID_TOKEN);
        String accessToken = (String) presult.getCustomProperties().get(ClientConstants.ACCESS_TOKEN);
        Map<String, Object> tokens = new HashMap<String, Object>();
        tokens.put(ClientConstants.ACCESS_TOKEN, accessToken);
        tokens.put(ClientConstants.ID_TOKEN, idToken);
        AuthorizationCodeAuthenticator aca = new AuthorizationCodeAuthenticator(clientConfig, tokens);

        TAIResult authnResult = null;
        try {
            aca.createJwtUserApiResponseAndIssuedJwtFromIdToken(idToken);
            TAISubjectUtils subjectUtils = getTAISubjectUtils(aca);
            // if have userinfo data, put it in the UserProfile object
            String userInfo = (String) presult.getCustomProperties().get(com.ibm.ws.security.openidconnect.common.Constants.USERINFO_STR);
            if (userInfo != null) {
                subjectUtils.setUserInfo(userInfo);
            }       
            authnResult = subjectUtils.createResult(response, clientConfig);
        } catch (Exception e) {
            Tr.error(tc, "AUTH_CODE_ERROR_CREATING_RESULT", new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return taiWebUtils.sendToErrorPage(response, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }
        
        taiWebUtils.restorePostParameters(request); // did oidc already do this?

        return authnResult;

    }

    private void discoverOPAgain(ProviderAuthenticationResult presult, OidcLoginConfigImpl clientConfig) {
		
    	if (clientConfig.isDiscoveryInUse()) {
    		if (presult.getStatus().compareTo(AuthResult.SUCCESS) == 0) {
    			clientConfig.setNextDiscoveryTime();
    		} else if (System.currentTimeMillis() > clientConfig.getNextDiscoveryTime()) {
    			clientConfig.handleDiscoveryEndpoint(clientConfig.getDiscoveryEndpointUrl());
    		}
    	}
		
	}

	/**
     * Check for some things that will always fail and emit message about bad config.
     * Do here so 1) classic oidc messages don't change and 2) put error message closer in log to failure.
     *
     * @param config
     * @return true if config is valid
     */
    boolean isConfigValid(ConvergedClientConfig config) {
        boolean valid = true;
        String clientId = config.getClientId();
        String clientSecret = config.getClientSecret();
        String authorizationEndpoint = config.getAuthorizationEndpointUrl();
        String jwksUri = config.getJwkEndpointUrl();
        if (clientId == null || clientId.length() == 0) {
            Tr.error(tc, "INVALID_CONFIG_PARAM", new Object[] { OidcLoginConfigImpl.KEY_clientId, clientId }); //CWWKS5500E
            valid = false;
        }
        if (clientSecret == null || clientSecret.length() == 0) {
            Tr.error(tc, "INVALID_CONFIG_PARAM", new Object[] { OidcLoginConfigImpl.KEY_clientSecret, "" }); //CWWKS5500E
            valid = false;
        }
        if (authorizationEndpoint == null || authorizationEndpoint.length() == 0
                || (!authorizationEndpoint.toLowerCase().startsWith("http"))) {
            Tr.error(tc, "INVALID_CONFIG_PARAM", new Object[] { OidcLoginConfigImpl.KEY_authorizationEndpoint, authorizationEndpoint }); //CWWKS5500E
            valid = false;
        }

        // TODO: we need a message when we have bad jwks uri's, but this kind of check fails when jwks uri is not set.
        /*
         * if (jwksUri == null || jwksUri.length() == 0
         * || (!jwksUri.toLowerCase().startsWith("http"))) {
         * Tr.error(tc, "INVALID_CONFIG_PARAM", new Object[] { OidcLoginConfigImpl.KEY_jwksUri, jwksUri }); //CWWKS5500E
         *
         * }
         */
        return valid;

    }

    TAISubjectUtils getTAISubjectUtils(AuthorizationCodeAuthenticator authzCodeAuthenticator) {
        return new TAISubjectUtils(authzCodeAuthenticator);
    }

    @Override
    public boolean postLogout(HttpServletRequest arg0, HttpServletResponse arg1) {
        // TODO Auto-generated method stub
        return false;
    }

}
