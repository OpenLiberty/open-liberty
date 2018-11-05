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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class OAuthClientHttpUtilTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    OAuthClientHttpUtil util = null;
    OAuthClientHttpUtil mockUtil = null;
    OAuthClientHttpUtil mockEndpointUtil = null;

    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    private final HttpUriRequest httpUriRequest = mockery.mock(HttpUriRequest.class);
    private final HttpResponse httpResponse = mockery.mock(HttpResponse.class);
    private final HttpClient httpClient = mockery.mock(HttpClient.class);
    private final StatusLine statusLine = mockery.mock(StatusLine.class);
    private final Header header = mockery.mock(Header.class);

    private final String clientId = "myClientId";
    private final String clientSecret = "myClientSecret";
    private final String redirectUri = "http://redirect-uri.com/some/path";
    private final String code = "myCode";
    private final String grantType = "myGrantType";
    private final String authMethod = ClientConstants.METHOD_client_secret_post;
    private final String accessToken = "myAccessToken";
    private final boolean isHostnameVerification = false;
    private final String httpsScheme = "https";
    private final String host = "some-domain.com";
    private final String url = httpsScheme + "://" + host + "/some/path";
    private final String urlWithQuery = url + "?query=string&values";

    private final static String key1 = "key1";
    private final static String key2 = "key2";
    private final static String param1 = "param1";
    private final static String param2 = "param2";
    private final static String header1 = "header1";
    private final static String header2 = "header2";
    private final static String value1 = "value1";
    private final static String value2 = "value2";
    private final static Map<String, Object> basicMap = new HashMap<String, Object>();
    static {
        basicMap.put(key1, value1);
        basicMap.put(key2, value2);
    }
    private final static List<NameValuePair> params = new ArrayList<NameValuePair>();
    static {
        params.add(new BasicNameValuePair(param1, value1));
        params.add(new BasicNameValuePair(param2, value2));
    }
    private final static List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();
    static {
        commonHeaders.add(new BasicNameValuePair(header1, value1));
        commonHeaders.add(new BasicNameValuePair(header2, value2));
    }

    public interface MockInterface {
        public HttpClient createHTTPClient();

        public void verifyResponse() throws SocialLoginException;

        public Map<String, Object> commonEndpointInvocation() throws SocialLoginException;
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        util = new OAuthClientHttpUtil();
        mockUtil = new OAuthClientHttpUtil() {
            @Override
            public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useJvmProps) {
                return mockInterface.createHTTPClient();
            }

            @Override
            public void verifyResponse(String url, HttpResponse response) throws SocialLoginException {
                mockInterface.verifyResponse();
            }
        };
        mockEndpointUtil = new OAuthClientHttpUtil() {
            @Override
            public Map<String, Object> commonEndpointInvocation(HttpUriRequest httpUriRequest, String url, String baUsername, @Sensitive String baPassword, String accessToken, SSLSocketFactory sslSocketFactory, boolean isHostnameVerification, String authMethod, boolean useJvmProps) throws SocialLoginException {
                return mockInterface.commonEndpointInvocation();
            }
        };
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

    /************************************** extractTokensFromResponse **************************************/

    @Test
    public void extractTokensFromResponse_nullInput() {
        try {
            String result = util.extractTokensFromResponse(null);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractTokensFromResponse_emptyInput() {
        try {
            String result = util.extractTokensFromResponse(new HashMap<String, Object>());
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractTokensFromResponse_missingRequiredParam() {
        try {
            String result = util.extractTokensFromResponse(basicMap);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractTokensFromResponse_nullResponseEntity() {
        try {
            Map<String, Object> mapWithResponse = new HashMap<String, Object>();
            mapWithResponse.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getEntity();
                    will(returnValue(null));
                }
            });

            String result = util.extractTokensFromResponse(mapWithResponse);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractTokensFromResponse_nullEntityContent() {
        try {
            Map<String, Object> mapWithResponse = new HashMap<String, Object>();
            mapWithResponse.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                    one(httpEntity).getContentType();
                    will(returnValue(header));
                    one(header).getElements();
                    will(returnValue(new HeaderElement[0]));
                    one(httpEntity).getContent();
                    will(returnValue(null));
                }
            });

            String result = util.extractTokensFromResponse(mapWithResponse);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void extractTokensFromResponse_validEntityContent() {
        try {
            Map<String, Object> mapWithResponse = new HashMap<String, Object>();
            mapWithResponse.put(ClientConstants.RESPONSEMAP_CODE, httpResponse);

            String response = "some response";
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                }
            });
            entityUtilsToStringExpectations(response);

            String result = util.extractTokensFromResponse(mapWithResponse);
            assertEquals("Result did not match expected value.", response, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createPostMethod **************************************/

    @Test
    public void createPostMethod_nullArgs() {
        try {
            try {
                HttpPost result = util.createPostMethod(null, null);
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
    public void createPostMethod_nullOrEmptyRequestUrl() {
        try {
            String url = RandomUtils.getRandomSelection(null, "");
            try {
                HttpPost result = util.createPostMethod(url, params);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createPostMethod_nullOrEmptyParams() {
        try {
            List<NameValuePair> params = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());

            HttpPost result = util.createPostMethod(url, params);
            assertNotNull("Result should not have been null.", result);
            assertEquals("Resulting object should have POST method.", "POST", result.getMethod());
            assertEquals("Result should not have any headers but had " + Arrays.toString(result.getAllHeaders()), 0, result.getAllHeaders().length);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createPostMethod_invalidUrl() {
        try {
            String url = "invalid URL";
            try {
                HttpPost result = util.createPostMethod(url, params);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, url);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createPostMethod_withParams() {
        try {
            HttpPost result = util.createPostMethod(urlWithQuery, params);
            assertNotNull("Result should not have been null.", result);
            assertEquals("Resulting object should have POST method.", "POST", result.getMethod());
            assertEquals("Request did not have the expected number of headers. Found headers " + Arrays.toString(result.getAllHeaders()), params.size(), result.getAllHeaders().length);
            assertNotNull("Result should have had " + param1 + " header.", result.getFirstHeader(param1));
            assertNotNull("Result should have had " + param2 + " header.", result.getFirstHeader(param2));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createHttpGetMethod **************************************/

    @Test
    public void createHttpGetMethod_nullArgs() {
        try {
            try {
                HttpGet result = util.createHttpGetMethod(null, null);
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
    public void createHttpGetMethod_nullOrEmptyRequestUrl() {
        try {
            String url = RandomUtils.getRandomSelection(null, "");
            try {
                HttpGet result = util.createHttpGetMethod(url, params);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createHttpGetMethod_nullOrEmptyParams() {
        try {
            List<NameValuePair> params = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());

            HttpGet result = util.createHttpGetMethod(url, params);
            assertNotNull("Result should not have been null.", result);
            assertEquals("Resulting object should have GET method.", "GET", result.getMethod());
            assertEquals("Result should not have any headers but had " + Arrays.toString(result.getAllHeaders()), 0, result.getAllHeaders().length);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createHttpGetMethod_invalidUrl() {
        try {
            String url = "invalid URL";
            try {
                HttpGet result = util.createHttpGetMethod(url, params);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, url);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createHttpGetMethod_withParams() {
        try {
            HttpGet result = util.createHttpGetMethod(urlWithQuery, params);
            assertNotNull("Result should not have been null.", result);
            assertEquals("Resulting object should have GET method.", "GET", result.getMethod());
            assertEquals("Result should not have any headers but had " + Arrays.toString(result.getAllHeaders()), params.size(), result.getAllHeaders().length);
            assertNotNull("Result should have had " + param1 + " header.", result.getFirstHeader(param1));
            assertNotNull("Result should have had " + param2 + " header.", result.getFirstHeader(param2));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** executeRequest **************************************/

    @Test
    public void executeRequest_nullArgs() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(null);
                }
            });
            HttpResponse result = mockUtil.executeRequest(null, null, isHostnameVerification, null, false);
            assertNotNull("Result should not have been null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void executeRequest_nullSSLContext() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(httpUriRequest);
                }
            });
            HttpResponse result = mockUtil.executeRequest(null, url, isHostnameVerification, httpUriRequest, false);
            assertNotNull("Result should not have been null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void executeRequest_nullOrEmptyUrl() {
        try {
            String url = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(httpUriRequest);
                }
            });
            HttpResponse result = mockUtil.executeRequest(sslSocketFactory, url, isHostnameVerification, httpUriRequest, false);
            assertNotNull("Result should not have been null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void executeRequest_hostnameVerification() {
        try {
            boolean isHostnameVerification = !this.isHostnameVerification;
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(httpUriRequest);
                }
            });
            HttpResponse result = mockUtil.executeRequest(sslSocketFactory, url, isHostnameVerification, httpUriRequest, false);
            assertNotNull("Result should not have been null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void executeRequest_nullRequestObject() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(null);
                }
            });
            HttpResponse result = mockUtil.executeRequest(sslSocketFactory, url, isHostnameVerification, null, false);
            assertNotNull("Result should not have been null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void executeRequest_executeThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(httpUriRequest);
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });
            try {
                HttpResponse result = mockUtil.executeRequest(sslSocketFactory, urlWithQuery, isHostnameVerification, httpUriRequest, false);
                fail("Should have thrown SocialLoginException but got " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5476E_ERROR_EXECUTING_REQUEST + ".*\\[" + Pattern.quote(urlWithQuery) + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** verifyResponse **************************************/

    @Test
    public void verifyResponse_nullArgs() {
        try {
            util.verifyResponse(null, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void verifyResponse_nullOrEmptyUrl() {
        try {
            String url = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getStatusLine();
                    will(returnValue(statusLine));
                    one(statusLine).getStatusCode();
                    will(returnValue(HttpServletResponse.SC_OK));
                }
            });

            util.verifyResponse(url, httpResponse);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void verifyResponse_nullResponseObject() {
        try {
            util.verifyResponse(url, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void verifyResponse_missingStatusLine_nullEntity() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getStatusLine();
                    will(returnValue(null));
                    one(httpResponse).getEntity();
                    will(returnValue(null));
                }
            });

            try {
                util.verifyResponse(url, httpResponse);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                String insertMsg = CWWKS5477E_RESPONSE_STATUS_MISSING_OR_ERROR;
                verifyException(e, CWWKS5478E_RESPONSE_STATUS_UNSUCCESSFUL + ".*\\[" + url + "\\].*" + Pattern.quote(insertMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void verifyResponse_response200() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getStatusLine();
                    will(returnValue(statusLine));
                    one(statusLine).getStatusCode();
                    will(returnValue(HttpServletResponse.SC_OK));
                }
            });

            util.verifyResponse(urlWithQuery, httpResponse);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void verifyResponse_entityToStringThrowsException() {
        try {
            final int statusCode = HttpServletResponse.SC_BAD_REQUEST;
            final String reasonPhrase = "Some reason phrase";
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getStatusLine();
                    will(returnValue(statusLine));
                    allowing(statusLine).getStatusCode();
                    will(returnValue(statusCode));
                    one(statusLine).getReasonPhrase();
                    will(returnValue(reasonPhrase));
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
                util.verifyResponse(urlWithQuery, httpResponse);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                String insertMsg = CWWKS5477E_RESPONSE_STATUS_MISSING_OR_ERROR + ".*\\[" + statusCode + " " + reasonPhrase + "\\]";
                verifyException(e, CWWKS5478E_RESPONSE_STATUS_UNSUCCESSFUL + ".*\\[" + Pattern.quote(urlWithQuery) + "\\].*" + insertMsg);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void verifyResponse_withEntityErrorMessage() {
        try {
            final String eMsg = "Some error message.";
            mockery.checking(new Expectations() {
                {
                    one(httpResponse).getStatusLine();
                    will(returnValue(statusLine));
                    allowing(statusLine).getStatusCode();
                    one(statusLine).getReasonPhrase();
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                }
            });
            entityUtilsToStringExpectations(eMsg);

            try {
                util.verifyResponse(url, httpResponse);
                fail("Should have thrown SocialLoginException.");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5478E_RESPONSE_STATUS_UNSUCCESSFUL + ".*\\[" + url + "\\].*" + Pattern.quote(eMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** postToEndpoint **************************************/

    @Test
    public void postToEndpoint_nullArgs() {
        try {
            try {
                Map<String, Object> result = mockUtil.postToEndpoint(null, null, null, null, null, null, null, isHostnameVerification, null, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_nullOrEmptyUrl() {
        try {
            final String url = RandomUtils.getRandomSelection(null, "");
            try {
                Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_invalidUrl() {
        try {
            final String url = "invalid URL";
            try {
                Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
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
    public void postToEndpoint_nullOrEmptyParams() {
        try {
            final List<NameValuePair> params = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_nullOrEmptyUsername() {
        try {
            final String clientId = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_nullOrEmptyPassword() {
        try {
            final String clientSecret = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);

            boolean expectAuthzHeader = false;
            int expectedNumHeaders = commonHeaders.size();
            if (accessToken != null) {
                // Non-null access token will be added in Authorization header, otherwise no Authorization header will be added
                expectedNumHeaders += 1;
                expectAuthzHeader = true;
            }
            verifyEndpointRequest(result, "POST", expectedNumHeaders, expectAuthzHeader);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_nullSSLContext() {
        try {
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, null, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void postToEndpoint_nullOrEmptyHeaders() {
        try {
            final List<NameValuePair> commonHeaders = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_hostnameVerification() {
        try {
            final boolean isHostnameVerification = !this.isHostnameVerification;
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_nullOrEmptyAuthMethod() {
        try {
            final String authMethod = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_executingRequestThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.postToEndpoint(urlWithQuery, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5476E_ERROR_EXECUTING_REQUEST + ".*\\[" + Pattern.quote(urlWithQuery) + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToEndpoint_verifyingResponseThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(httpResponse));
                    one(mockInterface).verifyResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.postToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // Exception isn't currently wrapped by anything, so the raw exception message will be thrown
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getToEndpoint **************************************/

    @Test
    public void getToEndpoint_nullArgs() {
        try {
            try {
                Map<String, Object> result = mockUtil.getToEndpoint(null, null, null, null, null, null, null, isHostnameVerification, null, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_nullOrEmptyUrl() {
        try {
            final String url = RandomUtils.getRandomSelection(null, "");
            try {
                Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_invalidUrl() {
        try {
            final String url = "some invalid URL";
            try {
                Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
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
    public void getToEndpoint_nullOrEmptyParams() {
        try {
            final List<NameValuePair> params = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "GET", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_nullOrEmptyClientId() {
        try {
            final String clientId = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "GET", params.size() + commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_nullOrEmptyClientSecret() {
        try {
            final String clientSecret = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "GET", params.size() + commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);

            boolean expectAuthzHeader = false;
            int expectedNumHeaders = params.size() + commonHeaders.size();
            if (accessToken != null) {
                // Non-null access token will be added in Authorization header, otherwise no Authorization header will be added
                expectedNumHeaders += 1;
                expectAuthzHeader = true;
            }
            verifyEndpointRequest(result, "GET", expectedNumHeaders, expectAuthzHeader);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_nullSSLContext() {
        try {
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, null, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "GET", params.size() + commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getToEndpoint_nullOrEmptyHeaders() {
        try {
            final List<NameValuePair> commonHeaders = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            // Only parameters should show up as headers since no common headers provided
            verifyEndpointRequest(result, "GET", params.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_hostnameVerification() {
        try {
            boolean isHostnameVerification = !this.isHostnameVerification;
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(urlWithQuery, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "GET", params.size() + commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_nullOrEmptyAuthMethod() {
        try {
            final String authMethod = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "GET", params.size() + commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_executingRequestThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.getToEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5476E_ERROR_EXECUTING_REQUEST + ".*\\[" + url + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getToEndpoint_verifyingResponseThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(httpResponse));
                    one(mockInterface).verifyResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.getToEndpoint(urlWithQuery, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // Exception isn't currently wrapped by anything, so the raw exception message will be thrown
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** postToIntrospectEndpoint **************************************/

    @Test
    public void postToIntrospectEndpoint_nullArgs() {
        try {
            try {
                Map<String, Object> result = mockUtil.postToIntrospectEndpoint(null, null, null, null, null, null, null, isHostnameVerification, null, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_nullOrEmptyUrl() {
        try {
            final String url = RandomUtils.getRandomSelection(null, "");
            try {
                Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_invalidUrl() {
        try {
            final String url = "invalid URL";
            try {
                Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
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
    public void postToIntrospectEndpoint_nullOrEmptyParams() {
        try {
            final List<NameValuePair> params = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_nullOrEmptyUsername() {
        try {
            final String clientId = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_nullOrEmptyPassword() {
        try {
            final String clientSecret = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);

            boolean expectAuthzHeader = false;
            int expectedNumHeaders = commonHeaders.size();
            if (accessToken != null) {
                // Non-null access token will be added in Authorization header, otherwise no Authorization header will be added
                expectedNumHeaders += 1;
                expectAuthzHeader = true;
            }
            verifyEndpointRequest(result, "POST", expectedNumHeaders, expectAuthzHeader);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_nullSSLContext() {
        try {
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, null, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void postToIntrospectEndpoint_nullOrEmptyHeaders() {
        try {
            final List<NameValuePair> commonHeaders = RandomUtils.getRandomSelection(null, new ArrayList<NameValuePair>());
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_hostnameVerification() {
        try {
            final boolean isHostnameVerification = !this.isHostnameVerification;
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_nullOrEmptyAuthMethod() {
        try {
            final String authMethod = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();

            Map<String, Object> result = mockUtil.postToIntrospectEndpoint(urlWithQuery, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyEndpointRequest(result, "POST", commonHeaders.size() + 1, true);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_executingRequestThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5476E_ERROR_EXECUTING_REQUEST + ".*\\[" + url + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void postToIntrospectEndpoint_verifyingResponseThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(httpResponse));
                    one(mockInterface).verifyResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.postToIntrospectEndpoint(url, params, clientId, clientSecret, accessToken, sslSocketFactory, commonHeaders, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // Exception isn't currently wrapped by anything, so the raw exception message will be thrown
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** commonEndpointInvocation **************************************/

    @Test
    public void commonEndpointInvocation_nullArgs() {
        try {
            commonEndpointInvocationExpectations();

            // HttpUriRequest is created internally and shouldn't be null; URI is expected to have already been verified by the time this method is called
            Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, urlWithQuery, null, null, null, null, isHostnameVerification, null, false);

            verifyEndpointResult(result);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_nullOrEmptyUsername() {
        try {
            final String clientId = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_nullOrEmptyPassword() {
        try {
            final String clientSecret = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_nullOrEmptyAccessToken() {
        try {
            final String accessToken = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();
            if (accessToken != null) {
                mockery.checking(new Expectations() {
                    {
                        one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                    }
                });
            }

            Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_nullSSLContext() {
        try {
            commonEndpointInvocationExpectations();
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, null, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_hostnameVerification() {
        try {
            final boolean isHostnameVerification = !this.isHostnameVerification;
            commonEndpointInvocationExpectations();
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_nullOrEmptyAuthMethod() {
        try {
            final String authMethod = RandomUtils.getRandomSelection(null, "");
            commonEndpointInvocationExpectations();
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, authMethod, false);

            verifyEndpointResult(result);
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_executingRequestThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5476E_ERROR_EXECUTING_REQUEST + ".*\\[" + url + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void commonEndpointInvocation_verifyingResponseThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                    allowing(mockInterface).createHTTPClient();
                    will(returnValue(httpClient));
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(httpResponse));
                    one(mockInterface).verifyResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = mockUtil.commonEndpointInvocation(httpUriRequest, url, clientId, clientSecret, accessToken, sslSocketFactory, isHostnameVerification, authMethod, false);
                fail("Should have thrown SocialLoginException but got result " + result);
            } catch (SocialLoginException e) {
                // Exception isn't currently wrapped by anything, so the raw exception message will be thrown
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Test
    public void setAuthorizationHeader_nullArgs() {
        try {
            mockUtil.setAuthorizationHeader(null, null, null, null, null);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAuthorizationHeader_nullOrEmptyUsername() {
        try {
            final String clientId = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            mockUtil.setAuthorizationHeader(clientId, clientSecret, accessToken, httpUriRequest, authMethod);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAuthorizationHeader_nullOrEmptyPassword() {
        try {
            final String clientSecret = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            mockUtil.setAuthorizationHeader(clientId, clientSecret, accessToken, httpUriRequest, authMethod);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAuthorizationHeader_nullAccessToken() {
        try {
            mockUtil.setAuthorizationHeader(clientId, clientSecret, null, httpUriRequest, authMethod);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAuthorizationHeader_emptyAccessToken() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER);
                }
            });

            mockUtil.setAuthorizationHeader(clientId, clientSecret, "", httpUriRequest, authMethod);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAuthorizationHeader_nullOrEmptyAuthMethod() {
        try {
            final String authMethod = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            mockUtil.setAuthorizationHeader(clientId, clientSecret, accessToken, httpUriRequest, authMethod);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAuthorizationHeader_basicAuthWithAccessToken() {
        try {
            final String authMethod = ClientConstants.METHOD_client_secret_basic;
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
                }
            });

            mockUtil.setAuthorizationHeader(clientId, clientSecret, accessToken, httpUriRequest, authMethod);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void setAuthorizationHeader_basicAuthWithoutAccessToken() {
        try {
            final String authMethod = ClientConstants.METHOD_client_secret_basic;
            mockery.checking(new Expectations() {
                {
                    one(httpUriRequest).addHeader(ClientConstants.AUTHORIZATION, "Basic " + Base64Coder.base64Encode(clientId + ":" + clientSecret));
                }
            });

            mockUtil.setAuthorizationHeader(clientId, clientSecret, null, httpUriRequest, authMethod);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private void commonEndpointInvocationExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(mockInterface).createHTTPClient();
                will(returnValue(httpClient));
                one(httpClient).execute(with(any(HttpUriRequest.class)));
                will(returnValue(httpResponse));
                one(mockInterface).verifyResponse();
            }
        });
    }

    private void verifyEndpointResult(Map<String, Object> result) {
        assertNotNull("Result from posting to endpoint should not have been null.", result);
        assertEquals("Number of entries in result did not match expected value. Result was " + result, 2, result.size());
        assertNotNull("Result is missing " + ClientConstants.RESPONSEMAP_CODE + " entry. Result was " + result, result.get(ClientConstants.RESPONSEMAP_CODE));
        assertNotNull("Result is missing " + ClientConstants.RESPONSEMAP_METHOD + " entry. Result was " + result, result.get(ClientConstants.RESPONSEMAP_METHOD));
    }

    /**
     * Verifies information about the request object used to obtain the provided result.
     * 
     * @param result
     * @param requestMethod
     * @param numHeaders
     *            Expected number of headers in the request object.
     * @param expectAuthzHeader
     *            Boolean indicating whether the request object is expected to include an Authorization header.
     */
    private void verifyEndpointRequest(Map<String, Object> result, String requestMethod, int numHeaders, boolean expectAuthzHeader) {
        assertNotNull("Result is missing " + ClientConstants.RESPONSEMAP_METHOD + " entry. Result was " + result, result.get(ClientConstants.RESPONSEMAP_METHOD));
        // Get the request method object from the result
        HttpRequestBase request = null;
        if (requestMethod.equals("POST")) {
            request = (HttpPost) result.get(ClientConstants.RESPONSEMAP_METHOD);
        } else {
            request = (HttpGet) result.get(ClientConstants.RESPONSEMAP_METHOD);
        }

        // Validate method
        assertEquals("Request method did not match the expected value.", requestMethod, request.getMethod());

        // Validate number of headers
        Header[] headers = request.getAllHeaders();
        if (numHeaders == 0) {
            assertNull("Request should not have had any headers. Found headers " + Arrays.toString(headers), headers);
        } else {
            assertEquals("Request did not have the expected number of headers. Found headers " + Arrays.toString(headers), numHeaders, headers.length);
        }

        if (expectAuthzHeader) {
            assertNotNull("Request should have had an " + ClientConstants.AUTHORIZATION + " header but did not. Headers were " + Arrays.toString(headers), request.getFirstHeader(ClientConstants.AUTHORIZATION));
        } else {
            assertNull("Request should not have had an " + ClientConstants.AUTHORIZATION + " header but did. Headers were " + Arrays.toString(headers), request.getFirstHeader(ClientConstants.AUTHORIZATION));
        }
    }

}
