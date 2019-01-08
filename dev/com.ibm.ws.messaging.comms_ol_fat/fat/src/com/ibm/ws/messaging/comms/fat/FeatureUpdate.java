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

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class FeatureUpdate extends AbstractSuite {

    private static LibertyServer servletServer = LibertyServerFactory.getLibertyServer("FeatureUpdate_com.ibm.ws.messaging.comms.client");
    private static LibertyServer jmsServer = LibertyServerFactory.getLibertyServer("FeatureUpdate_com.ibm.ws.messaging.comms.server");


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setupShrinkWrap(servletServer);
        copyFiles(jmsServer, "resources/security",
                "serverLTPAKeys/certnew.der", "serverLTPAKeys/ltpa.keys", "serverLTPAKeys/mykeynew.jks");
        copyFiles(servletServer,"resources/security", "clientLTPAKeys/mykeynew.jks");

        jmsServer.startServer();
        servletServer.startServer();
        waitForServerStart(servletServer, false);
        waitForServerStart(jmsServer, true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
    	System.out.println("starting teardown ");
        servletServer.stopServer();
        jmsServer.stopServer();
        System.out.println("completed  teardown ");
    }


    @Test
    public void testSendReceive2LP() throws Exception {
        setMarkUpdateConfigs(servletServer, "server1.xml");
        setMarkUpdateConfigs(jmsServer, "server2.xml");
        
        runInServlet(servletServer, "testQueueSendMessage");
        runInServlet(servletServer, "testQueueReceiveMessages");

        String msg = servletServer.waitForStringInLog("Queue Message", servletServer.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the queue message in the trace file", msg);
    }
   

    @Test
    public void testwasJmsSecurityFeatureUpdate() throws Exception {
        System.out.println("starting testwasJmsSecurityFeatureUpdate");
        setMarkUpdateConfigs(servletServer, "server1.xml");
        setMarkUpdateConfigs(jmsServer, "SecurityDisabledServer.xml");
        System.out.println("marker set , running test in servelet");
        runInServlet(servletServer, "testQueueSendMessageExpectException");

        String uploadMessage = servletServer.waitForStringInLogUsingMark("CWSIC1001E:.*", servletServer.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find CWSIC1001E (unable to connect to ME exception) in trace.log", uploadMessage);

        System.out.println("stop server");
        jmsServer.stopServer();
        System.out.println("changing server.xml to SecurityEnabledServer");
        jmsServer.setServerConfigurationFile("SecurityEnabledServer.xml");
        System.out.println("starting server ");
        jmsServer.startServer();
    }

    
    //@Test
    public void testSSLFeatureUpdate() throws Exception {

    	System.out.println("starting testSSLFeatureUpdate and server stop");
        servletServer.stopServer();
        servletServer.setServerConfigurationFile("SecurityDisabledClient.xml");
        servletServer.startServer();

        System.out.println("server started again");
        
        String uploadMessage = servletServer.waitForStringInLog("CWWKF0011I:.*", servletServer.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the server start info message in trace file", uploadMessage);

        System.out.println("running test");
        runInServlet(servletServer, "testQueueSendMessageExpectException");

        uploadMessage = servletServer.waitForStringInLogUsingMark("CWSIT0127E:.*", servletServer.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find CWSIT0127E (unable to connect to ME exception) in trace.log", uploadMessage);
    }


    private static void setMarkUpdateConfigs(LibertyServer server, String initialConfig, String... subsequentConfigs) throws Exception {
        updateConfig(server, initialConfig);
        for (String config: subsequentConfigs) {
            final String logLine = updateConfig(server, config);
            assertThat("Server configuration should have been updated", logLine, not(containsString("CWWKG0018I")));
        }
    }


    private static String updateConfig(LibertyServer server, String newServerXml) throws Exception {
        // set a new mark
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        // make the configuration update
        server.setServerConfigurationFile(newServerXml);

        // wait for configuration update to complete
        return server.waitForStringInLogUsingMark("CWWKG001[78]I");
    }
}
