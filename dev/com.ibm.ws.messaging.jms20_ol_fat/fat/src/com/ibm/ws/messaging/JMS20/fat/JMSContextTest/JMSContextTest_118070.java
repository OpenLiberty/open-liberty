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
public class JMSContextTest_118070 {
    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118070_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("JMSContextTest_118070_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;
        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSContext_118070?test="
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

            if (lines.indexOf(test + " COMPLETED SUCCESSFULLY") < 0) {
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

        server1.setHttpDefaultPort(8030);
        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer("JMSContext_118070_Server.log");
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

        server.setServerConfigurationFile("JMSContext_Client.xml");

        server.startServer("JMSContext_118070_Client.log");

        changedMessageFromLog = server.waitForStringInLog(
                                                          "CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);

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
    public void testCloseClosedContext_B_SecOff() throws Exception {

        val = runInServlet("testCloseClosedContext_B_SecOff");
        assertTrue("testCloseClosedContext_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testCloseClosedContext_TcpIp_SecOff() throws Exception {

        val = runInServlet("testCloseClosedContext_B_SecOff");
        assertTrue("testCloseClosedContext_TcpIp_SecOff failed ", val);

    }

    // dup of testCloseAckRecMsg_B_SecOff

    @Mode(TestMode.FULL)
    @Test
    public void testAckOnClosedContext_B_SecOff() throws Exception {

        val = runInServlet("testAckOnClosedContext_B_SecOff");
        assertTrue("testAckOnClosedContext_B_SecOff failed ", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testAckOnClosedContext_TcpIp_SecOff() throws Exception {

        val = runInServlet("testAckOnClosedContext_TcpIp_SecOff");
        assertTrue("testAckOnClosedContext_TcpIp_SecOff failed ", val);

    }

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
        Archive JMSContext_118070war = ShrinkWrap.create(WebArchive.class, "JMSContext_118070.war")
            .addClass("web.JMSContext_118070Servlet")
            .add(new FileAsset(new File("test-applications//JMSContext_118070.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext_118070.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContext_118070war, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContext_118070war, OVERWRITE);
    }
}
