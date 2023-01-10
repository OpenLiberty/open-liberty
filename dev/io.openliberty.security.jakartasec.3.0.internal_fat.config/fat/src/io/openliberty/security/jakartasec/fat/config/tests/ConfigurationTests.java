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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.config.tests;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
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
 * Tests various values set for annotation attributes that do NOT have Expression attributes (test the attributes that can have EL
 * values without having to have a separate attribute defined)
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ConfigurationTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    @ClassRule
    public static RepeatTests repeat = createRandomTokenTypeRepeats();

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

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
        // deploy the apps that are defined 100% by the source code tree
        swh.defaultDropinApp(rpServer, "OnlyProviderInAnnotation.war", "oidc.client.onlyProvider.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "NoProviderURIInAnnotationWithProviderMetadata.war", "oidc.client.noProviderURIInAnnotationWithProviderMetadata.servlets",
                             "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "NoProviderURIInAnnotationWithoutProviderMetadata.war", "oidc.client.noProviderURIInAnnotationWithoutProviderMetadata.servlets",
                             "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "MinimumAnnotation.war", "oidc.client.minimumAnnotation.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "MaximumAnnotation.war", "oidc.client.maximumAnnotation.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "MaximumAnnotationUsingEL.war", "oidc.client.maximumAnnotationUsingEL.servlets", "oidc.client.base.*");

        // deploy the apps that will be updated at runtime (now) (such as deploying the same app runtime with different names and embedded configs)
        swh.deployConfigurableTestApps(rpServer, "badClientId.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "badClientId", "allValues.openIdConfig.properties", TestConfigMaps.getBadClientId()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "omittedClientId.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "omittedClientId", "allValues.openIdConfig.properties", TestConfigMaps.getEmptyClientId()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "badClientSecret.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "badClientSecret", "allValues.openIdConfig.properties", TestConfigMaps.getBadClientSecret()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "omittedClientSecret.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "omittedClientSecret", "allValues.openIdConfig.properties", TestConfigMaps.getEmptyClientSecret()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "literalRedirectURI.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "literalRedirectURI", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getLiteralRedirectURI(rpHttpsBase)),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "notRegisteredWithOPRedirectURI.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "notRegisteredWithOPRedirectURI", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getNotRegisteredWithOPRedirectURI(rpHttpsBase)),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "doesNotExistRedirectURI.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "doesNotExistRedirectURI", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getDoesNotExistRedirectURI(rpHttpsBase)),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "badRedirectURI.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "badRedirectURI", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getBadRedirectURI()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "omittedRedirectURI.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "omittedRedirectURI", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getEmptyRedirectURI()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ELRedirectURI.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ELRedirectURI", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getELRedirectURI()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ELRedirectURIConcatInSingleEvalExpression.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ELRedirectURIConcatInSingleEvalExpression", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getELRedirectURIConcatInSingleEvalExpression()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ELRedirectURITwoEvalExpressions.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ELRedirectURITwoEvalExpressions", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getELRedirectURITwoEvalExpressions()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ELRedirectURIDoesNotContainBaseURL.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "ELRedirectURIDoesNotContainBaseURL", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getELRedirectURIDoesNotContainBaseURL(rpHttpsBase)),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "useSessionTrue.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "useSessionTrue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseSessionExpressionTrue()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "useSessionFalse.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "useSessionFalse", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseSessionExpressionFalse()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "responseTypeCode.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeCode", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeCode()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "responseTypeIdToken.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeIdToken", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeIdToken()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "responseTypeIdTokenToken.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeIdTokenToken", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeIdTokenToken()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "responseTypeCodeIdToken.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeCodeIdToken", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeCodeIdToken()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "responseTypeCodeToken.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeCodeToken", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeCodeToken()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "responseTypeCodeIdTokenToken.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeCodeIdTokenToken", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeCodeIdTokenToken()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "responseTypeUnknown.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeUnknown", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeUnknown()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "responseTypeEmpty.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseTypeEmpty", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseTypeEmpty()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * Test with the minimum required/needed config values in the annotation
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_minimumAnnotation() throws Exception {

        runGoodEndToEndTest("MinimumAnnotation", "MinimumAnnotationServlet");

    }

    /**
     * Test with all possible values specified (as values, not using EL) in the annotation
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_maximumAnnotation() throws Exception {

        // TODO config needs additional updates
        runGoodEndToEndTest("MaximumAnnotation", "MaximumAnnotationServlet");

    }

    /**
     * Test with all possible values specified (using EL) in the annotation
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_maximumAnnotationUsingEL() throws Exception {

        // TODO config needs additional updates
        runGoodEndToEndTest("MaximumAnnotationUsingEL", "MaximumAnnotationUsingELServlet");

    }

    /**
     * Test with providerURI omitted from the annotation. We'll also omit the providerMetadata. We should see failures indicating
     * that we haven't provided needed info to perform
     * discovery and have also not provided the info that discovery could have provided
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_noProviderURI_withoutProviderMetadata() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "NoProviderURIInAnnotationWithoutProviderMetadataServlet";
        String url = rpHttpsBase + "/NoProviderURIInAnnotationWithoutProviderMetadata/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, url, "Did not fail to land on " + url));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2401E_INVALID_CLIENT_CONFIG, "Did not receive an error message stating that the client config is invalid"));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2404W_PROVIDERURI_MISSING, "Did not receive an error message stating that the providerURI is missing."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2400E_AUTHORIZATION_REQ_FAILED, "Did not receive an error message stating that the authorization request failed."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS1652A_AUTH_SEND_FAILURE, "Did not receive an error message stating that Authentication failed with a SEND_FAILURE."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test with providerURI omitted from the annotation. We'll include the providerMetadata. With the providerMetadata, we don't
     * need the values from discovery.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException" })
    @Test
    public void ConfigurationTests_noProviderURI_withProviderMetadata() throws Exception {

        runGoodEndToEndTest("NoProviderURIInAnnotationWithProviderMetadata", "NoProviderURIInAnnotationWithProviderMetadataServlet");

    }

    /**
     * Only specify the providerURI - we should fail because the clientId is missing (the provider that is discovered can't
     * provide a default clientId)
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void ConfigurationTests_onlySpecifyProviderURIInAnnotation() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "OnlyProviderInAnnotationServlet";
        String url = rpHttpsBase + "/OnlyProviderInAnnotation/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, opHttpsBase
                                                                                          + "/oidc/endpoint/OP1/authorize", "Did not fail to invoke the authorization endpoint."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING, "Did not receive an error message stating that the required client_id was missing"));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Specify a "bad" client in the annotation - this will be a client that does not exist in the OP.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    @Test
    public void ConfigurationTests_badClientId() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/badClientId/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, opHttpsBase
                                                                                          + "/oidc/endpoint/OP1/authorize", "Did not fail to invoke the authorization endpoint."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWOAU0061E_COULD_NOT_FIND_CLIENT, "Did not receive an error message stating that the client name was not valid"));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0023E_CLIENTID_NOT_FOUND, "Did not receive an error message stating that the OP could not find the clientId."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Do not specify the clientId in the annotation - we should fail because the clientId is missing.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void ConfigurationTests_omittedClientId() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/omittedClientId/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, opHttpsBase
                                                                                          + "/oidc/endpoint/OP1/authorize", "Did not fail to invoke the authorization endpoint."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING, "Did not receive an error message stating that the required client_id was missing"));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Specify a "bad" clientSecret in the annotation - this will be a value that does not match the client's secret.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.http.BadPostRequestException", "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void ConfigurationTests_badClientSecret() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/badClientSecret/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();

        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS1406E_INVALID_CLIENT_CREDENTIAL, "Did not receive an error message stating that the client credential was invalid."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2416E_FAILED_TO_REACH_ENDPOINT, "Did not receive an error message stating that we couldn't react the token endpoint."));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED, "Did not receive an error message stating that the client could not be verified."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Do not specify the ClientSecret in the annotation - we should fail because the clientSecret is not specified.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.http.BadPostRequestException", "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void ConfigurationTests_omittedClientSecret() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/omittedClientSecret/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();

        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED, "Did not receive an error message stating that the client could not be verified."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with a literal redirectURI (no EL expression).
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_literalRedirectURI() throws Exception {

        runGoodEndToEndTest("literalRedirectURI", "GenericOIDCAuthMechanism");

    }

    /**
     *
     * Test with a redirectURI not registered with the OP. This should fail.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    @Test
    public void ConfigurationTests_notRegisteredWithOPRedirectURI() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/notRegisteredWithOPRedirectURI/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, opHttpsBase
                                                                                          + "/oidc/endpoint/OP1/authorize", "Did not fail to invoke the authorization endpoint."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWOAU0062E_REDIRECT_URI_INVALID, "Did not receive an error message stating that the redirect URI is not valid."));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0056E_REDIRECT_URI_NOT_REGISTERED, "Did not receive an error message stating that the redirect URI is not registered with the OP."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with a redirectURI that is registered with the OP, but does not exist on the RP.
     * This should fail with a 404.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_doesNotExistRedirectURI() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/doesNotExistRedirectURI/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addNotFoundStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.SRVE0190E_FILE_NOT_FOUND, "Did not receive an error message stating that the redirect URI resource was not found in the RP."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with a redirectURI that isn't a proper URI. This should fail.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    @Test
    public void ConfigurationTests_badRedirectURI() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/badRedirectURI/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, opHttpsBase
                                                                                          + "/oidc/endpoint/OP1/authorize", "Did not fail to invoke the authorization endpoint."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWOAU0062E_REDIRECT_URI_INVALID, "Did not receive an error message stating that the redirect URI is not valid."));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0026E_REDIRECT_URI_INVALID, "Did not receive an error message stating that the redirect URI is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with a redirectURI that is empty. This should fail.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void ConfigurationTests_omittedRedirectURI() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/omittedRedirectURI/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, opHttpsBase
                                                                                          + "/oidc/endpoint/OP1/authorize", "Did not fail to invoke the authorization endpoint."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING, "Did not receive an error message stating that the required redirectURI was missing."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with a redirectURI which contains an EL expression with baseURL.
     * e.g., "${baseURL}/Callback"
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_ELRedirectURI() throws Exception {

        runGoodEndToEndTest("ELRedirectURI", "GenericOIDCAuthMechanism");

    }

    /**
     *
     * Test with a redirectURI which contains an EL expression with baseURL
     * concatenated with another variable in the same expression.
     * e.g., "${baseURL += openIdConfig.callbackServlet}"
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_ELRedirectURI_concatInSingleEvalExpression() throws Exception {

        runGoodEndToEndTest("ELRedirectURIConcatInSingleEvalExpression", "GenericOIDCAuthMechanism");

    }

    /**
     *
     * Test with a redirectURI which contains two eval expressions.
     * e.g., "${baseURL}${openIdConfig.callbackServlet}"
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_ELRedirectURI_twoEvalExpressions() throws Exception {

        runGoodEndToEndTest("ELRedirectURITwoEvalExpressions", "GenericOIDCAuthMechanism");

    }

    /**
     *
     * Test with a redirectURI which does not contain the baseURL variable.
     * This should fail, since we only do double EL expression evaluation if the first EL returned
     * contains the baseURL variable.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    @Test
    public void ConfigurationTests_ELRedirectURI_doesNotContainBaseURL() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/ELRedirectURIDoesNotContainBaseURL/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, opHttpsBase
                                                                                          + "/oidc/endpoint/OP1/authorize", "Did not fail to invoke the authorization endpoint."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWOAU0062E_REDIRECT_URI_INVALID, "Did not receive an error message stating that the redirect URI is not valid."));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0026E_REDIRECT_URI_INVALID, "Did not receive an error message stating that the redirect URI is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    // useSession tested in class ConfigurationELValuesOverride & ConfigurationWithoutHttpSessionTests
    // useSessionExpression tested in class ConfigurationELValuesOverride & ConfigurationWithoutHttpSessionTests

    // test claimsDefinition with invalid and omitted name and groups (omitted name will fail - make sure we get a useful msg - ommited groups should work just fine)

    // these need to be run with both testSession and testSessionExpression
    //    /**
    //     * Specify useSession set to true - the overall server configs specify
    //     * <httpSession cookieHttpOnly="false" cookieName="<serverUniqueName"/>
    //     * This means that the useSession flag will not really make a difference.
    //     * The ConfigurationWithoutHttpSessionTests_noServerWideHttpSession_useSession_true will test without the httpSession config
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void ConfigurationTests_serverWideUniqueHttpSession_useSession_true() throws Exception {
    //
    //        runGoodEndToEndTest("useSessionTrue", "GenericOIDCAuthMechanism");
    //
    //    }
    //
    //    /**
    //     * Specify useSession set to false - the overall server configs specify
    //     * <httpSession cookieHttpOnly="false" cookieName="<serverUniqueName"/>
    //     * This means that the useSession flag will not really make a difference.
    //     * The ConfigurationWithoutHttpSessionTests_noServerWideHttpSession_useSession_false will test without the httpSession config
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void ConfigurationTests_serverWideUniqueHttpSession_useSession_false() throws Exception {
    //
    //        runGoodEndToEndTest("useSessionFalse", "GenericOIDCAuthMechanism");
    //
    //    }

    // useSession - may need a different OP with different httpSession values in the server config in order to fully test
    // <httpSession cookieHttpOnly="false" cookieName="clientJSESSIONID"/>

    /**
     *
     * Test with Authorization Code Flow.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTests_responseType_code() throws Exception {

        runGoodEndToEndTest("responseTypeCode", "GenericOIDCAuthMechanism");

    }

    /**
     *
     * Test with Implicit Flow. This should fail, since implicit flow isn't currently supported.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException" })
    @Test
    public void ConfigurationTests_responseType_idToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseTypeIdToken/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2423E_OIDC_CLIENT_INVALID_RESPONSE_TYPE, "Did not receive an error message stating that the response type is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with Implicit Flow. This should fail, since implicit flow isn't currently supported.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException" })
    @Test
    public void ConfigurationTests_responseType_idTokenToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseTypeIdTokenToken/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2423E_OIDC_CLIENT_INVALID_RESPONSE_TYPE, "Did not receive an error message stating that the response type is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with Hybrid Flow. This should fail, since hybrid flow isn't currently supported.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException" })
    @Test
    public void ConfigurationTests_responseType_codeIdToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseTypeCodeIdToken/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2423E_OIDC_CLIENT_INVALID_RESPONSE_TYPE, "Did not receive an error message stating that the response type is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with Hybrid Flow. This should fail, since hybrid flow isn't currently supported.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException" })
    @Test
    public void ConfigurationTests_responseType_codeToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseTypeCodeToken/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2423E_OIDC_CLIENT_INVALID_RESPONSE_TYPE, "Did not receive an error message stating that the response type is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with Hybrid Flow. This should fail, since hybrid flow isn't currently supported.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException" })
    @Test
    public void ConfigurationTests_responseType_codeIdTokenToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseTypeCodeIdTokenToken/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2423E_OIDC_CLIENT_INVALID_RESPONSE_TYPE, "Did not receive an error message stating that the response type is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with an unknown response type. This should fail.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException" })
    @Test
    public void ConfigurationTests_responseType_unknown() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseTypeUnknown/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2423E_OIDC_CLIENT_INVALID_RESPONSE_TYPE, "Did not receive an error message stating that the response type is not valid."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with an empty response type. This should fail.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException" })
    @Test
    public void ConfigurationTests_responseType_empty() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseTypeEmpty/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2423E_OIDC_CLIENT_INVALID_RESPONSE_TYPE, "Did not receive an error message stating that the response type is not valid."));

        validationUtils.validateResult(response, expectations);

    }

}
