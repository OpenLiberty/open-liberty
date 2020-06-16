/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.web;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

import io.openliberty.security.oauth20.web.OAuthSupportedHttpMethodHandler;

@SuppressWarnings("restriction")
public class OidcSupportedHttpMethodHandler extends OAuthSupportedHttpMethodHandler {

    private static TraceComponent tc = Tr.register(OidcSupportedHttpMethodHandler.class);

    public OidcSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected Set<HttpMethod> getDefaultSupportedMethodsForEndpoint(EndpointType endpointType) {
        Set<HttpMethod> supportedMethods = new HashSet<HttpMethod>();
        supportedMethods.add(HttpMethod.OPTIONS);

        if (endpointType == EndpointType.discovery) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.userinfo) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.end_session) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.check_session_iframe) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.jwk) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Received a request for an unknown OIDC endpoint: [" + endpointType + "]. Checking if it's an OAuth endpoint...");
            }
            return super.getDefaultSupportedMethodsForEndpoint(endpointType);
        }
        return supportedMethods;
    }

}