/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.web;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
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
import com.ibm.ws.security.openidconnect.backchannellogout.BackchannelLogoutHelper;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

/**
 * Servlet for OpenID Connect Back-Channel Logout (https://openid.net/specs/openid-connect-backchannel-1_0.html)
 */
@Component(name = "com.ibm.ws.security.openidconnect.client.web.OidcBackchannelLogoutServlet", service = {}, property = { "service.vendor=IBM" })
public class OidcBackchannelLogoutServlet extends HttpServlet {

    private static TraceComponent tc = Tr.register(OidcBackchannelLogoutServlet.class);

    private static final long serialVersionUID = 1L;

    private static final ConcurrentServiceReferenceSet<OidcClientConfig> oidcClientConfigRef = new ConcurrentServiceReferenceSet<OidcClientConfig>("oidcClientConfigService");

    @Reference(name = "oidcClientConfigService", service = OidcClientConfig.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setOidcClientConfigService(ServiceReference<OidcClientConfig> reference) {
        oidcClientConfigRef.addReference(reference);
    }

    protected void unsetOidcClientConfigService(ServiceReference<OidcClientConfig> reference) {
        oidcClientConfigRef.removeReference(reference);
    }

    public void activate(ComponentContext cc) {
        oidcClientConfigRef.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        oidcClientConfigRef.deactivate(cc);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        OidcClientConfig matchingConfig = getMatchingConfig(requestUri);
        BackchannelLogoutHelper logoutHelper = new BackchannelLogoutHelper(request, response, matchingConfig);
        logoutHelper.handleBackchannelLogoutRequest();
    }

    private OidcClientConfig getMatchingConfig(String requestUri) {
        Iterator<ServiceAndServiceReferencePair<OidcClientConfig>> servicesWithRefs = oidcClientConfigRef.getServicesWithReferences();
        while (servicesWithRefs.hasNext()) {
            ServiceAndServiceReferencePair<OidcClientConfig> configServiceAndRef = servicesWithRefs.next();
            OidcClientConfig config = configServiceAndRef.getService();
            String configId = config.getId();
            if (requestUri.endsWith("/" + configId)) {
                return config;
            }
        }
        return null;
    }

}