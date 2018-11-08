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
package com.ibm.ws.messaging.comms.fat;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.matchers.Matchers;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FeatureUpdate {

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("FeatureUpdate_com.ibm.ws.messaging.comms.client");
    private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("FeatureUpdate_com.ibm.ws.messaging.comms.server");

    private static final int PORT1 = server1.getHttpDefaultPort();

    private StringBuilder runInServlet(String test) throws IOException {
        URL url = new URL("http://localhost:" + PORT1 + "/CommsLP?test=" + test);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
            setupShrinkWrap();


        server2.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
        server2.copyFileToLibertyServerRoot("resources/security", "serverLTPAKeys/certnew.der");
        server2.copyFileToLibertyServerRoot("resources/security", "serverLTPAKeys/ltpa.keys");
        server2.copyFileToLibertyServerRoot("resources/security", "serverLTPAKeys/mykeynew.jks");

        server1.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyServerRoot("resources/security", "clientLTPAKeys/mykeynew.jks");

        server2.startServer();
        server1.startServer();

        String uploadMessage = server1.waitForStringInLog("CWWKF0011I:.*", server1.getMatchingLogFile("myTraceS1.log"));
        assertNotNull("Could not find the server start info message in trace file", uploadMessage);

        uploadMessage = server2.waitForStringInLog("CWWKF0011I:.*", server2.getMatchingLogFile("myTraceS.log"));
        assertNotNull("Could not find the server start info message in the trace file", uploadMessage);

        uploadMessage = server2.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint.*", server2.getMatchingLogFile("myTraceS.log"));
        assertNotNull("Could not find the SSL port ready message in the trace file", uploadMessage);

        uploadMessage = server2.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint-ssl.*", server2.getMatchingLogFile("myTraceS.log"));
        assertNotNull("Could not find the SSL port ready message in the trace file", uploadMessage);
    }

    @AfterClass
    public static void tearDown() throws Exception {
    	System.out.println("starting teardown ");
        server1.stopServer();
        server2.stopServer();
        System.out.println("completed  teardown ");
    }

    @Test
    public void testSendReceive2LP() throws Exception {
        setMarkMakeConfigUpdate(server1, "server1.xml");
        setMarkEnsureConfigUpdate(server2, "server2.xml");
        
        runInServlet("testQueueSendMessage");
        runInServlet("testQueueReceiveMessages");

        String msg = server1.waitForStringInLog("Queue Message", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the queue message in the trace file", msg);
    }
   

    @Test
    public void testwasJmsSecurityFeatureUpdate() throws Exception {
        System.out.println("starting testwasJmsSecurityFeatureUpdate");
        setMarkMakeConfigUpdate(server1, "server1.xml");
        setMarkEnsureConfigUpdate(server2, "SecurityDisabledServer.xml");
        System.out.println("marker set , running test in servelet");
        runInServlet("testQueueSendMessageExpectException");

        String uploadMessage = server1.waitForStringInLogUsingMark("CWSIC1001E:.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find CWSIC1001E (unable to connect to ME exception) in trace.log", uploadMessage);

        System.out.println("stop server");
        server2.stopServer();
        System.out.println("changing server.xml to SecurityEnabledServer");
        server2.setServerConfigurationFile("SecurityEnabledServer.xml");
        System.out.println("starting server ");
        server2.startServer();
    }

    
    
    
    @Test
    public void testSSLFeatureUpdate() throws Exception {

    	System.out.println("starting testSSLFeatureUpdate and server stop");
        server1.stopServer();
        server1.setServerConfigurationFile("SecurityDisabledClient.xml");
        server1.startServer();

        System.out.println("server started again");
        
        String uploadMessage = server1.waitForStringInLog("CWWKF0011I:.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the server start info message in trace file", uploadMessage);

        System.out.println("running test");
        runInServlet("testQueueSendMessageExpectException");

        uploadMessage = server1.waitForStringInLogUsingMark("CWSIT0127E:.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find CWSIT0127E (unable to connect to ME exception) in trace.log", uploadMessage);
    }
    

    static void setMarkEnsureConfigUpdate(LibertyServer server, String newServerXml) throws Exception {
        final String logLine = setMarkMakeConfigUpdate(server, newServerXml);
        assertThat("Server configuration should have been updated", logLine, not(containsString("CWWKG0018I")));
    }

    static String setMarkMakeConfigUpdate(LibertyServer server, String newServerXml) throws Exception {
        // set a new mark
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        // make the configuration update
        server.setServerConfigurationFile(newServerXml);

        // wait for configuration update to complete
        return server.waitForStringInLogUsingMark("CWWKG001[78]I");
    }

    public static void setupShrinkWrap() throws Exception {
        final JavaArchive utilsJar = ShrinkWrap.create(JavaArchive.class, "utilLib.jar")
                .addPackages(true, "test.util");

        final Archive testWar = ShrinkWrap.create(WebArchive.class, "CommsLP.war")
                .addClass("web.CommsLPServlet")
                .add(new FileAsset(new File("test-applications/CommsLP.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
                .add(new FileAsset(new File("test-applications/CommsLP.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml")
                .addAsLibrary(utilsJar);

        ShrinkHelper.exportDropinAppToServer(server2, testWar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, testWar, OVERWRITE);
    }
}
