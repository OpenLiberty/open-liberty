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
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.junit.Test;

import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.responsetype.impl.OAuth20ResponseTypeHandlerTokenImpl;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

import test.common.SharedOutputManager;

public class OIDCResponseTypeHandlerImplicitImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final AttributeList attributeList = mock.mock(AttributeList.class, "attributeList");
    final OAuth20TokenFactory oauth20TokenFactory = mock.mock(OAuth20TokenFactory.class, "oauth20ToknFactory");
    final OAuth20Token code = mock.mock(OAuth20Token.class, "code");
    final Map<String, String[]> accessTokenMap = mock.mock(Map.class, "accessTokenmap");
    final OAuth20Token access = mock.mock(OAuth20Token.class, "access");
    final OAuth20Token accessToken = mock.mock(OAuth20Token.class, "accessToken");
    final OAuth20Token refresh = mock.mock(OAuth20Token.class, "refresh");
    final OAuth20ComponentInternal componentInternal = mock.mock(OAuth20ComponentInternal.class, "componentInternal");
    final OAuth20ConfigProvider oauth20ConfigProvider = mock.mock(OAuth20ConfigProvider.class, "oauth20ConfigProvider");
    final OAuthComponentInstance oauthComponentInstance = mock.mock(OAuthComponentInstance.class, "oauuthComponentInstance");
    final IDTokenHandler idTokenHandler = mock.mock(IDTokenHandler.class, "idTokenhandler");
    final OidcOAuth20ClientProvider oauth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class, "oauth20ClientProvider");
    final OidcOAuth20Client oauth20Client = mock.mock(OidcOAuth20Client.class, "oauth20Client");
    final IDTokenImpl idTokenImpl = mock.mock(IDTokenImpl.class, "idTokenImpl");
    final OAuth20ResponseTypeHandlerTokenImpl oa20rthti = mock.mock(OAuth20ResponseTypeHandlerTokenImpl.class, "oa20rthti");
    final OAuth20ComponentInternal oa20ci = mock.mock(OAuth20ComponentInternal.class, "oa20ci");
    final OAuthComponentInstance oaci = mock.mock(OAuthComponentInstance.class, "oaci");
    final OAuth20ConfigProvider oa2cp = mock.mock(OAuth20ConfigProvider.class, "oa2cp");
    final OAuth20TokenTypeHandler oa2tth = mock.mock(OAuth20TokenTypeHandler.class, "oa2tth");
    final OidcOAuth20ClientProvider oa2clientprovider = mock.mock(OidcOAuth20ClientProvider.class, "oa2clientprovider");
    final BaseClient oidcBaseClient = mock.mock(OidcBaseClient.class, "oidcBaseClient");
    final OAuth20Token oa20token = mock.mock(OAuth20Token.class, "oa20token");

    final String redirectUri = "https://mine.ibm.com:8020/oidcclient/redirect";
    final String implicitResponseType = "id_token token";

    static Map<String, String[]> blueMap = new HashMap<String, String[]>();
    static {
        blueMap.put("unitNewKey", new String[] { "unitNewValue1" });
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
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
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            OIDCResponseTypeHandlerImplicitImpl oidcResponseTypeHandlerImplicitImpl = new OIDCResponseTypeHandlerImplicitImpl();
            assertNotNull("Can not instantiate an oidcResponseTypeHandlerImplicitImpl", oidcResponseTypeHandlerImplicitImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testBuildTokenResponseTypeZero() {
        final String methodName = "testBuildTokenResponseTypeZero";
        try {
            List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            tokens.add(code);
            final String clientId = "client01"; //attributeList.getAttributeValueByNameAndType(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            final String username = "testuser"; // attributeList.getAttributeValueByNameAndType(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST);
            final String[] scopes = new String[] { "profile", "openid" };
            final List<OAuth20Token> tokenListZero = new ArrayList<OAuth20Token>();

            mock.checking(new Expectations() {
                {
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.RESPONSE_TYPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(implicitResponseType));

                    one(oa20rthti).buildTokensResponseType(attributeList, oauth20TokenFactory, redirectUri);
                    will(returnValue(tokenListZero));

                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(clientId));
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue(username));

                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(redirectUri));
                    one(attributeList).getAttributeValuesByName(OAuth20Constants.SCOPE);
                    will(returnValue(scopes));
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            List<OAuth20Token> listOAuth20Token = oidcrthii.buildTokensResponseType(attributeList, oauth20TokenFactory, redirectUri);
            assertEquals("It ought to have 0 tokens but get some tokens ", listOAuth20Token.size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testBuildTokenResponseTypeBad() {
        final String methodName = "testBuildTokenResponseTypeBad";
        try {
            List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            final List<OAuth20Token> tokenListOne = new ArrayList<OAuth20Token>();
            tokenListOne.add(access);
            final String clientId = "client01"; //attributeList.getAttributeValueByNameAndType(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            final String username = "testuser"; // attributeList.getAttributeValueByNameAndType(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST);
            final String[] scopes = new String[] { "profile", "openid" };

            mock.checking(new Expectations() {
                {
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.RESPONSE_TYPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(implicitResponseType));

                    one(oa20rthti).buildTokensResponseType(attributeList, oauth20TokenFactory, redirectUri);
                    will(returnValue(tokenListOne));

                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(clientId));
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue(username));

                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(redirectUri));
                    one(attributeList).getAttributeValuesByName(OAuth20Constants.SCOPE);
                    will(returnValue(scopes));

                    one(access).getType();
                    will(returnValue("BadTokenType"));
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            List<OAuth20Token> listOAuth20Token = oidcrthii.buildTokensResponseType(attributeList, oauth20TokenFactory, redirectUri);
            assertEquals("It ought to have 1 tokens but get less or more some tokens ", listOAuth20Token.size(), 1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testBuildTokenResponseType() {
        final String methodName = "testBuildTokenResponseType";
        try {
            List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            final List<OAuth20Token> tokenListOne = new ArrayList<OAuth20Token>();
            tokenListOne.add(accessToken);
            final String clientId = "client01"; //attributeList.getAttributeValueByNameAndType(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            final String username = "testuser"; // attributeList.getAttributeValueByNameAndType(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST);
            final String[] scopes = new String[] { "profile", "openid" };
            final String stateId = "FakedStateIdXYZ";
            final String accessTokenString = "fakedAccessTokenStringXYZ";
            final String issuerId = "opProvier1";
            final String nonce = "fakedNonceXYZ123";

            mock.checking(new Expectations() {
                {
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.RESPONSE_TYPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(implicitResponseType));

                    one(oa20rthti).buildTokensResponseType(attributeList, oauth20TokenFactory, redirectUri);
                    will(returnValue(tokenListOne));

                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(clientId));
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue(username));

                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(redirectUri));
                    one(attributeList).getAttributeValuesByName(OAuth20Constants.SCOPE);
                    will(returnValue(scopes));

                    one(accessToken).getType();
                    will(returnValue(OAuth20Constants.ACCESS_TOKEN));

                    one(accessToken).getStateId();
                    will(returnValue(stateId));
                    one(oauth20TokenFactory).getOAuth20ComponentInternal();
                    will(returnValue(oa20ci));
                    one(accessToken).getExtensionProperties();
                    will(returnValue(blueMap));
                    one(accessToken).getTokenString();
                    will(returnValue(accessTokenString));
                    one(attributeList).getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                    will(returnValue(issuerId));
                    one(attributeList).getAttributeValueByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue(nonce));
                    // oidc10TokenFactory.createIDToken(idTokenMap)
                    allowing(oa20ci).getParentComponentInstance();
                    will(returnValue(oaci));
                    allowing(oaci).getInstanceId();
                    will(returnValue("opProvierId"));
                    allowing(oa20ci).get20Configuration();
                    will(returnValue(oa2cp));
                    one(oa2cp).getTokenLifetimeSeconds();
                    will(returnValue(600)); // 10 minutes
                    one(oa2cp).getAccessTokenLength();
                    will(returnValue(30));
                    one(oa2cp).getIDTokenTypeHandler();
                    will(returnValue(oa2tth));
                    one(oa2cp).getClientProvider();
                    will(returnValue(oa2clientprovider));
                    one(oa2clientprovider).get("client01");
                    will(returnValue(oidcBaseClient));
                    one(oidcBaseClient).getClientSecret();
                    will(returnValue("password"));
                    one(oa2tth).createToken(with(any(Map.class)));
                    will(returnValue(idTokenImpl));
                    allowing(idTokenImpl).isPersistent();
                    will(returnValue(false));
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            List<OAuth20Token> listOAuth20Token = oidcrthii.buildTokensResponseType(attributeList, oauth20TokenFactory, redirectUri);
            assertEquals("It ought to have 2 tokens but get less or more some tokens ", listOAuth20Token.size(), 2);
            boolean bAccess = false;
            boolean bIdToken = false;
            for (OAuth20Token token : listOAuth20Token) {
                if (token.equals(accessToken))
                    bAccess = true;
                if (token.equals(idTokenImpl))
                    bIdToken = true;
            }
            assertTrue("Did not get the expected access toekn", bAccess);
            assertTrue("Did not get the expected id_token", bIdToken);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testBuildResponseTypeImplicit0() {
        final String methodName = "testBuildResponseTypeImplicit0";
        try {
            final List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            //tokens.add(idTokenImpl);
            final String idTokenString = "fakedIdTokenHeaders.fakedIdTokenPayload.fakedIdTokenSignature";
            final String[] strs = new String[] { idTokenString };
            mock.checking(new Expectations() {
                {
                    one(oa20rthti).buildResponseResponseType(attributeList, tokens);
                    // return nothing here

                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            oidcrthii.buildResponseResponseType(attributeList, tokens);
            // No failures, This is what we expected
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testBuildResponseTypeImplicit() {
        final String methodName = "testBuildResponseTypeImplicit";
        try {
            final List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            tokens.add(idTokenImpl);
            final String idTokenString = "fakedIdTokenHeaders.fakedIdTokenPayload.fakedIdTokenSignature";
            final String[] strs = new String[] { idTokenString };
            mock.checking(new Expectations() {
                {
                    one(oa20rthti).buildResponseResponseType(attributeList, tokens);
                    // return nothing here

                    one(idTokenImpl).getType();
                    will(returnValue(OIDCConstants.ID_TOKEN));
                    one(idTokenImpl).getTokenString();
                    will(returnValue(idTokenString));
                    one(attributeList).setAttribute(OIDCConstants.ID_TOKEN,
                                                    OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE,
                                                    strs);
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            oidcrthii.buildResponseResponseType(attributeList, tokens);
            // No failures, This is what we expected
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateRequestResponseTypeImplicitNoNonce() {
        final String methodName = "testValidateResponseTypeImplicitNoNonce";
        try {
            final String responseTypes = "id_token token";
            final String[] scopes = new String[] { "profile", "openid" };
            final String issuerId = "issuerId321";
            final String nonce = "fakedNonceXYZ";
            mock.checking(new Expectations() {
                {
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.RESPONSE_TYPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(responseTypes));
                    one(attributeList).getAttributeValuesByName(OAuth20Constants.SCOPE);
                    will(returnValue(scopes));
                    one(attributeList).getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                    will(returnValue(issuerId));
                    one(attributeList).getAttributeValueByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue((String) null));
                    //one(oa20rthti).validateRequestResponseType(attributeList, redirectUri);
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            oidcrthii.validateRequestResponseType(attributeList, OidcOAuth20Util.initJsonArray(redirectUri), false);
            //failures, SHould not be here
            fail("The validation should fail but not");
        } catch (OAuth20MissingParameterException e) {
            // this is what we expect
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateRequestResponseTypeImplicit() {
        final String methodName = "testValidateResponseTypeImplicit";
        try {
            final String responseTypes = "id_token token";
            final String[] scopes = new String[] { "profile", "openid" };
            final String issuerId = "issuerId321";
            final String nonce = "fakedNonceXYZ";
            mock.checking(new Expectations() {
                {
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.RESPONSE_TYPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(responseTypes));
                    one(attributeList).getAttributeValuesByName(OAuth20Constants.SCOPE);
                    will(returnValue(scopes));
                    one(attributeList).getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                    will(returnValue(issuerId));
                    one(attributeList).getAttributeValueByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue(nonce));
                    one(oa20rthti).validateRequestResponseType(attributeList, OidcOAuth20Util.initJsonArray(redirectUri), false);
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            oidcrthii.validateRequestResponseType(attributeList, OidcOAuth20Util.initJsonArray(redirectUri), false);
            // No failures, This is what we expected
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateRequestResponseTypeImplicitNoToken() {
        final String methodName = "testValidateResponseTypeImplicitNoToken";
        try {
            final String responseTypes = "id_token";
            final String[] scopes = new String[] { "profile", "openid" };
            final String issuerId = "issuerId321";
            final String nonce = "fakedNonceXYZ";
            mock.checking(new Expectations() {
                {
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.RESPONSE_TYPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(responseTypes));
                    one(attributeList).getAttributeValuesByName(OAuth20Constants.SCOPE);
                    will(returnValue(scopes));
                    one(attributeList).getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                    will(returnValue(issuerId));
                    one(attributeList).getAttributeValueByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue(nonce));
                    //one(oa20rthti).validateRequestResponseType(attributeList, redirectUri);
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            oidcrthii.validateRequestResponseType(attributeList, OidcOAuth20Util.initJsonArray(redirectUri), false);
            //failures, SHould not be here
            fail("The validation should fail but not");
        } catch (OIDCUnsupportedResponseTypeException e) {
            // this is what we expect
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateRequestResponseTypeImplicitNoIssuer() {
        final String methodName = "testValidateResponseTypeImplicitNoIssuer";
        try {
            final String responseTypes = "id_token";
            final String[] scopes = new String[] { "profile", "openid" };
            final String issuerId = "issuerId321";
            final String nonce = "fakedNonceXYZ";
            mock.checking(new Expectations() {
                {
                    one(attributeList).getAttributeValueByNameAndType(OAuth20Constants.RESPONSE_TYPE,
                                                                      OAuth20Constants.ATTRTYPE_PARAM_QUERY);
                    will(returnValue(responseTypes));
                    //one(attributeList).getAttributeValuesByName(OAuth20Constants.SCOPE);
                    //will(returnValue(scopes));
                    one(attributeList).getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                    will(returnValue((String) null));
                    //one(attributeList).getAttributeValueByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST);
                    //will(returnValue(nonce));
                    //one(oa20rthti).validateRequestResponseType(attributeList, redirectUri);
                }
            });

            OIDCResponseTypeHandlerImplicitImpl oidcrthii = new OIDCResponseTypeHandlerImplicitImpl();
            oidcrthii.oa20rthti = oa20rthti;
            oidcrthii.validateRequestResponseType(attributeList, OidcOAuth20Util.initJsonArray(redirectUri), false);
            //failures, Should not be here
            fail("The validation should fail but not");
        } catch (OAuth20InternalException e) {
            // this is what we expect
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
