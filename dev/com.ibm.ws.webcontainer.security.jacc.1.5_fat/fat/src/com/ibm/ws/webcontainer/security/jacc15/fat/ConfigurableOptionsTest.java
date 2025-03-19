/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

package com.ibm.ws.webcontainer.security.jacc15.fat;

/**
 * Note:
 * 1. User registry:
 * At this time, the test uses users, passwords, roles, and groups predefined in server.xml as
 * test user registry.
 *
 * TODO:  use different user registry
 *
 * 2. The constraints (which servlets can be accessed by which user/group/role) are defined in web.xml
 *
 <role-name>Employee</role-name>
 </auth-constraint>

 and

 <security-constraint id="SecurityConstraint_5">
 <web-resource-collection id="WebResourceCollection_5">
 <web-resource-name>Protected with overlapping * and Employee roles</web-resource-name>
 <url-pattern>/OverlapNoConstraintServlet</url-pattern>
 <http-method>GET</http-method>
 <http-method>POST</http-method>
 </web-resource-collection>
 <auth-constraint id="AuthConstraint_5">
 <role-name>*</role-name>
 </auth-constraint>
 </security-constraint>

 servlet OverlapNoConstraintServlet will allow access to all roles since
 the role = * (any role) and role =  Employee are combined and * will win.

 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;
import com.ibm.ws.webcontainer.security.test.servlets.SSLFormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;
import com.ibm.ws.webcontainer.security.test.servlets.TestConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ConfigurableOptionsTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.loginmethod");
    private static final Class<?> thisClass = ConfigurableOptionsTest.class;
    CommonTestHelper testHelper = new CommonTestHelper();

    protected final static String BASIC_AUTH_SERVLET = "BasicAuthServlet";
    private final static String SERVLETNAME = "FormLoginServlet";
    private final static String SECURE_SERVLET = "SecureSimpleServlet";
    private final static String PROGRAMMATICAPI_SERVLET = "ProgrammaticAPIServlet";
    private static final String BASIC_AUTH_SERVLET_URL = createHttpURL("/basicauth/SimpleServlet");
    private static final String HTTPS_BASIC_AUTH_SERVLET_URL = createHttpsURL("/basicauth/SecureSimpleServlet");
    private static final String LOGIN_CONFIG_INVALID_URL = createHttpURL("/loginConfigInvalid/SimpleServlet");
    private static final String LOGIN_CONFIG_NONE_URL = createHttpURL("/loginConfigNone/SimpleServlet");
    private static final String LOGIN_CONFIG_NO_REALM_NAME_URL = createHttpURL("/loginConfigNoRealmName/SimpleServlet");
    private static final String CLIENT_CERT_URL = createHttpURL("/clientcert/SimpleServlet");
    private static final String FORM_LOGIN_SERVLET_URL = createHttpURL("/formlogin/SimpleServlet");
    private static final String HTTPS_FORM_LOGIN_SERVLET_URL = createHttpsURL("/formlogin/SimpleServlet");
    private static final String PROGRAMMATICAPI_SERVLET_URL = createHttpURL("/basicauth/" + PROGRAMMATICAPI_SERVLET);
    private static final String HTTPS_SECURE_SERVLET_URL = createHttpsURL("/basicauth/" + SECURE_SERVLET);
    private static final String HTTPS_FORM_LOGIN_SECURE_SERVLET_URL = createHttpsURL("/formlogin/SecureSimpleServlet");

    private final static String webXMLRealm = "Basic Authentication";
    private final static String userRegistryRealm = "BasicRealm";
    private final static String defaultRealm = "defaultRealm";
    private final static String invalidRealm = "INVALID";
    private final static String clientCertRealm = "Client Cert Authentication";
    private final static String validUser = "user1";
    private final static String validPassword = "user1pwd";
    private final static String managerUser = "user2";
    private final static String managerPassword = "user2pwd";

    private final static String FORMLOGINJSP = "Form Login Page";
    private final static String FORM_LOGOUT_PAGE = "Form Logout Page";

    private final static String DEFAULT_CONFIG_FILE = "configoptions.server.orig.xml";
    private final static String CONFIG_OPTIONS_FILE = "configOptions.xml";
    private final static String NO_SSO_CONFIG_OPTIONS_FILE = "configOptionsNoSSO.xml";
    private final static String DOMAIN_NAMES_CONFIG_OPTIONS_FILE = "configOptionsDomainNames.xml";
    private final static String INVALID_DOMAIN_NAMES_CONFIG_OPTIONS_FILE = "configOptionsDomainNamesIBM.xml";
    private final static String POST_PARAM_SIZE_CONFIG_OPTIONS_FILE = "configOptionsPostParamCookieSize.xml";
    private final static String POST_PARAM_NONE_CONFIG_OPTIONS_FILE = "configOptionsPostParamNone.xml";
    private final static String SSO_REQUIRES_SSL_CONFIG_OPTIONS_FILE = "configOptionsSSORequiresSSL.xml";
    private final static String SSO_DOMAIN_FROM_URL_CONFIG_OPTIONS_FILE = "configOptionsSSOUseDomainFromURL.xml";

    private final static String DEFAULT_COOKIE_NAME = "LtpaToken2";
    private final static String UPDATED_COOKIE_NAME = "MyToken";
    private final static String DOMAIN_COOKIE_NAME = "Domain";

    private static String ipAddress = null;
    private static String fullyQualifiedDomainName = null;
    private static String basicAuthFQDNUrl;
    private static String formLoginFQDNUrl;
    private static String formLoginFQDNPath;
    private static String basicAuthIPAddressUrl;
    private static String ibmDomainName = "ibm.com";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static FormLoginClient client;
    private static SSLFormLoginClient sslClient;

    // TestName should be an instance level Rule, not a ClassRule, but static references are made to it,
    // so we will create it as a static field, then keep a reference to it from a non-static field which
    // is annotated with @Rule - junit can make the test method name changes to that field, which should
    // (hopefully) be reflected in the static references as well.
    private static TestName _name = new TestName();

    @Rule
    public TestName name = _name;

    private static final TestConfiguration testConfig = new TestConfiguration(server, thisClass, _name, "loginmethod");

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        testConfig.startServerClean(DEFAULT_CONFIG_FILE);

        JACCFatUtils.transformApps(server, "loginmethod.ear");

        client = new FormLoginClient(server);
        sslClient = new SSLFormLoginClient(server);
        client.setJaccValidation(true);
        sslClient.setJaccValidation(true);

        String methodName = "setUp";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            // Get IP Address
            ipAddress = localHost.getHostAddress();
            Log.info(thisClass, methodName, "ipAddress: " + ipAddress);
            basicAuthIPAddressUrl = createURL("http", ipAddress, server.getHttpDefaultPort(), "/basicauth/SimpleServlet");

            // Get hostname
            fullyQualifiedDomainName = localHost.getCanonicalHostName();
            Log.info(thisClass, methodName, "fullyQualifiedDomainName: " + fullyQualifiedDomainName);

            basicAuthFQDNUrl = createFQDNUrl("/basicauth/SimpleServlet");
            formLoginFQDNUrl = createFQDNUrl("/formlogin/SimpleServlet");
            formLoginFQDNPath = createFQDNUrl("/formlogin");
        } catch (UnknownHostException e) {
            Log.info(thisClass, methodName, "Caught UnknownHostException when getting IP address/hostname: " + e);
        }
    }

    @After
    public void tearDown() throws Exception {
        client.resetClientState();
        sslClient.resetClientState();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Reset server to use original configuration
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        server.stopServer();
    }

    private static String createHttpURL(String servletPath) {
        return createURL("http", server.getHostname(), server.getHttpDefaultPort(), servletPath);
    }

    private static String createHttpsURL(String servletPath) {
        return createURL("https", server.getHostname(), server.getHttpDefaultSecurePort(), servletPath);
    }

    private static String createFQDNUrl(String servletPath) {
        return createURL("http", fullyQualifiedDomainName, server.getHttpDefaultPort(), servletPath);
    }

    private static String createURL(String scheme, String hostName, int port, String servletPath) {
        return scheme + "://" + hostName + ":" + port + servletPath;
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in metatype.xml are being used:
     * <LI> ssoCookieName="LtpaToken2"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultCookieName() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        assertCookieNameAfterBasicAuthLogin(BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET, DEFAULT_COOKIE_NAME);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following default options defined in metatype.xml are being used:
     * <LI> httpOnlyCookies="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultHttpOnlyCookies() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        Header cookieHeader = authenticateWithValidAuthData_BA(webXMLRealm, validUser, validPassword,
                                                               BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET, true);
        verifyHttpOnlyCookies(cookieHeader, true);
    }

    /**
     * Test the user constraint CONFIDENTIAL, access on https port should
     * be successful
     * Tests default property setting in server.xml: ssoRequiresSSL="false"
     *
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permitted access to the protected servlet
     * <LI> Successful access to the servlet and returns an SSO cookie since SSL
     * <LI> is not required but is used
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_ssoRequiresSSLFalseWithHTTPS() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        assertCookieNameAfterBasicAuthLogin(HTTPS_BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET, DEFAULT_COOKIE_NAME);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be the <realm-name> specified in the web.xml file
     * <LI> since it will default to this realm-name if it exists in web.xml
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultAuthenticationRealmWebXML() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        checkRealmName(BASIC_AUTH_SERVLET_URL, webXMLRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be "INVALID" since displayAuthenticationRealm="false" and
     * <LI> the login-config specified in web.xml is invalid but will fall back to Basic Auth
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultAuthenticationRealmInvalidLoginConfig() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        checkRealmName(LOGIN_CONFIG_INVALID_URL, invalidRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be "Default Realm" since displayAuthenticationRealm="false" and
     * <LI> no login-config is specified in web.xml
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultAuthenticationRealmNoLoginConfig() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        checkRealmName(LOGIN_CONFIG_NONE_URL, defaultRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be "Default Realm" since displayAuthenticationRealm="false" and
     * <LI> no realm-name is specified in web.xml
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultAuthenticationRealmNoRealmName() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        checkRealmName(LOGIN_CONFIG_NO_REALM_NAME_URL, defaultRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be "Client Cert Authentication" specified in the web.xml file
     * <LI> since it will default to this realm-name if it exists in web.xml
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultAuthenticationRealmClientCert() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        checkRealmName(CLIENT_CERT_URL, clientCertRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using the FQDN.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> NO ssoDomainNames in server.xml
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultSSODomainName() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        assertDomainNotFoundInCookieAfterBasicAuthLogin(basicAuthFQDNUrl, BASIC_AUTH_SERVLET);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using the FQDN
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> No ssoUseDomainFromURL in server.xml, which defaults to "false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie because ssoUseDomainFromURL is set to false
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateDefaultSSOUseDomainFromURL() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);
        assertDomainNotFoundInCookieAfterBasicAuthLogin(basicAuthFQDNUrl, BASIC_AUTH_SERVLET);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login using the FQDN.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> NO ssoDomainNames in server.xml
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login page when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateDefaultSSODomainName() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        HttpClient client = new DefaultHttpClient();
        Header cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, validUser, validPassword,
                                                               formLoginFQDNUrl, true, true);

        assertDomainNotFoundInCookie(cookieHeader);

        formLogout(client, formLoginFQDNPath);

        // Rerun the test to make sure you are prompted after form logout
        cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, validUser, validPassword,
                                                        formLoginFQDNUrl, true, true);
        assertDomainNotFoundInCookie(cookieHeader);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login using the FQDN
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> No ssoUseDomainFromURL in server.xml, which defaults to "false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie because ssoUseDomainFromURL is set to false
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateDefaultSSOUseDomainFromURL() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        HttpClient client = new DefaultHttpClient();
        Header cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, validUser, validPassword,
                                                               formLoginFQDNUrl, true, true);
        assertDomainNotFoundInCookie(cookieHeader);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following default options defined in metatype.xml are being used:
     * <LI> ssoCookieName="LtpaToken2"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form.
     * <LI> Attempt to go to the servlet again after logging out,
     * <LI> User will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateDefaultCookieName() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        client.setSSOCookieName(DEFAULT_COOKIE_NAME);
        String response = client.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        assertTrue(client.verifyResponse(response, validUser, FormLoginClient.IS_EMPLOYEE_ROLE, FormLoginClient.NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following default options defined in metatype.xml are being used:
     * <LI> httpOnlyCookies="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form.
     * <LI> Attempt to go to the servlet again after logging out,
     * <LI> User will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateDefaultHttpOnlyCookies() throws Exception {
        String methodName = "testConfigOptionsFL_validateDefaultHttpOnlyCookies";

        Header cookieHeader2 = null;

        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        cookieHeader2 = authenticateWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                                         FORM_LOGIN_SERVLET_URL, true);

        // Get cookie
        String cookieReturned = verifyCookieName(cookieHeader2, DEFAULT_COOKIE_NAME);
        Log.info(thisClass, methodName, "cookie returned: " + cookieReturned);

        // Verify value of httpOnlyCookies
        verifyHttpOnlyCookies(cookieHeader2, true);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout, which redirects to login page
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following configurable options defined in metatype.xml are being used:
     * <LI> allowLogoutPageRedirectToAnyHost="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form, which redirects to login page
     * <LI> Successful logged out with logout3 form, which redirects to default logout page
     * <LI>
     * <LI>Note: This test uses httpunit for form logout since httpclient does not support form logout with 2 logout forms
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_LoginThenLogoutRedirectDefault() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        // Try a logout to the login page
        client.formLogout(LogoutOption.LOGOUT_TO_LOGIN_PAGE, validUser, validPassword);

        // Now try with an external page: "http://www.w3.org/Protocols/HTTP/AsImplemented.html"
        // Verify that redirect goes back to the default logout page
        // instead of external page
        client.formLogout(LogoutOption.LOGOUT_OFF_HOST_FAIL, validUser, validPassword);
    }

    /**
     * TODO: Change to use session timeout when available
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password
     * <LI>Server stopped and restarted.
     * <LI>Attempt to access the servlet again.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> logoutOnHttpSessionExpire="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> Server stopped and restarted.
     * <LI> After server restart, user is not required to log in before accessing the protected servlet.
     * </OL>
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_logoutOnHttpSessionExpireDefault() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        authenticateWithValidAuthDataSessionExpires_FL(client1, webXMLRealm, validUser, validPassword,
                                                       FORM_LOGIN_SERVLET_URL, false);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following default options defined in metatype.xml are being used:
     * <LI> preserveFullyQualifiedReferrerUrl="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form.
     * <LI> Attempt to go to the servlet again after logging out,
     * <LI> User will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateDefaultReferrerURL() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        // log in to the protected servlet
        DefaultHttpClient client1 = new DefaultHttpClient();
        authenticateLogInWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                              FORM_LOGIN_SERVLET_URL, false);
    }

    /**
     * Test the user constraint CONFIDENTIAL, access on https port should
     * be successful
     * Tests default property setting: ssoRequiresSSL="false"
     *
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permitted access to the protected servlet
     * <LI> Successful access to the servlet and returns an SSO cookie since SSL
     * <LI> is not required but is used
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_ssoRequiresSSLFalseWithHTTPS() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        // log in to the protected servlet
        HttpClient client = new DefaultHttpClient();
        Header cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, managerUser, managerPassword,
                                                               HTTPS_FORM_LOGIN_SERVLET_URL, true);

        // Get cookie and verify cookie name and Secure not set
        String cookieReturned = verifyCookieName(cookieHeader, DEFAULT_COOKIE_NAME);
        Log.info(thisClass, name.getMethodName(), "cookie returned: " + cookieReturned);
        verifySecureCookies(cookieHeader, false);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password
     * <LI>Verify that the following default options defined in metatype.xml are being used:
     * <LI> postParamSaveMethod="Cookie"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateDefaultPostParamMethod() throws Exception {
        testConfig.setDifferentConfig(DEFAULT_CONFIG_FILE);

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        String inputText = "Form login servlet test of post";
        authenticatePostParamWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                                  FORM_LOGIN_SERVLET_URL, inputText, true);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using the FQDN.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoUseDomainFromURL="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> The cookie should have "Domain" set to a valid domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedSSOUseDomainFromURL() throws Exception {
        if (fullyQualifiedDomainName.contains(ibmDomainName)) {
            // Use default configuration and wait for app update message TWICE because we are
            // switching from appSecurity-2.0 to appSecurity-1.0
            testConfig.modifyWebAppSecurity(SSO_DOMAIN_FROM_URL_CONFIG_OPTIONS_FILE);
            testConfig.assertApplicationUpdated();

            assertCookieDomainAfterBasicAuthLogin(basicAuthFQDNUrl, BASIC_AUTH_SERVLET, DOMAIN_COOKIE_NAME);
        } else {
            logNoFullyQualifiedDomainName();
        }
    }

    private void logNoFullyQualifiedDomainName() {
        Log.info(thisClass, name.getMethodName(), "Did not get FQDN from system, instead got: " + fullyQualifiedDomainName);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using the IP address.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoUseDomainFromURL="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie because the IP address should not match the domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedSSOUseDomainFromURLIPAddress() throws Exception {
        testConfig.modifyWebAppSecurity(SSO_DOMAIN_FROM_URL_CONFIG_OPTIONS_FILE);
        assertDomainNotFoundInCookieAfterBasicAuthLogin(basicAuthIPAddressUrl, BASIC_AUTH_SERVLET);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using localhost.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoUseDomainFromURL="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie because localhost should not match the domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedSSOUseDomainFromURLLocalHost() throws Exception {
        testConfig.modifyWebAppSecurity(SSO_DOMAIN_FROM_URL_CONFIG_OPTIONS_FILE);
        assertDomainNotFoundInCookieAfterBasicAuthLogin(BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login using the FQDN.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoUseDomainFromURL="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> The cookie should have "Domain" set to a valid domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateUpdatedSSOUseDomainFromURL() throws Exception {
        if (fullyQualifiedDomainName.contains(ibmDomainName)) {
            testConfig.modifyWebAppSecurity(SSO_DOMAIN_FROM_URL_CONFIG_OPTIONS_FILE);

            HttpClient client = new DefaultHttpClient();
            Header cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, validUser, validPassword,
                                                                   formLoginFQDNUrl, true, true);

            // Verify Domain ibm.com in cookie
            String cookieResult = verifyCookieName(cookieHeader, DOMAIN_COOKIE_NAME);
            assertTrue("Expect ibm.com in cookie but got: " + cookieResult, cookieResult.contains(ibmDomainName));

            formLogout(client, formLoginFQDNPath);

            // Rerun the test to make sure you are prompted after form logout
            cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, validUser, validPassword,
                                                            formLoginFQDNUrl, true, true);
            // Verify Domain ibm.com in cookie
            cookieResult = verifyCookieName(cookieHeader, DOMAIN_COOKIE_NAME);
            assertTrue("Expect ibm.com in cookie but got: " + cookieResult, cookieResult.contains(ibmDomainName));
        } else {
            logNoFullyQualifiedDomainName();
        }
    }

    /**
     * Test over HTTP to a protected servlet
     * Tests default property setting: ssoRequiresSSL="true"
     *
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permitted access to the protected servlet.
     * <LI> Successful access to the servlet but no SSO cookie since SSL
     * <LI> required but is not used
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_ssoRequiresSSLTrueWithHTTP() throws Exception {
        testConfig.modifyWebAppSecurity(SSO_REQUIRES_SSL_CONFIG_OPTIONS_FILE);

        // Log in to get the cookie. Should not get an SSO cookie, so authenticateWithValidAuthData_BA if SSO cookie is found
        authenticateWithValidAuthData_BA(webXMLRealm, validUser, validPassword,
                                         PROGRAMMATICAPI_SERVLET_URL, PROGRAMMATICAPI_SERVLET, false);
    }

    /**
     * Test the user constraint CONFIDENTIAL, access on https port should
     * be successful
     * Tests property setting: ssoRequiresSSL="true"
     *
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permitted access to the protected servlet
     * <LI> Successful access to the servlet and returns an SSO cookie since SSL
     * <LI> is required and is used
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_ssoRequiresSSLTrueWithHTTPS() throws Exception {
        testConfig.modifyWebAppSecurity(SSO_REQUIRES_SSL_CONFIG_OPTIONS_FILE);
        assertCookieNameAfterBasicAuthLogin(HTTPS_SECURE_SERVLET_URL, BASIC_AUTH_SERVLET, DEFAULT_COOKIE_NAME);
    }

    /**
     * Test over HTTP to a protected servlet
     * Tests default property setting: ssoRequiresSSL="true"
     *
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permitted access to the protected servlet.
     * <LI> Successful access to the servlet but no SSO cookie since SSL
     * <LI> required but is not used. Also, with form login the user will be
     * <LI> redirected back to the form login page since there is no SSO cookie to
     * <LI> give access to the original servlet
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_ssoRequiresSSLTrueWithHTTP() throws Exception {
        testConfig.modifyWebAppSecurity(SSO_REQUIRES_SSL_CONFIG_OPTIONS_FILE);

        // log in to the protected servlet. authenticateWithValidAuthData_FL will verify that the SSO cookie is found
        HttpClient client = new DefaultHttpClient();
        authenticateWithValidAuthData_FL(client, webXMLRealm, managerUser, managerPassword,
                                         FORM_LOGIN_SERVLET_URL, false);
    }

    /**
     * Test the user constraint CONFIDENTIAL, access on https port should
     * be successful
     * Tests property setting: ssoRequiresSSL="true"
     *
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permitted access to the protected servlet
     * <LI> Successful access to the servlet and returns an SSO cookie since SSL
     * <LI> is required and is used
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_ssoRequiresSSLTrueWithHTTPS() throws Exception {
        testConfig.modifyWebAppSecurity(SSO_REQUIRES_SSL_CONFIG_OPTIONS_FILE);

        // log in to the protected servlet
        HttpClient client = new DefaultHttpClient();
        Header cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, managerUser, managerPassword,
                                                               HTTPS_FORM_LOGIN_SECURE_SERVLET_URL, true);

        // Get cookie and verify cookie name and Secure set
        String cookieReturned = verifyCookieName(cookieHeader, DEFAULT_COOKIE_NAME);
        Log.info(thisClass, name.getMethodName(), "cookie returned: " + cookieReturned);
        verifySecureCookies(cookieHeader, true);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoCookieName="MyToken"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedCookieName() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        assertCookieNameAfterBasicAuthLogin(BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET, UPDATED_COOKIE_NAME);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> httpOnlyCookies="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedHttpOnlyCookies() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

        Header cookieHeader = authenticateWithValidAuthData_BA(webXMLRealm, validUser, validPassword,
                                                               BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET, true);

        verifyHttpOnlyCookies(cookieHeader, false);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be the <realm-name> specified in the web.xml file
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedAuthenticationRealmWebXML() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        checkRealmName(BASIC_AUTH_SERVLET_URL, webXMLRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be "INVALID" specified in web.xml since
     * <LI> the login-config specified in the web.xml file is invalid and falls back to Basic Auth
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedAuthenticationRealmInvalidLoginConfig() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        checkRealmName(LOGIN_CONFIG_INVALID_URL, invalidRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be the user registry "realm" specified in server.xml since
     * <LI> no login-config is specified in the web.xml file
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedAuthenticationRealmNoLoginConfig() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        checkRealmName(LOGIN_CONFIG_NONE_URL, userRegistryRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be the user registry "realm" specified in server.xml since
     * <LI> no there is no realm-name in the web.xml file
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedAuthenticationRealmNoRealmName() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        checkRealmName(LOGIN_CONFIG_NO_REALM_NAME_URL, userRegistryRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> displayAuthenticationRealm="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A basic auth prompt when accessing the protected page
     * <LI> The realm displayed should be "Client Cert Authentication" specified in the web.xml file
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedAuthenticationRealmClientCert() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        checkRealmName(CLIENT_CERT_URL, clientCertRealm);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using the FQDN.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoDomainNames="invalid.com|ibm.com"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> The cookie should have "Domain" set to a valid domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedSSODomainName() throws Exception {
        if (fullyQualifiedDomainName.contains(ibmDomainName)) {
            testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

            assertCookieDomainAfterBasicAuthLogin(basicAuthFQDNUrl, BASIC_AUTH_SERVLET, DOMAIN_COOKIE_NAME);
        } else {
            logNoFullyQualifiedDomainName();
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using the IP address.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoDomainNames="invalid.com|ibm.com"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie because the IP address should not match the domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedSSODomainNameIPAddress() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        assertDomainNotFoundInCookieAfterBasicAuthLogin(basicAuthIPAddressUrl, BASIC_AUTH_SERVLET);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication using localhost.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoDomainNames="invalid.com|ibm.com"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> We should NOT find "Domain" in the SSO cookie because localhost should not match the domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_validateUpdatedSSODomainNameLocalHost() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
        assertDomainNotFoundInCookieAfterBasicAuthLogin(BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login using the FQDN.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoDomainNames="invalid.com|ibm.com"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login page when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> The cookie should have "Domain" set to a valid domain name
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateUpdatedSSODomainName() throws Exception {
        if (fullyQualifiedDomainName.contains(ibmDomainName)) {
            testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

            HttpClient client = new DefaultHttpClient();
            Header cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, validUser, validPassword,
                                                                   formLoginFQDNUrl, true, true);

            // Verify Domain ibm.com is found in cookie
            String cookieResult = verifyCookieName(cookieHeader, DOMAIN_COOKIE_NAME);
            assertTrue("Expect ibm.com in cookie but got: " + cookieResult, cookieResult.contains(ibmDomainName));

            formLogout(client, formLoginFQDNPath);

            // Rerun the test to make sure you are prompted after form logout
            cookieHeader = authenticateWithValidAuthData_FL(client, webXMLRealm, validUser, validPassword,
                                                            formLoginFQDNUrl, true, true);

            // Verify Domain ibm.com is found in cookie
            cookieResult = verifyCookieName(cookieHeader, DOMAIN_COOKIE_NAME);
            assertTrue("Expect ibm.com in cookie but got: " + cookieResult, cookieResult.contains(ibmDomainName));
        } else {
            logNoFullyQualifiedDomainName();
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> ssoCookieName="MyToken"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form.
     * <LI> Attempt to go to the servlet again after logging out,
     * <LI> User will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateUpdatedCookieName() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

        client.setSSOCookieName(UPDATED_COOKIE_NAME);
        String response = client.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        assertTrue(client.verifyResponse(response, validUser, FormLoginClient.IS_EMPLOYEE_ROLE, FormLoginClient.NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> httpOnlyCookies="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form.
     * <LI> Attempt to go to the servlet again after logging out,
     * <LI> User will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateUpdatedHttpOnlyCookies() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

        Header cookieHeader2 = null;

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        cookieHeader2 = authenticateWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                                         FORM_LOGIN_SERVLET_URL, true);

        // Get cookie
        String cookieReturned = verifyCookieName(cookieHeader2, UPDATED_COOKIE_NAME);
        Log.info(thisClass, name.getMethodName(), "cookie returned: " + cookieReturned);

        // Verify value of httpOnlyCookies
        verifyHttpOnlyCookies(cookieHeader2, false);
    }

    /**
     * TODO: Add test for logoutPageRedirectDomainList after function is completed
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout, which redirects to login page
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> allowLogoutPageRedirectToAnyHost="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form, which redirects to login page
     * <LI> Successful logged out with logout3 form, which redirects to external page
     * <LI>
     * <LI>Note: This test uses httpunit for form logout since httpclient does not support form logout with 2 logout forms
     * <LI>Note: If this test fails, verify that external page still exists: http://www.w3.org/Protocols/HTTP/AsImplemented.html
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_LoginThenLogoutRedirectUpdated() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

        // Try a logout to the login page
        client.formLogout(LogoutOption.LOGOUT_TO_LOGIN_PAGE, validUser, validPassword);

        // Now try with an external page: "http://www.w3.org/Protocols/HTTP/AsImplemented.html"
        // Verify that redirect goes to the external page
        client.formLogout(LogoutOption.LOGOUT_OFF_HOST_SUCCESS, validUser, validPassword);
    }

    /**
     * TODO: Change to use session timeout when available
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password
     * <LI>Server stopped and restarted.
     * <LI>Attempt to access the servlet again.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> logoutOnHttpSessionExpire="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> Server stopped and restarted.
     * <LI> After server restart, user is required to log in before accessing the protected servlet.
     * </OL>
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_logoutOnHttpSessionExpireUpdated() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        authenticateWithValidAuthDataSessionExpires_FL(client1, webXMLRealm, validUser, validPassword,
                                                       FORM_LOGIN_SERVLET_URL, true);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> preserveFullyQualifiedReferrerUrl="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form.
     * <LI> Attempt to go to the servlet again after logging out,
     * <LI> User will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateUpdatedReferrerURL() throws Exception {
        testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);

        // log in to the protected servlet
        DefaultHttpClient client1 = new DefaultHttpClient();
        authenticateLogInWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                              FORM_LOGIN_SERVLET_URL, true);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> postParamSaveMethod="Session"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    // TODO: Remove @ExpectedFFDC when defect 55844 is resolved and uncomment out test
    /*
     * @Test
     *
     * @ExpectedFFDC("java.io.IOException")
     * public void testConfigOptionsFL_validateUpdatedPostParamSessionMethod() throws Exception {
     *
     *
     * testConfig.modifyWebAppSecurity(CONFIG_OPTIONS_FILE);
     * // setServerConfiguration(CONFIG_OPTIONS_FILE, true);
     *
     * // log in to the protected servlet
     * HttpClient client1 = new DefaultHttpClient();
     * String inputText = "Form login servlet test";
     * authenticatePostParamWithValidAuthData_FL(client1, validRealm, validUser, validPassword, FORM_LOGIN_SERVLET_URL, inputText, false);
     *
     *
     * }
     */

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> postParamSaveMethod="None"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateUpdatedPostParamNone() throws Exception {
        testConfig.modifyWebAppSecurity(POST_PARAM_NONE_CONFIG_OPTIONS_FILE);

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        String inputText = "Form login servlet test";
        authenticatePostParamWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                                  FORM_LOGIN_SERVLET_URL, inputText, false);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> postParamSaveMethod="Cookie"
     * <LI> postParamCookieSize="100"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_validateUpdatedPostParamCookieSize() throws Exception {
        testConfig.modifyWebAppSecurity(POST_PARAM_SIZE_CONFIG_OPTIONS_FILE);

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        String inputText = "Support web application security configurable options that are already defined in the webcontainer.security metatype.properties.";
        authenticatePostParamWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                                  FORM_LOGIN_SERVLET_URL, inputText, false);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout, which redirects to external page defined in domain names list
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> logoutPageRedirectDomainNames="w3.org"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form, which redirects to login page
     * <LI> Successful logged out with logout3 form, which redirects to external page
     * <LI>
     * <LI>Note: This test uses httpunit for form logout since httpclient does not support form logout with 2 logout forms
     * <LI>Note: If this test fails, verify that external page still exists: http://www.w3.org/Protocols/HTTP/AsImplemented.html
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_LoginThenLogoutRedirectValidDomain() throws Exception {
        testConfig.modifyWebAppSecurity(DOMAIN_NAMES_CONFIG_OPTIONS_FILE);

        client.setSSOCookieName(DEFAULT_COOKIE_NAME);

        // Try a logout to the login page
        client.formLogout(LogoutOption.LOGOUT_TO_LOGIN_PAGE, validUser, validPassword);

        // Now try with an external page: "http://www.w3.org/Protocols/HTTP/AsImplemented.html"
        // Verify that redirect goes to the external page
        client.formLogout(LogoutOption.LOGOUT_OFF_HOST_SUCCESS, validUser, validPassword);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout, which redirects to login page since external page not defined in domain names
     * list
     * <LI>Attempt to access the servlet again
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> logoutPageRedirectDomainNames="austin.ibm.com|raleigh.ibm.com"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form, which redirects to login page
     * <LI> Successful logged out with logout3 form, which redirects to default logout page
     * <LI>
     * <LI>Note: If this test fails, verify that external page still exists: http://www.w3.org/Protocols/HTTP/AsImplemented.html
     * </OL>
     */
    @Test
    public void testConfigOptionsFL_LoginThenLogoutRedirectInvalidDomain() throws Exception {
        testConfig.modifyWebAppSecurity(INVALID_DOMAIN_NAMES_CONFIG_OPTIONS_FILE);

        // Try a logout to the login page
        client.formLogout(LogoutOption.LOGOUT_TO_LOGIN_PAGE, validUser, validPassword);

        // Now try with an external page: "http://www.w3.org/Protocols/HTTP/AsImplemented.html"
        // Verify that redirect goes back to the default logout page
        // instead of external page
        client.formLogout(LogoutOption.LOGOUT_OFF_HOST_FAIL, validUser, validPassword);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password.
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> 1) singleSignonEnabled = false
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No cookie is present in the response.
     * </OL>
     */
    @Test
    public void testConfigOptionsBasicAuth_noSSO() throws Exception {
        testConfig.modifyWebAppSecurity(NO_SSO_CONFIG_OPTIONS_FILE);

        authenticateWithValidAuthData_BA(webXMLRealm, validUser, validPassword,
                                         BASIC_AUTH_SERVLET_URL, BASIC_AUTH_SERVLET, false);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password with single signon not enabled
     * <LI>Verify that the following configurable options defined in server.xml are being used:
     * <LI> 1) singleSignonEnabled = false
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> No cookie is present in the response.
     * <LI> A valid userId and password not permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testConfigOptionsFL__noSSO() throws Exception {
        testConfig.modifyWebAppSecurity(NO_SSO_CONFIG_OPTIONS_FILE);

        // log in to the protected servlet
        HttpClient client1 = new DefaultHttpClient();
        authenticateWithValidAuthData_FL(client1, webXMLRealm, validUser, validPassword,
                                         FORM_LOGIN_SERVLET_URL, false);
    }

    //----------------------------------
    // utility methods
    //----------------------------------

    /**
     * Asserts that the cookie for the specified cookie name is found in the
     * cookie header after a basic authentication login.
     */
    private void assertCookieNameAfterBasicAuthLogin(String url, String servletName, String cookieName) {
        Header cookieHeader = authenticateWithValidAuthData_BA(webXMLRealm, validUser, validPassword, url, servletName, true);
        verifyCookieName(cookieHeader, cookieName);
    }

    /**
     * Asserts that the cookie domain name is ibm.com after basic authentication login.
     */
    private void assertCookieDomainAfterBasicAuthLogin(String url, String servletName, String cookieName) {
        Header cookieHeader = authenticateWithValidAuthData_BA(webXMLRealm, validUser, validPassword, url, servletName, true);
        String cookieResult = verifyCookieName(cookieHeader, cookieName);
        assertTrue("Expect ibm.com in cookie but got: " + cookieResult, cookieResult.contains(ibmDomainName));
    }

    /**
     * Asserts that the Domain is not found in the cookie after a basic authentication login.
     */
    private void assertDomainNotFoundInCookieAfterBasicAuthLogin(String url, String servletName) {
        Header cookieHeader = authenticateWithValidAuthData_BA(webXMLRealm, validUser, validPassword, url, servletName, true);
        assertDomainNotFoundInCookie(cookieHeader);
    }

    /**
     * Asserts that the Domain is not found in cookie.
     */
    private void assertDomainNotFoundInCookie(Header cookieHeader) {
        String cookieResult = testHelper.getCookieValue(cookieHeader, DOMAIN_COOKIE_NAME);
        assertNull("Expect no Domain in cookie but got: " + cookieResult, cookieResult);
    }

    /**
     * This is an internal method used by the test methods that test the
     * configurable options, where a successful basic auth is expected when accessing the servlet.
     */
    private Header authenticateWithValidAuthData_BA(String realm, String user, String password, String url, String urlString, boolean cookieExpected) {
        String methodName = "authenticateWithValidAuthData_BA";
        String debugData = "URL:" + url + "; userId:[" + user + "]" + "; password:[" + password + "]";
        Log.info(thisClass, name.getMethodName(), "Verifying URL: " + url + " with user: " + user + " and password: " + password);
        Log.info(thisClass, methodName, debugData);

        CommonTestHelper testHelper = new CommonTestHelper();
        Header cookieHeader = null;
        HttpResponse response = null;
        try {
            response = testHelper.executeGetRequestWithAuthCreds(url, user, password, server);
            String authResult = response.getStatusLine().toString();
            Log.info(thisClass, methodName, "BasicAuth result: " + authResult);
            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + content);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "BasicAuth response: " + content);

            // Verify we get the correct response
            assertTrue("Expecting 200, got: " + authResult, authResult.contains("200"));
            assertTrue("Expected output, " + urlString + ", not returned: " + content, content.contains(urlString));

            // Verify the cookies
            cookieHeader = response.getFirstHeader("Set-Cookie");
            if (cookieExpected) {
                if (cookieHeader != null) {
                    Log.info(thisClass, methodName, "Cookie value: " + cookieHeader.getValue());
                } else {
                    fail("Expecting Set-Cookie in response and none was present");
                }
            } else {
                // Verify that no cookie for LTPA token is present
                String ssoCookie = testHelper.getCookieValue(cookieHeader, "MyToken");
                if (ssoCookie != null) {
                    Log.info(thisClass, methodName, "Cookie value: " + ssoCookie);
                    fail("Expecting LtpaToken to be null but cookie was present");
                }
            }
        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData, "caught unexpected exception for: ");
        }
        return cookieHeader;
    }

    /**
     * This is an internal method used by the test methods that test the
     * configurable options, where a successful form login is expected when accessing the servlet.
     */
    Header authenticateWithValidAuthData_FL(HttpClient client, String realm,
                                            String user, String password,
                                            String url, boolean cookieExpected) {
        return authenticateWithValidAuthData_FL(client, realm, user, password, url,
                                                cookieExpected, false);
    }

    private Header authenticateWithValidAuthData_FL(HttpClient client, String realm, String user, String password, String url, boolean cookieExpected,
                                                    boolean fullDomainNameExpected) {
        String methodName = "authenticateWithValidAuthData_FL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Header cookieHeader = null;
        CommonTestHelper testHelper = new CommonTestHelper();

        try {
            String postURL = null;
            String protocol = "http";
            int port = server.getHttpDefaultPort();
            if (url.contains("https")) {
                protocol = "https";
                port = server.getHttpDefaultSecurePort();
                // Setup for SSL
                SSLHelper.establishSSLContext(client, port, server);
            }

            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + content);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + response.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "Expecting form login page. Get response: " + content);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, content.contains(FORMLOGINJSP));

            // Post method to login
            if (fullDomainNameExpected) {
                postURL = protocol + "://" + fullyQualifiedDomainName + ":" + port + "/formlogin/j_security_check";
            } else {
                postURL = protocol + "://" + server.getHostname() + ":" + port + "/formlogin/j_security_check";
            }
            HttpPost postMethod = new HttpPost(postURL);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            assertTrue("Expecting form login getStatusCode 302", postResponse.getStatusLine().getStatusCode() == 302);

            // Get cookie
            cookieHeader = postResponse.getFirstHeader("Set-Cookie");
            if (!cookieExpected) {
                // Verify that no cookie for LTPA token is present
                String ssoCookie = testHelper.getCookieValue(cookieHeader, DEFAULT_COOKIE_NAME);
                if (ssoCookie != null) {
                    Log.info(thisClass, methodName, "Cookie value: " + ssoCookie);
                    fail("Expecting LtpaToken to be null but cookie was present");
                }
            }
            EntityUtils.consume(postResponse.getEntity());

        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData, "Caught unexpected Exception instead of successful form login: ");
        }
        return cookieHeader;
    }

    /**
     * This is an internal method used by the test methods that test the
     * configurable options, where a successful access of form login jsp is expected when accessing the servlet.
     */
    private Header authenticateLogInWithValidAuthData_FL(DefaultHttpClient client, String realm, String user, String password, String url, boolean hostNameExpected) {
        String methodName = "authenticateLogInWithValidAuthData_FL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Header cookieHeader = null;
        CommonTestHelper testHelper = new CommonTestHelper();

        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + content);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + response.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "Expecting form login page. Get response: " + content);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, content.contains(FORMLOGINJSP));

            // Get the cookie header for the request
            Cookie wasReqURLCookie = null;
            for (Cookie cookie : client.getCookieStore().getCookies()) {
                Log.info(thisClass, methodName, "store:" + cookie);
                Log.info(thisClass, methodName, "store:" + cookie.getName());
                if ("WASReqURL".equals(cookie.getName())) {
                    wasReqURLCookie = cookie;
                    break;
                }
            }
            // Verify the cookie name
            if (wasReqURLCookie != null) {
                if (hostNameExpected) {
                    assertTrue("Did not find expected hostname in WASReqURL cookie: " + wasReqURLCookie, wasReqURLCookie.getValue().contains(server.getHostname()));
                } else {
                    assertTrue("Unexpected hostname found in WASReqURL cookie: " + wasReqURLCookie, !wasReqURLCookie.getValue().contains(server.getHostname()));
                }
            } else {
                fail("Expected cookie WASReqURL not found in the request");
            }

        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData, "Caught unexpected Exception instead of successful form login: ");
        }
        return cookieHeader;
    }

    /**
     * This is an internal method used by the test methods that test the
     * configurable options, where a successful access of form login jsp is expected when accessing the servlet.
     */
    private void authenticatePostParamWithValidAuthData_FL(HttpClient client, String realm, String user, String password, String url, String inputText, boolean cookieExpected) {
        String methodName = "authenticatePostParamWithValidAuthData_FL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Header cookieHeader = null;
        CommonTestHelper testHelper = new CommonTestHelper();

        try {
            HttpPost postMethod = new HttpPost(url);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            nvps.add(new BasicNameValuePair("j_description", inputText));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            Log.info(thisClass, methodName, "Expecting getStatusCode 302. getMethod.getStatusCode(): " + postResponse.getStatusLine().getStatusCode());

            // Get the cookie header and the WASPostParam cookie
            cookieHeader = postResponse.getFirstHeader("Set-Cookie");
            String postParamCookie = testHelper.getCookieValue(cookieHeader, "WASPostParam");
            if (postParamCookie != null) {
                if (cookieExpected) {
                    assertTrue("Did not find expected input data in WASPostParam cookie: " + postParamCookie, !postParamCookie.isEmpty());
                } else {
                    assertTrue("Unexpected input data found in WASPostParam cookie: " + postParamCookie, postParamCookie.isEmpty());
                }
            } else {
                if (cookieExpected) {
                    fail("Expected cookie WASPostParam not found in the request");
                }
            }

        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData, "Caught unexpected Exception instead of successful form login: ");
        }
    }

    /**
     * This is an internal method to verify the cookie name
     */
    private String verifyCookieName(Header cookieHeader, String cookieName) {
        String ssoCookie = null;
        CommonTestHelper testHelper = new CommonTestHelper();

        // Verify the cookie name
        ssoCookie = testHelper.getCookieValue(cookieHeader, cookieName);
        if (ssoCookie == null) {
            fail("Expected LTPA token not found in the cookie after login");
        }
        return ssoCookie;
    }

    /**
     * This is an internal method to check the value of httpOnlyCookies
     */
    private void verifyHttpOnlyCookies(Header cookieHeader, boolean expectedValue) {
        // Check value of httpOnlyCookies
        boolean isHttpOnly = false;
        if (cookieHeader != null) {
            isHttpOnly = cookieHeader.toString().contains("HttpOnly");
        }
        assertTrue("Expecting httpOnlyCookies to be " + expectedValue, isHttpOnly == expectedValue);
    }

    private void verifySecureCookies(Header cookieHeader, boolean expectedValue) {
        // Check value of Secure
        boolean isSecure = false;
        if (cookieHeader != null) {
            isSecure = cookieHeader.toString().contains("Secure");
        }
        assertTrue("Expecting Secure to be " + expectedValue, isSecure == expectedValue);
    }

    /**
     * This is an internal method used by the test methods that test the
     * configurable options, where a successful form login is expected when accessing the servlet.
     */
    private void authenticateWithValidAuthDataSessionExpires_FL(HttpClient client, String realm, String user, String password, String url, boolean challengeExpected) {
        String methodName = "authenticateWithValidAuthDataSessionExpires_FL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        CommonTestHelper testHelper = new CommonTestHelper();

        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(thisClass, methodName, "HttpGet" + url + " result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            String response = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "Expecting getStatusCode 200. Received: " + getResponse.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "Expecting form login page. HttpGet response: " + response);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, response.contains(FORMLOGINJSP));

            // Post method to login
            HttpPost postMethod = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/j_security_check");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            assertTrue("Expecting form login getStatusCode 302", postResponse.getStatusLine().getStatusCode() == 302);

            // Verify redirect
            Header header = postResponse.getFirstHeader("Location");
            String location = header.getValue();
            Log.info(thisClass, methodName, "Redirect location: " + location);
            EntityUtils.consume(postResponse.getEntity());

            HttpGet getMethodRedirect = new HttpGet(location);
            HttpResponse redirectResponse = client.execute(getMethodRedirect);
            authResult = redirectResponse.getStatusLine().toString();
            Log.info(thisClass, methodName, "HttpGet(" + location + ") result: " + authResult);
            entity = redirectResponse.getEntity();
            String getResponseRedirect = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "Redirected location expecting getStatusCode 200. Received: " + redirectResponse.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "Response for redirected location: " + getResponseRedirect);
            // Verify redirect to servlet
            assertTrue("Did not hit expected servlet: " + SERVLETNAME, getResponseRedirect.contains(SERVLETNAME));

            Log.info(thisClass, methodName, "Restarting server...");

            // Stop and restart server
            server.stopServer();
            server.startServer(true);
            server.waitForStringInLog("CWWKF0008I");

            if (challengeExpected) {
                Log.info(thisClass, methodName, "Challenge expected. Should see " + FORMLOGINJSP);
                // Try to get the same page via the originalRequest. We should be challenged
                // since login should not have been preserved.
                // Get method on form login page
                getMethod = new HttpGet(url);
                getResponse = client.execute(getMethod);
                entity = getResponse.getEntity();
                response = EntityUtils.toString(entity);
                Log.info(thisClass, methodName, "Servlet response: " + response);
                EntityUtils.consume(entity);
                Log.info(thisClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
                Log.info(thisClass, methodName, "Expecting form login page. Get response: " + response);

                // Verify we get the form login JSP
                assertTrue("Did not find expected form login page: " + FORMLOGINJSP, response.contains(FORMLOGINJSP));
            } else {
                Log.info(thisClass, methodName, "Challenge not expected. Should not see " + FORMLOGINJSP);
                // lets try to get the same page via the originalRequest. We should not be challenged
                // since we already logged in and login should have been preserved.
                getMethod = new HttpGet(url);
                getResponse = client.execute(getMethod);
                entity = getResponse.getEntity();
                response = EntityUtils.toString(entity);
                Log.info(thisClass, methodName, "Servlet response: " + response);
                EntityUtils.consume(entity);
                Log.info(thisClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
                Log.info(thisClass, methodName, "Expecting form login page. Get response: " + response);

                // Verify we indeed hit the form login servletUrl the second time
                assertTrue("Did not hit expected servlet: " + SERVLETNAME, getResponseRedirect.contains(SERVLETNAME));
            }

        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData, "Caught unexpected Exception instead of successful form login: ");
        }
    }

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML, boolean waitForApplicationUpdateMessage, boolean waitForPropertiesUpdateMessage) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Use server.xml with non-default config options defined
            CommonTestHelper testHelper = new CommonTestHelper();
            testHelper.setServerConfiguration(server, serverXML, waitForApplicationUpdateMessage, waitForPropertiesUpdateMessage);
            serverConfigurationFile = serverXML;
        }
    }

    /**
     * This is an internal method used by the test methods
     * that checks for the realm name in the Basic Auth prompt
     */
    private void checkRealmName(String url, String realmCheck) {
        String methodName = "checkRealmName";
        URL javaURL = null;
        HttpURLConnection con = null;
        try {
            javaURL = new URL(url);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Invalid URL " + url);
        }
        try {
            con = (HttpURLConnection) javaURL.openConnection();
            con.connect();
            String realmName = con.getHeaderField("WWW-Authenticate");
            Log.info(thisClass, methodName, "realmName: " + realmName);
            if (realmName != null) {
                assertTrue("Expecting " + realmCheck + " for realmName, got: " + realmName, realmName.contains(realmCheck));
            } else {
                fail("realmName returned is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed to access the URL " + url);
        }
    }

    /**
     * This is an internal method used for form logout
     */
    private void formLogout(HttpClient client, String servletURL) {
        String methodName = "formLogout";

        try {
            // Validate we have the form logout page
            HttpGet getMethod = new HttpGet(servletURL + "/logout.html");
            HttpResponse response = client.execute(getMethod);

            // Verify we got the form logout page
            String content = EntityUtils.toString(response.getEntity());
            Log.info(thisClass, methodName, "getMethod.getStatusCode(): " + response.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "Get response for logout page: " + response);
            assertEquals("The response code was not 200 as expected", 200, response.getStatusLine().getStatusCode());
            assertTrue("Form logout page not found: " + FORM_LOGOUT_PAGE, content.contains(FORM_LOGOUT_PAGE));
            EntityUtils.consume(response.getEntity());

            // Post method to logout
            Log.info(thisClass, methodName, "logout URL: " + servletURL + "/ibm_security_logout");
            HttpPost postMethod = new HttpPost(servletURL + "/ibm_security_logout");

            // Set up the postMethod based on the action we wanted
            response = client.execute(postMethod);
            Log.info(thisClass, methodName, "postMethod.getStatusCode(): " + response.getStatusLine().getStatusCode());
            content = EntityUtils.toString(response.getEntity());
            Log.info(thisClass, methodName, "Form logout getResponseBodyAsString: " + content);
            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
        }

    }
}
