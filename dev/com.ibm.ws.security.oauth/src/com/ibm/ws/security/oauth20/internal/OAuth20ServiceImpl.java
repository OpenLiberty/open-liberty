/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Authenticator;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Service;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * This class is the OSGI service that is invoked from the mainline Liberty
 * authentication path to handle OAuth authentication of incoming web requests.
 * The absence of this service reference indicates that OAuth is not configured.
 */
public class OAuth20ServiceImpl implements OAuth20Service {
    public static final TraceComponent tc = Tr.register(OAuth20ServiceImpl.class);

    OAuth20Authenticator authenticator = new OAuth20AuthenticatorImpl();
    protected static final Pattern OAUTH_PROTECTED_PATTERN = Pattern.compile("/([\\w-]+)/(authorize|registration)");
    protected static final Pattern OAUTH_SPECIFIC_PATTERN = Pattern.compile("/([\\w-]+)/.*");

    static protected final String KEY_ID = "id";
    static protected final String KEY_oauth20Provider = "oauth20Provider";
    static protected final ConcurrentServiceReferenceMap<String, OAuth20Provider> oauth20ProviderRef = new ConcurrentServiceReferenceMap<String, OAuth20Provider>(KEY_oauth20Provider);

    protected void setOauth20Provider(ServiceReference<OAuth20Provider> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (oauth20ProviderRef) {
            oauth20ProviderRef.putReference(id, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setOAuth20Provider id:" + id);
        }
    }

    protected void updatedOauth20Provider(ServiceReference<OAuth20Provider> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (oauth20ProviderRef) {
            oauth20ProviderRef.putReference(id, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateOAuth20Provider id:" + id);
        }
    }

    protected void unsetOauth20Provider(ServiceReference<OAuth20Provider> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        synchronized (oauth20ProviderRef) {
            oauth20ProviderRef.removeReference(id, ref);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetOAuth20Provider id:" + id);
        }
    }

    protected synchronized void activate(ComponentContext cc) {
        synchronized (oauth20ProviderRef) {
            oauth20ProviderRef.activate(cc);
        }
    }

    protected synchronized void modify(Map<String, Object> properties) {
    }

    protected synchronized void deactivate(ComponentContext cc) {
        synchronized (oauth20ProviderRef) {
            oauth20ProviderRef.deactivate(cc);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res) {
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);
        // handle the result when it's OAuthChallengeReply
        handleOauthChallenge(res, result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res,
            ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef) {
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOauthSpecificURI(HttpServletRequest req, boolean protectedOrAll) {
        boolean isOauthSpecific = false;
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "contextPath=" + contextPath + " uri=" + uri);
        }
        if (contextPath != null && contextPath.equals("/oauth2")) {
            Matcher matcher = endpointRequest(req);
            if (matcher != null) { // these are oidc endpoints
                String oauthProviderName = getProviderNameFromUrl(matcher);

                OAuth20Provider oauthServerConfig = getOAuth20Provider(oauthProviderName);
                if (oauthServerConfig != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "check " + (protectedOrAll ? "Protected-Endpoints" : "All-endpoints"));
                    }
                    if (!protectedOrAll) {
                        return true; // it has been checked by endpointRequest already
                    }
                    if (protectedEndpointRequest(req) != null)
                        return true;
                }

            }
        }

        if (!isOauthSpecific && !protectedOrAll) { // all and not true yet
            // let's check if misc
            synchronized (oauth20ProviderRef) {
                Iterator<OAuth20Provider> oauth20Providers = oauth20ProviderRef.getServices();
                while (oauth20Providers.hasNext()) {
                    OAuth20Provider provider = oauth20Providers.next();
                    if (provider.isMiscUri(req))
                        return true;
                }
            }
        }
        return isOauthSpecific;
    }

    private Matcher endpointRequest(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "path=" + path);
        }
        if (path == null || path.isEmpty())
            return null;

        Matcher m = OAUTH_SPECIFIC_PATTERN.matcher(path);
        if (m.matches()) {
            return m;
        }
        return null;
    }

    private Matcher protectedEndpointRequest(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "path=" + path);
        }
        if (path == null || path.isEmpty())
            return null;

        Matcher m = OAUTH_PROTECTED_PATTERN.matcher(path);
        if (m.matches()) {
            return m;
        }
        return null;
    }

    protected String getProviderNameFromUrl(Matcher m) {
        String componentId = m.group(1);
        return componentId;
    }

    /**
     * @param oidcProviderName
     * @return
     */
    private OAuth20Provider getOAuth20Provider(String oauth20ProviderId) {
        synchronized (oauth20ProviderRef) {
            return oauth20ProviderRef.getService(oauth20ProviderId);
        }
    }

    /**
     * @param res
     * @param result
     */
    void handleOauthChallenge(HttpServletResponse rsp, ProviderAuthenticationResult oauthResult) {
        String errorDescription = null;
        if (oauthResult.getStatus() == AuthResult.FAILURE) {
            if (HttpServletResponse.SC_UNAUTHORIZED == oauthResult.getHttpStatusCode()) {
                errorDescription = "OAuth service failed the request";
            }
        } else if (oauthResult.getStatus() != AuthResult.SUCCESS) {
            if (HttpServletResponse.SC_UNAUTHORIZED == oauthResult.getHttpStatusCode()) {
                errorDescription = "OAuth service failed the request due to unsuccessful request";
            }
        }
        if (errorDescription != null) {
            try {
                OAuth20ProviderUtils.handleOAuthChallenge(rsp, oauthResult, errorDescription);
            } catch (IOException ioe) {
                // TODO error handling further
                //
                // Since this is a part of error handling.
                // even though this did not set up proper message here,
                // the error handling will continue handling it
            }

        }
    }
}
