/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.commandline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class DumpCommandTest {
    private static final Class<?> c = DumpCommandTest.class;

    @Rule
    public final TestName testName = new TestName();

    private static LibertyServer server;
    private static File serverRoot;
    private static boolean isIBM_JVM = false;

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.shutdown.fat");
        ShrinkHelper.defaultApp(server, "shutdownfat", "com.ibm.ws.kernel.boot.fat");
        Log.info(c, "before", "starting server");
        server.startServer("DumpCommandTest.log");
        serverRoot = new File(server.getServerRoot());

        String javaHome = server.getMachineJavaJDK();
        Properties env = new Properties();
        env.setProperty("JAVA_HOME", javaHome);
        String javaBinDir = javaHome + "/bin";
        ProgramOutput javaVersionOutput = server.getMachine().execute(javaBinDir + "/java", new String[] { "-version" }, javaBinDir, env);
        String stdout = javaVersionOutput.getStdout();
        String stderr = javaVersionOutput.getStderr();
        Log.info(c, "before", "java -version  stdout: " + stdout);
        Log.info(c, "before", "java -version  stderr: " + stderr);
        assertEquals("Unexpected return code from java -version", 0, javaVersionOutput.getReturnCode());

        if ((stdout != null && stdout.contains("IBM J9 VM")) || (stderr != null && stderr.contains("IBM J9 VM"))) {
            isIBM_JVM = true;
        }
    }

    @AfterClass
    public static void after() throws Exception {
        // We stop the server by other means, but call stopServer in order to
        // save logs, reset log offsets, etc.
        server.stopServer();
    }

    /**
     * Tests the command line tool: server javadump &lt;serverName&gt;
     * Test will pass if the command executes successfully and (if using an
     * IBM JVM) creates a file, "javacore*.txt", in the server root dir.
     * Non-IBM JDKs do not produce javacores - and the javadump command
     * does not usually return successfully on these platforms, so this test
     * only runs on IBM JDKs.
     */
    @Test
    public void testJavadump() throws Exception {
        assumeTrue(isIBM_JVM);
        File[] filesBeforeDump = serverRoot.listFiles();
        Log.info(c, "testJavadump", "javadump");
        ProgramOutput output = server.javadumpThreads();
        assertEquals("Unexpected output code running javadump", 0, output.getReturnCode());

        assertNotNull("Did not see expected CWWKE0068I in logs", server.waitForStringInLog("CWWKE0068I.*javacore"));

        File[] filesAfterDump = serverRoot.listFiles();
        assertTrue("Did not find expected additional files in serverRoot: " + serverRoot.getAbsolutePath(),
                   filesBeforeDump.length < filesAfterDump.length);
        boolean foundJavacoreFile = false;
        for (File f : filesAfterDump) {
            String fileName = f.getName();
            Log.info(c, "testJavadump", "Found file: " + fileName);
            if (fileName.startsWith("javacore") && fileName.endsWith(".txt")) {
                foundJavacoreFile = true;
                break;
            }
        }
        assertTrue("Expected javacore*.txt file not found in serverRoot: " + serverRoot.getAbsolutePath(), foundJavacoreFile);

        Log.info(c, "testJavadump", "exit success");
    }

    /**
     * Verifies that the JEP 472 restricted-method warning
     * ("A restricted method in java.lang.System has been called") is NOT present
     * in the stdout of the server dump command on z/OS with Java 24+.
     *
     * On z/OS, the client JVM launched by "server dump" opens a log file via
     * TaggedFileOutputStream, which triggers NativeMethodHelper.loadAndInitialize()
     * and System.loadLibrary(). Without --enable-native-access=ALL-UNNAMED on the
     * client JVM command line, Java 24+ emits a JEP 472 warning to stdout.
     */
    @Test
    public void testNoJep472WarningOnDump() throws Exception {
        final String METHOD_NAME = "testNoJep472WarningOnDump";

        // Skip on non-z/OS or Java < 24: JEP 472 restricted-method warnings only appear there
        assumeTrue(server.getMachine().getOperatingSystem() == OperatingSystem.ZOS
                   && JavaInfo.forServer(server).majorVersion() >= 24);

        Log.info(c, METHOD_NAME, "running server dump");
        ProgramOutput output = server.serverDump();

        String stdout = output.getStdout();
        Log.info(c, METHOD_NAME, "server dump stdout = " + stdout);
        Log.info(c, METHOD_NAME, "server dump stderr = " + output.getStderr());

        assertFalse("JEP 472 restricted-method warning found in 'server dump' stdout -- "
                    + "--enable-native-access=ALL-UNNAMED may be missing from the client JVM command line",
                    stdout.contains("A restricted method in java.lang.System has been called"));

        Log.info(c, METHOD_NAME, "exit success");
    }

    /**
     * Tests the command line tool: server dump &lt;serverName&gt; --include=heap,system,thread
     * Test will pass if the command executes successfully and creates a file,
     * "dump.&lt;serverName&gt;.dump*.zip", in the server root dir. Non-IBM JDKs do not produce
     * artifacts like javacores, heap dumps, system cores in the same way as IBM JDKs - and the
     * dump command does not usually return successfully on these platforms, so this test
     * only runs on IBM JDKs.
     */
    @Test
    public void testDump() throws Exception {
        String method = "testDump";
        assumeTrue(isIBM_JVM);
        File[] filesBeforeDump = serverRoot.listFiles();
        Log.info(c, method, "dump");
        ProgramOutput output = server.serverDump("heap,system,thread");

        assertEquals("Unexpected output code running dump", 0, output.getReturnCode());

        assertNotNull("Did not find expected CWWKE0068I message for heap dump in logs", server.waitForStringInLog("CWWKE0068I.*phd")); //heap dump
        if (server.getMachine().getOperatingSystem() == OperatingSystem.ZOS) {
            assertNotNull("Did not find expected CWWKE0092I message for java system dump on zos in logs", server.waitForStringInLogUsingMark("CWWKE0092I"));
        } else {
            // On Linux systems (like CENTOS, RHEL, SUSE) if there is a proc/sys/kernel/core_pattern which pipes the core to some other program
            // the core file will not be found as the JVM cannot rename it.  In that case we should skip checking for the "CWWKE0068I.*dmp" message.
            if (server.waitForStringInLogUsingMark("CWWKE0068I.* was not found") == null) {
                assertNotNull("Did not find expected CWWKE0068I message for system core in logs", server.waitForStringInLogUsingMark("CWWKE0068I.*dmp")); //system core
            } else {
                Log.info(c, method, "Ignoring System core dump since it is being re-directed.");
            }
        }
        assertNotNull("Did not find expected CWWKE0068I message for javacore in logs", server.waitForStringInLogUsingMark("CWWKE0068I.*javacore")); //javacore

        File[] filesAfterDump = serverRoot.listFiles();
        assertTrue("Did not find expected additional files in serverRoot: " + serverRoot.getAbsolutePath(),
                   filesBeforeDump.length < filesAfterDump.length);
        String serverName = server.getServerName();
        boolean foundDumpZip = false;
        for (File f : filesAfterDump) {
            String fileName = f.getName();
            Log.info(c, method, "Found file: " + fileName);
            if (fileName.startsWith(serverName + ".dump") && fileName.endsWith(".zip")) {
                foundDumpZip = true;
                break;
            }
        }
        assertTrue("Expected <serverName>.dump*.zip file not found in serverRoot: " + serverRoot.getAbsolutePath(), foundDumpZip);

        Log.info(c, method, "exit success");
    }
}
