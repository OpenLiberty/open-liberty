/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server startup process.
 */
@RunWith(FATRunner.class)
public class ServerEnvTest {
    private static final Class<?> c = ServerEnvTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.serverstart.fat";
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static Locale saveLocale;
    private static final String BACKUP_EXT = ".backup";

    private File tempConsoleLog; // This is global because we can't delete the log file until the 'after' method.

    private static LibertyServer server;

    // Make these global so that we can delete then in the 'AfterClass'.
    private static String backup_etc_ServerEnv_FileName;
    private static String backup_shared_ServerEnv_FileName;
    private static String backup_server_ServerEnv_FileName;

    // Describes the 3 locations for the server.env file
    private enum ServerEnvType {
        ETC("etc/server.env"),
        SHARED("shared/server.env"),
        SERVER("usr/servers/" + SERVER_NAME + "/server.env");

        public final String path;

        private ServerEnvType(String path) {
            this.path = path;
        }
    }

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() {
        saveLocale = Locale.getDefault();
    }

    @Before
    public void before() {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        Log.info(c, "before", "server hash code: " + server.hashCode());
        backupServerEnvFiles();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

        Locale.setDefault(saveLocale);
        Utils.deleteFile(backup_etc_ServerEnv_FileName);
        Utils.deleteFile(backup_shared_ServerEnv_FileName);
        Utils.deleteFile(backup_server_ServerEnv_FileName);
    }

    @After
    public void after() throws Exception {
        String METHOD_NAME = "after";

        String consoleLogName = null;
        try {
            consoleLogName = server.getConsoleLogFile().getName();
        } catch (Exception e) {
            Log.info(c, METHOD_NAME, "Caught Exception: [" + e.getMessage() + "]");
            e.printStackTrace();
        }
        Log.info(c, METHOD_NAME, "Logs dir =[" + server.getLogsRoot() + "] log file name [" + consoleLogName + "]");

        if (server.isStarted()) {
            // Set the console.log file name in the server so that it knows which logs to search for messages
            // during the server stop.   This avoids a 30 second delay caused by searching the wrong log file.
            if (tempConsoleLog != null) {
                server.setConsoleLogName(tempConsoleLog.getName());
            }
            Utils.displayDirectoryContents(METHOD_NAME, new File(server.getLogsRoot()));

            server.stopServer();
        }

        restoreServerEnvFiles();

        // Some tests create a log file other than "console.log".  Delete these as part of clean up.
        if (tempConsoleLog != null) {
            Utils.deleteFile(tempConsoleLog);
            tempConsoleLog = null;
        }
        Utils.displayDirectoryContents(METHOD_NAME, new File(server.getLogsRoot()));
    }

    /**
     * Make a copy of any existing server.env files. The tests modify server.env. So restore them after the tests.
     */
    private void backupServerEnvFiles() {
        final String METHOD_NAME = "backupServerEnvFiles";

        try {
            File etc_ServerEnv_File = new File(server.getInstallRoot() + "/" + ServerEnvType.ETC.path);
            File shared_ServerEnv_File = new File(server.getInstallRoot() + "/" + ServerEnvType.SHARED.path);
            File server_ServerEnv_File = new File(server.getInstallRoot() + "/" + ServerEnvType.SERVER.path);
            backup_etc_ServerEnv_FileName = etc_ServerEnv_File.getAbsolutePath() + BACKUP_EXT;
            backup_shared_ServerEnv_FileName = shared_ServerEnv_File.getAbsolutePath() + BACKUP_EXT;
            backup_server_ServerEnv_FileName = server_ServerEnv_File.getAbsolutePath() + BACKUP_EXT;

            if (etc_ServerEnv_File.exists()) {
                Utils.copyFile(etc_ServerEnv_File, backup_etc_ServerEnv_FileName);
            }

            if (shared_ServerEnv_File.exists()) {
                Utils.copyFile(shared_ServerEnv_File, backup_shared_ServerEnv_FileName);
            }

            if (server_ServerEnv_File.exists()) {
                Utils.copyFile(server_ServerEnv_File, backup_server_ServerEnv_FileName);
            }
        } catch (IOException ioe) {
            Log.warning(c, METHOD_NAME + " Failed to backup server.env files");
            Log.error(c, METHOD_NAME, ioe);
        }
    }

    /**
     * Restore the original server.env files from backups. If the server.env file was not originally there, delete it.
     */
    private void restoreServerEnvFiles() {
        final String METHOD_NAME = "restoreServerEnvFiles";
        String etc_ServerEnv_Path = server.getInstallRoot() + "/" + ServerEnvType.ETC.path;
        String shared_ServerEnv_Path = server.getInstallRoot() + "/" + ServerEnvType.SHARED.path;
        String server_ServerEnv_Path = server.getInstallRoot() + "/" + ServerEnvType.SERVER.path;

        File etc_ServerEnv_File = new File(etc_ServerEnv_Path);
        File shared_ServerEnv_File = new File(shared_ServerEnv_Path);
        File server_ServerEnv_File = new File(server_ServerEnv_Path);

        File backup_etc_ServerEnv_File = new File(backup_etc_ServerEnv_FileName);
        File backup_shared_ServerEnv_File = new File(backup_shared_ServerEnv_FileName);
        File backup_server_ServerEnv_File = new File(backup_server_ServerEnv_FileName);

        // Initially delete all server.env files.  If a backup was created, it will be restored.
        Utils.deleteFile(etc_ServerEnv_File);
        Utils.deleteFile(shared_ServerEnv_File);
        Utils.deleteFile(server_ServerEnv_File);

        try {
            if (backup_etc_ServerEnv_File.exists()) {
                Log.info(c, METHOD_NAME, "restoring " + etc_ServerEnv_Path);
                Utils.copyFile(backup_etc_ServerEnv_File, etc_ServerEnv_Path);
                Utils.displayFile(etc_ServerEnv_Path);
            }

            if (backup_shared_ServerEnv_File.exists()) {
                Log.info(c, METHOD_NAME, "restoring " + shared_ServerEnv_Path);
                Utils.copyFile(backup_etc_ServerEnv_File, shared_ServerEnv_Path);
                Utils.displayFile(shared_ServerEnv_Path);
            }

            if (backup_server_ServerEnv_File.exists()) {
                Log.info(c, METHOD_NAME, "restoring " + server_ServerEnv_Path);
                Utils.copyFile(backup_etc_ServerEnv_File, server_ServerEnv_Path);
                Utils.displayFile(server_ServerEnv_Path);
            }
        } catch (IOException ioe) {
            Log.warning(c, METHOD_NAME + " Failed to backup server.env files");
            Log.error(c, METHOD_NAME, ioe);
        }
    }

    /**
     * Test - Variable expansion in server.env works when enabled.
     *
     * To enable this test for Z/OS, I believe you would need to ensure that
     * both the server script and the server.env file are generated in EBCDIC.
     *
     * server.env contents:
     * [
     * #enable_variable_expansion
     * LOG_FILE=${CONSOLE_LOG_FILE_NAME}
     * ]
     * Verify that the CONSOLE_LOG_FILE_NAME got expanded by checking if
     * the log file name with value of ${CONSOLE_LOG_FILE_NAME} gets created.
     */
    @Test
    public void testVariableExpansionInServerEnv() throws Exception {

        // Temporarily disable on Windows.  The test is not cleaning up properly and is breaking other tests.
        if (OS.contains("win")) {
            return;
        }

        final String METHOD_NAME = "testVariableExpansionInServerEnv";
        Log.info(c, METHOD_NAME, "ENTER ==============================");

        if (OS.contains("os/390") || OS.contains("z/os") || OS.contains("zos")) {
            return;
        }
        varsExpandInServerEnv(true, ServerEnvType.ETC);

        Log.info(c, METHOD_NAME, "EXIT");
    }

    /**
     * Test - Variable expansion in server.env does NOT happen when it is NOT enabled.
     * -- Note it is ALWAYS enabled for Windows --
     * -- So this test not applicable to Windows. There is no way to disable variable expansion on Windows. --
     *
     * To enable this test for Z/OS, I believe you would need to ensure that
     * both the server script and the server.env file are generated in EBCDIC.
     *
     * Example server.env contents:
     * [
     * LOG_FILE=${CONSOLE_LOG_FILE_NAME}
     * ]
     * Since variable expansion is not enabled,
     * verify that the CONSOLE_LOG_FILE_NAME did NOT expand by checking
     * that the log file name with value of ${CONSOLE_LOG_FILE_NAME} does NOT get created.
     */
    @Test
    public void testVariableExpansionInServerEnvWhenExpansionNotEnabled() throws Exception {

        // Temporarily disable on Windows.  The test is not cleaning up properly and is breaking other tests.
        if (OS.contains("win")) {
            return;
        }

        final String METHOD_NAME = "testVariableExpansionInServerEnvWhenExpansionNotEnabled";
        Log.info(c, METHOD_NAME, "ENTER ==============================");
        if (OS.contains("win") || OS.contains("os/390") || OS.contains("z/os") || OS.contains("zos")) {
            return;
        }
        varsExpandInServerEnv(false, ServerEnvType.ETC);

        Log.info(c, METHOD_NAME, "EXIT");
    }

    /**
     * If server name is specified with the "server version" command. for example
     * server version MyServer
     * , the server.env file of the specified server should be read.
     * The test creates a server.env file for the server, and adds an echo "hello world"
     * statement in the file. On Linux systems the server.env is run as a script. So
     * the hello world should be displayed in the output.
     *
     * @throws Exception
     */
    @Test
    public void testVersionCommandReadsServerEnvOfServer() throws Exception {

        final String METHOD_NAME = "testVersionCommandReadsServerEnvOfServer";
        final boolean EXPANSION_ENABLED = true; // ensures server.env will be execute
        Log.info(c, METHOD_NAME, "ENTER ==============================");

        // Implementation will not work on Windows because server.env is not executed on Windows.
        // There are some ASCII - EBCDIC issues that would have to be worked out for Z/OS.
        if (!OS.contains("linux") && !OS.contains("mac os")) {
            Log.info(c, METHOD_NAME, "Returning.  Test not valid on [{0}]", OS);
            Log.exiting(c, METHOD_NAME);
            return;
        }
        Log.info(c, METHOD_NAME, "ETC server.env ---");
        Utils.displayFile(server.getInstallRoot() + "/" + ServerEnvType.ETC.path);
        Log.info(c, METHOD_NAME, "SERVER server.env ---");
        Utils.displayFile(server.getInstallRoot() + "/" + ServerEnvType.SERVER.path);

        // Start the server passing null for the environment variable properties.
        // We don't need the properties set for this test.
        executeStartServerCommand(null);
        assertTrue("the server should have been started", server.isStarted());

        // Create file contents for server.env.   Contents will echo Hello World
        final String HELLO_WORLD = "Hello World";
        String fileContents = createFileContentsForServerEnv(EXPANSION_ENABLED, "echo " + HELLO_WORLD);

        // Create server.env in the server directory.
        File serverEnvFile = Utils.createFile(server.getInstallRoot() + "/" + ServerEnvType.SERVER.path,
                                              fileContents);
        Log.info(c, METHOD_NAME, "SERVER server.env +++");
        Utils.displayFile(server.getInstallRoot() + "/" + ServerEnvType.SERVER.path);

        assertNotNull("The server.env file was not created. Null returned.", serverEnvFile);
        assertTrue("The server.env file was not created.", serverEnvFile.exists());

        // execute "server version <SERVER_MAME> and check output for hello world message
        String output = executeServerVersionCommand();
        assertTrue("The server's server.env did not get invoked as expected. "
                   + HELLO_WORLD + "not found in output", output.contains(HELLO_WORLD));

        Log.info(c, METHOD_NAME, "EXIT");
    }

    // End tests methods ---------- Begin Utility Methods --------

    /**
     * Set the LOG_FILE variable in the server.env file.
     * The value of the LOG_FILE will be a reference to an environment variable.
     * For example:
     * LOG_FILE=$CONSOLE_LOG_FILE_NAME (Linux)
     * or
     * LOG_FILE=%CONSOLE_LOG_FILE_NAME% (Windows)
     *
     * The liberty default for LOG_FILE is "console.log".
     * If the variable gets resolved properly, the console.log file will have a
     * new name.
     * Check whether the log file with the new name exists (depending on whether
     * expansion is enabled) after starting the server.
     *
     * @param expansionEnabled if true, the log file with the new name should exist, otherwise it should not.
     * @param envType          Specifies which of the 3 server.env files to create
     * @throws Exception
     */
    public void varsExpandInServerEnv(boolean expansionEnabled, ServerEnvType envType) throws Exception {
        final String METHOD_NAME = "varsExpandInServerEnv[" + expansionEnabled + "]";
        Log.entering(c, METHOD_NAME);

        String environmentVariableName = "LOG_FILE_NAME_VARIABLE";

        // Get the proper syntax !!, ${} for the environment variable to be referenced in .../server.env
        final String envVarReferenceSyntax = createReferenceSyntaxForVariableName(environmentVariableName);

        // Create file contents for server.env with or without expansion enabled.
        // Sets the LOG_FILE variable to an environment variable reference.
        // This should cause the name of the "console.log" to be something different.
        String fileContents = createFileContentsForServerEnv(expansionEnabled, "LOG_FILE=" + envVarReferenceSyntax);

        File serverEnvFile = Utils.createFile(server.getInstallRoot() + "/" + envType.path,
                                              fileContents);
        assertNotNull("The server.env file was not created. Null returned.", serverEnvFile);
        assertTrue("The server.env file was not created.", serverEnvFile.exists());

        // Set the log file name in an environment variable
        String newLogFileName = "AlternativeLiberty.log";
        Properties envVars = new Properties();
        envVars.put(environmentVariableName, newLogFileName);

        // The temporary log file name will depend on whether variables are expanded in server.env
        if (expansionEnabled) {
            tempConsoleLog = new File(server.getLogsRoot() + newLogFileName); // store name for clean up later.
        } else {
            tempConsoleLog = new File(server.getLogsRoot() + envVarReferenceSyntax); // store name for clean up later.
        }

        executeStartServerCommand(envVars);
        assertTrue("The server should have been started", server.isStarted());

        Log.info(c, METHOD_NAME, "About to display directory contents.  server.getLogsRoot()[" + server.getLogsRoot() + "]");
        Utils.displayDirectoryContents(METHOD_NAME, new File(server.getLogsRoot())); // DEBUG

        // Finally, verify that the log file with the new name exists when expansion is enabled
        String newLogFullPathName = server.getLogsRoot() + newLogFileName;
        File logFile = new File(newLogFullPathName);
        if (expansionEnabled) {
            assertTrue("The log file with the new name [ " + newLogFullPathName + " ] does not exist", logFile.exists());
        } else {
            // if expansion is not enabled, we should see a log like ${LOG_FILE_NAME_VARIABLE}
            File logFileUnExpandedCase = new File(server.getLogsRoot() + envVarReferenceSyntax);
            assertTrue("Was expecting the log file with the unexpanded name [ " + envVarReferenceSyntax + " ] to exist.  It does not.", logFileUnExpandedCase.exists());
        }

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Prefixes contents with "#enable_variable_expansion" marker if expansionEnabled flag is set
     *
     * @param expansionEnabled
     * @param contents
     * @return
     */
    private String createFileContentsForServerEnv(boolean expansionEnabled, String contents) {
        if (expansionEnabled) {
            return "#enable_variable_expansion\n" + contents;
        } else {
            return contents;
        }
    }

    /**
     * Given a variable name, returns the appropriate reference syntax for
     * the variable based on operating system; eg Windows !varName!; Linux ${varName}
     *
     * @param variableName
     * @return
     */
    private String createReferenceSyntaxForVariableName(String variableName) {
        if (OS.contains("win")) {
            return "!" + variableName + "!";
        } else {
            return "${" + variableName + "}";
        }
    }

    /**
     * Start the server using server.getMachine().execute(<server start command>)
     *
     * @param envVars Properties containing environment variable names and values.
     * @throws Exception
     */
    private String executeStartServerCommand(Properties envVars) throws Exception {
        final String METHOD_NAME = "executeStartServer";

        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Log.info(c, METHOD_NAME, "\nExecuting 'server start'...");
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);

        String stdoutAndstdErr = "\nStandard Out:\n[\n" + po.getStdout() + "\n]" +
                                 "\nStandard Error:\n[\n" + po.getStderr() + "\n]\n";
        Log.info(c, METHOD_NAME, stdoutAndstdErr);

        // Check for server ready
        String serverReady = server.waitForStringInLog("CWWKF0011I");
        if (serverReady == null) {
            Log.info(c, METHOD_NAME, "Timed out waiting for server ready message, CWWKF0011I");
        }

        // Because we didn't start the server using the LibertyServer APIs, we need
        // to have it detect its started state so it will stop and save logs properly
        server.resetStarted();
        return stdoutAndstdErr;
    }

    /**
     * Execute the "server version <SERVER_NAME>" command and return the output of the command"
     *
     * @return
     * @throws Exception
     */
    private String executeServerVersionCommand() throws Exception {
        final String METHOD_NAME = "executeServerVersionCommand";

        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "version";
        parms[1] = SERVER_NAME;
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir);
        String stdoutAndstdErr = "\nStandard Out:\n" + po.getStdout() + "\nStandard Error:\n" + po.getStderr() + "\n";
        Log.info(c, METHOD_NAME, stdoutAndstdErr);

        return stdoutAndstdErr;
    }

    /**
     * Set a variable in server.env using a pre-set environment
     * variable as the value. In this case we set the LOG_FILE
     * variable which provides a new name for "console.log".
     * Then check whether the log file with the new name exists
     * after starting the server.
     *
     * @throws Exception
     */
    @Test
    public void testAlternateLocaleServerEnv() throws Exception {

        // Temporarily disable on Windows.  The test is not cleaning up properly and is breaking other tests.
        if (OS.contains("win")) {
            return;
        }

        final String METHOD_NAME = "testAlternateLocaleServerEnv";
        Log.info(c, METHOD_NAME, "ENTER ==============================");
        //Set Locale to turkish
        Locale.setDefault(new Locale("tr"));

        // Create server.env file.
        // Sets the LOG_FILE variable to new log file name.
        // This should cause the name of the "console.log" to be something different.
        String fileContents;
        String logFileName = "default.log";
        tempConsoleLog = new File(server.getLogsRoot() + logFileName); // store name for clean up later.

        fileContents = "LOG_FILE=" + logFileName;
        File serverEnvFile = Utils.createFile(server.getInstallRoot() + "/" + ServerEnvType.ETC.path,
                                              fileContents);
        assertNotNull("The server.env file was not created. Null returned.", serverEnvFile);
        assertTrue("The server.env file was not created.", serverEnvFile.exists());

        executeStartServerCommand(null);
        assertTrue("the server should have been started", server.isStarted());

        Utils.displayDirectoryContents(METHOD_NAME, new File(server.getLogsRoot())); // DEBUG

        // Finally, verify that the log file with the new name exists
        String logFullPathName = server.getLogsRoot() + logFileName;
        File logFile = new File(logFullPathName);
        assertTrue("the log file with the new name [ " + logFullPathName + " ] does not exist", logFile.exists());

        Log.exiting(c, METHOD_NAME);
    }
}
