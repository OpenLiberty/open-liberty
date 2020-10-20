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

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.consumer.actions.JwtConsumerActions;
import com.ibm.ws.security.jwt.fat.consumer.utils.ConsumerHelpers;
import com.ibm.ws.security.jwt.fat.consumer.utils.JwtConsumerMessageConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtConsumerApiConfigBlankIdTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.consumer")
    public static LibertyServer consumerServer;

    public static final TestValidationUtils validationUtils = new TestValidationUtils();
    public static final ConsumerHelpers consumerHelpers = new ConsumerHelpers();
    private static final JwtConsumerActions actions = new JwtConsumerActions();
    private final String currentAction = null;

    protected JWTTokenBuilder builder = null;

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(consumerServer);
        skipRestoreServerTracker.addServer(consumerServer);
        consumerServer.addInstalledAppForValidation(JwtConsumerConstants.JWT_CONSUMER_SERVLET);
        consumerServer.startServerUsingExpandedConfiguration("server_configTests2.xml");
        SecurityFatHttpUtils.saveServerPorts(consumerServer, JwtConsumerConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        // one of the JWT Consumer configs has an empty SignatureAlg value which results in a CWWKG0032W warning - mark this as "OK"
        consumerServer.addIgnoredErrors(Arrays.asList(JwtConsumerMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".+" + "signatureAlgorithm", JwtConsumerMessageConstants.CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED));

        // set the default signing key for this test class (individual test cases can override if needed)
        consumerHelpers.setDefaultKeyFile(consumerServer, "rsa_privateKey.pem");
    }

    /**************************************************************
     * Tests
     **************************************************************/

    /**
     * server.xml has a config that has a blank ("") configId. The request to the consumer passes a blank ("") id.
     * We expect a failure as the blank ("") configId passed will be substituted with the default "defaultJwtConfig".
     * This built in config does NOT have a trusted issuer, so, the test will fail with a trusted issuer not found error.
     *
     * @throws Exception
     */
    @Test
    public void JwtConsumerApiConfigTests_blankId_blankConsumerIdPassedInRequest() throws Exception {

        builder = consumerHelpers.createBuilderWithDefaultClaims();

        builder.setIssuer("testIssuer2");
        String jwtToken = consumerHelpers.buildToken(builder, _testName);

        Expectations expectations = consumerHelpers.buildNegativeAttributeExpectations(JwtConsumerMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".+\\[testIssuer2\\]", currentAction, consumerServer, JwtConsumerConstants.JWT_CONSUMER_DEFAULT_CONFIG);

        Page response = actions.invokeJwtConsumer(_testName, consumerServer, "", jwtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }

}
