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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HttpMethodOmissionTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth");
    private static Class<?> logClass = HttpMethodOmissionTest.class;
    private static CommonTestHelper testHelper = new CommonTestHelper();
    private static String authTypeBasic = "BASIC";
    private final static String OMISSION_BASIC_SERVLET = "/basicauth/OmissionBasic";
    private final static String OMISSION_COMPLEX_SERVLET = "/basicauth/OmissionComplex";
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;
    protected final static String employeeUser = "user1";
    protected final static String employeePassword = "user1pwd";
    protected final static String managerUser = "user2";
    protected final static String managerPassword = "user2pwd";

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(logClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(logClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation("basicauth");

        JACCFatUtils.installJaccUserFeature(server);
        JACCFatUtils.transformApps(server, "basicauth.war", "basicauthXMI.ear", "basicauthXMInoAuthz.ear", "basicauthXML.ear", "basicauthXMLnoAuthz.ear");

        server.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I"));

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
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /OmissionBasic, where all methods are protected except POST
     * <LI>Expected result: GET, CUSTOM - protected with AllAuthenticated role, POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionBasic_GetWithEmployee() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_BASIC_SERVLET;
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(url, employeeUser, employeePassword, url, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /OmissionBasic, where all methods are protected except POST
     * <LI>Expected result: GET, CUSTOM - protected with AllAuthenticated role, POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionBasic_CustomWithEmployee() throws Exception {
        String methodName = "testHttpMethodOmissionBasic_CustomWithEmployee";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_BASIC_SERVLET;
        try {
            String response = testHelper.httpCustomMethodResponse(url, "CUSTOM", true, server.getHttpDefaultPort(), employeeUser, employeePassword);
            Log.info(logClass, methodName, "response: " + response);
            testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed to access the URL " + url);
        }
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /OmissionBasic, where all methods are protected except POST
     * <LI>Expected result: GET, CUSTOM - protected with AllAuthenticated role, POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionBasic_PostUnprotected() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_BASIC_SERVLET;
        testHelper.accessPostUnprotectedServlet(url);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /OmissionComplex, where all methods are denied access except POST
     * <LI>Expected result: GET, CUSTOM - denied access, POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionComplex_GetWithEmployee() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_COMPLEX_SERVLET;
        testHelper.accessGetProtectedServletWithInvalidCredentials(url, employeeUser, employeePassword, url, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /OmissionComplex, where all methods are denied access except POST
     * <LI>Expected result: GET, CUSTOM - denied access, POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionComplex_CustomWithEmployee() throws Exception {
        String methodName = "testHttpMethodOmissionComplex_CustomWithEmployee";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_COMPLEX_SERVLET;
        try {
            String response = testHelper.httpCustomMethodResponse(url, "CUSTOM", true, server.getHttpDefaultPort(), employeeUser, employeePassword);
            Log.info(logClass, methodName, "response: " + response);
            assertTrue("Expected 403 Forbidden not found", response.contains("403 Forbidden"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed to access the URL " + url);
        }
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /OmissionComplex, where all methods are denied access except POST
     * <LI>Expected result: GET, CUSTOM - denied access, POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionComplex_PostUnprotected() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_COMPLEX_SERVLET;
        testHelper.accessPostUnprotectedServlet(url);
    }

}
