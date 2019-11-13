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
package com.ibm.ws.security.social.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.regex.Pattern;

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
            //nls 

            verifyException(e, "OPENSHIFT_USER_API_BAD_RESPONSE");

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
            //nls 
            verifyException(e, "OPENSHIFT_USER_API_BAD_RESPONSE");

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
            //nls 
            verifyException(e, "CWWKS5374E");

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
            //nls 
            verifyException(e, "CWWKS5374E");

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
            //nls 
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
            //nls 
            verifyException(e, "OPENSHIFT_USER_API_BAD_RESPONSE");

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
                }
            });
            userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":\"yes\",\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            fail();
        } catch (SocialLoginException e) {
            //nls 
            verifyException(e, "OPENSHIFT_USER_API_RESPONSE_MISCONFIGURED_KEY");

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
                }
            });
            String returnedString = userApiUtils.modifyExistingResponseToJSON("{\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"ef111c43-d33a-11e9-b239-0016ac102af6\",\"groups\":[],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}");
            assertEquals(returnedString, "{\"username\":\"admin\",\"groups\":[]}");
            
        } catch (SocialLoginException e) {
            //nls 
           // verifyException(e, "OPENSHIFT_USER_API_RESPONSE_MISCONFIGURED_KEY");
               fail();
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
                verifyException(e, CWWKS5371E_OPENSHIFT_ERROR_GETTING_USER_INFO + ".+" + CWWKS5372E_OPENSHIFT_ACCESS_TOKEN_MISSING);
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
                    one(httpUtils).setHeaders(with(any(HttpURLConnection.class)), with(any(Map.class)));
                    will(returnValue(httpUrlConnection));
                    one(httpUrlConnection).setDoOutput(true);
                    one(httpUrlConnection).getOutputStream();
                    will(returnValue(outputStream));
                    one(config).getUserApiToken();
                    will(returnValue(serviceAccountToken));
                }
            });
            try {
                userApiUtils.sendUserApiRequest(accessToken, sslSocketFactory);
                fail("Should have thrown an exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5372E_OPENSHIFT_ACCESS_TOKEN_MISSING);
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

    @Test
    public void test_createUserApiRequestBody_nullAccessToken() {
        final String accessToken = null;
        try {
            try {
                String body = userApiUtils.createUserApiRequestBody(accessToken);
                fail("Should have thrown an exception but did not. Instead, got: [" + body + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5372E_OPENSHIFT_ACCESS_TOKEN_MISSING);
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
                verifyException(e, CWWKS5373E_OPENSHIFT_USER_API_BAD_STATUS + ".+" + responseCode + ".+" + Pattern.quote(connectionResponse));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
