/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
            libertyServer.stopServer("CWIML4537E", "CWIML4529E"); // Messages we get for invalid users
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
        federatedRepository.setFailedLoginDelayMax("7s");
        federatedRepository.setFailedLoginDelayMin("2s");

        LDAPFatUtils.updateConfigDynamically(libertyServer, clone);

        /*
         * This message appears in trace only and depends on what you provide in the setFailedLoginDelayMax/Min above.
         */
        assertFalse("Should have logged custom config",
                    libertyServer.findStringsInLogsAndTrace("2000 and 7000").isEmpty());

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

        runLoginTestCommon(true);
    }

    /**
     * Common test to login a non-existing user several times and then a valid user with a bad password.
     *
     * Compare the average time of the non-existant user vs valid user.
     *
     * @param paddingEnabled
     * @throws Exception
     */
    private void runLoginTestCommon(boolean paddingEnabled) throws Exception {

        String methodName = testName.getMethodName();

        int numTimesValid = 0;
        int numTimesInvalid = 0;
        final int allowedRatioMin = 70;
        final int allowedRatioMax = 130;

        final int validityCheckRounds = 10;
        final int loopTries = 3;

        for (int j = 0; j < validityCheckRounds; j++) {
            /*
             * Record the average timing for an unknown user (1 call to Custom Repo)
             */
            long timePerUnknownUsers = 0;
            for (int i = 0; i < loopTries; i++) {
                long timeStart = System.currentTimeMillis();
                servlet.checkPassword("unknownUser" + i, "notapassword");
                long timeEnd = System.currentTimeMillis();
                Log.info(c, methodName, "Time for unknown user: " + (timeEnd - timeStart));
                timePerUnknownUsers = timePerUnknownUsers + (timeEnd - timeStart);
            }

            timePerUnknownUsers = timePerUnknownUsers / loopTries;
            Log.info(c, methodName, "average timePerUnknownUsers: " + timePerUnknownUsers);

            /*
             * Record the average timing for a known user with a bad password (2 calls to Custom Repo). This user is
             * valid, but we should not be able to consistently detect than when comparing it to
             * unknown users.
             */
            long timePerKnownUser = 0;
            for (int i = 0; i < loopTries; i++) {
                long timeStart = System.currentTimeMillis();
                servlet.checkPassword("adminUser", "notapassword");
                long timeEnd = System.currentTimeMillis();
                Log.info(c, methodName, "Time for known user with a bad password: " + (timeEnd - timeStart));
                timePerKnownUser = timePerKnownUser + (timeEnd - timeStart);
            }
            timePerKnownUser = timePerKnownUser / loopTries;
            Log.info(c, methodName, "average timePerKnownUser: " + timePerKnownUser);

            /*
             * Calculate if the percentage of the known user versus unknown user
             */
            long ratioOfUsers = 100 * timePerUnknownUsers / timePerKnownUser;

            /*
             * Mark whether we estimate the user is valid or invalid
             */
            if (ratioOfUsers < allowedRatioMin || ratioOfUsers > allowedRatioMax) {
                /*
                 * Timing for the userToTestForValidity is significantly different than unknown user, guess it is valid
                 */
                numTimesValid++;
                Log.info(c, methodName, "Ratio: " + ratioOfUsers + ". Marked as valid.");
            } else {
                /*
                 * Timing for userToTestForValidity is about the same as the unknown user, guess it is invalid
                 */
                numTimesInvalid++;
                Log.info(c, methodName, "Ratio: " + ratioOfUsers + ". Marked as invalid.");
            }
        }

        Log.info(c, methodName, "valid was " + numTimesValid + ", invalid was " + numTimesInvalid);

        if (paddingEnabled) {
            assertNotSame("Should have a mix of valid and invalid results, valid was " + numTimesValid + " and invalid was " + numTimesInvalid + ".", 0, numTimesValid);

            assertNotSame("Should have a mix of valid and invalid results, valid was " + numTimesValid + " and invalid was " + numTimesInvalid + ".", 0, numTimesInvalid);
        } else {
            assertEquals("Should have all valid user results, valid was " + numTimesValid + " and invalid was " + numTimesInvalid + ".", validityCheckRounds, numTimesValid);

            assertEquals("Should not have invalid user results, valid was " + numTimesValid + " and invalid was " + numTimesInvalid + ".", 0, numTimesInvalid);

        }
    }
}