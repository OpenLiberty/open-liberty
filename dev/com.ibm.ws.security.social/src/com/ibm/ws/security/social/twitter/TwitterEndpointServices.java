/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.twitter;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jose4j.json.JsonUtil;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.TwitterLoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.OAuthClientHttpUtil;
import com.ibm.ws.security.social.internal.utils.SocialUtil;

public class TwitterEndpointServices {

    private static TraceComponent tc = Tr.register(TwitterEndpointServices.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private String consumerKey = null;
    @Sensitive
    private String consumerSecret = null;

    @Sensitive
    private String tokenSecret = null;

    private final String DEFAULT_SIGNATURE_ALGORITHM = TwitterConstants.HMAC_SHA1;
    private final String DEFAULT_OAUTH_VERSION = "1.0";

    private String requestMethod = "POST";

    private OAuthClientHttpUtil httpUtil = null;

    public TwitterEndpointServices() {
        httpUtil = OAuthClientHttpUtil.getInstance();
    }

    protected void setOAuthClientHttpUtil(OAuthClientHttpUtil util) {
        httpUtil = util;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String newKey) {
        if (newKey == null || newKey.isEmpty()) {
            Tr.warning(tc, "TWITTER_MISSING_REQ_ATTR", new Object[] { TwitterLoginConfigImpl.KEY_consumerKey });
        }
        consumerKey = newKey;
    }

    public void setConsumerSecret(@Sensitive String newConsumerSecret) {
        if (newConsumerSecret == null || newConsumerSecret.isEmpty()) {
            Tr.warning(tc, "TWITTER_MISSING_REQ_ATTR", new Object[] { TwitterLoginConfigImpl.KEY_consumerSecret });
        }
        consumerSecret = newConsumerSecret;
    }

    protected void setTokenSecret(@Sensitive String newSecret) {
        tokenSecret = newSecret;
    }

    /**
     * Computes the signature for an oauth/request_token request per
     * {@link https://dev.twitter.com/oauth/overview/creating-signatures}.
     * Expects consumerSecret and tokenSecret to already be set to the desired values.
     *
     * @param requestMethod
     * @param targetUrl
     * @param params
     * @return
     */
    public String computeSignature(String requestMethod, String targetUrl, Map<String, String> params) {
        return computeSignature(requestMethod, targetUrl, params, consumerSecret, tokenSecret);
    }

    /**
     * Computes the signature for an authorized request per {@link https://dev.twitter.com/oauth/overview/creating-signatures}
     *
     * @param requestMethod
     * @param targetUrl
     * @param params
     * @param consumerSecret
     * @param tokenSecret
     * @return
     */
    public String computeSignature(String requestMethod, String targetUrl, Map<String, String> params, @Sensitive String consumerSecret, @Sensitive String tokenSecret) {

        String signatureBaseString = createSignatureBaseString(requestMethod, targetUrl, params);

        // Hash the base string using the consumer secret (and request token secret, if known) as a key
        String signature = "";
        try {
            String secretToEncode = null;
            if (consumerSecret != null) {
                secretToEncode = consumerSecret;
            }
            StringBuilder keyString = new StringBuilder(Utils.percentEncodeSensitive(secretToEncode)).append("&");
            if (tokenSecret != null) {
                keyString.append(Utils.percentEncodeSensitive(tokenSecret));
            }
            signature = computeSha1Signature(signatureBaseString, keyString.toString());

        } catch (GeneralSecurityException e) {
            Tr.warning(tc, "TWITTER_ERROR_CREATING_SIGNATURE", new Object[] { e.getLocalizedMessage() });
        } catch (UnsupportedEncodingException e) {
            // Should be using UTF-8 encoding, so this should be highly unlikely
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught unexpected exception: " + e.getLocalizedMessage(), e);
            }
        }

        return signature;
    }

    /**
     * Computes the HMAC-SHA1 signature of the provided baseString using keyString as the secret key.
     *
     * @param baseString
     * @param keyString
     * @return
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    protected String computeSha1Signature(String baseString, @Sensitive String keyString) throws GeneralSecurityException, UnsupportedEncodingException {

        byte[] keyBytes = keyString.getBytes(CommonWebConstants.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(secretKey);

        byte[] text = baseString.getBytes(CommonWebConstants.UTF_8);

        return new String(Base64.encodeBase64(mac.doFinal(text)), CommonWebConstants.UTF_8).trim();
    }

    /**
     * Per {@link https://dev.twitter.com/oauth/overview/creating-signatures}, the parameter string for signatures must be built
     * the following way:
     * 1. Percent encode every key and value that will be signed.
     * 2. Sort the list of parameters alphabetically by encoded key.
     * 3. For each key/value pair:
     * 4. Append the encoded key to the output string.
     * 5. Append the '=' character to the output string.
     * 6. Append the encoded value to the output string.
     * 7. If there are more key/value pairs remaining, append a '&' character to the output string.
     *
     * @param parameters
     * @return
     */
    public String createParameterStringForSignature(Map<String, String> parameters) {
        if (parameters == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null parameters object provided; returning empty string");
            }
            return "";
        }

        Map<String, String> encodedParams = new HashMap<String, String>();

        // Step 1: Percent encode every key and value that will be signed
        for (Entry<String, String> entry : parameters.entrySet()) {
            String encodedKey = Utils.percentEncode(entry.getKey());
            String encodedValue = Utils.percentEncode(entry.getValue());
            encodedParams.put(encodedKey, encodedValue);
        }

        // Step 2: Sort the list of parameters alphabetically by encoded key
        List<String> encodedKeysList = new ArrayList<String>();
        encodedKeysList.addAll(encodedParams.keySet());
        Collections.sort(encodedKeysList);

        StringBuilder paramString = new StringBuilder();

        // Step 3: Go through each key/value pair
        for (int i = 0; i < encodedKeysList.size(); i++) {
            String key = encodedKeysList.get(i);
            String value = encodedParams.get(key);

            // Steps 4-6: Append encoded key, "=" character, and encoded value to the output string
            paramString.append(key).append("=").append(value);

            if (i < (encodedKeysList.size() - 1)) {
                // Step 7: If more key/value pairs remain, append "&" character to the output string
                paramString.append("&");
            }
        }

        return paramString.toString();
    }

    /**
     * Per {@link https://dev.twitter.com/oauth/overview/creating-signatures}, a signature for an authorized request takes the
     * following form:
     *
     * [HTTP Method] + "&" + [Percent encoded URL] + "&" + [Percent encoded parameter string]
     *
     * - HTTP Method: Request method (either "GET" or "POST"). Must be in uppercase.
     * - Percent encoded URL: Base URL to which the request is directed, minus any query string or hash parameters. Be sure the
     * URL uses the correct protocol (http or https) that matches the actual request sent to the Twitter API.
     * - Percent encoded parameter string: Each request parameter name and value is percent encoded according to a specific
     * structure
     *
     * @param requestMethod
     * @param baseUrl
     *            Raw base URL, not percent encoded. This method will perform the percent encoding.
     * @param parameters
     *            Raw parameter names and values, not percent encoded. This method will perform the percent encoding.
     * @return
     */
    public String createSignatureBaseString(String requestMethod, String baseUrl, Map<String, String> parameters) {
        if (requestMethod == null || (!requestMethod.equalsIgnoreCase("GET") && !requestMethod.equalsIgnoreCase("POST"))) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Request method was not an expected value (GET or POST) so defaulting to POST");
            }
            requestMethod = "POST";
        }

        String cleanedUrl = removeQueryAndFragment(baseUrl);
        String parameterString = createParameterStringForSignature(parameters);

        StringBuilder signatureBaseString = new StringBuilder();
        signatureBaseString.append(requestMethod.toUpperCase());
        signatureBaseString.append("&");
        signatureBaseString.append(Utils.percentEncode(cleanedUrl));
        signatureBaseString.append("&");
        signatureBaseString.append(Utils.percentEncode(parameterString));

        return signatureBaseString.toString();
    }

    /**
     * Removes any query string and/or fragment in the provided string, if present, and returns the new string.
     *
     * @param url
     * @return
     */
    protected String removeQueryAndFragment(String url) {
        if (url == null) {
            return url;
        }
        String cleanUrl = url;

        int queryIndex = url.indexOf("?");
        int fragmentIndex = url.indexOf("#");
        if (queryIndex > 0) {
            if (fragmentIndex > 0 && fragmentIndex < queryIndex) {
                // Found fragment before the query string; remove everything after fragment
                cleanUrl = url.substring(0, fragmentIndex);
            } else {
                // Didn't find fragment or fragment is after query string; remove everything after query string
                cleanUrl = url.substring(0, queryIndex);
            }
        } else if (fragmentIndex > 0) {
            // No query string, but did find a fragment
            cleanUrl = url.substring(0, fragmentIndex);
        }
        return cleanUrl;
    }

    /**
     * Per {@link https://dev.twitter.com/oauth/overview/authorizing-requests}, the authorization header for requests must be
     * built the following way:
     * <ol type="1">
     * <li>Append the string "OAuth " (including the space at the end) to DST.</li>
     * <li>For each key/value pair of the 7 parameters* listed below:
     * <ol type="a">
     * <li>Percent encode the key and append it to DST.</li>
     * <li>Append the equals character '=' to DST.</li>
     * <li>Append a double quote '"' to DST.</li>
     * <li>Percent encode the value and append it to DST.</li>
     * <li>Append a double quote '"' to DST.</li>
     * <li>If there are key/value pairs remaining, append a comma ',' and a space ' ' to DST.</li>
     * </ol>
     * </li>
     * </ol>
     *
     * *The seven parameters are: oauth_consumer_key, oauth_nonce, oauth_signature, oauth_signature_method, oauth_timestamp,
     * oauth_token, and oauth_version.
     *
     * Note: The oauth_token parameter may be omitted in some flows. Generally this value will be an access token.
     *
     * @param parameters
     *            Raw parameter names and values, not percent encoded. This method will perform the percent encoding.
     * @return
     */
    public String createAuthorizationHeaderString(Map<String, String> parameters) {
        StringBuilder authzHeaderString = new StringBuilder();
        // Step 1: Append "OAuth "
        authzHeaderString.append("OAuth ");

        if (parameters == null) {
            return authzHeaderString.toString();
        }

        // Sort parameter names alphabetically
        List<String> sortedKeys = new ArrayList<String>();
        sortedKeys.addAll(parameters.keySet());
        Collections.sort(sortedKeys);

        // Determines whether a param was already added to the string, and therefore whether to prepend ", " to delineate the params
        boolean isParamAlreadyPresent = false;

        for (String key : sortedKeys) {

            // Step 2: Go through each of the seven expected parameters
            if (!TwitterConstants.AUTHZ_HEADER_PARAMS.contains(key)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping unexpected parameter: " + key);
                }
                continue;
            }

            if (isParamAlreadyPresent) {
                // Step 2f: Another non-ignored parameter was previously appended, so append ", " to the output string
                authzHeaderString.append(", ");
            }

            // Steps 2a-2e
            String value = parameters.get(key);
            String encodedKey = Utils.percentEncode(key);
            String encodedValue = Utils.percentEncode(value);
            authzHeaderString.append(encodedKey).append("=\"").append(encodedValue).append("\"");

            isParamAlreadyPresent = true;
        }
        return authzHeaderString.toString();
    }

    /**
     * Creates a map of parameters and values that are common to all requests that require an Authorization header.
     *
     * @return
     */
    private Map<String, String> populateCommonAuthzHeaderParams() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put(TwitterConstants.PARAM_OAUTH_CONSUMER_KEY, consumerKey);
        parameters.put(TwitterConstants.PARAM_OAUTH_NONCE, Utils.generateNonce());
        parameters.put(TwitterConstants.PARAM_OAUTH_SIGNATURE_METHOD, DEFAULT_SIGNATURE_ALGORITHM);
        parameters.put(TwitterConstants.PARAM_OAUTH_TIMESTAMP, Utils.getCurrentTimestamp());
        parameters.put(TwitterConstants.PARAM_OAUTH_VERSION, DEFAULT_OAUTH_VERSION);

        return parameters;
    }

    private Map<String, String> populateRequestTokenEndpointAuthzHeaderParams(String callbackUrl) {
        Map<String, String> parameters = populateCommonAuthzHeaderParams();
        parameters.put(TwitterConstants.PARAM_OAUTH_CALLBACK, callbackUrl);
        return parameters;
    }

    private Map<String, String> populateAuthorizedEndpointAuthzHeaderParams(String token) {
        Map<String, String> parameters = populateCommonAuthzHeaderParams();
        parameters.put(TwitterConstants.PARAM_OAUTH_TOKEN, token);
        return parameters;
    }

    private Map<String, String> populateVerifyCredentialsEndpointAuthzHeaderParams(String token) {
        Map<String, String> parameters = populateCommonAuthzHeaderParams();
        parameters.put(TwitterConstants.PARAM_OAUTH_TOKEN, token);
        parameters.put(TwitterConstants.PARAM_INCLUDE_EMAIL, TwitterConstants.INCLUDE_EMAIL);
        parameters.put(TwitterConstants.PARAM_SKIP_STATUS, TwitterConstants.SKIP_STATUS);
        return parameters;
    }

    /**
     * Generates the Authorization header with all the requisite content for the specified endpoint request by computing the
     * signature, adding it to the parameters, and generating the Authorization header string.
     *
     * @param endpointUrl
     * @param parameters
     * @return
     */
    private String signAndCreateAuthzHeader(String endpointUrl, Map<String, String> parameters) {

        String signature = computeSignature(requestMethod, endpointUrl, parameters);
        parameters.put(TwitterConstants.PARAM_OAUTH_SIGNATURE, signature);

        String authzHeaderString = createAuthorizationHeaderString(parameters);
        return authzHeaderString;
    }

    /**
     * Generates the Authorization header value required for a {@value TwitterConstants#TWITTER_ENDPOINT_REQUEST_TOKEN} endpoint
     * request. See {@link https://dev.twitter.com/oauth/reference/post/oauth/request_token} for details.
     *
     * @param callbackUrl
     * @param endpointUrl
     * @return
     */
    public String createAuthzHeaderForRequestTokenEndpoint(String callbackUrl, String endpointUrl) {
        Map<String, String> parameters = populateRequestTokenEndpointAuthzHeaderParams(callbackUrl);
        return signAndCreateAuthzHeader(endpointUrl, parameters);
    }

    /**
     * Generates the Authorization header value required for an authorized endpoint request. Assumes that {@code tokenSecret} has
     * already been set to the appropriate value based on the provided token. See
     * {@link https://dev.twitter.com/oauth/reference/post/oauth/access_token} for details.
     *
     * @param endpointUrl
     * @param token
     *            For {@value TwitterConstants#TWITTER_ENDPOINT_ACCESS_TOKEN} requests, this is the request token obtained in an
     *            earlier call. For other authorized requests, this is a valid access token.
     * @return
     */
    public String createAuthzHeaderForAuthorizedEndpoint(String endpointUrl, String token) {
        Map<String, String> parameters = populateAuthorizedEndpointAuthzHeaderParams(token);
        return signAndCreateAuthzHeader(endpointUrl, parameters);
    }

    /**
     * Generates the Authorization header value required for a {@value TwitterConstants#TWITTER_ENDPOINT_VERIFY_CREDENTIALS}
     * endpoint request. See {@link https://dev.twitter.com/rest/reference/get/account/verify_credentials} for details.
     *
     * @param endpointUrl
     * @param token
     * @return
     */
    public String createAuthzHeaderForVerifyCredentialsEndpoint(String endpointUrl, String token) {
        Map<String, String> parameters = populateVerifyCredentialsEndpointAuthzHeaderParams(token);
        return signAndCreateAuthzHeader(endpointUrl, parameters);
    }

    /**
     * Populates a map of key/value pairs from the response body. Because the response body may contain token secrets, that value
     * has been annotated as @Sensitive. This method expects the responseBody value to be formatted:
     * key=value[&key2=value2]*
     *
     * @param responseBody
     * @return
     */
    @Sensitive
    public Map<String, String> populateResponseValues(@Sensitive String responseBody) {
        Map<String, String> responseValues = new HashMap<String, String>();

        if (responseBody == null) {
            return responseValues;
        }

        StringTokenizer st = new StringTokenizer(responseBody, "&");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            String[] keyValue = token.split("=");
            String key = keyValue[0];
            String value = "";
            if (keyValue.length > 1) {
                value = keyValue[1];
            }
            if (tc.isDebugEnabled()) {
                // Do not trace token secrets
                Tr.debug(tc, key + ": [" + (TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET.equals(key) ? "****" : value) + "]");
            }
            responseValues.put(key, value);
        }
        return responseValues;
    }

    /**
     * Populates a Map from the response body. This method expects the responseBody value to be in JSON format.
     *
     * @param responseBody
     * @return {@code null} if the response body was {@code null} or empty. Otherwise returns a Map with all entries and values
     *         contained in the response body.
     * @throws JoseException
     */
    public Map<String, Object> populateJsonResponse(String responseBody) throws JoseException {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        return JsonUtil.parseJson(responseBody);
    }

    /**
     * Creates and returns a Map with a {@value TwitterConstants#RESULT_RESPONSE_STATUS} entry with its value set to
     * {@value TwitterConstants#RESULT_ERROR} and a {@value TwitterConstants#RESULT_MESSAGE} entry set to a translated error
     * message using the arguments provided.
     *
     * @param msgKey
     * @param args
     * @return
     */
    protected Map<String, Object> createErrorResponse(String msgKey, Object[] args) {
        Map<String, Object> response = new HashMap<String, Object>();

        if (msgKey == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null message key provided; result will include empty error message");
            }
            msgKey = "";
        }
        String errorMsg = Tr.formatMessage(tc, msgKey, args);

        response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_ERROR);
        response.put(TwitterConstants.RESULT_MESSAGE, errorMsg);

        return response;
    }

    /**
     * Creates and returns a Map with a {@value TwitterConstants#RESULT_RESPONSE_STATUS} entry with its value set to
     * {@value TwitterConstants#RESULT_ERROR} and a {@value TwitterConstants#RESULT_MESSAGE} entry set to the message of the
     * provided exception.
     *
     * @param e
     * @return
     */
    protected Map<String, Object> createErrorResponse(Exception exception) {
        Map<String, Object> response = new HashMap<String, Object>();

        response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_ERROR);
        response.put(TwitterConstants.RESULT_MESSAGE, exception.getLocalizedMessage());

        return response;
    }

    /**
     * Checks that the provided response body includes at least one parameter.
     *
     * @param endpoint
     * @param responseBody
     *            May contain token secrets, so has been annotated as @Sensitive.
     * @param responseValues
     *            May contain token secrets, so has been annotated as @Sensitive.
     * @return {@code null} if the response contained at least one parameter. If no parameters were present, this returns a
     *         Map with an error response status and error message.
     */
    public <T extends Object> Map<String, Object> checkForEmptyResponse(String endpoint, @Sensitive String responseBody, @Sensitive Map<String, T> responseValues) {
        if (responseValues == null || responseValues.isEmpty()) {
            return createErrorResponse("TWITTER_RESPONSE_HAS_NO_PARAMS", new Object[] { endpoint, responseBody });
        }
        return null;
    }

    /**
     * Checks that the provided map contains all of the required parameters.
     *
     * @param endpoint
     * @param responseValues
     *            May contain token secrets, so has been annotated as @Sensitive.
     * @param requiredParams
     * @return {@code null} if the map contains all of the specified required parameters. If any parameters are missing, a
     *         Map with an error response status and error message is returned.
     */
    public <T extends Object> Map<String, Object> checkForRequiredParameters(String endpoint, @Sensitive Map<String, T> responseValues, String... requiredParams) {
        if (responseValues == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The provided response values map is null, so all required parameters will be determined to be missing");
            }
        }
        List<String> missingParams = new ArrayList<String>();
        for (String param : requiredParams) {
            if (responseValues == null || !responseValues.containsKey(param)) {
                missingParams.add(param);
            }
        }
        if (!missingParams.isEmpty()) {
            return createErrorResponse("TWITTER_RESPONSE_MISSING_PARAMETER", new Object[] { endpoint, Arrays.toString(missingParams.toArray(new String[0])) });
        }
        return null;
    }

    /**
     * Evaluate the response from the oauth/request_token endpoint. This checks the status code of the response and ensures
     * that oauth_callback_confirmed, oauth_token, and oauth_token_secret values are contained in the response.
     *
     * @param responseBody
     * @return
     */
    public Map<String, Object> evaluateRequestTokenResponse(String responseBody) {
        Map<String, Object> response = new HashMap<String, Object>();
        String endpoint = TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN;

        Map<String, String> responseValues = populateResponseValues(responseBody);

        Map<String, Object> result = checkForEmptyResponse(endpoint, responseBody, responseValues);
        if (result != null) {
            return result;
        }

        // Ensure response contains oauth_callback_confirmed value
        result = checkForRequiredParameters(endpoint, responseValues, TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED, TwitterConstants.RESPONSE_OAUTH_TOKEN, TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET);
        if (result != null) {
            return result;
        }

        String callbackConfirmedVal = responseValues.get(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED);
        if (!callbackConfirmedVal.equalsIgnoreCase("true")) {
            return createErrorResponse("TWITTER_RESPONSE_PARAM_WITH_WRONG_VALUE",
                    new Object[] { TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED, endpoint, "true", callbackConfirmedVal });
        }

        String requestToken = "";

        for (Entry<String, String> entry : responseValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equals(TwitterConstants.RESPONSE_OAUTH_TOKEN)) {
                requestToken = value;
                if (requestToken.isEmpty()) {
                    return createErrorResponse("TWITTER_RESPONSE_PARAMETER_EMPTY", new Object[] { TwitterConstants.RESPONSE_OAUTH_TOKEN, endpoint });
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, TwitterConstants.RESPONSE_OAUTH_TOKEN + "=" + requestToken);
                }
            } else if (key.equals(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET)) {
                tokenSecret = value;
                if (tokenSecret.isEmpty()) {
                    return createErrorResponse("TWITTER_RESPONSE_PARAMETER_EMPTY", new Object[] { TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET, endpoint });
                }
                if (tc.isDebugEnabled()) {
                    // Request token secrets are short lived, so logging them should not be an issue
                    Tr.debug(tc, TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + tokenSecret);
                }
            } else if (!key.equals(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found an unexpected parameter in the response: " + key + "=" + value);
                }
            }
        }

        response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);
        response.put(TwitterConstants.RESPONSE_OAUTH_TOKEN, requestToken);
        response.put(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET, tokenSecret);

        return response;
    }

    /**
     * Evaluate the response from the oauth/access_token endpoint. This checks the status code of the response and ensures
     * that oauth_token and oauth_token_secret values are contained in the response. Also looks for user_id and screen_name
     * values.
     *
     * @param responseBody
     *            Response should contain sensitive token secret, so this is annotated @Sensitive.
     * @return
     */
    @Sensitive
    public Map<String, Object> evaluateAccessTokenResponse(@Sensitive String responseBody) {
        Map<String, Object> response = new HashMap<String, Object>();
        String endpoint = TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN;

        Map<String, String> responseValues = populateResponseValues(responseBody);

        Map<String, Object> result = checkForEmptyResponse(endpoint, responseBody, responseValues);
        if (result != null) {
            return result;
        }

        // Ensure response contains oauth_token and oauth_token_secret values
        result = checkForRequiredParameters(endpoint, responseValues, TwitterConstants.RESPONSE_OAUTH_TOKEN, TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET);
        if (result != null) {
            return result;
        }

        String accessToken = "";
        String userId = "";
        String screenName = "";

        for (Entry<String, String> entry : responseValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equals(TwitterConstants.RESPONSE_OAUTH_TOKEN)) {
                accessToken = value;
                if (accessToken.isEmpty()) {
                    return createErrorResponse("TWITTER_RESPONSE_PARAMETER_EMPTY", new Object[] { TwitterConstants.RESPONSE_OAUTH_TOKEN, endpoint });
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, TwitterConstants.RESPONSE_OAUTH_TOKEN + "=" + accessToken);
                }
            } else if (key.equals(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET)) {
                tokenSecret = value;
                if (tokenSecret.isEmpty()) {
                    return createErrorResponse("TWITTER_RESPONSE_PARAMETER_EMPTY", new Object[] { TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET, endpoint });
                }
                if (tc.isDebugEnabled()) {
                    // Do not trace access token secrets
                    Tr.debug(tc, TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=****");
                }
            } else if (key.equals(TwitterConstants.RESPONSE_USER_ID)) {
                userId = value;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, TwitterConstants.RESPONSE_USER_ID + "=" + userId);
                }
            } else if (key.equals(TwitterConstants.RESPONSE_SCREEN_NAME)) {
                screenName = value;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, TwitterConstants.RESPONSE_SCREEN_NAME + "=" + screenName);
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found an unexpected parameter in the response: " + key + "=" + value);
                }
            }
        }

        response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);
        response.put(TwitterConstants.RESULT_ACCESS_TOKEN, accessToken);
        response.put(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET, tokenSecret);
        response.put(TwitterConstants.RESULT_USER_ID, userId);
        response.put(TwitterConstants.RESULT_SCREEN_NAME, screenName);

        return response;
    }

    /**
     * Evaluate the response from the {@value TwitterConstants#TWITTER_ENDPOINT_VERIFY_CREDENTIALS} endpoint. This checks the
     * status code of the response and ensures that an email value is contained in the response.
     *
     * @param responseBody
     * @return
     */
    public Map<String, Object> evaluateVerifyCredentialsResponse(String responseBody) {
        String endpoint = TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS;

        Map<String, Object> responseValues = null;
        try {
            responseValues = populateJsonResponse(responseBody);
        } catch (JoseException e) {
            return createErrorResponse("TWITTER_RESPONSE_NOT_JSON", new Object[] { endpoint, e.getLocalizedMessage(), responseBody });
        }

        Map<String, Object> result = checkForEmptyResponse(endpoint, responseBody, responseValues);
        if (result != null) {
            return result;
        }

        // Ensure response contains email
        result = checkForRequiredParameters(endpoint, responseValues, TwitterConstants.RESPONSE_EMAIL);
        if (result != null) {
            return result;
        }

        responseValues.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);
        return responseValues;
    }

    @Sensitive
    public Map<String, Object> evaluateRequestResponse(@Sensitive String responseBody, String endpointPath) {
        return evaluateRequestResponse(responseBody, HttpServletResponse.SC_OK, endpointPath);
    }

    /**
     * Evaluate the response from the specified endpoint. This checks the status code of the response and ensures that the
     * expected content is contained in the response.
     *
     * @param responseBody
     *            May contain token secrets, so annotated as @Sensitive.
     * @param statusLine
     * @param endpointPath
     * @return
     */
    @Sensitive
    public Map<String, Object> evaluateRequestResponse(@Sensitive String responseBody, int statusLine, String endpointPath) {
        Map<String, Object> response = null;

        if (responseBody == null) {
            return createErrorResponse("TWITTER_EMPTY_RESPONSE_BODY", new Object[] { endpointPath });
        }

        // Error status
        if (HttpServletResponse.SC_OK != statusLine) {
            return createErrorResponse("TWITTER_ENDPOINT_REQUEST_FAILED", new Object[] { endpointPath, statusLine, responseBody });
        }

        if (endpointPath == null) {
            endpointPath = TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A Twitter endpoint path was not found; defaulting to using " + endpointPath + " as the Twitter endpoint path.");
            }
        }

        if (endpointPath.equals(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN)) {
            response = evaluateRequestTokenResponse(responseBody);
        } else if (endpointPath.equals(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN)) {
            response = evaluateAccessTokenResponse(responseBody);
        } else if (endpointPath.equals(TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS)) {
            response = evaluateVerifyCredentialsResponse(responseBody);
        }

        return response;
    }

    /**
     * Sends a request to the specified Twitter endpoint and returns a Map object containing the evaluated response.
     *
     * @param config
     * @param requestMethod
     * @param authzHeaderString
     * @param url
     * @param endpointType
     * @param verifierValue
     *            Only used for {@value TwitterConstants#TWITTER_ENDPOINT_ACCESS_TOKEN} requests
     * @return
     */
    @FFDCIgnore(SocialLoginException.class)
    @Sensitive
    public Map<String, Object> executeRequest(SocialLoginConfig config, String requestMethod, String authzHeaderString, String url, String endpointType, String verifierValue) {

        if (endpointType == null) {
            endpointType = TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A Twitter endpoint path was not found; defaulting to using " + endpointType + " as the Twitter endpoint path.");
            }
        }

        try {
            SocialUtil.validateEndpointWithQuery(url);
        } catch (SocialLoginException e) {
            return createErrorResponse(e);
        }

        StringBuilder uri = new StringBuilder(url);
        if (endpointType.equals(TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS)) {
            // Include the include_email and skip_status parameters for these endpoint requests
            uri.append("?").append(TwitterConstants.PARAM_INCLUDE_EMAIL).append("=").append(TwitterConstants.INCLUDE_EMAIL).append("&").append(TwitterConstants.PARAM_SKIP_STATUS).append("=").append(TwitterConstants.SKIP_STATUS);
        }

        try {
            Map<String, Object> result = getEndpointResponse(config, uri.toString(), requestMethod, authzHeaderString, endpointType, verifierValue);

            String responseContent = httpUtil.extractTokensFromResponse(result);

            return evaluateRequestResponse(responseContent, endpointType);

        } catch (SocialLoginException e) {
            return createErrorResponse("TWITTER_EXCEPTION_EXECUTING_REQUEST", new Object[] { url, e.getLocalizedMessage() });
        }
    }

    protected Map<String, Object> getEndpointResponse(SocialLoginConfig config, String uri, String requestMethod, String authzHeaderString, String endpointPath, String verifierValue) throws SocialLoginException {
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = config.getSSLSocketFactory();
        } catch (Exception e) {
            throw new SocialLoginException("FAILED_TO_GET_SSL_CONTEXT", e, new Object[] { config.getUniqueId(), e.getLocalizedMessage() });
        }

        SocialUtil.validateEndpointWithQuery(uri);

        // Set up the requisite headers
        List<NameValuePair> headers = new ArrayList<NameValuePair>();
        headers.add(new BasicNameValuePair(CommonWebConstants.HTTP_HEADER_CONTENT_TYPE, CommonWebConstants.HTTP_CONTENT_TYPE_FORM_URL_ENCODED));
        if (authzHeaderString != null) {
            headers.add(new BasicNameValuePair(CommonWebConstants.HTTP_HEADER_AUTHORIZATION, authzHeaderString));
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();

        if (endpointPath != null && endpointPath.equals(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding verifier value [" + verifierValue + "] to POST body");
            }
            // Include the oauth_verifier value obtained earlier
            // Note: Percent encoding the oauth_verifier value will also protect against HTTP response splitting
            params.add(new BasicNameValuePair(TwitterConstants.PARAM_OAUTH_VERIFIER, Utils.percentEncode(verifierValue)));
        }

        Map<String, Object> result = null;
        if (requestMethod != null && requestMethod.equalsIgnoreCase("POST")) {
            result = httpUtil.postToEndpoint(uri.toString(), params, null, null, null, sslSocketFactory, headers, false, null, config.getUseSystemPropertiesForHttpClientConnections());
        } else {
            result = httpUtil.getToEndpoint(uri.toString(), params, null, null, null, sslSocketFactory, headers, false, null, config.getUseSystemPropertiesForHttpClientConnections());
        }
        return result;
    }

    /**
     * Invokes the {@value TwitterConstants#TWITTER_ENDPOINT_REQUEST_TOKEN} endpoint in order to obtain a request token. The
     * request is authorized for the consumer key set by the class and the callback URL provided to the method. The appropriate
     * consumer key must be set before invoking this method in order to obtain a request token for the correct consumer. For more
     * information, see {@link https://dev.twitter.com/oauth/reference/post/oauth/request_token}.
     *
     * @param config
     * @param callbackUrl
     * @return
     */
    public Map<String, Object> obtainRequestToken(SocialLoginConfig config, String callbackUrl) {

        String endpointUrl = config.getRequestTokenUrl();
        try {
            SocialUtil.validateEndpointWithQuery(endpointUrl);
        } catch (SocialLoginException e) {
            return createErrorResponse("TWITTER_BAD_REQUEST_TOKEN_URL", new Object[] { endpointUrl, TwitterLoginConfigImpl.KEY_requestTokenUrl, config.getUniqueId(), e.getLocalizedMessage() });
        }

        // Create the Authorization header string necessary to authenticate the request
        String authzHeaderString = createAuthzHeaderForRequestTokenEndpoint(callbackUrl, endpointUrl);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Authz header string: " + authzHeaderString);
        }

        return executeRequest(config, requestMethod, authzHeaderString, endpointUrl, TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, null);
    }

    /**
     * Invokes the {@value TwitterConstants#TWITTER_ENDPOINT_ACCESS_TOKEN} endpoint in order to obtain an access token. The
     * request is authorized for the consumer key set by the class and the previously obtained request token. The appropriate
     * consumer key must be set before invoking this method in order to obtain an access token for the correct consumer. The
     * verifierOrPinValue argument must match the oauth_verifier or PIN value returned from an earlier
     * {@value TwitterConstants#TWITTER_ENDPOINT_AUTHORIZE} request. For more information, see
     * {@link https://dev.twitter.com/oauth/reference/post/oauth/access_token}.
     *
     * @param config
     * @param requestToken
     * @param verifierOrPinValue
     * @return
     */
    @Sensitive
    public Map<String, Object> obtainAccessToken(SocialLoginConfig config, String requestToken, String verifierOrPinValue) {

        String endpointUrl = config.getTokenEndpoint();
        try {
            SocialUtil.validateEndpointWithQuery(endpointUrl);
        } catch (SocialLoginException e) {
            return createErrorResponse("TWITTER_BAD_ACCESS_TOKEN_URL", new Object[] { endpointUrl, TwitterLoginConfigImpl.KEY_accessTokenUrl, config.getUniqueId(), e.getLocalizedMessage() });
        }

        // Create the Authorization header string necessary to authenticate the request
        String authzHeaderString = createAuthzHeaderForAuthorizedEndpoint(endpointUrl, requestToken);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Authz header string: " + authzHeaderString);
        }

        return executeRequest(config, requestMethod, authzHeaderString, endpointUrl, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, verifierOrPinValue);
    }

    /**
     * Invokes the {@value TwitterConstants#TWITTER_ENDPOINT_VERIFY_CREDENTIALS} endpoint in order to obtain a value to use for
     * the user subject.
     *
     * @param config
     * @param accessToken
     * @param accessTokenSecret
     * @return
     */
    public Map<String, Object> verifyCredentials(SocialLoginConfig config, String accessToken, @Sensitive String accessTokenSecret) {

        String endpointUrl = config.getUserApi();
        try {
            SocialUtil.validateEndpointWithQuery(endpointUrl);
        } catch (SocialLoginException e) {
            return createErrorResponse("TWITTER_BAD_USER_API_URL", new Object[] { endpointUrl, TwitterLoginConfigImpl.KEY_userApi, config.getUniqueId(), e.getLocalizedMessage() });
        }

        tokenSecret = accessTokenSecret;
        requestMethod = "GET";

        // Create the Authorization header string necessary to authenticate the request
        String authzHeaderString = createAuthzHeaderForVerifyCredentialsEndpoint(endpointUrl, accessToken);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Authz header string: " + authzHeaderString);
        }

        return executeRequest(config, requestMethod, authzHeaderString, endpointUrl, TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS, null);
    }

}
