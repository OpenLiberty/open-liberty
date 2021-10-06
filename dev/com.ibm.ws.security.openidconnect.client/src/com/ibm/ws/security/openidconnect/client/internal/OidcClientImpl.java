/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.authentication.filter.internal.AuthFilterConfig;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.common.structures.BoundedHashMap;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils;
import com.ibm.ws.security.openidconnect.client.AccessTokenAuthenticator;
import com.ibm.ws.security.openidconnect.client.AttributeToSubjectExt;
import com.ibm.ws.security.openidconnect.client.OidcClientAuthenticator;
import com.ibm.ws.security.openidconnect.client.OidcClientCache;
import com.ibm.ws.security.openidconnect.client.web.OidcRedirectServlet;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.oauth20.UserCredentialResolver;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * This class is the OSGI service that is invoked from the main line Liberty
 * authentication path to handle OpenID Connect client authentication of
 * incoming web requests. The absence of this service reference indicates that
 * OpenID Connect is not configured.
 */
public class OidcClientImpl implements OidcClient, UnprotectedResourceService {

    private static final TraceComponent tc = Tr.register(OidcClientImpl.class);

    static final String KEY_SECURITY_SERVICE = "securityService";
    public static final String CFG_KEY_OPENID_CONNECT_CLIENT_CONFIG = "oidcClientConfig";
    public final static String KEY_FILTER = "authFilter";
    static protected final String KEY_SERVICE_PID = "service.pid";
    public static final String CFG_KEY_ID = "id";
    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected final AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);
    OidcClientAuthenticator oidcClientAuthenticator;
    AccessTokenAuthenticator accessTokenAuthenticator;
    boolean initOidcClientAuth = false;
    private final Object initOidcClientAuthLock = new Object() {
    };
    boolean initBeforeSso = false;
    private final BoundedHashMap authFilterWarnings = new BoundedHashMap(10);

    static final String KEY_AUTH_CACHE_SERVICE = "authCacheService";
    static final AtomicServiceReference<AuthCacheService> authCacheServiceRef = new AtomicServiceReference<AuthCacheService>(KEY_AUTH_CACHE_SERVICE);

    static SubjectHelper subjectHelper = new SubjectHelper();

    private final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    SecurityService securityService = null;

    private final ConcurrentServiceReferenceMap<String, OidcClientConfig> oidcClientConfigRef = new ConcurrentServiceReferenceMap<String, OidcClientConfig>(
            CFG_KEY_OPENID_CONNECT_CLIENT_CONFIG);

    protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);

    static public final String KEY_USER_RESOLVER = "userResolver";
    protected final ConcurrentServiceReferenceMap<String, UserCredentialResolver> userResolverRef = new ConcurrentServiceReferenceMap<String, UserCredentialResolver>(KEY_USER_RESOLVER);

    int iClientIsBeforeSso = 0;
    boolean needProviderHint = true;

    protected void setOidcClientConfig(ServiceReference<OidcClientConfig> ref) {
        synchronized (initOidcClientAuthLock) {
            oidcClientConfigRef.putReference((String) ref.getProperty(CFG_KEY_ID), ref);
            initOidcClientAuth = true;
            initBeforeSso = true;
            warnIfAuthFilterUseDuplicated(null);
        }
    }

    protected void updatedOidcClientConfig(ServiceReference<OidcClientConfig> ref) {
        synchronized (initOidcClientAuthLock) {
            oidcClientConfigRef.putReference((String) ref.getProperty(CFG_KEY_ID), ref);
            initOidcClientAuth = true;
            initBeforeSso = true;
            warnIfAuthFilterUseDuplicated(null);
        }
    }

    protected void unsetOidcClientConfig(ServiceReference<OidcClientConfig> ref) {
        synchronized (initOidcClientAuthLock) {
            oidcClientConfigRef.removeReference((String) ref.getProperty(CFG_KEY_ID), ref);
            initOidcClientAuth = true;
            initBeforeSso = true;
        }
    }

    protected void setSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        initOidcClientAuth = true;
    }

    protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        initOidcClientAuth = true;
    }

    protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.unsetReference(ref);
        initOidcClientAuth = true;
    }

    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
        securityService = securityServiceRef.getService();
        initOidcClientAuth = true;
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
        securityService = null;
        initOidcClientAuth = true;
    }

    protected void setOidcClientAuthenticator(OidcClientAuthenticator oidcClientAuthenticator) {
        this.oidcClientAuthenticator = oidcClientAuthenticator;
    }

    protected OidcClientAuthenticator getOidcClientAuthenticator() {
        return oidcClientAuthenticator;
    }

    protected void setAccessTokenAuthenticator(AccessTokenAuthenticator accessTokenAuthenticator) {
        this.accessTokenAuthenticator = accessTokenAuthenticator;
    }

    protected AccessTokenAuthenticator getAccessTokenAuthenticator() {
        return accessTokenAuthenticator;
    }

    protected void setAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setAuthenticationFilter id:" + ref.getProperty(AuthFilterConfig.KEY_ID));
        }
        authFilterServiceRef.putReference((String) ref.getProperty(AuthFilterConfig.KEY_ID), ref);
    }

    protected void updatedAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updatedAuthenticationFilter id:" + ref.getProperty(AuthFilterConfig.KEY_ID));
        }
        authFilterServiceRef.putReference((String) ref.getProperty(AuthFilterConfig.KEY_ID), ref);
    }

    protected void unsetAuthFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetAuthenticationFilter id:" + ref.getProperty(AuthFilterConfig.KEY_ID));
        }
        authFilterServiceRef.removeReference((String) ref.getProperty(AuthFilterConfig.KEY_ID), ref);
    }

    protected void setAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.setReference(reference);
    }

    protected void unsetAuthCacheService(ServiceReference<AuthCacheService> reference) {
        authCacheServiceRef.unsetReference(reference);
    }

    protected void setUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.putReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setUserResolver id:" + serviceId);
        }
    }

    protected void updatedUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.putReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateUserResolver id:" + serviceId);
        }
    }

    protected void unsetUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.removeReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetUserResolverRef id:" + serviceId);
        }
    }

    protected void activate(ComponentContext cc) {
        sslSupportRef.activate(cc);
        securityServiceRef.activate(cc);
        authFilterServiceRef.activate(cc);
        authCacheServiceRef.activate(cc);
        userResolverRef.activate(cc);
        AttributeToSubjectExt.setActivatedUserResolverRef(userResolverRef);
        oidcClientConfigRef.activate(cc);
        initOidcClientAuth = true;
        initBeforeSso = true;
        OidcRedirectServlet.setActivatedOidcClientImpl(this);
        // oidcClientAuthenticator = new OidcClientAuthenticator(sslSupportRef);
    }

    protected synchronized void modify(Map<String, Object> properties) {
    }

    protected synchronized void deactivate(ComponentContext cc) {
        sslSupportRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
        authFilterServiceRef.deactivate(cc);
        authCacheServiceRef.deactivate(cc);
        oidcClientConfigRef.deactivate(cc);
        oidcClientAuthenticator = null;
    }

    // warn (only once) if >1 client specifies the same auth filter
    void warnIfAuthFilterUseDuplicated(Iterator<OidcClientConfig> configs) {
        if (configs == null) {
            configs = oidcClientConfigRef.getServices();
        }
        ArrayList<String> authFilters = new ArrayList<String>();
        while (configs.hasNext()) {
            OidcClientConfig config = configs.next();
            String authFilter = config.getAuthFilterId();
            if (authFilter != null) {
                if (authFilters.contains(authFilter)) {
                    if (!authFilterWarnings.containsKey(authFilter)) {
                        Tr.warning(tc, "CONFIG_AUTHFILTER_NOTUNIQUE", authFilter); // CWWKS1530W
                        authFilterWarnings.put(authFilter, null);
                    }
                } else {
                    authFilters.add(authFilter);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res,
            String provider,
            ReferrerURLCookieHandler referrerURLCookieHandler,
            boolean beforeSso) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO RP PROCESS IS STARTING.");
            Tr.debug(tc, "OIDC _SSO RP inbound URL " + WebUtils.getRequestStringForTrace(req, "client_secret"));
        }
        PostParameterHelper.savePostParams((IExtendedRequest) req);
        OidcClientConfig oidcClientConfig = oidcClientConfigRef.getService(provider);
        OidcClientRequest oidcClientRequest = new OidcClientRequest(req, res, oidcClientConfig, referrerURLCookieHandler);
        req.setAttribute(ClientConstants.ATTRIB_OIDC_CLIENT_REQUEST, oidcClientRequest);
        ProviderAuthenticationResult result = authenticate(req, res, provider, referrerURLCookieHandler, beforeSso, oidcClientConfig, oidcClientRequest);
        // handle the result when it's OAuthChallengeReply
        handleOauthChallenge(res, result);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OIDC _SSO RP PROCESS HAS ENDED.");
        }
        return result;
    }

    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res,
            String provider,
            ReferrerURLCookieHandler referrerURLCookieHandler,
            boolean beforeSso,
            OidcClientConfig oidcClientConfig,
            OidcClientRequest oidcClientRequest) {
        ProviderAuthenticationResult result = null;
        if (beforeSso) {
            if (oidcClientConfig.isDisableLtpaCookie()) { // ltpa disabled
                // check if any cached and valid subject
                OidcClientCache oidcCache = new OidcClientCache(authCacheServiceRef.getService(), oidcClientConfig, oidcClientRequest);
                Subject readOnlySubject = oidcCache.getBackValidSubject(req, oidcClientConfig); // need
                if (readOnlySubject != null) {
                    String username = OidcUtil.getUserNameFromSubject(readOnlySubject);
                    Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
                    String customCacheKey = oidcCache.getCustomCacheKey();
                    // Tf we are able to find an readOnlySUbject, the
                    // customCacheKey will not be null for sure
                    // Debug to double check
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "customCacheKey is :" + customCacheKey);
                    }
                    if (oidcClientConfig.isIncludeCustomCacheKeyInSubject()) {
                        customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
                    }
                    customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
                    result = new ProviderAuthenticationResult(AuthResult.SUCCESS,
                            HttpServletResponse.SC_OK,
                            username,
                            null,
                            customProperties,
                            null);
                } else {
                    // if no cached and valid subject, call regular authenticate
                    result = this.authenticate(req, res, provider, oidcClientRequest);
                    if (result.getStatus() == AuthResult.SUCCESS) {
                        // The customCacheKey must be valid
                        oidcClientRequest.createOidcClientCookieIfAnyAndDisableLtpa();
                    }
                }
            } else {
                if (OidcClient.inboundRequired.equalsIgnoreCase(oidcClientConfig.getInboundPropagation())) {
                    result = this.authenticate(req, res, provider, oidcClientRequest);
                } else {
                    result = new ProviderAuthenticationResult(AuthResult.CONTINUE, HttpServletResponse.SC_OK);
                }
            }
        } else {
            if (!oidcClientConfig.isDisableLtpaCookie()) { // ltpa token enable
                result = this.authenticate(req, res, provider, oidcClientRequest);
            } else { // when disableLtpaCookie, we already handle this in
                     // beforeLtpa and deal with its cookie already
                result = new ProviderAuthenticationResult(AuthResult.CONTINUE, HttpServletResponse.SC_OK);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "result httpStatusCode:" + result.getHttpStatusCode() + " status:" + result.getStatus() + " result:" + result);
        }
        return result;

    }

    /** {@inheritDoc} */
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res,
            String provider,
            OidcClientRequest oidcClientRequest) {

        OidcClientConfig oidcClientConfig = oidcClientRequest.getOidcClientConfig();
        if (initOidcClientAuth) {
            synchronized (initOidcClientAuthLock) {
                if (initOidcClientAuth) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Initializing authenticator objects");
                    }
                    oidcClientAuthenticator = new OidcClientAuthenticator(sslSupportRef);
                    accessTokenAuthenticator = new AccessTokenAuthenticator(sslSupportRef);
                    initOidcClientAuth = false;
                }
            }
        }

        if (!oidcClientConfig.getInboundPropagation().equalsIgnoreCase(ClientConstants.PROPAGATION_NONE)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Token propagation is supported or required. Will attempt to authenticate using a provided token");
            }
            ProviderAuthenticationResult oidcResult = accessTokenAuthenticator.authenticate(req, res, oidcClientConfig, oidcClientRequest);
            if (oidcResult.getHttpStatusCode() == HttpServletResponse.SC_OK) {
                return oidcResult;
            }
            if (oidcClientConfig.getInboundPropagation().equalsIgnoreCase(ClientConstants.PROPAGATION_REQUIRED)) {
                oidcClientRequest.setWWWAuthenticate();
                return oidcResult;
            } else {
                // This is propagation "supported"
                // 218872 provider is the id of the oidc client
                //CWWKS1740W: The inbound propagation token for client [{1}] is not valid due to [{0}]. The request will be authenticated using OpenID Connect.
                String rsFailMsg = oidcClientRequest.getRsFailMsg();
                if (rsFailMsg != null) {
                    boolean suppress = rsFailMsg.equals("suppress_CWWKS1704W");
                    if (!suppress) {
                        Tr.warning(tc, "OIDC_CLIENT_BAD_RS_TOKEN", rsFailMsg, provider);
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "access token was not present, warning message was suppressed");
                        }
                    }
                }
            }
        }
        return oidcClientAuthenticator.authenticate(req, res, oidcClientConfig);
    }

    private boolean requestHasOidcCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie ck = cookies[i];
                if (ck.getName().startsWith(ClientConstants.COOKIE_NAME_OIDC_CLIENT_PREFIX)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getOidcProvider(HttpServletRequest req) {
        String ctxPath = req.getContextPath();
        if ("/IBMJMXConnectorREST".equals(ctxPath)) { //RTC244184
            if (!requestHasOidcCookie(req)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "return null for contextPath=/IBMJMXConnectorREST");
                }
                return null;
            } else {
                // admin center scenario - avoid forcing a double login if already authenticated via oidc.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "contextPath=/IBMJMXConnectorREST but oidc cookie found, let authentication proceed");
                }
            }
        }

        // RTC248370
        // if we have entered a redirect loop and are trying to authenticate ourself, don't do that.
        Iterator<OidcClientConfig> clientConfigs = oidcClientConfigRef.getServices();
        while (clientConfigs.hasNext()) {
            if (ctxPath.equals(clientConfigs.next().getContextPath())) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "return null for contextPath " + ctxPath + " matches a clientConfig context path");
                }
                return null;
            }
        }

        String oidcProvider = null;
        String reqProviderHint = null;
        if (needProviderHint()) {
            reqProviderHint = getReqProviderHint(req);
        }
        Iterator<OidcClientConfig> oidcClientConfigs = oidcClientConfigRef.getServices();
        oidcProvider = getProviderConfig(oidcClientConfigs, reqProviderHint, req);
        if (oidcProvider != null) {
            warnIfAmbiguousAuthFilters(oidcClientConfigRef.getServices(), req, authFilterServiceRef);
        }
        return oidcProvider;
    }

    // warn  if >1 auth filter could have matched this request. If so, inconsistent behavior is possible.
    void warnIfAmbiguousAuthFilters(Iterator<OidcClientConfig> oidcClientConfigs, HttpServletRequest req,
            ConcurrentServiceReferenceMap<String, AuthenticationFilter> AFServiceRef) {

        HashMap<String, Object> acceptingAuthFilterIds = new HashMap<String, Object>();
        while (oidcClientConfigs.hasNext()) {
            OidcClientConfig clientConfig = oidcClientConfigs.next();
            if (!clientConfig.isValidConfig()) {
                continue;
            }
            String authFilterId = clientConfig.getAuthFilterId();
            if (authFilterId != null && authFilterId.length() > 0) {
                AuthenticationFilter authFilter = AFServiceRef.getService(authFilterId);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "authFilter id:" + authFilterId + " authFilter:" + authFilter);
                }
                if (authFilter != null && authFilter.isAccepted(req)) {
                    acceptingAuthFilterIds.put(authFilterId, null);
                }
            }
        }
        if (acceptingAuthFilterIds.size() > 1) {
            String filterIds = "";
            Iterator<String> it = acceptingAuthFilterIds.keySet().iterator();
            while (it.hasNext()) {
                filterIds = filterIds + it.next() + " ";
            }
            filterIds = filterIds.trim();
            Tr.warning(tc, "AUTHFILTER_MULTIPLE_MATCHED", req.getRequestURL(), filterIds); // CWWKS1531W
        }
    }

    String getReqProviderHint(HttpServletRequest req) {
        // get it from header first
        String providerHint = getHeader(req, ClientConstants.OIDC_AUTHN_HINT_HEADER);
        if (providerHint == null || providerHint.isEmpty())
            providerHint = getHeader(req, ClientConstants.OIDC_CLIENT);
        if (providerHint == null || providerHint.isEmpty()) {
            PostParameterHelper.savePostParams((IExtendedRequest) req);
            // otherwise get it from parameter
            providerHint = req.getParameter(ClientConstants.OIDC_CLIENT);
            if (providerHint != null)
                providerHint = providerHint.trim();
        }
        return providerHint;
    }

    String getHeader(HttpServletRequest req, String headerName) {
        // get from header
        String providerHint = req.getHeader(headerName);
        if (providerHint != null)
            providerHint = providerHint.trim();
        return providerHint;
    }

    /**
     * 1) if request provider is null, then return the first provider in the
     * list. 2) If request provider is not null, return the match provider;
     * Otherwise, return null
     *
     * @param oidcClientConfigs
     * @param reqProviderHint
     * @return
     */
    protected String getProviderConfig(Iterator<OidcClientConfig> oidcClientConfigs,
            String reqProviderHint,
            HttpServletRequest req) {
        while (oidcClientConfigs.hasNext()) {
            OidcClientConfig oidcClientConfig = oidcClientConfigs.next();
            if (oidcClientConfig.isValidConfig()) { // skip bad oidcClientConfig
                String provider = oidcClientConfig.getId();
                if (reqProviderHint != null) {
                    // This is undocumented scenario. It allows servlet filter
                    // to select an RP instance for SSO
                    if (reqProviderHint.equalsIgnoreCase(provider) && authFilter(oidcClientConfig, req, provider) != null) {
                        return provider;
                    }
                    String issuerIdentifier = oidcClientConfig.getIssuerIdentifier();
                    if (reqProviderHint.equals(issuerIdentifier) && authFilter(oidcClientConfig, req, provider) != null) {
                        return provider;
                    }
                } else {
                    // let's see the filter
                    String providerCfg = authFilter(oidcClientConfig, req, provider);
                    if (providerCfg != null)
                        return providerCfg;
                }
            }
        }
        return null;
    }

    /**
     * @param oidcClientConfig
     * @param provider
     * @return
     */
    String authFilter(OidcClientConfig oidcClientConfig, HttpServletRequest req,
            String provider) {
        // handle filter if any
        String authFilterId = oidcClientConfig.getAuthFilterId();
        if (authFilterId != null && authFilterId.length() > 0) {
            AuthenticationFilter authFilter = authFilterServiceRef.getService(authFilterId);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "authFilter id:" + authFilterId + " authFilter:" + authFilter);
            }
            if (authFilter != null) {
                if (!authFilter.isAccepted(req))
                    return null;
            }
        }
        return provider;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMapIdentityToRegistryUser(String provider) {
        return oidcClientConfigRef.getService(provider).isMapIdentityToRegistryUser();
    }

    @Override
    public boolean isValidRedirectUrl(HttpServletRequest req) {
        String uri = req.getRequestURI();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Validate Request URI:" + uri);
        }

        String clientId = null;
        int iPrefix = uri.lastIndexOf("/");
        if (iPrefix > -1) {
            clientId = uri.substring(iPrefix + 1);
        }
        OidcClientConfig oidcClientConfig = oidcClientConfigRef.getService(clientId);
        if (oidcClientConfig != null) {
            String path = oidcClientConfig.getContextPath() + "/redirect/" + clientId;
            if (uri.endsWith(path)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Configuration id:" + clientId);
                }
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthenticationRequired(HttpServletRequest request) {
        // Since the URLs in the oidcClient are already protected by the web.xml
        // security_constraint
        // not need to request and further authentication for the oidcClient
        // URLs
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
        // 2) remove the spCookie and its cached subject
        synchronized (oidcClientConfigRef) {
            Iterator<OidcClientConfig> services = oidcClientConfigRef.getServices();

            while (services.hasNext()) {
                OidcClientConfig oidcClientConfig = services.next();
                OidcClientRequest oidcClientRequest = new OidcClientRequest(request, response, oidcClientConfig, (ReferrerURLCookieHandler) null);
                if (handleOidcCookie(request, response, oidcClientRequest, userName, bSetSubject)) {
                    bSetSubject = true;
                }
            }
        }
        return bSetSubject;
    }

    /**
     * When we get a valid SP Cookie, authenticate it Then remove the SP Cookie
     *
     * @param req
     * @param samlRequest
     * @return
     */
    @FFDCIgnore(value = { CredentialExpiredException.class, CredentialDestroyedException.class })
    boolean handleOidcCookie(HttpServletRequest req,
            HttpServletResponse response,
            OidcClientRequest oidcClientRequest,
            String userName,
            boolean bSetSubjectAlready) {
        boolean bSetSubject = false;
        OidcClientConfig oidcClientConfig = oidcClientRequest.getOidcClientConfig();
        if (oidcClientConfig.isDisableLtpaCookie()) {
            // check if any cached and valid subject
            OidcClientCache oidcCache = new OidcClientCache(authCacheServiceRef.getService(), oidcClientConfig, oidcClientRequest);
            String preKeyValue = oidcCache.getPreKeyValue(req);
            if (preKeyValue == null || preKeyValue.isEmpty()) {
                // no cookie, no further checking
                return false;
            }
            // otherwise, oidcClientRequest.getOidcClientCookieName() will not
            // be null
            Subject subject = oidcCache.getBackCachedSubject(req, preKeyValue);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "subject from cache is:" + subject);
            }
            if (subject == null) {
                // hmm... no subject? well, let's remove the invalid cookie
                OidcUtil.removeCookie(oidcClientRequest);
                return false;
            }

            if (userName != null && !bSetSubjectAlready) {
                WSCredential wsCredential = getWSCredential(subject);
                if (wsCredential != null) {
                    try {
                        String accessId = wsCredential.getAccessId();
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "wsCredential user:" + accessId);
                        }
                        // if this is the same user as the caller, authenticate
                        // it for core-security
                        if (sameUser(userName, accessId)) {
                            AuthenticationData authenticationData = new WSAuthenticationData();
                            authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
                            authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, response);
                            authenticationData.set(AuthenticationData.TOKEN64, oidcCache.getCustomCacheKey());
                            // authenticate as the subject
                            if (authenticateSubject(subject, req, response, authenticationData)) {
                                bSetSubject = true;
                            }
                        }
                    } catch (CredentialExpiredException e) {
                        // TODO handle the Exception?
                    } catch (CredentialDestroyedException e) {
                        // TODO handle the Exception?
                    }
                }
            }
            oidcCache.removeSubject(req);
        }
        return bSetSubject;
    }

    /**
     * @param subject
     * @return
     */
    boolean authenticateSubject(Subject subject, HttpServletRequest req, HttpServletResponse resp, AuthenticationData authenticationData) {
        // TODO check the lifetime of the subject. If expires, no needs to
        // authenticate
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "subject from oidcCookie is:" + subject);
        }
        SecurityService securityService = securityServiceRef.getService();
        AuthenticationService authenticationService = securityService.getAuthenticationService();
        return authenticateWithSubject(req, resp, subject, authenticationService, authenticationData);
    }

    /**
     * @param req
     * @param res
     * @param subject
     * @return
     * @throws AuthenticationException
     */
    @FFDCIgnore(AuthenticationException.class)
    boolean authenticateWithSubject(HttpServletRequest req,
            HttpServletResponse res,
            Subject subject,
            AuthenticationService authenticationService,
            AuthenticationData authenticationData) {
        try {
            Subject newSubject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, subject);
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setCallerSubject(newSubject);
        } catch (AuthenticationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "authenticationException:" + e);
            }
            return false;
        }
        return true;
    }

    /**
     * @param subject
     * @return
     */
    protected WSCredential getWSCredential(Subject subject) {
        if (subject == null)
            return null;
        Set<WSCredential> credentials = subject.getPublicCredentials(WSCredential.class);
        return credentials.iterator().next();
    }

    /**
     * @return
     */
    protected boolean sameUser(String userName, String accessId) {
        if (userName == null) {
            return false;
        }
        return userName.equals(accessId);
    }

    // The request was processed by this Configuration ID. The client
    // should/must not change
    public OidcClientConfigImpl getOidcClientConfig(HttpServletRequest req, String clientId) {

        Iterator<OidcClientConfig> oidcClientConfigs = oidcClientConfigRef.getServices();
        while (oidcClientConfigs.hasNext()) {
            OidcClientConfig oidcClientConfig = oidcClientConfigs.next();
            if (oidcClientConfig.isValidConfig()) { // skip bad oidcClientConfig
                if (oidcClientConfig.getId().equals(clientId)) {
                    return (OidcClientConfigImpl) oidcClientConfig;
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean anyClientIsBeforeSso() {
        if (initBeforeSso) {
            initOidcClientConfigs();
        }
        return iClientIsBeforeSso > 0;
    }

    /** {@inheritDoc} */
    public boolean needProviderHint() {
        if (initBeforeSso) {
            initOidcClientConfigs();
        }
        return needProviderHint;
    }

    void initOidcClientConfigs() { // 226889
        synchronized (initOidcClientAuthLock) {
            if (initBeforeSso) {
                iClientIsBeforeSso = 0;
                needProviderHint = false;
                // loop through all OidcClientConfig(s) to init these 2
                // flags
                Iterator<OidcClientConfig> oidcClientConfigs = oidcClientConfigRef.getServices();
                while (oidcClientConfigs.hasNext()) {
                    OidcClientConfig oidcClientConfig = oidcClientConfigs.next();
                    if (oidcClientConfig.isDisableLtpaCookie()) { // ltpa
                                                                  // disabled
                        iClientIsBeforeSso++;
                    } else {
                        if (OidcClient.inboundRequired.equalsIgnoreCase(oidcClientConfig.getInboundPropagation())) {
                            iClientIsBeforeSso++;
                        }
                    }
                    // Let's check any one need reqProviderHint.
                    // Even only one needs it, we have to check reqProviderHint,
                    // which calls HttpServletRequest.getParameter().
                    // 226889
                    if (oidcClientConfig.isOidcclientRequestParameterSupported()) {
                        needProviderHint = true;
                    }
                }
                // init beforeSso
                initBeforeSso = false;
            }
        }
    }

    /**
     * @param res
     * @param result
     */
    void handleOauthChallenge(HttpServletResponse rsp, ProviderAuthenticationResult oidcResult) {
        if (oidcResult.getStatus() == AuthResult.CONTINUE || oidcResult.getStatus() == AuthResult.REDIRECT_TO_PROVIDER) {
            // do not handle these statuses
            return;
        }
        String errorDescription = null;
        if (oidcResult.getStatus() == AuthResult.FAILURE) {
            if (HttpServletResponse.SC_UNAUTHORIZED == oidcResult.getHttpStatusCode()) {
                errorDescription = "OpenID Connect client failed the request...";
            }
        } else if (oidcResult.getStatus() != AuthResult.SUCCESS) {
            if (HttpServletResponse.SC_UNAUTHORIZED == oidcResult.getHttpStatusCode()) {
                errorDescription = "OpenID Connect client returned with status: " + oidcResult.getStatus();
            }
        }
        if (errorDescription != null) {
            try {
                OAuth20ProviderUtils.handleOAuthChallenge(rsp, oidcResult, errorDescription);
            } catch (IOException ioe) {
                // TODO error handling further
                //
                // Since this is a part of error handling.
                // even though this did not set up proper message here,
                // the error handling will continue handling it
            }

        }
    }

    //@Override
    @Override
    public boolean postLogout(HttpServletRequest arg0, HttpServletResponse arg1) {
        // TODO Auto-generated method stub
        return false;
    }
}
