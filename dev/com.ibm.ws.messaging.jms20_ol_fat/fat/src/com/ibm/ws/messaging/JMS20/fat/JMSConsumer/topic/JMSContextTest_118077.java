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
package com.ibm.ws.messaging.JMS20.fat.JMSConsumer.topic;

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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSContextTest_118077 {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118077_TestServer");
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("JMSContextTest_118077_TestServer2");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/TemporaryQueue?test="
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
            } else
                result = true;

            return result;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
            setUpShirnkWrap();


        server.setServerConfigurationFile("ApiTD.xml");
        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");

        server1.setHttpDefaultPort(8030);
        server.startServer("JMSContextTest_118077_Client.log");
        server1.startServer("JMSContextTest_118077_Server.log");

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveMessageTopicTranxSecOff_B() throws Exception {

        val = runInServlet("testReceiveMessageTopicTranxSecOff_B");
        assertTrue("testReceiveMessageTopicTranxSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveMessageTopicTranxSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveMessageTopicTranxSecOff_TCP");
        assertTrue("testReceiveMessageTopicTranxSecOff_TCP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTransactionTopicSecOff_B");
        assertTrue("testReceiveBodyTransactionTopicSecOff_B failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTransactionTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTransactionTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTransactionTopicSecOff_TCPIP failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTextMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTextMessageTopicSecOff_B failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTextMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTextMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTextMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyObjectMessageTopicSecOff_B");
        assertTrue("testReceiveBodyObjectMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyObjectMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyObjectMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyObjectMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyMapMessageTopicSecOff_B");
        assertTrue("testReceiveBodyMapMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMapMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyMapMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyMapMessageTopicSecOff_TCPIP failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyByteMessageTopicSecOff_B");
        assertTrue("testReceiveBodyByteMessageTopicSecOff_B failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyByteMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyByteMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyByteMessageTopicSecOff_TCPIP failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTransactionTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutTransactionTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTextMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutTextMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutObjectMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutObjectMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMapMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMapMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutByteMessageTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutByteMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTransactionTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitTransactionTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMessageTopicSecOff_B() throws Exception {
        val = runInServlet("testReceiveBodyNoWaitTextMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitTextMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitObjectMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitObjectMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMapMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMapMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitByteMessageTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitByteMessageTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP");
        assertTrue("testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP failed ", val);
    }

    // ----mfe

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyMFEUnsupportedTypeTopicSecOff_B");
        assertTrue("testReceiveBodyMFEUnsupportedTypeTopicSecOff_B failed ", val);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP");
        assertTrue("testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP failed ", val);
    }

    @Test
    public void testReceiveNoWaitNoMessageTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveNoWaitNullMessageTopicSecOff_B");
        assertTrue("testReceiveNoWaitNoMessageTopicSecOff_B failed ", val);
    }

    @Test
    public void testReceiveNoWaitNoMessageTopicSecOff_TCPIP() throws Exception {

        val = runInServlet("testReceiveNoWaitNullMessageTopicSecOff_TCP");
        assertTrue("testReceiveNoWaitNoMessageTopicSecOff_TCPIP failed ", val);
    }

    @Test
    public void testReceiveBodyMFENoBodyTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyMFENoBodyTopicSecOff_B");
        assertTrue("testReceiveBodyMFENoBodyTopicSecOff_B failed ", val);

    }

    @Test
    public void testReceiveBodyMFENoBodyTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyMFENoBodyTopicSecOff_TCP");
        assertTrue("testReceiveBodyMFENoBodyTopicSecOff_TCP failed ", val);

    }

    @Test
    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMFENoBodyTopicSecOff_B");
        assertTrue("testReceiveBodyTimeOutMFENoBodyTopicSecOff_B failed ", val);
    }

    @Test
    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP");
        assertTrue("testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP failed ", val);
    }

    @Test
    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_B() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMFENoBodyTopicSecOff_B");
        assertTrue("testReceiveBodyNoWaitMFENoBodyTopicSecOff_B failed ", val);
    }

    @Test
    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP() throws Exception {

        val = runInServlet("testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP");
        assertTrue("testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP failed ", val);
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();
            server1.stopServer();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void setUpShirnkWrap() throws Exception {

        Archive TemporaryQueuewar = ShrinkWrap.create(WebArchive.class, "TemporaryQueue.war")
            .addClass("web.JMSContextTestServlet")
            .add(new FileAsset(new File("test-applications//TemporaryQueue.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//TemporaryQueue.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, TemporaryQueuewar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, TemporaryQueuewar, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
