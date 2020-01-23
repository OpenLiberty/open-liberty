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

package com.ibm.ws.security.openidconnect.web;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.OAuth20RequestFilter;

public class OidcRequestFilter extends OAuth20RequestFilter {
    private static TraceComponent tc = Tr.register(OidcRequestFilter.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    public OidcRequestFilter() {
        super();
    }

    public void setEndpointRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Matcher matcher) throws IOException, ServletException {
        OidcRequest oidcRequest = new OidcRequest(getProviderNameFromUrl(matcher), getEndpointTypeFromUrl(matcher), request);
        request.setAttribute("OidcRequest", oidcRequest);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OidcRequest:" + oidcRequest);
        };
        chain.doFilter(request, response);
    }
}
