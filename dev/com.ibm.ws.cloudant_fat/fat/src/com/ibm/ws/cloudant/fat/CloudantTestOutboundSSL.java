/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.fat;

import static com.ibm.ws.cloudant.fat.FATSuite.cloudant;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.OutboundConnection;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CloudantTestOutboundSSL extends FATServletClient {

    @Server("com.ibm.ws.cloudant.fat.outboundSSL")
    public static LibertyServer server;

    private static final String DB_NAME = "outboundssldb";
    public static final String JEE_APP = "cloudantfat";
    public static final String SERVLET_NAME = "CloudantTestServlet";
    // CWWKO0801E (SSLHandshakeErrorTracker - no cipher suites in common) : See defect 260787
    public static String[] expectedFailures = { "CWWKG0033W.*does_not_exist", "CWWKO0801E", "CWPKI0063W" };

    @BeforeClass
    public static void setUp() throws Exception {
        server.addEnvVar("cloudant_url", cloudant.getURL(false));
        server.addEnvVar("cloudant_url_secure", cloudant.getURL(true));
        server.addEnvVar("cloudant_port_secure", "" + cloudant.getMappedPort(CouchDBContainer.PORT_SECURE));
        server.addEnvVar("cloudant_username", cloudant.getUser());
        server.addEnvVar("cloudant_password", cloudant.getPassword());
        server.addEnvVar("cloudant_databaseName", DB_NAME);

        cloudant.createDb(DB_NAME);

        // TODO extract security files from container prior to server start
        // TODO delete security files from git

//        cloudant.copyFileFromContainer("/etc/couchdb/cert/server.crt", server.getServerRoot() + "/security/server.crt");
//        FATSuite.createKeystore(server.getServerRoot() + "/security/keystore.jks", server.getServerRoot() + "/security/server.crt");

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, JEE_APP, "cloudant.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(expectedFailures);
    }

    private void runTest() throws Exception {
        runTest(server, JEE_APP + '/' + SERVLET_NAME, testName.getMethodName() + "&databaseName=" + DB_NAME);
    }

    /*
     * Testing to make sure that if Cloudant is configuration without a SSLRef that the
     * connection uses the SSL configuration defined as the outbound SSL default.
     *
     * This test is using the com.ibm.ws.cloudant.fat.outboundSSL default server.xml
     * so no configuration updates are needed here.
     */
    @Test
    public void testSSLOutboundDefault() throws Exception {
        //running with no sslRef with this configuration will pick up the SSL outbound default
        runTest();
    }

    /*
     * Testing to make sure that if Cloudant is configuration without a SSLRef that the
     * connection uses the SSL configuration set up with filter that points to the host
     * and port of the Cloundant DB.
     */
    @Test
    public void testSSLOutboundFilter() throws Exception {

        // Set up an outbound SSL default configuration
        ServerConfiguration config = server.getServerConfiguration();

        // Save the configuration to restore after testing
        ServerConfiguration savedConfig = config.clone();

        //get the cloudant SSL configuration add the outbound filter to it
        SSL cloudSSLCfg = config.getSSLById("cloudantSSLConfig");

        //Add the outbound filter to cloudantSSLConfig
        OutboundConnection outConnection = new OutboundConnection();
        outConnection.setHost("${cloudant_server}");
        outConnection.setPort("${cloudant_port_secure}");
        cloudSSLCfg.setOutboundConnectionToList(outConnection);

        // update the server and wait for configuration change messages
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);

        //Make sure the configuration has been updated and the application restarted
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("cloudantfat"));

        //running with no sslRef with this configuration will pick up the outbound SSL filter
        runTest();

        //test has run lets restore the config to the original
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("cloudantfat"));
    }

    /*
     * Testing to make sure the SSL configuration specified by the sslRef
     * on the cloudant element is used and not outbound default SSL configuration
     * or the configuration with a filter going to the cloudant database
     */
    @Test
    public void testSSLRefWithFilter() throws Exception {

        // Set up an outbound SSL default configuration
        ServerConfiguration config = server.getServerConfiguration();

        // Save the configuration to restore after testing
        ServerConfiguration savedConfig = config.clone();

        //Setup the outbound filter to the database on "defaultSSLConfig"
        //If the filter is uses, and it should not be, there will be an error
        SSL defaultSSLCfg = config.getSSLById("defaultSSLConfig");

        //Add the outbound filter to defaultSSLConfig
        OutboundConnection outConnection = new OutboundConnection();
        outConnection.setHost("${cloudant_server}");
        outConnection.setPort("${cloudant_port_secure}");
        defaultSSLCfg.setOutboundConnectionToList(outConnection);

        // update the server and wait for configuration change messages
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);

        //Make sure the configuration has been updated and the application restarted
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("cloudantfat"));

        //running to the cloudant configuration with sslRef set to cloudantSSLConfig
        runTest();

        //test has run lets restore the config
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(savedConfig);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("cloudantfat"));
    }

}
