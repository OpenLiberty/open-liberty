/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.common.http.HttpUtils;

import test.common.SharedOutputManager;

public class OidcClientHttpUtilTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    static final OidcClientHttpUtil defaultInstance = OidcClientHttpUtil.getInstance();

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
    final String jwks = "{\"keys\":[{\"kid\":\"jzJ247XK8epeoTvntypp\",\"use\":\"sig\",\"alg\":\"RS256\",\"kty\":\"RSA\",\"n\":\"6SIkxdS6_RTfnyLwVDP5D_f3HI53horUMc5VJ-e33Z9EcNFb71e2_lqVEzulREjNzPOW7bVFAd8FOrRgKW1T6_bkbzW0ygV02ouLL4CqdD4xZu-4RuvEbb8jUPQE7CPkka7fwHMnWyP_LrXkAaVK60_tthzQNyCVBBvtZaPQbDKhANic0jf8RKCr2eVOjMfbxIb60dGsc4Ya-pDItazlRDr75KLDE98UMSxbGUDRjNAg-AGbdAJSP2I30z-DhqdThZxGijcGj6lqmOA9RU9Xv2ruaIQd7lp7DUHIN9xlS-hgbpDE7Rdo2aWnX3WjvQ6hEUspY5tj1oDc44iMHk2sBQ\",\"e\":\"AQAB\"},{\"kid\":\"SnO5oQ19hkcCZIfOkimH\",\"use\":\"sig\",\"alg\":\"RS256\",\"kty\":\"RSA\",\"n\":\"nL7zzM8XOFqsi_77dDxuRaBTvccvEFh01HUc6OeVMAnpPwWz725oIdUNOXC3ylDHq-FhngNDdkh8XiPfAQ711BO6JMn4U4yfqHi6jAxLGSgdrxwWy8KNiAopb0gVkOX7JWLK-W8L_L5BPFCQKiuxHs-BuWow09u8vKHeSbC69OiVKKVjT3ftn7ogeL8V5I5fwGdetkqyuQCLqT35NNC2qNde7DX6Ti7lGAGNCNNg4NU-d2RaiOs5jaFCsWPkO6PRPbiWMPaH9Ouk6ZswWj_nbZsBAvlMKqE6Z0M07a-XoAKDVyZBVT52AK2ejBww0MDqNm_LuVy-7s9FQrNuQp_8Ww\",\"e\":\"AQAB\"}]}";
    protected final HttpResponse httpResponse = mock.mock(HttpResponse.class, "httpResponse");
    protected final HttpEntity httpEntity = mock.mock(HttpEntity.class, "httpEntity");
    protected final javax.net.ssl.SSLSocketFactory sslJavaFactory = mock.mock(javax.net.ssl.SSLSocketFactory.class, "sslJavaFactory");
    protected final SSLContext sslContext = mock.mock(SSLContext.class, "sslContext");
    //protected final InputStream inputStream = mock.mock(InputStream.class, "inputStream");
    protected final InputStream inputStream = new MockInputStream(strContent);
    protected final InputStream inputStreamBadResponse = new MockInputStream("");
    protected final InputStream inputStreamJwkResponse = new MockInputStream(jwks);
    protected final SSLContextSpi sslContextSpi = mock.mock(SSLContextSpi.class, "sslContextSpi");
    protected final mockSSLContextSpi mockSslContextSpi = mock.mock(mockSSLContextSpi.class, "mockSslContextSpi");
    protected final javax.net.ssl.SSLSocketFactory javaxSslSocketFactory = mock.mock(javax.net.ssl.SSLSocketFactory.class, "javaxSslSocketFactory");
    protected final Provider provider = mock.mock(Provider.class, "provider");
    // protected final SSLServerSocketFactory sslServerSocketFactory = mock.mock(SSLServerSocketFactory.class, "sslServerSocketFactory");
    protected final CredentialsProvider credentialsProvider = mock.mock(CredentialsProvider.class, "credentialsProvider");
    protected final HttpGet httpGet = mock.mock(HttpGet.class, "httpGet");
    protected final HttpPost httpPost = mock.mock(HttpPost.class, "httpPost");
    protected final HttpClient httpClient = mock.mock(HttpClient.class, "httpClient");
    protected final StatusLine statusLine = mock.mock(StatusLine.class, "statusLine");
    protected final IOException ioException = mock.mock(IOException.class, "ioException");
    protected final Throwable stackTraceThrowable = mock.mock(Throwable.class, "stackTraceThrowable");
    protected final SSLSocketFactory sslSocketFactory = mock.mock(SSLSocketFactory.class, "sslSocketFactory");
    private static final String authMethod = "basic";
    mockOidcClientHttpUtil moichu = new mockOidcClientHttpUtil();
    protected final HttpUtils mockHttpUtils = mock.mock(HttpUtils.class, "mockHttpUtils");
    protected final BasicCredentialsProvider mockBcp = mock.mock(BasicCredentialsProvider.class, "mockBcp");

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
        OidcClientHttpUtil.instance = defaultInstance; // reset the static variable after tests
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            OidcClientHttpUtil oichu = new OidcClientHttpUtil();
            assertNotNull("Expected to get an instance of OidcClientHttpUtil but none", oichu);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testExtractTokensFromResponse() {

        final String methodName = "testExtractTokensFromResponse";
        try {
            Map<String, Object> postResponseMap = new HashMap<String, Object>();
            postResponseMap.put(HttpConstants.RESPONSEMAP_CODE, httpResponse);
            mock.checking(new Expectations() {
                {
                    one(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                    one(httpEntity).getContent();
                    will(returnValue(inputStream));
                    allowing(httpEntity).getContentLength();
                    will(returnValue((long) strContent.length()));
                    allowing(httpEntity).getContentType();
                    will(returnValue((Header) null));
                }
            });
            OidcClientHttpUtil oichu = new OidcClientHttpUtil();
            String result = oichu.extractEntityFromTokenResponse(postResponseMap);
            assertNotNull("expect to get some tokens but not", result);
            assertEquals("expect to get strContent but get '" + result + "'", strContent, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testExtractTokensFromResponse_null() {

        final String methodName = "testExtractTokensFromResponse_null";
        try {
            Map<String, Object> postResponseMap = new HashMap<String, Object>();
            postResponseMap.put(HttpConstants.RESPONSEMAP_CODE, httpResponse);
            mock.checking(new Expectations() {
                {
                    one(httpResponse).getEntity();
                    will(returnValue((HttpEntity) null));

                }
            });
            OidcClientHttpUtil oichu = new OidcClientHttpUtil();
            String result = oichu.extractEntityFromTokenResponse(postResponseMap);
            assertNull("expect to get a Null back but not", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetAuthorizationHeaderForGetMethod() {
        final String methodName = "testSetAuthorizationHeaderForGetMethod";
        try {
            String baUsername = "testuser";
            String baPassword = "testuserpwd";

            OidcClientHttpUtil oidcHttpUtil = new OidcClientHttpUtil();
            oidcHttpUtil.createHTTPClient(sslSocketFactory,
                    "https://localhost:8020/oidc/app1",
                    true,
                    baUsername,
                    baPassword, false);
            // It supposed to be OK without any Exception
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void testSetAuthorizationHeaderForPostMethod() {
        final String methodName = "testSetAuthorizationHeaderForPostMethod";
        final String accessToken = "accessTokenContent123456";
        try {
            mock.checking(new Expectations() {
                {
                    one(httpPost).setHeader(with(any(String.class)), with(any(String.class)));
                    one(httpPost).addHeader(HttpConstants.AUTHORIZATION, (HttpConstants.BEARER + accessToken));
                }
            });
            String baUsername = "testuser";
            String baPassword = "testuserpwd";

            OidcClientHttpUtil oidcHttpUtil = new OidcClientHttpUtil();
            oidcHttpUtil.setAuthorizationHeaderForPostMethod(baUsername,
                    baPassword,
                    accessToken,
                    httpPost,
                    authMethod);
            // It supposed to be OK without any Exception
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    SSLContext createFakedSSLContext() {
        return new mockSSLContext(mockSslContextSpi, provider, "https");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostToEndPoint() {
        final String methodName = "testPostToEndPoint";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(httpClient).execute(with(any(HttpPost.class)));
                    will(returnValue(httpResponse));
                    allowing(httpResponse).getStatusLine();
                    will(returnValue(statusLine));
                    allowing(statusLine).getStatusCode();
                    will(returnValue(200));
                }
            });
            assertNotNull("Expected to get an instance of OidcClientHttpUtil but none", moichu);
            String url = "http://unknownclient/url1";
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            String baUsername = "testuser";
            String baPassword = "testuserpwd";
            String accessToken = "accesstokenstringabcdef";

            Map<String, Object> map = moichu.postToEndpoint(url, params, baUsername, baPassword, accessToken, sslSocketFactory, false, authMethod, false);
            HttpResponse myRes = (HttpResponse) map.get(HttpConstants.RESPONSEMAP_CODE);
            assertEquals("Did not get baclk HttepResponse " + httpResponse + " but " + myRes,
                    httpResponse, myRes);
            Object httpPost = map.get(HttpConstants.RESPONSEMAP_METHOD);
            assertTrue("Did not get back a HttpPost class but " + httpPost.getClass().getName(), httpPost instanceof HttpPost);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPostToEndPoint_executeThrowsException() {
        final String methodName = "testPostToEndPoint_executeThrowsException";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(httpClient).execute(with(any(HttpPost.class)));
                    will(throwException(ioException));
                    allowing(ioException).fillInStackTrace();
                    will(returnValue(stackTraceThrowable));
                    allowing(ioException).getMessage();
                    will(returnValue("The OpenID Connect client is unable to contact the OpenID Connect provider."));
                }
            });
            mockOidcClientHttpUtil mockClientUtil = new mockOidcClientHttpUtil();
            assertNotNull("Expected to get an instance of OidcClientHttpUtil but did not get one.", mockClientUtil);
            String url = "http://unknownclient/url1";
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            String baUsername = "testuser";
            String baPassword = "testuserpwd";
            String accessToken = "accesstokenstringabcdef";

            Map<String, Object> map = mockClientUtil.postToEndpoint(url, params, baUsername, baPassword, accessToken, sslSocketFactory, false, authMethod, false);
            fail("Expected an IOException when executing the post, but no exception was thrown. Got map: " + map);
        } catch (IOException e) {
            String message = e.getMessage();
            assertTrue("Ought to get 'The OpenID Connect client is unable to contact the OpenID Connect provider.' message, but get:" + message,
                    message.indexOf("The OpenID Connect client is unable to contact the OpenID Connect provider.") >= 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPostToEndPoint_executeThrowsExceptionWithNullMessage() {
        final String methodName = "testPostToEndPoint_executeThrowsException";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(httpClient).execute(with(any(HttpPost.class)));
                    will(throwException(ioException));
                    allowing(ioException).fillInStackTrace();
                    will(returnValue(stackTraceThrowable));
                    //one(ioException).getMessage();
                    //will(returnValue(null));
                    one(ioException).getMessage();
                    will(returnValue("Http response failure"));
                }
            });
            mockOidcClientHttpUtil mockClientUtil = new mockOidcClientHttpUtil();
            assertNotNull("Expected to get an instance of OidcClientHttpUtil but did not get one.", mockClientUtil);
            String url = "http://unknownclient/url1";
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            String baUsername = "testuser";
            String baPassword = "testuserpwd";
            String accessToken = "accesstokenstringabcdef";

            Map<String, Object> map = mockClientUtil.postToEndpoint(url, params, baUsername, baPassword, accessToken, sslSocketFactory, false, authMethod, false);
            fail("Expected an IOException when executing the POST, but no exception was thrown. Got map: " + map);
        } catch (IOException e) {
            String message = e.getMessage();
            assertTrue("Ought to get 'Http response failure' message, but did not.", message.indexOf("Http response failure") >= 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPostToEndPoint_badResponseNullStatus() {
        final String methodName = "testPostToEndPoint_badResponseNullStatus";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(httpClient).execute(with(any(HttpPost.class)));
                    will(returnValue(httpResponse));
                    allowing(httpResponse).getStatusLine();
                    will(returnValue(null));
                    allowing(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                    one(httpEntity).getContent();
                    will(returnValue(inputStreamBadResponse));
                    allowing(httpEntity).getContentLength();
                    will(returnValue(-1L));
                    allowing(httpEntity).getContentType();
                    will(returnValue((Header) null));
                }
            });
            mockOidcClientHttpUtil mockClientUtil = new mockOidcClientHttpUtil();
            assertNotNull("Expected to get an instance of OidcClientHttpUtil but did not get one.", mockClientUtil);
            String url = "http://unknownclient/url1";
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            String baUsername = "testuser";
            String baPassword = "testuserpwd";
            String accessToken = "accesstokenstringabcdef";

            Map<String, Object> map = mockClientUtil.postToEndpoint(url, params, baUsername, baPassword, accessToken, sslSocketFactory, false, authMethod, false);
            fail("Expected to receive a response from the endpoint resulting in an IOException, but no exception was thrown. Got map: " + map);
        } catch (IOException e) {
            String message = e.getMessage();
            assertTrue("Ought to get 'unknownclient' message, but got:" + message, message.indexOf("unknownclient") >= 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPostToEndPoint_badResponseNonOkStatus() {
        final String methodName = "testPostToEndPoint_badResponseNonOkStatus";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(httpClient).execute(with(any(HttpPost.class)));
                    will(returnValue(httpResponse));
                    allowing(httpResponse).getStatusLine();
                    will(returnValue(statusLine));
                    allowing(statusLine).getStatusCode();
                    will(returnValue(404));
                    allowing(httpResponse).getEntity();
                    will(returnValue(httpEntity));
                    one(httpEntity).getContent();
                    will(returnValue(inputStreamBadResponse));
                    allowing(httpEntity).getContentLength();
                    will(returnValue(-1L));
                    allowing(httpEntity).getContentType();
                    will(returnValue((Header) null));
                }
            });
            mockOidcClientHttpUtil mockClientUtil = new mockOidcClientHttpUtil();
            assertNotNull("Expected to get an instance of OidcClientHttpUtil but did not get one.", mockClientUtil);
            String url = "http://unknownclient.ibm.com:8010/oidcserver/postopendpoint";
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            String baUsername = "testuser";
            String baPassword = "testuserpwd";
            String accessToken = "accesstokenstringabcdef";

            Map<String, Object> map = mockClientUtil.postToEndpoint(url, params, baUsername, baPassword, accessToken, sslSocketFactory, false, authMethod, false);
            fail("Expected to receive an error response from the endpoint resulting in an IOException, but no exception was thrown. Got map: " + map);
        } catch (IOException e) {
            String message = e.getMessage();
            assertTrue("Ought to get 'unknownclient.ibm.com' message, but get:" + message, message.indexOf("unknownclient.ibm.com") >= 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetTokenEndPointPort_Normal() {
        int port = 8011;
        String url = "https://hostname:8011/oidc/endpoint";
        assertEquals(port, OidcClientHttpUtil.getTokenEndPointPort(url));
    }

    @Test
    public void testGetTokenEndPointPort_NormalNoPortSSL() {
        int port = 443;
        String url = "https://hostname/oidc/endpoint";
        assertEquals(port, OidcClientHttpUtil.getTokenEndPointPort(url));
    }

    @Test
    public void testGetTokenEndPointPort_NormalNoPortTCP() {
        int port = 80;
        String url = "http://hostname/oidc/endpoint";
        assertEquals(port, OidcClientHttpUtil.getTokenEndPointPort(url));
    }

    @Test
    public void testGetFromEndpoint() {
        final String methodName = "testGetFromEndpoint";
        try {
            String url = "http://localhost:8010/oidc/someEndPoint";
            String baUsername = "baUsername";
            String baPassword = "baPassword";
            String accessToken = "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw";
            List<NameValuePair> params = new ArrayList<NameValuePair>();

            mock.checking(new Expectations() {
                {
                    one(httpClient).execute(with(any(HttpUriRequest.class)));
                    will(returnValue(httpResponse));
                }
            });

            Map<String, Object> result = moichu.getFromEndpoint(url, params, baUsername, baPassword, accessToken, sslSocketFactory, false, false);

            HttpResponse responseCode = (HttpResponse) result.get(HttpConstants.RESPONSEMAP_CODE);
            assertNotNull("Expect to see valid response code", responseCode);

            HttpGet getMethod = new HttpGet(url);
            HttpGet getMethod2 = (HttpGet) result.get(HttpConstants.RESPONSEMAP_METHOD);
            assertEquals("HttpGet method ", getMethod.getMethod(), getMethod2.getMethod());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    class MockInputStream extends InputStream {
        String strOut = null;
        int iCnt = 0;

        public MockInputStream(String strOut) {
            this.strOut = strOut;
        }

        @Override
        public int read() {
            if (iCnt < strOut.length()) {
                return strOut.charAt(iCnt++);
            } else {
                return -1;
            }
        }
    }

    class mockSSLContext extends SSLContext {
        protected mockSSLContext(SSLContextSpi contextSpi, Provider provider, String protocol) {
            super(contextSpi, provider, protocol);
        };
    }

    abstract class mockSSLContextSpi extends SSLContextSpi {
        @Override
        protected javax.net.ssl.SSLSocketFactory engineGetSocketFactory() throws IllegalStateException {
            return javaxSslSocketFactory;
        }
    }

    class mockOidcClientHttpUtil extends OidcClientHttpUtil {
        public mockOidcClientHttpUtil() {
            instance = this;
        }

        @Override
        public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean b, boolean useJvmProps) {
            //return new mockDefaultHttpClient();
            return httpClient;
        }

        @Override
        public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean b, String baUser, String baPassword, boolean useJvmProps) {
            //return new mockDefaultHttpClient();
            return httpClient;
        }

        @Override
        public HttpClient createHttpClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useSystemPropertiesForHttpClientConnections, BasicCredentialsProvider credentialsProvider) {
            return httpClient;
        }
    }

};
