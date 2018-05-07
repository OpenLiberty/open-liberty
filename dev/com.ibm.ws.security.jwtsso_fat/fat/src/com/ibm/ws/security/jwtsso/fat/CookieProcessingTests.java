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

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.expectations.CookieExpectation;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class CookieProcessingTests extends CommonJwtFat {

    protected static Class<?> thisClass = CookieProcessingTests.class;

    // some common params used my multiple tests
    Page response = null;
    Expectations expectations = null;
    WebClient wc = null;
    String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/SimpleServlet";
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private TestActions actions = new TestActions();
    private TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {
        setUpAndStartServer(server, JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");
    }

    /**
     * perform the happy path login and access of protected resource.
     *
     * @throws Exception
     */
    void doHappyPath() throws Exception {
        expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE));
        wc = new WebClient();
        response = actions.invokeUrl(testName.getMethodName(), wc, protectedUrl); // get back the login page
        validationUtils.validateResult(response, TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, expectations);

        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, protectedUrl));
        expectations.addExpectation(new CookieExpectation(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, wc, JwtFatConstants.JWT_COOKIE_NAME, ".+", JwtFatConstants.NOT_SECURE, JwtFatConstants.HTTPONLY));
        expectations.addExpectations(CommonExpectations.getResponseTextExpectationsForJwtCookie(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, JwtFatConstants.JWT_COOKIE_NAME,
                                                                                                defaultUser, JwtFatConstants.BASIC_REALM));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));

        response = actions.doFormLogin(response, JwtFatConstants.TESTUSER, JwtFatConstants.TESTUSERPWD);
        // confirm protected resource was accessed
        validationUtils.validateResult(response, TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, expectations);
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
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_testlargecookies.xml");

        doHappyPath();
        // The test app logs the cookies,  check them that way.  Or we could look at the response headers.
        String responseStr = response.getWebResponse().getContentAsString();
        assertTrue("expected cookie jwtToken    not found in cookies", responseStr.contains("cookie: jwtToken"));
        assertTrue("expected cookie jwtToken02  not found in cookies", responseStr.contains("cookie: jwtToken02"));

        // now access resource a second time, force cookies to be rejoined into a single token
        response = actions.invokeUrl(testName.getMethodName(), wc, protectedUrl);
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
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_testlargecookies.xml");
        doHappyPath();

        // add attribute to tell the app to logout
        String logoutUrl = protectedUrl + "?logout=true";
        // now access resource a second time, force cookies to be rejoined into a single token
        response = actions.invokeUrl(testName.getMethodName(), wc, logoutUrl);
        String responseStr = response.getWebResponse().getContentAsString();
        boolean check2 = responseStr.contains("Test Application class BaseServlet logged out");
        assertTrue("Did not get a response indicating logout was invoked", check2);
        confirmCookiesCleared(true);

        // and make sure we cannot access protected resource
        response = actions.invokeUrl(testName.getMethodName(), wc, protectedUrl);
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
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_testlargecookies.xml");
        doHappyPath();
        // add attribute to tell the app to logout
        String logoutUrl = protectedUrl.replace("SimpleServlet", "ibm_security_logout");
        // now access resource a second time, force cookies to be rejoined into a single token
        WebRequest request = new WebRequest(new URL(logoutUrl), HttpMethod.POST);
        response = actions.submitRequest(testName.getMethodName(), wc, request);
        confirmCookiesCleared(true);

        // and make sure we cannot access protected resource
        response = actions.invokeUrl(testName.getMethodName(), wc, protectedUrl);
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
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");
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
        response = actions.invokeUrl(testName.getMethodName(), wc, logoutUrl);
        responseStr = response.getWebResponse().getContentAsString();
        boolean check2 = responseStr.contains("Test Application class BaseServlet logged out");
        assertTrue("Did not get a response indicating logout was invoked", check2);

        // attempt the replay.
        String thisMethod = "test_CookieReplay";
        loggingUtils.printMethodName(thisMethod);

        WebRequest request = new WebRequest(new URL(protectedUrl), HttpMethod.GET);
        Log.info(thisClass, "", "setting cookie for replay:" + token);
        request.setAdditionalHeader("Cookie", "jwtToken=" + token);
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

}
