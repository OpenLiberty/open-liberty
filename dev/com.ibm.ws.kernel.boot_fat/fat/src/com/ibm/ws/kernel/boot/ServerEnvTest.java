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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
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
        final String METHOD_NAME = "varsExpandInServerEnv[" + expansionEnabled + "] ";
        Log.entering(c, METHOD_NAME);

        String logString;
        StringBuffer sb = new StringBuffer("");

        // *********************
        // Create Environment Variable to be used by command
        // *********************
        final String consoleLogEnvVar = "CONSOLE_LOG_FILE_NAME";
        String consoleLogEnvVarReference;
        if (OS.contains("win")) {
            consoleLogEnvVarReference = "!" + consoleLogEnvVar + "!";
        } else {
            consoleLogEnvVarReference = "${" + consoleLogEnvVar + "}";
        }

        // Set the log file name in an environment variable
        String logFileName = "GiveMeLibertyOrGiveMeDeath.log";
        Properties envVars = new Properties();
        envVars.put(consoleLogEnvVar, logFileName);

        // *********************
        // Create server.env
        // *********************
        String fileContents;
        if (expansionEnabled) {
            fileContents = "#enable_variable_expansion\nLOG_FILE=" + consoleLogEnvVarReference;
        } else {
            fileContents = "LOG_FILE=" + consoleLogEnvVarReference;
        }

        logString = "Creating /etc/server.env with contents:\n";

        Log.info(c, METHOD_NAME, logString + fileContents);
        sb.append(METHOD_NAME + logString + fileContents + "\n\n");

        File wlpEtcDir = new File(server.getInstallRoot(), "etc");
        Log.info(c, METHOD_NAME, "wlpEtcDir:" + wlpEtcDir.getAbsolutePath());
        String serverEnvFileFullPath = createServerEnvFile(fileContents, wlpEtcDir, sb);
        logString = "server.env location = " + serverEnvFileFullPath;
        Log.info(c, METHOD_NAME, logString);
        sb.append(METHOD_NAME + logString + "\n\n");
        assertTrue(sb.toString() + "\n\nThe server.env file was not created.", serverEnvFileFullPath.contains("server.env"));

        // *********************
        // Execute the server start command and display stdout and stderr
        // *********************
        String executionDir = server.getInstallRoot() + File.separator + "bin";
        String command = "." + File.separator + "server";
        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        String po_command = po.getCommand();
        int po_returnCode = po.getReturnCode();
        String po_stdout = po.getStdout();
        String po_stderr = po.getStderr();

        sb.append("\n\nSB: (start server)Program output:\ncommand="
                  + po_command + "\nrc=" + po_returnCode + "\n\nstdout:[  \n" + po_stdout + "]\n\nstderr: \n[" + po_stderr + "]");
        Log.info(c, METHOD_NAME, sb.toString());
        Log.info(c, METHOD_NAME, "po command: " + po_command);
        Log.info(c, METHOD_NAME, "po rc: " + po_returnCode);
        Log.info(c, METHOD_NAME, "po stdout: " + po_stdout);
        Log.info(c, METHOD_NAME, "po stderr:" + po_stderr);

        // *********************
        // Check Server Ready
        // *********************
        String serverReady = server.waitForStringInLog("CWWKF0011I");
        if (serverReady == null) {
            logString = "Timed out waiting for server ready message, CWWKF0011I";
            Log.info(c, METHOD_NAME, logString);
            sb.append(METHOD_NAME + logString + "\n\n");
        } else {
            logString = "Found server ready message, CWWKF0011I";
            Log.info(c, METHOD_NAME, logString);
            sb.append(METHOD_NAME + logString + "\n\n");
        }

        // Because we didn't start the server using the LibertyServer APIs, we need
        // to have it detect its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue(sb.toString() + "\n\nthe server should have been started:", server.isStarted());

        sb.append(logDirectoryContents(METHOD_NAME, new File(server.getLogsRoot()))); // DEBUG

        // Finally, verify that the log file with the new name exists when expansion is enabled
        String logFullPathName = server.getLogsRoot() + logFileName;
        File logFile = new File(logFullPathName);
        if (expansionEnabled) {
            assertTrue(sb.toString() + "\n\nthe log file with the new name [ " + logFullPathName + " ] does not exist", logFile.exists());
        } else {
            assertTrue(sb.toString() + "\n\nthe log file with the new name [ " + logFullPathName + " ] exists, but it should not", !logFile.exists());
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
    private String createServerEnvFile(String fileContents, File dir, StringBuffer sb) throws Exception {

        File serverEnvFile = new File(dir, "server.env");

        String serverEnvFileName = serverEnvFile.getAbsolutePath();

        if (!dir.exists()) {
            dir.mkdirs();
        }

//        Charset charset = Charset.forName("UTF-8");
//        Machine machine = Machine.getLocalMachine();
//        if (machine.getOperatingSystem() == OperatingSystem.ZOS) {
//            charset = Charset.forName("IBM-1047");
//        }

        FileOutputStream fos = null;
        PrintWriter pw = null;
        Writer w = null;
        try {
            fos = new FileOutputStream(serverEnvFile);

            Machine machine = Machine.getLocalMachine();
            if (machine.getOperatingSystem() == OperatingSystem.ZOS) {
                w = new OutputStreamWriter(fos, Charset.forName("IBM-1047"));
                sb.append("\n ZOS - set charset to IBM-1047\n");
            } else {
                w = new OutputStreamWriter(fos);
                sb.append("\n using default charset.\n");
            }

            pw = new PrintWriter(w);
            pw.println(fileContents);
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
        return serverEnvFileName;
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
    public static StringBuffer logDirectoryContents(final String methodName, File folder) {

        File[] listOfFiles = folder.listFiles();
        StringBuffer sb = new StringBuffer("");

        Log.info(c, methodName, "Server logs directory contents: ");
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    sb.append(methodName + "File: " + listOfFiles[i].getName() + "\n");
                    Log.info(c, methodName, "File: " + listOfFiles[i].getName());
                } else if (listOfFiles[i].isDirectory()) {
                    sb.append(methodName + "Directory: " + listOfFiles[i].getName() + "\n");
                    Log.info(c, methodName, "Directory: " + listOfFiles[i].getName());
                }
            }
        }
        return sb;
    }
}
