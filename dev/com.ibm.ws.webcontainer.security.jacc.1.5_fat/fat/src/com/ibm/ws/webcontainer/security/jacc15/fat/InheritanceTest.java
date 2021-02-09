/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Performs RunAs tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class InheritanceTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.loginmethod");
    private static final Class<?> thisClass = InheritanceTest.class;
    @Rule
    public TestName testName = new TestName();

    private static class expectedSettings {
        public String method = null;
        public String user = null;
        public int status1 = 0;
        public int status2 = 0;
        public String urlBase = null;
        public String servlet = null;
        public String methodServlet = null;

        public expectedSettings(String theMethod, String theUser, int theStatus1, int theStatus2, String theUrlBase, String theServlet, String mServlet) {
            method = theMethod;
            user = theUser;
            status1 = theStatus1;
            status2 = theStatus2;
            urlBase = theUrlBase;
            servlet = theServlet;
            methodServlet = mServlet;
        }
    }

    private static class inputSettings {
        public String method = null;
        public String user = null;
        public String password = null;
        public String url = null;

        public inputSettings(String theMethod, String theUser, String thePassword, String theUrl) {
            method = theMethod;
            user = theUser;
            password = thePassword;
            url = theUrl;
        }
    }

    protected final boolean debug = true;
    // Keys to help readability of the test
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;
    private static final String REMOTE_USER_HEADER = "getRemoteUser: ";
    private static final String USER_PRINCIPAL_HEADER = "getUserPrincipal: ";
    private static final String USER_PRINCIPAL_NAME_HEADER = "getUserPrincipal().getName(): ";
    private static final String WSPRINCIPAL = "WSPrincipal:";
    private static final String EMPLOYEE_ROLE_HEADER = "isUserInRole(Employee): ";
    private static final String MANAGER_ROLE_HEADER = "isUserInRole(Manager): ";
    protected final int HTTP_OK = 200;
    protected final int HTTP_Found = 302;
    protected final int HTTP_Unauthorized = 401;
    protected final int HTTP_Forbidden = 403;
    protected final int HTTP_Not_Found = 404;
    protected final int HTTP_Not_Allowed = 405;
    private static final String doPost = "doPost";
    private static final String doPut = "doPut";
    private static final String doGet = "doGet";
    private static final String doCustom = "CUSTOM";
    private static final String service = "service";

    // Users defined by role
    protected final static String employeeUser = "user1";
    protected final static String employeePassword = "user1pwd";
    protected final static String managerUser = "user2";
    protected final static String managerPassword = "user2pwd";

    // Servlet settings
    private static String inheritUrlBase;
    private static String inheritSSLUrlBase;
    protected final static String parentServlet = "InheritanceParent";
    private static String parentServletUrl;
    private static String parentServletSSLUrl;
    protected final static String child1Servlet = "InheritanceChild1";
    private static String child1ServletUrl;
    // private static String child1ServletSSLUrl;
    protected final static String child2Servlet = "InheritanceChild2";
    private static String child2ServletUrl;
    private static String child2ServletSSLUrl;
    protected final static String child3Servlet = "InheritanceChild3";
    private static String child3ServletUrl;
    private static String child3ServletSSLUrl;
    protected final static String child4Servlet = "InheritanceChild4";
    private static String child4ServletUrl;
    //private static String child4ServletSSLUrl;
    protected final static String rootContext = "/inheritance";

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation("loginmethod");

        JACCFatUtils.installJaccUserFeature(server);
        JACCFatUtils.transformApps(server, "loginmethod.ear");

        server.startServer(true);
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I"));

        Log.info(thisClass, "setUp", "server started");
        // get the base http & https portion of the urls
        inheritUrlBase = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + rootContext;
        inheritSSLUrlBase = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + rootContext;
        // Parent servlet
        parentServletUrl = inheritUrlBase + "/" + parentServlet;
        parentServletSSLUrl = inheritSSLUrlBase + "/" + parentServlet;
        // Child1 servlet
        child1ServletUrl = inheritUrlBase + "/" + child1Servlet;
        //child1ServletSSLUrl = inheritSSLUrlBase + "/" + child1Servlet;
        // Child2 servlet
        child2ServletUrl = inheritUrlBase + "/" + child2Servlet;
        child2ServletSSLUrl = inheritSSLUrlBase + "/" + child2Servlet;
        // Child3 servlet
        child3ServletUrl = inheritUrlBase + "/" + child3Servlet;
        child3ServletSSLUrl = inheritSSLUrlBase + "/" + child3Servlet;
        // Child4 servlet
        child4ServletUrl = inheritUrlBase + "/" + child4Servlet;
        //child4ServletSSLUrl = inheritSSLUrlBase + "/" + child4Servlet;

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
        }
    }

    /**
     *
     * Scenario - Parent Servlet
     *
     * Web.xml - contains 1 mapping (url = "/InheritanceParent") and
     * 1 role ((AllAuthenticated")
     *
     * @WebServlet(name = "InheritanceParent", urlPatterns = { "/InheritanceParent" })
     * @ServletSecurity(@HttpConstraint(rolesAllowed = "Manager", transportGuarantee = TransportGuarantee.CONFIDENTIAL))
     *                                               Methods - doGet, doCustom and service
     *                                               - verify access to methods that exist in "Parent" if user specified IS in the required role
     *                                               - verify no access to methods that are not defined (but supported (doPut))
     *                                               - verify https is required - calls to http are redirected (either dynamically, or via the return of a 302
     *                                               - verify access to methods is not allowed if user specified is NOT in the required role
     *
     */

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using http on the parent servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call has automatically been redirected from http to https
     * <LI> doGet of the Parent servlet is invoked (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestParentGet_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, managerUser, managerPassword, parentServletUrl);
        // we expect to use the doGet method, logged in as mangagerUser, getting a status code of HTTP_OK, using HTTPS(SSL)
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using https on the parent servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call uses https
     * <LI> doGet of the Parent servlet is invoked (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestParentGet_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, managerUser, managerPassword, parentServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using https on the parent servlet
     * <LI>SSL setup is performed
     * <LI>A non- mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> doGet of the Parent servlet is NOT invoked (status code 403)
     * <LI> we're not authorized
     * </OL>
     */
    @Test
    public void testInheritTestParentGet_nonMgr() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, employeeUser, employeePassword, parentServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, employeeUser, HTTP_Forbidden, HTTP_Forbidden, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPost using http on the parent servlet
     * <LI>doPost does not exist in the parent servlet, it is found in the base servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call is NOT automatically been redirected from http to https
     * <LI> retry using the redirect url - succeeds (status code 302, then 200 on retry)
     * <LI> service of the Parent servlet is invoked (as doPost does not exist in Parent, it is in the Base and is run there) (status code 302, then 200)
     * <LI> user and role info is correct
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestParentPost_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, managerUser, managerPassword, parentServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(service, managerUser, HTTP_Found, HTTP_OK, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPost using https on the parent servlet
     * <LI>doPost does not exist in the parent servlet, it is found in the base servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call uses https
     * <LI> service of the Parent servlet is invoked (as doPost does not exist in Parent, it is in the Base and is run there) (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestParentPost_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, managerUser, managerPassword, parentServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(service, managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPut using http on the parent servlet
     * <LI>doPut does not exist in the parent servlet, nor the base servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call is NOT automatically been redirected from http to https
     * <LI> call results in "not allowed/not found" (status code 405)
     * </OL>
     */
    @Test
    public void testInheritTestParentPut_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPut, managerUser, managerPassword, parentServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPut, managerUser, HTTP_Found, HTTP_Not_Allowed, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPut using https on the parent servlet
     * <LI>doPut does not exist in the parent servlet, nor the base servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call uses https
     * <LI> call results in "not allowed/not found" (status code 405)
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestParentPut_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPut, managerUser, managerPassword, parentServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPut, managerUser, HTTP_Not_Allowed, HTTP_Not_Allowed, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doCustom using http on the parent servlet
     * <LI>doCustom exists in the parent servlet,
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call is NOT automatically been redirected from http to https
     * <LI> retry using the redirect url - succeeds
     * <LI> doCustom of the Parent servlet is invoked (status code 302, then 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestParentCustom_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doCustom, managerUser, managerPassword, parentServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings("doCustom", managerUser, HTTP_Found, HTTP_OK, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doCustom using https on the parent servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call uses https
     * <LI> doCustom of the Parent servlet is invoked (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestParentCustom_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doCustom, managerUser, managerPassword, parentServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings("doCustom", managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, parentServlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     *
     * Scenario - Child1 Servlet
     *
     * Web.xml - contains 1 mapping (url = "/InheritanceParent") and
     * 1 role ((AllAuthenticated")
     *
     * no URL nor security defined
     * Methods - doPost, doCustom
     * (inherits role of manager and trustguarentee of confidentiality, but not the url)
     *
     * - Ensure that we can not access anything under Child1 - either defined methods, or inherited
     * - All tests should return a 404 status code
     *
     */

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using http on the Child1 servlet
     * <LI>doGet does NOT exist in the Child1 servlet, but does exist in Parent
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call fails because the inheritanceChild1 servlet URL does NOT exist. (status code 404)
     * <LI> Test throws an exception (file not found)
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     *
     * @ExpectedFFDC("java.io.FileNotFoundException")
     */
    public void testInheritTestChild1Get() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, managerUser, managerPassword, child1ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, managerUser, HTTP_Not_Found, HTTP_Not_Found, inheritUrlBase, child1Servlet, child1Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPost using http on the Child1 servlet
     * <LI>doPost does exist in the Child1 servlet
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call fails because the inheritanceChild1 servlet URL does NOT exist. (status code 404)
     * <LI> Test throws an exception (file not found)
     * </OL>
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testInheritTestChild1Post() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, managerUser, managerPassword, child1ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPost, managerUser, HTTP_Not_Found, HTTP_Not_Found, inheritUrlBase, child1Servlet, child1Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doCustom using http on the Child1 servlet
     * <LI>doCustom does exist in the Child1 servlet
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call fails because the inheritanceChild1 servlet URL does NOT exist. (status code 404)
     * <LI> Test throws an exception (file not found)
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     *
     * @ExpectedFFDC("java.io.FileNotFoundException")
     */
    public void testInheritTestChild1Custom() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doCustom, managerUser, managerPassword, child1ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings("doCustom", managerUser, HTTP_Not_Found, HTTP_Not_Found, inheritUrlBase, child1Servlet, child1Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     *
     * Scenario - Child2 Servlet
     *
     * Web.xml - contains 1 mapping (url = "/InheritanceParent") and
     * 1 role ((AllAuthenticated")
     *
     * @WebServlet(urlPatterns = { "/InheritanceChild2" })
     *                         (no security constraints)
     *                         Methods - doPost and doCustom
     *
     *                         (inherits role of manager and trustguarentee of confidentiality, but not the url)
     *                         - verify access to methods that exist in "Child2" if user specified IS in the required role
     *                         - verify access to methods that don't exist in "Child2", but do exist in the Parent
     *                         - verify no access to methods that are not defined (but supported (doPut))
     *                         - verify https is required - calls to http are redirected (either dynamically, or via the return of a 302)
     *                         - verify access to methods is not allowed if user specified is NOT in the required role
     *
     */

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using http on the Child2 servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call has automatically been redirected from http to https
     * <LI> doGet of the Parent servlet is invoked (as no doGet exists in Child2) (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestChild2Get_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, managerUser, managerPassword, child2ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, child2Servlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using https on the Child2 servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call uses https
     * <LI> doGet of the Parent servlet is invoked (as no doGet exists in Child2) (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestChild2Get_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, managerUser, managerPassword, child2ServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, child2Servlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using https on the Child2 servlet
     * <LI>SSL setup is performed
     * <LI>A non- mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> doGet of the Parent servlet is NOT invoked (status code 403)
     * <LI> we're not authorized
     * </OL>
     */
    @Test
    public void testInheritTestChild2Get_nonMgr() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, employeeUser, employeePassword, child2ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, employeeUser, HTTP_Forbidden, HTTP_Forbidden, inheritUrlBase, child2Servlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPost using http on the Child2 servlet
     * <LI>doPost does exist in the Child2 servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call is NOT automatically been redirected from http to https
     * <LI> retry using the redirect url - succeeds (status code 302, then 200 on retry)
     * <LI> doPost on the Child2 servlet is invoked
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestChild2Post_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, managerUser, managerPassword, child2ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPost, managerUser, HTTP_Found, HTTP_OK, inheritSSLUrlBase, child2Servlet, child2Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPost using https on the Child2 servlet
     * <LI>SSL setup is performed
     * <LI>A non- mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> doPost of the Child2 servlet is NOT invoked (status code 403)
     * <LI> we're not authorized
     * </OL>
     */
    @Test
    public void testInheritTestChild2Post_nonMgr() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, employeeUser, employeePassword, child2ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPost, employeeUser, HTTP_Found, HTTP_Forbidden, inheritSSLUrlBase, child2Servlet, child2Servlet);
        genericInheritTest(testValues, expectedValues);
        //genericInheritTest("testInheritTestChild2Post_nonMgr", doPost, doPost, child2ServletUrl, employeeUser, employeePassword, child2Servlet, child2Servlet, HTTP_Found,
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doPost using https on the Child2 servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call uses https
     * <LI> doPost of the Child2 servlet is invoked (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestChild2Post_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, managerUser, managerPassword, child2ServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPost, managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, child2Servlet, child2Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doCustom using http on the Child2 servlet
     * <LI>doCustom does exist in the Child2 servlet
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call is NOT automatically redirected from http to https
     * <LI> retry using the redirect url - succeeds (status code 302, then 200 on retry)
     * <LI> doCustom on the Child2 servlet is invoked
     * <LI> user and role info is correct
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestChild2Custom_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doCustom, managerUser, managerPassword, child2ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings("doCustom", managerUser, HTTP_Found, HTTP_OK, inheritSSLUrlBase, child2Servlet, child2Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doCustom using https on the Child2 servlet
     * <LI>SSL setup is performed
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call uses https
     * <LI> doCustom of the Child2 servlet is invoked (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestChild2Custom_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doCustom, managerUser, managerPassword, child2ServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings("doCustom", managerUser, HTTP_OK, HTTP_OK, inheritSSLUrlBase, child2Servlet, child2Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     *
     * Scenario - Child3 Servlet
     *
     * Web.xml - contains 1 mapping (url = "/InheritanceParent") and
     * 1 role ((AllAuthenticated")
     *
     * @WebServlet(urlPatterns = { "/InheritanceChild3" })
     * @ServletSecurity
     *                  Methods - doPost and doCustom
     *
     *                  (inherits no annotations and not the url)
     *                  - verify access to methods that exist in "Child3" by anyone
     *                  - verify access to methods that don't exist in "Child3", but do exist in the Parent by anyone
     *                  - verify no access to methods that are not defined (but supported (doPut))
     *                  - verify https is NOT required - calls to http are not redirected (either dynamically, or via the return of a 302)
     *
     */

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doGet using http on the Child3 servlet
     * <LI>The mgr id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call was NOT automatically redirected from http to https
     * <LI> doGet of the Parent servlet is invoked (as no doGet exists in Child3) (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestChild3Get_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, managerUser, managerPassword, child3ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, null, HTTP_OK, HTTP_OK, inheritUrlBase, child3Servlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doGet using http on the Child3 servlet
     * <LI>The employee id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call was NOT automatically redirected from http to https
     * <LI> doGet of the Parent servlet is invoked (as no doGet exists in Child3) (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestChild3Get_nonMgr() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, employeeUser, employeePassword, child3ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, null, HTTP_OK, HTTP_OK, inheritUrlBase, child3Servlet, parentServlet);
        genericInheritTest(testValues, expectedValues);

    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doGet using https on the Child3 servlet
     * <LI>No authentication is done
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call used https
     * <LI> doGet of the Parent servlet is invoked (as no doGet exists in Child3) (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestChild3Get_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, null, managerPassword, child3ServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, null, HTTP_OK, HTTP_OK, inheritSSLUrlBase, child3Servlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doPost using https on the Child3 servlet
     * <LI>No authentication is done
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call used http
     * <LI> doPost of the Child3 servlet is invoked (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestChild3Post_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, null, managerPassword, child3ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPost, null, HTTP_OK, HTTP_OK, inheritUrlBase, child3Servlet, child3Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doPost using https on the Child3 servlet
     * <LI>No authentication is done
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call used https
     * <LI> doPost of the Child3 servlet is invoked (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestChild3Post_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, null, managerPassword, child3ServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPost, null, HTTP_OK, HTTP_OK, inheritSSLUrlBase, child3Servlet, child3Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doCustom using https on the Child3 servlet
     * <LI>No authentication is done
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call used http
     * <LI> doCustom of the Child3 servlet is invoked (status code 200)
     * <LI> user and role info is not checked
     * </OL>
     */
    @Test
    public void testInheritTestChild3Custom_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doCustom, null, managerPassword, child3ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings("doCustom", null, HTTP_OK, HTTP_OK, inheritUrlBase, child3Servlet, child3Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doCustom using https on the Child3 servlet
     * <LI>No authentication is done
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call used https
     * <LI> doCustom of the Child3 servlet is invoked (status code 200)
     * <LI> user and role info is checked
     * </OL>
     */
    /*
     * DON'T run - overkill
     *
     * @Test
     */
    public void testInheritTestChild3Custom_https() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doCustom, null, managerPassword, child3ServletSSLUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings("doCustom", null, HTTP_OK, HTTP_OK, inheritSSLUrlBase, child3Servlet, child3Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * <P> Do the following:
     * <LI>Attempt to access a non-protected servlet via a call to doPost using https on the Child3 servlet
     * <LI>The employee id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call used http
     * <LI> doPost of the Child3 servlet is invoked (status code 200)
     * <LI> user and role info is not checked
     * </OL>
     */
    @Test
    public void testInheritTestChild3Post_nonMgr() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doPost, employeeUser, employeePassword, child3ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doPost, null, HTTP_OK, HTTP_OK, inheritUrlBase, child3Servlet, child3Servlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     *
     * Scenario - Child4 Servlet
     *
     * Web.xml - contains 1 mapping (url = "/InheritanceParent") and
     * 1 role ((AllAuthenticated")
     *
     * @WebServlet(urlPatterns = { "/InheritanceChild4" })
     * @ServletSecurity(@HttpConstraint(rolesAllowed = "Employee"))
     *                                               Methods - doPost and doCustom
     *
     *                                               (inherits no annotations and not the url)
     *                                               - verify access to methods that exist in "Child4" by employee (vs other children that are accessed by mgr)
     *                                               - verify https is NOT required - calls to http are not redirected (either dynamically, or via the return of a 302)
     *
     */
    /**
     * <P> Do the following:
     * <LI>Attempt to access a protected servlet via a call to doGet using http on the Child4 servlet
     * <LI>The employee id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> call was automatically redirected from http to https
     * <LI> doGet of the Parent servlet is invoked (as no doGet exists in Child4) (status code 200)
     * <LI> user and role info is correct
     * </OL>
     */
    @Test
    public void testInheritTestChild4Get_http() throws Exception {
        // input values are:         <method>, <user>, <password>, <url>
        inputSettings testValues = new inputSettings(doGet, employeeUser, employeePassword, child4ServletUrl);
        // expected values are:         <method>, <user>, <first status code>, <second status code (if needed)>, <base of the url hit>, <servlet instance requested>, <servlet instance used>
        expectedSettings expectedValues = new expectedSettings(doGet, employeeUser, HTTP_OK, HTTP_OK, inheritUrlBase, child4Servlet, parentServlet);
        genericInheritTest(testValues, expectedValues);
    }

    /**
     * Common code to convert an HttpResponse to a string
     *
     * @param HttpResponse
     * @throws Exception
     * @return String (response in String form)
     */
    private String convertHttpResponseToString(HttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }

    /**
     * Generic test routine for inheritance (all tests call this - altering the inputSettings/expectedSettings as appropriate)
     * sets up to invoke the method on the servlets
     * - verifies the status code returned
     * - redirects to https if needed
     * - verifies that the correct servlet instance was hit
     * - verifies that the user info printed by the servlet is correct
     *
     * @param inputSetting (local inputSettings structure - containing all the info needed to invoke any of the methods)
     * @param expectedSettings (local expectedSettings structure - containing all the info needed to verify test behavior)
     * @returns Exception
     **/
    private void genericInheritTest(inputSettings testValues, expectedSettings expectedValues) throws Exception {
        String response = null;
        HttpResponse rawResponse = null;
        Header header = null;
        String location = null;
        String theTest = testName.getMethodName();
        String methodName = theTest + " - genericInheritTest";
        int thisPort;
        CommonTestHelper testHelper = new CommonTestHelper();

        Log.info(thisClass, methodName, "Entering test " + theTest);

        // If we're trying to test http redirect, and we're expecting the redirect status code of
        // 302, don't do the ssl setup.
        boolean sslSetup = true;
        if (expectedValues.status1 == HTTP_Found) {
            sslSetup = false;
        }

        // Custom is invoked differently than the other methods
        if (testValues.method.equals(doCustom)) {
            if (testValues.url.contains("https")) {
                thisPort = server.getHttpDefaultSecurePort();
            } else {
                thisPort = server.getHttpDefaultPort();
            }
            // we need to authenticate if the caller passed in a user
            boolean secure = false;
            if (testValues.user != null) {
                secure = true;
            }
            // invoke custom method
            response = testHelper.httpCustomMethodResponse(testValues.url, testValues.method, secure, thisPort, testValues.user, testValues.password, server);
            // set up the url for a secure connection - in case we'll call this test method again
            location = testValues.url.replace("http:", "https:").replace(Integer.toString(server.getHttpDefaultPort()), Integer.toString(server.getHttpDefaultSecurePort()));
        } else {
            // invoke the requested method
            rawResponse = testHelper.executeHttpMethodRequestWithAuthCreds(testValues.method, testValues.url, testValues.user,
                                                                           testValues.password, server, sslSetup);
            Log.info(thisClass, methodName, "Status: " + rawResponse.getStatusLine());

            Log.info(thisClass, methodName, "Status code  " + expectedValues.status1 + " expected");
            Log.info(thisClass, methodName, "Status code  " + rawResponse.getStatusLine().getStatusCode() + " received");
            assertEquals("Expected " + expectedValues.status1 + " was not returned",
                         expectedValues.status1, rawResponse.getStatusLine().getStatusCode());
            // convert the response to a string for additional checking
            response = convertHttpResponseToString(rawResponse);
            // in the cases where we'll get a redirect, get the new url
            header = rawResponse.getFirstHeader("Location");
            if (header != null) {
                location = header.getValue();
                Log.info(thisClass, methodName, "after getting location");
            }
        }
        Log.info(thisClass, methodName, "Servlet response: " + response);

        // if we received a 200, validate the remainder of the response
        if (expectedValues.status1 == HTTP_OK) {
            verifyMethodInstance(response, expectedValues);
            verifyCredentials(response, expectedValues);
        } else if (expectedValues.status1 == HTTP_Found) {

            if (debug) {
                Log.info(thisClass, methodName, "Redirect location: " + location);
            }
            // HTTP_Found is a redirected servlet
            // re-run with https and check for a different status code
            inputSettings newTestValues = new inputSettings(testValues.method, testValues.user, testValues.password, location);
            expectedSettings newExpectedValues = new expectedSettings(expectedValues.method, expectedValues.user, expectedValues.status2, expectedValues.status2, expectedValues.urlBase, expectedValues.servlet, expectedValues.methodServlet);
            genericInheritTest(newTestValues, newExpectedValues);
        }
        Log.info(thisClass, methodName, "Exiting test " + theTest);
    }

    /**
     * Routine to verify that we made it to the method in the expected servlet instance
     *
     * @param String
     * @param expectedSetting (local structure containing values to be checked)
     */
    private boolean verifyMethodInstance(String response, expectedSettings expectedValues) {

        String failMsgPrefix = "Expected to hit " + expectedValues.methodServlet + " and access to be granted, but, ... ";
        String methodName = "verifyMethodInstance";
        if (expectedValues.servlet != null) {
            Log.info(thisClass, methodName, "Expecting requested Servlet Name of: " + expectedValues.servlet);
            assertTrue(failMsgPrefix + "The response did not contain the expected requested servletName (..." + rootContext + "/" + expectedValues.servlet + ")",
                       response.contains(rootContext + "/" + expectedValues.servlet));
        }
        if (expectedValues.methodServlet != null) {
            Log.info(thisClass, methodName, "Expecting used Servlet Name of: " + expectedValues.methodServlet);
            assertTrue(failMsgPrefix + "The response did not contain the expected used servletName (" + expectedValues.methodServlet + ")",
                       response.contains("ServletName: " + expectedValues.methodServlet));
        }
        if (expectedValues.method != null) {
            Log.info(thisClass, methodName, "Expecting to call method: " + expectedValues.method);
            assertTrue(failMsgPrefix + "The response did not contain the expected method invoked  (" + expectedValues.method + ")",
                       response.contains(expectedValues.method));
        }

        return true;
    }

    /**
     * Routine to verify that we the expected user info was printed by the called servlet
     *
     * @param String
     * @param expectedSetting (local structure containing values to be checked)
     */
    private boolean verifyCredentials(String response, expectedSettings expectedValues) {

        String methodName = "verifyCredentials";
        String userNameToVerify = null;
        boolean isManager = false;
        boolean isEmployee = false;

        assertNotNull("The response should not be null", response);

        // if the user to be checked is null, we'll need to skip the user in role checks
        if (expectedValues.user != null) {
            userNameToVerify = expectedValues.user;
            if (expectedValues.user == employeeUser) {
                isEmployee = true;
            } else {
                if (expectedValues.user == managerUser) {
                    isManager = true;
                }
                // we're not really testing with anything other than manager, employee, or not logged in
            }
        }

        Log.info(thisClass, methodName, "Expecting requested Url: " + expectedValues.urlBase);
        assertTrue("The response did not contain the expected invoked url",
                   response.contains("getRequestURL: " + expectedValues.urlBase));

        Log.info(thisClass, methodName, "Expecting remote user: " + expectedValues.user);
        assertTrue("The response did not contain the expected remoteUser",
                   response.contains(REMOTE_USER_HEADER + userNameToVerify));

        Log.info(thisClass, methodName, "Expecting isUserInEmplyeeRole: " + isEmployee);
        assertTrue("The response did not contain the expected isUserInRole(Employee)",
                   response.contains(EMPLOYEE_ROLE_HEADER + isEmployee));

        Log.info(thisClass, methodName, "Expecting isUserInManagerRole: " + isManager);
        assertTrue("The response did not contain the expected isUserInRole(Manager)",
                   response.contains(MANAGER_ROLE_HEADER + isManager));

        if (expectedValues.user != null) {
            Log.info(thisClass, methodName, "Expecting user principal: " + WSPRINCIPAL);
            assertTrue("The response did not contain the expected userPrincipal",
                       response.contains(USER_PRINCIPAL_HEADER + WSPRINCIPAL
                                         + expectedValues.user));
            Log.info(thisClass, methodName, "Expecting principal name: " + userNameToVerify);
            assertTrue("The response did not contain the expected Principal name",
                       response.contains(USER_PRINCIPAL_NAME_HEADER + userNameToVerify));
        } else {
            Log.info(thisClass, methodName, "Expecting user princial null");
            assertTrue("The response did not contain the expected userPrincipal",
                       response.contains(USER_PRINCIPAL_HEADER + "null"));
        }

        return true;
    }
}
