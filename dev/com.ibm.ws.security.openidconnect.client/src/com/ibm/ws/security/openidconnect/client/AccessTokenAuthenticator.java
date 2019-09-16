/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.client.internal.TraceConstants;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.clients.common.UserInfoHelper;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

public class AccessTokenAuthenticator {
    private static final TraceComponent tc = Tr.register(AccessTokenAuthenticator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String Authorization_Header = "Authorization";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String INVALID_CLIENT = "invalid_client";
    private static final String INVALID_TOKEN = "invalid_token";
    private static final String JWT_SEGMENTS = "-segments";
    private static final String JWT_SEGMENT_INDEX = "-";

    OidcClientUtil oidcClientUtil = new OidcClientUtil();
    SSLSupport sslSupport = null;
    private Jose4jUtil jose4jUtil = null;

    public AccessTokenAuthenticator() {
    }

    /**
     * @param sslSupportRef
     * @param clientConfig
     */
    public AccessTokenAuthenticator(AtomicServiceReference<SSLSupport> sslSupportRef) {
        this.sslSupport = sslSupportRef.getService();
        jose4jUtil = new Jose4jUtil(sslSupport);
    }

    /**
     * Perform OpenID Connect client authenticate for the given web request.
     * Return an OidcAuthenticationResult which contains the status and subject
     *
     * @param HttpServletRequest
     * @param HttpServletResponse
     * @param OidcClientConfig
     * @param ReferrerURLCookieHandler
     * @return OidcAuthenticationResult
     */
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res,
            OidcClientConfig clientConfig,
            OidcClientRequest oidcClientRequest) {
        oidcClientRequest.setTokenType(OidcClientRequest.TYPE_ACCESS_TOKEN);
        ProviderAuthenticationResult oidcResult = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_UNAUTHORIZED);
        String accessToken = null;
        if (clientConfig.getAccessTokenInLtpaCookie()) {
            accessToken = getAccessTokenFromReqAsAttribute(req);
        }
        if (accessToken == null) {
            accessToken = getBearerAccessTokenToken(req, clientConfig);
        }

        if (accessToken == null) {

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "access token in the request as attribute: ", accessToken);
            }
            //logError(clientConfig, oidcClientRequest, "PROPAGATION_TOKEN_MISSING_ACCESSTOKEN");  //CWWKS1726E server failed request
            // logging an error produced spurious log messages if the token was missing but fallbacks were available.
            oidcClientRequest.setRsFailMsg("", "suppress_CWWKS1704W"); // suppress later warning message if token is just missing.
            return oidcResult;

        }

        String validationMethod = clientConfig.getValidationMethod();
        if (accessToken.indexOf(".") >= 0) {
            // found n jwt token
            validationMethod = ClientConstants.VALIDATION_LOCAL;
            oidcClientRequest.setTokenType(OidcClientRequest.TYPE_JWT_TOKEN);
        }
        SSLSocketFactory sslSocketFactory = null;
        try {
            // get the real validationURL to set up SSLContext
            String validationUrl = getPropagationValidationURL(clientConfig, validationMethod);
            sslSocketFactory = getSSLSocketFactory(validationUrl, clientConfig.getSSLConfigurationName(), clientConfig.getClientId());
        } catch (SSLException e) {
            logError(clientConfig, oidcClientRequest, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { e.getMessage() != null ? e.getMessage() : "invalid ssl context", clientConfig.getClientId() });
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        if (validationMethod.equalsIgnoreCase(ClientConstants.VALIDATION_LOCAL)) {
            oidcResult = parseJwtToken(clientConfig, accessToken, sslSocketFactory, oidcClientRequest);
        } else {
            String validationEndpointURL = clientConfig.getValidationEndpointUrl();
            if (validationEndpointURL != null && !validationEndpointURL.isEmpty()) {
                if (!OIDCClientAuthenticatorUtil.checkHttpsRequirement(clientConfig, validationEndpointURL)) {
                    logError(clientConfig, oidcClientRequest, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", validationEndpointURL);
                    return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
                }
                if (validationMethod.equalsIgnoreCase(ClientConstants.VALIDATION_INTROSPECT)) {
                    oidcResult = introspectToken(clientConfig, accessToken, sslSocketFactory, oidcClientRequest);
                    // put userinfo json on the subject if we can get it, even tho it's not req'd. for authentication
                    (new UserInfoHelper(clientConfig)).getUserInfoIfPossible(oidcResult, accessToken, oidcResult.getUserName(), sslSocketFactory);
                } else if (validationMethod.equalsIgnoreCase(ClientConstants.VALIDATION_USERINFO)) {
                    oidcResult = getUserInfoFromToken(clientConfig, accessToken, sslSocketFactory, oidcClientRequest);
                }
            } else {
                logError(clientConfig, oidcClientRequest, "PROPAGATION_TOKEN_INVALID_VALIDATION_URL", validationEndpointURL);
            }
        }

        if (AuthResult.SUCCESS == oidcResult.getStatus()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "attribute:" + OidcClient.PROPAGATION_TOKEN_AUTHENTICATED);
            }
            oidcResult = fixSubject(oidcResult); // replace IdToken Object if needed
            // this is authenticated by the access_token
            req.setAttribute(OidcClient.PROPAGATION_TOKEN_AUTHENTICATED, Boolean.TRUE);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "oidcResult httpStatusCode:" + oidcResult.getHttpStatusCode() + " status:" + oidcResult.getStatus() + " result:" + oidcResult);
            Tr.debug(tc, "Token is owned by '" + oidcResult.getUserName() + "'");
        }
        return oidcResult;
    }

    ProviderAuthenticationResult fixSubject(ProviderAuthenticationResult oidcResult) {
        return new OidcClientAuthenticator().fixSubject(oidcResult);
    }

    /**
     * @param req
     * @return
     */
    private String getAccessTokenFromReqAsAttribute(HttpServletRequest req) {
        // TODO Auto-generated method stub
        String token = null;
        if (req.getAttribute(OidcClient.OIDC_ACCESS_TOKEN) != null) {
            token = (String) req.getAttribute(OidcClient.OIDC_ACCESS_TOKEN);
            req.removeAttribute(OidcClient.OIDC_ACCESS_TOKEN); // per request setting, so remove it here and sso authenticator will add again.
        }
        return token;
    }

    /**
     * @param clientConfig
     * @return an endpointURL which is in use when getSSLContext can not get an
     *         SSLContext
     */
    String getPropagationValidationURL(OidcClientConfig clientConfig, String validationMethod) {
        if (validationMethod.equalsIgnoreCase(ClientConstants.VALIDATION_INTROSPECT) ||
                validationMethod.equalsIgnoreCase(ClientConstants.VALIDATION_USERINFO)) {
            return clientConfig.getValidationEndpointUrl();
        } else if (validationMethod.equalsIgnoreCase(ClientConstants.VALIDATION_LOCAL)) {
            return clientConfig.getJwkEndpointUrl();
        } else {
            // Should not reach here, just in case
            return clientConfig.getTokenEndpointUrl();
        }
    }

    protected SSLContext getSSLContext(String tokenUrl, String sslConfigurationName, String clientId) throws SSLException {
        SSLContext sslContext = null;
        JSSEHelper jsseHelper = null;
        if (sslSupport != null) {
            jsseHelper = sslSupport.getJSSEHelper();
        }

        if (jsseHelper != null) {
            sslContext = jsseHelper.getSSLContext(sslConfigurationName, null, null, true);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sslContext (" + ") get: " + sslContext);
            }
        }

        if (sslContext == null) {
            if (tokenUrl != null && tokenUrl.startsWith("https")) {
                throw new SSLException(Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl conext", clientId }));
            }
        }
        return sslContext;
    }

    protected SSLSocketFactory getSSLSocketFactory(String tokenUrl, String sslConfigurationName, String clientId) throws SSLException {
        SSLSocketFactory sslSocketFactory = null;
        if (sslSupport != null) {
            try {
                sslSocketFactory = sslSupport.getSSLSocketFactory(sslConfigurationName);
            } catch (javax.net.ssl.SSLException e) {
                throw new SSLException(e.getMessage());
            }

            if (sslSocketFactory != null)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "sslSocketFactory (" + ") get: " + sslSocketFactory);
                }
        }

        if (sslSocketFactory == null) {
            if (tokenUrl != null && tokenUrl.startsWith("https")) {
                throw new SSLException(Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl socket factory", clientId }));
            }
        }
        return sslSocketFactory;
    }

    @FFDCIgnore({ IOException.class })
    JSONObject handleResponseMap(Map<String, Object> responseMap, OidcClientConfig clientConfig, OidcClientRequest oidcClientRequest) throws ParseException, IOException {
        String jresponse = null;
        JSONObject jobj = null, errorjson = null;

        if (responseMap.get(ClientConstants.RESPONSEMAP_CODE) != null) {
            HttpResponse response = (HttpResponse) responseMap.get(ClientConstants.RESPONSEMAP_CODE);
            if (isErrorResponse(response)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    jresponse = EntityUtils.toString(entity);
                    try {
                        if (jresponse != null) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "received error from OP =", jresponse);
                                // Tr.debug(tc, "debugging:" +
                                // OidcUtil.dumpStackTrace(new Exception(),
                                // -1));
                            }
                            errorjson = JSONObject.parse(jresponse);
                            logErrorMessage(errorjson, clientConfig, oidcClientRequest);// we
                                                                                        // are
                                                                                        // logging
                                                                                        // error
                                                                                        // messages
                                                                                        // here,
                                                                                        // so
                                                                                        // we
                                                                                        // stop
                                                                                        // the
                                                                                        // processing
                                                                                        // here.
                            return null;
                        }
                    } catch (IOException ioe) {
                        // ignore, we'll continue with logging the error message
                    }
                }

                if (jresponse == null || jresponse.isEmpty()) {
                    // WWW-Authenticate: Bearer error=invalid_token,
                    // error_description=CWWKS1617E: A userinfo request was made
                    // with an access token that was not recognized. The request
                    // URI was /oidc/endpoint/OidcConfigSample/userinfo.
                    Header header = response.getFirstHeader("WWW-Authenticate");
                    jresponse = header.getValue();
                }

                if (jresponse != null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "received error from OP and extracted it from the header =", jresponse);
                        // Tr.debug(tc, "debugging:" +
                        // OidcUtil.dumpStackTrace(new Exception(), -1));
                    }

                    if (jresponse.contains(INVALID_TOKEN)) {
                        logError(clientConfig, oidcClientRequest, "PROPAGATION_TOKEN_NOT_ACTIVE", clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
                    }
                    String originalError = extractErrorDescription(jresponse);
                    if (originalError != null) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "the original error from OP =", originalError);
                        }
                    }
                    logError(clientConfig, oidcClientRequest, "OIDC_PROPAGATION_FAIL", originalError, clientConfig.getValidationEndpointUrl());
                } else {
                    logError(clientConfig, oidcClientRequest, "OIDC_PROPAGATION_FAIL", "", clientConfig.getValidationEndpointUrl());
                }

                jobj = null;
            } else {
                // HTTP 200 response
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    jresponse = EntityUtils.toString(entity);
                }
                try {
                    jobj = JSONObject.parse(jresponse);
                } catch (IOException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "the response from OP is not in JSON format = ", jresponse);
                    }
                    logError(clientConfig, oidcClientRequest, "PROPAGATION_TOKEN_INVALID_VALIDATION_URL", clientConfig.getValidationEndpointUrl());
                }
            }
        }
        return jobj;
    }

    /**
     * Attempts to find and extract the error_description from the provided
     * response. If no error_description is found, null is returned.
     *
     * @param response
     * @return
     */
    protected String extractErrorDescription(String response) {
        if (response == null) {
            return null;
        }

        // "error_description" must not be immediately preceded by an
        // alphanumeric character - it must be recognizable as a distinct entry
        String regexHeader = "(?:.*[^a-zA-Z0-9])?" + Constants.ERROR_DESCRIPTION + "=(.*)";

        Pattern pattern = Pattern.compile(regexHeader);
        Matcher m = pattern.matcher(response);
        if (!m.matches()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Response did not appear to contain an error description formatted as expected. Returning response as-is");
            }
            return response;
        }
        String description = null;
        if (m.groupCount() > 0) {
            description = m.group(1);
            if (description != null && description.length() > 1) {
                // If first AND last characters are double quotes, they should
                // not be considered part of the error_description value
                if (description.charAt(0) == '"' && description.charAt(description.length() - 1) == '"') {
                    description = description.substring(1, description.length() - 1);
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Extracted description: [" + description + "]");
        }
        return description;
    }

    protected ProviderAuthenticationResult introspectToken(OidcClientConfig clientConfig, String accessToken, SSLSocketFactory sslSocketFactory, OidcClientRequest oidcClientRequest) {
        ProviderAuthenticationResult oidcResult = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_UNAUTHORIZED);

        try {

            Map<String, Object> responseMap = oidcClientUtil.checkToken(clientConfig.getValidationEndpointUrl(),
                    clientConfig.getClientId(), clientConfig.getClientSecret(),
                    accessToken, clientConfig.isHostNameVerificationEnabled(), clientConfig.getTokenEndpointAuthMethod(), sslSocketFactory, clientConfig.getUseSystemPropertiesForHttpClientConnections());

            JSONObject jobj = null;
            jobj = handleResponseMap(responseMap, clientConfig, oidcClientRequest);

            if (jobj != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "introspectToken=", jobj.serialize());
                    // Tr.debug(tc, "debugging:" + OidcUtil.dumpStackTrace(new
                    // Exception(), -1));
                }

                if (!validateJsonResponse(jobj, clientConfig, oidcClientRequest)) {
                    logErrorMessage(jobj, clientConfig, oidcClientRequest);
                    // Add error message here
                    // logError(clientConfig, "OIDC_PROPAGATION_FAIL",
                    // new Object[]{
                    // clientConfig.getClientId(),
                    // "access_tooken",
                    // clientConfig.getValidationEndpointUrl(),});
                    return oidcResult;
                }
                oidcResult = createProviderAuthenticationResult(jobj, clientConfig, accessToken);
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "exception during introspectToken =", e.getMessage());
                // Tr.debug(tc, "debugging:" + OidcUtil.dumpStackTrace(new
                // Exception(), -1));
            }
            logError(clientConfig, oidcClientRequest, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
            return oidcResult;
        }
        return oidcResult;
    }

    protected ProviderAuthenticationResult parseJwtToken(OidcClientConfig clientConfig,
            String accessToken,
            SSLSocketFactory sslSocketFactory,
            OidcClientRequest oidcClientRequest) {
        ProviderAuthenticationResult oidcResult = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_UNAUTHORIZED);

        oidcClientRequest.setTokenType(OidcClientRequest.TYPE_JWT_TOKEN);
        
        // jose4jUtil will log an error if something was wrong with the token and set the PAR to 401. 
        return jose4jUtil.createResultWithJose4JForJwt(accessToken, clientConfig, oidcClientRequest);
    }

    /**
     * @param response
     * @return
     */
    private boolean isErrorResponse(HttpResponse response) {
        // TODO Auto-generated method stub
        // Check the response from the endpoint to see if there was an error
        StatusLine status = response.getStatusLine();
        if (status == null || status.getStatusCode() != 200) {
            return true;
            // String errorMsg =
            // "Could not get the status of the response, or the response returned an error.";
            // HttpEntity entity = response.getEntity();
            // if (entity != null) {
            // errorMsg = EntityUtils.toString(entity).trim();
            // }
            // throw new IOException("Failed to reach endpoint " + url +
            // " because of the following error: " + errorMsg);
        }
        return false;

    }

    private void logErrorMessage(JSONObject jobj, OidcClientConfig clientConfig, OidcClientRequest oidcClientRequest) {
        String err = (String) jobj.get(Constants.ERROR);
        String inboundPropagation = clientConfig.getInboundPropagation();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "rs_err:" + err + " inboundPropagation:" + inboundPropagation);
        }
        boolean isPropagationSupported = ClientConstants.PROPAGATION_SUPPORTED.equals(inboundPropagation);
        String msgKey = null;
        Object[] msgObjs = null;

        if (err == null) {
            return;
        }
        if (INVALID_CLIENT.equals(err)) {
            msgKey = "PROPAGATION_TOKEN_INVALID_CLIENTID";
            msgObjs = new Object[] { clientConfig.getClientId(), clientConfig.getValidationEndpointUrl() };
        } else if (INVALID_TOKEN.equals(err)) {
            msgKey = "PROPAGATION_TOKEN_NOT_ACTIVE";
            msgObjs = new Object[] { clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl() };
        } else {
            // TODO need to see what other error types that we would receive
            String err_desc = null;
            if ((String) jobj.get(Constants.ERROR_DESCRIPTION) != null) {
                err_desc = (String) jobj.get(Constants.ERROR_DESCRIPTION);
            }
            msgKey = "OIDC_PROPAGATION_FAIL";
            msgObjs = new Object[] { err_desc, clientConfig.getValidationEndpointUrl() };
        }
        if (msgKey != null) {
            if (oidcClientRequest != null) {
                oidcClientRequest.setRsFailMsg(null, Tr.formatMessage(tc, msgKey, msgObjs));
            }
            if (!isPropagationSupported) {
                Tr.error(tc, msgKey, msgObjs);
            }
            // do not log error message since supported will fall down to RP
            // procedure. Good or bad, it's the job of RP
        }

    }

    protected ProviderAuthenticationResult getUserInfoFromToken(OidcClientConfig clientConfig, String accessToken, SSLSocketFactory sslSocketFactory, OidcClientRequest oidcClientRequest) {
        ProviderAuthenticationResult oidcResult = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_UNAUTHORIZED);
        Map<String, Object> responseMap = null;
        JSONObject jobj = null;
        try {
            responseMap = oidcClientUtil.getUserinfo(clientConfig.getValidationEndpointUrl(), accessToken, sslSocketFactory, clientConfig.isHostNameVerificationEnabled(), clientConfig.getUseSystemPropertiesForHttpClientConnections());

            jobj = handleResponseMap(responseMap, clientConfig, oidcClientRequest);
            if (jobj != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "userinfo=", jobj.serialize());
                }
                if (!validateUserinfoJsonResponse(jobj, clientConfig, oidcClientRequest)) {
                    return oidcResult;
                }
                oidcResult = createProviderAuthenticationResult(jobj, clientConfig, accessToken);
            }
        } catch (IllegalArgumentException e) {
            // There was likely a problem with the validationEndpointUrl syntax
            Tr.error(tc, "PROPAGATION_TOKEN_INVALID_VALIDATION_URL", clientConfig.getValidationEndpointUrl());
            oidcClientRequest.setRsFailMsg(null, Tr.formatMessage(tc, "PROPAGATION_TOKEN_INVALID_VALIDATION_URL", clientConfig.getValidationEndpointUrl()));
            return oidcResult;
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "exception while getting the userInfo =", e.getLocalizedMessage());
                // Tr.debug(tc, "debugging:" + OidcUtil.dumpStackTrace(new
                // Exception(), -1));
            }
            logError(clientConfig, oidcClientRequest, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
            return oidcResult;
        }

        // if result was good, put userinfo string on the subject as well.
        try {
            String userInfoStr = jobj == null ? null : jobj.serialize();
            if (oidcResult != null && oidcResult.getUserName() != null && userInfoStr != null) {
                oidcResult.getCustomProperties().put(Constants.USERINFO_STR, userInfoStr);
            }
        } catch (IOException e) {
        } // ffdc

        return oidcResult;
    }

    /**
     * @param jobj
     * @param clientConfig
     * @return
     */
    private boolean validateUserinfoJsonResponse(JSONObject jobj, OidcClientConfig clientConfig, OidcClientRequest oidcClientRequest) {
        // TODO Auto-generated method stub
        String err = (String) jobj.get(Constants.ERROR);
        if (err != null) {
            logErrorMessage(jobj, clientConfig, oidcClientRequest);
            return false;
        } else {
            String issuer = (String) jobj.get("iss");
            String issuers = null;
            if (issuer != null) {
                if (issuer.isEmpty() ||
                        ((issuers = getIssuerIdentifier(clientConfig)) == null) ||
                        notContains(issuers, issuer)) {
                    logError(clientConfig, oidcClientRequest, "PROPAGATION_TOKEN_ISS_ERROR", issuers, issuer);
                    return false;
                }
            }
            // TODO
            // else {
            // required field
            // logError(clientConfig,
            // "PROPAGATION_TOKEN_MISSING_REQUIRED_CLAIM_ERR", "iss",
            // "iss, iat, exp");
            // return false;
            // }
        }
        return true;
    }

    /**
     * @param issuers
     * @param issuer
     * @return
     */
    boolean notContains(String issuers, String issuer) { // issuer and issuers
                                                         // are not null
        if (issuers.equals(issuer)) {
            return false;
        }
        StringTokenizer st = new StringTokenizer(issuers, " ,");
        while (st.hasMoreTokens()) {
            String iss = st.nextToken();
            if (issuer.equals(iss)) {
                return false;
            }
        }
        return true;
    }

    protected boolean validateJsonResponse(JSONObject jobj, OidcClientConfig clientConfig) {
        return validateJsonResponse(jobj, clientConfig, null);
    }

    protected boolean validateJsonResponse(JSONObject jobj, OidcClientConfig clientConfig, OidcClientRequest req) {
        if (jobj.get("active") != null && ((Boolean) jobj.get("active")) != true) {
            logError(clientConfig, "PROPAGATION_TOKEN_NOT_ACTIVE", clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
            if (req != null) { // avoid subsequent CWWKS1704W with null cause.
                req.setRsFailMsg("", Tr.formatMessage(tc, "PROPAGATION_TOKEN_NOT_ACTIVE",
                        new Object[] { clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl() }));
            }
            return false;
        }
        // ToDo: check exp, iat
        Date currentDate = new Date();

        Long exp = 0L;
        if (jobj.get("exp") != null && (exp = getLong(jobj.get("exp"))) != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "exp = ", exp);
            }
            if (!verifyExpirationTime(exp, currentDate, clientConfig.getClockSkewInSeconds(), clientConfig)) {
                return false;
            }
        } else {
            // required field
            logError(clientConfig, "PROPAGATION_TOKEN_MISSING_REQUIRED_CLAIM_ERR", "exp", "iss, iat, exp"); // TODO
                                                                                                            // include
                                                                                                            // iss?
            return false;
        }
        Long iat = 0L;
        if (jobj.get("iat") != null && (iat = getLong(jobj.get("iat"))) != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "iat = ", iat);
            }
            if (!checkIssueatTime(iat, currentDate, clientConfig.getClockSkewInSeconds(), clientConfig)) {
                return false;
            }
        } else {
            // required field
            logError(clientConfig, "PROPAGATION_TOKEN_MISSING_REQUIRED_CLAIM_ERR", "iat", "iss, iat, exp");
            return false;
        }
        /*
         * String issuer = null; String issuerIdentifier = null; if
         * (jobj.get("iss") != null) { issuer = (String)jobj.get("iss"); if
         * (issuer.isEmpty() || ((issuerIdentifier =
         * getIssuerIdentifier(clientConfig)) == null) ||
         * !issuer.equals(issuerIdentifier)) { logError(clientConfig,
         * "PROPAGATION_TOKEN_ISS_ERROR", issuerIdentifier, issuer); return
         * false; } } else { //required field logError(clientConfig,
         * "PROPAGATION_TOKEN_MISSING_REQUIRED_CLAIM_ERR", "iss",
         * "iss, iat, exp"); return false; }
         */
        if (issuerChecking(jobj, clientConfig)) {
            Long nbf = 0L;
            if (jobj.get("nbf") != null && (nbf = getLong(jobj.get("nbf"))) != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "nbf = ", nbf);
                }
                if (!checkNotBeforeTime(nbf, currentDate, clientConfig.getClockSkewInSeconds(), clientConfig)) {
                    return false;
                }
            }
        } else {
            return false;
        }

        // TODO see whether we need client id checking

        return true;
    }

    boolean issuerChecking(JSONObject jobj, OidcClientConfig clientConfig) {
        String issuer = (String) jobj.get("iss");
        String issuers = null;
        if (clientConfig.disableIssChecking()) {
            if (issuer != null) {
                logError(clientConfig, "PROPAGATION_TOKEN_ISS_CLAIM_NOT_REQUIRED_ERR", clientConfig.getValidationEndpointUrl(), "iss", "disableIssChecking");
                return false;
            }
            return true;
        } else {
            if (issuer != null) {
                if (issuer.isEmpty()
                        || ((issuers = getIssuerIdentifier(clientConfig)) == null)
                        || notContains(issuers, issuer)) {
                    logError(clientConfig, "PROPAGATION_TOKEN_ISS_ERROR", issuers, issuer);
                    return false;
                }
            } else {
                // required field
                logError(clientConfig, "PROPAGATION_TOKEN_MISSING_REQUIRED_CLAIM_ERR", "iss", "iss, iat, exp");
                return false;
            }
        }

        return true;
    }

    String getIssuerIdentifier(OidcClientConfig clientConfig) {
        String issuer = null;
        issuer = clientConfig.getIssuerIdentifier();
        if (issuer == null || issuer.isEmpty()) {
            String validationEndpoint = clientConfig.getValidationEndpointUrl();
            if (validationEndpoint != null) {
                int lastSlashIndex = validationEndpoint.lastIndexOf("/");
                issuer = validationEndpoint.substring(0, lastSlashIndex);
            }
        }
        return issuer;
    }

    protected Long getLong(Object obj) {
        if (obj == null || obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Integer) {
            Integer int_obj = (Integer) obj;
            return Long.valueOf(int_obj.intValue());
        }

        Long result = null;
        try {
            String str = null;
            if (obj instanceof String[]) {
                str = ((String[]) obj)[0];
            } else {
                str = (String) obj;
            }
            result = Long.valueOf(str);
        } catch (Exception e) {
            // null result
        }
        return result;
    }

    /**
     * @param nbf
     * @param currentDate
     * @param clockSkewInSeconds
     * @return
     */
    private boolean checkNotBeforeTime(Long nbfInSeconds, Date currentDate, long clockSkewInSeconds, OidcClientConfig clientConfig) {
        long nbf = nbfInSeconds.longValue() * 1000; // MilliSeconds
        Date nbfDate = new Date(nbf);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "AccessToken nbf : " + nbfDate + ", currentDate:" + currentDate);
        }
        // nbf should not be in future
        Date currentDatePlusClockSkew = new Date(currentDate.getTime() + (clockSkewInSeconds * 1000));
        if (nbfDate.after(currentDatePlusClockSkew)) {// nbf is future time
            logError(clientConfig, true, "PROPAGATION_TOKEN_NBF_ERR", nbfDate.toString(), currentDatePlusClockSkew.toString());
            return false;
        }
        return true;
    }

    protected boolean verifyExpirationTime(Long expInSeconds, Date currentDate, long clockSkewInSeconds, OidcClientConfig clientConfig) {

        long exp = expInSeconds.longValue() * 1000; // MilliSeconds
        Date expDate = new Date(exp);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "AccessToken exp: " + expDate + ", currentDate:" + currentDate);
        }

        Date currentDateMinusClockSkew = new Date(currentDate.getTime() - (clockSkewInSeconds * 1000));
        if (expDate.before(currentDateMinusClockSkew)) { // if expiration time
                                                         // is before current
                                                         // time... expired
            logError(clientConfig, true, "PROPAGATION_TOKEN_EXPIRED_ERR", expDate.toString(), currentDateMinusClockSkew.toString());
            return false;
        }
        return true;
    }

    protected boolean checkIssueatTime(Long iatInSeconds, Date currentDate, long clockSkewInSeconds, OidcClientConfig clientConfig) {

        long iat = iatInSeconds.longValue() * 1000; // MilliSeconds
        Date iatDate = new Date(iat);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "AccessToken iat : " + iatDate + ", currentDate:" + currentDate);
        }
        // Let's check if the token is issued in a future time(iat)
        Date currentDatePlusClockSkew = new Date(currentDate.getTime() + (clockSkewInSeconds * 1000));
        if (iatDate.after(currentDatePlusClockSkew)) {// iat is future time
            logError(clientConfig, true, "PROPAGATION_TOKEN_FUTURE_TOKEN_ERR", iatDate.toString(), currentDatePlusClockSkew.toString());
            return false;
        }
        return true;
    }

    protected ProviderAuthenticationResult createProviderAuthenticationResult(JSONObject jobj, OidcClientConfig clientConfig, String accessToken) {

        AttributeToSubjectExt attributeToSubject = new AttributeToSubjectExt(clientConfig, jobj, accessToken);
        if (attributeToSubject.checkUserNameForNull())/*
                                                       * || attributeToSubject.
                                                       * checkForNullRealm())
                                                       */ { // TODO enable this
                                                            // null realm
                                                            // checking once
                                                            // userinfo code is
                                                            // fixed to emit
                                                            // "iss"
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        Hashtable<String, Object> customProperties = attributeToSubject.handleCustomProperties();
        customProperties.put(Constants.ACCESS_TOKEN, accessToken);
        ProviderAuthenticationResult oidcResult = null;
        // The doMapping() will save the current time.
        // ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS
        // In this RS scenario, the access_token is considered as expired. It
        // should not be reused.
        // Since there are no expires_in attribute in the customProperties, it
        // indicates the access_token is expired
        // **Unless we change the design. But this needs to think much further,
        // such as: the customized WASOidcClient_ token... etc
        oidcResult = attributeToSubject.doMapping(customProperties, new Subject());

        return oidcResult;
    }

    public static String getBearerAccessTokenToken(HttpServletRequest req, OidcClientConfig clientConfig) {

        String headerName = clientConfig.getHeaderName();
        if (headerName != null) {
            String hdrValue = req.getHeader(headerName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, headerName + " content=", hdrValue);
            }
            if (hdrValue != null) {
                if (hdrValue.startsWith("Bearer ")) {
                    hdrValue = hdrValue.substring(7);
                }
                return hdrValue.trim();
            } else {
                StringBuffer sb1 = new StringBuffer(headerName);
                sb1.append(JWT_SEGMENTS);
                String headerSegments = req.getHeader(sb1.toString());
                if (headerSegments != null) {
                    try {
                        int iSegs = Integer.parseInt(headerSegments);
                        StringBuffer sb3 = new StringBuffer();
                        for (int i = 1; i < iSegs + 1; i++) {
                            StringBuffer sb2 = new StringBuffer(headerName);
                            sb2.append(JWT_SEGMENT_INDEX).append(i);
                            String segHdrValue = req.getHeader(sb2.toString());
                            if (segHdrValue != null) {
                                sb3.append(segHdrValue.trim());
                            }
                        }
                        hdrValue = sb3.toString();
                        if (hdrValue != null && hdrValue.isEmpty()) {
                            hdrValue = null;
                        }
                    } catch (Exception e) {
                        //can be ignored
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Fail to read Header Segments:", e.getMessage());
                        }
                    }
                    return hdrValue;
                } else {
                    return null;
                }
            }
        } else {
            String hdrValue = req.getHeader(Authorization_Header);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Authorization header=", hdrValue);
            }
            if (hdrValue != null && hdrValue.startsWith("Bearer ")) {
                hdrValue = hdrValue.substring(7);
            } else {
                String reqMethod = req.getMethod();
                if (ClientConstants.REQ_METHOD_POST.equalsIgnoreCase(reqMethod)) {
                    String contentType = req.getHeader(ClientConstants.REQ_CONTENT_TYPE_NAME);
                    if (ClientConstants.REQ_CONTENT_TYPE_APP_FORM_URLENCODED.equals(contentType)) {
                        hdrValue = req.getParameter(ACCESS_TOKEN);
                    }
                }
            }
            return hdrValue;
        }
    }

    // do not show the error in messages.log yet. Since this will fall down to
    // RP.
    // If RP can not handle it. RP will display its own error 212457
    void logError(OidcClientConfig oidcClientConfig, String msgKey, Object... objs) {
        logError(oidcClientConfig, false, msgKey, objs);
    }

    void logError(OidcClientConfig oidcClientConfig, OidcClientRequest oidcClientRequest, String msgKey, Object... objs) {
        logError(oidcClientConfig, false, oidcClientRequest, msgKey, objs);
    }

    void logError(OidcClientConfig oidcClientConfig, boolean warningWhenSupported, String msgKey, Object... objs) {
        logError(oidcClientConfig, warningWhenSupported, null, msgKey, objs);
    }

    // do not show the error in messages.log yet. Since this will fall down to
    // RP.
    // If RP can not handle it. RP will display its own error 212457
    void logError(OidcClientConfig oidcClientConfig, boolean warningWhenSupported, OidcClientRequest oidcClientRequest, String msgKey, Object... objs) {
        String inboundPropagation = oidcClientConfig.getInboundPropagation();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ac_err msg:" + msgKey + " inboundPropagation:" + inboundPropagation + " warning?:" + warningWhenSupported);
        }
        if (ClientConstants.PROPAGATION_SUPPORTED.equalsIgnoreCase(inboundPropagation)) {
            // do not show the error in messages.log yet. Since this will fall
            // down to RP.
            // If RP can not handle it. RP will display its own error
            if (warningWhenSupported) { // But when it's the errors on the
                                        // access_token, such as: expired, we
                                        // want to do warning
                                        // TODO change the error message id to warning message id, such
                                        // as: CWWKS1732E to CWWKS1732W
                Tr.warning(tc, msgKey, objs);
            }
        } else {
            Tr.error(tc, msgKey, objs);
        }
        if (oidcClientRequest != null) {
            String existingFailMsg = oidcClientRequest.getRsFailMsg();
            if (existingFailMsg == null) {
                String format = Tr.formatMessage(tc, msgKey, objs);
                oidcClientRequest.setRsFailMsg(null, format);
            } else {
                Tr.debug(tc, "Not setting new RS fail message since one was already found: " + existingFailMsg);
            }
        }
    }
}
