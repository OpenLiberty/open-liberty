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

package com.ibm.ws.security.registry.basic.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

@RunWith(FATRunner.class)
public class FATTestIgnoreCase {
    private static final String DEFAULT_CONFIG_FILE = "basic.ignore.server.xml.orig";
    private static final String ALTERNATE_BASIC_REGISTRY_CONFIG_IGNORE_CASE_TRUE = "alternateIgnoreCaseTrueBasicRegistry.xml";
    private static final String ALTERNATE_BASIC_REGISTRY_CONFIG_IGNORE_CASE_FALSE = "alternateIgnoreCaseFalseBasicRegistry.xml";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.basic.fat.ignorecase");
    private static final Class<?> c = FATTestIgnoreCase.class;
    private static UserRegistryServletConnection servlet;
    private static int updateCount = 1;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features", "basicRegistryInternals-1.0.mf");

        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.addInstalledAppForValidation("userRegistry");
        server.startServer(c.getName() + ".log");

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        server.stopServer();
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        setServerConfiguration(server, DEFAULT_CONFIG_FILE);
        assertEquals("SampleBasicRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithGoodCredentialsIgnoreCaseTrueForAuthentication() throws Exception {
        Log.info(c, "checkPasswordWithGoodCredentialsIgnoreCaseForAuthentication", "Checking good credentials with case insensitive authentication");

        setServerConfiguration(server, ALTERNATE_BASIC_REGISTRY_CONFIG_IGNORE_CASE_TRUE);

        String password = "password123";
        assertEquals("Authentication should succeed.",
                     "administrator", servlet.checkPassword("administrator", password));

        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Modify the basicRegistry configuration and verify the update takes
     * effect dynamically -- the old user must become invalid and the new
     * user must take effect.
     */
    @Test
    public void dynamicallyChangeBasicRegistryConfigurationIgnoreCaseFalse() throws Exception {
        Log.info(c, "checkPasswordWithBadCredentials", "Checking bad credentials");

        setServerConfiguration(server, ALTERNATE_BASIC_REGISTRY_CONFIG_IGNORE_CASE_FALSE);

        assertNull("Authentication should not succeed for lower case administrator.",
                   servlet.checkPassword("administrator", "password123"));
        assertEquals("Authentication should succeed for case-matching user.",
                     "Administrator", servlet.checkPassword("Administrator", "password123"));
    }

    /**
     * This method is used to set the server.xml
     */
    private static void setServerConfiguration(LibertyServer server,
                                               String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(c, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile(serverXML);
            server.waitForMultipleStringsInLog(updateCount++, "CWWKG0017I");
            serverConfigurationFile = serverXML;
        }
    }
}
