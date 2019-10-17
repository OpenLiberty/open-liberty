/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.BoundedCommonCache;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.common.cl.JWTVerifier;
import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

public class GrantTypeCustomizeHandlerJwtImplTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final JwtGrantTypeHandlerConfig mockJwtConfig = mock.mock(JwtGrantTypeHandlerConfig.class, "mockJwtConfig");
    final OAuth20Provider mockConfig = mock.mock(OAuth20Provider.class, "mockConfig");
    final SecurityService mockSecurityService = mock.mock(SecurityService.class, "mockSecurityService");
    final AttributeList mockAttrList = mock.mock(AttributeList.class, "mockAttrList");
    final OidcServerConfig mockOidcServerConfig = mock.mock(OidcServerConfig.class, "mockOidcServerConfig");
    final List<Attribute> mockAttributes = mock.mock(List.class, "mockAttributes");
    final Iterator mockIterator = mock.mock(Iterator.class, "mockIterator");
    final Attribute mockAttribute = mock.mock(Attribute.class, "mockAttribute");
    final OAuth20TokenFactory mockTokenFactory = mock.mock(OAuth20TokenFactory.class, "mockTokenFactory");
    final OAuth20Token mockAccessToken = mock.mock(OAuth20Token.class, "mockAccessToken");
    final JWTPayload mockPayload = mock.mock(JWTPayload.class, "mockPayload");
    final UserRegistryService mockUserRegistryService = mock.mock(UserRegistryService.class, "mockUserRegistryService");
    final UserRegistry mockUserRegistry = mock.mock(UserRegistry.class, "mockUserRegistry");
    final JWTVerifier mockJwtVerifier = mock.mock(JWTVerifier.class, "mockJwtVerifier");
    final PublicKey mockPublicKey = mock.mock(PublicKey.class, "mockPublicKey");

    final String providerId = "testProviderId";
    final String clientId = "client03";
    final String client_secret = "client03pwd";
    final String signatureAlgorithm = "HS256";
    final String redirectUri1 = "https://localhost:8020/test/redirect";
    final String redirectUri2 = "https://localhost:8021/test/redirect";
    final String[] redirectUris = new String[] { redirectUri1 };
    final String userName = "user1";
    final String[] scopes = new String[] { "openid", "profile" };
    final Map<String, String[]> accessTokenMap = new HashMap<String, String[]>();
    final String issuerIdentifier = "opIssuerIdentifier";
    final String oidcTokenEndpoint = "https://localhost:8045/oidc/endpoint/unitoidc/token";
    final String oauthTokenEndpoint = "https://localhost:8045/oauth2/providers/unitpauth/token";
    final String tokenHS256 = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImF1dG9rZXlpZCJ9.eyJpc3MiOiJjbGllbnQ" +
                              "wMSIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUiLCJpYXQiOjE0MDc4NjcwNDAsImV" +
                              "4cCI6MTQwNzg3NDI0MCwic3ViIjoidGVzdHVzZXIiLCJhdWQiOiJodHRwczovL2x" +
                              "vY2FsaG9zdDo4OTQ1L29hdXRoMi9wcm92aWRlcnMvT0F1dGhDb25maWdTYW1wbGU" +
                              "ifQ.odcaquc4HvsmVS8Z8VtZHdln6h0E0ZBMwNPDPrSFcTE";
    final String tokenRS256 = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImtleWlkcnMyNTYifQ.eyJpc3MiOiJjbGllb" +
                              "nQwMSIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUiLCJpYXQiOjE0MDc4NjcxMzYsI" +
                              "mV4cCI6MTQwNzg3NDMzNiwic3ViIjoidGVzdHVzZXIiLCJhdWQiOiJodHRwczovL" +
                              "2xvY2FsaG9zdDo4OTQ1L29pZGMvZW5kcG9pbnQvT2lkY0NvbmZpZ1NhbXBsZSJ9." +
                              "Z8DMQeA0zPEr-Bv2rx9W1_Lf_4mQCT8Z-byoI0TbwF8Q2l4mJ1otwnW8JH7J2ma8" +
                              "V9aO275kxVmObIgWiJo25SoSnlIkng72yLwB2e50xpUQk0U5nVPbdZ0atWPJDA9a" +
                              "d-VaaG1H-9LVyHrUMROaFQVE0qjO5L6un4amBbyIdSFnjY-q2llhOyHram3KvP1_" +
                              "RHv7VePTEWu7UMptfv1mHPD90j7TBG5rdmiBr3i_PNo1x2aiCcqz9IYuu3ayo-z4" +
                              "2SSz7Oa8B-14SjvPIUTpren9TsW9Os_Az3tkisO51yTCTHZFwrCovrz3MrzKLaXW" +
                              "P4tcm_As8Z9yJV5z-vo9dg";

    String issuerCompany = "ibm.com";
    String keyId = "9876543";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.trace("*=all");
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testGetKeysGrantType() {
        final String methodName = "testGetKeysGrantType";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl jwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            jwtHandler.setHandlerInfo("testJwtHandler", mockConfig);
            List<String> list = jwtHandler.getKeysGrantType(new AttributeList());
            assertEquals("The list is not the _emptyList", list, GrantTypeCustomizedHandlerJwtImpl._emptyList);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateRequestGrantType() {
        final String methodName = "testvalidateRequestGrantType";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            mockGrantTypeCustomizedHandlerJwtImpl jwtHandler = new mockGrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;

            final String clientSecret = "secretsecretsecretsecretsecretsecret"; // need >=256bit key for jose4j
            JWT jwtToken = createJwtString(issuerCompany, keyId, clientSecret);
            jwtHandler.setGetOidcServerConfig(true);
            final String strToken = jwtToken.getSignedJWTString();
            jwtHandler.setJwtTokenString(strToken);
            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAllAttributes();
                    will(returnValue(mockAttributes));
                    one(mockAttributes).iterator();
                    will(returnValue(mockIterator));
                    one(mockIterator).hasNext();
                    will(returnValue(true));
                    one(mockIterator).next();
                    will(returnValue(mockAttribute));
                    one(mockAttribute).getName();
                    will(returnValue("testKey"));
                    one(mockIterator).hasNext();
                    will(returnValue(false));

                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecret));
                    one(mockJwtConfig).getProviderId();
                    will(returnValue(providerId));
                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.REQUEST_FEATURE);
                    will(returnValue(OAuth20Constants.REQUEST_FEATURE_OIDC));
                    one(mockAttrList).getAttributeValuesByName(OIDCConstants.ASSERTION);
                    will(returnValue(new String[] { strToken }));
                    one(mockJwtConfig).getJwtClockSkew();
                    will(returnValue(300L));
                }
            });
            jwtHandler.validateRequestGrantType(mockAttrList, new ArrayList<OAuth20Token>());
            // no exception is what we expect
        } catch (OAuthException e) {
            outputMgr.failWithThrowable(methodName, e);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateRequestGrantTypeFailSignature() {
        final String methodName = "testvalidateRequestGrantTypeFailSignature";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            mockGrantTypeCustomizedHandlerJwtImpl jwtHandler = new mockGrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;

            final String clientSecret = "secretsecretsecretsecretsecretsecret"; // need >=256bit key for jose4j
            JWT jwtToken = createJwtString(issuerCompany, keyId, clientSecret);
            jwtHandler.setGetOidcServerConfig(true);
            String strTokenTmp = jwtToken.getSignedJWTString();
            final String strToken = strTokenTmp.substring(0, strTokenTmp.length() - 4) + "WXYZ"; // mess up signature
            jwtHandler.setJwtTokenString(strToken);
            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAllAttributes();
                    will(returnValue(mockAttributes));
                    one(mockAttributes).iterator();
                    will(returnValue(mockIterator));
                    one(mockIterator).hasNext();
                    will(returnValue(true));
                    one(mockIterator).next();
                    will(returnValue(mockAttribute));
                    one(mockAttribute).getName();
                    will(returnValue("testKey"));;
                    one(mockIterator).hasNext();
                    will(returnValue(false));

                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecret));
                    one(mockJwtConfig).getProviderId();
                    will(returnValue(providerId));
                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.REQUEST_FEATURE);
                    will(returnValue(OAuth20Constants.REQUEST_FEATURE_OIDC));
                    one(mockAttrList).getAttributeValuesByName(OIDCConstants.ASSERTION);
                    will(returnValue(new String[] { strToken }));
                    one(mockJwtConfig).getJwtClockSkew();
                    will(returnValue(300L));
                }
            });
            jwtHandler.validateRequestGrantType(mockAttrList, new ArrayList<OAuth20Token>());
        } catch (OAuthException e) {
            // Yes, we expect this OAuthException
            return;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " is supposed to throw OAuthException because we mess up the signature of JWT Token");
    }

    @Test
    public void testBuildTokensGrantType() {
        final String methodName = "testBuildTokensGrantType";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            mockGrantTypeCustomizedHandlerJwtImpl jwtHandler = new mockGrantTypeCustomizedHandlerJwtImpl();
            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                    will(returnValue(clientId));
                    one(mockAttrList).getAttributeValuesByNameAndType(OAuth20Constants.REDIRECT_URI,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_BODY);
                    will(returnValue(redirectUris));
                    one(mockAttrList).getAttributeValuesByNameAndType(OAuth20Constants.CLAIM_NAME_SCOPE, OAuth20Constants.ATTRTYPE_PARAM_JWT);
                    will(returnValue(scopes));
                    one(mockAttrList).getAttributeValueByNameAndType(OAuth20Constants.CLAIM_NAME_SUB, OAuth20Constants.ATTRTYPE_PARAM_JWT);
                    will(returnValue(userName));
                    one(mockAttrList).getAttributeValueByName("issuerIdentifier");
                    will(returnValue(issuerIdentifier));
                    one(mockAttrList).getAttributeValueByNameAndType(OAuth20Constants.CLIENT_SECRET,
                                                                     OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
                    will(returnValue(client_secret));
                    one(mockAttrList).getAttributeValuesByNameAndType(OAuth20Constants.RESOURCE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
                    will(returnValue(new String[] { "client01", "client02" }));
                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.PROXY_HOST);
                    will(returnValue("proxyHost"));
                    one(mockTokenFactory).buildTokenMap(clientId, userName, redirectUri1,
                                                        (String) null, scopes, (OAuth20Token) null, OAuth20Constants.GRANT_TYPE_JWT);
                    will(returnValue(accessTokenMap));
                    one(mockTokenFactory).createAccessToken(accessTokenMap);
                    will(returnValue(mockAccessToken));
                }
            });
            List<OAuth20Token> tokens = jwtHandler.buildTokensGrantType(mockAttrList,
                                                                        mockTokenFactory,
                                                                        new ArrayList<OAuth20Token>());
            assertTrue("The size of tokens is 1", tokens.size() == 1);
            OAuth20Token token = tokens.get(0);
            assertEquals("Did not get back the right OAuth20Token", token, mockAccessToken);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testVerifyJwtContentAndAddAttrib() {
        final String methodName = "testVerifyJwtContentAndAddAttrib";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            mockGrantTypeCustomizedHandlerJwtImpl jwtHandler = new mockGrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            jwtHandler.setTestVerifyJwtContentAndAddAttrib(true);
            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAttributeValueByName(OIDCConstants.ISSUER_IDENTIFIER);
                    will(returnValue(issuerIdentifier));
                    one(mockAttrList).getAttributeValuesByNameAndType(OIDCConstants.CLIENT_REDIRECT_URI,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
                    will(returnValue(redirectUris));
                    one(mockPayload).get(OIDCConstants.PAYLOAD_ISSUER);
                    will(returnValue(clientId));
                    one(mockPayload).get(OIDCConstants.PAYLOAD_SUBJECT);
                    will(returnValue(userName));

                    allowing(mockAttrList).setAttribute(with(any(String.class)), with(any(String.class)), with(any(String[].class)));

                    one(mockPayload).get(OIDCConstants.PAYLOAD_AUDIENCE);
                    will(returnValue(issuerIdentifier));

                    one(mockPayload).get(OIDCConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
                    will(returnValue(Long.valueOf((new Date()).getTime() / 1000 + 300))); // expired after 5 minutes

                    one(mockPayload).get(OIDCConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
                    will(returnValue(Long.valueOf((new Date()).getTime() / 1000 - 300))); // issued 5 minutes ago
                    one(mockJwtConfig).isJwtIatRequired();
                    will(returnValue(true));
                    one(mockJwtConfig).getJwtTokenMaxLifetime();
                    will(returnValue(7200L));

                    one(mockPayload).get(OIDCConstants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS);
                    will(returnValue(Long.valueOf((new Date()).getTime() / 1000 - 180))); // 3 minute ago (current time already after (not-before))

                    one(mockPayload).get(OIDCConstants.PAYLOAD_JWTID);
                    will(returnValue("UNIQUE1"));
                    one(mockJwtConfig).getJtiCache();
                    will(returnValue(new BoundedCommonCache<String>(100)));

                    one(mockAttrList).getAttributeValuesByNameAndType(OAuth20Constants.SCOPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);
                    will(returnValue(scopes));
                }
            });
            jwtHandler.verifyJwtContentAndAddAttrib(mockAttrList, mockPayload, clientId, (OidcServerConfig) null,
                                                    300L);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testVerifyJwtContentAndAddAttribDuplicateJti() {
        final String methodName = "testVerifyJwtContentAndAddAttrib";
        System.out.println("----------------" + methodName + "-----------------");
        String jtiKey = clientId + " - " + "UNIQUE2";
        try {
            mockGrantTypeCustomizedHandlerJwtImpl jwtHandler = new mockGrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            jwtHandler.setTestVerifyJwtContentAndAddAttrib(true);
            final BoundedCommonCache<String> cache = new BoundedCommonCache<String>(100);
            cache.put(jtiKey);
            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAttributeValueByName(OIDCConstants.ISSUER_IDENTIFIER);
                    will(returnValue(issuerIdentifier));
                    one(mockAttrList).getAttributeValuesByNameAndType(OIDCConstants.CLIENT_REDIRECT_URI,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
                    will(returnValue(redirectUris));
                    one(mockPayload).get(OIDCConstants.PAYLOAD_ISSUER);
                    will(returnValue(clientId));
                    one(mockPayload).get(OIDCConstants.PAYLOAD_SUBJECT);
                    will(returnValue(userName));

                    allowing(mockAttrList).setAttribute(with(any(String.class)), with(any(String.class)), with(any(String[].class)));

                    one(mockPayload).get(OIDCConstants.PAYLOAD_AUDIENCE);
                    will(returnValue(issuerIdentifier));
                    one(mockOidcServerConfig).getIssuerIdentifier();
                    will(returnValue(issuerIdentifier));

                    one(mockPayload).get(OIDCConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
                    will(returnValue(Long.valueOf((new Date()).getTime() / 1000 + 300))); // expired after 5 minutes

                    one(mockPayload).get(OIDCConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
                    will(returnValue(Long.valueOf((new Date()).getTime() / 1000 - 300))); // issued 5 minutes ago
                    one(mockJwtConfig).isJwtIatRequired();
                    will(returnValue(true));
                    one(mockJwtConfig).getJwtTokenMaxLifetime();
                    will(returnValue(7200L));

                    one(mockPayload).get(OIDCConstants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS);
                    will(returnValue(Long.valueOf((new Date()).getTime() / 1000 - 180))); // 3 minute ago (current time already after (not-before))

                    one(mockPayload).get(OIDCConstants.PAYLOAD_JWTID);
                    will(returnValue("UNIQUE2"));
                    one(mockJwtConfig).getJtiCache();
                    will(returnValue(cache));

                }
            });
            jwtHandler.verifyJwtContentAndAddAttrib(mockAttrList, mockPayload,
                                                    clientId,
                                                    mockOidcServerConfig,
                                                    300L);
        } catch (OAuthException e) {
            // Yes, we expect this OAuthException
            return;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " sHould have fail, because jwtKey " + jtiKey + " already in the cache. ");
    }

    @Test
    public void testIsInUserRegistry() {
        final String methodName = "testIsInUserRegistry";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            mock.checking(new Expectations() {
                {
                    one(mockJwtConfig).getSecurityService();
                    will(returnValue(mockSecurityService));
                    one(mockSecurityService).getUserRegistryService();
                    will(returnValue(mockUserRegistryService));
                    one(mockUserRegistryService).getUserRegistry();
                    will(returnValue(mockUserRegistry));

                    one(mockUserRegistry).isValidUser(userName);
                    will(returnValue(true));
                }
            });
            boolean isUserOk = realJwtHandler.isInUserRegistry(userName);
            assertTrue("isInUserRegistry does not return true as expected",
                       isUserOk);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetTokenString() {
        final String methodName = "testGetTokenString";
        System.out.println("----------------" + methodName + "-----------------");
        boolean bTestOneToken = false;
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            final String tokenString1 = "fakeTokenString1";
            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAttributeValuesByName(OIDCConstants.ASSERTION);
                    will(returnValue(new String[] { tokenString1 }));

                    one(mockAttrList).getAttributeValuesByName(OIDCConstants.ASSERTION);
                    will(returnValue(new String[] { tokenString1, "fakedTokenString2" }));
                }
            });
            assertEquals("Does not get TokenString1", tokenString1, realJwtHandler.getTokenString(mockAttrList));
            bTestOneToken = true;
            realJwtHandler.getTokenString(mockAttrList); // this ought to fail because 2 toke strings
        } catch (OAuthException e) {
            assertTrue("test only one tokenString failed before throws Exception", bTestOneToken);
            return; // expect it to fail when run with 2 tokenStrings
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed because we run the getTokenString twice. The second it has 2 tokenStrings but not");
    }

    @Test
    public void testGetTokenStringNull() {
        final String methodName = "testGetTokenString";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAttributeValuesByName(OIDCConstants.ASSERTION);
                    will(returnValue((String[]) null));
                }
            });
            realJwtHandler.getTokenString(mockAttrList); // this ought to fail because 2 toke strings
        } catch (OAuthException e) {
            return; // expect it to fail when run with 2 tokenStrings
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed because we run the getTokenString twice. The second it has 2 tokenStrings but not");
    }

    @Test
    public void testGetOidcServerConfigOAuth() {
        final String methodName = "testGetOidcServerConfigOAuth";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl jwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;

            mock.checking(new Expectations() {
                {
                    one(mockAttrList).getAttributeValueByName(OAuth20Constants.REQUEST_FEATURE);
                    will(returnValue(OAuth20Constants.REQUEST_FEATURE_OAUTH2));
                }
            });
            OidcServerConfig oidcServerConfig = jwtHandler.getOidcServerConfig(mockAttrList);
            assertNull("SHould not get an oidcServerConfig", oidcServerConfig);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetJwtVerifierOidcRS256() {
        final String methodName = "testGetJwtVerifierOidcRS256";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl jwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            mock.checking(new Expectations() {
                {
                    one(mockOidcServerConfig).getSignatureAlgorithm();
                    will(returnValue(OAuth20Constants.SIGNATURE_ALGORITHM_RS256));
                    one(mockOidcServerConfig).getPublicKey(with(any(String.class)));
                    will(returnValue(mockPublicKey));
                }
            });
            JWTVerifier jwtVerifier = jwtHandler.getJwtVerifier(clientId,
                                                                client_secret,
                                                                tokenRS256,
                                                                300L,
                                                                mockOidcServerConfig);
            assertNotNull("Should not get an jwtVerifier", jwtVerifier);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetJwtVerifierOauthRS256Bad() {
        final String methodName = "testGetJwtVerifierOauthRS256Bad";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl jwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            JWTVerifier jwtVerifier = jwtHandler.getJwtVerifier(clientId,
                                                                client_secret,
                                                                tokenRS256,
                                                                300L,
                                                                null);
            fail("Should have received an Exception");
        } catch (Throwable t) {
            // this is what we expect
            return;
        }

    }

    @Test
    public void testGetJwtVerifierOidcHS256() {
        final String methodName = "testGetJwtVerifierOidcHS256";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl jwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            jwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            mock.checking(new Expectations() {
                {
                    one(mockOidcServerConfig).getSignatureAlgorithm();
                    will(returnValue(OAuth20Constants.SIGNATURE_ALGORITHM_HS256));
                }
            });
            JWTVerifier jwtVerifier = jwtHandler.getJwtVerifier(clientId,
                                                                client_secret,
                                                                tokenHS256,
                                                                300L,
                                                                mockOidcServerConfig);
            assertNotNull("Should not get an jwtVerifier", jwtVerifier);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCheckIatTimeNull() {
        final String methodName = "testcheckIatTime";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            mock.checking(new Expectations() {
                {
                    one(mockJwtConfig).isJwtIatRequired();
                    will(returnValue(true));
                }
            });
            realJwtHandler.checkIatTime(null, new Date(), 300L);
        } catch (OAuthException e) {
            return; // expect it to fail since IAT is required but we give iat as null
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed because we put iatRequired as true and give iat as null");
    }

    @Test
    public void testCheckIatTime() {
        final String methodName = "testcheckIatTime";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            mock.checking(new Expectations() {
                {
                    one(mockJwtConfig).isJwtIatRequired();
                    will(returnValue(true));
                    one(mockJwtConfig).getJwtTokenMaxLifetime();
                    will(returnValue(Long.valueOf(7200)));
                }
            });
            realJwtHandler.checkIatTime((new Date()).getTime() / 1000 - 300, new Date(), 300L);
            return;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have succeeded but not");
    }

    @Test
    public void testcCheckIatTimeTooOld() {
        final String methodName = "testcheckIatTimeTooOld";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            mock.checking(new Expectations() {
                {
                    one(mockJwtConfig).isJwtIatRequired();
                    will(returnValue(true));
                    one(mockJwtConfig).getJwtTokenMaxLifetime();
                    will(returnValue(Long.valueOf(7200)));
                }
            });
            realJwtHandler.checkIatTime((new Date()).getTime() / 1000 - 8000, new Date(), 300L);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testcCheckIatTimeFuture() {
        final String methodName = "testcheckIatTimeFuture";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler._jwtGrantTypeHandlerConfig = mockJwtConfig;
            mock.checking(new Expectations() {
                {
                    one(mockJwtConfig).isJwtIatRequired();
                    will(returnValue(true));
                    one(mockJwtConfig).getJwtTokenMaxLifetime();
                    will(returnValue(Long.valueOf(7200)));
                }
            });
            realJwtHandler.checkIatTime((new Date()).getTime() / 1000 + 500, new Date(), 300L);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testVerifyExpiredTimeNull() {
        final String methodName = "testVerifyExpiredTime";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler.verifyExpiredTime((Long) null, new Date(), 300L);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testVerifyJwtAudienceNull() {
        final String methodName = "testVerifyJwtAudienceNull";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler.verifyJwtAudience((String[]) null, issuerIdentifier, oidcTokenEndpoint);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testVerifyJwtAudienceBad() {
        final String methodName = "testVerifyJwtAudienceBad";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler.verifyJwtAudience(new String[] { "badAudience", "anotherBadAudience" }, issuerIdentifier, oidcTokenEndpoint);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testVerifyJwtIssuerNull() {
        final String methodName = "testVerifyJwtIssuerNull";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler.verifyJwtIssuer((String) null, new String[] { redirectUri1, redirectUri2 }, clientId);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testVerifyJwtIssuerBad() {
        final String methodName = "testVerifyJwtIssuerBad";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler.verifyJwtIssuer(redirectUri1 + "/bad", new String[] { redirectUri1, redirectUri2 }, clientId);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testVerifyJwtSubNull() {
        final String methodName = "testVerifyJwtSubNull";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler.verifyJwtSub((String) null);
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testVerifyJwtSubBad() {
        final String methodName = "testVerifyJwtSubBad";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            mockGrantTypeCustomizedHandlerJwtImpl jwtHandler = new mockGrantTypeCustomizedHandlerJwtImpl();
            jwtHandler.setIsInUserRegistry(false);
            jwtHandler.verifyJwtSub("badUser");
        } catch (OAuthException e) {
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testGetHandlerInstance() {
        final String methodName = "testVerifyJwtSubBad";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            OAuth20GrantTypeHandler handler = realJwtHandler.getHandlerInstance();
            assertEquals("Did not get back the realJwtHandler", realJwtHandler, handler);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testVerifyExpiredTime() {
        final String methodName = "testVerifyExpiredTime";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();

            realJwtHandler.verifyExpiredTime((new Date()).getTime() / 1000, new Date(), 300L);
            return; // expect it to fail since the IAT time is too long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have succeeded but not");
    }

    @Test
    public void testVerifyExpiredTimeExpired() {
        final String methodName = "testVerifyExpiredTime";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            realJwtHandler.verifyExpiredTime((new Date()).getTime() / 1000 - 400, new Date(), 300L);
        } catch (OAuthException e) {
            return; // The exp time already expired
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testGetLong() {
        final String methodName = "testGetLong";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            mock.checking(new Expectations() {
                {
                    one(mockPayload).get(with(any(String.class)));
                    will(returnValue(Long.valueOf(800)));
                }
            });
            Long value = realJwtHandler.getLong(mockPayload, "exp");
            assertEquals("Should get value 800 but not", value.longValue(), 800L);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetStringM() {
        final String methodName = "testGetStringM";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            String value1 = realJwtHandler.getString(new String[] { "openid", "profile" });
            assertEquals("Should get value openid but not", value1, "openid");
            String value2 = realJwtHandler.getString(Long.valueOf(88L));
            assertEquals("Should get value 88 but not", value2, "88");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetStringMEmpty() {
        final String methodName = "testGetStringMEmpty";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            String value = realJwtHandler.getString(new String[] {});
            assertEquals("Should get value openid but not", value, null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetLongInteger() {
        final String methodName = "testGetLongInterger";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            mock.checking(new Expectations() {
                {
                    one(mockPayload).get(with(any(String.class)));
                    will(returnValue(Integer.valueOf(800)));
                }
            });
            Long value = realJwtHandler.getLong(mockPayload, "exp");
            assertEquals("Should get value 800 but not", value.longValue(), 800L);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetLongString() {
        final String methodName = "testGetLongString";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            mock.checking(new Expectations() {
                {
                    one(mockPayload).get(with(any(String.class)));
                    will(returnValue("800"));
                }
            });
            Long value = realJwtHandler.getLong(mockPayload, "exp");
            assertEquals("Should get value 800 but not", value.longValue(), 800L);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetLongStrings() {
        final String methodName = "testGetLongStrings";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            mock.checking(new Expectations() {
                {
                    one(mockPayload).get(with(any(String.class)));
                    will(returnValue(new String[] { "800" }));
                }
            });
            Long value = realJwtHandler.getLong(mockPayload, "exp");
            assertEquals("Should get value 800 but not", value.longValue(), 800L);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetLongBadString() {
        final String methodName = "testGetLongBadString";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            mock.checking(new Expectations() {
                {
                    one(mockPayload).get(with(any(String.class)));
                    will(returnValue("not800"));
                }
            });
            realJwtHandler.getLong(mockPayload, "exp");
        } catch (OAuthException e) {
            return; // expect it to fail because not800 is not a legal value of long
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    @Test
    public void testGetStrings() {
        final String methodName = "testGetStringM";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            String[] value = realJwtHandler.getStrings((String[]) null);
            assertTrue("Should get null but not", value == null);
            value = realJwtHandler.getStrings(new String[] { "openid", "profile" });
            assertEquals("Should get 2 strings", value.length, 2);
            value = realJwtHandler.getStrings("openid");
            assertEquals("Should get openid but not", value[0], "openid");
            List list = new ArrayList();
            list.add("array2");
            list.add(Integer.valueOf(2014));
            value = realJwtHandler.getStrings(list);
            assertEquals("Should get array2 but not", value[0], "array2");
            assertEquals("should get 2014 but not", value[1], "2014");
            value = realJwtHandler.getStrings(Long.valueOf(20140716));
            assertEquals("should get 20140716 but not", value[0], "20140716");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConvertToString() {
        final String methodName = "testConvertToString";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            String value = realJwtHandler.convertToString(new String[] { "openid", "profile" });
            assertEquals("Should get strings", value, "openid profile");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testcheckNotBeforeTime() {
        final String methodName = "testcheckNotBeforeTime";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            Date currentDate = new Date();
            long currentTimeSeconds = currentDate.getTime() / 1000;
            currentDate.setTime(currentTimeSeconds * 1000 + 1);
            long lNbfSeconds = currentTimeSeconds + 300;
            // The time already passed current time, so current time is invalid
            // but the clock skew is 300, so currentTime is 1 mill_second OK
            realJwtHandler.checkNotBeforeTime(Long.valueOf(lNbfSeconds), currentDate, 300);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void testcheckNotBeforeTime1MillSecondBad() {
        final String methodName = "testcheckNotBeforeTime1MillSecondBad";
        System.out.println("----------------" + methodName + "-----------------");
        try {
            GrantTypeCustomizedHandlerJwtImpl realJwtHandler = new GrantTypeCustomizedHandlerJwtImpl();
            Date currentDate = new Date();
            long currentTimeSeconds = currentDate.getTime() / 1000;
            currentDate.setTime(currentTimeSeconds * 1000 - 1);
            long lNbfSeconds = currentTimeSeconds + 300;
            // The time already passed current time, so current time is invalid
            // even the clock skew is 300, so currentTime is 1 mill_second too earilier
            realJwtHandler.checkNotBeforeTime(Long.valueOf(lNbfSeconds), currentDate, 300);
        } catch (OAuthException e) {
            return;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        fail(methodName + " should have failed but not");
    }

    // ----- tools ---
    public JWT createJwtString(String issuer, String keyId, String clientSecret) {
        JWT output = null;
        try {
            JWSHeader header = new JWSHeader();
            header.setAlgorithm("HS256");
            header.setKeyId(keyId);

            JWTPayload payload = new JWTPayload();

            payload.setIssuer(issuerCompany);

            byte[] keyBytes = clientSecret.getBytes("UTF-8");
            output = new JWT(header, payload, keyBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    class mockGrantTypeCustomizedHandlerJwtImpl extends GrantTypeCustomizedHandlerJwtImpl {
        boolean testVerifyJwtContentAndAddAttrib = false;
        boolean testGetTokenString = true;
        String strJwtToken = null;
        boolean bIsInUserRegistry = true;
        boolean bGetOidcServerConfig = false;

        void setIsInUserRegistry(boolean bIs) {
            bIsInUserRegistry = bIs;
        }

        void setGetOidcServerConfig(boolean bTest) {
            bGetOidcServerConfig = bTest;
        }

        void setTestVerifyJwtContentAndAddAttrib(boolean test) {
            testVerifyJwtContentAndAddAttrib = test;
        }

        void setJwtTokenString(String jwtTokenString) {
            strJwtToken = jwtTokenString;
        }

        @Override
        protected OidcServerConfig getOidcServerConfig(AttributeList attributeList) throws OAuthException {
            if (bGetOidcServerConfig) {
                return super.getOidcServerConfig(attributeList);
            } else {
                return mockOidcServerConfig;
            }
        }

        @Override
        protected void verifyJwtContentAndAddAttrib(@Sensitive AttributeList attributeList, JWTPayload payload, String clientId, OidcServerConfig oidcServerConfig,
                                                    long lSkewSeconds) throws OAuthException {
            if (testVerifyJwtContentAndAddAttrib) {
                super.verifyJwtContentAndAddAttrib(attributeList, payload, clientId, oidcServerConfig, lSkewSeconds);
            }
        }

        @Override
        protected String getTokenString(AttributeList attributeList) throws OAuthException {
            if (testGetTokenString) {
                return super.getTokenString(attributeList);
            } else {
                try {
                    return strJwtToken;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        @Override
        protected boolean isInUserRegistry(String user) throws OAuth20Exception {
            return bIsInUserRegistry;
        }
    }

}
