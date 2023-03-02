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

import java.util.List;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests @OpenIdAuthenticationMechanismDefinition extraParameters and extraParametersExpression.
 *
 * This class contains tests to ensure that extra parameters specified in extraParameters
 * or resolved from extraParametersExpression are added to the authentication endpoint request correctly.
 * This includes making making sure that multiple parameters are added correctly,
 * special characters are encoded in the keys/values,
 * extraParametersExpression takes precedence over extraParameters,
 * and that the parameters are in the format of key=value.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ConfigurationExtraParametersTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationExtraParametersTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.extraParameters")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats();

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_extraParameters.xml", waitForMsgs);
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

        swh.defaultDropinApp(rpServer, "ExtraParametersOneParam.war", "oidc.client.extraParametersOneParam.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "ExtraParametersTwoParams.war", "oidc.client.extraParametersTwoParams.servlets", "oidc.client.base.*");

        // these two need their own apps, since the leading spaces in the keys were being stripped
        // when reading the key/value pairs from a file using java.util.Properties
        swh.defaultDropinApp(rpServer, "ExtraParametersLeadingSpaceInKey.war", "oidc.client.extraParametersLeadingSpaceInKey.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "ExtraParametersSpaceAsKey.war", "oidc.client.extraParametersSpaceAsKey.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "ExtraParametersOneParamELTwoDifferentParams.war", "ExtraParametersOneParamWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersOneParamELTwoDifferentParams", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersTwoParams()),
                                       "oidc.client.extraParametersOneParamWithEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELDuplicateKeys.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELDuplicateKeys", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersDuplicateKeys()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELSpaceInKey.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELSpaceInKey", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsSpaceInKey()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELTrailingSpaceInKey.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELTrailingSpaceInKey", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsTrailingSpaceInKey()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELSpaceInValue.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELSpaceInValue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsSpaceInValue()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELLeadingSpaceInValue.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELLeadingSpaceInValue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsLeadingSpaceInValue()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELTrailingSpaceInValue.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELTrailingSpaceInValue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsTrailingSpaceInValue()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELEmptyKey.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELEmptyKey", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsEmptyKey()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELEmptyValue.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELEmptyValue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsEmptyValue()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELSpaceAsValue.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELSpaceAsValue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsSpaceAsValue()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELTwoEqualsSigns.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELTwoEqualsSigns", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsTwoEqualsSigns()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELMissingEqualsSign.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersMissingEqualsSign", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersMissingEqualsSign()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELEmpty.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELEmpty", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersIsEmpty()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELEqualsSign.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELEqualsSign", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersIsAnEqualsSign()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELSpecialCharacterInKey.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELSpecialCharacterInKey", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsSpecialCharacterInKey()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ExtraParametersELSpecialCharacterInValue.war", "ExtraParametersUsingEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ExtraParametersELSpecialCharacterInValue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getExtraParametersContainsSpecialCharacterInValue()),
                                       "oidc.client.extraParametersUsingEL.servlets", "oidc.client.base.*");

    }

    private void runGoodEndToEndWithExtraParametersCheck(String appRoot, String app, String expectedExtraParameters) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        // disable redirect, so we are able to see the 302 responses
        // (otherwise, can't see redirect response from auth endpoint)
        webClient.getOptions().setRedirectEnabled(false);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        // follow redirect from login form to auth endpoint
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        String authEndpointRegex = "https:\\/\\/localhost:" + opServer.getBvtSecurePort() + "\\/oidc\\/endpoint\\/OP1\\/authorize";
        String defaultParametersRegex = "\\?scope=[^&]+&response_type=[^&]+&client_id=[^&]+&redirect_uri=[^&]+&state=[^&]+&nonce=[^&]+&display=[^&]+";
        String authEndpointWithParametersRegex = authEndpointRegex + defaultParametersRegex;
        if (expectedExtraParameters != null && !expectedExtraParameters.isEmpty()) {
            authEndpointWithParametersRegex += "&" + Pattern.quote(expectedExtraParameters) + "$";
        }

        // validate 302 response and that the auth endpoint is correct with expected query params
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.REDIRECT_STATUS));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, Constants.FOUND_MSG, "Did not receive the Found message."));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_MATCHES, authEndpointWithParametersRegex, "The auth endpoint and its query params were not as expected."));
        validationUtils.validateResult(response, expectations);

        // follow redirect from auth endpoint to redirect uri
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // follow redirect from callback servlet to original request
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validate that we were able to get to the original request and that we have an openid context
        validationUtils.validateResult(response, getGeneralAppExpecations(app));

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     *
     * Test with extraParameters={"key1=value1"}
     * It should append key1=value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParameters_oneParam() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersOneParam", "ExtraParametersOneParamServlet", "key1=value1");

    }

    /**
     *
     * Test with extraParameters={"key1=value1", "key2=value2"}
     * It should append key1=value1 and key2=value2 as a query params to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParameters_twoParams() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersTwoParams", "ExtraParametersTwoParamsServlet", "key1=value1&key2=value2");

    }

    /**
     *
     * Test with extraParameters={"key3=value3"} and extraParametersExpression which resolves to {"key1=value1", "key2=value2"}
     * The value resolved by extraParametersExpression should take precedence
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParameters_oneParam_extraParametersExpression_twoDifferentParams() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersOneParamELTwoDifferentParams", "ExtraParametersOneParamWithELServlet", "key1=value1&key2=value2");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1=value1", "key1=value2"}
     * It should append key1=value1 and key1=value2 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_duplicateKeys() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELDuplicateKeys", "ExtraParametersUsingELServlet", "key1=value1&key1=value2");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key 1=value1"}
     * It should URL encode the space and append key+1=value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_spaceInKey() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELSpaceInKey", "ExtraParametersUsingELServlet", "key+1=value1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {" key1=value1"}
     * It should URL encode the space and append +key1=value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_leadingSpaceInKey() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersLeadingSpaceInKey", "ExtraParametersLeadingSpaceInKeyServlet", "+key1=value1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1 =value1"}
     * It should URL encode the space and append key1+=value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_trailingSpaceInKey() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELTrailingSpaceInKey", "ExtraParametersUsingELServlet", "key1+=value1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1=value 1"}
     * It should URL encode the space and append key1=value+1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_spaceInValue() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELSpaceInValue", "ExtraParametersUsingELServlet", "key1=value+1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1= value1"}
     * It should URL encode the space and append key1=+value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_leadingSpaceInValue() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELLeadingSpaceInValue", "ExtraParametersUsingELServlet", "key1=+value1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1=value1 "}
     * It should URL encode the space and append key1=value1+ as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_trailingSpaceInValue() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELTrailingSpaceInValue", "ExtraParametersUsingELServlet", "key1=value1+");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"=value1"}
     * It should append =value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_emptyKey() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELEmptyKey", "ExtraParametersUsingELServlet", "=value1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {" =value1"}
     * It should URL encode the space and append +=value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_spaceAsKey() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersSpaceAsKey", "ExtraParametersSpaceAsKeyServlet", "+=value1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1="}
     * It should append key1= as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_emptyValue() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELEmptyValue", "ExtraParametersUsingELServlet", "key1=");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1= "}
     * It should URL encode the space and append key1=+ as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_spaceAsValue() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELSpaceAsValue", "ExtraParametersUsingELServlet", "key1=+");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1=value1=value2"}
     * It should URL encode the second equals sign and append key1=value1%3Dvalue2 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_twoEqualsSigns() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELTwoEqualsSigns", "ExtraParametersUsingELServlet", "key1=value1%3Dvalue2");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1value1"}
     * It should not append key1value1 as a query param to the authentication endpoint request, since it is malformed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_missingEqualsSign() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELMissingEqualsSign", "ExtraParametersUsingELServlet", "");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {""}
     * It should not append anything to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_empty() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELEmpty", "ExtraParametersUsingELServlet", "");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"="}
     * It should append "=" as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_equalsSign() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELEqualsSign", "ExtraParametersUsingELServlet", "=");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key&1=value1"}
     * It should URL encode the & in the key and append key%261=value1 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_specialCharacterInKey() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELSpecialCharacterInKey", "ExtraParametersUsingELServlet", "key%261=value1");

    }

    /**
     *
     * Test with extraParametersExpression which resolves to {"key1=value&1"}
     * It should URL encode the & in the value and append key1=value%261 as a query param to the authentication endpoint request
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationExtraParametersTests_extraParametersExpression_specialCharacterInValue() throws Exception {

        runGoodEndToEndWithExtraParametersCheck("ExtraParametersELSpecialCharacterInValue", "ExtraParametersUsingELServlet", "key1=value%261");

    }

}