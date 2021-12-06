/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server startup process.
 */
public class ServerEnvTest {
    private static final Class<?> c = ServerEnvTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.serverstart.fat";
    private static final String OS = System.getProperty("os.name").toLowerCase();

    private static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
    }

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            logDirectoryContents("after", new File(server.getLogsRoot())); // DEBUG
            server.stopServer();
        }
    }

    /**
     * Test - Variable expansion in server.env works when enabled.
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
        final String METHOD_NAME = "testVariableExpansionInServerEnv";
        Log.entering(c, METHOD_NAME);

        varsExpandInServerEnv(true);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test - Variable expansion in server.env does NOT work when it is NOT enabled.
     * -- Note it is ALWAYS enabled for Windows --
     * -- So this test not applicable to Windows --
     * server.env contents:
     * [
     * LOG_FILE=${CONSOLE_LOG_FILE_NAME}
     * ]
     * Since variable expansion is not enabled,
     * verify that the CONSOLE_LOG_FILE_NAME did NOT expand by checking
     * that the log file name with value of ${CONSOLE_LOG_FILE_NAME} does NOT get created.
     */
    @Test
    public void testVariableExpansionInServerEnvWhenExpansionNotEnabled() throws Exception {
        final String METHOD_NAME = "testVariableExpansionInServerEnvWhenExpansionNotEnabled";
        Log.entering(c, METHOD_NAME);
        if (OS.contains("win")) {
            return;
        }
        varsExpandInServerEnv(false);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Set a variable in server.env using a pre-set environment
     * variable as the value. In this case we set the LOG_FILE
     * variable which provides a new name for "console.log".
     * Then check whether the log file with the new name exists
     * after starting the server.
     *
     * @param expansionEnabled if true, the new log file should exist, otherwise it should not
     * @throws Exception
     */
    public void varsExpandInServerEnv(boolean expansionEnabled) throws Exception {
        final String METHOD_NAME = "varsExpandInServerEnv[" + expansionEnabled + "]";
        Log.entering(c, METHOD_NAME);

        // The environment variable to be referenced in etc/server.env
        final String consoleLogEnvVar = "CONSOLE_LOG_FILE_NAME";
        String consoleLogEnvVarReference;
        if (OS.contains("win")) {
            consoleLogEnvVarReference = "!" + consoleLogEnvVar + "!";
        } else {
            consoleLogEnvVarReference = "${" + consoleLogEnvVar + "}";
        }

        // Create server.env with or without expansion enabled.
        // Sets the LOG_FILE variable to an environment variable reference.
        // This should cause the name of the "console.log" to be something different.
        String fileContents;
        if (expansionEnabled) {
            fileContents = "#enable_variable_expansion\nLOG_FILE=" + consoleLogEnvVarReference;
        } else {
            fileContents = "LOG_FILE=" + consoleLogEnvVarReference;
        }
        Log.info(c, METHOD_NAME, "Creating /etc/server.env with contents:\n" + fileContents);
        File wlpEtcDir = new File(server.getInstallRoot(), "etc");
        String serverEnvCreate = createServerEnvFile(fileContents, wlpEtcDir);
        Log.info(c, METHOD_NAME, "server.env location = " + serverEnvCreate);
        assertTrue("The server.env file was not created.", serverEnvCreate.contains("server.env"));

        // Set the log file name in an environment variable
        String logFileName = "GiveMeLibertyOrGiveMeDeath.log";
        Properties envVars = new Properties();
        envVars.put(consoleLogEnvVar, logFileName);

        // Execute the server start command and display stdout and stderr
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        // Check for server ready
        String serverReady = server.waitForStringInLog("CWWKF0011I");
        if (serverReady == null) {
            Log.info(c, METHOD_NAME, "Timed out waiting for server ready message, CWWKF0011I");
        }

        // Because we didn't start the server using the LibertyServer APIs, we need
        // to have it detect its started state so it will stop and save logs properly
        server.resetStarted();
        assertTrue("the server should have been started", server.isStarted());

        logDirectoryContents(METHOD_NAME, new File(server.getLogsRoot())); // DEBUG

        // Finally, verify that the log file with the new name exists when expansion is enabled
        String logFullPathName = server.getLogsRoot() + logFileName;
        File logFile = new File(logFullPathName);
        if (expansionEnabled) {
            assertTrue("the log file with the new name [ " + logFullPathName + " ] does not exist", logFile.exists());
        } else {
            assertTrue("the log file with the new name [ " + logFullPathName + " ] exists, but it should not", !logFile.exists());
        }

        // Test is complete, but create "console.log" file.
        // Verification of server stop depends on the default name.
        // Otherwise, we get FileNotFoundException in the test logs.
        String defaultLogFullPathName = server.getLogsRoot() + "console.log";
        Log.info(c, METHOD_NAME, "Creating default console log: " + defaultLogFullPathName);
        createFile(defaultLogFullPathName);

        // Cleanup - If the server.env file is not deleted, it can cause other test cases to fail.
        deleteServerEnvFile(wlpEtcDir);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Create the server.env file in the specified directory
     *
     * @param fileContents
     * @param dir directory for server.env
     * @throws IOException
     */
    private String createServerEnvFile(String fileContents, File dir) throws IOException {

        File serverEnvFile = new File(dir, "server.env");

        String serverEnv = serverEnvFile.getAbsolutePath();

        if (!dir.exists()) {
            dir.mkdirs();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(serverEnvFile);
            fos.write(fileContents.getBytes());
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

        return serverEnv;
    }

    /**
     * @param dir directory containing server.env file to delete
     */
    private void deleteServerEnvFile(File dir) {

        File serverEnvFile = new File(dir, "server.env");

        if (!serverEnvFile.exists()) {
            return;
        }

        try {
            serverEnvFile.delete();
        } catch (Exception e) {
            Log.info(c, "deleteServerEnv", "Failed to delete : " + serverEnvFile.getAbsolutePath());
        }

        return;
    }

    /**
     * Creates an empty file.
     *
     * @param fileToCreate
     * @throws IOException
     */
    public static void createFile(final String fileName) throws IOException {
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

        } catch (Throwable t) {
            Log.info(c, METHOD_NAME, "Caught exception:\n[\n" + t.toString() + "\n]");

        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * @param methodName
     * @param folder
     */
    public static void logDirectoryContents(final String methodName, File folder) {

        File[] listOfFiles = folder.listFiles();

        Log.info(c, methodName, "Server logs directory contents: ");
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    Log.info(c, methodName, "File: " + listOfFiles[i].getName());
                } else if (listOfFiles[i].isDirectory()) {
                    Log.info(c, methodName, "Directory: " + listOfFiles[i].getName());
                }
            }
        }
    }
}
