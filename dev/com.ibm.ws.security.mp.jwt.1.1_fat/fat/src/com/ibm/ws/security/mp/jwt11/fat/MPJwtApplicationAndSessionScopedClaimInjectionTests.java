/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.CommonMpJwtFat;
import com.ibm.ws.security.mp.jwt11.fat.utils.MpJwtMessageConstants;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run test for mpJwt config attributes.
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
 * <LI>test will invoke genericReconfigTest to:
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
public class MPJwtApplicationAndSessionScopedClaimInjectionTests extends CommonMpJwtFat {

    protected static Class<?> thisClass = MPJwtBasicTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private final TestValidationUtils validationUtils = new TestValidationUtils();
    String testAction = TestActions.ACTION_INSTALL_APP;

    private final String[] reconfigMsgs = { MpJwtMessageConstants.CWWKS5603E_CLAIM_CANNOT_BE_INJECTED, MpJwtMessageConstants.CWWKZ0002E_EXCEPTION_WHILE_STARTING_APP };

    /**
     * Startup the resource server
     * Set flag to tell the code that runs between tests NOT to restore the server config between tests
     * (none of the tests use the config that the server starts with - this setting will save run time)
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartRSServerForTests(resourceServer, "rs_server_noApp.xml");

    }

    /**
     * Don't restore between tests
     * All of the tests in this class will reconfigure the server
     */
    @Override
    public void restoreTestServers() {
        Log.info(thisClass, "restoreTestServersWithCheck", "* Skipping server restore **");
        logTestCaseInServerLogs("** Skipping server restore **");
    }

    /**
     * Gets the resource server up and running.
     * Sets properties in bootstrap.properties that will affect server behavior
     * Sets up and installs the test apps (does not start them)
     * Adds the server to the serverTracker (used for server restore and test class shutdown)
     * Starts the server using the provided configuration file
     * Saves the port info for this server (allows tests with multiple servers to know what ports each server uses)
     * Allow some failure messages that occur during startup (they're ok and doing this prevents the test framework from failing)
     *
     * @param server
     *            - the server to process
     * @param configFile
     *            - the config file to start the server with
     * @param jwkEnabled
     *            - do we want jwk enabled (sets properties in bootstrap.properties that the configs will use)
     * @throws Exception
     */
    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");

        generateRSServerTestApps(server);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH));
    }

    /**
     * create the wars that the tests will use
     *
     * @param server
     *            - resource server
     * @throws Exception
     */
    protected static void generateRSServerTestApps(LibertyServer server) throws Exception {
        genericDeployApp(server, "microProfileApplicationScopedClaimInjectString", "com.ibm.ws.jaxrs.fat.microProfileApplicationScopedClaimInjectString.MicroProfileApp");
        genericDeployApp(server, "microProfileApplicationScopedClaimInjectLong", "com.ibm.ws.jaxrs.fat.microProfileApplicationScopedClaimInjectLong.MicroProfileApp");
        genericDeployApp(server, "microProfileApplicationScopedClaimInjectSet", "com.ibm.ws.jaxrs.fat.microProfileApplicationScopedClaimInjectSet.MicroProfileApp");
        genericDeployApp(server, "microProfileApplicationScopedClaimInjectBoolean", "com.ibm.ws.jaxrs.fat.microProfileApplicationScopedClaimInjectBoolean.MicroProfileApp");
        genericDeployApp(server, "microProfileSessionScopedClaimInjectString", "com.ibm.ws.jaxrs.fat.microProfileSessionScopedClaimInjectString.MicroProfileApp");
        genericDeployApp(server, "microProfileSessionScopedClaimInjectLong", "com.ibm.ws.jaxrs.fat.microProfileSessionScopedClaimInjectLong.MicroProfileApp");
        genericDeployApp(server, "microProfileSessionScopedClaimInjectSet", "com.ibm.ws.jaxrs.fat.microProfileSessionScopedClaimInjectSet.MicroProfileApp");
        genericDeployApp(server, "microProfileSessionScopedClaimInjectBoolean", "com.ibm.ws.jaxrs.fat.microProfileSessionScopedClaimInjectBoolean.MicroProfileApp");

    }

    /**
     * deploy/generate an individual app based on the the war and app name passed in
     *
     * @param server
     *            - he resource server
     * @param warName
     *            - the war to create
     * @param className
     *            - the class to add to the war
     * @throws Exception
     */
    private static void genericDeployApp(LibertyServer server, String warName, String className) throws Exception {
        List<String> classList = new ArrayList<String>();
        classList.add(className);
        ShrinkHelper.exportAppToServer(server, setupUtils.genericCreateArchiveWithoutJsps(warName, classList), DeployOptions.DISABLE_VALIDATION);

    }

    /**
     * The generic test flow - all of the tests are simply reconfiguring the server (to add an app), then waiting for specific
     * error messages in the server side log.
     *
     * @param configFile
     *            - the configuration to use in the reconfig
     * @param appName
     *            - the appname to look for in the error message (makes sure we see the correct instance of the failure)
     * @throws Exception
     */
    protected void genericReconfigTest(String configFile, String appName) throws Exception {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, "CWWKZ0002E.*" + appName, "Did not find failure CWWKZ0002E message starting app " + appName
                                                                                                           + "."));

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, configFile, reconfigMsgs);

        // just checking for server log messages, so, no need to pass is a response or step/action (there is only one step)
        validationUtils.validateResult(expectations);

    }

    /***************************************************** Tests ****************************************************/
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_ApplicationScope_StringType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) application scope
        // 2) injects a java.lang.String type
        genericReconfigTest("rs_server_appScopeClaimInjectString.xml", "microProfileApplicationScopedClaimInjectString");

    }

    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_ApplicationScope_LongType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) application scope
        // 2) injects a java.lang.Long type
        genericReconfigTest("rs_server_appScopeClaimInjectLong.xml", "microProfileApplicationScopedClaimInjectLong");

    }

    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_ApplicationScope_SetType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) application scope
        // 2) injects a java.util.Set type
        genericReconfigTest("rs_server_appScopeClaimInjectSet.xml", "microProfileApplicationScopedClaimInjectSet");

    }

    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_ApplicationScope_BooleanType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) application scope
        // 2) injects a java.lang.Boolean type
        genericReconfigTest("rs_server_appScopeClaimInjectBoolean.xml", "microProfileApplicationScopedClaimInjectBoolean");

    }

    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_SessionScope_StringType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) Session scope
        // 2) injects a java.lang.String type
        genericReconfigTest("rs_server_SessionScopeClaimInjectString.xml", "microProfileSessionScopedClaimInjectString");

    }

    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_SessionScope_LongType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) Session scope
        // 2) injects a java.lang.Long type
        genericReconfigTest("rs_server_SessionScopeClaimInjectLong.xml", "microProfileSessionScopedClaimInjectLong");

    }

    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_SessionScope_SetType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) Session scope
        // 2) injects a java.util.Set type
        genericReconfigTest("rs_server_SessionScopeClaimInjectSet.xml", "microProfileSessionScopedClaimInjectSet");

    }

    @ExpectedFFDC({ "org.jboss.weld.exceptions.DefinitionException", "com.ibm.ws.container.service.state.StateChangeException" })
    @Test
    public void MPJwtApplicationAndSessionScopedClaimInjection_SessionScope_BooleanType() throws Exception {

        // reconfigure the server to have an app that has:
        // 1) Session scope
        // 2) injects a java.lang.Boolean type
        genericReconfigTest("rs_server_SessionScopeClaimInjectBoolean.xml", "microProfileSessionScopedClaimInjectBoolean");

    }
}
