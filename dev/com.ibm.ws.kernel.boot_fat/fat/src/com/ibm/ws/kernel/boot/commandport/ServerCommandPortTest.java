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
package com.ibm.ws.kernel.boot.commandport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * This test bucket validates the functionality of the server command port, the port that is used by the server script
 * to communicate with a running server, if required.
 */
public class ServerCommandPortTest {
    private static final String COMMAND_PORT_DISABLED_SERVER_NAME = "com.ibm.ws.kernel.boot.commandport.disabled.fat";
    private static final String COMMAND_PORT_ENABLED_SERVER_NAME = "com.ibm.ws.kernel.boot.commandport.enabled.fat";

    private static final LibertyServer commandPortDisabledServer = LibertyServerFactory.getLibertyServer(COMMAND_PORT_DISABLED_SERVER_NAME);
    private static final LibertyServer commandPortEnabledServer = LibertyServerFactory.getLibertyServer(COMMAND_PORT_ENABLED_SERVER_NAME);

    private static final boolean isMac = System.getProperty("os.name", "unknown").toLowerCase().indexOf("mac os") >= 0;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setupApp() throws Exception {
        ShrinkHelper.defaultApp(commandPortDisabledServer, "shutdownfat", "com.ibm.ws.kernel.boot.fat");
    }

    @Test
    /**
     * This test validates the functionality of a server with the command port disabled. The server script has
     * only limited ability to administer a server with the command port disabled.
     *
     * @throws Exception
     */
    public void testServerCommandPortDisabled() throws Exception {
        LibertyServer server = commandPortDisabledServer;
        JavaInfo java = JavaInfo.forCurrentVM();
//        if (java.majorVersion() != 8) {
//            server.copyFileToLibertyServerRoot("illegalAccess/jvm.options");
//        }

        // server should start, but with a warning message that we can't actually tell if it completed starting
        // because the command port is disabled
        //
        // NOTE:  don't use "server.startServer()" because this causes the LibertyServer to remember that it is
        //        started and it will try to stop it during test cleanup, failing due to command port disablement
        String output = server.executeServerScript("start", null).getStdout();

        try {
            // there are two messages for starting a server, one that contains a process ID and one that does not;
            // either message indicates success of the start command for this purpose
            assertTrue(output.contains("CWWKE0088W") || output.contains("CWWKE0087W"));
            server.waitForStringInLog("CWWKF0011I");

            // save the PID to validate the server status command - note that on Windows, the PID is not returned
            // as part of the start message, so we can't always perform this verification
            int serverPid = getServerPid(output);

            // server status should work as usual, since the command port is not needed for it
            output = server.executeServerScript("status", null).getStdout();
            assertTrue(output.contains("Server " + server.getServerName() + " is running"));

            if (serverPid > 0) {
                assertTrue(output.contains("with process ID " + serverPid));
            }

            // ensure that the command port in the .sCommand file is -1
            assertEquals(-1, getCommandPort(server));

            // generating a java dump should produce an error saying that the dump can't be taken due to command
            // port disablement
            output = server.executeServerScript("javadump", null).getStdout();
            assertTrue(output.contains("CWWKE0091E"));

            // generating a server dump should produce a warning saying that the dump was taken, but that some
            // information is missing
            output = server.executeServerScript("dump", null).getStdout();
            assertTrue(output.contains("CWWKE0090W"));
            validateDumpFile(output, server, COMMAND_PORT_DISABLED_SERVER_NAME);

            output = server.executeServerScript("pause", null).getStdout();
            assertTrue(output.contains("CWWKE0944E"));

            output = server.executeServerScript("resume", null).getStdout();
            assertTrue(output.contains("CWWKE0945E"));

            // stopping the server should produce an error
            output = server.executeServerScript("stop", null).getStdout();
            assertTrue(output.contains("CWWKE0089E"));
        } finally {
            Log.info(ServerCommandPortTest.class, "testServerCommandPortDisabled",
                     "Stop the server via a servlet since the command port is disabled.");
            try {
                // make sure the shutdown app is available before using it to shutdown the server
                server.validateAppLoaded("shutdownfat");

                HttpUtils.findStringInUrl(server, "/shutdownfat", "exit=");
            } catch (Throwable t) {
                Log.error(ServerCommandPortTest.class, "testServerCommandPortDisabled", t);
            }
            server.waitForStringInLog("CWWKE0036I");

            // make sure logs are collected (not stopping the normal way...no command port)
            server.postStopServerArchive();
        }
    }

    @Test
    /**
     * This test validates the functionality of a server with the command port enabled.
     *
     * @throws Exception
     */
    public void testServerCommandPortEnabled() throws Exception {
        LibertyServer server = commandPortEnabledServer;

        JavaInfo java = JavaInfo.forCurrentVM();
//        if (java.majorVersion() != 8) {
//            server.copyFileToLibertyServerRoot("illegalAccess/jvm.options");
//        }

        String output = server.startServer().getStdout();
        assertTrue(output.contains("Server " + server.getServerName() + " started"));

        // save the PID to validate the server status command - note that on Windows, the PID is not returned
        // as part of the start message, so we can't always perform this verification
        int serverPid = getServerPid(output);

        // validate server status command
        output = server.executeServerScript("status", null).getStdout();
        assertTrue(output.contains("Server " + server.getServerName() + " is running"));

        if (serverPid > 0) {
            assertTrue(output.contains("with process ID " + serverPid));
        }

        // ensure that the command port in the .sCommand file is greater than 0
        assertTrue(getCommandPort(server) > 0);

        // validate server javadump command on all platforms except mac, because javadump
        // is unreliable on hotspot jvms
        if (!isMac) {
            output = server.executeServerScript("javadump", null).getStdout();
            assertTrue(output.contains("Server " + server.getServerName() + " dump complete in"));
            validateDumpFile(output, server, COMMAND_PORT_ENABLED_SERVER_NAME);
        }

        // validate server dump command
        output = server.executeServerScript("dump", null).getStdout();
        assertTrue(output.contains("Server " + server.getServerName() + " dump complete in"));
        validateDumpFile(output, server, COMMAND_PORT_ENABLED_SERVER_NAME);

        // validate server stop command
        output = server.stopServer().getStdout();
        assertTrue(output.contains("Server " + server.getServerName() + " stopped."));

        // make sure server really stopped
        output = server.executeServerScript("status", null).getStdout();
        assertTrue(output.contains("Server " + server.getServerName() + " is not running."));
    }

    /**
     * Validates the existence of a dump file. The dump file name is parsed from the input parameter,
     * which is the output of the server script command that requested the dump.
     *
     * @param outputMessage the output of the server script command that requested the dump
     */
    private void validateDumpFile(String outputMessage, LibertyServer server, final String serverName) throws Exception {
        String dumpFileName = null;

        // The format of the dump complete message is:
        //   Server <SERVER_NAME> dump complete in <FILENAME>.
        // The filename comes after "in" and has a period attached.
        boolean foundServerName = false;
        String[] words = outputMessage.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            // we are checking for the message header here to avoid a Mac JDK bug
            // that prints a message that includes the word "in" but then causes a
            // StringIndexOutOfBoundsException below.  This check could be removed
            // when the Mac bug is fixed, but it doesn't hurt to leave it in.
            // See defects 124026 and 123011 for more details.
            if (i > 0 && words[i - 1].equals("Server") && words[i].equals(serverName)) {
                foundServerName = true;
            } else if (words[i].equals("in") && foundServerName && i < (words.length - 1)) {
                dumpFileName = words[i + 1];

                int serverRootLength = server.getServerRoot().length();
                int periodIndex = dumpFileName.length() - 1;

                // strip the server root off the front and the period off the end
                dumpFileName = dumpFileName.substring(serverRootLength, periodIndex);
                break;
            }
        }

        // verify that the server dump file exists and that *something* is inside it
        RemoteFile dumpFile = server.getFileFromLibertyServerRoot(dumpFileName);
        assertTrue(dumpFile.exists());
        assertTrue(dumpFile.length() > 0);
    }

    /**
     * Parses and returns the server PID from the output of the server start command.
     *
     * @param outputMessage the output of the server start command
     * @return the server's PID, or -1 if the PID cannot be parsed from the command output
     */
    private int getServerPid(String outputMessage) throws Exception {
        // The format of the server started message is:
        //   Server <SERVER_NAME> started with process ID <PID>.
        // The PID comes after "ID" and has a period or comma attached.
        String[] words = outputMessage.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("ID")) {
                String pidString = words[i + 1];
                pidString = pidString.substring(0, pidString.length() - 2); // strip trailing period
                return Integer.parseInt(pidString);
            }
        }
        return -1;
    }

    /**
     * Gets the current command port for the specified server. Note that this is
     * NOT something that is visible to the customer. This is a test of our own
     * internals added as a secondary verification that command port enablement
     * and disablement works.
     *
     * @param server the server to get the command port for
     * @return the server's command port
     */
    private int getCommandPort(LibertyServer server) throws Exception {
        RemoteFile commandFile = server.getFileFromLibertyServerRoot("workarea/.sCommand");
        BufferedReader reader = new BufferedReader(new InputStreamReader(commandFile.openForReading()));
        String line = reader.readLine();
        reader.close();
        return Integer.parseInt(line.split(":")[1]);
    }
}
