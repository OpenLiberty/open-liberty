/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec.fat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SSOTest extends JavaEESecTestBase {

    private static final String COOKIE_NAME = "LtpaToken2";
    private static final String JAR_NAME = "JavaEESecBase.jar";

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    private static Class<?> logClass = SSOTest.class;
    private static String urlHttps;
    private static String basicUrl;
    private static String formContextRoot;
    private static String formUrl;

    private static String PARAM_FIRST = "firstName";
    private static String PARAM_LAST = "lastName";
    private static String PARAM_EMAIL = "eMailAddr";
    private static String PARAM_PHONE = "phoneNum";
    private static String PARAM_OPERATION = "operation";
    private static String PARAM_SUBMIT = "submitAdd";
    private static String VALUE_FIRST = "firstNameValue";
    private static String VALUE_LAST = "lastNameValue";
    private static String VALUE_EMAIL = "eMailAddr@value.com";
    private static String VALUE_PHONE = "123-123-1234";
    private static String VALUE_OPERATION = "Add";

    private DefaultHttpClient httpclient;

    public SSOTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESec.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets.basic");
        WCApplicationHelper.addWarToServerApps(myServer, "FormPostRedirect.war", true, "FormPostResources", JAR_NAME, false, "web.jar.base", "web.war.servlets.form.post.redirect");
        myServer.setServerConfigurationFile("sso.xml");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("JavaEESec");
        myServer.addInstalledAppForValidation("FormPostRedirect");
        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
        basicUrl = urlHttps + "/JavaEESec/MultipleISBasicAuthServlet";
        formContextRoot = urlHttps + "/FormPostRedirect";
        formUrl = formContextRoot + "/FormPostServlet";
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        myServer.stopServer();
    }

    @Before
    public void setUp() {
        httpclient = new DefaultHttpClient();
        SSLHelper.establishSSLContext(httpclient, 0, myServer, null, null, null, null, null);
    }

    @After
    public void tearDown() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    @Test
    public void testSSOForBasicAuthenticationMechanismDefinition() throws Exception {
        String cookieHeaderString = driveResourceFlow(basicUrl);
        assertCookie(cookieHeaderString, false, true);
        String response = redriveFlowWithCookieOnly(basicUrl, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.jaspi_servlet30User, Constants.getRemoteUserFound + Constants.jaspi_servlet30User);
    }

    @Test
    public void testSSOForFormAuthenticationMechanismDefinition() throws Exception {
        List<NameValuePair> params = createPostParams();
        driveResourceFlowWithFormAuth(formUrl, params);
        httpclient.getCredentialsProvider().clear();
        HttpResponse httpResponse = accessPageUsingPost(httpclient, formUrl, params);
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        verifyPostResponse(response, Constants.jaspi_servlet30User, VALUE_FIRST, VALUE_LAST, VALUE_EMAIL, VALUE_PHONE);
    }

    private String driveResourceFlow(String resource) throws Exception, IOException {
        HttpResponse httpResponse = executeGetRequestBasicAuthCreds(httpclient, resource, Constants.jaspi_servlet30User, Constants.jaspi_servlet30UserPwd);
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.jaspi_servlet30User, Constants.getRemoteUserFound + Constants.jaspi_servlet30User);
        Header cookieHeader = getCookieHeader(httpResponse, COOKIE_NAME);
        return cookieHeader.toString();
    }

    private void driveResourceFlowWithFormAuth(String resource, List<NameValuePair> params) throws Exception, IOException {
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = postFormLoginPage(httpclient, resource, params, true, formContextRoot + "/login.jsp", "login page for the form login test");

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, formContextRoot + "/j_security_check", Constants.jaspi_servlet30User, Constants.jaspi_servlet30UserPwd, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, "FormPostServlet");

        verifyPostResponse(response, Constants.jaspi_servlet30User, VALUE_FIRST, VALUE_LAST, VALUE_EMAIL, VALUE_PHONE);
    }

    private String redriveFlowWithCookieOnly(String resource, int expectedStatusCode) throws Exception {
        httpclient.getCredentialsProvider().clear();
        return executeGetRequestNoAuthCreds(httpclient, resource, expectedStatusCode);
    }

    private static List<NameValuePair> createPostParams() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_FIRST, VALUE_FIRST));
        params.add(new BasicNameValuePair(PARAM_LAST, VALUE_LAST));
        params.add(new BasicNameValuePair(PARAM_EMAIL, VALUE_EMAIL));
        params.add(new BasicNameValuePair(PARAM_PHONE, VALUE_PHONE));
        params.add(new BasicNameValuePair(PARAM_OPERATION, VALUE_OPERATION));
        params.add(new BasicNameValuePair(PARAM_SUBMIT, VALUE_OPERATION));
        return params;
    }

}
