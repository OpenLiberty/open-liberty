/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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

package com.ibm.ws.security.wim.registry.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPFatUtils;

/**
 * Regression tests for fixes made to the WIM UserRegistry component for user enumeration.
 *
 * This test uses a custom repository with sleeps and login to successfully differentiate on call times.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class UserEnumerationTest {

    @Server("com.ibm.ws.security.wim.registry.fat.UserEnumeration")
    public static LibertyServer libertyServer;

    private static final Class<?> c = UserEnumerationTest.class;
    private static UserRegistryServletConnection servlet;

    private static ServerConfiguration startConfiguration = null;

    @Rule
    public TestName testName = new TestName();

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {

        Log.info(c, "setupClass", "Starting the server... (will wait for userRegistry servlet to start)");
        libertyServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        libertyServer.addInstalledAppForValidation("userRegistry");

        libertyServer.installUserBundle("com.ibm.ws.security.wim.repository_test.custom.delay_1.0");
        libertyServer.installUserFeature("customRepositorySampleDelay-1.0");
        libertyServer.startServer(c.getName() + ".log");

        /*
         * Make sure the application has come up before proceeding
         */
        assertNotNull("Application userRegistry does not appear to have started.",
                      libertyServer.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      libertyServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      libertyServer.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setupClass", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(libertyServer.getHostname(), libertyServer.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }

        startConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        if (libertyServer != null) {
            libertyServer.stopServer("CWIML4537E", "CWIML4529E", "CWIMK0012W", "CWWKG0075E"); // Messages we get for invalid users, and bad config value
        }
    }

    /**
     * Test the default path which includes a randomization delay when a user fails to login. A valid user
     * with a bad password versus a user that doesn't exist should have inconsistent time return.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testUserEnumerationDefaultSettings() throws Exception {
        ServerConfiguration clone = startConfiguration.clone();

        FederatedRepository federatedRepository = clone.getFederatedRepository();
        federatedRepository.setFailedLoginDelayMax(null);
        federatedRepository.setFailedLoginDelayMin(null);

        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * Run logins with the default configuration. We should have inconsistent return times.
         */
        runLoginTestCommon(true);
    }

    /**
     * Test with the login delay disabled. A valid user
     * with a bad password will take longer than a user that doesn't exist.
     *
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testUserEnumerationDisabled() throws Exception {
        libertyServer.setTraceMarkToEndOfDefaultTrace();

        ServerConfiguration clone = startConfiguration.clone();

        FederatedRepository federatedRepository = clone.getFederatedRepository();
        federatedRepository.setFailedLoginDelayMax("0");
        federatedRepository.setFailedLoginDelayMin("0");

        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * This message appears in trace only.
         */
        assertFalse("Should have logged that delay is disabled",
                    libertyServer.findStringsInLogsAndTrace("failed response login delay is disabled: failedLoginDelayMax=0").isEmpty());

        assertFalse("Should have logged that delay is disabled with official warning - CWIMK0012W",
                    libertyServer.findStringsInLogsAndTrace("CWIMK0012W").isEmpty());

        runLoginTestCommon(false);
    }

    /**
     * Test with a custom failedLoginDelay which includes a randomization delay when a user fails to login. A valid user
     * with a bad password versus a user that doesn't exist should have inconsistent time return.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void testUserEnumerationCustomConfig() throws Exception {
        libertyServer.setTraceMarkToEndOfDefaultTrace();

        ServerConfiguration clone = startConfiguration.clone();

        FederatedRepository federatedRepository = clone.getFederatedRepository();
        federatedRepository.setFailedLoginDelayMax("4s");
        federatedRepository.setFailedLoginDelayMin("2ms");

        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * This message appears in trace only and depends on what you provide in the setFailedLoginDelayMax/Min above.
         */
        assertFalse("Should have logged custom config",
                    libertyServer.findStringsInLogsAndTrace("2 and 4000").isEmpty());

        runLoginTestCommon(true);
    }

    /**
     * Test invalid configurations for the FailedLoginDelayMax/Min. We should run with the default settings.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    @AllowedFFDC(value = { "java.lang.IllegalArgumentException" })
    public void testUserEnumerationInvalidValues() throws Exception {

        ServerConfiguration clone = startConfiguration.clone();
        /*
         * Reset to default config
         */
        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        libertyServer.setTraceMarkToEndOfDefaultTrace();

        /*
         * Set the min to be longer than the max -- we should trace and ignore this bad config and
         * run with the default settings.
         */
        FederatedRepository federatedRepository = clone.getFederatedRepository();
        federatedRepository.setFailedLoginDelayMax("5s");
        federatedRepository.setFailedLoginDelayMin("10s");

        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * This message appears in trace only and depends on what you provider in the setFailedLoginDelayMax/Min above.
         */
        assertFalse("Should have logged the config is invalid",
                    libertyServer.findStringsInLogsAndTrace("Provided config of, 10000:5000, was invalid. Using the default settings").isEmpty());

        runLoginTestCommon(true);

        libertyServer.setTraceMarkToEndOfDefaultTrace();

        clone = startConfiguration.clone();

        /*
         * Set a non-duration value. This should cause a general configuration failure that results in an
         * IllegalArgumentException FFDC. We should run with the default settings.
         */
        federatedRepository = clone.getFederatedRepository();
        federatedRepository.setFailedLoginDelayMax("BadValue");
        federatedRepository.setFailedLoginDelayMin("10s");

        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        assertNotNull("The BadValue for login delay did not cause an expected CWWKG0075E error.",
                      libertyServer.waitForStringInLog("CWWKG0075E:.*BadValue"));

        runLoginTestCommon(true);
    }

    /**
     * Common test to login a non-existing user and a known valid user with a bad password.
     *
     * Compare the average time of the non-existent user (not found in the Repo) vs known user (found in the Repo) and
     * calculate if we can guess the unknown user is valid/invalid based on the return time.
     *
     * @param paddingEnabled
     * @throws Exception
     */
    private void runLoginTestCommon(boolean paddingEnabled) throws Exception {

        String methodName = testName.getMethodName();

        int numTimesUserSeemsValid = 0;
        int numTimesUserSeemsInvalid = 0;
        final int allowedRatioMin = 50;

        final int validityCheckRounds = 10;
        final int retry = 2;

        for (int i = 0; i < retry; i++) {

            /*
             * Since I switched to the CustomRepo instead of LDAP, we were sometimes
             * sampling ourselves to extra average. Try 1 to 1 comparison.
             *
             * With an unknown user and without UserEnum padding, we should return very quickly compared to the known user.
             *
             * With an unknown user and with UserEnum padding, we should return both quickly and similar or longer than the known user.
             */

            for (int j = 0; j < validityCheckRounds; j++) {
                /*
                 * Record the average timing for an unknown user, results in a search in the Custom Repo
                 */
                long timeStart = System.currentTimeMillis();
                servlet.checkPassword("unknownUser", "notapassword");
                long timeEnd = System.currentTimeMillis();
                long timePerUnknownUsers = timeEnd - timeStart;
                Log.info(c, methodName, "average timePerUnknownUsers: " + timePerUnknownUsers);

                /*
                 * Record the average timing for a known user with a bad password, results in a search, sleep, password check in the Custom
                 * Repo (to simulate 2 calls to Ldap, search + checkpassword).
                 */
                timeStart = System.currentTimeMillis();
                servlet.checkPassword("adminUser", "notapassword");
                timeEnd = System.currentTimeMillis();
                long timePerKnownUser = (timeEnd - timeStart);
                Log.info(c, methodName, "average timePerKnownUser: " + timePerKnownUser);

                /*
                 * Calculate if the percentage of the known user versus unknown user
                 *
                 * For an unknown user, without padding, we expect it will take much less time than a known user takes.
                 * For example, we expect an unknown user to take <50% of the time it takes a known user without padding.
                 */
                long ratioOfUsers = 100 * timePerUnknownUsers / timePerKnownUser;

                /*
                 * Mark whether we estimate the user is valid or invalid, compared to the known user timing
                 *
                 * If padding is not enabled, we should always detect the unknown user is invalid as it should
                 * take significantly less than the known user.
                 *
                 * If padding is enabled, we should get variable return times on the unknown user compared to the known user,
                 * making the unknown user appear both valid and invalid.
                 */
                if (ratioOfUsers < allowedRatioMin) {
                    /*
                     * Timing for the unknownUser is significantly less than the known user, guess it is invalid (user
                     * not found)
                     */
                    numTimesUserSeemsInvalid++;
                    Log.info(c, methodName, "Ratio: " + ratioOfUsers + ". Marked as invalid.");
                } else {
                    /*
                     * Timing for unknownUser is about the same or longer than the known user, guess it is valid (user
                     * is found and we did a check password)
                     */
                    numTimesUserSeemsValid++;
                    Log.info(c, methodName, "Ratio: " + ratioOfUsers + ". Marked as valid.");
                }
            }

            Log.info(c, methodName, "valid was " + numTimesUserSeemsValid + ", invalid was " + numTimesUserSeemsInvalid);

            if (paddingEnabled) {
                if ((numTimesUserSeemsValid == 0 || numTimesUserSeemsInvalid == 0) && i < retry) {
                    Log.info(c, methodName, "Valid and invalid results not mixed as expected, try again.");
                } else {
                    assertNotSame("Should have a mix of valid and invalid results, valid was " + numTimesUserSeemsValid + " and invalid was " + numTimesUserSeemsInvalid + ".", 0,
                                  numTimesUserSeemsValid);

                    assertNotSame("Should have a mix of valid and invalid results, valid was " + numTimesUserSeemsValid + " and invalid was " + numTimesUserSeemsInvalid + ".", 0,
                                  numTimesUserSeemsInvalid);
                }
            } else {
                if ((validityCheckRounds != numTimesUserSeemsInvalid) && i < retry) {
                    Log.info(c, methodName, "Expected all invalid results, try again.");
                } else {
                    assertEquals("Should have all invalid user results, valid was " + numTimesUserSeemsValid + " and invalid was " + numTimesUserSeemsInvalid + ".",
                                 validityCheckRounds,
                                 numTimesUserSeemsInvalid);

                    assertEquals("Should not have valid user results, valid was " + numTimesUserSeemsValid + " and invalid was " + numTimesUserSeemsInvalid + ".", 0,
                                 numTimesUserSeemsValid);
                }
            }

        }
    }

}