/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSContextTest;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.junit.BeforeClass;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import com.ibm.websphere.simplicity.ShrinkHelper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSContextTest_118058 {

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118058_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118058_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean testResult = false;

    private static boolean runInServlet(String test) throws IOException {
        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSContext?test="
                          + test);
        System.out.println("The Servlet URL is : " + url.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.connect();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);
                result = false;
            }

            else
                result = true;

            return result;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
            setUpShirnkWrap();


        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");

        server.setServerConfigurationFile("JMSContext.xml");
        server1.setServerConfigurationFile("TestServer1.xml");
        server.startServer("JMSContextTestClient_118058.log");
        server1.startServer("JMSContextTestServer_118058.log");

    }

    // 118058_1_2 : Verify the default session mode using getSessionMode() when
    // JMSContext is created using createContext().

    // Bindings and Security off
    @Mode(TestMode.FULL)
    @Test
    public void testGetSessionMode_B_SecOff() throws Exception {

        testResult = runInServlet("testGetSessionMode_B_SecOff");

        assertNotNull("Test testGetSessionMode_B_SecOff failed", testResult);

    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetSessionMode_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetSessionMode_TCP_SecOff");
        assertNotNull("Test testGetSessionMode_TCP_SecOff failed", testResult);

    }

    // 118058_1_8 :when a JMSContext is created by calling one of several
    // createContext methods on a ConnectionFactory, is closed by calling its
    // close method.

    // Bindings and Security off
    @Mode(TestMode.FULL)
    @Test
    public void testClose_B_SecOff() throws Exception {

        testResult = runInServlet("testClose_B_SecOff");

        assertTrue("Test testClose_B_SecOff failed", testResult);

    }

    // TCP and Security off
    @Mode(TestMode.FULL)
    @Test
    public void testClose_TCP_SecOff() throws Exception {

        testResult = runInServlet("testClose_TCP_SecOff");

        assertTrue("Test close failed", testResult);

    }

    // 118058_1_10 : Verify getClientID method for JMSContext gets the client
    // identifier for the JMSContext's connection

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_B_SecOff() throws Exception {

        testResult = runInServlet("testGetClientID_B_SecOff");

        assertTrue("Test testGetClientID_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_TCP_SecOff() throws Exception {
        testResult = runInServlet("testGetClientID_TCP_SecOff");

        assertTrue("Test testGetClientID_TCP_SecOff failed", testResult);

    }

    // 118058_1_14 : Verify setAutoStart() and getAutoStart()

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_B_SecOff() throws Exception {

        testResult = runInServlet("testSetGetAutoStart_B_SecOff");

        assertTrue("Test testSetGetAutoStart_B_SecOff failed", testResult);

    }

    // TCP and Security off
    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetGetAutoStart_TCP_SecOff");

        assertTrue("Test testSetGetAutoStart_TCP_SecOff failed", testResult);

    }

    // 118058_2_2: Call createContext with username /password as empty string

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUser_empty_B_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_empty_B_SecOff");

        assertNotNull("Test testcreateContextwithUser_empty_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUser_empty_TCP_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_empty_TCP_SecOff");

        assertTrue("Test testcreateContextwithUser_empty_TCP_SecOff failed", testResult);

    }

    // 118058_2_3: Call createContext with username /password as null

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUser_null_B_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_null_B_SecOff");

        assertTrue("Test testcreateContextwithUser_null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUser_null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_null_TCP_SecOff");

        assertTrue("Test testcreateContextwithUser_null_TCP_SecOff failed", testResult);

    }

    // 118058_2_4 118058_2_5 Verify setAutoStart() and getAutoStart() for
    // createContext(String userName, String password)

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_createContextwithUser_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testSetGetAutoStart_createContextwithUser_B_SecOff");

        assertTrue("Test testSetGetAutoStart_createContextwithUser_B_SecOff failed",
                   testResult);

    }

    // TCP and Security off
    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_createContextwithUser_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testSetGetAutoStart_createContextwithUser_TCP_SecOff");

        assertTrue("Test testSetGetAutoStart_createContextwithUser_TCP_SecOff failed",
                   testResult);

    }

    // 118058_2_6 Verify getClientID method for JMSContext gets the client
    // identifier for the JMSContext's connection

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_createContextUser_B_SecOff() throws Exception {

        testResult = runInServlet("testGetClientID_createContextUser_B_SecOff");

        assertTrue("Test testGetClientID_createContextUser_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_createContextUser_TCP_SecOff() throws Exception {
        String msg = null;

        testResult = runInServlet("testGetClientID_createContextUser_TCP_SecOff");

        assertTrue("Test testGetClientID_createContextUser_TCP_SecOff failed", testResult);

    }

    // 118058_2_7 Verify setClientID used in a Java EE web application causes a
    // JMSRuntimeException to be thrown

    // Bindings and Sec off
    @Mode(TestMode.FULL)
    @Test
    public void testsetClientID_createContextUser_B_SecOff() throws Exception {

        testResult = runInServlet("testsetClientID_createContextUser_B_SecOff");

        assertTrue("Test testsetClientID_createContextUser_B_SecOff failed", testResult);

    }

    // TCP and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testsetClientID_createContextUser_TCP_SecOff() throws Exception {

        testResult = runInServlet("testsetClientID_createContextUser_TCP_SecOff");

        assertNotNull("Test testsetClientID_createContextUser_TCP_SecOff failed", testResult);

    }

    // / 118058_3_2 : Check if CLIENT_ACKNOWLEDGE session mode is used , then it
    // is ignored and AUTO_ACKNOWLEDGE is set
    // Bindings an Security Off

    // Latest fix :JMSRuntimeException will be thrown when client_ack is used
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithInvalidsessionMode_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithInvalidsessionMode_B_SecOff");

        assertTrue("Test testcreateContextwithInvalidsessionMode_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithInvalidsessionMode_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithInvalidsessionMode_TCP_SecOff");

        assertTrue("Test testcreateContextwithInvalidsessionMode_TCP_SecOff failed", testResult);

    }

    // 118058_3_3 and 118058_3_4 : Call createContext with session mode as
    // negative value and 10000

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithNegsessionMode_B_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithNegsessionMode_B_SecOff");

        assertTrue("Test testcreateContextwithNegSessionMode_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithNegsessionMode_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithNegsessionMode_TCP_SecOff");

        assertTrue("Test testcreateContextwithNegsessionMode_TCP_SecOff failed", testResult);

    }

    // 118058_4_7 Verify the connection is in stopped mode and automatically
    // started when a JMSConsumer is created

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_createContextUserSessionMode_B_SecOff()
                    throws Exception {

        runInServlet("testConnStartAuto_createContextUserSessionMode_B_SecOff");

        assertTrue("Test testConnStartAuto_createContextUserSessionMode failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_createContextUserSessionMode_TCP_SecOff()
                    throws Exception {
        runInServlet("testConnStartAuto_createContextUserSessionMode_TCP_SecOff");

        assertTrue("Test testConnStartAuto_createContextUserSessionMode_TCP_SecOff failed", testResult);

    }

    // 118058_4_3 session mode -ve
    // 118058_4_4 session mode 10000

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserNegsessionMode_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithUserNegsessionMode_B_SecOff");

        assertTrue("Test testcreateContextwithUserNegsessionMode_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserNegsessionMode_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithUserNegsessionMode_TCP_SecOff");

        assertTrue("Test testcreateContextwithUserNegsessionMode_TCP_SecOff failed", testResult);

    }

    // 118058_4_5 username empty

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserSessionMode_empty_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithUserSessionMode_empty_B_SecOff");

        assertTrue("Test testcreateContextwithUserSessionMode_empty_B_SecOff failed", testResult);

    }

    // TCP and security off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserSessionMode_empty_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithUserSessionMode_empty_TCP_SecOff");

        assertTrue("Test testcreateContextwithUserSessionMode_empty_TCP_SecOff failed", testResult);

    }

    // 118058_4_6 username null

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserSessionMode_null_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithUserSessionMode_null_B_SecOff");

        assertTrue("Test testcreateContextwithUserSessionMode_null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testcreateContextwithUserSessionMode_null_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testcreateContextwithUserSessionMode_null_TCP_SecOff");

        assertTrue("Test testcreateContextwithUserSessionMode_null_TCP_SecOff failed", testResult);

    }

    // 118058_1_3 : Verify the connection is in stopped mode and automatically
    // started when a JMSConsumer is created

    // Bindings and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_B_SecOff() throws Exception {
        testResult = runInServlet("testConnStartAuto_B_SecOff");

        assertTrue("Test testConnStartAuto_B_SecOff failed", testResult);

    }

    // TCP and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_TCP_SecOff() throws Exception {
        testResult = runInServlet("testConnStartAuto_TCP_SecOff");

        assertTrue("Test testConnStartAuto_TCP_SecOff failed", testResult);

    }

    // 118058_9 Verify setClientID used in a Java EE web application causes a
    // JMSRuntimeException to be thrown

    // Bindings and Sec off

    @Test
    public void testsetClientID_B_SecOff() throws Exception {

        testResult = runInServlet("testsetClientID_B_SecOff");

        assertTrue("Test testsetClientID_B_SecOff failed", testResult);

    }

    // TCP and Sec Off

    @Test
    public void testsetClientID_TCP_SecOff() throws Exception {

        testResult = runInServlet("testsetClientID_TCP_SecOff");

        assertTrue("Test testsetClientID_TCP_SecOff failed", testResult);

    }

    // 118058_1_12 :Verify getMetaData method gets the connection metadata for
    // the JMSContext's connection

    // Bindings and Security off

    @Test
    public void testGetMetadata_B_SecOff() throws Exception {

        testResult = runInServlet("testGetMetadata_B_SecOff");

        assertTrue("Test testGetMetadata_B_SecOff failed", testResult);

    }

    // TCP and Security off

    @Test
    public void testGetMetadata_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetMetadata_TCP_SecOff");

        assertTrue("Test testGetMetadata_TCP_SecOff failed", testResult);

    }

    // 118058_2_1 :Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext(String userName, String password)
    // Bindings and Security off

    @Test
    public void testcreateContextwithUser_B_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_B_SecOff");

        assertTrue("Test testcreateContextwithUser_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testcreateContextwithUser_TCP_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithUser_TCP_SecOff");

        assertTrue("Test testcreateContextwithUser_TCP_SecOff failed", testResult);

    }

    // 118058_2_8 Verify the connection is in stopped mode and automatically
    // started when a JMSConsumer is created

    // Bindings and Sec Off

    @Test
    public void testConnStartAuto_createContextUser_B_SecOff() throws Exception {

        testResult = runInServlet("testConnStartAuto_createContextUser_B_SecOff");

        assertNotNull("Test testConnStartAuto_createContextUser_B_SecOff failed", testResult);

    }

    // TCP and Sec Off

    @Test
    public void testConnStartAuto_createContextUser_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testConnStartAuto_createContextUser_TCP_SecOff");

        assertTrue("Test testConnStartAuto_createContextUser_TCP_SecOff failed", testResult);

    }

    // 118058_3_1 : Creation of JMSContext from Connection
    // factory.ConnectionFactory.createContext(int sessionMode)

    // Bindings and security Off

    @Test
    public void testcreateContextwithsessionMode_B_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithsessionMode_B_SecOff");

        assertTrue("Test testcreateContextwithsessionMode_B_SecOff failed", testResult);

    }

    // TCP and security Off

    @Test
    public void testcreateContextwithsessionMode_TCP_SecOff() throws Exception {

        testResult = runInServlet("testcreateContextwithsessionMode_TCP_SecOff");

        assertTrue("Test testcreateContextwithsessionMode_TCP_SecOff failed", testResult);

    }

    // 118058_4 :Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext(String userName,String password, int
    // sessionMode)
    // Bindings and Sec off

    @Test
    public void testcreateContextwithUserSessionMode_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithUserSessionMode_B_SecOff");

        assertTrue("Test testcreateContextwithUserSessionMode_B_SecOff failed", testResult);

    }

    // TCP and Sec off

    @Test
    public void testcreateContextwithUserSessionMode_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testcreateContextwithUserSessionMode_TCP_SecOff");

        assertTrue("Test testcreateContextwithUserSessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_6 :Exception should be thrown when attempt is made to create more
    // than one active (not closed) Session object per connection.
    // JMSContext.createContext(int sessionMode)

    // Bindings and Security Off

    @Test
    public void testcreateContextfromJMSContext_B_SecOff() throws Exception {
        testResult = runInServlet("testcreateContextfromJMSContext_B_SecOff");

        assertTrue("Test testcreateContextfromJMSContext_B_SecOff failed", testResult);
    }

    // TCP and Security Off

    @Test
    public void testcreateContextfromJMSContext_TCP_SecOff() throws Exception {
        testResult = runInServlet("testcreateContextfromJMSContext_TCP_SecOff");

        assertTrue("Test testcreateContextfromJMSContext_TCP_SecOff failed", testResult);
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {

            server.stopServer();
            server1.stopServer();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static void setUpShirnkWrap() throws Exception {

        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
