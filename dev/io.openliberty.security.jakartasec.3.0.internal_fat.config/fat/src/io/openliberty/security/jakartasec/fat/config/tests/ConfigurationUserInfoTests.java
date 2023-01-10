/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletRequestExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;
import io.openliberty.security.jakartasec.fat.utils.WsSubjectExpectationHelpers;

/**
 * Tests  @OpenIdProviderMetadata userinfo in @OpenIdAuthenticationMechanismDefinition.  Tests will create and use multiple test applications that set the userinfo endpoint
 * via EL variables.  The userinfo endpoint apps that the test apps will use create userinfo responses in the form of JWT tokens or Json.  These responses will show that the runtime
 * can process userinfo in either form.
 * The JWTs will contain various settins for the sub and groupIds claims or will define unique claims that match the callerNameClaim and callerGroupsClaim of @ClaimsDefinition.
 * These tests will show that the runtime will find the unique claims in userinfo after not finding those claims defined in @ClaimsDefinition in the access or id tokens.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
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

    protected static String app = "GenericOIDCAuthMechanism";

    protected static String userinfoResponseFormat = Constants.JWT_TOKEN_FORMAT;

    @ClassRule
    public static RepeatTests repeat = createMultipleTokenTypeRepeats(Constants.USERINFO_JWT, Constants.USERINFO_JSONOBJECT);

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    // we dont' need to run some tests 4 times, we'll pick on of the 4 repeats an only run those tests during that one repeat
    // an example of a test where the access_token and userinfo response type doesn't matter would be an invalid userinfo endpoint
    public static class skipIfNotJwtUserInfoAndJwtToken extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            Log.info(thisClass, "skipIfNotJwtUserInfoAndJwtToken", "here");

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

    public static class skipIfJwtUserInfo extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {
            String instance = RepeatTestFilter.getRepeatActionsAsString();
            Log.info(thisClass, "skipIfJwtUserInfo", instance);

            if (instance.contains(Constants.USERINFO_JWT)) {
                Log.info(thisClass, "skipIfJwtUserInfo", "Test case is using a userinfo endpoint that returns data in JWT format - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfJwtUserInfo", "Test case is using a userinfo endpoint that returns data in JSON format - run test");
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

        // deploy the userinfo test apps
        // apps where the callerNameClaim and callerGroupsClaim are the default
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet1.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet1", "OP1", BothDefault, CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet2.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet2", "OP1", Groups, CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, !UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet3.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet3", "OP1", Caller, CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             !UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        // apps where the token created by the OP does not contain the claim specified by either the callerNameClaim or callerGroupsClaim - we'll
        // then try to get the claims from the userinfo content.
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet4.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet4", "OP1", Groups, CallerNameClaimDefault, !CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet5.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet5", "OP1", Groups, CallerNameClaimDefault, !CallerGroupsClaimDefault,
                                                             UserinfoSubValid, !UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet6.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet6", "OP1", Groups, CallerNameClaimDefault, !CallerGroupsClaimDefault,
                                                             !UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet7.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet7", "OP1", Caller, !CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet8.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet8", "OP1", Caller, !CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             UserinfoSubValid, !UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet9.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap("UserinfoTestServlet9", "OP1", Caller, !CallerNameClaimDefault, CallerGroupsClaimDefault,
                                                             !UserinfoSubValid, UserinfoGroupsValid),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet10.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UserinfoTestServlet10", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUserInfo(rpHttpsBase, "appDoesNotExist")),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UserinfoTestServlet11.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UserinfoTestServlet11", "allValues.openIdConfig.properties", TestConfigMaps.getUserInfoEmpty()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
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
    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, String whatAreWeTesting, boolean isCallerNameClaimDefault,
                                                            boolean isCallerGroupsClaimDefault,
                                                            boolean isUserinfoSubValid, boolean isUserinfoGroupsValid) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);

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

        Log.info(thisClass, "buildUpdatedConfigMap", "whatAreWeTesting: " + whatAreWeTesting);

        // set the userinfo config setting that will return the appropriate userinfo content
        if (whatAreWeTesting.equals(Caller)) {
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
        if (whatAreWeTesting.equals(Groups)) {
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
        if (whatAreWeTesting.equals(BothDefault)) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getUserInfo(rpHttpsBase, userinfoResponseFormat + TestConfigMaps.AllDefault));
        }

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, app, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    /**
     * Common test method that runs the standard end to end test, but also checks that the subject in the access_token/id_token is not set (because we used a unique callerNameClaim
     * that we won't
     * get from the OP. The test app will be accessed using the sub provided from userinfo.
     *
     * @param appRoot
     * @param app
     * @return
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
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJwtUserInfo.class)
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
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJwtUserInfo.class)
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
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJwtUserInfo.class)
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
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJwtUserInfo.class)
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
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJwtUserInfo.class)
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

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNotJwtUserInfoAndJwtToken.class)
    public void ConfigurationUserInfoTests_invalidUserinfoEndpoint() throws Exception {

        Page response = runGoodEndToEndTest("UserinfoTestServlet10", app);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.SRVE0190E_FILE_NOT_FOUND, "Did not receive a file not found error message stating that the userinfo endpoint is not found."));

        validationUtils.validateResult(response, expectations);

    }

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNotJwtUserInfoAndJwtToken.class)
    public void ConfigurationUserInfoTests_emptyUserinfoEndpoint() throws Exception {

        runGoodEndToEndTest("UserinfoTestServlet11", app);
    }

}
