/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.util.JSONUtil;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.error.OAuthExceptionHandler;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.TemplateRetriever;

/**
 * An exception handler class that handles exceptions and error responses for an
 * OAuth 2.0 authorization endpoint request. This exception handler is part of
 * the examples as opposed to the core component for two reasons:
 * <ul>
 * <li>Extensibility - Component consumers may want to return additional error
 * information such as an error_uri as part of the error response.
 * <li>User Interface - Component consumers will have their own UI requirements
 * about how error messages should be displayed on pages.
 * </ul>
 */
public class OAuth20AuthorizeRequestExceptionHandler implements
        OAuthExceptionHandler {
    private static TraceComponent tc = Tr.register(
            OAuth20AuthorizeRequestExceptionHandler.class, "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");
    public static final String HEADER_ACCEPT_LANGUAGE = TemplateRetriever.HEADER_ACCEPT_LANGUAGE;
    public static final String RESPONSE_TYPE_TOKEN = "token";
    public static final String RESPONSE_TYPE_ID_TOKEN = "id_token";
    public static final String RESPONSE_TYPE_TOKEN_ID_TOKEN = "token id_token";
    public static final String RESPONSE_TYPE_ID_TOKEN_TOKEN = "id_token token";

    public static final String STATE = "state";

    private final String _responseType;
    private final String _redirectUri;
    private final String _templateUrl;

    public OAuth20AuthorizeRequestExceptionHandler(String responseType,
            String redirectUri, String templateUrl) {
        _responseType = responseType;
        _redirectUri = redirectUri;
        _templateUrl = templateUrl;
    }

    @Override
    public void handleResultException(HttpServletRequest req,
            HttpServletResponse rsp, OAuthResult result) {

        boolean handled = false;
        String encoding = req.getCharacterEncoding() != null ? req.getCharacterEncoding() : "utf-8";
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            OAuthException e = result.getCause();

            if (tc.isDebugEnabled())
                Tr.debug(tc,
                        "com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet result is bad", // GK1
                        new Object[] { e, _responseType, _redirectUri, _templateUrl }); // GK1

            if (e != null) {
                boolean processException = true;
                if (e instanceof OAuth20Exception) {
                    String error = e.getError();
                    if (OAuth20Exception.INVALID_REQUEST.equals(error) &&
                            OIDCConstants.MESSAGE_LOGIN_REQUIRED_ID_TOKEN_HINT_INVALID.equals(e.getMessage())) {
                        processException = false;
                    }
                }
                if (e instanceof OAuth20InvalidResponseTypeException ||
                        e instanceof OAuth20InvalidClientException ||
                        e instanceof OAuth20InvalidGrantTypeException ||
                        e instanceof OAuth20InvalidScopeException) {
                    processException = false;
                }
                if (processException) {
                    // in some cases, the FFDC showed in file system too late. And the test case failed. Move FFDC here (earilier)
                    com.ibm.ws.ffdc.FFDCFilter.processException(e,
                            "com.ibm.ws.security.oauth20.error.impl.OAuth20AuthorizeRequestExceptionHandler", "96", this);

                }
                /*
                 * Error handling is explained in the spec at various places
                 * depending on the grant type being used.
                 *
                 * Authorization code:
                 * http://tools.ietf.org/html/draft-ietf-oauth
                 * -v2#section-4.1.2.1
                 *
                 * Implicit:
                 * http://tools.ietf.org/html/draft-ietf-oauth-v2#section
                 * -4.2.2.1
                 */
                if (e instanceof OAuth20Exception) {
                    /*
                     * If the error is due to an invalid client id or a missing
                     * or invalid redirect URI, we should inform the resource
                     * owner of the error
                     */
                    OAuth20Exception e2 = (OAuth20Exception) e;
                    String error = e2.getError();

                    if (OAuth20Exception.UNAUTHORIZED_CLIENT.equals(error)
                            || (e2 instanceof OAuth20InvalidRedirectUriException)
                            || ((e2 instanceof OAuth20DuplicateParameterException) && OAuth20Constants.REDIRECT_URI.equals(((OAuth20DuplicateParameterException) e2).getParamName()))
                            || _redirectUri == null) {
                        /*
                         * Simply inform the resource owner
                         */
                        try {
                            /*
                             * Here is where the error response is written to
                             * the page. Add any additional custom UI
                             * requirements here.
                             */
                            if (_templateUrl != null && !"".equals(_templateUrl.trim())) {
                                renderErrorPage(req, rsp, e2);
                            } else {
                                PrintWriter pw = rsp.getWriter();
                                pw.print(e2.formatSelf(req.getLocale(), encoding));
                            }
                            handled = true;
                        } catch (IOException ioe) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Internal error writing JSON response", new Object[] { ioe });
                        }
                    } else {
                        if (OAuth20Exception.INVALID_GRANT.equals(error)
                                || OAuth20Exception.UNSUPPORED_GRANT_TPE.equals(error)) {
                            /*
                             * Set the response code - 401 Unauthorized
                             */
                            rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
                            /*
                             * If the resource owner denies the access request or if
                             * the request fails for reasons other than a missing or
                             * invalid redirection URI, the authorization server
                             * informs the client by adding parameters to the
                             * fragment/query component of the redirection URI
                             * depending on the grant type being used. Default to
                             * query string.
                             */
                            String separator = "?";
                            if (RESPONSE_TYPE_TOKEN.equals(_responseType)
                                    // handle open_id response types
                                    || RESPONSE_TYPE_ID_TOKEN.equals(_responseType)
                                    || RESPONSE_TYPE_ID_TOKEN_TOKEN.equals(_responseType)
                                    || RESPONSE_TYPE_TOKEN_ID_TOKEN.equals(_responseType)) {
                                separator = "#";
                            }

                            int index = _redirectUri.indexOf(separator);
                            StringBuilder location = new StringBuilder(_redirectUri);

                            if (index == (location.length() - 1)) {
                                // nothing to do
                            } else if (index > -1) {
                                location.append("&");
                            } else {
                                location.append(separator);
                            }

                            location.append(ERROR);
                            location.append("=");
                            try {
                                error = URLEncoder.encode(error, "utf-8");
                            } catch (UnsupportedEncodingException e3) {
                                // ignore
                            }
                            location.append(error);
                            String errorDesc = e2.formatSelf(req.getLocale(), encoding);
                            if (errorDesc != null) {
                                location.append("&");
                                location.append(ERROR_DESCRIPTION);
                                location.append("=");
                                try {
                                    errorDesc = URLEncoder.encode(errorDesc, "utf-8");
                                } catch (UnsupportedEncodingException e1) {
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Internal error encoding error description", new Object[] { e1 });
                                }
                                location.append(errorDesc);
                            }

                            /*
                             * If a state was present, return it also.
                             */
                            AttributeList al = result.getAttributeList();
                            if (al != null) {
                                String state = al.getAttributeValueByName(STATE);
                                if (state != null) {
                                    location.append("&");
                                    location.append(STATE);
                                    location.append("=");
                                    try {
                                        state = URLEncoder.encode(state, "utf-8");
                                    } catch (UnsupportedEncodingException e3) {
                                        // ignore
                                    }
                                    location.append(state);
                                }
                                String authType = al.getAttributeValueByName(Constants.WWW_AUTHENTICATE);
                                if (authType != null && authType.length() > 0) { // not empty
                                    rsp.setHeader(Constants.WWW_AUTHENTICATE, authType);
                                }
                            }

                            int iStatusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
                            if (e2 instanceof OAuth20AccessDeniedException) {
                                iStatusCode = ((OAuth20AccessDeniedException) e2).getHttpStatusCode();
                            }
                            if (iStatusCode >= 400 && iStatusCode < 600) {
                                try {
                                    rsp.sendError(iStatusCode, ERROR + "=" + error + "&" +
                                            ERROR_DESCRIPTION + "=" + e2.formatSelf(req.getLocale(), encoding));
                                } catch (IOException ioe) {
                                    /*
                                     * If we can't do this, we are in trouble.
                                     */
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "Internal error", new Object[] { ioe });
                                }
                            } else {
                                rsp.setStatus(iStatusCode);
                                rsp.setHeader(LOCATION, location.toString());
                            }

                            handled = true;
                        }
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
                        if (_templateUrl != null && !"".equals(_templateUrl.trim())) {
                            renderErrorPage(req, rsp, e);
                        } else {
                            PrintWriter pw = rsp.getWriter();
                            pw.print(e.getMessage());
                        }
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
    }

    private void renderErrorPage(HttpServletRequest request,
            HttpServletResponse response, OAuthException exception) throws IOException {
        String methodName = "renderErrorPage";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }
        String acceptLanguage = request.getHeader(HEADER_ACCEPT_LANGUAGE);
        ErrorPageRenderer renderer = new ErrorPageRenderer();
        renderer.renderErrorPage(exception, _templateUrl, acceptLanguage,
                request, response);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

}
