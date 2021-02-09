/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.gson.JsonObject;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.simplicity.config.HttpSession;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.openidconnect.clients.common.AuthorizationCodeHandler;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.HashUtils;
import com.ibm.ws.security.openidconnect.clients.common.MockOidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.token.IDToken;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.ws.security.openidconnect.token.impl.IdTokenImpl;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

public class OidcClientAuthenticatorTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    //private static final String TEST_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMSIsImlhdCI6MTM4OTk3NjkzMCwic3ViIjoidGVzdHVzZXIiLCJleHAiOjEzODk5ODA1MzAsImF1ZCI6ImNsaWVudDAxIn0.79VNXveAqipBzK5wB-cWTCJN7gXzECgj60w5K4GfZe4";
    // need one signed with new secret for jose4j
    private static final String TEST_TOKEN_STRING = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMSIsImlhdCI6MTM4OTk3NjkzMCwic3ViIjoidGVzdHVzZXIiLCJleHAiOjEzODk5ODA1MzAsImF1ZCI6ImNsaWVudDAxIn0.vPZ0Hg5HY7-lDhBZTOkuVBE5GrV5uSphdyi1AZgbEbI";
    private static final String TEST_ACCESS_TOKEN = "w3sYFV4Xrp4JzzfYdLKuQM0aVIVY6na8qzcGmEdg";
    private static final String TEST_COOKIE_VALUE = "rO0ABXNyABNqYXZhLnV0aWwuSGFzaHRhYmxlE7sPJSFK5LgDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNob2xkeHA/QAAAAAAACHcIAAAACwAAAAJ0AARjb2RldAAeS1FISUhFOEZVWkFZWWV2THBCWTdVbnFZVE8zbEZ3dAAFc3RhdGV0ABQzNFcybmIwTFlUREw5RnJ5dmgzWHg=";
    private static final long TEST_CLOCK_SKEW_IN_SECONDS = 300L;
    private static final String TEST_STATE_KEY = "stateKey6543210";
    private static final String TEST_ORIGINAL_STATE = "originalState6547";
    private static final String ANOTHER_ORIGINAL_STATE = "34W2nb0LYTDL9Fryvh3X";
    private static final String TEST_URL = "http://harmonic.austin.ibm.com:8010/formlogin/SimpleServlet";
    private static final String TEST_REDIRECT_URL = "https://my.rp.client.com:8010/redirect/client";
    private static final String TEST_AUTHORIZATION_ENDPOINT = "https://op.ibm.com:8020/oidc/endpoint";
    private static final String TEST_AUTHORIZATION_CODE = "KQHIHE8FUZAYYevLpBY7UnqYTO3lFw";
    private static final String TEST_TOKEN_ENDPOINT = "http://harmonic:8011/oidc/endpoint/OidcConfigSample/token";
    private static final String TEST_JWK_ENDPOINT = "http://acme:8011/oidc/endpoint/OidcConfigSample/jwk";
    private static final String TEST_GRANT_TYPE = "openid profile";
    private static final String CLIENTID = "clientid";
    private static final String CLIENT01 = "client01";
    // private static final String SHARED_KEY = "secret";  // conversion from net.oauth to jose4j requires a longer key
    private static final String SHARED_KEY = "secretsecretsecretsecretsecretsecret";
    private static final String TEST_ACR_VALUES = "urn:mace:incommon:iap:silver urn:mace:incommon:iap:bronze";
    private static final String authMethod = "basic";
    private static final String AUTHZ_CODE = "authorizaCodeAAA";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static final String SIGNATURE_ALG_NONE = "none";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_GET = "GET";
    private static final String PARAMETER_OIDC_CLIENT = "oidc_client";

    protected final OidcClientConfig clientConfig = mock.mock(OidcClientConfig.class, "clientConfig");
    @SuppressWarnings("unchecked")
    protected final AtomicServiceReference<SSLSupport> sslSupportRef = mock.mock(AtomicServiceReference.class, "sslSupportRef");
    protected final SSLSupport sslSupport = mock.mock(SSLSupport.class, "sslSupport");
    protected final JSSEHelper jsseHelper = mock.mock(JSSEHelper.class, "jsseHelper");
    protected final IExtendedRequest req = mock.mock(IExtendedRequest.class, "req");
    protected final IExtendedRequest req2 = mock.mock(IExtendedRequest.class, "req2");
    protected final HttpSession session = mock.mock(HttpSession.class, "session");
    protected final HttpServletResponse res = mock.mock(HttpServletResponse.class, "res");
    protected final ReferrerURLCookieHandler referrerURLCookieHandler = mock.mock(ReferrerURLCookieHandler.class, "referrerURLCookieHandler");
    protected final Cookie cookie1 = mock.mock(Cookie.class, "cookie1");
    protected final Cookie cookie2 = mock.mock(Cookie.class, "cookie2");
    protected final OidcClientUtil oidcClientUtil = mock.mock(OidcClientUtil.class, "oidcClientUtil");
    protected final IDToken idToken = mock.mock(IDToken.class, "idToken");
    protected final Payload payload = mock.mock(Payload.class, "payload");
    protected final SSLContext sslContext = mock.mock(SSLContext.class, "sslContext");
    protected final SSLSocketFactory sslSocketFactory = mock.mock(SSLSocketFactory.class, "sslSocketFactory");
    protected final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    protected final PublicKey publicKey = mock.mock(PublicKey.class, "publicKey");
    protected final PrintWriter pw = mock.mock(PrintWriter.class, "pw");
    protected final MockOidcClientRequest oidcClientRequest = mock.mock(MockOidcClientRequest.class, "oidcClientRequest");
    protected final OidcClientRequest convClientRequest = mock.mock(OidcClientRequest.class, "convClientRequest");
    protected final OidcClientConfig oidcClientConfig = mock.mock(OidcClientConfig.class, "oidcClientConfig");
    protected final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class, "convClientConfig");

    protected final JWSHeader jwsHeader = new JWSHeader();
    final String STATE = "state";

    protected final HashMap<String, String> tokens = new HashMap<String, String>(100);
    protected OidcClientAuthenticator commonAuthn;
    protected OIDCClientAuthenticatorUtil OidcCAUtil = new OIDCClientAuthenticatorUtil();

    private final Cookie cookie = new Cookie(ClientConstants.WAS_OIDC_CODE, STATE);
    private final Cookie[] cookies = new Cookie[] { cookie };

    String idTokenContent = "aaaaa.bbbb.ccccc";
    String id_token = "id_token";
    String[] idTokens = new String[] { idTokenContent };
    Cookie reqParameterCookie = null;
    final String clientSecret = "secret";
    String encodedReqParams = null;

    @Before
    public void setUp() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(webAppSecConfig).getSSODomainList();//
                will(returnValue(null)); //
                allowing(webAppSecConfig).getSSOUseDomainFromURL();//
                will(returnValue(false));
                allowing(webAppSecConfig).createSSOCookieHelper();
                will(returnValue(new SSOCookieHelperImpl(webAppSecConfig)));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                allowing(req).getAttribute("com.ibm.wsspi.security.oidc.client.request");
                will(returnValue(oidcClientRequest));

                allowing(res).addCookie(cookie2);

                allowing(oidcClientRequest).getAndSetCustomCacheKeyValue();
                will(returnValue("ThisIsCustomCacheKeyvalue"));

                allowing(sslSupportRef).getService();
                will(returnValue(sslSupport));

                allowing(referrerURLCookieHandler).createCookie(with(any(String.class)), with(any(String.class)), with(any(HttpServletRequest.class)));
                will(returnValue(cookie2));

            }
        });

        commonAuthn = new OidcClientAuthenticator();
        tokens.clear();
    }

    @After
    public void tearDown() {
        //mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        try {
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", commonAuthn);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testConstructorOicc() {
        try {
            createConstructorExpectations(clientConfig);
            mock.checking(new Expectations() {
                {
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(jsseHelper));
                }
            });
            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);
            JSSEHelper jHelper = oica.getJSSEHelper();
            assertEquals("Did not get the JSSEHelper we expected", jHelper, jsseHelper);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    void createReqCookie() {
        // set up request parameter cookie
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("access_token", new String[] { "access_token_content" });
        map.put(id_token, idTokens);
        map.put("refresh_token", new String[] { "refresh_token_content" });
        JsonObject jsonObject = new JsonObject();
        Set<Map.Entry<String, String[]>> entries = map.entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            String[] strs = entry.getValue();
            if (strs != null && strs.length > 0) {
                jsonObject.addProperty(key, strs[0]);
            }
        }
        String requestParameters = jsonObject.toString();

        // digest with the client_secret value
        String digestValue = HashUtils.digest(requestParameters + clientSecret);
        String hashReqParams = requestParameters + digestValue;

        try {
            encodedReqParams = Base64Coder.toString(Base64Coder.base64Encode(hashReqParams.getBytes(ClientConstants.CHARSET)));
        } catch (UnsupportedEncodingException e) {
            //This should not happen, we are using UTF-8
        }
        reqParameterCookie = new Cookie(ClientConstants.WAS_OIDC_CODE, encodedReqParams);
    }

    //@Test  - we have this test in OL, so commenting out here.
    public void testValidateReqParameters() {

        createReqCookie();
        mock.checking(new Expectations() {
            {
                one(convClientConfig).getClientSecret();
                will(returnValue(clientSecret));
            }
        });

        Hashtable<String, String> reqParameters = new Hashtable<String, String>();
        boolean validParameter = OidcCAUtil.validateReqParameters(convClientConfig, reqParameters, encodedReqParams);
        assertTrue("the request parameter is supposed to be correct but not", validParameter);
        String newIdTokenContent = reqParameters.get(id_token);
        assertTrue("the value of id_token is not " + idTokenContent + " but " + newIdTokenContent, idTokenContent.equals(newIdTokenContent));
        String newAccessTokenContent = reqParameters.get("access_token");
        assertTrue("The access token content is not \"access_token_content\"", "access_token_content".equals(newAccessTokenContent));
        String newRefreshTokenContent = reqParameters.get("refresh_token");
        assertTrue("The access token content is not \"refresh_token_content\"", "refresh_token_content".equals(newRefreshTokenContent));
    }

    // getCookieValueAsBytes  name --> WASOidcCode value --> rO0ABXNyABNqYXZhLnV0aWwuSGFzaHRhYmxlE7sPJSFK5LgDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNob2xkeHA/QAAAAAAACHcIAAAACwAAAAJ0AARjb2RldAAeS1FISUhFOEZVWkFZWWV2THBCWTdVbnFZVE8zbEZ3dAAFc3RhdGV0ABQzNFcybmIwTFlUREw5RnJ5dmgzWHg=
    // OidcClientAuthenticator < getAuthzCodeAndStateFromCookie Exit
    // {code=KQHIHE8FUZAYYevLpBY7UnqYTO3lFw, state=34W2nb0LYTDL9Fryvh3X}
    // TODO: Delete since it's covered by testAuthenticate_authorizationCode* tests
    @Test
    public void testGetAuthzCodeAndStateFromCookie() {
        try {
            createReqCookieExpectation(TEST_COOKIE_VALUE.getBytes());

            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
            createReferrerUrlCookieExpectations(ClientConstants.WAS_OIDC_CODE);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", commonAuthn);
            Hashtable<String, String> hashTable = commonAuthn.getAuthzCodeAndStateFromCookie(req, res);
            assertEquals("The hashTable ought to have 2 entries but get " + hashTable.size(), 2, hashTable.size());
            String code = hashTable.get("code");
            assertEquals("Expect get code as: KQHIHE8FUZAYYevLpBY7UnqYTO3lFw but get '" + code + "'",
                    TEST_AUTHORIZATION_CODE, code);
            String state = hashTable.get("state");
            assertEquals("Expect get state as: 34W2nb0LYTDL9Fryvh3X but get '" + state + "'",
                    ANOTHER_ORIGINAL_STATE, state);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO: Delete since it's covered by testAuthenticate_redirectToServer* tests
    @Test
    public void testGetAuthzCodeAndStateFromCookieNull() {
        try {
            createReqCookieExpectation((byte[]) null);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", commonAuthn);
            Hashtable<String, String> hashTable = commonAuthn.getAuthzCodeAndStateFromCookie(req, res);

            assertNull("The hashTable ought to be null but not ", hashTable);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetReqUrlNull() {
        try {
            createReqUrlExpectations(null);
            String strUrl = commonAuthn.getReqURL(req);

            assertEquals("The URL must not contain a query string.", TEST_URL, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetReqUrlQuery() {
        try {
            final String query = "response_type=code";
            createReqUrlExpectations(query);
            String strUrl = commonAuthn.getReqURL(req);
            String expect = TEST_URL + "?" + query;

            assertEquals("The URL must contain the query string.", expect, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetReqUrlQuery_encoded() {
        try {
            String value = "code>\"><script>alert(100)</script>";
            final String query = "response_type=" + value;
            createReqUrlExpectations(query);
            String strUrl = commonAuthn.getReqURL(req);
            String expect = TEST_URL + "?response_type=" + URLEncoder.encode(value, "UTF-8");

            assertEquals("The URL must contain the query string.", expect, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier() {

        try {
            final String issuer = "https://localhost:8011/oidc/endpoint/OidcConfigSample";
            mock.checking(new Expectations() {
                {

                    one(clientConfig).getIssuerIdentifier();
                    will(returnValue(issuer));

                }
            });
            String issuerResult = commonAuthn.getIssuerIdentifier(clientConfig);

            assertEquals("Issuer result is not the one expected!", issuer, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifierWithNullIssuer() {
        try {

            final String tokenEndpoint = "http://localhost:8011/oidc/endpoint/OidcConfigSample/token";
            final String issuer = null;
            final String expected = "http://localhost:8011/oidc/endpoint/OidcConfigSample";

            mock.checking(new Expectations() {
                {
                    one(clientConfig).getIssuerIdentifier();
                    will(returnValue(issuer));
                    one(clientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenEndpoint));

                }
            });
            String issuerResult = commonAuthn.getIssuerIdentifier(clientConfig);
            assertEquals("Issuer result is not the one expected!", expected, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleRedirectToServerHttpBad() {

        try {
            final String authorizationEndpoint = "http://op.ibm.com:8010/oidc/endpoint";
            createHttpsRequirementExpectationsForAuthorizationEndpoint(authorizationEndpoint);
            ProviderAuthenticationResult result = commonAuthn.handleRedirectToServer(req, res, clientConfig);

            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //0509 @Test
    public void testAuthenticate_redirectToServer() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(clientConfig).getGrantType();
                    will(returnValue("authorization_code"));
                }
            });
            createCommonAuthenticateExpectations(null);
            createHandleRedirectToServerExpectations(TEST_AUTHORIZATION_ENDPOINT, null);

            mock.checking(new Expectations() {
                {
                    allowing(clientConfig).getPrompt();
                    will(returnValue(null));
                }
            });

            ProviderAuthenticationResult result = commonAuthn.authenticate(req, res, clientConfig);

            assertRedirectResult(result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createGetAuthzCodeAndSateFromCookieExpectations() {
        createReqCookieExpectation(TEST_COOKIE_VALUE.getBytes());
        createReferrerUrlCookieExpectations(ClientConstants.WAS_OIDC_CODE);
    }

    private void createHandleTokensExpectations(String cookieState, String currentState, boolean isStateMismatch, final String idToken, final String accessToken) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
            }
        });
        createVerifyResponseStateExpectations(cookieState, currentState);

        if (isStateMismatch) {
            // Runtime errors out and returns 401
            createPostParameterHelperExpectations();
            createReferrerUrlCookiesExpectations();
        } else {
            final Enumeration<String> enumParams = Collections.enumeration(Arrays.asList(Constants.ID_TOKEN, Constants.ACCESS_TOKEN, Constants.REFRESH_TOKEN));
            mock.checking(new Expectations() {
                {
                    one(req).getParameterNames();
                    will(returnValue(enumParams));
                    one(req).getParameterValues(Constants.ID_TOKEN);
                    will(returnValue(new String[] { idToken }));
                    one(req).getParameterValues(Constants.ACCESS_TOKEN);
                    will(returnValue(new String[] { accessToken }));
                    one(req).getParameterValues(Constants.REFRESH_TOKEN);
                    will(returnValue(new String[0]));
                }
            });
            createCreateResultExpectations(idToken);
        }
    }

    private void createVerifyResponseStateExpectations(final String cookieState, final String currentState) {
        final Cookie[] cookies = new Cookie[] { cookie1 };
        mock.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                one(cookie1).getName();
                will(returnValue(ClientConstants.WAS_OIDC_STATE_KEY + cookieState.hashCode()));
                one(cookie1).getValue();
                will(returnValue(currentState));
            }
        });
        createReferrerUrlCookieExpectations(ClientConstants.WAS_OIDC_STATE_KEY + cookieState.hashCode());
    }

    private void createCreateResultExpectations(String idToken) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
            }
        });
        if (idToken == null) {
            // Runtime errors out and returns 401
            mock.checking(new Expectations() {
                {
                    one(clientConfig).getTokenEndpointUrl();
                    will(returnValue(TEST_TOKEN_ENDPOINT));
                }
            });
        } else {
            try {
                createResultExpectations();
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unexpected exception creating the result expectations: " + e);
            }
        }
        createPostParameterHelperExpectations();
        createReferrerUrlCookiesExpectations();
    }

    //0509 @Test
    public void testAuthenticate_implicit() {
        try {
            WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
            mock.checking(new Expectations() {
                {
                    one(clientConfig).getGrantType();
                    will(returnValue(Constants.IMPLICIT));
                }
            });
            createGetAuthzCodeAndSateFromCookieExpectations();
            createHandleTokensExpectations(ANOTHER_ORIGINAL_STATE, ANOTHER_ORIGINAL_STATE, false, TEST_TOKEN_STRING, TEST_ACCESS_TOKEN);

            final String myStateKey = ANOTHER_ORIGINAL_STATE;
            final String originalState = ANOTHER_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            final OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            //mock.checking(new Expectations() {
            //   {
            //      one(oica.getAttributeToSubject(clientConfig, payload, "tokenStr"));
            //      will(returnValue(attributeToSubject));
            //  }
            //});

            //oica.requestStates = requestStates;
            oica.oidcClientUtil = oidcClientUtil;

            ProviderAuthenticationResult result = oica.authenticate(req, res, clientConfig); //
            assertEquals("The authentication result status must be SUCCESS.", AuthResult.SUCCESS, result.getStatus());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //0509 @Test
    public void testAuthenticate_implicit_stateMismatch() {
        try {
            WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
            mock.checking(new Expectations() {
                {
                    one(clientConfig).getGrantType();
                    will(returnValue(Constants.IMPLICIT));
                }
            });
            createGetAuthzCodeAndSateFromCookieExpectations();
            createHandleTokensExpectations(ANOTHER_ORIGINAL_STATE, ANOTHER_ORIGINAL_STATE, true, TEST_TOKEN_STRING, TEST_ACCESS_TOKEN);

            final String myStateKey = ANOTHER_ORIGINAL_STATE;
            final String originalState = TEST_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            //oica.requestStates = requestStates;
            oica.oidcClientUtil = oidcClientUtil;

            ProviderAuthenticationResult result = oica.authenticate(req, res, clientConfig); //
            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createCommonAuthenticateExpectations(final byte[] oidcCodeCookie) {
        createReqCookieExpectation(oidcCodeCookie);
    }

    private void createReqCookieExpectation(final byte[] oidcCodeCookie) {
        mock.checking(new Expectations() {
            {
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("http://foo.com/something")));
                one(req).getCookieValueAsBytes(ClientConstants.WAS_OIDC_CODE);
                will(returnValue(oidcCodeCookie));
            }
        });
    }

    private void createHandleRedirectToServerExpectations(String authorizationEndpoint, final String queryString) {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(authorizationEndpoint);
        createClientRedirectRequirementExpectations(true);
        createSessionExpectations();
        createReferrerCookiesExpectations();
        createRedirectUrlExpectations();
        createAuthorizationEndpointUrlExpectations();
        createOptionalExpectations();
    }

    private void createSessionExpectations() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).createSession();
                will(returnValue(true));
                one(req).getSession(true);
                will(returnValue(session));
            }
        });
    }

    /**
     *
     */
    private void createOptionalExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getAuthContextClassReference();
                will(returnValue(TEST_ACR_VALUES));
            }
        });

    }

    private void createHttpsRequirementExpectationsForAuthorizationEndpoint(final String authorizationEndpoint) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getAuthorizationEndpointUrl();
                will(returnValue(authorizationEndpoint));
                allowing(req).setAttribute(with(any(String.class)), with(any(OidcClientRequest.class)));
            }
        });
        createHttpsRequirementExpectations(true);
    }

    private void createReqUrlExpectations(final String queryString) {
        mock.checking(new Expectations() {
            {
                allowing(req).getScheme();
                will(returnValue("https"));
                one(req).getServerPort();
                will(returnValue(8020));
                one(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL)));
                one(req).getQueryString();
                will(returnValue(queryString));
            }
        });
    }

    private void createReferrerCookiesExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
            }
        });
        try {
            final long exp = 0;
            mock.checking(new Expectations() {
                {
                    one(res).setStatus(200);
                    one(res).getWriter();
                    will(returnValue(pw));
                    allowing(pw).println(with(any(String.class)));
                    allowing(res).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(res).setDateHeader("Expires", exp);
                    allowing(res).setContentType(with(any(String.class)));
                    allowing(pw).close();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createRedirectUrlExpectations() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_REDIRECT_URL));
            }
        });
        createHttpsRequirementExpectations(true);
    }

    private void createHttpsRequirementExpectations(final boolean httpsRequired) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).isHttpsRequired();
                will(returnValue(httpsRequired));
            }
        });
    }

    private void createClientRedirectRequirementExpectations(final boolean clientSideRedirect) {
        mock.checking(new Expectations() {
            {
                one(clientConfig).isClientSideRedirect();
                will(returnValue(clientSideRedirect));
            }
        });
    }

    private void createAuthorizationEndpointUrlExpectations() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
                allowing(clientConfig).getScope();
                will(returnValue(TEST_GRANT_TYPE));
                allowing(clientConfig).isNonceEnabled();
                will(returnValue(false));
                one(clientConfig).getAuthorizationEndpointUrl(); // TODO: Refactor code to remove duplicate calls
                will(returnValue(TEST_AUTHORIZATION_ENDPOINT));
            }
        });
    }

    private void assertRedirectResult(ProviderAuthenticationResult result) {
        assertEquals("The result status must be REDIRECT_TO_PROVIDER.", AuthResult.REDIRECT_TO_PROVIDER, result.getStatus());
        String redirectUrl = result.getRedirectUrl();
        assertTrue("The redirect URL must contain the authorization endpoint " + TEST_AUTHORIZATION_ENDPOINT + ".", redirectUrl.indexOf(TEST_AUTHORIZATION_ENDPOINT) >= 0);
        assertTrue("The redirect URL must contain 'response_type=code' in the query string.", redirectUrl.indexOf("response_type=code") >= 0);
        assertTrue("The redirect URL must contain 'client_id=client01' in the query string.", redirectUrl.indexOf("client_id=client01") >= 0);
    }

    //0509 @Test
    //    public void testAuthenticate_authorizationCode() {
    //        try {
    //            createConstructorExpectations(clientConfig);
    //            final String cookieValue = TEST_COOKIE_VALUE;
    //            createCommonAuthenticateExpectations(cookieValue.getBytes());
    //            createHandleAuthorizationCodeExpectations(TEST_STATE_KEY);
    //
    //            final String myStateKey = TEST_STATE_KEY;
    //            final String originalState = ANOTHER_ORIGINAL_STATE;
    //            Cache requestStates = new Cache(0, 0);// LinkedHashMap<String, Object>(10000);
    //            requestStates.put(myStateKey, originalState);
    //
    //            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
    //            //oica.requestStates = requestStates;
    //            oica.oidcClientUtil = oidcClientUtil;
    //
    //            ProviderAuthenticationResult result = oica.authenticate(req, res, clientConfig); //
    //            assertEquals("The authentication result status must be SUCCESS.", AuthResult.SUCCESS, result.getStatus());
    //        } catch (Throwable t) {
    //            outputMgr.failWithThrowable(testName.getMethodName(), t);
    //        }
    //    }

    private void createConstructorExpectations(final OidcClientConfig clientConfig) {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        createJwkRetrieverConstructorExpectations(clientConfig);
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getId();
                will(returnValue(CLIENTID));
            }
        });
    }

    private void createJwkRetrieverConstructorExpectations(final OidcClientConfig clientConfig) {

        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getSslRef();
                will(returnValue("sslRef"));
                allowing(clientConfig).getJwkEndpointUrl();
                will(returnValue(TEST_JWK_ENDPOINT));
                allowing(clientConfig).getJwkSet();
                will(returnValue(null));
                allowing(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(false));
                allowing(clientConfig).getJwkClientId();
                will(returnValue(null));
                allowing(clientConfig).getJwkClientSecret();
                will(returnValue(null));
            }
        });
    }

    private void createResponseStateExpectations(final String stateKey) {
        final Cookie[] cookies = new Cookie[] { cookie1 };
        final String cookieName = ClientConstants.WAS_OIDC_STATE_KEY + ANOTHER_ORIGINAL_STATE.hashCode();
        mock.checking(new Expectations() {
            {
                one(req).getCookies();
                will(returnValue(cookies));
                one(cookie1).getName();
                will(returnValue(cookieName));
                one(cookie1).getValue();
                will(returnValue(stateKey));
            }
        });
        createReferrerUrlCookieExpectations(cookieName);
    }

    private void createHttpsRequirementExpectationsForTokenEndpoint() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_TOKEN_ENDPOINT));
                allowing(clientConfig).isDisableLtpaCookie();
                will(returnValue(false));
            }
        });
        createHttpsRequirementExpectations(false);
    }

    private void createSSLContextExpectations(final String sslConfigurationName, final SSLContext sslContext) throws SSLException {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_TOKEN_ENDPOINT));
                allowing(clientConfig).getSSLConfigurationName();
                will(returnValue(sslConfigurationName));
                one(sslSupport).getJSSEHelper();
                will(returnValue(jsseHelper));
                one(jsseHelper).getSSLContext(sslConfigurationName, null, null, true);
                will(returnValue(sslContext));
            }
        });
    }

    private void createResultExpectations() throws IDTokenValidationFailedException {
        // IDToken creation and properties
        mock.checking(new Expectations() {
            {
                one(clientConfig).getIssuerIdentifier();
                will(returnValue(""));
                one(clientConfig).getTokenEndpointUrl(); // TODO: Refactor code so that this is only called once.
                will(returnValue(TEST_TOKEN_ENDPOINT));
                allowing(clientConfig).getSignatureAlgorithm();
                will(returnValue("HS256"));
                one(clientConfig).getSharedKey();
                will(returnValue(SHARED_KEY));
                allowing(clientConfig).getClientId(); // TODO: Refactor code so that this is only called once.
                will(returnValue(CLIENT01));
                one(oidcClientUtil).createIDToken(with(any(String.class)),
                        with(any(Object.class)),
                        with(any(String.class)),
                        with(any(String.class)),
                        with(any(String.class)),
                        with(any(String.class)));
                will(returnValue(idToken));
                allowing(idToken).getHeader();
                will(returnValue(jwsHeader));
            }
        });
        createClientConfigExpectationsForIDToken();
        mock.checking(new Expectations() {
            {
                one(idToken).verify(with(any(Long.class)), with(any(Object.class)));
                will(returnValue(true));
                allowing(idToken).getPayload();
                will(returnValue(payload));
                one(payload).setJwtId(CLIENT01);
                will(returnValue(payload));
                one(payload).get(ClientConstants.SUB);
                will(returnValue("testuser"));
                one(idToken).addToIdTokenImpl(with(any(IdTokenImpl.class)));
                one(clientConfig).isIncludeCustomCacheKeyInSubject();
                will(returnValue(true));
                one(clientConfig).isIncludeIdTokenInSubject();
                will(returnValue(true));
            }
        });
        createClientConfigExpectationsForResult();
    }

    /**
     *
     */
    private void createClientConfigExpectationsForResultBeforeMapping() {
        // TODO Auto-generated method stub
        //AttributeToSubject
        //mock.checking(new Expectations() {
        //    {
        //        one(attributeToSubject).checkUserNameForNull();
        //        will(returnValue(false));
        //    }
        //});
        // Identity Assertion

        mock.checking(new Expectations() {
            {
                allowing(clientConfig).isMapIdentityToRegistryUser();
                will(returnValue(true));
            }
        });

    }

    private void createPostParameterHelperExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(req).getMethod();
                will(returnValue("POST"));
            }
        });
        createOtherParameterHelperExpectations();
    }

    private void createOtherParameterHelperExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(req).getRequestURI();
                will(returnValue(TEST_URL));

                allowing(webAppSecConfig).getPostParamSaveMethod();
                will(returnValue("NoneExist"));
            }
        });
    }

    private void createReferrerUrlCookiesExpectations() {
        mock.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).invalidateReferrerURLCookies(req, res,
                        OIDCClientAuthenticatorUtil.OIDC_COOKIES);
            }
        });
    }

    private void createReferrerUrlCookieExpectations(final String cookieName) {
        mock.checking(new Expectations() {
            {
                allowing(req).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL)));
                allowing(cookie2).setMaxAge(0);
                allowing(cookie2).setMaxAge(-1);
                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res,
                        cookieName);
            }
        });
    }

    //0509 @Test
    //    public void testAuthenticate_authorizationCode_tokenRequestFailureWithIOException() {
    //        try {
    //            createConstructorExpectations(clientConfig);
    //            createCommonAuthenticateExpectations(TEST_COOKIE_VALUE.getBytes());
    //            createHandleAuthorizationCodeExpectationsWithException(TEST_STATE_KEY, new IOException());
    //
    //            final String myStateKey = TEST_STATE_KEY;
    //            final String originalState = ANOTHER_ORIGINAL_STATE;
    //            Cache requestStates = new Cache(0, 0);
    //            requestStates.put(myStateKey, originalState);
    //            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
    //
    //            oica.oidcClientUtil = oidcClientUtil;
    //
    //            ProviderAuthenticationResult result = oica.authenticate(req, res, clientConfig); //
    //            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());
    //        } catch (Throwable t) {
    //            outputMgr.failWithThrowable(testName.getMethodName(), t);
    //        }
    //    }

    //0509 @Test
    //    public void testAuthenticate_authorizationCode_tokenRequestFailureWithHttpException() {
    //        try {
    //            createConstructorExpectations(clientConfig);
    //            createCommonAuthenticateExpectations(TEST_COOKIE_VALUE.getBytes());
    //            createHandleAuthorizationCodeExpectationsWithException(TEST_STATE_KEY, new HttpException());
    //
    //            final String myStateKey = TEST_STATE_KEY;
    //            final String originalState = ANOTHER_ORIGINAL_STATE;
    //            Cache requestStates = new Cache(0, 0);
    //            requestStates.put(myStateKey, originalState);
    //
    //            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
    //
    //            oica.oidcClientUtil = oidcClientUtil;
    //
    //            ProviderAuthenticationResult result = oica.authenticate(req, res, clientConfig); //
    //
    //            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());
    //        } catch (Throwable t) {
    //            outputMgr.failWithThrowable(testName.getMethodName(), t);
    //        }
    //    }

    @Test
    public void testSetRedirectUrlIfNotDefined() {
        try {
            //final String redirectUri = "https://mine.ibm.com:8020/oidcclient/redirect";
            final String redirectUri = "https://mine.ibm.com:8020";
            final String contextPath = "/oidcclient";
            final String fullUrl = redirectUri + contextPath + "/redirect/" + CLIENT01;
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri);
                    will(returnValue(redirectUri));
                    allowing(convClientConfig).isSocial();
                    will(returnValue(false));
                    allowing(convClientConfig).getId();
                    will(returnValue(CLIENT01));
                    allowing(convClientConfig).getContextPath();
                    will(returnValue(contextPath));
                    one(convClientConfig).getRedirectUrlFromServerToClient();
                    will(returnValue(redirectUri));
                    one(convClientConfig).getRedirectUrlWithJunctionPath(fullUrl);
                    will(returnValue(fullUrl));
                }
            });
            assertNotNull("Get an Result which is not right", OidcCAUtil.setRedirectUrlIfNotDefined(req, convClientConfig));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testSetRedirectUrlIfNotDefined1() {
        try {
            final String clientId = CLIENT01;
            final String redirectUri = "https://mine.ibm.com:8020/oidcclient/redirect";
            final String redirectUri2 = "https://mine.ibm.com:8020/oidcclient/redirect/client01";
            final String contextPath = "/oidcclient";
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).isSocial();
                    will(returnValue(false));
                    allowing(convClientConfig).getId();
                    will(returnValue(clientId));
                    one(convClientConfig).getRedirectUrlFromServerToClient();
                    will(returnValue(null));
                    allowing(convClientConfig).getContextPath();
                    will(returnValue(contextPath));
                    allowing(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri);
                    will(returnValue(redirectUri));
                    allowing(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri2);
                    will(returnValue(redirectUri2));
                    one(req).getServerName();
                    will(returnValue("mine.ibm.com"));
                    one(req).isSecure();
                    will(returnValue(true));
                    one(req).getServerPort();
                    will(returnValue(8020));
                    one(req).getScheme();
                    will(returnValue("https"));
                }
            });
            mock.checking(new Expectations() {
                {
                    one(oidcClientUtil).getRedirectUrl(req, "/oidcclient/redirect/" + clientId);
                    will(returnValue(redirectUri));
                }
            });
            commonAuthn.oidcClientUtil = oidcClientUtil;
            String returnedUrl = OidcCAUtil.setRedirectUrlIfNotDefined(req, convClientConfig);
            assertNotNull("Get an Result which is not right", returnedUrl);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testVerifyResponseStateFailure() {
        try {
            final Cookie[] cookies = new Cookie[] {
                    cookie1
            };
            final String cookieName = ClientConstants.WAS_OIDC_STATE_KEY +
                    "notAoriginalState6547".hashCode();
            final String myStateKey = TEST_STATE_KEY;
            final String originalState = TEST_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0); //LinkedHashMap<String, Object>(10000);
            requestStates.put(myStateKey, originalState);
            mock.checking(new Expectations() {
                {
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue(myStateKey));
                    one(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClockSkewInSeconds();
                    will(returnValue(300L));
                    one(convClientConfig).getId();
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClientSecret();
                    will(returnValue("clientsecret"));
                    one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey");
                }
            });
            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
            createReferrerUrlCookieExpectations(cookieName);

            ProviderAuthenticationResult result = OidcCAUtil.verifyResponseState(req, res, "notA" + originalState, convClientConfig);
            assertNotNull("Did not get an expecyted result", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetJSSEHelperNull() {
        try {
            createConstructorExpectations(clientConfig);
            mock.checking(new Expectations() {
                {
                    one(sslSupport).getJSSEHelper();
                    will(returnValue((JSSEHelper) (null)));
                }
            });
            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);
            JSSEHelper jHelper = oica.getJSSEHelper();
            assertNull("Get an un-expected JSSEHelper", jHelper);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetJSSEHelperNull2() {
        try {
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", commonAuthn);
            JSSEHelper jHelper = commonAuthn.getJSSEHelper();
            assertNull("Get an un-expected JSSEHelper", jHelper);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetSSLContextNull() {
        try {
            createConstructorExpectations(clientConfig);
            mock.checking(new Expectations() {
                {
                    one(sslSupport).getJSSEHelper();
                    will(returnValue((JSSEHelper) (null)));
                }
            });
            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);
            SSLContext sslContext = oica.getSSLContext("http://localhost:8010/oidc/token", null, CLIENT01);
            assertNull("Get an un-expected SSLContext", sslContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetSSLContextNull2() {
        try {

            mock.checking(new Expectations() {
                {
                    createConstructorExpectations(clientConfig);
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(jsseHelper));
                    one(jsseHelper).getSSLContext(null, null, null, true);
                    will(returnValue((SSLContext) null));
                }
            });
            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);
            oica.getSSLContext("https://localhost:8010/oidc/token", null, CLIENT01);
            fail("Should threw an SSLException but not");
        } catch (SSLException t) {
            // This is the expecting Exception
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createClientConfigExpectationsForIDToken() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getClockSkewInSeconds();
                will(returnValue(TEST_CLOCK_SKEW_IN_SECONDS));
                allowing(clientConfig).isNonceEnabled();
                will(returnValue(false));
            }
        });
    }

    private void createClientConfigExpectationsForResult() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getUserIdentifier();
                will(returnValue(ClientConstants.SUB));
            }
        });
        createClientConfigExpectationsForResultBeforeMapping();
    }

    @Test
    public void testHandleAuthorizationCodeFailure() {
        try {
            final String clientId = CLIENTID;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getClientId();
                    will(returnValue(clientId));
                }
            });

            // verifyResponseState
            final Cookie[] cookies = new Cookie[] {
                    cookie1
            };
            final String cookieName = ClientConstants.WAS_OIDC_STATE_KEY +
                    TEST_ORIGINAL_STATE.hashCode();
            final String myStateKey = TEST_STATE_KEY;
            final String originalState = TEST_ORIGINAL_STATE;

            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getClientId(); //oidcClientConfig
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClockSkewInSeconds();//
                    will(returnValue(300L));
                    one(convClientConfig).getId();//
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClientSecret();//
                    will(returnValue("clientsecret"));
                    one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey"); //
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue("BadStateKey"));
                    allowing(oidcClientConfig).getSslRef();
                    will(returnValue("sslRef"));
                    allowing(oidcClientConfig).getJwkEndpointUrl();
                    will(returnValue(TEST_JWK_ENDPOINT));
                    allowing(oidcClientConfig).getJwkSet();
                    will(returnValue(null));
                    allowing(oidcClientConfig).isHostNameVerificationEnabled();
                    will(returnValue(false));
                    allowing(oidcClientConfig).getJwkClientId();
                    will(returnValue(null));
                    allowing(oidcClientConfig).getJwkClientSecret();
                    will(returnValue(null));
                }
            });

            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);

            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
            createReferrerUrlCookieExpectations(cookieName);
            oica.oidcClientUtil = oidcClientUtil;
            AuthorizationCodeHandler ach = new AuthorizationCodeHandler(sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(req, res,
                    AUTHZ_CODE, //"authorizaCodeAAA",
                    originalState,
                    convClientConfig);
            assertNotNull("Ought to get a instance of ProviderAuthenticationResult", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCodeFailure2() {
        try {
            final String clientId = CLIENTID;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getClientId();
                    will(returnValue(clientId));
                }
            });

            // verifyResponseState
            final Cookie[] cookies = new Cookie[] {
                    cookie1
            };
            final String cookieName = ClientConstants.WAS_OIDC_STATE_KEY +
                    TEST_ORIGINAL_STATE.hashCode();
            final String myStateKey = TEST_STATE_KEY;
            final String originalState = TEST_ORIGINAL_STATE;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getClientId(); //
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClockSkewInSeconds();//
                    will(returnValue(300L));
                    one(convClientConfig).getId();//
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClientSecret();//
                    will(returnValue("clientsecret"));
                    one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey"); //
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue("BadStateKey"));
                }
            });
            createJwkRetrieverConstructorExpectations(clientConfig);

            OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);

            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
            createReferrerUrlCookieExpectations(cookieName);
            oica.oidcClientUtil = oidcClientUtil;
            AuthorizationCodeHandler ach = new AuthorizationCodeHandler(sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(req, res,
                    AUTHZ_CODE, //"authorizaCodeAAA",
                    originalState,
                    convClientConfig);
            assertNotNull("Ought to get a instance of ProviderAuthenticationResult", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCodeHttpBad() {
        try {
            final String tokenUrl = "http://op.ibm.com:8010/oidc/endpoint/token";
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenUrl));
                    one(convClientConfig).isHttpsRequired();
                    will(returnValue(true));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenUrl));
                }
            });
            OidcClientAuthenticator oica = new mockOidcClientAuthenticator();
            assertNotNull("Expected to get an instance of OidcClientAuthenticator but none", oica);
            oica.oidcClientUtil = oidcClientUtil;
            AuthorizationCodeHandler ach = new AuthorizationCodeHandler(sslSupport);
            ProviderAuthenticationResult result = ach.handleAuthorizationCode(req, res,
                    AUTHZ_CODE, //"authorizaCodeAAA",
                    "orignalState",
                    convClientConfig);
            assertNotNull("Ought to get a instance of ProviderAuthenticationResult", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testHandleAuthorizationCode_RedirectURLNotHttps() {
        mock.checking(new Expectations() {
            {
                exactly(3).of(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
                one(convClientConfig).getClockSkewInSeconds();//
                will(returnValue(300L));
                one(convClientConfig).getId();//
                will(returnValue(CLIENT01));
                one(convClientConfig).getClientSecret();//
                will(returnValue("clientsecret"));
                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey"); //
                one(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(true));
                one(convClientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
            }
        });

        createJwkRetrieverConstructorExpectations(clientConfig);
        AuthorizationCodeHandler ach = new AuthorizationCodeHandler(sslSupport);
        ProviderAuthenticationResult oidcResult = ach.handleAuthorizationCode(req, res, AUTHZ_CODE, "orignalState", convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    @Test
    public void testHandleAuthorizationCode_CatchSSLException() throws javax.net.ssl.SSLException {
        mock.checking(new Expectations() {
            {
                exactly(3).of(convClientConfig).getClientId(); // 2->3
                will(returnValue(CLIENTID));
                one(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_URL));
                one(convClientConfig).getSSLConfigurationName();
                will(returnValue(with(any(String.class))));
                one(sslSupport).getSSLSocketFactory((String) null);
                will(throwException(new javax.net.ssl.SSLException("bad factory")));
                one(convClientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
            }
        });

        //OidcClientAuthenticator oica = new mockOidcClientAuthenticator(sslSupportRef, 2);
        AuthorizationCodeHandler ach = new AuthorizationCodeHandler(sslSupport);
        ProviderAuthenticationResult oidcResult = ach.handleAuthorizationCode(req, res, AUTHZ_CODE, "orignalState", convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDoIdAssertion() {
        try {
            final String realm = "myRealm.ibm.com";
            final String uniqueSecurityName = "myUniqueSecurityname";
            final ArrayList arrayList = new ArrayList();
            arrayList.add("groupId1");
            arrayList.add("groupId2");
            mock.checking(new Expectations() {
                {
                    one(clientConfig).isMapIdentityToRegistryUser();
                    will(returnValue(false));
                    one(clientConfig).getRealmIdentifier();
                    will(returnValue(ClientConstants.REALM_NAME));
                    one(payload).get(ClientConstants.REALM_NAME);
                    will(returnValue(realm));
                    one(clientConfig).getUniqueUserIdentifier();
                    will(returnValue(ClientConstants.UNIQUE_SECURITY_NAME));
                    one(payload).get(ClientConstants.UNIQUE_SECURITY_NAME);
                    will(returnValue(uniqueSecurityName));
                    one(payload).get(ClientConstants.GROUPS_ID);
                    will(returnValue(arrayList));
                    one(clientConfig).isDisableLtpaCookie();
                    will(returnValue(false));
                }
            });

            createClientConfigExpectationsForIdentityAssertion();

            Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
            commonAuthn.doIdAssertion(customProperties, payload, clientConfig);
            assertEquals("Expect to have 3 iteam in customerProperties but only " + customProperties.size(),
                    3, customProperties.size());
            String resultID = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID);
            assertTrue("expected to get a String begings with 'user:'", resultID.startsWith("user:"));
            String resultRealm = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM);
            assertEquals("Expected to get an realm '" + realm + "' but get '" + resultRealm + "'",
                    realm, resultRealm);
            ArrayList resultList = (ArrayList) customProperties.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
            assertNotNull("Expected to get an ArrayList but get null", resultList);
            assertEquals("Expected to get 2 entries in the list but get " + resultList.size(), 2, resultList.size());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoIdAssertion2() {
        final String REALMID = "realm_id", REALM1 = "realm1";
        final String USERID = "user_id", USERID2 = "user_id2", USER1 = "user1";
        final String GROUP1 = "group1";

        mock.checking(new Expectations() {
            {
                one(clientConfig).isMapIdentityToRegistryUser();
                will(returnValue(false));
                one(clientConfig).getRealmIdentifier();
                will(returnValue(REALMID));
                one(clientConfig).getUniqueUserIdentifier();
                will(returnValue(USERID));
                one(clientConfig).getUserIdentityToCreateSubject();
                will(returnValue(USERID2));
                one(clientConfig).getGroupIdentifier();
                will(returnValue(GROUP1));

                one(payload).get(REALMID);
                will(returnValue(null));
                one(payload).get(ClientConstants.ISS);
                will(returnValue(REALM1));
                one(payload).get(USERID);
                will(returnValue(null));
                one(payload).get(USERID2);
                will(returnValue(USER1));
                one(payload).get(GROUP1);
                will(returnValue(null));
                one(clientConfig).isDisableLtpaCookie();
                will(returnValue(false));
            }
        });

        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        commonAuthn.doIdAssertion(customProperties, payload, clientConfig);

        assertNotNull("Expected a valid value for 'uniqueId' but received null.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertNotNull("Expected a valid value for 'realm' but received null.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM));
    }

    @Test
    public void testHandleRedirectToServer_URLNotHttps() {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
                allowing(clientConfig).getContextPath();
                will(returnValue("/oidcclient"));
                one(clientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(cookie2).setMaxAge(420);
                allowing(req).getScheme();
                will(returnValue("https"));
                one(clientConfig).createSession();
                will(returnValue(false));
                allowing(clientConfig).isSocial();
                will(returnValue(false));

                one(clientConfig).getId();//
                will(returnValue(CLIENT01));//
                one(clientConfig).getClientSecret();//
                will(returnValue("client01secret"));//
                one(req).getRequestURL();//
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//
                one(clientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(clientConfig).isHttpsRequired();
                will(returnValue(true));

                exactly(7).of(req).getMethod();
                will(returnValue(METHOD_POST));
                exactly(2).of(req).getParameter(PARAMETER_OIDC_CLIENT);
                will(returnValue("parameter"));
                one(cookie2).setSecure(true);
                one(clientConfig).getRedirectUrlWithJunctionPath(TEST_URL + "/oidcclient/redirect/" + CLIENT01);
                will(returnValue(TEST_URL));
                one(clientConfig).isHttpsRequired();
                will(returnValue(true));
            }
        });
        createJwkRetrieverConstructorExpectations(clientConfig);

        OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oica.handleRedirectToServer(req, res, clientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    @Test
    public void testHandleRedirectToServer_MissingOpenIDScope() {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
                allowing(clientConfig).getContextPath();
                will(returnValue("/oidcclient"));
                allowing(req).getScheme();
                will(returnValue("https"));
                one(clientConfig).createSession();
                will(returnValue(false));
                one(clientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(cookie2).setMaxAge(420);

                allowing(clientConfig).isSocial();
                will(returnValue(false));

                one(clientConfig).getId();//
                will(returnValue(CLIENT01));//
                one(clientConfig).getClientSecret();//
                will(returnValue("client01secret"));//
                one(req).getRequestURL();//
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//

                one(clientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                exactly(2).of(clientConfig).getScope();
                will(returnValue("bad_scope"));
                one(clientConfig).getClientId();
                will(returnValue(null));

                exactly(2).of(req).getMethod();
                will(returnValue(METHOD_GET));
                one(req).getParameter("acr_values");
                will(returnValue(null));

                one(clientConfig).getRedirectUrlWithJunctionPath(TEST_URL + "/oidcclient/redirect/client01");
                will(returnValue(TEST_URL + "/oidcclient/redirect/client01"));
                one(clientConfig).isHttpsRequired();
                will(returnValue(true));
            }
        });
        createJwkRetrieverConstructorExpectations(clientConfig);

        OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oica.handleRedirectToServer(req, res, clientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    @Test
    public void testHandleRedirectToServer_CatchUnsupportedEncodingException() {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        final String TEST_REDIRECT_TO_HOST_AND_PORT = "http://harmonic.austin.ibm.com:8010";
        final String TEST_FULL_URL = TEST_REDIRECT_TO_HOST_AND_PORT + "/oidcclient/redirect/client01";
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getRedirectUrlWithJunctionPath(TEST_REDIRECT_TO_HOST_AND_PORT);
                will(returnValue(TEST_REDIRECT_TO_HOST_AND_PORT));
                allowing(clientConfig).getContextPath();
                will(returnValue("/oidcclient"));
                allowing(req).getScheme();
                will(returnValue("https"));
                one(req).getMethod();
                will(returnValue(METHOD_GET));
                one(clientConfig).getGrantType();
                will(returnValue("code"));
                one(clientConfig).createSession();
                will(returnValue(false));
                one(clientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(cookie2).setMaxAge(420);

                allowing(clientConfig).isSocial();
                will(returnValue(false));

                one(clientConfig).getId();//
                will(returnValue(CLIENT01));//
                one(clientConfig).getClientSecret();//
                will(returnValue("client01secret"));//
                one(req).getRequestURL();//
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//

                one(clientConfig).getRedirectUrlFromServerToClient();
                //will(returnValue(TEST_URL));
                will(returnValue(TEST_REDIRECT_TO_HOST_AND_PORT));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                one(clientConfig).getScope();
                will(returnValue(TEST_GRANT_TYPE));
                one(clientConfig).getClientId();
                will(returnValue("client1"));

                exactly(4).of(req).getMethod();
                will(returnValue(METHOD_POST));
                one(req).getParameter(PARAMETER_OIDC_CLIENT);
                will(returnValue(null));
                one(req).getParameter("acr_values");
                will(returnValue(null));

                one(clientConfig).getRedirectUrlWithJunctionPath(TEST_FULL_URL);
                will(returnValue(TEST_FULL_URL));
                one(clientConfig).isHttpsRequired();
                will(returnValue(true));

            }
        });

        OidcClientAuthenticator oica = new mockOidcClientAuthenticator(sslSupportRef, 1);

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oica.handleRedirectToServer(req, res, clientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    //@Test
    public void testHandleRedirectToServer_CatchUnsupportedEncodingException2() {
        final String query = "response_type=code";

        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        //createReqUrlExpectations(query);
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
                allowing(clientConfig).getContextPath();
                will(returnValue("/oidcclient"));
                allowing(req2).getScheme();
                will(returnValue("https"));
                one(req2).getServerPort();
                will(returnValue(8020));
                one(clientConfig).createSession();
                will(returnValue(false));
                one(clientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(cookie2).setMaxAge(-1);
                allowing(cookie2).setMaxAge(420);
                allowing(clientConfig).getClientId();
                will(returnValue(CLIENT01));
                one(clientConfig).getId();//
                will(returnValue(CLIENT01));//
                one(clientConfig).getClientSecret();//
                will(returnValue("client01secret"));//
                one(req2).getRequestURL();//
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//

                allowing(clientConfig).getAuthzRequestParams(); //TODO wait for OL changes
                will(returnValue(null));

                one(clientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                allowing(clientConfig).getScope();
                will(returnValue(TEST_GRANT_TYPE));
                one(clientConfig).getGrantType();
                will(returnValue(Constants.IMPLICIT));
                one(clientConfig).getResponseType();
                will(returnValue("id_token token"));
                one(clientConfig).getClientId();
                will(returnValue(CLIENTID));
                one(clientConfig).isNonceEnabled();
                will(returnValue(false));//

                one(clientConfig).getId(); //
                will(returnValue(CLIENT01));//
                one(clientConfig).getClientSecret(); //
                will(returnValue("clientSecret"));//

                one(oidcClientRequest).getRequest(); //
                will(returnValue(req)); //
                one(req2).getCookies();//
                will(returnValue(cookies));//

                one(clientConfig).getAuthContextClassReference();
                will(returnValue(null));
                exactly(2).of(clientConfig).getPrompt();
                will(returnValue("prompt"));
                one(clientConfig).getResources();
                will(returnValue(null));
                one(clientConfig).getAuthorizationEndpointUrl();
                will(returnValue(TEST_AUTHORIZATION_ENDPOINT));
                one(clientConfig).isClientSideRedirect();
                will(returnValue(false));

                exactly(2).of(req2).getRequestURL();//
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//

                one(oidcClientRequest).getResponse(); //
                will(returnValue(res));//
                one(res).addCookie(with(any(Cookie.class)));//

                allowing(clientConfig).isSocial();
                will(returnValue(false));

                exactly(3).of(req2).getMethod();
                will(returnValue(METHOD_GET));
                one(req2).getParameter("acr_values");
                will(returnValue(null));

                one(clientConfig).getRedirectUrlWithJunctionPath(TEST_URL + "/oidcclient/redirect/client01");
                will(returnValue(TEST_URL + "/oidcclient/redirect/client01"));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                one(clientConfig).isHttpsRequired();
                will(returnValue(false));
                allowing(req2).getAttribute("com.ibm.wsspi.security.oidc.client.request");
                will(returnValue(convClientRequest));
                allowing(convClientRequest).getRequest();
                will(returnValue(req2));
                allowing(convClientRequest).getResponse();
                will(returnValue(res));

                allowing(req2).getScheme();
                will(returnValue("https"));
                one(req2).getServerPort();
                will(returnValue(8020));
                one(req2).getRequestURL();
                will(returnValue(new StringBuffer(TEST_URL)));
                one(req2).getQueryString();
                will(returnValue(query));
            }
        });
        createJwkRetrieverConstructorExpectations(clientConfig);

        OidcClientAuthenticator oica = new OidcClientAuthenticator(sslSupportRef);

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oica.handleRedirectToServer(req2, res, clientConfig);

        assertEquals("Expected to receive status:" + AuthResult.REDIRECT_TO_PROVIDER + " but received:" + oidcResult.getStatus() + ".", AuthResult.REDIRECT_TO_PROVIDER,
                oidcResult.getStatus());
        assertEquals("Expected to receive status code:" + HttpServletResponse.SC_OK + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_OK, oidcResult.getHttpStatusCode());
    }

    @Test
    public void testVerifyResponseState_NullResponseState() {
        mock.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT01));
                one(convClientConfig).getClockSkewInSeconds();//
                will(returnValue(300L));
                one(req).getCookies();//
                will(returnValue(cookies));//
                one(convClientConfig).getId();//
                will(returnValue(CLIENT01));//
                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey"); //
                one(convClientConfig).getClientSecret();//
                will(returnValue("clientsecret"));//
                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res, "WASOidcStateKey");//
            }
        });
        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = OidcCAUtil.verifyResponseState(req, res, null, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    private void createClientConfigExpectationsForIdentityAssertion() {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getGroupIdentifier();
                will(returnValue(ClientConstants.GROUPS_ID));
            }
        });
    }

    private void checkForBadStatusExpectations(ProviderAuthenticationResult oidcResult) {
        assertEquals("Expected to receive status:" + AuthResult.SEND_401 + " but received:" + oidcResult.getStatus() + ".", AuthResult.SEND_401, oidcResult.getStatus());

        assertEquals("Expected to receive status code:" + HttpServletResponse.SC_UNAUTHORIZED + " but received:" + oidcResult.getHttpStatusCode() + ".",
                HttpServletResponse.SC_UNAUTHORIZED, oidcResult.getHttpStatusCode());
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

    class mockOidcClientUtil extends OidcClientUtil {
        IOException ioe = null;
        HttpException httpe = null;

        public mockOidcClientUtil(IOException e) {
            super();
            ioe = e;
        }

        public mockOidcClientUtil(HttpException e) {
            super();
            httpe = e;
        }

        //@Override
        @Override
        public HashMap<String, String> getTokensFromAuthzCode(String tokenEnpoint,
                String clientId,
                @Sensitive String clientSecret,
                String redirectUri,
                String code,
                String grantType,
                SSLSocketFactory sslSocketFactory,
                boolean b,
                String authMethod,
                String resources, HashMap<String, String> customParams, boolean useJvmProps) throws HttpException, IOException {
            if (ioe != null) {
                throw ioe;
            }
            if (httpe != null) {
                throw httpe;
            }
            return new HashMap<String, String>();
        }

    }

    class mockOidcClientAuthenticator extends OidcClientAuthenticator {
        int iTest = 0;

        public mockOidcClientAuthenticator() {
            super();
        }

        public mockOidcClientAuthenticator(AtomicServiceReference<SSLSupport> sslSupportRef, int iTest) {
            super(sslSupportRef);
            this.iTest = iTest;
        }

        @Override
        protected SSLContext getSSLContext(String tokenUrl, String sslConfigurationName, String clientId) throws SSLException {
            if (iTest == 1 || iTest == 2)
                throw new SSLException("Invalid SSLContext");
            else
                return super.getSSLContext(tokenUrl, sslConfigurationName, clientId);
        }

    }

}
