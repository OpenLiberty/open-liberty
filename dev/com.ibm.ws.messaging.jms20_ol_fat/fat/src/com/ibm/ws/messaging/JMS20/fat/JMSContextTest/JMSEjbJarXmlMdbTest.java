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

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class JMSEjbJarXmlMdbTest {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("JMSEjbJarXmlMdbTest_TestServer");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSRedelivery_120846?test="
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

        server.setServerConfigurationFile("EJBMDB_server.xml");
        server.startServer("JMSEjbJarXmlMdb_Client.log");

    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testQueueSendMDB() throws Exception {

        val = runInServlet("testQueueSendMDB");
        assertTrue("testQueueSendMDB failed", val);

        String msg = server.waitForStringInLog("Message received on EJB MDB: testQueueSendMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    @Test
    public void testAnnotatedMDB() throws Exception {

        val = runInServlet("testAnnotatedMDB");
        assertTrue("testAnnotatedMDB failed", val);

        String msg = server.waitForStringInLog("Message received on Annotated MDB: testAnnotatedMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

// for defect 175486
    @Test
    public void testTopicSendMDB() throws Exception {

        val = runInServlet("testTopicSendMDB");
        assertTrue("testTopicSendMDB failed", val);

        String msg = server.waitForStringInLog("Message received on EJB Topic MDB: testTopicSendMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    @Test
    public void testTopicAnnotatedMDB() throws Exception {

        val = runInServlet("testTopicAnnotatedMDB");
        assertTrue("testTopicAnnotatedMDB failed", val);

        String msg = server.waitForStringInLog("Message received on Annotated Topic MDB: testTopicAnnotatedMDB", 5000);
        assertNotNull("Could not find the TEST PASSED message in the trace.log", msg);
    }

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSRedelivery_120846war = ShrinkWrap.create(WebArchive.class, "JMSRedelivery_120846.war")
            .addClass("web.JMSRedelivery_120846Servlet")
            .add(new FileAsset(new File("test-applications//JMSRedelivery_120846.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSRedelivery_120846.war/resources/WEB-INF/beans.xml")), "WEB-INF/beans.xml")
            .add(new FileAsset(new File("test-applications//JMSRedelivery_120846.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSRedelivery_120846war, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);
    }
}
