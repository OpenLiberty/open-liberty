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
package com.ibm.ws.security.saml.sso20.web;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.impl.KnownSamlUrl;

/**
 * Servlet Filter implementation class RequestFilter
 */
public class RequestFilter implements Filter {
    private static TraceComponent tc = Tr.register(RequestFilter.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    public static final String PATH_SAML_METADATA = "samlmetadata";
    public static final String PATH_ACS = "acs";
    public static final String PATH_SAML_LOGOUT = "logout";
    public static final String PATH_SAML_SLO = "slo";

    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    }

    /**
     * Default constructor.
     */
    public RequestFilter() {}

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {}

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        Matcher matcher = endpointRequest(request);
        if (matcher != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "doFiler response.isCommitted():" + response.isCommitted());
            }
            if (response.isCommitted()) {
                // Some component has committed the Http Servlet Response already
                // do nothing in this case
            } else {
                setEndpointRequest(request, response, chain, matcher);
            }
        } else {
            Tr.error(tc, "SAML20_INVALID_ACS_URL", request.getPathInfo());
            //"CWWKS5003E: The request directed to the URL of [" + request.getPathInfo()
            //  + "] is not a valid Assertion Consumer Services endpoint URL."

            String message = Tr.formatMessage(tc, "SAML20_AUTHENTICATION_FAIL");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
        }
    }

    /**
     * @param response
     * @param matcher
     * @throws ServletException
     * @throws IOException
     */
    public void setEndpointRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Matcher matcher) throws IOException, ServletException {
        SsoRequest samlRequest = new SsoRequest(getProviderNameFromUrl(matcher), getEndpointTypeFromUrl(matcher), request, getSamlVersion());
        request.setAttribute(Constants.ATTRIBUTE_SAML20_REQUEST, samlRequest);
        chain.doFilter(request, response);
    }

    protected String getProviderNameFromUrl(Matcher m) {
        String componentId = m.group(1);
        return componentId;
    }

    protected Constants.EndpointType getEndpointTypeFromUrl(Matcher m) {
        Constants.EndpointType type = RequestFilter.getType(m.group(2));
        return type;
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        // do nothing
    }

    public static Matcher endpointRequest(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path != null) {
            Matcher m = KnownSamlUrl.matchKnownSamlUrl(path);
            if (m.matches()) {
                return m;
            }
        }
        return null;
    }

    /**
     * Method to determine type of request based on known
     * path values (ex: authorize, token, introspect, revoke,
     * etc...)
     *
     * @param pathType is one of the known endpoint path values
     * @return enum type characterizing the request
     */
    private static Constants.EndpointType getType(String pathType) {
        //Need to do map to type discovery because enum types don't support certain symbols

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "path type is " + pathType);
        }

        if (pathType.equals(PATH_SAML_METADATA)) {
            return Constants.EndpointType.SAMLMETADATA;
        }

        //Needed to handle paths such as /registration/*
        if (pathType.startsWith(PATH_ACS)) {
            return Constants.EndpointType.ACS;
        }

        if (pathType.equals(PATH_SAML_SLO)) {
            return Constants.EndpointType.SLO;
        }
        if (pathType.equals(PATH_SAML_LOGOUT)) {
            return Constants.EndpointType.LOGOUT;
        }

        return Constants.EndpointType.valueOf(pathType);
    }
}
