/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FATTestReuseConn {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.ids.reuseConnection");
    private static final Class<?> c = FATTestReuseConn.class;
    private static UserRegistryServletConnection servlet;

    protected static String serverConfigurationFile = "";
    private static final String REUSE_CONNECTION_XML = "reuseConnectionOn.xml";
    private static final String REUSE_CONNECTION_CONTEXTPOOL_XML = "reuseConnectionAndContextPool.xml";

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("userRegistry");
        server.startServer(c.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application userRegistry does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        // set a new mark
        server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs\\trace.log"));

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer();
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(c, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);

            // set a new mark
            server.setMarkToEndOfLog(server.getDefaultLogFile());
            server.setMarkToEndOfLog(server.getFileFromLibertyServerRoot("logs\\trace.log"));

            // Update the server xml
            server.setServerConfigurationFile("/" + serverXML);

            // Wait for CWWKG0017I and CWWKZ0003I to appear in logs after we started the config update
            Log.info(c, "setServerConfiguration",
                     "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
            server.waitForStringInLogUsingMark("CWWKG0017I"); //CWWKG0017I: The server configuration was successfully updated in 0.2 seconds.
            //server.waitForStringInLogUsingMark("CWWKZ0003I"); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.

            serverConfigurationFile = serverXML;
        }
    }

    /**
     * Hit the test servlet to see if context pool is disabled when reuseConnection is set to false
     */
    @Test
    public void reuseConnectionFalse() throws Exception {
        String group = "group1";
        Log.info(c, "reuseConnectionFalse", "Checking for disabled context pool.");
        servlet.isValidGroup(group);
        assertThat(server.findStringsInLogsAndTrace("Context Pool is disabled").size(), Matchers.greaterThan(0));
    }

    /**
     * Hit the test servlet to see if context pool is enabled when reuseConnection is set to default
     */
    @Test
    public void reuseConnectionDefault() throws Exception {
        try {
            setServerConfiguration(REUSE_CONNECTION_XML);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String group = "group1";
        Log.info(c, "reuseConnectionFalse", "Checking for enabled context pool.");
        servlet.isValidGroup(group);
        assertEquals("Context pool enabled message not found in log file.", 1,
                     server.findStringsInLogsAndTrace("Context Pool is enabled").size());
    }

    /**
     * Hit the test servlet to see if context pool is disabled when reuseConnection is set to false
     * but context pool is set to true.
     */
    @Test
    public void reuseConnectionAndContextPool() throws Exception {
        try {
            setServerConfiguration(REUSE_CONNECTION_CONTEXTPOOL_XML);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String group = "group1";
        Log.info(c, "reuseConnectionFalse", "Checking for disabled context pool.");
        servlet.isValidGroup(group);
        assertThat(server.findStringsInLogsAndTrace("Context Pool is disabled").size(), Matchers.greaterThan(1));
    }
}
