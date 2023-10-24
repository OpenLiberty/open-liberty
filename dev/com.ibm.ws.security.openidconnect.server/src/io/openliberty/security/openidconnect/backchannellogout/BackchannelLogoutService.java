/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.sso.common.Constants;
import com.ibm.ws.webcontainer.security.UnprotectedResourceService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

@Component(service = UnprotectedResourceService.class)
public class BackchannelLogoutService implements UnprotectedResourceService {

    private static TraceComponent tc = Tr.register(BackchannelLogoutService.class);

    private static final ConcurrentServiceReferenceSet<OidcServerConfig> oidcServerConfigRef = new ConcurrentServiceReferenceSet<OidcServerConfig>("oidcServerConfigService");

    private static final Pattern ACCESS_ID_PATTERN = Pattern.compile("^[^:]+" + ":" + ".*" + "/" + "([^/]+)$");

    @Reference(name = "oidcServerConfigService", service = OidcServerConfig.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setOidcClientConfigService(ServiceReference<OidcServerConfig> reference) {
        oidcServerConfigRef.addReference(reference);
    }

    protected void unsetOidcClientConfigService(ServiceReference<OidcServerConfig> reference) {
        oidcServerConfigRef.removeReference(reference);
    }

    public void activate(ComponentContext cc) {
        oidcServerConfigRef.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        oidcServerConfigRef.deactivate(cc);
    }

    @Override
    public boolean isAuthenticationRequired(HttpServletRequest request) {
        return false;
    }

    @Override
    public boolean postLogout(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    @Override
    public boolean logout(HttpServletRequest request, HttpServletResponse response, String userName) {
        if (userName == null || userName.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The userName is null or empty, so logout will not be performed.");
            }
            return false;
        }
        userName = normalizeUserName(userName);
        String requestUri = request.getRequestURI();
        OidcServerConfig oidcServerConfig = getMatchingConfig(requestUri);
        if (oidcServerConfig == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find a matching OIDC provider for the request sent to [" + requestUri + "]");
            }
            return false;
        }
        String idTokenString = request.getParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT);

        sendBackchannelLogoutRequests(request, oidcServerConfig, userName, idTokenString);
        return true;
    }

    String normalizeUserName(String userName) {
        Matcher userNameMatcher = ACCESS_ID_PATTERN.matcher(userName);
        if (userNameMatcher.matches()) {
            userName = userNameMatcher.group(1);
        }
        return userName;
    }

    private OidcServerConfig getMatchingConfig(String requestUri) {
        Iterator<ServiceAndServiceReferencePair<OidcServerConfig>> servicesWithRefs = oidcServerConfigRef.getServicesWithReferences();
        while (servicesWithRefs.hasNext()) {
            ServiceAndServiceReferencePair<OidcServerConfig> configServiceAndRef = servicesWithRefs.next();
            OidcServerConfig config = configServiceAndRef.getService();
            String configId = config.getProviderId();
            if (isEndpointThatMatchesConfig(requestUri, configId) || isDelegatedLogoutRequestForConfig(requestUri, configId)) {
                return config;
            }
        }
        return null;
    }

    boolean isEndpointThatMatchesConfig(String requestUri, String providerId) {
        return (requestUri.endsWith("/" + providerId + "/" + EndpointType.end_session.name())
                || requestUri.endsWith("/" + providerId + "/" + EndpointType.logout.name()));
    }

    /**
     * Checks if the logout request originated from a SAML IDP-initiated single logout request.
     */
    boolean isDelegatedLogoutRequestForConfig(String requestUri, String providerId) {
        String getOpFromSubject = getPropertyFromRunAsSubjectPrivateCredentials(Constants.WSCREDENTIAL_OIDC_OP_USED);
        if (!providerId.equals(getOpFromSubject)) {
            return false;
        }
        String samlIdpFromSubject = getPropertyFromRunAsSubjectPrivateCredentials(Constants.WSCREDENTIAL_SAML_IDP_USED);
        return (samlIdpFromSubject != null && requestUri.endsWith("/" + samlIdpFromSubject + "/" + "slo"));
    }

    @SuppressWarnings("rawtypes")
    String getPropertyFromRunAsSubjectPrivateCredentials(String key) {
        Subject runAsSubject = getRunAsSubject();
        if (runAsSubject != null) {
            Set<Hashtable> hashtableCreds = runAsSubject.getPrivateCredentials(Hashtable.class);
            if (hashtableCreds != null) {
                for (Hashtable hashtable : hashtableCreds) {
                    String propertyValue = (String) hashtable.get(key);
                    if (propertyValue != null) {
                        return propertyValue;
                    }
                }
            }
        }
        return null;
    }

    Subject getRunAsSubject() {
        try {
            return WSSubject.getRunAsSubject();
        } catch (WSSecurityException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while getting runAsSubject:", e.getCause());
            }
        }
        return null;
    }

    void sendBackchannelLogoutRequests(HttpServletRequest request, OidcServerConfig oidcServerConfig, String userName, String idTokenString) {
        BackchannelLogoutRequestHelper bclRequestCreator = new BackchannelLogoutRequestHelper(request, oidcServerConfig);
        bclRequestCreator.sendBackchannelLogoutRequests(userName, idTokenString);
    }

}
