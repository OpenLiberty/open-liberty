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

package com.ibm.ws.messaging.JMS20.fat.SharedSubscription;

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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class SharedSubscriptionTest_129626 {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("SharedSubscriptionTest_129626_TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("SharedSubscriptionTest_129626_TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean testResult = false;

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/SharedSubscription?test=" + test);
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
        server1.setHttpDefaultPort(8030);
        server1.setServerConfigurationFile("TestServer1.xml");
        server.startServer("SharedSubscriptionTestClient_129626.log");
        server1.startServer("SharedSubscriptionTestServer_129626.log");
    }

    //  129626_1    JMSConsumer createSharedConsumer(Topic topic,String sharedSubscriptionName)
    //  129626_1_1      Creates a shared non-durable subscription with the specified name on the specified topic (if one does not already exist) and creates a consumer on that subscription. This method creates the non-durable subscription without a message selector.
    //129626_1_4      Non-durable subscription is not persisted and will be deleted (together with any undelivered messages associated with it) when there are no consumers on it. The term "consumer" here means a MessageConsumer or JMSConsumer object in any client.

    // Bindings and Security Off

    @Test
    public void testCreateSharedNonDurable_B_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        val1 = runInServlet("testCreateSharedNonDurableConsumer_create_B_SecOff");

        server.stopServer();
        server.startServer("SharedSubscriptionTestClient_129626.log");

        val2 = runInServlet("testCreateSharedNonDurableConsumer_consume_B_SecOff");

        if (val1 == true && val2 == true)
            testResult = true;

        assertTrue("Test testCreateSharedNonDurable_B_SecOff failed", testResult);

    }

    //TCP and SecurityOff
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurable_TCP_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;
        val1 = runInServlet("testCreateSharedNonDurableConsumer_create_TCP_SecOff");

        server.stopServer();
        server.startServer("SharedSubscriptionTestClient_129626.log");
        server1.stopServer();
        server1.startServer("SharedSubscriptionTestServer_129626.log");

        val2 = runInServlet("testCreateSharedNonDurableConsumer_consume_TCP_SecOff");

        if (val1 == true && val2 == true)
            testResult = true;

        assertTrue("Test testCreateSharedNonDurable_TCP_SecOff failed", testResult);

    }

    // 129626_1_2  If a shared non-durable subscription already exists with the same name and client identifier (if set), and the same topic and message selector has been specified, then this method creates a JMSConsumer on the existing subscription.
    // Bindings and SecOff

    @Test
    public void testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff failed", testResult);

    }

    @Test
    public void testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_2Subscribers_TCP failed",
                   testResult);

    }


//129626_1_6    If a shared non-durable subscription already exists with the same name and client identifier (if set) but a different topic or message selector value has been specified, and there is a consumer already active (i.e. not closed) on the subscription, then a JMSRuntimeException will be thrown.
    // Bindings and Security Off
    @ExpectedFFDC("com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumer_JRException_B_SecOff()
                    throws Exception {

        testResult = runInServlet("testCreateSharedNonDurableConsumer_JRException_B_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_JRException_B_SecOff failed", testResult);

    }

    //TCP and SecurityOff
    @ExpectedFFDC("com.ibm.websphere.sib.exception.SIErrorException , com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException")
    @Mode(TestMode.FULL)
//    @Test
    public void testCreateSharedNonDurableConsumer_JRException_TCP_SecOff()
                    throws Exception {

        runInServlet("testCreateSharedNonDurableConsumer_JRException_TCP_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_JRException_TCP_SecOff failed",
                   testResult);

    }

    //129626_1_7  There is no restriction on durable subscriptions and shared non-durable subscriptions having the same name and clientId (which may be unset). Such subscriptions would be completely separate.

    @Test
    public void testCreateSharedNonDurableConsumer_coexist_B_SecOff() throws Exception {

        testResult = runInServlet("testCreateSharedNonDurableConsumer_coexist_B_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_coexist_B_SecOff failed", testResult);

    }

    @Test
    public void testCreateSharedNonDurableConsumer_coexist_TCP_SecOff() throws Exception {

        testResult = runInServlet("testCreateSharedNonDurableConsumer_coexist_TCP_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_coexist_TCP_SecOff failed", testResult);

    }

    //  129626_1_9  InvalidDestinationRuntimeException - if an invalid topic is specified.
    // Bindings and Security Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff()
                    throws Exception {
        testResult = runInServlet("testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff failed",
                   testResult);

    }

    // TCP and Sec Off

    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff()
                    throws Exception {
        testResult = runInServlet("testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff");

        assertTrue("Test testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff failed",
                   testResult);

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

        Archive SharedSubscriptionwar = ShrinkWrap.create(WebArchive.class, "SharedSubscription.war")
            .addClass("web.SharedSubscriptionServlet")
            .add(new FileAsset(new File("test-applications//SharedSubscription.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//SharedSubscription.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, SharedSubscriptionwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, SharedSubscriptionwar, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
