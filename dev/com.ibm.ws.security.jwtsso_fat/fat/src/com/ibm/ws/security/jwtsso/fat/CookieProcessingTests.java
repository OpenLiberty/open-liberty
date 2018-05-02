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
import java.util.Iterator;
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
import com.gargoylesoftware.htmlunit.util.NameValuePair;
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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class CookieProcessingTests extends CommonSecurityFat {

    public static final String ACTION_INVOKE_PROTECTED_RESOURCE = "invokeProtectedResource";
    public static final String ACTION_SUBMIT_LOGIN_CREDENTIALS = "submitLoginCredentials";

    public static final String JWT_COOKIE_NAME = "jwtToken";

    public static final String BASIC_REALM = "BasicRealm";
    public static final String TESTUSER = "testuser";
    public static final String TESTUSERPWD = "testuserpwd";

    protected static Class<?> thisClass = CookieProcessingTests.class;

    // some common params used my multiple tests
    Page response = null;
    Expectations expectations = null;
    WebClient wc = null;
    String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/SimpleServlet";

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
     * perform the happy path login and access of protected resource.
     *
     * @throws Exception
     */
    void doHappyPath() throws Exception {
        expectations = new Expectations();
        expectations.addExpectations(getSuccessfulLoginPageExpectations(ACTION_INVOKE_PROTECTED_RESOURCE));
        wc = new WebClient();
        response = invokeUrl(wc, protectedUrl); // get back the login page
        validationUtils.validateResult(response, ACTION_INVOKE_PROTECTED_RESOURCE, expectations);

        expectations.addExpectations(getSuccessfulProtectedResourceExpectationsForJwtCookie(ACTION_SUBMIT_LOGIN_CREDENTIALS, protectedUrl));
        expectations.addExpectations(getJwtPrincipalExpectations(ACTION_SUBMIT_LOGIN_CREDENTIALS));
        response = performLogin(response);
        // confirm protected resource was accessed
        validationUtils.validateResult(response, ACTION_SUBMIT_LOGIN_CREDENTIALS, expectations);
    }

    /**
     * Test the splitting of a very large JWT token into multiple cookies.
     * A contrived group name will be used to construct a very large token.
     * Invoke app on the happy path, check that jwt cookies with correct names came back.
     * Access the app a second time to force the cookies to be merged back into a single token,
     * confirm that access is successful.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_largeCookies() throws Exception {
        reconfigServer("server_testlargecookies.xml");
        doHappyPath();
        // The test app logs the cookies,  check them that way.  Or we could look at the response headers.
        String responseStr = response.getWebResponse().getContentAsString();
        assertTrue("expected cookie jwtToken    not found in cookies", responseStr.contains("cookie: jwtToken"));
        assertTrue("expected cookie jwtToken02  not found in cookies", responseStr.contains("cookie: jwtToken02"));

        // now access resource a second time, force cookies to be rejoined into a single token
        response = invokeUrl(wc, protectedUrl);
        responseStr = response.getWebResponse().getContentAsString();
        boolean check2 = responseStr.contains("SimpleServlet");
        assertTrue("Did not successfully access the protected resource a second time", check2);

    }

    /**
     * check for presence of cleared cookies in the response headers
     *
     * @param lookForSecondCookie
     */
    void confirmCookiesCleared(boolean lookForSecondCookie) {
        //  look through all the headers as >1 Set-Cookie header might be sent back.
        List<NameValuePair> headerList = response.getWebResponse().getResponseHeaders();
        Iterator it = headerList.iterator();
        String combinedCookieValues = null;
        while (it.hasNext()) {
            NameValuePair nvp = (NameValuePair) it.next();
            if (nvp.getName().equals("Set-Cookie")) {
                combinedCookieValues += (nvp.getValue() + " ");
            }

        }
        //String cookie = response.getWebResponse().getResponseHeaderValue("Set-Cookie");
        Log.info(thisClass, "", "value of combined cookie header values: " + combinedCookieValues);
        assertTrue("did not find expected  cookie", combinedCookieValues != null);
        assertTrue("cookie name is wrong", combinedCookieValues.contains("jwtToken"));

        assertTrue("cookie jwtToken is not cleared", combinedCookieValues.contains("jwtToken=\"\";"));
        if (lookForSecondCookie) {
            assertTrue("cookie jwtToken02 is not cleared", combinedCookieValues.contains("jwtToken02=\"\";"));
        }
    }

    /**
     * Test that after accessing a protected resource and getting back a token, a call to HttpServletRequest.logout()
     * clears all jwt cookies
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_ServletLogout() throws Exception {
        reconfigServer("server_testlargecookies.xml");
        doHappyPath();

        // add attribute to tell the app to logout
        String logoutUrl = protectedUrl + "?logout=true";
        // now access resource a second time, force cookies to be rejoined into a single token
        response = invokeUrl(wc, logoutUrl);
        String responseStr = response.getWebResponse().getContentAsString();
        boolean check2 = responseStr.contains("Test Application class BaseServlet logged out");
        assertTrue("Did not get a response indicating logout was invoked", check2);
        confirmCookiesCleared(true);

        // and make sure we cannot access protected resource
        response = invokeUrl(wc, protectedUrl);
        responseStr = response.getWebResponse().getContentAsString();
        assertFalse("should not have been able to access protected url ", responseStr.contains("SimpleServlet"));
    }

    /**
     * Test that after accessing a protected resource and getting back a token, a call to the ibm_security_logout
     * url clears all jwt cookies
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_ibm_security_logout() throws Exception {
        reconfigServer("server_testlargecookies.xml");
        doHappyPath();
        // add attribute to tell the app to logout
        String logoutUrl = protectedUrl.replace("SimpleServlet", "ibm_security_logout");
        // now access resource a second time, force cookies to be rejoined into a single token
        response = invokeUrl(wc, logoutUrl, HttpMethod.POST);
        confirmCookiesCleared(true);

        // and make sure we cannot access protected resource
        response = invokeUrl(wc, protectedUrl);
        String responseStr = response.getWebResponse().getContentAsString();
        assertFalse("should not have been able to access protected url ", responseStr.contains("SimpleServlet"));
    }

    /**
     * Test that after accessing a protected resource and then logging out,
     * Replaying the jwttoken is detected and access is denied.
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_CookieReplay() throws Exception {
        reconfigServer("server.xml");
        doHappyPath();
        String responseStr = response.getWebResponse().getContentAsString();
        String beginStr = "cookie: jwtToken value: ";
        int begin = responseStr.indexOf(beginStr) + beginStr.length();
        int end = responseStr.indexOf("\n", begin);
        String token = responseStr.substring(begin, end);
        Log.info(thisClass, "", "value of cookie from response text " + token);
        assertTrue("did not find expected  cookie", token != null);

        // perform logout
        String logoutUrl = protectedUrl + "?logout=true";
        response = invokeUrl(wc, logoutUrl);
        responseStr = response.getWebResponse().getContentAsString();
        boolean check2 = responseStr.contains("Test Application class BaseServlet logged out");
        assertTrue("Did not get a response indicating logout was invoked", check2);

        // attempt the replay.
        String thisMethod = "test_CookieReplay";
        loggingUtils.printMethodName(thisMethod);

        WebRequest request = new WebRequest(new URL(protectedUrl), HttpMethod.GET);
        Log.info(thisClass, "", "setting cookie for replay:" + token);
        request.setAdditionalHeader("Cookie", token);
        loggingUtils.printRequestParts(wc, request, testName.getMethodName());

        Page response = wc.getPage(request); // should get bounced to login page
        loggingUtils.printResponseParts(response, testName.getMethodName(), "Response from URL: ");

        boolean accessedResource = response.getWebResponse().getContentAsString().contains("SimpleServlet");
        assertFalse("should not have been able to access the protected resource", accessedResource);

        // CWWKS9126A: Authentication using a JSON Web Token did not succeed because the token was previously logged out.
        String errorMsg = server.waitForStringInLogUsingMark("CWWKS9126A", 100);
        assertFalse("Did not find expected replay warning message CWWKS9126A in log", errorMsg == null);
    }

    //TODO: more tests for multiple cookies on non-root path

    void reconfigServer(String fileName) throws Exception {
        String relpath = "../../publish/servers/com.ibm.ws.security.jwtsso.fat/";
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setServerConfigurationFile(relpath + fileName);
        server.waitForStringInLog("CWWKF0008I|CWWKG0017I|CWWKG0018I", 180000);
        //  CWWKG0018I The server configuration was not updated. No functional changes were detected.
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
        return invokeUrl(wc, url, HttpMethod.GET);
    }

    public Page invokeUrl(WebClient wc, String url, HttpMethod method) throws Exception {
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
