/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.test.common.CommonTestClass;

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
     * {@link com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil#getFromEndpoint(java.lang.String, java.util.List, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetFromEndpoint() {
        OidcClientUtil oicu = new OidcClientUtil();
        try {
            final HttpGet getMethod = new HttpGet("http://localhost:8010/oidc/someEndPoint");

            mock.checking(new Expectations() {
                {
                    one(oidcHttpUtil).createHTTPClient(with(any(SSLSocketFactory.class)), with(any(String.class)),
                            with(any(Boolean.class)), with(any(String.class)), with(any(String.class)), with(any(Boolean.class)));
                    will(returnValue(mockHttpClient));
                    one(mockHttpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(mockHttpResponse));
                }
            });

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            oicu.oidcHttpUtil = oidcHttpUtil;
            Map<String, Object> result = oicu.getFromEndpoint("http://localhost:8010/oidc/someEndPoint", params, "baUsername", "baPassword", access_token_content, sslSocketFactory, false, false);
            HttpResponse responseCode = (HttpResponse) result.get(ClientConstants.RESPONSEMAP_CODE);
            assertNotNull("Expect to see valid response code", responseCode);
            HttpGet getMethod2 = (HttpGet) result.get(ClientConstants.RESPONSEMAP_METHOD);
            assertEquals("HttpGet method ", getMethod.getMethod(), getMethod2.getMethod());
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
            final Map<String, Object> postResponseMap = new HashMap<String, Object>();
            postResponseMap.put(token_type, "bearer");

            mock.checking(new Expectations() {
                {
                    one(oidcHttpUtil).createHTTPClient(with(any(SSLSocketFactory.class)), with(any(String.class)),
                            with(any(Boolean.class)), with(any(Boolean.class)));
                    will(returnValue(mockHttpClient));
                    one(mockHttpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(mockHttpResponse));
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
            final String strClientSecret = "secret";
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
                            with(any(List.class)), // commonHeaders
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

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTokensFromAuthzCode() {
        try {
            final String strTokenEndpoint = "https://unknown.ibm.com:8020/openidserver/token";
            final String strClientId = "client01";
            final String strClientSecret = "secret";
            final String strRedirectUri = "https://client.ibm.com:8020/oidcclient/redirect";
            final String strCode = "xabceyfghil";
            final String strGrantType = "autoiztion_code";
            OidcClientUtil oicu = new OidcClientUtil();
            assertNotNull("Expected to get an instance of OidcClientutil but none", oicu);
            oicu.oidcHttpUtil = oidcHttpUtil;
            final Map<String, Object> postResponseMap = new HashMap<String, Object>();
            // {"access_token":"qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw","token_type":"bearer","expires_in":3599,"scope":"openid profile","refresh_token":"QGCYpfziPZY2saAagbsf5jxbMucqcF3743euknBxzkUlof7uSv","id_token":"eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk"}
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
                    one(oidcHttpUtil).postToEndpoint(with(any(String.class)), //strTokenEndpoint
                            with(any(List.class)), // params
                            with(any(String.class)), // strClientId,
                            with(any(String.class)), // strClientSecret,
                            with(any(String.class)), // (String) null,
                            with(any(SSLSocketFactory.class)), // sslContext,
                            with(any(List.class)), // commonHeaders
                            with(any(Boolean.class)),
                            with(any(String.class)), //isHostnameVerification
                            with(any(Boolean.class)));
                    will(returnValue(postResponseMap));
                    allowing(oidcHttpUtil).setClientId(strClientId);
                    one(oidcHttpUtil).extractTokensFromResponse(with(any(Map.class)));
                    will(returnValue(strContent));
                }
            });
            HashMap<String, String> results = oicu.getTokensFromAuthzCode(strTokenEndpoint,
                    strClientId,
                    strClientSecret,
                    strRedirectUri,
                    strCode,
                    strGrantType,
                    sslSocketFactory,
                    false,
                    authMethod,
                    null, null, false);

            Set<String> keys = results.keySet();
            boolean bAccessToken = false;
            boolean bTokenType = false;
            boolean bScope = false;
            boolean bRefreshToken = false;
            boolean bIdToken = false;
            for (String key : keys) {
                String value = results.get(key);
                System.out.println("**" + key + ":" + value);
                if (key.equalsIgnoreCase(access_token))
                    bAccessToken = true;
                if (key.equalsIgnoreCase(token_type))
                    bTokenType = true;
                if (key.equalsIgnoreCase(scope))
                    bScope = true;
                if (key.equalsIgnoreCase(refresh_token))
                    bRefreshToken = true;
                if (key.equalsIgnoreCase(id_token))
                    bIdToken = true;
            }
            assertNotNull("Expect to get an instance of Tokens but none returned", results);
            assertTrue("access_token not found", bAccessToken);
            assertTrue("token_type not found", bTokenType);
            assertTrue("scope not found", bScope);
            assertTrue("refresh_token not found", bRefreshToken);
            assertTrue("id_token not found", bIdToken);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostToTokenEndpoint() {
        try {
            final String strTokenEndpoint = "https://unknown.ibm.com:8020/openidserver/token";
            final String strClientId = "client01";
            final String strClientSecret = "secret";
            final String strRedirectUri = "https://client.ibm.com:8020/oidcclient/redirect";
            final String strCode = "xabceyfghil";
            final String strGrantType = "autoiztion_code";
            OidcClientUtil oicu = new OidcClientUtil();
            assertNotNull("Expected to get an instance of OidcClientutil but none", oicu);
            oicu.oidcHttpUtil = oidcHttpUtil;
            final Map<String, Object> postResponseMap = new HashMap<String, Object>();
            // {"access_token":"qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw","token_type":"bearer","expires_in":3599,"scope":"openid profile","refresh_token":"QGCYpfziPZY2saAagbsf5jxbMucqcF3743euknBxzkUlof7uSv","id_token":"eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk"}
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
                    one(oidcHttpUtil).postToEndpoint(with(any(String.class)), //strTokenEndpoint
                            with(any(List.class)), // params
                            with(any(String.class)), // strClientId,
                            with(any(String.class)), // strClientSecret,
                            with(any(String.class)), // (String) null,
                            with(any(SSLSocketFactory.class)), // sslContext,
                            with(any(List.class)), // commonHeaders
                            with(any(Boolean.class)), //isHostnameVerification
                            with(any(String.class)),
                            with(any(Boolean.class)));
                    will(returnValue(postResponseMap));
                }
            });
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(ClientConstants.GRANT_TYPE, strGrantType));
            params.add(new BasicNameValuePair(ClientConstants.REDIRECT_URI, strRedirectUri));
            params.add(new BasicNameValuePair(Constants.CODE, strCode));

            Map<String, Object> results = oicu.postToTokenEndpoint(strTokenEndpoint,
                    params,
                    strClientId,
                    strClientSecret,
                    sslSocketFactory,
                    false,
                    authMethod,
                    false);
            Set<String> keys = results.keySet();
            boolean bAccessToken = false;
            boolean bTokenType = false;
            boolean bExpiresIn = false;
            boolean bScope = false;
            boolean bRefreshToken = false;
            boolean bIdToken = false;
            for (String key : keys) {
                Object obj = results.get(key);
                String value = null;
                if (obj instanceof String)
                    value = (String) obj;
                else
                    value = obj.toString();
                System.out.println("**" + key + ":" + value);
                if (key.equalsIgnoreCase(access_token))
                    bAccessToken = true;
                if (key.equalsIgnoreCase(token_type))
                    bTokenType = true;
                if (key.equalsIgnoreCase(expires_in))
                    bExpiresIn = true;
                if (key.equalsIgnoreCase(scope))
                    bScope = true;
                if (key.equalsIgnoreCase(refresh_token))
                    bRefreshToken = true;
                if (key.equalsIgnoreCase(id_token))
                    bIdToken = true;
            }
            assertNotNull("Expect to get an instance of Tokens but none returned", results);
            assertTrue("access_token not found", bAccessToken);
            assertTrue("token_type not found", bTokenType);
            assertTrue("expires_in not found", bExpiresIn);
            assertTrue("scope not found", bScope);
            assertTrue("refresh_token not found", bRefreshToken);
            assertTrue("id_token not found", bIdToken);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
