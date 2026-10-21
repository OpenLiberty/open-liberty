/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.utility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * FAT test class for the securityUtility tlsProfiler command.
 *
 * Tests run against a live Liberty server (TLSProfilerTestServer) that has
 * the transportSecurity-1.0 feature enabled so that real TLS handshakes can
 * be performed and profiled.
 */
@RunWith(FATRunner.class)
public class SecurityUtilityTLSProfilerTest {
    private static final Class<?> thisClass = SecurityUtilityTLSProfilerTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TLSProfilerTestServer");

    /** Isolated server used only by {@link #testUntrustedCertificateFails} so its log is free of noise from other tests. */
    private static LibertyServer untrustedCertServer = LibertyServerFactory.getLibertyServer("TLSProfilerUntrustedCertServer");

    private static Machine testMachine;
    private static Properties testEnvironment;
    private static String libertyInstallRoot;
    private static String securityUtilityPath;
    private static int httpsPort;

    // Return codes
    private static final int SUCCESS_RC = 0;
    private static final int FAILURE_RC = 1;

    // Reusable test constants
    private static final String HOST = "localhost";
    private static final int SHORT_TIMEOUT_SECS = 30;
    private static final int DUMMY_PORT = 8020;

    // Liberty keystore defaults
    private static final String DEFAULT_KEYSTORE_FILE = "resources/security/key.p12";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "Liberty";
    private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";

    // Alternate server configuration files (under publish/files/TLSProfilerTestServer/)
    private static final String SERVER_XML_TLSv11 = "serverTLSv11.xml";
    private static final String SERVER_XML_TLSv13 = "serverTLSv13.xml";
    /** Config updated/completed message logged by Liberty whenever server.xml is reloaded. */
    private static final String CONFIG_UPDATE_MSG = "CWWKG001[7-8]I";

    @Rule
    public TestName testName = new TestName();

    @Rule
    public final TestRule logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n@@@@@@@@@@@@@@@@@\nExiting test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    //--------------------------------------------------------------------------
    // Class-level setup / teardown
    //--------------------------------------------------------------------------

    @BeforeClass
    public static void setUpClass() throws Exception {
        server.startServer(true);

        assertServerReady();

        libertyInstallRoot = server.getInstallRoot();
        securityUtilityPath = libertyInstallRoot + "/bin/securityUtility";
        testMachine = server.getMachine();
        httpsPort = server.getHttpDefaultSecurePort();

        // Point the securityUtility JVM at the Liberty server's auto-generated keystore so
        // that TLS handshakes with the self-signed certificate succeed
        String serverKeystorePath = server.getServerRoot() + File.separator + DEFAULT_KEYSTORE_FILE;
        
        String jvmArgs = "-Djavax.net.ssl.trustStore=" + serverKeystorePath
                         + " -Djavax.net.ssl.trustStorePassword=" + DEFAULT_KEYSTORE_PASSWORD
                         + " -Djavax.net.ssl.trustStoreType=" + DEFAULT_KEYSTORE_TYPE;

        testEnvironment = new Properties();
        testEnvironment.setProperty("JVM_ARGS", jvmArgs);

        Log.info(thisClass, "setUpClass",
                 "libertyInstallRoot=" + libertyInstallRoot + ", httpsPort=" + httpsPort
                 + ", keystorePath=" + serverKeystorePath);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (server != null && server.isStarted()) {
            // CWWKO0801E (SSL connection cannot be initialized) is expected when TLS versions aren't compatible
            server.stopServer("CWWKO0801E");
        }

        // Remove any stray tlsProfiler output files left in the install root
        if (libertyInstallRoot != null) {
            File installDir = new File(libertyInstallRoot);
            File[] stray = installDir.listFiles(
                    (dir, name) -> name.startsWith("tlsProfiler") && name.endsWith(".txt"));
            if (stray != null) {
                for (File f : stray) {
                    boolean deleted = f.delete();
                    Log.info(thisClass, "tearDownClass",
                             "Deleted stray file: " + f.getName() + " — success: " + deleted);
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // Per-test setup / teardown
    //--------------------------------------------------------------------------

    /**
     * Restore the server to the default TLSv1.3 configuration before each test
     * so that TLS-version-specific tests start from a known baseline.
     */
    @Before
    public void resetServerConfig() throws Exception {
        switchServerConfig(SERVER_XML_TLSv13);
    }

    //--------------------------------------------------------------------------
    // Helper methods
    //--------------------------------------------------------------------------

    /**
     * Swap the live {@code server.xml} to one of the alternate configurations
     * stored under {@code publish/files/TLSProfilerTestServer/}
     */
    private static void switchServerConfig(String configFileName) throws Exception {
        server.setMarkToEndOfLog();
        // Copy from LibertyFATTestFiles/TLSProfilerTestServer/<name> into serverRoot/
        server.copyFileToLibertyServerRoot(server.pathToAutoFVTTestFiles + "TLSProfilerTestServer", null, configFileName);
        // Rename the copied file to server.xml so Liberty picks up the change
        server.renameLibertyServerRootFile(configFileName, "server.xml");
        assertNotNull("Config update did not complete after switching to " + configFileName,
                      server.waitForStringInLogUsingMark(CONFIG_UPDATE_MSG));
    }

    private static void assertServerReady() throws Exception {
        assertNotNull("Feature update did not complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("SSL certificate not created; HTTPS port not yet open",
                      server.waitForStringInLogUsingMark("CWPKI0803A"));
    }

    /**
     * Execute the tlsProfiler command targeting {@code localhost} on the server's
     * HTTPS port with any additional arguments appended.
     */
    private ProgramOutput runTlsProfiler(String... extraArgs) throws Exception {
        String[] cmd = new String[3 + extraArgs.length];
        cmd[0] = "tlsProfiler";
        cmd[1] = "--host=" + HOST;
        cmd[2] = "--port=" + httpsPort;
        System.arraycopy(extraArgs, 0, cmd, 3, extraArgs.length);

        ProgramOutput output = testMachine.execute(
                securityUtilityPath, cmd, libertyInstallRoot, testEnvironment, 120);

        Log.info(thisClass, "runTlsProfiler",
                 "stdout:\n" + output.getStdout()
                 + "\nstderr:\n" + output.getStderr()
                 + "\nRC: " + output.getReturnCode());
        return output;
    }

    //--------------------------------------------------------------------------
    // Tests
    //--------------------------------------------------------------------------

    /**
     * Tests tlsProfiler on a server with TLSv1.1, an unsafe/outdated version, to confirm it runs 
     * with no output. It then swaps the servers version to TLSv1.3, a safe version, to confirm that
     * there is output.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDifferentTLSVersionProfiling() throws Exception {
        // TLSv1.1 server — profiler completes but no successful handshakes
        switchServerConfig(SERVER_XML_TLSv11);
        ProgramOutput tlsv11Output = runTlsProfiler();

        Log.info(thisClass, testName.getMethodName(), "TLSv1.1 stdout:\n" + tlsv11Output.getStdout());

        assertEquals("TLSv1.1 server should exit cleanly", SUCCESS_RC, tlsv11Output.getReturnCode());
        assertTrue("stdout should be empty when no handshakes succeed (TLSv1.1 server)",
                tlsv11Output.getStdout().trim().isEmpty());

        // TLSv1.3 server — profiler must report successful handshakes
        switchServerConfig(SERVER_XML_TLSv13);
        ProgramOutput tlsv13Output = runTlsProfiler();

        Log.info(thisClass, testName.getMethodName(), "TLSv1.3 stdout:\n" + tlsv13Output.getStdout());

        assertEquals("TLSv1.3 server should exit cleanly", SUCCESS_RC, tlsv13Output.getReturnCode());

        assertTrue("stdout should list TLSv1.3 as a successful protocol",
                tlsv13Output.getStdout().contains("TLSv1.3"));
        assertFalse("stdout must NOT list TLSv1.1 as successful (server is TLSv1.3-only)",
                tlsv13Output.getStdout().contains("TLSv1.1"));
    }

    /**
     * {@code --verbose} must produce both the successful-handshakes and the
     * unsuccessful-handshakes sections.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testVerboseOutputReportsUnsuccessfulHandshakes() throws Exception {
        ProgramOutput output = runTlsProfiler("--verbose");
        assertEquals(SUCCESS_RC, output.getReturnCode());
        assertTrue("--verbose stdout should contain successful-handshakes section",
                   output.getStdout().contains("Successful handshakes"));
        assertTrue("--verbose stdout should contain unsuccessful-handshakes section",
                   output.getStdout().contains("Unsuccessful handshakes"));
    }

    /**
     * Tests that when {@code --file} is given, the profiler writes results to that path.
     * If there is already a conflicting file in the path, the task should not
     * overwrite that file, it should create and write to an alternate file instead.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testFileOutputContainsProbeResultsAndHandlesConflict() throws Exception {
        File outFile = new File(libertyInstallRoot + "/tlsProfiler-results-test.txt");
        File numberedFile = new File(libertyInstallRoot + "/tlsProfiler-results-test1.txt");

        // Pre-create the file to exercise conflict-resolution
        outFile.createNewFile();

        ProgramOutput output = runTlsProfiler("--file=" + outFile.getAbsolutePath());
        assertEquals(SUCCESS_RC, output.getReturnCode());

        // The original file must still exist (not overwritten)
        assertTrue("Original (conflicting) file should still exist", outFile.exists());

        // The alternate file must exist and contain probe results
        assertTrue("Alternate file should have been created", numberedFile.exists());

        String fileContent = new String(Files.readAllBytes(numberedFile.toPath()));

        assertTrue("Output file should list TLSv1.3 as a successful protocol",
                   fileContent.contains("TLSv1.3"));
    }

    /**
     * When the profiler's JVM has no trust for the server's self-signed certificate, every TLS
     * handshake fails and the server logs {@code CWWKO0801E} for each rejected connection.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testUntrustedCertificateFails() throws Exception {

        untrustedCertServer.startServer();
        assertNotNull("SSL certificate not ready on untrusted-cert server",
                      untrustedCertServer.waitForStringInLogUsingMark("CWPKI0803A"));
        int certServerPort = untrustedCertServer.getHttpSecondarySecurePort();

        try {
            untrustedCertServer.setMarkToEndOfLog();

            Log.info(thisClass, testName.getMethodName(), "running on localhost: " + certServerPort);

            // Run without the JVM_ARGS that inject our trust store — every handshake
            // will be rejected, causing the server to log CWWKO0801E.
            ProgramOutput output = testMachine.execute(
                    securityUtilityPath,
                    new String[] { "tlsProfiler", "--host=" + HOST, "--port=" + certServerPort },
                    libertyInstallRoot, new Properties(), SHORT_TIMEOUT_SECS);

            Log.info(thisClass, testName.getMethodName(),
                     "RC: " + output.getReturnCode() + "\nstdout:\n" + output.getStdout() + "\nstderr:\n" + output.getStderr());

            assertNotNull("Server should log CWWKO0801E with 'certificate_unknown' when the profiler connects without a trusted certificate",
                          untrustedCertServer.waitForStringInLogUsingMark("CWWKO0801E.*certificate_unknown", 3000));
        } finally {
            // CWWKO0801E (SSL connection cannot be initialized) - server is expecting certificate_unknown
            untrustedCertServer.stopServer("CWWKO0801E");
        }
    }

    /**
     * Pointing at a port on which nothing is listening causes the profiler to
     * fail on the connect attempt and exit non-zero.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUnreachablePortFails() throws Exception {
        ProgramOutput output = testMachine.execute(
                securityUtilityPath,
                new String[] { "tlsProfiler", "--host=" + HOST, "--port=19999" },
                libertyInstallRoot, testEnvironment, SHORT_TIMEOUT_SECS);

        Log.info(thisClass, testName.getMethodName(), "RC: " + output.getReturnCode() + "\nstdout:\n" + output.getStdout());

        assertEquals("Unreachable target should exit with failure", FAILURE_RC, output.getReturnCode());
    }
}
