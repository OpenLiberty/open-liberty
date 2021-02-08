/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JMSContextTest_118058 {
    private static LibertyServer engineServer = LibertyServerFactory.getLibertyServer("JMSContextEngine");

    private static LibertyServer clientServer = LibertyServerFactory.getLibertyServer("JMSContextClient");

    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "JMSContext";
    private static final String[] appPackages = new String[] { "jmscontext.web" };
    private static final String contextRoot = "JMSContext";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test);
        // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
                                                  "lib/features",
                                                  "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("JMSContextEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
                                                  "lib/features",
                                                  "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("JMSContextClient.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("JMSContextTest_118058_Engine.log");
        clientServer.startServer("JMSContextTest_118058_Client.log");
    }

    @AfterClass
    public static void tearDown() {
        try {
            clientServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            engineServer.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ShrinkHelper.cleanAllExportedArchives();
    }

    // 118058_1_2 : Verify the default session mode using getSessionMode() when
    // JMSContext is created using createContext().

    @Mode(TestMode.FULL)
    @Test
    public void testGetSessionMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetSessionMode_B_SecOff");
        assertTrue("Test testGetSessionMode_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetSessionMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetSessionMode_TCP_SecOff");
        assertTrue("Test testGetSessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_1_8 :when a JMSContext is created by calling one of several
    // createContext methods on a ConnectionFactory, is closed by calling its
    // close method.

    @Mode(TestMode.FULL)
    @Test
    public void testClose_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testClose_B_SecOff");
        assertTrue("Test testClose_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testClose_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testClose_TCP_SecOff");
        assertTrue("Test close failed", testResult);
    }

    // 118058_1_10 : Verify getClientID method for JMSContext gets the client
    // identifier for the JMSContext's connection

    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetClientID_B_SecOff");
        assertTrue("Test testGetClientID_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetClientID_TCP_SecOff");
        assertTrue("Test testGetClientID_TCP_SecOff failed", testResult);
    }

    // 118058_1_14 : Verify setAutoStart() and getAutoStart()

    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetGetAutoStart_B_SecOff");
        assertTrue("Test testSetGetAutoStart_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetGetAutoStart_TCP_SecOff");
        assertTrue("Test testSetGetAutoStart_TCP_SecOff failed", testResult);
    }

    // 118058_2_2: Call createContext with username /password as empty string

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUser_empty_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUser_empty_B_SecOff");
        assertTrue("Test testcreateContextwithUser_empty_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUser_empty_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUser_empty_TCP_SecOff");
        assertTrue("Test testcreateContextwithUser_empty_TCP_SecOff failed", testResult);
    }

    // 118058_2_3: Call createContext with username /password as null

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUser_null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUser_null_B_SecOff");
        assertTrue("Test testcreateContextwithUser_null_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUser_null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUser_null_TCP_SecOff");
        assertTrue("Test testcreateContextwithUser_null_TCP_SecOff failed", testResult);
    }

    // 118058_2_4 118058_2_5 Verify setAutoStart() and getAutoStart() for
    // createContext(String userName, String password)

    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_createContextWithUser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetGetAutoStart_createContextwithUser_B_SecOff");
        assertTrue("Test testSetGetAutoStart_createContextwithUser_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetGetAutoStart_createContextWithUser_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testSetGetAutoStart_createContextwithUser_TCP_SecOff");
        assertTrue("Test testSetGetAutoStart_createContextwithUser_TCP_SecOff failed", testResult);
    }

    // 118058_2_6 Verify getClientID method for JMSContext gets the client
    // identifier for the JMSContext's connection

    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_createContextUser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetClientID_createContextUser_B_SecOff");
        assertTrue("Test testGetClientID_createContextUser_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetClientID_createContextUser_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetClientID_createContextUser_TCP_SecOff");
        assertTrue("Test testGetClientID_createContextUser_TCP_SecOff failed", testResult);
    }

    // 118058_2_7 Verify setClientID used in a Java EE web application causes a
    // JMSRuntimeException to be thrown

    @Mode(TestMode.FULL)
    @Test
    public void testSetClientID_createContextUser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testsetClientID_createContextUser_B_SecOff");
        assertTrue("Test testsetClientID_createContextUser_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetClientID_createContextUser_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testsetClientID_createContextUser_TCP_SecOff");
        assertTrue("Test testsetClientID_createContextUser_TCP_SecOff failed", testResult);
    }

    // 118058_3_2 : Check if CLIENT_ACKNOWLEDGE session mode is used , then it
    // is ignored and AUTO_ACKNOWLEDGE is set
    // Bindings an Security Off

    /**
     * See JMS 2.0 specifcation: https://javaee.github.io/javaee-spec/javadocs/javax/jms/Connection.html
     *
     * In the Java EE web or EJB container, when there is no active JTA transaction in progress:
     * If transacted is set to false and acknowledgeMode is set to JMSContext.CLIENT_ACKNOWLEDGE then
     * the JMS provider is recommended to ignore the specified parameters and instead provide a non-transacted,
     * auto-acknowledged session. However the JMS provider may alternatively provide
     * a non-transacted session with client acknowledgement.
     *
     * So, using CLIENT_ACKNOWLEDGE should not cause an exception to be thrown, which was previously the case.
     */

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithInvalidSessionMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithInvalidsessionMode_B_SecOff");
        assertTrue("Test testcreateContextwithInvalidsessionMode_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextwithInvalidsessionMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithInvalidsessionMode_TCP_SecOff");
        assertTrue("Test testcreateContextwithInvalidsessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_3_3 and 118058_3_4 : Call createContext with session mode as
    // negative value and 10000

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithNegSessionMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithNegsessionMode_B_SecOff");
        assertTrue("Test testcreateContextwithNegSessionMode_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithNegSessionMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithNegsessionMode_TCP_SecOff");
        assertTrue("Test testcreateContextwithNegsessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_4_7 Verify the connection is in stopped mode and automatically
    // started when a JMSConsumer is created

    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_createContextUserSessionMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_createContextUserSessionMode_B_SecOff");
        assertTrue("Test testConnStartAuto_createContextUserSessionMode failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_createContextUserSessionMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_createContextUserSessionMode_TCP_SecOff");
        assertTrue("Test testConnStartAuto_createContextUserSessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_4_3 session mode -ve
    // 118058_4_4 session mode 10000

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUserNegSessionMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserNegsessionMode_B_SecOff");
        assertTrue("Test testcreateContextwithUserNegsessionMode_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUserNegSessionMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserNegsessionMode_TCP_SecOff");
        assertTrue("Test testcreateContextwithUserNegsessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_4_5 username empty

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUserSessionMode_empty_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserSessionMode_empty_B_SecOff");
        assertTrue("Test testcreateContextwithUserSessionMode_empty_B_SecOff failed", testResult);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUserSessionMode_empty_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserSessionMode_empty_TCP_SecOff");
        assertTrue("Test testcreateContextwithUserSessionMode_empty_TCP_SecOff failed", testResult);
    }

    // 118058_4_6 username null

    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUserSessionMode_null_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserSessionMode_null_B_SecOff");
        assertTrue("Test testcreateContextwithUserSessionMode_null_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testCreateContextWithUserSessionMode_null_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserSessionMode_null_TCP_SecOff");
        assertTrue("Test testcreateContextwithUserSessionMode_null_TCP_SecOff failed", testResult);
    }

    // 118058_1_3 : Verify the connection is in stopped mode and automatically
    // started when a JMSConsumer is created

    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_B_SecOff");
        assertTrue("Test testConnStartAuto_B_SecOff failed", testResult);
    }

    // TCP and Sec Off
    @Mode(TestMode.FULL)
    @Test
    public void testConnStartAuto_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_TCP_SecOff");
        assertTrue("Test testConnStartAuto_TCP_SecOff failed", testResult);
    }

    // 118058_9 Verify setClientID used in a Java EE web application causes a
    // JMSRuntimeException to be thrown

    @Test
    public void testSetClientID_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testsetClientID_B_SecOff");
        assertTrue("Test testsetClientID_B_SecOff failed", testResult);
    }

    @Test
    public void testSetClientID_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testsetClientID_TCP_SecOff");
        assertTrue("Test testsetClientID_TCP_SecOff failed", testResult);
    }

    // 118058_1_12 :Verify getMetaData method gets the connection metadata for
    // the JMSContext's connection

    @Test
    public void testGetMetadata_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMetadata_B_SecOff");
        assertTrue("Test testGetMetadata_B_SecOff failed", testResult);
    }

    @Test
    public void testGetMetadata_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testGetMetadata_TCP_SecOff");
        assertTrue("Test testGetMetadata_TCP_SecOff failed", testResult);
    }

    // 118058_2_1 :Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext(String userName, String password)

    @Test
    public void testCreateContextWithUser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUser_B_SecOff");
        assertTrue("Test testcreateContextwithUser_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateContextWithUser_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUser_TCP_SecOff");
        assertTrue("Test testcreateContextwithUser_TCP_SecOff failed", testResult);
    }

    // 118058_2_8 Verify the connection is in stopped mode and automatically
    // started when a JMSConsumer is created

    @Test
    public void testConnStartAuto_createContextUser_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_createContextUser_B_SecOff");
        assertTrue("Test testConnStartAuto_createContextUser_B_SecOff failed", testResult);
    }

    @Test
    public void testConnStartAuto_createContextUser_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_createContextUser_TCP_SecOff");
        assertTrue("Test testConnStartAuto_createContextUser_TCP_SecOff failed", testResult);
    }

    // 118058_3_1 : Creation of JMSContext from Connection
    // factory.ConnectionFactory.createContext(int sessionMode)

    @Test
    public void testCreateContextwithsessionMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithsessionMode_B_SecOff");
        assertTrue("Test testcreateContextwithsessionMode_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateContextwithsessionMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithsessionMode_TCP_SecOff");
        assertTrue("Test testcreateContextwithsessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_4 :Creation of JMSContext from Connection factory.
    // ConnectionFactory.createContext(String userName,String password, int
    // sessionMode)

    @Test
    public void testCreateContextwithUserSessionMode_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserSessionMode_B_SecOff");
        assertTrue("Test testcreateContextwithUserSessionMode_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateContextwithUserSessionMode_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextwithUserSessionMode_TCP_SecOff");
        assertTrue("Test testcreateContextwithUserSessionMode_TCP_SecOff failed", testResult);
    }

    // 118058_6 :Exception should be thrown when attempt is made to create more
    // than one active (not closed) Session object per connection.
    // JMSContext.createContext(int sessionMode)

    @Test
    public void testCreateContextfromJMSContext_B_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextfromJMSContext_B_SecOff");
        assertTrue("Test testcreateContextfromJMSContext_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateContextfromJMSContext_TCP_SecOff() throws Exception {
        boolean testResult = runInServlet("testcreateContextfromJMSContext_TCP_SecOff");
        assertTrue("Test testcreateContextfromJMSContext_TCP_SecOff failed", testResult);
    }
}
