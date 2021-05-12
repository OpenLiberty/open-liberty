/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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

import java.util.ArrayList;
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
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;

import test.common.SharedOutputManager;

public class OIDCGrantTypeHandlerRefreshImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
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
    final OidcOAuth20Client oidcOauth20Client = mock.mock(OidcOAuth20Client.class, "oidcOauth20Client");
    final IDTokenImpl idTokenImpl = mock.mock(IDTokenImpl.class, "idTokenImpl");

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
            OIDCGrantTypeHandlerRefreshImpl oidcGrantTypeHandlerRefreshImpl = new OIDCGrantTypeHandlerRefreshImpl(oauth20ConfigProvider);
            assertNotNull("Can not instantiate an oidcGrantTypehandlerRefreshImpl", oidcGrantTypeHandlerRefreshImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testBuildTokenGrantType() {
        final String methodName = "testBuildTokenGrantType";
        try {
            List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            tokens.add(code);
            final String clientId = "client01";
            final String username = "testuser";
            final String redirectUri = "https://unknown.ibm.com:8010/oidcclient/redirect";
            final String stateId = "areyousurethisis1stateid";
            final String[] scopes = new String[] { "openid", "profile" };
            final String[] origGrantType = new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE };
            final String[] refreshid = { "refresh_token_string" };
            mock.checking(new Expectations() {
                {
                    allowing(attributeList).getAttributeValueByName(OAuth20Constants.CLIENT_ID);// "client_id");
                    will(returnValue(clientId));
                    allowing(attributeList).getAttributeValueByName(OAuth20Constants.PROXY_HOST);
                    will(returnValue(host));
                    allowing(attributeList).getAttributeValueByNameAndType(OAuth20Constants.CLIENT_SECRET, OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
                    allowing(attributeList).getAttributeValuesByNameAndType(OAuth20Constants.RESOURCE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH);//
                    will(returnValue(null));//
                    allowing(attributeList).getAttributeValuesByNameAndType(OAuth20Constants.RESOURCE_IDS, OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
                    allowing(code).getScope();
                    will(returnValue(scopes));
                    allowing(code).getUsername();
                    will(returnValue(username));
                    allowing(code).getRedirectUri();
                    will(returnValue(redirectUri));
                    allowing(attributeList).getAttributeValuesByNameAndType(OAuth20Constants.SCOPE,
                                                                            OAuth20Constants.ATTRTYPE_PARAM_BODY);
                    will(returnValue(scopes));
                    allowing(code).getStateId();
                    will(returnValue(stateId));
                    allowing(code).getId();
                    will(returnValue("refresh_code_string"));
                    allowing(code).getGrantType();
                    will(returnValue(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
                    allowing(code).getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.REFRESH_TOKEN_ORIGINAL_GT);
                    will(returnValue(origGrantType));
                    allowing(oauth20TokenFactory).buildTokenMap(clientId, username, redirectUri, stateId, scopes, code, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    will(returnValue(accessTokenMap));
                    allowing(oauth20TokenFactory).createAccessToken(with(any(Map.class)));
                    will(returnValue(access));
                    allowing(oauth20TokenFactory).createRefreshToken(with(any(Map.class)));
                    will(returnValue(refresh));
                    allowing(refresh).getId();
                    will(returnValue("refresh_token_string"));
                    allowing(accessTokenMap).put(with(any(String.class)), with(any(String[].class)));
                    will(returnValue(refreshid));
                    allowing(oauth20TokenFactory).getOAuth20ComponentInternal();
                    will(returnValue(componentInternal));
                    allowing(componentInternal).get20Configuration();
                    will(returnValue(oauth20ConfigProvider));
                    allowing(oauth20ConfigProvider).getMaxAuthGrantLifetimeSeconds();
                    will(returnValue(3600)); // 1 hour
                    allowing(oauth20ConfigProvider).getCodeLifetimeSeconds();
                    will(returnValue(300)); // 5 minutes
                    allowing(code).getType();
                    will(returnValue("refresh_token"));
                    allowing(access).getType();
                    will(returnValue("access_token"));
                    allowing(access).getTokenString();
                    will(returnValue("access_token_string"));
                    allowing(attributeList).getAttributeValueByName("issuerIdentifier");
                    will(returnValue("https://op.ibm.com:8020/op/token"));
                    allowing(componentInternal).getParentComponentInstance();
                    will(returnValue(oauthComponentInstance));
                    allowing(oauthComponentInstance).getInstanceId();
                    will(returnValue("myOp"));
                    allowing(oauth20ConfigProvider).getTokenLifetimeSeconds();
                    will(returnValue(3600)); // 1 hour
                    allowing(oauth20ConfigProvider).getAccessTokenLength();
                    will(returnValue(24)); //
                    //allowing(oauth20ConfigProvider).getIDTokenTypeHandler();
                    //will(returnValue(idTokenHandler));
                    //allowing(oauth20ConfigProvider).getClientProvider();
                    //will(returnValue(oauth20ClientProvider));
                    //allowing(oauth20ClientProvider).get("client01");
                    //will(returnValue(oauth20Client));
                    //allowing(idTokenHandler).createToken(with(any(Map.class)));
                    //will(returnValue(idTokenImpl));
                }
            });

            OIDCGrantTypeHandlerRefreshImpl oidcGrantTypehandlerRefreshImpl = new OIDCGrantTypeHandlerRefreshImpl(oauth20ConfigProvider);
            List<OAuth20Token> listOAuth20Token = oidcGrantTypehandlerRefreshImpl.buildTokensGrantType(attributeList, oauth20TokenFactory, tokens);
            assertTrue("It ought to have 2 tokens: access_token, refresh_token ", listOAuth20Token.size() == 2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
