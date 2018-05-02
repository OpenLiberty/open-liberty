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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.expectations.JwtExpectation;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class FeatureOnlyTest extends CommonSecurityFat {

    public static final String ACTION_INVOKE_PROTECTED_RESOURCE = "invokeProtectedResource";
    public static final String ACTION_SUBMIT_LOGIN_CREDENTIALS = "submitLoginCredentials";

    public static final String APP_TESTMARKER = "testmarker";
    public static final String APP_FORMLOGIN = "formlogin";

    public static final String JWT_COOKIE_NAME = "jwtToken";

    public static final String BASIC_REALM = "BasicRealm";
    public static final String TESTUSER = "testuser";
    public static final String TESTUSERPWD = "testuserpwd";

    protected static Class<?> thisClass = FeatureOnlyTest.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {
        setUpServer(server);

        server.startServer();
    }

    static void setUpServer(LibertyServer server) throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, getTestMarkerApp());
        ShrinkHelper.exportAppToServer(server, getFormLoginApp());

        server.addInstalledAppForValidation(APP_TESTMARKER);
        server.addInstalledAppForValidation(APP_FORMLOGIN);

        serverTracker.addServer(server);
    }

    private static WebArchive getTestMarkerApp() throws Exception {
        return ShrinkHelper.buildDefaultApp(APP_TESTMARKER, "com.ibm.ws.security.fat.common.apps.testmarker.*");
    }

    private static WebArchive getFormLoginApp() throws Exception {
        return ShrinkHelper.buildDefaultApp(APP_FORMLOGIN, "com.ibm.ws.security.fat.common.apps.formlogin.*");
    }

    /**
     * Tests:
     * - Invoke the protected resource with the feature configured
     * - Log in with valid credentials
     * Expects:
     * - Should reach the protected resource
     * - JWT SSO cookie should be present
     * - LTPA cookie should not be present
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_goldenPath() throws Exception {
        Expectations expectations = new Expectations();
        expectations.addExpectations(getSuccessfulLoginPageExpectations(ACTION_INVOKE_PROTECTED_RESOURCE));

        String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/SimpleServlet";

        Page response = invokeUrl(new WebClient(), protectedUrl);

        validationUtils.validateResult(response, ACTION_INVOKE_PROTECTED_RESOURCE, expectations);

        expectations.addExpectations(getSuccessfulProtectedResourceExpectationsForJwtCookie(ACTION_SUBMIT_LOGIN_CREDENTIALS, protectedUrl));
        expectations.addExpectations(getJwtPrincipalExpectations(ACTION_SUBMIT_LOGIN_CREDENTIALS));

        response = performLogin(response);

        validationUtils.validateResult(response, ACTION_SUBMIT_LOGIN_CREDENTIALS, expectations);
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
        // cookies are now only present in j_security_check page response,
        //expectations.addExpectations(getCookieHeaderExpectationsForJwtCookie(testAction));
        expectations.addExpectations(getResponseTextExpectationsForJwtCookie(testAction));
        return expectations;
    }

    Expectations getResponseTextExpectationsForJwtCookie(String testAction) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "cookie: " + JWT_COOKIE_NAME,
                                                                          "Did not find a JWT cookie in the response body, but should have."));
        expectations.addExpectation(Expectation.createResponseMissingValueExpectation(testAction, "cookie: " + Constants.LTPA_COOKIE_NAME,
                                                                                      "Found an LTPA cookie in the response body but shouldn't have."));
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
