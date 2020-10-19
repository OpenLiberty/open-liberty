/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.builder.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.JwtBuilderMessageConstants;
import com.ibm.ws.security.jwt.fat.builder.validation.BuilderTestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run basic JWT Builder Config tests.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderAPIConfigNoIdTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final BuilderTestValidationUtils validationUtils = new BuilderTestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(builderServer);
        builderServer.addInstalledAppForValidation(JWTBuilderConstants.JWT_BUILDER_SERVLET);
        builderServer.startServerUsingExpandedConfiguration("server_noId.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // the server's default config contains an invalid value (on purpose),
        // tell the fat framework to ignore it!
        builderServer.addIgnoredErrors(Arrays.asList(JwtBuilderMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE, JwtBuilderMessageConstants.CWPKI0812E_CANT_FIND_KEY, JwtBuilderMessageConstants.CWWKS6059W_KEY_MANAGEMENT_KEY_ALIAS_MISSING));

    }

    /**
     * <P>
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has "" as the id
     * <LI>Create a JWT build specifying "" as the config id and show that even
     * with a config with "" as the id, the builder throws and exception *
     * <LI>The builder does not pick up the config that has the id ""
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get another token built using the default values for the JWT
     * Token
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigNoIdTests_noId() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtBuilderMessageConstants.CWWKS6008E_BUILD_ID_UNKNOWN + ".+\\[\\]", "Response did not show the expected failure."));

        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, JWTBuilderConstants.EMPTY_STRING);
        validationUtils.validateResult(response, expectations);

    }

}
