/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt21.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt21MPConfigTests;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run test for mpJwt 2.1 config attributes.
 * <OL>
 * <LI>Setup
 * <OL>
 * <LI>a builder server is started and runs an app that will use the jwt builder to generate jwt tokens
 * The server config contains multiple builder configs to allow generation of tokens with different content.
 * <LI>a resource server will be started with a generic mpJwt config
 * </OL>
 * <LI>All of the tests follow the same "flow".
 * <OL>
 * <LI>the resource server will be re-configured to suit the needs of the test case
 * <LI>any extra/unique claims (IF NEEDED) will be created
 * <LI>a token will be created using the builder app (passing the extra/unique claims if they exist - for inclusion
 * in the token)
 * <LI>if test has set up a negative condition, expectations specific to the test will be created
 * <LI>test will invoke genericConfigTest to:
 * <OL>
 * <LI>initialize some jwt token processing tooling and log the contents of the JWT Token (in a human readable format)
 * <LI>if expectations were not passed in, generate expectations to validate output from the test apps
 * (validates we got the correct app, and that the runtime processed/sees the correct token content)
 * <LI>Loop through 3 different test apps (each using injection in a different way)
 * <OL>
 * <LI>Invoke the app
 * <LI>Validate the response/log contents against the expectations
 * </OL>
 * </OL>
 * </OL>
 * </OL>
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat({ MPJwt21FatConstants.MP_JWT_11, MPJwt21FatConstants.MP_JWT_12, MPJwt21FatConstants.MP_JWT_20 })
public class MPJwt21ConfigUsingBuilderTests extends MPJwt21MPConfigTests {

    protected static Class<?> thisClass = MPJwt21ConfigUsingBuilderTests.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat")
    public static LibertyServer resourceServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    //    private static final boolean ExpectExtraMsgs = true;

    public static final JwtTokenBuilderUtils builderHelpers = new JwtTokenBuilderUtils();

    /**
     * Startup the builder and resource servers
     * Set flag to tell the code that runs between tests NOT to restore the server config between tests
     * (very few of the tests use the config that the server starts with - this setting will save run time)
     * Build the list of app urls and class names that each test will use
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml", false);

        setUpAndStartRSServerForApiTests(resourceServer, jwtBuilderServer, "rs_server_orig_2_1.xml", false);

        skipRestoreServerTracker.addServer(resourceServer);

    }

    /***************************************************** Tests ****************************************************/

    // TODO - do we need the parms passed to genericConfigTest?
    /********************************* Start Token Age tests *********************************/
    /**
     * Tests specified, but longer token_age in config
     * The request should succeed.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt21ConfigUsingBuilderTests_Default_TokenAge() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_orig_2_1.xml");
        // by default the test tooling puts the token in Authorization header using "Bearer"
        genericConfigTest(resourceServer, MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, 5, null);

    }

    @Test
    public void MPJwt21ConfigUsingBuilderTests_Long_TokenAge() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_TokenAge_Long.xml");
        // by default the test tooling puts the token in Authorization header using "Bearer"
        genericConfigTest(resourceServer, MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, 5, null);

    }

    @Test
    public void MPJwt21ConfigUsingBuilderTests_Short_TokenAge() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_TokenAge_Short.xml");

        // by default the test tooling puts the token in Authorization header using "Bearer"
        genericConfigTest(resourceServer, MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, 5, setShortTokenAgeExpectationsVerbose(resourceServer));

    }

    /********************************** End Token Age tests **********************************/

    /********************************* Start Clock Skew tests ********************************/
    // Clock skew in the mpJwt config is not new and those tests are covered in the MPJwtConfigUsingBuilderTests of the com.ibm.ws.security.mp.jwt.1.1_fat.commonTest project
    /********************************** End Clock Skew tests *********************************/

    /********************************* Start (new encrypt attr) tests *********************************/
    // keyManagementKeyAlias in the mpJwt config is not new and those tests are covered in the MPJwt12ConfigUsingBuilderTests in the com.ibm.ws.security.mp.jwt.1.2_fat.commonTest project
    /********************************* End (new encrypt attr) tests ***********************************/

}
