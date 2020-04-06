/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.consumer;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.consumer.actions.JwtConsumerActions;
import com.ibm.ws.security.jwt.fat.consumer.utils.ConsumerHelpers;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * These tests validate that the behavior is as expected based on the configuration used.
 * They specifically focus on server configs that have a server SSL configuration defined.
 *
 */

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtConsumerApiConfigWithGlobalTrustTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.consumer")
    public static LibertyServer consumerServer;

    public static final TestValidationUtils validationUtils = new TestValidationUtils();
    public static final ConsumerHelpers consumerHelpers = new ConsumerHelpers();
    private static final JwtConsumerActions actions = new JwtConsumerActions();
    private final String currentAction = null;
    protected static String jwtToken = null;

    protected static JWTTokenBuilder builder = null;

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(consumerServer);
        consumerServer.addInstalledAppForValidation(JwtConsumerConstants.JWT_CONSUMER_SERVLET);
        consumerServer.startServerUsingExpandedConfiguration("server_configGlobalTrust.xml", CommonWaitForAppChecks.getSSLChannelReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(consumerServer, JwtConsumerConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        createBuilderWithDefaultClaims();
        jwtToken = builder.build();

    }

    public Expectations addGoodConsumerClientResponseAndClaimsExpectations() throws Exception {
        return consumerHelpers.addGoodConsumerClientResponseAndClaimsExpectations(currentAction, builder, consumerServer);
    }

    private static void createBuilderWithDefaultClaims() throws Exception {

        builder = consumerHelpers.createBuilderWithDefaultClaims();
        // set the default signing key for this test class (individual test cases can override if needed)
        consumerHelpers.setDefaultKeyFile(consumerServer, "rsa_privateKey.pem");
        consumerHelpers.updateBuilderWithRSASettings(builder);

    }

    /**************************************************************
     * Tests
     **************************************************************/
    /**
     * server.xml has a config that has omitted the trustStoreRef, but did specify a trustedAlias. The signatureAlgorithm is RS256
     * and there IS global trust. The token will be generated using RS256.
     * We expect a successful outcome as the needed cert will be found in the global trust
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigWithGlobalTrustTests_trustStoreRefOmitted_withTrustedAlias_globalTrust_oneCert() throws Exception {

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefOmitted_RS256_withTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has omitted the trustStoreRef, and does not specify a trustedAlias. The signatureAlgorithm is
     * RS256
     * and there IS global trust. The token will be generated using RS256.
     * We expect a successful outcome as the needed cert will be found in the global trust
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigWithGlobalTrustTests_trustStoreRefOmitted_withoutTrustedAlias_globalTrust_oneCert() throws Exception {

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefOmitted_RS256_withoutTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank trustStoreRef (""), but did specify a trustedAlias. The signatureAlgorithm is
     * RS256
     * and there IS global trust. The token will be generated using RS256.
     * We expect a successful outcome as the needed cert will be found in the global trust
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigWithGlobalTrustTests_trustStoreRefBlank_withTrustedAlias_globalTrust_oneCert() throws Exception {

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefBlank_RS256_withTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * server.xml has a config that has a blank trustStoreRef (""), and does not specify a trustedAlias. The signatureAlgorithm is
     * RS256
     * and there IS global trust. The token will be generated using RS256.
     * We expect a successful outcome as the needed cert will be found in the global trust
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigWithGlobalTrustTests_trustStoreRefBlank_withoutTrustedAlias_globalTrust_oneCert() throws Exception {

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "trustStoreRefBlank_RS256_withoutTrustedAlias", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

}
