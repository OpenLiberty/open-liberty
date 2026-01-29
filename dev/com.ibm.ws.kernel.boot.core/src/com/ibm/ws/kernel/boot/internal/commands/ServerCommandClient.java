/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.text.MessageFormat;
import java.util.Set;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.ServerCommand;
import com.ibm.ws.kernel.boot.internal.ServerLock;

/**
 *
 */
public class ServerCommandClient extends ServerCommand {

    final String serverName;

    private static final char DELIM = '#';
    
    private static void debug(String message) {
        System.out.println("ServerCommandClient: " + message);
    }

    /**
     * This constructor is intended for use by a client. No server socket listener is
     * established.
     *
     * @param bootProps
     */
    public ServerCommandClient(BootstrapConfig bootProps) {
        super(bootProps);
        serverName = bootProps.getProcessName();
        this.serverUUID = "CLIENT";
        debug("Created for server: " + serverName + ", command file: " + commandFile.getAbsolutePath());
    }

    public boolean isValid() {
        boolean exists = this.commandFile.exists();
        debug("isValid() - command file exists: " + exists + " (" + commandFile.getAbsolutePath() + ")");
        return exists;
    }

    /**
     * Create a new server command instance: read from the
     * .sCommand file, obtain the UUID and the port from that file,
     * and construct a new command (to be sent to that port) containing
     * the UUID and the command string.
     *
     * @param command
     * @return
     * @throws Exception
     */
    private ServerCommandID createServerCommand(String command) throws IOException {
        debug("Reading command file: " + commandFile.getAbsolutePath());
        debug("Command file exists: " + commandFile.exists() + ", canRead: " + commandFile.canRead());
        FileInputStream fis = null;
        BufferedReader reader = null;

        try {
            fis = new FileInputStream(commandFile);
            reader = new BufferedReader(new InputStreamReader(fis));
            String line = reader.readLine();
            if (line == null)
                line = "";

            debug("Command file content: '" + line + "'");
            ServerCommandID cmdID = new ServerCommandID(line, command);
            debug("Parsed UUID: " + cmdID.getUUID() + ", Port: " + cmdID.getPort());
            return cmdID;
        } catch (IOException e) {
            debug("ERROR reading command file: " + e.getMessage());
            throw e;
        } finally {
            if (!Utils.tryToClose(reader)) {
                Utils.tryToClose(fis);
            }
        }
    }

    /**
     * Write a command to the server process.
     *
     * @param command the command to write
     * @param notStartedRC the return code if the server could not be reached
     * @param errorRC the return code if an error occurred while communicating
     *            with the server
     * @return {@link ReturnCode#OK} if the command was sent, notStartedRC if
     *         the server could not be reached, timeoutRC if the client timed
     *         out reading a response from the server, {@link ReturnCode#SERVER_COMMAND_PORT_DISABLED_STATUS} if the
     *         server's command port listener is disabled, or errorRC if any
     *         other communication error occurred
     */
    private ReturnCode write(String command, ReturnCode notStartedRC, ReturnCode errorRC) {
        debug("========================================");
        debug("Attempting to send command: " + command);
        SocketChannel channel = null;
        try {
            ServerCommandID commandID = createServerCommand(command);
            if (commandID.getPort() > 0) {
                InetAddress localhost = InetAddress.getByName(null);
                debug("Attempting connection to " + localhost.getHostAddress() + ":" + commandID.getPort());
                debug("Opening socket channel...");
                
                channel = SelectorProvider.provider().openSocketChannel();
                debug("Socket channel created, attempting connect...");
                
                long connectStart = System.currentTimeMillis();
                channel.connect(new InetSocketAddress(localhost, commandID.getPort()));
                long connectTime = System.currentTimeMillis() - connectStart;
                
                debug("Socket connected successfully in " + connectTime + "ms");

                // Write command.
                debug("Sending command string: " + commandID.getCommandString());
                write(channel, commandID.getCommandString());

                // Receive authorization challenge.
                debug("Reading authorization challenge");
                String authID = read(channel);
                debug("Received auth challenge: " + authID);

                // Respond to authorization challenge.
                File authFile = new File(commandAuthDir, authID);
                debug("Attempting to delete auth file: " + authFile.getAbsolutePath());
                // Delete a file created by the server (check for write access)
                if (!authFile.delete()) {
                    debug("ERROR - Could not delete auth file: " + authFile.getAbsolutePath());
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandAuthFailure"), command, authFile.getAbsolutePath()));
                    return errorRC;
                }
                debug("Auth file deleted successfully");

                // respond to the server to indicate the delete has happened.
                debug("Sending auth response");
                write(channel, authID);

                // Read command response.
                debug("Reading command response");
                String cmdResponse = read(channel), targetServerUUID = null, responseCode = null;
                debug("Received response: " + cmdResponse);
                if (cmdResponse.isEmpty()) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandCommFailure"), command));
                    debug("ERROR - Server returned empty response");
                    return errorRC;
                }

                if (cmdResponse.indexOf(DELIM) != -1) {
                    targetServerUUID = cmdResponse.substring(0, cmdResponse.indexOf(DELIM));
                    responseCode = cmdResponse.substring(cmdResponse.indexOf(DELIM) + 1);
                } else {
                    targetServerUUID = cmdResponse;
                }
                debug("Parsing response - targetUUID: " + targetServerUUID + ", responseCode: " + responseCode);
                if (!commandID.validateTarget(targetServerUUID)) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandCommFailure"), command));
                    debug("ERROR - UUID mismatch: expected=" + commandID.getUUID() + ", received=" + targetServerUUID);
                    return errorRC;
                }
                ReturnCode result = ReturnCode.OK;
                if (responseCode != null) {
                    try {
                        int returnCode = Integer.parseInt(responseCode.trim());
                        result = ReturnCode.getEnum(returnCode);
                        debug("Parsed return code: " + returnCode + " -> " + result);
                        if (result == ReturnCode.INVALID) {
                            debug("ERROR - Invalid return code: " + returnCode);
                            return ReturnCode.INVALID;
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandCommFailure"), command));
                        debug("ERROR - Could not parse return code: '" + responseCode + "'");
                        return ReturnCode.INVALID;
                    }
                }

                debug("Command completed successfully with result: " + result);
                return result;
            }

            if (commandID.getPort() == -1) {
                debug("Command port is disabled (port=-1)");
                return ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS;
            }

            debug("Server not started (port=" + commandID.getPort() + ")");
            return notStartedRC;
        } catch (ConnectException e) {
            debug("========================================");
            debug("CONNECTION REFUSED");
            debug("The server process is running but not accepting connections on the command port");
            debug("This usually means:");
            debug("  1. Server is still initializing and hasn't opened the command port yet");
            debug("  2. Server crashed after writing .sCommand file but before opening socket");
            debug("  3. Port conflict - another process grabbed the port");
            debug("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            debug("========================================");
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandCommFailure"), command));
            Debug.printStackTrace(e);
            return notStartedRC;
        } catch (IOException e) {
            debug("========================================");
            debug("I/O ERROR during communication");
            debug("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            debug("========================================");
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverCommandCommFailure"), command));
            Debug.printStackTrace(e);
            return ReturnCode.ERROR_COMMUNICATE_SERVER;
        } finally {
            Utils.tryToClose(channel);
        }
    }

    /**
     * Waits for the server to be fully started.
     *
     * @param lock the server lock, which must be held by the server process
     *            before this method is called
     */
    public ReturnCode startStatus(ServerLock lock) {
        debug("Starting status check for server: " + serverName);
        debug("Waiting for command file to appear");
        
        // The server process might not have created the command file yet.
        // Wait for it to appear.
        int waitCount = 0;
        while (!isValid()) {
            waitCount++;
            debug("Command file not found yet, wait attempt: " + waitCount);
            ReturnCode rc = startStatusWait(lock);
            if (rc != ReturnCode.START_STATUS_ACTION) {
                debug("Server process not running while waiting for command file (attempt " + waitCount + ")");
                return rc;
            }
        }
        
        debug("Command file found after " + waitCount + " wait(s), beginning status polling");

        for (int i = 0; i < BootstrapConstants.MAX_POLL_ATTEMPTS && isValid(); i++) {
            debug("Poll attempt " + (i + 1) + " of " + BootstrapConstants.MAX_POLL_ATTEMPTS);
            
            // Try to connect to the server's command file.  This might fail if
            // the command file is written but the server hasn't opened the
            // socket yet.
            ReturnCode rc = write(STATUS_START_COMMAND,
                                  ReturnCode.START_STATUS_ACTION,
                                  ReturnCode.ERROR_SERVER_START);
            if (rc != ReturnCode.START_STATUS_ACTION) {
                debug("Server responded successfully on poll attempt " + (i + 1));
                return rc;
            }

            debug("No response yet, waiting before retry");
            // Wait a bit, ensuring that the server process is still running.
            rc = startStatusWait(lock);
            if (rc != ReturnCode.START_STATUS_ACTION) {
                debug("Server process stopped running during polling (attempt " + (i + 1) + ")");
                return rc;
            }
        }

        debug("Exhausted all " + BootstrapConstants.MAX_POLL_ATTEMPTS + " polling attempts, making final attempt");
        return write(STATUS_START_COMMAND,
                     ReturnCode.ERROR_SERVER_START,
                     ReturnCode.ERROR_SERVER_START);
    }

    /**
     * Wait a bit because the server process could not be contacted, and then
     * verify that the server process is still running.
     *
     * @param lock
     * @return {@link ReturnCode#START_STATUS_ACTION} to try contacting the
     *         server process again, or another return code to give up
     */
    private ReturnCode startStatusWait(ServerLock lock) {
        debug("Sleeping for " + BootstrapConstants.POLL_INTERVAL_MS + "ms");
        try {
            Thread.sleep(BootstrapConstants.POLL_INTERVAL_MS);
        } catch (InterruptedException ex) {
            debug("Interrupted during sleep");
            Debug.printStackTrace(ex);
            return ReturnCode.ERROR_SERVER_START;
        }

        // This method is only called if the server process was holding
        // the server lock.  If this process is suddenly able to obtain the
        // lock, then the server process didn't finish starting.
        boolean serverRunning = lock.testServerRunning();
        debug("Server running check: " + serverRunning);
        if (!serverRunning) {
            debug("Server process is no longer running");
            return ReturnCode.ERROR_SERVER_START;
        }

        return ReturnCode.START_STATUS_ACTION;
    }

    /**
     * Stop the server by issuing a "stop" instruction to the server listener
     */
    public ReturnCode stopServer(boolean force) {
        return write(force ? FORCE_STOP_COMMAND : STOP_COMMAND,
                     ReturnCode.REDUNDANT_ACTION_STATUS,
                     ReturnCode.ERROR_SERVER_STOP);
    }

    /**
     * Dump the server by issuing a "introspect" instruction to the server listener
     */
    public ReturnCode introspectServer(String dumpTimestamp, Set<JavaDumpAction> javaDumpActions) {
        // Since "server dump" is used for diagnostics, we go out of our way to
        // not send an unrecognized command to the server even if the user has
        // broken their environment such that the client process supports java
        // dumps but the server doesn't.
        String command;
        if (javaDumpActions.isEmpty()) {
            command = INTROSPECT_COMMAND + DELIM + dumpTimestamp;
        } else {
            StringBuilder commandBuilder = new StringBuilder().append(INTROSPECT_JAVADUMP_COMMAND).append(DELIM).append(dumpTimestamp);
            for (JavaDumpAction javaDumpAction : javaDumpActions) {
                commandBuilder.append(',').append(javaDumpAction.name());
            }
            command = commandBuilder.toString();
        }

        return write(command,
                     ReturnCode.DUMP_ACTION,
                     ReturnCode.ERROR_SERVER_DUMP);
    }

    /**
     * Create a java dump of the server JVM by issuing a "javadump" instruction
     * to the server listener
     */
    public ReturnCode javaDump(Set<JavaDumpAction> javaDumpActions) {
        StringBuilder commandBuilder = new StringBuilder(JAVADUMP_COMMAND);
        char sep = DELIM;
        for (JavaDumpAction javaDumpAction : javaDumpActions) {
            commandBuilder.append(sep).append(javaDumpAction.toString());
            sep = ',';
        }

        return write(commandBuilder.toString(),
                     ReturnCode.SERVER_INACTIVE_STATUS,
                     ReturnCode.ERROR_SERVER_DUMP);
    }

    /**
     * Attempt to Stop the inbound work to a server by issuing a "pause" request
     * to the server.
     */
    public ReturnCode pause(String targetArg) {
        StringBuilder commandBuilder = new StringBuilder(PAUSE_COMMAND);

        char sep = DELIM;
        if (targetArg != null) {
            commandBuilder.append(sep).append(targetArg);
        }

        return write(commandBuilder.toString(),
                     ReturnCode.SERVER_INACTIVE_STATUS,
                     ReturnCode.ERROR_SERVER_PAUSE);
    }

    /**
     * Resume Inbound work to a server by issuing a "resume" request
     * to the server.
     */
    public ReturnCode resume(String targetArg) {
        StringBuilder commandBuilder = new StringBuilder(RESUME_COMMAND);

        char sep = DELIM;
        if (targetArg != null) {
            commandBuilder.append(sep).append(targetArg);
        }

        return write(commandBuilder.toString(),
                     ReturnCode.SERVER_INACTIVE_STATUS,
                     ReturnCode.ERROR_SERVER_RESUME);
    }
}