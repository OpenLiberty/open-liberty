/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSContextTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.messaging.JMS20.fat.TestUtils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class JMSEjbJarXmlMdbTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("JMSRedeliveryServer");

    private static final int serverPort = server.getHttpDefaultPort();
    private static final String serverHostName = server.getHostname();

    private static final String redeliveryAppName = "JMSRedelivery_120846";
    private static final String redeliveryContextRoot = "JMSRedelivery_120846";
    private static final String[] redeliveryPackages = new String[] { "jmsredelivery_120846.web" };

    private static final String mdbAppName = "mdbapp";
    private static final String[] mdbPackages = new String[] { "mdbapp.ejb" };

    private static final String mdb1AppName = "mdbapp1";
    private static final String[] mdb1Packages = new String[] { "mdbapp1.ejb" };

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(serverHostName, serverPort, redeliveryContextRoot, test); // throws IOException
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        server.copyFileToLibertyInstallRoot(
                                            "lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server.setServerConfigurationFile("EJBMDB_server.xml");

        // The redelivery application sends messages ... then waits for a message
        // to be written to a server log.
        TestUtils.addDropinsWebApp(server, redeliveryAppName, redeliveryPackages);

        // MDBs in the MDB applications write to the server logs upon receipt of
        // a message.
        TestUtils.addDropinsWebApp(server, mdbAppName, mdbPackages);
        TestUtils.addDropinsWebApp(server, mdb1AppName, mdb1Packages);

        server.startServer("EJBMDB.log");
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            server.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // JMSRedelivery_120846

    @Test
    public void testQueueSendMDB() throws Exception {
        boolean testResult = runInServlet("testQueueSendMDB");
        assertTrue("testQueueSendMDB failed", testResult);

        String msg = server.waitForStringInLog("Message received on EJB MDB: testQueueSendMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    // JMSRedelivery_120846
    @Test
    public void testAnnotatedMDB() throws Exception {
        boolean testResult = runInServlet("testAnnotatedMDB");
        assertTrue("testAnnotatedMDB failed", testResult);

        String msg = server.waitForStringInLog("Message received on Annotated MDB: testAnnotatedMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    // JMSRedelivery_120846
    // for defect 175486
    @Test
    public void testTopicSendMDB() throws Exception {
        boolean testResult = runInServlet("testTopicSendMDB");
        assertTrue("testTopicSendMDB failed", testResult);

        String msg = server.waitForStringInLog("Message received on EJB Topic MDB: testTopicSendMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    // JMSRedelivery_120846
    @Test
    public void testTopicAnnotatedMDB() throws Exception {
        boolean testResult = runInServlet("testTopicAnnotatedMDB");
        assertTrue("testTopicAnnotatedMDB failed", testResult);

        String msg = server.waitForStringInLog("Message received on Annotated Topic MDB: testTopicAnnotatedMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

}
