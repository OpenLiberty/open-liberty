/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat.sharedTests;

import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt.fat.CommonMpJwtFat;
import com.ibm.ws.security.mp.jwt.fat.MpJwtMessageConstants;
import com.ibm.ws.security.mp.jwt.fat.actions.MpJwtFatActions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
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
public class MPJwtPropagationTests extends CommonMpJwtFat {

    protected static Class<?> thisClass = MPJwtPropagationTests.class;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

    @Server("com.ibm.ws.security.mp.jwt.fat.client")
    public static LibertyServer resourceClient;

    @Server("com.ibm.ws.security.mp.jwt.fat.builder")
    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private final MpJwtFatActions actions = new MpJwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();

    String defaultUser = MpJwtFatConstants.TESTUSER;
    String defaultPassword = MpJwtFatConstants.TESTUSERPWD;

    protected static Boolean getsGoodResults = true;
    protected static Boolean webTargetConfigured_true = true;
    protected static Boolean webTargetConfigured_false = false;

    protected static Boolean webTargetConfigured = webTargetConfigured_true;

    public static void propagationSetUp(String clientConfig, boolean webTargetConfiguredSetting) throws Exception {

        setUpAndStartRSClient(resourceClient, clientConfig);
        setUpAndStartRSServerForPropagationTests(resourceServer, "rs_server_for_propagation_tests.xml");
        setUpAndStartBuilderServer(jwtBuilderServer, "server_basicRegistry.xml");

        webTargetConfigured = webTargetConfiguredSetting;

    }

    /********************** Helper Methods **************************/
    protected static void setUpAndStartRSClient(LibertyServer server, String configFile) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        deployRSClientApps(server);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_3_PORT_NAME_ROOT);
    }

    protected static void setUpAndStartRSServerForPropagationTests(LibertyServer server, String configFile) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        deployRSServerPropagationApps(server);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
    }

    protected static void deployRSClientApps(LibertyServer server) throws Exception {
        setupUtils.deployMicroProfileClientApp(server);
    }

    /**
     * Deploy the Propagation app (runs on the RS server and invokes the apps on the rs client)
     *
     * @param server - server to install the propagation app on
     * @throws Exception
     */
    protected static void deployRSServerPropagationApps(LibertyServer server) throws Exception {
        setupUtils.deployMicroProfileLoginConfigNotInWebXmlMPJWTInApp(server);
    }

    /**
     * The method that runs the steps of propagation tests.
     * Get a goken, invoke the propagation client app which will pass the token to the final app.
     * A parm passed to the propagation app will tell the app how/where to pass the token
     *
     * @param where - where/how to pass the JWT token in the request from propagation client to test app
     * @param expectedSuccessOrFailure - Do we expect a successful or failed invocation of test app
     * @throws Exception
     */
    public void MPJwtPropagation_generic_test(String where, boolean expectedSuccessOrFailure) throws Exception {

        String builtToken = getDefaultJwtToken();

        String clientUrl = buildClientUrl();

        WebClient webClient = actions.createWebClient();

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair(MpJwtFatConstants.TARGET_APP, buildAppUrl(resourceServer, MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                                                               MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP)));
        // add where the client should set the token (or not set it at all)
        if (where != null) {
            params.add(new NameValuePair(MpJwtFatConstants.WHERE, where));
        }

        Page response = actions.invokeUrlWithBearerTokenAndParms(_testName, webClient, clientUrl, builtToken, params);
        Expectations expectations = getPropagationExpectations(expectedSuccessOrFailure);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Use the token endpoint to generate a JWT
     *
     * @return - returns the built JWT
     * @throws Exception
     */
    public String getDefaultJwtToken() throws Exception {
        String builtToken = actions.getJwtFromTokenEndpoint(_testName, "defaultJWT", SecurityFatHttpUtils.getServerSecureUrlBase(jwtBuilderServer), defaultUser, defaultPassword);
        Log.info(thisClass, _testName, "JWT Token: " + builtToken);
        return builtToken;
    }

    /**
     * Build the propagation app url
     *
     * @return - returns the built propagation app url
     * @throws Exception
     */
    public String buildClientUrl() throws Exception {
        String clientUrl = SecurityFatHttpUtils.getServerSecureUrlBase(resourceClient) + MpJwtFatConstants.LOGINCONFIG_PROPAGATION_ROOT_CONTEXT + "/rest/"
                           + MpJwtFatConstants.LOGINCONFIG_PROPAGATION
                           + "/" + MpJwtFatConstants.MPJWT_GENERIC_APP_NAME;
        return clientUrl;
    }

    /**
     * All cases get a status code of 200 - when there is supposed to be a failure, there will be a 401 logged as the client
     * attempts to access the server app
     * All tests invoke the same client and server apps - the difference is in how the client propagation is set (or not set) when
     * the client invokes the server side app
     *
     * @param currentAction
     *            - action invoking the client app (which will invoke the server app)
     * @param successfulResults
     *            - tests expects a successful outcome
     * @return - return the appropriate expectations
     * @throws Exception
     */
    public Expectations getPropagationExpectations(boolean successfulResults) throws Exception {
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MpJwtFatConstants.EXECUTED_MSG_STRING
                                                                                           + MpJwtFatConstants.MPJWT_APP_CLASS_PROPAGATION_CLIENT, "Did not execute "
                                                                                                                                                   + MpJwtFatConstants.LOGINCONFIG_PROPAGATION));

        if (successfulResults) {
            expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MpJwtFatConstants.EXECUTED_MSG_STRING
                                                                                               + MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTNOTINWEBXML_MPJWTINAPP, "Did not execute "
                                                                                                                                                                             + MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTNOTINWEBXML_MPJWTINAPP));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MpJwtFatConstants.HTTP_UNAUTHORIZED_EXCEPTION, "Did not get 401 exception out of target application."));
            expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5522E_MPJWT_TOKEN_NOT_FOUND));
        }

        return expectations;

    }

    /*************************************** Tests ***************************************/

    @Test
    public void MPJwtPropagation_doNotUseClientProp() throws Exception {
        MPJwtPropagation_generic_test(null, webTargetConfigured);
    }

    @Test
    public void MPJwtPropagation_useClientProp_stringTrue() throws Exception {
        MPJwtPropagation_generic_test(MpJwtFatConstants.PROPAGATE_TOKEN_STRING_TRUE, getsGoodResults);
    }

    @Test
    public void MPJwtPropagation_useClientProp_booleanTrue() throws Exception {
        MPJwtPropagation_generic_test(MpJwtFatConstants.PROPAGATE_TOKEN_BOOLEAN_TRUE, getsGoodResults);
    }

    @Test
    public void MPJwtPropagation_useClientProp_stringFalse() throws Exception {
        MPJwtPropagation_generic_test(MpJwtFatConstants.PROPAGATE_TOKEN_STRING_FALSE, webTargetConfigured);
    }

    @Test
    public void MPJwtPropagation_useClientProp_booleanFalse() throws Exception {
        MPJwtPropagation_generic_test(MpJwtFatConstants.PROPAGATE_TOKEN_BOOLEAN_FALSE, webTargetConfigured);
    }

}
