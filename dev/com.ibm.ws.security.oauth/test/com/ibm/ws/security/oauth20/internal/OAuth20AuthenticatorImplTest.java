/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.UtilConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

public class OAuth20AuthenticatorImplTest {

    private static final String PARAMETER = "parameter";
    private static final String HEADER = "header";
    private static final String AUTHORIZATION = "Authorization";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String BEARER_TEST_TOKEN = "Bearer testToken";
    private static final String ENCODING_UTF = "UTF-8";
    private static final String ENCODING_UNSUPPORTED = "unsupported";
    private static final String POST = "POST";
    private static final String GET = "GET";
    private Mockery mockery;
    private OAuth20AuthenticatorImpl authenticator;
    private OAuth20Provider oauth20provider;
    private OAuthResult providerResult;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private Map<String, String[]> paramMap;

    private class OAuth20AuthenticatorImplTestDouble extends OAuth20AuthenticatorImpl {

        private final OAuth20Provider provider;

        public OAuth20AuthenticatorImplTestDouble(OAuth20Provider provider) {
            this.provider = provider;
        }

        @Override
        protected List<OAuth20Provider> getProviders(HttpServletRequest req) {
            List<OAuth20Provider> output = new ArrayList<OAuth20Provider>();
            output.add(provider);
            return output;
        }
    }

    private class OAuth20AuthenticatorImplTestMultipleProviders extends OAuth20AuthenticatorImpl {

        private final OAuth20Provider provider;

        public OAuth20AuthenticatorImplTestMultipleProviders(OAuth20Provider provider) {
            this.provider = provider;
        }

        @Override
        protected List<OAuth20Provider> getProviders(HttpServletRequest req) {
            List<OAuth20Provider> output = new ArrayList<OAuth20Provider>();
            output.add(provider);
            output.add(new LibertyOAuth20Provider());
            return output;
        }
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockery = new JUnit4Mockery();
        oauth20provider = mockery.mock(OAuth20Provider.class);
        req = mockery.mock(HttpServletRequest.class);
        authenticator = new OAuth20AuthenticatorImplTestDouble(oauth20provider);
        paramMap = mockery.mock(Map.class);
    }

    @Test
    public void authenticate_noCharacterEncoding_error_serverError_tokenRequestParameterType_Continue() {
        createProviderExpectations(null, true);
        createRequestMethodExpectations(POST);
        createRequestParameterMapWithErrorExpectations(UtilConstants.ERROR, new String[] { OAuth20Exception.SERVER_ERROR });
        createTokenRequestExpectations(null, PARAMETER, UtilConstants.AUTHORIZATION_CODE);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_error_serverError_tokenRequestParameterType_Continue_GET() {
        createProviderExpectations(null, true);
        createRequestMethodExpectations(GET);
        createRequestParameterMapWithErrorExpectations(UtilConstants.ERROR, new String[] { OAuth20Exception.SERVER_ERROR });
        createTokenRequestExpectations(null, PARAMETER, UtilConstants.AUTHORIZATION_CODE);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestParameterType_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, PARAMETER, UtilConstants.AUTHORIZATION_CODE);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestHeaderAuthorizationCode_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, HEADER, UtilConstants.AUTHORIZATION_CODE);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestParameterToken_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, PARAMETER, UtilConstants.TOKEN);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestHeaderToken_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, HEADER, UtilConstants.TOKEN);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestParameterPassword_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, PARAMETER, UtilConstants.PASSWORD);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestHeaderPassword_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, HEADER, UtilConstants.PASSWORD);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestParameteCredentials_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, PARAMETER, UtilConstants.CLIENT_CREDENTIALS);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_tokenRequestHeaderCredentials_Continue() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createTokenRequestExpectations(null, HEADER, UtilConstants.CLIENT_CREDENTIALS);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_notOAuthOnly_noOAuthToken_Continue() {
        createProviderExpectations(null, false);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        createNoOAuthTokenExpectations();
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_utfProviderCharacterEncoding_notOAuthOnly_noOAuthToken_Continue() {
        createProviderExpectations(ENCODING_UTF, false);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        try {
            createRequestSetCharacterEncodingExpectations(ENCODING_UTF, false);
        } catch (UnsupportedEncodingException e) {
            fail("Test threw an unexpected UnsupportedEncodingException when setting the request character encoding: " + e);
        }
        createNoOAuthTokenExpectations();
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_unsupportedProviderCharacterEncoding_notOAuthOnly_noOAuthToken_Continue() {
        createProviderExpectations(ENCODING_UNSUPPORTED, false);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        try {
            createRequestSetCharacterEncodingExpectations(ENCODING_UNSUPPORTED, true);
        } catch (UnsupportedEncodingException e) {
            fail("Test threw an unexpected UnsupportedEncodingException when setting the request character encoding: " + e);
        }
        createNoOAuthTokenExpectations();
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_multipleProviders() {
        authenticator = new OAuth20AuthenticatorImplTestMultipleProviders(oauth20provider);
        createProviderExpectations(null, false);
        createNonErrorPostRequestExpectations();
        createMultipleProvidersExpectations();
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be FAILURE.", AuthResult.FAILURE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_notOAuthOnly_oauthTokenBearer_TokenRequest_Continue() {
        createProviderExpectations(null, false);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        createOAuthTokenExpectations(AUTHORIZATION, BEARER_TEST_TOKEN);
        createTokenRequestExpectations(null, HEADER, UtilConstants.AUTHORIZATION_CODE);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_notOAuthOnly_oauthTokenAccessTokenInHeader_TokenRequest_Continue() {
        createProviderExpectations(null, false);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        createOAuthTokenExpectations(HEADER, "testToken");
        createTokenRequestExpectations(null, HEADER, UtilConstants.AUTHORIZATION_CODE);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_notOAuthOnly_oauthTokenAccessTokenInParameter_TokenRequest_Continue() {
        createProviderExpectations(null, false);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        createOAuthTokenExpectations(PARAMETER, "testToken");
        createTokenRequestExpectations(null, HEADER, UtilConstants.AUTHORIZATION_CODE);
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be CONTINUE.", AuthResult.CONTINUE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_notTokenRequest_noOAuthToken_Failure() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        createNotTokenRequestExpectation();
        createNoOAuthTokenExpectations();
        createNoOAuthTokenRequestExpectations();
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be FAILURE.", AuthResult.FAILURE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_oauthOnly_notTokenRequest_withFailureExpectations_Failure() {
        createProviderExpectations(null, true);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        createNotTokenRequestExpectation();
        createOAuthTokenExpectations(AUTHORIZATION, BEARER_TEST_TOKEN);
        createNoOAuthTokenRequestExpectations();
        createFailedProviderResultExpectations();
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be FAILURE.", AuthResult.FAILURE, result.getStatus());
    }

    @Test
    public void authenticate_noCharacterEncoding_notOAuthOnly_protectedResource_withFailureExpectations_Failure() {
        createProviderExpectations(null, false);
        createNonErrorPostRequestExpectations();
        createRequestCharacterEncodingExpectations(null);
        createNotTokenRequestExpectation();
        createOAuthTokenExpectations(AUTHORIZATION, BEARER_TEST_TOKEN);
        createNoOAuthTokenRequestExpectations();
        createFailedProviderResultExpectations();
        ProviderAuthenticationResult result = authenticator.authenticate(req, res);

        assertEquals("The result status must be FAILURE.", AuthResult.FAILURE, result.getStatus());
    }

    // TODO: Add tests for successful results. OAuth Only with not token request. Not OAuth Only with protected resource.

    private void createProviderExpectations(final String encoding, final boolean oauthOnly) {
        mockery.checking(new Expectations() {
            {
                allowing(oauth20provider).getCharacterEncoding();
                will(returnValue(encoding));
                allowing(oauth20provider).isOauthOnly();
                will(returnValue(oauthOnly));
            }
        });
    }

    private void createMultipleProvidersExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(oauth20provider).getID();
                will(returnValue("id"));
            }
        });
    }

    private void createNotTokenRequestExpectation() {
        createTokenRequestExpectations(null, null, null);
    }

    private void createTokenRequestExpectations(final String encoding, final String typeLocation, final String type) {
        createRequestCharacterEncodingExpectations(encoding);

        if (type != UtilConstants.TOKEN) {
            createRequestNullValueExpectation(UtilConstants.RESPONSE_TYPE);

            if (typeLocation == PARAMETER) {
                createRequestParameterExpectation(UtilConstants.GRANT_TYPE, type);
            } else if (typeLocation == HEADER) {
                createRequestHeaderExpectation(UtilConstants.GRANT_TYPE, type);
            } else {
                createRequestNullValueExpectation(UtilConstants.GRANT_TYPE);
            }
        } else {
            createRequestNullValueExpectation(UtilConstants.GRANT_TYPE);

            if (typeLocation == PARAMETER) {
                createRequestParameterExpectation(UtilConstants.RESPONSE_TYPE, type);
            } else if (typeLocation == HEADER) {
                createRequestHeaderExpectation(UtilConstants.RESPONSE_TYPE, type);
            } else {
                createRequestNullValueExpectation(UtilConstants.RESPONSE_TYPE);
            }
        }
    }

    private void createRequestCharacterEncodingExpectations(final String encoding) {
        mockery.checking(new Expectations() {
            {
                allowing(req).getCharacterEncoding();
                will(returnValue(encoding));
            }
        });
    }

    private void createRequestSetCharacterEncodingExpectations(final String encoding, final boolean willThrowException) throws UnsupportedEncodingException {
        if (willThrowException) {
            mockery.checking(new Expectations() {
                {
                    allowing(req).setCharacterEncoding(encoding);
                    will(throwException(new UnsupportedEncodingException()));
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    allowing(req).setCharacterEncoding(encoding);
                }
            });
        }
    }

    private void createRequestNullValueExpectation(final String name) {
        mockery.checking(new Expectations() {
            {
                allowing(req).getParameter(name);
                will(returnValue(null));
                allowing(req).getHeader(name);
                will(returnValue(null));
            }
        });
    }

    private void createRequestParameterExpectation(final String name, final String value) {
        mockery.checking(new Expectations() {
            {
                allowing(req).getParameter(name);
                will(returnValue(value));
                allowing(req).getHeader(name);
                will(returnValue(null));
            }
        });
    }

    private void createRequestHeaderExpectation(final String name, final String value) {
        mockery.checking(new Expectations() {
            {
                allowing(req).getParameter(name);
                will(returnValue(null));
                allowing(req).getHeader(name);
                will(returnValue(value));
            }
        });
    }

    private void createNoOAuthTokenExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(req).getHeader(AUTHORIZATION);
                will(returnValue(null));
                allowing(req).getHeader(ACCESS_TOKEN);
                will(returnValue(null));
                allowing(req).getParameter(ACCESS_TOKEN);
                will(returnValue(null));
            }
        });
    }
    
    private void createNoOAuthTokenRequestExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(req).getRequestURI();
                will(returnValue("/oidc/endpoint/op1/token"));
            }
        });
    }

    private void createOAuthTokenExpectations(final String location, final String value) {
        if (location == AUTHORIZATION) {
            mockery.checking(new Expectations() {
                {
                    allowing(req).getHeader(AUTHORIZATION);
                    will(returnValue(value));
                }
            });
        } else if (location == HEADER) {
            mockery.checking(new Expectations() {
                {
                    allowing(req).getHeader(AUTHORIZATION);
                    will(returnValue(null));
                    allowing(req).getHeader(ACCESS_TOKEN);
                    will(returnValue(value));
                    allowing(req).getParameter(ACCESS_TOKEN);
                    will(returnValue(null));
                }
            });
        } else if (location == PARAMETER) {
            mockery.checking(new Expectations() {
                {
                    allowing(req).getHeader(AUTHORIZATION);
                    will(returnValue(null));
                    allowing(req).getHeader(ACCESS_TOKEN);
                    will(returnValue(null));
                    allowing(req).getParameter(ACCESS_TOKEN);
                    will(returnValue(value));
                }
            });
        }
    }

    private void createFailedProviderResultExpectations() {
        createFailedProviderResponse();
        createHttpServletResponseExpectations();
        createProviderProcessResourceRequestExpectations();
    }

    private void createFailedProviderResponse() {
        providerResult = mockery.mock(OAuthResult.class);
        mockery.checking(new Expectations() {
            {
                allowing(providerResult).getStatus();
                will(returnValue(OAuthResult.STATUS_FAILED));
                allowing(providerResult).getCause();
                will(returnValue(null));
            }
        });
    }

    private void createHttpServletResponseExpectations() {
        res = mockery.mock(HttpServletResponse.class);
        // Maybe should set value to assert correct message being set
        mockery.checking(new Expectations() {
            {
                allowing(res).setHeader(with("WWW-Authenticate"), with(any(String.class)));
            }
        });
    }

    private void createProviderProcessResourceRequestExpectations() {
        mockery.checking(new Expectations() {
            {
                one(oauth20provider).processResourceRequest(req);
                will(returnValue(providerResult));
            }
        });
    }

    private void createNonErrorPostRequestExpectations() {
        createRequestMethodExpectations(POST);
        createRequestParameterMapWithoutErrorExpectations();
    }

    private void createRequestParameterMapWithoutErrorExpectations() {
        mockery.checking(new Expectations() {
            {
                one(req).getParameterMap();
                will(returnValue(paramMap));
                one(paramMap).get(with(any(String.class)));
                will(returnValue(null));
            }
        });
    }

    private void createRequestParameterMapWithErrorExpectations(final String param, final String[] value) {
        mockery.checking(new Expectations() {
            {
                one(req).getParameterMap();
                will(returnValue(paramMap));
                one(paramMap).get(param);
                will(returnValue(value));
            }
        });
    }

    private void createRequestMethodExpectations(final String reqMethod) {
        mockery.checking(new Expectations() {
            {
                one(req).getMethod();
                will(returnValue(reqMethod));
            }
        });
    }
}
