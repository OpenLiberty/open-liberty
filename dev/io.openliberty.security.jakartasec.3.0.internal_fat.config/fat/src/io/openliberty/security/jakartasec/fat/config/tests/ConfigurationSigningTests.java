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
package io.openliberty.security.jakartasec.fat.config.tests;

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 *
 * Tests  @OpenIdProviderMetadata idTokenSigningAlgorithmsSupported in @OpenIdAuthenticationMechanismDefinition and jwksConnectTimeout/jwksConnectTimeoutExpression, jwksReadTimeout/jwksReadTimeoutExpression in @OpenIdProviderMetadata.
 * Tests will show that when we use the same signature algorithm in the OP and the annotation that we'll have access to the app.  When there is a mismatch, we won't have access and will have messages recording the mismatch in the server logs.
 * Tests will also show that the jwks timeouts will be used and when we we have timeouts that we will not be allowed access to the app and that there will be appropriate error messages in the server logs.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ConfigurationSigningTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationSigningTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;

    @Server("jakartasec-3.0_fat.config.rp.signing.jwt")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.config.rp.signing.opaque")
    public static LibertyServer rpOpaqueServer;
    public static LibertyServer rpServer;
    protected static ShrinkWrapHelpers swh = null;

    protected static boolean CallerNameClaimDefault = true;
    protected static boolean CallerGroupsClaimDefault = true;
    protected static boolean UserinfoSubValid = true;
    protected static boolean UserinfoGroupsValid = true;

    protected static String Caller = "caller";
    protected static String Groups = "groups";
    protected static String BothDefault = "bothDefault";

    protected static String app = "Signing";

    protected static int ignoreTimeoutValue = 9999;

    protected static String userinfoResponseFormat = Constants.JWT_TOKEN_FORMAT;

    public interface JwksUri {
    }

    public static enum JwksUriType implements JwksUri {
        NULL, UNKNOWN_HOST, TEST_APP
    };

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats();

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // due to the way we create the test apps on the fly, we need a unique rp server instance for each repeat.

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.JWT_TOKEN_FORMAT)) {
            rpServer = rpJwtServer;
        } else {
            rpServer = rpOpaqueServer;
        }
        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_signing.xml", waitForMsgs);
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
        swh.defaultDropinApp(rpServer, "JwksEndpoint.war", "jwks.servlets");

        // deploy the signing test apps
        // OP that annotations point to uses RS256, client allows 1 - 3 signature Algorithms
        deployAnApp("OP1", Constants.SIGALG_RS256);
        deployAnApp("OP1", Constants.SIGALG_HS256);
        deployAnApp("OP1", Constants.SIGALG_NONE);
        deployAnApp("OP1", Constants.SIGALG_HS512);
        deployAnApp("OP1", Constants.SIGALG_RS256, Constants.SIGALG_HS256);
        deployAnApp("OP1", Constants.SIGALG_RS256, Constants.SIGALG_NONE);
        deployAnApp("OP1", Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        deployAnApp("OP1", Constants.SIGALG_RS256, Constants.SIGALG_HS256, Constants.SIGALG_NONE);

        // OP that annotations point to uses HS256, client allows 1 - 3 signature Algorithms
        deployAnApp("OP2", Constants.SIGALG_RS256);
        deployAnApp("OP2", Constants.SIGALG_HS256);
        deployAnApp("OP2", Constants.SIGALG_NONE);
        deployAnApp("OP2", Constants.SIGALG_RS384);
        deployAnApp("OP2", Constants.SIGALG_RS256, Constants.SIGALG_HS256);
        deployAnApp("OP2", Constants.SIGALG_RS256, Constants.SIGALG_NONE);
        deployAnApp("OP2", Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        deployAnApp("OP2", Constants.SIGALG_RS256, Constants.SIGALG_HS256, Constants.SIGALG_NONE);

        // OP that annotations point to uses NONE, client allows 1 - 3 signature Algorithms
        deployAnApp("OP3", Constants.SIGALG_RS256);
        deployAnApp("OP3", Constants.SIGALG_HS256);
        deployAnApp("OP3", Constants.SIGALG_NONE);
        deployAnApp("OP3", Constants.SIGALG_ES256);
        deployAnApp("OP3", Constants.SIGALG_RS256, Constants.SIGALG_HS256);
        deployAnApp("OP3", Constants.SIGALG_RS256, Constants.SIGALG_NONE);
        deployAnApp("OP3", Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        deployAnApp("OP3", Constants.SIGALG_RS256, Constants.SIGALG_HS256, Constants.SIGALG_NONE);

        // Use EL values
        deployAnApp("OPDefaultJWKS", 500, 500, Constants.SIGALG_RS256); // on slow machines, tests using the default read timeout may have issues, but, we have to test it
        deployAnApp("OPInfiniteJWKS", 0, 0, Constants.SIGALG_RS256);
        deployAnApp("OPShortJWKS", 0, 1, JwksUriType.TEST_APP, Constants.SIGALG_RS256);
        deployAnApp("OPShortJWKS", 1, 0, JwksUriType.UNKNOWN_HOST, Constants.SIGALG_RS256);
        deployAnApp("OPNegativeJWKS", 0, -1, Constants.SIGALG_RS256);
        deployAnApp("OPNegativeJWKS", -1, 0, Constants.SIGALG_RS256);

        // uses jwksConnectTimeout
        swh.deployConfigurableTestApps(rpServer, "SigningShortJwksConnectionTimeout.war", "SigningShortJwksConnectionTimeout.war",
                                       buildUpdatedConfigMap("SigningShortJwksConnectionTimeout", "OPShortJWKS", ignoreTimeoutValue, ignoreTimeoutValue, JwksUriType.UNKNOWN_HOST),
                                       "signing.shortConnectionTimeout.servlets", "oidc.client.base.*");
        // uses jwksConnectTimeout and jwksConnectTimeoutExpression
        swh.deployConfigurableTestApps(rpServer, "SigningShortJwksConnectionTimeoutELOverride.war", "SigningShortJwksConnectionTimeoutELOverride.war",
                                       buildUpdatedConfigMap("SigningShortJwksConnectionTimeoutELOverride", "OPShortJWKS", 1, ignoreTimeoutValue, JwksUriType.UNKNOWN_HOST),
                                       "signing.shortConnectionTimeoutELOverride.servlets", "oidc.client.base.*");

        // uses jwksReadTimeout
        swh.deployConfigurableTestApps(rpServer, "SigningShortJwksReadTimeout.war", "SigningShortJwksReadTimeout.war",
                                       buildUpdatedConfigMap("SigningShortJwksReadTimeout", "OPShortJWKS", ignoreTimeoutValue, ignoreTimeoutValue, JwksUriType.TEST_APP),
                                       "signing.shortReadTimeout.servlets", "oidc.client.base.*");
        // uses jwksReadTimeout and jwksReadTimeoutExpression
        swh.deployConfigurableTestApps(rpServer, "SigningShortJwksReadTimeoutELOverride.war", "SigningShortJwksReadTimeoutELOverride.war",
                                       buildUpdatedConfigMap("SigningShortJwksReadTimeoutELOverride", "OPShortJWKS", ignoreTimeoutValue, 1, JwksUriType.TEST_APP),
                                       "signing.shortReadTimeoutELOverride.servlets", "oidc.client.base.*");

//        deployAnApp("OP1", Constants.EMPTY_VALUE); // empty string
        swh.deployConfigurableTestApps(rpServer, "EmptySigning.war", "EmptySigning.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "EmptySigning", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getProviderUri(opHttpsBase, "OP1")),
                                       "signing.empty.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "ComplexSigning1.war", "ComplexSigning1.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ComplexSigning1", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getProviderUri(opHttpsBase, "OP1")),
                                       "signing.complex_1.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ComplexSigning2.war", "ComplexSigning2.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ComplexSigning2", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getProviderUri(opHttpsBase, "OP1")),
                                       "signing.complex_2.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ComplexSigning3.war", "ComplexSigning3.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ComplexSigning3", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getProviderUri(opHttpsBase, "OP1")),
                                       "signing.complex_3.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ComplexSigning4.war", "ComplexSigning4.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ComplexSigning4", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getProviderUri(opHttpsBase, "OP1")),
                                       "signing.complex_4.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ComplexSigning5.war", "ComplexSigning5.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ComplexSigning5", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getProviderUri(opHttpsBase, "OP1")),
                                       "signing.complex_5.servlets", "oidc.client.base.*");

    }

    /**
     * Deploy a test application based on the Signing test app - We'll build the name of the app to create based on the provider, (default) timeouts and signature algorithms
     *
     * @param provider - the provider that the app will use
     * @param clientSigAlgs - the signature algorithms that the app/client will allow
     * @throws Exception
     */
    public static void deployAnApp(String provider, String... clientSigAlgs) throws Exception {

        String appName = buildAppName(provider, ignoreTimeoutValue, ignoreTimeoutValue, clientSigAlgs);
        swh.deployConfigurableTestApps(rpServer, appName + ".war", "Signing.war",
                                       buildUpdatedConfigMap(appName, provider, clientSigAlgs), "signing.servlets", "oidc.client.base.*");

    }

    /**
     * Deploy a test application based on the Signing test app - We'll build the name of the app to create based on the provider, timeouts and signature algorithms
     *
     * @param provider - the provider that the app will use
     * @param clientSigAlgs - the signature algorithms that the app/client will allow
     * @throws Exception
     */
    public static void deployAnApp(String provider, int jwksConnTimeout, int jwksReadTimeout, String... clientSigAlgs) throws Exception {
        deployAnApp(provider, jwksConnTimeout, jwksReadTimeout, JwksUriType.NULL, clientSigAlgs);
    }

    public static void deployAnApp(String provider, int jwksConnTimeout, int jwksReadTimeout, JwksUriType jwksUriType, String... clientSigAlgs) throws Exception {

        String appName = buildAppName(provider, jwksConnTimeout, jwksReadTimeout, clientSigAlgs);
        swh.deployConfigurableTestApps(rpServer, appName + ".war", "Signing.war",
                                       buildUpdatedConfigMap(appName, provider, jwksConnTimeout, jwksReadTimeout, jwksUriType, clientSigAlgs), "signing.servlets",
                                       "oidc.client.base.*");

    }

    /**
     * Build a map of EL var settings to put into the openIdConfig.properties file in the app.
     * We'll set connection timeouts for jwks connection and read as well as the signature alg to use
     *
     * @param app - the app that we're building
     * @param provider - the OP provider that app uses - used to set endpoint urls
     * @param alg - the list of signature algorithms that the annotation in the app will be set to
     * @return - return the map of config el vars to set
     * @throws Exception
     */
    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, String... alg) throws Exception {

        return buildUpdatedConfigMap(app, provider, ignoreTimeoutValue, ignoreTimeoutValue, alg);
    }

    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, int jwksConnTimeout, int jwksReadTimeout, String... alg) throws Exception {

        return buildUpdatedConfigMap(app, provider, jwksConnTimeout, jwksReadTimeout, JwksUriType.NULL, alg);
    }

    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, int jwksConnTimeout, int jwksReadTimeout, JwksUriType jwksUriType,
                                                            String... alg) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);
        testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getIdTokenSigningAlgValuesSupported(alg));
        if (jwksConnTimeout != ignoreTimeoutValue) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getJwksConnectTimeoutExpression(jwksConnTimeout));
        }
        if (jwksReadTimeout != ignoreTimeoutValue) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getJwksReadTimeoutExpression(jwksReadTimeout));
        }
        if (jwksUriType == JwksUriType.UNKNOWN_HOST) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getJwksURI_BadHost(Integer.toString(rpServer.getBvtSecurePort()), provider));
        }
        if (jwksUriType == JwksUriType.TEST_APP) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getJwksURI_TestAppSleeps5Seconds(rpHttpBase));
        }

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, app, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    /**
     * Build the test app name based on the signature alg and ignored jwks timeouts - set the provider value that the test code will use to verify the tokens created
     *
     * @param provider - the provider that we'll be using
     * @param clientSigAlgs - the list of signature algorithms that the app supports
     * @return - the name of the app to use based on the values passed in
     * @throws Exception
     */
    public String buildAppNameAndUpdateIssuer(String provider, String... clientSigAlgs) throws Exception {
        return buildAppNameAndUpdateIssuer(provider, ignoreTimeoutValue, ignoreTimeoutValue, clientSigAlgs);
    }

    /**
     * Build the test app name based on the signature alg and the jwks timeout values passed in - set the provider value that the test code will use to verify the tokens created
     *
     * @param provider - the provider that we'll be using
     * @param jwksConnTimeout - the jwks connection timeout value
     * @param jwksReadTimeout - the jwks Read timeout value
     * @param clientSigAlgs - the list of signature algorithms that the app supports
     * @return - the name of the app to use based on the values passed in
     * @throws Exception
     */
    public String buildAppNameAndUpdateIssuer(String provider, int jwksConnTimeout, int jwksReadTimeout, String... clientSigAlgs) throws Exception {

        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);

        return buildAppName(provider, jwksConnTimeout, jwksReadTimeout, clientSigAlgs);
    }

    /**
     * Build the test app name based on the signature alg and the jwks timeout values passed in
     *
     * @param provider - the provider that we'll be using
     * @param jwksConnTimeout - the jwks connection timeout value
     * @param jwksReadTimeout - the jwks Read timeout value
     * @param clientSigAlgs - the list of signature algorithms that the app supports
     * @return - the name of the app to use based on the values passed in
     * @throws Exception
     */
    public static String buildAppName(String provider, int jwksConnTimeout, int jwksReadTimeout, String... clientSigAlgs) throws Exception {

        String providerSigAlg = Constants.SIGALG_RS256;
        if (provider == "OP2") {
            providerSigAlg = Constants.SIGALG_HS256;
        } else {
            if (provider == "OP3") {
                providerSigAlg = Constants.SIGALG_NONE;
            }
        }

        String clientSigAlg = null;
        for (String alg : clientSigAlgs) {
            if (clientSigAlg == null) {
                clientSigAlg = alg;
            } else {
                clientSigAlg = clientSigAlg + alg;
            }
        }

        String appName = "SigningTestServlet_" + providerSigAlg + "_" + clientSigAlg;

        if (jwksConnTimeout != ignoreTimeoutValue) {
            appName = appName + "_jwksConnTimeout" + Integer.toString(jwksConnTimeout);
        }
        if (jwksReadTimeout != ignoreTimeoutValue) {
            appName = appName + "_jwksReadTimeout" + Integer.toString(jwksReadTimeout);
        }

        return appName;
    }

    /**
     * Generic code to run a signature algorithm mismatch type test - when the login page is filled in and submitted, we'll expect a 401 and failure messages in the log
     *
     * @param appRoot - the application root to invoke
     * @param app - the application servlet name to invoke
     * @return - return the final page in case the caller needs to perform additional checks
     * @throws Exception
     */
    public Page runSigningMismatchTest(String appRoot, String app) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        Log.info(thisClass, _testName, "headers: " + rspValues.getHeaders());

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, Constants.UNAUTHORIZED_MESSAGE, "Did not receive the Unauthorized message."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2504E_CREDENTIAL_VALIDATION_ERROR, "Did not receive an error message stating that the signature algorithm was invalid."));

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, expectations);

        return response;

    }

    /**
     * Generic code to run a jwks timeout type test - when the login page is filled in and submitted, we'll expect a 401 and failure messages in the log indicating that there are
     * failures
     *
     * @param appRoot - the application root to invoke
     * @param app - the application servlet name to invoke
     * @param readTimeout - flag indicating that the calling test expects a read timeout - additional messages will be logged
     * @return - return the final page in case the caller needs to perform additional checks
     * @throws Exception
     */
    public Page runSigningShortTimeoutTest(String appRoot, String app, boolean readTimeout) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        Log.info(thisClass, _testName, "headers: " + rspValues.getHeaders());

        String url = rpHttpsBase + "/" + appRoot + "/" + app;
        Page response = invokeAppReturnLoginPage(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.UNAUTHORIZED_STATUS));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, Constants.UNAUTHORIZED_MESSAGE, "Did not receive the Unauthorized message."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2504E_CREDENTIAL_VALIDATION_ERROR, "Did not receive an error message stating that the signature algorithm was invalid."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2415E_TOKEN_VALIDATION_EXCEPTION, "Did not receive an error message stating that token validation failed."));
        if (readTimeout) {
            expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2420E_ID_TOKEN_VERIFY, "Did not receive a message stating that there was an error while getting the key to verify the id_token."));
            expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2422E_FAILED_TO_READ_DATA, "Did not receive an error message stating that the read timed out."));
        }

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, expectations);

        return response;

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to RS256
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_RS256);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to HS256
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsHS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_HS256);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to none
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsNone() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_NONE);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to HS512
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsHS512() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_HS512);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, HS256"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientAllowssRS256HS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_RS256, Constants.SIGALG_HS256);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, none"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientAllowssRS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_RS256, Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "HS256, none"
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientAllowssHS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, HS256, none"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientAllowssRS256HS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP1", Constants.SIGALG_RS256, Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to RS256
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientExpectsRS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_RS256);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to HS256
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientExpectsHS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_HS256);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to none
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientExpectsNone() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_NONE);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to RS384
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientExpectsRS384() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_RS384);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, HS256"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientAllowssRS256HS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_RS256, Constants.SIGALG_HS256);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, none"
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientAllowssRS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_RS256, Constants.SIGALG_NONE);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "HS256, none"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientAllowssHS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies HS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, HS256, none"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithHS256_clientAllowssRS256HS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP2", Constants.SIGALG_RS256, Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to RS256
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientExpectsRS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_RS256);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to HS256
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientExpectsHS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_HS256);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to none
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientExpectsNone() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to ES256
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientExpectsES256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_ES256);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, HS256"
     * Test shows that the token received from the OP is not considered valid (401 status code) and signature mismatch error messages are recorded
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientAllowssRS256HS256() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_RS256, Constants.SIGALG_HS256);
        runSigningMismatchTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, none"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientAllowssRS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_RS256, Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to "HS256, none"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientAllowssHS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies none and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256, HS256, none"
     * Test shows that the token received from the OP can be processed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithNone_clientAllowssRS256HS256None() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OP3", Constants.SIGALG_RS256, Constants.SIGALG_HS256, Constants.SIGALG_NONE);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksConnectTimeout and jwksConnectTimeout values
     * are set to the default value of 500.
     * Test shows that we receive access and id tokens and that they are valid
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_defaultJwksTimeoutValuesSpecified() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OPDefaultJWKS", 500, 500, Constants.SIGALG_RS256);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksConnectTimeout and jwksConnectTimeout values
     * are set to infinite (0/no timeout).
     * Test shows that we receive access and id tokens and that they are valid
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_infiniteJwksTimeoutValues() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OPInfiniteJWKS", 0, 0, Constants.SIGALG_RS256);
        runGoodEndToEndTest(appName, app);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksConnectTimeout is set to a very short value
     * (forcing a timeout).
     * Test shows that we'll get an ssl exception/connection failure and will fail to access the protected app
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.net.ssl.SSLException" })
    @ExpectedFFDC({ "org.apache.http.conn.ConnectTimeoutException" })
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_shortJwksConnectionTimeout_withoutEL() throws Exception {

        runSigningShortTimeoutTest("SigningShortJwksConnectionTimeout", "SigningShortConnectionTimeout", false);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksConnectTimeoutExpression is set to a very
     * short value (forcing a timeout).
     * Test shows that we'll get an ssl exception/connection failure and will fail to access the protected app
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.net.ssl.SSLException" })
    @ExpectedFFDC({ "org.apache.http.conn.ConnectTimeoutException" })
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_shortJwksConnectionTimeout_withEL() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OPShortJWKS", 1, 0, Constants.SIGALG_RS256);
        runSigningShortTimeoutTest(appName, app, false);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksConnectTimeout is set to the default value
     * and jwksConnectTimeoutExpression is set to a very short value (forcing a timeout).
     * Test shows that we'll get an ssl exception/connection failure and will fail to access the protected app (showing that the el value overrides)
     *
     * @throws Exception
     */
    @AllowedFFDC({ "javax.net.ssl.SSLException" })
    @ExpectedFFDC({ "org.apache.http.conn.ConnectTimeoutException" })
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_shortJwksConnectionTimeout_withELOverride() throws Exception {

        runSigningShortTimeoutTest("SigningShortJwksConnectionTimeoutELOverride", "SigningShortConnectionTimeout", false);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksConnectTimeout is set to a negative value .
     * Test shows that we'll be able to access the app, but, we'll see a message about the negative value in the server message log.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_negativeJwksConnectionTimeoutValue() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OPNegativeJWKS", -1, 0, Constants.SIGALG_RS256);
        Page response = runGoodEndToEndTest(appName, app);

        // since the runtime will end up using the default of 500, make sure we see our app set the value to -1
        Expectations extraExpectations = new Expectations();
        extraExpectations.addExpectation(new ServerMessageExpectation(rpServer, "Setting the value for config attribute: jwksConnectTimeoutExpression to: -1", "Did not find a message indicating that the jwksConnectionTimeoutExpression was being set to -1."));
        extraExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2507W_OIDC_MECH_CONFIG_NEGATIVE_INT, "Did not receive an error message stating that the OpenIdAuthenticationMechanismDefinition attribute should not be negative."));

        validationUtils.validateResult(response, extraExpectations);

    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksReadTimeout is set to a very short value
     * (forcing a timeout).
     * Test shows that we'll get a timeout exception failure and will fail to access the protected app
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.net.SocketTimeoutException" })
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_shortJwksReadTimeout_withoutEL() throws Exception {

        runSigningShortTimeoutTest("SigningShortJwksReadTimeout", "SigningShortReadTimeout", true);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksReadTimeoutExpression is set to a very short
     * value (forcing a timeout).
     * Test shows that we'll get a timeout exception failure and will fail to access the protected app
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.net.SocketTimeoutException" })
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_shortJwksReadTimeout_withEL() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OPShortJWKS", 0, 1, Constants.SIGALG_RS256);
        runSigningShortTimeoutTest(appName, app, true);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksReadTimeout is set to the default value and
     * jwksReadTimeoutExpression is set to a very short value (forcing a timeout).
     * Test shows that we'll get a timeout exception failure and will fail to access the protected app (showing that the el value overrides)
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.net.SocketTimeoutException" })
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_shortJwksReadTimeout_withELOverride() throws Exception {

        runSigningShortTimeoutTest("SigningShortJwksReadTimeoutELOverride", "SigningShortReadTimeout", true);
    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to "RS256" the jwksReadTimeout is set to a negative value .
     * Test shows that we'll be able to access the app, but, we'll see a message about the negative value in the server message log.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256_negativeJwksReadTimeoutValue() throws Exception {

        String appName = buildAppNameAndUpdateIssuer("OPNegativeJWKS", 0, -1, Constants.SIGALG_RS256);
        Page response = runGoodEndToEndTest(appName, app);

        // since the runtime will end up using the default of 500, make sure we see our app set the value to -1
        Expectations extraExpectations = new Expectations();
        extraExpectations.addExpectation(new ServerMessageExpectation(rpServer, "Setting the value for config attribute: jwksReadTimeoutExpression to: -1", "Did not find a message indicating that the jwksReadTimeoutExpression was being set to -1."));
        extraExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2507W_OIDC_MECH_CONFIG_NEGATIVE_INT, "Did not receive an error message stating that the OpenIdAuthenticationMechanismDefinition attribute should not be negative."));

        validationUtils.validateResult(response, extraExpectations);

    }

    /**
     * Test with an OP that specifies RS256 and the annotation in the app has idTokenSigningAlgorithmsSupported set to an empty string ("")
     * Test shows that the token received from the OP can be processed since the default value is used
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256BySettingEmptyString() throws Exception {

        runGoodEndToEndTest("EmptySigning", "EmptySigning");
    }

    /**
     * Test that a complex signature algorithm in the idTokenSigningAlgorithmsSupported atribute can be resolved and used properly.
     * The OP uses RS256 and the client idTokenSigningAlgorithmsSupported annotation attribute is specified as ${'RS384'.concat(', RS256')}
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256BySettingComplex1() throws Exception {

        runGoodEndToEndTest("ComplexSigning1", "ComplexSigning1");
    }

    /**
     * Test that a complex signature algorithm in the idTokenSigningAlgorithmsSupported atribute can be resolved and used properly.
     * The OP uses RS256 and the client idTokenSigningAlgorithmsSupported annotation attribute is specified as #{'RS384'.concat(', RS256')}
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256BySettingComplex2() throws Exception {

        runGoodEndToEndTest("ComplexSigning2", "ComplexSigning2");
    }

    /**
     * Test that a complex signature algorithm in the idTokenSigningAlgorithmsSupported atribute can be resolved and used properly.
     * The OP uses RS256 and the client idTokenSigningAlgorithmsSupported annotation attribute is specified as RS384, ES256, ${'RS256'}, RS512
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256BySettingComplex3() throws Exception {

        runGoodEndToEndTest("ComplexSigning3", "ComplexSigning3");
    }

    /**
     * Test that a complex signature algorithm in the idTokenSigningAlgorithmsSupported atribute can be resolved and used properly.
     * The OP uses RS256 and the client idTokenSigningAlgorithmsSupported annotation attribute is specified as RS384, ES256, #{'RS256'}, RS512
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256BySettingComplex4() throws Exception {

        runGoodEndToEndTest("ComplexSigning4", "ComplexSigning4");
    }

    /**
     * Test that a complex signature algorithm in the idTokenSigningAlgorithmsSupported atribute can be resolved and used properly.
     * The OP uses RS256 and the client idTokenSigningAlgorithmsSupported annotation attribute is specified as ${'RS384, RS256'}
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationSigningTests_providerSignsWithRS256_clientExpectsRS256BySettingComplex5() throws Exception {

        runGoodEndToEndTest("ComplexSigning5", "ComplexSigning5");
    }

    // userinfo response validation is also affected by signature settings and those tests are located in the ConfigurationUserInfoTests class

}
