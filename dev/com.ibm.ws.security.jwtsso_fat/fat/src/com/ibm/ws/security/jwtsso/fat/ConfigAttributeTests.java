/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.apps.CommonFatApplications;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.expectations.JwtExpectation;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ConfigAttributeTests extends CommonSecurityFat {

    public static final String ACTION_INVOKE_PROTECTED_RESOURCE = "invokeProtectedResource";
    public static final String ACTION_SUBMIT_LOGIN_CREDENTIALS = "submitLoginCredentials";

    public static final String JWT_COOKIE_NAME = "jwtToken";
    public static final String JWT_REGEX = "[a-zA-Z0-9_=-]+\\.[a-zA-Z0-9_=-]+\\.[a-zA-Z0-9_=-]+";

    public static final String BASIC_REALM = "BasicRealm";
    public static final String TESTUSER = "testuser";
    public static final String TESTUSERPWD = "testuserpwd";

    protected static Class<?> thisClass = ConfigAttributeTests.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, CommonFatApplications.getTestMarkerApp());
        ShrinkHelper.exportAppToServer(server, CommonFatApplications.getFormLoginApp());
        serverTracker.addServer(server);
        server.startServer();
    }

    /**
     * Test the config attributes cookieName and includeLtpaCookie.
     * Invoke app on the happy path, check that jwt cookie with correct name came back,
     * and that ltpa cookie came back.
     *
     */
    @Mode(TestMode.LITE)
    //@Test
    public void test_cookieName_includeLtpa() throws Exception {
        reconfigServer("server_testcookiename.xml");
        Expectations expectations = new Expectations();
        expectations.addExpectations(getSuccessfulLoginPageExpectations(ACTION_INVOKE_PROTECTED_RESOURCE));

        String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/SimpleServlet";
        Page response = invokeUrl(new WebClient(), protectedUrl); // get back the login page
        validationUtils.validateResult(response, ACTION_INVOKE_PROTECTED_RESOURCE, expectations);
        expectations.addExpectations(getSuccessfulProtectedResourceExpectationsForJwtCookie(ACTION_SUBMIT_LOGIN_CREDENTIALS, protectedUrl));
        expectations.addExpectations(getJwtPrincipalExpectations(ACTION_SUBMIT_LOGIN_CREDENTIALS));

        response = performLogin(response);

        validationUtils.validateResult(response, ACTION_SUBMIT_LOGIN_CREDENTIALS, expectations);

        // htmlunit getCookies seems not to work, but the test app logs them, check them that way
        String responseStr = response.getWebResponse().getContentAsString();
        assertTrue("expected cookie easyrider  not found in cookies", responseStr.contains("cookie: easyrider"));
        assertTrue("expected cookie LtpaToken2  not found in cookies", responseStr.contains("cookie: LtpaToken2"));

    }

    /**
     * specify a nonexistent builderRef, login should fail and we should get a meaningful error message in the log.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidBuilderException",
                   "com.ibm.ws.security.jwt.internal.JwtTokenException",
                   "com.ibm.websphere.security.WSSecurityException" })
    public void test_badJwtBuilderRef() throws Exception {
        reconfigServer("server_testbadbuilder.xml");
        ArrayList<String> ignoredErrors = new ArrayList<String>();
        ignoredErrors.add("CWWKS6201W");
        ignoredErrors.add("CWWKS6016E");
        server.addIgnoredErrors(ignoredErrors);
        Expectations expectations = new Expectations();
        expectations.addExpectations(getSuccessfulLoginPageExpectations(ACTION_INVOKE_PROTECTED_RESOURCE));
        String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/SimpleServlet";
        Page response = invokeUrl(new WebClient(), protectedUrl); // get back the login page
        validationUtils.validateResult(response, ACTION_INVOKE_PROTECTED_RESOURCE, expectations);
        response = performLogin(response); // things should have bombed and we should be back at the login page.
        boolean accessedResource = response.getWebResponse().getContentAsString().contains("SimpleServlet");
        assertFalse("should not have been able to access the protected resource", accessedResource);

        String warningMsg = server.waitForStringInLog("CWWKS6201W", 500); // Creation of a JWT Token failed
        assertTrue("did not find expected warning message in log", warningMsg != null);
    }

    /**
     * specify an existing and valid jwtBuilderRef, authenticate, examine the token contents in the
     * output of the test application to confirm the ref was used to create the token. *
     *
     * TODO: we have bug here, when builder changes in config, we keep building tokens with previous builder.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_validBuilderRef() throws Exception {
        reconfigServer("server_testgoodbuilder.xml");
        Expectations expectations = new Expectations();
        expectations.addExpectations(getSuccessfulLoginPageExpectations(ACTION_INVOKE_PROTECTED_RESOURCE));

        String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/SimpleServlet";

        Page response = invokeUrl(new WebClient(), protectedUrl); // get back the login page

        validationUtils.validateResult(response, ACTION_INVOKE_PROTECTED_RESOURCE, expectations);

        expectations.addExpectations(getSuccessfulProtectedResourceExpectationsForJwtCookie(ACTION_SUBMIT_LOGIN_CREDENTIALS, protectedUrl));
        expectations.addExpectations(getJwtPrincipalExpectations(ACTION_SUBMIT_LOGIN_CREDENTIALS));

        response = performLogin(response);

        validationUtils.validateResult(response, ACTION_SUBMIT_LOGIN_CREDENTIALS, expectations);

        String responseStr = response.getWebResponse().getContentAsString();
        boolean check = responseStr.contains("\"iss\":\"https://flintstone:19443/jwt/defaultJWT\"");
        assertTrue("Issuer in token did not match the one configured in the builder", check);

    }

    void reconfigServer(String fileName) throws Exception {
        String relpath = "../../publish/servers/com.ibm.ws.security.jwtsso.fat/";
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setServerConfigurationFile(relpath + fileName);
        server.waitForStringInLog("CWWKF0008I|CWWKG0017I", 180000);
        // CWWKF0008I Feature update completed, CWWKG0017I: The server configuration was successfully updated

    }

    Expectations getSuccessfulLoginPageExpectations(String testAction) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseStatusExpectation(testAction, 200));
        expectations.addExpectation(new ResponseTitleExpectation(testAction, Constants.STRING_MATCHES, "^login.jsp$", "Title of page returned during test step " + testAction
                                                                                                                      + " did not match expected value."));
        return expectations;
    }

    Expectations getSuccessfulProtectedResourceExpectationsForJwtCookie(String testAction, String protectedUrl) {
        Expectations expectations = new Expectations();
        expectations.addSuccessStatusCodesForActions(new String[] { testAction });
        expectations.addExpectation(new ResponseUrlExpectation(testAction, Constants.STRING_MATCHES, "^" + Pattern.quote(protectedUrl)
                                                                                                     + "$", "Did not reach the expected protected resource URL."));

        expectations.addExpectations(getResponseTextExpectationsForJwtCookie(testAction));
        return expectations;
    }

    Expectations getResponseTextExpectationsForJwtCookie(String testAction) {
        Expectations expectations = new Expectations();

        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "getRemoteUser: " + TESTUSER, "Did not find expected user in the response body."));

        String jwtPrincipalRegex = "getUserPrincipal: (\\{.+\\})";
        expectations.addExpectation(new ResponseFullExpectation(testAction, Constants.STRING_MATCHES, jwtPrincipalRegex, "Did not find expected JWT principal regex in response content."));

        String accessId = "accessId=user:" + BASIC_REALM + "/" + TESTUSER;
        expectations.addExpectation(new ResponseFullExpectation(testAction, Constants.STRING_MATCHES, "Public Credential: .+"
                                                                                                      + accessId, "Did not find expected access ID in response content."));
        return expectations;
    }

    Expectations getJwtPrincipalExpectations(String testAction) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new JwtExpectation(testAction, "token_type", "Bearer"));
        expectations.addExpectation(new JwtExpectation(testAction, "sub", TESTUSER));
        expectations.addExpectation(new JwtExpectation(testAction, "upn", TESTUSER));
        expectations.addExpectation(new JwtExpectation(testAction, "iss", "http://" + "[^/]+" + "/jwtsso/defaultJwtSso"));
        return expectations;
    }

    public Page invokeUrl(WebClient wc, String url) throws Exception {
        String thisMethod = "invokeUrl";
        loggingUtils.printMethodName(thisMethod);

        WebRequest request = new WebRequest(new URL(url), HttpMethod.GET);
        loggingUtils.printRequestParts(wc, request, testName.getMethodName());

        Page response = wc.getPage(request);
        loggingUtils.printResponseParts(response, testName.getMethodName(), "Response from URL: ");
        return response;
    }

    public Page performLogin(Page loginPage) throws Exception {
        String thisMethod = "performLogin";
        loggingUtils.printMethodName(thisMethod);

        if (!(loginPage instanceof HtmlPage)) {
            throw new Exception("Cannot perform login because the provided page object is not a " + HtmlPage.class.getName() + " instance. Page class is: "
                                + loginPage.getClass().getName());
        }
        return performLogin((HtmlPage) loginPage);
    }

    public Page performLogin(HtmlPage loginPage) throws Exception {
        String thisMethod = "performLogin";
        loggingUtils.printMethodName(thisMethod);
        try {
            Page postSubmissionPage = getAndSubmitLoginForm(loginPage);
            loggingUtils.printResponseParts(postSubmissionPage, thisMethod, "Response from login form submission:");
            return postSubmissionPage;
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Exception occurred in " + thisMethod);
            throw e;
        }
    }

    Page getAndSubmitLoginForm(HtmlPage loginPage) throws Exception {
        HtmlForm form = getAndValidateLoginForm(loginPage);
        return fillAndSubmitCredentialForm(form);
    }

    HtmlForm getAndValidateLoginForm(HtmlPage loginPage) throws Exception {
        List<HtmlForm> forms = loginPage.getForms();
        assertPageContainsAtLeastOneForm(forms);
        HtmlForm form = forms.get(0);
        validateLoginPageFormAction(form);
        return form;
    }

    void assertPageContainsAtLeastOneForm(List<HtmlForm> forms) throws Exception {
        if (forms == null || forms.isEmpty()) {
            throw new Exception("There were no forms found in the provided HTML page. We most likely didn't reach the login page. Check the page content to ensure we arrived at the expected web page.");
        }
    }

    void validateLoginPageFormAction(HtmlForm loginForm) throws Exception {
        String formAction = loginForm.getActionAttribute();
        if (formAction == null || !formAction.equals(Constants.J_SECURITY_CHECK)) {
            throw new Exception("The action attribute [" + formAction + "] of the form to use was either null or was not \"" + Constants.J_SECURITY_CHECK
                                + "\" as expected. Check the page contents to ensure we reached the correct page.");
        }
    }

    Page fillAndSubmitCredentialForm(HtmlForm form) throws Exception {
        getAndSetUsernameField(form);
        getAndSetPasswordField(form);
        return submitForm(form, "Login");
    }

    void getAndSetUsernameField(HtmlForm form) {
        getAndSetInputField(form, Constants.J_USERNAME, TESTUSER);
    }

    void getAndSetPasswordField(HtmlForm form) {
        getAndSetInputField(form, Constants.J_PASSWORD, TESTUSERPWD);
    }

    void getAndSetInputField(HtmlForm form, String inputName, String value) {
        String thisMethod = "getAndSetInputField";
        HtmlInput input = form.getInputByName(inputName);
        Log.info(thisClass, thisMethod, "Found input field for name \"" + inputName + "\": " + input);
        Log.info(thisClass, thisMethod, "Setting input value to: " + value);
        input.setValueAttribute(value);
    }

    Page submitForm(HtmlForm form, String submitButtonValue) throws IOException {
        HtmlInput submitButton = form.getInputByValue(submitButtonValue);
        return submitButton.click();
    }

}
