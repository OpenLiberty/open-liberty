/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt.sharedTests;

import org.jose4j.jwt.NumericDate;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This test class provides common test cases for validating the consumption of JWT Tokens within Liberty.
 * An RS Server and consumer API can use these tests to verify that they are processing JWT's properly.
 * This class is to be extended in other FAT projects.
 * Those project will set up servers that are appropriate to test their own function:
 * ie: The RS server config will specify what should be in the JWT
 * ie: The Consumer API tests will specify jwtConsumer configs that define what should be in the JWT
 * Those projects also need to provide the methods that result in the JWT Token being consumed:
 * ie: In the case of the RS server invoke an app protected by a config that can consume a JWT
 * ie: In the case of the consumer api, invoke an app that will then invoke the consumer api.
 * Those projects also need to need to provide methods to validate that
 * the JWT token was "consumed" properly:
 * ie: In the case of the RS server, validate the output from our standard security test apps (not all
 * attribute values in the jwt token can be validated in this case)
 * ie: In the case of the Consumer API, validate that all expected attributes exist and that they
 * have the correct value
 * note: the validation methods of negative results for the RS server and Consumer API FATs
 * will most likely be different as the error messages will differ.
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public abstract class ConsumeMangledJWTTests extends CommonSecurityFat {

    public static final TestValidationUtils validationUtils = new TestValidationUtils();
    public static final JwtTokenBuilderUtils jwtTokenBuilderUtils = new JwtTokenBuilderUtils();

    protected JWTTokenBuilder builder = null;
    protected final String badString = "someBadValue";
    private final String someString = "someString";
    protected final Long twentyFourHourSeconds = 86400000L; // use milliseconds

    protected String currentAction = null;

    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {

        JWTTokenBuilder builder = jwtTokenBuilderUtils.createBuilderWithDefaultClaims();
        return builder;
    }

    /*
     * Wrap the call to the builder so that we can log the raw values and the generated token
     * for debug purposes and not have to duplicate 3 simple lines of code
     */
    public String buildToken() throws Exception {
        return jwtTokenBuilderUtils.buildToken(builder, _testName);
    }

    // provide an override to test the consumption of the token
    public abstract Page consumeToken(String token) throws Exception;

    // provide overrides for validation of the response
    public abstract Expectations addGoodResponseAndClaimsExpectations(String currentAction, JWTTokenBuilder builder) throws Exception;

    public abstract Expectations updateExpectationsForJsonAttribute(Expectations expectations, String key, Object value) throws Exception;

    protected abstract Expectations buildNegativeAttributeExpectations(String specificErrorId) throws Exception;

    // get error messages
    // provided by extending class as the exact error messages (msg numbers) may be different based on what we're testing
    protected abstract String getJtiReusedMsg();

    protected abstract String getIssuerNotTrustedMsg();

    protected abstract String getSignatureNotValidMsg();

    protected abstract String getTokenExpiredMsg();

    protected abstract String getMalformedClaimMsg();

    protected abstract String getIatAfterExpMsg();

    protected abstract String getIatAfterCurrentTimeMsg();

    protected abstract String getBadAudienceMsg();

    protected abstract String getBadNotBeforeMsg();

    /************************************************************** Tests **************************************************************/

    /**
     * A general test to show that the JWT Token creator code is working properly
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConsumeMangledJWTTests_test_testGeneratedToken() throws Exception {

        // build a token with default values
        String jwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);

        Page response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with an bad "iss" value (value that will not match what is in the config)
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Bad_iss() throws Exception {

        // update the builder with a bad value for the issuer, then create a JWT token with that bad issuer
        builder.setIssuer(badString);
        String badJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getIssuerNotTrustedMsg());

        Page response = consumeToken(badJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token without an "iss" value.
     * For JWT Consumer testing, we always require the issuer
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Missing_iss_disableIssCheckingFalse() throws Exception {

        builder.unsetClaim(PayloadConstants.ISSUER);
        String badJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getIssuerNotTrustedMsg() + ".+\\[null\\]");

        Page response = consumeToken(badJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a bad signature
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    @Test
    public void ConsumeMangledJWTTests_Bad_signature() throws Exception {

        String jwtToken = buildToken();

        // mangle the signature (replace the last for characters - keep the length the same - just mess with the content)
        String badJwtToken = jwtToken.substring(0, jwtToken.length() - 4);
        badJwtToken = badJwtToken + "ABCD"; // mess up with signature

        Expectations expectations = buildNegativeAttributeExpectations(getSignatureNotValidMsg());

        Page response = consumeToken(badJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a bad "alg"
     *
     * @throws Exception
     */
    // disabled for now - have to create a token with an invalid alg (the current tooling won't allow it)
//    @Test
    public void ConsumeMangledJWTTests_Bad_alg() throws Exception {

    }

    /**
     * Create a JWT token with a missing "alg"
     *
     * @throws Exception
     */
    // disabled for now - have to create a token with a missing alg (the current tooling won't allow it)
//    @Test
    public void ConsumeMangledJWTTests_Missing_alg() throws Exception {

        builder.setAlorithmHeaderValue(null);
        buildToken();
    }

    /**
     * Create a JWT token with a bad "sub" - the users are NOT mapped to the registry
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Bad_sub_NotMapped() throws Exception {

        builder.setSubject(badString);

        String updatedJwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a missing "sub" - the users are NOT mapped to the registry
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Missing_sub_NotMapped() throws Exception {

        builder.unsetClaim(PayloadConstants.SUBJECT);

        String updatedJwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a valid "exp". Wait for the token to expire, but the time is still within clockskew of the
     * expiration
     * We should get access to the app. Sleep beyond the exp + clockskew and we should then receive a failure
     *
     * NOTE **************************************
     * On very slow machines, the first use of the token may fail if it takes too long to process the token (we could be beyond
     * the clock skew even on the first call - this should be very, very, very rare and until we see otherwise, it is NOT worth
     * making the clock skew larger and sleeping longer (and therefore making the test take longer)
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_exp() throws Exception {

        builder.setExpirationTimeSecondsFromNow(5);

        String updatedJwtToken = buildToken();

        Expectations beforeExpiredExpectations = addGoodResponseAndClaimsExpectations(currentAction, builder);

        Expectations afterExpiredExpectations = buildNegativeAttributeExpectations(getTokenExpiredMsg());

        // sleep beyond token lifetime, but not beyond lifetime + clockskew
        Thread.sleep(10 * 1000);

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, beforeExpiredExpectations);

        Thread.sleep(10 * 1000);

        response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, afterExpiredExpectations);

    }

    /**
     * Create a JWT token with a bad "exp" - really, really old
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_exp_before_iat() throws Exception {

        builder.setExpirationTimeSecondsFromNow(-2500);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getIatAfterExpMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a missing "exp"
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Missing_exp() throws Exception {

        builder.unsetClaim(PayloadConstants.EXPIRATION_TIME);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getTokenExpiredMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a bad "exp"
     * The request should fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "org.jose4j.jwt.MalformedClaimException" })
    @Test
    public void ConsumeMangledJWTTests_Bad_exp() throws Exception {

        builder.unsetClaim(PayloadConstants.EXPIRATION_TIME);
        builder.setClaim(PayloadConstants.EXPIRATION_TIME, badString);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getMalformedClaimMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a missing "iat" - the "iat" is not required
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Missing_iat() throws Exception {

        builder.unsetClaim(PayloadConstants.ISSUED_AT);

        String updatedJwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a bad "iat"
     * The request should fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "org.jose4j.jwt.MalformedClaimException" })
    @Test
    public void ConsumeMangledJWTTests_Bad_iat() throws Exception {

        builder.unsetClaim(PayloadConstants.ISSUED_AT);
        builder.setClaim(PayloadConstants.ISSUED_AT, badString);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getMalformedClaimMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a future "iat"
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Future_iat() throws Exception {

        NumericDate later = NumericDate.now();
        later.addSeconds(twentyFourHourSeconds);
        builder.setIssuedAt(later);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getIatAfterCurrentTimeMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a bad "aud" - "aud" does not match the request url as the config doesn't have an audience
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Bad_aud() throws Exception {

        builder.setAudience(badString);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getBadAudienceMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a missing "aud" - there is an audience in the config
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Missing_aud() throws Exception {

        // remove the audience from the token
        builder.unsetClaim(PayloadConstants.AUDIENCE);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getBadAudienceMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with "nbf" set to the current time
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Current_nbf() throws Exception {

        builder.setNotBeforeMinutesInThePast(0);

        String updatedJwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with "nbf" set out in the future
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Future_nbf() throws Exception {

        NumericDate later = NumericDate.now();
        later.addSeconds(300);
        builder.setNotBefore(later);

        String updatedJwtToken = buildToken();

        Expectations expectations = buildNegativeAttributeExpectations(getBadNotBeforeMsg());

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a jti claim. Use the same token again.
     * The request should fail because we can't reuse a token with the same jti
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Reuse_jti() throws Exception {

        builder.setGeneratedJwtId();

        String updatedJwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.JWT_ID, builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        Page response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

        // re-use the token and expect to get a failure because the jti was already used
        Expectations reusedExpectations = buildNegativeAttributeExpectations(getJtiReusedMsg());

        response = consumeToken(updatedJwtToken);
        validationUtils.validateResult(response, currentAction, reusedExpectations);

    }

    /**
     * Create a JWT token with NO jti claim. Use the same token again.
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_Reuse_jwt_no_jti() throws Exception {

        String jwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.JWT_ID, builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        Page response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

        // re-use the same token and show that this is allowed when a jti is NOT in the token
        response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create 2 distinct JWT tokens, but use the same value for the jti claim in both.
     * The request should fail because we can't reuse a token with the same jti
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_two_jwt_use_same_jti() throws Exception {

        builder.setGeneratedJwtId();

        String firstJwtToken = buildToken();
        String secondJwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.JWT_ID, builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        Page response = consumeToken(firstJwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

        // re-use the token and expect to get a failure because the jti was already used
        Expectations reusedExpectations = buildNegativeAttributeExpectations(getJtiReusedMsg());

        response = consumeToken(secondJwtToken);
        validationUtils.validateResult(response, currentAction, reusedExpectations);

    }

    /**
     * Create multiple JWT tokens each with a unique jti claim. Use each token at least 2 times.
     * The first use of each token should be successful - all subsequent uses of each token should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_mult_token_mult_jti() throws Exception {

        // create unique jwt tokens - each using the same builder, but each having a unique jti
        builder.setGeneratedJwtId();
        Expectations goodExpectations1 = addGoodResponseAndClaimsExpectations(currentAction, builder);
        goodExpectations1 = updateExpectationsForJsonAttribute(goodExpectations1, PayloadConstants.JWT_ID, builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));
        Expectations reusedExpectations1 = buildNegativeAttributeExpectations(getJtiReusedMsg());
        String firstJwtToken = buildToken();

        builder.setGeneratedJwtId();
        Expectations goodExpectations2 = addGoodResponseAndClaimsExpectations(currentAction, builder);
        goodExpectations2 = updateExpectationsForJsonAttribute(goodExpectations2, PayloadConstants.JWT_ID, builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));
        Expectations reusedExpectations2 = buildNegativeAttributeExpectations(getJtiReusedMsg());
        String secondJwtToken = buildToken();

        builder.setGeneratedJwtId();
        Expectations goodExpectations3 = addGoodResponseAndClaimsExpectations(currentAction, builder);
        goodExpectations3 = updateExpectationsForJsonAttribute(goodExpectations3, PayloadConstants.JWT_ID, builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));
        Expectations reusedExpectations3 = buildNegativeAttributeExpectations(getJtiReusedMsg());
        String thirdJwtToken = buildToken();

        builder.setGeneratedJwtId();
        Expectations goodExpectations4 = addGoodResponseAndClaimsExpectations(currentAction, builder);
        goodExpectations4 = updateExpectationsForJsonAttribute(goodExpectations4, PayloadConstants.JWT_ID, builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));
        Expectations reusedExpectations4 = buildNegativeAttributeExpectations(getJtiReusedMsg());
        String fourthJwtToken = buildToken();

        // use each token for the first time and expect success
        Page response1 = consumeToken(firstJwtToken);
        validationUtils.validateResult(response1, currentAction, goodExpectations1);
        Page response2 = consumeToken(secondJwtToken);
        validationUtils.validateResult(response2, currentAction, goodExpectations2);
        Page response3 = consumeToken(thirdJwtToken);
        validationUtils.validateResult(response3, currentAction, goodExpectations3);
        Page response4 = consumeToken(fourthJwtToken);
        validationUtils.validateResult(response4, currentAction, goodExpectations4);

        // re-use the tokens and expect to get a failure because each jti was already used
        response1 = consumeToken(firstJwtToken);
        validationUtils.validateResult(response1, currentAction, reusedExpectations1);
        response2 = consumeToken(secondJwtToken);
        validationUtils.validateResult(response2, currentAction, reusedExpectations2);
        response3 = consumeToken(thirdJwtToken);
        validationUtils.validateResult(response3, currentAction, reusedExpectations3);
        response4 = consumeToken(fourthJwtToken);
        validationUtils.validateResult(response4, currentAction, reusedExpectations4);

    }

    /**
     * Create a JWT token with a jti claim and an exp value that is short. Use the token a couple of times after sleeping
     * (waiting for the token to expire)
     * The exp should be checked before the jti
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_exp_jti() throws Exception {

        // build 2 JWT tokens - both use the same jti
        builder.setGeneratedJwtId();
        String longLivedJwtToken = buildToken();

        Expectations longLivedExpectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        longLivedExpectations = updateExpectationsForJsonAttribute(longLivedExpectations, PayloadConstants.JWT_ID,
                                                                   builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        builder.setExpirationTimeSecondsFromNow(-2500);
        String shortLivedJwtToken = buildToken();

        Expectations shortLivedExpectations = buildNegativeAttributeExpectations(getIatAfterExpMsg());

        Page longLivedResponse = consumeToken(longLivedJwtToken);
        validationUtils.validateResult(longLivedResponse, currentAction, longLivedExpectations);

        Page shortLivedResponse = consumeToken(shortLivedJwtToken);
        validationUtils.validateResult(shortLivedResponse, currentAction, shortLivedExpectations);

    }

    /**
     * Create a JWT token with a jti claim. Create another JWT token with a jti claim that uses a substring of the value used in
     * the previous JWT token
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_subset_jti() throws Exception {

        // build 2 JWT tokens - one jti is a subset of the other
        builder.setGeneratedJwtId();
        String shorterJtiJwtToken = buildToken();
        Expectations shorterJtiExpectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        shorterJtiExpectations = updateExpectationsForJsonAttribute(shorterJtiExpectations, PayloadConstants.JWT_ID,
                                                                    builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        builder.setJwtId(builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID) + "xx");
        String longerJtiJwtToken = buildToken();
        Expectations longerJtiExpectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        longerJtiExpectations = updateExpectationsForJsonAttribute(longerJtiExpectations, PayloadConstants.JWT_ID,
                                                                   builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        // start with the longer length jti - show that the token works
        Page longerJtiResponse = consumeToken(longerJtiJwtToken);
        validationUtils.validateResult(longerJtiResponse, currentAction, longerJtiExpectations);

        // use the token with the subset jti - show that the token works
        Page shorterJtiResponse = consumeToken(shorterJtiJwtToken);
        validationUtils.validateResult(shorterJtiResponse, currentAction, shorterJtiExpectations);

        // test shows that a substring of a jti doesn't match
    }

    /**
     * Create a JWT token with a jti claim. Create another JWT token with a jti claim that uses a superset of the value used in
     * the previous JWT token
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_superset_jti() throws Exception {

        // build 2 JWT tokens - one jti is a subset of the other
        builder.setGeneratedJwtId();
        String shorterJtiJwtToken = buildToken();
        Expectations shorterJtiExpectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        shorterJtiExpectations = updateExpectationsForJsonAttribute(shorterJtiExpectations, PayloadConstants.JWT_ID,
                                                                    builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        builder.setJwtId(builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID) + "xx");
        String longerJtiJwtToken = buildToken();
        Expectations longerJtiExpectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        longerJtiExpectations = updateExpectationsForJsonAttribute(longerJtiExpectations, PayloadConstants.JWT_ID,
                                                                   builder.getRawClaims().getStringClaimValue(PayloadConstants.JWT_ID));

        // use the token with the slightly shorter jti - show that the token works
        Page shorterJtiResponse = consumeToken(shorterJtiJwtToken);
        validationUtils.validateResult(shorterJtiResponse, currentAction, shorterJtiExpectations);

        // use the token with the longest jti - and show that it works
        Page longerJtiResponse = consumeToken(longerJtiJwtToken);
        validationUtils.validateResult(longerJtiResponse, currentAction, longerJtiExpectations);

        // test shows that a superset of a jti doesn't match

    }

    /**
     * Create a JWT token with a some unknown claim
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_other() throws Exception {

        builder.setClaim("other", someString);
        String jwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, "other", someString);

        Page response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with "nonce"
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_nonce() throws Exception {

        builder.setClaim(PayloadConstants.NONCE, someString);
        String jwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.NONCE, someString);

        Page response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with "at_hash"
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_at_hash() throws Exception {

        builder.setClaim(PayloadConstants.AT_HASH, someString);
        String jwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.AT_HASH, someString);

        Page response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with "typ"
     * The request should succeed as we should not look at the claim
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_typ() throws Exception {

        builder.setClaim(PayloadConstants.TYPE, someString);
        String jwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.TYPE, someString);

        Page response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token with a valid"azp"
     * The request should succeed
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_azp() throws Exception {

        builder.setClaim(PayloadConstants.AUTHORIZED_PARTY, builder.getRawClaims().getIssuer());
        String jwtToken = buildToken();

        Expectations expectations = addGoodResponseAndClaimsExpectations(currentAction, builder);
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.AUTHORIZED_PARTY, builder.getRawClaims().getIssuer());

        Page response = consumeToken(jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token missing part 1 of the 3 part token
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_omit_part1() throws Exception {

        String jwtToken = buildToken();
        String[] parts = jwtToken.split("\\.");
        String badToken = parts[1] + "." + parts[2];

        Expectations expectations = buildNegativeAttributeExpectations(".+JoseException.+was 2");

        Page response = consumeToken(badToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token missing part 2 of the 3 part token
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_omit_part2() throws Exception {

        String jwtToken = buildToken();
        String[] parts = jwtToken.split("\\.");
        String badToken = parts[0] + "." + parts[2];

        Expectations expectations = buildNegativeAttributeExpectations(".+JoseException.+was 2");

        Page response = consumeToken(badToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Create a JWT token missing part 3 of the 3 part token
     * The request should fail
     *
     * @throws Exception
     */
    @Test
    public void ConsumeMangledJWTTests_omit_part3() throws Exception {

        String jwtToken = buildToken();
        String[] parts = jwtToken.split("\\.");
        String badToken = parts[0] + "." + parts[1];

        Expectations expectations = buildNegativeAttributeExpectations(".+JoseException.+was 2");

        Page response = consumeToken(badToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }
}
