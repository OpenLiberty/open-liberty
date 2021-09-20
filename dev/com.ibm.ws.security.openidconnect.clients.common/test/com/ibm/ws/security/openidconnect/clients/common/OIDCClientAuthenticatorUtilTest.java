/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.google.gson.JsonObject;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.common.structures.Cache;
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
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;
import test.common.junit.rules.MaximumJavaLevelRule;

public class OIDCClientAuthenticatorUtilTest {

    // Cap this unit test to Java 8 because it relies on legacy cglib which is not supported post JDK 8
    @ClassRule
    public static TestRule maxJavaLevel = new MaximumJavaLevelRule(8);

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

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
    private static final String TEST_ORIGINAL_STATE = "orignalStateThatIsAtLeastAsLongAsRequired";
    private static final String ANOTHER_ORIGINAL_STATE = "34W2nb0LYTDL9Fryvh3X";
    private static final String TEST_URL = "http://harmonic.austin.ibm.com:8010/formlogin/SimpleServlet";
    private static final String TEST_REDIRECT_URL = "https://my.rp.client.com:8010/redirect/client";
    private static final String TEST_AUTHORIZATION_ENDPOINT = "https://op.ibm.com:8020/oidc/endpoint";
    private static final String TEST_AUTHORIZATION_CODE = "KQHIHE8FUZAYYevLpBY7UnqYTO3lFw";
    private static final String TEST_TOKEN_ENDPOINT = "http://harmonic:8011/oidc/endpoint/OidcConfigSample/token";
    private static final String TEST_GRANT_TYPE = "openid profile";
    private static final String CLIENTID = "clientid";
    private static final String CLIENT01 = "client01";
    // private static final String SHARED_KEY = "secret";  // conversion from net.oauth to jose4j requires a longer key
    private static final String SHARED_KEY = "secretsecretsecretsecretsecretsecret";
    private static final String TEST_ACR_VALUES = "urn:mace:incommon:iap:silver urn:mace:incommon:iap:bronze";
    private static final String authMethod = "basic";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_GET = "GET";
    private static final String PARAMETER_OIDC_CLIENT = "oidc_client";

    private final OidcClientConfig clientConfig = mock.mock(OidcClientConfig.class, "clientConfig");
    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<SSLSupport> sslSupportRef = mock.mock(AtomicServiceReference.class, "sslSupportRef");
    private final SSLSupport sslSupport = mock.mock(SSLSupport.class, "sslSupport");
    private final JSSEHelper jsseHelper = mock.mock(JSSEHelper.class, "jsseHelper");
    private final IExtendedRequest req = mock.mock(IExtendedRequest.class, "req");
    private final IExtendedRequest req2 = mock.mock(IExtendedRequest.class, "req2");
    private final HttpSession session = mock.mock(HttpSession.class, "session");
    private final HttpServletResponse res = mock.mock(HttpServletResponse.class, "res");
    private final ReferrerURLCookieHandler referrerURLCookieHandler = mock.mock(ReferrerURLCookieHandler.class, "referrerURLCookieHandler");
    private final Cookie cookie1 = mock.mock(Cookie.class, "cookie1");
    private final Cookie cookie2 = mock.mock(Cookie.class, "cookie2");
    private final OidcClientUtil oidcClientUtil = mock.mock(OidcClientUtil.class, "oidcClientUtil");
    private final OIDCClientAuthenticatorUtil oidcClientAuthUtil = mock.mock(OIDCClientAuthenticatorUtil.class);
    private final IDToken idToken = mock.mock(IDToken.class, "idToken");
    private final Payload payload = mock.mock(Payload.class, "payload");
    private final SSLContext sslContext = mock.mock(SSLContext.class, "sslContext");
    private final SSLSocketFactory sslSocketFactory = mock.mock(SSLSocketFactory.class, "sslSocketFactory");
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final PrintWriter pw = mock.mock(PrintWriter.class, "pw");
    private final MockOidcClientRequest oidcClientRequest = mock.mock(MockOidcClientRequest.class, "oidcClientRequest");
    private final OidcClientRequest convClientRequest = mock.mock(OidcClientRequest.class, "convClientRequest");
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class, "convClientConfig");

    private final JWSHeader jwsHeader = new JWSHeader();
    final String STATE = "state";

    private final HashMap<String, String> tokens = new HashMap<String, String>(100);
    private final OIDCClientAuthenticatorUtil oidcCAUtil = new OIDCClientAuthenticatorUtil();

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
                allowing(webAppSecConfig).getSSODomainList();
                will(returnValue(null));
                allowing(webAppSecConfig).getSSOUseDomainFromURL();
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

        createConstructorExpectations(convClientConfig);
        tokens.clear();
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    private void createReqCookie() {
        // set up request parameter cookie
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put("access_token", new String[] { "access_token_content" });
        map.put(id_token, idTokens);
        map.put("refresh_token", new String[] { "refresh_token_content" });
        map.put("code", new String[] { "YMKexUVcHci2dhDJzNRHW2w9rhf70u" });
        map.put("state", new String[] { "001534964952438QID21LdnF" });

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

        String localEncoded = null;
        try {
            localEncoded = Base64Coder.toString(Base64Coder.base64Encode(requestParameters.getBytes(ClientConstants.CHARSET)));
        } catch (UnsupportedEncodingException e) {
            //This should not happen, we are using UTF-8
        }

        // digest with the client_secret value
        String tmpStr = new String(localEncoded);
        tmpStr = tmpStr.concat("_").concat(clientSecret);

        encodedReqParams = new String(localEncoded).concat("_").concat(HashUtils.digest(tmpStr));
        reqParameterCookie = new Cookie(ClientConstants.WAS_OIDC_CODE, encodedReqParams);
    }

    @Test
    public void testValidateReqParameters() {

        createReqCookie();
        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getClientSecret();
                will(returnValue(clientSecret));
                allowing(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
            }
        });

        Hashtable<String, String> reqParameters = new Hashtable<String, String>();
        boolean validParameter = oidcCAUtil.validateReqParameters(convClientConfig, reqParameters, encodedReqParams);
        assertTrue("the request parameter is supposed to be correct but not", validParameter);
        String newIdTokenContent = reqParameters.get(id_token);
        assertTrue("the value of id_token is not " + idTokenContent + " but " + newIdTokenContent, idTokenContent.equals(newIdTokenContent));
        String newAccessTokenContent = reqParameters.get("access_token");
        assertTrue("The access token content is not \"access_token_content\"", "access_token_content".equals(newAccessTokenContent));
        String newRefreshTokenContent = reqParameters.get("refresh_token");
        assertTrue("The access token content is not \"refresh_token_content\"", "refresh_token_content".equals(newRefreshTokenContent));
    }

    @Test
    public void testGetReqUrlNull() {
        try {
            createReqUrlExpectations(null);
            String strUrl = oidcCAUtil.getReqURL(req);

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
            String strUrl = oidcCAUtil.getReqURL(req);
            String expect = TEST_URL + "?" + query;

            assertEquals("The URL must contain the query string.", expect, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetReqUrlQuery_withSpecialCharacters() {
        try {
            String value = "code>\"><script>alert(100)</script>";
            final String query = "response_type=" + value;
            createReqUrlExpectations(query);
            String strUrl = oidcCAUtil.getReqURL(req);
            String expect = TEST_URL + "?response_type=" + value;

            assertEquals("The URL must contain the unencoded query string.", expect, strUrl);
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
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(issuer));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer result is not the one expected!", issuer, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointNotConfigured() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(null));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertNull("Issuer was expected to be null but was [" + issuerResult + "].", issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointEmpty() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(""));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer was expected to be an empty string but was not.", "", issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSimpleString() {
        final String endpointValue = "some simple string that's not a URL";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointOnlySlash() {
        final String endpointValue = "/";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer was expected to be an empty string but was not.", "", issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointBeginningSlash() {
        final String endpointValue = "";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue + "/after"));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSimpleStringWithSlashes() {
        final String endpointValue = "before/middle";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue + "/after"));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSimpleStringTrailingSlash() {
        final String endpointValue = "before/middle/after";
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(endpointValue + "/"));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", endpointValue, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointSchemeOnly() {
        try {
            final String tokenEndpointUrl = "http://";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenEndpointUrl));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", tokenEndpointUrl, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_issuerNotConfigured_tokenEndpointNoContext() {
        try {
            final String tokenEndpointUrl = "http://localhost";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    one(convClientConfig).getTokenEndpointUrl();
                    will(returnValue(tokenEndpointUrl));
                }
            });
            String issuerResult = oidcCAUtil.getIssuerIdentifier(convClientConfig);
            assertEquals("Issuer did not match expected value.", tokenEndpointUrl, issuerResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //0509 @Test
    public void testAuthenticate_redirectToServer() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getGrantType();
                    will(returnValue("authorization_code"));
                }
            });
            createCommonAuthenticateExpectations(null);
            createHandleRedirectToServerExpectations(TEST_AUTHORIZATION_ENDPOINT, null);

            mock.checking(new Expectations() {
                {
                    allowing(convClientConfig).getPrompt();
                    will(returnValue(null));
                }
            });

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);

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
                    one(convClientConfig).getGrantType();
                    will(returnValue(Constants.IMPLICIT));
                }
            });
            createGetAuthzCodeAndSateFromCookieExpectations();
            createHandleTokensExpectations(ANOTHER_ORIGINAL_STATE, ANOTHER_ORIGINAL_STATE, false, TEST_TOKEN_STRING, TEST_ACCESS_TOKEN);

            final String myStateKey = ANOTHER_ORIGINAL_STATE;
            final String originalState = ANOTHER_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
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
                    one(convClientConfig).getGrantType();
                    will(returnValue(Constants.IMPLICIT));
                }
            });
            createGetAuthzCodeAndSateFromCookieExpectations();
            createHandleTokensExpectations(ANOTHER_ORIGINAL_STATE, ANOTHER_ORIGINAL_STATE, true, TEST_TOKEN_STRING, TEST_ACCESS_TOKEN);

            final String myStateKey = ANOTHER_ORIGINAL_STATE;
            final String originalState = TEST_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAuthenticate_missingAuthEndpoint() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getAuthorizationEndpointUrl();
                    will(returnValue(null));
                    one(convClientConfig).getClientId();
                    will(returnValue(CLIENTID));
                }
            });

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
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
                //                one(req).getRequestURL();
                //                will(returnValue(new StringBuffer("http://foo.com/something")));
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
                allowing(clientConfig).getUseSystemPropertiesForHttpClientConnections();
                will(returnValue(false));
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
    public void testAuthenticate_authorizationCode() {
        try {
            createConstructorExpectations(convClientConfig);
            final String cookieValue = TEST_COOKIE_VALUE;
            createCommonAuthenticateExpectations(cookieValue.getBytes());
            createHandleAuthorizationCodeExpectations(TEST_STATE_KEY);

            final String myStateKey = TEST_STATE_KEY;
            final String originalState = ANOTHER_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);// LinkedHashMap<String, Object>(10000);
            requestStates.put(myStateKey, originalState);

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
            assertEquals("The authentication result status must be SUCCESS.", AuthResult.SUCCESS, result.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createConstructorExpectations(final ConvergedClientConfig clientConfig) {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
    }

    private void createHandleAuthorizationCodeExpectations(final String stateKey) throws Exception {
        createResponseStateExpectations(stateKey);
        createHttpsRequirementExpectationsForTokenEndpoint();
        createRedirectUrlExpectations();
        createSSLContextExpectations(null, sslContext);
        createTokenRequestExpectations();
        createResultExpectations();
        createPostParameterHelperExpectations();
        createReferrerUrlCookiesExpectations();
        createReferrerUrlCookieExpectations(ClientConstants.WAS_OIDC_CODE);
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

    private void createTokenRequestExpectations() throws Exception {
        mock.checking(new Expectations() {
            {
                one(clientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_TOKEN_ENDPOINT)); // TODO: Refactor code so that this is only called once.
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
                one(clientConfig).getClientSecret();
                will(returnValue(SHARED_KEY));
                allowing(clientConfig).getGrantType();
                will(returnValue(TEST_GRANT_TYPE));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(false));
                one(clientConfig).getTokenEndpointAuthMethod();
                will(returnValue(authMethod));
                allowing(clientConfig).getUseSystemPropertiesForHttpClientConnections();
                will(returnValue(false));
            }
        });

        final String tokenString = TEST_TOKEN_STRING;
        String accessToken = TEST_ACCESS_TOKEN;
        tokens.put(Constants.ID_TOKEN, tokenString);
        tokens.put(Constants.ACCESS_TOKEN, accessToken);
        mock.checking(new Expectations() {
            {
                one(oidcClientUtil).getTokensFromAuthzCode(TEST_TOKEN_ENDPOINT, CLIENT01, SHARED_KEY,
                        TEST_REDIRECT_URL, TEST_AUTHORIZATION_CODE,
                        TEST_GRANT_TYPE, sslSocketFactory, false, authMethod, null, null, false);
                will(returnValue(tokens));
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

    private void createClientConfigExpectationsForResultBeforeMapping() {
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
                //                one(referrerURLCookieHandler).invalidateReferrerURLCookie(req, res,
                //                        cookieName);
            }
        });
    }

    //0509 @Test
    public void testAuthenticate_authorizationCode_tokenRequestFailureWithIOException() {
        try {
            createConstructorExpectations(convClientConfig);
            createCommonAuthenticateExpectations(TEST_COOKIE_VALUE.getBytes());
            createHandleAuthorizationCodeExpectationsWithException(TEST_STATE_KEY, new IOException());

            final String myStateKey = TEST_STATE_KEY;
            final String originalState = ANOTHER_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);
            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //0509 @Test
    public void testAuthenticate_authorizationCode_tokenRequestFailureWithHttpException() {
        try {
            createConstructorExpectations(convClientConfig);
            createCommonAuthenticateExpectations(TEST_COOKIE_VALUE.getBytes());
            createHandleAuthorizationCodeExpectationsWithException(TEST_STATE_KEY, new HttpException());

            final String myStateKey = TEST_STATE_KEY;
            final String originalState = ANOTHER_ORIGINAL_STATE;
            Cache requestStates = new Cache(0, 0);
            requestStates.put(myStateKey, originalState);

            ProviderAuthenticationResult result = oidcCAUtil.authenticate(req, res, convClientConfig);

            assertEquals("The authentication result status must be SEND_401.", AuthResult.SEND_401, result.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createHandleAuthorizationCodeExpectationsWithException(final String stateKey, final Throwable throwable) throws Exception {
        createResponseStateExpectations(stateKey);
        createHttpsRequirementExpectationsForTokenEndpoint();
        createRedirectUrlExpectations();
        createSSLContextExpectations(null, sslContext);
        createTokenRequestExpectationsWithException(throwable);
        createPostParameterHelperExpectations();
        createReferrerUrlCookiesExpectations();
        createReferrerUrlCookieExpectations(ClientConstants.WAS_OIDC_CODE);
    }

    private void createTokenRequestExpectationsWithException(final Throwable throwable) throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(clientConfig).getTokenEndpointUrl();
                will(returnValue(TEST_TOKEN_ENDPOINT)); // TODO: Refactor code so that this is only called once.
                one(clientConfig).getClientId();
                will(returnValue(CLIENT01));
                one(clientConfig).getClientSecret();
                will(returnValue(SHARED_KEY));
                allowing(clientConfig).getGrantType();
                will(returnValue(TEST_GRANT_TYPE));
                one(clientConfig).isHostNameVerificationEnabled();
                will(returnValue(false));
                one(clientConfig).getTokenEndpointAuthMethod();
                will(returnValue(authMethod));
            }
        });
        mock.checking(new Expectations() {
            {
                one(oidcClientUtil).getTokensFromAuthzCode(TEST_TOKEN_ENDPOINT, CLIENT01, SHARED_KEY,
                        TEST_REDIRECT_URL, TEST_AUTHORIZATION_CODE,
                        TEST_GRANT_TYPE, sslSocketFactory, false, authMethod, null, null, false);

                will(throwException(throwable));
            }
        });
    }

    @Test
    public void testSetRedirectUrlIfNotDefined() {
        try {
            final String redirectUri = "https://mine.ibm.com:8020/oidcclient/redirect";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).isSocial();
                    will(returnValue(false));
                    one(convClientConfig).getRedirectUrlFromServerToClient();
                    will(returnValue(redirectUri));
                    one(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri);
                    will(returnValue(redirectUri));
                }
            });
            assertEquals("Redirect URL did not match expected value.", redirectUri, oidcCAUtil.setRedirectUrlIfNotDefined(req, convClientConfig));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testSetRedirectUrlIfNotDefined_socialFlow() {
        try {
            final String scheme = "https";
            final int port = 8020;
            final String serverName = "mine.ibm.com";
            final String clientId = CLIENT01;
            final String contextPath = "/oidcclient";
            final String redirectUri = scheme + "://" + serverName + ":" + port + contextPath + "/redirect/" + clientId;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).isSocial();
                    will(returnValue(true));
                    allowing(convClientConfig).getId();
                    will(returnValue(clientId));
                    allowing(convClientConfig).getContextPath();
                    will(returnValue(contextPath));
                    one(convClientConfig).getRedirectUrlFromServerToClient();
                    will(returnValue(null));
                    one(req).getServerName();
                    will(returnValue(serverName));
                    one(req).isSecure();
                    will(returnValue(true));
                    one(req).getServerPort();
                    will(returnValue(port));
                    one(req).getScheme();
                    will(returnValue(scheme));
                    one(convClientConfig).getRedirectUrlWithJunctionPath(redirectUri);
                    will(returnValue(redirectUri));
                }
            });
            String returnedUrl = oidcCAUtil.setRedirectUrlIfNotDefined(req, convClientConfig);
            assertEquals("Redirect URL did not match expected value.", redirectUri, returnedUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_forwardLoginParametersNull() {
        try {
            String query = "";
            final List<String> configuredValue = null;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_forwardLoginParametersEmpty() {
        try {
            String query = "The quick brown fox jumps over the lazy dog.";
            final List<String> configuredValue = new ArrayList<String>();
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_emptyString_requestMissingThatParameter() {
        try {
            String query = "scope=myScope";
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(null));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_emptyString() {
        try {
            String query = "some existing query string";
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            String expectedQuery = query + "&=";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_whitespaceOnly() {
        try {
            String query = "some existing query string";
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = " \t\n \r";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            // Parameter value should have been encoded
            String expectedQuery = query + "&=+%09%0A+%0D";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_emptyString_matchingParam_nonEmpty() {
        try {
            String query = "some existing query string";
            final String paramName = "";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "some_simple_param_value";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            String expectedQuery = query + "&=" + paramValue;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_whitespace_requestMissingThatParameter() {
        try {
            String query = "some existing query string";
            final String paramName = " ";
            final List<String> configuredValue = Arrays.asList(paramName);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(null));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_whitespace_matchingParam_whitespaceOnly() {
        try {
            String query = "some existing query string";
            final String paramName = "\n\r\t";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "    ";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + "%0A%0D%09" + "=" + "++++";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_whitespace_matchingParam_nonEmpty() {
        try {
            String query = "some existing query string";
            final String paramName = "\n \n";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = "some parameter value";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + "%0A+%0A" + "=" + "some+parameter+value";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_nonEmpty_requestMissingThatParameter() {
        try {
            String query = "scope=mySCope";
            final String paramName = "missingParam";
            final List<String> configuredValue = Arrays.asList(paramName);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(null));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_oneParameter_specialChars_matchingParam_specialChars() {
        try {
            String query = "scope=myScope&redirect_uri=some value";
            final String paramName = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
            final List<String> configuredValue = Arrays.asList(paramName);
            final String paramValue = paramName;
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValue));
                    one(req).getParameter(paramName);
                    will(returnValue(paramValue));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            String encodedSpecialChars = "%60%7E%21%40%23%24%25%5E%26*%28%29-_%3D%2B%5B%7B%5D%7D%5C%7C%3B%3A%27%22%2C%3C.%3E%2F%3F";
            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + encodedSpecialChars + "=" + encodedSpecialChars;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_multipleParameters_noneInRequest() {
        try {
            String query = "initial query";
            final List<String> configuredValues = Arrays.asList("", "my param", "Special! \n\t (Param) ", " 1234567890 ");
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                }
            });
            for (final String configuredVal : configuredValues) {
                mock.checking(new Expectations() {
                    {
                        one(req).getParameter(configuredVal);
                        will(returnValue(null));
                    }
                });
            }
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);
            assertEquals("Returned query should have matched original query.", query, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_multipleParameters_oneInRequest() {
        try {
            String query = "initial query";
            final String emptyParam = "";
            final String paramWithSpace = "my param";
            final String paramWithSpecialChars = "Special! \n\t (Param) ";
            final String paramWithNumbers = " 1234567890 ";
            final List<String> configuredValues = Arrays.asList(emptyParam, paramWithSpace, paramWithSpecialChars, paramWithNumbers);
            final String foundParamValue = "My\nParam\rValue";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                    one(req).getParameter(emptyParam);
                    will(returnValue(null));
                    // The request happens to have this parameter
                    one(req).getParameter(paramWithSpace);
                    will(returnValue(foundParamValue));
                    one(req).getParameter(paramWithSpecialChars);
                    will(returnValue(null));
                    one(req).getParameter(paramWithNumbers);
                    will(returnValue(null));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            // Parameter name and value should have been encoded
            String expectedQuery = query + "&" + "my+param" + "=" + "My%0AParam%0DValue";
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_multipleParameters_multipleInRequest() {
        try {
            String query = "initial query";
            final String emptyParam = "";
            final String paramWithSpace = "my param";
            final String paramWithSpecialChars = "Special! \n\t (Param) ";
            final String paramWithNumbers = " 1234567890 ";
            final List<String> configuredValues = Arrays.asList(emptyParam, paramWithSpace, paramWithSpecialChars, paramWithNumbers);
            final String foundParamValue1 = "My\nParam\rValue";
            final String foundParamValue2 = "a_simple_param_value";
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                    one(req).getParameter(emptyParam);
                    will(returnValue(null));
                    one(req).getParameter(paramWithSpace);
                    will(returnValue(foundParamValue1));
                    one(req).getParameter(paramWithSpecialChars);
                    will(returnValue(null));
                    one(req).getParameter(paramWithNumbers);
                    will(returnValue(foundParamValue2));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            // Parameter names and values should have been encoded
            String expectedQuery = query + "&" + "my+param" + "=" + "My%0AParam%0DValue" + "&" + "+1234567890+" + "=" + foundParamValue2;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAddForwardLoginParamsToQuery_multipleParameters_allInRequest() {
        try {
            String query = "initial query";
            final String paramName1 = "name1";
            final String paramName2 = "name2";
            final String paramName3 = "name3";
            final String paramName4 = "name4";
            final String paramValue1 = "value1";
            final String paramValue2 = "value2";
            final String paramValue3 = "value3";
            final String paramValue4 = "value4";
            final List<String> configuredValues = Arrays.asList(paramName1, paramName2, paramName3, paramName4);
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getForwardLoginParameter();
                    will(returnValue(configuredValues));
                    one(req).getParameter(paramName1);
                    will(returnValue(paramValue1));
                    one(req).getParameter(paramName2);
                    will(returnValue(paramValue2));
                    one(req).getParameter(paramName3);
                    will(returnValue(paramValue3));
                    one(req).getParameter(paramName4);
                    will(returnValue(paramValue4));
                }
            });
            String newQuery = oidcCAUtil.addForwardLoginParamsToQuery(convClientConfig, req, query);

            // Parameter names and values should have been encoded
            String expectedQuery = query + "&" + paramName1 + "=" + paramValue1 + "&" + paramName2 + "=" + paramValue2 + "&" + paramName3 + "=" + paramValue3 + "&" + paramName4 + "=" + paramValue4;
            assertEquals("Returned query did not match expected value.", expectedQuery, newQuery);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testVerifyResponseStateFailure() {
        try {
            final String originalState = TEST_ORIGINAL_STATE;
            final String cookieName = ClientConstants.WAS_OIDC_STATE_KEY + ("notA" + originalState).hashCode();
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getClientId();
                    will(returnValue(CLIENT01));
                    one(convClientConfig).getClockSkewInSeconds();
                    will(returnValue(TEST_CLOCK_SKEW_IN_SECONDS));
                    one(convClientConfig).getAuthenticationTimeLimitInSeconds();
                    will(returnValue(420L));
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(convClientConfig).getClientSecret();
                    will(returnValue("clientsecret"));
                }
            });
            OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
            createReferrerUrlCookieExpectations(cookieName);

            ProviderAuthenticationResult result = oidcCAUtil.verifyResponseState(req, res, "notA" + originalState, convClientConfig);
            assertNotNull("Did not get an expecyted result", result);
            assertTrue("Expect to get an " + AuthResult.SEND_401 + " but a " + result.getStatus(),
                    AuthResult.SEND_401.equals(result.getStatus()));

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
    public void testHandleRedirectToServer_authorizationEndpointNotHttps_httpsRequired() {
        try {
            mock.checking(new Expectations() {
                {
                    one(convClientConfig).getAuthorizationEndpointUrl();
                    will(returnValue("some non URL"));
                    one(convClientConfig).isHttpsRequired();
                    will(returnValue(true));
                }
            });
            ProviderAuthenticationResult result = oidcCAUtil.handleRedirectToServer(req, res, convClientConfig);
            checkForBadStatusExpectations(result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //@Test
    public void testHandleRedirectToServer_URLNotHttps() {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        mock.checking(new Expectations() {
            {
                one(oidcClientAuthUtil).handleRedirectToServer(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class)), with(any(ConvergedClientConfig.class)));
                will(returnValue(new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED)));
                //                one(clientConfig).getAuthenticationTimeLimitInSeconds();
                //                will(returnValue(420L));
                //                one(cookie2).setMaxAge(420);
                //                allowing(req).getScheme();
                //                will(returnValue("https"));
                //                one(clientConfig).createSession();
                //                will(returnValue(false));
                //                one(clientConfig).getId();
                //                will(returnValue(CLIENT01));
                //                one(clientConfig).getClientSecret();
                //                will(returnValue("client01secret"));
                //                one(req).getRequestURL();
                //                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));
                //                one(clientConfig).getRedirectUrlFromServerToClient();
                //                will(returnValue(TEST_URL));
                //                one(clientConfig).isHttpsRequired();
                //                will(returnValue(true));
                //
                //                exactly(7).of(req).getMethod();
                //                will(returnValue(METHOD_POST));
                //                exactly(2).of(req).getParameter(PARAMETER_OIDC_CLIENT);
                //                will(returnValue("parameter"));
                //                one(cookie2).setSecure(true);
                //                one(clientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                //                will(returnValue(TEST_URL));
                //                one(clientConfig).isHttpsRequired();
                //                will(returnValue(true));
            }
        });

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oidcCAUtil.handleRedirectToServer(req, res, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    // TODO - Needs to be moved to dedicated unit test class for OIDCClientAuthenticatorUtil
    //@Test
    public void testHandleRedirectToServer_MissingOpenIDScope() {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        mock.checking(new Expectations() {
            {
                allowing(req).getScheme();
                will(returnValue("https"));
                one(convClientConfig).createSession();
                will(returnValue(false));
                one(convClientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(cookie2).setMaxAge(420);

                one(convClientConfig).getId();
                will(returnValue(CLIENT01));
                one(convClientConfig).getClientSecret();
                will(returnValue("client01secret"));
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));

                one(convClientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                exactly(2).of(convClientConfig).getScope();
                will(returnValue("bad_scope"));
                one(convClientConfig).getClientId();
                will(returnValue(null));

                exactly(2).of(req).getMethod();
                will(returnValue(METHOD_GET));
                one(req).getParameter("acr_values");
                will(returnValue(null));

                one(convClientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(true));
            }
        });

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oidcCAUtil.handleRedirectToServer(req, res, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    // TODO - Needs to be moved to dedicated unit test class for OIDCClientAuthenticatorUtil
    //@Test
    public void testHandleRedirectToServer_CatchUnsupportedEncodingException() {
        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        mock.checking(new Expectations() {
            {
                allowing(req).getScheme();
                will(returnValue("https"));
                one(req).getMethod();
                will(returnValue(METHOD_GET));
                one(convClientConfig).getGrantType();
                will(returnValue("code"));
                one(convClientConfig).createSession();
                will(returnValue(false));
                one(convClientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(cookie2).setMaxAge(420);

                one(convClientConfig).getId();
                will(returnValue(CLIENT01));
                one(convClientConfig).getClientSecret();
                will(returnValue("client01secret"));
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));

                one(convClientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).getScope();
                will(returnValue(TEST_GRANT_TYPE));
                one(convClientConfig).getClientId();
                will(returnValue("client1"));

                exactly(4).of(req).getMethod();
                will(returnValue(METHOD_POST));
                one(req).getParameter(PARAMETER_OIDC_CLIENT);
                will(returnValue(null));
                one(req).getParameter("acr_values");
                will(returnValue(null));

                one(convClientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(true));

            }
        });

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oidcCAUtil.handleRedirectToServer(req, res, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    // TODO - Needs to be moved to dedicated unit test class for OIDCClientAuthenticatorUtil
    //@Test
    public void testHandleRedirectToServer_CatchUnsupportedEncodingException2() {
        final String query = "response_type=code";

        createHttpsRequirementExpectationsForAuthorizationEndpoint(TEST_AUTHORIZATION_ENDPOINT);
        //createReqUrlExpectations(query);
        mock.checking(new Expectations() {
            {
                allowing(req2).getScheme();
                will(returnValue("https"));
                one(req2).getServerPort();
                will(returnValue(8020));
                one(convClientConfig).createSession();
                will(returnValue(false));
                one(convClientConfig).getAuthenticationTimeLimitInSeconds();
                will(returnValue(420L));
                one(cookie2).setMaxAge(-1);
                allowing(cookie2).setMaxAge(420);

                one(convClientConfig).getId();
                will(returnValue(CLIENT01));
                one(convClientConfig).getClientSecret();
                will(returnValue("client01secret"));
                one(req2).getRequestURL();
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));

                one(convClientConfig).getRedirectUrlFromServerToClient();
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                exactly(2).of(convClientConfig).getScope();
                will(returnValue(TEST_GRANT_TYPE));
                one(convClientConfig).getGrantType();
                will(returnValue(Constants.IMPLICIT));
                one(convClientConfig).getResponseType();
                will(returnValue("id_token token"));
                one(convClientConfig).getClientId();
                will(returnValue(CLIENTID));
                one(convClientConfig).isNonceEnabled();
                will(returnValue(false));

                one(convClientConfig).getId();
                will(returnValue(CLIENT01));
                one(convClientConfig).getClientSecret();
                will(returnValue("clientSecret"));

                one(oidcClientRequest).getRequest();
                will(returnValue(req));
                one(req2).getCookies();
                will(returnValue(cookies));

                one(convClientConfig).getAuthContextClassReference();
                will(returnValue(null));
                exactly(2).of(convClientConfig).getPrompt();
                will(returnValue("prompt"));
                one(convClientConfig).getResources();
                will(returnValue(null));
                one(convClientConfig).getAuthorizationEndpointUrl();
                will(returnValue(TEST_AUTHORIZATION_ENDPOINT));
                one(convClientConfig).isClientSideRedirect();
                will(returnValue(false));

                exactly(2).of(req2).getRequestURL();
                will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));

                one(oidcClientRequest).getResponse();
                will(returnValue(res));
                one(res).addCookie(with(any(Cookie.class)));

                exactly(3).of(req2).getMethod();
                will(returnValue(METHOD_GET));
                one(req2).getParameter("acr_values");
                will(returnValue(null));

                one(convClientConfig).getRedirectUrlWithJunctionPath(TEST_URL);
                will(returnValue(TEST_URL));
                one(convClientConfig).isHttpsRequired();
                will(returnValue(false));
                one(convClientConfig).isHttpsRequired();
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

        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
        ProviderAuthenticationResult oidcResult = oidcCAUtil.handleRedirectToServer(req2, res, convClientConfig);

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
            }
        });
        ProviderAuthenticationResult oidcResult = oidcCAUtil.verifyResponseState(req, res, null, convClientConfig);

        checkForBadStatusExpectations(oidcResult);
    }

    private void checkForBadStatusExpectations(ProviderAuthenticationResult oidcResult) {
        assertNotNull("ProviderAuthenticationResult was null but should not have been.", oidcResult);
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
                String resources,
                HashMap<String, String> customParams,
                boolean useJvmProps) throws HttpException, IOException {

            if (ioe != null) {
                throw ioe;
            }
            if (httpe != null) {
                throw httpe;
            }
            return new HashMap<String, String>();
        }

    }

}
