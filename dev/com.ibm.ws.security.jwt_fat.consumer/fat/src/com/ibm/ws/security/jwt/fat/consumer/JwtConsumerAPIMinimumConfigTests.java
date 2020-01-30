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
import com.ibm.ws.security.fat.common.servers.ServerInstanceUtils;
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
 * The configuration used for these tests is the minimum supported
 */

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtConsumerAPIMinimumConfigTests extends CommonSecurityFat {

    public final String AppStartMsg = ".*CWWKZ0001I.*" + JwtConsumerConstants.JWT_CONSUMER_SERVLET + ".*";

    @Server("com.ibm.ws.security.jwt_fat.consumer")
    public static LibertyServer consumerServer;

    public static final TestValidationUtils validationUtils = new TestValidationUtils();
    public static final ConsumerHelpers consumerHelpers = new ConsumerHelpers();
    private static final JwtConsumerActions actions = new JwtConsumerActions();
    private final String currentAction = null;

    protected static JWTTokenBuilder builder = null;

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(consumerServer);
        consumerServer.startServerUsingExpandedConfiguration("server_minimumConfig.xml");
        SecurityFatHttpUtils.saveServerPorts(consumerServer, JwtConsumerConstants.BVT_SERVER_1_PORT_NAME_ROOT);

    }

    public Expectations addGoodConsumerClientResponseAndClaimsExpectations() throws Exception {
        return consumerHelpers.addGoodConsumerClientResponseAndClaimsExpectations(currentAction, builder, consumerServer);
    }

    private static void createBuilderWithRSAClaims() throws Exception {

        builder = consumerHelpers.createBuilderWithDefaultClaims();
        // set the default signing key for this test class (individual test cases can override if needed)
        consumerHelpers.setDefaultKeyFile(consumerServer, "rsa_privateKey.pem");
        consumerHelpers.updateBuilderWithRSASettings(builder);
    }

    public void createBuilderWithHSClaims() throws Exception {

        builder = consumerHelpers.createBuilderWithDefaultClaims();
    }

    /**************************************************************
     * Tests
     **************************************************************/

    /**
     * Use the minimum configuration needed to use HS256.
     * This means that the config has to specify HS256 for the signatureAlgorithm and specify the sharedKey.
     * It also has to specify an Issuer (since we require one)
     * We're creating a JWT Token to meet what the config specifies, so, the token should be validated.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerAPIMinimumConfigTests_minimumHSARunableConfig() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_minimumHS256Config.xml", AppStartMsg);
        ServerInstanceUtils.waitForSSLMsg(consumerServer);

        createBuilderWithHSClaims();
        String jwtToken = builder.build();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "defaultJwtConsumer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Use the minimum configuration needed to use RS256.
     * This means that the config has to specify an Issuer (since we require one)
     * The config does not specify any specific SSL Config attributes, so, server wide trust is used to validate the signature
     * We're creating a JWT Token to meet what the config specifies, so, the token should be validated.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerAPIMinimumConfigTests_minimumSSLConfig_global() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_minimumConfig_ServerWideSSL.xml", AppStartMsg);
        ServerInstanceUtils.waitForSSLMsg(consumerServer);

        createBuilderWithRSAClaims();
        String jwtToken = builder.build();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "defaultJwtConsumer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

    /**
     * Use the minimum configuration needed to use RS256.
     * This means that the config has to specify an Issuer (since we require one)
     * The config does specify SSL Config attributes, so, that will be used to validate the signature
     * We're creating a JWT Token to meet what the config specifies, so, the token should be validated.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerAPIMinimumConfigTests_minimumSSLConfig_consumer() throws Exception {

        consumerServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_minimumConfig_SSLInConsumer.xml", AppStartMsg);
        ServerInstanceUtils.waitForSSLMsg(consumerServer);

        createBuilderWithRSAClaims();
        String jwtToken = builder.build();

        Expectations expectations = addGoodConsumerClientResponseAndClaimsExpectations();

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "defaultJwtConsumer", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }
}
