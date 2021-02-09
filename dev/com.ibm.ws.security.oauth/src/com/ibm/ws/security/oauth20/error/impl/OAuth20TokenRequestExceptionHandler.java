/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.error.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.util.JSONUtil;
import com.ibm.ws.security.oauth20.error.OAuthExceptionHandler;

/**
 * An exception handler class that handles exceptions and error responses for an
 * OAuth 2.0 token endpoint request. This exception handler is part of the
 * examples as opposed to the core component for two reasons:
 * <ul>
 * <li>Extensibility - Component consumers may want to return additional error
 * information such as an error_uri as part of the error response.
 * <li>User Interface - Component consumers will have their own UI requirements
 * about how error messages should be displayed on pages.
 * </ul>
 */
public class OAuth20TokenRequestExceptionHandler implements
        OAuthExceptionHandler {

    private static final TraceComponent tc = Tr.register(
            OAuth20TokenRequestExceptionHandler.class, "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    public static final String EXAMPLE_WWW_AUTHENTICATE_BASIC_VALUE = "Basic: realm=\"test\"";

    public void handleResultException(HttpServletRequest req,
            HttpServletResponse rsp, OAuthResult result) {
        String methodName = "handleResultException";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, result);
        }

        boolean handled = false;
        String encoding = req.getCharacterEncoding() != null ? req.getCharacterEncoding() : "utf-8";

        if (result.getStatus() != OAuthResult.STATUS_OK) {
            OAuthException e = result.getCause();

            if (tc.isDebugEnabled())
                Tr.debug(tc,
                        "com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.TokenRequest result is bad",
                        new Object[] { e });

            if (e != null) {
                if (!(e instanceof OAuth20InvalidGrantTypeException)) {
                    // handle the FFDC earlier because, sometimes, it shows on the file system too late.
                    com.ibm.ws.ffdc.FFDCFilter.processException(e,
                            "com.ibm.ws.security.oauth20.error.impl.OAuth20TokenRequestExceptionHandler",
                            "80",
                            this);
                }

                if (e instanceof OAuth20Exception) {
                    /*
                     * For the token endpoint, return JSON back to the client.
                     * See
                     * http://tools.ietf.org/html/draft-ietf-oauth-v2#section
                     * -5.2 for more details.
                     */
                    OAuth20Exception e2 = (OAuth20Exception) e;

                    /*
                     * From the OAuth 2.0 spec when the error is invalid_client:
                     *
                     * The authorization server MAY return an HTTP 401
                     * (Unauthorized) status code to indicate which HTTP
                     * authentication schemes are supported. If the client
                     * attempted to authenticate via the "Authorization" request
                     * header field, the authorization server MUST respond with
                     * an HTTP 401 (Unauthorized) status code, and include the
                     * "WWW-Authenticate" response header field matching the
                     * authentication scheme used by the client.
                     */
                    String error = e2.getError();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "processing exception with OAuthResult: " + error + " and error message = " + e2.getMessage() + ", localized message = " + e2.getLocalizedMessage());
                    }
                    if (OAuth20Exception.INVALID_CLIENT.equals(error)) {
                        /*
                         * Set the response code - 401 Unauthorized
                         */
                        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                        /*
                         * Normally a 'WWW-Authenticate' header would be added
                         * to the response to indicate which authentication
                         * mechanisms are available but this sample execution
                         * environment doesn't perform any authentication other
                         * than validating the client_id and client_secret
                         * passed in the request parameters.
                         */
                        /*-
                        if (false) {
                            // This example returns a Basic header
                            rsp.setHeader(WWW_AUTHENTICATE,
                                    EXAMPLE_WWW_AUTHENTICATE_BASIC_VALUE);
                        }
                         */
                    } else if (OAuth20Exception.INVALID_TOKEN.equals(error)) {
                        /*
                         * Set the response code - 401 Unauthorized
                         */
                        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    } else if (OAuth20Exception.INVALID_SCOPE.equals(error)) {
                        /*
                         * Set the response code - 302
                         */
                        rsp.setStatus(HttpServletResponse.SC_FOUND);
                    } else {
                        /*
                         * Set the response code - 400 Bad Request
                         */
                        rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }

                    /*
                     * Set the response headers according to the spec
                     */
                    rsp.setHeader(CACHE_CONTROL, CACHE_CONTROL_VALUE);
                    rsp.setHeader(PRAGMA, PRAGMA_VALUE);
                    rsp.setHeader(CONTENT_TYPE, CONTENT_TYPE_JSON);

                    /*
                     * Any additional response headers could be set here
                     */

                    /*
                     * Build up the JSON String to return to the client
                     */
                    String errorDescription = e2.formatSelf(req.getLocale(), encoding);
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put(ERROR, error);
                    attributes.put(ERROR_DESCRIPTION, errorDescription);

                    /*
                     * If an error_uri or additional information needs to be
                     * added to the error response, append it to the
                     * StringBuilder as shown below. Note: The URL below is used
                     * as an example only.
                     */
                    /*-
                    if (false) {
                        String errorUri = "http://localhost:9080/error.jsp?error="
                                + error;
                        attributes.put(ERROR_URI, errorUri);
                    }
                     */

                    try {
                        PrintWriter pw = rsp.getWriter();
                        pw.print(JSONUtil.getJSON(attributes));
                        handled = true;
                    } catch (IOException ioe) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Internal error writing JSON response", new Object[] { ioe });
                    }
                } else {
                    if (e instanceof OAuthConfigurationException) {
                        OAuthConfigurationException e2 = (OAuthConfigurationException) e;
                        String error = e2.getError();
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "processing exception with OAuthResult: " + error);
                        }
                        rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                        /*
                         * Set the response headers according to the spec
                         */
                        rsp.setHeader(CACHE_CONTROL, CACHE_CONTROL_VALUE);
                        rsp.setHeader(PRAGMA, PRAGMA_VALUE);
                        rsp.setHeader(CONTENT_TYPE, CONTENT_TYPE_JSON);

                        /*
                         * Any additional response headers could be set here
                         */

                        /*
                         * Build up the JSON String to return to the client
                         */
                        String errorDescription = e2.formatSelf(req.getLocale(), encoding);
                        Map<String, Object> attributes = new HashMap<String, Object>();
                        attributes.put(ERROR, error);
                        attributes.put(ERROR_DESCRIPTION, errorDescription);

                        /*
                         * If an error_uri or additional information needs to be
                         * added to the error response, append it to the
                         * StringBuilder as shown below. Note: The URL below is used
                         * as an example only.
                         */
                        /*-
                        if (false) {
                          String errorUri = "http://localhost:9080/error.jsp?error="
                                  + error;
                          attributes.put(ERROR_URI, errorUri);
                        }
                         */

                        try {
                            PrintWriter pw = rsp.getWriter();
                            pw.print(JSONUtil.getJSON(attributes));
                            handled = true;
                        } catch (IOException ioe) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Internal error writing JSON response", new Object[] { ioe });
                        }
                    }
                }

                /*
                 * If not handled explicitly, write out the exception.
                 */
                if (!handled) {
                    try {
                        PrintWriter pw = rsp.getWriter();
                        pw.print(e.formatSelf(req.getLocale(), encoding));
                        handled = true;
                    } catch (IOException ioe) {
                        /*
                         * If we can't do this, we are in trouble.
                         */
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Internal error", new Object[] { ioe });
                    }
                }
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, result);
        }
    }
}
