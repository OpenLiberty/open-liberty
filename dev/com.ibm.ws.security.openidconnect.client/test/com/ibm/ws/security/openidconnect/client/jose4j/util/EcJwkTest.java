/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.jose4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.common.jwk.impl.Jose4jEllipticCurveJWK;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
// import com.ibm.ws.security.common.jwk.interfaces.JSONWebKey;
import com.ibm.ws.security.oauth20.plugins.jose4j.JWTData;
import com.ibm.ws.security.oauth20.plugins.jose4j.JwtCreator;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.common.OidcCommonClientRequest;
import com.ibm.ws.security.openidconnect.server.internal.MockJWKProvider;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class EcJwkTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final static MockJWKProvider jwkProvider = new MockJWKProvider(256, "ES256", 20 * 60 * 1000);
    final OidcServerConfig oidcServerConfig = mock.mock(OidcServerConfig.class, "oidcServerConfig");
    final JWTData jwtData = mock.mock(JWTData.class, "jwtData");
    final ConvergedClientConfig oidcClientConfig = mock.mock(ConvergedClientConfig.class, "oidcClientConfig");
    final OidcClientRequest oidcClientRequest = mock.mock(OidcClientRequest.class, "oidcClientRequest");
    final JwKRetriever jwkRetriever = mock.mock(JwKRetriever.class, "jwkTriever");
    final JWKSet jwkset = mock.mock(JWKSet.class, "jwkset");
    final SSLSupport sslsupport = mock.mock(SSLSupport.class, "sslsupport");

    static final String issuerIdentifier = "https://localhost:8947/oidc/endpoint/OidcConfigSample";
    static JwtContext jwtContext = null;
    static Map<String, String[]> tokenMap = new HashMap<String, String[]>();
    static {
        tokenMap.put("state_id", new String[] { "state_id_xyz" });
        tokenMap.put("username", new String[] { "testuser" });
        tokenMap.put("scope", new String[] { "openid", "profile" });
        tokenMap.put("grant_type", new String[] { "authorization_code" });
        tokenMap.put("redirect_uri", new String[] { "https://localhost:8946/oidcclient/redirect/client01" });
        tokenMap.put("issuerIdentifier", new String[] { issuerIdentifier });
        tokenMap.put("COMPONENTID", new String[] { "OAuthConfigSample" });
        tokenMap.put("sharedKey", new String[] { "secret" });
        tokenMap.put("client_id", new String[] { "client01" });
        tokenMap.put("LENGTH", new String[] { "50" });
        tokenMap.put("LIFETIME", new String[] { "3600000" });
        tokenMap.put("access_token", new String[] { "access_token_id_xyz123" });

    };
    static Map<String, Object> userClaims = new HashMap<String, Object>();
    static {
        userClaims.put("realmName", "BasicRealm");
        userClaims.put("uniqueSecurityName", "testuser");
        userClaims.put("at_hash", "mIv-RHqnJS8sAePoSvqAzw");
    }

    static JSONWebKey jsonWebKey = null;
    static String jwtString = null;
    static boolean bJwtString = false;
    static JSONObject jsonObject = null;
    static {
        jwkProvider.setAlg("ES256");
        jsonWebKey = jwkProvider.getJWK(); // this has generated the jwks
        if (jsonWebKey instanceof Jose4jEllipticCurveJWK) {
            Jose4jEllipticCurveJWK ecJwk = (Jose4jEllipticCurveJWK) jsonWebKey;
            jsonObject = ecJwk.getJsonObject();
        }
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    private class MyJose4jUtil extends Jose4jUtil {
        public MyJose4jUtil(SSLSupport sslSupport) {
            super(sslSupport);
        }

        @Override
        public JwKRetriever createJwkRetriever(ConvergedClientConfig config) {
            return jwkRetriever;
        }
    }

    @Test
    public void testJWTCreaterEC() {
        if (bJwtString) {// tested
            return;
        }
        final String methodName = "testJWTCreaterEC";
        bJwtString = true;
        jwtString = jwtCreaterEC(false);
        assertNotNull("The jwtString is expect to have a String but it returns null", jwtString);
        assertNotNull("The jsonObject is expect to have an instance but it is null", jsonObject);
        // assertNull("The jwtString is:" + jwtString, jwtString); // debugging
        // assertNull("The jsonObject is " + jsonObject, jsonObject);
    }

    // really create the jwt
    String jwtCreaterEC(boolean microProfileFormat) {
        mock.checking(new Expectations() {
            {
                allowing(jwtData).isJwt();
                will(returnValue(true));
                allowing(oidcServerConfig).getGroupIdentifier();
                will(returnValue("groupIds"));
                one(oidcServerConfig).isJTIClaimEnabled();
                will(returnValue(true));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(oidcServerConfig).isCustomClaimsEnabled();
                will(returnValue(false));
                one(jwtData).getSigningKey();
                will(returnValue(jsonWebKey.getPrivateKey()));
                one(jwtData).getKeyID();
                will(returnValue(jsonWebKey.getKeyID()));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("ES256"));
                allowing(oidcServerConfig).getProviderId();
                will(returnValue("oidcServerSample"));
            }
        });

        String result = null;
        try {
            result = JwtCreator.createJwtAsString(oidcServerConfig, "client01", "testuser",
                    new String[] { "openid", "profile" }, 7200,
                    tokenMap, userClaims, jwtData, microProfileFormat);

        } catch (Throwable t) {
            outputMgr.failWithThrowable("jwtCreaterEC", t);
        }
        return result;
    }

    @SuppressWarnings("static-access")
    //@Test - TODO - move to OL
    public void testCreateMicproProfileFormatJWT() {
        String methodName = "testCreateMicproProfileFormatJWT";
        try {
            Jose4jUtil jose4jUtil = new Jose4jUtil(null);
            String jwtStr = jwtCreaterEC(true);
            JwtContext jwtContext = jose4jUtil.parseJwtWithoutValidation(jwtStr);
            assertNotNull("The jwtContext is expect to have an instance but it returns null", jwtContext);
            JwtClaims jwtClaims = jwtContext.getJwtClaims();
            assertNotNull("The jwtClaims is expected to an instance but it return null", jwtClaims);
            String issuer = jwtClaims.getIssuer();
            assertEquals("issuer is not " + issuerIdentifier + " but " + issuer, issuerIdentifier, issuer);
            System.out.println("**** raw claims json:\n " + jwtClaims.getRawJson()); // handy for debug
            Map<String, Object> claimsMap = jwtClaims.getClaimsMap();
            // sub should be the username, same as before.
            String user = (String) claimsMap.get("sub");
            assertTrue("expected sub to be testuser but was " + user, user.equals("testuser"));
            // realmName should be gone.
            String realm = (String) claimsMap.get("realmName");
            // upn and groups need a registry from open liberty we can't readily mock, will have to be FAT.
            assertTrue("expected realmName to be null but was: >" + realm + "<", realm == null);
            // azp should be gone (otherwise would be = clientId)
            String azp = (String) claimsMap.get("azp");
            assertTrue("expected azp to be null but was: >" + azp + "<", azp == null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @SuppressWarnings("static-access")
    //@Test
    public void testJWTValidaterEC() {
        final String methodName = "testJWTValidaterEC";

        if (!bJwtString) { // the JWTString is not created yet
            testJWTCreaterEC();
        }

        // Let's veryfy the jsonObject is ec
        String strKty = (String) jsonObject.get("kty");
        assertEquals("kty is not EC but " + strKty, strKty, "EC");

        final JWK jwk = Jose4jEllipticCurveJWK.getInstance(jsonObject);
        assertNotNull("The jwk is expect to have an instance but it is null", jwk);

        final List<String> audiences = new ArrayList<String>();
        audiences.add(OidcCommonClientRequest.ALL_AUDIENCES);
        try {
            mock.checking(new Expectations() {
                {
                    one(oidcClientConfig).getSignatureAlgorithm();
                    will(returnValue("RS256")); // We may need to change this value to "ES256" later if we do support EC
                    one(oidcClientConfig).getJwkEndpointUrl();
                    will(returnValue(issuerIdentifier + "/jwk"));
                    one(jwkRetriever).getPublicKeyFromJwk(jwk.getKeyID(), null, false);
                    will(returnValue(jwk.getPublicKey()));
                    one(oidcClientConfig).getClockSkewInSeconds();
                    will(returnValue(300L));
                    one(oidcClientConfig).getIssuerIdentifier();
                    will(returnValue(issuerIdentifier));
                    one(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                    one(oidcClientConfig).getSignatureAlgorithm();
                    will(returnValue("ES256"));
                    allowing(oidcClientRequest).getTokenType();
                    will(returnValue(oidcClientRequest.TYPE_JWT_TOKEN));
                    one(oidcClientRequest).disableIssChecking();
                    will(returnValue(false));
                    one(oidcClientRequest).getAudiences();
                    will(returnValue(audiences));
                    one(oidcClientRequest).allowedAllAudiences();
                    will(returnValue(true));
                }
            });
        } catch (Exception e) {
            outputMgr.failWithThrowable(methodName, e);
        }
        Jose4jUtil jose4jUtil = new Jose4jUtil(null);
        try {
            JwtContext jwtContext = jose4jUtil.parseJwtWithoutValidation(jwtString);
            assertNotNull("The jwtContext is expect to have an instance but it returns null", jwtContext);
            jose4jUtil = createJwkRetrieverConstructorExpectations();
            JwtClaims jwtClaims = jose4jUtil.parseJwtWithValidation(oidcClientConfig, jwtString, jwtContext, oidcClientRequest);
            assertNotNull("The jwtClaims is expected to an instance but it return null", jwtContext);
            String issuer = jwtClaims.getIssuer();
            assertEquals("issuer is not " + issuerIdentifier + " but " + issuer, issuerIdentifier, issuer);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private Jose4jUtil createJwkRetrieverConstructorExpectations() {

        return new MyJose4jUtil(null);
        //        mock.checking(new Expectations() {
        //            {
        //
        //                allowing(clientConfig).getSslRef();
        //                will(returnValue("sslRef"));
        //                allowing(clientConfig).getJwkEndpointUrl();
        //                will(returnValue("http://localhost:TEST_JWK_ENDPOINT"));
        //                allowing(clientConfig).getId();
        //                will(returnValue("http://localhost:TEST_JWK_ENDPOINT"));
        //                allowing(clientConfig).getJwkSet();
        //                will(returnValue(null));
        //                allowing(clientConfig).isHostNameVerificationEnabled();
        //                will(returnValue(false));
        //                allowing(clientConfig).getJwkClientId();
        //                will(returnValue(null));
        //                allowing(clientConfig).getJwkClientSecret();
        //                will(returnValue(null));
        //            }
        //        });
    }

    //    @SuppressWarnings("static-access")
    //    @Test
    public void testJWTValidaterECBad() {
        final String methodName = "testJWTValidaterECBad";

        if (!bJwtString) { // the JWTString is not created yet
            testJWTCreaterEC();
        }

        // Let's veryfy the jsonObject is ec
        String strKty = (String) jsonObject.get("kty");
        assertEquals("kty is not EC but " + strKty, strKty, "EC");

        final JWK jwk = Jose4jEllipticCurveJWK.getInstance(jsonObject);
        assertNotNull("The jwk is expect to have an instance but it is null", jwk);

        // let's make an bad jwtString
        String badJwtString = null;
        String[] jwtParts = jwtString.split("\\.");
        assertTrue("jwtParts are not 3 parts but " + jwtParts.length, jwtParts.length == 3);
        badJwtString = jwtParts[0] + "." + jwtParts[1] + "." + "badSignatureXYZ123456";

        final List<String> audiences = new ArrayList<String>();
        audiences.add(OidcCommonClientRequest.ALL_AUDIENCES);
        try {
            mock.checking(new Expectations() {
                {
                    one(oidcClientConfig).getSignatureAlgorithm();
                    will(returnValue("RS256")); // We may need to change this value to "ES256" later if we do support EC
                    one(oidcClientConfig).getJwkEndpointUrl();
                    will(returnValue(issuerIdentifier + "/jwk"));
                    one(jwkRetriever).getPublicKeyFromJwk(jwk.getKeyID(), null, false);
                    will(returnValue(jwk.getPublicKey()));
                    one(oidcClientConfig).getClockSkewInSeconds();
                    will(returnValue(300L));
                    one(oidcClientConfig).getIssuerIdentifier();
                    will(returnValue(issuerIdentifier));
                    one(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                    one(oidcClientConfig).getSignatureAlgorithm();
                    will(returnValue("ES256"));
                    allowing(oidcClientRequest).getTokenType();
                    will(returnValue(oidcClientRequest.TYPE_JWT_TOKEN));
                    one(oidcClientRequest).disableIssChecking();
                    will(returnValue(false));
                    one(oidcClientRequest).getAudiences();
                    will(returnValue(audiences));
                    one(oidcClientRequest).allowedAllAudiences();
                    will(returnValue(true));
                    one(oidcClientRequest).errorCommon(with(any(String[].class)), with(any(String[].class)));
                    allowing(oidcClientRequest).getTokenType();
                    will(returnValue(oidcClientRequest.TYPE_JWT_TOKEN));
                }
            });
        } catch (Exception e) {
            outputMgr.failWithThrowable(methodName, e);
        }
        Jose4jUtil jose4jUtil = new Jose4jUtil(null);
        try {
            JwtContext jwtContext = jose4jUtil.parseJwtWithoutValidation(badJwtString);
            assertNotNull("The jwtContext is expect to have an instance but it returns null", jwtContext);
            jose4jUtil = createJwkRetrieverConstructorExpectations();
            JwtClaims jwtClaims = jose4jUtil.parseJwtWithValidation(oidcClientConfig, badJwtString, jwtContext, oidcClientRequest);
            fail("This is expected to throw exception but did not");
        } catch (Throwable t) {
            if (t instanceof com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException) {
                String errMsg = t.getMessage();
                assertTrue("Error messages did not start with 'JWS signature is invalid' but '" + errMsg + "'",
                        errMsg.startsWith("JWS signature is invalid"));
            } else {
                outputMgr.failWithThrowable(methodName, t);
            }
        }
    }

    //@Test
    public void testParseECJwk() {
        JwKRetriever jwkRetriever = new JwKRetriever("id", "sslref", "jwkep", jwkset, sslsupport, false, null, null, "ES256");
        String jwksString = jwkProvider.getJwkSetString();
        JWKSet jwkset = new JWKSet();

        boolean bJwk = jwkRetriever.parseJwk(jwksString, null, jwkset, "ES256");
        assertTrue("No EC JWk was parsing", bJwk);
        List<JWK> jwks = jwkset.getJWKs();
        int iCnt = 0;
        for (JWK jwk : jwks) {
            assertTrue("It is not Jose4jEllipticCurveJWK", jwk instanceof Jose4jEllipticCurveJWK);
            Jose4jEllipticCurveJWK ecJwk = (Jose4jEllipticCurveJWK) jwk;
            JSONObject ecJsonObject = ecJwk.getJsonObject();
            // Let's veryfy the jsonObject is ec
            //"kty": "EC",
            String strKty = (String) ecJsonObject.get("kty");
            assertEquals("kty is not EC but " + strKty, strKty, "EC");
            //"kid": "jwt_ec384_signer",

            String strKid = (String) ecJsonObject.get("kid");
            assertFalse("kid is null", strKid == null);
            //"use": "sig",
            String strUse = (String) ecJsonObject.get("use");
            assertEquals("strUse is not sig but " + strUse, strUse, "sig");
            //"x": "OhrGpYrRC_u6eDXUYosmfmM1sWF8zkDgifztbJ694bVu3nUV6xY7jWZhWhUk0Kfe",
            String strX = (String) ecJsonObject.get("x");
            assertFalse("strX should not be null " + strX, strX == null);
            //"y": "1zSvB8He-QmBhqXlQ1FYUQyrFTDjDfSFj52StSJNFXbH_B2v8frhmNVv5UOaLBAM",
            String strY = (String) ecJsonObject.get("y");
            assertFalse("strY should not be null " + strY, strY == null);
            //"crv": "P-256"
            String strCrv = (String) ecJsonObject.get("crv");
            assertEquals("crv is not P-256 but" + strCrv, strCrv, "P-256");
            iCnt++;
        }
        // assertTrue("jwk is less 2 instances:" + iCnt, iCnt >= 2); TODO: OL implementation is generating 1 jwk by default, CL was generating 2
    }

    @Test
    public void testJWTRS256ECBad() {
        final String methodName = "testJWTRS256ECBad";
        MockJWKProvider rsJwkProvider = new MockJWKProvider(256, "RS256", 20 * 60 * 1000);
        rsJwkProvider.setAlg("RS256");
        final JSONWebKey rsJsonWebKey = rsJwkProvider.getJWK(); // get RS256 jwk
        JSONObject rsJsonObject = null;
        if (rsJsonWebKey instanceof Jose4jEllipticCurveJWK) {
            Jose4jEllipticCurveJWK ecJwk = (Jose4jEllipticCurveJWK) rsJsonWebKey;
            rsJsonObject = ecJwk.getJsonObject();
        }
        assertNotNull("rsJsonObject is null", rsJsonObject);

        // Let's veryfy the jsonObject is ec
        String strKty = (String) rsJsonObject.get("kty");
        assertEquals("kty is not EC but " + strKty, strKty, "EC");

        final JWK jwk = Jose4jEllipticCurveJWK.getInstance(rsJsonObject);
        assertNotNull("The jwk is expect to have an instance but it is null", jwk);

        String rsJwtString = null;
        mock.checking(new Expectations() {
            {
                one(jwtData).isJwt();
                will(returnValue(true));
                one(oidcServerConfig).isJTIClaimEnabled();
                will(returnValue(true));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(oidcServerConfig).isCustomClaimsEnabled();
                will(returnValue(false));
                one(jwtData).getSigningKey();
                will(returnValue(rsJsonWebKey.getPrivateKey()));
                one(jwtData).getKeyID();
                will(returnValue(rsJsonWebKey.getKeyID()));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("RS256")); // ES256
                allowing(oidcServerConfig).getProviderId();
                will(returnValue("oidcServerSample"));
            }
        });

        try {
            rsJwtString = JwtCreator.createJwtAsString(oidcServerConfig, "client01", "testuser",
                    new String[] { "openid", "profile" }, 7200,
                    tokenMap, userClaims, jwtData, false);
            fail("This should have failed but not");
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                String errMsg = t.getMessage();
                assertTrue("Error messages did not start with 'CWWKS1456E' but '" + errMsg + "'",
                        errMsg.startsWith("CWWKS1456E"));
            } else {
                outputMgr.failWithThrowable(methodName, t);
            }
        }

    }

    //@Test
    public void testJWTRS256ECBad2() {
        final String methodName = "testJWTRS256ECBad2";
        MockJWKProvider rsJwkProvider = new MockJWKProvider(256, "RS256", 20 * 60 * 1000);
        rsJwkProvider.setAlg("RS256");
        final JSONWebKey rsJsonWebKey = rsJwkProvider.getJWK(); // get RS256 jwk
        JSONObject rsJsonObject = null;
        if (rsJsonWebKey instanceof Jose4jEllipticCurveJWK) {
            Jose4jEllipticCurveJWK ecJwk = (Jose4jEllipticCurveJWK) rsJsonWebKey;
            rsJsonObject = ecJwk.getJsonObject();
        }
        assertNotNull("rsJsonObject is null", rsJsonObject);

        // Let's veryfy the jsonObject is ec
        String strKty = (String) rsJsonObject.get("kty");
        assertEquals("kty is not EC but " + strKty, strKty, "EC");

        final JWK jwk = Jose4jEllipticCurveJWK.getInstance(rsJsonObject);
        assertNotNull("The jwk is expect to have an instance but it is null", jwk);

        String rsJwtString = null;
        mock.checking(new Expectations() {
            {
                one(jwtData).isJwt();
                will(returnValue(true));
                one(oidcServerConfig).isJTIClaimEnabled();
                will(returnValue(true));
                one(oidcServerConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
                one(oidcServerConfig).isCustomClaimsEnabled();
                will(returnValue(false));
                one(jwtData).getSigningKey();
                will(returnValue(rsJsonWebKey.getPrivateKey()));
                one(jwtData).getKeyID();
                will(returnValue(rsJsonWebKey.getKeyID()));
                one(oidcServerConfig).getSignatureAlgorithm();
                will(returnValue("ES256")); // ES256
                allowing(oidcServerConfig).getProviderId();
                will(returnValue("oidcServerSample"));
            }
        });

        try {
            rsJwtString = JwtCreator.createJwtAsString(oidcServerConfig, "client01", "testuser",
                    new String[] { "openid", "profile" }, 7200,
                    tokenMap, userClaims, jwtData, false);
            assertNotNull("The rsJwtString is expect to have a String but it returns null", rsJwtString);
            assertNotNull("The rsJsonObject is expect to have an instance but it is null", rsJsonObject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

        final List<String> audiences = new ArrayList<String>();
        audiences.add(OidcCommonClientRequest.ALL_AUDIENCES);
        try {
            mock.checking(new Expectations() {
                {
                    one(oidcClientConfig).getSignatureAlgorithm();
                    will(returnValue("RS256")); // We may need to change this value to "ES256" later if we do support EC
                    one(oidcClientConfig).getJwkEndpointUrl();
                    will(returnValue(issuerIdentifier + "/jwk"));
                    one(jwkRetriever).getPublicKeyFromJwk(jwk.getKeyID(), null, false);
                    will(returnValue(jwk.getPublicKey()));
                    one(oidcClientConfig).getClockSkewInSeconds();
                    will(returnValue(300L));
                    one(oidcClientConfig).getIssuerIdentifier();
                    will(returnValue(issuerIdentifier));
                    one(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                    one(oidcClientConfig).getSignatureAlgorithm();
                    will(returnValue("RS256")); // ES256
                    allowing(oidcClientRequest).getTokenType();
                    will(returnValue(OidcClientRequest.TYPE_JWT_TOKEN));
                    one(oidcClientRequest).disableIssChecking();
                    will(returnValue(false));
                    one(oidcClientRequest).getAudiences();
                    will(returnValue(audiences));
                    one(oidcClientRequest).allowedAllAudiences();
                    will(returnValue(true));
                    one(oidcClientRequest).errorCommon(with(any(Boolean.class)), with(any(TraceComponent.class)),
                            with(any(String[].class)), with(any(Object[].class)));
                    //one(oidcClientRequest).getTokenType();
                    //will(returnValue(oidcClientRequest.TYPE_JWT_TOKEN));
                }
            });
        } catch (Exception e) {
            outputMgr.failWithThrowable(methodName, e);
        }
        Jose4jUtil jose4jUtil = new Jose4jUtil(null);
        try {
            JwtContext jwtContext = jose4jUtil.parseJwtWithoutValidation(rsJwtString);
            assertNotNull("The jwtContext is expect to have an instance but it returns null", jwtContext);
            jose4jUtil = createJwkRetrieverConstructorExpectations();
            JwtClaims jwtClaims = jose4jUtil.parseJwtWithValidation(oidcClientConfig, rsJwtString, jwtContext, oidcClientRequest);
            fail("This should have failed but  not");

        } catch (Throwable t) {
            if (t instanceof com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException) {
                String errMsg = t.getMessage();
                assertTrue("Error messages should be '' but it's '" + errMsg + "'",
                        errMsg.isEmpty());
            } else {
                outputMgr.failWithThrowable(methodName, t);
            }
        }
    }
}
