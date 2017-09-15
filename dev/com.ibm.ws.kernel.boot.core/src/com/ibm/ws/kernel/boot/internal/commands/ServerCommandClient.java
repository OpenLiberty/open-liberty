/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    }

    public boolean isValid() {
        return this.commandFile.exists();
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
        FileInputStream fis = null;
        BufferedReader reader = null;

        try {
            fis = new FileInputStream(commandFile);
            reader = new BufferedReader(new InputStreamReader(fis));
            String line = reader.readLine();
            if (line == null)
                line = "";

            return new ServerCommandID(line, command);
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
        SocketChannel channel = null;
        try {
            ServerCommandID commandID = createServerCommand(command);
            if (commandID.getPort() > 0) {
                channel = SelectorProvider.provider().openSocketChannel();
                channel.connect(new InetSocketAddress(InetAddress.getByName(null), commandID.getPort()));

                // Write command.
                write(channel, commandID.getCommandString());

                // Receive authorization challenge.
                String authID = read(channel);

                // Respond to authorization challenge.
                File authFile = new File(commandAuthDir, authID);
                // Delete a file created by the server (check for write access)
                authFile.delete();

                // respond to the server to indicate the delete has happened.
                write(channel, authID);

                // Read command response.
                String cmdResponse = read(channel), targetServerUUID = null, responseCode = null;
                if (cmdResponse.isEmpty()) {
                    throw new IOException("connection closed by server without a reply");
                }

                if (cmdResponse.indexOf(DELIM) != -1) {
                    targetServerUUID = cmdResponse.substring(0, cmdResponse.indexOf(DELIM));
                    responseCode = cmdResponse.substring(cmdResponse.indexOf(DELIM) + 1);
                } else {
                    targetServerUUID = cmdResponse;
                }
                if (!commandID.validateTarget(targetServerUUID)) {
                    throw new IOException("command file mismatch");
                }
                ReturnCode result = ReturnCode.OK;
                if (responseCode != null) {
                    try {
                        int returnCode = Integer.parseInt(responseCode.trim());
                        result = ReturnCode.getEnum(returnCode);
                    } catch (NumberFormatException nfe) {
                        throw new IOException("invalid return code");
                    }
                }
                if (result == ReturnCode.INVALID) {
                    throw new IOException("invalid return code");
                }
                return result;
            }

            if (commandID.getPort() == -1) {
                return ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS;
            }

            return notStartedRC;
        } catch (ConnectException e) {
            Debug.printStackTrace(e);
            return notStartedRC;
        } catch (IOException e) {
            Debug.printStackTrace(e);
            return errorRC;
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
        // The server process might not have created the command file yet.
        // Wait for it to appear.
        while (!isValid()) {
            ReturnCode rc = startStatusWait(lock);
            if (rc != ReturnCode.START_STATUS_ACTION) {
                return rc;
            }
        }

        for (int i = 0; i < BootstrapConstants.MAX_POLL_ATTEMPTS && isValid(); i++) {
            // Try to connect to the server's command file.  This might fail if
            // the command file is written but the server hasn't opened the
            // socket yet.
            ReturnCode rc = write(STATUS_START_COMMAND,
                                  ReturnCode.START_STATUS_ACTION,
                                  ReturnCode.ERROR_SERVER_START);
            if (rc != ReturnCode.START_STATUS_ACTION) {
                return rc;
            }

            // Wait a bit, ensuring that the server process is still running.
            rc = startStatusWait(lock);
            if (rc != ReturnCode.START_STATUS_ACTION) {
                return rc;
            }
        }

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
        try {
            Thread.sleep(BootstrapConstants.POLL_INTERVAL_MS);
        } catch (InterruptedException ex) {
            Debug.printStackTrace(ex);
            return ReturnCode.ERROR_SERVER_START;
        }

        // This method is only called if the server process was holding
        // the server lock.  If this process is suddenly able to obtain the
        // lock, then the server process didn't finish starting.
        if (!lock.testServerRunning()) {
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