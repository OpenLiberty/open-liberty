/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.common.claims.UserClaimsRetrieverService;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.openidconnect.token.IDToken;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class IDTokenHandlerTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String CFG_KEY_ISSUER_IDENTIFIER = "issuerIdentifier";
    private static final String SHARED_KEY = "sharedKey";
    private static final String SIGNATURE_ALG_NONE = "none";
    private static final String SIGNATURE_ALG_HS256 = "HS256";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static final String NONCE = "nonce";
    private static final Long IDTOKEN_LIFETIME_DEFAULT = new Long(7200);

    private final String oidcProviderId = "oidcOpConfigSample";
    private final String componentId = "OAuthConfigSample";
    private final String clientId = "client01";
    private final String username = "user1";
    private final String redirectUri = "http://localhost/oauthclient/redirect.jsp";
    private final String stateId = "stateId01";
    private final String[] scopes = { "openid", "email" };
    private final String lifeStr = "60000";
    private final String lengthStr = "40";
    // change from net.oauth: shared keys must be >=256 bits for jose4j
    private final String sharedKey = "sharedKeyThatNoOneElseKnowssharedKeyThatNoOneElseKnows";
    private final String differentSharedKey = "differentSharedKeyThatNoOneElseKnowssharedKeyThatNoOneElseKnows";
    private final String issuerIdentifier = "https://www.ibm.com";
    private final String differentIssuer = "https://www.test.ibm.com";
    private final String groupIdentifier = "groupsIds";
    private final String differentGroupIdentifier = "groups";
    private final String accessToken = "testAccessToken";
    private final boolean jtiClaimEnabledDefault = false;
    private final boolean customClaimsEnabledDefault = true;
    private final String signatureAlgorithmDefault = SIGNATURE_ALG_HS256;
    private final String groupIdentifierDefault = "groupsIds";

    private Map<String, String[]> idTokenMap;
    private IDTokenHandler idTokenHandler;
    private List<String> groups;

    private ServiceReference<OidcServerConfig> oidcServerConfigRef;
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = mockery.mock(AtomicServiceReference.class);
    private final KeyStoreService kss = mockery.mock(KeyStoreService.class);
    private OidcServerConfig oidcServerConfig;

    @Before
    public void setUp() {
        idTokenHandler = new IDTokenHandler();
        idTokenMap = createIdTokenMap();
        setupTestGroups();
        createOidcServerConfigExpectations();
        OIDCProvidersConfig.putOidcServerConfig(oidcProviderId, oidcServerConfig);
    }

    private Map<String, String[]> createIdTokenMap() {
        Map<String, String[]> idTokenMap = new HashMap<String, String[]>();
        idTokenMap.put(OAuth20Constants.COMPONENTID, new String[] { componentId });
        idTokenMap.put(OAuth20Constants.CLIENT_ID, new String[] { clientId });
        idTokenMap.put(OAuth20Constants.USERNAME, new String[] { username });
        idTokenMap.put(OAuth20Constants.REDIRECT_URI, new String[] { redirectUri });
        idTokenMap.put(OAuth20Constants.STATE_ID, new String[] { stateId });
        idTokenMap.put(OAuth20Constants.SCOPE, scopes);
        idTokenMap.put(OAuth20Constants.LIFETIME, new String[] { lifeStr });
        idTokenMap.put(OAuth20Constants.LENGTH, new String[] { lengthStr });
        idTokenMap.put(SHARED_KEY, new String[] { sharedKey });
        return idTokenMap;
    }

    private void setupTestGroups() {
        groups = new ArrayList<String>();
        groups.add("group1");
        groups.add("group2");
    }

    @SuppressWarnings("unchecked")
    private void createOidcServerConfigExpectations() {
        oidcServerConfigRef = mockery.mock(ServiceReference.class);
        oidcServerConfig = mockery.mock(OidcServerConfig.class);
        createOidcServerConfigExpectations(oidcProviderId, componentId);
    }

    private void createOidcServerConfigExpectations(final String oidcProviderName, final String oauth20providerName) {
        oidcServerConfigRefExpectations(oidcProviderName);
        oidcServerConfigExpectations(oauth20providerName);
    }

    private void oidcServerConfigRefExpectations(final String oidcProviderName) {
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfigRef).getProperty("id");
                will(returnValue(oidcProviderName));
                allowing(oidcServerConfigRef).getProperty("service.id"); // Related to ConcurrentServiceReferenceMap internal code
                will(returnValue(Long.valueOf(1234)));
                allowing(oidcServerConfigRef).getProperty("service.ranking"); // Related to ConcurrentServiceReferenceMap internal code
                will(returnValue(Integer.valueOf(0)));
            }
        });
    }

    private void oidcServerConfigExpectations(final String oauth20providerName) {
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getProviderId();
                will(returnValue(oidcProviderId));
                allowing(oidcServerConfig).getOauthProviderName();
                will(returnValue(oauth20providerName));
            }
        });
    }

    @After
    public void tearDown() {
        ConfigUtils.setUserClaimsRetrieverService(null);
        OIDCProvidersConfig.removeOidcServerConfig(oidcProviderId);
    }

    @Test
    public void testGetTokenType() {
        assertEquals("The token type must be id_token.", "id_token", idTokenHandler.getTypeTokenType());
    }

    @Test
    public void testGetKeysTokenType() throws Exception {
        AttributeList attributeList = null;
        assertNotNull("There must not be token type keys since id_token is not cached.",
                      idTokenHandler.getKeysTokenType(attributeList));
    }

    @Test
    public void testValidateRequestTokenType() {
        try {
            AttributeList attributeList = null;
            List<OAuth20Token> tokens = Collections.emptyList();
            idTokenHandler.validateRequestTokenType(attributeList, tokens);
        } catch (OAuthException e) {
            fail("There must not be a request token validation exception, but received " + e);
        }
    }

    @Test
    public void testBuildResponseTokenType() {
        AttributeList attributeList = new AttributeList();
        attributeList.setAttribute("test", "testType", new String[] { "values" });
        List<OAuth20Token> tokens = Collections.emptyList();
        idTokenHandler.buildResponseTokenType(attributeList, tokens);
        assertTrue("The attribute list must not be modified.",
                   attributeList.getAllAttributes().size() == 1);
    }

    @Test
    public void testCreateToken() {
        createOIDCTestDefaultExpectations();

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);

        assertNotNull("There must be an id token.", idToken);
    }

    @Test
    public void createToken_plainTextToken() throws Exception {
        oidcCommonExpectations(issuerIdentifier, jtiClaimEnabledDefault, customClaimsEnabledDefault, SIGNATURE_ALG_NONE);

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertTokenIsNotSigned(idToken);
        assertRequiredClaims(idTokenClaimsJSON);
        assertClaimsValues(idTokenClaimsJSON);
    }

    @Test
    public void createToken_signedToken() throws Exception {
        idTokenMap.put(OAuth20Constants.ACCESS_TOKEN, new String[] { accessToken });
        createOIDCTestDefaultExpectations();

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertRequiredClaims(idTokenClaimsJSON);
        assertClaimsValues(idTokenClaimsJSON);
        assertOptionalClaims(idTokenClaimsJSON);
    }

    @Test
    public void createToken_differentIssuer() throws Exception {
        oidcCommonExpectations(differentIssuer, jtiClaimEnabledDefault, customClaimsEnabledDefault, signatureAlgorithmDefault);

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertEquals("The issuer identifier must be set.", differentIssuer, idTokenClaimsJSON.get("iss"));
    }

    @Test
    public void createToken_useIssuerFromRequestIfMissingInConfig() throws Exception {
        idTokenMap.put(CFG_KEY_ISSUER_IDENTIFIER, new String[] { issuerIdentifier });
        oidcCommonExpectations(null, jtiClaimEnabledDefault, customClaimsEnabledDefault, signatureAlgorithmDefault);

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertEquals("The issuer identifier must be set.", issuerIdentifier, idTokenClaimsJSON.get("iss"));
    }

    @Test
    public void createToken_useIssuerFromRequestIfMissingEmptyInConfig() throws Exception {
        idTokenMap.put(CFG_KEY_ISSUER_IDENTIFIER, new String[] { issuerIdentifier });
        oidcCommonExpectations("", jtiClaimEnabledDefault, customClaimsEnabledDefault, signatureAlgorithmDefault);

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertEquals("The issuer identifier must be set.", issuerIdentifier, idTokenClaimsJSON.get("iss"));
    }

    @Test
    public void createToken_validToken_HS256() throws Exception {
        createOIDCTestDefaultExpectations();

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        IDToken token = new IDToken(idToken.getTokenString(), sharedKey.getBytes(), clientId, issuerIdentifier, SIGNATURE_ALG_HS256);

        assertTrue("The id token must be valid.", token.verify());
    }

    @Test
    public void createToken_validToken_differentSharedKeyWithHS256() throws Exception {
        idTokenMap.put(SHARED_KEY, new String[] { differentSharedKey });
        createOIDCTestDefaultExpectations();

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        IDToken token = new IDToken(idToken.getTokenString(), differentSharedKey.getBytes(), clientId, issuerIdentifier, SIGNATURE_ALG_HS256);

        assertTrue("The id token must be valid.", token.verify());
    }

    @Test
    public void createToken_customClaims() throws Exception {
        createOIDCTestDefaultExpectations();
        // Expectations for custom claims
        setupUserClaimsRetrieverService(groupIdentifier);
        createGroupIdentifierExpectation(groupIdentifierDefault);

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertCustomClaims(idTokenClaimsJSON);
    }

    @Test
    public void createToken_customClaims_differentGroupIdentifier() throws Exception {
        createOIDCTestDefaultExpectations();
        // Expectations for custom claims
        setupUserClaimsRetrieverService(differentGroupIdentifier);
        createGroupIdentifierExpectation(differentGroupIdentifier);

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertTrue("There must be a claim for group identifier groupIds.", idTokenClaimsJSON.containsKey(differentGroupIdentifier));
    }

    @Test
    public void createToken_customClaims_disabled() throws Exception {
        oidcCommonExpectations(issuerIdentifier, jtiClaimEnabledDefault, false, signatureAlgorithmDefault);

        final UserClaimsRetrieverService userClaimsRetrieverService = mockery.mock(UserClaimsRetrieverService.class);
        mockery.checking(new Expectations() {
            {
                never(userClaimsRetrieverService).getUserClaims(username, groupIdentifier);
            }
        });

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertCustomClaimsNotThere(idTokenClaimsJSON);
    }

    // For coverage, test that the id token is still built.
    @Test
    public void createToken_noStateId() throws Exception {
        idTokenMap.put(OAuth20Constants.STATE_ID, null);
        createOIDCTestDefaultExpectations();

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertRequiredClaims(idTokenClaimsJSON);
    }

    private void setupUserClaimsRetrieverService(final String groupIdentifier) {
        final UserClaims userClaims = createUserClaims(groupIdentifier);
        UserClaimsRetrieverService userClaimsRetrieverService = createUserClaimsRetrieverService(userClaims, groupIdentifier);
        ConfigUtils.setUserClaimsRetrieverService(userClaimsRetrieverService);
    }

    private UserClaims createUserClaims(String groupIdentifier) {
        final UserClaims user1Claims = new UserClaims(username, groupIdentifier);
        user1Claims.setRealmName("realm1");
        user1Claims.setUniqueSecurityName(username);
        user1Claims.setGroups(groups);
        return user1Claims;
    }

    private UserClaimsRetrieverService createUserClaimsRetrieverService(final UserClaims userClaims, final String groupIdentifier) {
        final UserClaimsRetrieverService userClaimsRetrieverService = mockery.mock(UserClaimsRetrieverService.class);
        mockery.checking(new Expectations() {
            {
                one(userClaimsRetrieverService).getUserClaims(username, groupIdentifier);
                will(returnValue(userClaims));
            }
        });
        return userClaimsRetrieverService;
    }

    private void assertCustomClaims(JSONObject idTokenClaimsJSON) {
        assertTrue("There must be a realm name.", idTokenClaimsJSON.containsKey("realmName"));
        assertTrue("There must be a uniqueSecurityName.", idTokenClaimsJSON.containsKey("uniqueSecurityName"));
        assertTrue("There must be groups.", idTokenClaimsJSON.containsKey(groupIdentifier));
    }

    private void assertCustomClaimsNotThere(JSONObject idTokenClaimsJSON) {
        assertFalse("There must not be a realm name.", idTokenClaimsJSON.containsKey("realmName"));
        assertFalse("There must not be a uniqueSecurityName.", idTokenClaimsJSON.containsKey("uniqueSecurityName"));
        assertFalse("There must not be any groups.", idTokenClaimsJSON.containsKey(groupIdentifier));
    }

    @Test
    public void createToken_without_nonce() throws Exception {
        createOIDCTestDefaultExpectations();

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertEquals("The nonce value should not be set.", null, idTokenClaimsJSON.get(NONCE));
    }

    @Test
    public void createToken_nonce() throws Exception {
        createOIDCTestDefaultExpectations();

        String nonce_value = "asd123qwe!";
        idTokenMap.put(NONCE, new String[] { nonce_value });
        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);

        assertEquals("The nonce value should be set.", nonce_value, idTokenClaimsJSON.get(NONCE));
    }

    @Test
    public void createToken_validToken_differentSharedKeyWithRS256() throws Exception {
        oidcCommonExpectations(issuerIdentifier, jtiClaimEnabledDefault, customClaimsEnabledDefault, SIGNATURE_ALG_RS256);
        JwtUtils.setKeyStoreService(keyStoreServiceRef);

        KeyPairGenerator rsaKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = rsaKeyPairGenerator.generateKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getPrivateKey();
                will(returnValue(privateKey));
                allowing(keyStoreServiceRef).getService();
                will(returnValue(kss));
                allowing(kss).getPrivateKeyFromKeyStore("keystore", "alias", null);
                will(returnValue(privateKey));
                allowing(kss).getX509CertificateFromKeyStore("keystore", "alias");
                will(returnValue(null));

            }
        });

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        IDToken token = new IDToken(idToken.getTokenString(), keyPair.getPublic(), clientId, issuerIdentifier, SIGNATURE_ALG_RS256);

        assertTrue("The id token must be valid.", token.verify());
        JwtUtils.setKeyStoreService(null);

    }

    @Test(expected = RuntimeException.class)
    public void createToken_exceptionGettingRSAPrivateKey_convertsToRuntimeException() throws Exception {
        oidcCommonExpectations(issuerIdentifier, jtiClaimEnabledDefault, customClaimsEnabledDefault, SIGNATURE_ALG_RS256);

        mockery.checking(new Expectations() {
            {
                one(oidcServerConfig).getPrivateKey();
                will(throwException(new KeyStoreException()));
            }
        });

        idTokenHandler.createToken(idTokenMap);
    }

    @Test
    public void createToken_jti() throws Exception {
        oidcCommonExpectations(issuerIdentifier, true, customClaimsEnabledDefault, signatureAlgorithmDefault);

        OAuth20Token idToken = idTokenHandler.createToken(idTokenMap);
        JSONObject idTokenClaimsJSON = getIdTokenClaims(idToken);
        OAuth20Token secondIDToken = idTokenHandler.createToken(idTokenMap);
        JSONObject secondIDTokenClaimsJSON = getIdTokenClaims(secondIDToken);

        assertFalse("The jti claims must not be the same.", idTokenClaimsJSON.get("jti").equals(secondIDTokenClaimsJSON.get("jti")));
    }

    @Test
    public void missingRequiredClaims_iss() {
        Payload payload = new Payload();
        try {
            idTokenHandler.validateRequiredClaims(payload);
            fail("The RuntimeException was not thrown for the missing iss claim.");
        } catch (RuntimeException re) {
            String message = re.getMessage();
            assertTrue(re.getMessage().startsWith("ID Token is missing required claims:"));
            assertTrue(message.contains(" iss"));
        }
    }

    @Test
    public void missingRequiredClaims_sub() {
        Payload payload = new Payload();
        payload.setIssuer(issuerIdentifier);
        try {
            idTokenHandler.validateRequiredClaims(payload);
            fail("The RuntimeException was not thrown for the missing sub claim.");
        } catch (RuntimeException re) {
            String message = re.getMessage();
            assertTrue(message.startsWith("ID Token is missing required claims:"));
            assertTrue(message.contains(" sub"));
        }
    }

    @Test
    public void missingRequiredClaims_aud() {
        Payload payload = new Payload();
        payload.setIssuer(issuerIdentifier);
        payload.setSubject(username);
        try {
            idTokenHandler.validateRequiredClaims(payload);
            fail("The RuntimeException was not thrown for the missing aud claim.");
        } catch (RuntimeException re) {
            String message = re.getMessage();
            assertTrue(message.startsWith("ID Token is missing required claims:"));
            assertTrue(message.contains(" aud"));
        }
    }

    @Test
    public void missingRequiredClaims_exp() {
        Payload payload = new Payload();
        payload.setIssuer(issuerIdentifier);
        payload.setSubject(username);
        payload.setAudience(clientId);
        try {
            idTokenHandler.validateRequiredClaims(payload);
            fail("The RuntimeException was not thrown for the missing exp claim.");
        } catch (RuntimeException re) {
            String message = re.getMessage();
            assertTrue(message.startsWith("ID Token is missing required claims:"));
            assertTrue(message.contains(" exp"));
        }
    }

    @Test
    public void missingRequiredClaims_iat() {
        Payload payload = new Payload();
        payload.setIssuer(issuerIdentifier);
        payload.setSubject(username);
        payload.setAudience(clientId);
        payload.setExpirationTimeSeconds(new Date().getTime() / 1000);
        try {
            idTokenHandler.validateRequiredClaims(payload);
            fail("The RuntimeException was not thrown for the missing iat claim.");
        } catch (RuntimeException re) {
            String message = re.getMessage();
            assertTrue(message.startsWith("ID Token is missing required claims:"));
            assertTrue(message.contains(" iat"));
        }
    }

    private JSONObject getIdTokenClaims(OAuth20Token idToken) throws IOException {
        String[] segments = getIdTokenSegments(idToken);
        String base64EncodedClaims = segments[1];
        String idTokenClaims = StringUtils.newStringUtf8(Base64.decodeBase64(base64EncodedClaims));
        System.out.println("id_token claims " + idTokenClaims);
        JSONObject idTokenClaimsJSON = JSONObject.parse(idTokenClaims);
        System.out.println("idTokenClaimsJSON " + idTokenClaimsJSON);
        return idTokenClaimsJSON;
    }

    private void assertTokenIsNotSigned(OAuth20Token idToken) {
        String[] segments = getIdTokenSegments(idToken);
        assertTrue("The id token must not be signed.", segments.length < 3);
    }

    private String[] getIdTokenSegments(OAuth20Token idToken) {
        String idTokenJWT = idToken.getTokenString();
        String[] segments = idTokenJWT.split("\\.");
        return segments;
    }

    private void assertRequiredClaims(JSONObject idTokenClaimsJSON) {
        assertTrue("There must be an issuer identifier.", idTokenClaimsJSON.containsKey("iss"));
        assertTrue("There must be a subject identifier.", idTokenClaimsJSON.containsKey("sub"));
        assertTrue("There must be an audience.", idTokenClaimsJSON.containsKey("aud"));
        assertTrue("There must be an expiration time.", idTokenClaimsJSON.containsKey("exp"));
        assertTrue("There must be an issuance time.", idTokenClaimsJSON.containsKey("iat"));
    }

    private void assertClaimsValues(JSONObject idTokenClaimsJSON) {
        assertTrue("The issuer identifier must use the https scheme.",
                   ((String) idTokenClaimsJSON.get("iss")).startsWith("https"));
        assertEquals("The subject identifier must be set.", username, idTokenClaimsJSON.get("sub"));
        assertEquals("The audience must be set.", clientId, idTokenClaimsJSON.get("aud"));
    }

    private void assertOptionalClaims(JSONObject idTokenClaimsJSON) {
        assertTrue("There should be an access token hash.", idTokenClaimsJSON.containsKey("at_hash"));
    }

    private void createOIDCTestDefaultExpectations() {
        oidcCommonExpectations(issuerIdentifier, jtiClaimEnabledDefault, customClaimsEnabledDefault, signatureAlgorithmDefault);
    }

    private void oidcCommonExpectations(final String issuerIdentifier,
                                        final boolean jtiClaimEnabled,
                                        final boolean customClaimsEnabled,
                                        final String signatureAlgorithm) {
        try {
            mockery.checking(new Expectations() {
                {
                    // bt: wait for 8239
                    //allowing(oidcServerConfig).getKeyAliasName();
                    //will(returnValue(null));
                    //allowing(oidcServerConfig).getKeyStoreRef();
                    //will(returnValue(null));
                    allowing(oidcServerConfig).getJSONWebKey();
                    will(returnValue((JSONWebKey) null));
                    allowing(oidcServerConfig).getIdTokenLifetime();
                    will(returnValue(IDTOKEN_LIFETIME_DEFAULT));
                    allowing(oidcServerConfig).getIssuerIdentifier();
                    will(returnValue(issuerIdentifier));
                    allowing(oidcServerConfig).isJTIClaimEnabled();
                    will(returnValue(jtiClaimEnabled));
                    allowing(oidcServerConfig).isCustomClaimsEnabled();
                    will(returnValue(customClaimsEnabled));
                    allowing(oidcServerConfig).getCustomClaims();
                    will(returnValue((String[]) null));
                    allowing(oidcServerConfig).getSignatureAlgorithm();
                    will(returnValue(signatureAlgorithm));
                    allowing(oidcServerConfig).isJwkEnabled();
                    will(returnValue(false));
                    allowing(oidcServerConfig).getKeyAliasName();
                    will(returnValue("alias"));
                    allowing(oidcServerConfig).getKeyStoreRef();
                    will(returnValue("keystore"));
                }
            });
        } catch (Exception e) {

            e.printStackTrace(System.out);
        }
    }

    // Expectations for custom claims
    private void createGroupIdentifierExpectation(final String groupIdentifier) {
        mockery.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getGroupIdentifier();
                will(returnValue(groupIdentifier));
            }
        });
    }

}
