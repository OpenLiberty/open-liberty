/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.security.thread.ThreadIdentityException;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.collaborator.CollaboratorUtils;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator;
import com.ibm.ws.webcontainer.security.internal.DenyReply;
import com.ibm.ws.webcontainer.security.internal.FormLoginExtensionProcessor;
import com.ibm.ws.webcontainer.security.internal.FormLogoutExtensionProcessor;
import com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler;
import com.ibm.ws.webcontainer.security.internal.JCacheLoggedOutCookieCache;
import com.ibm.ws.webcontainer.security.internal.PermitReply;
import com.ibm.ws.webcontainer.security.internal.ReturnReply;
import com.ibm.ws.webcontainer.security.internal.SRTServletRequestUtils;
import com.ibm.ws.webcontainer.security.internal.URLHandler;
import com.ibm.ws.webcontainer.security.internal.WebAppSecurityConfigChangeEventImpl;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.ws.webcontainer.security.internal.WebSecurityCollaboratorException;
import com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl;
import com.ibm.ws.webcontainer.security.jacc.WebAppJaccAuthorizationHelper;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.ws.webcontainer.security.util.SSOAuthFilter;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

import io.openliberty.jcache.CacheService;

public class WebAppSecurityCollaboratorImpl implements IWebAppSecurityCollaborator, WebAppAuthorizationHelper {
    private static final TraceComponent tc = Tr.register(WebAppSecurityCollaboratorImpl.class);

    static final String KEY_ID = "id";
    static final String KEY_SERVICE_ID = "service.id";
    static final String KEY_COMPONENT_NAME = "component.name";
    public static final String KEY_SECURITY_SERVICE = "securityService";
    public static final String KEY_SSO_SERVICE = "ssoAuthFilter";
    public static final String KEY_TAI_SERVICE = "taiService";
    public static final String KEY_INTERCEPTOR_SERVICE = "interceptorService";
    static final String KEY_WEB_JACC_SERVICE = "webJaccService";
    static final String JASPI_SERVICE_COMPONENT_NAME = "com.ibm.ws.security.jaspi";
    public static final String KEY_WEB_AUTHENTICATOR = "webAuthenticator";
    public static final String KEY_UNPROTECTED_RESOURCE_SERVICE = "unprotectedResourceService";
    static final String KEY_CONFIG_CHANGE_LISTENER = "webAppSecurityConfigChangeListener";

    static final String DELEGATION_USERS_LIST = "DELEGATION_USERS_LIST";
    protected final ConcurrentServiceReferenceMap<String, WebAuthenticator> webAuthenticatorRef = new ConcurrentServiceReferenceMap<String, WebAuthenticator>(KEY_WEB_AUTHENTICATOR);
    protected final ConcurrentServiceReferenceMap<String, UnprotectedResourceService> unprotectedResourceServiceRef = new ConcurrentServiceReferenceMap<String, UnprotectedResourceService>(KEY_UNPROTECTED_RESOURCE_SERVICE);

    protected final AtomicServiceReference<SSOAuthFilter> ssoAuthFilterRef = new AtomicServiceReference<SSOAuthFilter>(KEY_SSO_SERVICE);
    protected final AtomicServiceReference<TAIService> taiServiceRef = new AtomicServiceReference<TAIService>(KEY_TAI_SERVICE);
    protected final ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef = new ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor>(KEY_INTERCEPTOR_SERVICE);
    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    protected final AtomicServiceReference<WebJaccService> webJaccServiceRef = new AtomicServiceReference<WebJaccService>(KEY_WEB_JACC_SERVICE);
    protected final ConcurrentServiceReferenceSet<WebAppSecurityConfigChangeListener> webAppSecurityConfigchangeListenerRef = new ConcurrentServiceReferenceSet<WebAppSecurityConfigChangeListener>(KEY_CONFIG_CHANGE_LISTENER);

    private static final String KEY_LOCATION_ADMIN = "locationAdmin";
    protected final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);
    private static final WebReply PERMIT_REPLY = new PermitReply();
    private static final WebReply DENY_AUTHZ_FAILED = new DenyReply("AuthorizationFailed");
    private static final String AUTH_TYPE = "AUTH_TYPE";
    private static final String SECURITY_CONTEXT = "SECURITY_CONTEXT";

    // '**' will represent the Servlet 3.1 defined all authenticated Security constraint
    private static final String ALL_AUTHENTICATED_ROLE = "**";

    // '_starstar_' will represent if ** was specified as a defined role
    private static final String STARSTAR_ROLE = "_starstar_";

    private static WebAppSecurityConfig globalConfig = null;
    private static WebAuthenticatorFactory globalWebAuthenticatorFactory = null;
    protected final Map<String, Object> currentProps = new HashMap<String, Object>();
    protected volatile WebAuthenticatorFactory authenticatorFactory = null;
    protected volatile WebAppSecurityConfig webAppSecConfig = null;
    protected volatile AuthenticateApi authenticateApi = null;
    protected volatile PostParameterHelper postParameterHelper = null;
    protected CollaboratorUtils collabUtils;

    protected SubjectHelper subjectHelper;
    protected SubjectManager subjectManager;
    protected HTTPSRedirectHandler httpsRedirectHandler;

    protected AuditManager auditManager;

    protected WebAuthenticatorProxy authenticatorProxy;
    protected WebProviderAuthenticatorProxy providerAuthenticatorProxy;

    private UnauthenticatedSubjectService unauthenticatedSubjectService;
    private WebAppAuthorizationHelper wasch = this;
    private FeatureProvisioner provisionerService;

    private boolean isJaspiEnabled = false;
    private Subject savedSubject = null;
    private boolean isActive = false;

    /**
     * Zero length constructor required by DS.
     */
    public WebAppSecurityCollaboratorImpl() {
        this(new SubjectHelper(), new SubjectManager(), new HTTPSRedirectHandler());
        this.auditManager = new AuditManager();
    }

    /**
     * This constructor allows for control over the collaborator objects
     * is intended for use by the unit tests. The runtime will use the
     * default constructor as it is required by DS.
     */
    public WebAppSecurityCollaboratorImpl(SubjectHelper subjectHelper,
                                          SubjectManager subjectManager,
                                          HTTPSRedirectHandler httpsRedirectHandler) {
        this.subjectHelper = subjectHelper;
        this.subjectManager = subjectManager;
        this.httpsRedirectHandler = httpsRedirectHandler;
        this.collabUtils = new CollaboratorUtils(subjectManager);
    }

    /**
     * Unit test constructor. <b>DO NOT INVOKE FROM PRODUCTION CODE.</b>
     * <p>
     * (This overall logic needs to get cleaned up so we don't need this in the future).
     */
    public WebAppSecurityCollaboratorImpl(SubjectHelper subjectHelper,
                                          SubjectManager subjectManager,
                                          HTTPSRedirectHandler httpsRedirectHandler,
                                          WebAppSecurityConfig webAppSecConfig) {
        this.subjectHelper = subjectHelper;
        this.subjectManager = subjectManager;
        this.httpsRedirectHandler = httpsRedirectHandler;
        this.webAppSecConfig = webAppSecConfig;
        WebSecurityHelperImpl.setWebAppSecurityConfig(webAppSecConfig);
    }

    public void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    public void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    public void setSsoAuthFilter(ServiceReference<SSOAuthFilter> reference) {
        ssoAuthFilterRef.setReference(reference);
    }

    public void unsetSsoAuthFilter(ServiceReference<SSOAuthFilter> reference) {
        ssoAuthFilterRef.unsetReference(reference);
    }

    public void setTaiService(ServiceReference<TAIService> reference) {
        taiServiceRef.setReference(reference);
    }

    public void unsetTaiService(ServiceReference<TAIService> reference) {
        taiServiceRef.unsetReference(reference);
    }

    public synchronized void setInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        interceptorServiceRef.putReference(id, ref);
    }

    public synchronized void unsetInterceptorService(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = getComponentId(ref);
        interceptorServiceRef.removeReference(id, ref);
    }

    public void setWebAuthenticator(ServiceReference<WebAuthenticator> ref) {
        String cn = (String) ref.getProperty(KEY_COMPONENT_NAME);
        webAuthenticatorRef.putReference(cn, ref);
        if (cn.equals(JASPI_SERVICE_COMPONENT_NAME)) {
            isJaspiEnabled = true;
        }
    }

    public void unsetWebAuthenticator(ServiceReference<WebAuthenticator> ref) {
        String cn = (String) ref.getProperty(KEY_COMPONENT_NAME);
        webAuthenticatorRef.removeReference(cn, ref);
        if (cn.equals(JASPI_SERVICE_COMPONENT_NAME)) {
            isJaspiEnabled = false;
        }
    }

    public void setUnprotectedResourceService(ServiceReference<UnprotectedResourceService> ref) {
        unprotectedResourceServiceRef.putReference(getServiceId(ref), ref);
    }

    public void unsetUnprotectedResourceService(ServiceReference<UnprotectedResourceService> ref) {
        unprotectedResourceServiceRef.removeReference(getServiceId(ref), ref);
    }

    String getServiceId(ServiceReference<UnprotectedResourceService> ref) {
        long lId = (Long) ref.getProperty(KEY_SERVICE_ID);
        return "urs_" + lId;
    }

    public void setUnauthenticatedSubjectService(UnauthenticatedSubjectService srv) {
        this.unauthenticatedSubjectService = srv;
    }

    protected void unsetUnauthenticatedSubjectService(UnauthenticatedSubjectService srv) {
        if (this.unauthenticatedSubjectService == srv) {
            this.unauthenticatedSubjectService = null;
        }
    }

    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    public void setAuthenticatorFactory(WebAuthenticatorFactory authenticatorFactory) {
        this.authenticatorFactory = authenticatorFactory;
        globalWebAuthenticatorFactory = authenticatorFactory;
        if (!FrameworkState.isStopping() && isActive && webAppSecConfig != null) {
            activateComponents();
        }
    }

    public void unsetAuthenticatorFactory(WebAuthenticatorFactory authenticatorFactory) {
        if (this.authenticatorFactory == authenticatorFactory) {
            this.authenticatorFactory = null;
        }
    }

    protected void setWebJaccService(ServiceReference<WebJaccService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "enabling JACC service");
        }
        webJaccServiceRef.setReference(ref);
        wasch = new WebAppJaccAuthorizationHelper(webJaccServiceRef);
    }

    protected void unsetWebJaccService(ServiceReference<WebJaccService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "disabling JACC service");
        }
        webJaccServiceRef.unsetReference(ref);
        wasch = this;
    }

    protected synchronized void setKernelProvisioner(FeatureProvisioner provisionerService) {
        this.provisionerService = provisionerService;
    }

    protected synchronized void unsetKernelProvisioner(FeatureProvisioner provisionerService) {
        this.provisionerService = null;
    }

    protected void setWebAppSecurityConfigChangeListener(ServiceReference<WebAppSecurityConfigChangeListener> ref) {
        webAppSecurityConfigchangeListenerRef.addReference(ref);
    }

    protected void unsetWebAppSecurityConfigChangeListener(ServiceReference<WebAppSecurityConfigChangeListener> ref) {
        webAppSecurityConfigchangeListenerRef.removeReference(ref);
    }

    /**
     * Set the {@link CacheService} for the logged out cookie cache on this {@link WebAppSecurityCollaboratorImpl}.
     *
     * @param service the {@link CacheService}
     */
    protected void setLoggedOutCookieCacheService(CacheService service) {
        LoggedOutCookieCacheHelper.setLoggedOutCookieCacheService(new JCacheLoggedOutCookieCache(service));
    }

    /**
     * Unset the {@link CacheService} for the logged out cookie cache on this {@link WebAppSecurityCollaboratorImpl}.
     *
     * @param service the {@link CacheService}
     */
    protected void unsetLoggedOutCookieCacheService(CacheService service) {
        LoggedOutCookieCacheHelper.setLoggedOutCookieCacheService(null);
    }

    protected void activate(ComponentContext cc, Map<String, Object> props) {
        isActive = true;
        locationAdminRef.activate(cc);
        securityServiceRef.activate(cc);
        interceptorServiceRef.activate(cc);
        ssoAuthFilterRef.activate(cc);
        taiServiceRef.activate(cc);
        webJaccServiceRef.activate(cc);
        webAuthenticatorRef.activate(cc);
        unprotectedResourceServiceRef.activate(cc);
        webAppSecurityConfigchangeListenerRef.activate(cc);
        currentProps.clear();
        if (props != null) {
            currentProps.putAll(props);
        }
        activateComponents();
    }

    protected void activateComponents() {
        webAppSecConfig = authenticatorFactory.createWebAppSecurityConfigImpl(currentProps, locationAdminRef, securityServiceRef);
        updateComponents();
    }

    protected void updateComponents() {
        WebSecurityHelperImpl.setWebAppSecurityConfig(webAppSecConfig);
        SSOCookieHelper ssoCookieHelper = webAppSecConfig.createSSOCookieHelper();
        authenticateApi = authenticatorFactory.createAuthenticateApi(ssoCookieHelper, securityServiceRef, collabUtils, webAuthenticatorRef, unprotectedResourceServiceRef,
                                                                     unauthenticatedSubjectService, ssoAuthFilterRef);
        postParameterHelper = new PostParameterHelper(webAppSecConfig);
        providerAuthenticatorProxy = authenticatorFactory.createWebProviderAuthenticatorProxy(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecConfig,
                                                                                              webAuthenticatorRef, ssoAuthFilterRef);
        authenticatorProxy = authenticatorFactory.createWebAuthenticatorProxy(webAppSecConfig, postParameterHelper, securityServiceRef, providerAuthenticatorProxy);
    }

    protected void modified(Map<String, Object> newProperties) {
        WebAppSecurityConfig newWebAppSecConfig = authenticatorFactory.createWebAppSecurityConfigImpl(newProperties, locationAdminRef, securityServiceRef);
        // Capture the properties that were changed for our audit record
        Map<String, String> deltaMap = newWebAppSecConfig.getChangedPropertiesMap(webAppSecConfig);
        String deltaString = toStringFormChangedPropertiesMap(deltaMap);

        currentProps.clear();
        if (newProperties != null) {
            currentProps.putAll(newProperties);
        }
        webAppSecConfig = newWebAppSecConfig;
        updateComponents();
        if (deltaMap != null) {
            notifyWebAppSecurityConfigChangeListeners(new ArrayList<String>(deltaMap.keySet()));
        }
        Tr.audit(tc, "WEB_APP_SECURITY_CONFIGURATION_UPDATED", deltaString);
    }

    protected void deactivate(ComponentContext cc) {
        isActive = false;
        locationAdminRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
        ssoAuthFilterRef.deactivate(cc);
        taiServiceRef.deactivate(cc);
        interceptorServiceRef.deactivate(cc);
        webJaccServiceRef.deactivate(cc);
        webAuthenticatorRef.deactivate(cc);
        unprotectedResourceServiceRef.deactivate(cc);
        webAppSecurityConfigchangeListenerRef.deactivate(cc);
        WebSecurityHelperImpl.setWebAppSecurityConfig(null);
    }

    @Override
    public ExtensionProcessor getFormLoginExtensionProcessor(IServletContext webapp) {
        try {
            SecurityService securityService = securityServiceRef.getService();
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            UserRegistry userRegistry = null;

            if (userRegistryService.isUserRegistryConfigured())
                userRegistry = userRegistryService.getUserRegistry();

            return new FormLoginExtensionProcessor(webAppSecConfig, securityService.getAuthenticationService(), userRegistry, webapp, providerAuthenticatorProxy, webAuthenticatorRef);
        } catch (RegistryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegistryException while trying to create FormLoginExtensionProcessor", e);
            }
        }
        return null;
    }

    @Override
    public ExtensionProcessor getFormLogoutExtensionProcessor(IServletContext webapp) {
        return new FormLogoutExtensionProcessor(webapp, webAppSecConfig, getAuthenticateApi());
    }

    /**
     * public java.security.Principal getUserPrincipal()
     * Returns a java.security.Principal object containing the name of the current authenticated user. If the user has not been authenticated, the method returns null.
     * Returns:
     * a java.security.Principal containing the name of the user making this request; null if the user has not been authenticated{@inheritDoc}
     *
     * Look at the Subject on the thread only.
     * We will extract, from the set of Principals, our WSPrincipal type.
     */
    @Override
    public Principal getUserPrincipal() {
        if (System.getSecurityManager() == null) {
            return collabUtils.getCallerPrincipal(false, null, true, isJaspiEnabled);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<Principal>() {

                @Override
                public Principal run() {
                    return collabUtils.getCallerPrincipal(false, null, true, isJaspiEnabled);
                }
            });
        }
    }

    /**
     * Handle exceptions raised from <code>preInvoke</code> while
     * performing a servlet dispatch.
     */
    @Override
    public void handleException(HttpServletRequest req, HttpServletResponse rsp, Throwable wse) throws ServletException, IOException, ClassCastException {
        WebReply reply = ((WebSecurityCollaboratorException) (wse)).getWebReply();
        int sc = reply.getStatusCode();

        // Throw an exception if an error
        if (sc == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            String method = null;
            if (req != null) {
                method = req.getMethod();
            }
            String url = getRequestURL(req);

            final ServletException ex = new ServletException(TraceNLS.getFormattedMessage(
                                                                                          this.getClass(),
                                                                                          TraceConstants.MESSAGE_BUNDLE,
                                                                                          "SEC_WEB_INTERNAL_SERVER_ERROR",
                                                                                          new Object[] { method, url },
                                                                                          "CWWKS9115E: The server encountered an unexpected condition which prevented it from fulfilling the request of method {0} for URL {1}. Review the server logs for more information."), wse);
            throw ex;
        }
        reply.writeResponse(rsp);
    }

    @Override
    public boolean isCDINeeded() {
        /*
         * Tried to future-proof this check by iterating over all the
         * installed features and finding the version of appSecurity installed,
         * but that introduced a performance degradation. So for now, checking
         * for the appSecurity-3.0/4.0/5.0 features directly.
         */
        if (WebContainer.getServletContainerSpecLevel() < WebContainer.SPEC_LEVEL_40) {
            return false;
        }
        Set<String> features = provisionerService.getInstalledFeatures();
        return features.contains("appSecurity-3.0") || features.contains("appSecurity-4.0") || features.contains("appSecurity-5.0") || features.contains("appSecurity-6.0")
               || features.contains("mpJwt-2.1");
    }

    /**
     * {@inheritDoc} If the role is null or the call is unauthenticated,
     * return false. Otherwise, check if the authenticated subject is in
     * the requested role.
     */
    @Override
    public boolean isUserInRole(String role, IExtendedRequest req) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isUserInRole role = " + role);
        }

        if (role == null)
            return false;
        Subject subject = subjectManager.getCallerSubject();
        if (subject == null) {
            return false;
        }

        return wasch.isUserInRole(role, req, subject);

    }

    /**
     * Restore the security context to what it looked like before the servlet
     * dispatch. This method is always called if <code>preInovke</code> was
     * called.
     *
     * @param secObject the cookie returned from preInvoke. This
     *            may be null if <code>preInvoke</code> exited with an
     *            exception or if no work was done.
     */
    @Override
    public void postInvokeForSecureResponse(Object secObject) throws ServletException {
        try {
            if (webJaccServiceRef.getService() != null) {
                webJaccServiceRef.getService().resetPolicyContextHandlerInfo();
            }

            if (secObject != null) {
                WebSecurityContext webSecurityContext = (WebSecurityContext) secObject;
                if (webSecurityContext.getJaspiAuthContext() != null &&
                    webAuthenticatorRef != null) {
                    WebAuthenticator jaspiService = webAuthenticatorRef.getService(JASPI_SERVICE_COMPONENT_NAME);
                    if (jaspiService != null) {
                        try {
                            ((JaspiService) jaspiService).postInvoke(webSecurityContext);
                        } catch (Exception e) {
                            throw new ServletException(e);
                        }
                    }
                }
            }
        } finally {
            if (secObject != null) {
                removeModuleMetaDataFromThreadLocal(secObject);
            }
        }
    }

    @Override
    public void postInvoke(Object secObject) throws ServletException {
        if (secObject != null) {
            WebSecurityContext webSecurityContext = (WebSecurityContext) secObject;
            Subject invokedSubject = webSecurityContext.getInvokedSubject();
            Subject receivedSubject = webSecurityContext.getReceivedSubject();

            subjectManager.setCallerSubject(receivedSubject);
            subjectManager.setInvocationSubject(invokedSubject);

            try {
                resetSyncToOSThread(webSecurityContext);

            } catch (ThreadIdentityException e) {
                throw new ServletException(e);
            }
        }
    }

    /**
     * This preInvoke is called for every request and when processing
     * AsyncErrorHandling. It is also called (passing in false for
     * <code>enforceSecurity</code>) for static files to check whether
     * the user has access to the file on the zOS file system.
     * <p>
     * preInvoke also handles runAs delegation.
     * <p> {@inheritDoc}
     */
    @Override
    public Object preInvoke(HttpServletRequest req, HttpServletResponse resp, String servletName, boolean enforceSecurity) throws SecurityViolationException, IOException {

        Subject invokedSubject = subjectManager.getInvocationSubject();
        Subject receivedSubject = subjectManager.getCallerSubject();

        WebSecurityContext webSecurityContext = new WebSecurityContext(invokedSubject, receivedSubject);
        setUnauthenticatedSubjectIfNeeded(invokedSubject, receivedSubject);

        if (req != null) {
            SRTServletRequestUtils.setPrivateAttribute(req, SECURITY_CONTEXT, webSecurityContext);
        }

        if (enforceSecurity) {
            // Authentication and authorization are not required
            // for servlet init or destroy and, per spec, should
            // not be done for forward or include paths.
            SecurityMetadata securityMetadata;
            if (req != null) {
                setModuleMetaDataToThreadLocal(webSecurityContext);
                securityMetadata = getSecurityMetadata();
                performSecurityChecks(req, resp, receivedSubject, webSecurityContext, securityMetadata);
            } else {
                securityMetadata = getSecurityMetadata();
            }

            //auditManager.setHttpServletRequest(req);

            performDelegation(req, servletName, securityMetadata);

            syncToOSThread(webSecurityContext);
        }

        return webSecurityContext;
    }

    /**
     * Sync the invocation Subject's identity to the thread, if request by the application.
     *
     * @param WebSecurityContext The security context object for this application invocation.
     *            MUST NOT BE NULL.
     * @throws SecurityViolationException
     */
    private void syncToOSThread(WebSecurityContext webSecurityContext) throws SecurityViolationException {
        try {
            Object token = ThreadIdentityManager.setAppThreadIdentity(subjectManager.getInvocationSubject());
            webSecurityContext.setSyncToOSThreadToken(token);
        } catch (ThreadIdentityException tie) {
            SecurityViolationException secVE = convertWebSecurityException(new WebSecurityCollaboratorException(tie.getMessage(), DENY_AUTHZ_FAILED, webSecurityContext));
            throw secVE;
        }
    }

    /**
     * Remove the invocation Subject's identity from the thread, if it was previously sync'ed.
     *
     * @param WebSecurityContext The security context object for this application invocation.
     *            MUST NOT BE NULL.
     * @throws ThreadIdentityException
     */
    private void resetSyncToOSThread(WebSecurityContext webSecurityContext) throws ThreadIdentityException {
        Object token = webSecurityContext.getSyncToOSThreadToken();
        if (token != null) {
            ThreadIdentityManager.resetChecked(token);
        }
    }

    private void performSecurityChecks(HttpServletRequest req, HttpServletResponse resp, Subject receivedSubject,
                                       WebSecurityContext webSecurityContext, SecurityMetadata securityMetadata) throws SecurityViolationException, IOException {
        String uriName = new URLHandler(webAppSecConfig).getServletURI(req);

        savedSubject = receivedSubject;

        MatchResponse matchResponse = getMatchResponse(req, uriName, securityMetadata);
        WebRequest webRequest = new WebRequestImpl(req, resp, getApplicationName(), webSecurityContext, securityMetadata, matchResponse, webAppSecConfig);

        WebReply webReply = null;

        /**
         * JASPI spec requires that we check JASPI for every request, protected or
         * unprotected, SSO token or no SSO token, etc. which means we can't do our
         * normal processing.
         */
        if (isJaspiEnabled &&
            ((JaspiService) webAuthenticatorRef.getService(JASPI_SERVICE_COMPONENT_NAME)).isAnyProviderRegistered(webRequest)) {
            webReply = handleJaspi(receivedSubject, uriName, webRequest, webSecurityContext);
        }

        /**
         * If JASPI is not enabled or no JASPI provider handled the request
         * we do our normal processing
         */
        if (webReply == null) {
            performPrecludedAccessTests(webRequest, webSecurityContext, uriName);
            webReply = determineWebReply(receivedSubject, uriName, webRequest);

        }
        validateWebReply(webSecurityContext, webReply);

        webReply.writeResponse(resp);
    }

    /**
     * @param webRequest
     * @return
     */
    private WebReply handleJaspi(Subject receivedSubject,
                                 String uriName,
                                 WebRequest webRequest,
                                 WebSecurityContext webSecurityContext) throws SecurityViolationException, IOException {
        WebReply webReply = null;
        HttpServletRequest req = webRequest.getHttpServletRequest();
        if (wasch.isSSLRequired(webRequest, uriName)) {
            webReply = httpsRedirectHandler.getHTTPSRedirectWebReply(req);
            AuthenticationResult authResult = new AuthenticationResult(AuthResult.FAILURE, receivedSubject, AuditEvent.CRED_TYPE_JASPIC, null, AuditEvent.OUTCOME_FAILURE);
            Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(webReply.getStatusCode()));
            return webReply;
        }

        webReply = unprotectedSpecialURI(webRequest, uriName, req);
        if (webReply != null) {
            logAuditEntriesBeforeAuthn(webReply, receivedSubject, uriName, webRequest);
        } else {
            performPrecludedAccessTests(webRequest, webSecurityContext, uriName);
            // check global cert login.
            String authMech = webAppSecConfig.getOverrideHttpAuthMethod();
            AuthenticationResult authResult = null;
            if (authMech != null && authMech.equals("CLIENT_CERT")) {
                // process client cert auth first.
                // disable failover in order to avoid failover to non-jaspi code path.
                webRequest.setDisableClientCertFailOver(true);
                // client cert.
                authResult = authenticateRequest(webRequest);
                // reset the value.
                webRequest.setDisableClientCertFailOver(false);
            }
            boolean isUnprotected = false;
            if (authResult == null || (authResult.getStatus() != AuthResult.SUCCESS && webAppSecConfig.allowFailOver())) {
                // if client cert is not processed or failed and allowFailOver is configured.
                // set unprotected flag if the target url is not protected or assigned to everyone role.
                isUnprotected = (unprotectedResource(webRequest) == PERMIT_REPLY);
                authResult = providerAuthenticatorProxy.handleJaspi(webRequest, null);
                authResult.setAuditCredType(AuditEvent.CRED_TYPE_JASPIC);
                if (receivedSubject != null && receivedSubject.getPrincipals() != null) {
                    authResult.setAuditCredValue(receivedSubject.getPrincipals().iterator().next().getName());
                }
            }
            if (authResult.getStatus() == AuthResult.RETURN) {
                //
                // AuthResult.RETURN means return to the user agent with the response as is.
                // Do not invoke the target servlet. Do not set anything in the response as
                // the response may be committed.
                //
                String reason = authResult.getReason();
                int statusCode = webRequest.getHttpServletResponse().getStatus();
                if (reason != null && reason.contains("SEND_FAILURE")) {
                    // isUnprotected is only set when handleJaspi is invoked, but it's ok since AuthResult.RETURN is only set
                    // by the JASPIC code.
                    if (isUnprotected) {
                        AuthenticationResult permitResult = new AuthenticationResult(AuthResult.SUCCESS, (Subject) null, AuditEvent.CRED_TYPE_JASPIC, null, AuditEvent.OUTCOME_SUCCESS);
                        Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(statusCode));
                        Audit.audit(Audit.EventID.SECURITY_AUTHZ_01, webRequest, permitResult, uriName, Integer.valueOf(HttpServletResponse.SC_OK));
                        return PERMIT_REPLY;
                    } else if (statusCode == HttpServletResponse.SC_OK) {
                        // SEND_FAILURE but did not set the response code or set the wrong response code.
                        // We have to overwrite it to 401
                        statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                        webRequest.getHttpServletResponse().setStatus(statusCode);
                    }
                }
                webReply = new ReturnReply(statusCode, reason);
                Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(webReply.getStatusCode()));
                SecurityViolationException secVE = convertWebSecurityException(new WebSecurityCollaboratorException(webReply.message, webReply, webSecurityContext));
                throw secVE;

            } else if (authResult.getStatus() != AuthResult.CONTINUE) {
                webReply = determineWebReply(receivedSubject, uriName, webRequest, authResult);
            }
        }
        return webReply;
    }

    /**
     * Access precluded explicitly specified in the security constraints or
     * by the usage of the com.ibm.ws.webcontainer.security.checkdefaultmethod attribute.
     * This attribute is set by the web container in ServletWrapper.startRequest() with the
     * intent of blocking hackers from crafting an HTTP HEAD method to bypass security constraints.
     * See PK83258 for full details.
     * This method also checks for uncovered HTTP methods
     */
    @FFDCIgnore(SecurityViolationException.class)
    private void performPrecludedAccessTests(WebRequest webRequest, WebSecurityContext webSecurityContext, String uriName) throws SecurityViolationException {
        WebReply webReply = null;

        webReply = wasch.checkPrecludedAccess(webRequest, uriName);
        if (webReply != null) {
            try {
                validateWebReply(webSecurityContext, webReply);
            } catch (SecurityViolationException secVE) {
                AuthenticationResult authResult = new AuthenticationResult(AuthResult.FAILURE, savedSubject, AuditEvent.CRED_TYPE_BASIC, null, AuditEvent.OUTCOME_FAILURE);
                Audit.audit(Audit.EventID.SECURITY_AUTHZ_01, webRequest, authResult, uriName, Integer.valueOf(webReply.getStatusCode()));
                throw secVE;
            }

        }
    }

    private void validateWebReply(WebSecurityContext webSecurityContext, WebReply webReply) throws SecurityViolationException {
        if (webReply.getStatusCode() != HttpServletResponse.SC_OK) {
            SecurityViolationException secVE = convertWebSecurityException(new WebSecurityCollaboratorException(webReply.message, webReply, webSecurityContext));
            throw secVE;
        }
    }

    /**
     * Authenticate the request if the following conditions are met:
     * 1. Persist cred is enabled (a.k.a. use authentication data for unprotected resource)
     * 2. We have authentication data
     * 3. The resource is unprotected (if its protected, will authenticate after this)
     *
     * @param webRequest
     */
    public AuthenticationResult optionallyAuthenticateUnprotectedResource(WebRequest webRequest) {
        AuthenticationResult authResult = null;
        if (webAppSecConfig.isUseAuthenticationDataForUnprotectedResourceEnabled() &&
            (unprotectedResource(webRequest) == PERMIT_REPLY) &&
            needToAuthenticateSubject(webRequest)) {
            webRequest.disableFormLoginRedirect();
            authResult = setAuthenticatedSubjectIfNeeded(webRequest);
        }
        return authResult;
    }

    protected boolean needToAuthenticateSubject(WebRequest webRequest) {
        Boolean result = authenticatorFactory.needToAuthenticateSubject(webRequest);
        if (result != null) {
            return result.booleanValue();
        }
        if (webRequest.hasAuthenticationData())
            return true;
        return isUnprotectedResourceAuthenRequired(webRequest);
    }

    private WebReply unprotectedResource(WebRequest webRequest) {
        WebReply webReply = null;
        // per the spec: if there are no required roles, then we simply permit the
        // request without even authenticating the user.
        List<String> requiredRoles = webRequest.getRequiredRoles();
        if (requiredRoles.isEmpty()) {
            webRequest.setUnprotectedURI(true);
            return PERMIT_REPLY;
        }

        SecurityService securityService = securityServiceRef.getService();
        AuthorizationService authzService = securityService.getAuthorizationService();
        if (authzService == null) {
            // If we can not get the authorization service, fail securely
            return new DenyReply("An internal error occured. Unable to perform authorization check.");
        } else {
            boolean everyoneAllowed = authzService.isEveryoneGranted(webRequest.getApplicationName(), requiredRoles);
            // per the spec: if there are required roles but at least one of them has been granted to "Everyone"
            // then the resource is considered unprotected so permit without even authenticating the user
            if (everyoneAllowed) {
                webRequest.setUnprotectedURI(true);
                return PERMIT_REPLY;
            }
        }
        return webReply;
    }

    public AuthenticationResult setAuthenticatedSubjectIfNeeded(WebRequest webRequest) {
        AuthenticationResult authResult = authenticateRequest(webRequest);
        if (authResult != null && authResult.getStatus() == AuthResult.SUCCESS) {
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setCallerSubject(authResult.getSubject());
        }
        return authResult;
    }

    private void performDelegationAudit(HttpServletRequest req, Subject callerSubject, String roleName, Subject delegationSubject, boolean success,
                                        AuthenticationService authService) {
        String outcome = success ? AuditConstants.SUCCESS : AuditConstants.FAILURE;

        // if HttpRequest is null, the audit does nothing, so don't call it if null
        if (req == null || !Audit.isAuditRequired(Audit.EventID.SECURITY_AUTHN_DELEGATION_01, outcome)) {
            return;
        }

        HashMap<String, Object> extraAuditData = new HashMap<String, Object>();

        if (req != null) {
            extraAuditData.put("HTTP_SERVLET_REQUEST", req);
        }

        Set<WSCredential> publicCredentials = (callerSubject == null ? null : callerSubject.getPublicCredentials(WSCredential.class));
        Iterator<WSCredential> it = null;
        if (publicCredentials != null && (it = publicCredentials.iterator()) != null && it.hasNext()) {
            WSCredential credential = it.next();
            try {
                extraAuditData.put("REALM", credential.getRealmName());
            } catch (CredentialExpiredException e) {
            } catch (CredentialDestroyedException e) {
            }
        }

        ArrayList<String> delUsers = new ArrayList<String>();
        if (callerSubject != null) {
            String buff = getSubjectToString(callerSubject);
            if (buff != null) {
                int a = buff.indexOf("accessId");
                if (a != -1) {
                    buff = buff.substring(a + 9);
                    a = buff.indexOf(",");
                    if (a != -1) {
                        buff = buff.substring(0, a);
                        delUsers.add(buff);
                    }
                }

            }
        }

        extraAuditData.put("RUN_AS_ROLE", roleName);
        if (delegationSubject == null) {
            String invalidUser = authService.getInvalidDelegationUser();
            delUsers.add(invalidUser);
        } else if (delegationSubject != callerSubject) {
            String buff = getSubjectToString(delegationSubject);
            if (buff != null) {
                int a = buff.indexOf("accessId");
                if (a != -1) {
                    buff = buff.substring(a + 9);
                    a = buff.indexOf(",");
                    if (a != -1) {
                        buff = buff.substring(0, a);
                        delUsers.add(buff);
                    }
                }

            }
        }

        extraAuditData.put("DELEGATION_USERS_LIST", delUsers);

        Audit.audit(Audit.EventID.SECURITY_AUTHN_DELEGATION_01, extraAuditData, outcome, success ? Integer.valueOf(200) : Integer.valueOf(401));
    }

    private void performDelegation(HttpServletRequest req, String servletName, SecurityMetadata secMetadata) {

        Subject callerSubject = subjectManager.getCallerSubject();

        String roleName = secMetadata == null ? null : secMetadata.getRunAsRoleForServlet(servletName);

        Subject delegationSubject = callerSubject;
        if (roleName != null) {
            SecurityService securityService = securityServiceRef.getService();
            AuthenticationService authService = securityService.getAuthenticationService();
            boolean success;
            try {
                delegationSubject = authService.delegate(roleName, getApplicationName());
                success = delegationSubject != null;
            } catch (IllegalArgumentException e) {
                success = false;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception performing delegation.", e);
                }
            }
            performDelegationAudit(req, callerSubject, roleName, delegationSubject, success, authService);
        }
        if (delegationSubject != null) {
            subjectManager.setInvocationSubject(delegationSubject);
        }
    }

    /**
     * This method does:
     * 1. pre-authentication checks
     * 2. Authentication
     * 3. Authorization
     *
     * @param receivedSubject
     * @param uriName
     * @param webRequest
     * @return Non-null WebReply
     */
    public WebReply determineWebReply(Subject receivedSubject, String uriName, WebRequest webRequest) {

        AuthenticationResult authResult = optionallyAuthenticateUnprotectedResource(webRequest);
        WebReply webReply = performInitialChecks(webRequest, uriName);
        if (webReply != null) {
            logAuditEntriesBeforeAuthn(webReply, receivedSubject, uriName, webRequest);
            return webReply;
        }
        if (authResult == null) {
            authResult = authenticateRequest(webRequest);
        }
        return determineWebReply(receivedSubject, uriName, webRequest, authResult);
    }

    /**
     *
     * @param receivedSubject
     * @param uriName
     * @param webRequest
     * @param authResult
     * @return
     */
    private WebReply determineWebReply(Subject receivedSubject,
                                       String uriName,
                                       WebRequest webRequest,
                                       AuthenticationResult authResult) {
        WebReply reply = null;
        if (authResult != null && authResult.getStatus() != AuthResult.SUCCESS) {
            String realm = authResult.realm;
            if (realm == null) {
                realm = collabUtils.getUserRegistryRealm(securityServiceRef);
            }
            reply = createReplyForAuthnFailure(authResult, realm);
            authResult.setTargetRealm(authResult.realm != null ? authResult.realm : collabUtils.getUserRegistryRealm(securityServiceRef));
            Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(reply.getStatusCode()));
            return reply;
        }
        boolean isAuthorized = false;

        if (authResult != null) {
            authResult.setTargetRealm(authResult.realm != null ? authResult.realm : collabUtils.getUserRegistryRealm(securityServiceRef));
            subjectManager.setCallerSubject(authResult.getSubject());
            Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, Integer.valueOf(HttpServletResponse.SC_OK));
            isAuthorized = wasch.authorize(authResult, webRequest, uriName);
        }
        // For audit set reply now but leave Subject on thread
        reply = isAuthorized ? new PermitReply() : DENY_AUTHZ_FAILED;

        auditManager.setWebRequest(webRequest);
        if (authResult != null) {
            auditManager.setRealm(authResult.getTargetRealm());
        }

        Audit.audit(Audit.EventID.SECURITY_AUTHZ_01, webRequest, authResult, uriName, Integer.valueOf(reply.getStatusCode()));
        // now update current thread context
        if (isAuthorized) {
            // at this point set invocation subject = caller subject.
            // delegation may change the invocation subject later
            if (authResult != null) {
                subjectManager.setInvocationSubject(authResult.getSubject());
            }
        } else {
            // if authorization failure, put the caller subject back to the original one.
            subjectManager.setCallerSubject(receivedSubject);
        }
        return reply;
    }

    /**
     * This preInvoke is called during init & during destroy of a Servlet class object.
     * It will call the other preInvoke to ensure delegation occurs. {@inheritDoc}
     */
    @Override
    public Object preInvoke(String servletName) throws SecurityViolationException, IOException {
        // preInvoke will ensure delegation is done when run-as is specified
        return preInvoke(null, null, servletName, true);
    }

    /**
     * This preInvoke is called at the start of every request in
     * WebContainer.handleRequest to null out the thread. {@inheritDoc}
     */
    @Override
    public Object preInvoke() throws SecurityViolationException {
        Subject invokedSubject = subjectManager.getInvocationSubject();
        Subject receivedSubject = subjectManager.getCallerSubject();

        subjectManager.clearSubjects();

        return null;
    }

    @Override
    public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JaspiService jaspiService = null;
        boolean isNewAuthenticate = false;
        boolean isCredentialPresent = false;
        if (isJaspiEnabled) {
            jaspiService = (JaspiService) webAuthenticatorRef.getService(JASPI_SERVICE_COMPONENT_NAME);
            isNewAuthenticate = jaspiService.isProcessingNewAuthentication(req);
            isCredentialPresent = jaspiService.isCredentialPresent(req);
        }

        // if JSR-375 HttpAuthenticationMechanism is not enabled, and if there is a valid subject in the context, return it.
        if (!isNewAuthenticate && isAlreadyAuthenticated()) {
            return true;
        }

        WebReply webReply = PERMIT_REPLY;
        boolean result = true;
        WebRequest webRequest = new WebRequestImpl(req, resp, getSecurityMetadata(), webAppSecConfig);
        webRequest.setRequestAuthenticate(true);
        AuthenticationResult authResult = null;

        if (isJaspiEnabled && jaspiService.isAnyProviderRegistered(webRequest)) {
            authResult = providerAuthenticatorProxy.handleJaspi(webRequest, null);
        }
        if (authResult == null || authResult.getStatus() == AuthResult.CONTINUE) {
            authResult = authenticateRequest(webRequest);
        }
        if (authResult.getStatus() == AuthResult.SUCCESS) {
            boolean isPostLoginProcessDone = isPostLoginProcessDone(req);
            getAuthenticateApi().postProgrammaticAuthenticate(req, resp, authResult, !isPostLoginProcessDone, !isNewAuthenticate && !isPostLoginProcessDone);
        } else {
            result = false;
            if (!isCredentialPresent) {
                String realm = authResult.realm != null ? authResult.realm : collabUtils.getUserRegistryRealm(securityServiceRef);
                authResult.setTargetRealm(realm);
                webReply = createReplyForAuthnFailure(authResult, realm);
            }
        }

        if (!isCredentialPresent && !resp.isCommitted() && webReply != null) {
            webReply.writeResponse(resp);
        }

        int statusCode = webReply != null ? Integer.valueOf(webReply.getStatusCode()) : resp.getStatus();
        Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, statusCode);
        return result;
    }

    private boolean isAlreadyAuthenticated() {
        if (System.getSecurityManager() == null) {
            return isCallerSubjectAuthenticated();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

                @Override
                public Boolean run() {
                    return isCallerSubjectAuthenticated();
                }
            });
        }
    }

    private Boolean isCallerSubjectAuthenticated() {
        Subject callerSubject = subjectManager.getCallerSubject();
        if (!subjectHelper.isUnauthenticated(callerSubject)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "The underlying login mechanism has committed");
            return true;
        }
        return false;
    }

    @Override
    public List<String> getURIsInSecurityConstraints(String appName, String contextRoot, String vHost, List<String> urlPatternsInAnnotation) {
        SecurityMetadata ddMetaData = getSecurityMetadata();
        //need to return null if there are no conflicts, not an empty list
        List<String> urlPatternConflicts = null;
        for (String urlPatternFromAnno : urlPatternsInAnnotation) {
            SecurityConstraintCollection securityConstraintCollection = ddMetaData.getSecurityConstraintCollection();
            if (securityConstraintCollection != null) {
                List<SecurityConstraint> securityConstraints = securityConstraintCollection.getSecurityConstraints();
                for (SecurityConstraint securityConstraint : securityConstraints) {
                    for (WebResourceCollection webResourceColl : securityConstraint.getWebResourceCollections()) {
                        List<String> urlPatternsInDD = webResourceColl.getUrlPatterns();
                        if (urlPatternsInDD.contains(urlPatternFromAnno)) {
                            if (urlPatternConflicts == null) {
                                urlPatternConflicts = new ArrayList<String>();
                            }
                            urlPatternConflicts.add(urlPatternFromAnno);
                        }
                    }
                }
            }
        }
        return urlPatternConflicts;
    }

    @Override
    public void login(HttpServletRequest req, HttpServletResponse resp, String username, @Sensitive String password) throws ServletException {
        BasicAuthAuthenticator basicAuthAuthenticator = getBasicAuthAuthenticator();
        if (basicAuthAuthenticator == null) {
            String url = getRequestURL(req);
            throw new ServletException(TraceNLS.getFormattedMessage(
                                                                    this.getClass(),
                                                                    TraceConstants.MESSAGE_BUNDLE,
                                                                    "SEC_WEB_NULL_AUTHENTICATOR",
                                                                    new Object[] { url, username },
                                                                    "CWWKS9116E: Login to the URL {0} failed for user {1} due to an internal error. Review the server logs for more information."));

        }
        getAuthenticateApi().login(req, resp, username, password, webAppSecConfig, basicAuthAuthenticator);
        String authType = getSecurityMetadata().getLoginConfiguration().getAuthenticationMethod();
        SRTServletRequestUtils.setPrivateAttribute(req, AUTH_TYPE, authType);

    }

    @Override
    public void logout(HttpServletRequest res, HttpServletResponse resp) throws ServletException {
        getAuthenticateApi().logoutServlet30(res, resp, webAppSecConfig);
    }

    private SecurityViolationException convertWebSecurityException(WebSecurityCollaboratorException e) {
        int sc = HttpServletResponse.SC_FORBIDDEN;
        WebReply wr = e.getWebReply();
        if (wr != null) {
            sc = wr.getStatusCode();
        }
        SecurityViolationException secVE = new SecurityViolationException(e.getMessage(), sc);
        secVE.initCause(e);
        secVE.setWebSecurityContext(e.getWebSecurityContext());
        return secVE;
    }

    /**
     * If invoked and received cred are null, then set the unauthenticated subject.
     *
     * @param invokedSubject
     * @param receivedSubject
     * @return {@code true} if the unauthenticated subject was set, {@code false} otherwise.
     */
    private boolean setUnauthenticatedSubjectIfNeeded(Subject invokedSubject, Subject receivedSubject) {
        if ((invokedSubject == null) && (receivedSubject == null)) {
            // create the unauthenticated subject and set as the invocation subject
            SubjectManager sm = new SubjectManager();
            sm.setInvocationSubject(unauthenticatedSubjectService.getUnauthenticatedSubject());
            return true;
        }
        return false;
    }

    /**
     * The main method called by the preInvoke. The return value of this method tells
     * us if access to the requested resource is allowed or not.
     * Delegates to the authenticator proxy to handle the authentication.
     *
     * @param webRequest
     * @return AuthenticationResult
     */
    public AuthenticationResult authenticateRequest(WebRequest webRequest) {
        WebAuthenticator authenticator = getWebAuthenticatorProxy();
        return authenticator.authenticate(webRequest);
    }

    protected WebAuthenticatorProxy getWebAuthenticatorProxy() {
        return authenticatorProxy;
    }

    /**
     * Create an instance of BasicAuthAuthenticator.
     *
     * @return A BasicAuthAuthenticator or {@code null} if the it could not be created.
     */
    public BasicAuthAuthenticator getBasicAuthAuthenticator() {
        WebAuthenticatorProxy authenticatorProxy = getWebAuthenticatorProxy();
        return authenticatorProxy.getBasicAuthAuthenticator();
    }

    public WebReply createReplyForAuthnFailure(AuthenticationResult authResult, String realm) {
        return getAuthenticateApi().createReplyForAuthnFailure(authResult, realm);
    }

    /**
     * Call the authorization service to determine if the subject is authorized to the given roles
     *
     * @param authResult the authentication result, containing the subject, user name and realm
     * @param appName the name of the application, used to look up the correct authorization table
     * @param uriName the uri being accessed
     * @param previousCaller the previous caller, used to restore the previous state if authorization fails
     * @param requiredRoles the roles required to access the resource
     * @return true if the subject is authorized, otherwise false
     */
    public boolean authorize(AuthenticationResult authResult, String appName, String uriName, Subject previousCaller, List<String> requiredRoles) {
        // Set the authorized subject on the thread
        subjectManager.setCallerSubject(authResult.getSubject());
        boolean isAuthorized = authorize(authResult, appName, uriName, requiredRoles);
        if (isAuthorized) {
            // at this point set invocation subject = caller subject.
            // delegation may change the invocation subject later
            subjectManager.setInvocationSubject(authResult.getSubject());
        } else {
            subjectManager.setCallerSubject(previousCaller);
        }
        return isAuthorized;
    }

    private boolean authorize(AuthenticationResult authResult, String appName, String uriName, List<String> requiredRoles) {

        SecurityService securityService = securityServiceRef.getService();
        if (securityService == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Authorization failed due to null securityService object. Known to occur when a request comes during server shutdown.");
            }
            return false;
        }
        AuthorizationService authzService = securityService.getAuthorizationService();
        if (authzService == null) {
            // If we can not get the authorization service, fail securely
            return false;
        } else {
            boolean isAuthorized = authzService.isAuthorized(appName, requiredRoles, authResult.getSubject());
            if (!isAuthorized) {
                String authUserName = authResult.getUserName();
                String authRealm = authResult.getRealm();
                if (authRealm != null && authUserName != null) {
                    Tr.audit(tc, "SEC_AUTHZ_FAILED", authUserName.concat(":").concat(authRealm), appName, uriName, requiredRoles);
                } else {
                    // We have a subject if we got this far, use it to determine the name
                    authUserName = authResult.getSubject().getPrincipals(WSPrincipal.class).iterator().next().getName();
                    Tr.audit(tc, "SEC_AUTHZ_FAILED", authUserName, appName, uriName, requiredRoles);
                }
            }
            return isAuthorized;
        }
    }

    /**
     * Perform the preliminary checks to see if we should proceed to authentication
     * and authorization.
     *
     * TODO: add authn/autz auditing for each of these cases
     *
     * These checks are, in order:
     * <ol>
     * <li>when uriName is null or empty, then return Deny</li>
     * <li>if the challenge typs is DIGEST, then return Deny</li>
     * <li>if the required roles is specified but empty, then return Deny</li>
     * <li>if SSL is required but the request is not HTTPS, then return Redirect</li>
     * <li>if the method is TRACE and there are no required roles, then return Deny</li>
     * <li>if the security constraints in the dd are invalid, then return Deny</li>
     * <li>if there are no required roles, then return Permit</li>
     * <li>if Everyone is allowed, then return Permit</li>
     * </ol>
     *
     * @param req the servlet request
     * @param uriName the uri name of the request
     * @return a WebReply if the request should be returned without authenticating/authorizing, otherwise null
     */
    public WebReply performInitialChecks(WebRequest webRequest, String uriName) {
        WebReply webReply = null;

        if (uriName == null || uriName.length() == 0) {
            return new DenyReply("Invalid URI passed to Security Collaborator.");
        }

        if (unsupportedAuthMech(webRequest.getSecurityMetadata()) == true) {
            return new DenyReply("Authentication Failed : DIGEST not supported");
        }

        HttpServletRequest req = webRequest.getHttpServletRequest();
        if (wasch.isSSLRequired(webRequest, uriName)) {
            return httpsRedirectHandler.getHTTPSRedirectWebReply(req);
        }

        webReply = unprotectedSpecialURI(webRequest, uriName, req);
        if (webReply != null) {
            return webReply;
        }

        webReply = unprotectedResource(webRequest);
        if (webReply == PERMIT_REPLY) {
            if (shouldWePerformTAIForUnProtectedURI(webRequest))
                return null;
            else
                return webReply;
        }

        return null;
    }

    private boolean shouldWePerformTAIForUnProtectedURI(WebRequest webRequest) {
        if (taiServiceRef.getService() != null && taiServiceRef.getService().isInvokeForUnprotectedURI()) {
            webRequest.setPerformTAIForUnProtectedURI(true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return true when challenge type is DIGEST, otherwise false.
     */
    public boolean unsupportedAuthMech(SecurityMetadata sm) {
        boolean result = false;
        if (sm != null) {
            LoginConfiguration lc = sm.getLoginConfiguration();
            if (lc != null) {
                String authType = lc.getAuthenticationMethod();
                if (HttpServletRequest.DIGEST_AUTH.equalsIgnoreCase(authType)) {
                    result = true;
                }
            }
        }
        return result;
    }

    private MatchResponse getMatchResponse(HttpServletRequest req, String uriName, SecurityMetadata securityMetadata) throws SecurityViolationException {
        MatchResponse matchResponse = MatchResponse.NO_MATCH_RESPONSE;

        if (req != null) {
            SecurityConstraintCollection collection = securityMetadata.getSecurityConstraintCollection();
            if (null != collection) {
                String method = req.getMethod();
                matchResponse = collection.getMatchResponse(uriName, method);
                if (MatchResponse.CUSTOM_NO_MATCH_RESPONSE.equals(matchResponse)) {
                    String url = getRequestURL(req);
                    String formattedMessage = TraceNLS.getFormattedMessage(this.getClass(),
                                                                           TraceConstants.MESSAGE_BUNDLE,
                                                                           "SEC_WEB_ILLEGAL_REQUEST",
                                                                           new Object[] { method, url },
                                                                           "CWWKS9117E: The method {0} is not allowed to process for URL {1}. If this error is unexpected, ensure that the application allows the methods that the client is requesting.");
                    throw convertWebSecurityException(new WebSecurityCollaboratorException(formattedMessage, DENY_AUTHZ_FAILED));
                }
            }
        }
        return matchResponse;
    }

    protected String getApplicationName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
        return wmmd.getConfiguration().getApplicationName();
    }

    protected String getModuleName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
        return wmmd.getConfiguration().getModuleName();
    }

    public SecurityMetadata getSecurityMetadata() {
        return WebConfigUtils.getSecurityMetadata();
    }

    protected void setSecurityMetadata(SecurityMetadata secMetadata) {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
        wmmd.setSecurityMetaData(secMetadata);
    }

    /**
     * Determines if the authentication method is valid for a FORM login flow.
     * The valid contitions are:
     * 1. The authentication method is FORM
     * 2. The authentication method is CLIENT_CERT and allowFailOverToFormLogin=true
     *
     * @param authenticationMethod
     * @return {@code true} if the authentication method is valid for FORM login
     */
    private boolean isValidAuthMethodForFormLogin(String authenticationMethod) {
        return LoginConfiguration.FORM.equals(authenticationMethod) ||
               (LoginConfiguration.CLIENT_CERT.equals(authenticationMethod) && webAppSecConfig.getAllowFailOverToFormLogin());
    }

    /**
     * Determines if the URI requested is "special" and should always be treated
     * as unprotected, such as the form login page.
     *
     * @param webRequest
     * @param uriName
     * @param req
     * @return Non-null WebReply if the URI is not special, or a PERMIT_REPLY if it is.
     */
    private WebReply unprotectedSpecialURI(WebRequest webRequest, String uriName, HttpServletRequest req) {
        LoginConfiguration loginConfig = webRequest.getLoginConfig();
        if (loginConfig == null)
            return null;

        FormLoginConfiguration formLoginConfig = loginConfig.getFormLoginConfiguration();
        if (formLoginConfig == null)
            return null;

        String authenticationMethod = loginConfig.getAuthenticationMethod();
        if (authenticationMethod == null)
            return null;

        String loginPage = formLoginConfig.getLoginPage();
        String errorPage = formLoginConfig.getErrorPage();

        // We check to see if we are either a FORM or CLIENT_CERT auth method.
        // These are the only valid auth methods supported (CLIENT_CERT can
        // fail over to FORM).
        if (isValidAuthMethodForFormLogin(authenticationMethod) && loginPage != null && errorPage != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " We have a custom login or error page request, web app login URL:[" + loginPage
                             + "], errorPage URL:[" + errorPage + "], and the requested URI:[" + uriName + "]");
            }
            if (loginPage.equals(uriName) || errorPage.equals(uriName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "authorize, login or error page[" + uriName + "]  requested, permit: ", PERMIT_REPLY);
                return PERMIT_REPLY;
            } else if ((uriName != null && uriName.equals("/j_security_check")) &&
                       "POST".equals(req.getMethod())) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "authorize, login or error page[" + uriName + "]  requested, permit: ", PERMIT_REPLY);
                return PERMIT_REPLY;
            }
        } else {
            if (webRequest.getHttpServletRequest().getDispatcherType().equals(DispatcherType.ERROR)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "authorize, error page[" + uriName + "]  requested, permit: ", PERMIT_REPLY);
                return PERMIT_REPLY;
            }
        }
        return null;
    }

    protected String getRequestURL(HttpServletRequest req) {
        String url = null;
        if (req != null) {
            StringBuffer buf = req.getRequestURL();
            String query = req.getQueryString();
            if (query != null && query.length() > 0) {
                buf.append("?").append(query);
            }
            url = buf.toString();
        }
        return url;
    }

    public static void setGlobalWebAppSecurityConfig(WebAppSecurityConfig config) {
        globalConfig = config;
    }

    public static WebAppSecurityConfig getGlobalWebAppSecurityConfig() {
        return globalConfig;
    }

    public static WebAuthenticatorFactory getWebAuthenticatorFactory() {
        return globalWebAuthenticatorFactory;
    }

    protected AuthenticateApi getAuthenticateApi() {
        if (authenticateApi == null) {
            SSOCookieHelper ssoCookieHelper = webAppSecConfig.createSSOCookieHelper();
            authenticateApi = authenticatorFactory.createAuthenticateApi(ssoCookieHelper, securityServiceRef, collabUtils, webAuthenticatorRef, unprotectedResourceServiceRef,
                                                                         unauthenticatedSubjectService, ssoAuthFilterRef);
        }
        return authenticateApi;
    }

    /**
     * Check to see if running under servlet spec 3.1
     *
     * @return true if using servlet spec 3.1 and false otherwise
     */
    private boolean isServletSpec31() {
        if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31)
            return true;

        return false;
    }

    protected WebAppConfig getWebAppConfig() {
        WebAppConfig wac = null;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd instanceof WebComponentMetaData) { // Only get the header for web modules, i.e. not for EJB
            WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
            wac = wmmd.getConfiguration();
            if (!(wac instanceof com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration)) {
                wac = null;
            }
        }
        return wac;
    }

    @Override
    public boolean isUserInRole(String role, IExtendedRequest req, Subject subject) {
        RequestProcessor reqProc = req.getWebAppDispatcherContext().getCurrentServletReference();
        String realRole = null;
        if (reqProc != null) {
            String servletName = reqProc.getName();
            realRole = getSecurityMetadata().getSecurityRoleReferenced(servletName, role);
        } else {
            // PM98409 - when servlet reference is not available, use provided role name as real role
            realRole = role;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isUserInRole realRole = " + realRole);
        }

        /*
         * Servlet 3.1 defines a role name called "**" that means all authenticated, unless
         * "**" is already a defined role. If "**" is a defined role it will be returned by
         * getSecurityRoleReferenced and set to realRole. So if realRole is "**" change it
         * to "_starstar_" to be checked later as in isGranted.
         */
        if (realRole != null && realRole.equals(ALL_AUTHENTICATED_ROLE)) {
            realRole = STARSTAR_ROLE;
        }

        /*
         * So there is no role defined return false if the role name is not "**" otherwise
         * set realRole to "**" so it can proceed with authorization checking.
         */
        if (realRole == null) {
            if (role.equals(ALL_AUTHENTICATED_ROLE) && isServletSpec31()) {
                realRole = ALL_AUTHENTICATED_ROLE;
            } else {
                return false;
            }
        }

        final Subject finalSubject = subject;
        final List<String> roles = new ArrayList<String>();
        roles.add(realRole);
        final SecurityService securityService = securityServiceRef.getService();
        boolean inRole = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            @Override
            public Boolean run() {
                AuthorizationService authzService = securityService.getAuthorizationService();
                if (authzService == null) {
                    // If we can not get the authorization service, fail securely
                    return false;
                } else {
                    return authzService.isAuthorized(getApplicationName(), roles, finalSubject);
                }
            }
        });
        return inRole;
    }

    @Override
    public boolean authorize(AuthenticationResult authResult, WebRequest webRequest, String uriName) {
        return authorize(authResult, webRequest.getApplicationName(), uriName, webRequest.getRequiredRoles());
    }

    @Override
    public boolean isSSLRequired(WebRequest webRequest, String uriName) {
        return httpsRedirectHandler.shouldRedirectToHttps(webRequest);
    }

    /**
     * Access precluded explicitly specified in the security constraints or
     * by the usage of the com.ibm.ws.webcontainer.security.checkdefaultmethod attribute.
     * This attribute is set by the web container in ServletWrapper.startRequest() with the
     * intent of blocking hackers from crafting an HTTP HEAD method to bypass security constraints.
     * See PK83258 for full details.
     * This method also checks for uncovered HTTP methods
     */
    @Override
    public WebReply checkPrecludedAccess(WebRequest webRequest, String uriName) {
        WebReply webReply = null;
        if (webRequest.isAccessPrecluded()) {
            webReply = new DenyReply("Access is precluded because security constraints are specified, but the required roles are empty.");
        } else if (MatchResponse.DENY_MATCH_RESPONSE.equals(webRequest.getMatchResponse())) {
            webReply = new DenyReply("Http uncovered method found, denying reply.");
        } else {
            List<String> requiredRoles = webRequest.getRequiredRoles();
            if (requiredRoles.isEmpty()) {
                HttpServletRequest req = webRequest.getHttpServletRequest();
                String defaultMethod = (String) req.getAttribute("com.ibm.ws.webcontainer.security.checkdefaultmethod");
                if ("TRACE".equals(defaultMethod)) {
                    webReply = new DenyReply("Illegal request. Default implementation of TRACE not allowed.");
                }
            }
        }
        return webReply;
    }

    protected boolean isUnprotectedResourceAuthenRequired(WebRequest webRequest) {
        HttpServletRequest request = webRequest.getHttpServletRequest();
        Set<String> serviceIds = unprotectedResourceServiceRef.keySet();
        for (String serviceId : serviceIds) {
            UnprotectedResourceService service = unprotectedResourceServiceRef.getService(serviceId);
            if (service.isAuthenticationRequired(request))
                return true;
        }
        return false;
    }

    private String getComponentId(ServiceReference<TrustAssociationInterceptor> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        if (id == null) {
            id = (String) ref.getProperty("component.name");
            if (id == null) {
                id = (String) ref.getProperty("component.id");
            }
        }
        return id;
    }

    private void setModuleMetaDataToThreadLocal(Object key) {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        WebModuleMetaData wmmd = (WebModuleMetaData) cmd.getModuleMetaData();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "set WebModuleMetaData : " + wmmd);
        }
        WebConfigUtils.setWebModuleMetaData(key, wmmd);
    }

    private void removeModuleMetaDataFromThreadLocal(Object key) {
        WebConfigUtils.removeWebModuleMetaData(key);
    }

    /**
     * Notify the registered listeners of the change to the UserRegistry
     * configuration.
     */
    private void notifyWebAppSecurityConfigChangeListeners(List<String> delta) {
        WebAppSecurityConfigChangeEvent event = new WebAppSecurityConfigChangeEventImpl(delta);
        for (WebAppSecurityConfigChangeListener listener : webAppSecurityConfigchangeListenerRef.services()) {
            listener.notifyWebAppSecurityConfigChanged(event);
        }
    }

    /**
     * Format the map of config change attributes for the audit function. The output format would be the
     * same as original WebAppSecurityConfig.getChangedProperties method.
     *
     * @return String in the format of "name=value, name=value, ..." encapsulating the
     *         properties that are different between this WebAppSecurityConfig and the specified one
     */
    private String toStringFormChangedPropertiesMap(Map<String, String> delta) {
        if (delta == null || delta.isEmpty()) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : delta.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private boolean isPostLoginProcessDone(HttpServletRequest req) {
        Boolean result = (Boolean) req.getAttribute("com.ibm.ws.security.javaeesec.donePostLoginProcess");
        if (result != null && result) {
            return true;
        }
        return false;
    }

    // no null check for webReply object, so make sure it is not null upon calling this method.
    private void logAuditEntriesBeforeAuthn(WebReply webReply, Subject receivedSubject, String uriName, WebRequest webRequest) {
        if (Audit.isAuditServiceEnabled()) {
            AuthenticationResult authResult;
            if (webReply instanceof PermitReply) {
                authResult = new AuthenticationResult(AuthResult.SUCCESS, receivedSubject, null, null, AuditEvent.OUTCOME_SUCCESS);
            } else {
                authResult = new AuthenticationResult(AuthResult.FAILURE, receivedSubject, null, null, AuditEvent.OUTCOME_FAILURE);
            }
            Integer statusCode = Integer.valueOf(webReply.getStatusCode());
            Audit.audit(Audit.EventID.SECURITY_AUTHN_01, webRequest, authResult, statusCode);
            Audit.audit(Audit.EventID.SECURITY_AUTHZ_01, webRequest, authResult, uriName, statusCode);
        }
    }

    /**
     * Get the toString for a Subject. Even though the {@link Subject#toString()} method doesn't declare that
     * it throws a SecurityException, if there is a missing credential permission, you can get one.
     *
     * @param subject The Subject to get the toString for.
     * @return The string representation of the Subject.
     */
    @Sensitive
    private String getSubjectToString(Subject subject) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return subject.toString();
            }
        });
    }
}
