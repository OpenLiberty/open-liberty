/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue.ValueType;
import javax.net.ssl.SSLSocketFactory;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.http.HttpUtils.RequestMethod;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class OpenShiftUserApiUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private final Oauth2LoginConfigImpl config = mockery.mock(Oauth2LoginConfigImpl.class);

    private final HttpUtils httpUtils = mockery.mock(HttpUtils.class);
    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    private final HttpURLConnection httpUrlConnection = mockery.mock(HttpURLConnection.class);
    private final OutputStream outputStream = mockery.mock(OutputStream.class);

    private final String userApi = "https://openshift.default.svc/apis/authentication.k8s.io/v1/tokenreviews";
    private final String serviceAccountToken = "myServiceAccountToken";

    private OpenShiftUserApiUtils userApiUtils;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        userApiUtils = new OpenShiftUserApiUtils(config);
        userApiUtils.httpUtils = httpUtils;
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

    @Test
    public void correctJSONTest() {
        final String correctString = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"OR4SdSuy-8NRK8NEiYXxxDu01DZcT6jPj5RJ32CDA_c\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[\"arunagroup\",\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";
        String returnedString = "";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("username"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            returnedString = userApiUtils.modifyExistingResponseToJSON(correctString);
            assertEquals(returnedString, "{\"username\":\"admin\",\"groups\":[\"arunagroup\",\"system:authenticated:oauth\",\"system:authenticated\"]}");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void nullJSONTest() {
        try {
            userApiUtils.modifyExistingResponseToJSON(null);
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5377E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void emptyJSONTest() {
        try {
            userApiUtils.modifyExistingResponseToJSON("");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5377E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void userKeyDoesNotExist() {
        try {
            userApiUtils.modifyExistingResponseToJSON("{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"OR4SdSuy-8NRK8NEiYXxxDu01DZcT6jPj5RJ32CDA_c\"},\"status\":{\"authenticated\":\"true\"}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void statusKeyDoesNotExist() {
        try {
            userApiUtils.modifyExistingResponseToJSON("{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"OR4SdSuy-8NRK8NEiYXxxDu01DZcT6jPj5RJ32CDA_c\"}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void jsonResponseStatusIsFailure() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("username"));
                }
            });
            userApiUtils.modifyExistingResponseToJSON("{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"Unauthorized\",\"reason\":\"Unauthorized\",\"code\":401}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "Unauthorized");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void responseIsNotJSONObject() {
        try {
            userApiUtils.modifyExistingResponseToJSON("vlah");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5378E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void groupsIsNotJsonArray() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("username"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":\"yes\",\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5379E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void emailExistsInUserIncorrectType() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("email"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"email\":2,\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5379E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void emailExistsInUserCorrectType() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("email"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"email\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            assertEquals(returnedString, "{\"email\":\"admin\",\"groups\":[]}");
        } catch (SocialLoginException e) {
            fail();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void emailDoesNotExistInNameWithDefaultValue() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("email"));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            assertEquals(returnedString, "{\"email\":\"admin\",\"groups\":[]}");
        } catch (SocialLoginException e) {
            fail();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void emailDoesNotExistInNameNoDefaultValue() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("email"));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });

            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY);
            verifyLogMessageWithInserts(outputMgr, CWWKS5381W_KUBERNETES_USER_API_RESPONSE_DEFAULT_USER_ATTR_NOT_FOUND, uniqueId, "email", Oauth2LoginConfigImpl.KEY_userNameAttribute, "username");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void usernameAttributeDoesNotExist() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("blah"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void usernameAttributeIncorrectTypeInteger() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("blah"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"blah\":1,\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5379E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void usernameAttributeIncorrectType() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("blah"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"blah\":null,\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5379E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void usernameAttributeSuccess() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("blah"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"blah\":\"blue\",\"groups\":[\"arunagroup\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            assertEquals(returnedString, "{\"blah\":\"blue\",\"groups\":[\"arunagroup\"]}");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void userNameAttributeIsJunk() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("blah"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"email\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void groupsIsEmpty() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("username"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            assertEquals(returnedString, "{\"username\":\"admin\",\"groups\":[]}");

        } catch (SocialLoginException e) {
            fail();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void missingUserKeyFromResponse() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("username"));
                }
            });
            String errorResponse = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"somebadvalueForAnAccessToken\"},\"status\":{\"user\":{},\"error\":\"[invalid bearer token, token lookup failed]\"}}";
            userApiUtils.modifyExistingResponseToJSON(errorResponse);
        } catch (SocialLoginException e) {
            verifyException(e, "CWWKS5380E");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void userResponseApiUserEmpty() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getUserNameAttribute();
                    will(returnValue("blah"));
                    allowing(config).getGroupNameAttribute();
                    will(returnValue("groups"));
                }
            });
            userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{}}}");
            fail();
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getUserApiResponse_nullAccessToken() {
        String accessToken = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(userApi));
                    one(httpUtils).createConnection(RequestMethod.POST, userApi, sslSocketFactory);
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    one(config).getUserApiToken();
                    will(returnValue(serviceAccountToken));
                }
            });
            try {
                String response = userApiUtils.getUserApiResponse(accessToken, sslSocketFactory);
                fail("Should have thrown an exception but did not. Got response [" + response + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5371E_KUBERNETES_ERROR_GETTING_USER_INFO + ".+" + CWWKS5372E_KUBERNETES_ACCESS_TOKEN_MISSING);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_sendUserApiRequest_nullAccessToken() {
        String accessToken = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(userApi));
                    one(httpUtils).createConnection(RequestMethod.POST, userApi, sslSocketFactory);
                    will(returnValue(httpUrlConnection));
                    one(config).getUserApiToken();
                    will(returnValue(serviceAccountToken));
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    will(returnValue(httpUrlConnection));
                    one(httpUrlConnection).setDoOutput(true);
                    one(httpUrlConnection).getOutputStream();
                    will(returnValue(outputStream));
                }
            });
            try {
                userApiUtils.sendUserApiRequest(accessToken, sslSocketFactory);
                fail("Should have thrown an exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5372E_KUBERNETES_ACCESS_TOKEN_MISSING);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_sendUserApiRequest_emptyAccessToken() {
        String accessToken = "";
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(userApi));
                    one(httpUtils).createConnection(RequestMethod.POST, userApi, sslSocketFactory);
                    will(returnValue(httpUrlConnection));
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    will(returnValue(httpUrlConnection));
                    one(httpUrlConnection).setDoOutput(true);
                    one(httpUrlConnection).getOutputStream();
                    will(returnValue(outputStream));
                    one(config).getUserApiToken();
                    will(returnValue(serviceAccountToken));
                    one(outputStream).write(with(any(byte[].class)), with(any(int.class)), with(any(int.class)));
                    allowing(outputStream).close();
                    one(httpUrlConnection).connect();
                }
            });
            userApiUtils.sendUserApiRequest(accessToken, sslSocketFactory);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_sendUserApiRequest_nonEmptyAccessToken() {
        String accessToken = "this.is.an.access.token";
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(userApi));
                    one(httpUtils).createConnection(RequestMethod.POST, userApi, sslSocketFactory);
                    will(returnValue(httpUrlConnection));
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    will(returnValue(httpUrlConnection));
                    one(httpUrlConnection).setDoOutput(true);
                    one(httpUrlConnection).getOutputStream();
                    will(returnValue(outputStream));
                    one(config).getUserApiToken();
                    will(returnValue(serviceAccountToken));
                    one(outputStream).write(with(any(byte[].class)), with(any(int.class)), with(any(int.class)));
                    allowing(outputStream).close();
                    one(httpUrlConnection).connect();
                }
            });
            userApiUtils.sendUserApiRequest(accessToken, sslSocketFactory);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_sendServiceAccountIntrospectRequest_nullServiceAccountToken() {
        String serviceAccountToken = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(userApi));
                    one(httpUtils).createConnection(RequestMethod.GET, userApi, sslSocketFactory);
                    will(returnValue(httpUrlConnection));
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    will(returnValue(httpUrlConnection));
                    one(httpUrlConnection).connect();
                }
            });
            userApiUtils.sendServiceAccountIntrospectRequest(serviceAccountToken, sslSocketFactory);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_sendServiceAccountIntrospectRequest_emptyServiceAccountToken() {
        String serviceAccountToken = "";
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(userApi));
                    one(httpUtils).createConnection(RequestMethod.GET, userApi, sslSocketFactory);
                    will(returnValue(httpUrlConnection));
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    will(returnValue(httpUrlConnection));
                    one(httpUrlConnection).connect();
                }
            });
            userApiUtils.sendServiceAccountIntrospectRequest(serviceAccountToken, sslSocketFactory);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_sendServiceAccountIntrospectRequest_nonEmptyServiceAccountToken() {
        String serviceAccountToken = "Some interesting service account token value";
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApi();
                    will(returnValue(userApi));
                    one(httpUtils).createConnection(RequestMethod.GET, userApi, sslSocketFactory);
                    will(returnValue(httpUrlConnection));
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    will(returnValue(httpUrlConnection));
                    one(httpUrlConnection).connect();
                }
            });
            userApiUtils.sendServiceAccountIntrospectRequest(serviceAccountToken, sslSocketFactory);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createUserApiRequestBody_nullAccessToken() {
        final String accessToken = null;
        try {
            try {
                String body = userApiUtils.createUserApiRequestBody(accessToken);
                fail("Should have thrown an exception but did not. Instead, got: [" + body + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5372E_KUBERNETES_ACCESS_TOKEN_MISSING);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createUserApiRequestBody_nonEmptyAccessToken() {
        final String accessToken = "this.is.an.access.token";
        try {
            String body = userApiUtils.createUserApiRequestBody(accessToken);
            String expectedTokenEntry = "\"token\":\"" + accessToken + "\"";
            assertTrue("Body did not contain the expected \"token\" entry with the access token. Expected to find " + expectedTokenEntry + " but body was [" + body + "].", body.contains(expectedTokenEntry));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readUserApiResponse_unexpectedStatus() {
        // The OpenShift user info API actually returns a 201 in the successful case; any other status code is unexpected
        final int responseCode = 200;
        final String connectionResponse = "This is the response.";
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUrlConnection).getResponseCode();
                    will(returnValue(responseCode));
                    one(httpUtils).readConnectionResponse(httpUrlConnection);
                    will(returnValue(connectionResponse));
                }
            });
            try {
                String response = userApiUtils.readUserApiResponse(httpUrlConnection);
                fail("Should have thrown an exception because we didn't get the right response code, but instead got [" + response + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5373E_KUBERNETES_USER_API_BAD_STATUS + ".+" + responseCode + ".+" + Pattern.quote(connectionResponse));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readServiceAccountIntrospectResponse_unexpectedStatus() {
        final int responseCode = 401;
        final String connectionResponse = "This is the response.";
        try {
            mockery.checking(new Expectations() {
                {
                    one(httpUrlConnection).getResponseCode();
                    will(returnValue(responseCode));
                    one(httpUtils).readConnectionResponse(httpUrlConnection);
                    will(returnValue(connectionResponse));
                }
            });
            try {
                String response = userApiUtils.readServiceAccountIntrospectResponse(httpUrlConnection);
                fail("Should have thrown an exception because we didn't get the right response code, but instead got [" + response + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5383E_SERVICE_ACCOUNT_USER_API_BAD_STATUS + ".+" + responseCode + ".+" + Pattern.quote(connectionResponse));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_emptyResponseObject() {
        try {
            JsonObject response = Json.createObjectBuilder().build();
            try {
                JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY, "status");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_nonEmptyResponseObject_missingStatus() {
        try {
            JsonObject response = Json.createObjectBuilder().add("user", true).add("other", "value").build();
            try {
                JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY, "status");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_statusEntryNotJsonObject() {
        try {
            JsonObject response = Json.createObjectBuilder().add("status", 123).build();
            try {
                JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5379E_KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE + ".+" + "status" + ".+" + ValueType.OBJECT + ".*" + ValueType.NUMBER);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_statusEntryNonFailureString() {
        try {
            JsonObject response = Json.createObjectBuilder().add("status", "some string value").build();
            try {
                JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5379E_KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE + ".+" + "status" + ".+" + ValueType.OBJECT + ".*" + ValueType.STRING);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_failureStatus_noMessage() {
        try {
            JsonObject response = Json.createObjectBuilder().add("status", "Failure").build();
            try {
                JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5379E_KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE + ".+" + "status" + ".+" + ValueType.OBJECT + ".*" + ValueType.STRING);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_failureStatus_includesMessage_messageNotAString() {
        try {
            JsonObject response = Json.createObjectBuilder().add("status", "Failure").add("message", true).build();
            try {
                JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5379E_KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE + ".+" + "status" + ".+" + ValueType.OBJECT + ".*" + ValueType.STRING);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_failureStatus_includesMessage() {
        try {
            String messageString = "This is the error message";
            JsonObject response = Json.createObjectBuilder().add("status", "Failure").add("message", messageString).build();
            try {
                JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, messageString);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse_emptyStatusEntry() {
        try {
            JsonObject response = Json.createObjectBuilder().add("status", Json.createObjectBuilder().build()).build();

            JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
            assertTrue("Result was expected to be empty but wasn't. Result was: " + result, result.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getStatusJsonObjectFromResponse() {
        try {
            JsonObject statusEntry = Json.createObjectBuilder().add("one", 1).add("two", "value").build();
            JsonObject response = Json.createObjectBuilder().add("status", statusEntry).build();

            JsonObject result = userApiUtils.getStatusJsonObjectFromResponse(response);
            assertEquals("Result did not match the expected value.", statusEntry, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getUserJsonObjectFromResponse_emptyResponseObject() {
        try {
            JsonObject response = Json.createObjectBuilder().build();
            try {
                JsonObject result = userApiUtils.getUserJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5374E_KUBERNETES_USER_API_RESPONSE_MISSING_KEY, "user");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getUserJsonObjectFromResponse_responseContainsError() {
        try {
            int errorValue = 1234;
            JsonObject response = Json.createObjectBuilder().add("error", 1234).build();
            try {
                JsonObject result = userApiUtils.getUserJsonObjectFromResponse(response);
                fail("Should have thrown an exception but got: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5380E_KUBERNETES_USER_API_RESPONSE_ERROR + ".+" + errorValue);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO
    // getUserJsonObjectFromResponse

    // TODO
    // createModifiedResponse
    // addUserAttributeToResponseWithEmail
    // addUserToResponseWithoutEmail

    @Test
    public void test_addGroupNameToResponse_emptyUserMap() {
        try {
            final String groupNameAttribute = "blah";
            JsonObject userInnerMap = Json.createObjectBuilder().build();
            JsonObjectBuilder modifiedResponse = Json.createObjectBuilder();

            mockery.checking(new Expectations() {
                {
                    one(config).getGroupNameAttribute();
                    will(returnValue(groupNameAttribute));
                }
            });
            userApiUtils.addGroupNameToResponse(userInnerMap, modifiedResponse);

            JsonObject result = modifiedResponse.build();
            assertTrue("Groups should not have been added to the result, but were. Result was: " + result, result.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addGroupNameToResponse_groupEntryWrongType() {
        try {
            final String groupNameAttribute = "groups";
            JsonObject userInnerMap = Json.createObjectBuilder().add(groupNameAttribute, 123).build();
            JsonObjectBuilder modifiedResponse = Json.createObjectBuilder();

            mockery.checking(new Expectations() {
                {
                    allowing(config).getGroupNameAttribute();
                    will(returnValue(groupNameAttribute));
                }
            });
            try {
                userApiUtils.addGroupNameToResponse(userInnerMap, modifiedResponse);
                fail("Should have thrown an exception but did not. Instead, created result: " + modifiedResponse.build());
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5379E_KUBERNETES_USER_API_RESPONSE_WRONG_JSON_TYPE + ".*" + groupNameAttribute + ".*" + ValueType.ARRAY + ".*" + ValueType.NUMBER);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addGroupNameToResponse_emptyGroups() {
        try {
            final String groupNameAttribute = "blah";
            JsonArray groupsArray = Json.createArrayBuilder().build();
            JsonObject userInnerMap = Json.createObjectBuilder().add(groupNameAttribute, groupsArray).build();
            JsonObjectBuilder modifiedResponse = Json.createObjectBuilder();

            mockery.checking(new Expectations() {
                {
                    allowing(config).getGroupNameAttribute();
                    will(returnValue(groupNameAttribute));
                }
            });
            userApiUtils.addGroupNameToResponse(userInnerMap, modifiedResponse);

            JsonObject result = modifiedResponse.build();
            assertFalse("Groups should have been added to the result, but the result was empty.", result.isEmpty());
            assertEquals("Only one entry should have been added to the result. Result was: " + result, 1, result.size());
            assertTrue("Result was missing expected group name attribute key [" + groupNameAttribute + "]. Result was: " + result, result.containsKey(groupNameAttribute));
            assertEquals("Groups array entry did not match the expected value.", groupsArray, result.getJsonArray(groupNameAttribute));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addGroupNameToResponse_nonEmptyGroups() {
        try {
            final String groupNameAttribute = "groups";
            JsonArray groupsArray = Json.createArrayBuilder().add("one").add(2).add(true).build();
            JsonObject userInnerMap = Json.createObjectBuilder().add(groupNameAttribute, groupsArray).build();
            JsonObjectBuilder modifiedResponse = Json.createObjectBuilder();

            mockery.checking(new Expectations() {
                {
                    allowing(config).getGroupNameAttribute();
                    will(returnValue(groupNameAttribute));
                }
            });
            userApiUtils.addGroupNameToResponse(userInnerMap, modifiedResponse);

            JsonObject result = modifiedResponse.build();
            assertFalse("Groups should have been added to the result, but the result was empty.", result.isEmpty());
            assertEquals("Only one entry should have been added to the result. Result was: " + result, 1, result.size());
            assertTrue("Result was missing expected group name attribute key [" + groupNameAttribute + "]. Result was: " + result, result.containsKey(groupNameAttribute));
            assertEquals("Groups array entry did not match the expected value.", groupsArray, result.getJsonArray(groupNameAttribute));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readResponseAsJsonObject_nullResponse() {
        try {
            String response = null;
            JsonObject result = userApiUtils.readResponseAsJsonObject(response);
            fail("Should have thrown an exception but got: " + result);
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5384E_RESPONSE_NOT_JSON);
        }
    }

    @Test
    public void test_readResponseAsJsonObject_emptyResponse() {
        try {
            String response = "";
            JsonObject result = userApiUtils.readResponseAsJsonObject(response);
            fail("Should have thrown an exception but got: " + result);
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5384E_RESPONSE_NOT_JSON);
        }
    }

    @Test
    public void test_readResponseAsJsonObject_nonJsonResponse() {
        String response = "This is not JSON";
        try {
            JsonObject result = userApiUtils.readResponseAsJsonObject(response);
            fail("Should have thrown an exception but got: " + result);
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5384E_RESPONSE_NOT_JSON);
        }
    }

    @Test
    public void test_readResponseAsJsonObject_emptyJsonResponse() {
        try {
            String response = "{}";
            JsonObject result = userApiUtils.readResponseAsJsonObject(response);
            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have been empty but was " + result, result.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readResponseAsJsonObject_normalJsonResponse() {
        try {
            String response = "{\"key\":\"value\",\"int\":42,\"boolean\":true,\"array\":[1,2,3]}";
            JsonObject result = userApiUtils.readResponseAsJsonObject(response);
            assertNotNull("Result should not have been null but was.", result);
            assertFalse("Result should not have been empty but was.", result.isEmpty());
            assertEquals("Result did not have the expected number of entries. Result was " + result, 4, result.size());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getJsonObjectValueFromJson_emptyJsonResponse() {
        String key = "myKey";
        JsonObject input = Json.createObjectBuilder().build();
        try {
            JsonObject result = userApiUtils.getJsonObjectValueFromJson(input, key);
            fail("Should have thrown an exception but got: " + result);
        } catch (SocialLoginException e) {
            verifyExceptionWithInserts(e, CWWKS5385E_JSON_MISSING_KEY, key);
        }
    }

    @Test
    public void test_getJsonObjectValueFromJson_missingKey() {
        String key = "myKey";
        JsonObject input = Json.createObjectBuilder().add("1", "value").add(key + "2", "other value").build();
        try {
            JsonObject result = userApiUtils.getJsonObjectValueFromJson(input, key);
            fail("Should have thrown an exception but got: " + result);
        } catch (SocialLoginException e) {
            verifyExceptionWithInserts(e, CWWKS5385E_JSON_MISSING_KEY, key);
        }
    }

    @Test
    public void test_getJsonObjectValueFromJson_keyValueNotJsonObject() {
        String key = "myKey";
        JsonObject input = Json.createObjectBuilder().add(key, "value").build();
        try {
            JsonObject result = userApiUtils.getJsonObjectValueFromJson(input, key);
            fail("Should have thrown an exception but got: " + result);
        } catch (SocialLoginException e) {
            verifyException(e, CWWKS5386E_JSON_ENTRY_WRONG_JSON_TYPE);
        }
    }

    @Test
    public void test_getJsonObjectValueFromJson() {
        try {
            String key = "myKey";
            JsonObject value = Json.createObjectBuilder().add("1", "value").add("other key", 42).build();
            JsonObject input = Json.createObjectBuilder().add(key, value).build();
            JsonObject result = userApiUtils.getJsonObjectValueFromJson(input, key);
            assertEquals("Value for key \"" + key + "\" did not match expected value.", value, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_modifyUsername_nullUsernameAttribute() {
        try {
            JsonObject input = Json.createObjectBuilder().build();
            mockery.checking(new Expectations() {
                {
                    one(config).getUserNameAttribute();
                    will(returnValue(null));
                }
            });
            JsonObject result = userApiUtils.modifyUsername(input);
            assertEquals("Result should have remained an unmodified, empty JSON object.", input, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_modifyUsername_missingUsernameAttribute() {
        final String usernameAttribute = "user";
        JsonObject input = Json.createObjectBuilder().build();
        mockery.checking(new Expectations() {
            {
                one(config).getUserNameAttribute();
                will(returnValue(usernameAttribute));
            }
        });
        try {
            JsonObject result = userApiUtils.modifyUsername(input);
            fail("Should have thrown an exception but got " + result);
        } catch (SocialLoginException e) {
            verifyExceptionWithInserts(e, CWWKS5385E_JSON_MISSING_KEY, usernameAttribute);
        }
    }

    @Test
    public void test_modifyUsername_usernameAttributeEntryWrongType() {
        final String usernameAttribute = "user";
        JsonObject input = Json.createObjectBuilder().add(usernameAttribute, 123).build();
        mockery.checking(new Expectations() {
            {
                one(config).getUserNameAttribute();
                will(returnValue(usernameAttribute));
            }
        });
        try {
            JsonObject result = userApiUtils.modifyUsername(input);
            fail("Should have thrown an exception but got " + result);
        } catch (SocialLoginException e) {
            verifyExceptionWithInserts(e, CWWKS5386E_JSON_ENTRY_WRONG_JSON_TYPE, usernameAttribute);
        }
    }

    @Test
    public void test_modifyUsername_usernameIsEmpty() {
        try {
            final String usernameAttribute = "user";
            String usernameValue = "";
            JsonObject input = Json.createObjectBuilder().add(usernameAttribute, usernameValue).build();
            mockery.checking(new Expectations() {
                {
                    one(config).getUserNameAttribute();
                    will(returnValue(usernameAttribute));
                }
            });
            JsonObject result = userApiUtils.modifyUsername(input);
            assertEquals("Result should have remained an unmodified from the inputted JSON object.", input, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_modifyUsername_nonServiceAccount() {
        try {
            final String usernameAttribute = "username should be here";
            String usernameValue = "jane doe";
            JsonObject input = Json.createObjectBuilder().add(usernameAttribute, usernameValue).build();
            mockery.checking(new Expectations() {
                {
                    one(config).getUserNameAttribute();
                    will(returnValue(usernameAttribute));
                }
            });
            JsonObject result = userApiUtils.modifyUsername(input);
            assertEquals("Result should have remained an unmodified from the inputted JSON object.", input, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_modifyUsername_serviceAccount_noGroupOrName() {
        try {
            final String usernameAttribute = "user";
            String prefix = "system:serviceaccount:";
            String groupAndName = "";
            String usernameValue = prefix + groupAndName;
            JsonObject input = Json.createObjectBuilder().add(usernameAttribute, usernameValue).build();
            mockery.checking(new Expectations() {
                {
                    one(config).getUserNameAttribute();
                    will(returnValue(usernameAttribute));
                }
            });
            JsonObject result = userApiUtils.modifyUsername(input);
            assertNotSame("Result should have been modified in some way, but wasn't.", input, result);
            assertEquals("Modified username did not match the expected value. Full result was " + result, groupAndName, result.getString(usernameAttribute));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_modifyUsername_serviceAccount_groupAndNameMatchPrefix() {
        try {
            final String usernameAttribute = "user";
            String prefix = "system:serviceaccount:";
            String groupAndName = prefix;
            String usernameValue = prefix + groupAndName;
            JsonObject input = Json.createObjectBuilder().add(usernameAttribute, usernameValue).build();
            mockery.checking(new Expectations() {
                {
                    one(config).getUserNameAttribute();
                    will(returnValue(usernameAttribute));
                }
            });
            JsonObject result = userApiUtils.modifyUsername(input);
            assertNotSame("Result should have been modified in some way, but wasn't.", input, result);
            assertEquals("Modified username did not match the expected value. Full result was " + result, groupAndName, result.getString(usernameAttribute));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_modifyUsername_serviceAccount_typical() {
        try {
            final String usernameAttribute = "user";
            String prefix = "system:serviceaccount:";
            String groupAndName = "some group:and user";
            String usernameValue = prefix + groupAndName;
            JsonObject input = Json.createObjectBuilder().add(usernameAttribute, usernameValue).build();
            mockery.checking(new Expectations() {
                {
                    one(config).getUserNameAttribute();
                    will(returnValue(usernameAttribute));
                }
            });
            JsonObject result = userApiUtils.modifyUsername(input);
            assertNotSame("Result should have been modified in some way, but wasn't.", input, result);
            assertEquals("Modified username did not match the expected value. Full result was " + result, groupAndName, result.getString(usernameAttribute));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addProjectNameAsGroup_noProjectInUserMetadata() {
        JsonObject userMetadata = Json.createObjectBuilder().add("name", "value").add("other key", 42).build();
        final String userNameAttribute = "name";
        mockery.checking(new Expectations() {
            {
                one(config).getUserNameAttribute();
                will(returnValue (userNameAttribute));
            }
        });
        JsonObject result = null;
        try {
            result = userApiUtils.addProjectNameAsGroup(userMetadata);
        } catch (SocialLoginException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
        assertEquals("Output should have matched the original user metadata object, but did not.", userMetadata, result);
    }

   
    @Test
    public void test_addProjectNameAsGroup_singleCharacterProject() {
        JsonObject userMetadata = Json.createObjectBuilder().add("name", "p:username").add("groups", "group123").build();
        final String userNameAttribute = "name";
        final String groupNameAttribute = "groups";
        String groupsValue = "p";
        mockery.checking(new Expectations() {
            {
                one(config).getUserNameAttribute();
                will(returnValue(userNameAttribute));
                one(config).getGroupNameAttribute();
                will(returnValue(groupNameAttribute));
            }
        });
        JsonObject result = null;
        try {
            result = userApiUtils.addProjectNameAsGroup(userMetadata);
        } catch (SocialLoginException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
        assertEquals("Groups entry in result did not match the expected value.", groupsValue, result.getString(groupNameAttribute));
    }
    @Test
    public void test_addProjectNameAsGroup_nullGroupNameAttribute() {
        JsonObject userMetadata = Json.createObjectBuilder().add("name", "p:username").add("groups", "group123").build();
        final String userNameAttribute = "name";
        
        mockery.checking(new Expectations() {
            {
                one(config).getUserNameAttribute();
                will(returnValue(userNameAttribute));
                one(config).getGroupNameAttribute();
                will(returnValue (null) );
            }
        });
        JsonObject result = null;
        try {
            result = userApiUtils.addProjectNameAsGroup(userMetadata);
        } catch (SocialLoginException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
        assertEquals("Output should have matched the original user metadata object, but did not.", userMetadata, result);
    }
    
    @Test
    public void test_addProjectNameAsGroup() {
        JsonObject userMetadata = Json.createObjectBuilder().add("name", "myproject:userA").add("groups", "groupABC").build();
        final String userNameAttribute = "name";
        final String groupNameAttribute = "groups";
        String groupsValue = "myproject";
        mockery.checking(new Expectations() {
            {
                one(config).getUserNameAttribute();
                will(returnValue(userNameAttribute));
                one(config).getGroupNameAttribute();
                will(returnValue(groupNameAttribute));
            }
        });
        JsonObject result = null;
        try {
            result = userApiUtils.addProjectNameAsGroup(userMetadata);
        } catch (SocialLoginException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
        assertEquals("Groups entry in result did not match the expected value.", groupsValue, result.getString(groupNameAttribute));
    }

//    @Test
//    public void test_addGroupsToResult_groupsReplacesExistingEntryInJson() {
//        final String groupNameAttribute = "groups";
//        JsonArray groupsValue = Json.createArrayBuilder().add("group1").add("group2").build();
//        JsonObject userMetadata = Json.createObjectBuilder().add(groupNameAttribute, "value").add("other key", 42).build();
//        JsonObject rawResponse = Json.createObjectBuilder().add(groupNameAttribute, groupsValue).build();
//        mockery.checking(new Expectations() {
//            {
//                one(config).getGroupNameAttribute();
//                will(returnValue(groupNameAttribute));
//            }
//        });
//        JsonObject result = userApiUtils.addGroupsToResult(userMetadata, rawResponse);
//        assertNotSame("Output should not have matched the original user metadata object, but did.", userMetadata, result);
//        assertTrue("Output was missing the entry for the groups data. Result was " + result, result.containsKey(groupNameAttribute));
//        assertEquals("Groups entry in result did not match the expected value.", groupsValue, result.getJsonArray(groupNameAttribute));
//    }

}
