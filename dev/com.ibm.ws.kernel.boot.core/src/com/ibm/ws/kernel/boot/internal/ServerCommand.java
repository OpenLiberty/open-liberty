/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import com.ibm.ws.kernel.boot.BootstrapConfig;

/**
 *
 */
public abstract class ServerCommand {
    protected static final String STATUS_START_COMMAND = "status:start";
    protected static final String STOP_COMMAND = "stop";
    protected static final String FORCE_STOP_COMMAND = "forceStop";
    protected static final String INTROSPECT_COMMAND = "introspect";
    protected static final String INTROSPECT_JAVADUMP_COMMAND = "introspectJavadump";
    protected static final String JAVADUMP_COMMAND = "javadump";
    protected static final String PAUSE_COMMAND = "pause";
    protected static final String RESUME_COMMAND = "resume";

    private final CharsetDecoder decoder = StandardCharsets.ISO_8859_1.newDecoder();
    private final CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();

    private final ByteBuffer buffer = ByteBuffer.allocate(256);
    private final CharBuffer charBuffer = CharBuffer.allocate(256);

    /**
     * The value of the UUID associated with this server
     */
    protected String serverUUID;

    /**
     * The file that contains the UUID and port information
     */
    protected final File commandFile;

    /**
     * The directory for command authorization checks.
     */
    protected final File commandAuthDir;

    public ServerCommand(BootstrapConfig bootProps) {
        commandFile = bootProps.getWorkareaFile(BootstrapConstants.S_COMMAND_FILE);
        commandAuthDir = bootProps.getWorkareaFile(BootstrapConstants.S_COMMAND_AUTH_DIR);
    }

    /**
     * Reads a command or command response from a socket channel.
     *
     * <p>This method is not safe for use by multiple concurrent threads.
     *
     * @param sc the socket channel
     * @return the command or command response
     */
    protected String read(SocketChannel sc) throws IOException {
        sc.read(buffer);
        buffer.flip();

        decoder.decode(buffer, charBuffer, true);
        charBuffer.flip();

        String result = charBuffer.toString();

        // Clear out buffers
        buffer.clear();
        charBuffer.clear();
        decoder.reset();

        return result;
    }

    /**
     * Writes a command or command response to a socket channel.
     *
     * <p>This method is not safe for use by multiple concurrent threads.
     *
     * @param sc the socket channel
     * @param s the command or command response
     */
    protected void write(SocketChannel sc, String s) throws IOException {
        sc.write(encoder.encode(CharBuffer.wrap(s)));
    }

    /**
     * Utility class to represent the UUID, port, and operation
     */
    public class ServerCommandID {
        /**
         * The delimiter between the UUID+port and the operation.
         */
        private static final char OPERATION_DELIMITER = ',';

        /**
         * The delimiter between the UUID and port.
         *
         * <p>For compatibility, this value must NOT be '.'. The GA release of
         * the tools attempted to find, parse, and use the .sCommand directly
         * before falling back to the server script. 71450 required a change
         * to the .sCommand protocol, so we need to force the tools to use the
         * server script fallback. We can either move the .sCommand, break the
         * parsing, or somehow cause the socket write to fail. We chose to
         * break the parsing by using a different port delimiter.
         */
        private static final char PORT_DELIMITER = ':';

        private String uuid = null;
        private int port;
        private String operation;

        /**
         *
         * @param uuidAndPort A string of the form UUID.port
         * @param operation
         */
        public ServerCommandID(String uuidAndPort, String operation) {
            int idx = uuidAndPort.indexOf(PORT_DELIMITER);
            if (idx == -1)
                return;
            this.uuid = uuidAndPort.substring(0, idx);
            this.port = Integer.valueOf(uuidAndPort.substring(idx + 1));
            this.operation = operation;
        }

        /**
         * @param uuidAndCommand A string of the form UUID,command
         */
        public ServerCommandID(String uuidAndCommand) {
            int idx = uuidAndCommand.indexOf(OPERATION_DELIMITER);
            if (idx == -1)
                return;
            this.uuid = uuidAndCommand.substring(0, idx);
            this.operation = uuidAndCommand.substring(idx + 1);
        }

        /**
         *
         * @param port The port value of the server socket
         * @param uuid The server UUID value
         */
        public ServerCommandID(int port, String uuid) {
            this.port = port;
            this.uuid = uuid;
        }

        /**
         * Get the command string to send to the server
         *
         * @return
         */
        public String getCommandString() {
            return this.uuid + OPERATION_DELIMITER + this.operation;
        }

        /**
         * Get the ID string to store in the server command file
         *
         * @return
         */
        public String getIDString() {
            return this.uuid + PORT_DELIMITER + this.port;
        }

        /**
         * Get the port that the server is using to listen for requests
         *
         * @return
         */
        public int getPort() {
            return this.port;
        }

        /**
         * Get the operation
         *
         * @return
         */
        public String getOperation() {
            return this.operation;
        }

        /**
         * Check to see if the server UUID equals the provided UUID
         *
         * @return
         */
        public boolean validate() {
            return ServerCommand.this.serverUUID.equals(this.uuid);
        }

        /**
         * Check to see if the target server UUID equals the command UUID.
         */
        public boolean validateTarget(String targetServerUUID) {
            return targetServerUUID.equals(this.uuid);
        }

        /**
         * @return
         */
        public String getUUID() {
            return this.uuid;
        }
    }

}
