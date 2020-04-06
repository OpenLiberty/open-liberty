/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.openidconnect.web.TraceConstants;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Service;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * This class is the OSGI service that is invoked from the main line Liberty
 * authentication path to handle access token for incoming web requests.
 * The absence of this service reference indicates that OpenID Connect is not configured.
 */
public class OidcServerImpl implements OidcServer {
    private static TraceComponent tc = Tr.register(OidcServerImpl.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    public static final String REGEX_COMPONENT_ID = "/([\\w-]+)/";

    //Matches paths such as /registration/* to extract clientId values
    public static final String REGEX_REGISTRATION = "registration(/\\S*)?";
    public static final String apwPattern = OAuth20Constants.APP_PASSWORD_URI + "|" + OAuth20Constants.APP_PASSWORD_URI + "/.*";
    public static final String atokPattern = OAuth20Constants.APP_TOKEN_URI + "|" + OAuth20Constants.APP_TOKEN_URI + "/.*";
    private static final Pattern PATH_RE = Pattern.compile("^" + REGEX_COMPONENT_ID + 
                    "(authorize|token|introspect|revoke|.well-known/openid-configuration|userinfo|"
                     + REGEX_REGISTRATION + "|check_session_iframe|end_session|coverage_map"
                     + "|proxy|"+ apwPattern + "|" + atokPattern + ")$");

    public static final String CFG_KEY_ID = "id";
    public static final String CFG_KEY_OIDC_SERVER_CONFIG = "oidcServerConfig";
    private final ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef =
                    new ConcurrentServiceReferenceMap<String, OidcServerConfig>(CFG_KEY_OIDC_SERVER_CONFIG);
    private boolean bOidcUpdated = false;
    HashMap<String, OidcServerConfig> oidcMap = new HashMap<String, OidcServerConfig>();
    ConfigUtils configUtils = new ConfigUtils();
    static protected final String KEY_ID = "id";
    static protected final String KEY_oauth20Provider = "oauth20Provider";
    static protected final ConcurrentServiceReferenceMap<String, OAuth20Provider> oauth20ProviderRef =
                    new ConcurrentServiceReferenceMap<String, OAuth20Provider>(KEY_oauth20Provider);

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

    protected void setOidcServerConfig(ServiceReference<OidcServerConfig> ref) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.putReference((String) ref.getProperty(CFG_KEY_ID), ref);
            bOidcUpdated = true;
        }
    }

    protected void unsetOidcServerConfig(ServiceReference<OidcServerConfig> ref) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.removeReference((String) ref.getProperty(CFG_KEY_ID), ref);
            bOidcUpdated = true;
        }
    }

    protected synchronized void activate(ComponentContext cc) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.activate(cc);
            bOidcUpdated = true;
        }
        synchronized (oauth20ProviderRef) {
            oauth20ProviderRef.activate(cc);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activate");
        }
    }

    protected synchronized void modify(Map<String, Object> properties) {}

    protected synchronized void deactivate(ComponentContext cc) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.deactivate(cc);
            bOidcUpdated = true;
        }
        synchronized (oauth20ProviderRef) {
            oauth20ProviderRef.deactivate(cc);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
                                                     HttpServletResponse res,
                                                     AtomicServiceReference<OAuth20Service> oauthServiceRef) {

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOIDCSpecificURI(HttpServletRequest req, boolean protectedOrAll) {
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "contextPath=" + contextPath + " uri=" + uri);
            Tr.debug(tc, "check " + (protectedOrAll ? "Protected-Endpoints" : "All-endpoints"));
        }
        if (contextPath != null && contextPath.equals("/oidc")) {
            Matcher matcher = endpointRequest(req);
            if (matcher != null) { // these are oidc endpoints
                String oidcProviderName = getProviderNameFromUrl(matcher);
                OidcServerConfig oidcServerConfig = getOidcServerConfig(oidcProviderName);
                if (oidcServerConfig != null) {
                    if (protectedOrAll) {
                        // protected
                        Matcher matcherEndpoint = oidcServerConfig.getProtectedEndpointsPattern().matcher(uri);
                        if (matcherEndpoint.matches())
                            return true;
                    } else {
                        // all
                        // we will excklude end_session and check_session_iframe
                        Matcher matcherEndpoint = oidcServerConfig.getEndpointsPattern().matcher(uri);
                        if (matcherEndpoint.matches()) {
                            Matcher matcherNonEndpoint = oidcServerConfig.getNonEndpointsPattern().matcher(uri);
                            return !matcherNonEndpoint.matches(); // when end_session and check_session_iframe, return false
                        };
                    }
                }
            }
        }

        if (!protectedOrAll) {
            // when all, let's check if misc 
            synchronized (oauth20ProviderRef) {
                Iterator<OAuth20Provider> oauth20Providers = oauth20ProviderRef.getServices();
                while (oauth20Providers.hasNext()) {
                    OAuth20Provider provider = oauth20Providers.next();
                    if (provider.isMiscUri(req))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * @param oidcProviderName
     * @return
     */
    private OidcServerConfig getOidcServerConfig(String oidcProviderName) {
        synchronized (oidcServerConfigRef) {
            if (bOidcUpdated) {
                oidcMap = configUtils.checkDuplicateOAuthProvider(oidcServerConfigRef);
                bOidcUpdated = false;
            }
        }
        OidcServerConfig oidcServerConfig = oidcMap.get(oidcProviderName);
        return oidcServerConfig;
    }

    protected String getProviderNameFromUrl(Matcher m) {
        String componentId = m.group(1);
        return componentId;
    }

    private Matcher endpointRequest(HttpServletRequest request) {

        String path = request.getPathInfo();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "path=" + path);
        }
        if (path == null || path.isEmpty())
            return null;

        Matcher m = PATH_RE.matcher(path);
        if (m.matches()) {
            return m;
        }
        return null;
    }

    public boolean allowDefaultSsoCookieName() {
        synchronized (oidcServerConfigRef) {
            if (bOidcUpdated) {
                oidcMap = configUtils.checkDuplicateOAuthProvider(oidcServerConfigRef);
                bOidcUpdated = false;
            }
        }
        boolean allow = false;

        if (oidcMap.entrySet() != null) {
            Iterator<Entry<String, OidcServerConfig>> it = oidcMap.entrySet().iterator();
            while (it.hasNext()) {
                OidcServerConfig cfg = it.next().getValue();
                if (cfg.allowDefaultSsoCookieName()) {
                    allow = true;
                }
                else {
                    allow = false;
                    break;
                }
            }
        }

        return allow;
    }

}
