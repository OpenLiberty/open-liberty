/*******************************************************************************
 * Copyright (c) 2011,2013 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

public class FATTest {
    private static final String DEFAULT_CONFIG_FILE = "basic.server.xml.orig";
    private static final String ALTERNATE_BASIC_REGISTRY_CONFIG = "alternateBasicRegistry.xml";
    private static final String DEFAULT_AES_CONFIG_FILE = "defaultAESBasicRegistry.xml";
    private static final String CUSTOM_AES_CONFIG_FILE = "customAESBasicRegistry.xml";
    private static final String DEFAULT_HASH_CONFIG_FILE = "defaultHashBasicRegistry.xml";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.basic.fat");
    private static final Class<?> c = FATTest.class;
    private static UserRegistryServletConnection servlet;
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
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
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
    public void checkPasswordWithGoodCredentials() throws Exception {
        Log.info(c, "checkPasswordWithGoodCredentials", "Checking good credentials");

        setServerConfiguration(server, DEFAULT_CONFIG_FILE);

        String password = "password123";
        assertEquals("Authentication should succeed.",
                     "admin", servlet.checkPassword("admin", password));

        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Hit the test servlet to see if checkPassword works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPasswordWithBadCredentials() throws Exception {
        Log.info(c, "checkPasswordWithBadCredentials", "Checking bad credentials");

        setServerConfiguration(server, DEFAULT_CONFIG_FILE);

        String password = "badPassword";
        assertNull("Authentication should not succeed.",
                   servlet.checkPassword("admin", password));

        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * This test just validates that the server correctly processes AES encoded credentials.
     * This is really a test for password decoding making use of the fact basic registry FAT
     * will exercise the test code. We also check the dynamism here to ensure when changing
     * config to have a new encoding key we pick it up dynamically.
     */
    @Test
    public void checkPasswordEncodedUsingAES() throws Exception {
        Log.info(c, "checkPasswordEncodedUsingAES", "Checking aes encoded credentials");

        setServerConfiguration(server, DEFAULT_AES_CONFIG_FILE);

        String password = "alternatepwd";
        assertEquals("Authentication should succeed.",
                     "defaultUser", servlet.checkPassword("defaultUser", password));

        passwordChecker.checkForPasswordInAnyFormat(password);

        setServerConfiguration(server, CUSTOM_AES_CONFIG_FILE);

        assertEquals("Authentication should succeed.",
                     "customUser", servlet.checkPassword("customUser", password));

        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * This test just validates that the server correctly processes hashed passwords
     * which was generated by default parameters.
     */
    @Test
    public void checkPasswordEncodedUsingHashDefault() throws Exception {
        Log.info(c, "checkPasswordEncodedUsingHash", "Checking hash encoded credentials");

        setServerConfiguration(server, DEFAULT_HASH_CONFIG_FILE);

        String GoodPassword = "pa$$w0rd";
        String BadPassword = "pa@@w0rd";
        String user = "hashedUser";
        assertEquals("Authentication should succeed.",
                     user, servlet.checkPassword(user, GoodPassword));
        passwordChecker.checkForPasswordInAnyFormat(GoodPassword);

        assertNull("Authentication should fail.", servlet.checkPassword(user, BadPassword));
        passwordChecker.checkForPasswordInAnyFormat(BadPassword);

    }

    /**
     * This test just validates that the server correctly processes hashed passwords
     * which was generated by following parameters.
     * securityutility encode --encoding=hash --salt=$alt --iteration=999 --algorithm=SHA-256 1234!@#$
     */
    @Test
    public void checkPasswordEncodedUsingHashCustom() throws Exception {
        Log.info(c, "checkPasswordEncodedUsingHash", "Checking hash encoded credentials");

        setServerConfiguration(server, DEFAULT_HASH_CONFIG_FILE);

        String GoodPassword = "WebAS";
        String BadPassword = "WebA$";
        String user = "customHashedUser";
        assertEquals("Authentication should succeed.",
                     user, servlet.checkPassword(user, GoodPassword));
        passwordChecker.checkForPasswordInAnyFormat(GoodPassword);

        assertNull("Authentication should fail.", servlet.checkPassword(user, BadPassword));
        passwordChecker.checkForPasswordInAnyFormat(BadPassword);

    }

    /**
     * Modify the basicRegistry configuration and verify the update takes
     * effect dynamically -- the old user must become invalid and the new
     * user must take effect.
     */
    @Test
    public void dynamicallyChangeBasicRegistryConfiguration() throws Exception {
        Log.info(c, "checkPasswordWithBadCredentials", "Checking bad credentials");

        setServerConfiguration(server, ALTERNATE_BASIC_REGISTRY_CONFIG);

        assertEquals("Should get the new realm name",
                     "AlternateRealm", servlet.getRealm());
        assertNull("Authentication should not succeed for old user.",
                   servlet.checkPassword("admin", "password123"));
        assertEquals("Authentication should succeed for new user.",
                     "alternateUser", servlet.checkPassword("alternateUser", "alternatepwd"));
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
            server.waitForStringInLog("CWWKG0017I");
            serverConfigurationFile = serverXML;
        }
    }
}