/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.tai;

import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.authentication.filter.internal.AuthFilterConfig;
import com.ibm.ws.security.openid20.OpenidClientConfig;
import com.ibm.ws.security.openid20.OpenidConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebAuthenticatorProxy;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.webcontainer.security.openid20.OpenidClientService;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

public class OpenidTAI implements TrustAssociationInterceptor {
    private static final TraceComponent tc = Tr.register(OpenidTAI.class);

    static final String openid_identifier = OpenidConstants.OPENID_IDENTIFIER;

    public static final String KEY_OPENID_CLIENT_CONFIG = "openidClientConfig";
    public static final String KEY_OPENID_CLIENT_SERVICE = "openidClientService";
    public static final String KEY_SECURITY_SERVICE = "securityService";
    public final static String KEY_FILTER = "authenticationFilter";

    protected final AtomicServiceReference<OpenidClientConfig> openidClientConfigRef =
                    new AtomicServiceReference<OpenidClientConfig>(KEY_OPENID_CLIENT_CONFIG);
    private final AtomicServiceReference<OpenidClientService> openIdClientServiceRef =
                    new AtomicServiceReference<OpenidClientService>(KEY_OPENID_CLIENT_SERVICE);
    private final AtomicServiceReference<SecurityService> securityServiceRef =
                    new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    protected final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef =
                    new ConcurrentServiceReferenceMap<String, AuthenticationFilter>(KEY_FILTER);

    private WebProviderAuthenticatorHelper authHelper;

    /** Default Constructor **/
    public OpenidTAI() {}

    /**
     * 
     **/
    protected void activate(ComponentContext componentContext, Map<String, Object> newProperties) {
        openidClientConfigRef.activate(componentContext);
        openIdClientServiceRef.activate(componentContext);
        securityServiceRef.activate(componentContext);
        authFilterServiceRef.activate(componentContext);

        authHelper = new WebProviderAuthenticatorHelper(securityServiceRef);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "authHelper:" + authHelper);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        openidClientConfigRef.deactivate(componentContext);
        openIdClientServiceRef.deactivate(componentContext);
        securityServiceRef.deactivate(componentContext);
        authFilterServiceRef.deactivate(componentContext);
    }

    protected void setOpenidClientConfig(ServiceReference<OpenidClientConfig> ref) {
        openidClientConfigRef.setReference(ref);
    }

    protected void updatedOpenidClientConfig(ServiceReference<OpenidClientConfig> ref) {
        openidClientConfigRef.setReference(ref);
    }

    protected void unsetOpenidClientConfig(ServiceReference<OpenidClientConfig> ref) {
        openidClientConfigRef.unsetReference(ref);
    }

    protected void setOpenidClientService(ServiceReference<OpenidClientService> ref) {
        openIdClientServiceRef.setReference(ref);
    }

    protected void unsetOpenidClientService(ServiceReference<OpenidClientService> ref) {
        openIdClientServiceRef.unsetReference(ref);
    }

    protected void setSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.setReference(ref);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.unsetReference(ref);
    }

    protected void setAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
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

    protected void unsetAuthenticationFilter(ServiceReference<AuthenticationFilter> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetAuthenticationFilter id:" + ref.getProperty(AuthFilterConfig.KEY_ID));
        }
        authFilterServiceRef.removeReference((String) ref.getProperty(AuthFilterConfig.KEY_ID), ref);
    }

    @Override
    public void cleanup() {}

    @Override
    public String getType() {
        return "OpenidTAI";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public int initialize(Properties props)
                    throws WebTrustAssociationFailedException {
        // Initialize TAI according to the configuration values
        return 0;
    }

    @Override
    public boolean isTargetInterceptor(HttpServletRequest req)
                    throws WebTrustAssociationException {
        OpenidClientConfig openidClientConfig = openidClientConfigRef.getService();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "openidClientConfig:(" + openidClientConfig + ")");
        }
        if (openidClientConfig == null)
            return false;

        // handle filter if any
        String authFilterId = openidClientConfig.getAuthFilterId();
        if (authFilterId != null && authFilterId.length() > 0) {
            AuthenticationFilter authFilter = authFilterServiceRef.getService(authFilterId);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "authFilter id:" + authFilterId + " authFilter:" + authFilter);
            }
            if (authFilter != null) {
                if (!authFilter.isAccepted(req))
                    return false;
            }
        }

        String providerIdentifier = openidClientConfig.getProviderIdentifier();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "providerIdentifier(openid_identifier):(" + providerIdentifier + ")");
        }
        return !(providerIdentifier == null || providerIdentifier.isEmpty());
    }

    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest request, HttpServletResponse response)
                    throws WebTrustAssociationFailedException {
        OpenidClientConfig openidClientConfig = openidClientConfigRef.getService();
        String providerIdentifier = openidClientConfig.getProviderIdentifier();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "negotiateValidate...(" + providerIdentifier + ")");
        }

        OpenidClientService openIdClientService = openIdClientServiceRef.getService();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "openIdClientService:" + openIdClientService);
        }
        try {
            if (openIdClientService != null) {
                if (openIdClientService.getRpRequestIdentifier(request, response) != null) {
                    ProviderAuthenticationResult result = openIdClientService.verifyOpResponse(request, response);
                    if (result.getStatus() != AuthResult.SUCCESS) {
                        // return new AuthenticationResult(AuthResult.FAILURE, "OpenID client failed with status code " + result.getStatus());
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "verify failed:" + result);
                        }
                        return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
                    }
                    AuthenticationResult authResult = authHelper.loginWithUserName(request, response, result.getUserName(),
                                                                                   result.getSubject(), result.getCustomProperties(),
                                                                                   openIdClientService.isMapIdentityToRegistryUser());
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "authHelper authResult:" + authResult);
                    }
                    if (authResult.getStatus() == AuthResult.SUCCESS) {
                        // get principal and subject from authResult
                        Subject subject = authResult.getSubject();
                        return TAIResult.create(HttpServletResponse.SC_OK,
                                                getUserName(subject),
                                                subject);
                    } else {
                        // fail out or continue
                        return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
                    }
                }
                else {
                    TAIResult basicAuthResult = basicAuthorizationHeader(request, response);
                    if (basicAuthResult.getStatus() == HttpServletResponse.SC_CONTINUE) {
                        // fail out or continue?
                        request.setAttribute(openid_identifier, providerIdentifier); // "https://nc135020.tivlab.austin.ibm.com:9443/op");
                        openIdClientService.createAuthRequest(request, response);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "... expect to be redirected by the browser");
                        }
                        // expect to be redirected by the browser
                        return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        return basicAuthResult;
                    }
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "negotiateValidateandEstablishTrust() get Exception", e);
            }
            // fail out or continue?
            return TAIResult.create(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        // return continue just in case;
        return TAIResult.create(HttpServletResponse.SC_CONTINUE);
    }

    protected TAIResult basicAuthorizationHeader(HttpServletRequest request, HttpServletResponse response)
                    throws WebTrustAssociationFailedException {
        // check allowBasicAuthentication
        OpenidClientConfig openidClientConfig = openidClientConfigRef.getService();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "basicAuthorizationHeader:(" + openidClientConfig + ")");
        }
        if (openidClientConfig != null && openidClientConfig.allowBasicAuthentication()) {
            ClientAuthnData authData = new ClientAuthnData(request, response);
            if (authData.hasAuthnData()) {
                String error = "Username and password do not match";
                try {
                    WebAuthenticator basicAuthenticator = getBasicAuthenticator();
                    AuthenticationResult authResult = basicAuthenticator.authenticate(request, response, null);
                    AuthResult result = authResult.getStatus();
                    if (result.equals(AuthResult.SUCCESS)) {
                        return TAIResult.create(HttpServletResponse.SC_OK,
                                                authResult.getUserName(), authResult.getSubject());
                    }
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Failed to authenticate using basic auth token " + e.getMessage());
                    }
                }
                if (openidClientConfig.isTryOpenIDIfBasicAuthFails() == false) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "user authentication for " + authData.getUserName() + " failed... No attemping openid");
                    }
                    response.addHeader("WWW-Authenticate", "Basic error=" + error);
                    return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
                } // else continue
            }
        }

        return TAIResult.create(HttpServletResponse.SC_CONTINUE);
    }

    public WebAuthenticator getBasicAuthenticator() {
        WebAppSecurityConfig webAppSecurityConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        WebAuthenticatorProxy authenticatorProxy = webAppSecurityConfig.createWebAuthenticatorProxy();
        return authenticatorProxy.getBasicAuthAuthenticator();
    }

    /**
     * Gets the username from the principal of the subject.
     * 
     * @param subject {@code null} is not supported.
     * @return
     */
    public String getUserName(Subject subject) {
        if (subject == null)
            return null;
        Set<Principal> principals = subject.getPrincipals();
        Iterator<Principal> principalsIterator = principals.iterator();
        if (principalsIterator.hasNext()) {
            Principal principal = principalsIterator.next();
            return principal.getName();
        }
        return null;
    }
}
