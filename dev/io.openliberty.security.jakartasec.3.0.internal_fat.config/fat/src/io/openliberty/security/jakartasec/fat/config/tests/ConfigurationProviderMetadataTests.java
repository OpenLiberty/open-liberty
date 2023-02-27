/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.config.tests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests various values set in the @providerMetadata annotation
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ConfigurationProviderMetadataTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationProviderMetadataTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.providerMetadata.jwt")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    protected final String TestApp = "ProviderMetadata";

    public interface ProviderMetadataAttribute {
    }

    public static enum ProviderAttribute implements ProviderMetadataAttribute {
        AUTHORIZE, TOKEN, END_SESSION, USERINFO, JWKSURI, ISSUER, SUBJECTTYPESUPPORTED, RESPONSETYPESUPPORTED
    };

    public interface State {
    }

    public static enum EndpointState implements State {
        TEST, DEFAULT, BAD, EMPTY
    };

    public interface AppOutputLocation {
    }

    public static enum OutputLocation implements AppOutputLocation {
        RESPONSE, MESSAGES
    };

    protected static final String client_secret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeat(Constants.JWT_TOKEN_FORMAT);
//    public static RepeatTests repeat = createTokenTypeRepeats();

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_providerMetadata.xml", waitForMsgs);
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
        swh.defaultDropinApp(rpServer, "Endpoints.war", "endpoints.*", "oidc.client.base.utils");

        // deploy the apps that will be updated at runtime (now) (such as deploying the same app runtime with different names and embedded configs)
        swh.deployConfigurableTestApps(rpServer, "testAuthorizationEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("testAuthorizationEndpoint", "OP1", ProviderAttribute.AUTHORIZE, EndpointState.TEST),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "defaultAuthorizationEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("defaultAuthorizationEndpoint", "OP1", ProviderAttribute.AUTHORIZE, EndpointState.DEFAULT),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "badAuthorizationEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("badAuthorizationEndpoint", "OP1", ProviderAttribute.AUTHORIZE, EndpointState.BAD),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptyAuthorizationEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("emptyAuthorizationEndpoint", "OP1", ProviderAttribute.AUTHORIZE, EndpointState.EMPTY),
                                       "providerMetadata.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "testTokenEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("testTokenEndpoint", "OP1", ProviderAttribute.TOKEN, EndpointState.TEST),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "defaultTokenEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("defaultTokenEndpoint", "OP1", ProviderAttribute.TOKEN, EndpointState.DEFAULT),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "badTokenEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("badTokenEndpoint", "OP1", ProviderAttribute.TOKEN, EndpointState.BAD),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptyTokenEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("emptyTokenEndpoint", "OP1", ProviderAttribute.TOKEN, EndpointState.EMPTY),
                                       "providerMetadata.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "testUserinfoEndpoint.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("testUserinfoEndpoint", "OP1", ProviderAttribute.USERINFO, EndpointState.TEST),
                                       "providerMetadata.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "testJwksURI.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("testJwksURI", "OP1", ProviderAttribute.JWKSURI, EndpointState.TEST),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "defaultJwksURI.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("defaultJwksURI", "OP1", ProviderAttribute.JWKSURI, EndpointState.DEFAULT),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptyJwksURI.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("emptyJwksURI", "OP1", ProviderAttribute.JWKSURI, EndpointState.EMPTY),
                                       "providerMetadata.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "validDefaultIssuer.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("validDefaultIssuer", "OP1", ProviderAttribute.ISSUER, EndpointState.DEFAULT),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "badIssuer.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("badIssuer", "OP1", ProviderAttribute.ISSUER, EndpointState.BAD),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptyIssuer.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("emptyIssuer", "OP1", ProviderAttribute.ISSUER, EndpointState.EMPTY),
                                       "providerMetadata.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "validDefaultSubjectTypeSupported.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("validDefaultSubjectTypeSupported", "OP1", ProviderAttribute.SUBJECTTYPESUPPORTED, EndpointState.DEFAULT),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "badSubjectTypeSupported.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("badSubjectTypeSupported", "OP1", ProviderAttribute.SUBJECTTYPESUPPORTED, EndpointState.BAD),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptySubjectTypeSupported.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("emptySubjectTypeSupported", "OP1", ProviderAttribute.SUBJECTTYPESUPPORTED, EndpointState.EMPTY),
                                       "providerMetadata.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "validDefaultResponseTypeSupported.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("validDefaultResponseTypeSupported", "OP1", ProviderAttribute.RESPONSETYPESUPPORTED, EndpointState.DEFAULT),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "badResponseTypeSupported.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("badResponseTypeSupported", "OP1", ProviderAttribute.RESPONSETYPESUPPORTED, EndpointState.BAD),
                                       "providerMetadata.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptyResponseTypeSupported.war", "ProviderMetadata.war",
                                       buildUpdatedConfigMap("emptyResponseTypeSupported", "OP1", ProviderAttribute.RESPONSETYPESUPPORTED, EndpointState.EMPTY),
                                       "providerMetadata.servlets", "oidc.client.base.*");

    }

    /**
     * Build a map of the attributes to include in the openIdConfig.properties file
     *
     * @param app - the app that we're building the attributes for - some values may need to include that app info
     * @param provider - the provider to include in the values in openIdConfig.properties
     * @param typeTested - the attribute that this app will be used to test (defines which attribute(s) to set
     * @param state - the state of the value to use (good/bad/default/empty)
     * @return - the map of attributes to include in the openIdConfig.properties file
     * @throws Exception
     */
    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, ProviderAttribute typeTested, EndpointState state) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);

        switch (typeTested) {
            case AUTHORIZE:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, addAuthorizationEndpointToMap(state, provider));
                break;
            case TOKEN:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, addTokenEndpointToMap(state, provider));
                break;
            case USERINFO:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, addUserinfoEndpointToMap(state, provider));
                break;
            case END_SESSION:
                throw new Exception("No support currently - end_session case shouldn't be called.");
            case JWKSURI:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, addJwksURIToMap(state, provider));
                break;
            case ISSUER:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, addIssuerToMap(state, provider));
                break;
            case SUBJECTTYPESUPPORTED:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, addSubjectTypeSupportedToMap(state));
                break;
            case RESPONSETYPESUPPORTED:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, addResponseTypeSupportedToMap(state));
                break;
        }

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, app, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    /**
     * Build the appropriate authorization endpoint value
     *
     * @param state - the state of the value to use (good/bad/default/empty)
     * @param provider - the provider to include in the values in openIdConfig.properties
     * @return - returns the map containing the authorization endpoint value
     * @throws Exception
     */
    public static Map<String, Object> addAuthorizationEndpointToMap(EndpointState state, String provider) throws Exception {

        switch (state) {
            case TEST:
                return TestConfigMaps.getTestAuthorizationEndpoint(rpHttpsBase);
            case DEFAULT:
                return TestConfigMaps.getDefaultAuthorizationEndpoint(opHttpsBase, provider);
            case BAD:
                return TestConfigMaps.getBadAuthorizationEndpoint(rpHttpsBase);
            case EMPTY:
                return TestConfigMaps.getEmptyAuthorizationEndpoint();
        }

        return null;
    }

    /**
     * Build the appropriate token endpoint value
     *
     * @param state - the state of the value to use (good/bad/default/empty)
     * @param provider - the provider to include in the values in openIdConfig.properties
     * @return - returns the map containing the token endpoint value
     * @throws Exception
     */
    public static Map<String, Object> addTokenEndpointToMap(EndpointState state, String provider) throws Exception {

        switch (state) {
            case TEST:
                return TestConfigMaps.getTestTokenEndpoint(rpHttpsBase);
            case DEFAULT:
                return TestConfigMaps.getDefaultTokenEndpoint(opHttpsBase, provider);
            case BAD:
                return TestConfigMaps.getBadTokenEndpoint(rpHttpsBase);
            case EMPTY:
                return TestConfigMaps.getEmptyTokenEndpoint();
        }

        return null;
    }

    /**
     * Build the appropriate userinfo endpoint value
     *
     * @param state - the state of the value to use (good/bad/default/empty)
     * @param provider - the provider to include in the values in openIdConfig.properties
     * @return - returns the map containing the userinfo endpoint value
     * @throws Exception
     */
    public static Map<String, Object> addUserinfoEndpointToMap(EndpointState state, String provider) throws Exception {

        switch (state) {
            case TEST:
                return TestConfigMaps.getTestUserinfoEndpoint(rpHttpsBase);
            case DEFAULT:
                return TestConfigMaps.getDefaultAuthorizationEndpoint(opHttpsBase, provider);
            case BAD:
                return TestConfigMaps.getBadAuthorizationEndpoint(rpHttpsBase);
            case EMPTY:
                return TestConfigMaps.getEmptyAuthorizationEndpoint();
        }

        return null;
    }

    /**
     * Build the appropriate jwksURI value
     *
     * @param state - the state of the value to use (good/bad/default/empty)
     * @param provider - the provider to include in the values in openIdConfig.properties
     * @return - returns the map containing the jwksURI value
     * @throws Exception
     */
    public static Map<String, Object> addJwksURIToMap(EndpointState state, String provider) throws Exception {

        switch (state) {
            case TEST:
                return TestConfigMaps.getTestJwksURI(rpHttpsBase);
            case DEFAULT:
                return TestConfigMaps.getDefaultJwksURI(opHttpsBase, provider);
            case BAD:
                throw new Exception("Not used at this time - test is covered in the Signing test class.");
            case EMPTY:
                return TestConfigMaps.getEmptyJwksURI();
        }

        return null;
    }

    /**
     * Build the appropriate issuer value
     *
     * @param state - the state of the value to use (good/bad/default/empty)
     * @param provider - the provider to include in the values in openIdConfig.properties
     * @return - returns the map containing the issuer value
     * @throws Exception
     */
    public static Map<String, Object> addIssuerToMap(EndpointState state, String provider) throws Exception {

        switch (state) {
            case TEST:
                throw new Exception("Not used at this time - test is covered in the Signing test class.");
            case DEFAULT:
                return TestConfigMaps.getDefaultIssuer(opHttpsBase, provider);
            case BAD:
                return TestConfigMaps.getBadIssuer(rpHttpsBase);
            case EMPTY:
                return TestConfigMaps.getEmptyIssuer();
        }

        return null;
    }

    public static Map<String, Object> addSubjectTypeSupportedToMap(EndpointState state) throws Exception {

        switch (state) {
            case TEST:
                throw new Exception("Not used at this time - test is covered in the Signing test class.");
            case DEFAULT:
                return TestConfigMaps.getDefaultSubjectTypeSupported();
            case BAD:
                return TestConfigMaps.getBadSubjectTypeSupported();
            case EMPTY:
                return TestConfigMaps.getEmptySubjectTypeSupported();
        }

        return null;
    }

    public static Map<String, Object> addResponseTypeSupportedToMap(EndpointState state) throws Exception {

        switch (state) {
            case TEST:
                throw new Exception("Not used at this time - test is covered in the Signing test class.");
            case DEFAULT:
                return TestConfigMaps.getDefaultResponseTypeSupported();
            case BAD:
                return TestConfigMaps.getBadResponseTypeSupported();
            case EMPTY:
                return TestConfigMaps.getEmptyResponseTypeSupported();
        }

        return null;
    }

    /**
     * Build expectations for landing on a test version of the OP endpoints
     *
     * @param expectations - existing expectations to add to
     * @param location - location where values should be searched - values will be in the response, or will be found in the messages log
     * @param endpoint - the endpoint being tested (should show up in values we're looking for)
     * @param method - Should request to endpoint be GET or POST
     * @param justParms - list of parms to create expectations for - parms without values (the values would be something generated at runtime by the servers and we can't really
     *            validate those)
     * @param parmAndValues - a list of parms and values to validate - these are values that we can know ahead of time
     * @return
     * @throws Exception
     */
    public Expectations getTestEndpointAppExpectations(Expectations expectations, OutputLocation location, String endpoint, String method, String[] justParms,
                                                       Map<String, String> parmAndValues) throws Exception {

        expectations = addAppropriateExpectation(expectations, location, endpoint + " method: " + method,
                                                 "Did not land on the " + endpoint + " servlet being invoked by the " + method + " method.");

        if (justParms == null && parmAndValues == null) {
            expectations = addAppropriateExpectation(expectations, location, endpoint + " - No Parms were passed", "Unknown parms were passed to the " + endpoint);
        } else {
            if (justParms != null) {
                for (String parm : justParms) {
                    expectations = addAppropriateExpectation(expectations, location, endpoint + " - parmKey: " + parm,
                                                             "Parm " + parm + " was not found in the app output/response");
                }
            }
            if (parmAndValues != null) {
                for (Map.Entry<String, String> parm : parmAndValues.entrySet()) {
                    expectations = addAppropriateExpectation(expectations, location, endpoint + " - parmKey: " + parm.getKey() + " parmValue: " + parm.getValue(),
                                                             endpoint + " - parmKey: " + parm.getKey() + " parmValue: " + parm.getValue() + " was not found in the app output/response");
                }
            }
        }

        return expectations;
    }

    /**
     * Create an individual expectation to look for values in either the full response, or in the servers messages.log
     *
     * @param expectations - existing expectations to add to
     * @param location - Where the value should be found - used to build the appropriate expectation
     * @param searchFor - what should be searched for
     * @param msg- the failure message to issue if the serverFor is not found
     * @return - updated expecations
     * @throws Exception
     */
    public Expectations addAppropriateExpectation(Expectations expectations, OutputLocation location, String searchFor, String msg) throws Exception {

        if (location == OutputLocation.RESPONSE) {
            expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, searchFor, msg));
        } else {
            expectations.addExpectation(new ServerMessageExpectation(rpServer, searchFor, msg));
        }
        return expectations;
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * Test with with a "test" authorization endpoint. This endpoint will simply record the parms that are passed to it and return.
     * This means that we won't get to the login page - we'll receive the output from the test authorize app and will verify that we
     * received the proper parms
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_AuthorizationEndpoint_validateParms() throws Exception {

        String appRoot = "testAuthorizationEndpoint";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + TestApp;

        String[] parmOnlyToCheck = new String[] { "state", "nonce" };
        Map<String, String> parmAndValuesToCheck = new HashMap<String, String>();
        parmAndValuesToCheck.put("redirect_uri", rpHttpsBase + "/testAuthorizationEndpoint/Callback");
        parmAndValuesToCheck.put("client_id", rspValues.getClientId());
        parmAndValuesToCheck.put("display", "page");
        parmAndValuesToCheck.put("response_type", "code");
        parmAndValuesToCheck.put("scope", "openid email profile");

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations = getTestEndpointAppExpectations(expectations, OutputLocation.RESPONSE, "AuthorizationEndpointServlet", Constants.GETMETHOD, parmOnlyToCheck,
                                                      parmAndValuesToCheck);

        invokeApp(webClient, url, expectations);

    }

    /**
     * The value specified for the authorization endpoint is the "default" - it is the value that discovery would return.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_DefaultAuthorizationEndpoint() throws Exception {

        runGoodEndToEndTest("defaultAuthorizationEndpoint", TestApp);
    }

    /**
     * The value specified for the authorization endpoint is "" - The runtime will use the value returned from discovery.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_EmptyAuthorizationEndpoint() throws Exception {

        runGoodEndToEndTest("emptyAuthorizationEndpoint", TestApp);
    }

    /**
     * The values specified for the authorization endpoint is some url that doesn't exist - based on the way that authroization is invoked, the caller will get a 404 and file not
     * found in the response
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_BadAuthorizationEndpoint() throws Exception {

        String appRoot = "badAuthorizationEndpoint";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + TestApp;
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.NOT_FOUND_STATUS));
        invokeApp(webClient, url, expectations);
        rpServer.addIgnoredErrors(Arrays.asList(MessageConstants.SRVE0190E_FILE_NOT_FOUND));
    }

    /**
     * Test with with a "test" token endpoint. This endpoint will simply record the parms that are passed to it and return.
     * This means that we won't get to the application since a token will not be generated - we'll receive a 401 status code and
     * can find the ouptut from the test token endpoint in the RP server log. This test will validate the parms that were passed in.
     * Expect exceptions since the token endpoint is NOT doing what the runtime expects.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.io.IOException", "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void ConfigurationProviderMetadataTests_TokenEndpoint_validateParms() throws Exception {

        String appRoot = "testTokenEndpoint";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + TestApp;

        Page response = invokeAppReturnLoginPage(webClient, url);

        String[] parmOnlyToCheck = new String[] { "code" };
        Map<String, String> parmAndValuesToCheck = new HashMap<String, String>();
        parmAndValuesToCheck.put("redirect_uri", rpHttpsBase + "/testTokenEndpoint/Callback");
        parmAndValuesToCheck.put("client_secret", client_secret);
        parmAndValuesToCheck.put("grant_type", "authorization_code");
        parmAndValuesToCheck.put("client_id", rspValues.getClientId());

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));
        expectations = getTestEndpointAppExpectations(expectations, OutputLocation.MESSAGES, "TokenEndpointServlet", Constants.POSTMETHOD, parmOnlyToCheck, parmAndValuesToCheck);
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2416E_FAILED_TO_REACH_ENDPOINT, "Did not find an error about sending a request to the token endpoint."));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        // confirm output from the token endpoint was accessed
        validationUtils.validateResult(response, expectations);

    }

    /**
     * The value specified for the token endpoint is the "default" - it is the value that discovery would return.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_DefaultTokenEndpoint() throws Exception {

        runGoodEndToEndTest("defaultTokenEndpoint", TestApp);
    }

    /**
     * The value specified for the token endpoint is "" - The runtime will use the value returned from discovery.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_EmptyTokenEndpoint() throws Exception {

        runGoodEndToEndTest("emptyTokenEndpoint", TestApp);
    }

    /**
     * The values specified for the token endpoint is some url that doesn't exist - based on the way that the token endpoint is invoked, the caller will get a 401 status code and
     * the messages log will contain a message stating the the endpoint could not be reached.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.io.IOException", "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void ConfigurationProviderMetadataTests_BadTokenEndpoint() throws Exception {

        String appRoot = "badTokenEndpoint";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + TestApp;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2416E_FAILED_TO_REACH_ENDPOINT, "Did not find an error about sending a request to the token endpoint."));
        rpServer.addIgnoredErrors(Arrays.asList(MessageConstants.SRVE0190E_FILE_NOT_FOUND));

    }

    /**
     * Test with with a "test" userinfo endpoint. This endpoint will simply record the parms that are passed to it and return.
     * Since the response from userinfo is not needed (the id_token/access_token will contain all values that are needed), we will
     * be able to get to the app. We'll find the userinfo app output in the messages log and we'll validate that info from there.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_UserinfoEndpoint_validateParms() throws Exception {

        Page response = runGoodEndToEndTest("testUserinfoEndpoint", TestApp);

        String[] parmOnlyToCheck = null;
        Map<String, String> parmAndValuesToCheck = null;

        Expectations expectations = new Expectations();
        expectations = getTestEndpointAppExpectations(expectations, OutputLocation.MESSAGES, "UserinfoEndpointServlet", Constants.GETMETHOD, parmOnlyToCheck, parmAndValuesToCheck);

        // confirm output from the userinfo endpoint was accessed
        validationUtils.validateResult(response, expectations);

    }

    /**
     * NOTE:
     * Tests in the ConfigurationUserInfoTests class will test invalid userinfo endpoints. This includes non existant app as well as userinfo endpoints
     * that return valid/invalid results. This is done when the configuration dictates that the userinfo response is and and is not required.
     */

    /**
     * NOTE:
     * tests for the end_session endpoint are in the io.openliberty.security.jakartasec.3.0.internal_fat.logout project
     */

    /**
     * Test with with a "test" jwksURI. This endpoint will simply record the parms that are passed to it and return.
     * This means that we won't get to the application since a token can not be validated - we'll receive a 401 status code and
     * can find the ouptut from the test jwksURI in the RP server log. This test will validate the parms that were passed in.
     * Expect exceptions since the jwksURI is NOT doing what the runtime expects.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_JwksURI_validateParms() throws Exception {

        String appRoot = "testJwksURI";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + TestApp;

        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));
        expectations = getTestEndpointAppExpectations(expectations, OutputLocation.MESSAGES, "JwksURIServlet", Constants.GETMETHOD, null, null);
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2504E_CREDENTIAL_VALIDATION_ERROR, "Did not not receive an error in the messages log for a failure to validate a user credential."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2521E_SIGNING_KEY_MISSING, "Did not find an error in the messages log about a missing signing key."));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        // confirm output from the token endpoint was accessed
        validationUtils.validateResult(response, expectations);

    }

    /**
     * The value specified for the jwksURI is the "default" - it is the value that discovery would return.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_DefaultJwksURI() throws Exception {

        runGoodEndToEndTest("defaultJwksURI", TestApp);

    }

    /**
     * The value specified for the jwksURI is "" - The runtime will use the value returned from discovery.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_EmptyJwksURI() throws Exception {

        runGoodEndToEndTest("emptyJwksURI", TestApp);

    }

    /**
     * NOTE: a bad jwksURI test can be found in the ConfigurationUserInfoTests class
     */

    /**
     * The value specified for the issuer is the "default" - it is the value that discovery would return.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_Valid_DefaultIssuer() throws Exception {

        runGoodEndToEndTest("validDefaultIssuer", TestApp);

    }

    /**
     * Test with with an invalid issuer value - this value is something that we know the OP will NOT return. Test validates that we get a 401 status and are not granted access to
     * the test app. We'll also check the server log for the proper error messages stating that the issuer is not valid in the goken suppled by the OP.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_InvalidIssuer() throws Exception {

        String appRoot = "badIssuer";

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + TestApp;

        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, Constants.UNAUTHORIZED_MESSAGE, "Response message does not contain "
                                                                                                                              + Constants.UNAUTHORIZED_MESSAGE));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2504E_CREDENTIAL_VALIDATION_ERROR, "Did not receive a messages stating that there was a problem validating the user credential"));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2415E_TOKEN_VALIDATION_EXCEPTION, "Did not receive a message stating that there was an problem validating the token"));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2424E_CLAIM_MISMATCH, "Did not receive a message stating that the expected issuer ["
                                                                                                                        + rpHttpsBase
                                                                                                                        + "/someProvider] was not found in the token."));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        // confirm output from the token endpoint was accessed
        validationUtils.validateResult(response, expectations);

    }

    /**
     * The value specified for the issuer is "" - The runtime will use the value returned from discovery.
     * Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_EmptyIssuer() throws Exception {

        runGoodEndToEndTest("emptyIssuer", TestApp);

    }

    /**
     * NOTE:
     * idTokenSigningAlgorithmsSupported is tested in the ConfigurationSigningTests and ConfigurationUserInfoTests
     */

    /**
     * NOTE:
     * jwksUri with a bad value is tested in the ConfigurationSigningTests
     */

    /**
     * NOTE: There isn't too much we can tests with regard to subjectTypeSupported and responseTypeSupported since we're not
     * supporting client registration. Including a few simple tests with various values to show that we don't
     * fail.
     */

    /**
     * Set the default value for subjectTypeSupported of "public". Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_Valid_DefaultSubjectTypeSupported() throws Exception {
        runGoodEndToEndTest("validDefaultSubjectTypeSupported", TestApp);

    }

    /**
     * Set an invalid value for subjectTypeSupported of "someValue". Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_Valid_InvalidSubjectTypeSupported() throws Exception {
        runGoodEndToEndTest("badSubjectTypeSupported", TestApp);

    }

    /**
     * Set an empty value for subjectTypeSupported of "". Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_Valid_EmptySubjectTypeSupported() throws Exception {
        runGoodEndToEndTest("emptySubjectTypeSupported", TestApp);

    }

    /**
     * Set the default value for responseTypeSupported of "code,id_token,token id_token". Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_Valid_DefaultResponseTypeSupported() throws Exception {
        runGoodEndToEndTest("validDefaultResponseTypeSupported", TestApp);

    }

    /**
     * Set an invalid value for responseTypeSupported of "someValue". Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_Valid_InvalidResponseTypeSupported() throws Exception {
        runGoodEndToEndTest("badResponseTypeSupported", TestApp);

    }

    /**
     * Set an empty value for responseTypeSupported of "". Make sure that we have a normal good end to end run
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationProviderMetadataTests_Valid_EmptyResponseTypeSupported() throws Exception {
        runGoodEndToEndTest("emptyResponseTypeSupported", TestApp);

    }

}
