/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat.sharedTests;

import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt.fat.CommonMpJwtFat;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a common test class that will test for the proper behavior of webTarget/authnToken and the
 * client config property, com.ibm.ws.jaxrs.client.mpjwt.sendToken.
 * These tests are run with authnToken=mpjwt, or not set at all and
 * com.ibm.ws.jaxrs.client.mpjwt.sendToken is not set, or set to true, false, "true" or "false"
 *
 * Each test case will invoke an app on the mpjwt.client server. This app will set the client property
 * as requested and then invoke the target App on the mpjwt server.
 *
 * These tests are extended by 2 different classes. One specifies a server that has "webTarget"
 * configured and the other does not. That setting will affect behaviour and the webTargetConfigured
 * variable will be set by those 2 classes to indicate how some tests should behave.
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtLoginConfig_VariationTests extends CommonMpJwtFat {

    protected static Class<?> thisClass = MPJwtLoginConfig_VariationTests.class;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

    @Server("com.ibm.ws.security.mp.jwt.fat.builder")
    public static LibertyServer jwtBuilderServer;

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    protected static enum UseJWTToken {
        YES, NO
    };

//    protected static final boolean WillUseJWTToken = true;
//    protected static final boolean WillNotUseJwtToken = false;

    /********************** Helper Methods **************************/
    public static void loginConfigSetUp(String rsServerConfig) throws Exception {

        setUpAndStartRSServerForLoginConfigTests(resourceServer, rsServerConfig);
        setUpAndStartBuilderServer(jwtBuilderServer, "server_basicRegistry.xml");

    }

    protected static void setUpAndStartRSServerForLoginConfigTests(LibertyServer server, String configFile) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        deployRSServerLoginConfigApps(server);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
    }

    /**
     * Deploy the Apps that we'll use to test LoginConfig settings
     * 
     * @param server - the server to install the apps on
     * @throws Exception
     */
    protected static void deployRSServerLoginConfigApps(LibertyServer server) throws Exception {
        setupUtils.deployMicroProfileLoginConfigFormLoginInWebXmlBasicInApp(server);
        setupUtils.deployMicroProfileLoginConfigFormLoginInWebXmlMPJWTInApp(server);
        setupUtils.deployMicroProfileLoginConfigFormLoginInWebXmlNotInApp(server);
        setupUtils.deployMicroProfileLoginConfigMpJwtInWebXmlBasicInApp(server);
        setupUtils.deployMicroProfileLoginConfigMpJwtInWebXmlMPJWTInApp(server);
        setupUtils.deployMicroProfileLoginConfigMpJwtInWebXmlNotInApp(server);
        setupUtils.deployMicroProfileLoginConfigNotInWebXmlBasicInApp(server);
        setupUtils.deployMicroProfileLoginConfigNotInWebXmlMPJWTInApp(server);
        setupUtils.deployMicroProfileLoginConfigNotInWebXmlNotInApp(server);
        setupUtils.deployMicroProfileLoginConfigMultiLayerNotInWebXmlMPJWTInApp(server);

    }

    /**
     * Only tests that use form login call this method.
     * If ignoreApplicationAuthMethod is true, the server will always use mp_jwt, but, if it's false and there is login-config set
     * to MP-JWT, it'll use whatever login-config is set to. This method is called in a flow that expects to use FORM_LOGIN
     * 
     * @param _testName
     *            - test case name
     * @param builtToken
     *            - the token that we're using to test with
     * @param updatedJwtBuilderSettings
     *            - updated settings
     * @param expectations
     *            - what we expect after each step in the process.
     *
     * @throws Exception
     */
    public void genericLoginConfigFormLoginVariationTest(String rootContext, String app, String className, UseJWTToken useJwtToken) throws Exception {

        String builtToken = null;

        String testUrl = buildAppUrl(resourceServer, rootContext, app);

        WebClient webClient = actions.createWebClient();

        if (UseJWTToken.YES.equals(useJwtToken)) {

            builtToken = actions.getDefaultJwtToken(_testName, jwtBuilderServer);
        }

        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);

        if (UseJWTToken.YES.equals(useJwtToken)) {
            validationUtils.validateResult(response, goodAppExpectations(testUrl, className));
            return;
        } else {
            // make sure we got to the login page
            validationUtils.validateResult(response, CommonExpectations.successfullyReachedFormLoginPage());
        }
        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, goodAppExpectations(testUrl, className));
    }

    /**
     * This method runs the standard test flow for this test class. Basically, we obtain a JWT token and pass it to the test
     * servlet.
     * Depending on the the config and settings in the app, we expect access to the app, or a 401. This method makes sure that
     * we access the correct app, or get a 401 as expected. (it does check for a failure when success is expected, and for success
     * when a failure is expected)
     * 
     * @param rootContext
     *            - the apps root context
     * @param app
     *            - the app that should be invoked
     * @param className
     *            - the className that we should see in the server side log (in the case where we should access the app)
     * @param expectGoodResult
     *            - should we expect to get to the app, or expect a 401
     *
     * @throws Exception
     */
    public void genericLoginConfigVariationTest(String rootContext, String app, String className, ExpectedResult expectedResult) throws Exception {
        String builtToken = actions.getDefaultJwtToken(_testName, jwtBuilderServer);

        String testUrl = buildAppUrl(resourceServer, rootContext, app);

        WebClient webClient = actions.createWebClient();

        Expectations expectations = null;
        if (ExpectedResult.GOOD.equals(expectedResult)) {
            expectations = goodAppExpectations(testUrl, className);
        } else {
            expectations = badAppExpectations(MpJwtFatConstants.UNAUTHORIZED_MESSAGE);
        }
        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        validationUtils.validateResult(response, expectations);

    }
}