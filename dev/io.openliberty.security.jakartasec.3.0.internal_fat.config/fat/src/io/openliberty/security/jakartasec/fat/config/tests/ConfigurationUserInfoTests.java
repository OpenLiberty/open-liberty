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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.config.tests;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerTraceExpectation;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletRequestExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;
import io.openliberty.security.jakartasec.fat.utils.WsSubjectExpectationHelpers;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Tests  @OpenIdProviderMetadata userinfo in @OpenIdAuthenticationMechanismDefinition.  Tests will create and use multiple test applications that set the userinfo endpoint
 * via EL variables.  The userinfo endpoint apps that the test will use create userinfo responses in the form of JWT tokens or Json.  These responses will show that the runtime
 * can process userinfo in either form.
 * The JWTs will contain various settins for the sub and groupIds claims or will define unique claims that match the callerNameClaim and callerGroupsClaim of @ClaimsDefinition.
 * These tests will show that the runtime will find the unique claims in userinfo after not finding those claims defined in @ClaimsDefinition in the access or id tokens.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ConfigurationUserInfoTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationUserInfoTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.userinfo.jwt.jwt")
    public static LibertyServer rpJwtJwtServer;
    @Server("jakartasec-3.0_fat.config.rp.userinfo.json.jwt")
    public static LibertyServer rpJsonJwtServer;
    @Server("jakartasec-3.0_fat.config.rp.userinfo.jwt.opaque")
    public static LibertyServer rpJwtOpaqueServer;
    @Server("jakartasec-3.0_fat.config.rp.userinfo.json.opaque")
    public static LibertyServer rpJsonOpaqueServer;
    public static LibertyServer rpServer;
    protected static ShrinkWrapHelpers swh = null;

    protected static boolean CallerNameClaimDefault = true;
    protected static boolean CallerGroupsClaimDefault = true;
    protected static boolean UserinfoSubValid = true;
    protected static boolean UserinfoGroupsValid = true;

    protected static String Caller = "caller";
    protected static String Groups = "groups";
    protected static String BothDefault = "bothDefault";
    protected static String SigAlg = "sigAlg";

    protected static String app = "GenericOIDCAuthMechanism";

    protected static String userinfoResponseFormat = Constants.JWT_TOKEN_FORMAT;

    @ClassRule
    public static RepeatTests repeat = createMultipleTokenTypeRepeats(Constants.USERINFO_JWT, Constants.USERINFO_JSONOBJECT);

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    // we dont' need to run some tests 4 times, we'll pick one of the 4 repeats and only run those tests during that one repeat
    // an example of a test where the access_token and userinfo response type doesn't matter would be an invalid userinfo endpoint
    public static class skipIfNotJwtUserInfoAndJwtToken extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            String instance = RepeatTestFilter.getRepeatActionsAsString();
            Log.info(thisClass, "skipIfNotJwtUserInfoAndJwtToken", instance);

            if (instance.contains(Constants.USERINFO_JSONOBJECT)) {
                Log.info(thisClass, "skipIfNotJwtUserInfoAndJwtToken", "Test case is using a userinfo endpoint that returns data in json format - skip test");
                testSkipped();
                return true;
            }
            if (instance.contains(Constants.OPAQUE_TOKEN_FORMAT)) {
                Log.info(thisClass, "skipIfNotJwtUserInfoAndJwtToken", "Test case is using an opaque access_token - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfNotJwtUserInfoAndJwtToken",
                     "Test case is using a jwt access_token and userinfo should return a jwt response - run test");
            return false;
        }
    }

    public static class skipIfJsonUserInfo extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {
            String instance = RepeatTestFilter.getRepeatActionsAsString();
            Log.info(thisClass, "skipIfJsonUserInfo", instance);

            if (instance.contains(Constants.USERINFO_JSONOBJECT)) {
                Log.info(thisClass, "skipIfJsonUserInfo", "Test case is using a userinfo endpoint that returns data in JSON format - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfJsonUserInfo", "Test case is using a userinfo endpoint that returns data in JWT format - run test");
            return false;
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // due to the way we create the test apps on the fly, we need a unique rp server instance for each repeat.
        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.USERINFO_JSONOBJECT)) {
            userinfoResponseFormat = "Json";
            if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.JWT_TOKEN_FORMAT)) {
                rpServer = rpJsonJwtServer;
            } else {
                rpServer = rpJsonOpaqueServer;
            }
        } else {
            userinfoResponseFormat = "Jwt";
            if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.JWT_TOKEN_FORMAT)) {
                rpServer = rpJwtJwtServer;
            } else {
                rpServer = rpJwtOpaqueServer;
            }

        }
        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps(); // run this after starting the RP so we have the rp port to update the openIdConfig.properties file within the apps

    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);

        // deploy the userinfo endpoint apps
        swh.dropinAppWithJose4j(rpServer, "UserInfo.war", "userinfo.servlets");
        swh.dropinAppWithJose4j(rpServer, "Discovery.war", "discovery.servlets");

        // deploy the userinfo test apps
        // apps where the callerNameClaim and callerGroupsClaim are the default
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet1.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet1", "OP1", new String[] { Caller, Groups }, CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet2.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet2", "OP1", new String[] { Groups }, CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, !UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet3.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet3", "OP1", new String[] { Caller }, CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             !UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        // apps where the token created by the OP does not contain the claim specified by either the callerNameClaim or callerGroupsClaim - we'll
        // then try to get the claims from the userinfo content.
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet4.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet4", "OP1", new String[] { Groups }, CallerNameClaimDefault, !CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet5.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet5", "OP1", new String[] { Groups }, CallerNameClaimDefault, !CallerGroupsClaimDefault,
                                                             UserinfoSubValid, !UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet6.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet6", "OP1", new String[] { Groups }, CallerNameClaimDefault, !CallerGroupsClaimDefault,
                                                             !UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet7.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet7", "OP1", new String[] { Caller }, !CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet8.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet8", "OP1", new String[] { Caller }, !CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, !UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet9.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet9", "OP1", new String[] { Caller }, !CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             !UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet10.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UserinfoTestServlet10", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUserInfo(rpHttpsBase, "appDoesNotExist")),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet11.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UserinfoTestServlet11", "allValues.openIdConfig.properties", TestConfigMaps.getUserInfoEmpty()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet12.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UserinfoTestServlet12", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getGoodUserInfo(opHttpsBase, "OP1")),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet13.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UserinfoTestServlet13", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUserInfoSplash(opHttpsBase)),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.USERINFO_JWT)) {
            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletRS384.war", "GenericOIDCAuthMechanism.war",
                                           buildUpdatedConfigMap("UserinfoTestServletRS384", "OP1", new String[] { Constants.SIGALG_RS384 }, CallerNameClaimDefault,
                                                                 CallerGroupsClaimDefault,
                                                                 UserinfoSubValid, UserinfoGroupsValid),
                                           "oidc.client.generic.servlets", "oidc.client.base.*");

            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletRS512.war", "GenericOIDCAuthMechanism.war",
                                           buildUpdatedConfigMap("UserinfoTestServletRS512", "OP1", new String[] { Constants.SIGALG_RS512 }, CallerNameClaimDefault,
                                                                 CallerGroupsClaimDefault,
                                                                 UserinfoSubValid, UserinfoGroupsValid),
                                           "oidc.client.generic.servlets", "oidc.client.base.*");

//            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletES256.war", "GenericOIDCAuthMechanism.war",
//                                           buildUpdatedConfigMap("UserinfoTestServletES256", "OP1", new String[] { Constants.SIGALG_ES256 }, CallerNameClaimDefault,
//                                                                 CallerGroupsClaimDefault,
//                                                                 UserinfoSubValid, UserinfoGroupsValid),
//                                           "oidc.client.generic.servlets", "oidc.client.base.*");
//
//            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletES384.war", "GenericOIDCAuthMechanism.war",
//                                           buildUpdatedConfigMap("UserinfoTestServletES384", "OP1", new String[] { Constants.SIGALG_ES384 }, CallerNameClaimDefault,
//                                                                 CallerGroupsClaimDefault,
//                                                                 UserinfoSubValid, UserinfoGroupsValid),
//                                           "oidc.client.generic.servlets", "oidc.client.base.*");
//
//            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletES512.war", "GenericOIDCAuthMechanism.war",
//                                           buildUpdatedConfigMap("UserinfoTestServletES512", "OP1", new String[] { Constants.SIGALG_ES512 }, CallerNameClaimDefault,
//                                                                 CallerGroupsClaimDefault,
//                                                                 UserinfoSubValid, UserinfoGroupsValid),
//                                           "oidc.client.generic.servlets", "oidc.client.base.*");
//
            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletHS256.war", "GenericOIDCAuthMechanism.war",
                                           buildUpdatedConfigMap("UserinfoTestServletHS256", "OP1", new String[] { Constants.SIGALG_HS256 }, CallerNameClaimDefault,
                                                                 CallerGroupsClaimDefault,
                                                                 UserinfoSubValid, UserinfoGroupsValid),
                                           "oidc.client.generic.servlets", "oidc.client.base.*");

            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletHS384.war", "GenericOIDCAuthMechanism.war",
                                           buildUpdatedConfigMap("UserinfoTestServletHS384", "OP1", new String[] { Constants.SIGALG_HS384 }, CallerNameClaimDefault,
                                                                 CallerGroupsClaimDefault,
                                                                 UserinfoSubValid, UserinfoGroupsValid),
                                           "oidc.client.generic.servlets", "oidc.client.base.*");

            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletHS512.war", "GenericOIDCAuthMechanism.war",
                                           buildUpdatedConfigMap("UserinfoTestServletHS512", "OP1", new String[] { Constants.SIGALG_HS512 }, CallerNameClaimDefault,
                                                                 CallerGroupsClaimDefault,
                                                                 UserinfoSubValid, UserinfoGroupsValid),
                                           "oidc.client.generic.servlets", "oidc.client.base.*");

            // the sigAlg userinfo apps build/return the default content, so for the tests that have caller or group annotation values that don't match
            // what the OP returns, we'll get the proper info in the userinfo response, but, the sig alg will be wrong, so we shouldn't use this info
            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet20.war", "GenericOIDCAuthMechanism.war",
                                           buildUpdatedConfigMap("UserinfoTestServlet20", "OP1", new String[] { Caller, Constants.SIGALG_RS384 }, !CallerNameClaimDefault,
                                                                 CallerGroupsClaimDefault,
                                                                 UserinfoSubValid, UserinfoGroupsValid),
                                           "oidc.client.generic.servlets", "oidc.client.base.*");

            swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet21.war", "GenericOIDCAuthMechanism.war",
                                           buildUpdatedConfigMap("UserinfoTestServlet21", "OP1", new String[] { Groups, Constants.SIGALG_RS512 }, CallerNameClaimDefault,
                                                                 !CallerGroupsClaimDefault,
                                                                 UserinfoSubValid, UserinfoGroupsValid),
                                           "oidc.client.generic.servlets", "oidc.client.base.*");

        }

        // create an app where the provider points to our test discovery endpoint
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletDiscUserInfoRS384_ignored.war", "UserinfoAnnotation.war",
                                       buildUpdatedConfigMap("UserinfoTestServletDiscUserInfoRS384_ignored", "OP1_RS384_Discovery", new String[] { Constants.SIGALG_RS256 },
                                                             CallerNameClaimDefault,
                                                             CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "userinfo.annotation.servlets", "oidc.client.base.*");

        // create an app where the provider points to our test discovery endpoint
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServletDiscUserInfoRS384_used.war", "UserinfoAnnotation.war",
                                       buildUpdatedConfigMap("UserinfoTestServletDiscUserInfoRS384_used", "OP1_RS384_Discovery", new String[] { Constants.SIGALG_RS256, Caller },
                                                             !CallerNameClaimDefault,
                                                             CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "userinfo.annotation.servlets", "oidc.client.base.*");

    }

    /**
     * Build a map of EL var settings to put into the openIdConfig.properties file in the app.
     * We'll set callerNameClaims or callerGroupsClaim if requested, Then we'll set the userinfoEndpoint url value based on the parms
     *
     * @param app - the app that we're building
     * @param provider - the OP provider that app uses - used to set endpoint urls
     * @param whatAreWeTesting - flag indicating if the app will be testing name/sub or groups (helps set the app)
     * @param isCallerNameClaimDefault - flag indicating if we'll be setting a unique callerNameClaim value
     * @param isCallerGroupsClaimDefault - flag indicating if we'll be setting a unique callerGroupsClaim value
     * @param isUserinfoSubValid - flag indicating if we need a userinfo app that will set invalid content in the sub or callerNameClaim
     * @param isUserinfoGroupsValid - flag indicating if we need a userinfo app that will set invalid content in the groups or callerGroupsClaim
     * @return - return the map of config el vars to set
     * @throws Exception
     */
    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, String[] whatAreWeTesting, boolean isCallerNameClaimDefault,
                                                            boolean isCallerGroupsClaimDefault,
                                                            boolean isUserinfoSubValid, boolean isUserinfoGroupsValid) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = null;
        if (provider.contains("Discovery")) {
            testPropMap = TestConfigMaps.getProviderDiscUri(rpHttpsBase, provider);
        } else {
            testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);
        }

        //***********************************************************************************************************
        // set values that will cause the runtime to need to use the userinfo values
        // set the config caller name claim to an invalid value - by default, a good value is set
        if (!isCallerNameClaimDefault) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getBadCallerNameClaim());
        }

        // set the config caller groups claim to an invalid value - by default, a good value is set
        if (!isCallerGroupsClaimDefault) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getBadCallerGroupsClaim());
        }
        //***********************************************************************************************************

        Log.info(thisClass, "buildUpdatedConfigMap", "whatAreWeTesting: " + Arrays.toString(whatAreWeTesting));

        // set the userinfo config setting that will return the appropriate userinfo content
        if (isInList(whatAreWeTesting, new String[] { Caller }) && !isInList(whatAreWeTesting, new String[] { Groups })) {
            if (isCallerNameClaimDefault) {
                if (isUserinfoSubValid) {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase,
                                                                                      userinfoResponseFormat + TestConfigMaps.UserInfoDefaultCallerNameClaimGoodCaller));
                } else {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase,
                                                                                      userinfoResponseFormat + TestConfigMaps.UserInfoDefaultCallerNameClaimBadCaller));
                }
            } else {
                if (isUserinfoSubValid) {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase, userinfoResponseFormat + TestConfigMaps.UserInfoOtherCallerNameClaimGoodCaller));
                } else {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase, userinfoResponseFormat + TestConfigMaps.UserInfoOtherCallerNameClaimBadCaller));
                }
            }
        }
        if (isInList(whatAreWeTesting, new String[] { Groups }) && !isInList(whatAreWeTesting, new String[] { Caller })) {
            if (isCallerGroupsClaimDefault) {
                if (isUserinfoGroupsValid) {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase,
                                                                                      userinfoResponseFormat + TestConfigMaps.UserInfoDefaultCallerGroupsClaimGoodGroups));
                } else {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase,
                                                                                      userinfoResponseFormat + TestConfigMaps.UserInfoDefaultCallerGroupsClaimBadGroups));
                }
            } else {
                if (isUserinfoGroupsValid) {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase,
                                                                                      userinfoResponseFormat + TestConfigMaps.UserInfoOtherCallerGroupsClaimGoodGroups));
                } else {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                           TestConfigMaps.getUserInfo(rpHttpsBase,
                                                                                      userinfoResponseFormat + TestConfigMaps.UserInfoOtherCallerGroupsClaimBadGroups));
                }
            }
        }
        if (isInList(whatAreWeTesting, new String[] { Caller }) && isInList(whatAreWeTesting, new String[] { Groups })) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getUserInfo(rpHttpsBase, userinfoResponseFormat + TestConfigMaps.AllDefault));
        }

        if (isInList(whatAreWeTesting, Constants.ALL_TEST_SIGALGS)) {
            // we won't modify the signature alg that the client is using, we'll configure a userinfo endpoint app that will create a response
            // using the alg that we specify
            String sigAlg = null;
            // only expecting one sig alg in the list of things to process - once we find a valid sigalg, stop and use that
            for (String req : whatAreWeTesting) {
                if (Arrays.asList(Constants.ALL_TEST_SIGALGS).contains(req)) {
                    sigAlg = req;
                    break;
                }
            }
            if (sigAlg.equals(Constants.SIGALG_RS256)) {
                Log.info(thisClass, "buildUpdatedConfigMap", "Using the default signature algorithm, so use the default userinfo app, or the value that is set previously.");
            } else {
                testPropMap = TestConfigMaps.mergeMaps(testPropMap,
                                                       TestConfigMaps.getUserInfo(rpHttpsBase, userinfoResponseFormat + sigAlg + TestConfigMaps.AllDefault));
            }
        }

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, app, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    private static boolean isInList(String[] request, String[] checkAgainst) {

        for (String req : request) {
            Log.info(thisClass, "isInList", "req: " + req + " checkAgainst: " + checkAgainst.toString());
            if (Arrays.asList(checkAgainst).contains(req)) {
                Log.info(thisClass, "isInList", "true");
                return true;
            }
        }
        Log.info(thisClass, "isInList", "false");
        return false;

    }

    /**
     * Common test method that runs the standard end to end test, but also checks that the subject in the access_token/id_token is not set (because we used a unique callerNameClaim
     * that we won't get from the OP. The test app will be accessed using the sub provided from userinfo.
     *
     * @param appRoot - the app root to invoke
     * @param app - the servlet name to invoke
     * @return - return the page that we landed on (in case the caller needs to do further checking)
     * @throws Exception
     */
    public Page runGoodEndToEndTestWithSubOverride(String appRoot, String app) throws Exception {

        Page response = runGoodEndToEndTest(appRoot, app);
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                                           + ServletMessageConstants.SUBS_CLAIMS_SUB_NULL
                                                                                           + rspValues.getSubject(), "Did not see that the subject in the openidcontext was as expected and the sub claim in the userinfo claims was not set."));

        validationUtils.validateResult(response, expectations);

        return response;
    }

    /**
     * Check for the proper error messages in the trace (not messages log) for cases where we expect a signature algorithm mismatch
     *
     * @param response - the response to check
     * @throws Exception
     */
    public void extraUserinfoSigMismatchValidation(Page response) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerTraceExpectation(rpServer, MessageConstants.CWWKS2418W_USERINFO_PROBLEM, "Did not receive a message in the trace stating that there was a problem with the response from the userinfo endpoint."));
        expectations.addExpectation(new ServerTraceExpectation(rpServer, MessageConstants.CWWKS2520E_SIGNATURE_NOT_ALLOWED, "Did not receive a message in the trace stating that the signature algorithm was not one that was allowed."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Run the discovery endpooint and load the response into a json object. Update that object by adding the "userinfo_signing_alg_values_supported" attribute (which is
     * architected, but not included by the Liberty OP). Send the updated data to the test discovery endpoint where it'll be saved and then returned when the RP/client invokes
     * discovery.
     *
     * @param provider - the provider whose discovery endpoint should be invoked
     * @param alg - the algorithm that we'll say userinfo supports
     * @throws Exception
     */
    protected void setNextDiscoveryResponse(String provider, String alg) throws Exception {

        // get discovery info for provider
        Page response = invokeDiscovery(provider);

        // parse discovery response
        JsonObject origDiscData = loadJsonDataFromResponse(response);

        // add userinfo sig alg to discovery data
        JsonObject updatedDiscData = buildNewDiscoveryData(origDiscData, alg);

        // rebuild json data
        String updatedDiscDataString = updatedDiscData.toString();
        Log.info(thisClass, "setNextDiscoveryResponse", "Updated discovery data string: " + updatedDiscDataString);
        // push json data to test discovery endpoint
        updateDiscDataInTestApp(updatedDiscDataString);
    }

    /**
     * Invoke the providers discovery endpoint
     *
     * @param provider - the provider whose discovery endpoint should be invoked
     * @return - the discovery response
     * @throws Exception
     */
    protected Page invokeDiscovery(String provider) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        String url = opHttpsBase + "/oidc/endpoint/" + provider + "/.well-known/openid-configuration";
        Page response = actions.invokeUrl(_testName, webClient, url);

        return response;
    }

    /**
     * Parse the discovery response into a json object
     *
     * @param response - the response from the discovery endpoint
     * @return - the json object containing the discovery data
     * @throws Exception
     */
    protected JsonObject loadJsonDataFromResponse(Page response) throws Exception {

        JsonObject obj = null;
        try {
            String responseText = WebResponseUtils.getResponseText(response);
            Log.info(thisClass, "loadJsonDataFromResponse", responseText);
            obj = Json.createReader(new StringReader(responseText)).readObject();
        } catch (Exception e) {
            Log.error(thisClass, "loadJsonDataFromResponse", e);
        }

        return obj;
    }

    /**
     * Update the discovery data (in json format) with "userinfo_signing_alg_values_supported" set to the alg requested
     *
     * @param discResponse - the json formatted discovery response
     * @param alg - the algorithm that "userinfo_signing_alg_values_supported" will be set to
     * @return - the updated json discovery data
     * @throws Exception
     */
    protected JsonObject buildNewDiscoveryData(JsonObject discResponse, String alg) throws Exception {

        JsonObjectBuilder updatedDiscData = Json.createObjectBuilder(discResponse);

        updatedDiscData.add("userinfo_signing_alg_values_supported", Json.createArrayBuilder().add(alg).build());

        JsonObject updatedresponse = updatedDiscData.build();
        return updatedresponse;
    }

    /**
     * Invoke the test discovery endpoint to save the updated discovery data
     *
     * @param discData - the updated discovery data to save in the test discovery endpoint
     * @throws Exception
     */
    protected void updateDiscDataInTestApp(String discData) throws Exception {

        String url = rpHttpBase + "/Discovery/.well-known/openid-configuration";

        Map<String, List<String>> requestParms = new HashMap<String, List<String>>();
        requestParms.put("UpdatedDiscoveryData", Arrays.asList(discData));

        SecurityFatHttpUtils httpUtils = new SecurityFatHttpUtils();
        httpUtils.getHttpConnectionWithAnyResponseCode(url, HTTPRequestMethod.PUT, requestParms);

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * callerNameClaim and callerGroupsClaim are set to the Liberty default values within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * This means that we'll find the information that we need from the id or access tokens and do not need to get it from userinfo
     * A test userinfo app is used that sets sub and gruopIds to valid values (values that match the token created by the OP)
     * Show that we have access to the protected app and that the sub and groups are correct
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationUserInfoTests_defaultNameAndGroupClaims_UserinfoGoodSubGoodGroups() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet1", app);
    }

    /**
     * callerNameClaim and callerGroupsClaim are set to the Liberty default values within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * This means that we'll find the information that we need from the id or access tokens and do not need to get it from userinfo
     * A test userinfo app is used that sets sub to a valid value and sets groupIds to invalid groups
     * Show that we have access to the protected app and that the sub and groups are correct (and we don't pick up the bad groups from the userinfo data)
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUserInfoTests_defaultNameAndGroupClaims_UserinfoGoodSubBadGroups() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet2", app);
    }

    /**
     * callerNameClaim and callerGroupsClaim are set to the Liberty default values within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * This means that we'll find the information that we need from the id or access tokens and do not need to get it from userinfo
     * A test userinfo app is used that sets sub to an invalid value and sets groupIds to valid groups
     * Show that we have access to the protected app and that the sub and groups are correct (and we don't pick up the bad sub from the userinfo data)
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUserInfoTests_defaultNameAndGroupClaims_UserinfoBadSubGoodGroups() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet3", app);
    }

    /**
     * callerNameClaim is set to the Liberty default value within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * and set callerGroupsClaim is set to badCallerGroups.
     * This means that we'll find the sub in either the id or access tokens, but we won't find the group info - we'll need to get that from userinfo
     * A test userinfo app is used that sets sub and gruopIds to valid values (values that match what the OP puts into the default claims)
     * Show that we have access to the protected app and that the sub and groups are correct
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationUserInfoTests_defaultNameClaimOtherGroupClaims_UserinfoGoodSubGoodGroups() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet4", app);
    }

    /**
     * callerNameClaim is set to the Liberty default value within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * and set callerGroupsClaim is set to badCallerGroups.
     * This means that we'll find the sub in either the id or access tokens, but we won't find the group info - we'll need to get that from userinfo
     * A test userinfo app is used that sets sub to a valid value (a value that match what the OP puts into the default claims)
     * That userinfo app will include a claim called badCallerGroups with "group" values that don't exist.
     * Show that we DO NOT have access to the protected app since the groups are not in the roles that allow access
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUserInfoTests_defaultNameClaimOtherGroupClaims_UserinfoGoodSubBadGroups() throws Exception {

        String appRoot = "UserinfoTestServlet5";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addForbiddenStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, url, "Did not fail to land on " + url));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, Constants.AUTHORIZATION_ERROR, "Did not receive the authorization failed message."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS9104A_NO_ACCESS_FOR_USER, "Did not receive an error message stating that a user is not granted access to a resource."));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * callerNameClaim is set to the Liberty default value within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * and set callerGroupsClaim is set to badCallerGroups.
     * This means that we'll find the sub in either the id or access tokens, but we won't find the group info - we'll need to get that from userinfo
     * A test userinfo app is used that sets sub to an invalid value and gruopIds to valid values (value that match what the OP puts into the default claim)
     * The sub will still come from the id/access tokens and won't need that info from userinfo, but we will pick up the valid value that userinfo
     * returns for groups.
     * Show that we have access to the protected app and that the sub and groups are correct
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUserInfoTests_defaultNameClaimOtherGroupClaims_UserinfoBadSubGoodGroups() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet6", app);
    }

    /**
     * badCallerGroups is set to the Liberty default value within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * and set callerNameClaim is set to badCallerName.
     * This means that we'll find the groups in either the id or access tokens, but we won't find the sub info - we'll need to get that from userinfo
     * A test userinfo app is used that sets sub and gruopIds to valid values (values that match what the OP puts into the default claims)
     * Show that we have access to the protected app and that the sub and groups are correct
     * One thing of note - the subject information in the id_token and access_token of the OpenIdContext will not match the subject value within WSSubject -
     * the token values will be null, bug a valid value will be in WSSubject.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUserInfoTests_otherNameClaimDefaultGroupClaims_UserinfoGoodSubGoodGroups() throws Exception {

        runGoodEndToEndTestWithSubOverride("UserinfoTestServlet7", app);
    }

    /**
     * badCallerGroups is set to the Liberty default value within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * and set callerNameClaim is set to badCallerName.
     * This means that we'll find the groups in either the id or access tokens, but we won't find the sub info - we'll need to get that from userinfo
     * A test userinfo app is used that sets groups to an invalid value and sub to a valid value (value that match what the OP puts into the default claim)
     * The groups will still come from the id/access tokens and won't need that info from userinfo, but we will pick up the valid value that userinfo
     * returns for sub.
     * Show that we have access to the protected app and that the sub and groups are correct
     * One thing of note - the subject information in the id_token and access_token of the OpenIdContext will not match the subject value within WSSubject -
     * the token values will be null, but a valid value will be in WSSubject.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUserInfoTests_otherNameClaimDefaultGroupClaims_UserinfoGoodSubBadGroups() throws Exception {

        runGoodEndToEndTestWithSubOverride("UserinfoTestServlet8", app);
    }

    /**
     * badCallerGroups is set to the Liberty default value within the @ClaimsDefinition of the @OpenIdAuthenticationMechanismDefinition
     * and set callerNameClaim is set to badCallerName.
     * This means that we'll find the groups in either the id or access tokens, but we won't find the sub info - we'll need to get that from userinfo
     * A test userinfo app is used that sets groups to a valid value and sub to an invalid value (value that match what the OP puts into the default claim)
     * The groups will still come from the id/access tokens and won't need that info from userinfo, but we will pick up the value that userinfo
     * returns for sub.
     * Show that we have access to the protected app and that groups is correct - show that sub is actually set to the invalid value - the runtime
     * will only be checking the groups/role for access.
     * One thing of note - the subject information in the id_token and access_token of the OpenIdContext will not match the subject value within WSSubject -
     * the token values will be null, but the invalid value will be in WSSubject.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationUserInfoTests_otherNameClaimDefaultGroupClaims_UserinfoBadSubGoodGroups() throws Exception {

        String appRoot = "UserinfoTestServlet9";

        Expectations expectations = getGeneralAppExpecations(app);

        String updatedRequester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;

        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, updatedRequester + ServletMessageConstants.CONTEXT_SUBJECT
                                                                                                 + "someUnknownUser", "Did not find the correct subject in the OpenIdContext."));
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, updatedRequester
                                                                                                 + ServletMessageConstants.CLAIMS_SUBJECT
                                                                                                 + "null", "Subject from id_token is not null"));
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenExpectations(null, expectations, updatedRequester, rspValues);
        OpenIdContextExpectationHelpers.getOpenIdContextIdTokenExpectations(null, expectations, updatedRequester, rspValues, false);
        OpenIdContextExpectationHelpers.getOpenIdContextIssuerExpectations(null, expectations, updatedRequester, rspValues);
        OpenIdContextExpectationHelpers.getOpenIdContextTokenTypeExpectations(null, expectations, updatedRequester, rspValues);

        rspValues.setSubject("someUnknownUser");
        WsSubjectExpectationHelpers.getWsSubjectExpectations(null, expectations, ServletMessageConstants.SERVLET, rspValues);
        ServletRequestExpectationHelpers.getServletRequestExpectations(null, expectations, ServletMessageConstants.SERVLET, rspValues);

        WebClient webClient = getAndSaveWebClient();

        Log.info(thisClass, _testName, "headers: " + rspValues.getHeaders());

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test that we'll get a failure in the server log indicating that the userinfo endpoint was not valid - the request to access the app will succeed because the request can be
     * validated without the need of the userinfo response.
     *
     * @throws Exception
     */
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNotJwtUserInfoAndJwtToken.class)
    public void ConfigurationUserInfoTests_invalidUserinfoEndpoint() throws Exception {

        Page response = runGoodEndToEndTest("UserinfoTestServlet10", app);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.SRVE0190E_FILE_NOT_FOUND, "Did not receive a file not found error message stating that the userinfo endpoint is not found."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test that we'll use the default userinfo if the configured userinfo endpoint is ""
     *
     * @throws Exception
     */
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNotJwtUserInfoAndJwtToken.class)
    public void ConfigurationUserInfoTests_emptyUserinfoEndpoint() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet11", app);
    }

    /**
     * Test with a valid userinfo endpoint configured (when run, returns a jwt userinfo response signed with RS256)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNotJwtUserInfoAndJwtToken.class)
    public void ConfigurationUserInfoTests_goodUserinfoEndpoint() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet12", app);
    }

    /**
     * Test that with the userinfo endpoint set to the Open Liberty splash page, instead of a valid userinfo endpoint, we'll be able to access the app because the request can be
     * validated without the need of the userinfo response.
     *
     * @throws Exception
     */
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNotJwtUserInfoAndJwtToken.class)
    public void ConfigurationUserInfoTests_splashUserinfoEndpoint() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet13", app);
    }

    /**
     * Tset that with the userinfo endpoint returning a jwt signed with RS384 instead of RS256 we'll still be able to access the app because we don't need the information from
     * userinfo
     *
     * @throws Exception
     */
    // Don't need all combinations @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_invalidRS384JWTUserinfoResponse() throws Exception {

        Page response = runGoodEndToEndTest("UserinfoTestServletRS384", app);

        extraUserinfoSigMismatchValidation(response);

    }

    /**
     * Tset that with the userinfo endpoint returning a jwt signed with RS512 instead of RS256 we'll still be able to access the app because we don't need the information from
     * userinfo
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_invalidRS512JWTUserinfoResponse() throws Exception {

        Page response = runGoodEndToEndTest("UserinfoTestServletRS512", app);

        extraUserinfoSigMismatchValidation(response);

    }

    // TODO - enabled once the userinfo app can handle signing with ES algs
//    @Test
//    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
//    public void ConfigurationUserInfoTests_invalidES256JWTUserinfoResponse() throws Exception {
//
//        Page response = runGoodEndToEndTest("UserinfoTestServletES256", app);
//
//        extraUserinfoSigMismatchValidation(response);
//
//    }
//
//    @Test
//    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
//    public void ConfigurationUserInfoTests_invalidES384JWTUserinfoResponse() throws Exception {
//
//        Page response = runGoodEndToEndTest("UserinfoTestServletES384", app);
//
//        extraUserinfoSigMismatchValidation(response);
//
//    }
//
//    @Test
//    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
//    public void ConfigurationUserInfoTests_invalidES512JWTUserinfoResponse() throws Exception {
//
//        Page response = runGoodEndToEndTest("UserinfoTestServletES512", app);
//
//        extraUserinfoSigMismatchValidation(response);
//
//    }

    /**
     * Tset that with the userinfo endpoint returning a jwt signed with HS256 instead of RS256 we'll still be able to access the app because we don't need the information from
     * userinfo
     *
     * @throws Exception
     */
    // Don't need all combinations @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_invalidHS256JWTUserinfoResponse() throws Exception {

        Page response = runGoodEndToEndTest("UserinfoTestServletHS256", app);

        extraUserinfoSigMismatchValidation(response);

    }

    /**
     * Tset that with the userinfo endpoint returning a jwt signed with HS384 instead of RS256 we'll still be able to access the app because we don't need the information from
     * userinfo
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_invalidHS384JWTUserinfoResponse() throws Exception {

        Page response = runGoodEndToEndTest("UserinfoTestServletHS384", app);

        extraUserinfoSigMismatchValidation(response);

    }

    /**
     * Tset that with the userinfo endpoint returning a jwt signed with HS512 instead of RS256 we'll still be able to access the app because we don't need the information from
     * userinfo
     *
     * @throws Exception
     */
    // Don't need all combinations @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_invalidHS512JWTUserinfoResponse() throws Exception {

        Page response = runGoodEndToEndTest("UserinfoTestServletHS512", app);

        extraUserinfoSigMismatchValidation(response);

    }

    /**
     * Tset that with the userinfo endpoint returning a jwt signed with RS384 instead of RS256. We're not setting the callerNameClaim in the annotation and the Liberty OP does not
     * return the default callerNameClaim. This means that we won't be able to set the subject from the access or id token - we'll need to use information from userdata, but, the
     * userinfo response is in a jwt signed with a different signature algorithm. We'll get a 401 status code.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_otherNameClaimDefaultGroupClaims_UserinfoGoodSubGoodGroups_userinfoSigAlgMismatch() throws Exception {

        String appRoot = "UserinfoTestServlet20";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, expectations);

        extraUserinfoSigMismatchValidation(response);

    }

    /**
     * Tset that with the userinfo endpoint returning a jwt signed with RS384 instead of RS256. We're not setting the callerGroupClaim in the annotation and the Liberty OP does not
     * return the default callerGroupClaim. This means that we won't be able to set the groups from the access or id token - we'll need to use information from userdata, but, the
     * userinfo response is in a jwt signed with a different signature algorithm. We'll get a 401 status code.
     *
     * @throws Exception
     */
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_defaultNameClaimOtherGroupClaims_UserinfoGoodSubGoodGroups_userinfoSigAlgMismatch() throws Exception {

        String appRoot = "UserinfoTestServlet21";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.FORBIDDEN_STATUS));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, url, "Did not fail to land on " + url));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, Constants.FORBIDDEN, "Did not receive the forbidden message."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, Constants.AUTHORIZATION_ERROR, "Did not receive the authorization failed message."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS9104A_NO_ACCESS_FOR_USER, "Did not receive an error message stating that a user is not granted access to a resource."));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, expectations);

        extraUserinfoSigMismatchValidation(response);

    }

    /**
     * Test that we'll ignore the userinfo_signing_alg_values_supported value in the discovery data when we don't need to use userinfo data (because we can get the information that
     * we need from the id or access tokens)
     * The test will use a "test" discovery endpoint that will set "userinfo_signing_alg_values_supported" to "RS384" (the Liberty OP does not set
     * "userinfo_signing_alg_values_supported" in its discovery data).
     * The test "userinfo" endpoint will return a jwt signed with RS256 - since we can get the information that we need from the id/access token, we don't need to use the userinfo
     * data.
     *
     * @throws Exception
     */
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_UserInfoNameClaimAndGroupClaimNotNeeded_userInfoSigAlgMismatchWithDiscoveryUserInfoValue() throws Exception {

        setNextDiscoveryResponse("OP1", "RS384");
        runGoodEndToEndTest("UserinfoTestServletDiscUserInfoRS384_ignored", "UserinfoAnnotation");

    }

    /**
     * Test that we'll not be granted access to the test app when the "userinfo_signing_alg_values_supported" value in the discovery data is set to RS384 and we do need to use the
     * userinfo data (because we can not get the information that we need from the id or access tokens)
     * The test will use a "test" discovery endpoint that will set "userinfo_signing_alg_values_supported" to "RS384" (the Liberty OP does not set
     * "userinfo_signing_alg_values_supported" in its discovery data).
     * The test "userinfo" endpoint will return a jwt signed with RS256 - since we can not get the information that we need from the id/access token, we will need to use the
     * userinfo
     * data, but the signature will not match what discovery says we require.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJsonUserInfo.class)
    public void ConfigurationUserInfoTests_UserInfoNameClaimAndGroupClaimNeeded_userInfoSigAlgMismatchWithDiscoveryUserInfoValue() throws Exception {

        setNextDiscoveryResponse("OP1", "RS384");

        String appRoot = "UserinfoTestServletDiscUserInfoRS384_used";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/UserinfoAnnotation";;

        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_DOES_NOT_CONTAIN, url, "Did not fail to land on " + url));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, Constants.UNAUTHORIZED_MESSAGE, "Did not receive the unauthorized message."));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, expectations);

        extraUserinfoSigMismatchValidation(response);

    }
}
