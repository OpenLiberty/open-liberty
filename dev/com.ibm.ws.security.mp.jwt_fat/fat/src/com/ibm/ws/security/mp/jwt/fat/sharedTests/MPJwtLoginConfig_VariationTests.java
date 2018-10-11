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
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
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

    protected static final boolean ExpectGoodResult = true;
    protected static final boolean ExpectBadResult = false;
    protected static final boolean WillUseJWTToken = true;
    protected static final boolean WillNotUseJwtToken = false;

    /********************** Helper Methods **************************/
    public static void loginConfigSetUp(String rsServerConfig) throws Exception {

        setUpAndStartRSServerForLoginConfigTests(resourceServer, rsServerConfig);
        setUpAndStartBuilderServer(jwtBuilderServer, "server_basicRegistry.xml");

    }

    protected static void setUpAndStartRSServerForLoginConfigTests(LibertyServer server, String configFile) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        deployRSServerLoginConfigApps(server);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile);
        saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
    }

    /**
     *
     * @param currentAction
     * @param theClass
     * @return
     * @throws Exception
     */
    public Expectations getGoodExpectations(String currentAction, String theClass) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addSuccessStatusCodesForActions(new String[] { currentAction });
        expectations.addExpectation(Expectation.createResponseExpectation(currentAction, MpJwtFatConstants.EXECUTED_MSG_STRING + theClass, "Did not execute " + theClass));
        return expectations;

    }

    // TODO - this isn't correct
    public Expectations getBadExpectations(String currentAction, String theClass) throws Exception {

        //        List<validationData> expectations = vData.addExpectation(inExpectations, MpJwtConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtConstants.EXCEPTION_MESSAGE, MpJwtConstants.STRING_CONTAINS, "Did NOT get expected exception message.", null, "HTTP response code: " + HttpServletResponse.SC_UNAUTHORIZED);

        Expectations expectations = new Expectations();
        expectations.addSuccessStatusCodesForActions(new String[] { currentAction });
        expectations.addExpectation(Expectation.createResponseExpectation(currentAction, MpJwtFatConstants.EXECUTED_MSG_STRING + theClass, "Did not execute " + theClass));
        return expectations;

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
    public void genericLoginConfigFormLoginVariationTest(String rootContext, String app, String className, boolean shouldUseJWTToken) throws Exception {

        String builtToken = null;

        String testUrl = buildAppUrl(resourceServer, rootContext, app);

        WebClient webClient = actions.createWebClient();

        if (shouldUseJWTToken) {
            builtToken = getDefaultJwtToken(jwtBuilderServer);
        }

        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);

        if (shouldUseJWTToken) {
            validationUtils.validateResult(response, TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, goodAppExpectations(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, testUrl, className));
            return;
        } else {
            // make sure we got to the login page
            validationUtils.validateResult(response, TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, CommonExpectations.successfullyReachedFormLoginPage(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE));
        }
        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, goodAppExpectations(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, testUrl, className));
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
    public void genericLoginConfigVariationTest(String rootContext, String app, String className, boolean expectGoodResult) throws Exception {
        String builtToken = getDefaultJwtToken(jwtBuilderServer);

        String testUrl = buildAppUrl(resourceServer, rootContext, app);

        WebClient webClient = actions.createWebClient();

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = null;
        if (expectGoodResult) {
            expectations = goodAppExpectations(currentAction, testUrl, className);
        } else {
            expectations = badAppExpectations(currentAction, MpJwtFatConstants.UNAUTHORIZED_EXCEPTION + ".*" + rootContext + ".*" + app);
        }
        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        validationUtils.validateResult(response, currentAction, expectations);

    }
}