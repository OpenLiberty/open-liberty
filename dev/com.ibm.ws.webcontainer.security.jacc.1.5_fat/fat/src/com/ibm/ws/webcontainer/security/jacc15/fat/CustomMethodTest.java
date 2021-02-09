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
 * 3. Note on *Overlap* test:
 * When there are more than one constraints applied to the same servlet, the least constraint will win,
 * e.g.,
 *   <auth-constraint id="AuthConstraint_5">
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.codec.binary.Base64;
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
public class CustomMethodTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth");
    private final Class<?> thisClass = CustomMethodTest.class;
    private static CommonTestHelper testHelper = new CommonTestHelper();

    private final static String validUser = "user1";
    private final static String validPassword = "user1pwd";
    private static final String managerUser = "user2";
    private static final String managerPassword = "user2pwd";
    private static String authTypeBasic = "BASIC";

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

        JACCFatUtils.transformApps(server, "basicauth.war", "basicauthXMI.ear", "basicauthXMInoAuthz.ear", "basicauthXML.ear", "basicauthXMLnoAuthz.ear");

        server.addInstalledAppForValidation("basicauth");
        server.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCUSTOM call to the URL.
     * <LI>The id and pw are base 64 base 64 encoded
     * <LI>The servlet CustomMethodServlet supports doGET, doPOST, and doCUSTOM
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testCustomMethodProtected() throws Exception {
        String methodName = "testCustomMethodProtected";

        String httpMethod = "CUSTOM";
        String testuser = validUser;
        String testPwd = validPassword;

        //int sslPort = server.getHttpDefaultSecurePort();
        //Log.info(thisClass, "--debug debug", "find sslPort: " + sslPort + ", test name = " + methodName);

        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/CustomMethodServlet";
        Log.info(thisClass, methodName, "URL: " + testUrl + ", test name = " + methodName);

        try {
            String rs = httpCustomMethodResponse(testUrl, httpMethod, true, port, testuser, testPwd);//CustomMethodServlet
            //String rs = httpCustomMethodResponse(testUrl, httpMethod, false, port, testuser, testPwd);//UnprotectedCustomMethodServlet
            Log.info(thisClass, methodName, "response: " + rs);
            testHelper.verifyProgrammaticAPIValues(testuser, rs, authTypeBasic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("--debug Test failed to access the URL " + testUrl);
        }
    }

    /**
     * Custom method overlap test.
     * Authorization must be successful for Employee and must fail for Manager.
     */
    @Test
    public void testCustomMethodProtected_Overlap() throws Exception {
        String methodName = "testCustomMethodProtected_Overlap";

        String httpMethod = "CUSTOM";
        String testuser = validUser;
        String testPwd = validPassword;

        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/OverlapCustomMethodServlet";
        Log.info(thisClass, methodName, "Testing with user1 for Employee, shoud succeed: URL: " + testUrl + ", test name = " + methodName);

        try {
            String response = httpCustomMethodResponse(testUrl, httpMethod, true, port, testuser, testPwd);
            Log.info(thisClass, methodName, "response: " + response);
            testHelper.verifyProgrammaticAPIValues(testuser, response, authTypeBasic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("--debug Test failed to access the URL " + testUrl);
        }

        Log.info(thisClass, methodName, "Testing with user2 for Manager, shoud *fail*: URL: " + testUrl + ", test name = " + methodName);

        try {
            String response = httpCustomMethodResponse(testUrl, httpMethod, true, port, managerUser, managerPassword);
            Log.info(thisClass, methodName, "response: " + response);
            assertTrue("Expected 403 was not returned.", response.matches("HTTP/1.0 403 [\\s\\S]*\\z"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("--debug Test failed to access the URL " + testUrl);
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * <LI>The servlet SimpleCustomMethodServlet only supports doCUSTOM
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testSimpleCustomMethodServlet() throws Exception {
        String methodName = "testSimpleCustomMethodServlet";

        String httpMethod = "CUSTOM";
        String testuser = validUser;
        String testPwd = validPassword;

        //int sslPort = server.getHttpDefaultSecurePort();
        //Log.info(thisClass, "--debug debug", "find sslPort: " + sslPort + ", test name = " + methodName);

        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/SimpleCustomMethodServlet";
        Log.info(thisClass, methodName, "URL: " + testUrl + ", test name = " + methodName);

        try {
            String rs = httpCustomMethodResponse(testUrl, httpMethod, true, port, testuser, testPwd);
            Log.info(thisClass, methodName, "response: " + rs);
            testHelper.verifyProgrammaticAPIValues(testuser, rs, authTypeBasic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("--debug Test failed to access the URL " + testUrl);
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet using a java call to make a doCustom call to the URL.
     * <LI>The servlet UnprotectedCustomMethodServlet only supports doCUSTOM method
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Use can access the servlet from the doCUSTOM call
     * </OL>
     */
    @Test
    public void testCustomMethodUnprotected() throws Exception {
        String methodName = "testCustomMethodUnprotected";

        String httpMethod = "CUSTOM";
        //int sslPort = server.getHttpDefaultSecurePort();
        //Log.info(thisClass, "--debug debug", "find sslPort: " + sslPort + ", test name = " + methodName);

        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/UnprotectedCustomMethodServlet";
        Log.info(thisClass, methodName, "URL: " + testUrl + ", test name = " + methodName);

        try {
            String rs = httpCustomMethodResponse(testUrl, httpMethod, false, port, null, null);
            Log.info(thisClass, methodName, "response: " + rs);
            assertTrue("getRemoteUser is not null.", rs.contains("getRemoteUser: null"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("--debug Test failed to access the URL " + testUrl);
        }
    }

    /**
     * Verify the following:
     * <LI>Set up to enable SSL, then
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The servlet SecureCustomMethodServlet supports doCUSTOM method,
     * and has user constraint CONFIDENTIAL that allows access on HTTPS port.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Use can access the servlet from the doCUSTOM call
     * </OL>
     */
    @Test
    public void testCustomMethodSecureServletOnHTTPS() throws Exception {
        String methodName = "testSecureServletOnHTTPS";

        String httpMethod = "CUSTOM";
        String testuser = validUser;
        String testPwd = validPassword;

        int sslPort = server.getHttpDefaultSecurePort();
        String testUrl = "https://" + server.getHostname() + ":" + sslPort + "/basicauth/SecureCustomMethodServlet";
        Log.info(thisClass, methodName, "URL: " + testUrl + ", test name = " + methodName);

        String trustStoreFile = server.getServerRoot() + "/resources/security/key.jks";
        System.setProperty("javax.net.ssl.trustStore", trustStoreFile);
        System.setProperty("javax.net.ssl.trustStorePassword", "Liberty");

        try {
            String rs = httpCustomMethodResponse(testUrl, httpMethod, true, sslPort, testuser, testPwd);
            Log.info(thisClass, methodName, "response: " + rs);
            testHelper.verifyProgrammaticAPIValues(testuser, rs, authTypeBasic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("--debug Test failed to access the URL " + testUrl);
        }
    }

    /*
     * Helper method to make custom method call
     */
    public String httpCustomMethodResponse(String urlLink, String httpMethod, boolean secure, int port, String user, String password) throws Exception {
        URL url = null;
        try {
            url = new URL(urlLink);
            Log.info(thisClass, "--debug httpCustomMethodResponse", "urlLink = " + urlLink);
        } catch (MalformedURLException e) {
            return "Invalid URL " + urlLink;
        }

        Socket socket = null;

        if (urlLink.indexOf("https") > -1) {
            SocketFactory socketFactory = SSLSocketFactory.getDefault();
            socket = socketFactory.createSocket(url.getHost(), url.getPort());
        } else {
            socket = new Socket(url.getHost(), url.getPort());
        }
        Log.info(thisClass, "--debug httpCustomMethodResponse", "url.getHost()=" + url.getHost() + ",url.getPort()=" + url.getPort() + ",url.getPath()=" + url.getPath());

        // Send header
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        wr.write(httpMethod + " " + url.getPath() + " HTTP/1.0\r\n");
        //wr.write("GET" + " " + url.getPath() + " HTTP/1.0\r\n");  // try doGET

        if (secure) {
            byte[] encoding = Base64.encodeBase64((user + ":" + password).getBytes());

            String encodedStr = new String(encoding);
            wr.write("Authorization: Basic " + encodedStr + "\r\n");
        }
        wr.write("\r\n");
        wr.flush();

        // Get response
        Log.info(thisClass, "--debug httpCustomMethodResponse", ", getting response");

        BufferedReader d = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuffer responseString1 = new StringBuffer();
        String line1 = null;
        try {
            while ((line1 = d.readLine()) != null) {
                if (!(line1.isEmpty())) {
                    line1.concat(line1.trim());
                    responseString1.append(line1);
                }
            }
        } catch (IOException e) {
            throw new Exception("Failed to access the URL " + urlLink + " with message " + e.getMessage());
        }

        return responseString1.toString();
    }

}
