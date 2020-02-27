/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * This class reads the OAuth providers config
 */
@Component(configurationPid = "com.ibm.ws.security.oauth20.provider.config", configurationPolicy = ConfigurationPolicy.IGNORE, service = { ProvidersService.class }, immediate = true, property = { "service.vendor=IBM" })
public class ProvidersService {

    private static final String KEY_ID = "id";
    private static final String KEY_OAUTH20_PROVIDER = "oauth20Provider";
    private static final ConcurrentServiceReferenceMap<String, OAuth20Provider> oauth20Providers = new ConcurrentServiceReferenceMap<String, OAuth20Provider>(KEY_OAUTH20_PROVIDER);
    // Use locks instead of synchronize blocks to ensure concurrent access while reading and lock during modification only
    private static final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();;
    private static final WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private static final ReadLock readLock = reentrantReadWriteLock.readLock();

    public static final String REGEX_COMPONENT_ID = "/([\\w-]+)/";
    public static final String apwPattern = OAuth20Constants.APP_PASSWORD_URI + "|" + OAuth20Constants.APP_PASSWORD_URI + "/.*";
    public static final String atokPattern = OAuth20Constants.APP_TOKEN_URI + "|" + OAuth20Constants.APP_TOKEN_URI + "/.*";
    private static final Pattern PATH_RE = Pattern.compile("^" + REGEX_COMPONENT_ID
            + "(" + apwPattern + "|" + atokPattern + ")$");

    @Reference(name = KEY_OAUTH20_PROVIDER, service = OAuth20Provider.class, cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void setOAuth20Provider(ServiceReference<OAuth20Provider> ref) {
        writeLock.lock();
        try {
            oauth20Providers.putReference((String) ref.getProperty(KEY_ID), ref);
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetOAuth20Provider(ServiceReference<OAuth20Provider> ref) {
        writeLock.lock();
        try {
            oauth20Providers.removeReference((String) ref.getProperty(KEY_ID), ref);
        } finally {
            writeLock.unlock();
        }
    }

    @Activate
    protected void activate(ComponentContext cc) {
        writeLock.lock();
        try {
            oauth20Providers.activate(cc);
        } finally {
            writeLock.unlock();
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        writeLock.lock();
        try {
            oauth20Providers.deactivate(cc);
        } finally {
            writeLock.unlock();
        }
    }

    public static OAuth20Provider getOAuth20Provider(String id) {
        // This lock is needed here for getting the provider based on the most current configuration.
        readLock.lock();
        try {
            OAuth20Provider provider = oauth20Providers.getService(id);
            if (provider != null) {
                if (provider.isValid()) {
                    if (provider.getComponent() == null) {
                        provider.createCoreClasses();
                    }
                } else {
                    provider = null;
                }
            }
            return provider;
        } finally {
            readLock.unlock();
        }
    }

    public static List<OAuth20Provider> getProvidersMatchingRequest(HttpServletRequest req) {
        // This lock is needed here for getting the provider based on the most current configuration.
        readLock.lock();
        try {
            List<OAuth20Provider> providers = null;
            Iterator<OAuth20Provider> oauthProvidersIterator = oauth20Providers.getServices();
            while (oauthProvidersIterator.hasNext()) {
                OAuth20Provider provider = oauthProvidersIterator.next();
                if (provider.isValid() && (provider.isRequestAccepted(req) || isAppPasswordOrTokenRequest(provider, req))) {
                    if (provider.getComponent() == null) { // svt: avoid this access if we can
                        provider.createCoreClasses();
                    }
                    if (providers == null) {
                        providers = new ArrayList<OAuth20Provider>();
                    }
                    providers.add(provider);
                }
            }
            return providers;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * @param provider
     * @param req
     * @return
     */
    private static boolean isAppPasswordOrTokenRequest(OAuth20Provider provider, HttpServletRequest req) {
        boolean accept = false;
        boolean apppwuri = req != null && req.getRequestURI().contains(OAuth20Constants.APP_PASSWORD_URI);
        boolean apptokuri = req != null && req.getRequestURI().contains(OAuth20Constants.APP_TOKEN_URI);
        if (apppwuri || apptokuri) {
            OidcServerConfig oidcServerConfig = ConfigUtils.getOidcServerConfigForOAuth20Provider(provider.getID());
            if (oidcServerConfig != null && oidcServerConfig.getProviderId() != null) {
                accept = requestHasMatchingOidcProvider(req, oidcServerConfig.getProviderId());
                // if (req.getRequestURI().contains(oidcServerConfig.getProviderId())) {
                // accept = true;
                // }
            }

        }
        return accept;
    }

    public static boolean requestHasMatchingOidcProvider(HttpServletRequest req, String oidcProviderId) {

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();

        if (contextPath != null && contextPath.equals("/oidc")) {
            Matcher matcher = endpointRequest(req);
            if (matcher != null) { // these are app-password or app-token endpoints
                return oidcProviderId.equals(getProviderNameFromUrl(matcher));
            }
        }

        return false;
    }

    protected static String getProviderNameFromUrl(Matcher m) {
        String componentId = m.group(1);
        return componentId;
    }

    private static Matcher endpointRequest(HttpServletRequest request) {

        String path = request.getPathInfo();

        if (path == null || path.isEmpty())
            return null;

        Matcher m = PATH_RE.matcher(path);
        if (m.matches()) {
            return m;
        }
        return null;
    }

}
