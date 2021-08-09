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
package com.ibm.ws.security.social.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class OAuthClientUtilTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    OAuthClientUtil util = new OAuthClientUtil();

    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    private final OAuthClientHttpUtil httpUtil = mockery.mock(OAuthClientHttpUtil.class);
    private final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class);
    private final JwtToken jwt = mockery.mock(JwtToken.class);
    private final HttpResponse httpResponse = mockery.mock(HttpResponse.class);
    private final HttpClient httpClient = mockery.mock(HttpClient.class);
    private final StatusLine statusLine = mockery.mock(StatusLine.class);
    private final Header header = mockery.mock(Header.class);

    private final String clientId = "myClientId";
    private final String clientSecret = "myClientSecret";
    private final String redirectUri = "http://redirect-uri.com/some/path";
    private final String tokenEndpoint = "https://some-domain.com/path/token";
    private final String tokenEndpointWithQuery = "https://some-domain.com/path/token?with=some&query=string";
    private final String code = "myCode";
    private final String grantType = "myGrantType";
    private final String authMethod = ClientConstants.METHOD_client_secret_post;
    private final String resources = "myResources";
    private final String accessToken = "myAccessToken";
    private final String userApi = "myUserApi";
    private final String jwtRef = "myJwtRef";
    private final boolean isHostnameVerification = false;
    private final boolean needsSpecialHeader = false;
    private final String httpScheme = "http";
    private final String httpsScheme = "https";
    private final String localhost = "localhost";
    private final String host = "some-domain.com";
    private final String url = httpsScheme + "://" + host + "/some/path";

    private final static String key1 = "key1";
    private final static String key2 = "key2";
    private final static String value1 = "value1";
    private final static String value2 = "value2";
    private final static Map<String, Object> mapResponse = new HashMap<String, Object>();
    static {
        mapResponse.put(key1, value1);
        mapResponse.put(key2, value2);
    }
    private final static String jsonResponse = JsonUtils.toJson(mapResponse);
    private final static List<NameValuePair> params = new ArrayList<NameValuePair>();
    static {
        params.add(new BasicNameValuePair(key1, value1));
        params.add(new BasicNameValuePair(key2, value2));
    }
    private final static String paramsString = key1 + "=" + value1 + "&" + key2 + "=" + value2;

    public interface MockInterface {
        public Map<String, Object> postToCheckTokenEndpoint() throws SocialLoginException;

        public Map<String, Object> getFromUserApiEndpoint() throws ClientProtocolException, IOException;

        public Map<String, Object> getUserApi() throws Exception;

        public String getJsonStringResponse() throws SocialLoginException;

        public JwtToken createJwtTokenFromJson() throws Exception;

        public boolean isErrorResponse();

        public void handleError() throws SocialLoginException;

        public Integer getRedirectPortFromRequest();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    final OAuthClientUtil mockUtil = new OAuthClientUtil() {
        @Override
        Map<String, Object> postToCheckTokenEndpoint(String tokenEnpoint, @Sensitive List<NameValuePair> params, String baUsername, @Sensitive String baPassword,
                boolean isHostnameVerification, String authMethod, SSLSocketFactory sslSocketFactory, boolean useJvmProps) throws SocialLoginException {
            return mockInterface.postToCheckTokenEndpoint();
        }

        @Override
        Map<String, Object> getFromUserApiEndpoint(String userApiEndpoint, @Sensitive List<NameValuePair> params, @Sensitive String accessToken,
                SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, boolean needsSpecialHeader, boolean useJvmProps) throws ClientProtocolException, IOException {
            return mockInterface.getFromUserApiEndpoint();
        }

        @Override
        public Map<String, Object> getUserApi(String userApi, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, boolean needsSpecialHeader, boolean useJvmProps) throws Exception {
            return mockInterface.getUserApi();
        }

        @Override
        public String getJsonStringResponse(Map<String, Object> responseMap, String userApi) throws SocialLoginException {
            return mockInterface.getJsonStringResponse();
        }

        @Override
        public JwtToken createJwtTokenFromJson(String jsonString, String jwtRef) throws Exception {
            return mockInterface.createJwtTokenFromJson();
        }

        @Override
        protected Integer getRedirectPortFromRequest(HttpServletRequest req) {
            return mockInterface.getRedirectPortFromRequest();
        }
    };

    final OAuthClientUtil mockUtil_getUserApi = new OAuthClientUtil() {
        @Override
        Map<String, Object> getFromUserApiEndpoint(String userApiEndpoint, @Sensitive List<NameValuePair> params, @Sensitive String accessToken,
                SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, boolean needsSpecialHeader, boolean useJvmProps) throws ClientProtocolException, IOException {
            return mockInterface.getFromUserApiEndpoint();
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        util = new OAuthClientUtil();

        util.httpUtil = httpUtil;
        mockUtil.httpUtil = httpUtil;
        mockUtil_getUserApi.httpUtil = httpUtil;
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

    /************************************** constructor **************************************/

    @Test
    public void constructor() {
        OAuthClientUtil util = new OAuthClientUtil();

        assertNotNull("OAuthClientHttpUtil class should not have been null.", util.httpUtil);

        List<NameValuePair> headers = util.getCommonHeaders();
        assertNotNull("List of common headers should not have been null.", headers);
        assertEquals("Number of initial common headers did not match expected value.", 1, headers.size());
        assertEquals("List of initial common headers did not include expected header.", CommonWebConstants.HTTP_HEADER_ACCEPT, headers.get(0).getName());
        assertEquals("Initial common header value did not match expected value.", "application/json", headers.get(0).getValue());
    }

    /************************************** getTokensFromAuthzCode **************************************/

    @Test
    public void getTokensFromAuthzCode_nullArgs() {
        try {
            try {
                Map<String, Object> result = util.getTokensFromAuthzCode(null, null, null, null, null, null, null, isHostnameVerification, null, null, false);
                fail("Should have thrown SocialLoginException but did not. Result was: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5462E_TOKEN_ENDPOINT_NULL_OR_EMPTY);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyTokenEndpoint() {
        try {
            final String tokenEndpoint = RandomUtils.getRandomSelection(null, "");
            try {
                Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
                fail("Should have thrown SocialLoginException but did not. Result was: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5462E_TOKEN_ENDPOINT_NULL_OR_EMPTY);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_invalidTokenEndpoint() {
        try {
            final String tokenEndpoint = "Some invalid URI";
            try {
                Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
                fail("Should have thrown SocialLoginException but did not. Result was: " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, tokenEndpoint);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyClientId() {
        try {
            final String clientId = RandomUtils.getRandomSelection(null, "");
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpointWithQuery, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyLogMessageWithInserts(outputMgr, CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER, ClientConstants.CLIENT_ID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyClientSecret() {
        try {
            final String clientSecret = RandomUtils.getRandomSelection(null, "");
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyLogMessageWithInserts(outputMgr, CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER, ClientConstants.CLIENT_SECRET);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyRedirectUri() {
        try {
            final String redirectUri = RandomUtils.getRandomSelection(null, "");
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_invalidRedirectUri() {
        try {
            final String redirectUri = "Some invalid URI";
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            // The redirect URI format is not validated before being used, so no error/warnings messages will appear
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyCode() {
        try {
            final String code = RandomUtils.getRandomSelection(null, "");
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyGrantType() {
        try {
            final String grantType = RandomUtils.getRandomSelection(null, "");
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullSSLContext() {
        try {
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, null, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyAuthMethod() {
        try {
            final String authMethod = RandomUtils.getRandomSelection(null, "");
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_nullOrEmptyResources() {
        try {
            final String resources = RandomUtils.getRandomSelection(null, "");
            postAndExtractExpectations(mapResponse, jsonResponse);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertEquals("Result did not match expected value.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getTokensFromAuthzCode_postToEndpointThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUtil).postToEndpoint(with(any(String.class)), with(any(List.class)), with(any(String.class)), 
                            with(any(String.class)), with(any(String.class)), with(any(SSLSocketFactory.class)), 
                            with(any(List.class)), with(any(boolean.class)), with(any(String.class)), with(any(Boolean.class)));
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpointWithQuery, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getTokensFromAuthzCode_postToEndpointReturnsNull() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUtil).postToEndpoint(with(any(String.class)), with(any(List.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(List.class)), with(any(boolean.class)), with(any(String.class)), with(any(Boolean.class)));
                    will(returnValue(null));
                    one(httpUtil).extractTokensFromResponse(null);
                    will(returnValue(null));
                }
            });

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertTrue("Result should have been empty but was " + result, result.isEmpty());

            verifyLogMessageWithInserts(outputMgr, CWWKS5486W_POST_RESPONSE_NULL, Pattern.quote(tokenEndpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getTokensFromAuthzCode_extractingTokensThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUtil).postToEndpoint(with(any(String.class)), with(any(List.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(List.class)), with(any(boolean.class)), with(any(String.class)), with(any(Boolean.class)));
                    will(returnValue(mapResponse));
                    one(httpUtil).extractTokensFromResponse(mapResponse);
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_extractingTokensReturnsNull() {
        try {
            postAndExtractExpectations(mapResponse, null);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertTrue("Result should have been empty but was " + result, result.isEmpty());

            verifyLogMessageWithInserts(outputMgr, CWWKS5486W_POST_RESPONSE_NULL, Pattern.quote(tokenEndpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_emptyResponse() {
        try {
            final Map<String, Object> emptyMap = new HashMap<String, Object>();
            postAndExtractExpectations(emptyMap, "");

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpointWithQuery, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertTrue("Result should have been empty but was " + result, result.isEmpty());

            verifyLogMessageWithInserts(outputMgr, CWWKS5487W_ENDPOINT_RESPONSE_NOT_JSON, Pattern.quote(tokenEndpointWithQuery), "");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_emptyJsonResponse() {
        try {
            final Map<String, Object> emptyMap = new HashMap<String, Object>();
            postAndExtractExpectations(emptyMap, "{}");

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertTrue("Result should have been empty but was " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_responseInvalidJsonFormat() {
        try {
            final String response = "Something {not in[ JSON]: format";
            final Map<String, Object> emptyMap = new HashMap<String, Object>();
            postAndExtractExpectations(emptyMap, response);

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertTrue("Result should have been empty but was " + result, result.isEmpty());

            verifyLogMessageWithInserts(outputMgr, CWWKS5487W_ENDPOINT_RESPONSE_NOT_JSON, Pattern.quote(tokenEndpoint), Pattern.quote(response));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getTokensFromAuthzCode_responseValidJson() {
        try {
            final Map<String, Object> emptyMap = new HashMap<String, Object>();
            postAndExtractExpectations(emptyMap, "{\"1\":[2,\"array1\"],\"key1\":\"value1\",\"subObj\":{}}");

            Map<String, Object> result = util.getTokensFromAuthzCode(tokenEndpoint, clientId, clientSecret, redirectUri, code, grantType, sslSocketFactory, isHostnameVerification, authMethod, resources, false);
            assertFalse("Result should not have been empty.", result.isEmpty());
            assertEquals("Number of entries in result did not match expected value. Result was " + result, 3, result.size());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** checkToken **************************************/

    @Test
    public void checkToken_nullArgs() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).postToCheckTokenEndpoint();
                    will(returnValue(null));
                }
            });

            Map<String, Object> result = mockUtil.checkToken(null, null, null, null, isHostnameVerification, null, null, false);
            assertNull("Result should have been null but was " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_postingToTokenEndpointThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).postToCheckTokenEndpoint();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_nullOrEmptyTokenEndpoint() {
        try {
            final String tokenEndpoint = RandomUtils.getRandomSelection(null, "");
            validPostToCheckTokenExpectations(mapResponse);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
            assertEquals("Response from endpoint did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_nullOrEmptyClientId() {
        try {
            final String clientId = RandomUtils.getRandomSelection(null, "");
            validPostToCheckTokenExpectations(mapResponse);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
            assertEquals("Response from endpoint did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_nullOrEmptyClientSecret() {
        try {
            final String clientSecret = RandomUtils.getRandomSelection(null, "");
            validPostToCheckTokenExpectations(mapResponse);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
            assertEquals("Response from endpoint did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            validPostToCheckTokenExpectations(mapResponse);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
            assertEquals("Response from endpoint did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_nullOrEmptyAuthMethod() {
        try {
            final String authMethod = RandomUtils.getRandomSelection(null, "");
            validPostToCheckTokenExpectations(mapResponse);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
            assertEquals("Response from endpoint did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_nullSSLContext() {
        try {
            validPostToCheckTokenExpectations(mapResponse);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, null, false);
            assertEquals("Response from endpoint did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_postToEndpointReturnsNull() {
        try {
            validPostToCheckTokenExpectations(null);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpoint, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
            assertNull("Result should have been null but was " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void checkToken_postToEndpointReturnsMap() {
        try {
            validPostToCheckTokenExpectations(mapResponse);

            Map<String, Object> result = mockUtil.checkToken(tokenEndpointWithQuery, clientId, clientSecret, accessToken, isHostnameVerification, authMethod, sslSocketFactory, false);
            assertEquals("Response from endpoint did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getUserApi **************************************/

    @Test
    public void getUserApi_nullArgs() {
        try {
            validGetFromUserApiEndpointExpectations(mapResponse);

            Map<String, Object> result = mockUtil_getUserApi.getUserApi(null, null, null, isHostnameVerification, needsSpecialHeader, false );
            assertEquals("Response did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApi_nullOrEmptyUserApi() {
        try {
            final String userApi = RandomUtils.getRandomSelection(null, "");
            validGetFromUserApiEndpointExpectations(mapResponse);

            Map<String, Object> result = mockUtil_getUserApi.getUserApi(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Response did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApi_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            validGetFromUserApiEndpointExpectations(mapResponse);

            Map<String, Object> result = mockUtil_getUserApi.getUserApi(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Response did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApi_nullSSLContext() {
        try {
            validGetFromUserApiEndpointExpectations(mapResponse);

            Map<String, Object> result = mockUtil_getUserApi.getUserApi(userApi, accessToken, null, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Response did not match expected result.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApi_exceptionThrown() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getFromUserApiEndpoint();
                    will(throwException(new ClientProtocolException(defaultExceptionMsg)));
                }
            });

            try {
                Map<String, Object> result = mockUtil_getUserApi.getUserApi(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
                fail("Should have thrown ClientProtocolException but got result " + result);
            } catch (ClientProtocolException e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApi_nullResponseMap() {
        try {
            validGetFromUserApiEndpointExpectations(null);

            Map<String, Object> result = mockUtil_getUserApi.getUserApi(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertNull("Result should have been null but was " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApi_validResponseMap() {
        try {
            validGetFromUserApiEndpointExpectations(mapResponse);

            Map<String, Object> result = mockUtil_getUserApi.getUserApi(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Result did not match the result returned from the user API endpoint.", mapResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getUserApiResponse **************************************/

    @Test
    public void getUserApiResponse_nullArgs() {
        try {
            validUserApiResponseExpectations(mapResponse, jsonResponse);

            String result = mockUtil.getUserApiResponse(null, null, null, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Result did not match the expected value.", jsonResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_nullOrEmptyUserApi() {
        try {
            final String userApi = RandomUtils.getRandomSelection(null, "");
            validUserApiResponseExpectations(mapResponse, jsonResponse);

            String result = mockUtil.getUserApiResponse(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Result did not match the expected value.", jsonResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            validUserApiResponseExpectations(mapResponse, jsonResponse);

            String result = mockUtil.getUserApiResponse(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Result did not match the expected value.", jsonResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_nullSSLContext() {
        try {
            validUserApiResponseExpectations(mapResponse, jsonResponse);

            String result = mockUtil.getUserApiResponse(userApi, accessToken, null, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Result did not match the expected value.", jsonResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_exceptionThrownGettingUserApi() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getUserApi();
                    will(throwException(new Exception(defaultExceptionMsg)));
                }
            });
            try {
                String result = mockUtil.getUserApiResponse(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
                fail("Should have thrown Exception but got result " + result);
            } catch (Exception e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_nullUserApiResponse() {
        try {
            validUserApiResponseExpectations(null, jsonResponse);

            String result = mockUtil.getUserApiResponse(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Result did not match the expected value.", jsonResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_exceptionThrownGettingJsonString() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getUserApi();
                    one(mockInterface).getJsonStringResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });
            try {
                String result = mockUtil.getUserApiResponse(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_nullJsonString() {
        try {
            validUserApiResponseExpectations(mapResponse, null);

            String result = mockUtil.getUserApiResponse(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponse_validJsonString() {
        try {
            validUserApiResponseExpectations(mapResponse, jsonResponse);

            String result = mockUtil.getUserApiResponse(userApi, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            assertEquals("Result did not match the expected JSON string.", jsonResponse, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getUserApiAsJwtToken **************************************/

    @Test
    public void getUserApiAsJwtToken_nullArgs() {
        try {
            getValidUserApiAsJwtTokenExpectations(mapResponse, "some value", jwtRef, jwt);

            JwtToken result = mockUtil.getUserApiAsJwtToken(null, null, null, isHostnameVerification, config);
            assertEquals("JWT result did not match expected value.", jwt, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_nullOrEmptyUserApi() {
        try {
            final String userApi = RandomUtils.getRandomSelection(null, "");
            getValidUserApiAsJwtTokenExpectations(mapResponse, "some value", jwtRef, jwt);

            JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, sslSocketFactory, isHostnameVerification, config);
            assertEquals("JWT result did not match expected value.", jwt, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            getValidUserApiAsJwtTokenExpectations(mapResponse, "some value", jwtRef, jwt);

            JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, sslSocketFactory, isHostnameVerification, config);
            assertEquals("JWT result did not match expected value.", jwt, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_nullSSLContext() {
        try {
            getValidUserApiAsJwtTokenExpectations(mapResponse, "some value", jwtRef, jwt);

            JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, null, isHostnameVerification, config);
            assertEquals("JWT result did not match expected value.", jwt, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_exceptionThrownGettingUserApi() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApiNeedsSpecialHeader();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(mockInterface).getUserApi();                    
                    will(throwException(new Exception(defaultExceptionMsg)));
                }
            });

            try {
                JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, sslSocketFactory, isHostnameVerification, config);
                fail("Should have thrown Exception but got result " + result);
            } catch (Exception e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_nullUserApiResult() {
        try {
            getValidUserApiAsJwtTokenExpectations(null, "some value", jwtRef, jwt);

            JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, sslSocketFactory, isHostnameVerification, config);
            assertEquals("JWT result did not match expected value.", jwt, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_exceptionThrownGettingJsonResponse() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApiNeedsSpecialHeader();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(mockInterface).getUserApi();
                    one(mockInterface).getJsonStringResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, sslSocketFactory, isHostnameVerification, config);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_nullJsonResponse() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApiNeedsSpecialHeader();
                    one(mockInterface).getUserApi();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(mockInterface).getJsonStringResponse();
                    will(returnValue(null));
                }
            });

            try {
                JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, sslSocketFactory, isHostnameVerification, config);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5493E_USERAPI_NULL_RESP_STR, userApi);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_exceptionThrownCreatingJwt() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApiNeedsSpecialHeader();
                    one(mockInterface).getUserApi();
                    one(mockInterface).getJsonStringResponse();
                    will(returnValue("some value"));
                    one(config).getJwtRef();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(mockInterface).createJwtTokenFromJson();
                    will(throwException(new Exception(defaultExceptionMsg)));
                }
            });

            try {
                JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, sslSocketFactory, isHostnameVerification, config);
                fail("Should have thrown Exception but got result " + result);
            } catch (Exception e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiAsJwtToken_nullJwt() {
        try {
            getValidUserApiAsJwtTokenExpectations(mapResponse, "some value", jwtRef, null);

            JwtToken result = mockUtil.getUserApiAsJwtToken(userApi, accessToken, null, isHostnameVerification, config);
            assertNull("Result should have been null but was " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getJsonStringResponse **************************************/

    @Test
    public void getJsonStringResponse_nullArgs() {
        try {
            String result = util.getJsonStringResponse(null, null);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJsonStringResponse_emptyMap() {
        try {
            String result = util.getJsonStringResponse(new HashMap<String, Object>(), null);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJsonStringResponse_errorResponse_nullUserApi() {
        try {
            OAuthClientUtil util = new OAuthClientUtil() {
                @Override
                public boolean isErrorResponse(HttpResponse response) {
                    return mockInterface.isErrorResponse();
                }

                @Override
                public void handleError(HttpResponse response, String userApi) throws SocialLoginException {
                    mockInterface.handleError();
                }
            };

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isErrorResponse();
                    will(returnValue(true));
                    one(mockInterface).handleError();
                }
            });

            String result = util.getJsonStringResponse(responseMap, null);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJsonStringResponse_errorResponse() {
        try {
            OAuthClientUtil util = new OAuthClientUtil() {
                @Override
                public boolean isErrorResponse(HttpResponse response) {
                    return mockInterface.isErrorResponse();
                }

                @Override
                public void handleError(HttpResponse response, String userApi) throws SocialLoginException {
                    mockInterface.handleError();
                }
            };

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isErrorResponse();
                    will(returnValue(true));
                    one(mockInterface).handleError();
                }
            });

            String result = util.getJsonStringResponse(responseMap, userApi);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJsonStringResponse_handlingErrorThrowsException() {
        try {
            OAuthClientUtil util = new OAuthClientUtil() {
                @Override
                public boolean isErrorResponse(HttpResponse response) {
                    return mockInterface.isErrorResponse();
                }

                @Override
                public void handleError(HttpResponse response, String userApi) throws SocialLoginException {
                    mockInterface.handleError();
                }
            };

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isErrorResponse();
                    will(returnValue(true));
                    one(mockInterface).handleError();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                String result = util.getJsonStringResponse(responseMap, userApi);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // TODO - Ideally should include NLS message
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJsonStringResponse_nullResponseEntity() {
        try {
            OAuthClientUtil util = new OAuthClientUtil() {
                @Override
                public boolean isErrorResponse(HttpResponse response) {
                    return mockInterface.isErrorResponse();
                }
            };

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isErrorResponse();
                    will(returnValue(false));
                    one(httpResponse).getEntity();
                    will(returnValue(null));
                }
            });

            String result = util.getJsonStringResponse(responseMap, userApi);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJsonStringResponse_IOExceptionThrown() {
        try {
            OAuthClientUtil util = new OAuthClientUtil() {
                @Override
                public boolean isErrorResponse(HttpResponse response) {
                    return mockInterface.isErrorResponse();
                }
            };

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).isErrorResponse();
                    will(returnValue(false));
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                    one(httpEntity).getContentType();
                    will(returnValue(header));
                    one(header).getElements();
                    will(returnValue(new HeaderElement[0]));
                    one(httpEntity).getContent();
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            try {
                String result = util.getJsonStringResponse(responseMap, userApi);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5492E_USERAPI_RESP_PROCESS_ERR, userApi, defaultExceptionMsg);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** handleError **************************************/

    @Test
    public void handleError_nullArgs() {
        try {
            mockUtil.handleError(null, null);
            verifyNoLogMessage(outputMgr, MSG_BASE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleError_nullStatusLine_nullEntity() {
        try {
            getStatusAndEntityExpectations(null, 0, null);
            try {
                mockUtil.handleError(httpResponse, userApi);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5491E_USERAPI_ERROR_RESPONSE, userApi);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleError_emptyErrorResponse() {
        try {
            final int statusCode = HttpServletResponse.SC_BAD_REQUEST;

            getStatusAndEntityExpectations(statusLine, statusCode, httpEntity);
            entityUtilsToStringExpectations("{}");

            try {
                mockUtil.handleError(httpResponse, userApi);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5490E_USERAPI_RESP_INVALID_STATUS, userApi, String.valueOf(statusCode), Pattern.quote("{}"), null);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleError_validErrorResponse() {
        try {
            final int statusCode = HttpServletResponse.SC_BAD_REQUEST;
            final String error = "some_error";
            final String errorDescription = "This is an error description.";
            final Map<String, Object> errorResponseResponse = new HashMap<String, Object>();
            errorResponseResponse.put(OAuthClientUtil.ERROR, error);
            errorResponseResponse.put(OAuthClientUtil.ERROR_DESCRIPTION, errorDescription);
            final String jsonErrorResponse = JsonUtils.toJson(errorResponseResponse);

            getStatusAndEntityExpectations(statusLine, statusCode, httpEntity);
            entityUtilsToStringExpectations(jsonErrorResponse);

            try {
                mockUtil.handleError(httpResponse, userApi);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5490E_USERAPI_RESP_INVALID_STATUS, userApi, String.valueOf(statusCode), error, errorDescription);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleError_nullErrorResponse_nullAuthenticateHeader() {
        try {
            final int statusCode = HttpServletResponse.SC_BAD_REQUEST;

            getStatusAndEntityExpectations(statusLine, statusCode, httpEntity);
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getFirstHeader("WWW-Authenticate");
                    will(returnValue(null));
                }
            });
            entityUtilsToStringExpectations(null);

            try {
                mockUtil.handleError(httpResponse, userApi);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5490E_USERAPI_RESP_INVALID_STATUS, userApi, String.valueOf(statusCode), null, null);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleError_nullErrorResponse_validAuthenticateHeader() {
        try {
            final int statusCode = HttpServletResponse.SC_BAD_REQUEST;
            final String headerValue = "Some header value.";

            getStatusAndEntityExpectations(statusLine, statusCode, httpEntity);
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getFirstHeader("WWW-Authenticate");
                    will(returnValue(header));
                    one(header).getValue();
                    will(returnValue(headerValue));
                }
            });
            entityUtilsToStringExpectations(null);

            try {
                mockUtil.handleError(httpResponse, userApi);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5490E_USERAPI_RESP_INVALID_STATUS, userApi, String.valueOf(statusCode), null, headerValue);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** extractErrorDescription **************************************/

    @Test
    public void extractErrorDescription_nullInput() {
        try {
            String result = mockUtil.extractErrorDescription(null);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_emptyInput() {
        try {
            String result = mockUtil.extractErrorDescription("");
            assertEquals("Result did not match expected value.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_noErrorDescription() {
        try {
            final String input = "Some kind=of input, that:doesn't, have=an, error description in, it=";
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Result did not match expected value.", input, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_onlyErrorDescriptionKey() {
        try {
            final String input = OAuthClientUtil.ERROR_DESCRIPTION;
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Result did not match expected value.", input, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_emptyErrorDescription() {
        try {
            final String value = "";
            final String input = OAuthClientUtil.ERROR_DESCRIPTION + "=" + value;
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Result did not match expected value.", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_validErrorDescription_noQuotes() {
        try {
            final String value = "some value";
            final String input = OAuthClientUtil.ERROR_DESCRIPTION + "=" + value;
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Result did not match expected value.", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_validErrorDescription_withQuotes() {
        try {
            final String value = "some value";
            final String input = OAuthClientUtil.ERROR_DESCRIPTION + "=\"" + value + "\"";
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Should have extracted description value without the quotation marks.", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_validErrorDescription_withInternalQuotes() {
        try {
            final String value = "some, \"value= with\" internal quotes\"";
            final String input = OAuthClientUtil.ERROR_DESCRIPTION + "=" + value;
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Should have extracted description value without the quotation marks.", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_errorDescriptionPreceededByInvalidChars() {
        try {
            final String value = "some value";
            final String input = "must have non-alphanumeric char before description" + OAuthClientUtil.ERROR_DESCRIPTION + "=\"" + value + "\"";
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Should not have been able to extract description value because of non-alphanumeric char immediately preceeding description.", input, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_errorDescriptionPreceededByChars() {
        try {
            final String value = "some value";
            final String input = "other=values and, stuff\"" + OAuthClientUtil.ERROR_DESCRIPTION + "=\"" + value + "\"";
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Should have extracted description since non-alphanumeric char preceeds description.", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractErrorDescription_errorDescriptionFollowedByChars() {
        try {
            final String value = "some value, other=value, followed_by=entries that, are=also attributes";
            final String input = OAuthClientUtil.ERROR_DESCRIPTION + "=" + value;
            String result = mockUtil.extractErrorDescription(input);
            assertEquals("Should have extracted description and everything that exists after the description.", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getFromEndpoint **************************************/

    @Test
    public void getFromEndpoint_nullArgs() {
        try {
            try {
                Map<String, Object> result = mockUtil.getFromEndpoint(null, null, null, null, null, null, isHostnameVerification, needsSpecialHeader, false);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_nullOrEmptyUrl() {
        try {
            String url = RandomUtils.getRandomSelection(null, "");
            try {
                Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_invalidUrl() {
        try {
            String url = "Some invalid URL";
            try {
                Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, url);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getFromEndpoint_nullOrEmptyParams() {
        try {
            List<NameValuePair> params = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());
            final String calculatedUrl = url;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_endsWithQuery() {
        try {
            String url = this.url + "?";
            final String calculatedUrl = url + paramsString;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_urlContainsQuery() {
        try {
            String url = "https://graph.facebook.com/v2.8/me?fields=id,name,email";
            final String calculatedUrl = url + "&" + paramsString;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_nullClientId() {
        try {
            String clientId = null;
            final String calculatedUrl = url + "?" + paramsString;
            mockery.checking(new Expectations() {
                {
                    one(httpUtil).createHTTPClient(sslSocketFactory, calculatedUrl, isHostnameVerification, false);
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpGet.class)));
                }
            });

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_emptyClientId() {
        try {
            String clientId = "";
            final String calculatedUrl = url + "?" + paramsString;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_nullOrEmptyClientSecret() {
        try {
            String clientSecret = RandomUtils.getRandomSelection(null, "");
            final String calculatedUrl = url + "?" + paramsString;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_nullOrEmptyAccessToken() {
        try {
            String accessToken = RandomUtils.getRandomSelection(null, "");
            final String calculatedUrl = url + "?" + paramsString;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_nullSSLContext() {
        try {
            final String calculatedUrl = url + "?" + paramsString;
            httpClientCreationAndExecutionExpectations(null, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, null, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_hostnameVerification() {
        try {
            boolean isHostnameVerification = !this.isHostnameVerification;
            final String calculatedUrl = url + "?" + paramsString;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getFromEndpoint_needsSpecialHeader() {
        try {
            boolean needsSpecialHeader = !this.needsSpecialHeader;
            final String calculatedUrl = url + "?" + paramsString;
            httpClientCreationAndExecutionExpectations(sslSocketFactory, calculatedUrl, isHostnameVerification, clientId, clientSecret);

            Map<String, Object> result = mockUtil.getFromEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, needsSpecialHeader, false);
            verifyEndpointResponse(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @SuppressWarnings("unchecked")
    void postAndExtractExpectations(final Map<String, Object> postResponse, final String tokenResponse) throws SocialLoginException {
        mockery.checking(new Expectations() {
            {
                one(httpUtil).postToEndpoint(with(any(String.class)), with(any(List.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(List.class)), with(any(boolean.class)), with(any(String.class)), with(any(Boolean.class)));
                will(returnValue(postResponse));
                one(httpUtil).extractTokensFromResponse(postResponse);
                will(returnValue(tokenResponse));
            }
        });
    }

    void getValidUserApiAsJwtTokenExpectations(final Map<String, Object> userApiResponse, final String jsonResponse, final String jwtRef, final JwtToken jwt) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(config).getUserApiNeedsSpecialHeader();
                one(config).getUseSystemPropertiesForHttpClientConnections();
                one(mockInterface).getUserApi();
                will(returnValue(userApiResponse));
                one(mockInterface).getJsonStringResponse();
                will(returnValue(jsonResponse));
                one(config).getJwtRef();                
                will(returnValue(jwtRef));
                one(mockInterface).createJwtTokenFromJson();
                will(returnValue(jwt));
            }
        });
    }

    void validPostToCheckTokenExpectations(final Map<String, Object> response) throws SocialLoginException {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).postToCheckTokenEndpoint();
                will(returnValue(response));
            }
        });
    }

    void validGetFromUserApiEndpointExpectations(final Map<String, Object> responseMap) throws IOException {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getFromUserApiEndpoint();
                will(returnValue(responseMap));
            }
        });
    }

    void validUserApiResponseExpectations(final Map<String, Object> userApiResponse, final String jsonResponse) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getUserApi();
                will(returnValue(userApiResponse));
                one(mockInterface).getJsonStringResponse();
                will(returnValue(jsonResponse));
            }
        });
    }

    void getStatusAndEntityExpectations(final StatusLine statusLine, final int statusCode, final HttpEntity httpEntity) {
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
            }
        });
        if (statusLine != null) {
            mockery.checking(new Expectations() {
                {
                    one(statusLine).getStatusCode();
                    will(returnValue(statusCode));
                }
            });
        }
        mockery.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
            }
        });
    }

    void httpClientCreationAndExecutionExpectations(final SSLSocketFactory sslSocketFactory, final String url, final boolean isHostnameVerification, final String clientId, final String clientSecret) throws IOException {
        mockery.checking(new Expectations() {
            {
                one(httpUtil).createHTTPClient(sslSocketFactory, url, isHostnameVerification, clientId, clientSecret, false);
                will(returnValue(httpClient));
                one(httpClient).execute(with(any(HttpGet.class)));
            }
        });
    }

    void getRedirectUrlExpectations(final String serverName, final Integer portFromRequest, final boolean isSecure, final int serverPort, final String scheme) {
        mockery.checking(new Expectations() {
            {
                one(request).getServerName();
                will(returnValue(serverName));
                one(mockInterface).getRedirectPortFromRequest();
                will(returnValue(portFromRequest));
            }
        });
        if (portFromRequest == null) {
            mockery.checking(new Expectations() {
                {
                    one(request).isSecure();
                    will(returnValue(isSecure));
                }
            });
            if (isSecure) {
                mockery.checking(new Expectations() {
                    {
                        one(request).getServerPort();
                        will(returnValue(serverPort));
                        one(request).getScheme();
                        will(returnValue(scheme));
                    }
                });
            }
        }
    }

    void verifyEndpointResponse(Map<String, Object> result) {
        if (result == null) {
            assertNull("Returned map should have been null but was " + result, result);
            return;
        }
        assertNotNull("Returned map should not have been null.", result);
        assertEquals("Map should only have entries for the response code and the request object.", 2, result.size());
        assertNotNull("Returned map should have non-null " + ClientConstants.RESPONSEMAP_CODE + " entry.", result.get(ClientConstants.RESPONSEMAP_CODE));
        assertNotNull("Returned map should have non-null " + ClientConstants.RESPONSEMAP_METHOD + " entry.", result.get(ClientConstants.RESPONSEMAP_METHOD));
    }

}
