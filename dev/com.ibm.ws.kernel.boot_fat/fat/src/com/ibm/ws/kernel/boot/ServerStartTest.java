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
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server startup process.
 */
public class ServerStartTest {
    private static final Class<?> c = ServerStartTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.serverstart.fat";
    private static final String SYMLINK_NAME = "com.ibm.ws.kernel.boot.serverstart.fat.symlink";
    private static boolean isIBM_v8_JVM = false;

    private static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

        // IBM J9 does not contain the jcmd applicaiton to create dumps!
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
            isIBM_v8_JVM = true;
        }
    }

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    /**
     * This test validates that the server start script functions correctly when the CDPATH environment variable
     * is present.
     *
     * @throws Exception
     */
    public void testServerStartWithCDPATH() throws Exception {
        final String METHOD_NAME = "testServerStartWithCDPATH";
        Log.entering(c, METHOD_NAME);

        // issuing the command from the Liberty install root while supplying the bin directory as
        // part of the command itself causes the server script to cd to the bin directory, which
        // is where we noticed problems when CDPATH is set
        String executionDir = server.getInstallRoot();
        String command = "bin" + File.separator + "server";

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    /**
     * This test validates that the server start functions correctly when the server is referenced
     * via a symbolic link (on those systems which support same)
     *
     * @throws Exception
     */
    public void testServerStartViaSymbolicLink() throws Exception {
        String linkCommand = "/bin/ln";

        // only try this test if the unix ln command exists and can be executed
        File ln = new File(linkCommand);
        if (ln.exists() && ln.canExecute()) {
            // Copy the server directory elsewhere
            String originalPath = server.getServerRoot();
            String relocatedPath = server.getServerRoot() + "/../../" + server.getServerName();
            String copyCommand = "cp";
            String[] copyParms = new String[] { "-r", originalPath, relocatedPath }; // Make sure -r works on all these platforms...
            ProgramOutput po = server.getMachine().execute(copyCommand, copyParms);

            // Set up the symlink
            String symPath = server.getServerRoot() + "/../" + SYMLINK_NAME;
            String unlinkCommand = "rm";
            String[] unlinkParms = new String[] { symPath };
            po = server.getMachine().execute(unlinkCommand, unlinkParms);

            String[] linkParms = new String[] { "-s", relocatedPath, symPath }; // Symbolic link relocated copy as
            po = server.getMachine().execute(linkCommand, linkParms);

            // Get a handle to the alias.Note that this must use a new entry point to get a handle to an already configured server.
            LibertyServer symlink_server = LibertyServerFactory.getExistingLibertyServer(SYMLINK_NAME);

            // Confirm it can start and stop
            if (server.isStarted()) {
                server.stopServer(); // Pause the original
            }
            symlink_server.startServer(); // Start the alias (and confirm)
            symlink_server.stopServer(); // stop the alias (and confirm)
            server.startServer(); // Resume original (in case following test needs it).
        }

    }

    @Test
    /**
     * This test ensures that the servers starts without error when using the -Xfuture command line
     * argument. This argument enforces strict class file verification. One customer ran into
     * BundleExceptions due to empty package-info.class files (defect 177872).
     */
    public void testServerStartWithDashXFutureCmdArg() throws Exception {

        if (server.getMachine().getOperatingSystem() != OperatingSystem.WINDOWS) {
            server.copyFileToTempDir("server.xml", "origServer.xml");
            try {
                server.setServerConfigurationFile("Xfuture/server.xml");
                server.copyFileToLibertyServerRoot("Xfuture/jvm.options");

                server.startServer();
            } finally {
                server.deleteDirectoryFromLibertyServerRoot("jvm.options");
                server.setServerConfigurationFile("tmp/origServer.xml");
            }
        } else {
            // Windows, so we skip this test.
            assumeTrue(false);
        }
    }

    /**
     * This test validates that the server start script functions correctly when the SERVER_WORKING_DIR
     * environment variable is set. Output from the JVM (ex, javadumps) should be written to the value
     * specified. This can be an absolute path (c:\ or /) or relative path (../ or folder). If relative,
     * it is from the ${server.output.dir} location.
     *
     * This test case is for an absolute path which should resolve to ${server.output.dir}/logs/serverWorkingDir/.
     *
     * @throws Exception
     */
    @Test
    public void test_SWD_AbsolutePath() throws Exception {
        assumeTrue(!isIBM_v8_JVM);
        final String METHOD_NAME = "test_SWD_AbsolutePath";
        Log.entering(c, METHOD_NAME);

        String executionDir = server.getInstallRoot();
        String command = null;
        String javaCoreLocation = null;

        // Cover executing both scripts that utilize this environment variable
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            command = "bin" + File.separator + "server.bat";
            javaCoreLocation = server.getServerRoot().replace("/", "\\") + File.separator + "logs" + File.separator + "serverWorkingDir";
        } else {
            command = "bin" + File.separator + "server";
            javaCoreLocation = server.getServerRoot() + File.separator + "logs" + File.separator + "serverWorkingDir";
        }

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("SERVER_WORKING_DIR", javaCoreLocation);
        Log.info(c, METHOD_NAME, "SERVER_WORKING_DIR = " + javaCoreLocation);

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        // Use jcmd to generate a heap dump
        String[] execParameters = new String[] { "jcmd", findServerPid(), "GC.heap_dump", METHOD_NAME + ".hprof" };
        Process process = Runtime.getRuntime().exec(execParameters);
        try (Reader reader = new InputStreamReader(process.getInputStream());
                        BufferedReader br = new BufferedReader(reader);) {
            String output = null;
            while ((output = br.readLine()) != null) {
                Log.info(c, METHOD_NAME, "jcmd output = " + output);
            }
        }
        assertEquals("Jcmd didn't return 0.  See jcmd output above for troubleshooting", 0, process.waitFor());

        // Make sure we got the java heap dump at the expected location
        File core = new File(javaCoreLocation + File.separator + METHOD_NAME + ".hprof");
        assertTrue("The heap file did not exist at location = " + core.getAbsolutePath(), core.exists());
        Log.info(c, METHOD_NAME, "Removing file = " + core.getAbsolutePath());
        core.delete();

        // Stop the server
        parms[0] = "stop";
        parms[1] = SERVER_NAME;

        po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server stop stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKE0036I");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test validates that the server start script functions correctly when the SERVER_WORKING_DIR
     * environment variable is set. Output from the JVM (ex, javadumps) should be written to the value
     * specified. This can be an absolute path (c:\ or /) or relative path (../ or folder). If relative,
     * it is from the ${server.output.dir} location.
     *
     * This test case is for a relative path which should resolve to ${server.output.dir}/../../serverWorkingDir
     *
     * @throws Exception
     */
    @Test
    public void test_SWD_RelativePath() throws Exception {
        assumeTrue(!isIBM_v8_JVM);
        final String METHOD_NAME = "test_SWD_RelativePath";
        Log.entering(c, METHOD_NAME);

        String executionDir = server.getInstallRoot();
        String command = null;
        String javaCoreLocation = null;
        String relativePath = ".." + File.separator + ".." + File.separator + "serverWorkingDir";
        String absolutePath = null;

        // Cover executing both scripts that utilize this environment variable
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            command = "bin" + File.separator + "server.bat";
            javaCoreLocation = server.getServerRoot().replace("/", "\\").trim() + File.separator + relativePath;
            absolutePath = server.getUserDir().replace("/", "\\") + File.separator + "serverWorkingDir";
        } else {
            command = "bin" + File.separator + "server";
            javaCoreLocation = server.getServerRoot() + File.separator + relativePath;
            absolutePath = server.getUserDir() + File.separator + "serverWorkingDir";
        }

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        // This test location equates to /wlp/usr/serverWorkingDir/.  Note if you create a javacore
        // in the /wlp/usr/servers/ folder it gets picked up by the test framework as an error.
        Properties envVars = new Properties();
        envVars.put("SERVER_WORKING_DIR", relativePath);
        Log.info(c, METHOD_NAME, "SERVER_WORKING_DIR = " + relativePath + " and absolute path is " + absolutePath);

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        // Use jcmd to generate a heap dump
        String[] execParameters = new String[] { "jcmd", findServerPid(), "GC.heap_dump", METHOD_NAME + ".hprof" };
        Process process = Runtime.getRuntime().exec(execParameters);
        try (Reader reader = new InputStreamReader(process.getInputStream());
                        BufferedReader br = new BufferedReader(reader);) {
            String output = null;
            while ((output = br.readLine()) != null) {
                Log.info(c, METHOD_NAME, "jcmd output = " + output);
            }
        }
        assertEquals("Jcmd didn't return 0.  See jcmd output above for troubleshooting", 0, process.waitFor());

        // Make sure we got the java heap dump at the expected location
        File core = new File(javaCoreLocation + File.separator + METHOD_NAME + ".hprof");
        assertTrue("The heap file did not exist at location = " + core.getAbsolutePath(), core.exists());
        Log.info(c, METHOD_NAME, "Removing file = " + core.getAbsolutePath());
        core.delete();

        // Stop the server
        parms[0] = "stop";
        parms[1] = SERVER_NAME;

        po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server stop stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKE0036I");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test validates that the server start script functions correctly when the SERVER_WORKING_DIR
     * environment variable is set. Output from the JVM (ex, javadumps) should be written to the value
     * specified. This can be an absolute path (c:\ or /) or relative path (../ or folder). If relative,
     * it is from the ${server.output.dir} location.
     *
     * This test case is for a relative folder which should resolve to ${server.output.dir}/javadumps/.
     *
     * @throws Exception
     */
    @Test
    public void test_SWD_RelativeFolder() throws Exception {
        assumeTrue(!isIBM_v8_JVM);
        final String METHOD_NAME = "test_SWD_RelativeFolder";
        Log.entering(c, METHOD_NAME);

        String executionDir = server.getInstallRoot();
        String command = null;
        String javaCoreLocation = null;
        String folder = "javadumps";
        String absolutePath = null;

        // Cover executing both scripts that utilize this environment variable
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            command = "bin" + File.separator + "server.bat";
            javaCoreLocation = server.getServerRoot().replace("/", "\\").trim() + File.separator + folder;
            absolutePath = server.getServerRoot().replace("/", "\\") + File.separator + folder;
        } else {
            command = "bin" + File.separator + "server";
            javaCoreLocation = server.getServerRoot() + File.separator + folder;
            absolutePath = server.getServerRoot() + File.separator + folder;
        }
        Log.info(c, METHOD_NAME, "server command utilized is = " + command);

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("SERVER_WORKING_DIR", folder);
        Log.info(c, METHOD_NAME, "SERVER_WORKING_DIR = " + javaCoreLocation);

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);

        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        // Use jcmd to generate a heap dump
        String[] execParameters = new String[] { "jcmd", findServerPid(), "GC.heap_dump", METHOD_NAME + ".hprof" };
        Process process = Runtime.getRuntime().exec(execParameters);
        try (Reader reader = new InputStreamReader(process.getInputStream());
                        BufferedReader br = new BufferedReader(reader);) {
            String output = null;
            while ((output = br.readLine()) != null) {
                Log.info(c, METHOD_NAME, "jcmd output = " + output);
            }
        }
        assertEquals("Jcmd didn't return 0.  See jcmd output above for troubleshooting", 0, process.waitFor());

        // Make sure we got the java heap dump at the expected location
        File core = new File(javaCoreLocation + File.separator + METHOD_NAME + ".hprof");
        assertTrue("The heap file did not exist at location = " + core.getAbsolutePath(), core.exists());
        Log.info(c, METHOD_NAME, "Removing file = " + core.getAbsolutePath());
        core.delete();

        // Stop the server
        parms[0] = "stop";
        parms[1] = SERVER_NAME;

        po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server stop stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKE0036I");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test validates that the SERVER_WORKING_DIR is not utilized by the server whenever a
     * server javadump command is executed. This is considered 'server' output and should be
     * modified via the WLP_OUTPUT_DIR variable.
     *
     * This test case sets the SERVER_WORKING_DIR as ${server.output.dir}/logs/serverWorkingDir/ and
     * the server javadump being executed should utilize the ${server.output.dir} folder.
     *
     * @throws Exception
     */
    @Test
    public void test_SWD_ServerJavaDump() throws Exception {
        final String METHOD_NAME = "test_SWD_ServerJavaDump";
        Log.entering(c, METHOD_NAME);

        String executionDir = server.getInstallRoot();
        String command = null;
        String javaCoreLocation = null;
        String serverWorkingDir = null;

        // Cover executing both scripts that utilize this environment variable
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            command = "bin" + File.separator + "server.bat";
            javaCoreLocation = server.getServerRoot().replace("/", "\\");
            serverWorkingDir = server.getServerRoot().replace("/", "\\") + File.separator + "logs" + File.separator + "serverWorkingDir";
        } else {
            command = "bin" + File.separator + "server";
            javaCoreLocation = server.getServerRoot();
            serverWorkingDir = server.getServerRoot() + File.separator + "logs" + File.separator + "serverWorkingDir";
        }

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("SERVER_WORKING_DIR", serverWorkingDir);
        Log.info(c, METHOD_NAME, "SERVER_WORKING_DIR = " + serverWorkingDir);

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        // Do a server javadump, and ensure the output is located in the
        // ${server.output.dir} folder.
        parms[0] = "javadump";
        parms[1] = SERVER_NAME;

        po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        String msg = server.waitForStringInLogUsingMark("CWWKE0068I");
        assertTrue("The CWWKE0068I message was not found in the log. msg = " + msg, msg.contains("CWWKE0068I"));

        // Make sure we got the java core dump at the expected location
        File dumpFile = new File(msg.substring(msg.lastIndexOf(" ")).trim());
        assertTrue("The heap file did not exist at location = " + dumpFile.getAbsolutePath(), dumpFile.exists());
        Log.info(c, METHOD_NAME, "Removing file = " + dumpFile.getAbsolutePath());
        dumpFile.delete();

        // Stop the server
        parms[0] = "stop";
        parms[1] = SERVER_NAME;

        po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server stop stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKE0036I");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test validates that the server start script functions correctly when the SERVER_WORKING_DIR
     * environment variable is set. Output from the JVM (ex, javadumps) should be written to the value
     * specified. This can be an absolute path beginning with (c:\ or /) or relative path (../ or folder).
     * If relative, it is from the ${server.output.dir} location.
     *
     * This test case is for when the user just specifies a base drive letter like c:\ on Windows or a /
     * on Linux. In both cases we should default to ${server.output.dir}.
     *
     * @throws Exception
     */
    @Test
    public void test_SWD_RootDefault() throws Exception {
        assumeTrue(!isIBM_v8_JVM);
        final String METHOD_NAME = "test_SWD_RootDefault";
        Log.entering(c, METHOD_NAME);

        String executionDir = server.getInstallRoot();
        String command = null;
        String javaCoreLocation = null;
        String serverWorkingDir = null;

        // Cover executing both scripts that utilize this environment variable
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            command = "bin" + File.separator + "server.bat";
            javaCoreLocation = server.getServerRoot().replace("/", "\\");
            serverWorkingDir = "C:\\";
        } else { // Linux
            command = "bin" + File.separator + "server";
            javaCoreLocation = server.getServerRoot();
            serverWorkingDir = "/";
        }

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("SERVER_WORKING_DIR", serverWorkingDir);
        Log.info(c, METHOD_NAME, "SERVER_WORKING_DIR = " + serverWorkingDir);

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        // Use jcmd to generate a heap dump
        String[] execParameters = new String[] { "jcmd", findServerPid(), "GC.heap_dump", METHOD_NAME + ".hprof" };
        Process process = Runtime.getRuntime().exec(execParameters);

        try (Reader reader = new InputStreamReader(process.getInputStream());
                        BufferedReader br = new BufferedReader(reader);) {
            String output = null;
            while ((output = br.readLine()) != null) {
                Log.info(c, METHOD_NAME, "jcmd output = " + output);
            }
        }

        assertEquals("Jcmd didn't return 0.  See jcmd output above for troubleshooting", 0, process.waitFor());

        // Make sure we got the java heap dump at the expected location
        File core = new File(javaCoreLocation + File.separator + METHOD_NAME + ".hprof");
        assertTrue("The heap file did not exist at location = " + core.getAbsolutePath(), core.exists());
        Log.info(c, METHOD_NAME, "Removing file = " + core.getAbsolutePath());
        core.delete();

        // Stop the server
        parms[0] = "stop";
        parms[1] = SERVER_NAME;

        po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server stop stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKE0036I");

        Log.exiting(c, METHOD_NAME);

    }

    /**
     * This test validates that the server start script functions correctly when the SERVER_WORKING_DIR
     * environment variable is set. Output from the JVM (ex, javadumps) should be written to the value
     * specified. This can be an absolute path beginning with (c:\ or /) or relative path (../ or folder).
     * If relative, it is from the ${server.output.dir} location.
     *
     * This test case is for when the user just specifies an invalid URL. The OS for the given environment
     * would be the underlying handler of this scenario, and whatever the mkdir command does with the URL
     * would be the result. In most cases this results in a path being created similar to the following:
     *
     * export SERVER_WORKING_DIR=https://google.net would result in a path with the following location
     * \https*\google.net\dumpfile.hprof
     *
     * @throws Exception
     */
    @Test
    public void test_SWD_InvalidURL() throws Exception {
        assumeTrue(!isIBM_v8_JVM);
        final String METHOD_NAME = "test_SWD_InvalidURL";
        Log.entering(c, METHOD_NAME);

        String executionDir = server.getInstallRoot();
        String command = null;
        String javaCoreLocation = null;
        String serverWorkingDir = "https://google.net";

        // Cover executing both scripts that utilize this environment variable
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            command = "bin" + File.separator + "server.bat";
            javaCoreLocation = server.getInstallRoot().replace("/", "\\");
        } else {
            command = "bin" + File.separator + "server";
            javaCoreLocation = server.getServerRoot() + File.separator + "https:/google.net";
        }

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("SERVER_WORKING_DIR", serverWorkingDir);
        Log.info(c, METHOD_NAME, "SERVER_WORKING_DIR = " + serverWorkingDir);

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        // Use jcmd to generate a heap dump
        String[] execParameters = new String[] { "jcmd", findServerPid(), "GC.heap_dump", METHOD_NAME + ".hprof" };
        Process process = Runtime.getRuntime().exec(execParameters);

        try (Reader reader = new InputStreamReader(process.getInputStream());
                        BufferedReader br = new BufferedReader(reader);) {
            String output = null;
            while ((output = br.readLine()) != null) {
                Log.info(c, METHOD_NAME, "jcmd output = " + output);
            }
        }

        assertEquals("Jcmd didn't return 0.  See jcmd output above for troubleshooting", 0, process.waitFor());

        // Make sure we got the java heap dump at the expected location
        File core = new File(javaCoreLocation + File.separator + METHOD_NAME + ".hprof");
        assertTrue("The heap file did not exist at location = " + core.getAbsolutePath(), core.exists());
        Log.info(c, METHOD_NAME, "Removing file = " + core.getAbsolutePath());
        core.delete();

        // Stop the server
        parms[0] = "stop";
        parms[1] = SERVER_NAME;

        po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server stop stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKE0036I");

        Log.exiting(c, METHOD_NAME);

    }

    /**
     * This test validates that the server start script functions correctly when the SERVER_WORKING_DIR
     * environment variable is set. Output from the JVM (ex, javadumps) should be written to the value
     * specified. This can be an absolute path beginning with (c:\ or /) or relative path (../ or folder).
     * If relative, it is from the ${server.output.dir} location.
     *
     * This test case is for when the user just specifies a string ('not a path'). The OS for the given
     * environment would be the underlying handler of this scenario, and whatever the mkdir command does
     * with the URL would be the result.
     *
     * For Windows: The server would not start as the mkdir command causes the server.bat script to exit.
     *
     * For Linux: The server should start but the mkdir command will only create the first word as a path
     * (ie 'not' which should be relative to the ${server.output.dir} location).
     *
     * @throws Exception
     */
    @Test
    public void test_SWD_InvalidPath() throws Exception {
        final String METHOD_NAME = "test_SWD_InvalidPath";
        Log.entering(c, METHOD_NAME);

        String executionDir = server.getInstallRoot();
        String command = null;
        String javaCoreLocation = null;
        String serverWorkingDir = "not a path";
        boolean isWindows = false;

        // Cover executing both scripts that utilize this environment variable
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS) {
            command = "bin" + File.separator + "server.bat";
            javaCoreLocation = server.getInstallRoot().replace("/", "\\") + File.separator + "not";
            isWindows = true;
        } else {
            command = "bin" + File.separator + "server";
            javaCoreLocation = server.getServerRoot() + File.separator + "not a path";
        }

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("SERVER_WORKING_DIR", serverWorkingDir);
        Log.info(c, METHOD_NAME, "SERVER_WORKING_DIR = " + serverWorkingDir);

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        if (isWindows) {
            assertTrue("The server should NOT have started due to a bad SERVER_WORKING_DIR = not a path", !server.isStarted());
        } else {
            // Use jcmd to generate a heap dump
            String[] execParameters = new String[] { "jcmd", findServerPid(), "GC.heap_dump", METHOD_NAME + ".hprof" };
            Process process = Runtime.getRuntime().exec(execParameters);

            try (Reader reader = new InputStreamReader(process.getInputStream());
                            BufferedReader br = new BufferedReader(reader);) {
                String output = null;
                while ((output = br.readLine()) != null) {
                    Log.info(c, METHOD_NAME, "jcmd output = " + output);
                }
            }

            assertEquals("Jcmd didn't return 0.  See jcmd output above for troubleshooting", 0, process.waitFor());

            // Make sure we got the java heap dump at the expected location
            File core = new File(javaCoreLocation + File.separator + METHOD_NAME + ".hprof");
            assertTrue("The heap file did not exist at location = " + core.getAbsolutePath(), core.exists());
            Log.info(c, METHOD_NAME, "Removing file = " + core.getAbsolutePath());
            core.delete();

            // Stop the server
            parms[0] = "stop";
            parms[1] = SERVER_NAME;

            po = server.getMachine().execute(command, parms, executionDir, envVars);
            Log.info(c, METHOD_NAME, "server stop stdout = " + po.getStdout());
            Log.info(c, METHOD_NAME, "server stop stderr = " + po.getStderr());

            server.waitForStringInLog("CWWKE0036I");
        }

        Log.exiting(c, METHOD_NAME);

    }

    /**
     * Find the servers process and return its corresponding pid via jcmd.
     *
     * NOTE: The server.getPid() method does not seem to return a pid given
     * how the server was started via a machine vs a server start, and using the
     * ManagementFactory.getRuntimeMXBean().getName() returns the java process
     * of the framework. Thus using jcmd to obtain the pid for the server.
     *
     * @return
     * @throws Exception
     */
    private String findServerPid() throws Exception {
        String pid = null;
        String[] cmd = new String[] { "jcmd" };
        Process process = Runtime.getRuntime().exec(cmd);

        try (Reader reader = new InputStreamReader(process.getInputStream());
                        BufferedReader br = new BufferedReader(reader);) {
            String output = null;
            while ((output = br.readLine()) != null) {
                if (output.contains("ws-server.jar")) {
                    pid = output.substring(0, output.indexOf(" "));
                    return pid;
                }
            }
        }

        return pid;
    }
}
