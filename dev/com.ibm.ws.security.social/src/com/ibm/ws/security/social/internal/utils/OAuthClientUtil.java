/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.webcontainer.internalRuntimeExport.srt.IPrivateRequestAttributes;

/**
 *
 */
public class OAuthClientUtil {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(OAuthClientUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private final List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();
    private final List<NameValuePair> commonPostHeaders = new ArrayList<NameValuePair>();

    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";

    OAuthClientHttpUtil httpUtil = null;

    public OAuthClientUtil() {
        commonHeaders.add(new BasicNameValuePair(CommonWebConstants.HTTP_HEADER_ACCEPT, "application/json"));
        // commonHeaders.add(new BasicNameValuePair("Accept-Encoding",
        // "gzip, deflate"));
        commonPostHeaders.add(new BasicNameValuePair(CommonWebConstants.HTTP_HEADER_ACCEPT, "application/json"));
        commonPostHeaders.add(new BasicNameValuePair(CommonWebConstants.HTTP_HEADER_CONTENT_TYPE, CommonWebConstants.HTTP_CONTENT_TYPE_FORM_URL_ENCODED));
        init(OAuthClientHttpUtil.getInstance());
    }

    void init(OAuthClientHttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    /*
     * get CommonHeaders
     */
    final List<NameValuePair> getCommonHeaders() {
        return commonHeaders;
    }

    public Map<String, Object> getTokensFromAuthzCode(String tokenEndpoint,
            String clientId,
            @Sensitive String clientSecret,
            String redirectUri,
            String code,
            String grantType,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            String authMethod,
            String resources,
            boolean useJvmProps) throws SocialLoginException {

        if (tokenEndpoint == null || tokenEndpoint.isEmpty()) {
            throw new SocialLoginException("TOKEN_ENDPOINT_NULL_OR_EMPTY", null, new Object[0]);
        }

        SocialUtil.validateEndpointWithQuery(tokenEndpoint);

        if (clientId == null || clientId.isEmpty()) {
            Tr.warning(tc, "OUTGOING_REQUEST_MISSING_PARAMETER", new Object[] { tokenEndpoint, ClientConstants.CLIENT_ID });
            clientId = "";
        }
        if (clientSecret == null || clientSecret.isEmpty()) {
            Tr.warning(tc, "OUTGOING_REQUEST_MISSING_PARAMETER", new Object[] { tokenEndpoint, ClientConstants.CLIENT_SECRET });
            clientSecret = "";
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (grantType != null) {
            params.add(new BasicNameValuePair(ClientConstants.GRANT_TYPE, grantType));
        }
        if (resources != null) {
            params.add(new BasicNameValuePair("resource", resources));
        }
        if (redirectUri != null) {
            params.add(new BasicNameValuePair(ClientConstants.REDIRECT_URI, redirectUri));
        }
        if (code != null) {
            params.add(new BasicNameValuePair(ClientConstants.CODE, code));
        }
        if (authMethod != null && authMethod.equals(Constants.client_secret_post)) {
            params.add(new BasicNameValuePair(ClientConstants.CLIENT_ID, clientId));
            params.add(new BasicNameValuePair(ClientConstants.CLIENT_SECRET, clientSecret));
        }

        Map<String, Object> postResponseMap = httpUtil.postToEndpoint(tokenEndpoint, params,
                clientId, clientSecret, null, sslSocketFactory, commonPostHeaders, isHostnameVerification, authMethod, useJvmProps);

        String tokenResponse = httpUtil.extractTokensFromResponse(postResponseMap);
        if (tokenResponse == null) {
            Tr.warning(tc, "POST_RESPONSE_NULL", new Object[] { tokenEndpoint, postResponseMap });
            return new HashMap<String, Object>();
        }

        Map<String, Object> tokens;
        try {
            tokens = JsonUtil.parseJson(tokenResponse);
        } catch (JoseException e) {
            Tr.warning(tc, "ENDPOINT_RESPONSE_NOT_JSON", new Object[] { tokenEndpoint, e.getMessage(), tokenResponse });
            return new HashMap<String, Object>();
        }

        return tokens;
    }

    public Map<String, Object> checkToken(String tokenEndpoint, String clientId, @Sensitive String clientSecret,
            @Sensitive String accessToken, boolean isHostnameVerification, String authMethod, SSLSocketFactory sslSocketFactory, boolean useJvmProps) throws SocialLoginException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (accessToken != null) {
            params.add(new BasicNameValuePair("token", accessToken));
        }

        if (authMethod != null && authMethod.equals(ClientConstants.METHOD_client_secret_post)) {
            params.add(new BasicNameValuePair(ClientConstants.CLIENT_ID, clientId));
            params.add(new BasicNameValuePair(ClientConstants.CLIENT_SECRET, clientSecret));
        }

        Map<String, Object> postResponseMap = postToCheckTokenEndpoint(tokenEndpoint,
                params, clientId, clientSecret, isHostnameVerification, authMethod, sslSocketFactory, useJvmProps);

        // String tokenResponse =
        // httpUtil.extractTokensFromResponse(postResponseMap);

        // return tokenResponse;
        return postResponseMap;
    }

    public Map<String, Object> getUserApi(String userApi, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification, boolean needsSpecialHeader, boolean useJvmProps) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        boolean isLinkedIn = userApi != null && userApi.contains("https://api.linkedin.com");
        if (accessToken != null && !isLinkedIn) { //linkedin v2 api won't tolerate this param.
            params.add(new BasicNameValuePair("access_token", accessToken));
        }

        Map<String, Object> getResponseMap = getFromUserApiEndpoint(userApi, params, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, useJvmProps);
        return getResponseMap;
        // String userApiResponse =
        // httpUtil.extractTokensFromResponse(getResponseMap);

        // return userApiResponse;
    }

    public String getUserApiResponse(String userApi, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, boolean needsSpecialHeader, boolean useJvmProps) throws Exception {
        Map<String, Object> getResponseMap = getUserApi(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, useJvmProps);
        return getJsonStringResponse(getResponseMap, userApi);
    }

    public JwtToken getUserApiAsJwtToken(String userApi, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, SocialLoginConfig clientConfig) throws Exception {
        Map<String, Object> getResponseMap = getUserApi(userApi, accessToken, sslSocketFactory, isHostnameVerification, clientConfig.getUserApiNeedsSpecialHeader(), clientConfig.getUseSystemPropertiesForHttpClientConnections());
        String jsonString = getJsonStringResponse(getResponseMap, userApi);
        if (jsonString != null) {
            return createJwtTokenFromJson(jsonString, clientConfig.getJwtRef());
        } else {
            throw new SocialLoginException("USERAPI_NULL_RESP_STR", null, new Object[] { userApi });
        }
    }

    protected String getJsonStringResponse(Map<String, Object> responseMap, String userApi) throws SocialLoginException {
        String jresponse = null;
        if (responseMap == null) {
            return null;
        }

        if (responseMap.containsKey(ClientConstants.RESPONSEMAP_CODE)) {
            HttpResponse response = (HttpResponse) responseMap.get(ClientConstants.RESPONSEMAP_CODE);
            if (isErrorResponse(response)) {
                handleError(response, userApi);
            } else {
                // HTTP 200 response
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try {
                        jresponse = EntityUtils.toString(entity);
                    } catch (ParseException e) {
                        throw new SocialLoginException("USERAPI_RESP_PROCESS_ERR", e, new Object[] { userApi, e.getLocalizedMessage() });
                    } catch (IOException e) {
                        throw new SocialLoginException("USERAPI_RESP_PROCESS_ERR", e, new Object[] { userApi, e.getLocalizedMessage() });
                    }
                }
            }
        }
        return jresponse;
    }

    void handleError(HttpResponse response, String userApi) throws SocialLoginException {
        String jresponse = null;
        Object err = null;
        String err_desc = null;
        int status_code;

        if (response == null) {
            // Should not happen
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "HttpResponse is null so nothing to do");
            }
            return;
        }

        try {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine == null) {
                status_code = HttpServletResponse.SC_FORBIDDEN;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "StatusLine object not returned from response, so defaulting to status code of " + status_code);
                }
            } else {
                status_code = statusLine.getStatusCode();
            }
            HttpEntity entity = response.getEntity();
            jresponse = EntityUtils.toString(entity);
            if (jresponse == null || jresponse.isEmpty()) {
                err_desc = getErrorDescriptionFromAuthenticateHeader(response);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "error response from the user api ep: ", jresponse);
                }
                Map<String, Object> errorjsonmap = JsonUtil.parseJson(jresponse);
                err = errorjsonmap.get(ERROR);
                err_desc = (String) errorjsonmap.get(ERROR_DESCRIPTION);
                //240842 - some social media don't use the prescribed fields. If we got something unprescribed,
                // then reveal it to the user.
                if (err == null && err_desc == null && jresponse != null) {
                    err = jresponse;
                }
            }

        } catch (Exception e) {
            throw new SocialLoginException("USERAPI_ERROR_RESPONSE", e, new Object[] { userApi, e.getLocalizedMessage() });
        }
        throw new SocialLoginException("USERAPI_RESP_INVALID_STATUS", null, new Object[] { userApi, status_code, err, err_desc });
    }

    String getErrorDescriptionFromAuthenticateHeader(HttpResponse response) {
        // WWW-Authenticate: Bearer error=invalid_token,
        // error_description=CWWKS1617E: A userinfo request was made
        // with an access token that was not recognized. The request
        // URI was /oidc/endpoint/OidcConfigSample/userinfo.
        Header header = response.getFirstHeader("WWW-Authenticate");
        String jresponse = header == null ? null : header.getValue();
        return extractErrorDescription(jresponse);
    }

    protected String extractErrorDescription(String response) {
        if (response == null) {
            return null;
        }

        // "error_description" must not be immediately preceded by an
        // alphanumeric character - it must be recognizable as a distinct entry
        String regexHeader = "(?:.*[^a-zA-Z0-9])?" + ERROR_DESCRIPTION + "=(.*)";

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

    /*
     * Check the response from the endpoint to see if there was an error
     */
    boolean isErrorResponse(HttpResponse response) {
        StatusLine status = response.getStatusLine();
        if (status == null || status.getStatusCode() != 200) {
            return true;
        }
        return false;
    }

    public JwtToken getJwtTokenFromJson(String jsonString, SocialLoginConfig clientConfig) throws Exception {
        return createJwtTokenFromJson(jsonString, clientConfig.getJwtRef());
    }

    protected JwtToken createJwtTokenFromJson(String jsonString, String jwtRef) throws Exception {
        JwtBuilder jwtBuilder = JwtBuilder.create(jwtRef).claimFrom(jsonString);
        return jwtBuilder.buildJwt();
    }

    public JwtToken createJwtToken(String jsonString) throws Exception {
        JwtContext jwtCtx = parseJwtWithoutValidation(jsonString, 3 * 60l);
        Map<String, Object> claims = jwtCtx.getJwtClaims().getClaimsMap();
        JwtBuilder jwtBuilder = JwtBuilder.create().claim(claims);
        return jwtBuilder.buildJwt();
    }

    protected JwtContext parseJwtWithoutValidation(String jwtString, long clockSkewInMilliseconds) throws Exception {
        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        builder.setSkipAllValidators();
        builder.setDisableRequireSignature();
        builder.setSkipSignatureVerification();
        builder.setAllowedClockSkewInSeconds((int) (clockSkewInMilliseconds / 1000));

        JwtConsumer firstPassJwtConsumer = builder.build();

        JwtContext jwtContext = firstPassJwtConsumer.process(jwtString);
        return jwtContext;
    }

    Map<String, Object> postToTokenEndpoint(String tokenEnpoint,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            String authMethod, boolean useJvmProps)
            throws Exception {
        return httpUtil.postToEndpoint(tokenEnpoint, params, baUsername, baPassword, null, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, useJvmProps);
    }

    Map<String, Object> getToTokenEndpoint(String tokenEnpoint,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            String authMethod, boolean useJvmProps)
            throws Exception {
        return httpUtil.getToEndpoint(tokenEnpoint, params, baUsername, baPassword, null, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, useJvmProps);
    }

    Map<String, Object> postToCheckTokenEndpoint(String tokenEnpoint,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            boolean isHostnameVerification,
            String authMethod,
            SSLSocketFactory sslSocketFactory, boolean useJvmProps)
            throws SocialLoginException {
        return httpUtil.postToIntrospectEndpoint(tokenEnpoint, params,
                baUsername, baPassword, null, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, useJvmProps);
    }

    Map<String, Object> getFromUserApiEndpoint(String userApiEndpoint,
            @Sensitive List<NameValuePair> params,
            @Sensitive String accessToken,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification,
            boolean needsSpecialHeader,
            boolean useJvmProps) throws ClientProtocolException, IOException, SocialLoginException {
        return getFromEndpoint(userApiEndpoint, params, null, null, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, useJvmProps);
    }

    Map<String, Object> getFromEndpoint(String url,
            @Sensitive List<NameValuePair> params,
            String baUsername,
            @Sensitive String baPassword,
            @Sensitive String accessToken,
            SSLSocketFactory sslSocketFactory,
            boolean isHostnameVerification, boolean needsSpecialHeader, boolean useJvmProps) throws ClientProtocolException, IOException, SocialLoginException {

        SocialUtil.validateEndpointWithQuery(url);

        String query = null;
        if (params == null) {
            params = new ArrayList<NameValuePair>();
        }
        //params.add(new BasicNameValuePair("format", "json"));
        query = URLEncodedUtils.format(params, ClientConstants.CHARSET);

        if (query != null && !query.isEmpty()) {
            if (!url.endsWith("?")) {
                if (url.contains("?")) {
                    url += "&";
                } else {
                    url += "?";
                }
            }
            url += query;
        }
        HttpGet request = new HttpGet(url);
        for (NameValuePair nameValuePair : commonHeaders) {
            request.addHeader(nameValuePair.getName(), nameValuePair.getValue());
        }
        if (needsSpecialHeader) {
            request.addHeader("x-li-format", "json");
            request.addHeader("Authorization", "Bearer " + accessToken); // We need this for linkedIn, tested with Facebook also
        }

        HttpClient httpClient = baUsername != null ? httpUtil.createHTTPClient(sslSocketFactory, url, isHostnameVerification, baUsername, baPassword, useJvmProps) : httpUtil.createHTTPClient(sslSocketFactory, url, isHostnameVerification, useJvmProps);

        HttpResponse responseCode = httpClient.execute(request);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ClientConstants.RESPONSEMAP_CODE, responseCode);
        result.put(ClientConstants.RESPONSEMAP_METHOD, request);

        return result;
    }

    protected Integer getRedirectPortFromRequest(HttpServletRequest req) {
        HttpServletRequest sr = getWrappedServletRequestObject(req);
        if (sr instanceof IPrivateRequestAttributes) {
            return (Integer) ((IPrivateRequestAttributes) sr).getPrivateAttribute("SecurityRedirectPort");
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getRedirectUrl called for non-IPrivateRequestAttributes object", req);
            }
            return null;
        }

    }

    /**
     * Drill down through any possible HttpServletRequestWrapper objects.
     *
     * @param sr
     * @return
     */
    private static HttpServletRequest getWrappedServletRequestObject(HttpServletRequest sr) {
        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            // make sure we drill all the way down to an
            // SRTServletRequest...there
            // may be multiple proxied objects
            sr = (HttpServletRequest) w.getRequest();
            while (sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }
        return sr;
    }

}