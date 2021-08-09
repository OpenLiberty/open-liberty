/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.sharedTests;

import java.util.List;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MangleJWTTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test class for general JAXRS OAuth tests
 *
 * @author chrisc
 *
 */
public class JwtCommonTests extends MangleJWTTestTools {

    protected static Class<?> thisClass = JwtCommonTests.class;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.RSserver";

    /************************************* General token mangling ***************************************/

    /**
     * A general test to show that the JWT Token creator code is working properly
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void NoOPMangleJWT1ServerTests_test_testgeneratedToken() throws Exception {

        positiveTest(testSettings);

    }

    /**
     * Create a JWT token with an bad "iss" value (value that will not match what is in the oidcclient config)
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_iss() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_ISSUER, badString);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + testSettings.getJwtId() + ".+" + MessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED + ".+\\[" + badString + "\\]" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }
        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token without an "iss" value. The oidcclient config has disableIssChecking set to false, so we will
     * require an iss and that needs to match what is in the isserIdentifier of the config
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_iss_disableIssCheckingFalse() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_ISSUER, null);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + testSettings.getJwtId() + ".+" + MessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED + ".+\\[" + null + "\\]" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }
        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token without an "iss" value. The oidcclient config has disableIssChecking set to true, so we will
     * NOT require an iss
     * The request should fail succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_Iss_disableIssCheckingTrue() throws Exception {
        if (testSettings.getUseJwtConsumer()) {
            Log.info(thisClass, _testName, "This test does not apply to JWT consumer since consumer doesn't have a disableIssChecking config option.");
            return;
        }

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snapp");

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_ISSUER, null);

        positiveTest(updatedTestSettings, originalJwtToken);

    }

    /**
     * Create a JWT token with a bad "iss" value. The oidcclient config has disableIssChecking set to true, so we will
     * NOT require an iss
     * The request should fail succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_Iss_disableIssCheckingTrue() throws Exception {
        if (testSettings.getUseJwtConsumer()) {
            Log.info(thisClass, _testName, "This test does not apply to JWT consumer since consumer doesn't have a disableIssChecking config option.");
            return;
        }

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snapp");
        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_ISSUER, badString);

        positiveTest(updatedTestSettings, badJwtToken);

    }

    /**
     * Create a JWT token with a bad signature
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_signature() throws Exception {

        String originalJwtToken = buildAJWTToken(testSettings);
        String badJwtToken = originalJwtToken.substring(0, originalJwtToken.length() - 4);
        badJwtToken = badJwtToken + "ABCD"; // mess up with signature

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + testSettings.getJwtId() + ".+" + MessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1776E_SIGNATURE_VALIDATION, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }
        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a bad "alg"
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_alg() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.HEADER_ALGORITHM, badString);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + testSettings.getJwtId() + ".+" + MessageConstants.CWWKS6028E_BAD_ALGORITHM + ".+\\[" + badString + "\\].+\\[" + testSettings.getSignatureAlg() + "\\]" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }
        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a missing "alg"
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_alg() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.HEADER_ALGORITHM, null);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            // TODO - ultimately throws an NPE in open source
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + testSettings.getJwtId() };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }
        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a bad "sub" - the users are mapped to the registry
     * The request should fail with a 403 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.registry.EntryNotFoundException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_sub_Mapped() throws Exception {
        if (testSettings.getUseJwtConsumer()) {
            Log.info(thisClass, _testName, "This test does not apply to JWT consumer since consumer doesn't have a registry mapping config option.");
            return;
        }

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_SUBJECT, badString);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.FORBIDDEN_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating issue with the JWT token", MessageConstants.CWWKS1106A_AUTHENTICATION_FAILED);

        testSettings.printTestSettings();
        msgUtils.printOAuthOidcExpectations(expectations);

        WebConversation wc = new WebConversation();
        helpers.invokeRsProtectedResource(_testName, wc, badJwtToken, testSettings, expectations);

    }

    /**
     * Create a JWT token with a bad "sub" - the users are NOT mapped to the registry
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_sub_NotMapped() throws Exception {

        TestSettings updatedTestSettings = null;

        String badSub = badString;
        if (testSettings.getUseJwtConsumer()) {
            updatedTestSettings = testSettings.copyTestSettings();
            updatedTestSettings.setAdminUser(badSub);
        } else {
            updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snapp");
        }
        String badJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_SUBJECT, badSub);

        positiveTest(updatedTestSettings, badJwtToken);

    }

    /**
     * Create a JWT token with a missing "sub" - the users are mapped to the registry
     * The request should fail with a 401 exception
     * The sub claim is not longer required by the oidc client, but the default claim used to generate the subject is "sub", so,
     * token validation will no longer fail, but, we will get a failure trying to generate the subject - look for a new message
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_sub_Mapped() throws Exception {
        if (testSettings.getUseJwtConsumer()) {
            Log.info(thisClass, _testName, "This test does not apply to JWT consumer since consumer doesn't have a registry mapping config option.");
            return;
        }

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_SUBJECT, null);

        //        negativeTest(testSettings, badJwtToken, new String[] { "No Subject", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });
        negativeTest(testSettings, badJwtToken, new String[] { MessageConstants.CWWKS1738E_JWT_MISSING_CLAIM });

    }

    /**
     * Create a JWT token with a missing "sub" - the users are mapped to the registry
     * The request should work, because the token will contain "mySub" and the config specifies this value in the
     * userIdentityToCreateSubject attribute.
     * Tests shows that it's ok not to have "sub" and that using a different value in userIdentityToCreateSubject will work
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_sub_Mapped_override_userIdentityToCreateSubject() throws Exception {
        if (testSettings.getUseJwtConsumer()) {
            Log.info(thisClass, _testName, "This test does not apply to JWT consumer since consumer doesn't have a registry mapping config option.");
            return;
        }
        // use app that will use config that has userIdentityToCreateSubject set to "mySub"
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/noSub");

        // keep sub out of token, and then update generated token to have mySub
        String badJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_SUBJECT, null);
        String badJwtToken2 = reBuildAJWTToken(updatedTestSettings, "mySub", updatedTestSettings.getAdminUser(), badJwtToken);

        positiveTest(updatedTestSettings, badJwtToken2);

    }

    /**
     * Create a JWT token with a missing "sub" - the users are NOT mapped to the registry
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_sub_NotMapped() throws Exception {

        TestSettings updatedTestSettings = null;
        if (testSettings.getUseJwtConsumer()) {
            updatedTestSettings = testSettings.copyTestSettings();
            updatedTestSettings.setAdminUser(null);
        } else {
            updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snorking");
        }

        String tokenMissingSub = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_SUBJECT, null);

        if (updatedTestSettings.getUseJwtConsumer()) {
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, null, new String[] { Constants.PAYLOAD_SUBJECT });
            positiveTest(updatedTestSettings, tokenMissingSub);
        } else {
            negativeTest(updatedTestSettings, tokenMissingSub, new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });
        }

    }

    /**
     * Create a JWT token with a valid "exp". Wait for the token to expire, but the time is still within clockskew of the
     * expiration
     * We should get access to the app. Sleep beyond the exp + clockskewy and we should then get a 401 exception
     *
     * NOTE **************************************
     * On very slow machines, the first use of the token may fail if it takes too long to process the token (we could be beyond
     * the clock skew even on the first call - this should be very, very, very rare and until we see otherwise, it is NOT work
     * make the clock skew larger and sleeping longer (and therefore making the test take longer)
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_exp() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            updatedTestSettings.setValidateJWTTimeStamps(false);
        }

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, "5");

        // sleep, but NOT beyond the clock skew
        helpers.testSleep(10);

        positiveTest(updatedTestSettings, originalJwtToken);

        // now, sleep beyond the clock skew
        helpers.testSleep(10);

        String[] errorMsgs = null;
        if (updatedTestSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + testSettings.getJwtId() + ".+" + MessageConstants.CWWKS6025E_TOKEN_EXPIRED };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1773E_TOKEN_EXPIRED, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }
        negativeTest(updatedTestSettings, originalJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a bad "exp" - really, really old
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_exp() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, "hardcoded:1470326817");

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6024E_IAT_AFTER_EXP };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1773E_TOKEN_EXPIRED, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a missing "exp"
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "org.jose4j.jwt.consumer.InvalidJwtException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_exp() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, null);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6025E_TOKEN_EXPIRED + ".+\\[" + null + "\\]" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a bad "iat"
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.jwt.MalformedClaimException", "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_iat() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS + "string", "stringForTime");

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6043E_MALFORMED_CLAIM + ".+\\[" + Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS + "\\]" };
        } else {
            errorMsgs = new String[] { "iat.+not the expected type", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a missing "iat" - the "iat" is not required
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_iat() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, null);

        if (updatedTestSettings.getUseJwtConsumer()) {
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, null, new String[] { Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS });
            updatedTestSettings.setValidateJWTTimeStamps(false);
        }

        positiveTest(updatedTestSettings, originalJwtToken);
    }

    /**
     * Create a JWT token with a future "iat"
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Future_iat() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, twentyFourHourSeconds);

        if (testSettings.getUseJwtConsumer()) {
            TestSettings updatedTestSettings = testSettings.copyTestSettings();
            updatedTestSettings.setValidateJWTTimeStamps(false);
        }

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + testSettings.getJwtId() + ".+" + MessageConstants.CWWKS6044E_IAT_AFTER_CURRENT_TIME };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1773E_TOKEN_EXPIRED, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a bad "aud" - "aud" does not match the request url as the config doesn't have an audience
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Bad_aud() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, badString);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6023E_BAD_AUDIENCE + ".+\\[" + badString + "\\]" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a missing "aud" - there is an audience in the config
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Missing_aud() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, null);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6023E_BAD_AUDIENCE + ".+\\[\\]" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1779E_MISSING_AUD, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with "nbf" set to the current time
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Current_nbf() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "nbf" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS }, null);
        }

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, null);

        positiveTest(updatedTestSettings, originalJwtToken);

    }

    /**
     * Create a JWT token with "nbf" set out in the future
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Future_nbf() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, twentyFourHourSeconds);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6026E_FUTURE_NBF };
        } else {
            errorMsgs = new String[] { Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with a jti claim. Use the same token again.
     * The request should fail with a 401 exception because we can't reuse a token with the same jti
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void NoOPMangleJWT1ServerTests_Reuse_jti() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "jti" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_JWTID }, null);
        }

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, generateRandomString(8, true));
        positiveTest(updatedTestSettings, originalJwtToken);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6045E_JTI_REUSED + ".+'" + Constants.PAYLOAD_ISSUER + "'.+\\[" + updatedTestSettings.getClientID() + "\\].+'" + Constants.PAYLOAD_JWTID + "'" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1743E_REUSED_JTI };
        }

        negativeTest(updatedTestSettings, originalJwtToken, errorMsgs);

    }

    /**
     * Create a JWT token with NO jti claim. Use the same token again.
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_Reuse_jwt_no_jti() throws Exception {

        String originalJwtToken = buildAJWTToken(testSettings);
        positiveTest(testSettings, originalJwtToken);
        positiveTest(testSettings, originalJwtToken);

    }

    /**
     * Create 2 distinct JWT tokens, but use the same value for the jti claim in both.
     * The request should fail with a 401 exception because we can't reuse a token with the same jti
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void NoOPMangleJWT1ServerTests_two_jwt_use_same_jti() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "jti" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_JWTID }, null);
        }

        String jtiValue = generateRandomString(8, true);
        String originalJwtToken1 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, jtiValue);
        String originalJwtToken2 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, jtiValue);
        positiveTest(updatedTestSettings, originalJwtToken1);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6045E_JTI_REUSED + ".+'" + Constants.PAYLOAD_ISSUER + "'.+\\[" + updatedTestSettings.getClientID() + "\\].+'" + Constants.PAYLOAD_JWTID + "'.+\\[" + jtiValue + "\\]" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1743E_REUSED_JTI };
        }

        negativeTest(updatedTestSettings, originalJwtToken2, errorMsgs);

    }

    /**
     * Create multiple JWT tokens each with a unique jti claim. Use each token at least 2 times.
     * The first use of each token should be successful - all subsequent uses of each token should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void NoOPMangleJWT1ServerTests_mult_token_mult_jti() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "jti" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_JWTID }, null);
        }

        String originalJwtToken1 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, generateRandomString(8, true));
        String originalJwtToken2 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, generateRandomString(8, true));
        String originalJwtToken3 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, generateRandomString(8, true));
        String originalJwtToken4 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, generateRandomString(8, true));
        positiveTest(updatedTestSettings, originalJwtToken1);
        positiveTest(updatedTestSettings, originalJwtToken2);
        positiveTest(updatedTestSettings, originalJwtToken3);
        positiveTest(updatedTestSettings, originalJwtToken4);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6045E_JTI_REUSED + ".+'" + Constants.PAYLOAD_ISSUER + "'.+\\[" + updatedTestSettings.getClientID() + "\\].+'" + Constants.PAYLOAD_JWTID + "'" };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1743E_REUSED_JTI };
        }

        negativeTest(updatedTestSettings, originalJwtToken1, errorMsgs);
        negativeTest(updatedTestSettings, originalJwtToken2, errorMsgs);
        negativeTest(updatedTestSettings, originalJwtToken3, errorMsgs);
        negativeTest(updatedTestSettings, originalJwtToken4, errorMsgs);
        negativeTest(updatedTestSettings, originalJwtToken2, errorMsgs);

    }

    /**
     * Create a JWT token with a jti claim and an exp value that is short. Use the token a couple of times after sleeping
     * (waiting for the token to expire)
     * We should receive a 401 exception indicating that the token has expired - the exp should be checked before the jti
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void NoOPMangleJWT1ServerTests_exp_jti() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "jti" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_JWTID }, null);
            updatedTestSettings.setValidateJWTTimeStamps(false);
        }

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, generateRandomString(8, true));
        String originalJwtToken1 = reBuildAJWTToken(updatedTestSettings, Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, "5", originalJwtToken);

        // sleep, but NOT beyond the clock skew
        helpers.testSleep(10);

        positiveTest(updatedTestSettings, originalJwtToken1);

        // now, sleep beyond the clock skew
        helpers.testSleep(10);

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+" + MessageConstants.CWWKS6025E_TOKEN_EXPIRED };
        } else {
            errorMsgs = new String[] { MessageConstants.CWWKS1773E_TOKEN_EXPIRED, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(updatedTestSettings, originalJwtToken1, errorMsgs);

    }

    /**
     * Create a JWT token with a jti claim. Create another JWT token with a jti claim that uses a substring of the value used in
     * the previous JWT token
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_subset_jti() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "jti" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_JWTID }, null);
        }

        String jtiValue = generateRandomString(8, true);
        String originalJwtToken1 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, jtiValue);
        String originalJwtToken2 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, jtiValue.substring(0, jtiValue.length() - 2));
        positiveTest(updatedTestSettings, originalJwtToken1);
        positiveTest(updatedTestSettings, originalJwtToken2);

    }

    /**
     * Create a JWT token with a jti claim. Create another JWT token with a jti claim that uses a superset of the value used in
     * the previous JWT token
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_superset_jti() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "jti" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_JWTID }, null);
        }

        String jtiValue = generateRandomString(8, true);
        String originalJwtToken1 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, jtiValue);
        String originalJwtToken2 = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_JWTID, jtiValue + "foo");
        positiveTest(updatedTestSettings, originalJwtToken1);
        positiveTest(updatedTestSettings, originalJwtToken2);

    }

    /**
     * Create a JWT token with a some unknown claim
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_other() throws Exception {

        String originalJwtToken = buildAJWTToken(testSettings, "other", someString);

        WebResponse response = positiveTest(testSettings, originalJwtToken);

        if (testSettings.getUseJwtConsumer()) {
            List<validationData> expectations = addJwtJsonClaimExpectation(null, "other", someString);
            validationTools.validateResult(response, Constants.INVOKE_JWT_CONSUMER, expectations, testSettings);
        }
    }

    /**
     * Create a JWT token with "nonce"
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_nonce() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "nonce" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_NONCE }, null);
        }

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_NONCE, someString);

        WebResponse response = positiveTest(updatedTestSettings, originalJwtToken);

        if (updatedTestSettings.getUseJwtConsumer()) {
            List<validationData> expectations = addJwtJsonClaimExpectation(null, Constants.PAYLOAD_NONCE, someString);
            validationTools.validateResult(response, Constants.INVOKE_JWT_CONSUMER, expectations, updatedTestSettings);
        }
    }

    /**
     * Create a JWT token with "at_hash"
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_at_hash() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "at_hash" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_AT_HASH }, null);
        }

        String originalJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AT_HASH, someString);

        WebResponse response = positiveTest(updatedTestSettings, originalJwtToken);

        if (updatedTestSettings.getUseJwtConsumer()) {
            List<validationData> expectations = addJwtJsonClaimExpectation(null, Constants.PAYLOAD_AT_HASH, someString);
            validationTools.validateResult(response, Constants.INVOKE_JWT_CONSUMER, expectations, updatedTestSettings);
        }
    }

    /**
     * Create a JWT token with "typ"
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_typ() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "typ" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_TYPE }, null);
        }

        String originalJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_TYPE, someString);

        WebResponse response = positiveTest(updatedTestSettings, originalJwtToken);

        if (updatedTestSettings.getUseJwtConsumer()) {
            List<validationData> expectations = addJwtJsonClaimExpectation(null, Constants.PAYLOAD_TYPE, someString);
            validationTools.validateResult(response, Constants.INVOKE_JWT_CONSUMER, expectations, updatedTestSettings);
        }
    }

    /**
     * Create a JWT token with a valid"azp"
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void NoOPMangleJWT1ServerTests_azp() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (updatedTestSettings.getUseJwtConsumer()) {
            // "typ" claim should come up in the token
            updatedTestSettings = updateRequiredJwtKeys(updatedTestSettings, new String[] { Constants.PAYLOAD_AUTHORIZED_PARTY }, null);
        }

        String originalJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUTHORIZED_PARTY, updatedTestSettings.getClientID());

        WebResponse response = positiveTest(updatedTestSettings, originalJwtToken);

        if (updatedTestSettings.getUseJwtConsumer()) {
            List<validationData> expectations = addJwtJsonClaimExpectation(null, Constants.PAYLOAD_AUTHORIZED_PARTY, updatedTestSettings.getClientID());
            validationTools.validateResult(response, Constants.INVOKE_JWT_CONSUMER, expectations, updatedTestSettings);
        }
    }

    /**
     * Create a JWT token with a bad "azp"
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    // @Test // task 227327. The azp claim is ignored in jwt for now. It's an TODO item in product code now
    public void NoOPMangleJWT1ServerTests_azp_bad() throws Exception {

        String originalJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUTHORIZED_PARTY, someString);

        negativeTest(testSettings, originalJwtToken, new String[] { MessageConstants.CWWKS1775E_AZP_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token missing part 1 of the 3 part token
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException" })
    @Test
    public void NoOPMangleJWT1ServerTests_omit_part1() throws Exception {

        String originalJwtToken = buildAJWTToken(testSettings);
        Log.info(thisClass, _testName, "returned token: " + originalJwtToken);
        String[] parts = originalJwtToken.split("\\.");
        String badToken = parts[1] + "." + parts[2];

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            // TODO - ayoho serviceability standpoint - can we do anything based on the open source exception?
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+JoseException.+was 2" };
        } else {
            errorMsgs = new String[] { "JoseException.+was 2", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badToken, errorMsgs);

    }

    /**
     * Create a JWT token missing part 2 of the 3 part token
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException" })
    @Test
    public void NoOPMangleJWT1ServerTests_omit_part2() throws Exception {

        String originalJwtToken = buildAJWTToken(testSettings);
        String[] parts = originalJwtToken.split("\\.");
        String badToken = parts[0] + "." + parts[2];

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            // TODO - ayoho serviceability standpoint - can we do anything based on the open source exception?
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+JoseException.+was 2" };
        } else {
            errorMsgs = new String[] { "JoseException.+was 2", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badToken, errorMsgs);

    }

    /**
     * Create a JWT token missing part 3 of the 3 part token
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException" })
    @Test
    public void NoOPMangleJWT1ServerTests_omit_part3() throws Exception {

        String originalJwtToken = buildAJWTToken(testSettings);
        String[] parts = originalJwtToken.split("\\.");
        String badToken = parts[0] + "." + parts[1];

        String[] errorMsgs = null;
        if (testSettings.getUseJwtConsumer()) {
            // TODO - ayoho serviceability standpoint - can we do anything based on the open source exception?
            errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+\\[" + testSettings.getJwtId() + "\\].+JoseException.+was 2" };
        } else {
            errorMsgs = new String[] { "JoseException.+was 2", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        }

        negativeTest(testSettings, badToken, errorMsgs);

    }

    // /************************************* x509 specific token mangling ***************************************/
    // @Test
    // public void NoOPMangleJWT1ServerTests_x509_mainFlow() throws Exception {
    //
    // TestSettings updatedTestSettings = updateTestSettings(testSettings, "snooping", "OAuthConfigSample_JWT_x509", "OidcConfigSample_JWT_x509") ;
    // updatedTestSettings.setClientID("client05");
    //
    // // TestSettings updatedTestSettingsForBuild = updatedTestSettings.copyTestSettings() ;
    // // updatedTestSettingsForBuild.setClientID( );
    //
    // List<validationData> expectations = addRSProtectedAppExpectations(_testName, updatedTestSettings) ;
    //
    // WebConversation wc = new WebConversation();
    // // genericOP(_testName, wc, updatedTestSettings, goodActions, expectations) ;
    // String boo = buildAJWTToken(updatedTestSettings);
    // helpers.invokeRsProtectedResource(_testName, wc, boo, updatedTestSettings, expectations) ;
    //
    // }
    //
    //
    //
    // /************************************* JWK specific token mangling ***************************************/
    // @Test
    // public void NoOPMangleJWT1ServerTests_JWK_mainFlow() throws Exception {
    //
    // TestSettings updatedTestSettings = updateTestSettings(testSettings, "sniffing", "OAuthConfigSample_JWT_JWK", "OidcConfigSample_JWT_JWK") ;
    // updatedTestSettings.setClientID("client04");
    //
    // List<validationData> expectations = addRSProtectedAppExpectations(_testName, updatedTestSettings) ;
    //
    // WebConversation wc = new WebConversation();
    // WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations) ;
    //
    // Log.info(thisClass, "NoOPMangleJWT1ServerTests_JWK_mainFlow", "Before creating a new JWT Token");
    // String foo = validationTools.getTokenForType(updatedTestSettings, response) ;
    // String boo = buildAJWTToken(updatedTestSettings);
    // Log.info(thisClass, "NoOPMangleJWT1ServerTests_JWK_mainFlow", "After creating a new JWT Token");
    // Log.info(thisClass, "NoOPMangleJWT1ServerTests_JWK_mainFlow", "Before print the OP generated JWT Token");
    // validationTools.printJWTToken(foo);
    // Log.info(thisClass, "NoOPMangleJWT1ServerTests_JWK_mainFlow", "After print the OP generated JWT Token");
    //
    // helpers.invokeRsProtectedResource(_testName, wc, boo, updatedTestSettings, expectations) ;
    // // JsonToken token = parser.deserialize(foo) ;
    // }
    //
    // @Test
    // public void NoOPMangleJWT1ServerTests_x509_RS256_mainFlow() throws Exception {
    //
    // TestSettings updatedTestSettings = updateTestSettings(testSettings, "sniffing", "OAuthConfigSample_JWT_JWK", "OidcConfigSample_JWT_JWK") ;
    // updatedTestSettings.setClientID("client04");
    // updatedTestSettings.setSignatureAlg("RS256");
    // List<validationData> expectations = addRSProtectedAppExpectations(_testName, updatedTestSettings) ;
    //
    // WebConversation wc = new WebConversation();
    // // genericOP(_testName, wc, updatedTestSettings, goodActions, expectations) ;
    // String originalJwtToken = buildARS256JWTTokenForPropagation(updatedTestSettings, NO_OVERRIDE, NO_OVERRIDE, "./securitykeys/rsaKeys.jks", "Liberty", "rsaKey", "ibm.com", "92");
    // helpers.invokeRsProtectedResource(_testName, wc, originalJwtToken, updatedTestSettings, expectations) ;
    //
    // }

    /******************************************* Helper methods *******************************************/

    private TestSettings updateRequiredJwtKeys(TestSettings settings, String[] addRequiredKeys, String[] removeRequiredKeys) {
        String method = "updateRequiredJwtKeys";
        TestSettings updatedSettings = settings.copyTestSettings();
        Log.info(thisClass, method, "Initial list of required JWT keys: " + updatedSettings.getRequiredJwtKeys());
        if (removeRequiredKeys != null) {
            Log.info(thisClass, method, "Removing required JWT keys: " + removeRequiredKeys);
            for (String key : removeRequiredKeys) {
                updatedSettings.removeRequiredJwtKey(key);
            }
        }
        if (addRequiredKeys != null) {
            Log.info(thisClass, method, "Adding required JWT keys: " + addRequiredKeys);
            for (String key : addRequiredKeys) {
                updatedSettings.addRequiredJwtKey(key);
            }
        }
        Log.info(thisClass, method, "New list of required JWT keys: " + updatedSettings.getRequiredJwtKeys());
        return updatedSettings;
    }

}
