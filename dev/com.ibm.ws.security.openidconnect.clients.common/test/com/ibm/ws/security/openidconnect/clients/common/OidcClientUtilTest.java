/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.http.HttpConstants;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import test.common.SharedOutputManager;

public class OidcClientUtilTest extends CommonTestClass {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    // {"access_token":"qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw","token_type":"bearer","expires_in":3599,"scope":"openid profile","refresh_token":"QGCYpfziPZY2saAagbsf5jxbMucqcF3743euknBxzkUlof7uSv","id_token":"eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk"}
    //final String strContent = "access_token=EmItdyfKwjN03hW1URF67XrC9LuFDGqXwMoaudwN"
    //                          +
    //                          "&token_type=bearer&expires_in=3599"
    //                          +
    //                          "&scope=openid%20profile"
    //                          +
    //                          "&id_token=eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJGYWtlSXNzdWVySWRlbnRpZmllckluT0F1dGgyMENvbXBvbmVudEltcGw6Y29tLmlibS5vYXV0aC5jb3JlLmludGVybmFsLm9hdXRoMjAiLCJpYXQiOjEzODYxNzExODAsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg2MTc0NzgwLCJhdWQiOiJjbGllbnQwMSJ9.QYMy5AqsoZMneBHKiBKXotXNNQ2xm4jsx3iAbJHd4Bg"
    //                          +
    //                          "&state=Uo1pv2rm4WO9SMMUINed";
    final String strContent = "{\"access_token\":\"qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw\",\"token_type\":\"bearer\",\"expires_in\":3599,\"scope\":\"openid profile\",\"refresh_token\":\"QGCYpfziPZY2saAagbsf5jxbMucqcF3743euknBxzkUlof7uSv\",\"id_token\":\"eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk\"}";

    protected final OidcClientHttpUtil oidcHttpUtil = mock.mock(OidcClientHttpUtil.class, "oidcHttpUtil");
    protected final SSLContext sslContext = mock.mock(SSLContext.class, "sslContext");
    protected final SSLSocketFactory sslSocketFactory = mock.mock(SSLSocketFactory.class, "sslSocketFactory");

    protected final HttpServletRequest httpMockRequest = mock.mock(HttpServletRequest.class, "httpMockRequest");
    protected final HttpGet mockHttpGet = mock.mock(HttpGet.class, "mockHttpGet");
    protected final HttpClient mockHttpClient = mock.mock(HttpClient.class, "mockHttpClient");
    protected final HttpResponse mockHttpResponse = mock.mock(HttpResponse.class, "mockHttpResponse");
    protected final HttpUtils mockHttpUtils = mock.mock(HttpUtils.class, "mockHttpUtils");
    protected final BasicCredentialsProvider mockBcp = mock.mock(BasicCredentialsProvider.class, "mockBcp");
    private static final String authMethod = "basic";

    final String access_token = "access_token";
    final String access_token_content = "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw";
    final String token_type = "token_type";
    final String expires_in = "expires_in";
    final String scope = "scope";
    final String refresh_token = "refresh_token";
    final String id_token = "id_token";

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    private void createHttpClientExpectations(boolean isCredentialProviderNeeded) {
        if (isCredentialProviderNeeded) {
            mock.checking(new Expectations() {
                {
                    one(mockHttpUtils).createCredentialsProvider(with("baUsername"), with("baPassword"));
                    will(returnValue(mockBcp));
                    one(mockHttpUtils).createHttpClient(with(any(SSLSocketFactory.class)), with(any(String.class)),
                            with(any(Boolean.class)), with(any(Boolean.class)), with(any(BasicCredentialsProvider.class)));
                    will(returnValue(mockHttpClient));
                }
            });
        } else {
            mock.checking(new Expectations() {
                {
                    one(mockHttpUtils).createHttpClient(with(any(SSLSocketFactory.class)), with(any(String.class)),
                            with(any(Boolean.class)), with(any(Boolean.class)), with(aNull(BasicCredentialsProvider.class)));
                    will(returnValue(mockHttpClient));
                }
            });
        }
        try {
            mock.checking(new Expectations() {
                {
                    one(mockHttpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(mockHttpResponse));
                }
            });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testConstructor() {
        try {
            OidcClientUtil oidcClientUtil = new OidcClientUtil();
            assertNotNull("Expect to get an instance of OidcUtil but none returned", oidcClientUtil);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil#getRedirectUrl(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void testGetRedirectUrl() {
        try {
            final String server = "localhost";
            final int port = 8010;
            final String protocol = "https";

            mock.checking(new Expectations() {
                {
                    one(httpMockRequest).getScheme();
                    will(returnValue(protocol));
                    one(httpMockRequest).getServerName();
                    will(returnValue(server));
                    one(httpMockRequest).getServerPort();
                    will(returnValue(port));
                    one(httpMockRequest).isSecure();
                    will(returnValue(true));

                }
            });
            OidcClientUtil oidcClientUtil = new OidcClientUtil();
            String uri = oidcClientUtil.getRedirectUrl(httpMockRequest, "/oidcclient/redirect/configId");
            boolean uricheck = false;
            if (uri.equalsIgnoreCase("https://localhost:8010/oidcclient/redirect/configId"))
                uricheck = true;
            assertTrue("Redirect URI is not correct", uricheck);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil#getUserinfo(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetUserinfo() {
        try {
            final String userInfoEndpoint = "https://localhost:8010/oidc/userInfo";
            final OidcClientUtil oicu = new OidcClientUtil();
            oicu.oidcHttpUtil = oidcHttpUtil;
            oicu.httpUtils = mockHttpUtils;
            final Map<String, Object> postResponseMap = new HashMap<String, Object>();
            postResponseMap.put(HttpConstants.RESPONSEMAP_CODE, mockHttpResponse);
            postResponseMap.put(HttpConstants.RESPONSEMAP_METHOD, mockHttpGet);

            mock.checking(new Expectations() {
                {
                    one(oidcHttpUtil).getFromEndpoint(with(any(String.class)),
                            with(any(List.class)),
                            with(any(String.class)),
                            with(any(String.class)),
                            with(any(String.class)),
                            with(any(SSLSocketFactory.class)),
                            with(any(Boolean.class)),
                            with(any(Boolean.class)));
                    will(returnValue(postResponseMap));
                }
            });

            Map<String, Object> userInfoResponse = oicu.getUserinfo(userInfoEndpoint, access_token_content, sslSocketFactory, false, false);
            assertNotNull("Expected to get an instance of userInfo map", userInfoResponse);
            assertTrue("Returned map did not contain expected " + ClientConstants.RESPONSEMAP_CODE + " entry. Map was: " + userInfoResponse, userInfoResponse.containsKey(ClientConstants.RESPONSEMAP_CODE));
            assertTrue("Returned map did not contain expected " + ClientConstants.RESPONSEMAP_METHOD + " entry. Map was: " + userInfoResponse, userInfoResponse.containsKey(ClientConstants.RESPONSEMAP_METHOD));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil#checkToken(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCheckToken() {
        try {
            final String strTokenEndpoint = "https://unknown.ibm.com:8020/openidserver/token";
            final String strClientId = "client01";
            final String strClientSecret = "secret1234";
            final OidcClientUtil oicu = new OidcClientUtil();
            oicu.oidcHttpUtil = oidcHttpUtil;

            final Map<String, Object> postResponseMap = new HashMap<String, Object>();
            postResponseMap.put(access_token, "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw");
            postResponseMap.put(token_type, "bearer");
            postResponseMap.put(expires_in, new Long(3599));
            postResponseMap.put(scope, "openid profile");
            postResponseMap.put(refresh_token, "QGCYpfziPZY2saAagbsf5jxbMucqcF3743euknBxzkUlof7uSv");
            postResponseMap.put(
                    id_token,
                    "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk");

            mock.checking(new Expectations() {
                {
                    one(oidcHttpUtil).postToIntrospectEndpoint(with(any(String.class)), //strTokenEndpoint
                            with(any(List.class)), // params
                            with(any(String.class)), // strClientId,
                            with(any(String.class)), // strClientSecret,
                            with(any(String.class)), // (String) null,
                            with(any(SSLSocketFactory.class)), // sslContext,
                            with(any(Boolean.class)), //isHostnameVerification
                            with(any(String.class)),
                            with(any(Boolean.class))); // use jvm props
                    will(returnValue(postResponseMap));
                }
            });
            Map<String, Object> tokenResponse = oicu.checkToken(strTokenEndpoint, strClientId, strClientSecret, access_token_content, false, authMethod, sslSocketFactory, false);
            assertEquals("token response = ", tokenResponse.get(access_token), "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
