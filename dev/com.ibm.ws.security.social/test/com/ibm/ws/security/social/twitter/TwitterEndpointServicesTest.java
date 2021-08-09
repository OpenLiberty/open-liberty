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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jmock.Expectations;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.OAuthClientHttpUtil;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class TwitterEndpointServicesTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    // Values consistent with https://dev.twitter.com/oauth/overview/creating-signatures
    final String CONSUMER_KEY = "xvz1evFS4wEEPTGEFPHBog";
    @Sensitive
    final String CONSUMER_SECRET = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
    @Sensitive
    final String EMPTY_SECRET = "";
    final String NONCE = "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg";
    final String TIMESTAMP = "1318622958";
    final String SIGNATURE_METHOD = "HMAC-SHA1";
    final String OAUTH_VERSION = "1.0";
    final String OAUTH_TOKEN = "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb";
    final String OAUTH_TOKEN_SECRET = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
    final String BASE_URL = "https://api.twitter.com/1/statuses/update.json";

    final String EXPECTED_SIGNATURE = "tnnArxj06cWHq44gCs1OSKk/jLY=";

    final String POST = "POST";
    final String BASIC_URL = "https://www.example.com:80/context/path";
    final String EXTENDED_URL = "http://www.example.com:80/context?p1=val1&p2=val2#frag";

    final String CWWKS5409E_ERROR_CREATING_SIGNATURE = "CWWKS5409E";
    final String CWWKS5410E_RESPONSE_HAS_NO_PARAMS = "CWWKS5410E";
    final String CWWKS5411E_MISSING_PARAMETER = "CWWKS5411E";
    final String CWWKS5412E_PARAM_WITH_WRONG_VALUE = "CWWKS5412E";
    final String CWWKS5413E_PARAMETER_EMPTY = "CWWKS5413E";
    final String CWWKS5414E_EMPTY_RESPONSE_BODY = "CWWKS5414E";
    final String CWWKS5415E_ENDPOINT_REQUEST_FAILED = "CWWKS5415E";
    final String CWWKS5418E_EXCEPTION_EXECUTING_REQUEST = "CWWKS5418E";
    final String CWWKS5426E_RESPONSE_NOT_JSON = "CWWKS5426E";
    final String CWWKS5482E_TWITTER_BAD_REQUEST_TOKEN_URL = "CWWKS5482E";
    final String CWWKS5483E_TWITTER_BAD_ACCESS_TOKEN_URL = "CWWKS5483E";
    final String CWWKS5484E_TWITTER_BAD_USER_API_URL = "CWWKS5484E";

    static final String MY_REQUEST_TOKEN = "myRequestToken";
    static final String MY_REQUEST_TOKEN_SECRET = "myRequestTokenSecret";
    static final String MY_OAUTH_VERIFIER = "myOAuthVerifierValue";
    static final String MY_ACCESS_TOKEN = "myAccessToken";
    static final String MY_ACCESS_TOKEN_SECRET = "myAccessTokenSecret";
    static final String MY_EMAIL = "myEmail";
    static final String MY_USER_ID = "myUserId";
    static final String MY_SCREEN_NAME = "myScreenName";
    static final String MY_JSON_FAILURE_MSG = "JSON failure message. Something went wrong.";

    public interface MockInterface {
        public String mockComputeSha1Signature() throws GeneralSecurityException, UnsupportedEncodingException;

        public Map<String, Object> mockExecuteRequest();

        public Map<String, Object> getEndpointResponse() throws SocialLoginException;
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);
    final HttpURLConnection connection = mockery.mock(HttpURLConnection.class);
    final InputStream is = mockery.mock(InputStream.class);
    final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class);
    final HttpResponse httpResponse = mockery.mock(HttpResponse.class);
    final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    final OAuthClientHttpUtil oauthClientHttpUtil = mockery.mock(OAuthClientHttpUtil.class);

    TwitterEndpointServices twitter = new TwitterEndpointServices();
    TwitterEndpointServices mockTwitterExecuteRequest = new TwitterEndpointServices() {
        @Override
        public Map<String, Object> executeRequest(SocialLoginConfig config, String requestMethod, String authzHeaderString, String url, String endpointType, String verifierValue) {
            return mockInterface.mockExecuteRequest();
        }
    };
    TwitterEndpointServices mockTwitterEndpointResponse = new TwitterEndpointServices() {
        @Override
        public Map<String, Object> getEndpointResponse(SocialLoginConfig config, String uri, String requestMethod, String authzHeaderString, String endpointPath, String verifierValue) throws SocialLoginException {
            return mockInterface.getEndpointResponse();
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());

        twitter = new TwitterEndpointServices();
        twitter.setConsumerKey(CONSUMER_KEY);
        twitter.setConsumerSecret(CONSUMER_SECRET);
        twitter.setTokenSecret(OAUTH_TOKEN_SECRET);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();

        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**************************************** computeSignature ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#computeSignature(String, String, Map)} - uses the class' consumerSecret and
     * requestTokenSecret values</li>
     * <li>{@link TwitterEndpointServices#computeSignature(String, String, Map, String, String)}</li>
     * </ul>
     */
    @Test
    public void testComputeSignature() {
        Map<String, String> params = populateSignatureParameters();
        assertEquals(EXPECTED_SIGNATURE, twitter.computeSignature(POST, BASE_URL, params));

        // Both secret arguments should be treated as empty strings
        final String EMPTY_CONSUMER_SECRET_AND_REQ_TOKEN_SECRET_SIG = "6ajdVQTIanvDoJ3EXbc0AWMoBXw=";
        assertEquals(EMPTY_CONSUMER_SECRET_AND_REQ_TOKEN_SECRET_SIG, twitter.computeSignature(POST, BASE_URL, params, null, null));
        assertEquals(EMPTY_CONSUMER_SECRET_AND_REQ_TOKEN_SECRET_SIG, twitter.computeSignature(POST, BASE_URL, params, EMPTY_SECRET, null));
        assertEquals(EMPTY_CONSUMER_SECRET_AND_REQ_TOKEN_SECRET_SIG, twitter.computeSignature(POST, BASE_URL, params, null, ""));
        assertEquals(EMPTY_CONSUMER_SECRET_AND_REQ_TOKEN_SECRET_SIG, twitter.computeSignature(POST, BASE_URL, params, EMPTY_SECRET, ""));

        checkForSecretsInTrace();
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#computeSignature(String, String, Map)}</li>
     * </ul>
     * Tests results when underlying {@link TwitterEndpointServices#computeSha1Signature(String, String)} method throws a
     * GeneralSecurityException.
     */
    @Test
    public void testComputeSignature_GeneralSecurityException() {
        TwitterEndpointServices mockTwitter = new TwitterEndpointServices() {
            @Override
            protected String computeSha1Signature(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException {
                return mockInterface.mockComputeSha1Signature();
            }
        };

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockComputeSha1Signature();
                    will(throwException(new GeneralSecurityException()));
                }
            });

            HashMap<String, String> params = populateSignatureParameters();
            assertEquals("", mockTwitter.computeSignature(POST, BASE_URL, params));

            String logMsg = CWWKS5409E_ERROR_CREATING_SIGNATURE;
            assertTrue("Did not find [" + logMsg + "] message in messages.log", outputMgr.checkForMessages(logMsg));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#computeSignature(String, String, Map)}</li>
     * </ul>
     * Tests results when underlying {@link TwitterEndpointServices#computeSha1Signature(String, String)} method throws an
     * UnsupportedEncodingException.
     */
    @Test
    public void testComputeSignature_UnsupportedEncodingException() {
        TwitterEndpointServices mockTwitter = new TwitterEndpointServices() {
            @Override
            protected String computeSha1Signature(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException {
                return mockInterface.mockComputeSha1Signature();
            }
        };

        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockComputeSha1Signature();
                    will(throwException(new UnsupportedEncodingException()));
                }
            });

            HashMap<String, String> params = populateSignatureParameters();
            assertEquals("", mockTwitter.computeSignature(POST, BASE_URL, params));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** createParameterStringForSignature ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#createParameterStringForSignature(Map)}</li>
     * </ul>
     * Ensures parameter strings are created in accordance with {@link https://dev.twitter.com/oauth/overview/creating-signatures}
     * .
     */
    @Test
    public void testCreateParameterStringForSignature() {
        assertEquals("", twitter.createParameterStringForSignature(null));

        HashMap<String, String> params = new HashMap<String, String>();
        assertEquals("", twitter.createParameterStringForSignature(params));

        params.put("key", "value");
        assertEquals("key=value", twitter.createParameterStringForSignature(params));

        params.put("second key", "value");
        assertEquals("key=value&second%20key=value", twitter.createParameterStringForSignature(params));

        params.put("1", "one's value=test!");
        assertEquals("1=one%27s%20value%3Dtest%21&key=value&second%20key=value", twitter.createParameterStringForSignature(params));

        checkForSecretsInTrace();
    }

    /**************************************** createSignatureBaseString ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#createSignatureBaseString(String, String, Map)}</li>
     * </ul>
     * Ensures signature base strings are created in accordance with
     * {@link https://dev.twitter.com/oauth/overview/creating-signatures}.
     */
    @Test
    public void testCreateSignatureBaseString() throws UnsupportedEncodingException {

        assertEquals("POST&&", twitter.createSignatureBaseString(null, null, null));
        assertEquals("GET&&", twitter.createSignatureBaseString("get", null, null));
        assertEquals("POST&my%20URL&", twitter.createSignatureBaseString("Some weird request_method.", "my URL", null));

        Map<String, String> testMap = new HashMap<String, String>();
        assertEquals("POST&" + Utils.percentEncode("http://www.example.com") + "&", twitter.createSignatureBaseString("pOsT", "http://www.example.com", testMap));

        testMap.put("My 1st key", "123 Value!");
        testMap.put("This=that", "and=The other thing");
        // Each parameter should effectively be double encoded due to how the base string creation process works
        String encodedParams = Utils.percentEncode("My 1st key") + "=" + Utils.percentEncode("123 Value!") + "&" + Utils.percentEncode("This=that") + "=" + Utils.percentEncode("and=The other thing");
        String expectedValue = "POST&" + Utils.percentEncode(EXTENDED_URL.substring(0, EXTENDED_URL.indexOf("?"))) + "&" + Utils.percentEncode(encodedParams);
        assertEquals(expectedValue, twitter.createSignatureBaseString(" get ", EXTENDED_URL, testMap));

        // Value pulled from https://dev.twitter.com/oauth/overview/creating-signatures
        HashMap<String, String> params = populateSignatureParameters();
        expectedValue = "POST&https%3A%2F%2Fapi.twitter.com%2F1%2Fstatuses%2Fupdate.json&include_entities%3Dtrue%26oauth_consumer_key%3Dxvz1evFS4wEEPTGEFPHBog%26oauth_nonce%3DkYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1318622958%26oauth_token%3D370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb%26oauth_version%3D1.0%26status%3DHello%2520Ladies%2520%252B%2520Gentlemen%252C%2520a%2520signed%2520OAuth%2520request%2521";
        assertEquals(expectedValue, twitter.createSignatureBaseString(POST, BASE_URL, params));

        checkForSecretsInTrace();
    }

    /**************************************** removeQueryAndFragment ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#removeQueryAndFragment(String)}</li>
     * </ul>
     */
    @Test
    public void testRemoveQueryAndFragment() {
        String url = null;
        String result = twitter.removeQueryAndFragment(url);
        assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

        url = "";
        result = twitter.removeQueryAndFragment(url);
        assertEquals(url, result);

        url = "This is a test case.";
        result = twitter.removeQueryAndFragment(url);
        assertEquals(url, result);

        url = "http://www.example.com:80/my/path";
        result = twitter.removeQueryAndFragment(url);
        assertEquals(url, result);

        url = "http://www.example.com:80/my/path?simpleQuery";
        result = twitter.removeQueryAndFragment(url);
        assertEquals(url.substring(0, url.indexOf("?")), result);

        url = "http://www.example.com:80/my/path#simpleFrag";
        result = twitter.removeQueryAndFragment(url);
        assertEquals(url.substring(0, url.indexOf("#")), result);

        url = "http://www.example.com:80/my/path?query=value&other=value%20here#including Fragment";
        result = twitter.removeQueryAndFragment(url);
        assertEquals(url.substring(0, url.indexOf("?")), result);

        url = "http://www.example.com:80/my/path#fragmentFirst?query=actually part of%20the%25%20fragment.";
        result = twitter.removeQueryAndFragment(url);
        assertEquals(url.substring(0, url.indexOf("#")), result);

        url = EXTENDED_URL;
        result = twitter.removeQueryAndFragment(url);
        assertEquals(EXTENDED_URL.substring(0, EXTENDED_URL.indexOf("?")), result);
    }

    /**************************************** createAuthorizationHeaderString ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#createAuthorizationHeaderString(Map)}</li>
     * </ul>
     * Ensures Authorization header strings are created in accordance with
     * {@link https://dev.twitter.com/oauth/overview/authorizing-requests}.
     */
    @Test
    public void testCreateAuthorizationHeaderString() {
        assertEquals("OAuth ", twitter.createAuthorizationHeaderString(null));

        HashMap<String, String> params = new HashMap<String, String>();
        assertEquals("OAuth ", twitter.createAuthorizationHeaderString(params));

        // Parameters that aren't one of the expected parameters are ignored
        params.put("ignored1", "value");
        params.put("oauth_ignored", "otherValue");
        assertEquals("OAuth ", twitter.createAuthorizationHeaderString(params));

        String consumerKey = "consumerKey";
        params.put(TwitterConstants.PARAM_OAUTH_CONSUMER_KEY, consumerKey);
        assertEquals("OAuth " + TwitterConstants.PARAM_OAUTH_CONSUMER_KEY + "=\"" + Utils.percentEncode(consumerKey) + "\"", twitter.createAuthorizationHeaderString(params));

        String token = "My!Token=Value Te^sting123";
        params.put(TwitterConstants.PARAM_OAUTH_TOKEN, token);
        // Need to do a comparison that matches either order, because the token order is not guaranteed
        String option1 = "OAuth " + TwitterConstants.PARAM_OAUTH_CONSUMER_KEY + "=\"" + Utils.percentEncode(consumerKey) + "\", " + TwitterConstants.PARAM_OAUTH_TOKEN + "=\"" + Utils.percentEncode(token) + "\"";
        String option2 = "OAuth " + TwitterConstants.PARAM_OAUTH_TOKEN + "=\"" + Utils.percentEncode(token) + "\", " + TwitterConstants.PARAM_OAUTH_CONSUMER_KEY + "=\"" + Utils.percentEncode(consumerKey) + "\"";
        String result = twitter.createAuthorizationHeaderString(params);
        assertTrue("The created authorization header [" + result + "] did not match either [" + option1 + "] or [" + option2 + "]", result.equals(option1) || result.equals(option2));

        checkForSecretsInTrace();
    }

    /**************************************** createAuthzHeaderForRequestTokenEndpoint ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#createAuthzHeaderForRequestTokenEndpoint(String, String)}</li>
     * </ul>
     * The Authorization header for {@value TwitterConstants#TWITTER_ENDPOINT_REQUEST_TOKEN} endpoint requests must contain the
     * usual Authorization header parameters (see {@link https://dev.twitter.com/oauth/overview/authorizing-requests}) except
     * oauth_token. In addition, it must also contain a {@value TwitterConstants#PARAM_OAUTH_CALLBACK} parameter. See
     * {@link https://dev.twitter.com/oauth/reference/post/oauth/request_token}.
     */
    @Test
    public void testCreateAuthzHeaderForRequestTokenEndpoint() {
        // Expect the oauth_callback header parameter to be an empty string
        Map<String, String> expectedHeaderParams = new HashMap<String, String>();
        expectedHeaderParams.put(TwitterConstants.PARAM_OAUTH_CALLBACK, "");

        String header = twitter.createAuthzHeaderForRequestTokenEndpoint(null, null);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, header, expectedHeaderParams);

        header = twitter.createAuthzHeaderForRequestTokenEndpoint(null, "");
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, header, expectedHeaderParams);

        header = twitter.createAuthzHeaderForRequestTokenEndpoint("", null);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, header, expectedHeaderParams);

        header = twitter.createAuthzHeaderForRequestTokenEndpoint("", "");
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, header, expectedHeaderParams);

        // Expect the oauth_callback header parameter to match the provided callback URL
        String callback = "myCallback";
        expectedHeaderParams.put(TwitterConstants.PARAM_OAUTH_CALLBACK, Utils.percentEncode(callback));

        header = twitter.createAuthzHeaderForRequestTokenEndpoint(callback, null);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, header, expectedHeaderParams);

        // Expect the oauth_callback header parameter to match the provided callback URL
        callback = EXTENDED_URL;
        expectedHeaderParams.put(TwitterConstants.PARAM_OAUTH_CALLBACK, Utils.percentEncode(EXTENDED_URL));

        header = twitter.createAuthzHeaderForRequestTokenEndpoint(callback, null);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, header, expectedHeaderParams);

        // Make sure that even if consumerKey is set to null, the oauth_consumer_key parameter still gets included in the Authorization header
        twitter.setConsumerKey(null);
        header = twitter.createAuthzHeaderForRequestTokenEndpoint(callback, EXTENDED_URL);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, header, expectedHeaderParams);

        checkForSecretsInTrace();
    }

    /**************************************** createAuthzHeaderForAuthorizedEndpoint ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#createAuthzHeaderForAuthorizedEndpoint(String, String)}</li>
     * </ul>
     * The Authorization header for authorized endpoint requests must contain the usual Authorization header parameters (see
     * {@link https://dev.twitter.com/oauth/overview/authorizing-requests}).
     */
    @Test
    public void testCreateAuthzHeaderForAuthorizedEndpoint() {
        // Expect the oauth_token header parameter to be an empty string
        Map<String, String> expectedHeaderParams = new HashMap<String, String>();
        expectedHeaderParams.put(TwitterConstants.PARAM_OAUTH_TOKEN, "");

        String header = twitter.createAuthzHeaderForAuthorizedEndpoint(null, null);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, header, expectedHeaderParams);

        header = twitter.createAuthzHeaderForAuthorizedEndpoint("", null);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, header, expectedHeaderParams);

        header = twitter.createAuthzHeaderForAuthorizedEndpoint(null, "");
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, header, expectedHeaderParams);

        header = twitter.createAuthzHeaderForAuthorizedEndpoint("", "");
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, header, expectedHeaderParams);

        // Expect the oauth_token header parameter to match the provided request token value
        String requestToken = "myRequestToken";
        String endpointUrl = "myUrl";
        expectedHeaderParams.put(TwitterConstants.PARAM_OAUTH_TOKEN, requestToken);

        header = twitter.createAuthzHeaderForAuthorizedEndpoint(endpointUrl, requestToken);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, header, expectedHeaderParams);

        endpointUrl = EXTENDED_URL;
        header = twitter.createAuthzHeaderForAuthorizedEndpoint(endpointUrl, requestToken);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, header, expectedHeaderParams);

        // Make sure that even if consumerKey is set to null, the oauth_consumer_key parameter still gets included in the Authorization header
        twitter.setConsumerKey(null);
        endpointUrl = EXTENDED_URL;
        header = twitter.createAuthzHeaderForAuthorizedEndpoint(endpointUrl, requestToken);
        verifyAuthzHeaderParams(TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, header, expectedHeaderParams);

        checkForSecretsInTrace();
    }

    /**************************************** populateResponseValues ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#populateResponseValues(String)}</li>
     * </ul>
     * Response values are expected to be in the format: key=value[&key2=value2]*.
     */
    @Test
    public void testPopulateResponseValues() {
        Map<String, String> result = twitter.populateResponseValues(null);
        assertTrue("No response values should have been found. Result: " + result, result.isEmpty());

        result = twitter.populateResponseValues("");
        assertTrue("No response values should have been found. Result: " + result, result.isEmpty());

        result = twitter.populateResponseValues("&&&");
        assertTrue("No response values should have been found. Result: " + result, result.isEmpty());

        String key = " ";
        result = twitter.populateResponseValues(key);
        assertTrue("Did not find expected key: [" + key + "]. Result: " + result, result.containsKey(key));

        key = "test";
        result = twitter.populateResponseValues(key);
        assertTrue("Did not find expected key: [" + key + "]. Result: " + result, result.containsKey(key));
        assertEquals("", result.get(key));

        key = "The quick brown fox jumps over the lazy dog!";
        result = twitter.populateResponseValues(key);
        assertTrue("Did not find expected key: [" + key + "]. Result: " + result, result.containsKey(key));
        assertEquals("", result.get(key));

        key = "key";
        String value = "value";
        result = twitter.populateResponseValues(key + "=" + value);
        assertTrue("Did not find expected key: [" + key + "]. Result: " + result, result.containsKey(key));
        assertEquals(value, result.get(key));

        String key2 = "%20key_2";
        String value2 = "My+value";
        result = twitter.populateResponseValues(key + "=" + value + "&" + key2 + "=" + value2);
        assertTrue("Did not find expected key: [" + key + "]. Result: " + result, result.containsKey(key));
        assertEquals(value, result.get(key));
        assertTrue("Did not find expected key: [" + key2 + "]. Result: " + result, result.containsKey(key2));
        assertEquals(value2, result.get(key2));

        key = TwitterConstants.RESPONSE_OAUTH_TOKEN;
        value = OAUTH_TOKEN;
        key2 = TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET;
        value2 = OAUTH_TOKEN_SECRET;
        result = twitter.populateResponseValues(key + "=" + value + "&" + key2 + "=" + value2);
        assertTrue("Did not find expected key: [" + key + "]. Result: " + result, result.containsKey(key));
        assertEquals(value, result.get(key));
        assertTrue("Did not find expected key: [" + key2 + "]. Result: " + result, result.containsKey(key2));
        assertEquals(value2, result.get(key2));

        checkForSecretsInTrace();
    }

    /**************************************** populateJsonResponse ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#populateJsonResponse(String)}</li>
     * </ul>
     * Response values are expected to be in JSON format.
     */
    @Test
    public void testPopulateJsonResponse() {
        Map<String, Object> result = null;
        try {
            String[] invalidJson = new String[] { " \t\n\r ", "test", "{Invalid JSON}", "{[]}", "{key1:[]}" };
            for (String body : invalidJson) {
                result = null;
                try {
                    result = twitter.populateJsonResponse(body);
                    fail("Should have thrown JsonParsingException for response body [" + body + "] but did not.");
                } catch (JoseException e) {
                    // Expected
                }
            }

            result = twitter.populateJsonResponse(null);
            assertNull("Result should have been null but was not. Result: " + result, result);

            result = twitter.populateJsonResponse("");
            assertNull("Result should have been null but was not. Result: " + result, result);

            result = twitter.populateJsonResponse("{}");
            assertTrue("Result should have been empty but was not. Result: " + result, result.isEmpty());

            result = twitter.populateJsonResponse("{\"key1\":[]}");
            assertEquals("Result should have had only one entry but did not. Result: " + result, 1, result.size());
            assertTrue("Result should have contained \"key1\" entry but did not. Result: " + result, result.containsKey("key1"));

            result = twitter.populateJsonResponse("{\"key1\":\"value1\",\"key2\":[1,2],\"key 3\":{},\" key 4 \":{\"subKey1\":{}}}");
            assertEquals("Result should have had 4 entries but did not. Result: " + result, 4, result.size());
            assertTrue("Result should have contained \"key1\" entry but did not. Result: " + result, result.containsKey("key1"));
            assertTrue("Result should have contained \"key2\" entry but did not. Result: " + result, result.containsKey("key2"));
            assertTrue("Result should have contained \"key 3\" entry but did not. Result: " + result, result.containsKey("key 3"));
            assertTrue("Result should have contained \" key 4 \" entry but did not. Result: " + result, result.containsKey(" key 4 "));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** createErrorResponse ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#createErrorResponse(String, Object[], String)}</li>
     * </ul>
     *
     */
    @Test
    public void testCreateErrorResponse() {
        Map<String, Object> result = null;

        result = twitter.createErrorResponse(null, null);
        verifyError(result, "^$");

        result = twitter.createErrorResponse(null, new Object[] {});
        verifyError(result, "^$");

        result = twitter.createErrorResponse("", null);
        verifyError(result, "^$");

        result = twitter.createErrorResponse("", new Object[] { "testValue" });
        verifyError(result, "^$");

        // Translated message should be found, including the error messsage provided
        String errorMsg = "This is the error.";
        result = twitter.createErrorResponse("TWITTER_ERROR_CREATING_SIGNATURE", new Object[] { errorMsg });
        verifyError(result, "^" + Pattern.quote(CWWKS5409E_ERROR_CREATING_SIGNATURE) + ".+" + Pattern.quote(errorMsg) + "$");
    }

    /**************************************** checkForEmptyResponse ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#checkForEmptyResponse(String, String, Map)}</li>
     * </ul>
     * First two arguments are only used if the provided map is null or empty. If the map is not null or empty, the method should
     * return {@code null}.
     */
    @Test
    public void testCheckForEmptyResponse() {
        Map<String, Object> result = null;
        try {
            verifyError(twitter.checkForEmptyResponse(null, null, null), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);

            Map<String, String> map = new HashMap<String, String>();

            verifyError(twitter.checkForEmptyResponse(null, null, map), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
            verifyError(twitter.checkForEmptyResponse("", null, map), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
            verifyError(twitter.checkForEmptyResponse(null, "", map), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
            verifyError(twitter.checkForEmptyResponse("", "", map), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
            verifyError(twitter.checkForEmptyResponse("myEndpoint", "key=\"Some response value\"", map), CWWKS5410E_RESPONSE_HAS_NO_PARAMS + ".*myEndpoint.*key=\"Some response value\"");

            map.put("key", "value");
            result = twitter.checkForEmptyResponse("", "response=Body&key=value", map);
            assertNull("Result was not null when it should have been. Result: " + result, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** checkForRequiredParameters ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#checkForRequiredParameters(String, Map, String...)}</li>
     * </ul>
     * First argument is only used if an error occurs. The provided map must contain all of the strings included in the required
     * parameter list (the map may include additional entries). A successful invocation will return {@code null}.
     */
    @Test
    public void testCheckForRequiredParameters() {
        Map<String, Object> result = null;
        Map<String, String> map = new HashMap<String, String>();
        String reqParam1 = "requiredParam1";
        String reqParam2 = "requiredParam2";
        String reqParam3 = "requiredParam3";
        String reqParam4 = "requiredParam4";
        try {
            // No required parameters
            result = twitter.checkForRequiredParameters(null, null);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            result = twitter.checkForRequiredParameters(null, map);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            result = twitter.checkForRequiredParameters(EXTENDED_URL, map);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            // Required parameter(s) with null or empty response values map
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, null, reqParam1), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam1);
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam1);
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, null, reqParam2, reqParam3), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam2 + ".*" + reqParam3);
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam2, reqParam3), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam2 + ".*" + reqParam3);

            // Put one value in response values map
            map.put(reqParam1, "someValue");

            // No required parameters
            result = twitter.checkForRequiredParameters(EXTENDED_URL, map);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            // Required parameter is present in the map
            result = twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            // At least one required parameter not present in the map
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam2), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam2);
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1, reqParam2, reqParam3), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam2 + ".*" + reqParam3);

            // Multiple values in response values map
            map.put(reqParam2, "someValue");
            map.put(reqParam3, "someValue");
            map.put(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET, OAUTH_TOKEN_SECRET);

            // No required parameters
            result = twitter.checkForRequiredParameters(EXTENDED_URL, map);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            // Required parameter is present in the map
            result = twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            result = twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1, reqParam2);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            result = twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1, reqParam2, reqParam3);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            result = twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1, reqParam2, reqParam3, TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET);
            assertNull("Result was not null when it should have been. Result: " + result, result);
            // At least one required parameter not present in the map
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam4), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam4);
            verifyError(twitter.checkForRequiredParameters(EXTENDED_URL, map, reqParam1, reqParam4, reqParam2), CWWKS5411E_MISSING_PARAMETER + ".*" + reqParam4);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** evaluateRequestTokenResponse ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#evaluateRequestTokenResponse(String)}</li>
     * </ul>
     * Per {@link https://dev.twitter.com/oauth/reference/post/oauth/request_token}, the response from the
     * {@value TwitterConstants#TWITTER_ENDPOINT_REQUEST_TOKEN} endpoint should adhere to these requirements:
     * <ol>
     * <li>Must contain {@value TwitterConstants#RESPONSE_OAUTH_TOKEN}, {@value TwitterConstants#RESPONSE_OAUTH_TOKEN_SECRET}, and
     * {@value TwitterConstants#RESPONSE_OAUTH_CALLBACK_CONFIRMED} parameters.</li>
     * <li>{@value TwitterConstants#RESPONSE_OAUTH_CALLBACK_CONFIRMED} parameter value must be "true"</li>
     * <li>Neither {@value TwitterConstants#RESPONSE_OAUTH_TOKEN} nor {@value TwitterConstants#RESPONSE_OAUTH_TOKEN_SECRET}
     * parameter values may be empty.</li>
     * </ol>
     */
    @Test
    public void testEvaluateRequestTokenResponse() {
        Map<String, Object> result = null;

        verifyError(twitter.evaluateRequestTokenResponse(null), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
        verifyError(twitter.evaluateRequestTokenResponse(""), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);

        // Missing oauth_callback_confirmed, oauth_token, and oauth_token_secret
        verifyError(twitter.evaluateRequestTokenResponse("test"),
                CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + ".*");

        // Missing oauth_token and oauth_token_secret
        verifyError(twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true"),
                CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + ".*");

        // oauth_token="" and missing oauth_token_secret
        verifyError(twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "="),
                CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + ".*");

        // oauth_token_secret="" and missing oauth_token
        verifyError(twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "="),
                CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*");

        // oauth_token="" and oauth_token_secret=""
        verifyError(twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "="),
                CWWKS5413E_PARAMETER_EMPTY + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*");

        // oauth_token_secret=""
        verifyError(twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "="),
                CWWKS5413E_PARAMETER_EMPTY + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + ".*");

        // oauth_token=""
        verifyError(twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + MY_REQUEST_TOKEN_SECRET),
                CWWKS5413E_PARAMETER_EMPTY + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*");

        // oauth_callback_confirmed != "true"
        verifyError(twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=otherValue&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + MY_REQUEST_TOKEN_SECRET),
                CWWKS5412E_PARAM_WITH_WRONG_VALUE + ".*" + TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + ".*");

        // All valid values, includes extra parmeter
        String extraKey = "extra";
        result = twitter.evaluateRequestTokenResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + MY_REQUEST_TOKEN_SECRET + "&" + extraKey + "=value");
        verifySuccess(result);
        assertEquals("myToken", result.get(TwitterConstants.RESPONSE_OAUTH_TOKEN));
        assertEquals(MY_REQUEST_TOKEN_SECRET, result.get(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET));
        assertFalse("Found extra key [" + extraKey + "] in JSON result that should not be there. Result: " + result, result.containsKey(extraKey));

        checkForSecretsInTrace();

    }

    /**************************************** evaluateAccessTokenResponse ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#evaluateAccessTokenResponse(String)}</li>
     * </ul>
     * Per {@link https://dev.twitter.com/oauth/reference/post/oauth/access_token}, the response from the
     * {@value TwitterConstants#TWITTER_ENDPOINT_ACCESS_TOKEN} endpoint should adhere to these requirements:
     * <ol>
     * <li>Must contain {@value TwitterConstants#RESPONSE_OAUTH_TOKEN} and {@value TwitterConstants#RESPONSE_OAUTH_TOKEN_SECRET}
     * parameters.</li>
     * <li>May contain {@value TwitterConstants#RESPONSE_USER_ID} and {@value TwitterConstants#RESPONSE_SCREEN_NAME}
     * parameters.</li>
     * <li>Neither {@value TwitterConstants#RESPONSE_OAUTH_TOKEN} nor {@value TwitterConstants#RESPONSE_OAUTH_TOKEN_SECRET}
     * parameter values may be empty.</li>
     * </ol>
     */
    @Test
    public void testEvaluateAccessTokenResponse() {
        Map<String, Object> result = null;

        verifyError(twitter.evaluateAccessTokenResponse(null), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
        verifyError(twitter.evaluateAccessTokenResponse(""), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);

        final String tokenSecret = MY_ACCESS_TOKEN_SECRET;

        // Missing oauth_token and oauth_token_secret
        verifyError(twitter.evaluateAccessTokenResponse("test"), CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + ".*");

        // Missing oauth_token_secret
        verifyError(twitter.evaluateAccessTokenResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN), CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + ".*");

        // Missing oauth_token
        verifyError(twitter.evaluateAccessTokenResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET), CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*");

        // oauth_token="" and oauth_token_secret=""
        verifyError(twitter.evaluateAccessTokenResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN + "=&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET),
                CWWKS5413E_PARAMETER_EMPTY + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*");

        // oauth_token=""
        verifyError(twitter.evaluateAccessTokenResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN + "=&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + tokenSecret),
                CWWKS5413E_PARAMETER_EMPTY + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN + ".*");

        // oauth_token_secret=""
        verifyError(twitter.evaluateAccessTokenResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET),
                CWWKS5413E_PARAMETER_EMPTY + ".*" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + ".*");

        // All valid values (missing screen_name and user_id), includes extra parmeter
        result = twitter.evaluateAccessTokenResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + tokenSecret + "&extra=value!");
        verifySuccess(result);
        assertEquals("myToken", result.get(TwitterConstants.RESULT_ACCESS_TOKEN));
        assertEquals(tokenSecret, result.get(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET));
        assertEquals("", result.get(TwitterConstants.RESULT_USER_ID));
        assertEquals("", result.get(TwitterConstants.RESULT_SCREEN_NAME));
        assertFalse("Found extra key [extra] in JSON result that should not be there. Result: " + result, result.containsKey("extra"));

        // All valid values (includes screen_name and user_id), includes extra parmeter
        String extraKey = "extra value";
        result = twitter.evaluateAccessTokenResponse(extraKey + "=\"My V4l\"&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + tokenSecret + "&" + TwitterConstants.RESPONSE_SCREEN_NAME + "=my_screenName%20&" + TwitterConstants.RESPONSE_USER_ID + "=1234567890");
        verifySuccess(result);
        assertEquals("myToken", result.get(TwitterConstants.RESULT_ACCESS_TOKEN));
        assertEquals(tokenSecret, result.get(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET));
        assertEquals("1234567890", result.get(TwitterConstants.RESULT_USER_ID));
        assertEquals("my_screenName%20", result.get(TwitterConstants.RESULT_SCREEN_NAME));
        assertFalse("Found extra key [" + extraKey + "] in JSON result that should not be there. Result: " + result, result.containsKey(extraKey));

        checkForSecretsInTrace(tokenSecret);
    }

    /**************************************** evaluateVerifyCredentialsResponse ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#evaluateVerifyCredentialsResponse(String)}</li>
     * </ul>
     * Per {@link https://dev.twitter.com/rest/reference/get/account/verify_credentials}, the response from the
     * {@value TwitterConstants#TWITTER_ENDPOINT_VERIFY_CREDENTIALS} endpoint should contain several pieces of data.
     */
    @Test
    public void testEvaluateVerifyCredentialsResponse() {
        Map<String, Object> result = null;

        // Valid JSON but no entries
        verifyError(twitter.evaluateVerifyCredentialsResponse(null), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
        verifyError(twitter.evaluateVerifyCredentialsResponse(""), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);
        verifyError(twitter.evaluateVerifyCredentialsResponse("{}"), CWWKS5410E_RESPONSE_HAS_NO_PARAMS);

        // Invalid JSON
        verifyError(twitter.evaluateVerifyCredentialsResponse(" "), CWWKS5426E_RESPONSE_NOT_JSON);
        verifyError(twitter.evaluateVerifyCredentialsResponse("test"), CWWKS5426E_RESPONSE_NOT_JSON);
        verifyError(twitter.evaluateVerifyCredentialsResponse("key1=key2"), CWWKS5426E_RESPONSE_NOT_JSON);

        // Missing required parameter(s)
        verifyError(twitter.evaluateVerifyCredentialsResponse("{\"key1\":\"key2\"}"), CWWKS5411E_MISSING_PARAMETER);

        // Contains required parameter(s)
        result = twitter.evaluateVerifyCredentialsResponse("{\"" + TwitterConstants.RESPONSE_EMAIL + "\":\"value\"}");
        verifySuccess(result);
        assertEquals("value", result.get(TwitterConstants.RESULT_EMAIL));

        // Extra parameters
        result = twitter.evaluateVerifyCredentialsResponse("{" +
                "\"" + TwitterConstants.RESPONSE_EMAIL + "\":\"" + MY_EMAIL + "\"," +
                "\"" + TwitterConstants.RESPONSE_SCREEN_NAME + "\":\"" + MY_SCREEN_NAME + "\"," +
                "\"" + TwitterConstants.RESPONSE_ID + "\":\"" + MY_USER_ID + "\"," +
                "\"other1\":\"otherValue1\"," +
                "\"1\":{\"subStuff\":[1]}," +
                "}");
        verifySuccess(result);
        assertEquals(MY_EMAIL, result.get(TwitterConstants.RESULT_EMAIL));
        assertEquals(MY_SCREEN_NAME, result.get(TwitterConstants.RESULT_SCREEN_NAME));
        assertEquals(MY_USER_ID, result.get(TwitterConstants.RESULT_USER_ID));
        // All of the unique entries + an added TwitterConstants.RESPONSE_STATUS entry
        assertEquals("Result was populated with an unexpected number of entries. Result: " + result, 6, result.size());

        checkForSecretsInTrace();
    }

    /**************************************** evaluateRequestResponse ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#evaluateRequestResponse(String, int, String)}</li>
     * </ul>
     * Ensures the body of the response was not null, that a 200 status code was returned, and that the response content contained
     * the expected parameters for the specified endpoint request type.
     */
    @Test
    public void testEvaluateRequestResponse() {
        Map<String, Object> result = null;

        final String requestTokenSecret = "myRequestTokenSecret";
        final String accessTokenSecret = "myAccessTokenSecret";

        // null response body
        verifyError(twitter.evaluateRequestResponse(null, 200, TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN),
                CWWKS5414E_EMPTY_RESPONSE_BODY + ".*" + Pattern.quote(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN) + ".*");

        // 401 status code
        verifyError(twitter.evaluateRequestResponse("", 401, TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN),
                CWWKS5415E_ENDPOINT_REQUEST_FAILED + ".*401");

        // null endpoint path - should default to oauth/request_token endpoint path
        verifyError(twitter.evaluateRequestResponse("key=value", 200, null),
                CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED);

        // Golden paths and easy error cases for different endpoint types; in-depth tests covered in separate tests

        // Golden path for oauth/request_token endpoint
        result = twitter.evaluateRequestResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + requestTokenSecret, 200, TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN);
        verifySuccess(result);
        assertEquals("myToken", result.get(TwitterConstants.RESPONSE_OAUTH_TOKEN));
        assertEquals(requestTokenSecret, result.get(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET));

        // oauth/request_token response content verified against oauth/access_token expected request type
        result = twitter.evaluateRequestResponse(TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + accessTokenSecret, 200, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN);
        verifySuccess(result);
        assertEquals("myToken", result.get(TwitterConstants.RESULT_ACCESS_TOKEN));
        assertEquals(accessTokenSecret, result.get(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET));

        // Golden path for oauth/access_token endpoint
        result = twitter.evaluateRequestResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + accessTokenSecret, 200, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN);
        verifySuccess(result);
        assertEquals("myToken", result.get(TwitterConstants.RESULT_ACCESS_TOKEN));
        assertEquals(accessTokenSecret, result.get(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET));
        assertEquals("", result.get(TwitterConstants.RESULT_USER_ID));
        assertEquals("", result.get(TwitterConstants.RESULT_SCREEN_NAME));
        assertFalse("Found extra key in JSON result that should not be there. Result: " + result, result.containsKey("extra"));

        // oauth/access_token response content verified against oauth/request_token expected request type
        verifyError(twitter.evaluateRequestResponse(TwitterConstants.RESPONSE_OAUTH_TOKEN + "=myToken&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + requestTokenSecret, 200, TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN),
                CWWKS5411E_MISSING_PARAMETER + ".*" + TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + ".*");

        // Request token secrets are short-lived, so are not being considered sensitive. Access token secrets should not appear in trace.
        checkForSecretsInTrace(accessTokenSecret);
    }

    /**************************************** executeRequest ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#executeRequest(SocialLoginConfig, String, String, String, String, String)}</li>
     * </ul>
     */
    @Test
    public void testExecuteRequest_nullOrEmptyUrl() {
        try {
            String url = RandomUtils.getRandomSelection(null, "");
            Map<String, Object> result = mockTwitterEndpointResponse.executeRequest(config, null, null, url, null, null);
            verifyError(result, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#executeRequest(SocialLoginConfig, String, String, String, String, String)}</li>
     * </ul>
     */
    @Test
    public void testExecuteRequest_invalidUrl() {
        try {
            String url = "Some invalid URL";
            Map<String, Object> result = mockTwitterEndpointResponse.executeRequest(config, null, null, url, null, null);
            verifyError(result, CWWKS5417E_EXCEPTION_INITIALIZING_URL + ".+\\[" + Pattern.quote(url) + "\\]");

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#executeRequest(SocialLoginConfig, String, String, String, String, String)}</li>
     * </ul>
     * All arguments except the endpoint path should not be necessary to specify since we're mocking the calls in which they
     * would be used. The endpoint path should default to {@value TwitterConstants#TWITTER_ENDPOINT_REQUEST_TOKEN} when null.
     */
    @Test
    public void testExecuteRequest_nullEndpointPath() {
        final String SUCCESSFUL_REQUEST_TOKEN_RESPONSE = TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=" + MY_REQUEST_TOKEN + "&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + MY_REQUEST_TOKEN_SECRET;
        try {
            final Map<String, Object> response = createEndpointResponse(httpResponse);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getEndpointResponse();
                    will(returnValue(response));
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                }
            });
            entityUtilsToStringExpectations(SUCCESSFUL_REQUEST_TOKEN_RESPONSE);

            Map<String, Object> result = mockTwitterEndpointResponse.executeRequest(config, null, null, BASIC_URL, null, null);
            verifySuccess(result);

            // oauth_token_secret parameter value will appear in trace, but request token secrets are not considered sensitive so we won't look for it here
            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#executeRequest(SocialLoginConfig, String, String, String, String, String)}</li>
     * </ul>
     * Specifies {@value TwitterConstants#TWITTER_ENDPOINT_ACCESS_TOKEN} as the endpoint path but still mocks a response as if it
     * were from the {@value TwitterConstants#TWITTER_ENDPOINT_REQUEST_TOKEN} endpoint. The result should be successful because
     * the response contains the oauth_token and oauth_token_secret parameters that are necessary for the
     * {@value TwitterConstants#TWITTER_ENDPOINT_ACCESS_TOKEN} endpoint response to be considered valid.
     */
    @Test
    public void testExecuteRequest_accessTokenEndpointPath() {
        final String SUCCESSFUL_REQUEST_TOKEN_RESPONSE = TwitterConstants.RESPONSE_OAUTH_CALLBACK_CONFIRMED + "=true&" + TwitterConstants.RESPONSE_OAUTH_TOKEN + "=" + MY_REQUEST_TOKEN + "&" + TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET + "=" + MY_REQUEST_TOKEN_SECRET;
        try {
            final Map<String, Object> response = createEndpointResponse(httpResponse);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getEndpointResponse();
                    will(returnValue(response));
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                }
            });
            entityUtilsToStringExpectations(SUCCESSFUL_REQUEST_TOKEN_RESPONSE);

            Map<String, Object> result = mockTwitterEndpointResponse.executeRequest(config, null, null, BASIC_URL, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, null);
            verifySuccess(result);

            // Successful oauth/request_token response was mocked above - will contain the required parameters for the oauth/access_token endpoint path
            // but will be missing the screen_name and user_id parameters
            assertEquals(MY_REQUEST_TOKEN, result.get(TwitterConstants.RESULT_ACCESS_TOKEN));
            assertEquals(MY_REQUEST_TOKEN_SECRET, result.get(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET));
            assertEquals("", result.get(TwitterConstants.RESULT_SCREEN_NAME));
            assertEquals("", result.get(TwitterConstants.RESULT_USER_ID));

            checkForSecretsInTrace(MY_REQUEST_TOKEN_SECRET);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#executeRequest(SocialLoginConfig, String, String, String, String, String)}</li>
     * </ul>
     * Specifies {@value TwitterConstants#TWITTER_ENDPOINT_VERIFY_CREDENTIALS} as the endpoint path.
     */
    @Test
    public void testExecuteRequest_verifyCredentialsEndpointPath() {
        final String SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE = "{\"" + TwitterConstants.RESPONSE_EMAIL + "\":\"" + MY_EMAIL + "\"}";
        try {
            final Map<String, Object> response = createEndpointResponse(httpResponse);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getEndpointResponse();
                    will(returnValue(response));
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                }
            });
            entityUtilsToStringExpectations(SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE);

            Map<String, Object> result = mockTwitterEndpointResponse.executeRequest(config, null, null, BASIC_URL, TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS, null);
            verifySuccess(result);

            // Successful response was mocked above - will contain the required parameters for the verify_credentials endpoint
            assertEquals(MY_EMAIL, result.get(TwitterConstants.RESULT_EMAIL));
            assertEquals("Result did not contain the expected number of entries. Result: " + result, 2, result.size());

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#executeRequest(SocialLoginConfig, String, String, String, String, String)}</li>
     * </ul>
     */
    @Test
    public void testExecuteRequest_SocialLoginException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getEndpointResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            Map<String, Object> result = mockTwitterEndpointResponse.executeRequest(config, null, null, BASIC_URL, null, null);
            verifyError(result, CWWKS5418E_EXCEPTION_EXECUTING_REQUEST + ".+" + Pattern.quote(BASIC_URL) + ".+" + Pattern.quote(defaultExceptionMsg));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** getEndpointResponse ****************************************/

    @Test
    public void getEndpointResponse_gettingSslInfoThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getSSLSocketFactory();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            try {
                Map<String, Object> result = twitter.getEndpointResponse(config, null, null, null, null, null);
                fail("Should have thrown a SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5463E_FAILED_TO_GET_SSL_CONTEXT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg));
            }

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_nullOrEmptyUrl() {
        try {
            final String url = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                }
            });

            try {
                Map<String, Object> result = twitter.getEndpointResponse(config, url, null, null, null, null);
                fail("Should have thrown a SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_invalidUrl() {
        try {
            final String url = "Some invalid url";
            mockery.checking(new Expectations() {
                {
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                }
            });

            try {
                Map<String, Object> result = twitter.getEndpointResponse(config, url, null, null, null, null);
                fail("Should have thrown a SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL + ".+\\[" + Pattern.quote(url) + "\\]");
            }

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_nullArgs() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);
            final List<NameValuePair> headers = getCommonHeaders(null);

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).getToEndpoint(BASIC_URL, new ArrayList<NameValuePair>(), null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, null, null, null, null);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_unknownRequestMethod() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);
            final List<NameValuePair> headers = getCommonHeaders(null);

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).getToEndpoint(BASIC_URL, new ArrayList<NameValuePair>(), null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, "some req method", null, null, null);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_getRequestMethod() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);
            final List<NameValuePair> headers = getCommonHeaders(null);
            final String endpointPath = RandomUtils.getRandomSelection(null, "Some unknown value");
            final String verifierValue = RandomUtils.getRandomSelection(null, "Some unused value");

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).getToEndpoint(BASIC_URL, new ArrayList<NameValuePair>(), null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, "GET", null, endpointPath, verifierValue);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_postRequestMethod() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);
            final List<NameValuePair> headers = getCommonHeaders(null);
            final String endpointPath = RandomUtils.getRandomSelection(null, "Some unknown value");
            final String verifierValue = RandomUtils.getRandomSelection(null, "Some unused value");

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).postToEndpoint(BASIC_URL, new ArrayList<NameValuePair>(), null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, "pOsT", null, endpointPath, verifierValue);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_withAuthzHeader() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);
            final String authzHeader = "someAuthz header value";
            final List<NameValuePair> headers = getCommonHeaders(authzHeader);

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).getToEndpoint(BASIC_URL, new ArrayList<NameValuePair>(), null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, "gEt", authzHeader, null, null);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_accessTokenEndpoint_nullVerifier() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);

            final List<NameValuePair> headers = getCommonHeaders(null);

            // Verifier value should be percent encoded, and a null input value will result in an empty encoded string
            final String verifier = null;
            final List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(TwitterConstants.PARAM_OAUTH_VERIFIER, Utils.percentEncode(verifier)));

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).getToEndpoint(BASIC_URL, params, null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, null, null, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, verifier);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_accessTokenEndpoint_nonNullVerifier() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);

            final List<NameValuePair> headers = getCommonHeaders(null);

            // Verifier value should be percent encoded
            final String verifier = "Some verifier <alert>300</alert> value";
            final List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(TwitterConstants.PARAM_OAUTH_VERIFIER, Utils.percentEncode(verifier)));

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).getToEndpoint(BASIC_URL, params, null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, null, null, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN, verifier);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEndpointResponse_unknownEndpoint() {
        try {
            twitter.setOAuthClientHttpUtil(oauthClientHttpUtil);

            final List<NameValuePair> headers = getCommonHeaders(null);
            final String verifier = "Some verifier <alert>300</alert> value";

            mockery.checking(new Expectations() {
                {
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                    one(oauthClientHttpUtil).getToEndpoint(BASIC_URL, new ArrayList<NameValuePair>(), null, null, null, sslSocketFactory, headers, false, null, false);
                }
            });

            Map<String, Object> result = twitter.getEndpointResponse(config, BASIC_URL, null, null, "Unknown endpoint", verifier);
            assertNotNull("Result should not have been null but was.", result);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** obtainRequestToken ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainRequestToken(SocialLoginConfig, String)}</li>
     * </ul>
     */
    @Test
    public void testObtainRequestToken_missingRequestTokenUrl() {
        try {
            final String url = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(config).getRequestTokenUrl();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            Map<String, Object> result = mockTwitterExecuteRequest.obtainRequestToken(config, null);
            verifyError(result, CWWKS5482E_TWITTER_BAD_REQUEST_TOKEN_URL + ".*\\[" + url + "\\].*\\[" + uniqueId + "\\].*" + CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainRequestToken(SocialLoginConfig, String)}</li>
     * </ul>
     */
    @Test
    public void testObtainRequestToken_invalidRequestTokenUrl() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            final String url = "Some invalid URL";
            mockery.checking(new Expectations() {
                {
                    one(config).getRequestTokenUrl();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            Map<String, Object> result = mockTwitterExecuteRequest.obtainRequestToken(config, null);
            verifyError(result, CWWKS5482E_TWITTER_BAD_REQUEST_TOKEN_URL + ".*\\[" + url + "\\].*\\[" + uniqueId + "\\].*" + CWWKS5417E_EXCEPTION_INITIALIZING_URL + ".*\\[" + url + "\\]");

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainRequestToken(SocialLoginConfig, String)}</li>
     * </ul>
     * Passes {@code null} as the callback URL argument and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testObtainRequestToken_successNullCallback() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getRequestTokenUrl();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.obtainRequestToken(config, null));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainRequestToken(SocialLoginConfig, String)}</li>
     * </ul>
     * Passes a valid string as the callback URL argument and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testObtainRequestToken_successWithCallback() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getRequestTokenUrl();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.obtainRequestToken(config, EXTENDED_URL));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainRequestToken(SocialLoginConfig, String)}</li>
     * </ul>
     * Passes a valid string as the callback URL argument and mocks an unsuccessful request execution.
     * Result should show an error response status and related error message.
     */
    @Test
    public void testObtainRequestToken_error() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            String errorMsg = "An error occurred.";
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_ERROR);
            response.put(TwitterConstants.RESULT_MESSAGE, errorMsg);

            mockery.checking(new Expectations() {
                {
                    one(config).getRequestTokenUrl();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifyError(mockTwitterExecuteRequest.obtainRequestToken(config, EXTENDED_URL), Pattern.quote(errorMsg));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** obtainAccessToken ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainAccessToken(SocialLoginConfig, String, String)}</li>
     * </ul>
     */
    @Test
    public void testObtainAccessToken_missingAccessTokenUrl() {
        try {
            final String url = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            Map<String, Object> result = mockTwitterExecuteRequest.obtainAccessToken(config, null, null);
            verifyError(result, CWWKS5483E_TWITTER_BAD_ACCESS_TOKEN_URL + ".*\\[" + url + "\\].*\\[" + uniqueId + "\\].*" + CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainAccessToken(SocialLoginConfig, String, String)}</li>
     * </ul>
     */
    @Test
    public void testObtainAccessToken_invalidAccessTokenUrl() {
        try {
            final String url = "Some invalid URL";
            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            Map<String, Object> result = mockTwitterExecuteRequest.obtainAccessToken(config, null, null);
            verifyError(result, CWWKS5483E_TWITTER_BAD_ACCESS_TOKEN_URL + ".*\\[" + url + "\\].*\\[" + uniqueId + "\\].*" + CWWKS5417E_EXCEPTION_INITIALIZING_URL + ".*\\[" + url + "\\]");

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainAccessToken(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes {@code null} as the request token and verifier arguments and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testObtainAccessToken_nullVerifier() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.obtainAccessToken(config, null, null));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainAccessToken(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes {@code null} as the request token argument and a valid verifier argument and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testObtainAccessToken_validVerifierNullRequestToken() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.obtainAccessToken(config, null, "myVerifierValue"));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#obtainAccessToken(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes valid strings for the request token and verifier arguments and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testObtainAccessToken_validArgs() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.obtainAccessToken(config, "myRequestToken", "myVerifierValue"));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** verifyCredentials ****************************************/

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#verifyCredentials(SocialLoginConfig, String, String)}</li>
     * </ul>
     */
    @Test
    public void testVerifyCredentials_missingUserApi() {
        try {
            final String url = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            Map<String, Object> result = mockTwitterExecuteRequest.verifyCredentials(config, null, null);
            verifyError(result, CWWKS5484E_TWITTER_BAD_USER_API_URL + ".*\\[" + url + "\\].*\\[" + uniqueId + "\\].*" + CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#verifyCredentials(SocialLoginConfig, String, String)}</li>
     * </ul>
     */
    @Test
    public void testVerifyCredentials_invalidUserApi() {
        try {
            final String url = "Some invalid URL";
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            Map<String, Object> result = mockTwitterExecuteRequest.verifyCredentials(config, null, null);
            verifyError(result, CWWKS5484E_TWITTER_BAD_USER_API_URL + ".*\\[" + url + "\\].*\\[" + uniqueId + "\\].*" + CWWKS5417E_EXCEPTION_INITIALIZING_URL + ".*\\[" + url + "\\]");

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#verifyCredentials(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes {@code null} as the request token and verifier arguments and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testVerifyCredentials_nullArgs() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.verifyCredentials(config, null, null));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#verifyCredentials(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes {@code null} as the request token and an empty string as the token secret and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testVerifyCredentials_emptyTokenNullSecret() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.verifyCredentials(config, "", null));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#verifyCredentials(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes an empty string as the request token and {@code null} as the token secret and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testVerifyCredentials_nullTokenEmptySecret() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.verifyCredentials(config, null, ""));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#verifyCredentials(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes an empty string as the request token and token secret arguments and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testVerifyCredentials_emptyArgs() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.verifyCredentials(config, "", ""));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterEndpointServices#verifyCredentials(SocialLoginConfig, String, String)}</li>
     * </ul>
     * Passes valid strings as the request token and token secret arguments and mocks a successful request execution.
     * Result should be successful.
     */
    @Test
    public void testVerifyCredentials_validArgs() {
        try {
            final Map<String, String> response = new HashMap<String, String>();
            response.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);

            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(BASIC_URL));
                    one(mockInterface).mockExecuteRequest();
                    will(returnValue(response));
                }
            });

            verifySuccess(mockTwitterExecuteRequest.verifyCredentials(config, OAUTH_TOKEN, OAUTH_TOKEN_SECRET));

            checkForSecretsInTrace();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** Helper methods ****************************************/

    private Map<String, Object> createEndpointResponse(final HttpResponse httpResponse) {
        final Map<String, Object> response = new HashMap<String, Object>();
        response.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);
        return response;
    }

    private List<NameValuePair> getCommonHeaders(String authzHeader) {
        List<NameValuePair> headers = new ArrayList<NameValuePair>();
        headers.add(new BasicNameValuePair(CommonWebConstants.HTTP_HEADER_CONTENT_TYPE, CommonWebConstants.HTTP_CONTENT_TYPE_FORM_URL_ENCODED));
        if (authzHeader != null) {
            headers.add(new BasicNameValuePair(CommonWebConstants.HTTP_HEADER_AUTHORIZATION, authzHeader));
        }
        return headers;
    }

    /**
     * Ensure that no consumer secrets or token secrets show up in trace.
     */
    private void checkForSecretsInTrace(String... secrets) {
        assertFalse("Consumer secret [" + CONSUMER_SECRET + "] was found in trace when it should not have been.", outputMgr.checkForLiteralTrace(CONSUMER_SECRET));
        assertFalse("Token secret [" + OAUTH_TOKEN_SECRET + "] was found in trace when it should not have been.", outputMgr.checkForLiteralTrace(OAUTH_TOKEN_SECRET));
        assertFalse("Token secret [" + MY_ACCESS_TOKEN_SECRET + "] was found in trace when it should not have been.", outputMgr.checkForLiteralTrace(MY_ACCESS_TOKEN_SECRET));
        //        if (secrets != null && secrets.length > 0) {
        for (String secret : secrets) {
            assertFalse("Secret string [" + secret + "] was found in trace when it should not have been.", outputMgr.checkForLiteralTrace(secret));
        }
    }

    /**
     * Populates signature parameter map with values consistent with
     * {@link https://dev.twitter.com/oauth/overview/creating-signatures}
     *
     * @return
     */
    public HashMap<String, String> populateSignatureParameters() {
        HashMap<String, String> params = new HashMap<String, String>();

        params.put(TwitterConstants.PARAM_OAUTH_CONSUMER_KEY, CONSUMER_KEY);
        params.put(TwitterConstants.PARAM_OAUTH_NONCE, NONCE);
        params.put(TwitterConstants.PARAM_OAUTH_SIGNATURE_METHOD, SIGNATURE_METHOD);
        params.put(TwitterConstants.PARAM_OAUTH_TIMESTAMP, TIMESTAMP);
        params.put(TwitterConstants.PARAM_OAUTH_TOKEN, OAUTH_TOKEN);
        params.put(TwitterConstants.PARAM_OAUTH_VERSION, OAUTH_VERSION);

        params.put("status", "Hello Ladies + Gentlemen, a signed OAuth request!");
        params.put("include_entities", "true");

        return params;
    }

    /**
     * Verify that all of the expected Authorization header parameters are present in the specified header value. Also verifies
     * that each of the entries in expectedHeaderVals is present in the header.
     *
     * @param endpoint
     * @param header
     * @param expectedHeaderVals
     */
    private void verifyAuthzHeaderParams(String endpoint, String header, Map<String, String> expectedHeaderVals) {

        assertTrue("Header did not start with \"OAuth \". Header was: [" + header + "]", header.startsWith("OAuth "));

        header = header.replaceFirst("OAuth", "").trim();

        // Track all parameters and values in the header to verify specific values later
        Map<String, String> foundParams = new HashMap<String, String>();

        // Tokenize the header so we can verify that all, and ONLY, the expected parameters are included
        List<String> paramList = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(header, ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            String[] keyValue = token.split("=");
            String param = keyValue[0].trim();
            paramList.add(param);
            String value = "";
            if (keyValue.length > 1) {
                value = keyValue[1];
                // Remove encapsulating quotation marks
                if (!value.isEmpty() && !value.equals("\"") && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                    value = value.substring(1, value.length() - 1);
                }
            }
            foundParams.put(param, value);
        }

        // Authorization headers generally must have all of the parameters listed in Constants.AUTHZ_HEADER_PARAMS
        for (String headerParam : TwitterConstants.AUTHZ_HEADER_PARAMS) {
            String skipParam = (endpoint.equals(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN)) ? TwitterConstants.PARAM_OAUTH_TOKEN : TwitterConstants.PARAM_OAUTH_CALLBACK;
            if (headerParam.equals(skipParam)) {
                // The endpoint doesn't have to have this particular parameter in the Authorization header
                assertFalse("Authorization header param list contained " + headerParam + " when it should not have. Header was: [" + header + "]", paramList.contains(headerParam));
            } else {
                assertTrue("Authorization header param list did not contain " + headerParam + " when it should have. Header was: [" + header + "]", paramList.contains(headerParam));
            }
            paramList.remove(headerParam);
        }
        assertTrue("Found unexpected parameter(s) in Authorization header: " + Arrays.toString(paramList.toArray(new String[0])), paramList.isEmpty());

        if (expectedHeaderVals != null) {
            // Verify that all expected parameter names and their expected values are in the header
            for (Entry<String, String> entry : expectedHeaderVals.entrySet()) {
                String expectedKey = entry.getKey();
                String expectedValue = entry.getValue();
                assertTrue("Header did not contain expected parameter [" + expectedKey + "]. Header was: [" + header + "]", foundParams.containsKey(entry.getKey()));
                String actualValue = foundParams.get(expectedKey);
                assertEquals("Value for [" + expectedKey + "] parameter did not match expected value. Expected: [" + expectedValue + "]. Found: [" + actualValue + "]. Header was: [" + header + "]", expectedValue, actualValue);
            }
        }
    }

    /**
     * Verifies the following conditions:
     * <ol>
     * <li>The result is not null.</li>
     * <li>The result contains a {@value TwitterConstants#RESULT_RESPONSE_STATUS} key with a value of
     * {@value TwitterConstants#RESULT_SUCCESS}.</li>
     * </ol>
     *
     * @param result
     */
    private void verifySuccess(Map<String, Object> result) {
        assertNotNull("Result was null when it should not have been.", result);

        assertTrue("Result should have a " + TwitterConstants.RESULT_RESPONSE_STATUS + " entry. Result: " + result, result.containsKey(TwitterConstants.RESULT_RESPONSE_STATUS));
        String responseStatus = (String) result.get(TwitterConstants.RESULT_RESPONSE_STATUS);
        assertEquals("Did not get expected success status. Expected: [" + TwitterConstants.RESULT_SUCCESS + "]. Response status was: [" + responseStatus + "]. Full result: " + result, TwitterConstants.RESULT_SUCCESS, responseStatus);
    }

    /**
     * Verifies the following conditions:
     * <ol>
     * <li>The result is not null.</li>
     * <li>The result contains a {@value TwitterConstants#RESULT_RESPONSE_STATUS} key with a value of
     * {@value TwitterConstants#RESULT_ERROR}.</li>
     * <li>The result contains a {@value TwitterConstants#RESULT_MESSAGE} key with a value that matches the provided error message
     * regex.</li>
     * </ol>
     *
     * @param result
     * @param errorMsgRegex
     */
    private void verifyError(Map<String, Object> result, String errorMsgRegex) {
        assertNotNull("Result was null when it should not have been.", result);

        assertTrue("Result should have a " + TwitterConstants.RESULT_RESPONSE_STATUS + " entry. Result: " + result, result.containsKey(TwitterConstants.RESULT_RESPONSE_STATUS));
        String responseStatus = (String) result.get(TwitterConstants.RESULT_RESPONSE_STATUS);
        assertEquals("Did not get expected error status. Expected: [" + TwitterConstants.RESULT_ERROR + "]. Response status was: [" + responseStatus + "]", TwitterConstants.RESULT_ERROR, responseStatus);

        assertTrue("Result should have a " + TwitterConstants.RESULT_MESSAGE + " entry. Result: " + result, result.containsKey(TwitterConstants.RESULT_MESSAGE));
        String errorMsg = (String) result.get(TwitterConstants.RESULT_MESSAGE);
        verifyPattern(errorMsg, errorMsgRegex);
    }

}
