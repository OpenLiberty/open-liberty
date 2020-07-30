/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.utils.Constants;
import com.ibm.ws.security.jwt.utils.TokenBuilder;
import com.ibm.ws.security.jwt.web.JwtRequest.EndpointType;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

@Component(service = JwtEndpointServices.class, name = "com.ibm.ws.security.jwt.web.JwtEndpointServices", immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class JwtEndpointServices {
    private static TraceComponent tc = Tr.register(JwtEndpointServices.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    /***********************************
     * Begin OSGi-related fields and methods
     ***********************************/

    public static final String KEY_ID = "id";
    public static final String KEY_JWT_CONFIG = "jwtConfig";

    private final ConcurrentServiceReferenceMap<String, JwtConfig> jwtConfigRef = new ConcurrentServiceReferenceMap<String, JwtConfig>(
            KEY_JWT_CONFIG);

    @Reference(service = JwtConfig.class, name = KEY_JWT_CONFIG, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void setJwtConfig(ServiceReference<JwtConfig> ref) {
        synchronized (jwtConfigRef) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting reference for " + ref.getProperty(KEY_ID));
            }
            jwtConfigRef.putReference((String) ref.getProperty(KEY_ID), ref);
        }
    }

    protected void unsetJwtConfig(ServiceReference<JwtConfig> ref) {
        synchronized (jwtConfigRef) {
            jwtConfigRef.removeReference((String) ref.getProperty(KEY_ID), ref);
        }
    }

    @Activate
    protected void activate(ComponentContext cc) {
        jwtConfigRef.activate(cc);

        Tr.info(tc, "JWT_ENDPOINT_SERVICE_ACTIVATED");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        jwtConfigRef.deactivate(cc);
    }

    /***********************************
     * End OSGi-related fields and methods
     ***********************************/

    protected void handleEndpointRequest(HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext) throws IOException {
        JwtRequest jwtRequest = getJwtRequest(request, response);
        if (jwtRequest == null) {
            // Error redirect already taken care of
            return;
        }

        String jwtConfigId = jwtRequest.getJwtConfigId();
        JwtConfig jwtConfig = getJwtConfig(response, jwtConfigId);
        if (jwtConfig == null) {
            // Error redirect already taken care of
            return;
        }

        EndpointType endpointType = jwtRequest.getType();
        handleJwtRequest(request, response, servletContext, jwtConfig, endpointType);

    }

    /**
     * Extracts the {@value WebConstants#JWT_REQUEST_ATTR} attribute from the
     * provided request. That particular attribute is supposed to be created and
     * set by the filter directing the request.
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    private JwtRequest getJwtRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JwtRequest jwtRequest = (JwtRequest) request.getAttribute(WebConstants.JWT_REQUEST_ATTR);
        if (jwtRequest == null) {
            String errorMsg = Tr.formatMessage(tc, "JWT_REQUEST_ATTRIBUTE_MISSING",
                    new Object[] { request.getRequestURI(), WebConstants.JWT_REQUEST_ATTR });
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
        }
        return jwtRequest;
    }

    private JwtConfig getJwtConfig(HttpServletResponse response, String jwtConfigId) throws IOException {
        JwtConfig jwtConfig = jwtConfigRef.getService(jwtConfigId);
        if (jwtConfig == null) {
            String errorMsg = Tr.formatMessage(tc, "JWT_CONFIG_SERVICE_NOT_AVAILABLE", new Object[] { jwtConfigId });
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
        }
        return jwtConfig;
    }

    /**
     * Handle the request for the respective endpoint to which the request was
     * directed.
     *
     * @param request
     * @param response
     * @param servletContext
     * @param jwtConfig
     * @param endpointType
     * @throws IOException
     */
    protected void handleJwtRequest(HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext, JwtConfig jwtConfig, EndpointType endpointType) throws IOException {
        if (jwtConfig == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No JwtConfig object provided");
            }
            return;
        }
        switch (endpointType) {
        case jwk:
            processJWKRequest(response, jwtConfig);
            return;
        case token:
            try {
                if (!isTransportSecure(request)) {
                    String url = request.getRequestURL().toString();
                    Tr.error(tc, "SECURITY.JWT.ERROR.WRONG.HTTP.SCHEME", new Object[] { url });
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            Tr.formatMessage(tc, "SECURITY.JWT.ERROR.WRONG.HTTP.SCHEME", new Object[] { url }));
                    return;
                }

                boolean result = request.authenticate(response);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "request.authenticate result: " + result);
                }
                // if false, then not authenticated,
                // a 401 w. auth challenge will be sent back and
                // the response will be committed.
                // Requester can then try again with creds on a new request.
                if (result == false) {
                    return;
                }
            } catch (ServletException e) {
                // ffdc
                return;
            }
            processTokenRequest(response, jwtConfig);
            return;
        default:
            break;
        }
    }

    /**
     * determine if transport is secure. Either the protocol must be https or we
     * must see a forwarding header that indicates it was https upstream of a
     * proxy. Use of a configuration property to allow plain http was rejected
     * in review.
     *
     * @param req
     * @return
     */
    private boolean isTransportSecure(HttpServletRequest req) {
        req.getRequestURL().toString();
        if (req.getScheme().equals("https")) {
            return true;
        }

        String value = req.getHeader("X-Forwarded-Proto");
        if (value != null && value.toLowerCase().equals("https")) {
            return true;
        }
        return false;
    }

    /**
     * produces a JWT token based upon the jwt Configuration, and the security
     * credentials of the authenticated user that called this method. Returns
     * the token as JSON in the response.
     *
     * @param response
     * @param jwtConfig
     * @throws IOException
     */
    private void processTokenRequest(HttpServletResponse response, JwtConfig jwtConfig) throws IOException {

        String tokenString = new TokenBuilder().createTokenString(jwtConfig);
        addNoCacheHeaders(response);
        response.setStatus(200);

        if (tokenString == null) {
            return;
        }
        try {
            PrintWriter pw = response.getWriter();
            response.setHeader(WebConstants.HTTP_HEADER_CONTENT_TYPE, WebConstants.HTTP_CONTENT_TYPE_JSON);
            pw.write("{\"token\": \"" + tokenString + "\"}");
            pw.flush();
            pw.close();
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an exception attempting to get the response writer: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Obtains the JWK string that is active in the specified config and prints
     * it in JSON format in the response. If a JWK is not found, the response
     * will be empty.
     *
     * @param response
     * @param jwtConfig
     * @throws IOException
     */
    private void processJWKRequest(HttpServletResponse response, JwtConfig jwtConfig) throws IOException {

        /*
         * if (!jwtConfig.isJwkEnabled()) { String errorMsg =
         * Tr.formatMessage(tc, "JWK_ENDPOINT_JWK_NOT_ENABLED", new Object[] {
         * jwtConfig.getId() }); Tr.error(tc, errorMsg);
         * response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
         * return; }
         */

        String signatureAlg = jwtConfig.getSignatureAlgorithm();
        if (!isPossibleJwkAlgorithm(signatureAlg)) {
            String errorMsg = Tr.formatMessage(tc, "JWK_ENDPOINT_WRONG_ALGORITHM",
                    new Object[] { jwtConfig.getId(), signatureAlg, getAcceptableJwkSignatureAlgorithms() });
            Tr.error(tc, errorMsg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
            return;
        }

        String jwkString = jwtConfig.getJwkJsonString();

        addNoCacheHeaders(response);
        response.setStatus(200);

        if (jwkString == null) {
            return;
        }
        try {
            PrintWriter pw = response.getWriter();
            response.setHeader(WebConstants.HTTP_HEADER_CONTENT_TYPE, WebConstants.HTTP_CONTENT_TYPE_JSON);
            pw.write(jwkString);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an exception attempting to get the response writer: " + e.getLocalizedMessage());
            }
        }
    }

    boolean isPossibleJwkAlgorithm(String signatureAlgorithm) {
        if (signatureAlgorithm == null) {
            return false;
        }
        return signatureAlgorithm.matches("[RE]S[0-9]{3,}");
    }

    String getAcceptableJwkSignatureAlgorithms() {
        // TODO more will be added
        return Constants.SIGNATURE_ALG_RS256 + ", " +
                Constants.SIGNATURE_ALG_ES256;
    }

    /**
     * Adds header values to avoid caching of the provided response.
     *
     * @param response
     */
    protected void addNoCacheHeaders(HttpServletResponse response) {
        String cacheControlValue = response.getHeader(WebConstants.HEADER_CACHE_CONTROL);

        if (cacheControlValue != null && !cacheControlValue.isEmpty()) {
            cacheControlValue = cacheControlValue + ", " + WebConstants.CACHE_CONTROL_NO_STORE;
        } else {
            cacheControlValue = WebConstants.CACHE_CONTROL_NO_STORE;
        }

        response.setHeader(WebConstants.HEADER_CACHE_CONTROL, cacheControlValue);
        response.setHeader(WebConstants.HEADER_PRAGMA, WebConstants.PRAGMA_NO_CACHE);
    }

}