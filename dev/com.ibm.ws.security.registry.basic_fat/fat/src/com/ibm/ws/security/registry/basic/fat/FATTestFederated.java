/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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

package com.ibm.ws.security.registry.basic.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * Test the basic registry federation.
 */
@RunWith(FATRunner.class)
public class FATTestFederated {
    private static final String CWWKS1857E_INVALID_PASSWORD_CIPHER = "CWWKS1857E";
    private static final String CWWKS1860E_FIPS_128BIT_AES_SECRET_NOT_ALLOWED = "CWWKS1860E";
    private static final String CWWKS1863E_FIPS_SHA1_HASH_NOT_ALLOWED = "CWWKS1863E";
    private static final String CWWKS1864W_WEAK_ALGORITHM_WARNING = "CWWKS1864W";
    private static final String DEFAULT_CONFIG_FILE = "basic.server.xml.orig";
    private static final String ALTERNATE_BASIC_REGISTRY_CONFIG = "alternateBasicRegistry.xml";
    private static final String DEFAULT_AES_CONFIG_FILE = "defaultAESBasicRegistry.xml";
    private static final String CUSTOM_AES_CONFIG_FILE = "customAESBasicRegistry.xml";
    private static final String DEFAULT_HASH_CONFIG_FILE = "defaultHashBasicRegistry.xml";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.basic.fat.federated");
    private static final Class<?> c = FATTestFederated.class;
    private static UserRegistryServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);
    private static final List<String> expectedErrors = new ArrayList(Arrays.asList("CWIML4537E.*admin"));
    /** CWWKS1864w is only logged one time for SHA1 {hash} passwords per server start */
    private static boolean CWWKS1864wAlreadyLoggedForHash = false;
    /** CWWKS1864w is only logged one time for AES-128 {aes} passwords per server start */
    private static boolean CWWKS1864wAlreadyLoggedForAES = false;

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
        stopServer();
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
     * This test validates that the server correctly processes AES-128 (v0) encoded credentials.
     * This is really a test for password decoding making use of the fact basic registry FAT
     * will exercise the test code. We also check the dynamism here to ensure when changing
     * config to have a new encoding key we pick it up dynamically.
     */
    @Test
    public void checkPasswordEncodedUsingAES128() throws Exception {
        Log.info(c, "checkPasswordEncodedUsingAES128", "Checking aes encoded credentials");

        setServerConfiguration(server, DEFAULT_AES_CONFIG_FILE);

        String password = "alternatepwd";

        if (server.isFIPS140_3EnabledAndSupported()) {
            assertNotNull("FIPS 140-3 should not allow AES-128bit secrets",
                          server.waitForStringInLog(CWWKS1860E_FIPS_128BIT_AES_SECRET_NOT_ALLOWED));
            expectedErrors.add(CWWKS1860E_FIPS_128BIT_AES_SECRET_NOT_ALLOWED);
        } else {

            if (!!!CWWKS1864wAlreadyLoggedForAES) {
                assertNotNull("AES-128bit password Should cause CWWKS1864W",
                              server.waitForStringInLog(CWWKS1864W_WEAK_ALGORITHM_WARNING));
                expectedErrors.add(CWWKS1864W_WEAK_ALGORITHM_WARNING);
                CWWKS1864wAlreadyLoggedForAES = true;
            }

            assertEquals("Authentication should succeed.",
                         "defaultUser", servlet.checkPassword("defaultUser", password));

            passwordChecker.checkForPasswordInAnyFormat(password);

            setServerConfiguration(server, CUSTOM_AES_CONFIG_FILE);

            assertEquals("Authentication should succeed.",
                         "customUser", servlet.checkPassword("customUser", password));

            passwordChecker.checkForPasswordInAnyFormat(password);

        }
    }

    /**
     * This test validates that the server correctly processes AES-256 (v1) encoded credentials. Currently only BETA.
     * This is really a test for password decoding making use of the fact basic registry FAT
     * will exercise the test code. We also check the dynamism here to ensure when changing
     * config to have a new encoding key we pick it up dynamically.
     */
    @Test
    public void checkPasswordEncodedUsingAES256() throws Exception {
        Log.info(c, "checkPasswordEncodedUsingAES256", "Checking aes encoded credentials");

        setServerConfiguration(server, DEFAULT_AES_CONFIG_FILE);

        //The following errors/warnings are still expected because the server.xml still contains the AES-128 password
        if (server.isFIPS140_3EnabledAndSupported()) {
            assertNotNull("FIPS 140-3 should not allow AES-128bit secrets",
                          server.waitForStringInLog(CWWKS1860E_FIPS_128BIT_AES_SECRET_NOT_ALLOWED));
            expectedErrors.add(CWWKS1860E_FIPS_128BIT_AES_SECRET_NOT_ALLOWED);
        } else {
            if (!!!CWWKS1864wAlreadyLoggedForAES) {
                assertNotNull("AES-128bit password Should cause CWWKS1864W",
                              server.waitForStringInLog(CWWKS1864W_WEAK_ALGORITHM_WARNING));
                expectedErrors.add(CWWKS1864W_WEAK_ALGORITHM_WARNING);
                CWWKS1864wAlreadyLoggedForAES = true;
            }
        }

        String password = "superAES256password";
        assertEquals("Authentication should succeed.",
                     "defaultUserAES256", servlet.checkPassword("defaultUserAES256", password));

        passwordChecker.checkForPasswordInAnyFormat(password);

        setServerConfiguration(server, CUSTOM_AES_CONFIG_FILE);

        assertEquals("Authentication should succeed.",
                     "customUserAES256", servlet.checkPassword("customUserAES256", password));

        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * This test just validates that the server correctly processes hashed passwords
     * which was generated by old (<=25.0.0.2) default parameters.
     */
    @Test
    @AllowedFFDC(value = "com.ibm.websphere.crypto.InvalidPasswordEncodingException")
    public void checkPasswordEncodedUsingSHA1HashDefault() throws Exception {
        Log.info(c, "checkPasswordEncodedUsingSHA1HashDefault", "Checking hash encoded credentials");

        setServerConfiguration(server, DEFAULT_HASH_CONFIG_FILE);

        String GoodPassword = "pa$$w0rd";
        String BadPassword = "pa@@w0rd";
        String user = "hashedUser";

        if (server.isFIPS140_3EnabledAndSupported()) {
            try {
                servlet.checkPassword(user, GoodPassword);
                passwordChecker.checkForPasswordInAnyFormat(GoodPassword);
                fail("Expected IllegalArgumentException was not thrown.");
            } catch (IllegalArgumentException e) {
                Log.info(c, "checkPasswordEncodedUsingSHA1HashDefault", "Exception: " + e);
            }
            assertNotNull("FIPS 140-3 should not allow SHA1 password hash",
                          server.waitForStringInLog(CWWKS1863E_FIPS_SHA1_HASH_NOT_ALLOWED));
            expectedErrors.add(CWWKS1863E_FIPS_SHA1_HASH_NOT_ALLOWED);
            //Because SHA1 is not allowed, we're unable to process the hash, which will result in CWWKS1857E
            assertNotNull("FIPS 140-3 error CWWKS1863E should also cause CWWKS1857E: INVALID PASSWORD CIPHER",
                          server.waitForStringInLog(CWWKS1857E_INVALID_PASSWORD_CIPHER));
            expectedErrors.add(CWWKS1857E_INVALID_PASSWORD_CIPHER);
        } else {
            assertEquals("Authentication should succeed.",
                         user, servlet.checkPassword(user, GoodPassword));
            passwordChecker.checkForPasswordInAnyFormat(GoodPassword);

            if (!!!CWWKS1864wAlreadyLoggedForHash) {
                assertNotNull("SHA1 hash password Should cause CWWKS1864W",
                              server.waitForStringInLog(CWWKS1864W_WEAK_ALGORITHM_WARNING));
                expectedErrors.add(CWWKS1864W_WEAK_ALGORITHM_WARNING);
                CWWKS1864wAlreadyLoggedForHash = true;
            }

            assertNull("Authentication should fail.", servlet.checkPassword(user, BadPassword));
            passwordChecker.checkForPasswordInAnyFormat(BadPassword);
        }
    }

    /**
     * This test just validates that the server correctly processes hashed passwords
     * which was generated by new SHA512 (>25.0.0.2) default parameters.
     */
    @Test
    public void checkPasswordEncodedUsingSHA512HashDefault() throws Exception {
        Log.info(c, "checkPasswordEncodedUsingHash", "Checking hash encoded credentials");

        setServerConfiguration(server, DEFAULT_HASH_CONFIG_FILE);

        String GoodPassword = "sha512hashpassword";
        String BadPassword = "pa@@w0rd";
        String user = "hashedSHA512User";
        assertEquals("Authentication should succeed.",
                     user, servlet.checkPassword(user, GoodPassword));
        passwordChecker.checkForPasswordInAnyFormat(GoodPassword);

        assertNull("Authentication should fail.", servlet.checkPassword(user, BadPassword));
        passwordChecker.checkForPasswordInAnyFormat(BadPassword);

    }

    /**
     * This test just validates that the server correctly processes hashed passwords
     * which was generated by following parameters.
     * securityUtility encode --encoding=hash --iteration=500 --algorithm=PBKDF2WithHmacSHA256 WebAS
     * {hash}ARAAAAAUUEJLREYyV2l0aEhtYWNTSEEyNTYgAAAB9DAAAAAICc3/5EBfhnpAAAAAIGaOuUfsNQYb7+NIx8wU2Z7UgMuJEfRmmFLK24BQJGvk
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
     * Hit the test servlet to see if getUserDisplayName works.
     * This test is to verify defect 217071. The server XML includes federatedRegistry-1.0
     * to ensure that we test the fix for 217071.
     */
    @Test
    public void getUserDisplayName() throws Exception {
        Log.info(c, "getUserDisplayName", "Checking expected user display name");
        setServerConfiguration(server, DEFAULT_CONFIG_FILE);
        assertEquals("user1", servlet.getUserDisplayName("user1"));
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
            server.waitForStringInLog("CWWKG001[7-8]I");
            server.waitForStringInLog("CWWKZ0003I"); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
            serverConfigurationFile = serverXML;
        }
    }

    /**
     * Stops the server providing the expectedErrors list as input
     */
    private static void stopServer() throws IOException, Exception {
        server.stopServer(expectedErrors.toArray(new String[0]));
        CWWKS1864wAlreadyLoggedForHash = false;
        CWWKS1864wAlreadyLoggedForAES = false;
    }
}