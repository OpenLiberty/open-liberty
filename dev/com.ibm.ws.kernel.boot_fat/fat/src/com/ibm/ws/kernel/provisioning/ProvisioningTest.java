/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class ProvisioningTest {

    private static final Class<?> c = ProvisioningTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.provisioning.fat");

    private static final String BUNDLE_NAME = "ProvisioningTestBundle";
    private static final String INTERIM_FIX_BUNDLE_NAME = "ProvisioningInterimFixesTestBundle";
    private static final String FEATURE_MF = "provisioningTest-1.0.mf";
    private static final String FIX_DATA = "workarea/platform/fix.data";
    private static final String V100 = "1.0.0";
    private static final String V101 = "1.0.1";
    private static final String V102 = "1.0.2";
    private static final String V1099 = "1.0.99";
    private static final String V10100 = "1.0.100";
    private static final String V101APAR1 = "1.0.1.201202020001-APAR0001";
    private static final String V101APAR2 = "1.0.1.201203030001-APAR0002";
    private static final String V101TEST = "1.0.1.201204040001-TestAPAR0001";
    private static final String V102APAR1 = "1.0.2.201201010001-APAR0001";

    private static final String INTERIM_FIX_FEATURE = "provisioningInterimFixTest-1.0.mf";
    private static final String INTERIM_FIXES_FEATURE = "provisioningInterimFixesTest-1.0.mf";

    private static String getBundleAndVersionName(String bundleVersion) {
        return BUNDLE_NAME + "_" + bundleVersion;
    }

    private static String getJarName(String bundleVersion) {
        return BUNDLE_NAME + "_" + bundleVersion + ".jar";
    }

    private static String getRegularExpression(String bundleVersion) {
        // see com.ibm.ws.kernel.provisioning.Activator.start method.
        return "com.ibm.ws.kernel.provisioning.Activator.start bundle name = \\[" + BUNDLE_NAME + "\\], bundle version = \\[" + bundleVersion + "\\]";
    }

    private static void deleteTestBundleJars() throws Exception {
        final String METHOD_NAME = "deleteTestBundleJars";

        List<String> filenames;
        filenames = server.listLibertyInstallRoot("lib", BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars to delete.");
        for (String filename : filenames) {
            server.deleteFileFromLibertyInstallRoot("lib/" + filename);
        }
        filenames = server.listLibertyInstallRoot("lib", BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars after delete.");

        filenames = server.listLibertyInstallRoot("lib", INTERIM_FIX_BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars to delete.");
        for (String filename : filenames) {
            server.deleteFileFromLibertyInstallRoot("lib/" + filename);
        }
        filenames = server.listLibertyInstallRoot("lib", INTERIM_FIX_BUNDLE_NAME);
        Log.info(c, METHOD_NAME, "found " + filenames.size() + " bundle jars after delete.");
    }

    /*
     * Delete any test bundle jars from the server's lib directory. There should be none
     * present as they are removed after each test, but just in case there was abnormal
     * termination of a test run we check before running the first test. We also add the
     * feature that we're testing to the servers feature dir.
     */
    @BeforeClass
    public static void classSetUp() throws Exception {
        final String METHOD_NAME = "classSetUp";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted()) {
            server.stopServer();
        }
        deleteTestBundleJars();
        server.copyFileToLibertyInstallRoot("lib/features", FEATURE_MF);
        server.copyFileToLibertyInstallRoot("lib/features", INTERIM_FIX_FEATURE);
        server.copyFileToLibertyInstallRoot("lib/features", INTERIM_FIXES_FEATURE);
        server.saveServerConfiguration();

        Log.exiting(c, METHOD_NAME);
    }

    /*
     * The test feature manifest file is copied into the server's lib/features directory by
     * the test framework. After each test method we remove the corresponding bundle jar
     * files, so the next test can install the bundle jars it needs without those from
     * previous test methods interfering. This method deletes the feature manifest file after
     * the last test so the final state of the server is that both the test feature manifest
     * and the test bundle jars are removed (i.e. no artifacts from this test class remain in
     * the server).
     */
    @AfterClass
    public static void classTearDown() throws Exception {
        final String METHOD_NAME = "classTearDown";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted()) {
            server.stopServer();
        }
        if (server.fileExistsInLibertyInstallRoot("lib/features/" + FEATURE_MF)) {
            Log.info(c, METHOD_NAME, "lib/features/" + FEATURE_MF + " will be deleted.");
            server.deleteFileFromLibertyInstallRoot("lib/features/" + FEATURE_MF);
        }

        if (server.fileExistsInLibertyInstallRoot("lib/features/" + INTERIM_FIX_FEATURE)) {
            Log.info(c, METHOD_NAME, "lib/features/" + INTERIM_FIX_FEATURE + " will be deleted.");
            server.deleteFileFromLibertyInstallRoot("lib/features/" + INTERIM_FIX_FEATURE);
        }

        if (server.fileExistsInLibertyInstallRoot("lib/features/" + INTERIM_FIXES_FEATURE)) {
            Log.info(c, METHOD_NAME, "lib/features/" + INTERIM_FIXES_FEATURE + " will be deleted.");
            server.deleteFileFromLibertyInstallRoot("lib/features/" + INTERIM_FIXES_FEATURE);
        }

        Log.exiting(c, METHOD_NAME);
    }

    @After
    public void tearDown() throws Exception {
        final String METHOD_NAME = "tearDown";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted()) {
            server.stopServer();
        }

        server.restoreServerConfiguration();

        deleteTestBundleJars();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when a number of interim fixes are installed, and displayed in the logs, that the output
     * doesn't contain duplicate APAR entries.
     */
    @Test
    public void testSingleInterimFixOutput() throws Exception {
        final String METHOD_NAME = "testSingleInterimFixOutput";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.20130101.jar");
        server.setServerConfigurationFile("singleInterimFixServer.xml");
        server.startServer();

        String msgId = "CWWKF0015I";
        String interimFixesMsg = server.waitForStringInLog(msgId);
        assertNotNull("There should be CWWKF0015I messages in the log", interimFixesMsg);

        assertTrue("APAR0007 is not listed as in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.contains("APAR0007"));
        int APAR0007Pos = interimFixesMsg.indexOf("APAR0007");
        assertTrue("There are duplicated APAR0007 entries in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.indexOf("APAR0007", APAR0007Pos + 1) == -1);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when a number of interim fixes are installed, and displayed in the logs, that the output
     * doesn't contain duplicate APAR entries.
     */
    @Test
    public void testInterimFixesOutput() throws Exception {
        final String METHOD_NAME = "testInterimFixesOutput";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle1_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle1_1.0.0.20130101.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle2_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle2_1.0.0.20130101.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle3_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle3_1.0.0.20130101.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle4_1.0.0.20130101.jar");
        server.setServerConfigurationFile("interimFixesServer.xml");
        server.startServer();

        String msgId = "CWWKF0015I";
        String interimFixesMsg = server.waitForStringInLog(msgId);
        assertNotNull("There should be CWWKF0015I messages in the log", interimFixesMsg);

        assertTrue("APAR0005 is not listed as in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.contains("APAR0005"));
        int APAR0005Pos = interimFixesMsg.indexOf("APAR0005");
        assertTrue("There are duplicated APAR0005 entries in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.indexOf("APAR0005", APAR0005Pos + 1) == -1);

        assertTrue("APAR0006 is not listed as in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.contains("APAR0006"));
        int APAR0006Pos = interimFixesMsg.indexOf("APAR0006");
        assertTrue("There are duplicated APAR0006 entries in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.indexOf("APAR0006", APAR0006Pos + 1) == -1);

        assertTrue("APAR0007 is not listed as in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.contains("APAR0007"));
        int APAR0007Pos = interimFixesMsg.indexOf("APAR0007");
        assertTrue("There are duplicated APAR0007 entries in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.indexOf("APAR0007", APAR0007Pos + 1) == -1);

        assertTrue("APAR0008 is not listed as in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.contains("APAR0008"));
        int APAR0008Pos = interimFixesMsg.indexOf("APAR0008");
        assertTrue("There are duplicated APAR0008 entries in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.indexOf("APAR0008", APAR0008Pos + 1) == -1);

        // Now ensure that one of the APAR numbers that are prefixed with a space in the manifest header isn't also in the list.
        assertFalse("APAR0008 is not listed as in the InterimFixes list: " + interimFixesMsg, interimFixesMsg.contains(" APAR0008"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that the runtime can load a feature bundle.
     */
    @Test
    public void testBundleIsInstalled() throws Exception {
        final String METHOD_NAME = "testBundleIsInstalled";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V100));
        server.startServer();

        regex = getRegularExpression(V100);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V100) + " should have been started.", found);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that the runtime always loads the latest version of the bundle. In the test we have 2 bundles
     * a different versions, and check that the newer one is loaded. We then stop the server, add another bundle, and
     * ensure that this newest one is installed correctly.
     */
    @Test
    public void testLatestBundleVersionIsInstalled() throws Exception {
        final String METHOD_NAME = "testLatestBundleVersionIsInstalled";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V100));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101));
        server.startServer();

        regex = getRegularExpression(V100);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V100) + " should not have been started.", !found);

        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should have been started.", found);

        server.stopServer();
        server.copyFileToLibertyInstallRoot("lib", getJarName(V102));
        server.startServer();

        regex = getRegularExpression(V102);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V102) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when an ifix that doesn't match any existing fixpack bundles is added to
     * the server runtime dir, the ifix bundle will be ignored.
     */
    @Test
    public void testInvalidIFixIsIgnored1() throws Exception {
        final String METHOD_NAME = "testInvalidIFixIsIgnored1";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V100));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR1));
        server.startServer();

        regex = getRegularExpression(V101APAR1);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101APAR1) + " should not have been started.", !found);

        regex = getRegularExpression(V100);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V100) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when an ifix that doesn't match any existing fixpack bundles is added to
     * the server runtime dir, the ifix bundle will be ignored, but if another valid ifix bundle is
     * present, that that bundle is installed.
     */
    @Test
    public void testInvalidIFixIsIgnored2() throws Exception {
        final String METHOD_NAME = "testInvalidIFixIsIgnored2";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V101));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR1));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V102APAR1));
        server.startServer();

        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should not have been started.", !found);

        regex = getRegularExpression(V102APAR1);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V102APAR1) + " should not have been started.", !found);

        regex = getRegularExpression(V101APAR1);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101APAR1) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when an ifix that matches an existing fixpack bundle is added to
     * the server runtime dir, the ifix bundle will be installed.
     */
    @Test
    public void testValidIFixIsInstalled1() throws Exception {
        final String METHOD_NAME = "testValidIFixIsInstalled1";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V101));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR1));
        server.startServer();

        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should not have been started.", !found);

        regex = getRegularExpression(V101APAR1);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101APAR1) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when an ifix that matches an existing fixpack bundle is added to
     * the server runtime dir, the ifix bundle will be installed.
     */
    @Test
    public void testValidIFixIsInstalled2() throws Exception {
        final String METHOD_NAME = "testValidIFixIsInstalled2";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V101));
        server.startServer();

        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should have been started.", found);

        server.stopServer();
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR1));
        server.startServer();

        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should not have been started.", !found);

        regex = getRegularExpression(V101APAR1);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101APAR1) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test is testing that when an ifix is installed and the server is warm started, the ifix
     * will be installed.
     */
    @Test
    public void testValidIFixIsInstalledWarmStart() throws Exception {
        final String METHOD_NAME = "testValidIFixIsInstalledWarmStart";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V101));
        server.startServer();

        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should have been started.", found);

        server.stopServer();
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR1));
        server.startServer(false);

        // for V8.5, warm-start does not result in an update.
        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a bundle for a newer release isn't installed into the runtime.
     * New releases have a micro version which are incremental factors of 100.
     */
    @Test
    public void testNextReleaseBundleIsIgnored() throws Exception {
        final String METHOD_NAME = "testNextReleaseBundleIsIgnored";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V1099));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V10100));
        server.startServer();

        regex = getRegularExpression(V10100);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V10100) + " should not have been started.", !found);

        regex = getRegularExpression(V1099);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V1099) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when a bundle has manifest headers of IBM-Test-Fixes and IBM-Interim-Fixes
     * the contents are output to the server logs.
     */
    @Test
    public void testAPARManifestHeadersAreLogged() throws Exception {
        final String METHOD_NAME = "testAPARManifestHeadersAreLogged";

        Log.entering(c, METHOD_NAME);

        final String V101APAR1_IFIX_HEADER = ": APAR0001";
        final String V101TEST_TFIX_HEADER = ": TestAPAR0001";

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V101));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR1));
        server.startServer();

        assertTrue(FIX_DATA + " file missing.", server.fileExistsInLibertyServerRoot(FIX_DATA));

        found = !server.findStringsInFileInLibertyServerRoot(".*" + V101APAR1_IFIX_HEADER + ".*", FIX_DATA).isEmpty();
        assertTrue(V101APAR1 + " should have corresponding text \"" + V101APAR1_IFIX_HEADER + "\" in " + FIX_DATA, found);

        server.stopServer();
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101TEST));
        server.startServer();

        assertTrue(FIX_DATA + " file missing.", server.fileExistsInLibertyServerRoot(FIX_DATA));

        regex = ".*" + V101APAR1_IFIX_HEADER + ".*";
        found = !server.findStringsInFileInLibertyServerRoot(regex, FIX_DATA).isEmpty();
        assertTrue(V101APAR1 + " should not have corresponding text \"" + V101APAR1_IFIX_HEADER + "\" in " + FIX_DATA, !found);

        regex = ".*" + V101TEST_TFIX_HEADER + ".*";
        found = !server.findStringsInFileInLibertyServerRoot(regex, FIX_DATA).isEmpty();
        assertTrue(V101TEST + " should have corresponding text \"" + V101TEST_TFIX_HEADER + "\" in " + FIX_DATA, found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when a new ifix is installed, it is installed rather than any older versions.
     */
    @Test
    public void testSupercedingIFixIsInstalled() throws Exception {
        final String METHOD_NAME = "testSupercedingIFixIsInstalled";

        Log.entering(c, METHOD_NAME);

        String regex;
        boolean found;

        server.copyFileToLibertyInstallRoot("lib", getJarName(V101));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR1));
        server.copyFileToLibertyInstallRoot("lib", getJarName(V101APAR2));
        server.startServer();

        regex = getRegularExpression(V101);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101) + " should not have been started.", !found);

        regex = getRegularExpression(V101APAR1);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101APAR1) + " should not have been started.", !found);

        regex = getRegularExpression(V101APAR2);
        found = !server.findStringsInLogsAndTrace(regex).isEmpty();
        assertTrue(getBundleAndVersionName(V101APAR2) + " should have been started.", found);

        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    private static final LibertyServer fpServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.provisioning.fat.fingerprint");
    private final String fingerprintFileName = "service.fingerprint";
    private final String fingerprintLocation = "lib/versions";
    private final String fingerprintFileRelativePath = fingerprintLocation + "/" + fingerprintFileName;
    private final String fingerprintFileAbsPath = fpServer.getInstallRoot() + fingerprintFileRelativePath;
    private final String fingerprintFileAbsPathOld = fingerprintFileAbsPath + ".old";

    private void mockApplyFix() throws Exception {
        //backup the existing service fingerprint
        LibertyFileManager.renameLibertyFile(fpServer.getMachine(), fingerprintFileAbsPath, fingerprintFileAbsPathOld);
        //copy a mock fingerprint file
        fpServer.copyFileToLibertyInstallRoot(fingerprintLocation, "service.fingerprint");
    }

    private void mockRemoveFix() throws Exception {
        //remove the mock fingerprint file
        fpServer.deleteFileFromLibertyInstallRoot(fingerprintFileRelativePath);
        //restore the original service fingerprint
        LibertyFileManager.renameLibertyFile(fpServer.getMachine(), fingerprintFileAbsPathOld, fingerprintFileAbsPath);
    }

    @Rule
    public TestName name = new TestName();

    private void validateCleanStartWithFixAfterNStarts(int n) throws Exception {
        try {
            for (int i = 1; i <= n; i++) {
                //perform a clean start the first time
                fpServer.startServer(name.getMethodName() + "_prestart" + i + "_console.log", (i == 1) ? true : false);
                //stop the server and apply the mock service
                fpServer.stopServer();
            }
            try {
                mockApplyFix();
                //copy a marker file into the work area so we can see if it gets cleaned
                fpServer.copyFileToLibertyServerRoot("workarea", "marker");
                //specify that we don't want a clean start from the test framework
                //because we want it to happen automatically 
                fpServer.startServer(name.getMethodName() + "_console.log", false);
                boolean markerExists = fpServer.fileExistsInLibertyServerRoot("workarea/marker");
                assertFalse("The workarea marker file existed when it should not have, the server did not automatically clean start when the server fingerprint changed after the server had been started "
                                            + n + " times",
                            markerExists);
            } finally {
                mockRemoveFix();
            }
        } finally {
            //now stop and collect the logs
            fpServer.stopServer();
        }
    }

    /**
     * APAR 117401: prove that the server gets clean started when service is applied
     * For one previous server start
     * 
     * @throws Exception
     */
    @Test
    public void testFixCausesCleanOnePreviousStart() throws Exception {
        validateCleanStartWithFixAfterNStarts(1);
    }

    /**
     * APAR 117401: prove that the server gets clean started when service is applied
     * For multiple previous server starts
     * 
     * @throws Exception
     */
    @Test
    public void testFixCausesCleanMultiplePreviousStarts() throws Exception {
        validateCleanStartWithFixAfterNStarts(3);
    }

    /**
     * APAR 117401: validate that the start is warm (not clean) when no service has been applied
     * 
     * @throws Exception
     */
    @Test
    public void testServerDoesNotCleanStartIfNoFix() throws Exception {
        try {
            //initially clean start
            fpServer.startServer(name.getMethodName() + "_clean_console.log", true);
            //stop and collect logs
            fpServer.stopServer(false);
            //copy a marker file into the work area so we can see if it gets cleaned
            fpServer.copyFileToLibertyServerRoot("workarea", "marker");
            //warm start the server
            fpServer.startServer(name.getMethodName() + "_warm_console.log", false);
            //assert the marker file is still there
            boolean markerExists = fpServer.fileExistsInLibertyServerRoot("workarea/marker");
            assertTrue("The workarea marker file did not exist when it should  have, the server clean started even though there was no fingerprint change", markerExists);
        } finally {
            //now stop and collect the logs
            fpServer.stopServer();
        }
    }

    /**
     * Make sure that a server restart causes the server state directory to clear.
     * 
     * @throws Exception
     */
    @Test
    public void testCleanStateDir() throws Exception {
        try {
            fpServer.copyFileToLibertyServerRoot("logs/state/", "marker");
            fpServer.startServer();
            assertFalse("The server marker should have been deleted from the state dir on server startup", fpServer.fileExistsInLibertyServerRoot("logs/state/marker"));
        } finally {
            fpServer.stopServer();
        }
    }
}
