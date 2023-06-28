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
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    private static LibertyServer server;

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
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

        Locale.setDefault(saveLocale);

    }

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            displayDirectoryContents("after", new File(server.getLogsRoot())); // DEBUG
            server.stopServer();
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
        Log.info(c, METHOD_NAME, "ENTER");

        if (OS.contains("os/390") || OS.contains("z/os") || OS.contains("zos")) {
            return;
        }
        varsExpandInServerEnv(true, ServerEnvType.ETC);

        Log.exiting(c, METHOD_NAME);
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
        Log.info(c, METHOD_NAME, "ENTER");
        if (OS.contains("win") || OS.contains("os/390") || OS.contains("z/os") || OS.contains("zos")) {
            return;
        }
        varsExpandInServerEnv(false, ServerEnvType.ETC);

        Log.exiting(c, METHOD_NAME);
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
        Log.info(c, METHOD_NAME, "ENTER");

        // Implementation will not work on Windows because server.env is not executed on Windows.
        // There are some ASCII - EBCDIC issues that would have to be worked out for Z/OS.
        if (!OS.contains("linux") && !OS.contains("mac os")) {
            Log.info(c, METHOD_NAME, "Returning.  Test not valid on [{0}]", OS);
            Log.exiting(c, METHOD_NAME);
            return;
        }

        // Start the server passing null for the environment variable properties.
        // We don't need the properties set for this test.
        startServer(null);
        assertTrue("the server should have been started", server.isStarted());

        // Create file contents for server.env.   Contents will echo Hello World
        final String HELLO_WORLD = "Hello World";
        String fileContents = createFileContentsForServerEnv(EXPANSION_ENABLED, "echo " + HELLO_WORLD);

        // Create server.env in the server directory.
        File serverEnvFile = createServerEnvFile(fileContents, ServerEnvType.SERVER.path);
        assertNotNull("The server.env file was not created. Null returned.", serverEnvFile);
        assertTrue("The server.env file was not created.", serverEnvFile.exists());

        // execute "server version <SERVER_MAME> and check output for hello world message
        String output = executeServerVersionCommand();
        assertTrue("The server's server.env did not get invoked as expected. "
                   + HELLO_WORLD + "not found in output", output.contains(HELLO_WORLD));

        deleteFile(serverEnvFile);

        Log.exiting(c, METHOD_NAME);
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
        final String logEnvVarReferenceSyntax = createReferenceSyntaxForVariableName(environmentVariableName);

        // Create file contents for server.env with or without expansion enabled.
        // Sets the LOG_FILE variable to an environment variable reference.
        // This should cause the name of the "console.log" to be something different.
        String fileContents = createFileContentsForServerEnv(expansionEnabled, "LOG_FILE=" + logEnvVarReferenceSyntax);

        File serverEnvFile = createServerEnvFile(fileContents, envType.path);
        assertNotNull("The server.env file was not created. Null returned.", serverEnvFile);
        assertTrue("The server.env file was not created.", serverEnvFile.exists());

        // Set the log file name in an environment variable
        String newLogFileName = "AlternativeLiberty.log";
        Properties envVars = new Properties();
        envVars.put(environmentVariableName, newLogFileName);

        startServer(envVars);
        assertTrue("the server should have been started", server.isStarted());

        displayDirectoryContents(METHOD_NAME, new File(server.getLogsRoot())); // DEBUG

        // Finally, verify that the log file with the new name exists when expansion is enabled
        String newLogFullPathName = server.getLogsRoot() + newLogFileName;
        File logFile = new File(newLogFullPathName);
        if (expansionEnabled) {
            assertTrue("the log file with the new name [ " + newLogFullPathName + " ] does not exist", logFile.exists());
            renameFileTo(logFile, server.getLogsRoot() + "console.log"); // cleanup
        } else {
            // if expansion is not enabled, we should see a log like ${LOG_FILE_NAME_VARIABLE}
            File logFileUnExpandedCase = new File(server.getLogsRoot() + logEnvVarReferenceSyntax);
            assertTrue("Was expecting the log file with the unexpanded name [ " + logEnvVarReferenceSyntax + " ] to exist.  It does not.", logFileUnExpandedCase.exists());
            // and we should not see the log name that has been de-referenced
            assertTrue("Was not expecting the log file with the new name [ " + newLogFullPathName + " ] to exist, but it sure does.", !logFile.exists());
            renameFileTo(logFileUnExpandedCase, server.getLogsRoot() + "console.log"); // cleanup
        }

        deleteFile(serverEnvFile);

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
    private void startServer(Properties envVars) throws Exception {
        final String METHOD_NAME = "startServer";

        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "\nserver start stdout: \n[\n{0}\n]", po.getStdout());
        Log.info(c, METHOD_NAME, "\nserver start stderr: \n[\n{0}\n]", po.getStderr());

        // Check for server ready
        String serverReady = server.waitForStringInLog("CWWKF0011I");
        if (serverReady == null) {
            Log.info(c, METHOD_NAME, "Timed out waiting for server ready message, CWWKF0011I");
        }

        // Because we didn't start the server using the LibertyServer APIs, we need
        // to have it detect its started state so it will stop and save logs properly
        server.resetStarted();
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
     *
     * @param fileContents          - contents to put in the newly created server.env file
     * @param serverEnvRelativePath - relative path, below "wlp", to server.env file to create
     * @return
     */
    private File createServerEnvFile(String fileContents, String serverEnvRelativePath) {
        final String METHOD_NAME = "createServerEnvFile";
        Log.info(c, METHOD_NAME, "ENTER serverEnvRelativePath[{0}]\n", serverEnvRelativePath);

        String serverEnvPath = server.getInstallRoot() + "/" + serverEnvRelativePath;
        File serverEnvFile = new File(serverEnvPath);
        File parentDir = serverEnvFile.getParentFile();
        String path = serverEnvFile.getAbsolutePath();
        Log.info(c, METHOD_NAME, "Creating file [{0}] with contents \n[\n{1}\n]", new Object[] { path, fileContents });

        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(serverEnvFile);
            fos.write(fileContents.getBytes());
        } catch (IOException ioe) {
            Log.info(c, METHOD_NAME, "Caught exception while writing the file " + path);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe) {
                    Log.info(c, METHOD_NAME, "Caught exception while closing the file " + path);
                }
            }
        }

        return serverEnvFile;
    }

    /**
     * @param file to delete
     */
    private void deleteFile(File file) {

        if (!file.exists()) {
            return;
        }

        try {
            file.delete();
        } catch (Exception e) {
            Log.info(c, "deleteFile", "Failed to delete : " + file.getAbsolutePath());
        }

        return;
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
        Log.info(c, METHOD_NAME, "ENTER");
        //Set Locale to turkish
        Locale.setDefault(new Locale("tr"));

        // Create server.env file.
        // Sets the LOG_FILE variable to new log file name.
        // This should cause the name of the "console.log" to be something different.
        String fileContents;
        String logFileName = "default.log";
        fileContents = "LOG_FILE=" + logFileName;
        File serverEnvFile = createServerEnvFile(fileContents, ServerEnvType.ETC.path);
        assertNotNull("The server.env file was not created. Null returned.", serverEnvFile);
        assertTrue("The server.env file was not created.", serverEnvFile.exists());

        startServer(null);
        assertTrue("the server should have been started", server.isStarted());

        displayDirectoryContents(METHOD_NAME, new File(server.getLogsRoot())); // DEBUG

        // Finally, verify that the log file with the new name exists
        String logFullPathName = server.getLogsRoot() + logFileName;
        File logFile = new File(logFullPathName);
        assertTrue("the log file with the new name [ " + logFullPathName + " ] does not exist", logFile.exists());

        renameFileTo(logFile, server.getLogsRoot() + "console.log");
        deleteFile(serverEnvFile);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Creates an empty file.
     *
     * @param fileToCreate
     * @throws IOException
     */
    public static void createFile(final String fileName) {
        String METHOD_NAME = "createFile";
        File fileToCreate = new File(fileName);

        FileOutputStream fos = null;
        try {

            if (!fileToCreate.getParentFile().exists()) {
                Log.info(c, METHOD_NAME, "Making dirs");
                if (!fileToCreate.getParentFile().mkdirs()) {
                    throw new FileNotFoundException();
                }
            }

            Log.info(c, METHOD_NAME, "Creating outputStream");
            fos = new FileOutputStream(fileToCreate);
            if (!fileToCreate.exists()) {
                fileToCreate.createNewFile();
            }

        } catch (Throwable ioe) {
            Log.info(c, METHOD_NAME, "Caught exception while creating file [{0}]\n exception [{1}]\n", new String[] { fileName, ioe.toString() });

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Throwable ioe) {
                    Log.info(c, METHOD_NAME, "Caught exception while closing file [{0}]\n exception [{1}]\n", new String[] { fileName, ioe.toString() });
                }
            }
        }
    }

    public static void renameFileTo(File originalFile, String newFileName) {
        final String METHOD_NAME = "renameFileTo";
        Log.info(c, METHOD_NAME, "renaming:\n[{0}]\n to\n[{1}]", new String[] { originalFile.getAbsolutePath(), newFileName });
        File newFile = new File(newFileName);
        assertTrue("Original file [ " + originalFile.getAbsolutePath() + " ] does not exist.", originalFile.exists());
        assertFalse("File [ " + newFileName + " ] already exists", newFile.exists());

        boolean renameSucceeded = originalFile.renameTo(newFile);
        assertFalse("Original file [ " + originalFile.getAbsolutePath() + " ] still exists after rename.", originalFile.exists());
        assertTrue("File [ " + newFileName + " ] does not exist after rename", newFile.exists());

        Log.info(c, METHOD_NAME, "renameSucceeded[{0}]", renameSucceeded);
    }

    /**
     * @param methodName
     * @param folder
     */
    public static void displayDirectoryContents(final String methodName, File folder) {

        File[] listOfFiles = folder.listFiles();
        StringBuffer sb = new StringBuffer();
        sb.append("Server 'logs' directory contents: \n[\n");

        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    sb.append("File: " + listOfFiles[i].getName() + "\n");
                } else if (listOfFiles[i].isDirectory()) {
                    sb.append("Directory: " + listOfFiles[i].getName() + "\n");
                }
            }
        }
        sb.append("]\n");
        Log.info(c, methodName, sb.toString());
    }
}
