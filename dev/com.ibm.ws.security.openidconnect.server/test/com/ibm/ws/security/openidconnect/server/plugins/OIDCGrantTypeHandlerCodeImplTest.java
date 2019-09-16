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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
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
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

public class OIDCGrantTypeHandlerCodeImplTest {

    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final String[] testScopes = new String[] { "openid", "profile" };
    private final String redirectURI = "https://unknown.ibm.com:8010/oidcclient/redirect";
    private final String clientID = "client01";
    private final String clientSecret = "clientSecret";
    private final String issuer = "https://op.ibm.com:8020/op/token";
    private final String host = "op.ibm.com";

    final AttributeList attributeList = mock.mock(AttributeList.class, "attributeList");
    final OAuth20TokenFactory oauth20TokenFactory = mock.mock(OAuth20TokenFactory.class, "oauth20ToknFactory");
    final OAuth20Token code = mock.mock(OAuth20Token.class, "code");
    final Map<String, String[]> accessTokenMap = mock.mock(Map.class, "accessTokenmap");
    final OAuth20Token access = mock.mock(OAuth20Token.class, "access");
    final OAuth20Token refresh = mock.mock(OAuth20Token.class, "refresh");
    final OAuth20ComponentInternal componentInternal = mock.mock(OAuth20ComponentInternal.class, "componentInternal");
    final OAuth20ConfigProvider oauth20ConfigProvider = mock.mock(OAuth20ConfigProvider.class, "oauth20ConfigProvider");
    final OAuthComponentInstance oauthComponentInstance = mock.mock(OAuthComponentInstance.class, "oauuthComponentInstance");
    final IDTokenHandler idTokenHandler = mock.mock(IDTokenHandler.class, "idTokenhandler");
    final OidcOAuth20ClientProvider oidcOauth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class, "oidcOauth20ClientProvider");
    final OidcBaseClient oidcBaseClient = mock.mock(OidcBaseClient.class, "oidcBaseClient");
    final IDTokenImpl idTokenImpl = mock.mock(IDTokenImpl.class, "idTokenImpl");
    final List<OAuth20Token> tokenList = mock.mock(ArrayList.class);
    final Iterator iterator = mock.mock(Iterator.class, "iterator");

    static Map<String, String[]> blueMap = new HashMap<String, String[]>();
    static {
        blueMap.put("unitNewKey", new String[] { "unitNewValue1" });
    }

    private OIDCGrantTypeHandlerCodeImpl oidcGrantTypehandlerCodeImpl;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        oidcGrantTypehandlerCodeImpl = new OIDCGrantTypeHandlerCodeImpl();
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
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypehandlerCodeImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildTokenGrantType() {
        final String methodName = "testBuildTokenGrantType";
        try {
            List<OAuth20Token> tokens = createTokenListWithAuthorizationCode();
            final String username = "testuser";
            final String stateId = "areyousurethisis1stateid";
            final String[] scopes = testScopes;
            final String[] refreshid = { "refresh_token_string" };
            mock.checking(new Expectations() {
                {
                    allowing(attributeList).getAttributeValueByName(OAuth20Constants.CLIENT_ID);// "client_id");
                    will(returnValue(clientID));
                    allowing(attributeList).getAttributeValueByName(OAuth20Constants.PROXY_HOST);
                    will(returnValue(host));
                    allowing(attributeList).getAttributeValueByNameAndType(OAuth20Constants.REDIRECT_URI,
                                                                           OAuth20Constants.ATTRTYPE_PARAM_BODY);
                    will(returnValue(redirectURI));
                    allowing(attributeList).getAttributeValueByNameAndType(OAuth20Constants.REQUEST_FEATURE,
                                                                           OAuth20Constants.ATTRTYPE_REQUEST);
                    will(returnValue(OAuth20Constants.REQUEST_FEATURE_OIDC));
                    allowing(code).getScope();
                    will(returnValue(scopes));
                    allowing(code).getUsername();
                    will(returnValue(username));
                    allowing(code).getStateId();
                    will(returnValue(stateId));

                    allowing(oauth20TokenFactory).buildTokenMap(clientID, username, redirectURI, stateId, scopes, code, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    will(returnValue(accessTokenMap));
                    allowing(oauth20TokenFactory).createRefreshToken(with(any(Map.class)));
                    will(returnValue(refresh));
                    allowing(refresh).getId();
                    will(returnValue("refresh_token_string"));
                    allowing(accessTokenMap).put(with(any(String.class)), with(any(String[].class)));
                    will(returnValue(refreshid));
                    allowing(oauth20TokenFactory).createAccessToken(with(any(Map.class)));
                    will(returnValue(access));

                    allowing(oauth20TokenFactory).getOAuth20ComponentInternal();
                    will(returnValue(componentInternal));
                    allowing(componentInternal).get20Configuration();
                    will(returnValue(oauth20ConfigProvider));
                    allowing(oauth20ConfigProvider).getMaxAuthGrantLifetimeSeconds();
                    will(returnValue(3600)); // 1 hour
                    allowing(oauth20ConfigProvider).getCodeLifetimeSeconds();
                    will(returnValue(300)); // 5 minutes
                    allowing(code).getType();
                    will(returnValue("authorization_code"));
                    allowing(code).getExtensionProperties(); //
                    will(returnValue(blueMap)); //
                    allowing(access).getType();
                    will(returnValue("access_token"));
                    allowing(access).getTokenString();
                    will(returnValue("access_token_string"));
                    allowing(attributeList).getAttributeValueByName("issuerIdentifier");
                    will(returnValue(issuer));
                    allowing(attributeList).getAttributeValueByNameAndType(OAuth20Constants.CLIENT_SECRET,
                                                                           OAuth20Constants.ATTRTYPE_PARAM_OAUTH);//
                    will(returnValue(clientSecret)); //
                    allowing(attributeList).getAttributeValuesByNameAndType(OAuth20Constants.RESOURCE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH);//
                    will(returnValue(null));
                    allowing(attributeList).getAttributeValuesByNameAndType(OAuth20Constants.RESOURCE_IDS, // //
                                                                            OAuth20Constants.ATTRTYPE_PARAM_OAUTH); //
                    will(returnValue(new String[] { "audience1", "audience2" }));//
                    allowing(componentInternal).getParentComponentInstance();
                    will(returnValue(oauthComponentInstance));
                    allowing(oauthComponentInstance).getInstanceId();
                    will(returnValue("myOp"));
                    allowing(oauth20ConfigProvider).getTokenLifetimeSeconds();
                    will(returnValue(3600)); // 1 hour
                    allowing(oauth20ConfigProvider).getAccessTokenLength();
                    will(returnValue(24)); //
                    allowing(oauth20ConfigProvider).getIDTokenTypeHandler();
                    will(returnValue(idTokenHandler));
                    allowing(oauth20ConfigProvider).getClientProvider();
                    will(returnValue(oidcOauth20ClientProvider));
                    allowing(oidcOauth20ClientProvider).get(clientID);
                    will(returnValue(oidcBaseClient));
                    allowing(oidcBaseClient).getClientSecret();
                    allowing(idTokenHandler).createToken(with(any(Map.class)));
                    will(returnValue(idTokenImpl));
                    allowing(idTokenImpl).isPersistent();
                    will(returnValue(false));
                }
            });

            List<OAuth20Token> listOAuth20Token = oidcGrantTypehandlerCodeImpl.buildTokensGrantType(attributeList, oauth20TokenFactory, tokens);
            assertTrue("It ought to have 3 tokens: access_token, refresh_token, id_token ", listOAuth20Token.size() == 3);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateRequestGrantType_notOpenIDScope() {
        final String methodName = "testValidateRequestGrantType_notOpenIDScope";
        try {
            List<OAuth20Token> tokens = createTokenListWithAuthorizationCode();
            setScopesExpectation(null);
            setGoodSuperClassValidationExpectations();
            oidcGrantTypehandlerCodeImpl.validateRequestGrantType(attributeList, tokens);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Only need to validate the issuer since the username and client_id are validated
     * in the OAuth20ComponentImpl's buildAuthorizationAttributeList method.
     */
    @Test
    public void testValidateRequestGrantType_hasIssuer() {
        final String methodName = "testValidateRequestGrantType_hasIssuer";
        try {
            List<OAuth20Token> tokens = createTokenListWithAuthorizationCode();
            setScopesExpectation(testScopes);
            setGoodSuperClassValidationExpectations();
            setIssuerExpectation(issuer);
            oidcGrantTypehandlerCodeImpl.validateRequestGrantType(attributeList, tokens);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Only need to validate the issuer since the username and client_id are validated
     * in the OAuth20ComponentImpl's buildAuthorizationAttributeList method.
     */
    @Test
    public void testValidateRequestGrantType_noIssuer() {
        final String methodName = "testValidateRequestGrantType_noIssuer";
        try {
            List<OAuth20Token> tokens = createTokenListWithAuthorizationCode();
            setScopesExpectation(testScopes);
            setGoodSuperClassValidationExpectations();
            setIssuerExpectation(null);
            oidcGrantTypehandlerCodeImpl.validateRequestGrantType(attributeList, tokens);
            fail("The validation should fail.");
        } catch (OAuth20InternalException e) {
            assertMissingIssuerMessage(e);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Only need to validate the issuer since the username and client_id are validated
     * in the OAuth20ComponentImpl's buildAuthorizationAttributeList method.
     */
    @Test
    public void testValidateRequestGrantType_emptyIssuer() {
        final String methodName = "testValidateRequestGrantType_emptyIssuer";
        try {
            List<OAuth20Token> tokens = createTokenListWithAuthorizationCode();
            setScopesExpectation(testScopes);
            setGoodSuperClassValidationExpectations();
            setIssuerExpectation("");
            oidcGrantTypehandlerCodeImpl.validateRequestGrantType(attributeList, tokens);
            fail("The validation should fail.");
        } catch (OAuth20InternalException e) {
            assertMissingIssuerMessage(e);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private List<OAuth20Token> createTokenListWithAuthorizationCode() {
        List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
        tokens.add(code);
        return tokens;
    }

    /**
     * Expectations needed for when OIDCGrantypeHandler's validateRequestResponseType method calls
     * the OAuth20GrantTypeHandlerCodeImpl's validateRequestResponseType method.
     */
    private void setGoodSuperClassValidationExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(attributeList).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                will(returnValue(clientID));
                allowing(code).getClientId();
                will(returnValue(clientID));
                allowing(attributeList).getAttributeValueByNameAndType(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_BODY);
                will(returnValue(redirectURI));
                allowing(code).getRedirectUri();
                will(returnValue(redirectURI));
            }
        });
    }

    private void setScopesExpectation(final String[] scopes) {
        mock.checking(new Expectations() {
            {
                allowing(code).getScope();
                will(returnValue(scopes));
            }
        });
    }

    private void setIssuerExpectation(final String issuer) {
        mock.checking(new Expectations() {
            {
                allowing(attributeList).getAttributeValueByName("issuerIdentifier");
                will(returnValue(issuer));
            }
        });
    }

    private void assertMissingIssuerMessage(OAuth20InternalException e) {
        String message = e.getCause().getMessage();
        assertTrue("The message must indicate that the issuer identifier is missing.", message.contains("Missing issuerIdentifier"));
    }
}
