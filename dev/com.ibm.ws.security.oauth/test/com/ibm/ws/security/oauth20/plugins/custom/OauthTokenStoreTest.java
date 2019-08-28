/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.google.gson.JsonObject;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.util.JSONUtil;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.websphere.security.oauth20.store.OAuthToken;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.util.MessageDigestUtil;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

import test.common.SharedOutputManager;

/**
 *
 */
public class OauthTokenStoreTest {

    private static final String defaultExceptionMsg = "This is an exception message.";

    private final Mockery mockery = new JUnit4Mockery();
    private final String lookupKey = "1234567890";
    private final String hash = MessageDigestUtil.getDigest(lookupKey);
    private final String id = "uniqueId";
    private final String componentId = "OP";
    private final String grantType = "grant type";
    private final String type = "token type";
    private final String subType = "token sub type";
    private final long createdAt = System.currentTimeMillis();
    private final int lifetimeSeconds = 60;
    private final String tokenString = "test token string";
    private final String clientId = "clientId";
    private final String username = "test user";
    private final String[] scope = new String[] { "scope1", "scope2" };
    private final String redirectUri = AbstractOidcRegistrationBaseTest.REDIRECT_URI_1;
    private final String stateId = "some random state id";
    private final Map<String, String[]> extensionProperties = new HashMap<String, String[]>();

    private OauthTokenStore oauthTokenStore;
    private OAuthStore oauthStore;
    private OAuth20Token token;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.oauth20.plugins.custom.*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        oauthStore = mockery.mock(OAuthStore.class);
        oauthTokenStore = new OauthTokenStore(componentId, oauthStore, 0);
        token = new OAuth20TokenImpl(id, componentId, type, subType, createdAt, lifetimeSeconds, tokenString, clientId, username, scope, redirectUri, stateId, extensionProperties, grantType);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#add(java.lang.String, com.ibm.oauth.core.api.oauth20.token.OAuth20Token, int)}.
     */
    @Test
    public void testAdd() throws Exception {
        OAuthToken oauthToken = getOAuthToken(hash, token);
        createsToken(oauthToken);

        oauthTokenStore.add(lookupKey, token, lifetimeSeconds);
    }

    private void createsToken(final OAuthToken oauthToken) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).create(oauthToken);
            }
        });
    }

    @Test
    public void testAdd_OAuthStoreException() throws Exception {
        final OAuthToken oauthToken = getOAuthToken(hash, token);
        mockery.checking(new Expectations() {
            {
                one(oauthStore).create(oauthToken);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        oauthTokenStore.add(lookupKey, token, lifetimeSeconds);

        String msgRegex = "CWWKS1465E.+" + oauthToken.getLookupKey();
        verifyLogMessage(outputMgr, msgRegex);
    }

    private void verifyLogMessage(SharedOutputManager outputMgr, String msgRegex) {
        String messageRegex = msgRegex + ".+" + Pattern.quote(defaultExceptionMsg);
        assertTrue("Did not find message [" + messageRegex + "] in log.", outputMgr.checkForMessages(messageRegex));
    }

    @Test
    public void testAdd_accessToken() throws Exception {
        Map<String, String[]> extProps = new HashMap<String, String[]>();
        extProps.put(OAuth20Constants.GRANT_TYPE, new String[] { OAuth20Constants.TOKENTYPE_ACCESS_TOKEN });
        extProps.put(OAuth20Constants.REFRESH_TOKEN_ID, new String[] { "accessTokenOfAwesome!!1!" });

        OAuth20Token token = new OAuth20TokenImpl("accessTokenId", componentId, OAuth20Constants.TOKENTYPE_ACCESS_TOKEN, subType, createdAt, lifetimeSeconds, tokenString, clientId, username, scope, redirectUri, stateId, extProps, grantType);
        OAuthToken oauthToken = getOAuthToken(hash, token);

        createsToken(oauthToken);

        oauthTokenStore.add(lookupKey, token, lifetimeSeconds);

        readsToken(token);

        OAuth20Token retrievedToken = oauthTokenStore.getByHash(hash);

        Map<String, String[]> storeProps = retrievedToken.getExtensionProperties();
        assertNull("Grant type should not be retreived in extensionProps", storeProps.get(OAuth20Constants.GRANT_TYPE));
        assertNull("Refresh token should not be retreived in extensionProps", storeProps.get(OAuth20Constants.REFRESH_TOKEN_ID));

        assertNotNull("Should receive a refresh token", ((OAuth20TokenImpl) retrievedToken).getRefreshTokenKey());
    }

    @Test
    public void testAdd_idToken() throws Exception {
        Map<String, String[]> extProps = new HashMap<String, String[]>();
        extProps.put(OAuth20Constants.GRANT_TYPE, new String[] { OIDCConstants.TOKENTYPE_ID_TOKEN });
        extProps.put(OAuth20Constants.ACCESS_TOKEN_ID, new String[] { "idTokenOfAwesome!!1!" });

        OAuth20Token token = new OAuth20TokenImpl("idTokenId", componentId, OIDCConstants.TOKENTYPE_ID_TOKEN, OIDCConstants.ID_TOKEN, createdAt, lifetimeSeconds, tokenString, clientId, username, scope, redirectUri, stateId, extProps, grantType);
        OAuthToken oauthToken = getOAuthToken(hash, token);

        createsToken(oauthToken);

        oauthTokenStore.add(lookupKey, token, lifetimeSeconds);

        readsToken(token);

        OAuth20Token retrievedToken = oauthTokenStore.getByHash(hash);

        Map<String, String[]> storeProps = retrievedToken.getExtensionProperties();
        assertNull("Grant type should not be retreived in extensionProps", storeProps.get(OAuth20Constants.GRANT_TYPE));
        assertNull("ID token should not be retreived in extensionProps", storeProps.get(OAuth20Constants.ACCESS_TOKEN_ID));

        assertNotNull("Should receive an access token", ((OAuth20TokenImpl) retrievedToken).getAccessTokenKey());

    }

    private OAuthToken getOAuthToken(String lookupKey, OAuth20Token token) {
        long expires = 0;
        if (token.getLifetimeSeconds() > 0) {
            expires = token.getCreatedAt() + (1000L * token.getLifetimeSeconds());
        }

        String tokenString = PasswordUtil.passwordEncode(token.getTokenString());

        StringBuffer scopes = new StringBuffer();
        String[] ascopes = token.getScope();
        if (ascopes != null && ascopes.length > 0) {
            for (int i = 0; i < ascopes.length; i++) {
                scopes.append(ascopes[i].trim());
                if (i < (ascopes.length - 1)) {
                    scopes.append(" ");
                }
            }
        }
        String scope = scopes.toString();

        // TODO: Test with extra data based on token type
        JsonObject extendedFields = JSONUtil.getJsonObject(token.getExtensionProperties());
        if (extendedFields == null) {
            extendedFields = new JsonObject();
        }
        extendedFields.addProperty(OAuth20Constants.GRANT_TYPE, token.getGrantType());

        String refreshId = null, accessId = null;
        if (OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(token.getType())) {
            if ((refreshId = ((OAuth20TokenImpl) token).getRefreshTokenKey()) != null) {
                extendedFields.addProperty(OAuth20Constants.REFRESH_TOKEN_ID, refreshId);
            }
        } else if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(token.getType())) {
            if ((accessId = ((OAuth20TokenImpl) token).getAccessTokenKey()) != null) {
                extendedFields.addProperty(OAuth20Constants.ACCESS_TOKEN_ID, accessId);
            }
        }

        OAuthToken oauthToken = new OAuthToken(lookupKey, token.getId(), token.getComponentId(), token.getType(), token.getSubType(), token.getCreatedAt(), token.getLifetimeSeconds(), expires, tokenString, token.getClientId(), token.getUsername(), scope, token.getRedirectUri(), token.getStateId(), extendedFields.toString());
        return oauthToken;
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#getByHash(java.lang.String)}.
     */
    @Test
    public void testGetByHash() throws Exception {
        readsToken(token);

        OAuth20Token retrievedToken = oauthTokenStore.getByHash(hash);

        assertNotNull("There must be an OAuth 2.0 token.", retrievedToken);
        assertEqualsOAuth20Token(token, retrievedToken);
    }

    public static void assertEqualsOAuth20Token(OAuth20Token token1, OAuth20Token token2) {
        assertEquals(token1.getComponentId(), token2.getComponentId());
        assertEquals(token1.getClientId(), token2.getClientId());
        assertEquals(token1.getCreatedAt(), token2.getCreatedAt());
        assertEquals(token1.getExtensionProperties(), token2.getExtensionProperties());
        assertEquals(token1.getGrantType(), token2.getGrantType());
        assertEquals(token1.getId(), token2.getId());
        assertEquals(token1.getLifetimeSeconds(), token2.getLifetimeSeconds());
        assertEquals(token1.getRedirectUri(), token2.getRedirectUri());
        assertEquals(token1.getScope(), token2.getScope());
        assertEquals(token1.getStateId(), token2.getStateId());
        assertEquals(token1.getSubType(), token2.getSubType());
        assertEquals(token1.getTokenString(), token2.getTokenString());
        assertEquals(token1.getType(), token2.getType());
        assertEquals(token1.getUsername(), token2.getUsername());
        assertEquals(token1.isPersistent(), token2.isPersistent());
    }

    @Test
    public void testGetByHash_OAuthStoreException() throws Exception {
        readThrowsOAuthStoreException(hash);

        OAuth20Token retrievedToken = oauthTokenStore.getByHash(hash);

        assertNull("There must not be an OAuth 2.0 token.", retrievedToken);
        String msgRegex = "CWWKS1469E.+" + hash;
        verifyLogMessage(outputMgr, msgRegex);
    }

    private void readThrowsOAuthStoreException(final String hash) throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(oauthStore).readToken(componentId, hash);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });
    }

    @Test
    public void testGetByHash_expiredReturnsNull() throws Exception {
        OAuth20Token token = new OAuth20TokenImpl("expiredToken", componentId, type, subType, createdAt, -10, tokenString, clientId, username, scope, redirectUri, stateId, extensionProperties, grantType);
        readsToken(token);

        OAuth20Token retrievedToken = oauthTokenStore.getByHash(hash);

        assertNull("There must not be an OAuth 2.0 token.", retrievedToken);
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#get(java.lang.String)}.
     */
    @Test
    public void testGet() throws Exception {
        readsToken(token);
        OAuth20Token retrievedToken = oauthTokenStore.get(lookupKey);
        assertNotNull("There must be an OAuth 2.0 token.", retrievedToken);
        assertEqualsOAuth20Token(token, retrievedToken);
    }

    private void readsToken(OAuth20Token token) throws Exception {
        final OAuthToken oauthToken = getOAuthToken(hash, token);
        // TODO: Determine if component id is used in the lookup.
        mockery.checking(new Expectations() {
            {
                one(oauthStore).readToken(componentId, hash);
                will(returnValue(oauthToken));
            }
        });
    }

    @Test
    public void testGet_OAuthStoreException() throws Exception {
        readThrowsOAuthStoreException(hash);

        OAuth20Token retrievedToken = oauthTokenStore.get(lookupKey);

        assertNull("There must not be an OAuth 2.0 token.", retrievedToken);
        String msgRegex = "CWWKS1469E.+" + hash;
        verifyLogMessage(outputMgr, msgRegex);
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#getAllUserTokens(java.lang.String)}.
     */
    @Test
    public void testGetAllUserTokens() throws Exception {
        OAuthToken oauthToken = getOAuthToken(hash, token);
        Collection<OAuthToken> oauthTokens = new ArrayList<OAuthToken>();
        oauthTokens.add(oauthToken);
        readsAllTokens(oauthTokens);

        Collection<OAuth20Token> tokens = oauthTokenStore.getAllUserTokens(username);

        assertFalse("There must be a collection of OAuth 2.0 tokens.", tokens.isEmpty());
        // TODO: Assert token contents.
    }

    @Test
    public void testGetAllUserTokens_OAuthStoreException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).readAllTokens(componentId, username);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        Collection<OAuth20Token> tokens = oauthTokenStore.getAllUserTokens(username);

        assertTrue("There must be an empty collection.", tokens.isEmpty());
        String msgRegex = "CWWKS1470E";
        verifyLogMessage(outputMgr, msgRegex);
    }

    @Test
    public void testGetAllUserTokens_nullReturnsEmptyCollection() throws Exception {
        readsAllTokens(null);
        Collection<OAuth20Token> tokens = oauthTokenStore.getAllUserTokens(username);
        assertTrue("There must be an empty collection.", tokens.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllUserTokens_noTokensReturnsEmptyCollection() throws Exception {
        readsAllTokens(Collections.EMPTY_LIST);

        Collection<OAuth20Token> tokens = oauthTokenStore.getAllUserTokens(username);

        assertTrue("There must be an empty collection.", tokens.isEmpty());
    }

    private void readsAllTokens(final Collection<OAuthToken> oauthTokens) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).readAllTokens(componentId, username);
                will(returnValue(oauthTokens));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#getAll()}.
     */
    @Test
    public void testGetAll() {
        Collection<OAuth20Token> tokens = oauthTokenStore.getAll();

        assertTrue("There must be an empty collection.", tokens.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#getNumTokens(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetNumTokens() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).countTokens(componentId, username, clientId);
                will(returnValue(1));
            }
        });

        int tokenCount = oauthTokenStore.getNumTokens(username, clientId);
        assertEquals("The number of tokens for the user for the client must be counted.", 1, tokenCount);
    }

    @Test
    public void testGetNumTokens_OAuthStoreException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).countTokens(componentId, username, clientId);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });

        int tokenCount = oauthTokenStore.getNumTokens(username, clientId);

        assertEquals("The number of tokens must be 0.", 0, tokenCount);
        String msgRegex = "CWWKS1471E";
        verifyLogMessage(outputMgr, msgRegex);
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#removeByHash(java.lang.String)}.
     */
    @Test
    public void testRemoveByHash() throws Exception {
        deletesToken();
        oauthTokenStore.removeByHash(hash);
    }

    @Test
    public void testRemoveByHash_OAuthStoreException() throws Exception {
        deleteTokenThrowsOAuthStoreException();

        oauthTokenStore.removeByHash(hash);

        String msgRegex = "CWWKS1477E.+" + hash;
        verifyLogMessage(outputMgr, msgRegex);
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#remove(java.lang.String)}.
     */
    @Test
    public void testRemove() throws Exception {
        deletesToken();
        oauthTokenStore.remove(lookupKey);
    }

    @Test
    public void testRemove_OAuthStoreException() throws Exception {
        deleteTokenThrowsOAuthStoreException();

        oauthTokenStore.remove(lookupKey);

        String msgRegex = "CWWKS1477E.+" + hash;
        verifyLogMessage(outputMgr, msgRegex);
    }

    private void deletesToken() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).deleteToken(componentId, hash);
            }
        });
    }

    private void deleteTokenThrowsOAuthStoreException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oauthStore).deleteToken(componentId, hash);
                will(throwException(new OAuthStoreException(defaultExceptionMsg)));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore#stopCleanupThread()}.
     */
    @Ignore
    @Test
    public void testStopCleanupThread() {
        fail("Not yet implemented");
    }

}
