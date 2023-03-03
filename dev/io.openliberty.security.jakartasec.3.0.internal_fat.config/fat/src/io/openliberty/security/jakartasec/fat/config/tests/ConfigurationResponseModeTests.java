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

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseHeaderExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests @OpenIdAuthenticationMechanismDefinition responseMode.
 *
 * This class contains tests using different response_mode values in the call to the auth endpoint.
 * Since Jakarta Security 3.0 currently only supports the auth code flow, the only valid
 * response_mode values are query and form_post. In these cases, the tests will verify that the correct response
 * is retrieved from the OP's authorization endpoint and that a successful flow can be completed.
 * Since the OpenLiberty OP does not allow values other than query and form_post for the auth code flow,
 * mock authorization endpoints were created to test the fragment and error cases. In these cases, the tests will
 * verify that the correct response is retrieved from the mock authorization endpoint and the callback is handled
 * the correct way.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ConfigurationResponseModeTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationResponseModeTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.responseMode")
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

        swh.defaultDropinApp(opServer, "Authorization.war", "authorization.servlets");

        swh.deployConfigurableTestApps(rpServer, "responseModeQuery.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseModeQuery", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseModeQuery()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "responseModeFragment.war", "ResponseModeWithMockAuthEndpoint.war",
                                       buildUpdatedConfigMapWithMockAuthorization(opServer, rpServer, "responseModeFragment", "AuthorizationResponseModeFragmentServlet",
                                                                                  "allValues.openIdConfig.properties",
                                                                                  TestConfigMaps.getResponseModeFragment()),
                                       "oidc.client.responseModeWithMockAuthEndpoint.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "responseModeFormPost.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "responseModeFormPost", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getResponseModeFormPost()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "responseModeError.war", "ResponseModeWithMockAuthEndpoint.war",
                                       buildUpdatedConfigMapWithMockAuthorization(opServer, rpServer, "responseModeError", "AuthorizationResponseModeErrorServlet",
                                                                                  "allValues.openIdConfig.properties",
                                                                                  TestConfigMaps.getResponseModeError()),
                                       "oidc.client.responseModeWithMockAuthEndpoint.servlets", "oidc.client.base.*");

    }

    public static Map<String, Object> buildUpdatedConfigMapWithMockAuthorization(LibertyServer opServer, LibertyServer rpServer, String appName, String authServletName,
                                                                                 String configFileName,
                                                                                 Map<String, Object> overrideConfigSettings) throws Exception {

        Map<String, Object> authorizationConfigMap = TestConfigMaps.getAuthorizationEndpoint(opHttpsBase, authServletName);
        overrideConfigSettings.putAll(authorizationConfigMap);
        return buildUpdatedConfigMap(opServer, rpServer, appName, configFileName, overrideConfigSettings);
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     *
     * Tests with responseMode = "query"
     * The auth endpoint should return a 302 response, redirecting to redirect uri with the code and state params as query params.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationResponseModeTests_responseMode_query() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseModeQuery/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        // disable redirect, so we are able to see the 302 responses
        // (otherwise, can't see redirect response from auth endpoint)
        webClient.getOptions().setRedirectEnabled(false);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        // follow redirect from login form to auth endpoint
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validate 302 response, redirect uri, and that the code and state params were included as query params
        Expectations expectations = new Expectations();
        expectations.addFoundStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ResponseHeaderExpectation(null, Constants.STRING_MATCHES, Constants.RESPONSE_HEADER_LOCATION, "https:\\/\\/localhost:"
                                                                                                                                      + rpServer.getBvtSecurePort()
                                                                                                                                      + "\\/responseModeQuery\\/Callback\\?code=.+&state=.+", "Did not get the code and state params as query params."));
        validationUtils.validateResult(response, expectations);

        // follow redirect from auth endpoint to redirect uri
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // follow redirect from callback servlet to original request
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validate that we were able to get to the original request and that we have an openid context
        validationUtils.validateResult(response, getGeneralAppExpecations(app));

    }

    /**
     *
     * Tests with responseMode = "fragment"
     * Since the OpenLiberty OP won't allow this value for code flow, a mock auth endpoint was used in this test case.
     * The mock auth endpoint should return a 302 response, redirecting to redirect uri with the code and state params as fragment params.
     * However, the callback should be treated a regular call to an unprotected resource, since the fragment params aren't passed from
     * the browser to server (rp) and thus, the rp can't tell if it's a callback request (determined by existence of state param).
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationResponseModeTests_responseMode_fragment() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        // disable redirect, so we are able to see the 302 responses
        // (otherwise, can't see redirect response from auth endpoint)
        webClient.getOptions().setRedirectEnabled(false);

        String app = "ResponseModeWithMockAuthEndpointServlet";
        String url = rpHttpsBase + "/responseModeFragment/" + app;

        // invoke the mock auth endpoint, this endpoint will return a 302 to the redirect uri with the code and state params as fragment params
        Page response = actions.invokeUrl(_testName, webClient, url);

        // follow redirect from original request to auth endpoint (mocked in this test case)
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validate 302 response, redirect uri, and that the code and state params were included as fragment params
        Expectations expectations = new Expectations();
        expectations.addFoundStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ResponseHeaderExpectation(null, Constants.STRING_MATCHES, Constants.RESPONSE_HEADER_LOCATION, "https:\\/\\/localhost:"
                                                                                                                                      + rpServer.getBvtSecurePort()
                                                                                                                                      + "\\/responseModeFragment\\/Callback#code=.+&state=.+", "Did not get the code and state params as fragment params."));
        validationUtils.validateResult(response, expectations);

        // follow redirect from auth endpoint to redirect uri
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validate 200 response, since fragment params are not passed to the rp (they are only read by the browser),
        // so it should be treated as a call to an unprotected resource (since it can't detect the state param to determine if it's a callback req)
        expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, "got here callback", "Did not land on the callback."));
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, ServletMessageConstants.CALLBACK + ServletMessageConstants.OPENID_CONTEXT
                                                                                                 + "OpenIdContext subject: null", "The subject was not null and should have been."));
        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with respondeMode = "form_post"
     * The auth endpoint should return a 200 response with an html form which contains the code and state params as input elements.
     * The html form should send a POST request to the redirect uri when submitted.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationResponseModeTests_responseMode_formPost() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/responseModeFormPost/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        // disable javascript, so the form_post form doesn't automatically redirect us
        // (otherwise, can't see form_post response from auth endpoint)
        webClient.getOptions().setJavaScriptEnabled(false);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        // validate 200 response and html form response containing the redirect uri, code param, and state param
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_MATCHES, ".*<FORM .* action=\"https:\\/\\/localhost:"
                                                                                          + rpServer.getBvtSecurePort()
                                                                                          + "\\/responseModeFormPost\\/Callback\" method=\"POST\">.*<input type=\"hidden\" name=\"code\" value=\".+\" \\/><input type=\"hidden\" name=\"state\" value=\".+\" \\/>.*", "Did not get the redirect uri, code param, and state param in the html form."));
        validationUtils.validateResult(response, expectations);

        // manually submit form, since we disabled javascript from automatically submitting it
        HtmlPage formPostPage = (HtmlPage) response;
        HtmlForm formPostForm = formPostPage.getForms().get(0);
        HtmlButton submitButton = formPostForm.getButtonByName("redirectform");
        response = submitButton.click();

        // validate that we were able to get to the original request and that we have an openid context
        validationUtils.validateResult(response, getGeneralAppExpecations(app));

    }

    /**
     *
     * Tests with responseMode = "error"
     * Since the OpenLiberty OP won't allow this value for code flow, a mock auth endpoint was used in this test case.
     * The mock auth endpoint should return a 302 response, redirecting to redirect uri with the error, error_description, and state params as query params.
     * The callback should detect the error param and fail.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationResponseModeTests_responseMode_error() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        // disable redirect, so we are able to see the 302 responses
        // (otherwise, can't see redirect response from auth endpoint)
        webClient.getOptions().setRedirectEnabled(false);

        String app = "ResponseModeWithMockAuthEndpointServlet";
        String url = rpHttpsBase + "/responseModeError/" + app;

        // invoke the mock auth endpoint, this endpoint will return a 302 to the redirect uri with the error and state params as query params
        Page response = actions.invokeUrl(_testName, webClient, url);

        // follow redirect from original request to auth endpoint (mocked in this test case)
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validate 302 response, redirect uri, and that the error and state params were included as query params
        Expectations expectations = new Expectations();
        expectations.addFoundStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ResponseHeaderExpectation(null, Constants.STRING_MATCHES, Constants.RESPONSE_HEADER_LOCATION, "https:\\/\\/localhost:"
                                                                                                                                      + rpServer.getBvtSecurePort()
                                                                                                                                      + "\\/responseModeError\\/Callback\\?error=.+&error_description=.+&state=.+", "Did not get the error, error_description, and state params as query params."));
        validationUtils.validateResult(response, expectations);

        // follow redirect from auth endpoint to redirect uri
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validate 401 response and error param detected messages
        expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2414E_CALLBACK_URL_INCLUDES_ERROR_PARAMETER, "Did not receive an error message stating that the callback url includes an error param."));
        validationUtils.validateResult(response, expectations);

    }

}
