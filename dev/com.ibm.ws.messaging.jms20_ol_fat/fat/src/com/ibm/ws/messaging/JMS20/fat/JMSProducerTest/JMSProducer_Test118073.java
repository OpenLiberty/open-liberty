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
package com.ibm.ws.messaging.JMS20.fat.JMSProducerTest;

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
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSProducer_Test118073 {
    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSProducer_Test118073_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSProducer_Test118073_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();
    private static boolean testResult = false;

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSProducer_118073?test="
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

        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer();

        server.setServerConfigurationFile("JMSContext_Client.xml");
        server.startServer();

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

    @Mode(TestMode.FULL)
    @Test
    public void testSetGetJMSReplyTo_Topic_B_SecOff() throws Exception {

        testResult = runInServlet("testSetGetJMSReplyTo_Topic_B_SecOff");

        assertTrue("testSetGetJMSReplyTo_Topic_B_SecOff", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetGetJMSReplyTo_Topic_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetGetJMSReplyTo_Topic_TCP_SecOff");

        assertTrue("testSetGetJMSReplyTo_Topic_TCP_SecOff", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testNullJMSReplyTo_B_SecOff() throws Exception {
        testResult = runInServlet("testNullJMSReplyTo_B_SecOff");

        assertTrue("testNullJMSReplyTo_B_SecOff", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testNullJMSReplyTo_TCP_SecOff() throws Exception {

        testResult = runInServlet("testNullJMSReplyTo_TCP_SecOff");

        assertTrue("testNullJMSReplyTo_TcpIp_SecOff: Expected output not found", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetAsync_B_SecOff() throws Exception {

        testResult = runInServlet("testSetAsync_B_SecOff");

        assertTrue("testSetAsync_B_SecOff failed", testResult);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetAsync_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetAsync_TCP_SecOff");

        assertTrue("testSetAsync_TCP_SecOff failed", testResult);

    }

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSProducer_118073war = ShrinkWrap.create(WebArchive.class, "JMSProducer_118073.war")
            .addClass("web.JMSProducer_118073Servlet")
            .add(new FileAsset(new File("test-applications//JMSProducer_118073.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSProducer_118073.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSProducer_118073war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSProducer_118073war, OVERWRITE);
        Archive JMSProducerwar = ShrinkWrap.create(WebArchive.class, "JMSProducer.war")
            .addClass("web.JMSProducerServlet")
            .add(new FileAsset(new File("test-applications//JMSProducer.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSProducer.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSProducerwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSProducerwar, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
