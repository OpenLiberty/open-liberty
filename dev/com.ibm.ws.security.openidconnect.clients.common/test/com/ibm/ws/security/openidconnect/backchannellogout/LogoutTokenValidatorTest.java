/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backchannellogout;

import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jmock.Expectations;
import org.jose4j.base64url.Base64;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class LogoutTokenValidatorTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.common.*=all=enabled");

    final String CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS = "CWWKS1536E";
    final String CWWKS1537E_OIDC_CLIENT_JWE_REQUIRED_BUT_TOKEN_NOT_JWE = "CWWKS1537E";
    final String CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR = "CWWKS1543E";
    final String CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS = "CWWKS1545E";
    final String CWWKS1751E_OIDC_IDTOKEN_VERIFY_ISSUER_ERR = "CWWKS1751E";
    final String CWWKS1754E_OIDC_IDTOKEN_VERIFY_AUD_ERR = "CWWKS1754E";
    final String CWWKS1778E_OIDC_JWT_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR = "CWWKS1778E";
    final String CWWKS1784E_JWT_MISSING_ISSUER = "CWWKS1784E";

    final String CONFIG_ID = "myConfigId";
    final String CLIENT_ID = "client01";
    final String SHARED_SECRET = "secret";
    final String ISSUER = "https://localhost/oidc/provider/OP";
    final String TOKEN_ENDPOINT = ISSUER + "/token";
    final String SUBJECT = "testuser";
    final String JTI_VALID_NO_SID = "valid-noSid";
    final String EVENTS_MEMBER_KEY = "http://schemas.openid.net/event/backchannel-logout";

    final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);
    final ConsumerUtils consumerUtils = mockery.mock(ConsumerUtils.class);

    LogoutTokenValidator validator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        validator = new LogoutTokenValidator(clientConfig);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_validateToken_nullString() throws Exception {
        String logoutTokenString = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_emptyString() throws Exception {
        String logoutTokenString = "";
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_notAJwt() throws Exception {
        String logoutTokenString = "This is not a jwt.";
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_jwsRequired_jweFormat() throws Exception {
        String logoutTokenString = "aaa.bbb.ccc.ddd.eee";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_jweRequired_jwsFormat() throws Exception {
        String logoutTokenString = "aaa.bbb.ccc";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue("someAlias"));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1537E_OIDC_CLIENT_JWE_REQUIRED_BUT_TOKEN_NOT_JWE);
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_configAlgHs256() throws Exception {
        String logoutTokenString = encode(getJwsHeader("none")) + "." + encode("{}") + ".";
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1778E_OIDC_JWT_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR);
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_configAlgNone_emptyClaims() throws Exception {
        String logoutTokenString = encode(getJwsHeader("none")) + "." + encode("{}") + ".";
        try {
            setConfigExpectations("none", null, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + "iss, aud, iat, jti, events");
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_minimumClaims() throws Exception {
        String logoutTokenString = encode(getJwsHeader("none")) + "." + encode(getMinimumClaimsNoSid()) + ".";
        try {
            setConfigExpectations("none", null, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
        } catch (BackchannelLogoutException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    @Test
    public void test_validateToken_hs256_keyMismatch() throws Exception {
        JsonObject claims = new JsonObject();
        String logoutTokenString = getHS256Jws(claims, "secret2");
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + "Invalid JWS Signature");
        }
    }

    @Test
    public void test_validateToken_hs256_emptyClaims() throws Exception {
        JsonObject claims = new JsonObject();
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + "iss, aud, iat, jti, events");
        }
    }

    @Test
    public void test_validateToken_hs256_missingIss() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.ISSUER;
        claims.remove(claimToRemove);
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_validateToken_hs256_missingAud() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.AUDIENCE;
        claims.remove(claimToRemove);
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_validateToken_hs256_missingIat() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.ISSUED_AT;
        claims.remove(claimToRemove);
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_validateToken_hs256_missingJti() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        String claimToRemove = Claims.ID;
        claims.remove(claimToRemove);
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_validateToken_hs256_missingEvents() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        String claimToRemove = "events";
        claims.remove(claimToRemove);
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1545E_LOGOUT_TOKEN_MISSING_CLAIMS + ".*" + claimToRemove + "[^,]");
        }
    }

    @Test
    public void test_validateToken_hs256_badIss() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        claims.addProperty(Claims.ISSUER, "https://localhost/oidc/provider/NOPE");
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1751E_OIDC_IDTOKEN_VERIFY_ISSUER_ERR);
        }
    }

    @Test
    public void test_validateToken_hs256_badAud() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        claims.addProperty(Claims.AUDIENCE, "client02");
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1754E_OIDC_IDTOKEN_VERIFY_AUD_ERR);
        }
    }

    // TODO

    @Test
    public void test_validateToken_hs256_minimumClaims() throws Exception {
        JsonObject claims = getMinimumClaimsNoSid();
        String logoutTokenString = getHS256Jws(claims, SHARED_SECRET);
        try {
            setConfigExpectations("HS256", SHARED_SECRET, 300L, ISSUER);

            validator.validateToken(logoutTokenString);
        } catch (BackchannelLogoutException e) {
            fail("Should not have thrown an exception but did: " + e);
        }
    }

    private JsonObject getJwsHeader(String alg) {
        JsonObject header = new JsonObject();
        header.addProperty("typ", "JWT");
        header.addProperty("alg", alg);
        return header;
    }

    private JsonObject getHS256Header() {
        return getJwsHeader("HS256");
    }

    private JsonObject getMinimumClaimsNoSid() {
        JsonObject claims = new JsonObject();
        claims.addProperty(Claims.ISSUER, ISSUER);
        claims.addProperty(Claims.AUDIENCE, CLIENT_ID);
        claims.addProperty(Claims.ISSUED_AT, System.currentTimeMillis() / 1000);
        claims.addProperty(Claims.ID, JTI_VALID_NO_SID);
        claims.add("events", getValidEventsEntry());
        claims.addProperty(Claims.SUBJECT, SUBJECT);
        return claims;
    }

    private JsonObject getValidEventsEntry() {
        JsonObject events = new JsonObject();
        events.add(EVENTS_MEMBER_KEY, new JsonObject());
        return events;
    }

    private String getHS256Jws(JsonObject claims, String secret) throws Exception {
        String headerAndPayload = encode(getHS256Header()) + "." + encode(claims);
        String signature = getHS256Signature(headerAndPayload, secret);
        return headerAndPayload + "." + signature;
    }

    private String getHS256Signature(String input, String secret) throws Exception {
        byte[] secretBytes = secret.getBytes("UTF-8");
        Mac hs256Mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
        hs256Mac.init(keySpec);
        byte[] hashBytes = hs256Mac.doFinal(input.getBytes("UTF-8"));
        return Base64.encode(hashBytes);
    }

    private String encode(Object input) throws UnsupportedEncodingException {
        return Base64.encode(input.toString().getBytes("UTF-8"));
    }

    private void setConfigExpectations(String signatureAlgorithm, String sharedKey, long clockSkew, String issuerIdentifier) {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getKeyManagementKeyAlias();
                will(returnValue(null));
                allowing(clientConfig).getSignatureAlgorithm();
                will(returnValue(signatureAlgorithm));
                allowing(clientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                allowing(clientConfig).getClockSkewInSeconds();
                will(returnValue(clockSkew));
                allowing(clientConfig).getIssuerIdentifier();
                will(returnValue(issuerIdentifier));
            }
        });
        if (sharedKey != null) {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getSharedKey();
                    will(returnValue(sharedKey));
                }
            });
        }
    }

}
