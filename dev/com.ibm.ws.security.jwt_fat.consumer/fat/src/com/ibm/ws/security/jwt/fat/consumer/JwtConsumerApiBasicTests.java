/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.consumer;

import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtApiExpectation;
import com.ibm.ws.security.fat.common.jwt.sharedTests.ConsumeMangledJWTTests;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.consumer.actions.JwtConsumerActions;
import com.ibm.ws.security.jwt.fat.consumer.utils.ConsumerHelpers;
import com.ibm.ws.security.jwt.fat.consumer.utils.JwtConsumerMessageConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that use the Consumer API when extending the ConsumeMangledJWTTests.
 * The server will be configured with the appropriate jwtConsumer's
 * We will validate that we can <use> (and the output is correct):
 * 1) create a JWTConsumer
 * 2) create a JwtToken object
 * 3) create a claims object
 * 4) use all of the get methods on the claims object
 * 5) use toJsonString method got get all attributes in the payload
 *
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtConsumerApiBasicTests extends ConsumeMangledJWTTests {

    @Server("com.ibm.ws.security.jwt_fat.consumer")
    public static LibertyServer consumerServer;

    public static final ConsumerHelpers consumerHelpers = new ConsumerHelpers();
    private static final JwtConsumerActions actions = new JwtConsumerActions();
    private final String jwtConsumerId = "jwtConsumer";

    @BeforeClass
    public static void setUp() throws Exception {
        transformApps(consumerServer);
        consumerServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        serverTracker.addServer(consumerServer);
        skipRestoreServerTracker.addServer(consumerServer);
        consumerServer.addInstalledAppForValidation(JwtConsumerConstants.JWT_CONSUMER_SERVLET);
        consumerServer.startServerUsingExpandedConfiguration("server_jwtConsumer.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(consumerServer, JwtConsumerConstants.BVT_SERVER_1_PORT_NAME_ROOT);

    }

    @Override
    @Before
    public void commonBeforeTest() {
        super.commonBeforeTest();
        try {
            builder = createBuilderWithDefaultClaims();
        } catch (Exception e) {
            Log.info(thisClass, "commonBeforeTest", e.toString());
            e.printStackTrace(System.out);
            // just set the builder to null - this will cause the test cases to blow up
            builder = null;
        }

    }

    @Override
    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {

        JWTTokenBuilder builder = consumerHelpers.createBuilderWithDefaultConsumerClaims();

        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT);

        return builder;
    }

    /**
     * Consume the built JWT Token - for these tests, that means passing the JWT Token to the JWTCLientConsumer app. This app will
     * invoke the JWT Consumer api's which will process/verify the token.
     *
     * @param token
     *            - the token to consume
     */
    @Override
    public Page consumeToken(String token) throws Exception {

        return actions.invokeJwtConsumer(_testName, consumerServer, jwtConsumerId, token);

    }

    @Override
    public Expectations addGoodResponseAndClaimsExpectations(String currentAction, JWTTokenBuilder builder) throws Exception {

        return consumerHelpers.addGoodConsumerClientResponseAndClaimsExpectations(currentAction, builder, consumerServer);
    }

    @Override
    public Expectations updateExpectationsForJsonAttribute(Expectations expectations, String key, Object value) throws Exception {

        return consumerHelpers.updateExpectationsForJsonAttribute(expectations, key, value);

    }

    @Override
    protected Expectations buildNegativeAttributeExpectations(String specificErrorId) throws Exception {

        return consumerHelpers.buildNegativeAttributeExpectations(specificErrorId, currentAction, consumerServer, jwtConsumerId);

    }

    // get error messages
    @Override
    protected String getJtiReusedMsg() {
        return JwtConsumerMessageConstants.CWWKS6045E_JTI_REUSED;
    }

    @Override
    protected String getIssuerNotTrustedMsg() {
        return JwtConsumerMessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED;
    }

    @Override
    protected String getSignatureNotValidMsg() {
        return JwtConsumerMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID;
    }

    @Override
    protected String getTokenExpiredMsg() {
        return JwtConsumerMessageConstants.CWWKS6025E_TOKEN_EXPIRED;
    }

    @Override
    protected String getMalformedClaimMsg() {
        return JwtConsumerMessageConstants.CWWKS6043E_MALFORMED_CLAIM;
    }

    @Override
    protected String getIatAfterExpMsg() {
        return JwtConsumerMessageConstants.CWWKS6024E_IAT_AFTER_EXP;
    }

    @Override
    protected String getIatAfterCurrentTimeMsg() {
        return JwtConsumerMessageConstants.CWWKS6044E_IAT_AFTER_CURRENT_TIME;
    }

    @Override
    protected String getBadAudienceMsg() {
        return JwtConsumerMessageConstants.CWWKS6023E_BAD_AUDIENCE;
    }

    @Override
    protected String getBadNotBeforeMsg() {
        return JwtConsumerMessageConstants.CWWKS6026E_FUTURE_NBF;
    }

    /**************************************************************
     * Consumer specific Tests
     **************************************************************/
    /**
     * Test scenario:
     * - JwtConsumer.create() - Should use the default consumer config ID ({@value #DEFAULT_CONSUMER_ID})
     * - JwtConsumer.createJwt(null)
     * Expected results:
     * - Successfully instantiate JwtConsumer object
     * - CWWKS6042E message saying provided JWT string was null or empty
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiBasicTests_noConfigId_noToken() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, SecurityFatHttpUtils.getServerUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT));
        expectations.addExpectation(new JwtApiExpectation(JwtConsumerConstants.STRING_MATCHES, JwtConsumerMessageConstants.CWWKS6040E_JWT_STRING_EMPTY + ".+" + JwtConsumerConstants.JWT_CONSUMER_DEFAULT_CONFIG, "Response did not show the expected failure."));

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, null, null);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /***********************************************
     * Shared Tests are specified by ConsumeMangledJWTTests
     ***********************************************/

}
